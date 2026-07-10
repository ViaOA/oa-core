package com.viaoa.oa.api.services.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Public OA Hub data service operations for selected Hub membership queries.
 */
public interface HubDataOps {
	
	/**
	 * Returns the position of an object in a Hub with advanced master/link options.
	 *
	 * @param thisHub the Hub to search
	 * @param object the object or key-compatible value
	 * @param adjustMaster {@code true} to adjust master/detail state
	 * @param bUpdateLink {@code true} to update linked Hub state
	 * @return the object position, or {@code -1}
	 */
	public <T extends OAObject> int getPos(final Hub<T> thisHub, Object object, final boolean adjustMaster, final boolean bUpdateLink);
	
}
