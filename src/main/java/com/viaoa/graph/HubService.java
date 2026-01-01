package com.viaoa.graph;

import java.util.logging.Logger;

import com.viaoa.graph.hub.HubAddRemoveService;
import com.viaoa.graph.hub.HubDataService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubInternalBridge;

public class HubService {
	private static final Logger LOG = Logger.getLogger(HubService.class.getName());

	private final OAGraph graph;

	private final HubInternalBridge faBridge = new HubInternalBridge();
	
	private final Hub.FriendAccess faHub;
	
	private final HubDataService srvcHubData = new HubDataService(this, faBridge.getHubFriendAccess(), faBridge.getHubDataFriendAccess());

	
	private final HubAddRemoveService srvcHubAddRemove = new HubAddRemoveService(this,
			faBridge.getHubFriendAccess(), faBridge.getHubDataFriendAccess(), faBridge.getHubDataUniqueFriendAccess());
	
	
	public HubService(OAGraph graph) {
    	if (graph == null) throw new IllegalArgumentException("graph can not be null");
    	this.graph = graph;
    	this.faHub = faBridge.getHubFriendAccess();
	}

	public HubDataService getHubDataService() {
		return srvcHubData;
	}
	
	public HubAddRemoveService getHubAddRemoveService() {
		return srvcHubAddRemove;
	}
	
	
}
