package com.viaoa.graph;

import com.viaoa.hub.Hub;
import com.viaoa.object.OACascade;

public class HubService {
	private final OAGraph graph;

	public HubService(OAGraph graph) {
    	if (graph == null) throw new IllegalArgumentException("graph can not be null");
    	this.graph = graph;
	}

	public boolean getChanged(Hub hub, int cascadeNone, OACascade cascade) {
		// TODO Auto-generated method stub qqqqqqqqqqq
		return false;
	}
	
}
