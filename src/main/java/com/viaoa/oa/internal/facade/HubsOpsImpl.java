package com.viaoa.oa.internal.facade;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Comparator;

import com.viaoa.callback.OAObjectCallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.Hub.HubCurrentStateEnum;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.hub.auto.HubAutoMatch;
import com.viaoa.hub.auto.HubAutoSequence;
import com.viaoa.hub.copy.HubCopy;
import com.viaoa.hub.filter.HubFilter;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.hub.sort.HubSortListener;
import com.viaoa.hub.view.HubCombined;
import com.viaoa.hub.view.HubFlattened;
import com.viaoa.hub.view.HubGroupBy;
import com.viaoa.hub.view.HubLeftJoin;
import com.viaoa.hub.view.OAGroupBy;
import com.viaoa.hub.view.OALeftJoin;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.oa.api.internal.HubsOps;
import com.viaoa.oa.api.internal.hubs.HubAOOps;
import com.viaoa.oa.api.internal.hubs.HubAddRemoveOps;
import com.viaoa.oa.api.internal.hubs.HubAutoMatchOps;
import com.viaoa.oa.api.internal.hubs.HubCSOps;
import com.viaoa.oa.api.internal.hubs.HubCombineOps;
import com.viaoa.oa.api.internal.hubs.HubCopyOps;
import com.viaoa.oa.api.internal.hubs.HubDataOps;
import com.viaoa.oa.api.internal.hubs.HubDeleteOps;
import com.viaoa.oa.api.internal.hubs.HubDetailOps;
import com.viaoa.oa.api.internal.hubs.HubEventOps;
import com.viaoa.oa.api.internal.hubs.HubFilterOps;
import com.viaoa.oa.api.internal.hubs.HubFindOps;
import com.viaoa.oa.api.internal.hubs.HubLinkOps;
import com.viaoa.oa.api.internal.hubs.HubMergeOps;
import com.viaoa.oa.api.internal.hubs.HubPropertyOps;
import com.viaoa.oa.api.internal.hubs.HubRootOps;
import com.viaoa.oa.api.internal.hubs.HubSaveOps;
import com.viaoa.oa.api.internal.hubs.HubSelectOps;
import com.viaoa.oa.api.internal.hubs.HubSequenceOps;
import com.viaoa.oa.api.internal.hubs.HubSerializeOps;
import com.viaoa.oa.api.internal.hubs.HubShareOps;
import com.viaoa.oa.api.internal.hubs.HubSizeOps;
import com.viaoa.oa.api.internal.hubs.HubSortOps;
import com.viaoa.oa.api.internal.hubs.HubStatusOps;
import com.viaoa.oa.api.internal.hubs.HubViewOps;
import com.viaoa.oa.service.hub.HubParentService;
import com.viaoa.object.OAObject;
import com.viaoa.select.OASelect;

public class HubsOpsImpl implements HubsOps {

	private final HubParentService srvc;

	private HubAddRemoveOps opsAddRemove;
	private HubAOOps opsAO;
	private HubAutoMatchOps opsAutomatch;
	private HubCombineOps opsCombine;
	private HubCopyOps opsCopy;
	private HubCSOps opsCS;
	private HubDataOps opsData;
	private HubDeleteOps opsDelete;
	private HubDetailOps opsDetail;
	private HubEventOps opsEvent;
	private HubFilterOps opsFilter;
	private HubFindOps opsFind;
	private HubLinkOps opsLink;
	private HubMergeOps opsMerge;
	private HubPropertyOps opsProperty;
	private HubRootOps opsRoot;
	private HubSaveOps opsSave;
	private HubSelectOps opsSelect;
	private HubSequenceOps opsSequence;
	private HubSerializeOps opsSerialize;
	private HubShareOps opsShare;
	private HubSizeOps opsSize;
	private HubSortOps opsSort;
	private HubStatusOps opsStatus;
	private HubViewOps opsView;

	public HubsOpsImpl(HubParentService srvc) {
		this.srvc = srvc;
	}

	@Override
	public HubAutoMatchOps autoMatch() {
		if (opsAutomatch != null) return opsAutomatch;
		opsAutomatch = new HubAutoMatchOps() {
			@Override
			public <T extends OAObject, T2 extends OAObject> HubAutoMatch<T, T2> match(Hub<T> hub, String property, Hub<T2> hubMaster) {
				return srvc.getHubAutoMatchService().setAutoMatch(hub, property, hubMaster, false);
			}

			@Override
			public <T extends OAObject, T2 extends OAObject> HubAutoMatch<T,T2> setAutoMatch(Hub<T> hub, String property, Hub<T2> hubMaster, boolean bServerSideOnly) {
				return srvc.getHubAutoMatchService().setAutoMatch(hub, property, hubMaster, bServerSideOnly);
			}

			@Override
			public <T extends OAObject, T2 extends OAObject> HubAutoMatch<T,T2> setAutoMatch(Hub<T> hub, String property, Hub<T2> hubMaster, OAObject objStop, String stopProperty) {
				return srvc.getHubAutoMatchService().setAutoMatch(hub, property, hubMaster, false, objStop, stopProperty);
			}

			@Override
			public <T extends OAObject, T2 extends OAObject> HubAutoMatch<T, T2> setAutoMatch(Hub<T> hub, String property, Hub<T2> hubMaster, boolean bServerSideOnly, OAObject objStop, String stopProperty) {
				return srvc.getHubAutoMatchService().setAutoMatch(hub, property, hubMaster, bServerSideOnly, objStop, stopProperty);
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

			@Override
			public OALinkInfo getLinkInfoFromMasterObjectToDetail(Hub<?> hub) {
				return srvc.getHubDetailService().getLinkInfoFromMasterObjectToDetail(hub);
			}

			@Override
			public OALinkInfo getLinkInfoFromMasterToDetail(Hub<?> hub) {
				return srvc.getHubDetailService().getLinkInfoFromMasterToDetail(hub);
			}

			@Override
			public void setMasterObject(Hub<?> hub, OAObject masterObject) {
				srvc.getHubDetailService().setMasterObject(hub, masterObject);
			}

			@Override
			public void setMasterObject(Hub<?> hub, OAObject masterObject, OALinkInfo liDetailToMaster) {
				srvc.getHubDetailService().setMasterObject(hub, masterObject, liDetailToMaster);
			}

			@Override
			public HubDataMaster getDataMaster(Hub<?> hub) {
				return srvc.getHubDetailService().getDataMaster(hub);
			}

			@Override
			public boolean isOwned(Hub<?> hub) {
				return srvc.getHubDetailService().isOwned(hub);
			}

			@Override
			public Hub<?> getDetailHub(Hub<?> hub, String path) {
				return srvc.getHubDetailService().getDetailHub(hub, path);
			}

			@Override
			public Hub<?> getDetailHub(Hub<?> hub, String path, boolean bShareActive, String selectOrder) {
				return srvc.getHubDetailService().getDetailHub(hub, path, bShareActive, selectOrder);
			}

			@Override
			public Hub<?> getDetailHub(Hub<?> hub, String path, boolean bShareActive) {
				return srvc.getHubDetailService().getDetailHub(hub, path, bShareActive);
			}

			@Override
			public Hub<?> getDetailHub(Hub<?> hub, String path, String selectOrder) {
				return srvc.getHubDetailService().getDetailHub(hub, path, selectOrder);
			}

			@Override
			public <T extends OAObject> Hub<T> getDetailHub(Hub<?> hub, String path, Class<T> objectClass, boolean bShareActive) {
				return srvc.getHubDetailService().getDetailHub(hub, path, objectClass, bShareActive);
			}

			@Override
			public <T extends OAObject> Hub<T> getDetailHub(Hub<?> hub, Class<T> clazz, boolean bShareActive, String selectOrder) {
				return srvc.getHubDetailService().getDetailHub(hub, clazz, bShareActive, selectOrder);
			}

			@Override
			public Hub<?> getDetailHub(Hub<?> hub, Class<? extends OAObject>[] classes) {
				return srvc.getHubDetailService().getDetailHub(hub, classes);
			}

			@Override
			public void setMasterHub(Hub<?> thisHub, Hub<?> masterHub, String path, boolean bShared, String selectOrder) {
				srvc.getHubDetailService().setMasterHub(thisHub, masterHub, path, bShared, selectOrder);
			}

			@Override
			public Hub<? extends OAObject> getMasterHub(Hub<?> hub) {
				return srvc.getHubDetailService().getMasterHub(hub);
			}

			@Override
			public OAObject getMasterObject(Hub<?> hub) {
				return srvc.getHubDetailService().getMasterObject(hub);
			}

			@Override
			public Class<? extends OAObject> getMasterClass(Hub<?> hub) {
				return srvc.getHubDetailService().getMasterClass(hub);
			}

			@Override
			public boolean removeDetailHub(Hub<?> hub, Hub<?> hubDetail) {
				return srvc.getHubDetailService().removeDetailHub(hub, hubDetail);
			}

			@Override
			public OALinkInfo getLinkInfoFromDetailToMaster(Hub<?> hub) {
				return srvc.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}

			@Override
			public <T extends OAObject> Hub<T> getRealHub(Hub<T> hub) {
				return srvc.getHubDetailService().getRealHub(hub);
			}

			@Override
			public String getPropertyFromMasterToDetail(Hub<?> hub) {
				return srvc.getHubDetailService().getPropertyFromMasterToDetail(hub);
			}

			@Override
			public String getPropertyFromDetailToMaster(Hub<?> hub) {
				return srvc.getHubDetailService().getPropertyFromDetailToMaster(hub);
			}

			@Override
			public boolean getIsFromSameMasterHub(Hub<?> hub1, Hub<?> hub2) {
				return srvc.getHubDetailService().getIsFromSameMasterHub(hub1, hub2);
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

			@Override
			public void setLinkHub(Hub<?> thisHub, String propertyFrom, Hub<?> linkToHub, String propertyTo, boolean linkPosFlag, boolean bAutoCreate, boolean bAutoCreateAllowDups) {
				srvc.getHubLinkService().setLinkHub(thisHub, propertyFrom, linkToHub, propertyTo, linkPosFlag, bAutoCreate, bAutoCreateAllowDups);
			}
			@Override
			public String getLinkHubPath(Hub<?> hub, boolean bIncludeCopiedHubs) {
				return srvc.getHubLinkService().getLinkHubPath(hub, bIncludeCopiedHubs);			
			}

			@Override
			public <T extends OAObject> void updateLinkedToHub(Hub<T> hub, Hub<?> linkToHub, T obj) {
				srvc.getHubLinkService().updateLinkedToHub(hub, linkToHub, obj);
			}

			@Override
			public <T extends OAObject> void updateLinkedToHub(Hub<T> hub, Hub<?> linkToHub, T obj, String changedPropName) {
				srvc.getHubLinkService().updateLinkedToHub(hub, linkToHub, obj, changedPropName);
			}

			@Override
			public <T extends OAObject, U extends OAObject> Object getPropertyValueInLinkedToHub(Hub<T> hub, U linkObject) {
				return srvc.getHubLinkService().getPropertyValueInLinkedToHub(hub, linkObject);
			}

			@Override
			public boolean getLinkedOnPos(Hub<?> hub) {
				return srvc.getHubLinkService().getLinkedOnPos(hub);
			}

			@Override
			public <T extends OAObject> boolean getLinkedOnPos(final Hub<T> thisHub, boolean bIncludeCopiedHubs) {
				return srvc.getHubLinkService().getLinkedOnPos(thisHub, bIncludeCopiedHubs);
			}
			
			@Override
			public String getLinkToProperty(Hub<?> hub) {
				return srvc.getHubLinkService().getLinkToProperty(hub);
			}

			@Override
			public String getLinkFromProperty(Hub<?> thisHub) {
				return srvc.getHubLinkService().getLinkFromProperty(thisHub);
			}

			@Override
			public String getLinkFromProperty(Hub<?> thisHub, boolean bIncludeCopiedHubs) {
				return srvc.getHubLinkService().getLinkFromProperty(thisHub, bIncludeCopiedHubs);
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

			@Override
			public <T extends OAObject> void setSharedHub(Hub<T> hub, Hub<T> sharedMasterHub, boolean shareActiveObject) {
				srvc.getHubShareService().setSharedHub(hub, sharedMasterHub, shareActiveObject);
			}

			@Override
			public <T extends OAObject> void removeSharedHub(Hub<T> hub, Hub<T> hubToRemove) {
				srvc.getHubShareService().removeSharedHub(hub, hubToRemove);
			}

			@Override
			public <T extends OAObject> Hub<T> createSharedHub(Hub<T> hub, boolean shareActiveObject) {
				return srvc.getHubShareService().createSharedHub(hub, shareActiveObject);
			}

			@Override
			public boolean isUsingSameSharedHub(Hub<?> hub, Hub<?> hub2) {
				return srvc.getHubShareService().isUsingSameSharedHub(hub, hub2);
			}

			@Override
			public boolean isUsingSameSharedAO(Hub<?> hub, Hub<?> hub2) {
				return srvc.getHubShareService().isUsingSameSharedAO(hub, hub2);
			}

			@Override
			public <T extends OAObject> Hub<T> getMainSharedHub(Hub<T> hub) {
				return srvc.getHubShareService().getMainSharedHub(hub);
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
				HubGroupBy<F, G> hgb = new HubGroupBy<F, G>(hubFrom, hubGrpBy, propertyPath, createNullList);
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
				HubLeftJoin<A, B> hlj = new HubLeftJoin<A, B>(hubLeft, hub, propertyPath, shareActiveObject);
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

			@Override
			public <T extends OAObject> T setActiveObject(Hub<T> hub, int pos) {
				return srvc.getHubAOService().setActiveObject(hub, pos);
			}

			@Override
			public <T extends OAObject> void setActiveObject(Hub<T> hub, T obj) {
				srvc.getHubAOService().setActiveObject(hub, obj);
			}

			@Override
			public <T extends OAObject> void setActiveObjectForce(Hub<T> hub, T obj) {
				srvc.getHubAOService().setActiveObjectForce(hub, obj);
			}

			@Override
			public <T extends OAObject> T setActiveObject(Hub<T> hub, Object obj) {
				return srvc.getHubAOService().setActiveObject(hub, obj);
			}
		};
		return opsAO;
	}

	@Override
	public HubDataOps data() {
		if (opsData != null) return opsData;
		opsData = new HubDataOps() {
			
			@Override
			public int getPos(final Hub<?> hub, Object object, final boolean adjustMaster, final boolean bUpdateLink) {
				return srvc.getHubDataService().getPos(hub, object, adjustMaster, bUpdateLink);
			}

			@Override
			public <T extends OAObject> void setObjectClass(Hub<T> hubDetail, Class<T> clazz) {
				srvc.getHubDataService().setObjectClass(hubDetail, clazz);
			}

			@Override
			public void ensureCapacity(Hub<?> hub, int size) {
				srvc.getHubDataService().ensureCapacity(hub, size);
			}

			@Override
			public void resizeToFit(Hub<?> hub) {
				srvc.getHubDataService().resizeToFit(hub);
			}

			@Override
			public <T extends OAObject> void copyInto(Hub<T> hub, T[] anArray) {
				srvc.getHubDataService().copyInto(hub, anArray);
			}

			@Override
			public <T extends OAObject> T[] toArray(Hub<T> hub) {
				return srvc.getHubDataService().toArray(hub);
			}

			@Override
			public int getCurrentSize(Hub<?> hub) {
				return srvc.getHubDataService().getCurrentSize(hub);
			}

			@Override
			public <T extends OAObject> T getObject(Hub<T> hub, Object key) {
				return srvc.getHubDataService().getObject(hub, key);
			}

			@Override
			public <T extends OAObject> T getObjectAt(Hub<T> hub, int pos) {
				return srvc.getHubDataService().getObjectAt(hub, pos);
			}

			@Override
			public boolean contains(Hub<?> hub, Object obj) {
				return srvc.getHubDataService().contains(hub, obj);
			}

			@Override
			public boolean setLoadingAllData(Hub<?> hub, boolean bIsLoading) {
				return srvc.getHubDataService().setLoadingAllData(hub, bIsLoading);
			}

			@Override
			public void setLoadingAllData(Hub<?> hub, boolean bIsLoadingAllData, Thread thread) {
				srvc.getHubDataService().setLoadingAllData(hub, bIsLoadingAllData, thread);
			}

			@Override
			public void clearHubChanges(Hub<?> hub) {
				srvc.getHubDataService().clearHubChanges(hub);
			}

			@Override
			public <T extends OAObject> void _clone(Hub<T> thisHub, Hub<T> newHub) {
				srvc.getHubDataService()._clone(thisHub, newHub);
				
			}
		};
		return opsData;
	}

	@Override
	public HubStatusOps status() {
		if (opsStatus != null)
			return opsStatus;
		opsStatus = new HubStatusOps() {
			@Override
			public <T extends OAObject> HubCurrentStateEnum getCurrentState(Hub<T> thisHub, Hub<T> hubNew, ArrayList<T> alNew) {
				return srvc.getHubStatusService().getCurrentState(thisHub, hubNew, alNew);
			}

			@Override
			public boolean isValid(Hub<?> hub) {
				return srvc.getHubStatusService().isValid(hub);
			}

			@Override
			public boolean getChanged(Hub<?> thisHub, int iCascadeRule, OACascade cascade) {
				return srvc.getHubStatusService().getChanged(thisHub, iCascadeRule, cascade);
			}

			@Override
			public void setChanged(Hub<?> hub, boolean bIsChanged) {
				srvc.getHubStatusService().setChanged(hub, bIsChanged);
			}
		};
		return opsStatus;
	}

	@Override
	public HubRootOps root() {
		if (opsRoot != null)return opsRoot;
		opsRoot = new HubRootOps() {
			@Override
			public <T extends OAObject> Hub<T> getRootHub(Hub<T> thisHub) {
				return srvc.getHubRootService().getRootHub(thisHub);
			}

			@Override
			public void setRootHub(Hub<?> hub, boolean bIsRoot) {
				srvc.getHubRootService().setRootHub(hub, bIsRoot);
			}
		};
		return opsRoot;
	}

	@Override
	public HubAddRemoveOps addRemove() {
		if (opsAddRemove != null) return opsAddRemove;
		opsAddRemove = new HubAddRemoveOps() {
			@Override
			public <T extends OAObject> boolean add(Hub<T> hub, T obj) {
				return srvc.getHubAddRemoveService().add(hub, obj);
			}

			@Override
			public void swap(Hub<?> hub, int pos1, int pos2) {
				srvc.getHubAddRemoveService().swap(hub, pos1, pos2);
			}

			@Override
			public void move(Hub<?> hub, int posFrom, int posTo) {
				srvc.getHubAddRemoveService().move(hub, posFrom, posTo);
			}

			@Override
			public <T extends OAObject> boolean insert(Hub<T> hub, T obj, int pos) {
				return srvc.getHubAddRemoveService().insert(hub, obj, pos);
			}

			@Override
			public void clear(Hub<?> hub) {
				srvc.getHubAddRemoveService().clear(hub);
			}

			@Override
			public <T extends OAObject> boolean canAdd(Hub<T> hub, T object) {
				return srvc.getHubAddRemoveService().canAdd(hub, object);
			}

			@Override
			public <T extends OAObject> String canAddMsg(Hub<T> hub, T obj) {
				return srvc.getHubAddRemoveService().canAddMsg(hub, obj);
			}

			@Override
			public String getCantRemoveAllMessage(Hub<?> hub, OAObjectCallback.CheckType[] onlyCheckTypes) {
				return srvc.getHubAddRemoveService().getCantRemoveAllMessage(hub, onlyCheckTypes);
			}

			@Override
			public <T extends OAObject> void add(Hub<T> hub, T obj, boolean bAlreadyCalledContains) {
				srvc.getHubAddRemoveService().add(hub, obj, bAlreadyCalledContains);
			}

			@Override
			public void clear(Hub<?> thisHub, boolean bSetAOtoNull, boolean bSendNewList) {
				srvc.getHubAddRemoveService().clear(thisHub, bSetAOtoNull, bSendNewList);
			}

			@Override
			public <T extends OAObject> boolean remove(Hub<T> hub, T obj) {
				return srvc.getHubAddRemoveService().remove(hub, obj);
			}

			@Override
			public <T extends OAObject> T remove(Hub<T> hub, int pos) {
				return srvc.getHubAddRemoveService().remove(hub, pos);
			}

			@Override
			public <T extends OAObject> boolean remove(Hub<T> hub, Object obj) {
				return srvc.getHubAddRemoveService().remove(hub, obj);
			}

			@Override
			public <T extends OAObject> void remove(Hub<T> thisHub, T obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll) {
				srvc.getHubAddRemoveService().remove(thisHub, obj, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
			}

			@Override
			public <T extends OAObject> void sortMove(Hub<T> hub, T object) {
				srvc.getHubAddRemoveService().sortMove(hub, object);
			}

			@Override
			public <T extends OAObject> void refresh(Hub<T> hub, Hub<T> hubNew) {
				srvc.getHubAddRemoveService().refresh(hub, hubNew);
			}

			@Override
			public boolean isAllowAddRemove(Hub<?> thisHub) {
				return srvc.getHubAddRemoveService().isAllowAddRemove(thisHub);
			}

			@Override
			public boolean isAllowRemove(Hub<?> thisHub) {
				return srvc.getHubAddRemoveService().isAllowRemove(thisHub);
			}
		};
		return opsAddRemove;
	}

	@Override
	public HubCSOps cs() {
		if (opsCS != null) return opsCS;

		opsCS = new HubCSOps() {
			@Override
			public void sendRefresh(Hub<?> hub) {
				srvc.getHubCSService().sendRefresh(hub);
			}

			@Override
			public boolean isServer(Hub<?> hub) {
				return srvc.getHubCSService().isServer(hub);
			}

			@Override
			public boolean isClient(Hub<?> hub) {
				return srvc.getHubCSService().isClient(hub);
			}
		};
		return opsCS;
	}

	@Override
	public HubDeleteOps delete() {
		if (opsDelete != null) return opsDelete;
		opsDelete = new HubDeleteOps() {

			@Override
			public boolean isDeletingAll(Hub<?> hub) {
				return srvc.getHubDeleteService().isDeletingAll(hub);
			}

			@Override
			public void deleteAll(Hub<?> hub) {
				srvc.getHubDeleteService().deleteAll(hub);
			}
		};
		return opsDelete;
	}

	@Override
	public HubEventOps events() {
		if (opsEvent != null) return opsEvent;

		opsEvent = new HubEventOps() {
			@Override
			public <T extends OAObject> void removeHubListener(Hub<T> hub, HubListener<T> hl) {
				srvc.getHubEventService().removeHubListener(hub, hl);
			}

			@Override
			public void fireOnNewListEvent(Hub<?> hub, boolean bAll) {
				srvc.getHubEventService().fireOnNewListEvent(hub, bAll);
			}

			@Override
			public <T extends OAObject> void fireCalcPropertyChange(Hub<T> hub, T obj, String propertyName) {
				srvc.getHubEventService().fireCalcPropertyChange(hub, obj, propertyName);
			}

			@Override
			public <T extends OAObject> void fireAfterChangeActiveObjectEvent(Hub<T> thisHub, T obj, int pos, boolean bAllShared) {
				srvc.getHubEventService().fireAfterChangeActiveObjectEvent(thisHub, obj, pos, bAllShared);
			}

			@Override
			public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl) {
				srvc.getHubEventService().addHubListener(hub, hl);
			}

			@Override
			public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly, boolean bUseBackgroundThread) {
				srvc.getHubEventService().addHubListener(hub, hl, property, dependentPropertyPaths, bActiveObjectOnly, bUseBackgroundThread);
			}

			@Override
			public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly) {
				srvc.getHubEventService().addHubListener(hub, hl, property, dependentPropertyPaths, bActiveObjectOnly);
			}

			@Override
			public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths) {
				srvc.getHubEventService().addHubListener(hub, hl, property, dependentPropertyPaths);
			}

			@Override
			public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, boolean bActiveObjectOnly) {
				srvc.getHubEventService().addHubListener(hub, hl, bActiveObjectOnly);
			}

			@Override
			public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, boolean bActiveObjectOnly) {
				srvc.getHubEventService().addHubListener(hub, hl, property, bActiveObjectOnly);
			}

			@Override
			public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property) {
				srvc.getHubEventService().addHubListener(hub, hl, property);
			}
		};
		return opsEvent;
	}

	@Override
	public HubFindOps find() {
		if (opsFind != null) return opsFind;
		opsFind = new HubFindOps() {
			@Override
			public <T extends OAObject> T findFirst(Hub<T> hub, String propertyPath, Object findValue, boolean bSetAO, T lastFoundObject) {
				return srvc.getHubFindService().findFirst(hub, propertyPath, findValue, bSetAO, lastFoundObject);
			}
		};
		return opsFind;
	}

	@Override
	public HubPropertyOps property() {
		if (opsProperty != null) return opsProperty;
		opsProperty = new HubPropertyOps() {

			@Override
			public void setUniqueProperty(Hub<?> hub, String propertyName) {
				srvc.getHubPropertyService().setUniqueProperty(hub, propertyName);
			}

			@Override
			public void setProperty(Hub<?> hub, String name, Object obj) {
				srvc.getHubPropertyService().setProperty(hub, name, obj);
			}

			@Override
			public void removeProperty(Hub<?> hub, String name) {
				srvc.getHubPropertyService().removeProperty(hub, name);
			}

			@Override
			public Object getProperty(Hub<?> hub, String name) {
				return srvc.getHubPropertyService().getProperty(hub, name);
			}
		};
		return opsProperty;
	}

	@Override
	public HubSaveOps save() {
		if (opsSave != null) return opsSave;
		opsSave = new HubSaveOps() {
			@Override
			public void saveAll(Hub<?> hub, int cascadeRule) {
				srvc.getHubSaveService().saveAll(hub, cascadeRule);
			}

			@Override
			public void saveAll(Hub<?> thisHub, int iCascadeRule, OACascade cascade) {
				srvc.getHubSaveService().saveAll(thisHub, iCascadeRule, cascade);
			}
		};
		return opsSave;
	}

//qqqqqqqqqqqqqqqqqqqq	
	
	@Override
	public HubSelectOps select() {
		if (opsSelect != null) return opsSelect;
		opsSelect = new HubSelectOps() {

			@Override
			public void setSelectWhereHubPropertyPath(Hub<?> hub, String ppFromHub) {
				srvc.getHubSelectService().setSelectWhereHubPropertyPath(hub, ppFromHub);
			}

			@Override
			public <T extends OAObject> void setSelectWhereHub(Hub<T> hub, Hub<T> hubSelect) {
				srvc.getHubSelectService().setSelectWhereHub(hub, hubSelect);;
			}

			@Override
			public void setSelectWhere(Hub<?> hub, String whereClause) {
				srvc.getHubSelectService().setSelectWhere(hub, whereClause);
			}

			@Override
			public void setSelectOrder(Hub<?> hub, String orderClause) {
				srvc.getHubSelectService().setSelectOrder(hub, orderClause);
			}

			@Override
			public void selectPassthru(Hub<?> hub, String whereClause, String orderClause) {
				srvc.getHubSelectService().selectPassthru(hub, whereClause, orderClause);
			}

			@Override
			public <T extends OAObject> void select(Hub<T> hub, OASelect<T> select) {
				srvc.getHubSelectService().select(hub, select);
			}

			@Override
			public <T extends OAObject> void select(Hub<T> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderBy, boolean bAppendFlag, OAFilter<T> filter) {
				srvc.getHubSelectService().select(hub, whereObject, whereClause, whereParams, orderBy, bAppendFlag, filter);
			}

			@Override
			public void select(Hub<?> hub, boolean bAppendFlag) {
				srvc.getHubSelectService().select(hub, bAppendFlag);
			}

			@Override
			public void select(Hub<?> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderByClause, boolean bAppendFlag) {
				srvc.getHubSelectService().select(hub, whereObject, whereClause, whereParams, orderByClause, bAppendFlag);
			}

			@Override
			public void refresh(Hub<?> hub) {
				srvc.getHubSelectService().refresh(hub);
			}

			@Override
			public void loadAllData(Hub<?> hub) {
				srvc.getHubSelectService().loadAllData(hub);
			}

			@Override
			public boolean isMoreData(Hub<?> hub) {
				return srvc.getHubSelectService().isMoreData(hub);
			}

			@Override
			public String getSelectWhereHubPropertyPath(Hub<?> hub) {
				return srvc.getHubSelectService().getSelectWhereHubPropertyPath(hub);
			}

			@Override
			public <T extends OAObject> Hub<T> getSelectWhereHub(Hub<T> hub) {
				return srvc.getHubSelectService().getSelectWhereHub(hub);
			}

			@Override
			public String getSelectWhere(Hub<?> hub) {
				return srvc.getHubSelectService().getSelectWhere(hub);
			}

			@Override
			public String getSelectOrder(Hub<?> hub) {
				return srvc.getHubSelectService().getSelectOrder(hub);
			}

			@Override
			public <T extends OAObject> OASelect<T> getSelect(Hub<T> hub) {
				return srvc.getHubSelectService().getSelect(hub);
			}

			@Override
			public <T extends OAObject> OASelect<T> getSelect(Hub<T> hub, boolean bCreateIfNull) {
				return srvc.getHubSelectService().getSelect(hub, bCreateIfNull);
			}

			@Override
			public void cancelSelect(Hub<?> hub, boolean bRemoveSelect) {
				srvc.getHubSelectService().cancelSelect(hub, bRemoveSelect);
			}

			@Override
			public boolean adoptWhereHub(Hub<?> thisHub, String propName, Hub<?> hubFrom) {
				return srvc.getHubSelectService().adoptWhereHub(thisHub, propName, hubFrom);
			}
		};
		return opsSelect;
	}

	@Override
	public HubSequenceOps sequence() {
		if (opsSequence != null) return opsSequence;
		opsSequence = new HubSequenceOps() {
			@Override
			public void setAutoSequence(Hub<?> hub, String property, int startNumber, boolean bKeepSeq) {
				srvc.getHubSequenceService().setAutoSequence(hub, property, startNumber, bKeepSeq);
			}
			
			@Override
			public void resequence(Hub<?> hub) {
				srvc.getHubSequenceService().resequence(hub);
			}
			
			@Override
			public HubAutoSequence getAutoSequence(Hub<?> hub) {
				return srvc.getHubSequenceService().getAutoSequence(hub);
			}
		}; 
		return opsSequence;
	}

	@Override
	public HubSerializeOps serialize() {
		if (opsSerialize != null) return opsSerialize;
		opsSerialize = new HubSerializeOps() {
			@Override
			public void _writeObject(Hub<?> hub, ObjectOutputStream stream) throws IOException {
				srvc.getHubSerializeService()._writeObject(hub, stream);
			}
			@Override
			public Object _readResolve(Hub<?> hub) throws ObjectStreamException {
				return srvc.getHubSerializeService()._readResolve(hub);
			}
		};
		return opsSerialize;
	}

	@Override
	public HubSizeOps size() {
		if (opsSize != null) return opsSize;
		opsSize = new HubSizeOps() {

			@Override
			public int getSize(Hub<?> hub) {
				return srvc.getHubSizeService().getSize(hub);
			}

			@Override
			public int getLoadedSize(Hub<?> hub) {
				return srvc.getHubSizeService().getLoadedSize(hub);
			}
		};
		return opsSize;
	}

	@Override
	public HubSortOps sort() {
		if (opsSort != null) return opsSort;
		opsSort = new HubSortOps() {

			@Override
			public void sort(Hub<?> hub) {
				srvc.getHubSortService().sort(hub);
			}

			@Override
			public void sort(Hub<?> hub, String propertyPaths, boolean bAscending, Comparator<?> comp) {
				srvc.getHubSortService().sort(hub, propertyPaths, bAscending, comp);
			}

			@Override
			public void resort(Hub<?> hub) {
				srvc.getHubSortService().resort(hub);
			}

			@Override
			public boolean isSorted(Hub<?> hub) {
				return srvc.getHubSortService().isSorted(hub);
			}

			@Override
			public HubSortListener getSortListener(Hub<?> hub) {
				return srvc.getHubSortService().getSortListener(hub);
			}

			@Override
			public void cancelSort(Hub<?> hub) {
				srvc.getHubSortService().cancelSort(hub);
			}
		};
		return opsSort;
	}
}
