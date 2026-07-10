package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

/**
 * Internal datasource identity hooks used when OAObjects are created or assigned persistent ids.
 */
public interface OAObjectDSOps {

	/**
	 * Returns whether an object should receive a datasource id when it is created.
	 *
	 * @param oaObj the object to inspect
	 * @return {@code true} if id assignment occurs on create
	 */
	public boolean getAssignIdOnCreate(OAObject oaObj);
	/**
	 * Assigns a datasource identity to an object.
	 *
	 * @param oaObj the object that needs an id
	 */
	public void assignId(OAObject oaObj);
	/**
	 * Marks whether an object is currently in datasource id assignment.
	 *
	 * @param oaObj the object to update
	 * @param bIsAssigningId {@code true} while id assignment is in progress
	 */
	public void setAssigningId(OAObject oaObj, boolean bIsAssigningId);

}
