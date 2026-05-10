package com.viaoa.replication.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

@OARemoteInterface()
public interface RemoteClientInterface {

	// called by Master to process msg on Client
    @OARemoteMethod() 
    void processMessage(long masterSeq, String methodName, Object[] args);
	
}

