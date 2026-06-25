package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.view.HubFlattened;
import com.viaoa.hub.view.OAGroupBy;
import com.viaoa.hub.view.OALeftJoin;
import com.viaoa.object.OAObject;

public interface HubViewOps {

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


