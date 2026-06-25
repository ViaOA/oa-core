package com.viaoa.graph.api.internal.hubs;

import com.viaoa.cascade.OACascade;
import com.viaoa.hub.Hub;

public interface HubSaveOps {

	public void saveAll(Hub<?> hub, int cascadeRule);
	void saveAll(Hub<?> thisHub, int iCascadeRule, OACascade cascade);

}
