package com.viaoa.graph.service;

import com.viaoa.repl.OAReplicationClient;
import com.viaoa.repl.OAReplicationMaster;
import com.viaoa.sync.OASyncServer;
import com.viaoa.graph.api.internal.*;

/**
 * Used with OASyncServer to Replicate with another OASyncServer.
 * 
 * Allows offline support and reconnect with resync support.
 * 
 */
public class OAReplicationService implements ReplInternalOps {

	private final String guid;
    private final OASyncServer syncServer;
    private final String tLogFileName;
    private final String replicationMasterHostName;
    private final int replicationMasterPort;
    private final boolean bIsMaster;
	
	private OAReplicationMaster replMaster;
    private OAReplicationClient replClient;

    /**
     * Create OAReplication Master
     * @param guid
     * @param syncServer
     * @param tLogFileName
     */
	public OAReplicationService(String guid, OASyncServer syncServer, String tLogFileName) {
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
	public OAReplicationService(String guid, OASyncServer syncServer, String tLogFileName, String replicationMasterHostName, int replicationMasterPort) {
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
