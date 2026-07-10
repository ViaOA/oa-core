package com.viaoa.oa.api.internal.objects;

import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;

/**
 * Internal delete-state and sync-delete operations for OAObject instances.
 */
public interface OAObjectDeleteOps {

	/**
	 * Returns links that must be empty before the object can be deleted.
	 *
	 * @param oaObj the object being deleted
	 * @return the links that must be empty
	 */
	public OALinkInfo[] getMustBeEmptyBeforeDelete(OAObject oaObj);
	
	/**
	 * Sets the internal deleted flag for an object.
	 *
	 * @param oaObj the object to update
	 * @param bDeleted {@code true} to mark deleted
	 */
	public void setDeleted(OAObject oaObj, boolean bDeleted);
	/**
	 * Deletes an object using internal delete handling.
	 *
	 * @param oaObj the object to delete
	 */
	public void delete(OAObject oaObj);
	/**
	 * Applies server-side delete synchronization for an object.
	 *
	 * @param obj the deleted object
	 */
	public void syncServerDelete(OAObject obj);
	/**
	 * Applies client-side delete synchronization for an object.
	 *
	 * @param obj the deleted object
	 */
	public void syncClientDelete(OAObject obj);
	
}
