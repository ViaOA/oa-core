package com.viaoa.repl;

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
import com.viaoa.sync.remote.RemoteSyncInterface;


/**
 * Connects to OAReplicationMaster
 * gets Sync messages from server.oasync and sends them to OAReplicationMaster.
 * gets oasync messages from OAReplicationMaster, using remote RemoteRepInterface.processMessage, and calls RemoteSyncImpl method.
 * Keeps track of current message (oasync) position for client and master sync queue.
 * 
 */
public class OAReplicationClient extends OAReplicationBase {
    private static Logger LOG = Logger.getLogger(OAReplicationClient.class.getName());

	private final String masterHostName;
	private final int masterHostPort;

//qqqqqq need to store these	
	private long posMaster; 
	private long posClient; 
	
	
    private RemoteSyncImpl remoteSyncImpl;
	
	private volatile OAReplClientConnection replClientConnection;
	
	private final Map<Integer, Boolean> hmIgnoreRequestInfo = new ConcurrentHashMap<>();
	
	private final LinkedBlockingQueue<RequestInfoMessage> queRequestInfo = new LinkedBlockingQueue<>();
	
    public OAReplicationClient(OASyncServer syncServer, String masterHostName, int masterHostPort) {
    	super(syncServer);
    	this.masterHostName = masterHostName;
    	this.masterHostPort = masterHostPort;
    }

    @Override
    public void start() {
    	super.start();
    	LOG.fine("starting ReplMaster");
    	
        final String threadName = "OAReplicationClient";
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
		    	for (; !bStop; ) {
		    		OAReplClientConnection rcc = getReplClientConnection(); // will be null if cant connect to master
		    		
		    		if (rcc == null ) {
		    			OAThread.sleep(5 * 1000);
		    			continue;
		    		}
					try {
						RequestInfoMessage rim = queRequestInfo.poll(2, TimeUnit.SECONDS); 
						if (rim == null) {
							continue;
						}
						RequestInfo ri = rim.ri;
						posClient = ri.messageId;
						LOG.fine("sending message to Master, method="+ri.method.getName());
						rcc.getRemoteMaster().processMessage(rim.posMaster, posClient, ri.method.getName(), ri.args);
					}
					catch (Exception ex) {
						LOG.log(Level.WARNING, "exception calling RemoteRepl.processMessage", ex);
					}
		    	}
            }
        });
        t.setName(threadName);
        t.setDaemon(true);
        t.start();
    	LOG.fine("thread started to Replicate this Client with Master, thread name="+threadName);
    }
    
    public RemoteSyncInterface getRemoteSyncImpl() {
    	if (remoteSyncImpl == null) {
    		remoteSyncImpl = new RemoteSyncImpl();
    	}
    	return remoteSyncImpl;
    }
    

    public OAReplClientConnection getReplClientConnection() {
    	if (replClientConnection != null && !replClientConnection.isStopped()) return replClientConnection;
    	
    	LOG.fine("creating new ReplClientConnection");
    	replClientConnection = new OAReplClientConnection(masterHostName, masterHostPort) {
			@Override
			public void processMessageFromMaster(long posMaster, String methodName, Object[] args) {
				OAReplicationClient.this.posMaster = posMaster;
				Method method = getMethod(methodName);
				LOG.fine("received msg from Master, method="+method.getName());
				try {
					method.invoke(getRemoteSyncImpl(), args);
				}
				catch (Exception ex) {
					LOG.log(Level.WARNING, "error invoking method="+methodName, ex);
				}
				
				RequestInfo ri = OAThreadLocalDelegate.getRemoteRequestInfo();
				hmIgnoreRequestInfo.put(ri.messageId, true);
			}
			
			@Override
			protected void onSocketException(Exception e) {
				LOG.fine("stopping connection to Master");
				OAReplicationClient.this.replClientConnection = null;
				try {
					this.stop();
				}
				catch (Exception e2) {
				}
			}
			
			@Override
			protected void onSocketClose(boolean bError) {
				LOG.fine("stopping connection to Master");
				OAReplicationClient.this.replClientConnection = null;
				try {
					this.stop();
				}
				catch (Exception e2) {
				}
			}
		};
		
		try {
			replClientConnection.start();
		}
		catch (Exception e) {
			OAReplicationClient.this.replClientConnection = null;
			try {
				this.stop();
			}
			catch (Exception e2) {
			}
		};
		
    	return replClientConnection;
    }

	protected static class RequestInfoMessage {
		long posMaster;
		RequestInfo ri;
	}

    
	@Override
	protected void onNewRequestInfoMessage(long qpos, RequestInfo ri) {
		if (hmIgnoreRequestInfo.remove(ri.messageId) != null) return;
        LOG.fine("new message from Sync que");

		try {
			RequestInfoMessage rim = new RequestInfoMessage();
			rim.posMaster = this.posMaster;
			rim.ri = ri;
			queRequestInfo.put(rim);
		}
		catch (Exception e2) {
			
		}
	}
    
}
