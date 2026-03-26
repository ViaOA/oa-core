package com.viaoa.repl.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

@OARemoteInterface()
public interface RemoteMasterInterface {

    // used by Client to send msg to Master
    @OARemoteMethod() 
	void processMessage(long masterSeq, long clientSeq, String methodName, Object[] args);

    @OARemoteMethod() 
	long getMinimumClientSeq();
}

