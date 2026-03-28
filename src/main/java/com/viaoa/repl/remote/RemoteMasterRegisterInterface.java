package com.viaoa.repl.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;
import com.viaoa.remote.multiplexer.annotation.OARemoteParameter;

@OARemoteInterface()
public interface RemoteMasterRegisterInterface {


	/**
	 * Used to register a OAReplicationClient with OAReplicationMaster
	 * @param guid client unique name
	 * @param remoteClient used by Master to send messages to client.
	 * @param masterSeq last known master seq
	 * @param clientSeq last known client seq
	 * @return
	 */
    @OARemoteMethod() 
	RemoteMasterInterface registerClient(String guid, @OARemoteParameter() RemoteClientInterface remoteClient, long lastSentMasterSeq, long lastSentClientSeq);

}

