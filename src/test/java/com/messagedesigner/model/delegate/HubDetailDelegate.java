package com.messagedesigner.model.delegate;

import com.messagedesigner.model.oa.JsonType;
import com.messagedesigner.model.oa.MessageGroup;
import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

public class HubDetailDelegate {

	// TEMP for v4.0 phase 4

	public static <T extends OAObject> OALinkInfo getLinkInfoFromDetailToMaster(Hub<T> hub) {
		return ((HubService) OARuntime.graph(JsonType.class).hubs()).getHubDetailService().getLinkInfoFromDetailToMaster(hub);		
	}

	public static boolean getIsFromSameMasterHub(Hub<?> hub1, Hub<?> hub2) {
		return ((HubService) OARuntime.graph(JsonType.class).hubs()).getHubDetailService().getIsFromSameMasterHub(hub1, hub2);		
	}


}
