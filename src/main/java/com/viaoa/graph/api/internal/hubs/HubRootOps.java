package com.viaoa.graph.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAObject;

public interface HubRootOps {
	
	public <T extends OAObject> Hub<T> getRootHub(Hub<T> hub);
	public void setRootHub(Hub<?> hub, boolean bIsRoot);

}
