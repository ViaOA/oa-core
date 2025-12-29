/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.sync;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.sync.remote.RemoteSessionInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;

/**
 * Central registry and routing utility for OA's synchronization subsystem.
 * <p>
 * {@code OASyncDelegate} maintains all static state needed by OA's distributed
 * model, including:
 * <ul>
 *   <li>the active {@link OASyncServer} and {@link OASyncClient} instances,
 *       mapped per {@link Package},</li>
 *   <li>the remote interfaces used for distributed object graph operations,
 *       including {@code RemoteSyncInterface},
 *       {@code RemoteServerInterface}, {@code RemoteSessionInterface}, and
 *       {@code RemoteClientInterface},</li>
 *   <li>thread-local request metadata used while a remote method is executing,
 *       and</li>
 *   <li>helper methods to determine whether the current JVM is functioning as
 *       a client or as a server for a given model package.</li>
 * </ul>
 *
 * <h3>Singleton-or-Map Strategy</h3>
 * For each type of sync component (server, client, remote interfaces),
 * {@code OASyncDelegate} maintains:
 * <ul>
 *   <li>a single-instance reference when only one exists in the JVM, and</li>
 *   <li>a per-package map when multiple models or sync contexts are present.</li>
 * </ul>
 * This enables fast lookups in the common case while fully supporting multiple
 * independently synchronized OA models in the same process.
 *
 * <h3>Execution Role Detection</h3>
 * Methods such as {@link #isServer(Class)} and {@link #isClient(Class)} use
 * the presence or absence of registered clients and servers to infer whether
 * the current code path represents a server-side or client-side execution
 * context.
 *
 * <p>
 * All synchronization features in OA ultimately funnel through this delegate.
 */
public class OASyncDelegate {
	private static Logger LOG = Logger.getLogger(OASyncDelegate.class.getName());

	/**
	 * Default package used when no specific model package is supplied.
	 * All lookups fall back to this package if no explicit mapping exists.
	 */
	public static final Package ObjectPackage = Object.class.getPackage();

	/**
	 * Per-package registry of {@link RemoteServerInterface} instances.
	 * Used by clients to invoke server-side synchronization operations.
	 */
	private static final ConcurrentHashMap<Package, RemoteServerInterface> hmRemoteServer = new ConcurrentHashMap<Package, RemoteServerInterface>();

	/**
	 * Cached single-instance {@link RemoteServerInterface} when only one
	 * server exists in the JVM. Set to null when multiple servers are registered.
	 */
	private static RemoteServerInterface remoteServerInterface;

	/**
	 * Per-package registry of {@link RemoteSyncInterface} instances,
	 * used for distributing sync messages across clients and servers.
	 */
	private static final ConcurrentHashMap<Package, RemoteSyncInterface> hmRemoteSync = new ConcurrentHashMap<Package, RemoteSyncInterface>();

	/**
	 * Cached single-instance {@link RemoteSyncInterface} when only one
	 * sync context is active in the JVM. Set to null when multiple exist.
	 */
	private static RemoteSyncInterface remoteSyncInterface;

	/**
	 * Per-package registry of {@link RemoteSessionInterface} objects,
	 * representing per-client sessions on a sync server.
	 */
	private static final ConcurrentHashMap<Package, RemoteSessionInterface> hmRemoteSession = new ConcurrentHashMap<Package, RemoteSessionInterface>();

	/**
	 * Cached single-instance {@link RemoteSessionInterface}, or null if
	 * multiple distinct package sessions are registered.
	 */
	private static RemoteSessionInterface remoteSessionInterface;

	/**
	 * Per-package registry of {@link RemoteClientInterface} instances.
	 * These provide client-side callback methods invoked by the server.
	 */
	private static final ConcurrentHashMap<Package, RemoteClientInterface> hmRemoteClient = new ConcurrentHashMap<Package, RemoteClientInterface>();

	/**
	 * Cached reference to the single {@link RemoteClientInterface} instance
	 * when only one exists in the JVM.
	 */
	private static RemoteClientInterface remoteClientInterface;

	/**
	 * Registry of {@link OASyncClient} instances keyed by model package.
	 * Enables multiple independently synchronized models in the same JVM.
	 */
	private static final ConcurrentHashMap<Package, OASyncClient> hmSyncClient = new ConcurrentHashMap<Package, OASyncClient>();

	/**
	 * Cached single {@link OASyncClient} instance if only one exists;
	 * otherwise set to {@code null} to force per-package lookup.
	 */
	private static OASyncClient oaSyncClient;

	/**
	 * Per-package registry of {@link OASyncServer} instances. A sync server
	 * enables inbound client connections for distributed object graph updates.
	 */
	private static final ConcurrentHashMap<Package, OASyncServer> hmSyncServer = new ConcurrentHashMap<Package, OASyncServer>();

	/**
	 * Cached reference to a single {@link OASyncServer} when only one is
	 * active in the JVM. Set to {@code null} when multiple servers exist.
	 */
	private static OASyncServer oaSyncServer;

	/**
	 * Cache mapping classes to their owning package, used to determine the
	 * sync context for objects, hubs, and classes without repeated lookups.
	 */
	private static final ConcurrentHashMap<Class, Package> hmClassPackage = new ConcurrentHashMap<Class, Package>();

	/**
	 * Returns the package associated with the given class. If the class is
	 * {@code null}, returns {@link #ObjectPackage}. Results are cached.
	 *
	 * @param c the class whose package is requested
	 * @return the resolved package
	 */
	public static Package getPackage(Class c) {
		Package p;
		if (c == null) {
			p = ObjectPackage;
		} else {
			p = hmClassPackage.computeIfAbsent(c, k -> c.getPackage());
		}
		return p;
	}

	// ========= SyncServer ============

	/**
	 * Returns the active {@link OASyncServer}. If a single server instance
	 * exists, it is returned; otherwise package-based lookup is used.
	 *
	 * @return the sync server instance, or {@code null} if not registered
	 */
	public static OASyncServer getSyncServer() {
		if (oaSyncServer != null) {
			return oaSyncServer;
		}
		return getSyncServer(getPackage(null));
	}

	/**
	 * Returns the {@link OASyncServer} for the package associated with the
	 * given class. If a global server is cached, it is returned.
	 *
	 * @param c the class whose sync server is requested
	 * @return the matching sync server or {@code null} if none
	 */
	public static OASyncServer getSyncServer(Class c) {
		if (oaSyncServer != null) {
			return oaSyncServer;
		}
		return getSyncServer(getPackage(c));
	}

	/**
	 * Returns the {@link OASyncServer} for the given object's package.
	 * If a global server is cached, it is returned.
	 *
	 * @param obj the object whose sync server is requested
	 * @return the sync server instance or {@code null}
	 */
	public static OASyncServer getSyncServer(OAObject obj) {
		if (oaSyncServer != null) {
			return oaSyncServer;
		}
		Class c = obj == null ? null : obj.getClass();
		return getSyncServer(getPackage(c));
	}

	/**
	 * Returns the {@link OASyncServer} for the package of objects held
	 * by the supplied {@link Hub}. Falls back to global instance when set.
	 *
	 * @param h the hub used to determine the package context
	 * @return the sync server instance or {@code null}
	 */
	public static OASyncServer getSyncServer(Hub h) {
		if (oaSyncServer != null) {
			return oaSyncServer;
		}
		Class c;
		if (h != null) {
			c = h.getObjectClass();
		} else {
			c = null;
		}
		return getSyncServer(getPackage(c));
	}

	/**
	 * Returns the {@link OASyncServer} registered for the specified package.
	 * Falls back to the {@link #ObjectPackage} registration when needed.
	 *
	 * @param p the package whose sync server is requested
	 * @return the matching sync server, or {@code null} if none found
	 */
	public static OASyncServer getSyncServer(Package p) {
		if (oaSyncServer != null) {
			return oaSyncServer;
		}
		if (p == null) {
			p = ObjectPackage;
		}
		OASyncServer ss = hmSyncServer.get(p);
		if (ss == null && p != ObjectPackage) {
			ss = hmSyncServer.get(ObjectPackage);
		}
		return ss;
	}

	/**
	 * Registers or removes the global {@link OASyncServer}. Delegates to
	 * {@link #setSyncServer(Package, OASyncServer)} using the default package.
	 *
	 * @param ss the sync server instance to register, or {@code null} to remove
	 */
	public static void setSyncServer(OASyncServer ss) {
		setSyncServer(null, ss);
	}

	/**
	 * Registers or removes an {@link OASyncServer} for a specific package.
	 * Maintains both:
	 * <ul>
	 *   <li>a per-package mapping, and</li>
	 *   <li>a cached global instance when only one server exists.</li>
	 * </ul>
	 *
	 * @param p the package to associate with the server
	 * @param ss the server instance, or {@code null} to remove
	 */
	public static void setSyncServer(Package p, OASyncServer ss) {
		if (p != null && p != ObjectPackage) {
			if (ss != null) {
				hmSyncServer.put(p, ss);
				hmSyncServer.computeIfAbsent(ObjectPackage, k -> ss);
				if (oaSyncServer == null) {
					oaSyncServer = ss;
				} else if (oaSyncServer != ss) {
					oaSyncServer = null;
				}
			} else {
				OASyncServer ssx = hmSyncServer.remove(p);
				if (hmSyncServer.get(ObjectPackage) == ssx) {
					hmSyncServer.remove(ObjectPackage);
				}
				if (oaSyncServer == ssx) {
					oaSyncServer = null;
				}
			}
		} else {
			p = ObjectPackage;
			if (ss != null) {
				if (oaSyncServer == null) {
					oaSyncServer = ss;
				} else if (oaSyncServer != ss) {
					oaSyncServer = null;
				}
				hmSyncServer.put(p, ss);
			} else {
				OASyncServer ssx = hmSyncServer.remove(p);
				if (oaSyncServer == ssx) {
					oaSyncServer = null;
				}
			}
		}
	}

	// ========= SyncClient ============
	/**
	 * Returns the active {@link OASyncClient}. If a single client instance
	 * exists, it is returned; otherwise package-based lookup is used.
	 *
	 * @return the sync client instance, or {@code null} if not registered
	 */
	public static OASyncClient getSyncClient() {
		if (oaSyncClient != null) {
			return oaSyncClient;
		}
		return getSyncClient(getPackage(null));
	}

	/**
	 * Returns the {@link OASyncClient} for the package associated with the
	 * given class. If a global client is cached, it is returned immediately.
	 *
	 * @param c the class whose sync client is requested
	 * @return the matching sync client or {@code null}
	 */
	public static OASyncClient getSyncClient(Class c) {
		if (oaSyncClient != null) {
			return oaSyncClient;
		}
		return getSyncClient(getPackage(c));
	}

	/**
	 * Returns the {@link OASyncClient} for the specified object's package.
	 * Falls back to the global client if present.
	 *
	 * @param obj the object whose sync client is requested
	 * @return the sync client instance or {@code null}
	 */
	public static OASyncClient getSyncClient(OAObject obj) {
		if (oaSyncClient != null) {
			return oaSyncClient;
		}
		Class c = obj == null ? null : obj.getClass();
		return getSyncClient(getPackage(c));
	}

	/**
	 * Returns the {@link OASyncClient} for the package represented by the
	 * supplied {@link Hub}. If a global instance is cached, it is returned.
	 *
	 * @param h the hub used to determine the package context
	 * @return the sync client or {@code null}
	 */
	public static OASyncClient getSyncClient(Hub h) {
		if (oaSyncClient != null) {
			return oaSyncClient;
		}
		Class c;
		if (h != null) {
			c = h.getObjectClass();
		} else {
			c = null;
		}
		return getSyncClient(getPackage(c));
	}

	/**
	 * Retrieves the {@link OASyncClient} associated with a given package.
	 * Falls back to the {@link #ObjectPackage} registration when needed.
	 *
	 * @param p the package whose client is requested
	 * @return the matching sync client, or {@code null} if none found
	 */
	public static OASyncClient getSyncClient(Package p) {
		if (oaSyncClient != null) {
			return oaSyncClient;
		}
		if (p == null) {
			p = ObjectPackage;
		}
		OASyncClient sc = hmSyncClient.get(p);
		if (sc == null && p != ObjectPackage) {
			sc = hmSyncClient.get(ObjectPackage);
		}
		return sc;
	}

	/**
	 * Registers or removes the global {@link OASyncClient} by delegating
	 * to {@link #setSyncClient(Package, OASyncClient)} with a default package.
	 *
	 * @param sc the sync client to register, or {@code null} to remove
	 */
	public static void setSyncClient(OASyncClient sc) {
		setSyncClient(null, sc);
	}

	/**
	 * Registers or removes an {@link OASyncClient} for the specified package.
	 * Maintains both a per-package entry and a cached global instance when
	 * only one client exists.
	 *
	 * @param p the package to associate with the client
	 * @param sc the client instance, or {@code null} to remove
	 */
	public static void setSyncClient(Package p, OASyncClient sc) {
		if (p != null && p != ObjectPackage) {
			if (sc != null) {
				hmSyncClient.put(p, sc);
				hmSyncClient.computeIfAbsent(ObjectPackage, k -> sc);
				if (oaSyncClient == null) {
					oaSyncClient = sc;
				} else if (oaSyncClient != sc) {
					oaSyncClient = null;
				}
			} else {
				OASyncClient ssx = hmSyncClient.remove(p);
				if (hmSyncClient.get(ObjectPackage) == ssx) {
					hmSyncClient.remove(ObjectPackage);
				}
				if (oaSyncClient == sc) {
					oaSyncClient = null;
				}
			}
		} else {
			p = ObjectPackage;
			if (sc != null) {
				if (oaSyncClient == null) {
					oaSyncClient = sc;
				} else if (oaSyncClient != sc) {
					oaSyncClient = null;
				}
				hmSyncClient.put(p, sc);
			} else {
				OASyncClient scx = hmSyncClient.remove(p);
				if (oaSyncClient == scx) {
					oaSyncClient = null;
				}
			}
		}
	}

	// ========= RemoteServerInterface ============
	/**
	 * Returns the active {@link RemoteServerInterface}. If a single instance
	 * exists, it is returned; otherwise falls back to package-based lookup.
	 *
	 * @return the remote server interface or {@code null}
	 */
	public static RemoteServerInterface getRemoteServer() {
		if (remoteServerInterface != null) {
			return remoteServerInterface;
		}
		return getRemoteServer(getPackage(null));
	}

	/**
	 * Retrieves the {@link RemoteServerInterface} for the package associated
	 * with the given class. Uses global cached instance when present.
	 *
	 * @param c the class representing the package context
	 * @return the remote server interface or {@code null}
	 */
	public static RemoteServerInterface getRemoteServer(Class c) {
		if (remoteServerInterface != null) {
			return remoteServerInterface;
		}
		return getRemoteServer(getPackage(c));
	}

	/**
	 * Retrieves the {@link RemoteServerInterface} for the given object’s
	 * package. Uses global cached instance when available.
	 *
	 * @param obj the object used for package resolution
	 * @return the remote server interface or {@code null}
	 */
	public static RemoteServerInterface getRemoteServer(OAObject obj) {
		if (remoteServerInterface != null) {
			return remoteServerInterface;
		}
		Class c = obj == null ? null : obj.getClass();
		return getRemoteServer(getPackage(c));
	}

	/**
	 * Retrieves the {@link RemoteServerInterface} for the package implied
	 * by the supplied {@link Hub}. Uses global instance when set.
	 *
	 * @param h the hub used to determine the package
	 * @return the remote server interface or {@code null}
	 */
	public static RemoteServerInterface getRemoteServer(Hub h) {
		if (remoteServerInterface != null) {
			return remoteServerInterface;
		}
		Class c;
		if (h != null) {
			c = h.getObjectClass();
		} else {
			c = null;
		}
		return getRemoteServer(getPackage(c));
	}

	/**
	 * Retrieves the {@link RemoteServerInterface} registered for the given
	 * package. Falls back to the {@link #ObjectPackage} registration when
	 * appropriate. Uses cached global instance if available.
	 *
	 * @param p the package whose remote server interface is requested
	 * @return the interface instance or {@code null}
	 */
	public static RemoteServerInterface getRemoteServer(Package p) {
		if (remoteServerInterface != null) {
			return remoteServerInterface;
		}
		if (p == null) {
			p = ObjectPackage;
		}
		RemoteServerInterface rs = hmRemoteServer.get(p);
		if (rs == null && p != ObjectPackage) {
			rs = hmRemoteServer.get(ObjectPackage);
		}
		return rs;
	}

	/**
	 * Registers or removes the global {@link RemoteServerInterface}. Delegates
	 * to package-based registration using the default package.
	 *
	 * @param rs the server interface instance, or {@code null} to remove
	 */
	public static void setRemoteServer(RemoteServerInterface rs) {
		setRemoteServer(null, rs);
	}

	/**
	 * Registers or removes a {@link RemoteServerInterface} for the specified
	 * package. Maintains both a per-package entry and a cached global instance
	 * when only one server interface exists.
	 *
	 * @param p the package to associate with the remote interface
	 * @param rs the remote server interface, or {@code null} to remove
	 */
	public static void setRemoteServer(Package p, RemoteServerInterface rs) {
		if (p != null && p != ObjectPackage) {
			if (rs != null) {
				hmRemoteServer.put(p, rs);
				hmRemoteServer.computeIfAbsent(ObjectPackage, k -> rs);

				if (remoteServerInterface == null) {
					remoteServerInterface = rs;
				} else if (remoteServerInterface != rs) {
					remoteServerInterface = null;
				}
			} else {
				RemoteServerInterface rsx = hmRemoteServer.remove(p);
				if (hmRemoteServer.get(ObjectPackage) == rsx) {
					hmRemoteServer.remove(ObjectPackage);
				}
				if (remoteServerInterface == rsx) {
					remoteServerInterface = null;
				}
			}
		} else {
			p = ObjectPackage;
			if (rs != null) {
				hmRemoteServer.put(p, rs);
				if (remoteServerInterface == null) {
					remoteServerInterface = rs;
				} else if (remoteServerInterface != rs) {
					remoteServerInterface = null;
				}
			} else {
				RemoteServerInterface rsx = hmRemoteServer.remove(p);
				if (remoteServerInterface == rsx) {
					remoteServerInterface = null;
				}
			}
		}
	}

	// ========= RemoteSessionInterface ============
	/**
	 * Returns the active {@link RemoteSessionInterface}. Uses the cached
	 * global instance if present; otherwise performs package-based lookup.
	 *
	 * @return the remote session interface or {@code null}
	 */
	public static RemoteSessionInterface getRemoteSession() {
		if (remoteSessionInterface != null) {
			return remoteSessionInterface;
		}
		return getRemoteSession(getPackage(null));
	}

	/**
	 * Retrieves the {@link RemoteSessionInterface} for the package associated
	 * with the given class. Uses the cached global instance when present.
	 *
	 * @param c the class whose package is used for lookup
	 * @return the remote session interface or {@code null}
	 */
	public static RemoteSessionInterface getRemoteSession(Class c) {
		if (remoteSessionInterface != null) {
			return remoteSessionInterface;
		}
		return getRemoteSession(getPackage(c));
	}

	/**
	 * Retrieves the {@link RemoteSessionInterface} for the package associated
	 * with the given object. Falls back to global instance when applicable.
	 *
	 * @param obj the object whose package is used for lookup
	 * @return the remote session interface or {@code null}
	 */
	public static RemoteSessionInterface getRemoteSession(OAObject obj) {
		if (remoteSessionInterface != null) {
			return remoteSessionInterface;
		}
		Class c = obj == null ? null : obj.getClass();
		return getRemoteSession(getPackage(c));
	}

	/**
	 * Returns the {@link RemoteSessionInterface} associated with the package
	 * represented by the given {@link Hub}. Uses the global instance when one
	 * exists.
	 *
	 * @param h the hub whose object class determines the package context
	 * @return the remote session interface or {@code null}
	 */
	public static RemoteSessionInterface getRemoteSession(Hub h) {
		if (remoteSessionInterface != null) {
			return remoteSessionInterface;
		}
		Class c;
		if (h != null) {
			c = h.getObjectClass();
		} else {
			c = null;
		}
		return getRemoteSession(getPackage(c));
	}

	/**
	 * Retrieves the {@link RemoteSessionInterface} for the specified package.
	 * If no entry exists for that package, falls back to the
	 * {@link #ObjectPackage} mapping when appropriate.
	 *
	 * @param p the package to look up
	 * @return the corresponding remote session interface, or {@code null}
	 */
	public static RemoteSessionInterface getRemoteSession(Package p) {
		if (remoteSessionInterface != null) {
			return remoteSessionInterface;
		}
		if (p == null) {
			p = ObjectPackage;
		}
		RemoteSessionInterface rs = hmRemoteSession.get(p);
		if (rs == null && p != ObjectPackage) {
			rs = hmRemoteSession.get(ObjectPackage);
		}
		return rs;
	}

	/**
	 * Registers or removes the global {@link RemoteSessionInterface} by
	 * delegating to the package-based setter using the default package.
	 *
	 * @param rs the interface to register, or {@code null} to remove
	 */
	public static void setRemoteSession(RemoteSessionInterface rs) {
		setRemoteSession(null, rs);
	}

	/**
	 * Registers or removes a {@link RemoteSessionInterface} for the given
	 * package. Maintains:
	 * <ul>
	 *   <li>a per-package entry,</li>
	 *   <li>a cached global instance when only one exists.</li>
	 * </ul>
	 *
	 * @param p the package to associate with the remote session interface
	 * @param rs the remote session interface, or {@code null} to remove
	 */
	public static void setRemoteSession(Package p, RemoteSessionInterface rs) {
		if (p != null && p != ObjectPackage) {
			if (rs != null) {
				hmRemoteSession.put(p, rs);
				hmRemoteSession.computeIfAbsent(ObjectPackage, k -> rs);
				if (remoteSessionInterface == null) {
					remoteSessionInterface = rs;
				} else if (remoteSessionInterface != rs) {
					remoteSessionInterface = null;
				}
			} else {
				RemoteSessionInterface rsx = hmRemoteSession.remove(p);
				if (hmRemoteSession.get(ObjectPackage) == rsx) {
					hmRemoteSession.remove(ObjectPackage);
				}
				if (remoteSessionInterface == rsx) {
					remoteSessionInterface = null;
				}
			}
		} else {
			p = ObjectPackage;
			if (rs != null) {
				hmRemoteSession.put(p, rs);
				if (remoteSessionInterface == null) {
					remoteSessionInterface = rs;
				} else if (remoteSessionInterface != rs) {
					remoteSessionInterface = null;
				}
			} else {
				RemoteSessionInterface rsx = hmRemoteSession.remove(p);
				if (remoteSessionInterface == rsx) {
					remoteSessionInterface = null;
				}
			}
		}
	}

	// ========= RemoteClientInterface ============
	/**
	 * Returns the active {@link RemoteClientInterface}, using the cached
	 * global instance when present, otherwise performing package-based lookup.
	 *
	 * @return the remote client interface or {@code null}
	 */
	public static RemoteClientInterface getRemoteClient() {
		if (remoteClientInterface != null) {
			return remoteClientInterface;
		}
		return getRemoteClient(getPackage(null));
	}

	/**
	 * Retrieves the {@link RemoteClientInterface} associated with the package
	 * of the given class, falling back to a global instance when available.
	 *
	 * @param c the class whose package determines the sync context
	 * @return the remote client interface or {@code null}
	 */
	public static RemoteClientInterface getRemoteClient(Class c) {
		if (remoteClientInterface != null) {
			return remoteClientInterface;
		}
		return getRemoteClient(getPackage(c));
	}

	/**
	 * Retrieves the {@link RemoteClientInterface} for the package associated
	 * with the given object's class. Uses a cached global instance when set.
	 *
	 * @param obj the object whose sync context is requested
	 * @return the remote client interface or {@code null}
	 */
	public static RemoteClientInterface getRemoteClient(OAObject obj) {
		if (remoteClientInterface != null) {
			return remoteClientInterface;
		}
		Class c = obj == null ? null : obj.getClass();
		return getRemoteClient(getPackage(c));
	}

	/**
	 * Retrieves the {@link RemoteClientInterface} for the package determined
	 * by the given {@link Hub}. Uses the global instance when present.
	 *
	 * @param h the hub whose object class determines the sync context
	 * @return the remote client interface or {@code null}
	 */
	public static RemoteClientInterface getRemoteClient(Hub h) {
		if (remoteClientInterface != null) {
			return remoteClientInterface;
		}
		Class c;
		if (h != null) {
			c = h.getObjectClass();
		} else {
			c = null;
		}
		return getRemoteClient(getPackage(c));
	}

	/**
	 * Returns the {@link RemoteClientInterface} registered for the given
	 * package, falling back to the {@link #ObjectPackage} entry if necessary.
	 *
	 * @param p the package used for lookup
	 * @return the remote client interface or {@code null}
	 */
	public static RemoteClientInterface getRemoteClient(Package p) {
		if (remoteClientInterface != null) {
			return remoteClientInterface;
		}
		if (p == null) {
			p = ObjectPackage;
		}
		RemoteClientInterface rs = hmRemoteClient.get(p);
		if (rs == null && p != ObjectPackage) {
			rs = hmRemoteClient.get(ObjectPackage);
		}
		return rs;
	}

	/**
	 * Registers or removes the global {@link RemoteClientInterface}. Delegates
	 * to the package-based setter using the default package context.
	 *
	 * @param rc the remote client interface, or {@code null} to remove
	 */
	public static void setRemoteClient(RemoteClientInterface rc) {
		setRemoteClient(null, rc);
	}

	/**
	 * Registers or removes a {@link RemoteClientInterface} for the provided
	 * package. Maintains both a per-package mapping and a cached global entry
	 * when only one instance exists.
	 *
	 * @param p the package to bind to the client interface
	 * @param rc the interface instance, or {@code null} to remove
	 */
	public static void setRemoteClient(Package p, RemoteClientInterface rc) {
		if (p != null && p != ObjectPackage) {
			if (rc != null) {
				hmRemoteClient.put(p, rc);
				hmRemoteClient.computeIfAbsent(ObjectPackage, k -> rc);
				if (remoteClientInterface == null) {
					remoteClientInterface = rc;
				} else if (remoteClientInterface != rc) {
					remoteClientInterface = null;
				}

			} else {
				RemoteClientInterface rcx = hmRemoteClient.remove(p);
				if (hmRemoteClient.get(ObjectPackage) == rcx) {
					hmRemoteClient.remove(ObjectPackage);
				}
				if (remoteClientInterface == rcx) {
					remoteClientInterface = null;
				}
			}
		} else {
			p = ObjectPackage;
			if (rc != null) {
				hmRemoteClient.put(p, rc);
				if (remoteClientInterface == null) {
					remoteClientInterface = rc;
				} else if (remoteClientInterface != rc) {
					remoteClientInterface = null;
				}
			} else {
				RemoteClientInterface rcx = hmRemoteClient.remove(p);
				if (remoteClientInterface == rcx) {
					remoteClientInterface = null;
				}
			}
		}
	}

	// ========= RemoteSyncInterface ============
	/**
	 * Retrieves the active {@link RemoteSyncInterface}. If a cached global
	 * instance exists, it is returned; otherwise package-based lookup occurs.
	 *
	 * @return the remote sync interface or {@code null}
	 */
	public static RemoteSyncInterface getRemoteSync() {
		if (remoteSyncInterface != null) {
			return remoteSyncInterface;
		}
		return getRemoteSync(getPackage(null));
	}

	/**
	 * Retrieves the {@link RemoteSyncInterface} for the package of the
	 * specified class, using the global instance when available.
	 *
	 * @param c the class whose package determines the lookup context
	 * @return the remote sync interface or {@code null}
	 */
	public static RemoteSyncInterface getRemoteSync(Class c) {
		if (remoteSyncInterface != null) {
			return remoteSyncInterface;
		}
		return getRemoteSync(getPackage(c));
	}

	/**
	 * Retrieves the {@link RemoteSyncInterface} for the package associated
	 * with the given object. Uses a cached global instance if present.
	 *
	 * @param obj the object used to determine the sync context
	 * @return the remote sync interface or {@code null}
	 */
	public static RemoteSyncInterface getRemoteSync(OAObject obj) {
		if (remoteSyncInterface != null) {
			return remoteSyncInterface;
		}
		Class c = obj == null ? null : obj.getClass();
		return getRemoteSync(getPackage(c));
	}

	/**
	 * Retrieves the {@link RemoteSyncInterface} for the package represented
	 * by the supplied {@link Hub}. Uses the global instance when set.
	 *
	 * @param h the hub whose object class provides the lookup context
	 * @return the remote sync interface or {@code null}
	 */
	public static RemoteSyncInterface getRemoteSync(Hub h) {
		if (remoteSyncInterface != null) {
			return remoteSyncInterface;
		}
		Class c;
		if (h != null) {
			c = h.getObjectClass();
		} else {
			c = null;
		}
		return getRemoteSync(getPackage(c));
	}

	/**
	 * Retrieves the {@link RemoteSyncInterface} registered for the given
	 * package. Falls back to the {@link #ObjectPackage} entry if needed,
	 * and uses cached global instance when only one exists.
	 *
	 * @param p the package used for lookup
	 * @return the remote sync interface or {@code null}
	 */
	public static RemoteSyncInterface getRemoteSync(Package p) {
		if (remoteSyncInterface != null) {
			return remoteSyncInterface;
		}
		if (p == null) {
			p = ObjectPackage;
		}
		RemoteSyncInterface rs = hmRemoteSync.get(p);
		if (rs == null && p != ObjectPackage) {
			rs = hmRemoteSync.get(ObjectPackage);
		}
		return rs;
	}

	/**
	 * Registers or removes the global {@link RemoteSyncInterface}. Delegates
	 * to the package-specific setter with the default package.
	 *
	 * @param rs the remote sync interface to register, or {@code null} to remove
	 */
	public static void setRemoteSync(RemoteSyncInterface rs) {
		setRemoteSync(null, rs);
	}

	/**
	 * Registers or removes a {@link RemoteSyncInterface} for the specified
	 * package. Maintains both per-package and global-instance semantics.
	 *
	 * @param p the package key for the interface
	 * @param rs the interface instance, or {@code null} to remove
	 */
	public static void setRemoteSync(Package p, RemoteSyncInterface rs) {
		if (p != null && p != ObjectPackage) {
			if (rs != null) {
				hmRemoteSync.put(p, rs);
				hmRemoteSync.computeIfAbsent(ObjectPackage, k -> rs);
				if (remoteSyncInterface == null) {
					remoteSyncInterface = rs;
				} else if (remoteSyncInterface != rs) {
					remoteSyncInterface = null;
				}
			} else {
				RemoteSyncInterface rsx = hmRemoteSync.remove(p);
				if (hmRemoteSync.get(ObjectPackage) == rsx) {
					hmRemoteSync.remove(ObjectPackage);
				}
				if (remoteSyncInterface == rsx) {
					remoteSyncInterface = null;
				}
			}
		} else {
			p = ObjectPackage;
			if (rs != null) {
				hmRemoteSync.put(p, rs);
				if (remoteSyncInterface == null) {
					remoteSyncInterface = rs;
				} else if (remoteSyncInterface != rs) {
					remoteSyncInterface = null;
				}
			} else {
				RemoteSyncInterface rsx = hmRemoteSync.remove(p);
				if (remoteSyncInterface == rsx) {
					remoteSyncInterface = null;
				}
			}
		}
	}

	/**
	 * Returns the connection ID of the active {@link OASyncClient} associated
	 * with the given package. Returns {@code -1} if no client exists.
	 *
	 * @param p the package to look up
	 * @return the connection ID, or {@code -1} if unavailable
	 */
	public static int getConnectionId(Package p) {
		if (p == null) {
			p = ObjectPackage;
		}
		OASyncClient sc = getSyncClient(p);
		if (sc == null) {
			return -1;
		}
		return sc.getConnectionId();
	}

	/**
	 * Convenience method that returns the connection ID for the default
	 * package context.
	 *
	 * @return the connection ID, or {@code -1} if none exists
	 */
	public static int getConnectionId() {
		return getConnectionId((Package) null);
	}

	/**
	 * Determines whether the JVM is acting as a sync server.
	 * Returns {@code true} if a global {@link OASyncServer} instance is set,
	 * otherwise delegates to {@link #isServer(Class)}.
	 *
	 * @return {@code true} if this JVM is functioning as a server
	 */
	public static boolean isServer() {
		if (oaSyncServer != null) {
			return true;
		}
		return isServer((Class) null);
	}

	/**
	 * Determines whether the JVM is a sync server for the package associated
	 * with the given class.
	 *
	 * @param c the class whose package determines context
	 * @return {@code true} if this JVM is the server for that package
	 */
	public static boolean isServer(Class c) {
		if (c == null) {
			return isServer((Package) null);
		}
		return isServer(getPackage(c));
	}

	/**
	 * Determines whether the JVM is acting as the server for the given object’s
	 * package.
	 *
	 * @param obj the object whose sync context is evaluated
	 * @return {@code true} if this JVM is the server for the object's package
	 */
	public static boolean isServer(OAObject obj) {
		if (obj == null) {
			return isServer((Package) null);
		}
		return isServer(getPackage(obj.getClass()));
	}

	/**
	 * Determines whether the JVM is the server for the package represented by
	 * the given {@link Hub}.
	 *
	 * @param h the hub whose object class determines the sync context
	 * @return {@code true} if this JVM is the server for that package
	 */
	public static boolean isServer(Hub h) {
		if (h != null) {
			Class c = h.getObjectClass();
			if (c != null) {
				return isServer(getPackage(c));
			}
		}
		return isServer((Package) null);
	}

	/**
	 * Determines whether the JVM is the server for the specified package.
	 * Logic:
	 * <ul>
	 *   <li>If a server exists → true</li>
	 *   <li>If no client exists → also true</li>
	 * </ul>
	 *
	 * @param p the package to evaluate
	 * @return {@code true} if this JVM is acting as server
	 */
	public static boolean isServer(Package p) {
		if (p == null) {
			p = ObjectPackage;
		}
		OASyncServer ss = getSyncServer(p);
		OASyncClient sc = getSyncClient(p);
		return (ss != null) || (sc == null);
	}

	/**
	 * Determines whether this JVM is a sync client for the package associated
	 * with the given class.
	 *
	 * @param c the class used for determining package context
	 * @return {@code true} if this JVM is a client
	 */
	public static boolean isClient(Class c) {
		if (c == null) {
			return isClient((Package) null);
		}
		return isClient(getPackage(c));
	}

	/**
	 * Determines whether this JVM is a sync client for the specified package.
	 * A JVM is considered a client when:
	 * <ul>
	 *   <li>a client exists, and</li>
	 *   <li>a server does NOT exist.</li>
	 * </ul>
	 *
	 * @param p the package to evaluate
	 * @return {@code true} if this JVM is a client
	 */
	public static boolean isClient(Package p) {
		if (p == null) {
			p = ObjectPackage;
		}
		OASyncServer ss = getSyncServer(p);
		OASyncClient sc = getSyncClient(p);
		return (ss == null && sc != null);
	}

	/**
	 * Determines whether this JVM is acting as a client for the package of
	 * the specified object.
	 *
	 * @param obj the object whose sync context is evaluated
	 * @return {@code true} if this JVM is the client for that object
	 */
	public static boolean isClient(OAObject obj) {
		if (obj == null) {
			return isClient((Package) null);
		}
		return isClient(getPackage(obj.getClass()));
	}

	/**
	 * Returns {@code true} if neither a sync server nor sync client has been
	 * registered for the default package.
	 *
	 * @return {@code true} if running in single-user mode
	 */
	public static boolean isSingleUser() {
		return isSingleUser((Class) null);
	}

	/**
	 * Determines whether the specified class’s package has no registered sync
	 * server or client, meaning it operates in single-user mode.
	 *
	 * @param c the class whose package determines the lookup context
	 * @return {@code true} if single-user mode applies
	 */
	public static boolean isSingleUser(Class c) {
		return isSingleUser(getPackage(c));
	}

	/**
	 * Determines whether the package implied by the given {@link Hub} has
	 * no registered sync server or client.
	 *
	 * @param h the hub used for package resolution
	 * @return {@code true} if operating in single-user mode
	 */
	public static boolean isSingleUser(Hub h) {
		return isSingleUser(getPackage(h == null ? null : h.getObjectClass()));
	}

	/**
	 * Determines whether the specified package has neither a sync server nor
	 * sync client registered.
	 *
	 * @param p the package to evaluate
	 * @return {@code true} if the package operates in single-user mode
	 */
	public static boolean isSingleUser(Package p) {
		if (p == null) {
			p = ObjectPackage;
		}
		OASyncServer ss = getSyncServer(p);
		OASyncClient sc = getSyncClient(p);
		return (ss == null && sc == null);
	}

	/**
	 * Determines whether any {@link OASyncClient} is connected to a server for
	 * the default package.
	 *
	 * @return {@code true} if connected
	 */
	public static boolean isConnected() {
		return isConnected((Package) null);
	}

	/**
	 * Determines whether the sync client for the given object's package is
	 * connected.
	 *
	 * @param obj the object whose sync context is evaluated
	 * @return {@code true} if connected
	 */
	public static boolean isConnected(Class c) {
		return isConnected(getPackage(c));
	}

	/**
	 * Determines whether the sync client for the given object's package is
	 * connected.
	 *
	 * @param obj the object whose sync context is evaluated
	 * @return {@code true} if connected
	 */
	public static boolean isConnected(OAObject obj) {
		return isConnected(getPackage(obj == null ? null : obj.getClass()));
	}

	/**
	 * Returns {@code true} if:
	 * <ul>
	 *   <li>a sync client exists for the package and is connected, OR</li>
	 *   <li>no client exists but a server <i>does</i>, implying a local server
	 *       is acting as the sync endpoint.</li>
	 * </ul>
	 *
	 * @param p the package to test
	 * @return {@code true} if connected
	 */
	public static boolean isConnected(Package p) {
		if (p == null) {
			p = ObjectPackage;
		}
		OASyncClient sc = getSyncClient(p);

		if (sc == null) {
			OASyncServer ss = getSyncServer(p);
			return (ss != null);
		}
		return sc.isConnected();
	}

	/**
	 * Lock used to synchronize allocation of GUID values requested from the
	 * sync server in client mode.
	 */
	private final static Object NextGuidLock = new Object();
	
	/**
	 * The starting GUID value of the current allocation block received from
	 * the server. Incremented until {@link #maxNextGuid} is reached.
	 */
	private static long nextGuid;
	
	/**
	 * The upper bound (exclusive) for the current GUID block allocated from
	 * the server. When {@code nextGuid == maxNextGuid}, a new block is
	 * requested.
	 */
	private static long maxNextGuid;

	/**
	 * Returns the next GUID for objects of the class's package. Delegates to
	 * the package-based version.
	 *
	 * @param c the class whose package determines the sync context
	 * @return the next GUID value
	 */
	public static long getGuidFromServer(Class c) {
		if (c == null) {
			return getGuidFromServer((Package) null);
		}
		return getGuidFromServer(getPackage(c));
	}

	/**
	 * Returns the next GUID for the specified package.
	 *
	 * Server mode:
	 *   • GUIDs are generated locally using {@link OAObjectDelegate#getNextGuid()}.
	 *
	 * Client mode:
	 *   • GUIDs are allocated in blocks of 50 from the remote server.
	 *   • When the current block is exhausted, a new block is requested via
	 *     {@link RemoteServerInterface#getNextFiftyObjectGuids()}.
	 *
	 * @param p the package for which a GUID is requested
	 * @return the next GUID value
	 * @throws RuntimeException if the remote request fails
	 */
	public static long getGuidFromServer(Package p) {
		if (p == null) {
			p = ObjectPackage;
		}
		if (isServer(p)) {
			return OAObjectDelegate.getNextGuid(p);
		}
		long x;
		synchronized (NextGuidLock) {
			if (nextGuid == maxNextGuid) {
				try {
					nextGuid = getRemoteServer(p).getNextFiftyObjectGuids();
					maxNextGuid = nextGuid + 50;
				} catch (Exception ex) {
					LOG.log(Level.WARNING, "", ex);
					throw new RuntimeException("OAClient.getObjectGuid Error:", ex);
				}
			}
			x = nextGuid++;
		}
		return x;
	}

	/*
	 * If the currentThread is an OARemoteThead, then this is used to have sync changes (OAObject/Hub) sent to other computers. By default,
	 * all msgs processed by OARemoteThreads will not send out any sync changes to other computers (since they will receive the same msg).
	 * This will set a flag in the current OARemoteThread to allow any further changes during the current msg processing to be sent to the
	 * server/other clients. see OARemoteThread
	 */
	/**
	 * Returns whether sync messages should be sent from the current thread.
	 * Delegates to {@link OARemoteThreadDelegate#sendMessages()}.
	 *
	 * @return {@code true} if sync messages will be sent
	 */
	public static boolean sendMessages() {
		return OARemoteThreadDelegate.sendMessages();
	}

	/**
	 * Enables or disables sending sync messages for the current thread.
	 * Delegates to {@link OARemoteThreadDelegate#sendMessages(boolean)}.
	 *
	 * @param b {@code true} to send messages, {@code false} to suppress
	 * @return previous setting for message sending
	 */
	public static boolean sendMessages(boolean b) {
		return OARemoteThreadDelegate.sendMessages(b);
	}

	/**
	 * Determines whether the current thread is an {@code OARemoteThread},
	 * which is used internally to process incoming sync messages.
	 *
	 * @return {@code true} if the current thread is remote-thread context
	 */
	public static boolean isRemoteThread() {
		return OARemoteThreadDelegate.isRemoteThread();
	}

	/**
	 * Determines whether the current thread is processing sync-related
	 * activity. Returns {@code true} if the thread is:
	 *   • an {@code OARemoteThread}, OR
	 *   • marked as a sync thread via {@link OAThreadLocalDelegate}.
	 *
	 * @return {@code true} if the current thread is a sync-processing thread
	 */
	public static boolean isSyncThread() {
		if (OARemoteThreadDelegate.isRemoteThread()) {
			return true;
		}
		return OAThreadLocalDelegate.isSyncThread();
	}

	/*
	 * Checks to see if any sync changes will be sent to other computers. This will be true if the current thread is not an OARemoteThread,
	 * or if sendMessages([true]) was set.
	 */
	/**
	 * Determines whether sync changes made in the current thread should be
	 * broadcast to other computers. Delegates to
	 * {@link OARemoteThreadDelegate#shouldSendMessages()}.
	 *
	 * @return {@code true} if messages should be sent
	 */
	public static boolean shouldSendMessages() {
		return OARemoteThreadDelegate.shouldSendMessages();
	}

	/**
	 * Enables or disables suppression of client–server (CS) sync messages for
	 * the current thread. Delegates to
	 * {@link OAThreadLocalDelegate#setSuppressCSMessages(boolean)}.
	 *
	 * @param b whether to suppress CS messages
	 */
	public static void setSuppressCSMessages(boolean b) {
		OAThreadLocalDelegate.setSuppressCSMessages(b);
	}

	/**
	 * Returns whether CS sync messages are currently suppressed for the
	 * current thread. Delegates to
	 * {@link OAThreadLocalDelegate#isSuppressCSMessages()}.
	 *
	 * @return {@code true} if CS messages are suppressed
	 */
	public static boolean getSuppressCSMessages() {
		return OAThreadLocalDelegate.isSuppressCSMessages();
	}

	/**
	 * Returns the {@link RequestInfo} associated with the current thread
	 * if it is an {@code OARemoteThread}. This describes the sync message
	 * currently being processed.
	 *
	 * @return the request info, or {@code null} if not in remote-thread context
	 */
	public static RequestInfo getRequestInfo() {
		return OARemoteThreadDelegate.getRequestInfo();
	}

	/**
	 * Returns the connection ID associated with the sync message currently
	 * being processed by the current thread. If no request information is
	 * available, returns -1.
	 *
	 * @return the current request's connection ID, or -1 if unavailable
	 */
	public static int getRequestConnectionId() {
		RequestInfo ri = OARemoteThreadDelegate.getRequestInfo();
		if (ri == null) {
			return -1;
		}
		return ri.connectionId;
	}

	/*
	 * used to create a block of code that will only process on the server. Send messages if this is the
	 * server.
	 * 
	 * example: if (!OASync.beginServerOnly()) return; ... OASync.endServerOnly();
	 * 
	 * @return true if this is the server, else false.
	 * @see #endServerOnly()
	 * 
	 *      public static boolean beginServerOnly(Package p) { if (!isServer(p)) return false;
	 *      sendMessages(true); return true; } / **
	 * @see #beginServerOnly() / public static boolean endServerOnly(Package p) { if (!isServer(p))
	 *      return false; sendMessages(false); return true; }
	 */

	/* later private static OASyncCombinedClient syncCombinedClient; public static OASyncClient
	 * getSyncClient() { if (syncCombinedClient != null) { OASyncClient sc =
	 * syncCombinedClient.getCurrentThreadSyncClient(); if (sc != null) return sc; } return syncClient;
	 * }
	 * 
	 * public static OASyncCombinedClient getSyncCombinedClient() { return syncCombinedClient; } public
	 * static void setSyncCombinedClient(OASyncCombinedClient cc) { syncCombinedClient = cc; } */

	/**
	 * Marks the current thread as performing loading operations by setting
	 * the thread-local loading flag to {@code true}.
	 */
	public static void setLoading() {
		OAThreadLocalDelegate.setLoading(true);
	}

	/**
	 * Sets or clears the thread-local loading flag, used to indicate whether
	 * the current thread is performing object-loading operations.
	 *
	 * @param b {@code true} to mark as loading, {@code false} otherwise
	 */
	public static void setLoading(boolean b) {
		OAThreadLocalDelegate.setLoading(b);
	}
}
