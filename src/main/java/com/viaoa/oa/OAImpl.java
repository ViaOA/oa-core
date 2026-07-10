package com.viaoa.oa;

import java.io.IOException;
import java.util.logging.Logger;

import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListener;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.oa.api.ConfigOps;
import com.viaoa.oa.api.ModelUserOps;
import com.viaoa.oa.api.ReplicationOps;
import com.viaoa.oa.api.SessionUserOps;
import com.viaoa.oa.api.SyncOps;
import com.viaoa.oa.api.internal.InternalOps;
import com.viaoa.oa.api.services.ServicesOps;
import com.viaoa.oa.internal.facade.InternalOpsImpl;
import com.viaoa.oa.service.OAConfigService;
import com.viaoa.oa.service.OAModelUserService;
import com.viaoa.oa.service.OAReplicationService;
import com.viaoa.oa.service.OASessionUserService;
import com.viaoa.oa.service.OASyncService;
import com.viaoa.oa.service.OATriggerService;
import com.viaoa.oa.service.facade.ServicesOpsImpl;
import com.viaoa.oa.service.hub.HubParentService;
import com.viaoa.oa.service.object.OAObjectParentService;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.reflect.OAReflect;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;




/**
 * Default OA runtime implementation.
 * <p>
 * This class wires the public {@link OA} runtime facade to object, Hub, sync,
 * replication, trigger, model-user, session-user, service, and internal
 * operation services for one OA model package.
 */
public class OAImpl implements OA {
	private static Logger LOG = Logger.getLogger(OAImpl.class.getName());

	private String packageName;
	private volatile boolean bInit;

    private ServicesOps srvcServices;
    private InternalOps srvcInternal;
	
    private OASyncService srvcOASyncInternal;
    private OAReplicationService srvcOAReplicationInternal;
    private OATriggerService srvcOATrigger;

	private OAObjectParentService srvcObjectParent;
	private HubParentService srvcHubParent;
	
	private ConfigOps srvcConfig;
	private ModelUserOps srvcModelUser;
	private SessionUserOps srvcSessionUser;

    
	/**
	 * Creates an OA runtime implementation for a model package.
	 *
	 * @param packageName the model package name
	 */
	public OAImpl(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * Initializes this OA runtime and wires its service facades.
	 * <p>
	 * Repeated calls return without reinitializing an already initialized runtime.
	 *
	 * @throws ClassNotFoundException if a model class cannot be loaded
	 * @throws IOException if model class discovery fails
	 */
	public void initialize() throws ClassNotFoundException, IOException {
		if (bInit) return;
		_initialize();
		bInit = true;
	}
	
	/**
	 * Performs the one-time service construction and facade wiring for this runtime.
	 *
	 * @throws ClassNotFoundException if a model class cannot be loaded
	 * @throws IOException if model class discovery fails
	 */
	protected void _initialize() throws ClassNotFoundException, IOException {
		srvcObjectParent = new OAObjectParentService();
		srvcHubParent = new HubParentService();
		
		srvcConfig = new OAConfigService();
		
	    srvcOASyncInternal = new OASyncService(this);

	    srvcOAReplicationInternal = new OAReplicationService() {
			/**
			 * Creates a replication client using this runtime's sync server.
			 *
			 * @param guid the replication identity
			 * @param tLogFileName the transaction log file name
			 * @param replicationMasterHostName the replication master host name
			 * @param replicationMasterPort the replication master port
			 */
			@Override
			public void createClient(String guid, String tLogFileName, String replicationMasterHostName, int replicationMasterPort) {
				super.createClient(guid, srvcOASyncInternal.getServer(), tLogFileName, replicationMasterHostName, replicationMasterPort);
			}

			/**
			 * Creates a replication master using this runtime's sync server.
			 *
			 * @param guid the replication identity
			 * @param tLogFileName the transaction log file name
			 */
			@Override
			public void createMaster(String guid, String tLogFileName) {
				super.createMaster(guid, srvcOASyncInternal.getServer(), tLogFileName);
			}
	    };
	    srvcOATrigger = new OATriggerService(this);

		srvcObjectParent.initialize(srvcHubParent, srvcOASyncInternal, OARuntime.thread().getThreadLocalService(), OARuntime.thread().getRemoteThreadService(), srvcOATrigger);
		srvcHubParent.initialize(srvcObjectParent, srvcOASyncInternal, OARuntime.thread().getThreadLocalService(), OARuntime.thread().getRemoteThreadService());
	    
		
	    srvcServices = new ServicesOpsImpl(
			new com.viaoa.oa.service.facade.HubsOpsImpl(srvcHubParent),
			new com.viaoa.oa.service.facade.ObjectsOpsImpl(srvcObjectParent),
			new com.viaoa.oa.service.facade.TriggersOpsImpl(srvcOATrigger),
			new com.viaoa.oa.service.facade.RulesOpsImpl(srvcObjectParent.getOAObjectRulesService())
		);

	    srvcInternal = new InternalOpsImpl(
			new com.viaoa.oa.internal.facade.HubsOpsImpl(srvcHubParent),
			new com.viaoa.oa.internal.facade.ObjectsOpsImpl(srvcObjectParent),
			new com.viaoa.oa.internal.facade.TriggersOpsImpl(srvcOATrigger), 
			srvcOASyncInternal,
			srvcOAReplicationInternal
		);
	    
	    
		if (packageName != null) {
			String[] classNames = OAReflect.getOAObjectClasses(packageName);
			for (String cn : classNames) {
				Class<? extends OAObject> c = (Class<? extends OAObject>) Class.forName(packageName + "." + cn);
				if (OAObject.class.isAssignableFrom(c)) {
					OAObjectInfo oi = internal().objects().info().getObjectInfo(c);
					if (oi.getModelUserClass()) {
						if (srvcModelUser == null) srvcModelUser = new OAModelUserService(this, c);
					}
				}
			}
		}
		
		if (srvcModelUser == null) srvcModelUser = new OAModelUserService(this, null);
		srvcSessionUser = new OASessionUserService(this);
	}

	/**
	 * Returns the model package name for this OA runtime.
	 *
	 * @return the model package name
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * Returns whether {@link #initialize()} has completed for this runtime.
	 *
	 * @return {@code true} if initialization has completed
	 */
	public boolean wasInitCalled() {
		return bInit;
	}
    
	/**
	 * Returns synchronization operations for this runtime.
	 *
	 * @return the sync operations facade
	 */
	@Override
    public SyncOps sync() {
    	return srvcOASyncInternal;
    }

	/**
	 * Returns replication operations for this runtime.
	 *
	 * @return the replication operations facade
	 */
	@Override
    public ReplicationOps replication() {
    	return srvcOAReplicationInternal;
    }
	
	/**
	 * Creates a model object through the object services for this runtime.
	 *
	 * @param <T> the object type
	 * @param type the object class
	 * @return the new object instance
	 */
	@Override
	public <T extends OAObject> T create(Class<T> type) {
		T obj = internal().objects().reflect().createNewObject(type);
		return obj;
	}

	/**
	 * Creates a Hub for a model object type.
	 *
	 * @param <T> the object type
	 * @param type the object class
	 * @return the new Hub
	 */
	@Override
	public <T extends OAObject> Hub<T> createHub(Class<T> type) {
		return new Hub<T>(type);
	}

	/**
	 * Saves an object through its OAObject save behavior.
	 *
	 * @param obj the object to save
	 */
	@Override
	public void save(OAObject obj) {
		if (obj != null) obj.save();
	}

	/**
	 * Saves all objects in a Hub.
	 *
	 * @param hub the Hub whose objects are saved
	 */
	@Override
	public void save(Hub<?> hub) {
		if (hub != null) hub.saveAll();
	}

	/**
	 * Deletes an object through its OAObject delete behavior.
	 *
	 * @param obj the object to delete
	 */
	@Override
	public void delete(OAObject obj) {
		if (obj != null) obj.delete();
	}

	/**
	 * Deletes all objects in a Hub.
	 *
	 * @param hub the Hub whose objects are deleted
	 */
	@Override
	public void delete(Hub<?> hub) {
		if (hub != null) hub.deleteAll();
	}

	/**
	 * Returns an object by class and key value.
	 *
	 * @param <T> the object type
	 * @param type the object class
	 * @param key the key value
	 * @return the matching object, or {@code null}
	 */
	@Override
	public <T extends OAObject> T get(Class<T> type, Object key) {
		T obj = internal().objects().reflect().getObject(type, key);
		return obj;
	}

	/**
	 * Returns an object by class and OAObjectKey.
	 *
	 * @param <T> the object type
	 * @param type the object class
	 * @param key the object key
	 * @return the matching object, or {@code null}
	 */
	@Override
	public <T extends OAObject> T get(Class<T> type, OAObjectKey key) {
		T obj = internal().objects().reflect().getObject(type, key);
		return obj;
	}

	/**
	 * Selects objects of a class into a new Hub.
	 *
	 * @param <T> the object type
	 * @param type the object class
	 * @param where optional where clause
	 * @param orderBy optional order clause
	 * @param args where-clause arguments
	 * @return the selected Hub
	 */
	@Override
	public <T extends OAObject> Hub<T> select(Class<T> type, String where, String orderBy, Object... args) {
		if (type == null) return null;
		Hub<T> hub = new Hub<T>(type);
		hub.select(where, args, orderBy);
		return hub;
	}

	/**
	 * Selects objects into an existing Hub.
	 *
	 * @param hub the Hub to populate
	 * @param where optional where clause
	 * @param orderBy optional order clause
	 * @param args where-clause arguments
	 */
	@Override
	public void select(Hub<?> hub, String where, String orderBy, Object... args) {
		if (hub == null) return;
		hub.select(where, args, orderBy);
	}

	/**
	 * Creates an OASelect for a class and query options.
	 *
	 * @param <T> the object type
	 * @param type the object class
	 * @param where optional where clause
	 * @param orderBy optional order clause
	 * @param args where-clause arguments
	 * @return the configured select
	 */
	@Override
	public <T extends OAObject> OASelect<T> getSelect(Class<T> type, String where, String orderBy, Object... args) {
		OASelect<T> sel = new OASelect<>(type, where, args, orderBy);
		return sel;
	}


	/**
	 * Creates a finder from a source object and relationship path.
	 *
	 * @param <F> the source object type
	 * @param <T> the target object type
	 * @param obj the source object
	 * @param toType the target class
	 * @param path the relationship path
	 * @return the finder
	 */
	@Override
	public <F extends OAObject, T extends OAObject> OAFinder<F, T> finder(F obj, Class<T> toType, String path) {
		OAFinder<F, T> finder = new OAFinder<F, T>(obj, path);
		return finder;
	}

	/**
	 * Creates a finder from a Hub and relationship path.
	 *
	 * @param <F> the source object type
	 * @param <T> the target object type
	 * @param hub the source Hub
	 * @param toType the target class
	 * @param path the relationship path
	 * @param bUseAll {@code true} to use all Hub objects; {@code false} to use the active object
	 * @return the finder
	 */
	@Override
	public <F extends OAObject, T extends OAObject> OAFinder<F, T> finder(Hub<F> hub, Class<T> toType, String path, boolean bUseAll) {
		OAFinder<F, T> finder = new OAFinder<F, T>(hub, path, bUseAll);
		return finder;
	}
	
	/**
	 * Adds a listener to a Hub.
	 *
	 * @param <T> the Hub object type
	 * @param hub the Hub to observe
	 * @param hl the listener to add
	 */
	@Override
	public <T extends OAObject> void observe(Hub<T> hub, HubListener<T> hl) {
		if (hub == null || hl == null) return;
		hub.addHubListener(hl);
	}



	/**
	 * Returns metadata for an object class.
	 *
	 * @param type the object class
	 * @return the object metadata
	 */
	@Override
	public OAObjectInfo info(Class<? extends OAObject> type) {
		OAObjectInfo oi = internal().objects().info().getOAObjectInfo(type);
		return oi;
	}

	/**
	 * Returns metadata for an object instance.
	 *
	 * @param obj the object instance
	 * @return the object metadata
	 */
	@Override
	public OAObjectInfo info(OAObject obj) {
		Class<?>  c = obj == null ? null : obj.getClass();
		OAObjectInfo oi = internal().objects().info().getOAObjectInfo(c);
		return oi;
	}

	/**
	 * Returns metadata for a Hub object class.
	 *
	 * @param hub the Hub
	 * @return the object metadata
	 */
	@Override
	public OAObjectInfo info(Hub<?> hub) {
		Class<?>  c = hub == null ? null : hub.getObjectClass();
		OAObjectInfo oi = internal().objects().info().getOAObjectInfo(c);
		return oi;
	}

	/**
	 * Returns curated public and advanced services.
	 *
	 * @return the services facade
	 */
	@Override
	public ServicesOps services() {
		return srvcServices;
	}

	/**
	 * Returns OA-library/runtime internal operations.
	 *
	 * @return the internal facade
	 */
	@Override
	public InternalOps internal() {
		return srvcInternal;
	}

	private boolean bClosed;
	
	/**
	 * Closes this runtime.
	 */
	@Override
	public void close() {
		if (bClosed) return;
		bClosed = true;
		// srvcObjectParent.getOAObjectCacheService().close();
		// todo: might need to create & call close on child services		
	}

	/**
	 * Returns runtime configuration operations.
	 *
	 * @return the configuration facade
	 */
	@Override
	public ConfigOps config() {
		return srvcConfig;
	}

	/**
	 * Returns model-user operations.
	 *
	 * @return the model-user facade
	 */
	@Override
	public ModelUserOps modelUser() {
		return srvcModelUser;
	}

	/**
	 * Returns session-user operations.
	 *
	 * @return the session-user facade
	 */
	@Override
	public SessionUserOps sessionUser() {
		return srvcSessionUser;
	}
	
}

