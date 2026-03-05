package com.auto.dev.reportercorp.model.delegate;

import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

public class HubSelectDelegate {

	// TEMP for v4.0 phase 4
	
	public static boolean adoptWhereHub(final Hub<?> thisHub, final String propName, final Hub<?> hubFrom) {
		return ((HubService) OARuntime.graph(ReporterCorp.class).hubs()).getHubSelectService().adoptWhereHub(thisHub, propName, hubFrom);		
	}

}
