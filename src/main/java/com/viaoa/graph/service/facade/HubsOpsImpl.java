package com.viaoa.graph.service.facade;

import java.util.ArrayList;

import com.viaoa.filter.OAFilter;
import com.viaoa.graph.api.services.HubsOps;
import com.viaoa.graph.api.services.hubs.HubAOOps;
import com.viaoa.graph.api.services.hubs.HubAutoMatchOps;
import com.viaoa.graph.api.services.hubs.HubCombineOps;
import com.viaoa.graph.api.services.hubs.HubCopyOps;
import com.viaoa.graph.api.services.hubs.HubDataOps;
import com.viaoa.graph.api.services.hubs.HubDetailOps;
import com.viaoa.graph.api.services.hubs.HubFilterOps;
import com.viaoa.graph.api.services.hubs.HubLinkOps;
import com.viaoa.graph.api.services.hubs.HubMergeOps;
import com.viaoa.graph.api.services.hubs.HubRootOps;
import com.viaoa.graph.api.services.hubs.HubShareOps;
import com.viaoa.graph.api.services.hubs.HubStatusOps;
import com.viaoa.graph.api.services.hubs.HubViewOps;
import com.viaoa.graph.service.HubInternalService;
import com.viaoa.graph.service.hub.HubStatusService.HubCurrentStateEnum;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListenerAdapter;
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
	
	private HubAutoMatchOps opsAutomatch;
	private HubDetailOps opsDetail;
	private HubFilterOps opsFilter;
	private HubLinkOps opsLink;
	private HubMergeOps opsMerge;
	private HubShareOps opsShare;
	private HubViewOps opsView;
	private HubCopyOps opsCopy;
	private HubCombineOps opsCombine;
	private HubAOOps opsAO;
	private HubDataOps opsData;
	private HubStatusOps opsStatus;
	private HubRootOps opsRoot;
	
	public HubsOpsImpl(HubInternalService srvc) {
		this.srvc = srvc;
	}

	
	@Override
	public HubAutoMatchOps autoMatch() {
		if (opsAutomatch != null) return opsAutomatch;
		opsAutomatch = new HubAutoMatchOps() {
			@Override
			public <T extends OAObject, T2 extends OAObject> HubAutoMatch<T, T2> match(Hub<T> hub, String property, Hub<T2> hubMaster) {
				HubAutoMatch<T, T2> ham = new HubAutoMatch<>(hub, property, hubMaster);
				return ham;
			}
		}; 
		return opsAutomatch;
	}
	
	@Override
	public HubDetailOps detail() {
		if (opsDetail != null) return opsDetail;
		opsDetail = new HubDetailOps() {
			@Override
			public Hub<?> detail(Hub<?> hub, String path) {
				if (hub == null) return null;
				return hub.getDetailHub(path);
			}

			@Override
			public <T extends OAObject> void preloadDetailData(Hub<T> thisHub, int pos) {
				srvc.getHubDetailService().preloadDetailData(thisHub, pos);
			}
		};
		return opsDetail;
	}

	@Override
	public HubFilterOps filter() {
		if (opsFilter != null) return opsFilter;
		opsFilter = new HubFilterOps() {
			
			@Override
			public <T extends OAObject> HubFilter<T> filter(Hub<T> hubMaster, Hub<T> hub, OAFilter<T> filter, String... dependentPropertyPaths) {
				if (hubMaster == null) return null;
				HubFilter<T> filterx = new HubFilter<T>(hubMaster, hub, filter, dependentPropertyPaths);
				return filterx;
			}
			
			@Override
			public <T extends OAObject> HubFilter<T> filter(Hub<T> hubMaster, Hub<T> hubFiltered) {
				if (hubMaster == null) return null;
				HubFilter<T> filter = new HubFilter<T>(hubMaster, hubFiltered);
				return filter;
			}
		};
		return opsFilter;
	}

	@Override
	public HubLinkOps link() {
		if (opsLink != null) return opsLink;
		opsLink = new HubLinkOps() {
			@Override
			public void link(Hub<?> hub1, Hub<?> hub2, String referenceName) {
				if (hub1 == null) return;
				hub1.setLinkHub(hub2, referenceName);
			}

			@Override
			public <T extends OAObject> Hub<T> getHubWithLink(Hub<T> thisHub, boolean bIncludeCopiedHubs) {
				return srvc.getHubLinkService().getHubWithLink(thisHub, bIncludeCopiedHubs);
			}
		};
		
		return opsLink;
	}

	@Override
	public HubMergeOps merge() {
		if (opsMerge != null) return opsMerge;
		opsMerge = new HubMergeOps() {
			@Override
			public <F extends OAObject, T extends OAObject> HubMerger<F, T> merge(Hub<F> hubRoot, Hub<T> mergedHub, String path, boolean shareActiveObject, String selectOrder, boolean useAllObjects, boolean includeRootHub, boolean useBackgroundThread) {
				HubMerger<F, T> merger = new HubMerger<F, T>(hubRoot, mergedHub, path, shareActiveObject, selectOrder, useAllObjects, includeRootHub, useBackgroundThread);
				return merger;
			}
			@Override
			public <F extends OAObject, T extends OAObject> HubMerger<F, T> merge(Hub<F> hub, Hub<T> hubCombined, String path) {
				HubMerger<F, T> merger = new HubMerger<>(hub, hubCombined, path);
				return merger;
			}
		};
		return opsMerge;
	}

	@Override
	public HubShareOps share() {
		if (opsShare != null) return opsShare;
		opsShare = new HubShareOps() {
			@Override
			public <T extends OAObject> void share(Hub<T> hub, Hub<T> hubToShare, boolean shareActiveObject) {
				if (hub == null) return;
				hub.setSharedHub(hubToShare, shareActiveObject);
			}
		};
		return opsShare;
	}

	@Override
	public HubViewOps view() {
		if (opsView != null) return opsView;
		opsView = new HubViewOps() {
			
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
		};
		return opsView;
	}


	@Override
	public HubCopyOps copy() {
		if (opsCopy != null) return opsCopy;
		opsCopy = new HubCopyOps() {
			@Override
			public <T extends OAObject> HubCopy<T> copy(Hub<T> hubFrom, Hub<T> hubTo) {
				HubCopy<T> hc = new HubCopy<>(hubFrom, hubTo, true);
				return hc;
			}

			@Override
			public <T extends OAObject> HubCopy<T> copy(Hub<T> hubFrom, Hub<T> hubTo, boolean shareActiveObject) {
				HubCopy<T> hc = new HubCopy<>(hubFrom, hubTo, shareActiveObject);
				return hc;
			}
		};
		return opsCopy;
	}

	@Override
	public HubCombineOps combine() {
		if (opsCombine != null) return opsCombine;
		opsCombine = new HubCombineOps() {
			@Override
			public <T extends OAObject> HubCombined<T> combine(Hub<T> hubMaster, Hub<T>... hubs) {
				if (hubMaster == null) return null;
				HubCombined<T> hc = new HubCombined<>(hubMaster, hubs);
				return hc;
			}
		};
		return opsCombine;
	}

	@Override
	public HubAOOps ao() {
		if (opsAO != null) return opsAO;
		opsAO = new HubAOOps() {
			@Override
			public <T extends OAObject> HubListenerAdapter<T> keepActiveObject(final Hub<T> thisHub) {
				return srvc.getHubAOService().keepActiveObject(thisHub);
			}

			@Override
			public <T extends OAObject> void setActiveObject(Hub<T> thisHub, T object, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub, boolean bUpdateSharedHubDetail) {
				srvc.getHubAOService().setActiveObject(thisHub, object, pos, bUpdateLink, bForce, bCalledByShareHub, bUpdateSharedHubDetail);
			}

			@Override
			public <T extends OAObject> void updateDetailHubs(Hub<T> thisHub) {
				srvc.getHubAOService().updateDetailHubs(thisHub);
			}

			@Override
			public <T extends OAObject> void setActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
				srvc.getHubAOService().setActiveObject(thisHub, object, adjustMaster, bUpdateLink, bForce);
			}
		};
		return opsAO;
	}


	@Override
	public HubDataOps data() {
		if (opsData != null) return opsData;
		opsData = new HubDataOps() {
			@Override
			public <T extends OAObject> int getPos(Hub<T> thisHub, Object object, boolean adjustMaster, boolean bUpdateLink) {
				return srvc.getHubDataService().getPos(thisHub, object, adjustMaster, bUpdateLink);
			}
		};
		return opsData;
	}


	@Override
	public HubStatusOps status() {
		if (opsStatus != null) return opsStatus;
		opsStatus = new HubStatusOps() {
			@Override
			public <T extends OAObject> HubCurrentStateEnum getCurrentState(Hub<T> thisHub, Hub<T> hubNew, ArrayList<T> alNew) {
				return srvc.getHubStatusService().getCurrentState(thisHub, hubNew, alNew);
			}
		}; 
		return opsStatus;
	}


	@Override
	public HubRootOps root() {
		if (opsRoot != null) return opsRoot;
		opsRoot = new HubRootOps() {

			@Override
			public <T extends OAObject> Hub<T> getRootHub(Hub<T> thisHub) {
				return srvc.getHubRootService().getRootHub(thisHub);
			}
		}; 
		return opsRoot;
	}
}












