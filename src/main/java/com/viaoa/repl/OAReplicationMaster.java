package com.viaoa.repl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAThrottle;

public class OAReplicationMaster extends OAReplicationBase {
    private static Logger LOG = Logger.getLogger(OAReplicationMaster.class.getName());

	public static final String ReplicationMasterLookupName = "oaReplicationMaster";

	private Map<Integer, ReplClientSession> hmClientInfo = new ConcurrentHashMap<Integer, ReplClientSession>();

	
	// very short lived when each client is processing 
	private final Map<RequestInfo, OAReplTLog> hmRequestInfoTLog = new ConcurrentHashMap<>();
	
	private final List<List<OAReplTLog>> alListReplTLog = new ArrayList<>();
	private final int RequestInfoListSize = 1000;
	
	
	// seq # used for master msg #, must be unique.  Used starting value is stored/persisted. 
	private long messageCnt;

    public OAReplicationMaster(OASyncServer syncServer, String tlogFilename) {
    	super(syncServer);
    }

    private ObjectOutputStream objectOutputStream;

    @Override
    public void start() throws Exception {
    	LOG.fine("starting ReplMaster");
    	this._start();
    	super.start();
    }    
    
    protected void _start() throws Exception {
    	super.start(); //qqqqqqqqq dont start until this starts
    	
    	
    	// 1: load previous TLog records ================================== 
    	
    	final String fnameNew = "./runtime/demo/replMaster.bin";
        File file = new File(fnameNew);
        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis, 64 * 1024);
            ObjectInputStream ois = new ObjectInputStream(bis);
            
            for (int i=0; ; i++) {
            	OAReplTLog tlog;
            	try {
                	tlog = (OAReplTLog) ois.readObject();
            	}
            	catch (IOException e) {
            		break;
            	}
        		addTLog(tlog);
            	String s = String.format("%,d) %s", tlog.getMasterSeq(), tlog.getMethodName());
            	System.out.println(s);	                        	
            }
            ois.close();
    	}

        final boolean bAppend = file.exists() && file.length() > 0;
        FileOutputStream fos = new FileOutputStream(file, true); // append
        
        BufferedOutputStream bos = new BufferedOutputStream(fos, 64 * 1024);
        objectOutputStream = new ObjectOutputStream(bos) {
        	@Override
            protected void writeStreamHeader() throws IOException {
                if (bAppend) reset(); // do not write a new header
                else super.writeStreamHeader();
            }
        };
        

    	// 2: open append file for new TLog records ================================== 
        
        
    	// register Remote lookup object for clients to get.
    	final RemoteMasterRegisterInterface remoteMasterRegister = new RemoteMasterRegisterInterface() {
			@Override
			public RemoteMasterInterface registerClient(String guid, RemoteClientInterface remoteClient, long masterSeq, long clientSeq) {
				RequestInfo ri = OAThreadLocalDelegate.getRemoteRequestInfo();
				if (ri == null) throw new RuntimeException("RequestInfo is null");
				
				final ReplClientSession cs = new ReplClientSession(guid, ri.connectionId, remoteClient, masterSeq, clientSeq);
				hmClientInfo.put(ri.connectionId, cs);
				return cs.remoteMaster;
			}
    	};
    	
    	
    	// 3: start thread that replicate with Clients  ================================== 
    	
    	
    	syncServer.getRemoteMultiplexerServer().createLookup(ReplicationMasterLookupName, remoteMasterRegister, RemoteMasterRegisterInterface.class);
    	LOG.fine("created remote RemoteMasterRegister, lookup name=" + ReplicationMasterLookupName);
    	
    	// start thread that will repl each client 
        final String threadName = "OAReplicationMaster";
        Thread t = new Thread(new Runnable() {
        	final OAThrottle throttle = new OAThrottle(500);
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
	                        	if (throttle.check()) {
	                        		LOG.log(Level.WARNING, threadName + " exception while processing client merge for connection: " + id +", will continue", e);
	                        	}
	                        }
	            			hmRequestInfoTLog.clear(); // only needed while processing client
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
    	LOG.fine("thread started to Replicate Clients with this Master, thread name="+threadName);
    }

    
	protected class ReplClientSession {
//qqqqqq test: make sure dropped client session is removed
		final int sessionId;
		final RemoteClientInterface remoteClient;
		
		final LinkedBlockingQueue<ClientMsg> alClientMsg = new LinkedBlockingQueue<>();
		final Map<OAReplTLog, Boolean> hmIgnoreTLog = new ConcurrentHashMap<>();

		String guid;
		long lastMasterSeq;
		long clientSeq;

		long msLastProcessed;
		int lastRequestInfoSize;
		
		ReplClientSession(String guid, int sessionId, RemoteClientInterface remoteClient, long lastMasterSeq, long clientSeq) {
			this.guid = guid;
			this.sessionId = sessionId;
			this.remoteClient = remoteClient;
			this.lastMasterSeq = lastMasterSeq;
			this.clientSeq = clientSeq;
			
//qqqqqqqqqqqq see if Master has a larger clientSeq			
			
		}
		
		// ReplClient uses this as remote object to send sync messages to ReplMaster
		final RemoteMasterInterface remoteMaster = new RemoteMasterInterface() {
			@Override
			public void processMessage(long masterSeq, long clientSeq, String methodName, Object[] args) {
				LOG.fine("received message from Client.session="+sessionId+", method="+methodName);
				if (methodName == null) {//qqqqqqqqq remove this, should not happen
					if (masterSeq != 0) lastMasterSeq = masterSeq;
					return;
				}
				lastMasterSeq = masterSeq;
				
				ClientMsg msg = new ClientMsg();
				msg.posMaster = masterSeq;
				msg.posClient = clientSeq;
				msg.methodName = methodName;
				msg.args = args;
				try {
					alClientMsg.put(msg);
				}
				catch (Exception ex) {
					LOG.log(Level.WARNING, "", ex);
				}
			}

			@Override
			public long getMinimumClientSeq() {
				return clientSeq;
			}
		}; 
 
		void process() {
			long msNow = System.currentTimeMillis();
			int size = alClientMsg.size();
			int size2 = getRequestInfoSize();
			
			LOG.fine("processing msgs from Client.session="+sessionId+" clientMsg.size="+size+", masterMsg.size="+(size2 - lastRequestInfoSize));
			if (msLastProcessed != 0L && msLastProcessed + 5000 > msNow) {
				if (size < 50 && (size2 - lastRequestInfoSize) < 50) {
					return;
				}
			}
			lastRequestInfoSize = size2;

			hmIgnoreTLog.clear();			
			// invoke client changes on master.
			for (int i=0; ; i++) {
				try {
					ClientMsg cm = alClientMsg.poll();
					if (cm == null) break;
//qqqqqqqq need to know if it's local only ??
					
					Method method = getMethod(cm.methodName);
					LOG.fine("invoking message from Client.session="+sessionId+", method="+method.getName());
					method.invoke(syncServer.getRemoteSync(), cm.args);

					RequestInfo ri = OAThreadLocalDelegate.getRemoteRequestInfo();
					OAReplTLog tl = OAReplicationMaster.this.getTlog(ri, true);
//qqqqqqqqqqqqq this is returning ri=null, should be set during method.invoke 					
					hmIgnoreTLog.put(tl, true);
					
				}
				catch (Exception ex) {
					LOG.log(Level.WARNING, "exception invoking client message", ex);
				}
			}
			
			// send master server msgs to client
			final int x = alListReplTLog.size();
			for (int i=0; i < (x-1); i++) {
				List<OAReplTLog> al;
				synchronized (alListReplTLog) {
					al = alListReplTLog.get(i);
				}
				if (al.get(RequestInfoListSize-1).masterSeq <= lastMasterSeq) continue;
				for (OAReplTLog tlog : al) {
					//qqqqqqq this cnt compare wont work because restart resets to 0
					if (tlog.masterSeq <= lastMasterSeq) continue;
					if (hmIgnoreTLog.remove(tlog) != null) continue;
					LOG.fine("sending Master message to Client.session="+sessionId+", method="+tlog.methodName);
					remoteClient.processMessage(tlog.masterSeq, tlog.methodName, tlog.args);
					lastMasterSeq = tlog.masterSeq;
				}
			}

			if (x > 0) {
				List<OAReplTLog> al;
				synchronized (alListReplTLog) {
					al = alListReplTLog.get(x-1);
				}
				synchronized (al) {
					for (OAReplTLog tlog : al) {
						if (tlog.masterSeq <= lastMasterSeq) continue;
						if (hmIgnoreTLog.remove(tlog) != null) continue;
						LOG.fine("sending Master message to Client.session="+sessionId+", method="+tlog.methodName);
						remoteClient.processMessage(tlog.masterSeq, tlog.methodName, tlog.args);
						lastMasterSeq = tlog.masterSeq;
					}
				}
			}
			hmIgnoreTLog.clear();			
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
		int x = alListReplTLog.size();
		if (x == 0) return 0;
		int tot = 0;
		if (x > 1) {
			tot = RequestInfoListSize * (x-1);
		}
		List<OAReplTLog> al = null;
		synchronized (alListReplTLog) {
			al = alListReplTLog.get(x - 1);
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
	protected void onNewSyncMessage(RequestInfo ri) {
		messageCnt++;
        final OAReplTLog tlog = new OAReplTLog(new OADateTime(), messageCnt, 0L, ri.method.getName(), ri.args);
        hmRequestInfoTLog.put(ri, tlog);
		addTLog(tlog);
	}

	
	protected OAReplTLog getTlog(RequestInfo ri, boolean bRemove) {
		if (ri == null) return null;
		if (bRemove) return hmRequestInfoTLog.remove(ri);
		return hmRequestInfoTLog.get(ri);
	}
	
	
	protected void addTLog(final OAReplTLog tlog) {
		try {
	        objectOutputStream.writeObject(tlog);
	        objectOutputStream.flush();                            
	        objectOutputStream.close();
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
        
        LOG.fine("new message from Sync que");
		synchronized (alListReplTLog) {
			List<OAReplTLog> al = null;
			int x = alListReplTLog.size();
			if (x > 0) {
				al = alListReplTLog.get(x - 1);
				if (al.size() == RequestInfoListSize) al = null;
			}
			if (al == null) {
				al = new ArrayList<>(RequestInfoListSize);
				alListReplTLog.add(al);
			}
			synchronized (al) {
				al.add(tlog);
			}
		}
	}
}
