/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.sync;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.comm.multiplexer.OAMultiplexerServer;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAObjectUniqueDelegate;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.remote.multiplexer.OARemoteMultiplexerServer;
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
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;

/**
 * Server used to work with 1+ OASyncClients so that all OAObjects stay in sync. This allows OASyncClients to connect and lookup remote
 * objects and have a server side session.
 * 
 * @author vvia
 * @see OASync
 */
public class OASyncServer {
	private static Logger LOG = Logger.getLogger(OASyncServer.class.getName());

	public static final String ServerLookupName = "syncserver";
	public static final String SyncLookupName = "oasync";
	public static final String SyncQueueName = "oasync";
	public static final int QueueSize = 20000;

	private int port;
	private OAMultiplexerServer multiplexerServer;
	private OARemoteMultiplexerServer remoteMultiplexerServer;

	private RemoteSyncImpl remoteSync;
	private RemoteServerImpl remoteServer;

	/** used to log requests to a log file */
	private ArrayBlockingQueue<RequestInfo> queRemoteRequestLogging;

	private ConcurrentHashMap<Integer, ClientInfoExt> hmClientInfoExt = new ConcurrentHashMap<Integer, ClientInfoExt>();

	// for this server instance
	private ServerInfo serverInfo;
	private ClientInfo clientInfo;
	private RemoteSessionInterface remoteSessionServer;
	private RemoteClientInterface remoteClientForServer;
	private final Package packagex;

	// allow upload/download files with clients
	private ServerFile serverFile;

	public OASyncServer(int port) {
		this(null, port);
	}

	public OASyncServer(Package packagex, int port) {
		if (packagex == null) {
			packagex = Object.class.getPackage();
		}
		this.packagex = packagex;
		this.port = port;
		OASyncDelegate.setSyncServer(packagex, this);
	}

	public RemoteSyncImpl getRemoteSync() {
		if (remoteSync == null) {
			remoteSync = new RemoteSyncImpl();
		}
		return remoteSync;
	}

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
					OAObjectCacheDelegate.refresh(clazz);
				}

				@Override
				public OAObject getUnique(Class<? extends OAObject> clazz, String propertyName, Object uniqueKey, boolean bAutoCreate) {
					OAObject oaObj = OAObjectUniqueDelegate.getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
					return oaObj;
				}
			};
			OASyncDelegate.setRemoteServer(packagex, remoteServer);
			getRemoteSessionForServer();
		}
		return remoteServer;
	}

	public ClientInfo getClientInfo() {
		if (clientInfo == null) {
			clientInfo = new ClientInfo();
			clientInfo.setConnectionId(0);
			clientInfo.setCreated(new OADateTime());
		}
		return clientInfo;
	}

	protected RemoteSessionInterface getRemoteSessionForServer() {
		if (remoteSessionServer == null) {
			remoteSessionServer = getRemoteSession(getClientInfo(), null);
			OASyncDelegate.setRemoteSession(packagex, remoteSessionServer);
		}
		return remoteSessionServer;
	}

	protected RemoteClientInterface getRemoteClientForServer() {
		if (remoteClientForServer == null) {
			remoteClientForServer = getRemoteClient(getClientInfo());
			OASyncDelegate.setRemoteClient(packagex, remoteClientForServer);
		}
		return remoteClientForServer;
	}

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

		rs = new RemoteSessionImpl(ci.getConnectionId()) {
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
					clearCache();
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

			@Override
			public void removeGuids(int[] guids) {
				if (guids == null) {
					return;
				}
				removeFromServerCache(guids);
				if (cx.remoteClient != null) {
					cx.remoteClient.removeGuids(guids); // remove from getDetail cache/tree
				}
			}

		};
		cx.remoteSession = rs;
		return rs;
	}

	/**
	 * updates ClientInfo
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
						Thread.sleep(seconds * 1000);
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
	 * This can be overwritten to capture info about the server and client connections. see #startServerUpdateThread(int) see
	 * OASyncClient#startClientUpdateThread(int)
	 */
	public void onUpdate(ClientInfo ci) {
		int cid = ci.getConnectionId();
		ClientInfoExt cx = hmClientInfoExt.get(cid);
		if (cx != null) {
			cx.ci = ci;
		}
	}

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
		rc = new RemoteClientImpl(ci.getConnectionId()) {
			/**
			 * Add objects that need to be cached to the session. This is used by datasource and copy methods.
			 */
			@Override
			public void setCached(OAObject obj) {
				cx.remoteSession.addToServerCache(obj);
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
	 * Saved serverSide cached objects from clients.
	 */
	public void saveCache(OACascade cascade, int iCascadeRule) {
		for (Map.Entry<Integer, ClientInfoExt> entry : hmClientInfoExt.entrySet()) {
			ClientInfoExt cx = entry.getValue();
			if (cx.remoteSession != null) {
				cx.remoteSession.saveCache(cascade, iCascadeRule);
			}
		}
	}

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

	public void setInvalidConnectionMessage(String msg) {
		getMultiplexerServer().setInvalidConnectionMessage(msg);
	}

	public String getInvalidConnectionMessage(String defaultMsg) {
		return defaultMsg;
	}

	/**
	 * Used to manage multiplexed socket connections from client computers.
	 * 
	 * @return
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

	class ClientInfoExt {
		ClientInfo ci;
		Socket socket;
		RemoteSessionImpl remoteSession;
		RemoteClientImpl remoteClient;
		RemoteClientCallbackInterface remoteClientCallback;
	}

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

	public Socket getSocket(int connectionId) {
		ClientInfoExt cx = hmClientInfoExt.get(connectionId);
		if (cx != null) {
			return cx.socket;
		}
		return null;
	}

	protected void onClientException(ClientInfo ci, String msg, Throwable ex) {
		if (ci != null) {
			msg = String.format(
								"ConnectionId=%d, User=%s, msg=%s",
								ci.getConnectionId(), ci.getUserName(), msg);
		}
		LOG.log(Level.WARNING, msg, ex);
	}

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
			};

			// register remote objects
			remoteMultiplexerServer.createLookup(	ServerLookupName, getRemoteServer(), RemoteServerInterface.class, SyncQueueName,
													QueueSize);

			RemoteSyncInterface rsi = (RemoteSyncInterface) remoteMultiplexerServer
					.createBroadcast(SyncLookupName, getRemoteSync(), RemoteSyncInterface.class, SyncQueueName, QueueSize);
			OASyncDelegate.setRemoteSync(packagex, rsi);

			// have RemoteClient objects use sync queue
			// remoteMultiplexerServer.registerClassWithQueue(RemoteClientInterface.class, SyncQueueName, QueueSize);            
		}
		return remoteMultiplexerServer;
	}

	private AtomicInteger aiSessionCount = new AtomicInteger(); // needs to start at 1 (0 is for the server)

	public int getSessionCount() {
		return aiSessionCount.get();
	}

	protected void onSessionCreated(int connectionId, Socket socket) {
	}

	protected void onSessionRemoved(int connectionId) {
	}

	public void createLookup(String name, Object obj, Class interfaceClass) {
		getRemoteMultiplexerServer().createLookup(name, obj, interfaceClass, null, -1);
	}

	/**
	 * use the same queue that is used by sync remote object.
	 */
	public void createSyncLookup(String name, Object obj, Class interfaceClass) {
		getRemoteMultiplexerServer().createLookup(name, obj, interfaceClass, SyncQueueName, QueueSize);
	}

	public void createLookup(String name, Object obj, Class interfaceClass, String queueName, int queueSize) {
		getRemoteMultiplexerServer().createLookup(name, obj, interfaceClass, queueName, queueSize);
	}

	public Object createBroadcast(final String bindName, Class interfaceClass, String queueName, int queueSize) {
		return getRemoteMultiplexerServer().createBroadcast(bindName, interfaceClass, queueName, queueSize);
	}

	public Object createBroadcast(final String bindName, Object callback, Class interfaceClass, String queueName, int queueSize) {
		return getRemoteMultiplexerServer().createBroadcast(bindName, callback, interfaceClass, queueName, queueSize);
	}

	public Object createSyncBroadcast(final String bindName, Class interfaceClass) {
		return getRemoteMultiplexerServer().createBroadcast(bindName, interfaceClass, SyncQueueName, QueueSize);
	}

	public Object createSyncBroadcast(final String bindName, Object callback, Class interfaceClass) {
		return getRemoteMultiplexerServer().createBroadcast(bindName, callback, interfaceClass, SyncQueueName, QueueSize);
	}

	/* this was removed - args/result that are remote objects will use same queue as parent object    
	public void registerClassWithQueue(Class clazz) {
	    getRemoteMultiplexerServer().registerClassWithQueue(clazz, SyncQueueName, QueueSize);
	}
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

	/** thread used to log all requests */
	private Thread threadStatsLogger;
	/** stream to write request logs */
	private PrintWriter pwRemoteRequestLogger;
	/** time to change to another log file, so each file is by day */
	private long msNextRemoteRequestLogDateChange;

	/**
	 * Thread that will get requests from the queue, and write to request log file.
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

	// loops to log all requests that are added to the queue
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

	protected String getLogFileName() {
		return null;
	}

	public void start() throws Exception {
		startRequestLoggerThread();
		getServerInfo();
		getMultiplexerServer().start();
		getRemoteMultiplexerServer().start();
		getServerFile().start();
		startLoadDataInBackgroundThread();
	}

	public void stop() throws Exception {
		if (multiplexerServer != null) {
			multiplexerServer.stop();
		}
		getServerFile().stop();
	}

	public void performDGC() {
		if (remoteMultiplexerServer != null) {
			getRemoteMultiplexerServer().performDGC();
		}
	}

	public ServerFile getServerFile() {
		if (serverFile == null) {
			serverFile = new ServerFile("shared");
		}
		return serverFile;
	}

	// 20171217
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

	private final ArrayBlockingQueue<LoadSibling> queLoadDataInBackground = new ArrayBlockingQueue<>(250);

	/**
	 * called when props or sibling data cant be loaded for client getDetail request, because of timeout. This can be overwritten to have it
	 * done in a background thread.
	 */
	protected void loadDataInBackground(OAObject obj, String property) {
		queLoadDataInBackground.offer(new LoadSibling(obj, property));
	}

	private Thread threadLoadSibling;

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

				if (OAObjectPropertyDelegate.isPropertyLocked(ls.obj, ls.property)) {
					continue;
				}

				OAObjectReflectDelegate.getProperty(ls.obj, ls.property); // load from DS
			} catch (Exception e) {
				if (msNow > (msLastError + 5000)) {
					LOG.log(Level.WARNING, "Exception in LoadSibling thread", e);
					msLastError = msNow;
				}
			}
		}
	}

}
