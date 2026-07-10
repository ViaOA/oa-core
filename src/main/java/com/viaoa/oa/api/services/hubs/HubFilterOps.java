package com.viaoa.oa.api.services.hubs;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.filter.HubFilter;
import com.viaoa.object.OAObject;

/**
 * Public OA Hub filter service operations for maintaining live filtered Hub views.
 */
public interface HubFilterOps {

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
      * The optional {@code dependentPaths} identify additional property
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
      * @param dependentPaths optional property paths that affect filter results
      */
     <T extends OAObject> HubFilter<T> filter(Hub<T> hubMaster, Hub<T> hub, OAFilter<T> filter, String... dependentPaths);


}



