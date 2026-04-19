package com.viaoa.repl.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

@OARemoteInterface()
public interface RemoteMasterInterface {

    // used by Client to send msg to Master
    @OARemoteMethod() 
	void processMessage(long masterSeq, long clientSeq, String methodName, Object[] args);

    @OARemoteMethod() 
	long getLastReceivedClientSeq();
    
    @OARemoteMethod() 
	long getLastProcessedClientSeq();


    @OARemoteMethod() 
	long getLastReceivedMasterSeq();

    @OARemoteMethod() 
	void setLastReceivedMasterSeq(long seq);
    
    @OARemoteMethod() 
	long getLastProcessedMasterSeq();
    
    @OARemoteMethod() 
	void setEnabled(boolean b);
    
    @OARemoteMethod() 
	boolean getEnabled();
}

