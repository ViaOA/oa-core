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
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.runtime.OARuntime;
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

	public static final Package ObjectPackage = Object.class.getPackage();

	
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
		return getSyncServer(ObjectPackage);
	}

	/**
	 * Returns the {@link OASyncServer} for the package associated with the
	 * given class. If a global server is cached, it is returned.
	 *
	 * @param c the class whose sync server is requested
	 * @return the matching sync server or {@code null} if none
	 */
	public static OASyncServer getSyncServer(Class c) {
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
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return null;
		return g.sync().getSyncServer();
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
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return;
		g.sync().setSyncServer(ss);
	}

	// ========= SyncClient ============
	/**
	 * Returns the active {@link OASyncClient}. If a single client instance
	 * exists, it is returned; otherwise package-based lookup is used.
	 *
	 * @return the sync client instance, or {@code null} if not registered
	 */
	public static OASyncClient getSyncClient() {
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
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return null;
		return g.sync().getSyncClient();
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
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return;
		g.sync().setSyncClient(sc);
	}

	// ========= RemoteServerInterface ============
	/**
	 * Returns the active {@link RemoteServerInterface}. If a single instance
	 * exists, it is returned; otherwise falls back to package-based lookup.
	 *
	 * @return the remote server interface or {@code null}
	 */
	public static RemoteServerInterface getRemoteServer() {
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
		return getRemoteServer(getPackage(c));
	}

	public static RemoteServerInterface getRemoteServer(OAObject obj) {
		Class c = obj == null ? null : obj.getClass();
		return getRemoteServer(getPackage(c));
	}
	public static RemoteServerInterface getRemoteServer(Hub h) {
		Class c;
		if (h != null) {
			c = h.getObjectClass();
		} else {
			c = null;
		}
		return getRemoteServer(getPackage(c));
	}
	public static RemoteServerInterface getRemoteServer(Package p) {
		if (p == null) {
			p = ObjectPackage;
		}
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return null;
		return g.sync().getRemoteServer();
	}
	public static void setRemoteServer(RemoteServerInterface rs) {
		setRemoteServer(null, rs);
	}
	public static void setRemoteServer(Package p, RemoteServerInterface rs) {
		throw new RuntimeException("OASyncDelegate.setRemteServer not needed qqqqqqqqq");
/*		
		if (p == null) p = ObjectPackage;
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return;
		g.sync().setRemoteServer(rs);
*/		
	}

	public static RemoteSessionInterface getRemoteSession() {
		return getRemoteSession(getPackage(null));
	}
	public static RemoteSessionInterface getRemoteSession(Class c) {
		return getRemoteSession(getPackage(c));
	}
	public static RemoteSessionInterface getRemoteSession(OAObject obj) {
		Class c = obj == null ? null : obj.getClass();
		return getRemoteSession(getPackage(c));
	}
	public static RemoteSessionInterface getRemoteSession(Hub h) {
		Class c;
		if (h != null) {
			c = h.getObjectClass();
		} else {
			c = null;
		}
		return getRemoteSession(getPackage(c));
	}
	public static RemoteSessionInterface getRemoteSession(Package p) {
		if (p == null) {
			p = ObjectPackage;
		}
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return null;
		return g.sync().getRemoteSession();
	}

	public static void setRemoteSession(RemoteSessionInterface rs) {
		setRemoteSession(null, rs);
	}
	public static void setRemoteSession(Package p, RemoteSessionInterface rs) {
		throw new RuntimeException("OASyncDelegate not needed qqqqqqqqq");
/*		
		if (p == null) p = ObjectPackage;
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return;
		g.sync().setRemoteSession(rs);
*/		
	}

	// ========= RemoteClientInterface ============
	public static RemoteClientInterface getRemoteClient() {
		return getRemoteClient(getPackage(null));
	}
	public static RemoteClientInterface getRemoteClient(Class c) {
		return getRemoteClient(getPackage(c));
	}
	public static RemoteClientInterface getRemoteClient(OAObject obj) {
		Class c = obj == null ? null : obj.getClass();
		return getRemoteClient(getPackage(c));
	}
	public static RemoteClientInterface getRemoteClient(Hub h) {
		Class c;
		if (h != null) {
			c = h.getObjectClass();
		} else {
			c = null;
		}
		return getRemoteClient(getPackage(c));
	}
	public static RemoteClientInterface getRemoteClient(Package p) {
		if (p == null) {
			p = ObjectPackage;
		}
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return null;
		return g.sync().getRemoteClient();
	}
	public static void setRemoteClient(RemoteClientInterface rc) {
		setRemoteClient(null, rc);
	}
	public static void setRemoteClient(Package p, RemoteClientInterface rc) {
		throw new RuntimeException("OASyncDelegate.setRemoteClient not needed qqqqqqqqq");
		/*
		if (p == null) {
			p = ObjectPackage;
		}
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return;
		g.sync().setRemoteClient(rc);
		*/
	}

	// ========= RemoteSyncInterface ============
	public static RemoteSyncInterface getRemoteSync() {
		return getRemoteSync(getPackage(null));
	}
	public static RemoteSyncInterface getRemoteSync(Class c) {
		return getRemoteSync(getPackage(c));
	}
	public static RemoteSyncInterface getRemoteSync(OAObject obj) {
		Class c = obj == null ? null : obj.getClass();
		return getRemoteSync(getPackage(c));
	}
	public static RemoteSyncInterface getRemoteSync(Hub h) {
		Class c;
		if (h != null) {
			c = h.getObjectClass();
		} else {
			c = null;
		}
		return getRemoteSync(getPackage(c));
	}
	public static RemoteSyncInterface getRemoteSync(Package p) {
		if (p == null) {
			p = ObjectPackage;
		}
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return null;
		return g.sync().getRemoteSync();
	}
	public static void setRemoteSync(RemoteSyncInterface rs) {
		setRemoteSync(null, rs);
	}
	public static void setRemoteSync(Package p, RemoteSyncInterface rs) {
		throw new RuntimeException("OASyncDelegate.setRemoteSync not needed qqqqqqqqq");
		/*
		if (p == null) {
			p = ObjectPackage;
		}
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return;
		g.sync().setRemoteSync(rs);
		*/
	}

	public static int getConnectionId(Package p) {
		if (p == null) p = ObjectPackage;
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return 0;
		return g.sync().getConnectionId();
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
		if (p == null) p = ObjectPackage;
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return false;
		return g.sync().isServer();
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
		if (p == null) p = ObjectPackage;
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return false;
		return g.sync().isClient();
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
		if (p == null) p = ObjectPackage;
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return false;
		return g.sync().isSingleUser();
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
		if (p == null) p = ObjectPackage;
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return false;
		return g.sync().isConnected();
	}


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

	public static long getGuidFromServer(Package p) {
		if (p == null) p = ObjectPackage;
		OAGraph g = OARuntime.get().graph(p);
		if (g == null) return 0;
		return g.sync().getGuidFromServer();
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
