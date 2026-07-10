package com.viaoa.replication.remote;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;
import com.viaoa.remote.multiplexer.annotation.OARemoteParameter;

@OARemoteInterface()
/**
 * Remote lookup interface used by clients to register with a replication master.
 */
public interface RemoteMasterRegisterInterface {


	/**
	 * Registers a replication client with the master and returns a client-specific master session.
	 *
	 * @param guid client replication identifier
	 * @param remoteClient callback used by the master to send messages to the client
	 * @param lastSentMasterSeq last master sequence known by the client
	 * @param lastSentClientSeq last client sequence known by the master/client handshake
	 * @return remote master session for this client
	 */
    @OARemoteMethod() 
	RemoteMasterInterface registerClient(String guid, @OARemoteParameter() RemoteClientInterface remoteClient, long lastSentMasterSeq, long lastSentClientSeq);

}

