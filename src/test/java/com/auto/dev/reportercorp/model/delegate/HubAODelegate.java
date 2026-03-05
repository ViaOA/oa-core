package com.auto.dev.reportercorp.model.delegate;

import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

public class HubAODelegate {

	// TEMP for v4.0 phase 4
	
	public static void warnOnSettingAO(Hub h1) {
		((HubService) OARuntime.graph(ReporterCorp.class).hubs()).getHubAOService().warnOnSettingAO(h1);		
	}
	
}
