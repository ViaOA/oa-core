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

/*qqqqqqqqqq
CODEX

#9 — boundary / ownership risk
  File/class/method: src/main/java/com/viaoa/graph/OAImpl.java:136, create/get/save/delete/createHub
  Concern: graph verbs do not verify that the class/object/Hub belongs to the graph instance receiving the call.
  Why it matters: someGraph.create(ForeignObject.class) can route through this graph’s object services even when
  OARuntime.graph(ForeignObject.class) would resolve to another graph. That weakens graph ownership invariants.
  Minimal fix: add graph membership checks for class-based verbs, or document these as convenience delegates that
  may route by object/class graph.
  Invariant: GRAPH_VERBS_OPERATE_ON_OWNED_MODEL_TYPES
  Test coverage: two package graphs; call create/get/save/delete with foreign model classes and verify expected
  reject or reroute behavior.

 #12 — bug / lifecycle risk
  File/class/method: src/main/java/com/viaoa/graph/OAImpl.java:61, initialize()
  Concern: bInit is set before service construction and package scanning complete.
  Why it matters: a partially initialized graph can report initialized if failure occurs after line 63. This is
  especially risky around package scanning/class loading.
  Minimal fix: set bInit = true only after all services are initialized and class scanning succeeds.
  Invariant: GRAPH_INITIALIZED_MEANS_ALL_CORE_SERVICES_READY
  Test coverage: inject/fake package scan failure and verify wasInitCalled() remains false and no partial graph is
  exposed.


#1 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/OAImpl.java:111, initialize()
  Exact concern: bInit is set before service creation and package class scanning complete.
  Why it matters: a failed initialization can leave the graph reporting initialized while services/metadata are only
  partially ready.
  Minimal fix: set bInit = true only after all services are created, initialized, and package metadata scan
  succeeds.
  Suggested invariant: GRAPH_INITIALIZED_MEANS_ALL_SERVICES_READY
  Suggested test coverage: force class-scan failure and verify wasInitCalled() remains false and graph is not
  usable.

  #2 — boundary risk
  File/class/method: src/main/java/com/viaoa/graph/OAImpl.java:107, constructor; src/main/java/com/viaoa/graph/
  OAImpl.java:111, initialize()
  Exact concern: OAImpl has a public constructor and public lifecycle method, so callers can bypass OARuntime
  package ownership and create unregistered graphs.
  Why it matters: OA 4.0 graph singleton/package ownership assumptions become unenforceable if apps can instantiate
  implementation graphs directly.
  Minimal fix: make construction/lifecycle package-owned if possible, or document OAImpl as runtime-internal
  and test that public access goes through OARuntime.
  Suggested invariant: GRAPH_INSTANCES_ARE_RUNTIME_OWNED
  Suggested test coverage: graph created through OARuntime is canonical; direct implementation construction is
  unsupported or guarded.

  #3 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/OAImpl.java:186, create/get/save/delete/createHub/select/
  info
  Exact concern: facade verbs do not validate that the class/object/Hub belongs to this graph’s package.
  Why it matters: graphA.create(ForeignObject.class) can execute through graph A’s services even though runtime
  ownership belongs to graph B.
  Minimal fix: add graph-membership checks, or explicitly define these verbs as convenience delegates that may
  reroute by runtime graph ownership.
  Suggested invariant: GRAPH_VERBS_OPERATE_ON_OWNED_TYPES
  Suggested test coverage: two package graphs; invoke verbs with foreign classes/objects/Hubs and verify reject or
  documented reroute.

  #11 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/OAImpl.java:303, combine()
  Exact concern: combine() creates a HubCombined controller and discards the handle.
  Why it matters: the combined view has lifecycle behavior, including close/removal of listeners. The facade gives
  callers no way to manage that lifecycle.
  Minimal fix: return HubCombined<T> or document that combine() creates an unmanaged live binding.
  Suggested invariant: GRAPH_LIVE_VIEW_CONTROLLERS_HAVE_EXPLICIT_LIFECYCLE
  Suggested test coverage: combine creates live updates and can be closed or is documented as graph-owned/unmanaged.


 #12 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/OAImpl.java:358, flatten(Hub)
  Exact concern: convenience flatten(Hub) creates a HubFlattened controller but returns only the target Hub.
  Why it matters: like combine, this hides the lifecycle handle for a live binding.
  Minimal fix: document lifecycle ownership or provide a handle-returning convenience form.
  Suggested invariant: GRAPH_CONVENIENCE_LIVE_VIEWS_DECLARE_CONTROLLER_OWNERSHIP
  Suggested test coverage: returned flattened Hub remains live and lifecycle behavior is defined.


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

    
	public OAImpl(String packageName) {
		this.packageName = packageName;
	}

	public void initialize() throws ClassNotFoundException, IOException {
		if (bInit) return;
		_initialize();
		bInit = true;
	}
	
	protected void _initialize() throws ClassNotFoundException, IOException {
		srvcObjectParent = new OAObjectParentService();
		srvcHubParent = new HubParentService();
		
		srvcConfig = new OAConfigService();
		
	    srvcOASyncInternal = new OASyncService(this);

	    srvcOAReplicationInternal = new OAReplicationService() {
			@Override
			public void createClient(String guid, String tLogFileName, String replicationMasterHostName, int replicationMasterPort) {
				super.createClient(guid, srvcOASyncInternal.getServer(), tLogFileName, replicationMasterHostName, replicationMasterPort);
			}

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

	public String getPackageName() {
		return packageName;
	}

	public boolean wasInitCalled() {
		return bInit;
	}
    
	@Override
    public SyncOps sync() {
    	return srvcOASyncInternal;
    }

	@Override
    public ReplicationOps replication() {
    	return srvcOAReplicationInternal;
    }
	
	@Override
	public <T extends OAObject> T create(Class<T> type) {
		T obj = internal().objects().reflect().createNewObject(type);
		return obj;
	}

	@Override
	public <T extends OAObject> Hub<T> createHub(Class<T> type) {
		return new Hub<T>(type);
	}

	@Override
	public void save(OAObject obj) {
		if (obj != null) obj.save();
	}

	@Override
	public void save(Hub<?> hub) {
		if (hub != null) hub.saveAll();
	}

	@Override
	public void delete(OAObject obj) {
		if (obj != null) obj.delete();
	}

	@Override
	public void delete(Hub<?> hub) {
		if (hub != null) hub.deleteAll();
	}

	@Override
	public <T extends OAObject> T get(Class<T> type, Object key) {
		T obj = internal().objects().reflect().getObject(type, key);
		return obj;
	}

	@Override
	public <T extends OAObject> T get(Class<T> type, OAObjectKey key) {
		T obj = internal().objects().reflect().getObject(type, key);
		return obj;
	}

	@Override
	public <T extends OAObject> Hub<T> select(Class<T> type, String where, String orderBy, Object... args) {
		if (type == null) return null;
		Hub<T> hub = new Hub<T>(type);
		hub.select(where, args, orderBy);
		return hub;
	}

	@Override
	public void select(Hub<?> hub, String where, String orderBy, Object... args) {
		if (hub == null) return;
		hub.select(where, args, orderBy);
	}

	@Override
	public <T extends OAObject> OASelect<T> getSelect(Class<T> type, String where, String orderBy, Object... args) {
		OASelect<T> sel = new OASelect<>(type, where, args, orderBy);
		return sel;
	}


	@Override
	public <F extends OAObject, T extends OAObject> OAFinder<F, T> finder(F obj, Class<T> toType, String path) {
		OAFinder<F, T> finder = new OAFinder<F, T>(obj, path);
		return finder;
	}

	@Override
	public <F extends OAObject, T extends OAObject> OAFinder<F, T> finder(Hub<F> hub, Class<T> toType, String path, boolean bUseAll) {
		OAFinder<F, T> finder = new OAFinder<F, T>(hub, path, bUseAll);
		return finder;
	}
	
	@Override
	public <T extends OAObject> void observe(Hub<T> hub, HubListener<T> hl) {
		if (hub == null || hl == null) return;
		hub.addHubListener(hl);
	}



	@Override
	public OAObjectInfo info(Class<? extends OAObject> type) {
		OAObjectInfo oi = internal().objects().info().getOAObjectInfo(type);
		return oi;
	}

	@Override
	public OAObjectInfo info(OAObject obj) {
		Class<?>  c = obj == null ? null : obj.getClass();
		OAObjectInfo oi = internal().objects().info().getOAObjectInfo(c);
		return oi;
	}

	@Override
	public OAObjectInfo info(Hub<?> hub) {
		Class<?>  c = hub == null ? null : hub.getObjectClass();
		OAObjectInfo oi = internal().objects().info().getOAObjectInfo(c);
		return oi;
	}

	@Override
	public ServicesOps services() {
		return srvcServices;
	}

	@Override
	public InternalOps internal() {
		return srvcInternal;
	}

	private boolean bClosed;
	
	@Override
	public void close() {
		if (bClosed) return;
		bClosed = true;
		// srvcObjectParent.getOAObjectCacheService().close();
		// todo: might need to create & call close on child services		
	}

	@Override
	public ConfigOps config() {
		return srvcConfig;
	}

	@Override
	public ModelUserOps modelUser() {
		return srvcModelUser;
	}

	@Override
	public SessionUserOps sessionUser() {
		return srvcSessionUser;
	}
	
}

