package com.viaoa.graph.service.facade;

import com.viaoa.filter.OAFilter;
import com.viaoa.graph.api.services.HubsOps;
import com.viaoa.graph.service.HubInternalService;
import com.viaoa.hub.Hub;
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
import com.viaoa.object.OAObject;

public class HubsOpsImpl implements HubsOps {

	private final HubInternalService srvc;
	
	public HubsOpsImpl(HubInternalService srvc) {
		this.srvc = srvc;
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
	public <T extends OAObject> HubCombined<T> combine(Hub<T> hubMaster, Hub<T>... hubs) {
		if (hubMaster == null) return null;
		HubCombined<T> hc = new HubCombined<>(hubMaster, hubs);
		return hc;
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
	
	
}
