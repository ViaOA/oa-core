package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAObject;

/**
 * Internal active-object operations for Hub instances, including forced AO updates and detail-Hub synchronization.
 */
public interface HubAOOps {

	/**
	 * Installs support that keeps a Hub active object synchronized as needed by OA internals.
	 *
	 * @param thisHub the Hub whose active object is maintained
	 * @return the listener adapter used to maintain active-object state
	 */
	public <T extends OAObject> HubListenerAdapter<T> keepActiveObject(final Hub<T> thisHub);
	/**
	 * Sets the active object using the full internal active-object update options.
	 *
	 * @param thisHub the Hub to update
	 * @param object the object to make active
	 * @param pos the object position, when already known
	 * @param bUpdateLink {@code true} to update linked Hub state
	 * @param bForce {@code true} to force the active-object change
	 * @param bCalledByShareHub {@code true} when called from shared-Hub propagation
	 * @param bUpdateSharedHubDetail {@code true} to update shared detail Hubs
	 */
	public <T extends OAObject> void setActiveObject(final Hub<T> thisHub, T object, final int pos, final boolean bUpdateLink, final boolean bForce,
			final boolean bCalledByShareHub, final boolean bUpdateSharedHubDetail);

	/**
	 * Sets the active object with internal master/link/force options.
	 *
	 * @param thisHub the Hub to update
	 * @param object the object to make active
	 * @param adjustMaster {@code true} to adjust master/detail state
	 * @param bUpdateLink {@code true} to update linked Hub state
	 * @param bForce {@code true} to force the active-object change
	 */
	public <T extends OAObject> void setActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce);
	
	/**
	 * Updates detail Hubs that depend on the active object of a Hub.
	 *
	 * @param thisHub the master Hub whose detail Hubs are updated
	 */
	public <T extends OAObject> void updateDetailHubs(final Hub<T> thisHub);

	/**
	 * Sets the active object by position.
	 *
	 * @param hub the Hub to update
	 * @param pos the position to make active
	 * @return the active object, or {@code null}
	 */
	public <T extends OAObject> T setActiveObject(Hub<T> hub, int pos);
	/**
	 * Sets the active object by object reference.
	 *
	 * @param hub the Hub to update
	 * @param obj the object to make active
	 */
	public <T extends OAObject> void setActiveObject(Hub<T> hub, T obj);
	/**
	 * Forces an object to become the active object.
	 *
	 * @param hub the Hub to update
	 * @param obj the object to make active
	 */
	public <T extends OAObject> void setActiveObjectForce(Hub<T> hub, T obj);
	/**
	 * Sets the active object using an object or key-compatible value.
	 *
	 * @param hub the Hub to update
	 * @param obj the object or key-compatible value
	 * @return the active object, or {@code null}
	 */
	public <T extends OAObject> T setActiveObject(Hub<T> hub, Object obj);

}
