package com.viaoa.graph.impl;

import com.viaoa.graph.HubService;
import com.viaoa.graph.api.HubOps;

public class HubOpsImpl implements HubOps {

	private HubService srvcHub;
	
	public HubOpsImpl(HubService srvcHub) {
		this.srvcHub = srvcHub;
	}
}
