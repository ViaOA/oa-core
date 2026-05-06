package com.corptostore.model.delegate;


import com.corptostore.model.oa.Store;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.runtime.OARuntime;
import com.viaoa.object.OAObject;

public class HubDetailDelegate {

	// TEMP for v4.0 phase 4

	public static <T extends OAObject> OALinkInfo getLinkInfoFromDetailToMaster(Hub<T> hub) {
		return ((HubService) OARuntime.graph(Store.class).hubs()).getHubDetailService().getLinkInfoFromDetailToMaster(hub);		
	}

	public static <T extends OAObject> OALinkInfo callDetailGetLinkInfoFromDetailToMaster(Hub<T> hub) {
		return ((HubService) OARuntime.graph(Store.class).hubs()).getHubDetailService().getLinkInfoFromDetailToMaster(hub);		
	}
	
	public static boolean getIsFromSameMasterHub(Hub<?> hub1, Hub<?> hub2) {
		return ((HubService) OARuntime.graph(Store.class).hubs()).getHubDetailService().getIsFromSameMasterHub(hub1, hub2);		
	}

	public static boolean getIsValidRecursive(final Hub<?> hub) {
		return ((HubService) OARuntime.graph(Store.class).hubs()).getHubDetailService().getIsValidRecursive(hub);		
	}


}
