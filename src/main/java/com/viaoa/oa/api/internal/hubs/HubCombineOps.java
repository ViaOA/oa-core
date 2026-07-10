package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.view.HubCombined;
import com.viaoa.object.OAObject;

/**
 * Internal operations for maintaining a live Hub that combines the contents of multiple source Hubs.
 */
public interface HubCombineOps {

    /**
     * Combines the contents of multiple Hubs into a single live master Hub.
     * <p>
     * {@code combine(...)} keeps {@code hubMaster} synchronized so that it contains
     * the objects from the supplied source Hubs. As objects are added to or removed
     * from the source Hubs, the master Hub is automatically updated to reflect the
     * combined result.
     * <p>
     * This is used to maintain a single live Hub from multiple source Hubs of the
     * same object type, allowing application code, UI wiring, or other OA model
     * operations to work with one combined collection.
     *
     * @param <T> the model object type contained by the Hubs
     * @param hubMaster the target Hub that receives the combined objects
     * @param hubs the source Hubs whose contents are combined into the master Hub
     */
    <T extends OAObject> HubCombined<T> combine(final Hub<T> hubMaster, final Hub<T>... hubs);

}


