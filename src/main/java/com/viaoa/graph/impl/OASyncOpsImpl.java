package com.viaoa.graph.impl;

import com.viaoa.graph.OASyncService;
import com.viaoa.graph.api.OASyncOps;
import com.viaoa.object.OACascade;
import com.viaoa.sync.model.ClientInfo;
import com.viaoa.sync.remote.RemoteSessionInterface;

public class OASyncOpsImpl implements OASyncOps {

	private OASyncService srvcOASync;
	
	public OASyncOpsImpl(OASyncService srvc) {
		this.srvcOASync = srvc;;
	}

	@Override
	public boolean isServer() {
		return srvcOASync.isServer();
	}

	@Override
	public boolean isClient() {
		return srvcOASync.isClient();
	}

	@Override
	public boolean isSingleUser() {
		return srvcOASync.isSingleUser();
	}
	
	@Override
	public int getConnectionId() {
		return this.srvcOASync.getConnectionId();
	}

	@Override
	public boolean isConnected() {
		return this.srvcOASync.isConnected();
	}

	@Override
	public void sendException(String msg, Throwable ex) {
		RemoteSessionInterface rci = srvcOASync.getRemoteSession();
		if (rci != null) {
			rci.sendException("client exception: " + msg, ex);
		}
	}

	@Override
	public ClientInfo getClientInfo() {
		return srvcOASync.getSyncClient().getClientInfo();
	}

	@Override
	public void updateClientInfo(ClientInfo ci) {
		RemoteSessionInterface rsi = srvcOASync.getRemoteSession();
		if (rsi != null) {
			rsi.update(ci);
		}

	}

	@Override
	public void saveCache( OACascade cascade, int iCascadeRule) {
		srvcOASync.getSyncServer().saveCache(cascade, iCascadeRule);		
	}

	@Override
	public void performDGC() {
		srvcOASync.getSyncServer().performDGC();
	}

	
}









