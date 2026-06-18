package com.viaoa.graph.api.services.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public interface HubDataOps {
	
	public <T extends OAObject> int getPos(final Hub<T> thisHub, Object object, final boolean adjustMaster, final boolean bUpdateLink);
	
}
