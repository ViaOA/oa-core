package com.corptostore.model.delegate;

import com.corptostore.model.oa.Store;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

public class HubAODelegate {

	// TEMP for v4.0 phase 4
	
	public static void warnOnSettingAO(Hub h1) {
		((HubService) OARuntime.graph(Store.class).hubs()).getHubAOService().warnOnSettingAO(h1);		
	}
	
}
