package com.viaoa.graph.api.services.hubs;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.copy.HubCopy;
import com.viaoa.hub.filter.HubFilter;
import com.viaoa.hub.view.HubCombined;
import com.viaoa.object.OAObject;

public interface HubFilterOps {

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
    
    
    /**
     * Combines the contents of multiple Hubs into a single live master Hub.
     * <p>
     * {@code combine(...)} keeps {@code hubMaster} synchronized so that it contains
     * the objects from the supplied source Hubs. As objects are added to or removed
     * from the source Hubs, the master Hub is automatically updated to reflect the
     * combined result.
     * <p>
     * This is used to maintain a single live Hub from multiple source Hubs of the
     * same object type, allowing application code, UI wiring, or other graph
     * operations to work with one combined collection.
     *
     * @param <T> the model object type contained by the Hubs
     * @param hubMaster the target Hub that receives the combined objects
     * @param hubs the source Hubs whose contents are combined into the master Hub
     */
    <T extends OAObject> HubCombined<T> combine(final Hub<T> hubMaster, final Hub<T>... hubs);

    /**
     * Creates and returns a {@link HubFilter} that maintains a live filtered view
     * from one Hub into another.
     * <p>
     * {@code filter(...)} wires {@code hubFiltered} so that it contains only the
     * objects from {@code hubMaster} that satisfy the filter criteria managed by
     * the returned {@link HubFilter}.
     * <p>
     * The returned {@code HubFilter} can be configured with filtering logic and
     * optional dependent property paths. As objects are added, removed, or updated
     * in {@code hubMaster}, and as relevant properties change, {@code hubFiltered}
     * is automatically updated to reflect the current filtered result.
     * <p>
     * This is the dynamic form of filtering, where the filtering rules may be
     * defined or modified after the {@code HubFilter} is created.
     *
     * @param <T> the model object type contained by the Hubs
     * @param hubMaster the source Hub providing objects to be filtered
     * @param hubFiltered the target Hub that receives the filtered objects
     * @return a {@link HubFilter} used to manage and maintain the filtered view
     */
     <T extends OAObject> HubFilter<T> filter(Hub<T> hubMaster, Hub<T> hubFiltered);
    
	
     /**
      * Maintains a filtered live view from a source Hub into a target Hub.
      * <p>
      * {@code filter(...)} applies the supplied {@link OAFilter} to the objects in
      * {@code hub} and keeps {@code hubMaster} synchronized with the objects that
      * currently satisfy the filter.
      * <p>
      * The optional {@code dependentPropertyPaths} identify additional property
      * paths that affect whether an object matches the filter. Changes to those
      * properties are automatically observed, and the filtered result is updated
      * immediately without requiring any manual refresh.
      * <p>
      * Use this form when the filter logic is known up front and a live filtered
      * Hub should be maintained automatically.
      *
      * @param <T> the model object type contained by the Hubs
      * @param hubMaster the target Hub that receives the filtered objects
      * @param hub the source Hub to filter
      * @param filter the filter used to determine which objects are included
      * @param dependentPropertyPaths optional property paths that affect filter results
      */
     <T extends OAObject> HubFilter<T> filter(Hub<T> hubMaster, Hub<T> hub, OAFilter<T> filter, String... dependentPropertyPaths);


}



