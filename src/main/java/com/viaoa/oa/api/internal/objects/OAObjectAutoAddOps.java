package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

/**
 * Internal access to OAObject auto-add state used by ownership and reverse-link processing.
 */
public interface OAObjectAutoAddOps {

	/**
	 * Sets whether the object can be automatically added to owner/reverse-link Hubs.
	 *
	 * @param oaObj the object to update
	 * @param bAutoAdd {@code true} to enable auto-add behavior
	 */
	public void setAutoAdd(OAObject oaObj, boolean bAutoAdd);
	/**
	 * Returns whether the object is marked for automatic owner/reverse-link Hub insertion.
	 *
	 * @param oaObj the object to inspect
	 * @return {@code true} if auto-add is enabled
	 */
	public boolean getAutoAdd(OAObject oaObj);
}
