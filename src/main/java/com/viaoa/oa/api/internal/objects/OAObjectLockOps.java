package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

/**
 * Internal object and property lock operations.
 */
public interface OAObjectLockOps {

	/**
	 * Locks an object for internal OA processing.
	 *
	 * @param oaObj the object to lock
	 */
	public void lock(OAObject oaObj);
	/**
	 * Unlocks an object.
	 *
	 * @param oaObj the object to unlock
	 */
	public void unlock(OAObject oaObj);
	/**
	 * Returns whether an object is locked.
	 *
	 * @param oaObj the object to inspect
	 * @return {@code true} if locked
	 */
	public boolean isLocked(OAObject oaObj);
	/**
	 * Returns whether a property is locked.
	 *
	 * @param oaObj the object to inspect
	 * @param name the property name
	 * @return {@code true} if the property is locked
	 */
	public boolean isPropertyLocked(OAObject oaObj, String name);
	
}
