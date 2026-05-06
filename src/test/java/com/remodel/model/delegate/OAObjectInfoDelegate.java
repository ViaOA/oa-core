package com.remodel.model.delegate;

import com.remodel.model.oa.JsonColumn;
import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

public class OAObjectInfoDelegate {

	// TEMP for v4.0 phase 4
	
	public static void setPrimitiveNull(OAObject oaObj, String propertyName, boolean bSetToNull) {
		((OAObjectService) OARuntime.graph(JsonColumn.class).objects()).getOAObjectInfoService().setPrimitiveNull(oaObj, propertyName, bSetToNull);		
	}
	
	public static OAObjectInfo getObjectInfo(Class<?> c) {
		return ((OAObjectService) OARuntime.graph(JsonColumn.class).objects()).getOAObjectInfoService().getObjectInfo(c);		
	}

	
}
