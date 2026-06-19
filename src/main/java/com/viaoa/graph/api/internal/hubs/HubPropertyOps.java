package com.viaoa.graph.api.internal.hubs;

import com.viaoa.hub.Hub;

public interface HubPropertyOps {

	
	public void setProperty(Hub<?> hub, String name, Object obj);
	public Object getProperty(Hub<?> hub, String name);
	public void removeProperty(Hub<?> hub, String name);
	public void setUniqueProperty(Hub<?> hub, String propertyName);

}
