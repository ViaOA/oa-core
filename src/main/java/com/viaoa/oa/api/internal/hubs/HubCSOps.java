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
	/**
	 * Returns whether a Hub is running in server context.
	 *
	 * @param hub the Hub to inspect
	 * @return {@code true} if server-side
	 */
	public boolean isServer(Hub<?> hub);
	/**
	 * Returns whether a Hub is running in client context.
	 *
	 * @param hub the Hub to inspect
	 * @return {@code true} if client-side
	 */
	public boolean isClient(Hub<?> hub);
	
}
