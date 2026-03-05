package test.hifive.model.delegate;

import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

import test.hifive.model.oa.Ecard;

public class OAObjectPropertyDelegate {

	public static void setProperty(OAObject oaObj, String name, Object value) {
		((OAObjectService) OARuntime.graph(Ecard.class).objects()).getOAObjectPropertyService().setProperty(oaObj, name, value);		
	}

}
