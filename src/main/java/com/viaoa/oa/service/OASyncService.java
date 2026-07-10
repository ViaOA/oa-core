package com.viaoa.oa.service;

import java.util.logging.Logger;

import com.viaoa.cascade.OACascade;
import com.viaoa.datasource.clientserver.OADataSourceClient;
import com.viaoa.oa.OA;
import com.viaoa.oa.api.SyncOps;
import com.viaoa.oa.api.internal.SyncInternalOps;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.runtime.OARemoteThreadService;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.model.ClientInfo;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.sync.remote.RemoteSessionInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;




/**
 * Coordinates OA synchronization role and remote access for one OA runtime.
 * <p>
 * The service can be configured as server or client, exposes the remote sync
 * interfaces used by OA services, and provides role/status helpers used by
 * object, Hub, replication, and datasource paths.
 * </p>
 */
public class OASyncService implements SyncInternalOps, SyncOps {
	private static final Logger LOG = Logger.getLogger(OASyncService.class.getName());
	
	private final OA og;
	
	/**
	 * Cached reference to a single {@link OASyncServer} when only one is
	 * active in the JVM. Set to {@code null} when multiple servers exist.
	 */
	private OASyncServer syncServer;

	/**
	 * Cached single {@link OASyncClient} instance if only one exists;
	 * otherwise set to {@code null} to force per-package lookup.
	 */
	private OASyncClient syncClient;

	
	/**
	 * Lock used to synchronize allocation of GUID values requested from the
	 * sync server in client mode.
	 */
	private final Object NextGuidLock = new Object();
	
	/**
	 * The starting GUID value of the current allocation block received from
	 * the server. Incremented until {@link #maxNextGuid} is reached.
	 */
	private long nextGuid;
	
	/**
	 * The upper bound (exclusive) for the current GUID block allocated from
	 * the server. When {@code nextGuid == maxNextGuid}, a new block is
	 * requested.
	 */
	private long maxNextGuid;

	private volatile boolean bRunning; 
	
	
	/**
	 * Creates a synchronization service for an OA runtime.
	 *
	 * @param og owning OA runtime
	 */
	public OASyncService(OA og) {
		this.og = og;
	}

	/**
	 * Configures this sync service as a server listening on the supplied port.
	 *
	 * @param port server port
	 * @throws RuntimeException if a sync client or server has already been created
	 */
	@Override
    public void createServer(int port) {
		if (syncClient != null) throw new RuntimeException("Sync Client already created");
		if (syncServer != null) throw new RuntimeException("Sync Server already created");
        syncServer = new OASyncServer(port);
        syncServer.setInvalidConnectionMessage("Invalid connection, must use OAMultiplexer");
    }
	
	/**
	 * Configures this sync service as a client connected to the supplied server.
	 *
	 * @param serverName server host name
	 * @param port server port
	 * @throws RuntimeException if a sync client or server has already been created
	 */
	@Override
    public void createClient(String serverName, int port) {
		if (syncClient != null) throw new RuntimeException("Sync Client already created");
		if (syncServer != null) throw new RuntimeException("Sync Server already created");
		
    	syncClient = new OASyncClient(serverName, port) {
			private OADataSourceClient dataSourceClient;

			@Override
			protected void createRemoteDataSource() {
				if (dataSourceClient == null) {
					dataSourceClient = new OADataSourceClient(og.getPackageName());
				}
			}

			@Override
			protected void closeRemoteDataSource() {
				if (dataSourceClient != null) {
					dataSourceClient.close();
				}
			}

    	};
    }
	
	/**
	 * Starts the configured sync server or client.
	 *
	 * @throws Exception when startup fails or the service is already running
	 */
	@Override
	public void start() throws Exception {
		if (bRunning) throw new Exception("already running");
		if (syncServer != null) {
			syncServer.start();
		}
		else if (syncClient != null) {
			syncClient.start();
		}
		else {
			throw new RuntimeException("OASync is not Server or Client");
		}
		bRunning = true;
	}

	/**
	 * Returns whether the sync server or client has been started.
	 *
	 * @return {@code true} when running
	 */
	@Override
	public boolean isRunning() {
		return bRunning;
	}
	
	/**
	 * Stops the active sync server or client when running.
	 *
	 * @throws Exception when shutdown fails
	 */
	@Override
	public void stop() throws Exception {
		if (!bRunning) return;
		if (syncServer != null) syncServer.stop();
		if (syncClient != null) syncClient.stop();
		bRunning = false;
	}
	
	
	
	/**
	 * Returns the active {@link OASyncServer}. If a single server instance
	 * exists, it is returned; otherwise package-based lookup is used.
	 *
	 * @return the sync server instance, or {@code null} if not registered
	 */
	@Override
	public OASyncServer getServer() {
		return syncServer;
	}

	// ========= SyncClient ============
	/**
	 * Returns the active {@link OASyncClient}. If a single client instance
	 * exists, it is returned; otherwise package-based lookup is used.
	 *
	 * @return the sync client instance, or {@code null} if not registered
	 */
	@Override
	public OASyncClient getClient() {
		return syncClient;
	}

	/**
	 * Retrieves the {@link RemoteServerInterface} registered for the given
	 * package. Falls back to the {@link #ObjectPackage} registration when
	 * appropriate. Uses cached global instance if available.
	 *
	 * @param p the package whose remote server interface is requested
	 * @return the interface instance or {@code null}
	 */
	public RemoteServerInterface getRemoteServer() {
		if (syncServer != null) return syncServer.getRemoteServer();
		if (syncClient == null) return null;
		try {
			return syncClient.getRemoteServer();
		}
		catch (Exception e) {
			throw new RuntimeException("OASyncService.getRemoteServer from client exception", e);
		}
	}

	/**
	 * Returns the active {@link RemoteSessionInterface}. Uses the cached
	 * global instance if present; otherwise performs package-based lookup.
	 *
	 * @return the remote session interface or {@code null}
	 */
	public RemoteSessionInterface getRemoteSession() {
		if (syncServer != null) {
			// Implementation 
			return syncServer.getRemoteSessionForServer(); 
		}
		if (syncClient == null) return null;
		try {
			return syncClient.getRemoteSession();
		}
		catch (Exception e) {
			throw new RuntimeException("OASyncService.getRemoteSession from client exception", e);
		}
	}

	/**
	 * Returns the active {@link RemoteClientInterface}, using the cached
	 * global instance when present, otherwise performing package-based lookup.
	 *
	 * @return the remote client interface or {@code null}
	 */
	public RemoteClientInterface getRemoteClient() {
		if (syncServer != null) return syncServer.getRemoteClientForServer();
		if (syncClient == null) return null;
		try {
			return syncClient.getRemoteClient();
		}
		catch (Exception e) {
			throw new RuntimeException("OASyncService.getRemoteClient from client exception", e);
		}
	}

	/**
	 * Retrieves the active {@link RemoteSyncInterface}. If a cached global
	 * instance exists, it is returned; otherwise package-based lookup occurs.
	 *
	 * @return the remote sync interface or {@code null}
	 */
	public RemoteSyncInterface getRemoteSync() {
		if (syncServer != null) return syncServer.getRemoteSyncInterface();
		if (syncClient == null) return null;
		try {
			return syncClient.getRemoteSync();
		}
		catch (Exception e) {
			throw new RuntimeException("OASyncService.getRemotesync from client exception", e);
		}
	}


	/**
	 * Returns the connection ID of the active {@link OASyncClient} associated
	 * with the given package. Returns {@code -1} if no client exists.
	 *
	 * @param p the package to look up
	 * @return the connection ID, or {@code -1} if unavailable
	 */
	@Override
	public int getConnectionId() {
		OASyncClient sc = getClient();
		if (sc == null) {
			return -1;
		}
		return sc.getConnectionId();
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
	@Override
	public boolean isConnected() {
	    if (syncClient != null) return syncClient.isConnected();
	    return (syncServer != null);	
	}

	/**
	 * Returns the {@link RequestInfo} associated with the current thread
	 * if it is an {@code OARemoteThread}. This describes the sync message
	 * currently being processed.
	 *
	 * @return the request info, or {@code null} if not in remote-thread context
	 */
	public RequestInfo getRequestInfo() {
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
		return srvcOARemoteThread.getRequestInfo();
	}

	/**
	 * Returns the connection ID associated with the sync message currently
	 * being processed by the current thread. If no request information is
	 * available, returns -1.
	 *
	 * @return the current request's connection ID, or -1 if unavailable
	 */
	@Override
	public int getRequestConnectionId() {
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
		RequestInfo ri = srvcOARemoteThread.getRequestInfo();
		if (ri == null) {
			return -1;
		}
		return ri.connectionId;
	}

	
	
	/**
	 * Returns {@code true} if neither a sync server nor sync client has been
	 * registered for the default package.
	 *
	 * @return {@code true} if running in single-user mode
	 */
	@Override
	public boolean isSingleUser() {
		OASyncServer ss = getServer();
		OASyncClient sc = getClient();
		return ss == null && sc == null;
	}

	/**
	 * Returns whether this service is configured with a sync server.
	 *
	 * @return {@code true} when server role is configured
	 */
	@Override
	public boolean isServer() {
		OASyncServer ss = getServer();
		return (ss != null);
	}
	
	/**
	 * Returns whether this service is configured with a sync client.
	 *
	 * @return {@code true} when client role is configured
	 */
	@Override
	public boolean isClient() {
		OASyncClient sc = getClient();
		return (sc != null);
	}
	
	/**
	 * Sends an exception to the active remote session, when one is available.
	 *
	 * @param msg message describing the client-side exception
	 * @param ex exception to send
	 */
	@Override
	public void sendException(String msg, Throwable ex) {
		RemoteSessionInterface rci = getRemoteSession();
		if (rci != null) {
			rci.sendException("client exception: " + msg, ex);
		}
	}

	/**
	 * Returns sync client information for the active client role.
	 *
	 * @return client information, or {@code null} when no client is configured
	 */
	@Override
	public ClientInfo getClientInfo() {
		OASyncClient sc = getClient(); 
		if (sc == null) return null;
		return sc.getClientInfo();
	}

	/**
	 * Updates client information through the active remote session.
	 *
	 * @param ci client information to update
	 */
	@Override
	public void updateClientInfo(ClientInfo ci) {
		RemoteSessionInterface rsi = getRemoteSession();
		if (rsi != null) {
			rsi.update(ci);
		}
	}

	/**
	 * Requests server-side cache save processing.
	 *
	 * @param cascade cascade tracker
	 * @param iCascadeRule cascade rule to apply
	 */
	@Override
	public void saveCache(OACascade cascade, int iCascadeRule) {
		getServer().saveCache(cascade, iCascadeRule);		
	}

	/**
	 * Performs distributed garbage collection when this service is in server role.
	 */
	@Override
	public void performDGC() {
		if (isServer()) {
			getServer().performDGC();
		}
	}


	/**
	 * Requests a remote client refresh for an object.
	 *
	 * @param class1 object class
	 * @param objectKey object key
	 */
	@Override
	public void callRemoteClientRefresh(Class<? extends OAObject> class1, OAObjectKey objectKey) {
		if (syncClient == null) return;
		try {
			syncClient.getRemoteClient().refresh(class1, objectKey);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Requests a remote client refresh for an object link.
	 *
	 * @param class1 object class
	 * @param objectKey object key
	 * @param linkPropertyName link property to refresh
	 */
	@Override
	public void callRemoteClientRefresh(Class<? extends OAObject> class1, OAObjectKey objectKey, String linkPropertyName) {
		if (syncClient == null) return;
		try {
			syncClient.getRemoteClient().refresh(class1, objectKey, linkPropertyName);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets an existing sync server instance.
	 *
	 * @param ss sync server to use
	 */
	@Override
	public void createServer(OASyncServer ss) {
		syncServer = ss;
	}

	/**
	 * Sets an existing sync client instance.
	 *
	 * @param sc sync client to use
	 */
	@Override
	public void createClient(OASyncClient sc) {
		syncClient = sc;
	}

}
