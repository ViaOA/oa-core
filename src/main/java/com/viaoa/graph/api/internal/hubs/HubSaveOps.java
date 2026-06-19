package com.viaoa.graph.api.internal.hubs;

import com.viaoa.hub.Hub;

public interface HubSaveOps {

	public void saveAll(Hub<?> hub, int cascadeRule);

}
