package com.auto.dev.reportercorp.model.delegate;

import com.auto.dev.reportercorp.model.oa.ReportInstance;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

public class HubDetailDelegate {

	// TEMP for v4.0 phase 4

	public static <T extends OAObject> OALinkInfo getLinkInfoFromDetailToMaster(Hub<T> hub) {
		return ((HubService) OARuntime.graph(ReporterCorp.class).hubs()).getHubDetailService().getLinkInfoFromDetailToMaster(hub);		
	}

	public static boolean getIsFromSameMasterHub(Hub<?> hub1, Hub<?> hub2) {
		return ((HubService) OARuntime.graph(ReporterCorp.class).hubs()).getHubDetailService().getIsFromSameMasterHub(hub1, hub2);		
	}

	public static boolean getIsValidRecursive(final Hub<?> hub) {
		return ((HubService) OARuntime.graph(ReporterCorp.class).hubs()).getHubDetailService().getIsValidRecursive(hub);		
	}


}
