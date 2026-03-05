package com.cdi.model.delegate;

import com.cdi.model.oa.WorkOrderPallet;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

public class HubDelegate {

	// TEMP for v4.0 phase 4
	
	public static <T extends OAObject> void setObjectClass(Hub<T> thisHub, Class<T> objClass) {
		((HubService) OARuntime.graph(WorkOrderPallet.class).hubs()).getHubDataService().setObjectClass(thisHub, objClass);		
	}

}
