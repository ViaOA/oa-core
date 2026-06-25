package com.viaoa.oa.api.services.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public interface HubShareOps {

    /**
     * Shares the underlying collection of one Hub with another Hub.
     * <p>
     * {@code share(...)} wires {@code hub2} to use the same underlying objects and
     * collection state as {@code hub}, allowing both Hubs to work with the same
     * live object set within the Object Graph.
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
	
}
