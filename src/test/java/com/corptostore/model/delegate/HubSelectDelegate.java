package com.corptostore.model.delegate;

import com.corptostore.model.oa.Store;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

public class HubSelectDelegate {

	// TEMP for v4.0 phase 4
	
	public static boolean adoptWhereHub(final Hub<?> thisHub, final String propName, final Hub<?> hubFrom) {
		return ((HubService) OARuntime.graph(Store.class).hubs()).getHubSelectService().adoptWhereHub(thisHub, propName, hubFrom);		
	}

}
