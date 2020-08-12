package com.viaoa.remote.multiplexer.remote;

import com.viaoa.annotation.OAMethod;
import com.viaoa.remote.annotation.OARemoteInterface;
import com.viaoa.remote.annotation.OARemoteMethod;

@OARemoteInterface()
public interface RemoteServerInterface {

    void register(int id, RemoteClientInterface rci);
    
    @OARemoteMethod(noReturnValue=true)
    void registerNoResponse(int id, RemoteClientInterface rci);
    boolean isRegister(int id);

    RemoteSessionInterface getSession(int id);
    
    boolean isStarted();
    
    @OARemoteMethod(noReturnValue=true)
    void pingNoReturn(String msg);


    void registerTest(int id, RemoteClientInterface rci, RemoteBroadcastInterface rbi);

}
