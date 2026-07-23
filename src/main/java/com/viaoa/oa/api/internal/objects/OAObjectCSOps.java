package com.viaoa.oa.api.internal.objects;

import java.util.UUID;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Internal client/server hooks for OAObject reference resolution and distributed object state.
 */
public interface OAObjectCSOps {

	/**
	 * Notifies client/server support that an object identified by GUID has been finalized.
	 *
	 * @param guid the finalized object GUID
	 */
	public void objectFinalized(UUID guid);
	/**
	 * Loads or resolves a server-side reference Hub for an object link.
	 *
	 * @param oaObj the source object
	 * @param linkPropertyName the link property name
	 * @return the resolved server reference Hub
	 */
	public <T extends OAObject> Hub<T> getServerReferenceHub(T oaObj, String linkPropertyName);

	/**
	 * Updates tracking for objects that are not currently referenced by Hubs.
	 *
	 * @param oaObj the object to update
	 */
	public void updateObjectsWithoutHubs(OAObject oaObj);
	
}
