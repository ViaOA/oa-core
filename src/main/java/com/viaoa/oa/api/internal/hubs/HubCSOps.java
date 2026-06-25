package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;

public interface HubCSOps {

	
	public void sendRefresh(Hub<?> hub);
	public boolean isServer(Hub<?> hub);
	public boolean isClient(Hub<?> hub);
	
}
