package com.viaoa.graph.service;

import com.viaoa.sync.OASyncServer;
import com.viaoa.graph.api.internal.*;
import com.viaoa.replication.OAReplicationClient;
import com.viaoa.replication.OAReplicationMaster;

/**
 * Used with OASyncServer to Replicate with another OASyncServer.
 * 
 * Allows offline support and reconnect with resync support.
 * 
 */
public class OAReplicationService implements ReplInternalOps {

	
//qqqqqqqqqqqqqq exception if OASync is not server	
	
	private String guid;
    private OASyncServer syncServer;
    private String tLogFileName;
    private String replicationMasterHostName;
    private int replicationMasterPort;
    private boolean bIsMaster;
	
	private OAReplicationMaster replMaster;
    private OAReplicationClient replClient;

    
	public OAReplicationService() {
	}
    
    /**
     * Create OAReplication Master
     * @param guid
     * @param syncServer
     * @param tLogFileName
     */
	public void createMaster(String guid, OASyncServer syncServer, String tLogFileName) {
		this.guid = guid;
		this.syncServer = syncServer;
		this.tLogFileName = tLogFileName;
		this.replicationMasterHostName = null;
		this.replicationMasterPort = 0;
		this.bIsMaster = true;
	}

	
	/**
	 * Create OAReplication Client
	 * @param guid
	 * @param syncServer
	 * @param tLogFileName
	 * @param replicationMasterHostName
	 * @param replicationMasterPort
	 */
	public void createClient(String guid, OASyncServer syncServer, String tLogFileName, String replicationMasterHostName, int replicationMasterPort) {
		this.guid = guid;
		this.syncServer = syncServer;
		this.tLogFileName = tLogFileName;
		this.replicationMasterHostName = replicationMasterHostName;
		this.replicationMasterPort = replicationMasterPort;
		this.bIsMaster = false;
	}
	
	
    public void start() throws Exception {
    	if (bIsMaster) {
    		replMaster = new OAReplicationMaster(syncServer, tLogFileName);
    		replMaster.start();
    	}
    	else {
    		replClient = new OAReplicationClient(tLogFileName, guid, syncServer, replicationMasterHostName, replicationMasterPort);
    		replClient.start();
    	}
    }

    public void stop() throws Exception {
    	if (replClient != null) replClient.stop();
    	if (replMaster != null) replMaster.stop();
    }
	
}
