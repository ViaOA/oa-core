package com.viaoa.graph.api.internal.hubs;

import com.viaoa.hub.Hub;

public interface HubSizeOps {

	public int getSize(Hub<?> hub);
	public int getLoadedSize(Hub<?> hub);

}
