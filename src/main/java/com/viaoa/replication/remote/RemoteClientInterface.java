package com.viaoa.replication.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

@OARemoteInterface()
/**
 * Remote callback implemented by a replication client so the master can deliver replication messages.
 */
public interface RemoteClientInterface {

	// called by Master to process msg on Client
    @OARemoteMethod() 
    /**
     * Processes a replication message sent by the master.
     *
     * @param masterSeq master sequence number
     * @param methodName remote sync method name
     * @param args remote sync method arguments
     */
    void processMessage(long masterSeq, String methodName, Object[] args);
	
}

