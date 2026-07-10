package com.viaoa.oa.api.internal.objects;

import com.viaoa.cascade.OACascade;
import com.viaoa.object.OAObject;

/**
 * Internal save operations for OAObject cascade persistence.
 */
public interface OAObjectSaveOps {
	/**
	 * Saves an object using the supplied cascade rule.
	 *
	 * @param oaObj the object to save
	 * @param iCascadeRule the cascade rule
	 */
	public void save(OAObject oaObj, int iCascadeRule);
	/**
	 * Saves an object using the supplied cascade rule and cascade context.
	 *
	 * @param obj the object to save
	 * @param iCascadeRule the cascade rule
	 * @param cascade the cascade context
	 */
	public void save(OAObject obj, int iCascadeRule, OACascade cascade);
}
