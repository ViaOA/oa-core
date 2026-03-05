package com.messagedesigner.model.delegate;

import com.messagedesigner.model.oa.JsonType;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

public class HubAODelegate {

	// TEMP for v4.0 phase 4
	
	public static void warnOnSettingAO(Hub h1) {
		// TODO Auto-generated method stub
		((HubService) OARuntime.graph(JsonType.class).hubs()).getHubAOService().warnOnSettingAO(h1);		
	}
	
}
