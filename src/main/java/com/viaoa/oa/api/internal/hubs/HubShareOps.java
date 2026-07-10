package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Internal shared-Hub operations that allow Hubs to share membership and optionally active-object state.
 */
public interface HubShareOps {

    /**
     * Shares the underlying collection of one Hub with another Hub.
     * <p>
     * {@code share(...)} wires {@code hub2} to use the same underlying objects and
     * collection state as {@code hub}, allowing both Hubs to work with the same
     * live object set within the OA model.
     * <p>
     * The {@code shareActiveObject} flag controls whether the two Hubs also share the same
     * active object. When {@code true}, active object changes are shared between
     * the Hubs. When {@code false}, each Hub maintains its own active object while
     * still sharing the same underlying collection.
     * <p>
     * This is commonly used for runtime wiring scenarios where multiple views or
     * components need to work with the same live collection, but may require the
     * same or different active-object behavior.
     *
     * @param <T> the model object type contained by the Hubs
     * @param hub the target Hub that will share the collection
     * @param hubToShare the source Hub whose collection is shared
     * @param shareActiveObject {@code true} if the active object is also shared;
     *        {@code false} if each Hub should maintain its own active object
     */
    <T extends OAObject> void share(Hub<T> hub, Hub<T> hubToShare, boolean shareActiveObject);
	
	/**
	 * Configures a Hub to share another Hub.
	 *
	 * @param hub the Hub to configure
	 * @param sharedMasterHub the Hub whose data is shared
	 * @param shareActiveObject {@code true} to share active-object state
	 */
	public <T extends OAObject> void setSharedHub(Hub<T> hub, Hub<T> sharedMasterHub, boolean shareActiveObject);
	/**
	 * Removes shared-Hub wiring.
	 *
	 * @param hub the Hub being updated
	 * @param hubToRemove the shared Hub to remove
	 */
	public <T extends OAObject> void removeSharedHub(Hub<T> hub, Hub<T> hubToRemove);
	/**
	 * Creates a Hub that shares the supplied Hub.
	 *
	 * @param hub the source Hub
	 * @param shareActiveObject {@code true} to share active-object state
	 * @return the shared Hub
	 */
	public <T extends OAObject> Hub<T> createSharedHub(Hub<T> hub, boolean shareActiveObject);
	/**
	 * Returns whether two Hubs use the same shared data source.
	 *
	 * @param hub the first Hub
	 * @param hub2 the second Hub
	 * @return {@code true} if both use the same shared Hub
	 */
	public boolean isUsingSameSharedHub(Hub<?> hub, Hub<?> hub2);
	/**
	 * Returns whether two Hubs share active-object state.
	 *
	 * @param hub the first Hub
	 * @param hub2 the second Hub
	 * @return {@code true} if active-object state is shared
	 */
	public boolean isUsingSameSharedAO(Hub<?> hub, Hub<?> hub2);
	/**
	 * Returns the main shared Hub for a shared-Hub chain.
	 *
	 * @param hub the Hub to inspect
	 * @return the main shared Hub
	 */
	public <T extends OAObject> Hub<T> getMainSharedHub(Hub<T> hub);
}
