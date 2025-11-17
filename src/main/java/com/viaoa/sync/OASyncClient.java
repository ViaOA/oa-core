/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

	/** this is used to create a connection (socket) to server. */
	private OAMultiplexerClient multiplexerClient;

	/** Allow for making remote method calls to an object instance on the server. */
	private OARemoteMultiplexerClient remoteMultiplexerClient;

	/** information about this client */
	private ClientInfo clientInfo;

	private RemoteServerInterface remoteServerInterface;
	private RemoteSessionInterface remoteSessionInterface;
	private RemoteClientInterface remoteClientSyncInterface;
	private RemoteSyncInterface remoteSyncInterface;
	private RemoteSyncInterface remoteSyncImpl;
	private String serverHostName;
	private int serverHostPort;
	private final boolean bUpdateSyncDelegate; // flag to know if this is the main client. Otherwise it could be a combinedSyncClient

	private final Package packagex;

	private OADataSourceClient dataSourceClient;

    private final AtomicInteger aiCntGetDetail = new AtomicInteger();
    private final ConcurrentHashMap<Long, Boolean> hmIgnoreSibling = new ConcurrentHashMap<Long, Boolean>();

    private static final ConcurrentHashMap<Long, Long> hmNewObjectsNotYetSent = new ConcurrentHashMap<Long, Long>(31, .75f);

    /**
     * Objects that have been added on the server.session so that it wont be GCd on the server.
     */
    private static final ConcurrentHashMap<Long, Long> hmObjectsWithoutHubs = new ConcurrentHashMap<Long, Long>(31, .75f);
    private volatile LinkedBlockingQueue<OAObject> queObjectsWithoutHubs;
    private Thread threadObjectsWithoutHubs;
    
	
	public OASyncClient(String serverHostName, int serverHostPort) {
		this(null, serverHostName, serverHostPort);
	}

	public OASyncClient(Package packagex, String serverHostName, int serverHostPort) {
		this(packagex, serverHostName, serverHostPort, true);
	}

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
	 * This is sent to the server using ClientGetDetail, by using a customized objectSerializer
	 *
	 * @param masterObject
	 * @param propertyName
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

	public RemoteServerInterface getRemoteServer() throws Exception {
		if (remoteServerInterface == null) {
			remoteServerInterface = (RemoteServerInterface) getRemoteMultiplexerClient().lookup(ServerLookupName);
			if (bUpdateSyncDelegate) {
				OASyncDelegate.setRemoteServer(packagex, remoteServerInterface);
			}
		}
		return remoteServerInterface;
	}


	public RemoteSyncInterface getRemoteSync() throws Exception {
		if (remoteSyncInterface == null) {
			remoteSyncInterface = (RemoteSyncInterface) getRemoteMultiplexerClient().lookupBroadcast(SyncLookupName, getRemoteSyncImpl());
			if (bUpdateSyncDelegate) {
				OASyncDelegate.setRemoteSync(packagex, remoteSyncInterface);
			}
		}
		return remoteSyncInterface;
	}

    // used for oasync callback (messages from other computers)
    public RemoteSyncInterface getRemoteSyncImpl() throws Exception {
        if (remoteSyncImpl == null) {
            remoteSyncImpl = new RemoteSyncImpl();
        }
        return remoteSyncImpl;
    }
	
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

	public RemoteClientInterface getRemoteClient() throws Exception {
		if (remoteClientSyncInterface == null) {
			remoteClientSyncInterface = getRemoteServer().getRemoteClient(getClientInfo());
			if (bUpdateSyncDelegate) {
				OASyncDelegate.setRemoteClient(packagex, remoteClientSyncInterface);
			}
		}
		return remoteClientSyncInterface;
	}

	public Object lookup(String lookupName) throws Exception {
		return getRemoteMultiplexerClient().lookup(lookupName);
	}

	public Object lookupBroadcast(String lookupName, Object callback) throws Exception {
		return getRemoteMultiplexerClient().lookupBroadcast(lookupName, callback);
	}

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


	public OADataSourceClient getOADataSourceClient() {
		if (dataSourceClient == null) {
			dataSourceClient = new OADataSourceClient(packagex);
		}
		return dataSourceClient;
	}

	public boolean isStarted() {
		return getClientInfo().isStarted();
	}

	/** Sets the stop flag */
	public void stop() throws Exception {
		stop(true);
	}

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

	public void onStopCalled(String title, String msg) {
		LOG.warning("stopped called by server, title=" + title + ", msg=" + msg);
		try {
			getRemoteSession().sendException(title + ", " + msg, new Exception("onStopCalled on client"));
			stop();
		} catch (Exception e) {
		}
	}

	/**
	 * checks to see if this client has been connected to the GSMR server.
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

	public int getPort() {
		if (!isConnected()) {
			return -1;
		}
		return getRemoteMultiplexerClient().getMultiplexerClient().getPort();
	}

	public String getHost() {
		if (!isConnected()) {
			return null;
		}
		return getRemoteMultiplexerClient().getMultiplexerClient().getHost();
	}

	/**
	 * the socket connection to GSMR server.
	 *
	 * @see #onSocketException(Exception) for connection errors
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
	 * Called when there is an exception with the real socket.
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

	private long msLastThreadCountWarning;

	/** allows remote method calls to GSMR server. */
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

	public int getConnectionId() {
		return getMultiplexerClient().getConnectionId();
	}

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
     * called when object is created.  See OAObjectDelegate.initialize(..)
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

    public void objectSentToServer(OAObject obj) {
        // called by OAObjectSerializer
        if (obj == null) return;
        long guid = obj.getGuid();
        if (guid < 0) return;
        hmNewObjectsNotYetSent.remove(guid);
    }
    
    public boolean isObjectOnServer(OAObject obj) {
        if (obj == null) return false;
        long guid = obj.getGuid();
        if (guid < 0) return false;
        return hmNewObjectsNotYetSent.get(guid) == null;
    }

    
	
	/**
	 * called when object is finalized.
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

	private volatile LinkedBlockingQueue<Long> queObjectsFinalized;
	private Thread threadDistributedGC;

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
	 * called when object is removed from Hub(s) and could be GC'd on server.
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

	public boolean uploadFile(String toFileName, File file) throws Exception {
		ClientFile cf = new ClientFile();
		boolean b = cf.upload(toFileName, file);
		return b;
	}

	public boolean downloadFile(String fromFileName, File file) throws Exception {
		ClientFile cf = new ClientFile();
		boolean b = cf.download(fromFileName, file);
		return b;
	}

}
