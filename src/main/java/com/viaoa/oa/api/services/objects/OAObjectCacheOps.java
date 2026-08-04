package com.viaoa.oa.api.services.objects;

import com.viaoa.object.OAObject;

/**
 * Public OA object-cache service operations.
 * <p>
 * This is the curated service-facing cache boundary for application and
 * advanced OA runtime use. Methods should be added here only when cache
 * behavior is intended to be supported through {@code OA.services()} rather
 * than through internal OA runtime APIs.
 */
public interface OAObjectCacheOps {

	// Public cache service methods will be added as they become supported OA service API.
	
	public OAObject find(Class<? extends  OAObject> clazz, String path, Object findObject);

}
