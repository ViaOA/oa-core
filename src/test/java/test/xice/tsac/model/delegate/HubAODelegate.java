package test.xice.tsac.model.delegate;

import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;
import test.hifive.model.oa.Ecard;
import test.xice.tsac.model.oa.GSMRClient;

public class HubAODelegate {

	// TEMP for v4.0 phase 4
	
	public static void warnOnSettingAO(Hub h1) {
		((HubService) OARuntime.graph(GSMRClient.class).hubs()).getHubAOService().warnOnSettingAO(h1);		
	}
	
}
