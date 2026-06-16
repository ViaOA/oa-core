package com.viaoa.graph.api.services.objects;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public interface OAObjectReflectOps {

	public String getPropertyPathFromMaster(final OAObject objParent, final Hub<?> hubChild);
	
	public Object getProperty(OAObject oaObj, String propPath);
	
	public Object getProperty(Hub<?> hub, String propPath);
	
}
