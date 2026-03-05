package com.remodel.model.delegate;

import com.remodel.model.oa.JsonColumn;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

public class HubAODelegate {

	// TEMP for v4.0 phase 4
	
	public static void warnOnSettingAO(Hub h1) {
		((HubService) OARuntime.graph(JsonColumn.class).hubs()).getHubAOService().warnOnSettingAO(h1);		
	}
	
}
