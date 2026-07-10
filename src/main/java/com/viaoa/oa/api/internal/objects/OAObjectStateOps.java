package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

/**
 * Internal lifecycle-state operations for OAObject instances.
 */
public interface OAObjectStateOps {

	/**
	 * Sets the new-state flag for an object.
	 *
	 * @param oaObj the object to update
	 * @param bIsNew {@code true} to mark the object new
	 */
	public void setNew(OAObject oaObj, boolean bIsNew);
	

}


