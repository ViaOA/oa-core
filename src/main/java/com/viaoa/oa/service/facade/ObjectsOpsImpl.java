package com.viaoa.oa.service.facade;

import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.oa.api.services.ObjectsOps;
import com.viaoa.oa.api.services.objects.OAObjectCacheOps;
import com.viaoa.oa.api.services.objects.OAObjectDeleteOps;
import com.viaoa.oa.api.services.objects.OAObjectReflectOps;
import com.viaoa.oa.service.object.OAObjectParentService;
import com.viaoa.object.OAObject;

/**
 * Public OAObject service facade implementation.
 * <p>
 * This facade exposes curated object service nouns backed by the internal
 * {@link OAObjectParentService}. It keeps application-facing access separate
 * from lower-level OA runtime service wiring.
 * </p>
 */
public class ObjectsOpsImpl implements ObjectsOps {
	private final OAObjectParentService srvc;
	
	private OAObjectCacheOps opsCache;
	private OAObjectReflectOps opsReflect;
	private OAObjectDeleteOps opsDelete;
	
	/**
	 * Creates an object service facade backed by the OA object parent service.
	 *
	 * @param srvcObjectParent parent service that owns the object service family
	 */
	public ObjectsOpsImpl(OAObjectParentService srvcObjectParent) {
		this.srvc = srvcObjectParent;
	}

	/**
	 * Returns cache-related object operations.
	 *
	 * @return object cache operations facade
	 */
	@Override
	public OAObjectCacheOps cache() {
		if (opsCache != null) return opsCache;
		
		opsCache = new OAObjectCacheOps() {
			// Public cache service methods are added here only after becoming supported OA service API.
		};
		return opsCache;
	}

	/**
	 * Returns object reflection and property-path operations.
	 *
	 * @return object reflection operations facade
	 */
	@Override
	public OAObjectReflectOps reflect() {
		if (opsReflect != null) return opsReflect;
		
		opsReflect = new OAObjectReflectOps() {
			@Override
			public String getPathFromMaster(OAObject objParent, Hub<?> hubChild) {
				return srvc.getOAObjectReflectService().getPropertyFromMaster(objParent, hubChild);
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


	/**
	 * Returns delete-related object operations.
	 *
	 * @return object delete operations facade
	 */
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
