package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

/**
 * Internal OAObject initialization hooks used during construction and loading.
 */
public interface OAObjectInitializeOps {

	/**
	 * Initializes an OAObject after construction.
	 *
	 * @param oaObj the object to initialize
	 * @return {@code true} if initialization completed
	 */
	public boolean initialize(OAObject oaObj);
	/**
	 * Initializes an OAObject after it has been loaded.
	 *
	 * @param oaObj the loaded object
	 */
	public void initializeAfterLoading(OAObject oaObj);
	/**
	 * Initializes a loaded object with explicit id, null-initialization, and change-state options.
	 *
	 * @param oaObj the loaded object
	 * @param bAssignNewId {@code true} to assign a new id
	 * @param bInitializeNulls {@code true} to initialize null values
	 * @param bSetChangedToFalse {@code true} to clear changed state
	 */
	public void initializeAfterLoading(OAObject oaObj, boolean bAssignNewId, boolean bInitializeNulls, boolean bSetChangedToFalse);
	
}
