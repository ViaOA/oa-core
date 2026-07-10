package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.object.OAObject;

/**
 * Internal live merge operations that collect related objects from a source Hub into a target Hub.
 */
public interface HubMergeOps {

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

}
