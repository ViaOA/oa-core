package com.viaoa.graph;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.graph.api.ReplOps;
import com.viaoa.graph.api.SyncOps;
import com.viaoa.graph.api.internal.HubsInternalOps;
import com.viaoa.graph.api.internal.ObjectsInternalOps;
import com.viaoa.graph.api.internal.ReplInternalOps;
import com.viaoa.graph.api.internal.SyncInternalOps;
import com.viaoa.graph.context.OAContext;
import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.graph.service.OAReplicationService;
import com.viaoa.graph.service.OASyncService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAutoMatch;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubFilter;
import com.viaoa.hub.HubFlattened;
import com.viaoa.hub.HubGroupBy;
import com.viaoa.hub.HubLeftJoin;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubMerger;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAGroupBy;
import com.viaoa.object.OALeftJoin;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAReflect;

public class OAGraphImpl implements OAGraphInternal {
	private static Logger LOG = Logger.getLogger(OAGraphImpl.class.getName());

	private String packageName;
	private volatile boolean bInit;

	private OAObjectService srvcOAObject;
	private HubService srvcHub;
    private OASyncService srvcOASync;
    private OAReplicationService srvcOAReplication;
    private OAContext srvcOAContext;

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
	    srvcOAContext = new OAContext();

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
    

/*	
	public ObjectsOps objects() {
		return srvcOAObject;
	}

	@Override
	public HubsOps hubs() {
		return srvcHub;
	}
*/	
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


//qqqqqqqqqqqqqqqq	
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
		if (hub == null) return;
		hub.addHubListener(hl);
	}

	@Override
	public Hub<?> detail(Hub<?> hub, String path) {
		if (hub == null) return null;
		return hub.getDetailHub(path);
	}

	@Override
	public <T extends OAObject> void share(Hub<T> hub, Hub<T> hub2, boolean shareActiveObject) {
		if (hub == null) return;
		hub.setSharedHub(hub2);
	}

	@Override
	public void link(Hub<?> hub1, Hub<?> hub2, String referenceName) {
		if (hub1 == null) return;
		hub1.setLinkHub(hub2, referenceName);
	}

	
	

	@Override
	public <F extends OAObject, T extends OAObject> HubMerger<F, T> merge(Hub<F> hub, Hub<T> hubCombined, String path) {
		HubMerger<F, T> merger = new HubMerger(hub, hubCombined, path);
		return merger;
	}

	@Override
	public <F extends OAObject, T extends OAObject> HubMerger<F, T> merge(Hub<F> hubRoot, Hub<T> hubCombinedObjects, String path, boolean bShareActiveObject, String selectOrder, boolean bUseAll, boolean bIncludeRootHub, boolean bUseBackgroundThread) {
		HubMerger<F, T> merger = new HubMerger<F, T>(hubRoot, hubCombinedObjects, path, bShareActiveObject, bUseAll, bIncludeRootHub);
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
	public <T extends OAObject> void copy(Hub<T> hubFrom, Hub<T> hubTo) {
		HubCopy<T> hc = new HubCopy<>(hubFrom, hubTo, true);
	}

	public <T extends OAObject> void copy(Hub<T> hubFrom, Hub<T> hubTo, boolean shareActiveObject) {
		HubCopy<T> hc = new HubCopy<>(hubFrom, hubTo, shareActiveObject);
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
	public OAContext context() {
		return srvcOAContext;
	}





}

