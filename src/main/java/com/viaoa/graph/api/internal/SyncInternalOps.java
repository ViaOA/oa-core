package com.viaoa.graph.api.internal;

import com.viaoa.graph.api.SyncOps;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.model.ClientInfo;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteServerInterface;

public interface SyncInternalOps extends SyncOps {


//qqqqqqqqqqq make all of the method names "call*"	
	
	
	public boolean isServer();
	public boolean isClient();
	public boolean isSingleUser();
	
	
	public boolean isConnected();
	public int getConnectionId();
	
	public void sendException(String msg, Throwable ex);
	
	public ClientInfo getClientInfo();
	public void updateClientInfo(ClientInfo ci);

	public void saveCache( OACascade cascade, int iCascadeRule);

	
	public void performDGC();
	
	
	public boolean callSyncIsServer();
	
	
	public void callRemoteClientRefresh(Class<? extends OAObject> class1, OAObjectKey objectKey);
	public void callRemoteClientRefresh(Class<? extends OAObject> class1, OAObjectKey objectKey, String linkPropertyName);

//qqqqqq remove	
//	public void sendMessagesXXXX(boolean b);
	
//qqqqqq remove, from *Delegate days qqqqqq	
//	public void setSyncClient(OASyncClient oaSyncClient);
	//public void setSyncServer(OASyncServer oaSyncServer);

//qqqqqqq make these finer grain	
	
	public RemoteClientInterface getRemoteClient();
	public RemoteServerInterface getRemoteServer();
	public OASyncClient getSyncClient();
	
	
}
