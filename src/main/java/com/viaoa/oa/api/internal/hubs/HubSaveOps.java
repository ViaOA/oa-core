package com.viaoa.oa.api.internal.hubs;

import com.viaoa.cascade.OACascade;
import com.viaoa.hub.Hub;

/**
 * Internal save-all operations for Hubs and their contained objects.
 */
public interface HubSaveOps {

	/**
	 * Saves all objects in a Hub using a cascade rule.
	 *
	 * @param hub the Hub whose objects are saved
	 * @param cascadeRule the cascade rule
	 */
	public void saveAll(Hub<?> hub, int cascadeRule);
	/**
	 * Saves all objects in a Hub using an existing cascade context.
	 *
	 * @param thisHub the Hub whose objects are saved
	 * @param iCascadeRule the cascade rule
	 * @param cascade the cascade context
	 */
	void saveAll(Hub<?> thisHub, int iCascadeRule, OACascade cascade);

}
