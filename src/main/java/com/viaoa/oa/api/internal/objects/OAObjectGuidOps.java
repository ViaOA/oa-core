package com.viaoa.oa.api.internal.objects;

import java.util.UUID;

import com.viaoa.object.OAObject;

/**
 * Internal access to the runtime GUID assigned to OAObject instances.
 */
public interface OAObjectGuidOps {

	/**
	 * Sets the runtime GUID for an object.
	 *
	 * @param oaObj the object to update
	 * @param iguid the GUID to assign
	 */
	public void setGuid(OAObject oaObj, UUID iguid);
	/**
	 * Returns the runtime GUID for an object.
	 *
	 * @param oaObj the object to inspect
	 * @return the object GUID
	 */
	public UUID getGuid(OAObject oaObj);
	
}
