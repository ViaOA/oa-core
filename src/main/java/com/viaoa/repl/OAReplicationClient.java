package com.viaoa.repl;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.concurrent.OAThread;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.repl.client.OAReplClientConnection;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.remote.RemoteSyncImpl;
import com.viaoa.sync.remote.RemoteSyncInterface;

public class OAReplicationClient extends OAReplicationBase {
    private static Logger LOG = Logger.getLogger(OAReplicationClient.class.getName());

	private final String masterHostName;
	private final int masterHostPort;

    private RemoteSyncImpl remoteSyncImpl;
	
	private volatile long posMaster;
	private volatile long posThisClient;
	
	
	private volatile OAReplClientConnection replClientConnection;
	
	private final Map<RequestInfo, Boolean> hmIgnoreRequestInfo = new ConcurrentHashMap<>();
	
	
    public OAReplicationClient(OASyncServer syncServer, String masterHostName, int masterHostPort) {
    	super(syncServer);
    	this.masterHostName = masterHostName;
    	this.masterHostPort = masterHostPort;
    }

   
    public void start() {
    	//qqqqq create thread qqqqqq get from server code
    	for (;;) {
    		OAReplClientConnection rcc = getReplClientConnection();
    		if (rcc == null) {
    			OAThread.sleep(5 * 1000);
    			continue;
    		}
			try {
				
			}
			catch (Exception ex) {
				
				
			};
    		
    	}
    	
    }
    
    
    public RemoteSyncInterface getRemoteSyncImpl() {
    	if (remoteSyncImpl == null) {
    		remoteSyncImpl = new RemoteSyncImpl();
    	}
    	return remoteSyncImpl;
    }
    

    public OAReplClientConnection getReplClientConnection() {
    	if (replClientConnection != null && !replClientConnection.isStopped()) return replClientConnection;
    	
    	replClientConnection = new OAReplClientConnection(masterHostName, masterHostPort) {
			@Override
			public long processMessage(long posFrom, long posTo, String methodName, Object[] args) {
				// from Master to thisClient
				// TODO Auto-generated method stub
				//qqqqqqqqqq
				
				Method method = null; //qqqqqqqqqq
				//qqqqqqq try to get 
				
				
				try {
					method.invoke(getRemoteSyncImpl(), args);
				}
				catch (Exception ex) {
				}
				
				RequestInfo ri = OAThreadLocalDelegate.getRemoteRequestInfo();
				hmIgnoreRequestInfo.put(ri, true);
				return ri.cnt;
			}
			
			@Override
			protected void onSocketException(Exception e) {
				OAReplicationClient.this.replClientConnection = null;
				try {
					this.stop();
				}
				catch (Exception e2) {};
			}
			
			@Override
			protected void onSocketClose(boolean bError) {
				OAReplicationClient.this.replClientConnection = null;
				try {
					this.stop();
				}
				catch (Exception e2) {};
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
			catch (Exception e2) {};
		};
		
    	return replClientConnection;
    }
    
    
    

	@Override
	public void onNewRequestInfo(long qpos, RequestInfo ri) {
		if (hmIgnoreRequestInfo.remove(ri) != null) return;
		// TODO Auto-generated method stub
//qqqqqqqq
		//qqqqq need to store to file qqqqqqq
		// collect them
//		call(ri, getRemoteSync());
		
	}
    
}
