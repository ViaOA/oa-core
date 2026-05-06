package com.viaoa.repl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
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

import com.viaoa.concurrent.OAThrottle;
import com.viaoa.datetime.OADateTime;
import com.viaoa.io.OAFile;
import com.viaoa.lang.OAStr;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.repl.remote.RemoteClientInterface;
import com.viaoa.repl.remote.RemoteMasterInterface;
import com.viaoa.repl.remote.RemoteMasterRegisterInterface;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.runtime.thread.OAThread;
import com.viaoa.sync.OASyncServer;

public class OAReplicationMaster extends OAReplicationBase {
    private static Logger LOG = Logger.getLogger(OAReplicationMaster.class.getName());

	public static final String ReplicationMasterLookupName = "oaReplicationMaster";

	private Map<Integer, ReplClientSession> hmClientSession = new ConcurrentHashMap<Integer, ReplClientSession>();
	
	private final List<List<OAReplTLog>> alListReplTLog = new ArrayList<>();
	private final int RequestInfoListSize = 1000;
	
	protected final String tlogFileName;
    private FileOutputStream fileOutputStream;
    private ObjectOutputStream objectOutputStream;
	
	private long currentMasterSeq;

	private Thread threadRepl;
	
    public OAReplicationMaster(OASyncServer syncServer, String tlogFilename) {
    	super(syncServer);
    	this.tlogFileName = tlogFilename;
    }

    @Override
    public void start() throws Exception {
    	LOG.fine("starting ReplMaster");
    	this._start();
    	super.start();
    }    
    
    protected void _start() throws Exception {
    	loadTLogFile();
        openTLogFile();

    	// register Remote lookup object for clients to get.
    	final RemoteMasterRegisterInterface remoteMasterRegister = new RemoteMasterRegisterInterface() {
			@Override
			public RemoteMasterInterface registerClient(String guid, RemoteClientInterface remoteClient, long lastSentMasterSeq, long lastSentClientSeq) {
				final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
				RequestInfo ri = srvcOAThreadLocal.getRemoteRequestInfo();
				if (ri == null) throw new RuntimeException("RequestInfo is null");
				
				final ReplClientSession cs = new ReplClientSession(guid, ri.connectionId, remoteClient, lastSentMasterSeq, lastSentClientSeq);
				hmClientSession.put(ri.connectionId, cs);
				return cs.remoteMaster;
			}
    	};
    	
    	syncServer.getRemoteMultiplexerServer().createLookup(ReplicationMasterLookupName, remoteMasterRegister, RemoteMasterRegisterInterface.class);
    	LOG.fine("created remote RemoteMasterRegister, lookup name=" + ReplicationMasterLookupName);
    	
    	// start thread that will repl each client 
        final String threadName = "OAReplicationMaster";
        threadRepl = new Thread(new Runnable() {
            @Override
            public void run() {
            	runProcessClients();
            }
        });
        threadRepl.setName(threadName);
        threadRepl.setDaemon(true);
        threadRepl.start();
    	LOG.fine("thread started to Replicate Clients with this Master, thread name="+threadName);
    }

	public void stop() throws Exception {
    	LOG.fine("Stop called"); 
    	
		synchronized (lockTLogFile) {
			if (objectOutputStream != null) {
		    	objectOutputStream.close();
				objectOutputStream = null;
			}
		}    	
		super.stop();
    }

	protected void runProcessClients() {
    	final OAThrottle throttle = new OAThrottle(250);
    	try {
    		for (; !this.bStop; ) {
    	    	LOG.fine("checking to sync Repli Clients with Master");
    			long ms = System.currentTimeMillis();
        		for (int id : hmClientSession.keySet()) {
        			if (OAReplicationMaster.this.bStop) break;
        			ReplClientSession ci = hmClientSession.get(id);
        			
        			LOG.fine("processing client " +id);
        			if (ci == null) continue;
        			try {
        				ci.process();
        			}
                    catch (Exception e) {
                    	if (throttle.check()) {
                    		LOG.log(Level.WARNING, "exception while processing client merge for connection: " + id +", will continue", e);
                    	}
                    }
        		}
    			long diff = System.currentTimeMillis() - ms;
        		if (diff < 2000) OAThread.sleep(2000 - diff);
    		}
        }
        catch (Exception e) {
            String s = "ProcesClient Thread is stopping, which will stop replicating with clients.";
            LOG.log(Level.WARNING, s, e);
        }
	}
    
	protected class ReplClientSession {
		final int sessionId;  // same as connectionId
		final RemoteClientInterface remoteClient;
		
		final LinkedBlockingQueue<ClientMsg> alClientMsg = new LinkedBlockingQueue<>();

		final String guid;  // unique replClient name
		
		volatile long lastReceivedMasterSeq;
		volatile long lastProcessedMasterSeq;
		volatile long lastSentMasterSeq;
		
		volatile long lastReceivedClientSeq; 
		volatile long lastProcessedClientSeq;

		volatile long msLastProcessed;
		volatile int lastRequestInfoSize;
		
		volatile boolean bEnabled = true;
		
		ReplClientSession(String guid, int sessionId, RemoteClientInterface remoteClient, long lastSentMasterSeq, long lastSentClientSeq) {
			this.guid = guid;
			this.sessionId = sessionId;
			this.remoteClient = remoteClient;
			this.lastReceivedMasterSeq = lastSentMasterSeq;
			this.lastReceivedClientSeq = lastSentClientSeq;
		}
		
		// ReplClient uses this as remote object to send sync messages to ReplMaster
		final RemoteMasterInterface remoteMaster = new RemoteMasterInterface() {
			@Override
			public void processMessage(long masterSeq, long clientSeq, String methodName, Object[] args) {
				LOG.fine("received message from Client.session="+sessionId+", method="+methodName);
				
				ClientMsg msg = new ClientMsg();
				msg.masterSeq = masterSeq;
				msg.clientSeq = clientSeq;
				msg.methodName = methodName;
				msg.args = args;
				try {
					alClientMsg.put(msg);
				}
				catch (Exception ex) {
					LOG.log(Level.WARNING, "error adding ClientMsg to blocking que", ex);
				}
				if (clientSeq > lastReceivedClientSeq) {
					lastReceivedClientSeq = clientSeq;
					lastReceivedMasterSeq = masterSeq;
				}
			}
			@Override
			public long getLastReceivedClientSeq() {
				return lastReceivedClientSeq;
			}
			@Override
			public long getLastProcessedClientSeq() {
				return lastProcessedClientSeq;
			}
			@Override
			public long getLastReceivedMasterSeq() {
				return lastReceivedMasterSeq;
			}
			@Override
			public long getLastProcessedMasterSeq() {
				return lastProcessedMasterSeq;
			}
			@Override
			public void setEnabled(boolean b) {
				bEnabled = b;
			}
			@Override
			public boolean getEnabled() {
				return bEnabled;
			}
			@Override
			public void setLastReceivedMasterSeq(long seq) {
				lastSentMasterSeq = seq;
			}
		}; 
 
		void process() {
			if (!bEnabled) return;
			long msNow = System.currentTimeMillis();
			int size = alClientMsg.size();
			int size2 = getRequestInfoSize();
			
			LOG.fine("processing msgs from Client.session="+sessionId+" clientMsg.size="+size+", masterMsg.size="+(size2 - lastRequestInfoSize));
			if (msLastProcessed != 0L && msLastProcessed + 1000 > msNow) {
				if (size < 50 && (size2 - lastRequestInfoSize) < 50) {
					return;
				}
			}
			lastRequestInfoSize = size2;

			
			// invoke client changes on master.
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
			srvcOAThreadLocal.setReplicationSource(this.guid);
			for (int i=0; ; i++) {
				try {
					ClientMsg cm = alClientMsg.poll();
					if (cm == null) break;
					
					if (cm.clientSeq <= lastProcessedClientSeq) continue;
					
					Method method = getMethod(cm.methodName);
					LOG.fine("invoking message from Client.session="+sessionId+", method="+method.getName());
					method.invoke(syncServer.getRemoteSyncImpl(), cm.args);

					lastProcessedClientSeq = cm.clientSeq;
					lastProcessedMasterSeq = cm.masterSeq;
				}
				catch (Exception ex) {
					LOG.log(Level.WARNING, "exception invoking client message", ex);
					break;
				}
			}
			srvcOAThreadLocal.setReplicationSource(null);
			
			// send master server msgs to client
			final int x = alListReplTLog.size();
			for (int i=0; i < (x-1); i++) {
				List<OAReplTLog> al;
				synchronized (alListReplTLog) {
					al = alListReplTLog.get(i);
				}
				if (al.get(RequestInfoListSize-1).masterSeq <= lastProcessedMasterSeq) continue;
				for (OAReplTLog tlog : al) {
					if (tlog.masterSeq <= lastSentMasterSeq) continue;
					
					String s = tlog.getSource();
					if (OAStr.equals(s,  guid)) continue;
					
					LOG.fine("sending Master message to Client.session="+sessionId+", method="+tlog.methodName);
					remoteClient.processMessage(tlog.masterSeq, tlog.methodName, tlog.args);
					lastSentMasterSeq = tlog.masterSeq;
				}
			}

			if (x > 0) {
				List<OAReplTLog> al;
				synchronized (alListReplTLog) {
					al = alListReplTLog.get(x-1);
				}
				synchronized (al) {
					for (OAReplTLog tlog : al) {
						if (tlog.masterSeq <= lastSentMasterSeq) continue;

						String s = tlog.getSource();
						if (OAStr.equals(s,  guid)) continue;
						
						LOG.fine("sending Master message to Client.session="+sessionId+", method="+tlog.methodName);
						remoteClient.processMessage(tlog.masterSeq, tlog.methodName, tlog.args);
						lastSentMasterSeq = tlog.masterSeq;
					}
				}
			}
			msLastProcessed = System.currentTimeMillis();;
		}
	}
	
	protected static class ClientMsg {
		long masterSeq;
		long clientSeq;
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
	
	// Note:  needs to be called from OASyncServer	
	public void onClientDisconnected(int clientId) {
		hmClientSession.remove(clientId);
	}

	@Override
	protected void onNewSyncMessage(RequestInfo ri) {
		currentMasterSeq++;
		
        final OAReplTLog tlog = new OAReplTLog(ri.replicationSource, new OADateTime(), currentMasterSeq, 0L, ri.method.getName(), ri.args);
		
		writeTLog(tlog);
		addTLog(tlog);
	}
	

	protected void loadTLogFile() {
		try {
			String fn = OAFile.convertFileName(tlogFileName);
	        File file = new File(fn);
			
	    	LOG.fine(String.format("tlogFileName=%s, exists=%b", fn, file.exists()));
	        if (file.exists()) {
	            FileInputStream fis = new FileInputStream(file);
	            BufferedInputStream bis = new BufferedInputStream(fis, 64 * 1024);
	            ObjectInputStream ois = new ObjectInputStream(bis);
	            currentMasterSeq = ois.readLong();
	            
	            int cnt = 0;
	            for (; ; cnt++) {
	            	OAReplTLog tlog;
	            	try {
	                	tlog = (OAReplTLog) ois.readObject();
	                	currentMasterSeq = tlog.getMasterSeq();
	            	}
	            	catch (EOFException e) {
	            		break;
	            	}
	            	catch (IOException e) {
	            		throw new RuntimeException("Exception loading TLog file", e);
	            	}
	        		
        			addTLog(tlog);
			    	LOG.fine(String.format("%,d) masterSeq=%,d, methodName=%s", cnt+1, tlog.getMasterSeq(), tlog.getMethodName()));
	            }
	            ois.close();
		    	LOG.fine(String.format("tlogFileName=%s, total tlog records=%,d", fn, cnt));
	    	}
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
	}

	
	protected void openTLogFile() {
		synchronized (lockTLogFile) {
			_openTLogFile();
		}		
	}
	protected void _openTLogFile() {
		try {
			if (objectOutputStream != null) {
				objectOutputStream.close();
			}
			
			String fn = OAFile.convertFileName(tlogFileName);
			OAFile.mkdirsForFile(fn);
	        File file = new File(fn);
	        
	    	LOG.fine(String.format("tlogFileName=%s, exists=%b", fn, file.exists()));
	        final boolean bAppend = file.exists() && file.length() > 0;
	        fileOutputStream = new FileOutputStream(file, bAppend);
	        BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream, 64 * 1024);

	        objectOutputStream = new ObjectOutputStream(bos) {
	        	@Override
	            protected void writeStreamHeader() throws IOException {
	                if (bAppend) reset(); // writes a TC_RESET token into the stream, avoids duplicate stream header when appending
	                else super.writeStreamHeader();
	            }
	        };
	        if (!bAppend) {
			    LOG.fine(String.format("wrote header: masterSeq=%,d", currentMasterSeq));
	        	objectOutputStream.writeLong(currentMasterSeq);
		        objectOutputStream.flush();
		        fileOutputStream.getFD().sync();
	        }
		}
		catch (Exception e) {
			throw new RuntimeException("exception opening tlog file", e);
		}
	}

	protected void createNewTLogFile(String newFileName) {
		try {
			String fn = OAFile.convertFileName(newFileName);
			OAFile.mkdirsForFile(fn);
	        File file = new File(fn);
			
	    	LOG.fine(String.format("fileName=%s, exists=%b, wasOpen=%b", fn, file.exists(), (objectOutputStream != null)));
			if (objectOutputStream != null) {
				objectOutputStream.close();
			}
	        
	        fileOutputStream = new FileOutputStream(file); 
	        BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream, 64 * 1024);
	        objectOutputStream = new ObjectOutputStream(bos);
        	objectOutputStream.writeLong(currentMasterSeq);
	        objectOutputStream.flush();
	        fileOutputStream.getFD().sync();

		    LOG.fine(String.format("wrote header: masterSeq=%,d", currentMasterSeq));
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
	}

	private final Object lockTLogFile = new Object();

	
	protected void writeTLog(final OAReplTLog tlog) {
		try {
			synchronized (lockTLogFile) {
		        objectOutputStream.writeObject(tlog);
		        objectOutputStream.flush();                            
		        fileOutputStream.getFD().sync();
			}
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
	}
	
	protected void addTLog(final OAReplTLog tlog) {
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
