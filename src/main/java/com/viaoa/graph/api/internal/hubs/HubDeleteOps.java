package com.viaoa.graph.api.internal.hubs;

import com.viaoa.hub.Hub;

public interface HubDeleteOps {

	
	public void deleteAll(Hub<?> hub);
	public boolean isDeletingAll(Hub<?> hub);

}
