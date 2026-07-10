package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;

/**
 * Internal Hub size operations for logical and loaded counts.
 */
public interface HubSizeOps {

	/**
	 * Returns the logical size of a Hub.
	 *
	 * @param hub the Hub to inspect
	 * @return the logical size
	 */
	public int getSize(Hub<?> hub);
	/**
	 * Returns the loaded size of a Hub.
	 *
	 * @param hub the Hub to inspect
	 * @return the loaded size
	 */
	public int getLoadedSize(Hub<?> hub);

}
