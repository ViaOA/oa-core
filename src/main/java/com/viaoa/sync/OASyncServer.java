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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.cascade.OACascade;
import com.viaoa.comm.multiplexer.OAMultiplexerServer;
import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.object.*;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.remote.multiplexer.OARemoteMultiplexerServer;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.file.ServerFile;
import com.viaoa.sync.model.ClientInfo;
import com.viaoa.sync.model.ServerInfo;
import com.viaoa.sync.remote.RemoteClientCallbackInterface;
import com.viaoa.sync.remote.RemoteClientImpl;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteServerImpl;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.sync.remote.RemoteSessionImpl;
import com.viaoa.sync.remote.RemoteSessionInterface;
import com.viaoa.sync.remote.RemoteSyncImpl;
import com.viaoa.sync.remote.RemoteSyncInterface;

/*qqqqqqqqqqqqqq
CODEX


6. src/main/java/com/viaoa/sync/OASyncServer.java — onClientDisconnect(int)
     Concrete bug: cx.remoteSession.clearLocks() is called without checking whether remoteSession was ever created.
     Runtime scenario: client connects and disconnects before completing remote session setup.
     Why this violates sync semantics: disconnect cleanup can throw during a normal connection lifecycle path,
     skipping later cleanup and making reconnect/session accounting less reliable.
     Minimal fix direction: null-check cx.remoteSession before clearing locks.
     Suggested CODEX comment location: OASyncServer.onClientDisconnect, around lines 654-664.

1. src/main/java/com/viaoa/sync/OASyncServer.java — getRemoteSession(...).isLockedByAnotherClient(...)
     Concrete bug: the loop calls cx.remoteSession.isLockedByThisClient(...) for every ClientInfoExt without checking
     whether remoteSession is null.
     Runtime scenario: client A has an active session and checks/sets a lock while client B has connected but has not
     yet completed getRemoteSession(...), or client B disconnected before session creation.
     Why this violates OA/OG sync semantics: lock checks can fail with NPE during normal connect/disconnect timing,
     which can falsely break remote save/edit workflows and leave lock behavior unreliable.
     Minimal fix direction: skip entries where cx.remoteSession == null.
     Suggested CODEX comment location: OASyncServer.getRemoteSession(...), inside anonymous isLockedByAnotherClient,
     around the loop over hmClientInfoExt.
  2. src/main/java/com/viaoa/sync/OASyncServer.java — getRemoteClient(...).updateObjectCache(...)
     Concrete bug: the anonymous RemoteClientImpl.updateObjectCache(...) assumes cx.remoteSession is non-null.
     Runtime scenario: a client obtains/uses RemoteClientInterface before session creation is fully established, or a
     disconnect/reconnect race nulls/removes session state while a datasource/detail request is still returning
     objects.
     Why this violates OA/OG sync semantics: server-side cache-retention tracking can throw instead of recording that
     the client now holds returned objects, causing remote select/detail work to fail or lose retention state.
     Minimal fix direction: ensure getRemoteClient(...) creates/validates the session first, or null-check and fail
     visibly instead of dereferencing cx.remoteSession.
     Suggested CODEX comment location: OASyncServer.getRemoteClient(...), anonymous updateObjectCache, around the
     cx.remoteSession.updateObjectsWithoutHubs(...) call.
  3. src/main/java/com/viaoa/sync/OASyncServer.java — getRemoteMultiplexerServer().removeSession(int)
     Concrete bug: override decrements aiSessionCount unconditionally, even if the underlying remote multiplexer had
     no session to remove.
     Runtime scenario: onException(...) removes a session after a callback stop, then the socket close/disconnect path
     also calls removeSession(connectionId). OARemoteMultiplexerServer.removeSession is idempotent, but the sync
     server counter is not.
     Why this violates OA/OG sync semantics: session lifecycle state drifts from actual remote session state, making
     server monitoring/admin decisions and lifecycle assertions unreliable during normal failure/disconnect paths.
     Minimal fix direction: decrement only when a session actually existed, or move the count update behind a boolean-
     returning remove operation / local membership check.
     Suggested CODEX comment location: OASyncServer.getRemoteMultiplexerServer(), override removeSession, around the
     unconditional aiSessionCount.decrementAndGet().
  4. src/main/java/com/viaoa/sync/OASyncServer.java — start() / startLoadDataInBackgroundThread()
     Concrete bug: startLoadDataInBackgroundThread() starts a new background load thread every time start() is called;
     it has no guard for an existing live threadLoadSibling.
     Runtime scenario: server is stopped and started again, or start() is accidentally called twice during test/
     runtime setup. Multiple background workers then consume the same sibling-load queue concurrently.
     Why this violates OA/OG sync semantics: background detail/sibling preloading becomes nondeterministic and leaks
     daemon workers across lifecycle retries.
     Minimal fix direction: guard with if (threadLoadSibling != null && threadLoadSibling.isAlive()) return;, and add
     a stop/lifecycle flag if restart is supported.
     Suggested CODEX comment location: OASyncServer.startLoadDataInBackgroundThread, around thread creation.


1. src/main/java/com/viaoa/sync/OASyncServer.java — onUpdate(ClientInfo)
     Concrete bug: onUpdate replaces the server’s ClientInfoExt.ci with the client-supplied ClientInfo without
     preserving server-owned lifecycle state such as disconnected.
     Runtime scenario: a client update is in flight while the socket disconnects. onClientDisconnect(...) sets
     cx.ci.disconnected, then a late RemoteSessionImpl.update(ci) calls onUpdate(ci) and replaces cx.ci with a stale
     client object whose disconnected is null.
     Why this violates OA/OG sync semantics: a disconnected session can silently appear connected again. It also
     affects RemoteSessionImpl.saveCache(...), whose subclass clears caches only when cx.ci.getDisconnected() != null;
     a late update can prevent disconnected-session cache cleanup.
     Minimal fix direction: treat disconnect timestamp as server-owned state. Ignore updates after disconnect, or
     merge mutable client fields while preserving disconnected.
     Suggested CODEX comment location: OASyncServer.onUpdate, around the cx.ci = ci assignment.
  2. src/main/java/com/viaoa/sync/OASyncServer.java / src/main/java/com/viaoa/sync/model/ServerInfo.java — start(),
     stop(), ServerInfo.started
     Concrete bug: ServerInfo.started is never set by OASyncServer.start() or stop().
     Runtime scenario: server starts successfully, clients/admin call getDisplayMessage(), and the message reports
     started=false for a running sync server.
     Why this violates OA/OG sync semantics: runtime lifecycle state is false. Monitoring/admin/discovery code cannot
     rely on ServerInfo to distinguish started/stopped sync state.
     Minimal fix direction: set getServerInfo().setStarted(true) after successful startup completion, and set it false
     during stop/failed startup cleanup.
     Suggested CODEX comment location: OASyncServer.start() around lines 1215-1221, and stop() around lines 1230-1234.

1. src/main/java/com/viaoa/sync/OASyncServer.java:401 / src/main/java/com/viaoa/graph/service/
     OASyncService.java:215 / src/main/java/com/viaoa/graph/service/object/OAObjectLockService.java:60

  Concrete bug:
  Server-mode getRemoteSessionForServer() and getRemoteClientForServer() can never create the server’s own session/
  client facade because they call getRemoteSession(getClientInfo(), null) / getRemoteClient(getClientInfo()), and
  getClientInfo() uses connectionId=0. getRemoteSession(...) and getRemoteClient(...) require
  hmClientInfoExt.get(connectionId) to exist, but server connectionId 0 is never inserted into hmClientInfoExt.

  Runtime scenario:
  In sync-server mode, OAObjectLockService.lock/unlock/isLocked sees callSyncIsServer() == true, so it delegates to s
  rvcSync.getRemoteSession(). OASyncService.getRemoteSession() returns syncServer.getRemoteSessionForServer(), which
  returns null. The lock operation then returns false instead of using either a real server-local session lock or loc
  al lock path.

  Why this violates OA/OG sync semantics:
  Server-mode object locking silently becomes a false no-op. The code comments/Javadoc explicitly say the server-side
  session is created and registered, but the actual lookup path cannot create one without a ClientInfoExt entry.

  Minimal fix direction:
  Either create/register a real server-local ClientInfoExt for connectionId 0, or make getRemoteSessionForServer()
  construct a dedicated local RemoteSessionImpl that does not depend on multiplexer client-session state. Same
  decision should be made for getRemoteClientForServer().

  Suggested CODEX comment location:
  OASyncServer.getRemoteSessionForServer() and getRemoteClientForServer(), before the lazy assignment.

  Suggested regression test:
  testSyncServerObjectLockUsesServerLocalSessionInsteadOfReturningFalse()


  2. src/main/java/com/viaoa/sync/OASyncServer.java:864

  Concrete bug:
  onException(...) calls cx.remoteClientCallback.stop(...) and then removes the session in the same block. If callback
  stop throws or times out, removeSession(...) is skipped.

  Runtime scenario:
  Server detects a client remote exception and attempts to stop the client. The callback path is already degraded, so
  remoteClientCallback.stop(...) throws. The server does not remove the remote session.

  Why this violates sync semantics:
  A broken client/session can remain registered after a failure path that intended to remove it, leaving stale session
  state and stale sync filtering state.

  Minimal fix direction:
  Call removeSession(...) in a finally or catch callback failure and still remove when the session exists.

  Suggested CODEX comment location:
  OASyncServer.getRemoteMultiplexerServer().onException(...).

  Suggested regression test:
  testServerExceptionCleanupRemovesSessionWhenClientStopCallbackFails()



1. src/main/java/com/viaoa/sync/OASyncServer.java:929 / shouldSendSyncMessageToClient(...)

  Concrete bug: addToHub / insertInHub sync filtering assumes the server cache still has the master object after
  confirming only that the client GUID map contains the master GUID.

  Runtime scenario: a client has seen a master object, so its hmGuid contains the master GUID. Later the server
  broadcasts an addToHub or insertInHub message, but the weak object cache no longer has the master object at line
  937. Line 938 then calls property access with obj == null.

  Why this violates sync semantics: sync filtering must not fail while deciding whether to deliver a valid hub
  mutation. A cache miss during routing can break message delivery or remote processing even though the sync message
  already has the authoritative class/key/property data.

  Minimal fix direction: if callObjectCacheGet(...) returns null, do not dereference it. Either conservatively send
  the message after the GUID checks, or treat the hub-loaded probe as unavailable and return a deterministic routing
  decision.

  Suggested CODEX comment location: OASyncServer.shouldSendSyncMessageToClient(...), around lines 929-938.

  Suggested regression test: testAddToHubSyncFilteringDoesNotThrowWhenMasterGuidKnownButServerCacheMisses()

  2. src/main/java/com/viaoa/sync/OASyncServer.java:771 + src/main/java/com/viaoa/sync/OASyncClient.java:416 /
     connection startup

  Concrete bug: server connection setup creates the remote multiplexer session before inserting ClientInfoExt into
  hmClientInfoExt, while client startup accepts null remote session/client lookups as success.

  Runtime scenario: onClientConnect(...) calls createSession(socket, connectionId) at line 772, then inserts the
  connection state at line 773. If the client immediately calls getRemoteSession(...) or getRemoteClient(...) during
  that window, server lookup returns null because hmClientInfoExt does not yet contain the connection. Client start()
  does not validate non-null results and can still set clientInfo.started=true.

  Why this violates sync semantics: client startup can falsely succeed with missing remote session/client interfaces.
  Later sync operations then fail, no-op, or initialize inconsistently.

  Minimal fix direction: register ClientInfoExt before exposing the remote session, with rollback if
  createSession(...) fails. Also make OASyncClient.start() validate all required remote interfaces are non-null before
  marking started.

  Suggested CODEX comment location: OASyncServer.onClientConnect(...) around lines 771-773 and OASyncClient.start()
  around lines 416-428.

  Suggested regression test: testClientStartFailsIfRemoteSessionOrRemoteClientLookupReturnsNull()



1. src/main/java/com/viaoa/sync/OASyncServer.java:550 / getRemoteSession(...) and src/main/java/com/viaoa/sync/
     OASyncServer.java:668 / getRemoteClient(...)

  Concrete bug: both methods assume the remote multiplexer session still exists once hmClientInfoExt has an entry.

  Runtime scenario: a disconnect removes the remote multiplexer session, but hmClientInfoExt remains for disconnect/
  cache-save/admin state. A late remote lookup or reconnect race calls getRemoteSession(...) / getRemoteClient(...);
  getSession(id, false) returns null, then .getGuidHashMap() throws.

  Why this violates sync semantics: disconnected-session state is intentionally retained, but remote facade creation
  treats it as active. A normal disconnect race can fail with NPE instead of cleanly returning null or rejecting the
  stale session.

  Minimal fix direction: store Session session = getRemoteMultiplexerServer().getSession(id, false) and return null/
  fail visibly if null or disconnected before dereferencing.

  Suggested CODEX comment location: lines 550 and 668.

  Suggested regression test: testLateRemoteFacadeLookupAfterDisconnectDoesNotThrowNpe()



*/

/**
 * Server-side synchronization endpoint for an OA model.
 * <p>
 * An {@code OASyncServer} hosts the authoritative {@code OAObject} graph and
 * manages connections from one or more {@link OASyncClient} instances. It
 * routes remote method calls, broadcast sync messages, and manages per-client
 * session state.
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Start and manage the underlying {@code OAMultiplexerServer} and
 *       {@code OARemoteMultiplexerServer},</li>
 *   <li>create and maintain {@code ClientInfo} and session objects for each
 *       connected client,</li>
 *   <li>provide {@code RemoteServer}, {@code RemoteSession},
 *       {@code RemoteClient}, and {@code RemoteSync} implementations to
 *       clients,</li>
 *   <li>control lock ownership and conflict resolution for remote sessions,</li>
 *   <li>route sync messages to only those clients that have the relevant
 *       objects or hubs loaded,</li>
 *   <li>perform file transfer duties via {@code ServerFile},</li>
 *   <li>log remote requests to rotating daily log files,</li>
 *   <li>asynchronously load sibling properties for detail links that require
 *       background loading.</li>
 * </ul>
 *
 * <h2>Client Session Management</h2>
 * For each connection, the server creates a {@code ClientInfoExt} that stores:
 * <ul>
 *   <li>address and hostname,</li>
 *   <li>per-connection remote session and client implementations,</li>
 *   <li>lock state and pending updates,</li>
 *   <li>disconnect markers for cleanup and cache saving.</li>
 * </ul>
 *
 * <h2>Background Workers</h2>
 * Several daemon threads are used:
 * <ul>
 *   <li>request logging thread writing to timestamped files,</li>
 *   <li>sibling-property loader for detail link resolution,</li>
 *   <li>optional server-side update thread.</li>
 * </ul>
 *
 * <p>
 * {@code OASyncServer} is designed to be long-lived and to coordinate the
 * object graph state for all connected clients.
 */
public class OASyncServer {
	private static Logger LOG = Logger.getLogger(OASyncServer.class.getName());

	/**
	 * Default lookup name used when registering the primary {@code RemoteServer}
	 * instance with the remote multiplexer.
	 */
	public static final String ServerLookupName = "syncserver";

	/**
	 * Lookup name used when registering the {@code RemoteSync} broadcast endpoint
	 * with the remote multiplexer.
	 */
	public static final String SyncLookupName = "oasync";
	
	/**
	 * Name of the message queue used for synchronization-related remote method
	 * calls and broadcasts.
	 */
	public static final String SyncQueueName = "oasync";
	
	/**
	 * Default maximum size of the internal message queues used for sync-related
	 * remote calls and broadcasts.
	 */
	public static final int QueueSize = 20000;

	/**
	 * TCP port number that the underlying {@link OAMultiplexerServer} listens on
	 * for incoming client connections.
	 */
	private int port;
	
	/**
	 * Underlying socket multiplexer that accepts client connections and manages
	 * low-level network I/O for this sync server.
	 */
	private OAMultiplexerServer multiplexerServer;
	
	/**
	 * Remote invocation wrapper on top of the {@link OAMultiplexerServer} that
	 * exposes remote objects, broadcasts, and sessions to connected clients.
	 */
	private OARemoteMultiplexerServer remoteMultiplexerServer;

	/**
	 * Lazily created implementation of the {@link RemoteSyncInterface} used to
	 * broadcast synchronization messages to connected clients.
	 */
	private RemoteSyncImpl remoteSyncImpl;

	/**
	 * used by server to use as Client to Sync.
	 */
	private RemoteSyncInterface remoteSyncInterface;
	
	
	/**
	 * Lazily created implementation of the {@link RemoteServerInterface} that
	 * provides server-side services such as session creation and cache control to
	 * remote clients.
	 */
	private RemoteServerImpl remoteServer;

	/**
	 * Bounded queue used by the request-logging thread to asynchronously receive
	 * {@link RequestInfo} instances representing remote method invocations.
	 */
	private ArrayBlockingQueue<RequestInfo> queRemoteRequestLogging;

	/**
	 * Map of connection identifiers to {@link ClientInfoExt} structures that track
	 * per-connection state such as sockets, sessions, and client metadata.
	 */
	private ConcurrentHashMap<Integer, ClientInfoExt> hmClientInfoExt = new ConcurrentHashMap<Integer, ClientInfoExt>();

	/**
	 * Lazily initialized {@link ServerInfo} instance containing metadata about the
	 * running server, including host information and startup time.
	 */
	private ServerInfo serverInfo;

	/**
	 * {@link ClientInfo} representing this server when it acts as a logical
	 * client, for example when creating a server-side session and client proxy.
	 */
	private ClientInfo clientInfo;
	
	/**
	 * Server-side {@link RemoteSessionInterface} instance associated with the
	 * server's own {@link ClientInfo}, used for internal cache and lock
	 * management.
	 */
	private RemoteSessionInterface remoteSessionServer;
	
	/**
	 * Server-side {@link RemoteClientInterface} instance associated with the
	 * server's own {@link ClientInfo}, used when the server needs a client-style
	 * proxy for remote operations.
	 */
	private RemoteClientInterface remoteClientForServer;
	
	/**
	 * {@link ServerFile} helper used to coordinate file upload and download
	 * operations between the server and connected clients.
	 */
	private ServerFile serverFile;

	/**
	 * Constructs a new sync server bound to the specified port using the default
	 * package derived from {@link Object#getPackage()}.
	 *
	 * @param port the TCP port the multiplexer server will listen on for incoming
	 *             client connections
	 */
	public OASyncServer(int port) {
		this.port = port;
	}


	/**
	 * Returns the lazily created {@link RemoteSyncImpl} instance used to broadcast
	 * synchronization messages. A new instance is created on first access.
	 *
	 * @return the {@link RemoteSyncImpl} used for sync broadcasts
	 */
	public RemoteSyncImpl getRemoteSyncImpl() {
		if (remoteSyncImpl == null) {
			remoteSyncImpl = new RemoteSyncImpl();
		}
		return remoteSyncImpl;
	}

	public RemoteSyncInterface getRemoteSyncInterface() {
		return remoteSyncInterface;
	}

	
	/**
	 * Returns the lazily created {@link RemoteServerImpl} instance that provides
	 * remote server functionality to clients. On first access, this method creates
	 * a new instance with overridden methods for obtaining sessions, clients, and
	 * server display messages. It also registers the instance with
	 * {@link OASyncDelegate} and ensures the server's own session is created.
	 *
	 * @return the {@link RemoteServerImpl} instance for this server
	 */
	public RemoteServerImpl getRemoteServer() {
		if (remoteServer == null) {
			remoteServer = new RemoteServerImpl() {
				@Override
				public RemoteSessionInterface getRemoteSession(ClientInfo ci, RemoteClientCallbackInterface callback) {
					RemoteSessionInterface rsi = OASyncServer.this.getRemoteSession(ci, callback);
					return rsi;
				}

				@Override
				public RemoteClientInterface getRemoteClient(ClientInfo ci) {
					RemoteClientInterface rci = OASyncServer.this.getRemoteClient(ci);
					return rci;
				}

				@Override
				public String getDisplayMessage() {
					return OASyncServer.this.getDisplayMessage();
				}

				@Override
				public void refreshCache(Class clazz) {
					final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
			    	og.objectsInternal().callObjectCacheRefresh(clazz);
				}

				@Override
				public OAObject getUnique(Class<? extends OAObject> clazz, String propertyName, Object uniqueKey, boolean bAutoCreate) {
					
					final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
					
					OAObject oaObj = og.objectsInternal().callObjectUniqueGetUnique(clazz, propertyName, uniqueKey, bAutoCreate);
					return oaObj;
				}

				@Override
				public long getNextFiftyObjectGuids() {
					// TODO Auto-generated method stub
					return 0;
				}
			};
			getRemoteSessionForServer();
		}
		return remoteServer;
	}

	/**
	 * Returns the {@link ClientInfo} representing this server as a logical client.
	 * The instance is lazily created and initialized with connectionId=0 and the
	 * current timestamp.
	 *
	 * @return the server's {@link ClientInfo}
	 */
	public ClientInfo getClientInfo() {
		if (clientInfo == null) {
			clientInfo = new ClientInfo();
			clientInfo.setConnectionId(0);
			clientInfo.setCreated(new OADateTime());
		}
		return clientInfo;
	}

	/**
	 * Returns the server-side {@link RemoteSessionInterface} associated with the
	 * server's own {@link ClientInfo}. On first access, a session is created and
	 * registered with {@link OASyncDelegate}.
	 *
	 * @return the server’s {@link RemoteSessionInterface}
	 */
	public RemoteSessionInterface getRemoteSessionForServer() {
		if (remoteSessionServer == null) {
			remoteSessionServer = getRemoteSession(getClientInfo(), null);
		}
		return remoteSessionServer;
	}

	/**
	 * Returns the server-side {@link RemoteClientInterface} associated with the
	 * server's own {@link ClientInfo}. On first access, a client proxy is created
	 * and registered with {@link OASyncDelegate}.
	 *
	 * @return the server’s {@link RemoteClientInterface}
	 */
	public RemoteClientInterface getRemoteClientForServer() {
		if (remoteClientForServer == null) {
			remoteClientForServer = getRemoteClient(getClientInfo());
		}
		return remoteClientForServer;
	}

	/**
	 * Returns or creates a {@link RemoteSessionInterface} for the specified client
	 * connection. If the {@link ClientInfo} is not associated with an active
	 * connection, {@code null} is returned. Otherwise, a new
	 * {@link RemoteSessionImpl} is created if needed and configured with
	 * lock-checking, cache-handling, exception forwarding, and update callbacks.
	 *
	 * @param ci       the client connection information
	 * @param callback optional callback interface for client-side notifications
	 * @return the associated {@link RemoteSessionInterface}, or {@code null} if
	 *         the connection is not active
	 */
	protected RemoteSessionInterface getRemoteSession(final ClientInfo ci, RemoteClientCallbackInterface callback) {
		if (ci == null) {
			return null;
		}
		final ClientInfoExt cx = hmClientInfoExt.get(ci.getConnectionId());
		if (cx == null) {
			return null;
		}

		RemoteSessionImpl rs = cx.remoteSession;
		if (rs != null) {
			return rs;
		}
		cx.remoteClientCallback = callback;

        Map<UUID, Boolean> hm = getRemoteMultiplexerServer().getSession(ci.getConnectionId(), false).getGuidHashMap();
		rs = new RemoteSessionImpl(ci.getConnectionId(), hm) {
			boolean bClearedCache;

			@Override
			public boolean isLockedByAnotherClient(Class objectClass, OAObjectKey objectKey) {
				for (Map.Entry<Integer, ClientInfoExt> entry : hmClientInfoExt.entrySet()) {
					ClientInfoExt cx = entry.getValue();
					if (cx.remoteSession == this) {
						continue;
					}
					if (cx.remoteSession.isLockedByThisClient(objectClass, objectKey)) {
						return true;
					}
				}
				return false;
			}

			@Override
			public void saveCache(OACascade cascade, int iCascadeRule) {
				super.saveCache(cascade, iCascadeRule);
				if (!bClearedCache && cx.ci.getDisconnected() != null) {
					clearCaches();
					bClearedCache = true;
				}
			}

			@Override
			public boolean isLocked(Class objectClass, OAObjectKey objectKey) {
				boolean b = isLockedByThisClient(objectClass, objectKey);
				if (!b) {
					b = isLockedByAnotherClient(objectClass, objectKey);
				}
				return b;
			}

			@Override
			public void sendException(String msg, Throwable ex) {
				OASyncServer.this.onClientException(ci, msg, ex);
			}

			@Override
			public void update(ClientInfo ci) {
				OASyncServer.this.onUpdate(ci);
			}
		};
		cx.remoteSession = rs;
		return rs;
	}

	/**
	 * Starts a daemon thread that periodically updates this server’s
	 * {@link ClientInfo} with memory statistics and sends the update via
	 * {@link #onUpdate(ClientInfo)}. The thread sleeps the specified number of
	 * seconds between updates.
	 *
	 * @param seconds number of seconds between update notifications
	 */
	public void startUpdateThread(final int seconds) {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				getClientInfo();
				for (;;) {
					clientInfo.setFreeMemory(Runtime.getRuntime().freeMemory());
					clientInfo.setTotalMemory(Runtime.getRuntime().totalMemory());
					try {
						onUpdate(clientInfo);
						Thread.sleep(seconds * 1000L);
					} catch (Exception e) {
						break;
					}
				}
			}
		}, "OASyncServer.update." + seconds);
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Updates the stored {@link ClientInfo} for the specified connection. This
	 * method can be overridden to capture additional information. By default, the
	 * method replaces the existing {@link ClientInfo} entry in the internal map.
	 *
	 * @param ci the updated client information
	 */
	public void onUpdate(ClientInfo ci) {
		int cid = ci.getConnectionId();
		ClientInfoExt cx = hmClientInfoExt.get(cid);
		if (cx != null) {
			cx.ci = ci;
		}
	}

	/**
	 * Returns or creates a {@link RemoteClientInterface} for the specified client.
	 * If no active connection is found, {@code null} is returned. A new
	 * {@link RemoteClientImpl} is created when needed and configured to update the
	 * session cache and load background data.
	 *
	 * @param ci the client connection information
	 * @return the associated {@link RemoteClientInterface}, or {@code null} if the
	 *         connection is not active
	 */
	protected RemoteClientInterface getRemoteClient(ClientInfo ci) {
		if (ci == null) {
			return null;
		}
		final ClientInfoExt cx = hmClientInfoExt.get(ci.getConnectionId());
		if (cx == null) {
			return null;
		}

		RemoteClientImpl rc = cx.remoteClient;
		if (rc != null) {
			return rc;
		}
		
		Map<UUID, Boolean> hm = getRemoteMultiplexerServer().getSession(ci.getConnectionId(), false).getGuidHashMap();
		rc = new RemoteClientImpl(ci.getConnectionId(), hm) {
			/**
			 * Add objects that need to be cached to the session. This is used by datasource and copy methods.
			 */
			@Override
			public void updateObjectCache(OAObject obj) {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(obj);
				cx.remoteSession.updateObjectsWithoutHubs( obj.getClass(), obj.getObjectKey(), og.objectsInternal().callObjectHubIsInHubWithMaster(obj) );
			}

			@Override
			protected void loadDataInBackground(OAObject obj, String property) {
				OASyncServer.this.loadDataInBackground(obj, property);
			}
		};
		cx.remoteClient = rc;
		return rc;
	}

	/**
	 * Iterates over all active client sessions and instructs each session to save
	 * its cached objects using the specified cascade and rule.
	 *
	 * @param cascade       the cascade definition used when saving cached objects
	 * @param iCascadeRule  the cascade rule constant
	 */
	public void saveCache(OACascade cascade, int iCascadeRule) {
		for (Map.Entry<Integer, ClientInfoExt> entry : hmClientInfoExt.entrySet()) {
			ClientInfoExt cx = entry.getValue();
			if (cx.remoteSession != null) {
				cx.remoteSession.saveCache(cascade, iCascadeRule);
			}
		}
	}

	/**
	 * Returns the lazily created {@link ServerInfo} containing metadata such as
	 * creation time, host name, and IP address. Host information is retrieved from
	 * {@link InetAddress#getLocalHost()} when first initialized.
	 *
	 * @return the server's {@link ServerInfo} instance
	 */
	public ServerInfo getServerInfo() {
		if (serverInfo == null) {
			serverInfo = new ServerInfo();
			// serverInfo.setVersion(Resource.getValue(Resource.APP_Version, ""));
			serverInfo.setCreated(new OADateTime());
			try {
				InetAddress localHost = InetAddress.getLocalHost();
				serverInfo.setHostName(localHost.getHostName());
				serverInfo.setIpAddress(localHost.getHostAddress());
			} catch (Exception e) {
			}
		}
		return serverInfo;
	}

	/**
	 * Sets the message returned to clients when an invalid connection attempt is
	 * detected by the multiplexer server.
	 *
	 * @param msg the message to return for invalid connections
	 */
	public void setInvalidConnectionMessage(String msg) {
		getMultiplexerServer().setInvalidConnectionMessage(msg);
	}

	/**
	 * Returns a connection-invalid message. The default implementation simply
	 * returns the supplied {@code defaultMsg}, but subclasses may override this to
	 * customize the message.
	 *
	 * @param defaultMsg the fallback message
	 * @return the message to return to clients for invalid connections
	 */
	public String getInvalidConnectionMessage(String defaultMsg) {
		return defaultMsg;
	}

	/**
	 * Returns the lazily created {@link OAMultiplexerServer} that accepts and
	 * manages multiplexed socket connections. The instance overrides connection
	 * and disconnection handlers to integrate with this server and to supply
	 * server-specific invalid-connection messaging.
	 *
	 * @return the {@link OAMultiplexerServer} instance
	 */
	public OAMultiplexerServer getMultiplexerServer() {
		if (multiplexerServer == null) {
			multiplexerServer = new OAMultiplexerServer(port) {
				@Override
				protected void onClientConnect(Socket socket, int connectionId) {
					OASyncServer.this.onClientConnect(socket, connectionId);
				}

				@Override
				protected void onClientDisconnect(int connectionId) {
					getRemoteMultiplexerServer().removeSession(connectionId);
					OASyncServer.this.onClientDisconnect(connectionId);
				}

				@Override
				public String getInvalidConnectionMessage() {
					String s = super.getInvalidConnectionMessage();
					if (s == null) {
						s = OASyncServer.this.getDisplayMessage();
					}

					s = OASyncServer.this.getInvalidConnectionMessage(s);
					return s;
				}

			};
		}
		return multiplexerServer;
	}

	/**
	 * Internal container for per-connection state including {@link ClientInfo},
	 * socket reference, and remote session/client objects.
	 */
	class ClientInfoExt {
		ClientInfo ci;
		Socket socket;
		RemoteSessionImpl remoteSession;
		RemoteClientImpl remoteClient;
		RemoteClientCallbackInterface remoteClientCallback;
	}

	/**
	 * Handles a new client connection by creating a {@link ClientInfo}, recording
	 * host information, opening a remote multiplexer session, and storing the
	 * resulting {@link ClientInfoExt}.
	 *
	 * @param socket       the accepted socket connection
	 * @param connectionId the assigned connection identifier
	 */
	protected void onClientConnect(Socket socket, int connectionId) {
		LOG.fine("new client connection, id=" + connectionId);

		ClientInfo ci = new ClientInfo();
		ci.setCreated(new OADateTime());
		ci.setConnectionId(connectionId);
		ci.setIpAddress(socket.getInetAddress().getHostAddress());
		ci.setHostName(socket.getInetAddress().getHostName());

		ClientInfoExt cx = new ClientInfoExt();
		cx.ci = ci;
		cx.socket = socket;

		// this allows remoting to know if connection was removed
		getRemoteMultiplexerServer().createSession(socket, connectionId);
		hmClientInfoExt.put(connectionId, cx);
	}

	/**
	 * Handles a client disconnection by recording the disconnect timestamp,
	 * clearing locks, releasing caches when appropriate, and closing any existing
	 * {@link RemoteClientImpl}.
	 *
	 * @param connectionId the connection identifier of the disconnecting client
	 */
	protected void onClientDisconnect(int connectionId) {
		LOG.fine("client disconnect, connectionId=" + connectionId);
		ClientInfoExt cx = hmClientInfoExt.get(connectionId);
		if (cx != null) {
			cx.ci.setDisconnected(new OADateTime());
			cx.remoteSession.clearLocks();
			// 20180415 dont clear until after save is done
			// cx.remoteSession.clearCache();
			// 20160101 need to release so that it can be gc'd
			if (cx.remoteClient != null) {
				cx.remoteClient.close();
			}
			cx.remoteClient = null;
			cx.remoteClientCallback = null;
		}
	}

	/**
	 * Returns the underlying {@link Socket} for the given client connection, or
	 * {@code null} if no such connection is active.
	 *
	 * @param connectionId the connection identifier
	 * @return the associated socket, or {@code null} if unavailable
	 */
	public Socket getSocket(int connectionId) {
		ClientInfoExt cx = hmClientInfoExt.get(connectionId);
		if (cx != null) {
			return cx.socket;
		}
		return null;
	}

	/**
	 * Handles exceptions originating from a client by formatting the message with
	 * client metadata and logging it at warning level.
	 *
	 * @param ci  the client information, may be {@code null}
	 * @param msg the message describing the exception context
	 * @param ex  the thrown exception
	 */
	protected void onClientException(ClientInfo ci, String msg, Throwable ex) {
		if (ci != null) {
			msg = String.format(
								"ConnectionId=%d, User=%s, msg=%s",
								ci.getConnectionId(), ci.getUserName(), msg);
		}
		LOG.log(Level.WARNING, msg, ex);
	}

	/**
	 * Builds and returns a formatted status message containing server metadata,
	 * including version, host information, discovery state, OA version, and the
	 * number of connected clients.
	 *
	 * @return a descriptive server status message
	 */
	public String getDisplayMessage() {
		int ccnt = 0;
		for (Map.Entry<Integer, ClientInfoExt> entry : hmClientInfoExt.entrySet()) {
			ClientInfoExt cx = entry.getValue();
			if (cx.ci.getDisconnected() == null) {
				ccnt++;
			}
		}

		String msg = String.format(	"Server started=%s, version=%s, started=%b, host=%s, " +
				"ipAddress=%s, discovery=%b, oa=%d, clients connected=%d, total=%d",
									serverInfo.getCreated().toString(),
									serverInfo.getVersion(),
									serverInfo.isStarted(),
									serverInfo.getHostName(),
									serverInfo.getIpAddress(),
									serverInfo.isDiscoveryEnabled(),
									OAObject.getOAVersion(),
									ccnt,
									hmClientInfoExt.size());
		return msg;
	}

	/**
	 * Returns the lazily created {@link OARemoteMultiplexerServer} used for
	 * managing remote object lookup, broadcast channels, and client sessions.
	 * The instance overrides several behaviors, including post-invocation
	 * handling, exception routing, session lifecycle notifications, and
	 * filtering logic for determining whether sync messages should be sent to
	 * a specific client. It also registers the server’s remote interfaces and
	 * creates the sync broadcast channel.
	 *
	 * @return the configured {@link OARemoteMultiplexerServer}
	 */
	public OARemoteMultiplexerServer getRemoteMultiplexerServer() {
		if (remoteMultiplexerServer == null) {
			remoteMultiplexerServer = new OARemoteMultiplexerServer(getMultiplexerServer()) {
				@Override
				protected void afterInvokeForCtoS(RequestInfo ri) {
					OASyncServer.this.afterInvokeRemoteMethod(ri);
				}

				@Override
				protected void afterInvokeForStoC(RequestInfo ri) {
					OASyncServer.this.afterInvokeRemoteMethod(ri);
				}

				@Override
				protected void onException(int connectionId, String title, String msg, Exception e, boolean bWillDisconnect) {
					ClientInfoExt cx = hmClientInfoExt.get(connectionId);
					if (cx != null && cx.remoteClientCallback != null) {
						cx.remoteClientCallback.stop(title, msg);
						if (getSession(connectionId, false) != null) {
						    this.removeSession(connectionId);
						}
					}
				}

				@Override
				public void createSession(Socket socket, int connectionId) {
					aiSessionCount.incrementAndGet();
					super.createSession(socket, connectionId);
					OASyncServer.this.onSessionCreated(connectionId, socket);
				}

				@Override
				public void removeSession(int connectionId) {
					aiSessionCount.decrementAndGet();
					super.removeSession(connectionId);
					OASyncServer.this.onSessionRemoved(connectionId);
				}
				
                @Override
                protected boolean shouldSendSyncMessageToClient(RequestInfo ri, ConcurrentHashMap<UUID, Boolean> hmGuid) {
			        String mn = ri.method.getName();
			        if ("propertyChange".equals(mn)) {
			            OAObjectKey ok = (OAObjectKey) ri.args[1];
			            UUID x = ok.getGuid();
			            if (!hmGuid.containsKey(x)) return false;
			        }
			        else if ("removeFromHub".equals(mn)) {
			            OAObjectKey ok = (OAObjectKey) ri.args[4];
			            UUID x = ok.getGuid();
			            if (!hmGuid.containsKey(x)) return false;

			            ok = (OAObjectKey) ri.args[1];
			            x = ok.getGuid();
			            if (!hmGuid.containsKey(x)) return false;
			            
			        }
			        else if ("addToHub".equals(mn) || "insertInHub".equals(mn)) {
			            OAObjectKey ok = (OAObjectKey) ri.args[1];
			            UUID x = ok.getGuid();
			            if (!hmGuid.containsKey(x)) return false;
			            
			            //see if this client has the hub loaded by looking at an object in it
			            Class c = (Class) ri.args[0];
						final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
			            OAObject obj = (OAObject) og.objectsInternal().callObjectCacheGet(c, ok);
			            Object objx = og.objectsInternal().callObjectPropertyGetProperty(obj, (String) ri.args[2]);
			            if (objx instanceof Hub) {
			                Hub hub = (Hub) objx;
			                if (hub.size() > 1) {
			                    objx = hub.get(0);
			                    if (objx == ri.args[3]) objx = hub.get(1);
			                    if (objx instanceof OAObject) {
			                        x = ((OAObject) objx).getGuid();
			                        if (!hmGuid.containsKey(x)) return false; // hub not loaded
			                    }
			                }
			            }
			        }
			        else if ("addToNewHub".equals(mn)) {
			            OAObjectKey ok = (OAObjectKey) ri.args[1];
			            UUID x = ok.getGuid();
			            if (!hmGuid.containsKey(x)) return false;
			        }
			        else if ("removeFromHub".equals(mn)) {
			            OAObjectKey ok = (OAObjectKey) ri.args[4];
			            UUID x = ok.getGuid();
			            if (!hmGuid.containsKey(x)) return false;
			            
			            ok = (OAObjectKey) ri.args[1];
			            x = ok.getGuid();
			            if (!hmGuid.containsKey(x)) return false;
			            
			            //see if this client has the hub loaded by looking at an object in it
			            Class c = (Class) ri.args[0];
						final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
			            OAObject obj = (OAObject) og.objectsInternal().callObjectCacheGet(c, ok);
			            Object objx = og.objectsInternal().callObjectPropertyGetProperty(obj, (String) ri.args[2]);
			            if (objx instanceof Hub) {
			                Hub hub = (Hub) objx;
			                if (hub.size() > 1) {
			                    objx = hub.get(0);
			                    if (((OAObject)objx).getObjectKey() == ri.args[3]) objx = hub.get(1);
			                    if (objx instanceof OAObject) {
			                        x = ((OAObject) objx).getGuid();
			                        if (!hmGuid.containsKey(x)) return false; // hub not loaded
			                    }
			                }
			            }
			        }
			        else if ("moveObjectInHub".equals(mn)) {
			            OAObjectKey ok = (OAObjectKey) ri.args[1];
			            UUID x = ok.getGuid();
			            if (!hmGuid.containsKey(x)) return false;
			        }
			        else if ("clearHubChanges".equals(mn)) {
			            OAObjectKey ok = (OAObjectKey) ri.args[1];
			            UUID x = ok.getGuid();
			            if (!hmGuid.containsKey(x)) return false;
			        }
			        else if ("clientDelete".equals(mn)) {
			            OAObjectKey ok = (OAObjectKey) ri.args[1];
			            UUID x = ok.getGuid();
			            if (!hmGuid.containsKey(x)) return false;
			        }
			        return true;
			    }
			};

			// register remote objects
			remoteMultiplexerServer.createLookup(	ServerLookupName, getRemoteServer(), RemoteServerInterface.class, SyncQueueName,
													QueueSize);

			// create proxy so that server can also send msgs (as a Client)
			remoteSyncInterface = (RemoteSyncInterface) remoteMultiplexerServer
					.createBroadcast(SyncLookupName, getRemoteSyncImpl(), RemoteSyncInterface.class, SyncQueueName, QueueSize);
			
			// this is so that the server will use a proxy and not the impl
			//was: OASyncDelegate.setRemoteSync(packagex, rsi);

			// have RemoteClient objects use sync queue
			// remoteMultiplexerServer.registerClassWithQueue(RemoteClientInterface.class, SyncQueueName, QueueSize);            
		}
		return remoteMultiplexerServer;
	}

	private AtomicInteger aiSessionCount = new AtomicInteger(); // needs to start at 1 (0 is for the server)

	/**
	 * Returns the number of active remote sessions tracked by the remote
	 * multiplexer server.
	 *
	 * @return the current session count
	 */
	public int getSessionCount() {
		return aiSessionCount.get();
	}

	/**
	 * Callback invoked when a remote session is created. This implementation
	 * performs no additional work but can be overridden to add custom behavior.
	 *
	 * @param connectionId the identifier of the newly created session
	 * @param socket       the socket associated with the session
	 */
	protected void onSessionCreated(int connectionId, Socket socket) {
	}

	/**
	 * Callback invoked when a remote session is removed. This implementation
	 * performs no additional work but can be overridden to add custom behavior.
	 *
	 * @param connectionId the identifier of the removed session
	 */
	protected void onSessionRemoved(int connectionId) {
	}

	/**
	 * Registers a lookup entry with the remote multiplexer server using the
	 * specified name, object instance, and interface class. No custom queue
	 * configuration is used.
	 *
	 * @param name           the binding name for the remote object
	 * @param obj            the object instance to expose
	 * @param interfaceClass the interface clients will use when invoking methods
	 */
	public void createLookup(String name, Object obj, Class interfaceClass) {
		getRemoteMultiplexerServer().createLookup(name, obj, interfaceClass, null, -1);
	}

	/**
	 * Registers a lookup entry for the given object using the sync queue
	 * parameters defined by {@link #SyncQueueName} and {@link #QueueSize}.
	 *
	 * @param name           the binding name for the remote object
	 * @param obj            the object instance to expose
	 * @param interfaceClass the interface clients will use when invoking methods
	 */
	public void createSyncLookup(String name, Object obj, Class interfaceClass) {
		getRemoteMultiplexerServer().createLookup(name, obj, interfaceClass, SyncQueueName, QueueSize);
	}

	/**
	 * Registers a lookup entry for the given object and interface using the
	 * specified queue name and size.
	 *
	 * @param name           the binding name for the remote object
	 * @param obj            the object instance to expose
	 * @param interfaceClass the interface clients will use
	 * @param queueName      the name of the queue to associate with this lookup
	 * @param queueSize      maximum number of messages allowed in the queue
	 */
	public void createLookup(String name, Object obj, Class interfaceClass, String queueName, int queueSize) {
		getRemoteMultiplexerServer().createLookup(name, obj, interfaceClass, queueName, queueSize);
	}

	/**
	 * Creates and returns a broadcast remote object bound under the specified
	 * name, using the given interface, queue name, and queue size.
	 *
	 * @param bindName       the lookup name for the broadcast
	 * @param interfaceClass the broadcast interface class
	 * @param queueName      name of the broadcast queue
	 * @param queueSize      maximum broadcast queue size
	 * @return the created broadcast remote object
	 */
	public Object createBroadcast(final String bindName, Class interfaceClass, String queueName, int queueSize) {
		return getRemoteMultiplexerServer().createBroadcast(bindName, interfaceClass, queueName, queueSize);
	}

	/**
	 * Creates and returns a broadcast remote object bound under the specified
	 * name and configured with the given callback object, interface class,
	 * queue name, and queue size.
	 *
	 * @param bindName       the lookup name for the broadcast
	 * @param callback       the callback instance for broadcast events
	 * @param interfaceClass the broadcast interface class
	 * @param queueName      name of the broadcast queue
	 * @param queueSize      maximum broadcast queue size
	 * @return the created broadcast remote object
	 */
	public Object createBroadcast(final String bindName, Object callback, Class interfaceClass, String queueName, int queueSize) {
		return getRemoteMultiplexerServer().createBroadcast(bindName, callback, interfaceClass, queueName, queueSize);
	}

	/**
	 * Creates and returns a broadcast remote object using the sync queue
	 * parameters defined by {@link #SyncQueueName} and {@link #QueueSize}.
	 *
	 * @param bindName       the broadcast lookup name
	 * @param interfaceClass the broadcast interface class
	 * @return the created broadcast remote object
	 */
	public Object createSyncBroadcast(final String bindName, Class interfaceClass) {
		return getRemoteMultiplexerServer().createBroadcast(bindName, interfaceClass, SyncQueueName, QueueSize);
	}

	/**
	 * Creates and returns a broadcast remote object using the sync queue
	 * parameters and the specified callback handler.
	 *
	 * @param bindName       the broadcast lookup name
	 * @param callback       callback instance to receive broadcast events
	 * @param interfaceClass the broadcast interface class
	 * @return the created broadcast remote object
	 */
	public Object createSyncBroadcast(final String bindName, Object callback, Class interfaceClass) {
		return getRemoteMultiplexerServer().createBroadcast(bindName, callback, interfaceClass, SyncQueueName, QueueSize);
	}

	/* this was removed - args/result that are remote objects will use same queue as parent object    
	public void registerClassWithQueue(Class clazz) {
	    getRemoteMultiplexerServer().registerClassWithQueue(clazz, SyncQueueName, QueueSize);
	}
	*/

	/**
	 * Called after a remote method invocation completes. Unless the request is a
	 * normal sync message with no exception, the {@link RequestInfo} is added to
	 * the logging queue if available; otherwise it is logged at FINE level.
	 *
	 * @param ri information about the completed remote method invocation
	 */
	protected void afterInvokeRemoteMethod(RequestInfo ri) {
		if (ri == null) {
			return;
		}

		// dont log oasync msgs
		if (ri.bind == null) {
			return;
		}
		if (ri.bind.isOASync) {
			if (ri.exception == null && ri.exceptionMessage == null) {
				return;
			}
		}

		try {
			if (queRemoteRequestLogging != null) {
				if (queRemoteRequestLogging.offer(ri, 2, TimeUnit.MILLISECONDS)) {
					return;
				}
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "error adding remote request to log queue", e);
		}
		LOG.fine("RemoteLog data: " + ri.toLogString());
	}

	/**
	 * Background thread used to process and write remote request log entries.
	 */
	private Thread threadStatsLogger;

	/**
	 * Writer used by the request logging thread to write formatted log entries to
	 * the current log file.
	 */
	private PrintWriter pwRemoteRequestLogger;

	/**
	 * Timestamp indicating when the next rollover to a new daily log file should
	 * occur.
	 */
	private long msNextRemoteRequestLogDateChange;

	/**
	 * Starts the request-logging subsystem, including creating the logging queue
	 * and launching the background thread that writes request log entries to
	 * disk. If logging is disabled or already started, the method returns without
	 * creating additional threads.
	 *
	 * @throws Exception if the log writer cannot be created
	 */
	void startRequestLoggerThread() throws Exception {
		LOG.fine("starting remote method log thread");
		if (threadStatsLogger != null) {
			return;
		}

		if (getRemoteRequestLogPrintWriter() == null) {
			LOG.fine("remote log file name is null, will not log remote messages");
			return;
		}

		queRemoteRequestLogging = new ArrayBlockingQueue<RequestInfo>(1000);

		String tname = "OASyncServer_logRequests";
		LOG.config("starting thread that writes logs, threadName=" + tname);
		threadStatsLogger = new Thread(new Runnable() {
			@Override
			public void run() {
				_runRequestStatsLogger();
			}
		}, tname);
		threadStatsLogger.setDaemon(true);
		threadStatsLogger.setPriority(Thread.MIN_PRIORITY);
		threadStatsLogger.start();
	}

	/**
	 * Internal loop executed by the request-logging thread. Continuously takes
	 * {@link RequestInfo} objects from the logging queue and writes them to the
	 * log file, handling and rate-limiting errors.
	 */
	private void _runRequestStatsLogger() {
		LOG.fine("Request logger thread is now running");
		int errorCnt = 0;
		long tsLastError = 0;
		for (int i = 0;; i++) {
			try {
				RequestInfo ri = queRemoteRequestLogging.take();
				logRequest(ri);
			} catch (Exception e) {
				long tsNow = System.currentTimeMillis();
				if (tsLastError == 0 || tsLastError + 30000 < tsNow) {
					errorCnt++;
					LOG.log(Level.WARNING, "error processing request from log queue, errorCnt=" + errorCnt, e);
					tsLastError = tsNow;
				}
			}
		}
	}

	/**
	 * Writes a formatted log entry for the specified {@link RequestInfo} to the
	 * current log file, or prints it to standard output if the log writer is not
	 * available.
	 *
	 * @param ri the request information to log
	 * @throws Exception if the log writer cannot be accessed
	 */
	protected void logRequest(RequestInfo ri) throws Exception {
		if (ri == null) {
			return;
		}

		PrintWriter pw = null;
		try {
			pw = getRemoteRequestLogPrintWriter();
		} catch (Exception e) {
			pw = null;
		}
		if (pw != null) {
			pw.println(ri.toLogString());
			pw.flush();
		} else {
			System.out.println("Remote RequestLog data: " + ri.toLogString());
		}
	}

	/**
	 * Opens or returns the existing log file writer. Handles daily log rotation
	 * based on the current date. When creating a new log file, writes an initial
	 * header line.
	 *
	 * @return the active {@link PrintWriter} for request logs, or {@code null} if
	 *         logging is disabled
	 * @throws Exception if the log file cannot be opened
	 */
	private PrintWriter getRemoteRequestLogPrintWriter() throws Exception {
		if (pwRemoteRequestLogger != null) {
			if (System.currentTimeMillis() < msNextRemoteRequestLogDateChange) {
				return pwRemoteRequestLogger;
			}
		}
		OADate date = new OADate();
		msNextRemoteRequestLogDateChange = date.addDays(1).getTime();
		if (pwRemoteRequestLogger != null) {
			pwRemoteRequestLogger.close();
			pwRemoteRequestLogger = null;
		}
		String fileName = getLogFileName();
		LOG.config("Remote log file is " + fileName);
		if (fileName == null) {
			return null;
		}
		FileOutputStream fout = new FileOutputStream(fileName, true);
		BufferedOutputStream bout = new BufferedOutputStream(fout);
		pwRemoteRequestLogger = new PrintWriter(bout);
		pwRemoteRequestLogger.println(RequestInfo.getLogHeader());
		pwRemoteRequestLogger.flush();
		return pwRemoteRequestLogger;
	}

	/**
	 * Returns the full filename to use for writing request logs. The default
	 * implementation returns {@code null}, which disables request logging.
	 *
	 * @return the log filename, or {@code null} to disable logging
	 */
	protected String getLogFileName() {
		return null;
	}

	/**
	 * Starts the sync server by initializing and launching the request logger,
	 * server metadata, multiplexer server, remote multiplexer server, server file
	 * services, and background sibling-loading thread.
	 *
	 * @throws Exception if any startup operation fails
	 */
	public void start() throws Exception {
		startRequestLoggerThread();
		getServerInfo();
		getMultiplexerServer().start();
		getRemoteMultiplexerServer().start();
		getServerFile().start(getMultiplexerServer());
		startLoadDataInBackgroundThread();
	}

	/**
	 * Stops the sync server by shutting down the multiplexer server and file
	 * transfer services.
	 *
	 * @throws Exception if shutdown operations fail
	 */
	public void stop() throws Exception {
		if (multiplexerServer != null) {
			multiplexerServer.stop();
		}
		getServerFile().stop();
	}

	/**
	 * Performs distributed garbage collection on the remote multiplexer server,
	 * if it has been initialized.
	 */
	public void performDGC() {
		if (remoteMultiplexerServer != null) {
			getRemoteMultiplexerServer().performDGC();
		}
	}

	/**
	 * Returns the lazily created {@link ServerFile} instance used for handling
	 * server-side file uploads and downloads. The instance is created with the
	 * directory name {@code "shared"}.
	 *
	 * @return the {@link ServerFile} instance
	 */
	public ServerFile getServerFile() {
		if (serverFile == null) {
			serverFile = new ServerFile("shared");
		}
		return serverFile;
	}

	/**
	 * Creates a new request to load a sibling property for the specified object.
	 * The current system time is recorded for age-based filtering.
	 *
	 * @param obj      the object whose sibling data should be loaded
	 * @param property the property name to load
	 */
	private static class LoadSibling {
		long ms;
		OAObject obj;
		String property;

		public LoadSibling(OAObject obj, String property) {
			this.ms = System.currentTimeMillis();
			this.obj = obj;
			this.property = property;
		}
	}

	/**
	 * Queue of pending sibling-load requests to be processed by the background
	 * loading thread.
	 */
	private final ArrayBlockingQueue<LoadSibling> queLoadDataInBackground = new ArrayBlockingQueue<>(250);

	/**
	 * Adds a request to load sibling or detail data for the specified object when
	 * it could not be loaded during a client request, typically due to timeout.
	 *
	 * @param obj      the object requiring additional data loading
	 * @param property the property to load
	 */
	protected void loadDataInBackground(OAObject obj, String property) {
		queLoadDataInBackground.offer(new LoadSibling(obj, property));
	}

	private Thread threadLoadSibling;

	/**
	 * Starts the background thread responsible for processing sibling-load
	 * requests from {@link #queLoadDataInBackground}. The thread is created as a
	 * daemon with low priority.
	 *
	 * @throws Exception if the thread cannot be created
	 */
	protected void startLoadDataInBackgroundThread() throws Exception {
		LOG.fine("starting LoadSibling log thread");

		String tname = "OASyncServer_LoadSibling";
		LOG.config("starting thread that Load Sibling obj/props for a client getDetail request, threadName=" + tname);
		threadLoadSibling = new Thread(new Runnable() {
			@Override
			public void run() {
				_runLoadDataInBackground();
			}
		}, tname);
		threadLoadSibling.setDaemon(true);
		threadLoadSibling.setPriority(Thread.MIN_PRIORITY);
		threadLoadSibling.start();
	}

	/**
	 * Starts the background thread responsible for processing sibling-load
	 * requests from {@link #queLoadDataInBackground}. The thread is created as a
	 * daemon with low priority.
	 *
	 * @throws Exception if the thread cannot be created
	 */
	protected void _runLoadDataInBackground() {
		long msLastError = 0;
		for (;;) {
			long msNow = System.currentTimeMillis();
			try {
				LoadSibling ls = queLoadDataInBackground.take();
				if (ls.obj == null) {
					continue;
				}
				if (msNow > ls.ms + 10000) {
					LOG.finer("not loading, too old,  obj=" + ls.obj.getClass().getSimpleName() + ", prop=" + ls.property);
					continue;
				}
				LOG.finer("loading obj=" + ls.obj.getClass().getSimpleName() + ", prop=" + ls.property);

				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(ls.obj);
				if (og.objectsInternal().callObjectPropertyIsPropertyLocked(ls.obj, ls.property)) {
					continue;
				}

				og.objectsInternal().callObjectReflectGetProperty(ls.obj, ls.property); // load from DS
			} catch (Exception e) {
				if (msNow > (msLastError + 5000)) {
					LOG.log(Level.WARNING, "Exception in LoadSibling thread", e);
					msLastError = msNow;
				}
			}
		}
	}

}
