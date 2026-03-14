package com.viaoa.repl.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

@OARemoteInterface()
public interface RemoteClientInterface {

	// called by Master to process msg on Client
    @OARemoteMethod() 
    void processMessage(long posMaster, String methodName, Object[] args);
	
}

