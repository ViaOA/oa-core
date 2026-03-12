package com.viaoa.repl.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;

@OARemoteInterface()
public interface RemoteReplInterface {

	
	// returns updated yourPositionId
	/**
	 * Sends a Replication message from one server to another. OAReplicationClients and Masters use
	 * this to send messages back and forth.
	 * 
	 * @param myPositionId the qpos from the msg that is being sent.
	 * @param yourLastPositionId the qpos of the last know on the master server.
	 * @param methodName name of method in RemoteSyncInterface that this is to call.
	 * @param args method arguments.
	 * @return new value from receiving qpos 
	 */
	long processMessage(long myPositionId, long yourLastPositionId, String methodName, Object[] args);

	
	// qqqqq client will need to invoke the method in RemoteSyncImpl
	//qqq    master will determine to merge to perform
	
	
}
