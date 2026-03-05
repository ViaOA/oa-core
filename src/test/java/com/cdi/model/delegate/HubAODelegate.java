package com.cdi.model.delegate;

import com.cdi.model.oa.WorkOrderPallet;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

public class HubAODelegate {

	// TEMP for v4.0 phase 4
	
	public static void warnOnSettingAO(Hub h1) {
		((HubService) OARuntime.graph(WorkOrderPallet.class).hubs()).getHubAOService().warnOnSettingAO(h1);		
	}
	
}
