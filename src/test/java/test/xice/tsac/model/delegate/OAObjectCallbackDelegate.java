package test.xice.tsac.model.delegate;

import com.viaoa.graph.service.OAObjectService;
import com.viaoa.metadata.OAObjectModel;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import test.xice.tsac.model.oa.GSMRClient;

public class OAObjectCallbackDelegate {

	// TEMP for v4.0 phase 4
	
	public static void onObjectCallbackModel(Class<? extends OAObject> clazz, String property, OAObjectModel model) {
		((OAObjectService) OARuntime.graph(GSMRClient.class).objects()).getOAObjectCallbackService().onObjectCallbackModel(clazz, property, model);		
	}

}
