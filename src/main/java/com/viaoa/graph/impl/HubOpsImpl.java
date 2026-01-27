package com.viaoa.graph.impl;

import com.viaoa.graph.HubService;
import com.viaoa.graph.api.HubOps;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACascade;

public class HubOpsImpl implements HubOps {

	private HubService srvcHub;
	
	public HubOpsImpl(HubService srvcHub) {
		this.srvcHub = srvcHub;
	}

	@Override
	public void save(Hub hub, int iCascadeRule, OACascade cascade) {
		srvcHub.getHubSaveService().saveAll(hub, iCascadeRule, cascade);
	}
}
