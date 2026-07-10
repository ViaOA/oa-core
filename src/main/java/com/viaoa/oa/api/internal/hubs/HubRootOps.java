package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAObject;

/**
 * Internal root-Hub marker and lookup operations.
 */
public interface HubRootOps {
	
	/**
	 * Returns the root Hub for a Hub chain.
	 *
	 * @param hub the Hub to inspect
	 * @return the root Hub
	 */
	public <T extends OAObject> Hub<T> getRootHub(Hub<T> hub);
	/**
	 * Sets whether a Hub is marked as root.
	 *
	 * @param hub the Hub to update
	 * @param bIsRoot {@code true} to mark as root
	 */
	public void setRootHub(Hub<?> hub, boolean bIsRoot);

}
