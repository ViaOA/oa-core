package com.viaoa.graph;

import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.sync.OASyncServer;

public class OASyncService {
	private final OAGraph graph;

	private OASyncServer syncServer;
    private OASyncClient syncClient;

	public OASyncService(OAGraph graph) {
    	if (graph == null) throw new IllegalArgumentException("graph can not be null");
    	this.graph = graph;
	}

    public OAGraph graph() {
    	return graph;
    }
	
    public void createServer(int port) throws Exception {
    	stopServer();
        syncServer = new OASyncServer(graph.getPackage(), port);
        syncServer.setInvalidConnectionMessage("qqqqqqqq"); //qqqqqq
        syncServer.start();
        //qqqqq temp qqqqqqqqqq
		OASyncDelegate.setSyncServer(graph.getPackage(), syncServer);
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
    	syncClient = new OASyncClient(graph.getPackage(), serverName, port);
        syncClient.start();
        OASyncDelegate.setSyncClient(graph.getPackage(), syncClient);
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
}
