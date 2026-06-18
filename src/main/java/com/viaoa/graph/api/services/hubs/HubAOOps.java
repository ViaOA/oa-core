package com.viaoa.graph.api.services.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAObject;

public interface HubAOOps {
	
	public <T extends OAObject> HubListenerAdapter<T> keepActiveObject(final Hub<T> thisHub);
	public <T extends OAObject> void setActiveObject(final Hub<T> thisHub, T object, final int pos, final boolean bUpdateLink, final boolean bForce,
			final boolean bCalledByShareHub, final boolean bUpdateSharedHubDetail);

	public <T extends OAObject> void setActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce);
	
	public <T extends OAObject> void updateDetailHubs(final Hub<T> thisHub);
}
