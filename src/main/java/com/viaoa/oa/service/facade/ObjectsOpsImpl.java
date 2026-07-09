package com.viaoa.oa.service.facade;

import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.oa.api.services.ObjectsOps;
import com.viaoa.oa.api.services.objects.OAObjectCacheOps;
import com.viaoa.oa.api.services.objects.OAObjectDeleteOps;
import com.viaoa.oa.api.services.objects.OAObjectReflectOps;
import com.viaoa.oa.service.object.OAObjectParentService;
import com.viaoa.object.OAObject;

public class ObjectsOpsImpl implements ObjectsOps {
	private final OAObjectParentService srvc;
	
	private OAObjectCacheOps opsCache;
	private OAObjectReflectOps opsReflect;
	private OAObjectDeleteOps opsDelete;
	
	public ObjectsOpsImpl(OAObjectParentService srvcObjectParent) {
		this.srvc = srvcObjectParent;
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
	public OAObjectDeleteOps delete() {
		if (opsDelete != null) return opsDelete;
		opsDelete = new OAObjectDeleteOps() {
			@Override
			public OALinkInfo[] getMustBeEmptyBeforeDelete(OAObject oaObj) {
				return srvc.getOAObjectDeleteService().getMustBeEmptyBeforeDelete(oaObj);
			}
		}; 
		return opsDelete;
	}
}
