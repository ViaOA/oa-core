package com.viaoa.graph.service;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.graph.api.HubsOps;
import com.viaoa.graph.api.internal.HubsInternalOps;
import com.viaoa.graph.service.hub.*;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAutoSequence;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubSortListener;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAFilter;
import com.viaoa.xml.OAXMLWriter;

public class HubService extends HubParentService implements HubsOps, HubsInternalOps {
	private final Logger LOG = Logger.getLogger(HubService.class.getName());

	private boolean bInitialized;
	public static final Boolean TRUE = Boolean.valueOf(true);
	public static final Boolean FALSE = Boolean.valueOf(false);
	
	// Hub
	@Override
	public HubAutoSequence callHubGetAutoSequence(Hub<?> hub) {
	    return getAutoSequence(hub);
	}

	@Override
	public int callHubGetSize(Hub<?> hub) {
	    return getSize(hub);
	}

	@Override
	public int callHubGetLoadedSize(Hub<?> hub) {
	    return getLoadedSize(hub);
	}

	@Override
	public void callHubSetAutoSequence(Hub<?> hub, String property, int startNumber, boolean bKeepSeq) {
	    setAutoSequence(hub, property, startNumber, bKeepSeq);
	}

	@Override
	public void callHubResequence(Hub<?> hub) {
	    resequence(hub);
	}

	@Override
	public <T> HubCurrentStateEnum callHubGetCurrentState(Hub<T> thisHub, Hub<T> hubNew, ArrayList<T> alNew) {
	    return getCurrentState(thisHub, hubNew, alNew);
	}

	@Override
	public void callHubSetObjectClass(Hub<?> hubDetail, Class<?> clazz) {
	    setObjectClass(hubDetail, clazz);
	}

	@Override
	public void callHubSetAutoMatch(Hub<?> hub, String property, Hub<?> hubMaster, boolean bServerSideOnly) {
	    setAutoMatch(hub, property, hubMaster, bServerSideOnly);
	}

	@Override
	public void callHubSetAutoMatch(Hub<?> hub, String property, Hub<?> hubMaster, boolean bServerSideOnly, OAObject objStop, String stopProperty) {
	    setAutoMatch(hub, property, hubMaster, bServerSideOnly, objStop, stopProperty);
	}

	@Override
	public boolean callHubIsValid(Hub<?> hub) {
	    return isValid(hub);
	}

	@Override
	public boolean callHubGetChanged(Hub<?> thisHub, int iCascadeRule, OACascade cascade) {
	    return getChanged(thisHub, iCascadeRule, cascade);
	}

	@Override
	public void callHubSetProperty(Hub<?> hub, String name, Object obj) {
	    setProperty(hub, name, obj);
	}

	@Override
	public Object callHubGetProperty(Hub<?> hub, String name) {
	    return getProperty(hub, name);
	}

	@Override
	public void callHubRemoveProperty(Hub<?> hub, String name) {
	    removeProperty(hub, name);
	}

	@Override
	public void callHubSetUniqueProperty(Hub<?> hub, String propertyName) {
	    setUniqueProperty(hub, propertyName);
	}

	// AddRemove

	@Override
	public <T> boolean callHubAddRemoveAdd(Hub<T> hub, T obj) {
	    return getHubAddRemoveService().add(hub, obj);
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
	public <T> boolean callHubAddRemoveInsert(Hub<T> hub, T obj, int pos) {
	    return getHubAddRemoveService().insert(hub, obj, pos);
	}

	@Override
	public boolean callHubAddRemoveRemove(Hub<?> hub, Object obj) {
	    return getHubAddRemoveService().remove(hub, obj);
	}

	@Override
	public <T> T callHubAddRemoveRemove(Hub<T> hub, int pos) {
	    return getHubAddRemoveService().remove(hub, pos);
	}

	@Override
	public void callHubAddRemoveClear(Hub<?> hub) {
	    getHubAddRemoveService().clear(hub);
	}

	@Override
	public <T> boolean callHubAddRemoveCanAdd(Hub<T> hub, T object) {
	    return getHubAddRemoveService().canAdd(hub, object);
	}

	@Override
	public <T> String callHubAddRemoveCanAddMsg(Hub<T> hub, T obj) {
	    return getHubAddRemoveService().canAddMsg(hub, obj);
	}

	@Override
	public String callHubAddRemoveGetCantRemoveAllMessage(Hub<?> hub, int checkType) {
	    return getHubAddRemoveService().getCantRemoveAllMessage(hub, checkType);
	}

	@Override
	public <T> void callHubAddRemoveAdd(Hub<T> hub, T obj, boolean bAlreadyCalledContains) {
	    getHubAddRemoveService().add(hub, obj, bAlreadyCalledContains);
	}

	@Override
	public void callHubAddRemoveClear(Hub<?> thisHub, boolean bSetAOtoNull, boolean bSendNewList) {
	    getHubAddRemoveService().clear(thisHub, bSetAOtoNull, bSendNewList);
	}

	@Override
	public void callHubAddRemoveRemove(Hub<?> thisHub, Object obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll) {
	    getHubAddRemoveService().remove(thisHub, obj, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
	}

	@Override
	public <T> void callHubAddRemoveSortMove(Hub<T> hub, T object) {
	    getHubAddRemoveService().sortMove(hub, object);
	}

	@Override
	public <T> void callHubAddRemoveRefresh(Hub<T> hub, Hub<T> hubNew) {
	    getHubAddRemoveService().refresh(hub, hubNew);
	}

	// AO

	@Override
	public <T> T callHubAOSetActiveObject(Hub<T> hub, int pos) {
	    return getHubAOService().setActiveObject(hub, pos);
	}

	@Override
	public void callHubAOSetActiveObject(Hub<?> hub, Object obj) {
	    getHubAOService().setActiveObject(hub, obj);
	}

	@Override
	public void callHubAOSetActiveObjectForce(Hub<?> hub, Object obj) {
	    getHubAOService().setActiveObjectForce(hub, obj);
	}

	// CS

	@Override
	public void callHubCSSendRefresh(Hub<?> hub) {
	    getHubCSService().sendRefresh(hub);
	}

	@Override
	public boolean callHubCSIsServer(Hub<?> hub) {
	    return getHubCSService().isServer(hub);
	}

	// Data

	@Override
	public void callHubDataEnsureCapacity(Hub<?> hub, int size) {
	    getHubDataService().ensureCapacity(hub, size);
	}

	@Override
	public void callHubDataResizeToFit(Hub<?> hub) {
	    getHubDataService().resizeToFit(hub);
	}

	@Override
	public void callHubDataSetChanged(Hub<?> hub, boolean bIsChanged) {
	    getHubDataService().setChanged(hub, bIsChanged);
	}

	@Override
	public <T> void callHubDataCopyInto(Hub<T> hub, T[] anArray) {
	    getHubDataService().copyInto(hub, anArray);
	}

	@Override
	public <T> T[] callHubDataToArray(Hub<T> hub) {
	    return getHubDataService().toArray(hub);
	}

	@Override
	public int callHubDataGetCurrentSize(Hub<?> hub) {
	    return getHubDataService().getCurrentSize(hub);
	}

	@Override
	public void callHubDataClone(Hub<?> hub, Hub<?> hubNew) {
	    getHubDataService()._clone(hub, hubNew);
	}

	@Override
	public <T> T callHubDataGetObject(Hub<T> hub, Object key) {
	    return getHubDataService().getObject(hub, key);
	}

	@Override
	public <T> T callHubDataGetObjectAt(Hub<T> hub, int pos) {
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

	// Delete

	@Override
	public void callHubDeleteDeleteAll(Hub<?> hub) {
	    getHubDeleteService().deleteAll(hub);
	}

	@Override
	public boolean callHubDeleteIsDeletingAll(Hub<?> hub) {
	    return getHubDeleteService().isDeletingAll(hub);
	}

	// Detail

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
	public Hub<? extends OAObject> callHubDetailGetDetailHub(Hub<?> hub, String path) {
	    return getHubDetailService().getDetailHub(hub, path);
	}

	@Override
	public Hub<? extends OAObject> callHubDetailGetDetailHub(Hub<?> hub, String path, boolean bShareActive, String selectOrder) {
	    return getHubDetailService().getDetailHub(hub, path, bShareActive, selectOrder);
	}

	@Override
	public Hub<? extends OAObject> callHubDetailGetDetailHub(Hub<?> hub, String path, boolean bShareActive) {
	    return getHubDetailService().getDetailHub(hub, path, bShareActive);
	}

	@Override
	public Hub<? extends OAObject> callHubDetailGetDetailHub(Hub<?> hub, String path, String selectOrder) {
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
	public Hub<? extends OAObject> callHubDetailGetDetailHub(Hub<?> hub, Class<? extends OAObject>[] classes) {
	    return getHubDetailService().getDetailHub(hub, classes);
	}

	@Override
	public void callHubDetailSetMasterHub(Hub<?> thisHub, Hub<? extends OAObject> masterHub, String path, boolean bShared, String selectOrder) {
	    getHubDetailService().setMasterHub(thisHub, masterHub, path, bShared, selectOrder);
	}

	@Override
	public Hub<? extends OAObject> callHubDetailGetMasterHub(Hub<?> hub) {
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
	public boolean callHubDetailRemoveDetailHub(Hub<?> hub, Hub<? extends OAObject> hubDetail) {
	    return getHubDetailService().removeDetailHub(hub, hubDetail);
	}

	@Override
	public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
	    return getHubDetailService().getLinkInfoFromDetailToMaster(hub);
	}

	@Override
	public Hub<?> callHubDetailGetRealHub(Hub<?> hub) {
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

	// Event

	@Override
	public void callHubEventFireOnNewListEvent(Hub<?> hub, boolean bAll) {
	    getHubEventService().fireOnNewListEvent(hub, bAll);
	}

	@Override
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, String property) {
	    getHubEventService().addHubListener(hub, hl, property);
	}

	@Override
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, String property, boolean bActiveObjectOnly) {
	    getHubEventService().addHubListener(hub, hl, property, bActiveObjectOnly);
	}

	@Override
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, boolean bActiveObjectOnly) {
	    getHubEventService().addHubListener(hub, hl, bActiveObjectOnly);
	}

	@Override
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, String property, String[] dependentPropertyPaths) {
	    getHubEventService().addHubListener(hub, hl, property, dependentPropertyPaths);
	}

	@Override
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly) {
	    getHubEventService().addHubListener(hub, hl, property, dependentPropertyPaths, bActiveObjectOnly);
	}

	@Override
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly, boolean bUseBackgroundThread) {
	    getHubEventService().addHubListener(hub, hl, property, dependentPropertyPaths, bActiveObjectOnly, bUseBackgroundThread);
	}

	@Override
	public void callHubEventFireCalcPropertyChange(Hub<?> hub, OAObject obj, String property) {
	    getHubEventService().fireCalcPropertyChange(hub, obj, property);
	}

	@Override
	public void callHubEventAddHubListener(Hub<?> hub, HubListener<?> hl) {
	    getHubEventService().addHubListener(hub, hl);
	}

	@Override
	public void callHubEventRemoveHubListener(Hub<?> hub, HubListener<?> hl) {
	    getHubEventService().removeHubListener(hub, hl);
	}

	@Override
	public void callHubEventFireCalcPropertyChange(Hub<?> hub, Object obj, String propertyName) {
	    getHubEventService().fireCalcPropertyChange(hub, obj, propertyName);
	}

	// Find

	@Override
	public <T> T callHubFindFindFirst(Hub<T> hub, String propertyPath, Object findValue, boolean bSetAO, T lastFoundObject) {
	    return getHubFindService().findFirst(hub, propertyPath, findValue, bSetAO, lastFoundObject);
	}

	// Link

	@Override
	public Hub<?> callHubLinkGetHubWithLink(Hub<?> hub, boolean bIncludeCopiedHubs) {
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
	public void callHubLinkUpdateLinkedToHub(Hub<?> hub, Hub<?> linkToHub, Object obj) {
	    getHubLinkService().updateLinkedToHub(hub, linkToHub, obj);
	}

	@Override
	public void callHubLinkUpdateLinkedToHub(Hub<?> hub, Hub<?> linkToHub, Object obj, String changedPropName) {
	    getHubLinkService().updateLinkedToHub(hub, linkToHub, obj, changedPropName);
	}

	@Override
	public Object callHubLinkGetPropertyValueInLinkedToHub(Hub<?> hub, Object linkObject) {
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

	// Root

	@Override
	public Hub<?> callHubRootGetRootHub(Hub<?> hub) {
	    return getHubRootService().getRootHub(hub);
	}

	@Override
	public void callHubRootSetRootHub(Hub<?> hub, boolean bIsRoot) {
	    getHubRootService().setRootHub(hub, bIsRoot);
	}

	// Save

	@Override
	public void callHubSaveSaveAll(Hub<?> hub, int cascadeRule) {
	    getHubSaveService().saveAll(hub, cascadeRule);
	}

	// Select

	@Override
	public OASelect<? extends OAObject> callHubSelectGetSelect(Hub<?> hub, boolean bCreateIfNull) {
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
	public void callHubSelectSetSelectWhereHub(Hub<?> hub, Hub<?> hubSelect) {
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
	public void callHubSelectSelect(Hub<?> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderBy, boolean bAppendFlag, OAFilter filter) {
	    getHubSelectService().select(hub, whereObject, whereClause, whereParams, orderBy, bAppendFlag, filter);
	}

	@Override
	public void callHubSelectSelect(Hub<?> hub, OASelect<? extends OAObject> select) {
	    getHubSelectService().select(hub, select);
	}

	@Override
	public void callHubSelectSelectPassthru(Hub<?> hub, String whereClause, String orderClause) {
	    getHubSelectService().selectPassthru(hub, whereClause, orderClause);
	}

	@Override
	public OASelect<? extends OAObject> callHubSelectGetSelect(Hub<?> hub) {
	    return getHubSelectService().getSelect(hub);
	}

	@Override
	public void callHubSelectRefresh(Hub<?> hub) {
	    getHubSelectService().refresh(hub);
	}

	@Override
	public Hub<?> callHubSelectGetSelectWhereHub(Hub<?> hub) {
	    return getHubSelectService().getSelectWhereHub(hub);
	}

	@Override
	public String callHubSelectGetSelectWhereHubPropertyPath(Hub<?> hub) {
	    return getHubSelectService().getSelectWhereHubPropertyPath(hub);
	}

	// Serialize

	@Override
	public void callHubSerializeWriteObject(Hub<?> hub, ObjectOutputStream stream) throws IOException {
	    getHubSerializeService()._writeObject(hub, stream);
	}

	@Override
	public Object callHubSerializeReadResolve(Hub<?> hub) throws ObjectStreamException {
	    return getHubSerializeService()._readResolve(hub);
	}

	// Share
	@Override
	public void callHubShareSetSharedHub(Hub<?> hub, Hub<?> sharedMasterHub, boolean shareActiveObject) {
	    getHubShareService().setSharedHub(hub, sharedMasterHub, shareActiveObject);
	}

	@Override
	public void callHubShareRemoveSharedHub(Hub<?> hub, Hub<?> hubToRemove) {
	    getHubShareService().removeSharedHub(hub, hubToRemove);
	}

	@Override
	public <T> Hub<T> callHubShareCreateSharedHub(Hub<T> hub, boolean shareActiveObject) {		
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
	public Hub<?> callHubShareGetMainSharedHub(Hub<?> hub) {
	    return getHubShareService().getMainSharedHub(hub);
	}

	// Sort
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

	// XML
	@Override
	public void callHubXMLWrite(Hub<?> hub, OAXMLWriter ow, String tagName, boolean bKeyOnly, OACascade cascade) {
	    getHubXMLService().write(hub, ow, tagName, bKeyOnly, cascade);
	}
}
