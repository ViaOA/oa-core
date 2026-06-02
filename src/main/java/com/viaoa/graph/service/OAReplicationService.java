package com.viaoa.graph.service;

import com.viaoa.sync.OASyncServer;
import com.viaoa.graph.api.internal.*;
import com.viaoa.replication.OAReplicationClient;
import com.viaoa.replication.OAReplicationMaster;

/*qqqqqqqqq
CODEX

 #8 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/service/OAReplicationService.java:39, createMaster/createClient;
  src/main/java/com/viaoa/graph/service/OAReplicationService.java:67, start()
  Exact concern: replication accepts a raw OASyncServer and is not wired to the owning OASyncService or graph.
  start() can run with null/foreign sync server state.
  Why it matters: replication is graph-level distributed behavior and should not be able to bind to the wrong sync
  server or start before sync is configured.
  Minimal fix: inject/own OASyncService, validate server role and started state before replication start.
  Suggested invariant: GRAPH_REPLICATION_USES_OWNING_SYNC_SERVICE
  Suggested test coverage: replication start without sync server fails; replication cannot use a sync server from
  another graph.



*/


/**
 * Used with OASyncServer to Replicate with another OASyncServer.
 * 
 * Allows offline support and reconnect with resync support.
 * 
 */
public abstract class OAReplicationService implements ReplInternalOps {
	private String guid;
    private OASyncServer syncServer;
    private String tLogFileName;
    private String replicationMasterHostName;
    private int replicationMasterPort;
    private boolean bIsMaster;
    private Status status = Status.UNKNOWN;
	
	private OAReplicationMaster replMaster;
    private OAReplicationClient replClient;

	public OAReplicationService() {
	}

	
	@Override
	public String getLogFileName() {
		return tLogFileName;
	}

	@Override
	public String getMasterHostName() {
		return replicationMasterHostName;
	}

	@Override
	public int getMasterPort() {
		return replicationMasterPort;
	}
	
	@Override
	public Status getStatus() {
		return status;
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
		this.status = Status.READYTOSTART;
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
		this.status = Status.READYTOSTART;
	}
	
	@Override
    public void start() throws Exception {
    	if (this.status != Status.READYTOSTART) throw new IllegalStateException("must call create Client or Maste before starting");
		this.status = Status.STARTING;
		try {
	    	if (bIsMaster) {
	    		replMaster = new OAReplicationMaster(syncServer, tLogFileName);
	    		replMaster.start();
	    	}
	    	else {
	    		replClient = new OAReplicationClient(tLogFileName, guid, syncServer, replicationMasterHostName, replicationMasterPort);
	    		replClient.start();
	    	}
			this.status = Status.RUNNING;
		}
		finally {
			if (this.status != Status.RUNNING) this.status = Status.READYTOSTART;			
		}
    }

	@Override
    public void stop() throws Exception {
    	if (this.status != Status.RUNNING) return;
    	Status hold = this.status; 
		this.status = Status.STOPPING;
		try {
	    	if (replClient != null) replClient.stop();
	    	if (replMaster != null) replMaster.stop();
			this.status = Status.STOPPED;
		}
		finally {
			if (this.status != Status.STOPPED) this.status = hold;
		}
    }

	@Override
	public boolean isMaster() {
		return (status != Status.UNKNOWN && bIsMaster);
	}
	
	@Override
	public boolean isClient() {
		return (status != Status.UNKNOWN && !bIsMaster);
	}

	@Override
	public abstract void createClient(String guid, String tLogFileName, String replicationMasterHostName, int replicationMasterPort);

	@Override
	public abstract void createMaster(String guid, String tLogFileName);

}
