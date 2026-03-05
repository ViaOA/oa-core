package test.hifive.model.delegate;

import com.viaoa.graph.service.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;
import test.hifive.model.oa.Ecard;

public class HubSelectDelegate {

	// TEMP for v4.0 phase 4
	
	public static boolean adoptWhereHub(final Hub<?> thisHub, final String propName, final Hub<?> hubFrom) {
		return ((HubService) OARuntime.graph(Ecard.class).hubs()).getHubSelectService().adoptWhereHub(thisHub, propName, hubFrom);		
	}

}
