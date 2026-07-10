package com.viaoa.oa.api;

import com.viaoa.sync.OASyncServer;

/**
 * Public OA replication operations.
 * <p>
 * Replication is a runtime role used to coordinate persisted changes between a
 * master and one or more clients. This interface exposes the caller-visible
 * lifecycle and configuration state for that replication role.
 */
public interface ReplicationOps {

	/**
	 * Configures this runtime as the replication master.
	 *
	 * @param guid the replication identity
	 * @param tLogFileName the transaction log file name
	 */
	public void createMaster(String guid, String tLogFileName);

	/**
	 * Configures this runtime as a replication client.
	 *
	 * @param guid the replication identity
	 * @param logFileName the local replication log file name
	 * @param masterHostName the master host name
	 * @param masterPort the master port
	 */
	public void createClient(String guid, String logFileName, String masterHostName, int masterPort);
	
	/**
	 * Returns whether this runtime is configured as the replication master.
	 *
	 * @return {@code true} if configured as master
	 */
	public boolean isMaster();

	/**
	 * Returns whether this runtime is configured as a replication client.
	 *
	 * @return {@code true} if configured as client
	 */
	public boolean isClient();

	/**
	 * Starts replication for the configured role.
	 *
	 * @throws Exception if replication cannot be started
	 */
	public void start() throws Exception;

	/**
	 * Stops active replication.
	 *
	 * @throws Exception if replication cannot be stopped
	 */
	public void stop() throws Exception;
	
	
    /**
     * Replication lifecycle status.
     */
    public enum Status {
		/** Status has not been determined. */
    	UNKNOWN,
		/** Replication is configured and ready to start. */
    	READYTOSTART,
        /** Replication is starting. */
        STARTING,
        /** Replication is running. */
        RUNNING,
        /** Replication is stopping. */
        STOPPING,
        /** Replication is stopped. */
        STOPPED
    }
	/**
	 * Returns the current replication status.
	 *
	 * @return the replication status
	 */
	public Status getStatus(); 

	/**
	 * Returns the replication log file name.
	 *
	 * @return the log file name
	 */
	public String getLogFileName();

	/**
	 * Returns the replication master host name for client configuration.
	 *
	 * @return the master host name
	 */
	public String getMasterHostName();

	/**
	 * Returns the replication master port for client configuration.
	 *
	 * @return the master port
	 */
	public int getMasterPort();
	
}

