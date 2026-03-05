package test.xice.tsac.model.delegate;

import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.runtime.OARuntime;

import test.xice.tsac.model.oa.GSMRClient;

public class OAObjectInfoDelegate {

	// TEMP for v4.0 phase 4
	
	public static void setPrimitiveNull(OAObject oaObj, String propertyName, boolean bSetToNull) {
		((OAObjectService) OARuntime.graph(GSMRClient.class).objects()).getOAObjectInfoService().setPrimitiveNull(oaObj, propertyName, bSetToNull);		
	}
	
	public static OAObjectInfo getObjectInfo(Class<?> c) {
		return ((OAObjectService) OARuntime.graph(GSMRClient.class).objects()).getOAObjectInfoService().getObjectInfo(c);		
	}

	
}
