package com.viaoa.graph;

import java.io.IOException;
import java.util.logging.Logger;

import com.viaoa.filter.OAFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.graph.api.ReplOps;
import com.viaoa.graph.api.SyncOps;
import com.viaoa.graph.api.internal.HubsInternalOps;
import com.viaoa.graph.api.internal.ObjectsInternalOps;
import com.viaoa.graph.api.internal.ReplInternalOps;
import com.viaoa.graph.api.internal.SyncInternalOps;
import com.viaoa.graph.api.internal.TriggerInternalOps;
import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.graph.service.OAReplicationService;
import com.viaoa.graph.service.OASyncService;
import com.viaoa.graph.service.OATriggerService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.auto.HubAutoMatch;
import com.viaoa.hub.copy.HubCopy;
import com.viaoa.hub.filter.HubFilter;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.hub.view.HubCombined;
import com.viaoa.hub.view.HubFlattened;
import com.viaoa.hub.view.HubGroupBy;
import com.viaoa.hub.view.HubLeftJoin;
import com.viaoa.hub.view.OAGroupBy;
import com.viaoa.hub.view.OALeftJoin;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.reflect.OAReflect;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.select.OASelect;
import com.viaoa.trigger.OATrigger;

/*qqqqqqqqqq
CODEX

#9 — boundary / ownership risk
  File/class/method: src/main/java/com/viaoa/graph/OAGraphImpl.java:136, create/get/save/delete/createHub
  Concern: graph verbs do not verify that the class/object/Hub belongs to the graph instance receiving the call.
  Why it matters: someGraph.create(ForeignObject.class) can route through this graph’s object services even when
  OARuntime.graph(ForeignObject.class) would resolve to another graph. That weakens graph ownership invariants.
  Minimal fix: add graph membership checks for class-based verbs, or document these as convenience delegates that
  may route by object/class graph.
  Invariant: GRAPH_VERBS_OPERATE_ON_OWNED_MODEL_TYPES
  Test coverage: two package graphs; call create/get/save/delete with foreign model classes and verify expected
  reject or reroute behavior.

 #12 — bug / lifecycle risk
  File/class/method: src/main/java/com/viaoa/graph/OAGraphImpl.java:61, initialize()
  Concern: bInit is set before service construction and package scanning complete.
  Why it matters: a partially initialized graph can report initialized if failure occurs after line 63. This is
  especially risky around package scanning/class loading.
  Minimal fix: set bInit = true only after all services are initialized and class scanning succeeds.
  Invariant: GRAPH_INITIALIZED_MEANS_ALL_CORE_SERVICES_READY
  Test coverage: inject/fake package scan failure and verify wasInitCalled() remains false and no partial graph is
  exposed.


#1 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/OAGraphImpl.java:111, initialize()
  Exact concern: bInit is set before service creation and package class scanning complete.
  Why it matters: a failed initialization can leave the graph reporting initialized while services/metadata are only
  partially ready.
  Minimal fix: set bInit = true only after all services are created, initialized, and package metadata scan
  succeeds.
  Suggested invariant: GRAPH_INITIALIZED_MEANS_ALL_SERVICES_READY
  Suggested test coverage: force class-scan failure and verify wasInitCalled() remains false and graph is not
  usable.

  #2 — boundary risk
  File/class/method: src/main/java/com/viaoa/graph/OAGraphImpl.java:107, constructor; src/main/java/com/viaoa/graph/
  OAGraphImpl.java:111, initialize()
  Exact concern: OAGraphImpl has a public constructor and public lifecycle method, so callers can bypass OARuntime
  package ownership and create unregistered graphs.
  Why it matters: OA 4.0 graph singleton/package ownership assumptions become unenforceable if apps can instantiate
  implementation graphs directly.
  Minimal fix: make construction/lifecycle package-owned if possible, or document OAGraphImpl as runtime-internal
  and test that public access goes through OARuntime.
  Suggested invariant: GRAPH_INSTANCES_ARE_RUNTIME_OWNED
  Suggested test coverage: graph created through OARuntime is canonical; direct implementation construction is
  unsupported or guarded.

  #3 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/OAGraphImpl.java:186, create/get/save/delete/createHub/select/
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
  File/class/method: src/main/java/com/viaoa/graph/OAGraphImpl.java:303, combine()
  Exact concern: combine() creates a HubCombined controller and discards the handle.
  Why it matters: the combined view has lifecycle behavior, including close/removal of listeners. The facade gives
  callers no way to manage that lifecycle.
  Minimal fix: return HubCombined<T> or document that combine() creates an unmanaged live binding.
  Suggested invariant: GRAPH_LIVE_VIEW_CONTROLLERS_HAVE_EXPLICIT_LIFECYCLE
  Suggested test coverage: combine creates live updates and can be closed or is documented as graph-owned/unmanaged.


 #12 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/OAGraphImpl.java:358, flatten(Hub)
  Exact concern: convenience flatten(Hub) creates a HubFlattened controller but returns only the target Hub.
  Why it matters: like combine, this hides the lifecycle handle for a live binding.
  Minimal fix: document lifecycle ownership or provide a handle-returning convenience form.
  Suggested invariant: GRAPH_CONVENIENCE_LIVE_VIEWS_DECLARE_CONTROLLER_OWNERSHIP
  Suggested test coverage: returned flattened Hub remains live and lifecycle behavior is defined.


*/



public class OAGraphImpl implements OAGraphInternal {
	private static Logger LOG = Logger.getLogger(OAGraphImpl.class.getName());

	private String packageName;
	private volatile boolean bInit;

	private OAObjectService srvcOAObject;
	private HubService srvcHub;
    private OASyncService srvcOASync;
    private OAReplicationService srvcOAReplication;
    private OATriggerService srvcOATrigger;

	public OAGraphImpl(String packageName) {
		this.packageName = packageName;
	}

	public void initialize() throws ClassNotFoundException, IOException {
		if (bInit) return;
		bInit = true;
		
	    final OAThreadService srvcThread = OARuntime.thread();

		srvcOAObject = new OAObjectService();
		srvcHub = new HubService();
	    srvcOASync = new OASyncService(this);
	    srvcOAReplication = new OAReplicationService() {
			@Override
			public void createClient(String guid, String tLogFileName, String replicationMasterHostName, int replicationMasterPort) {
				super.createClient(guid, srvcOASync.getServer(), tLogFileName, replicationMasterHostName, replicationMasterPort);
			}

			@Override
			public void createMaster(String guid, String tLogFileName) {
				super.createMaster(guid, srvcOASync.getServer(), tLogFileName);
			}
	    	
	    };
	    srvcOATrigger = new OATriggerService();

		srvcOAObject.initialize(srvcHub, srvcOASync, srvcThread.getThreadLocalService(), srvcThread.getRemoteThreadService());
		srvcHub.initialize(srvcOAObject, srvcOASync, srvcThread.getThreadLocalService(), srvcThread.getRemoteThreadService());

		if (packageName != null) {
			String[] classNames = OAReflect.getOAObjectClasses(packageName);
			for (String cn : classNames) {
				Class<?> c = Class.forName(packageName + "." + cn);
				if (OAObject.class.isAssignableFrom(c)) {
					srvcOAObject.getOAObjectInfoService().getObjectInfo(c);
				}
			}
		}
	}

	public String getPackageName() {
		return packageName;
	}

	public boolean wasInitCalled() {
		return bInit;
	}
    
	@Override
    public SyncOps sync() {
    	return srvcOASync;
    }

	@Override
    public ReplOps replication() {
    	return srvcOAReplication;
    }
	
	@Override
	public ObjectsInternalOps objectsInternal() {
		return srvcOAObject;
	}

	@Override
	public HubsInternalOps hubsInternal() {
		return srvcHub;
	}
	
	@Override
    public SyncInternalOps syncInternal() {
    	return srvcOASync;
    }
	
	@Override
    public ReplInternalOps replInternal() {
    	return srvcOAReplication;
    }

	@Override
	public TriggerInternalOps triggerInternal() {
		return srvcOATrigger;
	}
	
	
//qqqqqqqqqqqqqqqqqq OAGraph verbs	
	
	
	@Override
	public <T extends OAObject> T create(Class<T> type) {
		T obj = srvcOAObject.callObjectReflectCreateNewObject(type);
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
		T obj = srvcOAObject.callObjectReflectGetObject(type, key);
		return obj;
	}

	@Override
	public <T extends OAObject> T get(Class<T> type, OAObjectKey key) {
		T obj = srvcOAObject.callObjectReflectGetObject(type, key);
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
	public Hub<?> detail(Hub<?> hub, String path) {
		if (hub == null) return null;
		return hub.getDetailHub(path);
	}

	@Override
	public <T extends OAObject> void share(Hub<T> hub, Hub<T> hubToShare, boolean shareActiveObject) {
		if (hub == null) return;
		hub.setSharedHub(hubToShare, shareActiveObject);
	}

	@Override
	public void link(Hub<?> hub1, Hub<?> hub2, String referenceName) {
		if (hub1 == null) return;
		hub1.setLinkHub(hub2, referenceName);
	}

	
	

	@Override
	public <F extends OAObject, T extends OAObject> HubMerger<F, T> merge(Hub<F> hub, Hub<T> hubCombined, String path) {
		HubMerger<F, T> merger = new HubMerger<>(hub, hubCombined, path);
		return merger;
	}

	@Override
	public <F extends OAObject, T extends OAObject> HubMerger<F, T> merge(Hub<F> hubRoot, Hub<T> hubCombinedObjects, String path, boolean bShareActiveObject, String selectOrder, boolean bUseAll, boolean bIncludeRootHub, boolean bUseBackgroundThread) {
		HubMerger<F, T> merger = new HubMerger<F, T>(hubRoot, hubCombinedObjects, path, bShareActiveObject, selectOrder, bUseAll, bIncludeRootHub, bUseBackgroundThread);
		return merger;
	}
	
	
	
	
	@Override
	public <T extends OAObject> void combine(Hub<T> hubMaster, Hub<T>... hubs) {
		if (hubMaster == null) return;
		HubCombined<T> hc = new HubCombined<>(hubMaster, hubs);
		
	}

	@Override
	public <T extends OAObject> HubFilter<T> filter(Hub<T> hubMaster, Hub<T> hubFiltered) {
		if (hubMaster == null) return null;
		HubFilter<T> filter = new HubFilter<T>(hubMaster, hubFiltered);
		return filter;
	}

	@Override
	public <T extends OAObject> HubFilter<T> filter(Hub<T> hubMaster, Hub<T> hubFiltered, OAFilter<T> filter, String... dependentPropertyPaths) {
		if (hubMaster == null) return null;
		HubFilter<T> filterx = new HubFilter<T>(hubMaster, hubFiltered, filter, dependentPropertyPaths);
		return filterx;
	}

	
	@Override
	public <T extends OAObject, T2 extends OAObject> HubAutoMatch<T,T2> match(Hub<T> hub, String property, Hub<T2> hubMaster) {
		HubAutoMatch<T, T2> ham = new HubAutoMatch<>(hub, property, hubMaster);
		return ham;
	}

	@Override
	public <T extends OAObject> HubCopy<T> copy(Hub<T> hubFrom, Hub<T> hubTo) {
		HubCopy<T> hc = new HubCopy<>(hubFrom, hubTo, true);
		return hc;
	}

	public <T extends OAObject> HubCopy<T> copy(Hub<T> hubFrom, Hub<T> hubTo, boolean shareActiveObject) {
		HubCopy<T> hc = new HubCopy<>(hubFrom, hubTo, shareActiveObject);
		return hc;
	}
	

	@Override
	public <F extends OAObject, G extends OAObject> Hub<OAGroupBy<F, G>> groupBy(Hub<F> hubFrom, Hub<G> hubGrpBy, String propertyPath, boolean createNullList) {
		HubGroupBy<F,G> hgb = new HubGroupBy<F,G>(hubFrom, hubGrpBy, propertyPath, createNullList);
		Hub<OAGroupBy<F, G>> hx = hgb.getCombinedHub();
		return hx;
	}

	@Override
	public <T extends OAObject> HubFlattened<T> flatten(Hub<T> hubRoot, Hub<T> hubFlat) {
		if (hubRoot == null || hubFlat == null) return null;
		HubFlattened<T> hf = new HubFlattened<T>(hubRoot, hubFlat);
		return hf;
	}

	@Override
	public <T extends OAObject> Hub<T> flatten(Hub<T> hubRoot) {
		if (hubRoot == null) return null;
		Hub<T> hubFlat = new Hub<>(hubRoot.getObjectClass());
		HubFlattened<T> hf = new HubFlattened<T>(hubRoot, hubFlat);
		return hubFlat;
	}

	@Override
	public <A extends OAObject, B extends OAObject> Hub<OALeftJoin<A, B>> leftJoin(Hub<A> hubLeft, Hub<B> hub, String propertyPath, boolean shareActiveObject) {
		HubLeftJoin<A,B> hlj = new HubLeftJoin<A,B>(hubLeft, hub, propertyPath, shareActiveObject);
		return hlj.getCombinedHub();
	}

	@Override
	public OAObjectInfo info(Class<? extends OAObject> type) {
		OAObjectInfo oi = srvcOAObject.callObjectInfoGetOAObjectInfo(type);
		return oi;
	}

	@Override
	public OAObjectInfo info(OAObject obj) {
		Class<?>  c = obj == null ? null : obj.getClass();
		OAObjectInfo oi = srvcOAObject.callObjectInfoGetOAObjectInfo(c);
		return oi;
	}

	@Override
	public OAObjectInfo info(Hub<?> hub) {
		Class<?>  c = hub == null ? null : hub.getObjectClass();
		OAObjectInfo oi = srvcOAObject.callObjectInfoGetOAObjectInfo(c);
		return oi;
	}

	@Override
	public void addTrigger(OATrigger trigger) {
		triggerInternal().addTrigger(trigger);
	}

	@Override
	public void removeTrigger(OATrigger trigger) {
		triggerInternal().removeTrigger(trigger);
	}

}

