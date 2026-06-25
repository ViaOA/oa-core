package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;

public interface HubDeleteOps {

	
	public void deleteAll(Hub<?> hub);
	public boolean isDeletingAll(Hub<?> hub);

}
