package com.viaoa.graph.api.internal.objects;

import com.viaoa.hub.Hub;
import com.viaoa.lang.oa.VEnum;
import com.viaoa.object.OAObject;

public interface OAObjectEnumOps {

	public Hub<VEnum> getVEnums(Class<? extends OAObject> clazz, String propertyName);
	
}
