package com.viaoa.repl.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

@OARemoteInterface()
public interface RemoteMasterInterface {

    // used by Client to send msg to Master
    @OARemoteMethod() 
	void processMessage(long posMaster, long posClient, String methodName, Object[] args);

}

