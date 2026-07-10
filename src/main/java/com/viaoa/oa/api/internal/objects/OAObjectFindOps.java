package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

/**
 * Internal property-path search operation for OA model object relationships.
 */
public interface OAObjectFindOps {

	/**
	 * Finds cached objects using a finder, filter, or property-path search.
	 *
	 * @return the first matching object, matching array, or {@code null} depending on overload
	 */
	public OAObject[] find(OAObject base, String path, Object findValue, boolean bFindAll);
}
