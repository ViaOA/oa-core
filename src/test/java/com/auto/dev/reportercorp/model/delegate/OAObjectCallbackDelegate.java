package com.auto.dev.reportercorp.model.delegate;

import com.auto.reportercorp.model.pojo.ReporterCorp;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.metadata.OAObjectModel;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

public class OAObjectCallbackDelegate {

	// TEMP for v4.0 phase 4
	
	public static void onObjectCallbackModel(Class<? extends OAObject> clazz, String property, OAObjectModel model) {
		((OAObjectService) OARuntime.graph(ReporterCorp.class).objects()).getOAObjectCallbackService().onObjectCallbackModel(clazz, property, model);		
	}

}
