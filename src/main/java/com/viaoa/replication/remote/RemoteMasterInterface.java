package com.viaoa.replication.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

@OARemoteInterface()
/**
 * Remote session interface returned to a registered replication client.
 */
public interface RemoteMasterInterface {

    // used by Client to send msg to Master
    @OARemoteMethod() 
	/**
	 * Processes a client-originated replication message on the master.
	 */
	void processMessage(long masterSeq, long clientSeq, String methodName, Object[] args);

    @OARemoteMethod() 
	/**
	 * Returns the last client sequence received by the master session.
	 */
	long getLastReceivedClientSeq();
    
    @OARemoteMethod() 
	/**
	 * Returns the last client sequence processed by the master session.
	 */
	long getLastProcessedClientSeq();


    @OARemoteMethod() 
	/**
	 * Returns the last master sequence acknowledged by the client.
	 */
	long getLastReceivedMasterSeq();

    @OARemoteMethod() 
	/**
	 * Updates the last master sequence acknowledged by the client.
	 */
	void setLastReceivedMasterSeq(long seq);
    
    @OARemoteMethod() 
	/**
	 * Returns the last master sequence sent or processed for this client session.
	 */
	long getLastProcessedMasterSeq();
    
    @OARemoteMethod() 
	/**
	 * Enables or disables this remote master session.
	 */
	void setEnabled(boolean b);
    
    @OARemoteMethod() 
	/**
	 * Returns whether this remote master session is enabled.
	 */
	boolean getEnabled();
}

