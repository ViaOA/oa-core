package com.viaoa.graph.api.services.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.copy.HubCopy;
import com.viaoa.object.OAObject;

public interface HubCopyOps {

    /**
     * Creates a live copy of the supplied Hub.
     * <p>
     * {@code copy(...)} creates a new {@link Hub} that contains the same objects
     * as the source Hub and stays automatically synchronized as objects are added
     * to or removed from the source Hub.
     * <p>
     * Unlike {@link #share(Hub, Hub, boolean)}, the returned Hub is a separate Hub
     * instance with its own internal state and may have different behavior such as
     * sorting or other Hub-level configuration, while still reflecting the same
     * object membership as the source Hub.
     * <p>
     * Changes to the source Hub are automatically observed and reflected in the
     * copied Hub without requiring any manual refresh.
     *
     * @param hubFrom the source Hub to copy
     * @param hubTo the Hub to copy to
     */
    <T extends OAObject> HubCopy<T> copy(Hub<T> hubFrom, Hub<T> hubTo);
	
    /**
     * Creates a live copy of the supplied Hub with optional shared active object behavior.
     * <p>
     * {@code copy(...)} creates a new {@link Hub} that contains the same objects
     * as the source Hub and stays automatically synchronized as objects are added
     * to or removed from the source Hub.
     * <p>
     * Unlike {@link #share(Hub, Hub, boolean)}, the returned Hub is a separate Hub
     * instance with its own internal state and may have different behavior such as
     * sorting or other Hub-level configuration, while still reflecting the same
     * object membership as the source Hub.
     * <p>
     * The {@code shareActiveObject} flag controls whether the copied Hub shares the same
     * active object as the source Hub. When {@code true}, active object changes
     * are shared. When {@code false}, each Hub maintains its own active object.
     * <p>
     * Changes to the source Hub are automatically observed and reflected in the
     * copied Hub without requiring any manual refresh.
     *
     * @param hubFrom the source Hub to copy
     * @param hubTo the Hub to copy to
     * @param shareActiveObject {@code true} if the active object is also shared;
     *        {@code false} if the copied Hub maintains its own active object
     */
    <T extends OAObject> HubCopy<T> copy(Hub<T> hubFrom, Hub<T> hubTo, boolean shareActiveObject);

}


