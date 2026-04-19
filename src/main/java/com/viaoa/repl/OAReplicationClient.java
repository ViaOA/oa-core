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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.concurrent.OAThread;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.repl.client.OAReplClientConnection;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.remote.RemoteSyncImpl;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAFile;
import com.viaoa.util.OAStr;

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

	private volatile long masterSeq; // last value from Master
	private long lastSentMasterSeq;
	private long clientSeq; // current value (last created/received from circque)(
	private long lastSentClientSeq; // last value sent to Master
	private boolean bGotSeqFromMaster;
	
    private RemoteSyncImpl remoteSyncImpl;
	
	private volatile OAReplClientConnection replClientConnection;
	
	// to Master
	private final LinkedBlockingQueue<OAReplTLog> alTLogToMaster = new LinkedBlockingQueue<>();

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
        String threadName = "OAReplicationClient";
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
		OAReplClientConnection rccLast = null;
    	for ( ; !bStop ; ) {
    		OAReplClientConnection rcc = null;
    		
    		rcc = getReplClientConnection(); // will be null if cant connect to master
    		if (rcc == null ) {
    			for (int i=0; i < 10 && !bStop; i++) OAThread.sleep(500);
    			continue;
    		}
    		
    		try {
    			if (rcc != rccLast) {
            		rcc.getRemoteMaster().setLastReceivedMasterSeq(this.masterSeq);
            		rccLast = rcc;
    			}
    			
    			if (!bGotSeqFromMaster) {
            		long x = rcc.getRemoteMaster().getLastReceivedClientSeq();
					if (x > this.lastSentClientSeq) {
						this.lastSentClientSeq = x;
					}
					/* add this back when Master keeps track of last know Client GUID lastSentMasterSeq
					x = rcc.getRemoteMaster().getLastReceivedMasterSeq();
					if (x > this.lastSentMasterSeq) {
						this.lastSentMasterSeq = x;
					}
					*/
					bGotSeqFromMaster = true;
				}
				if (tlog == null) {
					tlog = alTLogToMaster.poll(1, TimeUnit.SECONDS);
					if (tlog == null && !bStop) tlog = alTLogToMaster.poll(1, TimeUnit.SECONDS); 
					if (tlog == null) continue;
				}

				if (tlog.getClientSeq() > lastSentClientSeq) {
					LOG.fine("sending message to Master, method="+tlog.methodName);
	
					rcc.getRemoteMaster().processMessage(tlog.getMasterSeq(), tlog.getClientSeq(), tlog.getMethodName(), tlog.getArgs());
					lastSentClientSeq = tlog.getClientSeq();
					lastSentMasterSeq = tlog.getMasterSeq();
				}				
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
	    	createNewTLogFile(tlogFileName + ".tmp");
	    	int cnt = 0;
	    	for (;; cnt++) {
	    		if (tlog == null) {
	    			tlog = alTLogToMaster.poll();
		    		if (tlog == null) break;
	    		}
	    		writeTLog(tlog);
	    		tlog = null;
	    	}
    		objectOutputStream.close();
    		objectOutputStream = null;
    		
			String fn = OAFile.convertFileName(tlogFileName + ".tmp");
    		File f1 = new File(fn);
    		
			fn = OAFile.convertFileName(tlogFileName);
    		f1.renameTo(new File(fn));
	    	LOG.fine(String.format("rewrote file %s, %,d tlog records", fn, cnt)); 
    	}
    	catch (Exception e) {
    		LOG.log(Level.WARNING, "exception rewriting tlog file, will use original one (no data loss)", e);
    	}
	}

	// Sync message from this.Server's queue
	@Override
	protected void onNewSyncMessage(RequestInfo ri) {
		boolean bCausedByMasterMsg = (OAStr.equals(guid, ri.replicationSource)); 
		
        if (!bCausedByMasterMsg) {
        	clientSeq++;
        }
		LOG.fine(String.format("new OASync message from this server, skipping=%b, methodName=%s, masterSeq=%,d, clientSeq=%,d", bCausedByMasterMsg, ri.method.getName(), masterSeq, clientSeq));

        final OAReplTLog tlog = new OAReplTLog(guid, new OADateTime(), masterSeq, clientSeq, ri.method.getName(), ri.args);
		try {
			writeTLog(tlog);
			if (!bCausedByMasterMsg) { // happened when processing Master msg, dont send back to Master			
				alTLogToMaster.put(tlog);
			}
		}
		catch (Exception e2) {
    		throw new RuntimeException("exception writing to tlog", e2);
		}
	}
	
	
	private boolean bDisconnectFromMaster;	
	public void setDisconnectFromMaster(boolean b) {
		this.bDisconnectFromMaster = b;
		if (b) {
			try {
				replClientConnection.stop();
			}
			catch (Exception e) {}
			replClientConnection = null;
		}
	}
	
	
    /**
     * returns null if connection can not be made, and is set to null if connected is stopped.
     */
    protected OAReplClientConnection getReplClientConnection() {
    	if (replClientConnection != null && !replClientConnection.isStopped()) return replClientConnection;
    	if (bDisconnectFromMaster) return null;
    	
    	LOG.fine("creating new ReplClientConnection");
    	replClientConnection = new OAReplClientConnection(guid, masterHostName, masterHostPort, lastSentMasterSeq, lastSentClientSeq) {
			@Override
			public void processMessageFromMaster(long masterSeq, String methodName, Object[] args) {
				OAReplicationClient.this.onNewMessageFromMaster(masterSeq, methodName, args);
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

    /**
     * New message from OARepl Master.  This will add it to the queue that the OAReplicationThread
     * @param masterSeq
     * @param methodName
     * @param args
     */
    protected void onNewMessageFromMaster(long masterSeq, String methodName, Object[] args) {
    	if (remoteSyncImpl == null) {
    		remoteSyncImpl = new RemoteSyncImpl();
    	}
    	if (masterSeq < lastSentMasterSeq) {
    		return; 
    	}
    	
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		try {
			srvcOAThreadLocal.setReplicationSource(guid);
			LOG.fine(String.format("received msg from Master, masterSeq=%,d, methodName=%s", masterSeq, methodName)); 
			
			Method method = getMethod(methodName);
			method.invoke(remoteSyncImpl, args);   

			this.masterSeq = masterSeq;
		}
		catch (Exception e) {
    		throw new RuntimeException("Exception onNewMessageFromMaster", e);
		}
		finally {
			srvcOAThreadLocal.setReplicationSource(null);
		}
    }

	
	protected void loadTLogFile() throws Exception {
		String fn = OAFile.convertFileName(tlogFileName);
        File file = new File(fn);
    	LOG.fine(String.format("tlogFileName=%s, exists=%b", fn, file.exists()));
        if (!file.exists()) return;
        
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis, 64 * 1024);
        ObjectInputStream ois = new ObjectInputStream(bis);
        
    	String guidx = ois.readUTF();
        masterSeq = ois.readLong();
        lastSentMasterSeq = ois.readLong();
        clientSeq = ois.readLong();
        lastSentClientSeq = ois.readLong();
    	LOG.fine(String.format("guid=%s, masterSeq=%,d, clientSeq=%,d, lastClientSeqOnMaster=%,d", guidx, masterSeq, clientSeq, lastSentClientSeq));

    	if (OAStr.compare(guid, guidx) != 0) {
    		throw new RuntimeException(String.format("TLogFile guid=%s, does not match runtime guid=%s", guidx, guid));
    	}
    	
        int cnt = 0;
        for (; ; cnt++) {
        	OAReplTLog tlog;
        	try {
            	tlog = (OAReplTLog) ois.readObject();
            	clientSeq = tlog.getClientSeq();
            	masterSeq = tlog.getMasterSeq();
        	}
        	catch (EOFException e) {
        		break;
        	}
        	catch (IOException e) {
        		throw new RuntimeException("Exception loading TLog file", e);
        	}
    		
        	boolean bDontSend = (OAStr.equals(guid, tlog.getSource())); 
			if (!bDontSend) {
				alTLogToMaster.put(tlog);
		    	LOG.fine(String.format("%,d) guid=%s, masterSeq=%,d, clientSeq=%,d, methodName=%s", cnt+1, 
			    		guid, tlog.getMasterSeq(), tlog.getClientSeq(), tlog.getMethodName()));
			}
        }
        ois.close();
        bis.close();
        fis.close();
    	LOG.fine(String.format("tlogFileName=%s, total tlog records=%,d", fn, cnt));
	}

	protected void openTLogFile() throws Exception {
		if (objectOutputStream != null) {
			objectOutputStream.close();
		}
		
		String fn = OAFile.convertFileName(tlogFileName);
		OAFile.mkdirsForFile(fn);
        File file = new File(fn);
    	LOG.fine(String.format("tlogFileName=%s, exists=%b", fn, file.exists()));
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
        	objectOutputStream.writeLong(masterSeq);
        	objectOutputStream.writeLong(lastSentMasterSeq);
        	objectOutputStream.writeLong(clientSeq);
        	objectOutputStream.writeLong(lastSentClientSeq);
	        objectOutputStream.flush();
	        fileOutputStream.getFD().sync();
        }
	}

	protected void createNewTLogFile(String fileName) {
		try {
			String fn = OAFile.convertFileName(fileName);
			OAFile.mkdirsForFile(fn);
	        File file = new File(fn);
			
	    	LOG.fine(String.format("fileName=%s, exists=%b, wasOpen=%b", fn, file.exists(), (objectOutputStream != null)));
			if (objectOutputStream != null) {
				objectOutputStream.close();
			}
	        
	        fileOutputStream = new FileOutputStream(file); 
	        BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream, 64 * 1024);
	        objectOutputStream = new ObjectOutputStream(bos);
        	objectOutputStream.writeUTF(this.guid);
        	objectOutputStream.writeLong(masterSeq);
        	objectOutputStream.writeLong(lastSentMasterSeq);
        	objectOutputStream.writeLong(clientSeq);
        	objectOutputStream.writeLong(lastSentClientSeq);
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
