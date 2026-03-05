package test.hifive.model.delegate;

import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

import test.hifive.model.oa.Ecard;

public class OAObjectCacheDelegate {

	public static <T extends OAObject> T getObject(Class<T> clazz, Object key) {
		return ((OAObjectService) OARuntime.graph(Ecard.class).objects()).getOAObjectCacheService().getObject(clazz, key);
	}

}
