package test.xice.tsam.model.delegate;

import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import test.xice.tsac.model.oa.GSMRClient;

public class HubDelegate {

	// TEMP for v4.0 phase 4
	
	public static <T extends OAObject> void setObjectClass(Hub<T> thisHub, Class<T> objClass) {
//		((HubService) OARuntime.graph(GSMRClient.class).hubs()).getHubDataService().setObjectClass(thisHub, objClass);		
	}

}
