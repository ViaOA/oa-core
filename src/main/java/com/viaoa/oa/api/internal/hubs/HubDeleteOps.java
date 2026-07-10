package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;

/**
 * Internal delete-all state and delete execution operations for Hubs.
 */
public interface HubDeleteOps {

	
	/**
	 * Deletes all objects contained in a Hub.
	 *
	 * @param hub the Hub whose objects are deleted
	 */
	public void deleteAll(Hub<?> hub);
	/**
	 * Returns whether a Hub is currently deleting all contents.
	 *
	 * @param hub the Hub to inspect
	 * @return {@code true} if delete-all processing is active
	 */
	public boolean isDeletingAll(Hub<?> hub);

}
