package com.viaoa.graph.api.services;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.auto.HubAutoMatch;
import com.viaoa.hub.copy.HubCopy;
import com.viaoa.hub.filter.HubFilter;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.hub.view.HubCombined;
import com.viaoa.hub.view.HubFlattened;
import com.viaoa.hub.view.OAGroupBy;
import com.viaoa.hub.view.OALeftJoin;
import com.viaoa.object.OAObject;

public interface HubsOps {

	
	
    /**
     * Creates a detail Hub based on the supplied master Hub and property path.
     * <p>
     * {@code detail(...)} creates a {@link Hub} that represents the objects
     * referenced by the supplied property path from the active object of the
     * master Hub. The returned Hub is a live structure that automatically updates
     * as the active object or its referenced relationships change.
     * <p>
     * The {@code path} defines the relationship traversal using model property
     * names (for example, {@code "orders"} or {@code "orders.lineItems"}).
     * <p>
     * This is the primary verb for navigating relationships and creating
     * master/detail structures within the Object Graph.
     *
     * @param hub the master Hub
     * @param path the property path used to navigate relationships
     * @return a live detail Hub based on the supplied path
     */
    Hub<?> detail(Hub<?> hub, String path);
	

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
    
    /**
     * Links one Hub to a reference property of another Hub's active object.
     * <p>
     * {@code link(...)} wires {@code hub1} to the reference defined by
     * {@code referenceName} on the active object of {@code hub2}. As the active
     * object of {@code hub2} changes, {@code hub1} is automatically updated to
     * reflect the referenced object or collection.
     * <p>
     * The {@code referenceName} must be a valid model property that defines a
     * relationship (reference or Hub) on the objects contained in {@code hub2}.
     * <p>
     * This is commonly used to synchronize Hubs based on relationships, allowing
     * one Hub to follow another through the Object Graph as navigation occurs.
     *
     * @param hub1 the Hub to be linked and updated
     * @param hub2 the source Hub whose active object drives the link
     * @param referenceName the relationship property name on the source object's type
     */
    void link(Hub<?> hub1, Hub<?> hub2, String referenceName);

    // =========== Hub Composition / Shaping =========== 

    /**
     * Merges objects reached through a property path from the supplied source Hub
     * into an existing target Hub.
     * <p>
     * {@code merge(...)} traverses the supplied {@code path} starting from the
     * objects in {@code hub} and keeps the reachable objects synchronized in
     * {@code hubCombined}. As source objects, relationships, and reachable objects
     * change, the target Hub is automatically updated to reflect the merged result.
     * <p>
     * The {@code path} defines relationship traversal using model property names
     * and may span multiple levels (for example, {@code "orders.lineItems"}).
     * <p>
     * Use this form when the target Hub already exists and should be populated and
     * maintained as a live merged view of the source Hub.
     *
     * @param hub the source Hub
     * @param hubCombined the target Hub that receives and maintains the merged objects
     * @param path the relationship path used to collect objects into the target Hub
     */
    <F extends OAObject, T extends OAObject> HubMerger<F, T> merge(Hub<F> hub, Hub<T> hubCombined, String path);

    

    
    /**
     * Creates and returns a {@link HubMerger} that maintains a live merged view
     * from a source Hub into a target Hub using advanced merge options.
     * <p>
     * {@code merge(...)} traverses the supplied {@code path} starting from the
     * objects in {@code hubRoot} and keeps {@code mergedHub} synchronized with the
     * reachable objects. As source objects, relationships, and reachable objects
     * change, the merged Hub is automatically updated to reflect the current result.
     * <p>
     * This overload exposes advanced merge behavior such as active-object sharing,
     * ordering, whether traversal begins from all objects or only the active object,
     * whether the root Hub is included, and whether merge maintenance uses a
     * background thread.
     *
     * @param <F> the source model object type
     * @param <T> the merged target model object type
     * @param hubRoot the source Hub used as the merge starting point
     * @param mergedHub the target Hub that receives and maintains the merged objects
     * @param path the relationship path used to collect objects into the merged Hub
     * @param shareActiveObject {@code true} if the merged Hub shares the active
     *        object behavior of the source structure; {@code false} otherwise
     * @param selectOrder optional ordering expression applied to the merged result;
     *        {@code null} for default ordering
     * @param useAllObjects {@code true} to traverse from all objects in
     *        {@code hubRoot}; {@code false} to traverse only from its active object
     * @param includeRootHub {@code true} to include the root Hub level in merge
     *        processing; {@code false} to merge only objects reached through the path
     * @param useBackgroundThread {@code true} to maintain the merge using a
     *        background thread; {@code false} to maintain it on the calling thread
     * @return a {@link HubMerger} used to manage and maintain the merged view
     */
    <F extends OAObject, T extends OAObject> HubMerger<F, T> merge(
        Hub<F> hubRoot,
        Hub<T> mergedHub,
        String path,
        boolean shareActiveObject,
        String selectOrder,
        boolean useAllObjects,
        boolean includeRootHub,
        boolean useBackgroundThread
    );
    
    
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

    
    // =========== Filter / Match =========== 
    
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

    /**
     * Maintains a live match between objects in one Hub and objects in another Hub
     * based on a reference property.
     * <p>
     * {@code match(...)} keeps the contents of {@code hub} synchronized so that it
     * contains only the objects whose {@code property} value matches an object in
     * {@code hubMaster}.
     * <p>
     * The {@code property} must define a relationship or reference on the objects
     * in {@code hub}. As objects in either Hub change, the match is automatically
     * re-evaluated, and the contents of {@code hub} are updated immediately without
     * requiring any manual refresh.
     * <p>
     * This is commonly used to correlate objects across Hubs based on model
     * relationships, allowing one Hub to reflect the subset of objects that match
     * another Hub.
     *
     * @param <T> the model object type contained by the target Hub
     * @param hub the target Hub whose contents are matched and maintained
     * @param property the reference property used for matching
     * @param hubMaster the source Hub providing the matching objects
     */
    <T extends OAObject, T2 extends OAObject> HubAutoMatch<T,T2> match(Hub<T> hub, String property, Hub<T2> hubMaster);    

    
    // =========== Copy / Group / Flatten / Join =========== 
    
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
     * Creates and maintains a live grouped view from one Hub into another.
     * <p>
     * {@code groupBy(...)} groups the objects in {@code hubFrom} by the objects in
     * {@code hubGrpBy} using the supplied {@code propertyPath}, and returns a live
     * {@link Hub} of {@link OAGroupBy} entries.
     * <p>
     * Each {@code OAGroupBy} entry represents one grouping object from
     * {@code hubGrpBy} together with a detail Hub containing the objects from
     * {@code hubFrom} that match that grouping object through the supplied path.
     * <p>
     * Changes to either source Hub, or to the relationships used by the grouping
     * path, are automatically observed and reflected in the grouped result without
     * requiring any manual refresh.
     * <p>
     * When {@code createNullList} is {@code true}, group entries are also created
     * for grouping objects that currently have no matching source objects.
     *
     * @param <F> the model object type being grouped
     * @param <G> the model object type used as the grouping object
     * @param hubFrom the source Hub containing objects to group
     * @param hubGrpBy the Hub containing the grouping objects
     * @param propertyPath the relationship path from source objects to grouping objects
     * @param createNullList {@code true} to include empty groups for grouping
     *        objects with no matches; {@code false} to include only groups that
     *        currently have matching source objects
     * @return a live Hub of group entries, each with its matching detail Hub
     */
    <F extends OAObject, G extends OAObject> Hub<OAGroupBy<F, G>> groupBy(Hub<F> hubFrom, Hub<G> hubGrpBy, String propertyPath, boolean createNullList);
    
    /**
     * Flattens a recursive Hub structure into an existing target Hub.
     * <p>
     * {@code flatten(...)} traverses the recursive relationships reachable from
     * {@code hubRoot} and keeps {@code hubFlat} synchronized with the flattened
     * set of objects.
     * <p>
     * This is used for recursive model structures where objects reference other
     * objects of the same type through a recursive Hub relationship. As objects
     * are added, removed, or changed within the recursive structure, the flattened
     * Hub is automatically updated without requiring any manual refresh.
     * <p>
     * Use this form when the target Hub already exists and should be maintained as
     * a live flattened view of the recursive structure.
     *
     * @param <T> the model object type contained by the Hubs
     * @param hubRoot the root Hub of the recursive structure
     * @param hubFlat the target Hub that receives and maintains the flattened objects
     */
    <T extends OAObject> HubFlattened<T> flatten(Hub<T> hubRoot, Hub<T> hubFlat);
    
    /**
     * Creates a live flattened view of a recursive Hub structure.
     * <p>
     * {@code flatten(...)} traverses the recursive relationships reachable from
     * {@code hubRoot} and returns a new {@link Hub} containing the flattened set
     * of objects.
     * <p>
     * This is used for recursive model structures where objects reference other
     * objects of the same type through a recursive Hub relationship. The returned
     * Hub is a live structure that is automatically updated as objects are added,
     * removed, or changed within the recursive structure, without requiring any
     * manual refresh.
     * <p>
     * This is the convenience form of {@link #flatten(Hub, Hub)} that creates and
     * returns the flattened Hub.
     *
     * @param <T> the model object type contained by the Hubs
     * @param hubRoot the root Hub of the recursive structure
     * @return a new live Hub containing the flattened objects
     */
    <T extends OAObject> Hub<T> flatten(Hub<T> hubRoot);
    
    /**
     * Maintains a live left-join relationship between two Hubs.
     * <p>
     * {@code leftJoin(...)} keeps {@code hub} synchronized so that it reflects
     * the objects from the primary Hub together with their related objects from
     * {@code hubOther} using the supplied property paths.
     * <p>
     * The join is based on the relationship between the two Hubs as defined by
     * the model paths. All objects from the primary side are included, even if
     * there is no matching object on the other side, following left-join semantics.
     * <p>
     * As objects or relationships change in either Hub, the joined result is
     * automatically updated without requiring any manual refresh.
     * <p>
     * This is commonly used to correlate and navigate related data across Hubs
     * while preserving all objects from the primary side.
     *
     * @param hubLeft the primary Hub (left side of the join)
     * @param hubRight the secondary Hub (right side of the join)
     * @param propertyPath the relationship path from primary objects to the joined objects
     * @param shareActiveObject 
     */
    <A extends OAObject, B extends OAObject> Hub<OALeftJoin<A, B>> leftJoin(Hub<A> hubLeft, Hub<B> hubRight, String propertyPath, boolean shareActiveObject);
     
    
}
