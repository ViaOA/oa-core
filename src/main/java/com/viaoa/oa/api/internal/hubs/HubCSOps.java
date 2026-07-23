package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;

/**
 * Internal client/server hooks for Hub refresh and runtime-side detection.
 */
public interface HubCSOps {

	/**
	 * Sends a refresh notification for a Hub.
	 *
	 * @param hub the Hub being refreshed
	 */
	public void sendRefresh(Hub<?> hub);
	
}
