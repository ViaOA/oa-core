package com.viaoa.graph;

import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.sync.OASyncServer;

public class OASyncService {
	
	private final Package packageThis;

	private OASyncServer syncServer;
    private OASyncClient syncClient;

	public OASyncService(Package packagex) {
    	this.packageThis = packagex;
	}

    public void createServer(int port) throws Exception {
    	stopServer();
        syncServer = new OASyncServer(packageThis, port);
        syncServer.setInvalidConnectionMessage("qqqqqqqq"); //qqqqqq
        syncServer.start();
        //qqqqq temp qqqqqqqqqq
		OASyncDelegate.setSyncServer(packageThis, syncServer);
    }
    
    public void stopServer() throws Exception {
    	if (syncServer != null) {
    		syncServer.stop();
        	syncServer = null;
    	}
    }
    
    public OASyncServer getServer() {
    	return syncServer;
    }
    
    public void createClient(String serverName, int port) throws Exception {
    	stopClient();
    	syncClient = new OASyncClient(packageThis, serverName, port);
        syncClient.start();
        OASyncDelegate.setSyncClient(packageThis, syncClient);
    }
    
    public void stopClient() throws Exception {
    	if (syncClient != null) {
    		syncClient.stop();
    		syncClient = null;
    	}
    }
    
    public OASyncClient getClient() {
    	return syncClient;
    }

	public boolean isClient() {
		OASyncServer ss = getServer();
		OASyncClient sc = getClient();
		return (ss == null && sc != null);
	}

	public boolean isServer() {
		OASyncServer ss = getServer();
		OASyncClient sc = getClient();
		return (ss != null || sc == null);
	}
}
