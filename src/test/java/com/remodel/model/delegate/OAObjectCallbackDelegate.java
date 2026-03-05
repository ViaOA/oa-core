package com.remodel.model.delegate;

import com.remodel.model.oa.JsonColumn;
import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectModel;
import com.viaoa.runtime.OARuntime;

public class OAObjectCallbackDelegate {

	// TEMP for v4.0 phase 4
	
	public static void onObjectCallbackModel(Class<? extends OAObject> clazz, String property, OAObjectModel model) {
		((OAObjectService) OARuntime.graph(JsonColumn.class).objects()).getOAObjectCallbackService().onObjectCallbackModel(clazz, property, model);		
	}

}
