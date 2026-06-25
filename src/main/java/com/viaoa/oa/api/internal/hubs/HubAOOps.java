package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAObject;

public interface HubAOOps {

	public <T extends OAObject> HubListenerAdapter<T> keepActiveObject(final Hub<T> thisHub);
	public <T extends OAObject> void setActiveObject(final Hub<T> thisHub, T object, final int pos, final boolean bUpdateLink, final boolean bForce,
			final boolean bCalledByShareHub, final boolean bUpdateSharedHubDetail);

	public <T extends OAObject> void setActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce);
	
	public <T extends OAObject> void updateDetailHubs(final Hub<T> thisHub);

	public <T extends OAObject> T setActiveObject(Hub<T> hub, int pos);
	public <T extends OAObject> void setActiveObject(Hub<T> hub, T obj);
	public <T extends OAObject> void setActiveObjectForce(Hub<T> hub, T obj);
	public <T extends OAObject> T setActiveObject(Hub<T> hub, Object obj);

}
