package com.viaoa.graph.api;

import com.viaoa.object.OACascade;
import com.viaoa.sync.model.ClientInfo;

public interface SyncOps {

	
	public boolean isServer();
	public boolean isClient();
	public boolean isSingleUser();
	
	public int getConnectionId();

	public boolean isConnected();
	
	public void sendException(String msg, Throwable ex);
	
	public ClientInfo getClientInfo();

	public void updateClientInfo(ClientInfo ci);
	
	public void saveCache( OACascade cascade, int iCascadeRule);

	public void performDGC();
	
}

