package test.hifive.model.delegate;

import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectModel;
import com.viaoa.runtime.OARuntime;
import test.hifive.model.oa.Ecard;


public class OAObjectCallbackDelegate {

	// TEMP for v4.0 phase 4
	
	public static void onObjectCallbackModel(Class<? extends OAObject> clazz, String property, OAObjectModel model) {
		((OAObjectService) OARuntime.graph(Ecard.class).objects()).getOAObjectCallbackService().onObjectCallbackModel(clazz, property, model);		
	}

}
