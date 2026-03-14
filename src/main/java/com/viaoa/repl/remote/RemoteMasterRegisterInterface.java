package com.viaoa.repl.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;
import com.viaoa.remote.multiplexer.annotation.OARemoteParameter;

@OARemoteInterface()
public interface RemoteMasterRegisterInterface {

	
    @OARemoteMethod() 
	RemoteMasterInterface registerClient(@OARemoteParameter() RemoteClientInterface remoteClient);

}

