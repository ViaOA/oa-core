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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.concurrent.OAThread;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.repl.client.OAReplClientConnection;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.remote.RemoteSyncImpl;
import com.viaoa.util.OADateTime;

/**
 * Connects to OAReplicationMaster
 * gets Sync messages from this.server.oasync and sends them to OAReplicationMaster.
 * gets oasync messages from OAReplicationMaster, using remote RemoteRepInterface.processMessage, and calls this.RemoteSyncImpl method.
 * Keeps track of current message (oasync) position for client and master sync queue.
 * 
 */
public class OAReplicationClient extends OAReplicationBase {
    private static Logger LOG = Logger.getLogger(OAReplicationClient.class.getName());

	private final String masterHostName;
	private final int masterHostPort;

	private final String guid;
	private long masterSeq; 
	private long clientSeq; 
	
    private RemoteSyncImpl remoteSyncImpl;
	
	private volatile OAReplClientConnection replClientConnection;
	
	private final Map<Integer, Boolean> hmIgnoreRequestInfo = new ConcurrentHashMap<>();
	
	private final LinkedBlockingQueue<OAReplTLog> alTLog = new LinkedBlockingQueue<>();

    private FileOutputStream fileOutputStream;
    private ObjectOutputStream objectOutputStream;
	
	protected final String tlogFileName;
	
    public OAReplicationClient(String tlogFileName, String guid, OASyncServer syncServer, String masterHostName, int masterHostPort) {
    	super(syncServer);
    	this.guid = guid;
    	this.tlogFileName = tlogFileName;     	
    	this.masterHostName = masterHostName;
    	this.masterHostPort = masterHostPort;
    	
    	LOG.fine(String.format("OAReplicationClient guid=%s, tlogFileName=%s, masterHostName=%s, masterHostPort=%d", 
    			guid, tlogFileName, masterHostName, masterHostPort
    	));
    }
    
    @Override
    public void start() throws Exception {
    	LOG.fine("starting OAReplicationClient");
    	this._start();
    	super.start();
    }

    protected void _start() throws Exception {
    	loadTLogFile();
        openTLogFile();
    	
        // send Sync message from Client to Master
        final String threadName = "OAReplicationClient";
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
            	OAReplicationClient.this.runSendSyncMessagesToMaster();
            }
        });
        t.setName(threadName);
        t.setDaemon(false);
        t.start();
    	LOG.fine(String.format("thread started to send sync msgs to Master, thread=%s", threadName)); 
    }
    
	public void stop() throws Exception {
    	LOG.fine("Stop called"); 
		super.stop();
		if (replClientConnection != null) {
			replClientConnection.stop();
			replClientConnection = null;
		}
    }

	
	protected void runSendSyncMessagesToMaster() {
    	OAReplTLog tlog = null;
    	long minClientSeq = -1; // so that previous sent msgs can be skipped, if tlog did not remove during last write-out.
    	for ( ; !bStop ; ) {
    		OAReplClientConnection rcc = null;
    		
    		rcc = getReplClientConnection(); // will be null if cant connect to master
    		if (rcc == null ) {
    			for (int i=0; i < 10 && !bStop; i++) OAThread.sleep(500);
    			continue;
    		}

    		try {
				if (minClientSeq < 0) {
					minClientSeq = rcc.getRemoteMaster().getMinimumClientSeq();
				}
				if (tlog == null) {
					tlog = alTLog.poll(1, TimeUnit.SECONDS); 
					if (tlog == null && !bStop) tlog = alTLog.poll(1, TimeUnit.SECONDS); 
				}
				if (tlog == null) continue;

				if (tlog.getClientSeq() < minClientSeq) continue;
				
				LOG.fine("sending message to Master, method="+tlog.methodName);

				rcc.getRemoteMaster().processMessage(tlog.getMasterSeq(), tlog.getClientSeq(), tlog.getMethodName(), tlog.getArgs());
				
				tlog = null;
			}
			catch (Exception ex) {
				String s = String.format("exception calling OAReplicationClient.runSendSyncMessagesToMaster, Stop=%b, exception=%s",  bStop, ex.toString());
				if (!bStop) {
					LOG.log(Level.WARNING, s, ex);
					OAThread.sleep(1000);
				}
				else LOG.log(Level.WARNING, s, ex); 
			}
    	}

    	// re-write file
    	LOG.fine(String.format("rewriting to temp file %s.tmp", tlogFileName)); 
    	try {
	    	createNewTLogFile(tlogFileName + ".temp");
	    	int cnt = 0;
	    	for (;; cnt++) {
	    		if (tlog == null) tlog = alTLog.poll();
	    		if (tlog == null) break;
	    		writeTLog(tlog);
	    		tlog = null;
	    	}
    		objectOutputStream.close();
    		
    		File f1 = new File(tlogFileName + ".temp");
    		f1.renameTo(new File(tlogFileName));
	    	LOG.fine(String.format("rewrote file %s, %,d tlog records", tlogFileName, cnt)); 
    	}
    	catch (Exception e) {
    		LOG.log(Level.WARNING, "error rewriting tlog file, will use original one (no data loss)", e);
    	}
	}

	// Sync message from this.Server's queue
	@Override
	protected void onNewSyncMessage(RequestInfo ri) {
		boolean bFound = hmIgnoreRequestInfo.remove(ri.cnt) != null;

        if (!bFound) clientSeq++;

		LOG.fine(String.format("new OASync message from this server, skipping=%b, methodName=%s, masterSeq=%,d, clientSeq=%,d", bFound, ri.method.getName(), masterSeq, clientSeq));
		if (bFound) return;			

        final OAReplTLog tlog = new OAReplTLog(new OADateTime(), masterSeq, clientSeq, ri.method.getName(), ri.args);
		try {
			writeTLog(tlog);
			alTLog.put(tlog);
		}
		catch (Exception e2) {
		}
	}
	
	
    /**
     * returns null if connection can not be made, and is set to null if connected is stopped.
     */
    protected OAReplClientConnection getReplClientConnection() {
    	if (replClientConnection != null && !replClientConnection.isStopped()) return replClientConnection;
    	
    	LOG.fine("creating new ReplClientConnection");
    	replClientConnection = new OAReplClientConnection(guid, masterHostName, masterHostPort) {
			@Override
			public void processMessageFromMaster(long masterSeq, String methodName, Object[] args) {
				OAReplicationClient.this.processMessageFromMaster(masterSeq, methodName, args);
			}
			
			@Override
			protected void onSocketException(Exception e) {
				LOG.fine("stopping connection to Master");
				try {
					this.stop();
				}
				catch (Exception e2) {}
			}
			
			@Override
			protected void onSocketClose(boolean bError) {
				LOG.fine("stopping connection to Master");
				try {
					this.stop();
				}
				catch (Exception e2) {}
			}
			
			@Override
			public void stop() throws Exception {
				OAReplicationClient.this.replClientConnection = null;
				super.stop();
			}
		};
		
		try {
			replClientConnection.start();
			LOG.fine("connection server is available"); 
		}
		catch (Exception e) {
	    	LOG.log(Level.FINE, "could not start Repl Client connection, will briefly wait and try again, exception: " + e.toString());
			this.replClientConnection = null;
		};
    	return replClientConnection;
    }

	protected void processMessageFromMaster(long masterSeq, String methodName, Object[] args) {
		LOG.fine(String.format("received msg from Master, masterSeq=%,d, methodName=%s", masterSeq, methodName)); 

    	if (remoteSyncImpl == null) {
    		remoteSyncImpl = new RemoteSyncImpl();
    	}
		
		Method method = getMethod(methodName);
		try {
			method.invoke(remoteSyncImpl, args);

			RequestInfo ri = OAThreadLocalDelegate.getRemoteRequestInfo();

			hmIgnoreRequestInfo.put(ri.cnt, true);
			
			OAReplicationClient.this.masterSeq = masterSeq;
		}
		catch (Exception ex) {
			LOG.log(Level.WARNING, "error invoking method="+methodName, ex);
		}
	}
	
	protected void loadTLogFile() {
		try {
	        File file = new File(tlogFileName);
	    	LOG.fine(String.format("tlogFileName=%s, exists=%b", tlogFileName, file.exists()));
	        if (file.exists()) {
	            FileInputStream fis = new FileInputStream(file);
	            BufferedInputStream bis = new BufferedInputStream(fis, 64 * 1024);
	            ObjectInputStream ois = new ObjectInputStream(bis);
	            
	        	String guidx = ois.readUTF();
	            clientSeq = ois.readLong();
	            masterSeq = ois.readLong();
		    	LOG.fine(String.format("guid=%s, masterSeq=%,d, clientSeq=%,d", guidx, masterSeq, clientSeq));
	            
	            int cnt = 0;
	            for (; ; cnt++) {
	            	OAReplTLog tlog;
	            	try {
	                	tlog = (OAReplTLog) ois.readObject();
	                	clientSeq = tlog.getClientSeq();
	                	masterSeq = tlog.getMasterSeq();
	            	}
	            	catch (IOException e) {
	            		break;
	            	}
	        		
        			alTLog.put(tlog);
			    	LOG.fine(String.format("%,d) guid=%s, masterSeq=%,d, clientSeq=%,d, methodName=%s", cnt+1, 
			    		guid, tlog.getMasterSeq(), tlog.getClientSeq(), tlog.getMethodName()));
	            }
	            ois.close();
		    	LOG.fine(String.format("tlogFileName=%s, total tlog records=%,d", tlogFileName, cnt));
	    	}
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
	}

	protected void openTLogFile() {
		try {
			if (objectOutputStream != null) {
				objectOutputStream.close();
			}
			
	        File file = new File(tlogFileName);
	    	LOG.fine(String.format("tlogFileName=%s, exists=%b", tlogFileName, file.exists()));
	        final boolean bAppend = file.exists() && file.length() > 0;
	        fileOutputStream = new FileOutputStream(file, true); // append
	        BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream, 64 * 1024);
	        objectOutputStream = new ObjectOutputStream(bos) {
	        	@Override
	            protected void writeStreamHeader() throws IOException {
	                if (bAppend) reset(); // writes a TC_RESET token into the stream, avoids duplicate stream header when appending
	                else super.writeStreamHeader();
	            }
	        };
	        if (!bAppend) {
			    LOG.fine(String.format("wrote header: masterSeq=%,d, clientSeq=%,d, guid=%s", masterSeq, clientSeq, guid));
	        	objectOutputStream.writeUTF(this.guid);
	        	objectOutputStream.writeLong(clientSeq);
	        	objectOutputStream.writeLong(masterSeq);
		        objectOutputStream.flush();
		        fileOutputStream.getFD().sync();
	        }
		}
		catch (Exception e) {
			throw new RuntimeException("exception opening tlog file", e);
		}
	}

	protected void createNewTLogFile(String fileName) {
		try {
	        File file = new File(fileName);
	    	LOG.fine(String.format("fileName=%s, exists=%b, wasOpen=%b", fileName, file.exists(), (objectOutputStream != null)));
			if (objectOutputStream != null) {
				objectOutputStream.close();
			}
	        
	        fileOutputStream = new FileOutputStream(file); 
	        BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream, 64 * 1024);
	        objectOutputStream = new ObjectOutputStream(bos);
        	objectOutputStream.writeUTF(this.guid);
        	objectOutputStream.writeLong(clientSeq);
        	objectOutputStream.writeLong(masterSeq);
	        objectOutputStream.flush();
	        fileOutputStream.getFD().sync();

		    LOG.fine(String.format("wrote header: masterSeq=%,d, clientSeq=%,d, guid=%s", masterSeq, clientSeq, guid));
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
	}

	protected void writeTLog(final OAReplTLog tlog) {
    	LOG.fine(String.format(" masterSeq=%,d, clientSeq=%,d, methodName=%s", tlog.getMasterSeq(), tlog.getClientSeq(), tlog.getMethodName()));
		try {
	        objectOutputStream.writeObject(tlog);
	        objectOutputStream.flush();
	        fileOutputStream.getFD().sync();
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
	}
}
