package com.viaoa.repl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.concurrent.OAThread;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.repl.remote.RemoteClientInterface;
import com.viaoa.repl.remote.RemoteMasterInterface;
import com.viaoa.repl.remote.RemoteMasterRegisterInterface;
import com.viaoa.sync.OASyncServer;

public class OAReplicationMaster extends OAReplicationBase {
    private static Logger LOG = Logger.getLogger(OAReplicationMaster.class.getName());

	public static final String ReplicationMasterLookupName = "oaReplicationMaster";

	private Map<Integer, ReplClientSession> hmClientInfo = new ConcurrentHashMap<Integer, ReplClientSession>();
	
	private final List<List<RequestInfo>> alListRequestInfo = new ArrayList<>();
	private final int RequestInfoListSize = 1000;
	

    public OAReplicationMaster(OASyncServer syncServer) {
    	super(syncServer);
    }

    public void start() {
    	super.start();
    	LOG.fine("starting ReplMaster");
    	
    	// register Remote lookup object for clients to get.
    	final RemoteMasterRegisterInterface remoteMasterRegister = new RemoteMasterRegisterInterface() {
			@Override
			public RemoteMasterInterface registerClient(RemoteClientInterface remoteClient) {
				RequestInfo ri = OAThreadLocalDelegate.getRemoteRequestInfo();
				if (ri == null) throw new RuntimeException("RequestInfo is null");
				
				final ReplClientSession cs = new ReplClientSession(ri.connectionId, remoteClient);
				hmClientInfo.put(ri.connectionId, cs);
				return cs.remoteMaster;
			}
    	};
    	syncServer.getRemoteMultiplexerServer().createLookup(ReplicationMasterLookupName, remoteMasterRegister, RemoteMasterInterface.class);
    	LOG.fine("created remote RemoteMasterRegister, lookup name=" + ReplicationMasterLookupName);
    	
    	// start thread that will repl each client 
        final String threadName = "OAReplicationMaster";
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
            	try {
            		for (; !OAReplicationMaster.this.bStop; ) {
            	    	LOG.fine("thread="+threadName + ", checking to sync Repli Clients with Master");
            			long ms = System.currentTimeMillis();
	            		for (int id : hmClientInfo.keySet()) {
	            			if (OAReplicationMaster.this.bStop) break;
	            			ReplClientSession ci = hmClientInfo.get(id);
	            			
	            			syncServer.getClientInfo();
	            			LOG.fine(threadName + " processing client " +id);
	            			if (ci == null) continue;
	            			try {
	            				ci.process();
	            			}
	                        catch (Exception e) {
	        	                LOG.log(Level.WARNING, threadName + " exception while processing client merge for connection: " + id +", will continue", e);
	                        }
	            		}
            			long diff = System.currentTimeMillis() - ms;
	            		if (diff < 5000) OAThread.sleep(5000 - diff);
            		}
                }
                catch (Exception e) {
	                String s = "thread=" + threadName + ", is stopping, which will stop replicating with clients.";
	                LOG.log(Level.WARNING, s, e);
                }
            }
        });
        t.setName(threadName);
        t.setDaemon(true);
        t.start();
    	LOG.fine("thread started to Replicate Clients with Master, thread name="+threadName);
    }

    
	protected class ReplClientSession {

		final int sessionId;
		final RemoteClientInterface remoteClient;
		
		final LinkedBlockingQueue<ClientMsg> queClientMsg = new LinkedBlockingQueue<>();
		final Map<Integer, Boolean> hmIgnoreRequestInfo = new ConcurrentHashMap<>();

		long lastMasterMsgId;
		long msLastProcessed;
		int lastRequestInfoSize;
		
		ReplClientSession(int sessionId, RemoteClientInterface remoteClient) {
			this.sessionId = sessionId;
			this.remoteClient = remoteClient;
		}
		
		// ReplClient uses this as remote object to send sync messages to ReplMaster
		final RemoteMasterInterface remoteMaster = new RemoteMasterInterface() {
			@Override
			public void processMessage(long posMaster, long posClient, String methodName, Object[] args) {
				LOG.fine("received message from Client.session="+sessionId+", method="+methodName);
				if (methodName == null) {
					if (posMaster != 0) lastMasterMsgId = posMaster;
					return;
				}
				
				ClientMsg msg = new ClientMsg();
				msg.posMaster = posMaster;
				msg.posClient = posClient;
				msg.methodName = methodName;
				msg.args = args;
				try {
					queClientMsg.put(msg);
				}
				catch (Exception ex) {
					LOG.log(Level.WARNING, "", ex);
				}
			}
		}; 
 
		void process() {
			long msNow = System.currentTimeMillis();
			int size = queClientMsg.size();
			int size2 = getRequestInfoSize();
			
			LOG.fine("processing msgs from Client.session="+sessionId+" clientMsg.size="+size+", masterMsg.size="+(size2 - lastRequestInfoSize));
			if (msLastProcessed != 0L && msLastProcessed + 5000 > msNow) {
				if (size < 50 && (size2 - lastRequestInfoSize) < 50) {
					return;
				}
			}
			lastRequestInfoSize = size2;

			// invoke client changes on master.
			for (int i=0; i<250 ;i++) {
				try {
					ClientMsg cm = queClientMsg.poll();
					if (cm == null) break;
					Method method = getMethod(cm.methodName);
					LOG.fine("invoking message from Client.session="+sessionId+", method="+method.getName());
					method.invoke(syncServer.getRemoteSync(), cm.args);

					RequestInfo ri = OAThreadLocalDelegate.getRemoteRequestInfo();
					if (ri == null) throw new RuntimeException("RequestInfo is null after invoking method");
					//qqqqqqqq todo: there could be more than one ri created for this msg
					if (ri != null) hmIgnoreRequestInfo.put(ri.messageId, true);
				}
				catch (Exception ex) {
					LOG.log(Level.WARNING, "exception invoking client message", ex);
				}
			}
			
			// send master server msgs to client
			final int x = alListRequestInfo.size();
			for (int i=0; i < (x-1); i++) {
				List<RequestInfo> al;
				synchronized (alListRequestInfo) {
					al = alListRequestInfo.get(i);
				}
				if (al.get(RequestInfoListSize-1).messageId <= lastMasterMsgId) continue;
				for (RequestInfo ri : al) {
					if (ri.messageId <= lastMasterMsgId) continue;
					if (hmIgnoreRequestInfo.remove(ri.messageId) != null) continue;
					LOG.fine("sending Master message to Client.session="+sessionId+", method="+ri.method.getName());
					remoteClient.processMessage(ri.messageId, ri.method.getName(), ri.args);
					lastMasterMsgId = ri.messageId;
				}
			}

			if (x > 0) {
				List<RequestInfo> al;
				synchronized (alListRequestInfo) {
					al = alListRequestInfo.get(x-1);
				}
				synchronized (al) {
					for (RequestInfo ri : al) {
						if (ri.messageId <= lastMasterMsgId) continue;
						if (hmIgnoreRequestInfo.remove(ri.messageId) != null) continue;
						LOG.fine("sending Master message to Client.session="+sessionId+", method="+ri.method.getName());
						remoteClient.processMessage(ri.messageId, ri.method.getName(), ri.args);
						lastMasterMsgId = ri.messageId;
					}
				}
			}
			msLastProcessed = System.currentTimeMillis();;
		}
	}
	
	protected static class ClientMsg {
		long posMaster;
		long posClient;
		String methodName;
		Object[] args;
	}

	
	private int getRequestInfoSize() {
		int x = alListRequestInfo.size();
		if (x == 0) return 0;
		int tot = 0;
		if (x > 1) {
			tot = RequestInfoListSize * (x-1);
		}
		List<RequestInfo> al = null;
		synchronized (alListRequestInfo) {
			al = alListRequestInfo.get(x - 1);
		}
		synchronized (al) {
			tot += al.size();
		}
		return tot;
	}
	
//qqqqqq needs to be called from OASyncServer qqqqqqqqqqqqqqqq	
	public void disconnect(int clientId) {
		hmClientInfo.remove(clientId);
	}

	@Override
	protected void onNewRequestInfoMessage(long qpos, RequestInfo ri) {
        LOG.fine("new message from Sync que");
		synchronized (alListRequestInfo) {
			List<RequestInfo> al = null;
			int x = alListRequestInfo.size();
			if (x > 0) {
				al = alListRequestInfo.get(x - 1);
				if (al.size() == RequestInfoListSize) al = null;
			}
			if (al == null) {
				al = new ArrayList<>(RequestInfoListSize);
				alListRequestInfo.add(al);
			}
			synchronized (al) {
				al.add(ri);
			}
		}
	}
}
