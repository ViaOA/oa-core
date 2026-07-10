package com.viaoa.oa.api.services.objects;

import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;

/**
 * Public OA object-delete service operations.
 * <p>
 * This interface exposes curated delete-related metadata checks that are safe
 * for application and advanced OA service callers. Lower-level delete-state,
 * synchronization, and runtime lifecycle operations remain under
 * {@code OA.internal()}.
 */
public interface OAObjectDeleteOps {

	/**
	 * Returns links that must be empty before an object can be deleted.
	 * <p>
	 * The returned metadata identifies model relationships that block delete
	 * processing until their related contents have been removed or otherwise
	 * resolved.
	 *
	 * @param oaObj the object being evaluated for delete
	 * @return the link metadata that must be empty before delete can proceed
	 */
	public OALinkInfo[] getMustBeEmptyBeforeDelete(OAObject oaObj);
}
