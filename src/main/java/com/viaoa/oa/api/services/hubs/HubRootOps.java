package com.viaoa.oa.api.services.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAObject;

/**
 * Public OA Hub root service operations.
 */
public interface HubRootOps {
	
	/**
	 * Returns the root Hub for a Hub chain.
	 *
	 * @param thisHub the Hub to inspect
	 * @return the root Hub
	 */
	public <T extends OAObject> Hub<T> getRootHub(final Hub<T> thisHub);
	
}
