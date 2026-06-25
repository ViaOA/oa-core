package com.viaoa.oa.api.services.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAObject;

public interface HubRootOps {
	
	public <T extends OAObject> Hub<T> getRootHub(final Hub<T> thisHub);
	
}
