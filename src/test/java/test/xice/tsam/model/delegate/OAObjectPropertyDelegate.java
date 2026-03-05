package test.xice.tsam.model.delegate;

import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

import test.hifive.model.oa.Ecard;
import test.xice.tsac.model.oa.GSMRClient;
import test.xice.tsam.model.oa.Environment;

public class OAObjectPropertyDelegate {

	public static void setProperty(OAObject oaObj, String name, Object value) {
		((OAObjectService) OARuntime.graph(GSMRClient.class).objects()).getOAObjectPropertyService().setProperty(oaObj, name, value);		
	}

	public static Object getProperty(OAObject oaObj, String name) {
		return ((OAObjectService) OARuntime.graph(GSMRClient.class).objects()).getOAObjectPropertyService().getProperty(oaObj, name);		
	}

	public static void removeProperty(OAObject oaObj, String name, boolean bFirePropertyChange) {
		((OAObjectService) OARuntime.graph(GSMRClient.class).objects()).getOAObjectPropertyService().removeProperty(oaObj, name, bFirePropertyChange);		
		
	}

	public static void unsafeSetProperty(OAObject oaObj, String name, Object value) {
		((OAObjectService) OARuntime.graph(GSMRClient.class).objects()).getOAObjectPropertyService().unsafeSetProperty(oaObj, name, value);		
	}

}
