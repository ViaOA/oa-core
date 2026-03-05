package test.xice.tsac.model.delegate;

import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import test.xice.tsac.model.oa.GSMRClient;


public class HubDetailDelegate {

	// TEMP for v4.0 phase 4

	public static <T extends OAObject> OALinkInfo getLinkInfoFromDetailToMaster(Hub<T> hub) {
		return ((HubService) OARuntime.graph(GSMRClient.class).hubs()).getHubDetailService().getLinkInfoFromDetailToMaster(hub);		
	}

	public static <T extends OAObject> OALinkInfo callDetailGetLinkInfoFromDetailToMaster(Hub<T> hub) {
		return ((HubService) OARuntime.graph(GSMRClient.class).hubs()).getHubDetailService().getLinkInfoFromDetailToMaster(hub);		
	}
	
	public static boolean getIsFromSameMasterHub(Hub<?> hub1, Hub<?> hub2) {
		return ((HubService) OARuntime.graph(GSMRClient.class).hubs()).getHubDetailService().getIsFromSameMasterHub(hub1, hub2);		
	}

	public static boolean getIsValidRecursive(final Hub<?> hub) {
		return ((HubService) OARuntime.graph(GSMRClient.class).hubs()).getHubDetailService().getIsValidRecursive(hub);		
	}


}
