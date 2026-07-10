package com.viaoa.oa;

import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListener;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.oa.api.*;
import com.viaoa.oa.api.internal.InternalOps;
import com.viaoa.oa.api.services.ServicesOps;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.select.OASelect;

/**
 * OA runtime for a model package.
 * <p>
 * {@code OA} is the executable runtime for a model-defined blueprint.
 * It operates as a layer above the data source, allowing applications to work
 * with live objects and collections instead of directly interacting with
 * persistence or transport mechanisms.
 * <p>
 * The model defines the blueprint. {@code OA} executes that blueprint by
 * creating and wiring live {@link OAObject} instances and {@link Hub}
 * collections together based on model-defined relationships.
 * <p>
 * Through its verbs, {@code OA} produces and maintains live views into
 * the OA model. These views are automatically kept up to date as objects,
 * relationships, and state change, without requiring manual refresh.
 * <p>
 * {@code OA} also supports distributed OA model behavior through
 * runtime synchronization and replication, allowing live OA model state to remain
 * coordinated across threads, processes, clients, servers, or sites.
 * <p>
 * As the runtime layer for the executable blueprint, {@code OA} manages
 * core OA runtime behavior such as identity, uniqueness, relationships, validation,
 * policy, security, and blueprint introspection.
 * <p>
 * {@code OA} has two main kinds of verbs:
 * <ul>
 *   <li><b>Core OA runtime verbs</b> used to create, get, select, find, observe,
 *       save, and delete live model objects and collections.</li>
 *   <li><b>Blueprint composition verbs</b> used to create and maintain derived
 *       live structures such as detail Hubs, shared Hubs, merged Hubs,
 *       grouped views, flattened views, and joins.</li>
 * </ul>
 * <p>
 * {@code OA} also exposes runtime blueprint metadata through
 * {@link #info(Class)}, {@link #info(OAObject)}, and {@link #info(Hub)}.
 * <p>
 * In practice, {@code OA} is the entry point into the executable model:
 *
 * <pre>{@code
 * OA oa = OARuntime.oa(MyModel.class.getPackage());
 *
 * Hub<Order> orders = oa.select(Order.class, "store.region = ?", "SE");
 * Order order = oa.get(Order.class, 12345);
 * OAObjectInfo info = oa.info(Order.class);
 * }</pre>
 *
 * After entering through {@code OA}, developers typically continue working
 * directly with live model objects and Hubs by navigating relationships and
 * composing additional live views using OA runtime services.
 */
public interface OA {

	/**
	 * Returns the model package name used to create this {@code OA}.
	 * <p>
	 * The package identifies the set of model classes (OAObjects) that define
	 * the blueprint executed by this OA runtime.
	 * <p>
	 * Each {@code OA} instance is tied to a specific model package,
	 * which determines the available object types, relationships, and metadata.
	 *
	 * @return the fully qualified package name for this OA runtime's model
	 */
	public String getPackageName();
	
	/**
	 * Returns the real-time synchronization operations for this OA runtime.
	 * <p>
	 * {@code sync()} provides access to the runtime synchronization layer used
	 * to propagate changes to objects and Hubs immediately across the OA model.
	 * <p>
	 * Synchronization keeps live OA model state consistent in real time across
	 * connected runtimes, between clients and a server. Changes made in
	 * one part of the OA model are automatically reflected in others without requiring
	 * manual coordination.
	 * <p>
	 * This represents real-time coordination of the OA runtime. For eventual consistency
	 * with offline support and server-to-server synchronization, see
	 * {@link #replication()}.
	 *
	 * @return synchronization operations for this OA runtime
	 */	
	public SyncOps sync();

	
	 /**
	  * Returns the replication operations for this OA runtime.
	  * <p>
	  * {@code replication()} provides access to the runtime replication layer used
	  * to capture, transmit, and apply changes between OA model instances.
	  * <p>
	  * Replication supports eventual consistency across distributed runtimes,
	  * which is server-to-server coordination and offline scenarios. Changes are
	  * recorded, transmitted, and applied so that OA model state converges over time.
	  * <p>
	  * This represents eventual synchronization of OA model state with support for
	  * disconnected operation. For real-time synchronization between connected
	  * runtimes, see {@link #sync()}.
	  *
	  * @return replication operations for this OA runtime
	  */
	 public ReplicationOps replication();
	

	// =========== Core OA verbs (CRUD+) ===========
	
	 /**
	  * Creates a new live object instance for the supplied model type.
	  * <p>
	  * {@code create(...)} creates an {@link OAObject} that is part of this OA runtime's
	  * runtime model and is ready to participate in normal OA model behavior,
	  * including references, Hub membership, observation, persistence, and other
	  * model-driven services.
	  * <p>
	  * This is the object-level creation verb for the executable blueprint. Use it
	  * when a new model object should be created through the OA runtime rather than by
	  * directly calling a constructor.
	  * <p>
	  * The returned object is not automatically saved. Persist it using
	  * {@link #save(OAObject)} when appropriate.
	  *
	  * @param <T> the model object type
	  * @param type the model class to create
	  * @return a new live object instance for the supplied type
	  * @see OAObject
	  */
	 <T extends OAObject> T create(Class<T> type);
	
    
	 /**
	  * Creates a new live Hub for the supplied model type.
	  * <p>
	  * {@code createHub(...)} creates a {@link Hub} that participates in normal
	  * OA model behavior, including selection, detail wiring, observation,
	  * sharing, merging, grouping, and other OA-driven operations.
	  * <p>
	  * This is the collection-level creation verb for the executable blueprint.
	  * <p>
	  * The returned Hub is empty until populated by application code using standard
	  * Hub operations (such as add, remove, and iteration) or by OA runtime operations
	  * such as {@link #select(Class, String, String, Object...)},
	  * {@link #detail(Hub, String)}, {@link #merge(Hub, String)}, or
	  * {@link #combine(Hub, Hub[])}.
	  *
	  * @param <T> the model object type contained by the Hub
	  * @param type the model class for the Hub
	  * @return a new live Hub for the supplied type
	  * @see Hub
	  */
	 <T extends OAObject> Hub<T> createHub(Class<T> type);
	
	 /**
	  * Persists the supplied object using the OA runtime's runtime and data sources.
	  * <p>
	  * {@code save(...)} writes the current state of the object through the
	  * OA model, applying all model-defined behavior such as relationships,
	  * rules, and lifecycle handling.
	  * <p>
	  * This is equivalent to calling {@code save()} on the {@link OAObject} itself.
	  * <p>
	  * Save operations follow model-defined relationships and will cascade to
	  * related objects that are configured for cascading (for example, owned or
	  * dependent objects).
	  * <p>
	  * If the object is new, it becomes part of the persistent OA runtime. Its new-state
	  * is cleared, and any model-defined identity value may be assigned.
	  * <p>
	  * The object must be part of this OA runtime. Changes made to the object and its
	  * related OA model structure are persisted according to the configured
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
	 * the OA model, applying all model-defined behavior such as relationships,
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
     * Deletes the supplied object from the OA runtime and underlying data sources.
     * <p>
     * {@code delete(...)} removes the object through the OA model, applying
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
     * The object must be part of this OA runtime.
     *
     * @param obj the object to delete
     */
    void delete(OAObject obj);


    /**
     * Deletes all objects contained in the supplied Hub.
     * <p>
     * {@code delete(...)} removes each object in the Hub through the OA model,
     * applying all model-defined behavior such as relationships, rules, and
     * lifecycle handling.
     * <p>
     * Each deleted object is removed from the OA runtime, including its relationships
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

    
	// =========== Get / Select ===========
    
    /**
     * Returns the object of the specified type for the given identity key.
     * <p>
     * {@code get(...)} locates a single object in the OA model using its
     * identity (for example, primary key or unique identifier). The returned
     * object is a live instance that participates fully in the OA runtime, including
     * relationships, Hub membership, observation, and persistence.
     * <p>
     * If the object is not already present in the OA runtime, it may be loaded through
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
     * {@code get(...)} locates a single object in the OA model using its
     * {@link OAObjectKey}. The returned object is a live instance that participates
     * fully in the OA runtime, including relationships, Hub membership, observation,
     * and persistence.
     * <p>
     * If the object is not already present in the OA runtime, it may be loaded through
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
     * {@code select(...)} queries the OA model for objects of the requested
     * type using the supplied selection criteria and ordering, and returns the
     * results in a {@link Hub}. The returned Hub is a live OA model structure whose
     * objects participate fully in normal OA model behavior, including
     * relationships, observation, and persistence.
     * <p>
     * The {@code where} expression is used to define the selection criteria, and
     * may include object property paths. The optional {@code orderBy} expression
     * defines the ordering of the selected objects.
     * <p>
     * Selection may use the OA runtime, configured data sources, or both,
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
     * affect the selection results. For live OA model-based searching or continuously
     * updated matching behavior, use the appropriate find/filter mechanisms instead.
     *
     * @param targetHub the Hub to populate with the selected objects
     * @param where the selection criteria, or {@code null} for no filtering
     * @param orderBy the ordering expression, or {@code null} for default ordering
     * @param args argument values referenced by the {@code where} expression
     */
    void select(Hub<?> targetHub, String where, String orderBy, Object... args);
    

    /**
     * Creates and returns an {@link OASelect} for advanced selection scenarios.
     * <p>
     * {@code getSelect(...)} exposes the lower-level {@link OASelect} used for
     * query-style selection so that application code can work with it directly
     * when more control is needed than the higher-level {@link #select(Class, String, String, Object...)}
     * methods provide.
     * <p>
     * Most application code should use {@code select(...)}. Use this method when
     * direct access to {@code OASelect} behavior is needed.
     *
     * @param <T> the model object type
     * @param type the model class to select
     * @param where the selection criteria, or {@code null} for no filtering
     * @param orderBy the ordering expression, or {@code null} for default ordering
     * @param args argument values referenced by the {@code where} expression
     * @return an {@link OASelect} configured for the supplied selection
     */
    <T extends OAObject> OASelect<T> getSelect(Class<T> type, String where, String orderBy, Object... args);

    
	// =========== Finder (Traversal) ===========
    
    /**
     * Creates an {@link OAFinder} that traverses the supplied path starting from
     * a single source object.
     * <p>
     * {@code finder(...)} is the OA model traversal/search verb for navigating live
     * object relationships in memory. Unlike {@link #select(Class, String, String, Object...)},
     * it does not represent a query result set. Instead, it follows the supplied
     * relationship path from the source object and allows application code to work
     * with the reachable objects.
     * <p>
     * Use this form when traversal should begin from one specific object rather than
     * from a Hub.
     *
     * @param <F> the source model object type
     * @param <T> the target model object type reached by the path
     * @param obj the source object used as the traversal starting point
     * @param toType the target model class reached by the path
     * @param path the relationship path from the source object to target objects
     * @return an {@link OAFinder} configured for the supplied traversal
     */
    <F extends OAObject, T extends OAObject> OAFinder<F, T> finder(F obj, Class<T> toType, String path);
    
    /**
     * Creates an {@link OAFinder} that traverses the supplied path starting from
     * either the active object or all objects in the source Hub.
     * <p>
     * {@code finder(...)} is the OA model traversal/search verb for navigating live
     * object relationships in memory. Unlike {@link #select(Class, String, String, Object...)},
     * it does not represent a query result set. Instead, it follows the supplied
     * relationship path from the source object(s) and allows application code to
     * work with the reachable objects.
     * <p>
     * When {@code useAllObjects} is {@code false}, traversal begins only from the
     * active object of {@code hub}. When {@code true}, traversal begins from all
     * objects currently contained in {@code hub}.
     *
     * @param <F> the source model object type
     * @param <T> the target model object type reached by the path
     * @param hub the source Hub used as the traversal starting point
     * @param toType the target model class reached by the path
     * @param path the relationship path from source objects to target objects
     * @param useAllObjects {@code true} to traverse from all objects in the Hub;
     *        {@code false} to traverse only from the Hub's active object
     * @return an {@link OAFinder} configured for the supplied traversal
     */
    <F extends OAObject, T extends OAObject> OAFinder<F, T> finder(Hub<F> hub, Class<T> toType, String path, boolean useAllObjects);

    
    
    // =========== Observability ===========
    
    /**
     * Adds a listener to the supplied Hub so that changes to the Hub can be observed.
     * <p>
     * {@code observe(...)} is the OA runtime-level verb for Hub observation. It registers
     * the supplied {@link HubListener} with the Hub so that application code can
     * react to changes in the Hub, its active object, and the objects it contains.
     * <p>
     * This is equivalent to calling {@code addListener(...)} on the {@link Hub}.
     * <p>
     * Observation is a live runtime feature of the OA model. It is commonly used
     * for UI wiring, runtime coordination, and other behavior that depends on Hub
     * changes as the executable blueprint is running.
     *
     * @param <T> the model object type contained by the Hub
     * @param hub the Hub to observe
     * @param hl the listener to add
     */
    <T extends OAObject> void observe(Hub<T> hub, HubListener<T> hl);
    
    
    
    // =========== Metadata (Blueprint) =========== 
    
    /**
     * Returns the blueprint metadata for the supplied model object type.
     * <p>
     * {@code info(...)} provides access to the runtime metadata that defines how
     * the supplied {@link OAObject} type participates in the OA model,
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
    
    /**
     * Returns the blueprint metadata for the supplied model object instance.
     * <p>
     * {@code info(...)} provides access to the runtime metadata that defines how
     * the supplied {@link OAObject} participates in the OA model, including
     * its properties, relationships, methods, and other model-defined structure.
     * <p>
     * This is the object-instance form of {@link #info(Class)} and is commonly used
     * when application code already has a live object and needs to inspect the
     * executable blueprint for that object's type.
     *
     * @param obj the model object whose type metadata should be returned
     * @return the runtime metadata for the supplied object's model type
     */
    OAObjectInfo info(OAObject obj);

    /**
     * Returns the blueprint metadata for the model object type contained by the
     * supplied Hub.
     * <p>
     * {@code info(...)} provides access to the runtime metadata that defines how
     * the objects contained in the supplied {@link Hub} participate in the
     * OA model, including their properties, relationships, methods, and other
     * model-defined structure.
     * <p>
     * This is the Hub form of {@link #info(Class)} and is commonly used when
     * application code is working with a live Hub and needs to inspect the
     * executable blueprint for its object type.
     *
     * @param hub the Hub whose contained object type metadata should be returned
     * @return the runtime metadata for the Hub's model object type
     */
    OAObjectInfo info(Hub<?> hub);

    /**
     * Returns the curated advanced service surface for this OA model.
     * <p>
     * The top-level {@code OA} methods are intentionally small,
     * application-facing verbs for common OA model work. {@code services()} is the
     * controlled public escape hatch for advanced operations that are still part
     * of the supported OA model API, but are too service-specific for the
     * top-level OA runtime shape.
     * <p>
     * The returned object exposes public service contracts from
     * {@code com.viaoa.OA runtime.api.services.*}. It is not a direct handle to the
     * internal implementation services under {@code com.viaoa.OA runtime.service.*},
     * {@code com.viaoa.OA runtime.service.object.*}, or
     * {@code com.viaoa.OA runtime.service.hub.*}. Internal services remain free to
     * expose broader runtime machinery without making those methods part of the
     * public OA model contract.
     *
     * @return the advanced public service facade for this OA runtime
     */
    ServicesOps services();

	InternalOps internal();

	ConfigOps config();
	
    void close();


    ModelUserOps modelUser();

    SessionUserOps sessionUser();
}

