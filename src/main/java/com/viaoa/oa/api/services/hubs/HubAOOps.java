package com.viaoa.oa.api.services.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAObject;

/**
 * Public OA Hub active-object service operations.
 */
public interface HubAOOps {
	
	/**
	 * Installs support that keeps a Hub active object synchronized for service-level use.
	 *
	 * @param thisHub the Hub whose active object is maintained
	 * @return the listener adapter used to maintain active-object state
	 */
	public <T extends OAObject> HubListenerAdapter<T> keepActiveObject(final Hub<T> thisHub);
	/**
	 * Sets the active object using advanced service options.
	 *
	 * @param thisHub the Hub to update
	 * @param object the object to make active
	 * @param pos the object position, when known
	 * @param bUpdateLink {@code true} to update linked Hub state
	 * @param bForce {@code true} to force the active-object change
	 * @param bCalledByShareHub {@code true} when called during shared-Hub propagation
	 * @param bUpdateSharedHubDetail {@code true} to update shared detail Hubs
	 */
	public <T extends OAObject> void setActiveObject(final Hub<T> thisHub, T object, final int pos, final boolean bUpdateLink, final boolean bForce,
			final boolean bCalledByShareHub, final boolean bUpdateSharedHubDetail);

	/**
	 * Sets the active object with master/detail, link, and force options.
	 *
	 * @param thisHub the Hub to update
	 * @param object the object to make active
	 * @param adjustMaster {@code true} to adjust master/detail state
	 * @param bUpdateLink {@code true} to update linked Hub state
	 * @param bForce {@code true} to force the active-object change
	 */
	public <T extends OAObject> void setActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce);
	
	/**
	 * Updates detail Hubs that depend on a Hub active object.
	 *
	 * @param thisHub the master Hub whose detail Hubs are updated
	 */
	public <T extends OAObject> void updateDetailHubs(final Hub<T> thisHub);
}
