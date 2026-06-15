package com.viaoa.graph.service;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Logger;

import com.viaoa.cascade.OACascade;
import com.viaoa.filter.OAFilter;
import com.viaoa.graph.api.internal.HubsInternalOps;
import com.viaoa.graph.service.hub.*;
import com.viaoa.graph.service.hub.HubStatusService.HubCurrentStateEnum;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.auto.HubAutoSequence;
import com.viaoa.hub.sort.HubSortListener;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.select.OASelect;

public class HubInternalService extends HubParentService implements HubsInternalOps {
	private final Logger LOG = Logger.getLogger(HubInternalService.class.getName());

	// AddRemove =========================
	@Override
	public <T extends OAObject> boolean callHubAddRemoveAdd(Hub<T> hub, T obj) {
	    return getHubAddRemoveService().add(hub, obj);
	}

	@Override
	public <T extends OAObject> boolean callHubAddRemoveRemove(Hub<T> hub, Object obj) {
	    return getHubAddRemoveService().remove(hub, obj);
	}
	
	@Override
	public void callHubAddRemoveSwap(Hub<?> hub, int pos1, int pos2) {
	    getHubAddRemoveService().swap(hub, pos1, pos2);
	}

	@Override
	public void callHubAddRemoveMove(Hub<?> hub, int posFrom, int posTo) {
	    getHubAddRemoveService().move(hub, posFrom, posTo);
	}

	@Override
	public <T extends OAObject> boolean callHubAddRemoveInsert(Hub<T> hub, T obj, int pos) {
	    return getHubAddRemoveService().insert(hub, obj, pos);
	}

	@Override
	public <T extends OAObject> boolean callHubAddRemoveRemove(Hub<T> hub, T obj) {
	    return getHubAddRemoveService().remove(hub, obj);
	}

	@Override
	public <T extends OAObject> T callHubAddRemoveRemove(Hub<T> hub, int pos) {
	    return getHubAddRemoveService().remove(hub, pos);
	}

	@Override
	public void callHubAddRemoveClear(Hub<?> hub) {
	    getHubAddRemoveService().clear(hub);
	}

	@Override
	public <T extends OAObject> boolean callHubAddRemoveCanAdd(Hub<T> hub, T object) {
	    return getHubAddRemoveService().canAdd(hub, object);
	}

	@Override
	public <T extends OAObject> String callHubAddRemoveCanAddMsg(Hub<T> hub, T obj) {
	    return getHubAddRemoveService().canAddMsg(hub, obj);
	}

	@Override
	public String callHubAddRemoveGetCantRemoveAllMessage(Hub<?> hub, int checkType) {
	    return getHubAddRemoveService().getCantRemoveAllMessage(hub, checkType);
	}

	@Override
	public <T extends OAObject> void callHubAddRemoveAdd(Hub<T> hub, T obj, boolean bAlreadyCalledContains) {
	    getHubAddRemoveService().add(hub, obj, bAlreadyCalledContains);
	}

	@Override
	public void callHubAddRemoveClear(Hub<?> thisHub, boolean bSetAOtoNull, boolean bSendNewList) {
	    getHubAddRemoveService().clear(thisHub, bSetAOtoNull, bSendNewList);
	}

	@Override
	public <T extends OAObject> void callHubAddRemoveRemove(Hub<T> thisHub, T obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll) {
	    getHubAddRemoveService().remove(thisHub, obj, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
	}

	@Override
	public <T extends OAObject> void callHubAddRemoveSortMove(Hub<T> hub, T object) {
	    getHubAddRemoveService().sortMove(hub, object);
	}

	@Override
	public <T extends OAObject> void callHubAddRemoveRefresh(Hub<T> hub, Hub<T> hubNew) {
	    getHubAddRemoveService().refresh(hub, hubNew);
	}

	
	// AO =========================
	@Override
	public <T extends OAObject> T callHubAOSetActiveObject(Hub<T> hub, int pos) {
	    return getHubAOService().setActiveObject(hub, pos);
	}

	@Override
	public <T extends OAObject> void callHubAOSetActiveObject(Hub<T> hub, T obj) {
	    getHubAOService().setActiveObject(hub, obj);
	}

	@Override
	public <T extends OAObject> void callHubAOSetActiveObjectForce(Hub<T> hub, T obj) {
	    getHubAOService().setActiveObjectForce(hub, obj);
	}

	@Override
	public <T extends OAObject> T callHubAOSetActiveObject(Hub<T> hub, Object obj) {
	    return getHubAOService().setActiveObject(hub, obj);
	}
	
	
	// AutoMatch =========================
	@Override
	public void callHubAutoMatchSetAutoMatch(Hub<?> hub, String property, Hub<?> hubMaster, boolean bServerSideOnly) {
		getHubAutoMatchService().setAutoMatch(hub, property, hubMaster, bServerSideOnly);
	}

	@Override
	public void callHubAutoMatchSetAutoMatch(Hub<?> hub, String property, Hub<?> hubMaster, boolean bServerSideOnly, OAObject objStop, String stopProperty) {
		getHubAutoMatchService().setAutoMatch(hub, property, hubMaster, bServerSideOnly, objStop, stopProperty);
	}
	
	
	// CS =========================

	@Override
	public void callHubCSSendRefresh(Hub<?> hub) {
	    getHubCSService().sendRefresh(hub);
	}

	@Override
	public boolean callHubCSIsServer(Hub<?> hub) {
	    return getHubCSService().isServer(hub);
	}

	@Override
	public boolean callHubCSIsClient(Hub<?> hub) {
	    return getHubCSService().isClient(hub);
	}
	
	
	// Data =========================
	@Override
	public <T extends OAObject> void callHubDataSetObjectClass(Hub<T> hubDetail, Class<T> clazz) {
		getHubDataService().setObjectClass(hubDetail, clazz);
	}

	@Override
	public void callHubDataEnsureCapacity(Hub<?> hub, int size) {
	    getHubDataService().ensureCapacity(hub, size);
	}

	@Override
	public void callHubDataResizeToFit(Hub<?> hub) {
	    getHubDataService().resizeToFit(hub);
	}

	@Override
	public <T extends OAObject> void callHubDataCopyInto(Hub<T> hub, T[] anArray) {
	    getHubDataService().copyInto(hub, anArray);
	}

	@Override
	public <T extends OAObject> T[] callHubDataToArray(Hub<T> hub) {
	    return getHubDataService().toArray(hub);
	}

	@Override
	public int callHubDataGetCurrentSize(Hub<?> hub) {
	    return getHubDataService().getCurrentSize(hub);
	}

	@Override
	public <T extends OAObject> void callHubDataClone(Hub<T> hub, Hub<T> hubNew) {
	    getHubDataService()._clone(hub, hubNew);
	}

	@Override
	public <T extends OAObject> T callHubDataGetObject(Hub<T> hub, Object key) {
	    return getHubDataService().getObject(hub, key);
	}

	@Override
	public <T extends OAObject> T callHubDataGetObjectAt(Hub<T> hub, int pos) {
	    return getHubDataService().getObjectAt(hub, pos);
	}

	@Override
	public boolean callHubDataContains(Hub<?> hub, Object obj) {
	    return getHubDataService().contains(hub, obj);
	}

	@Override
	public int callHubDataGetPos(Hub<?> hub, Object object, boolean adjustMaster, boolean bUpdateLink) {
	    return getHubDataService().getPos(hub, object, adjustMaster, bUpdateLink);
	}

	@Override
	public boolean callHubDataSetLoadingAllData(Hub<?> hub, boolean bIsLoading) {
	    return getHubDataService().setLoadingAllData(hub, bIsLoading);
	}

	@Override
	public void callHubDataSetLoadingAllData(Hub<?> hub, boolean bIsLoadingAllData, Thread thread) {
	    getHubDataService().setLoadingAllData(hub, bIsLoadingAllData, thread);
	}

	@Override
	public void callHubDataClearHubChanges(Hub<?> hub) {
	    getHubDataService().clearHubChanges(hub);
	}

	// Delete =========================

	@Override
	public void callHubDeleteDeleteAll(Hub<?> hub) {
	    getHubDeleteService().deleteAll(hub);
	}

	@Override
	public boolean callHubDeleteIsDeletingAll(Hub<?> hub) {
	    return getHubDeleteService().isDeletingAll(hub);
	}

	// Detail =========================

	@Override
	public OALinkInfo callHubDetailGetLinkInfoFromMasterObjectToDetail(Hub<?> hub) {
	    return getHubDetailService().getLinkInfoFromMasterObjectToDetail(hub);
	}

	@Override
	public OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<?> hub) {
	    return getHubDetailService().getLinkInfoFromMasterHubToDetail(hub);
	}

	@Override
	public void callHubDetailSetMasterObject(Hub<?> hub, OAObject masterObject) {
	    getHubDetailService().setMasterObject(hub, masterObject);
	}

	@Override
	public void callHubDetailSetMasterObject(Hub<?> hub, OAObject masterObject, OALinkInfo liDetailToMaster) {
	    getHubDetailService().setMasterObject(hub, masterObject, liDetailToMaster);
	}

	@Override
	public HubDataMaster callHubDetailGetDataMaster(Hub<?> hub) {
	    return getHubDetailService().getDataMaster(hub);
	}

	@Override
	public boolean callHubDetailIsOwned(Hub<?> hub) {
	    return getHubDetailService().isOwned(hub);
	}

	@Override
	public Hub<?> callHubDetailGetDetailHub(Hub<?> hub, String path) {
	    return getHubDetailService().getDetailHub(hub, path);
	}

	@Override
	public Hub<?> callHubDetailGetDetailHub(Hub<?> hub, String path, boolean bShareActive, String selectOrder) {
	    return getHubDetailService().getDetailHub(hub, path, bShareActive, selectOrder);
	}

	@Override
	public Hub<?> callHubDetailGetDetailHub(Hub<?> hub, String path, boolean bShareActive) {
	    return getHubDetailService().getDetailHub(hub, path, bShareActive);
	}

	@Override
	public Hub<?> callHubDetailGetDetailHub(Hub<?> hub, String path, String selectOrder) {
	    return getHubDetailService().getDetailHub(hub, path, selectOrder);
	}

	@Override
	public <T extends OAObject> Hub<T> callHubDetailGetDetailHub(Hub<?> hub, String path, Class<T> objectClass, boolean bShareActive) {
	    return getHubDetailService().getDetailHub(hub, path, objectClass, bShareActive);
	}

	@Override
	public <T extends OAObject> Hub<T> callHubDetailGetDetailHub(Hub<?> hub, Class<T> clazz, boolean bShareActive, String selectOrder) {
	    return getHubDetailService().getDetailHub(hub, clazz, bShareActive, selectOrder);
	}

	@Override
	public Hub<?> callHubDetailGetDetailHub(Hub<?> hub, Class<? extends OAObject>[] classes) {
	    return getHubDetailService().getDetailHub(hub, classes);
	}

	@Override
	public void callHubDetailSetMasterHub(Hub<?> thisHub, Hub<?> masterHub, String path, boolean bShared, String selectOrder) {
	    getHubDetailService().setMasterHub(thisHub, masterHub, path, bShared, selectOrder);
	}

	@Override
	public Hub<?> callHubDetailGetMasterHub(Hub<?> hub) {
	    return getHubDetailService().getMasterHub(hub);
	}

	@Override
	public OAObject callHubDetailGetMasterObject(Hub<?> hub) {
	    return getHubDetailService().getMasterObject(hub);
	}

	@Override
	public Class<? extends OAObject> callHubDetailGetMasterClass(Hub<?> hub) {
	    return getHubDetailService().getMasterClass(hub);
	}

	@Override
	public boolean callHubDetailRemoveDetailHub(Hub<?> hub, Hub<?> hubDetail) {
	    return getHubDetailService().removeDetailHub(hub, hubDetail);
	}

	@Override
	public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
	    return getHubDetailService().getLinkInfoFromDetailToMaster(hub);
	}

	@Override
	public <T extends OAObject> Hub<T> callHubDetailGetRealHub(Hub<T> hub) {
	    return getHubDetailService().getRealHub(hub);
	}

	@Override
	public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> hub) {
	    return getHubDetailService().getPropertyFromMasterToDetail(hub);
	}

	@Override
	public String callHubDetailGetPropertyFromDetailToMaster(Hub<?> hub) {
	    return getHubDetailService().getPropertyFromDetailToMaster(hub);
	}

	@Override
	public OALinkInfo callHubDetailGetLinkInfoFromMasterToDetail(Hub<?> hub) {
	    return getHubDetailService().getLinkInfoFromMasterToDetail(hub);
	}

	// Event =========================

	@Override
	public void callHubEventFireOnNewListEvent(Hub<?> hub, boolean bAll) {
	    getHubEventService().fireOnNewListEvent(hub, bAll);
	}

	@Override
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, String property) {
	    getHubEventService().addHubListener(hub, hl, property);
	}

	@Override
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, String property, boolean bActiveObjectOnly) {
	    getHubEventService().addHubListener(hub, hl, property, bActiveObjectOnly);
	}

	@Override
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, boolean bActiveObjectOnly) {
	    getHubEventService().addHubListener(hub, hl, bActiveObjectOnly);
	}

	@Override
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths) {
	    getHubEventService().addHubListener(hub, hl, property, dependentPropertyPaths);
	}

	@Override
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly) {
	    getHubEventService().addHubListener(hub, hl, property, dependentPropertyPaths, bActiveObjectOnly);
	}

	@Override
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly, boolean bUseBackgroundThread) {
	    getHubEventService().addHubListener(hub, hl, property, dependentPropertyPaths, bActiveObjectOnly, bUseBackgroundThread);
	}

	@Override
	public <T extends OAObject> void callHubEventAddHubListener(Hub<T> hub, HubListener<T> hl) {
	    getHubEventService().addHubListener(hub, hl);
	}

	@Override
	public <T extends OAObject> void callHubEventRemoveHubListener(Hub<T> hub, HubListener<T> hl) {
	    getHubEventService().removeHubListener(hub, hl);
	}

	@Override
	public <T extends OAObject> void callHubEventFireCalcPropertyChange(Hub<T> hub, T obj, String propertyName) {
	    getHubEventService().fireCalcPropertyChange(hub, obj, propertyName);
	}

	// Find =========================

	@Override
	public <T extends OAObject> T callHubFindFindFirst(Hub<T> hub, String propertyPath, Object findValue, boolean bSetAO, T lastFoundObject) {
	    return getHubFindService().findFirst(hub, propertyPath, findValue, bSetAO, lastFoundObject);
	}

	
	// Link =========================

	@Override
	public <T extends OAObject> Hub<T> callHubLinkGetHubWithLink(Hub<T> hub, boolean bIncludeCopiedHubs) {
	    return getHubLinkService().getHubWithLink(hub, bIncludeCopiedHubs);
	}

	@Override
	public void callHubLinkSetLinkHub(Hub<?> thisHub, String propertyFrom, Hub<?> linkToHub, String propertyTo, boolean linkPosFlag, boolean bAutoCreate, boolean bAutoCreateAllowDups) {
	    getHubLinkService().setLinkHub(thisHub, propertyFrom, linkToHub, propertyTo, linkPosFlag, bAutoCreate, bAutoCreateAllowDups);
	}

	@Override
	public String callHubLinkGetLinkHubPath(Hub<?> hub, boolean bIncludeCopiedHubs) {
	    return getHubLinkService().getLinkHubPath(hub, bIncludeCopiedHubs);
	}

	@Override
	public <T extends OAObject> void callHubLinkUpdateLinkedToHub(Hub<T> hub, Hub<?> linkToHub, T obj) {
	    getHubLinkService().updateLinkedToHub(hub, linkToHub, obj);
	}

	@Override
	public <T extends OAObject> void callHubLinkUpdateLinkedToHub(Hub<T> hub, Hub<?> linkToHub, T obj, String changedPropName) {
	    getHubLinkService().updateLinkedToHub(hub, linkToHub, obj, changedPropName);
	}

	@Override
	public <T extends OAObject, U extends OAObject> Object callHubLinkGetPropertyValueInLinkedToHub(Hub<T> hub, U linkObject) { 
		// ex: hub=hubDepartment linked to hubEmp,  linkObject=employee
	    return getHubLinkService().getPropertyValueInLinkedToHub(hub, linkObject);
	}

	@Override
	public boolean callHubLinkGetLinkedOnPos(Hub<?> hub) {
	    return getHubLinkService().getLinkedOnPos(hub);
	}

	@Override
	public String callHubLinkGetLinkToProperty(Hub<?> hub) {
	    return getHubLinkService().getLinkToProperty(hub);
	}

	// Property =========================
	@Override
	public void callHubPropertySetProperty(Hub<?> hub, String name, Object obj) {
		getHubPropertyService().setProperty(hub, name, obj);
	}

	@Override
	public Object callHubPropertyGetProperty(Hub<?> hub, String name) {
	    return getHubPropertyService().getProperty(hub, name);
	}

	@Override
	public void callHubPropertyRemoveProperty(Hub<?> hub, String name) {
		getHubPropertyService().removeProperty(hub, name);
	}

	@Override
	public void callHubPropertySetUniqueProperty(Hub<?> hub, String propertyName) {
		getHubPropertyService().setUniqueProperty(hub, propertyName);
	}
	
	
	
	// Root =========================
	@Override
	public <T extends OAObject> Hub<T> callHubRootGetRootHub(Hub<T> hub) {
	    return getHubRootService().getRootHub(hub);
	}

	@Override
	public void callHubRootSetRootHub(Hub<?> hub, boolean bIsRoot) {
	    getHubRootService().setRootHub(hub, bIsRoot);
	}

	// Save =========================

	@Override
	public void callHubSaveSaveAll(Hub<?> hub, int cascadeRule) {
	    getHubSaveService().saveAll(hub, cascadeRule);
	}

	// Select =========================

	@Override
	public <T extends OAObject> OASelect<T> callHubSelectGetSelect(Hub<T> hub, boolean bCreateIfNull) {
	    return getHubSelectService().getSelect(hub, bCreateIfNull);
	}

	@Override
	public void callHubSelectLoadAllData(Hub<?> hub) {
	    getHubSelectService().loadAllData(hub);
	}

	@Override
	public void callHubSelectCancelSelect(Hub<?> hub, boolean bRemoveSelect) {
	    getHubSelectService().cancelSelect(hub, bRemoveSelect);
	}

	@Override
	public boolean callHubSelectIsMoreData(Hub<?> hub) {
	    return getHubSelectService().isMoreData(hub);
	}

	@Override
	public void callHubSelectSetSelectWhere(Hub<?> hub, String whereClause) {
	    getHubSelectService().setSelectWhere(hub, whereClause);
	}

	@Override
	public String callHubSelectGetSelectWhere(Hub<?> hub) {
	    return getHubSelectService().getSelectWhere(hub);
	}

	@Override
	public void callHubSelectSetSelectOrder(Hub<?> hub, String orderClause) {
	    getHubSelectService().setSelectOrder(hub, orderClause);
	}

	@Override
	public <T extends OAObject> void callHubSelectSetSelectWhereHub(Hub<T> hub, Hub<T> hubSelect) {
	    getHubSelectService().setSelectWhereHub(hub, hubSelect);
	}

	@Override
	public void callHubSelectSetSelectWhereHubPropertyPath(Hub<?> hub, String ppFromHub) {
	    getHubSelectService().setSelectWhereHubPropertyPath(hub, ppFromHub);
	}

	@Override
	public String callHubSelectGetSelectOrder(Hub<?> hub) {
	    return getHubSelectService().getSelectOrder(hub);
	}

	@Override
	public void callHubSelectSelect(Hub<?> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderByClause, boolean bAppendFlag) {
	    getHubSelectService().select(hub, whereObject, whereClause, whereParams, orderByClause, bAppendFlag);
	}

	@Override
	public void callHubSelectSelect(Hub<?> hub, boolean bAppendFlag) {
	    getHubSelectService().select(hub, bAppendFlag);
	}

	@Override
	public <T extends OAObject> void callHubSelectSelect(Hub<T> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderBy, boolean bAppendFlag, OAFilter<T> filter) {
		getHubSelectService().select(hub, whereObject, whereClause, whereParams, orderBy, bAppendFlag, filter);
	}

	@Override
	public <T extends OAObject> void callHubSelectSelect(Hub<T> hub, OASelect<T> select) {
	    getHubSelectService().select(hub, select);
	}

	@Override
	public void callHubSelectSelectPassthru(Hub<?> hub, String whereClause, String orderClause) {
	    getHubSelectService().selectPassthru(hub, whereClause, orderClause);
	}

	@Override
	public <T extends OAObject> OASelect<T> callHubSelectGetSelect(Hub<T> hub) {
	    return getHubSelectService().getSelect(hub);
	}

	@Override
	public void callHubSelectRefresh(Hub<?> hub) {
	    getHubSelectService().refresh(hub);
	}

	@Override
	public <T extends OAObject> Hub<T> callHubSelectGetSelectWhereHub(Hub<T> hub) {
	    return getHubSelectService().getSelectWhereHub(hub);
	}

	@Override
	public String callHubSelectGetSelectWhereHubPropertyPath(Hub<?> hub) {
	    return getHubSelectService().getSelectWhereHubPropertyPath(hub);
	}

	
	
	// Sequence =========================
	@Override
	public HubAutoSequence callHubSequenceGetAutoSequence(Hub<?> hub) {
	    return getHubSequenceService().getAutoSequence(hub);
	}
	@Override
	public void callHubSequenceSetAutoSequence(Hub<?> hub, String property, int startNumber, boolean bKeepSeq) {
		getHubSequenceService().setAutoSequence(hub, property, startNumber, bKeepSeq);
	}
	@Override
	public void callHubSequenceResequence(Hub<?> hub) {
		getHubSequenceService().resequence(hub);
	}
	
	
	// Serialize =========================
	@Override
	public void callHubSerializeWriteObject(Hub<?> hub, ObjectOutputStream stream) throws IOException {
	    getHubSerializeService()._writeObject(hub, stream);
	}

	@Override
	public Object callHubSerializeReadResolve(Hub<?> hub) throws ObjectStreamException {
	    return getHubSerializeService()._readResolve(hub);
	}

	// Share =========================
	@Override
	public <T extends OAObject> void callHubShareSetSharedHub(Hub<T> hub, Hub<T> sharedMasterHub, boolean shareActiveObject) {
	    getHubShareService().setSharedHub(hub, sharedMasterHub, shareActiveObject);
	}

	@Override
	public <T extends OAObject> void callHubShareRemoveSharedHub(Hub<T> hub, Hub<T> hubToRemove) {
	    getHubShareService().removeSharedHub(hub, hubToRemove);
	}

	@Override
	public <T extends OAObject> Hub<T> callHubShareCreateSharedHub(Hub<T> hub, boolean shareActiveObject) {		
	    return getHubShareService().createSharedHub(hub, shareActiveObject);
	}

	@Override
	public boolean callHubShareIsUsingSameSharedHub(Hub<?> hub, Hub<?> hub2) {
	    return getHubShareService().isUsingSameSharedHub(hub, hub2);
	}

	@Override
	public boolean callHubShareIsUsingSameSharedAO(Hub<?> hub, Hub<?> hub2) {
	    return getHubShareService().isUsingSameSharedAO(hub, hub2);
	}

	@Override
	public <T extends OAObject> Hub<T> callHubShareGetMainSharedHub(Hub<T> hub) {
	    return getHubShareService().getMainSharedHub(hub);
	}

	
	// Size =========================
	@Override
	public int callHubSizeGetSize(Hub<?> hub) {
	    return getHubSizeService().getSize(hub);
	}

	@Override
	public int callHubSizeGetLoadedSize(Hub<?> hub) {
	    return getHubSizeService().getLoadedSize(hub);
	}
	
	
	
	// Sort =========================
	@Override
	public HubSortListener callHubSortGetSortListener(Hub<?> hub) {
	    return getHubSortService().getSortListener(hub);
	}

	@Override
	public void callHubSortSort(Hub<?> hub, String propertyPaths, boolean bAscending, Comparator<?> comp) {
	    getHubSortService().sort(hub, propertyPaths, bAscending, comp);
	}

	@Override
	public boolean callHubSortIsSorted(Hub<?> hub) {
	    return getHubSortService().isSorted(hub);
	}

	@Override
	public void callHubSortCancelSort(Hub<?> hub) {
	    getHubSortService().cancelSort(hub);
	}

	@Override
	public void callHubSortSort(Hub<?> hub) {
	    getHubSortService().sort(hub);
	}

	@Override
	public void callHubSortResort(Hub<?> hub) {
	    getHubSortService().resort(hub);
	}

	
	// Status =========================
	@Override
	public boolean callHubStatusIsValid(Hub<?> hub) {
	    return getHubStatusService().isValid(hub);
	}
	
	@Override
	public boolean callHubStatusGetChanged(Hub<?> thisHub, int iCascadeRule, OACascade cascade) {
	    return getHubStatusService().getChanged(thisHub, iCascadeRule, cascade);
	}
	
	@Override
	public void callHubStatusSetChanged(Hub<?> hub, boolean bIsChanged) {
	    getHubStatusService().setChanged(hub, bIsChanged);
	}

	@Override
	public <T extends OAObject> HubCurrentStateEnum callHubStatusGetCurrentState(Hub<T> thisHub, Hub<T> hubNew, ArrayList<T> alNew) {
	    return getHubStatusService().getCurrentState(thisHub, hubNew, alNew);
	}
}
