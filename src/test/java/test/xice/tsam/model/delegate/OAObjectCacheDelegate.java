package test.xice.tsam.model.delegate;

import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import test.xice.tsac.model.oa.GSMRClient;

public class OAObjectCacheDelegate {

	public static <T extends OAObject> T getObject(Class<T> clazz, Object key) {
		return ((OAObjectService) OARuntime.graph(GSMRClient.class).objects()).getOAObjectCacheService().getObject(clazz, key);
	}

}
