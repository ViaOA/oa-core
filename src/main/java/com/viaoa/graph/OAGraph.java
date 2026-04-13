package com.viaoa.graph;

import java.util.List;

import com.viaoa.graph.api.*;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubFilter;
import com.viaoa.hub.HubListener;
import com.viaoa.object.OAGroupBy;
import com.viaoa.object.OALeftJoin;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.util.OAFilter;

/**
 * Object Graph runtime for a model package.
 * <p>
 * {@code OAGraph} is the executable runtime for a model-defined blueprint.
 * It operates as a layer above the data source, allowing applications to work
 * with live objects and collections instead of directly interacting with
 * persistence or transport mechanisms.
 * <p>
 * The model defines the blueprint. {@code OAGraph} executes that blueprint by
 * creating and wiring live {@link OAObject} instances and {@link Hub}
 * collections together based on model-defined relationships.
 * <p>
 * Through its verbs, {@code OAGraph} produces and maintains live views into
 * the object graph. These views are automatically kept up to date as objects,
 * relationships, and state change, without requiring manual refresh.
 * <p>
 * {@code OAGraph} also supports distributed object graph behavior through
 * runtime synchronization and replication, allowing live graph state to remain
 * coordinated across threads, processes, clients, servers, or sites.
 * <p>
 * As the runtime layer for the executable blueprint, {@code OAGraph} manages
 * core graph behavior such as identity, uniqueness, relationships, validation,
 * policy, security, and blueprint introspection.
 * <p>
 * {@code OAGraph} has two main kinds of verbs:
 * <ul>
 *   <li><b>Core graph verbs</b> used to create, get, select, find, observe,
 *       save, and delete live model objects and collections.</li>
 *   <li><b>Blueprint composition verbs</b> used to create and maintain derived
 *       live structures such as detail Hubs, shared Hubs, merged Hubs,
 *       grouped views, flattened views, and joins.</li>
 * </ul>
 * <p>
 * {@code OAGraph} also exposes runtime blueprint metadata through
 * {@link #info(Class)}, {@link #info(OAObject)}, and {@link #info(Hub)}.
 * <p>
 * In practice, {@code OAGraph} is the entry point into the executable model:
 *
 * <pre>{@code
 * OAGraph og = OARuntime.createGraph(MyModel.class.getPackage());
 *
 * Hub<Order> orders = og.select(Order.class, "store.region = ?", "SE");
 * Order order = og.get(Order.class, 12345);
 * OAObjectInfo info = og.info(Order.class);
 * }</pre>
 *
 * After entering through {@code OAGraph}, developers typically continue working
 * directly with live model objects and Hubs by navigating relationships and
 * composing additional live views using the graph.
 */
public interface OAGraph {

	/**
	 * Returns the model package used to create this {@code OAGraph}.
	 * <p>
	 * The package identifies the set of model classes (OAObjects) that define
	 * the blueprint executed by this graph.
	 * <p>
	 * Each {@code OAGraph} instance is tied to a specific model package,
	 * which determines the available object types, relationships, and metadata.
	 *
	 * @return the fully qualified package name for this graph's model
	 */
	public Package getPackage();
	
	/**
	 * Returns the real-time synchronization operations for this graph.
	 * <p>
	 * {@code sync()} provides access to the runtime synchronization layer used
	 * to propagate changes to objects and Hubs immediately across the Object Graph.
	 * <p>
	 * Synchronization keeps live graph state consistent in real time across
	 * connected runtimes, between clients and a server. Changes made in
	 * one part of the graph are automatically reflected in others without requiring
	 * manual coordination.
	 * <p>
	 * This represents real-time coordination of the graph. For eventual consistency
	 * with offline support and server-to-server synchronization, see
	 * {@link #replication()}.
	 *
	 * @return synchronization operations for this graph
	 */	public SyncOps sync();

	
	 /**
	  * Returns the replication operations for this graph.
	  * <p>
	  * {@code replication()} provides access to the runtime replication layer used
	  * to capture, transmit, and apply changes between Object Graph instances.
	  * <p>
	  * Replication supports eventual consistency across distributed runtimes,
	  * which is server-to-server coordination and offline scenarios. Changes are
	  * recorded, transmitted, and applied so that graph state converges over time.
	  * <p>
	  * This represents eventual synchronization of the graph with support for
	  * disconnected operation. For real-time synchronization between connected
	  * runtimes, see {@link #sync()}.
	  *
	  * @return replication operations for this graph
	  */
	 public ReplOps replication();
	

	// =========== Core graph verbs (CRUD+) ===========
	
	 /**
	  * Creates a new live object instance for the supplied model type.
	  * <p>
	  * {@code create(...)} creates an {@link OAObject} that is part of this graph's
	  * runtime model and is ready to participate in normal object graph behavior,
	  * including references, Hub membership, observation, persistence, and other
	  * model-driven services.
	  * <p>
	  * This is the object-level creation verb for the executable blueprint. Use it
	  * when a new model object should be created through the graph rather than by
	  * directly calling a constructor.
	  * <p>
	  * The returned object is not automatically saved. Persist it using
	  * {@link #save(OAObject)} or {@link #save(OAObject)}when appropriate.
	  *
	  * @param <T> the model object type
	  * @param type the model class to create
	  * @return a new live object instance for the supplied type
	  * @see OAObject
	  */
	 <T extends OAObject> T create(Class<T> type); //qqqqqqqq need to verify that type is in this OG
	
    
	 /**
	  * Creates a new live Hub for the supplied model type.
	  * <p>
	  * {@code createHub(...)} creates a {@link Hub} that participates in normal
	  * object graph behavior, including selection, detail wiring, observation,
	  * sharing, merging, grouping, and other graph-driven operations.
	  * <p>
	  * This is the collection-level creation verb for the executable blueprint.
	  * <p>
	  * The returned Hub is empty until populated by application code using standard
	  * Hub operations (such as add, remove, and iteration) or by graph operations
	  * such as {@link #select(Class, String, String, Object...)},
	  * {@link #detail(Hub, String)}, {@link #merge(Hub, String)}, or
	  * {@link #combine(Hub, Hub[])}.
	  *
	  * @param <T> the model object type contained by the Hub
	  * @param type the model class for the Hub
	  * @return a new live Hub for the supplied type
	  * @see Hub
	  */
	 <T extends OAObject> Hub<T> createHub(Class<T> type); //qqqqqqqq need to verify that type is in this OG
	
	 /**
	  * Persists the supplied object using the graph's runtime and data sources.
	  * <p>
	  * {@code save(...)} writes the current state of the object through the
	  * Object Graph, applying all model-defined behavior such as relationships,
	  * rules, and lifecycle handling.
	  * <p>
	  * This is equivalent to calling {@code save()} on the {@link OAObject} itself.
	  * <p>
	  * Save operations follow model-defined relationships and will cascade to
	  * related objects that are configured for cascading (for example, owned or
	  * dependent objects).
	  * <p>
	  * If the object is new, it becomes part of the persistent graph. Its new-state
	  * is cleared, and any model-defined identity value may be assigned.
	  * <p>
	  * The object must be part of this graph. Changes made to the object and its
	  * related graph structure are persisted according to the configured
	  * data sources and runtime policies.
	  * <p>
	  * This is the primary persistence verb for individual objects in the
	  * executable blueprint.
	  *
	  * @param obj the object to persist
	  */
	 void save(OAObject obj);
	
	
	/**
	 * Persists all objects contained in the supplied Hub.
	 * <p>
	 * {@code save(...)} writes the current state of each object in the Hub through
	 * the Object Graph, applying all model-defined behavior such as relationships,
	 * rules, and lifecycle handling.
	 * <p>
	 * Save operations may cascade to related objects based on the model's
	 * relationship definitions (for example, owned or dependent objects).
	 * <p>
	 * This is the primary persistence verb for collections of objects in the
	 * executable blueprint.
	 *
	 * @param hub the Hub containing objects to persist
	 */
    void save(Hub<?> hub);

    /**
     * Deletes the supplied object from the graph and underlying data sources.
     * <p>
     * {@code delete(...)} removes the object through the Object Graph, applying
     * all model-defined behavior such as relationships, rules, and lifecycle
     * handling.
     * <p>
     * This is equivalent to calling {@code delete()} on the {@link OAObject} itself.
     * <p>
     * Delete operations follow model-defined relationships and will cascade to
     * related objects that are configured for cascading (for example, owned or
     * dependent objects).
     * <p>
     * References to the deleted object from other objects are automatically
     * cleared according to the model, and the object is removed from any Hubs
     * in which it participates.
     * <p>
     * The object must be part of this graph.
     *
     * @param obj the object to delete
     */
    void delete(OAObject obj);


    /**
     * Deletes all objects contained in the supplied Hub.
     * <p>
     * {@code delete(...)} removes each object in the Hub through the Object Graph,
     * applying all model-defined behavior such as relationships, rules, and
     * lifecycle handling.
     * <p>
     * Each deleted object is removed from the graph, including its relationships
     * and collections. References to deleted objects from other objects are
     * cleared according to the model, and deleted objects are removed from any
     * Hubs in which they participate.
     * <p>
     * Delete operations may cascade to related objects based on the model's
     * relationship definitions (for example, owned or dependent objects).
     * <p>
     * This deletes the objects contained in the Hub. It is not the same as
     * clearing a Hub without deleting its objects.
     *
     * @param hub the Hub containing objects to delete
     */
    void delete(Hub<?> hub);

    /**
     * Returns the object of the specified type for the given identity key.
     * <p>
     * {@code get(...)} locates a single object in the Object Graph using its
     * identity (for example, primary key or unique identifier). The returned
     * object is a live instance that participates fully in the graph, including
     * relationships, Hub membership, observation, and persistence.
     * <p>
     * If the object is not already present in the graph, it may be loaded through
     * the configured data sources.
     * <p>
     * This is the primary verb for retrieving a single object by identity.
     *
     * @param <T> the model object type
     * @param type the model class
     * @param key the identity value used to locate the object
     * @return the matching object, or {@code null} if not found
     */
    <T extends OAObject> T get(Class<T> type, Object key);


    /**
     * Returns the object of the specified type for the given object key.
     * <p>
     * {@code get(...)} locates a single object in the Object Graph using its
     * {@link OAObjectKey}. The returned object is a live instance that participates
     * fully in the graph, including relationships, Hub membership, observation,
     * and persistence.
     * <p>
     * If the object is not already present in the graph, it may be loaded through
     * the configured data sources.
     * <p>
     * This is the object-key form of {@link #get(Class, Object)} and is used when
     * the identity is represented by an {@code OAObjectKey}.
     *
     * @param <T> the model object type
     * @param type the model class
     * @param key the object key used to locate the object
     * @return the matching object, or {@code null} if not found
     */
    <T extends OAObject> T get(Class<T> type, OAObjectKey key);
    
    /**
     * Selects objects of the supplied model type and returns them in a live Hub.
     * <p>
     * {@code select(...)} queries the Object Graph for objects of the requested
     * type using the supplied selection criteria and ordering, and returns the
     * results in a {@link Hub}. The returned Hub is a live graph structure whose
     * objects participate fully in normal object graph behavior, including
     * relationships, observation, and persistence.
     * <p>
     * The {@code where} expression is used to define the selection criteria, and
     * may include object property paths. The optional {@code orderBy} expression
     * defines the ordering of the selected objects.
     * <p>
     * Selection may use the graph runtime, configured data sources, or both,
     * depending on the model, runtime state, and query requirements.
     * <p>
     * This is the primary verb for querying a model type and obtaining a live
     * result set.
     *
     * @param <T> the model object type
     * @param type the model class to select
     * @param where the selection criteria, or {@code null} for no filtering
     * @param orderBy the ordering expression, or {@code null} for default ordering
     * @param args argument values referenced by the {@code where} expression
     * @return a live Hub containing the selected objects
     */
    <T extends OAObject> Hub<T> select(Class<T> type, String where, String orderBy, Object... args);

    /**
     * Selects objects into the supplied Hub using the selection criteria and ordering.
     * <p>
     * {@code select(...)} populates the supplied {@link Hub} with objects that
     * match the {@code where} expression and optional {@code orderBy} expression.
     * The selection is performed using the standard select/query mechanism
     * (for example, {@code OASelect}) rather than a live find/filter structure.
     * <p>
     * The {@code where} expression defines the selection criteria and may include
     * object property paths. The optional {@code orderBy} expression defines the
     * ordering of the selected objects.
     * <p>
     * This method does not keep the Hub synchronized with later changes that would
     * affect the selection results. For live graph-based searching or continuously
     * updated matching behavior, use the appropriate find/filter mechanisms instead.
     *
     * @param hub the Hub to populate with the selected objects
     * @param where the selection criteria, or {@code null} for no filtering
     * @param orderBy the ordering expression, or {@code null} for default ordering
     * @param args argument values referenced by the {@code where} expression
     */
    <T extends OAObject> Hub<T> select(Hub<T> hub, String where, String orderBy, Object... args);
    
    
    /**
     * Finds the first object in the supplied Hub that matches the search criteria.
     * <p>
     * {@code find(...)} searches the objects currently available through the
     * supplied {@link Hub} using the {@code where} expression and returns the
     * first matching object.
     * <p>
     * Unlike {@link #select(Hub, String, String, Object...)}, this is a graph-based
     * search that does not query external data sources. It evaluates the
     * {@code where} expression against the live objects in the graph using
     * object property paths.
     * <p>
     * This method is commonly used to locate a single matching object from an
     * existing live Hub without creating a new Hub result set.
     *
     * @param <T> the model object type
     * @param hub the Hub to search
     * @param where the search criteria
     * @param args argument values referenced by the {@code where} expression
     * @return the first matching object, or {@code null} if no match is found
     */
    <T extends OAObject> T findFirst(Hub<T> hub, String where, Object... args);
    <T extends OAObject, T2 extends OAObject> T findFirst(T2 fromObject, String where, Object... args);


    /**
     * Finds all objects in the supplied Hub that match the search criteria.
     * <p>
     * {@code findAll(...)} searches the objects currently available through the
     * supplied {@link Hub} using the {@code where} expression and returns all
     * matching objects as a {@link java.util.List}.
     * <p>
     * Unlike {@link #select(Hub, String, String, Object...)}, this is a graph-based
     * search that does not query external data sources. It evaluates the
     * {@code where} expression against the live objects in the graph using
     * object property paths.
     * <p>
     * This method is commonly used when multiple matching objects are needed from
     * an existing live Hub without creating a new Hub result set.
     *
     * @param <T> the model object type
     * @param hub the Hub to search
     * @param where the search criteria
     * @param args argument values referenced by the {@code where} expression
     * @return a list of matching objects (empty if no matches are found)
     */
    <T extends OAObject> List<T> findAll(Hub<T> hub, String where, Object... args);
    <T extends OAObject, T2 extends OAObject> List<T> findAll(T2 fromObject, String where, Object... args);
    

    
    
    // =========== Observability ===========
    
    /**
     * Adds a listener to the supplied Hub so that changes to the Hub can be observed.
     * <p>
     * {@code observe(...)} is the graph-level verb for Hub observation. It registers
     * the supplied {@link HubListener} with the Hub so that application code can
     * react to changes in the Hub, its active object, and the objects it contains.
     * <p>
     * This is equivalent to calling {@code addListener(...)} on the {@link Hub}.
     * <p>
     * Observation is a live runtime feature of the Object Graph. It is commonly used
     * for UI wiring, runtime coordination, and other behavior that depends on Hub
     * changes as the executable blueprint is running.
     *
     * @param <T> the model object type contained by the Hub
     * @param hub the Hub to observe
     * @param hl the listener to add
     */
    <T extends OAObject> void observe(Hub<T> hub, HubListener<T> hl);
    
    
    // =========== Hub Collection Wiring ===========
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
     * @param hub the source Hub whose collection is shared
     * @param hub2 the target Hub that will share the collection
     * @param shareActiveObject {@code true} if the active object is also shared;
     *        {@code false} if each Hub should maintain its own active object
     */
    <T extends OAObject> void share(Hub<T> hub, Hub<T> hub2, boolean shareActiveObject);
    
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
    

    
    // =========== Hub shaping (runtime blueprint composition) =========== 

    /**
     * Creates a live Hub by merging objects reached through a property path from
     * the supplied source Hub.
     * <p>
     * {@code merge(...)} traverses the supplied {@code path} starting from the
     * objects in {@code hub} and collects the reachable objects into a new
     * {@link Hub}. The returned Hub is a live graph structure that remains
     * synchronized as source objects, relationships, and reachable objects change.
     * <p>
     * The {@code path} defines relationship traversal using model property names
     * and may span multiple levels (for example, {@code "orders.lineItems"}).
     * <p>
     * This is commonly used to flatten related objects from a source Hub into a
     * single live Hub for runtime wiring, UI use, or further graph operations.
     *
     * @param hub the source Hub
     * @param path the relationship path used to collect objects into the merged Hub
     * @return a new live Hub containing the merged objects reached through the path
     */
    Hub<?> merge(Hub<?> hub, String path);

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
    void merge(Hub<?> hub, Hub<?> hubCombined, String path);

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
    <T extends OAObject> void combine(final Hub<T> hubMaster, final Hub<T>... hubs);
    
    /**
     * Creates and returns a HubFilter that maintains a filtered view from one or
     * more source Hubs into the supplied master Hub.
     * <p>
     * {@code filter(...)} sets up a live filtering structure for {@code hubMaster}
     * using the supplied source Hubs. The returned {@link HubFilter} can then be
     * configured with filtering rules to control which objects are included in the
     * master Hub.
     * <p>
     * As source Hub contents change, the filtering structure keeps the master Hub
     * synchronized according to the active filter rules.
     * <p>
     * Use this form when a reusable or configurable live filter is needed rather
     * than a one-time selection.
     *
     * @param <T> the model object type contained by the Hubs
     * @param hubMaster the target Hub that receives the filtered objects
     * @param hubs the source Hubs used by the filter
     * @return the live HubFilter used to define and maintain the filtered result
     */
    <T extends OAObject> HubFilter<T> filter(Hub<T> hubMaster, final Hub<T>... hubs);
    
    
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
    <T extends OAObject> void filter(Hub<T> hubMaster, Hub<T> hub, OAFilter<T> filter, String... dependentPropertyPaths);

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
    <T extends OAObject, T2 extends OAObject> void match(Hub<T> hub, String property, Hub<T2> hubMaster); // HubAutoMatch, see also: HubAutoAdd
    
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
     * @param hub the source Hub to copy
     * @return a new live Hub that mirrors the contents of the source Hub
     */
    Hub<?> copy(Hub<?> hub);
    
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
     * @param hub the source Hub to copy
     * @param shareActiveObject {@code true} if the active object is also shared;
     *        {@code false} if the copied Hub maintains its own active object
     * @return a new live Hub that mirrors the contents of the source Hub
     */
    Hub<?> copy(Hub<?> hub, boolean shareActiveObject);
    
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
     * When {@code bCreateNullList} is {@code true}, group entries are also created
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
    <T extends OAObject> void flatten(Hub<T> hubRoot, Hub<T> hubFlat);
    
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
     * @param hub the primary Hub (left side of the join)
     * @param hubOther the secondary Hub (right side of the join)
     * @param propertyPath the relationship path from primary objects to the joined objects
     * @param shareActiveObject
     */
    <A extends OAObject, B extends OAObject> Hub<OALeftJoin<A, B>> leftJoin(Hub<A> hubA, Hub<B> hubB, String propertyPath, boolean shareActiveObject);
    
    
    // Software Blueprints from OA Model
    
    /**
     * Returns the blueprint metadata for the supplied model object type.
     * <p>
     * {@code info(...)} provides access to the runtime metadata that defines how
     * the supplied {@link OAObject} type participates in the Object Graph,
     * including its properties, relationships, methods, and other model-defined
     * structure.
     * <p>
     * This is the primary blueprint introspection verb for a model class and is
     * commonly used by framework code, tools, UI generation, validation, and
     * other runtime features that need to inspect the executable blueprint.
     *
     * @param type the model object class
     * @return the runtime metadata for the supplied model type
     */
    OAObjectInfo info(Class<? extends OAObject> type);
    
    OAObjectInfo info(OAObject obj);

    OAObjectInfo info(Hub<?> hub);
}

