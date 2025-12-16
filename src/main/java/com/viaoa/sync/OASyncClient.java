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

import static com.viaoa.sync.OASyncServer.ServerLookupName;
import static com.viaoa.sync.OASyncServer.SyncLookupName;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.comm.multiplexer.OAMultiplexerClient;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.clientserver.OADataSourceClient;
import com.viaoa.hub.Hub;
import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.remote.multiplexer.OARemoteMultiplexerClient;
import com.viaoa.sync.file.ClientFile;
import com.viaoa.sync.model.ClientInfo;
import com.viaoa.sync.remote.RemoteClientCallbackInterface;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.sync.remote.RemoteSessionInterface;
import com.viaoa.sync.remote.RemoteSyncImpl;
import com.viaoa.sync.remote.RemoteSyncInterface;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OALogUtil;

/**
 * Client-side synchronization endpoint for an OA model.
 * <p>
 * An {@code OASyncClient} establishes a multiplexer connection to an
 * {@link OASyncServer}, obtains remote interfaces for server, session,
 * client, and sync operations, and participates in distributed object graph
 * synchronization.
 *
 * <h2>Startup Responsibilities</h2>
 * When {@link #start()} is invoked, the client:
 * <ul>
 *   <li>starts the underlying {@code OAMultiplexerClient},</li>
 *   <li>creates a {@code OARemoteMultiplexerClient} for remote method
 *       invocation,</li>
 *   <li>retrieves remote proxies for server, session, client, and sync
 *       interfaces,</li>
 *   <li>(optionally) registers itself with {@link OASyncDelegate} so the
 *       model can be located globally,</li>
 *   <li>spawns background threads for:
 *       <ul>
 *         <li>distributed garbage collection (tracking objects not referenced
 *             by any hub),</li>
 *         <li>tracking objects that require retention while in use on the
 *             client, and</li>
 *         <li>(optionally) periodic update calls to the remote session.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h2>Synchronization Behavior</h2>
 * The client receives and applies change events coming from the server:
 * <ul>
 *   <li>object property changes,</li>
 *   <li>hub insert/remove/replace events,</li>
 *   <li>link changes and detail loading,</li>
 *   <li>cache invalidation and remote lock notifications.</li>
 * </ul>
 *
 * <p>
 * The client also sends outbound changes initiated locally, ensuring that the
 * server maintains the authoritative model.
 *
 * <h2>Lifespan</h2>
 * An {@code OASyncClient} is intended to be long-lived. Its background threads
 * are daemon threads and will remain active until the JVM exits unless a
 * shutdown protocol is added externally.
 */
public class OASyncClient {
	protected static final Logger LOG = Logger.getLogger(OASyncClient.class.getName());

	/**
	 * Underlying multiplexer client responsible for managing the low-level
	 * socket connection to the sync server.
	 */
	private OAMultiplexerClient multiplexerClient;

	/**
	 * Remote multiplexer client wrapper that exposes remote method invocation
	 * capabilities over the {@link #multiplexerClient} connection.
	 */
	private OARemoteMultiplexerClient remoteMultiplexerClient;

	/**
	 * Client meta-data that is sent to the server, including host, port,
	 * creation time, memory statistics, and connection/session-related values.
	 */
	private ClientInfo clientInfo;

	/**
	 * Lazily obtained remote server interface used to create sessions and
	 * access other remote endpoints on the sync server.
	 */
	private RemoteServerInterface remoteServerInterface;

	/**
	 * Remote session interface representing this client's logical session on
	 * the server, used for updates, object lifecycle notifications, and GC
	 * coordination.
	 */
	private RemoteSessionInterface remoteSessionInterface;
	
	/**
	 * Remote client interface used to invoke client-specific sync operations,
	 * such as detail loading, on the server.
	 */
	private RemoteClientInterface remoteClientSyncInterface;
	
	/**
	 * Broadcast-capable remote sync interface used to receive and send sync
	 * messages between this client and the server (and other clients).
	 */
	private RemoteSyncInterface remoteSyncInterface;
	
	/**
	 * Local callback implementation of the sync interface that is registered
	 * with the server so it can send sync messages back to this client.
	 */
	private RemoteSyncInterface remoteSyncImpl;
	
	/**
	 * Host name of the sync server that this client connects to.
	 */
	private String serverHostName;
	
	/**
	 * Port number of the sync server that this client connects to.
	 */
	private int serverHostPort;
	
	/**
	 * Flag indicating whether this client should register itself and its
	 * remote interfaces with {@code OASyncDelegate}, marking it as the
	 * primary sync client for the associated package.
	 */
	private final boolean bUpdateSyncDelegate; // flag to know if this is the main client. Otherwise it could be a combinedSyncClient

	/**
	 * Package used as the key when registering this client and related
	 * remote interfaces with {@code OASyncDelegate} and when creating the
	 * {@link OADataSourceClient}.
	 */
	private final Package packagex;

	/**
	 * Lazily created data source client that provides remote database access
	 * for the model associated with this sync client.
	 */
	private OADataSourceClient dataSourceClient;

	/**
	 * Counter that tracks the number of {@link #getDetail(OAObject, String)}
	 * calls made by this client, and is used to include a per-call id in
	 * diagnostic logging.
	 */
    private final AtomicInteger aiCntGetDetail = new AtomicInteger();
    
    /**
     * Map of object GUIDs that should be temporarily ignored when calculating
     * sibling sets for detail loading, allowing {@code OASiblingHelperDelegate}
     * to avoid re-requesting the same siblings.
     */
    private final ConcurrentHashMap<Long, Boolean> hmIgnoreSibling = new ConcurrentHashMap<Long, Boolean>();

    /**
     * Map of object GUIDs that should be temporarily ignored when calculating
     * sibling sets for detail loading, allowing {@code OASiblingHelperDelegate}
     * to avoid re-requesting the same siblings.
     */
    private static final ConcurrentHashMap<Long, Long> hmNewObjectsNotYetSent = new ConcurrentHashMap<Long, Long>(31, .75f);

    /**
     * Global map of object GUIDs for objects that currently do not belong to
     * any hub with a master object, indicating that they may be eligible to
     * be garbage collected on the server.
     */
    private static final ConcurrentHashMap<Long, Long> hmObjectsWithoutHubs = new ConcurrentHashMap<Long, Long>(31, .75f);

    /**
     * Queue of objects whose hub-membership status has changed and that need
     * to be reported to the server by the background
     * {@link #threadObjectsWithoutHubs} worker.
     */
    private volatile LinkedBlockingQueue<OAObject> queObjectsWithoutHubs;
    
    /**
     * Background worker thread that consumes {@link #queObjectsWithoutHubs}
     * and notifies the remote session when objects transition between having
     * and not having hubs with master objects.
     */
    private Thread threadObjectsWithoutHubs;
    
	
    /**
     * Creates a new {@code OASyncClient} using the default package
     * ({@code Object.class.getPackage()}) and the specified server host
     * and port.
     *
     * @param serverHostName the host name of the sync server
     * @param serverHostPort the port number of the sync server
     */
	public OASyncClient(String serverHostName, int serverHostPort) {
		this(null, serverHostName, serverHostPort);
	}

	/**
	 * Creates a new {@code OASyncClient} for the specified package and
	 * server connection settings. The client will register itself with
	 * {@code OASyncDelegate}.
	 *
	 * @param packagex the package used as the registration key
	 * @param serverHostName the host name of the sync server
	 * @param serverHostPort the port number of the sync server
	 */
	public OASyncClient(Package packagex, String serverHostName, int serverHostPort) {
		this(packagex, serverHostName, serverHostPort, true);
	}

	/**
	 * Creates a new {@code OASyncClient} with complete control over
	 * whether it should register with {@code OASyncDelegate}.
	 * If {@code packagex} is {@code null}, it defaults to
	 * {@code Object.class.getPackage()}.
	 *
	 * @param packagex the package used for registration
	 * @param serverHostName the server host name
	 * @param serverHostPort the server port
	 * @param bUpdateSyncDelegate whether to register with {@code OASyncDelegate}
	 */
	protected OASyncClient(Package packagex, String serverHostName, int serverHostPort, boolean bUpdateSyncDelegate) {
		if (packagex == null) {
			packagex = Object.class.getPackage();
		}
		this.packagex = packagex;
		this.serverHostName = serverHostName;
		this.serverHostPort = serverHostPort;
		this.bUpdateSyncDelegate = bUpdateSyncDelegate;
		//was: if (bUpdateSyncDelegate) OASyncDelegate.setSyncClient(packagex, this);
	}

	
	/**
	 * Starts the sync client by:
	 * <ul>
	 *   <li>initializing client info,</li>
	 *   <li>starting the multiplexer connection,</li>
	 *   <li>retrieving remote server, session, client, and sync interfaces,</li>
	 *   <li>registering itself with {@code OASyncDelegate} if applicable,</li>
	 *   <li>starting distributed-GC and object-without-hubs background threads,</li>
	 *   <li>creating the {@link OADataSourceClient} if needed.</li>
	 * </ul>
	 *
	 * @throws Exception if any underlying startup operation fails
	 */
    public void start() throws Exception {
        LOG.config("starting");

        getClientInfo();
        getMultiplexerClient().setKeepAlive(115);

        LOG.fine("starting multiplexer client");
        getMultiplexerClient().start(); // this will connect to server using multiplexer

        LOG.fine("multiplexer client connected, connectionId=" + getMultiplexerClient().getConnectionId());

        clientInfo.setConnectionId(getMultiplexerClient().getConnectionId());

        if (bUpdateSyncDelegate) {
            OASyncDelegate.setSyncClient(packagex, this);
        }

        LOG.fine("getting remote objects for OASyncClient");
        getRemoteServer();
        getRemoteSession();
        getRemoteClient();
        getRemoteSync();
        if (bUpdateSyncDelegate) {
            startDistributedGCThread();
            startObjectsWithoutHubsThread();;
            
            LOG.fine("creating OADataSourceClient for remote database access");
            getOADataSourceClient();
        }
        clientInfo.setStarted(true);
        LOG.config("startup completed successful");
    }
	
	
    /**
     * Starts a background daemon thread that periodically updates memory
     * statistics in {@link ClientInfo} and calls
     * {@link RemoteSessionInterface#update(ClientInfo)} unless debug mode
     * is enabled.
     *
     * @param seconds the delay between updates
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
						if (!OAObject.getDebugMode()) {
							getRemoteSession().update(clientInfo);
						}
						Thread.sleep(seconds * 1000L);
					} catch (Exception e) {
						break;
					}
				}
			}
		}, "OASyncClient.update." + seconds);
		t.setDaemon(true);
		t.start();
	}


	/**
	 * Retrieves a detail or link value for a given master object and property
	 * name by invoking the server's {@code getDetailNow} method. Handles:
	 * <ul>
	 *   <li>sibling calculation,</li>
	 *   <li>loading additional master properties,</li>
	 *   <li>receiving serialized objects, hubs, and extra data,</li>
	 *   <li>updating local object/hub references,</li>
	 *   <li>logging performance metrics.</li>
	 * </ul>
	 *
	 * @param masterObject the object whose detail is requested
	 * @param propertyName the name of the desired property
	 * @return the detail value returned by the server, or {@code null}
	 */
	public Object getDetail(final OAObject masterObject, final String propertyName) {
		LOG.fine("masterObject=" + masterObject + ", propertyName=" + propertyName);
		long ts = System.currentTimeMillis();

		final int cntx = aiCntGetDetail.incrementAndGet();

		// LOG.fine("masterObject="+masterObject+", propertyName="+propertyName);
		if (masterObject == null || propertyName == null) {
			return null;
		}

		boolean bGetSibs = false;
		OAObjectKey[] siblingKeys = null;
		String[] additionalMasterProperties = null;
		Object result = null;

		int cntNew = 0;
		int cntDup = 0;

		OALinkInfo li = null;
		try {
			// both Hub && pp are set by HubMerger, HubGroupBy
			final boolean bHasSiblingHelper = OAThreadLocalDelegate.hasSiblingHelpers();

			if (OARemoteThreadDelegate.isRemoteThread()) {
				// use annotated version that does not use the msg queue
				cntDup = OAObjectSerializeDelegate.cntDup;
				cntNew = OAObjectSerializeDelegate.cntNew;
				result = getRemoteClient().getDetailNow(cntx, masterObject.getClass(), masterObject.getObjectKey(), propertyName,
														additionalMasterProperties, siblingKeys, bHasSiblingHelper);
			} else {
				// this will "ask" for additional data "around" the requested property
				bGetSibs = true;
				// send siblings to return back with same prop
				li = OAObjectInfoDelegate.getLinkInfo(masterObject.getClass(), propertyName);

				int max;
				if (li == null) {
					max = 20;
				} else if (li.getType() == OALinkInfo.TYPE_MANY) {
					if (li.getCouldBeLarge()) {
						max = 5;
					} else {
						max = 50;
					}
				} else {
					max = 100;
				}

				if (bHasSiblingHelper) {
					max *= 3;
				}
				siblingKeys = OASiblingHelperDelegate.getSiblings(masterObject, propertyName, max, hmIgnoreSibling);

				/* testing
				if (siblingKeys == null || siblingKeys.length == 0) {
				    siblingKeys = OASiblingHelperDelegate.getSiblings(masterObject, propertyName, max, hmIgnoreSibling);
				    int xx = 4;
				    xx++;
				}
				*/

				additionalMasterProperties = OAObjectReflectDelegate.getUnloadedReferences(masterObject, false, propertyName, false);

				try {
					cntDup = OAObjectSerializeDelegate.cntDup;
					cntNew = OAObjectSerializeDelegate.cntNew;
					result = getRemoteClient().getDetailNow(cntx, masterObject.getClass(), masterObject.getObjectKey(), propertyName,
															additionalMasterProperties, siblingKeys, bHasSiblingHelper);
				} finally {
					for (OAObjectKey ok : siblingKeys) {
						hmIgnoreSibling.remove(ok.getGuid());
					}
				}
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "getDetail error", e);
		}

		int cntSib = 0;

		Object resultHold = result;

		if (result instanceof OAObjectSerializer) {
			// see ClientGetDetail.getSerializedDetail(..)
			OAObjectSerializer os = (OAObjectSerializer) result;

			cntDup = os.dupCount;
			cntNew = os.newCount;

			result = os.getObject();

			// the custom serializer can send extra objects, and might use objKey instead of the object.
			Object objx = os.getExtraObject();

			if (objx instanceof HashMap) {
				HashMap<OAObjectKey, Object> hmExtraData = (HashMap<OAObjectKey, Object>) objx;
				cntSib = hmExtraData.size();
				for (Entry<OAObjectKey, Object> entry : hmExtraData.entrySet()) {
					Object value = entry.getValue();
					if (value == masterObject) {
						if (cntSib > 0) {
							cntSib--;
						}
						continue;
					}

					OAObject obj = OAObjectCacheDelegate.getObject(masterObject.getClass(), entry.getKey());
					if (obj == null) {
						continue;
					}

					if (value instanceof Hub) {
						Hub hub = (Hub) value;
						if (li == null) {
							li = OAObjectInfoDelegate.getLinkInfo(masterObject.getClass(), propertyName);
						}
						if (li != null) {
							if (OAObjectInfoDelegate.cacheHub(li, hub)) {
								value = new WeakReference(hub);
							}
						}
					}
					OAObjectPropertyDelegate.setProperty(obj, propertyName, value); // this will also set the hub.masterObj+li
				}
			}
		} else {
			cntDup = OAObjectSerializeDelegate.cntDup - cntDup;
			cntNew = OAObjectSerializeDelegate.cntNew - cntNew;
		}

		if (result instanceof Hub) {
			Hub hub = (Hub) result;
			if (li == null) {
				li = OAObjectInfoDelegate.getLinkInfo(masterObject.getClass(), propertyName);
			}
			if (OAObjectInfoDelegate.cacheHub(li, hub)) {
				OAObjectPropertyDelegate.setProperty(masterObject, propertyName, new WeakReference(hub));
			} else {
				OAObjectPropertyDelegate.setProperty(masterObject, propertyName, hub); // this will also set the hub.masterObj+li
			}
		}

		if (true || OAObjectSerializeDelegate.cntNew - cntNew > 25 || cntx % 100 == 0) {

			ts = System.currentTimeMillis() - ts;
			String s = "";
			if (ts > 750) {
				if (cntNew > 1000) {
					if (ts > 2000) {
						s = " ALERT";
					}
				} else {
					s = " ALERT";
				}
			}
			s = String.format(
								"client=%d, id=%,d, Obj=%s, prop=%s, ref=%s, getSib=%,d/%,d, moreProps=%d, " +
										"newCnt=%,d, dupCnt=%,d, totNewCnt=%,d, totDupCnt=%,d, ms=%,d%s",
								getConnectionId(),
								cntx,
								masterObject.getClass().getSimpleName() + "." + masterObject.getProperty("id"),
								propertyName,
								result == null ? "null" : result.getClass().getSimpleName(),
								cntSib,
								(siblingKeys == null) ? 0 : siblingKeys.length,
								additionalMasterProperties == null ? 0 : additionalMasterProperties.length,
								cntNew,
								cntDup,
								OAObjectSerializeDelegate.cntNew,
								OAObjectSerializeDelegate.cntDup,
								ts,
								s);
			OAPerformance.LOG.fine(s);
			LOG.fine(s);
			if (OAObject.getDebugMode()) {
				System.out.println("OASyncClient.getDetail: " + s);
			}
		}
		return result;
	}

	/**
	 * Lazily looks up and returns the remote server interface using the
	 * remote multiplexer client. Registers the interface with
	 * {@code OASyncDelegate} if enabled.
	 *
	 * @return the remote server interface
	 * @throws Exception if lookup fails
	 */
	public RemoteServerInterface getRemoteServer() throws Exception {
		if (remoteServerInterface == null) {
			remoteServerInterface = (RemoteServerInterface) getRemoteMultiplexerClient().lookup(ServerLookupName);
			if (bUpdateSyncDelegate) {
				OASyncDelegate.setRemoteServer(packagex, remoteServerInterface);
			}
		}
		return remoteServerInterface;
	}


	/**
	 * Lazily retrieves the remote broadcast sync interface using the remote
	 * multiplexer client. Registers it with {@code OASyncDelegate} if enabled.
	 *
	 * @return the remote sync interface
	 * @throws Exception if lookup fails
	 */
	public RemoteSyncInterface getRemoteSync() throws Exception {
		if (remoteSyncInterface == null) {
			remoteSyncInterface = (RemoteSyncInterface) getRemoteMultiplexerClient().lookupBroadcast(SyncLookupName, getRemoteSyncImpl());
			if (bUpdateSyncDelegate) {
				OASyncDelegate.setRemoteSync(packagex, remoteSyncInterface);
			}
		}
		return remoteSyncInterface;
	}

	/**
	 * Returns the local callback implementation used by the server to send
	 * sync messages to this client. Lazily instantiates the implementation.
	 *
	 * @return the local sync callback implementation
	 * @throws Exception never thrown in this implementation
	 */
    public RemoteSyncInterface getRemoteSyncImpl() throws Exception {
        if (remoteSyncImpl == null) {
            remoteSyncImpl = new RemoteSyncImpl();
        }
        return remoteSyncImpl;
    }
	
    /**
     * Obtains the remote session interface for this client by invoking the
     * server’s {@code getRemoteSession} method. Registers the session with
     * {@code OASyncDelegate} if enabled.
     *
     * @return the remote session interface
     * @throws Exception if remote lookup fails
     */
	public RemoteSessionInterface getRemoteSession() throws Exception {
		if (remoteSessionInterface == null) {
			remoteSessionInterface = getRemoteServer().getRemoteSession(getClientInfo(), getRemoteClientCallback());
			if (bUpdateSyncDelegate) {
				OASyncDelegate.setRemoteSession(packagex, remoteSessionInterface);
			}
		}
		return remoteSessionInterface;
	}

	private RemoteClientCallbackInterface remoteCallback;

	/**
	 * Returns the callback implementation that the server invokes to notify
	 * this client about stop requests, pings, or thread dumps. Lazily creates
	 * the callback instance.
	 *
	 * @return the remote client callback implementation
	 */
	public RemoteClientCallbackInterface getRemoteClientCallback() {
		if (remoteCallback == null) {
			remoteCallback = new RemoteClientCallbackInterface() {
				@Override
				public void stop(String title, String msg) {
					OASyncClient.this.onStopCalled(title, msg);
				}

				@Override
				public String ping(String msg) {
					return "client recvd " + msg;
				}

				@Override
				public String performThreadDump(String msg) {
					String s = OAThreadLocalDelegate.getAllStackTraces();
					LOG.warning(msg + "\n" + s);
					return s;
				}
			};
		}
		return remoteCallback;
	}

	/**
	 * Looks up and returns the remote client interface associated with this
	 * client’s {@link ClientInfo}. Registers the interface with
	 * {@code OASyncDelegate} if enabled.
	 *
	 * @return the remote client interface
	 * @throws Exception if lookup fails
	 */
	public RemoteClientInterface getRemoteClient() throws Exception {
		if (remoteClientSyncInterface == null) {
			remoteClientSyncInterface = getRemoteServer().getRemoteClient(getClientInfo());
			if (bUpdateSyncDelegate) {
				OASyncDelegate.setRemoteClient(packagex, remoteClientSyncInterface);
			}
		}
		return remoteClientSyncInterface;
	}

	/**
	 * Performs a remote lookup for a single remote object reference using
	 * the underlying remote multiplexer client.
	 *
	 * @param lookupName the lookup identifier
	 * @return the remote object reference
	 * @throws Exception if remote lookup fails
	 */
	public Object lookup(String lookupName) throws Exception {
		return getRemoteMultiplexerClient().lookup(lookupName);
	}

	/**
	 * Performs a remote broadcast lookup, returning a proxy that will dispatch
	 * calls to all remote receivers. A callback object may be supplied.
	 *
	 * @param lookupName the broadcast lookup identifier
	 * @param callback callback object to receive responses
	 * @return the broadcast-capable remote reference
	 * @throws Exception if lookup fails
	 */
	public Object lookupBroadcast(String lookupName, Object callback) throws Exception {
		return getRemoteMultiplexerClient().lookupBroadcast(lookupName, callback);
	}

	/**
	 * Lazily initializes and returns the client’s {@link ClientInfo}, filling
	 * in creation time, host name, IP address, and server connection settings.
	 *
	 * @return the client info object
	 */
	public ClientInfo getClientInfo() {
		if (clientInfo == null) {
			clientInfo = new ClientInfo();
			clientInfo.setCreated(new OADateTime());
			clientInfo.setServerHostName(this.serverHostName);
			clientInfo.setServerHostPort(this.serverHostPort);

			try {
				InetAddress localHost = InetAddress.getLocalHost();
				clientInfo.setHostName(localHost.getHostName());
				clientInfo.setIpAddress(localHost.getHostAddress());
			} catch (Exception e) {
			}

		}
		return clientInfo;
	}


	/**
	 * Lazily creates and returns the {@link OADataSourceClient} associated with
	 * this client’s package and used for remote database operations.
	 *
	 * @return the data source client
	 */
	public OADataSourceClient getOADataSourceClient() {
		if (dataSourceClient == null) {
			dataSourceClient = new OADataSourceClient(packagex);
		}
		return dataSourceClient;
	}

	/**
	 * Indicates whether this client has completed its startup process.
	 *
	 * @return {@code true} if the client is started, otherwise {@code false}
	 */
	public boolean isStarted() {
		return getClientInfo().isStarted();
	}

	/**
	 * Stops the client by delegating to {@link #stop(boolean)} with
	 * {@code true}, closing the multiplexer connection if active and
	 * clearing registered delegates.
	 *
	 * @throws Exception if shutdown operations fail
	 */
	public void stop() throws Exception {
		stop(true);
	}

	/**
	 * Stops the sync client by clearing state, optionally closing the
	 * multiplexer connection, and unregistering interfaces from
	 * {@code OASyncDelegate}. Closes the associated data source if present.
	 *
	 * @param bCallClose whether to close the multiplexer connection
	 * @throws Exception if shutdown operations fail
	 */
	public void stop(boolean bCallClose) throws Exception {
		if (!isStarted()) {
			return;
		}
		LOG.fine("Client stop");
		getClientInfo().setStarted(false);
		if (bCallClose && isConnected()) {
			getMultiplexerClient().close();
		}
		multiplexerClient = null;
		remoteMultiplexerClient = null;

		if (bUpdateSyncDelegate) {
			OASyncDelegate.setSyncClient(packagex, null);
			OASyncDelegate.setRemoteServer(packagex, null);
			OASyncDelegate.setRemoteSync(packagex, null);
			OASyncDelegate.setRemoteSession(packagex, null);
			OASyncDelegate.setRemoteClient(packagex, null);
			OADataSource ds = getOADataSourceClient();
			if (ds != null) {
				ds.close();
			}
		}
	}

	/**
	 * Callback invoked by the server to request that this client stop.
	 * Sends an exception notification back to the server and then shuts
	 * down the client.
	 *
	 * @param title brief stop reason
	 * @param msg detailed stop message
	 */
	public void onStopCalled(String title, String msg) {
		LOG.warning("stopped called by server, title=" + title + ", msg=" + msg);
		try {
			getRemoteSession().sendException(title + ", " + msg, new Exception("onStopCalled on client"));
			stop();
		} catch (Exception e) {
		}
	}

	/**
	 * Determines whether the underlying multiplexer client is active and
	 * the socket connection to the server is established.
	 *
	 * @return {@code true} if connected, otherwise {@code false}
	 */
	public boolean isConnected() {
		if (multiplexerClient == null) {
			return false;
		}
		if (!multiplexerClient.isConnected()) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the port number used by the active multiplexer connection.
	 *
	 * @return the port number, or {@code -1} if not connected
	 */
	public int getPort() {
		if (!isConnected()) {
			return -1;
		}
		return getRemoteMultiplexerClient().getMultiplexerClient().getPort();
	}

	/**
	 * Returns the host name associated with the active multiplexer connection.
	 *
	 * @return the host name, or {@code null} if not connected
	 */
	public String getHost() {
		if (!isConnected()) {
			return null;
		}
		return getRemoteMultiplexerClient().getMultiplexerClient().getHost();
	}

	/**
	 * Lazily creates and returns the underlying {@link OAMultiplexerClient}.
	 * The returned client overrides {@code onSocketException} and
	 * {@code onClose} to route socket events back to this instance.
	 *
	 * @return the multiplexer client used for socket communication
	 */
	protected OAMultiplexerClient getMultiplexerClient() {
		if (multiplexerClient != null) {
			return multiplexerClient;
		}
		multiplexerClient = new OAMultiplexerClient(getClientInfo().getServerHostName(), clientInfo.getServerHostPort()) {
			@Override
			protected void onSocketException(Exception e) {
				OASyncClient.this.onSocketException(e);
			}

			@Override
			protected void onClose(boolean bError) {
				OASyncClient.this.onSocketClose(bError);
			}
		};
		return multiplexerClient;
	}

	/**
	 * Handles exceptions originating from the underlying socket connection by
	 * logging the error and stopping the client without closing the socket.
	 *
	 * @param e the exception that occurred
	 */
	protected void onSocketException(Exception e) {
		try {
			LOG.log(Level.WARNING, "exception with connection to server", e);
		} catch (Exception ex) {
		}
		try {
			stop(false);
		} catch (Exception ex) {
		}
	}

	/**
	 * Handles remote socket close events, logging the situation and stopping
	 * the client. If {@code bError} is {@code false}, the connection is
	 * considered cleanly closed.
	 *
	 * @param bError whether the close resulted from an error
	 */
	protected void onSocketClose(boolean bError) {
		try {
			LOG.fine("closing, isError=" + bError);
		} catch (Exception ex) {
		}
		try {
			stop(!bError);
		} catch (Exception ex) {
		}
	}

	/**
	 * Timestamp used to throttle warning messages when excessive numbers of
	 * remote threads are created on the client side.
	 */
	private long msLastThreadCountWarning;

	/**
	 * Lazily creates and returns the {@link OARemoteMultiplexerClient} wrapper
	 * around the underlying multiplexer client. Overrides callbacks to update
	 * remote thread counts and to invoke {@link #afterInvokeRemoteMethod}.
	 *
	 * @return the remote multiplexer client used for remote invocation
	 */
	public OARemoteMultiplexerClient getRemoteMultiplexerClient() {
		if (remoteMultiplexerClient != null) {
			return remoteMultiplexerClient;
		}
		remoteMultiplexerClient = new OARemoteMultiplexerClient(getMultiplexerClient()) {
			@Override
			protected void onRemoteThreadCreated(int totalCount, int liveCount) {
				getClientInfo().setRemoteThreadCount(liveCount);
				if (liveCount > 80) {
					long msNow = System.currentTimeMillis();
					if (msLastThreadCountWarning + 2500 < msNow) {
						msLastThreadCountWarning = msNow;
						String s = OALogUtil.getAllThreadDump();
						LOG.warning("RemoteThread liveCount=" + liveCount + ", totalCreated=" + totalCount + "\n" + s);
					}
				}
			}

			@Override
			protected void afterInvokeForCtoS(RequestInfo ri) {
				OASyncClient.this.afterInvokeRemoteMethod(ri);
			}

			@Override
			public void afterInvokForStoC(RequestInfo ri) {
				OASyncClient.this.afterInvokeRemoteMethod(ri);
			}

		};
		return remoteMultiplexerClient;
	}

	/**
	 * Returns the unique connection ID assigned by the multiplexer client.
	 *
	 * @return the connection identifier
	 */
	public int getConnectionId() {
		return getMultiplexerClient().getConnectionId();
	}

	/**
	 * Post-invocation handler for remote method calls. Ignores internal
	 * OASync-related messages unless an exception is present, in which
	 * case the request is logged.
	 *
	 * @param ri metadata describing the remote invocation
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
		logRequest(ri);
	}

	/**
	 * Logs the supplied remote invocation request information at the FINE
	 * logging level.
	 *
	 * @param ri the request info to log
	 */
	protected void logRequest(RequestInfo ri) {
		/* debug
		if (ri.exception != null || ri.exceptionMessage != null) {
		    if (ri.exception != null) {
		        ri.exception.printStackTrace();
		    }
		}
		*/
		LOG.fine(ri.toLogString());
	}

    
	/**
	 * Notifies the client that a new object has been created. Adds its GUID
	 * to {@code hmNewObjectsNotYetSent} and informs the remote session via
	 * {@link RemoteSessionInterface#objectCreated(long)}.
	 *
	 * @param obj the newly created object
	 */
    public void objectCreated(OAObject obj) {
        if (obj == null) return;
        long guid = obj.getGuid();
        if (guid < 0) return;
        
        hmNewObjectsNotYetSent.put(guid, guid);
        try {
            RemoteSessionInterface rs = getRemoteSession();
            rs.objectCreated(guid);
        }
        catch (Exception e) {
        }
    }

    /**
     * Called by the object serializer after the object has been sent to the
     * server. Removes the object's GUID from {@code hmNewObjectsNotYetSent}.
     *
     * @param obj the object that was transmitted to the server
     */
    public void objectSentToServer(OAObject obj) {
        // called by OAObjectSerializer
        if (obj == null) return;
        long guid = obj.getGuid();
        if (guid < 0) return;
        hmNewObjectsNotYetSent.remove(guid);
    }
    
    /**
     * Determines whether the given object has already been sent to the server
     * by checking for its GUID in {@code hmNewObjectsNotYetSent}.
     *
     * @param obj the object to check
     * @return {@code true} if the object has been sent, otherwise {@code false}
     */
    public boolean isObjectOnServer(OAObject obj) {
        if (obj == null) return false;
        long guid = obj.getGuid();
        if (guid < 0) return false;
        return hmNewObjectsNotYetSent.get(guid) == null;
    }

    
    /**
     * Called when an object has been finalized. Adds its GUID to the
     * {@link #queObjectsFinalized} queue so the distributed GC thread can
     * notify the server.
     *
     * @param guid the GUID of the finalized object
     */
	public void objectFinalized(long guid) {
	    if (guid < 0) return;
		try {
			if (bUpdateSyncDelegate) {
				if (queObjectsFinalized != null) {
				    queObjectsFinalized.add(guid);
				}
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Queue holding GUIDs of finalized objects awaiting processing by the
	 * distributed GC background thread.
	 */
	private volatile LinkedBlockingQueue<Long> queObjectsFinalized;

	/**
	 * Background thread responsible for sending batches of finalized GUIDs
	 * to the remote session for distributed garbage collection.
	 */
	private Thread threadDistributedGC;

	/**
	 * Creates and starts the distributed GC background thread if it has not
	 * already been started. The thread consumes GUIDs from
	 * {@link #queObjectsFinalized} and periodically sends them to
	 * {@link RemoteSessionInterface#objectsFinalized(long[])}.
	 */
	private void startDistributedGCThread() {
		if (queObjectsFinalized != null) {
			return;
		}
		queObjectsFinalized = new LinkedBlockingQueue<Long>();
		threadDistributedGC = new Thread(new Runnable() {
			long msLastError;
			int cntError;
			long[] guids = new long[150];

			@Override
			public void run() {
				RemoteSessionInterface rsi = null;
				for (int guidPos = 0;;) {
					try {
						long guid = queObjectsFinalized.take();
						guids[guidPos++ % 150] = guid;
						if (guidPos % 150 == 0) {
							if (rsi == null) {
								rsi = OASyncClient.this.getRemoteSession();
							}
							if (rsi != null) {
								rsi.objectsFinalized(guids);
							}
						}
					} catch (Exception e) {
						LOG.log(Level.WARNING, "Error in removeGuid thread", e);
						long ms = System.currentTimeMillis();
						if (++cntError > 5) {
							if (ms - 2000 < msLastError) {
								LOG.warning("too many errors, will stop this GuidRemove thread (not critical)");
								queObjectsFinalized = null;
								break;
							} else {
								cntError = 0;
							}
						}
						msLastError = ms;
					}
				}
			}
		}, "OASyncClient.DistributedGC");
		threadDistributedGC.setPriority(Thread.MIN_PRIORITY);
		threadDistributedGC.setDaemon(true);
		threadDistributedGC.start();
	}

	
	/**
	 * Updates tracking information for whether an object is currently in any
	 * hub with a master. If an object enters or leaves such a hub, it is added
	 * to {@link #queObjectsWithoutHubs} for processing by the related thread.
	 *
	 * @param obj the object whose hub membership status changed
	 */
	public void updateObjectsWithoutHubs(OAObject obj) {
        final long guid = obj.getGuid();
        if (guid < 0) return;
	    
        final boolean b = OAObjectHubDelegate.isInHubWithMaster(obj);
        if (b) {
            if (hmObjectsWithoutHubs.get(guid) == null) return;
            hmObjectsWithoutHubs.remove(guid);
        }
        else {
            if (hmObjectsWithoutHubs.get(guid) != null) return;
            hmObjectsWithoutHubs.put(guid, guid);
        }
	    
		try {
			if (obj != null && bUpdateSyncDelegate) {
				LinkedBlockingQueue<OAObject> q = queObjectsWithoutHubs;
				if (q != null) {
					q.add(obj);
				}
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Creates and starts the background thread that monitors objects without
	 * hubs. The thread sends updates to the remote session indicating whether
	 * each object is currently in a hub with a master object.
	 */
	private void startObjectsWithoutHubsThread() {
		if (queObjectsWithoutHubs != null) {
			return;
		}
		queObjectsWithoutHubs = new LinkedBlockingQueue<>();
		threadObjectsWithoutHubs = new Thread(new Runnable() {
			long msLastError;
			int cntError;

			@Override
			public void run() {
				RemoteSessionInterface rsi = null;
				for (;;) {
					try {
						OAObject obj = queObjectsWithoutHubs.take();
						if (rsi == null) {
							rsi = OASyncClient.this.getRemoteSession();
						}
						if (rsi != null) {
						    boolean b = OAObjectHubDelegate.isInHubWithMaster(obj);
						    rsi.updateObjectsWithoutHubs(obj.getClass(), obj.getObjectKey(), b);
						}
					} catch (Exception e) {
						LOG.log(Level.WARNING, "Error in ObjectsWithoutHubs thread", e);
						long ms = System.currentTimeMillis();
						if (++cntError > 5) {
							if (ms - 2000 < msLastError) {
								LOG.warning("too many errors, will stop this GuidRemove thread (not critical)");
								break;
							} else {
								cntError = 0;
							}
						}
						msLastError = ms;
					}
				}
			}
		}, "OASyncClient.ObjectsWithoutHubs");
		threadObjectsWithoutHubs.setPriority(Thread.MIN_PRIORITY);
		threadObjectsWithoutHubs.setDaemon(true);
		threadObjectsWithoutHubs.start();
	}

	/**
	 * Uploads a file to the server using a {@link ClientFile} helper.
	 *
	 * @param toFileName the destination file name on the server
	 * @param file the local file to upload
	 * @return {@code true} if the upload succeeds, otherwise {@code false}
	 * @throws Exception if file transfer fails
	 */
	public boolean uploadFile(String toFileName, File file) throws Exception {
		ClientFile cf = new ClientFile();
		boolean b = cf.upload(toFileName, file);
		return b;
	}

	/**
	 * Downloads a file from the server using a {@link ClientFile} helper.
	 *
	 * @param fromFileName the name of the file on the server
	 * @param file the local destination file
	 * @return {@code true} if the download succeeds, otherwise {@code false}
	 * @throws Exception if file transfer fails
	 */
	public boolean downloadFile(String fromFileName, File file) throws Exception {
		ClientFile cf = new ClientFile();
		boolean b = cf.download(fromFileName, file);
		return b;
	}

}
