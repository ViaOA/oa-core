package com.messagedesigner.model.delegate;

import com.messagedesigner.model.oa.JsonType;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

public class OAObjectInfoDelegate {

	// TEMP for v4.0 phase 4
	
	public static void setPrimitiveNull(OAObject oaObj, String propertyName, boolean bSetToNull) {
		((OAObjectService) OARuntime.graph(JsonType.class).objects()).getOAObjectInfoService().setPrimitiveNull(oaObj, propertyName, bSetToNull);		
	}
	
	
}
