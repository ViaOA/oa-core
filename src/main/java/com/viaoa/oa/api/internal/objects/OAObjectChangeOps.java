package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

/**
 * Internal change-state access for OAObject save and cascade processing.
 */
public interface OAObjectChangeOps {

	/**
	 * Returns whether the object has changed state according to the supplied cascade rule.
	 *
	 * @param oaObj the object to inspect
	 * @param cascadeRule the cascade rule used when checking related objects
	 * @return {@code true} if the object or included related objects have changes
	 */
	public boolean getChanged(OAObject oaObj, int cascadeRule);
	
	public void setChanged(OAObject oaObj, boolean tf);
	
}
