package com.auto.dev.reportercorp.model.delegate;

import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

public class OAObjectInfoDelegate {

	// TEMP for v4.0 phase 4
	
	public static void setPrimitiveNull(OAObject oaObj, String propertyName, boolean bSetToNull) {
		((OAObjectService) OARuntime.graph(ReporterCorp.class).objects()).getOAObjectInfoService().setPrimitiveNull(oaObj, propertyName, bSetToNull);		
	}
	
	
}
