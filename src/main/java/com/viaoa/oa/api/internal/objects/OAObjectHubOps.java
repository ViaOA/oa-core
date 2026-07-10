package com.viaoa.oa.api.internal.objects;

import java.lang.ref.WeakReference;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Internal access to the Hubs that currently reference an OAObject.
 */
public interface OAObjectHubOps {

	/**
	 * Returns the internal weak Hub references for an object without copying them.
	 *
	 * @param oaObj the object to inspect
	 * @return the internal weak-reference array
	 */
	public WeakReference<Hub<? extends OAObject>>[] getHubReferencesNoCopy(OAObject oaObj);
	/**
	 * Returns Hubs that currently reference an object.
	 *
	 * @param oaObj the object to inspect
	 * @return the referencing Hubs
	 */
	public <T extends OAObject> Hub<T>[] getHubReferences(T oaObj);
	/**
	 * Registers a Hub reference for an object.
	 *
	 * @param oaObj the object being referenced
	 * @param hub the referencing Hub
	 * @param bAlwaysAddIfM2M {@code true} to always register many-to-many Hub references
	 * @return {@code true} if the Hub reference was added
	 */
	public <T extends OAObject> boolean addHub(T oaObj, Hub<T> hub, boolean bAlwaysAddIfM2M);
	/**
	 * Returns whether an object is in a Hub that has a master object.
	 *
	 * @param obj the object to inspect
	 * @return {@code true} if the object is in a master-backed Hub
	 */
	public boolean isInHubWithMaster(OAObject obj);
	/**
	 * Removes a Hub reference from an object.
	 *
	 * @param oaObj the object being dereferenced
	 * @param hub the Hub to remove
	 * @param bIsOnHubFinalize {@code true} when removal is part of Hub finalization
	 */
	public <T extends OAObject> void removeHub(final T oaObj, Hub<T> hub, boolean bIsOnHubFinalize);
	
}
