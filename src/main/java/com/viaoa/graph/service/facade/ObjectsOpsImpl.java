package com.viaoa.graph.service.facade;

import com.viaoa.graph.api.services.ObjectsOps;
import com.viaoa.graph.api.services.objects.OAObjectCacheOps;
import com.viaoa.graph.api.services.objects.OAObjectCallbackOps;
import com.viaoa.graph.api.services.objects.OAObjectReflectOps;
import com.viaoa.graph.service.OAObjectInternalService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.listener.HubChangeListener;
import com.viaoa.object.OAObject;

public class ObjectsOpsImpl implements ObjectsOps {
	private final OAObjectInternalService srvc;
	
	private OAObjectCacheOps opsCache;
	private OAObjectReflectOps opsReflect;
	private OAObjectCallbackOps opsCallback;
	
	public ObjectsOpsImpl(OAObjectInternalService srvcObjectInternal) {
		this.srvc = srvcObjectInternal;
	}

	@Override
	public OAObjectCacheOps cache() {
		if (opsCache != null) return opsCache;
		
		opsCache = new OAObjectCacheOps() {
			//qqqqqqqq add here, using srvc
		};
		return opsCache;
	}

	@Override
	public OAObjectReflectOps reflect() {
		if (opsReflect != null) return opsReflect;
		
		opsReflect = new OAObjectReflectOps() {
			@Override
			public String getPropertyPathFromMaster(OAObject objParent, Hub<?> hubChild) {
				return srvc.getOAObjectReflectService().getPropertyPathFromMaster(objParent, hubChild);
			}

			@Override
			public Object getProperty(OAObject oaObj, String propPath) {
				return srvc.getOAObjectReflectService().getProperty(oaObj, propPath);
			}

			@Override
			public Object getProperty(Hub<?> hub, String propPath) {
				return srvc.getOAObjectReflectService().getProperty(hub, propPath);
			}
		};
		return opsReflect;
	}

	@Override
	public OAObjectCallbackOps callback() {
		if (opsCallback != null) return opsCallback;
		opsCallback = new OAObjectCallbackOps() {
			@Override
			public <T extends OAObject> void addObjectCallbackChangeListeners(Hub<T> hub, Class<T> cz, String prop, String ppPrefix, HubChangeListener changeListener, boolean bEnabled) {
				srvc.getOAObjectCallbackService().addObjectCallbackChangeListeners(hub, cz, prop, ppPrefix, changeListener, bEnabled);
			}
		};
		return opsCallback;
	}

}


