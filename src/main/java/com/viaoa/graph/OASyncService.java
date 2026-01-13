package com.viaoa.graph;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.remote.*;

public class OASyncService {
	private static final Logger LOG = Logger.getLogger(OASyncService.class.getName());
	
	private final String pkgName;

	
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

	
	public OASyncService(String pkgName) {
    	this.pkgName = pkgName;
	}

	public void initialize() {
	}
	
    public void createServer(int port) throws Exception {
    	stopServer();
    	Package packageThis = Package.getPackage(pkgName);
        syncServer = new OASyncServer(packageThis, port);
        syncServer.setInvalidConnectionMessage("qqqqqqqq"); //qqqqqq
        syncServer.start();
    }
    
    public void stopServer() throws Exception {
    	if (syncServer != null) {
    		syncServer.stop();
        	syncServer = null;
    	}
    }
    
    public void createClient(String serverName, int port) throws Exception {
    	stopClient();
    	Package packageThis = Package.getPackage(pkgName);
    	syncClient = new OASyncClient(packageThis, serverName, port);
        syncClient.start();
    }

    public void stopClient() throws Exception {
    	if (syncClient != null) {
    		syncClient.stop();
    		syncClient = null;
    	}
    }
    
    
    
    
    public OASyncClient getClient() {
    	return syncClient;
    }


	/**
	 * Returns the active {@link OASyncServer}. If a single server instance
	 * exists, it is returned; otherwise package-based lookup is used.
	 *
	 * @return the sync server instance, or {@code null} if not registered
	 */
	public OASyncServer getSyncServer() {
		return syncServer;
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
	public void setSyncServer(OASyncServer ss) {
		syncServer = ss;
	}

	// ========= SyncClient ============
	/**
	 * Returns the active {@link OASyncClient}. If a single client instance
	 * exists, it is returned; otherwise package-based lookup is used.
	 *
	 * @return the sync client instance, or {@code null} if not registered
	 */
	public OASyncClient getSyncClient() {
		return syncClient;
	}

	/**
	 * Registers or removes an {@link OASyncClient} for the specified package.
	 * Maintains both a per-package entry and a cached global instance when
	 * only one client exists.
	 *
	 * @param p the package to associate with the client
	 * @param sc the client instance, or {@code null} to remove
	 */
	public void setSyncClient(OASyncClient sc) {
		syncClient = sc;
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
	public int getConnectionId() {
		OASyncClient sc = getSyncClient();
		if (sc == null) {
			return -1;
		}
		return sc.getConnectionId();
	}


	/**
	 * Returns {@code true} if neither a sync server nor sync client has been
	 * registered for the default package.
	 *
	 * @return {@code true} if running in single-user mode
	 */
	public boolean isSingleUser() {
		return syncServer == null && syncClient == null;
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
	public boolean isConnected() {
	    if (syncClient != null) return syncClient.isConnected();
	    return (syncServer != null);	}


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
	/*qqqqqqqqqqqqq 
	public long getGuidFromServer() {
		if (isServer()) {
	    	Package packageThis = Package.getPackage(pkgName);
			return OAObjectDelegate.getNextGuid(packageThis);
		}
		long x;
		synchronized (NextGuidLock) {
			if (nextGuid == maxNextGuid) {
				try {
					nextGuid = getSyncClient().getRemoteServer().getNextFiftyObjectGuids();
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
	*/

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
	public boolean sendMessages() {
		return OARemoteThreadDelegate.sendMessages();
	}

	/**
	 * Enables or disables sending sync messages for the current thread.
	 * Delegates to {@link OARemoteThreadDelegate#sendMessages(boolean)}.
	 *
	 * @param b {@code true} to send messages, {@code false} to suppress
	 * @return previous setting for message sending
	 */
	public boolean sendMessages(boolean b) {
		return OARemoteThreadDelegate.sendMessages(b);
	}

	/**
	 * Determines whether the current thread is an {@code OARemoteThread},
	 * which is used internally to process incoming sync messages.
	 *
	 * @return {@code true} if the current thread is remote-thread context
	 */
	public boolean isRemoteThread() {
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
	public boolean isSyncThread() {
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
	public boolean shouldSendMessages() {
		return OARemoteThreadDelegate.shouldSendMessages();
	}

	/**
	 * Enables or disables suppression of client–server (CS) sync messages for
	 * the current thread. Delegates to
	 * {@link OAThreadLocalDelegate#setSuppressCSMessages(boolean)}.
	 *
	 * @param b whether to suppress CS messages
	 */
	public void setSuppressCSMessages(boolean b) {
		OAThreadLocalDelegate.setSuppressCSMessages(b);
	}

	/**
	 * Returns whether CS sync messages are currently suppressed for the
	 * current thread. Delegates to
	 * {@link OAThreadLocalDelegate#isSuppressCSMessages()}.
	 *
	 * @return {@code true} if CS messages are suppressed
	 */
	public boolean getSuppressCSMessages() {
		return OAThreadLocalDelegate.isSuppressCSMessages();
	}

	/**
	 * Returns the {@link RequestInfo} associated with the current thread
	 * if it is an {@code OARemoteThread}. This describes the sync message
	 * currently being processed.
	 *
	 * @return the request info, or {@code null} if not in remote-thread context
	 */
	public RequestInfo getRequestInfo() {
		return OARemoteThreadDelegate.getRequestInfo();
	}

	/**
	 * Returns the connection ID associated with the sync message currently
	 * being processed by the current thread. If no request information is
	 * available, returns -1.
	 *
	 * @return the current request's connection ID, or -1 if unavailable
	 */
	public int getRequestConnectionId() {
		RequestInfo ri = OARemoteThreadDelegate.getRequestInfo();
		if (ri == null) {
			return -1;
		}
		return ri.connectionId;
	}

	/**
	 * Marks the current thread as performing loading operations by setting
	 * the thread-local loading flag to {@code true}.
	 */
	public void setLoading() {
		OAThreadLocalDelegate.setLoading(true);
	}

	/**
	 * Sets or clears the thread-local loading flag, used to indicate whether
	 * the current thread is performing object-loading operations.
	 *
	 * @param b {@code true} to mark as loading, {@code false} otherwise
	 */
	public void setLoading(boolean b) {
		OAThreadLocalDelegate.setLoading(b);
	}

	public boolean isClient() {
		OASyncServer ss = getSyncServer();
		OASyncClient sc = getClient();
		return (ss == null && sc != null);
	}

	public boolean isServer() {
		OASyncServer ss = getSyncServer();
		OASyncClient sc = getClient();
		return (ss != null || sc == null);
	}
	
}
