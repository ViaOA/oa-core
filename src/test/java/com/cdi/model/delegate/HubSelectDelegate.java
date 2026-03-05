package com.cdi.model.delegate;

import com.cdi.model.oa.WorkOrderPallet;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

public class HubSelectDelegate {

	// TEMP for v4.0 phase 4
	
	public static boolean adoptWhereHub(final Hub<?> thisHub, final String propName, final Hub<?> hubFrom) {
		return ((HubService) OARuntime.graph(WorkOrderPallet.class).hubs()).getHubSelectService().adoptWhereHub(thisHub, propName, hubFrom);		
	}

}
