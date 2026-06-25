package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public interface HubFindOps {

	
	public <T extends OAObject> T findFirst(Hub<T> hub, String propertyPath, Object findValue, boolean bSetAO, T lastFoundObject);

}
