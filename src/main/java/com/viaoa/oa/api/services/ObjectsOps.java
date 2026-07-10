package com.viaoa.oa.api.services;

import com.viaoa.oa.api.services.objects.*;

/**
 * Public OAObject service families exposed through {@code OA.services().objects()}.
 * <p>
 * This interface is the curated service boundary for object-level operations
 * intended for application and advanced service use. Lower-level object runtime
 * behavior remains under {@code OA.internal().objects()}.
 */
public interface ObjectsOps {

	
	/**
	 * Returns public object-cache service operations.
	 *
	 * @return the cache service facade
	 */
	public OAObjectCacheOps cache();
	
	/**
	 * Returns public object reflection and property-path operations.
	 *
	 * @return the reflection service facade
	 */
	public OAObjectReflectOps reflect();

	/**
	 * Returns public object delete metadata operations.
	 *
	 * @return the delete service facade
	 */
	public OAObjectDeleteOps delete();
	
	
}
