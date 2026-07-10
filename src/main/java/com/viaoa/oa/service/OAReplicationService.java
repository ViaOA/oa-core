package com.viaoa.oa.service;

import com.viaoa.sync.OASyncServer;
import com.viaoa.oa.api.ReplicationOps;
import com.viaoa.oa.api.internal.*;
import com.viaoa.replication.OAReplicationClient;
import com.viaoa.replication.OAReplicationMaster;




/**
 * Coordinates OA replication against an {@link OASyncServer}.
 * <p>
 * The service can be configured as a replication master or client and then
 * started to support offline/reconnect replication workflows.
 * </p>
 */
public abstract class OAReplicationService implements ReplicationInternalOps, ReplicationOps {
	private String guid;
    private OASyncServer syncServer;
    private String tLogFileName;
    private String replicationMasterHostName;
    private int replicationMasterPort;
    private boolean bIsMaster;
    private Status status = Status.UNKNOWN;
	
	private OAReplicationMaster replMaster;
    private OAReplicationClient replClient;

	/**
	 * Creates an unconfigured replication service.
	 */
	public OAReplicationService() {
	}

	
	/**
	 * Returns the replication transaction log file name.
	 *
	 * @return transaction log file name
	 */
	@Override
	public String getLogFileName() {
		return tLogFileName;
	}

	/**
	 * Returns the replication master host name for client mode.
	 *
	 * @return master host name, or {@code null} for master mode
	 */
	@Override
	public String getMasterHostName() {
		return replicationMasterHostName;
	}

	/**
	 * Returns the replication master port for client mode.
	 *
	 * @return master port, or {@code 0} for master mode
	 */
	@Override
	public int getMasterPort() {
		return replicationMasterPort;
	}
	
	/**
	 * Returns the current replication status.
	 *
	 * @return replication status
	 */
	@Override
	public Status getStatus() {
		return status;
	}
	
	/**
	 * Configures this service as a replication master.
	 *
	 * @param guid replication identifier
	 * @param syncServer sync server used by replication
	 * @param tLogFileName transaction log file name
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
	 * Configures this service as a replication client.
	 *
	 * @param guid replication identifier
	 * @param syncServer sync server used by replication
	 * @param tLogFileName transaction log file name
	 * @param replicationMasterHostName replication master host name
	 * @param replicationMasterPort replication master port
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
	
	/**
	 * Starts the configured replication master or client.
	 *
	 * @throws Exception when replication startup fails
	 */
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

	/**
	 * Stops the running replication master or client.
	 *
	 * @throws Exception when replication shutdown fails
	 */
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

	/**
	 * Returns whether this service is configured as replication master.
	 *
	 * @return {@code true} when configured as master
	 */
	@Override
	public boolean isMaster() {
		return (status != Status.UNKNOWN && bIsMaster);
	}
	
	/**
	 * Returns whether this service is configured as replication client.
	 *
	 * @return {@code true} when configured as client
	 */
	@Override
	public boolean isClient() {
		return (status != Status.UNKNOWN && !bIsMaster);
	}

	/**
	 * Configures this service as a replication client using runtime-provided sync wiring.
	 *
	 * @param guid replication identifier
	 * @param tLogFileName transaction log file name
	 * @param replicationMasterHostName replication master host name
	 * @param replicationMasterPort replication master port
	 */
	@Override
	public abstract void createClient(String guid, String tLogFileName, String replicationMasterHostName, int replicationMasterPort);

	/**
	 * Configures this service as a replication master using runtime-provided sync wiring.
	 *
	 * @param guid replication identifier
	 * @param tLogFileName transaction log file name
	 */
	@Override
	public abstract void createMaster(String guid, String tLogFileName);

}
