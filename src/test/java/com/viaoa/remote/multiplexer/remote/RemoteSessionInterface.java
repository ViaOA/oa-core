package com.viaoa.remote.multiplexer.remote;

import com.viaoa.remote.annotation.OARemoteInterface;

@OARemoteInterface()
public interface RemoteSessionInterface {

    String ping(String msg);
    
    
}
