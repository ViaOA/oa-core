package com.viaoa.graph.service.hub;


import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.api.internal.HubsInternalOps.HubCurrentStateEnum;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.graph.service.OASyncService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAutoMatch;
import com.viaoa.hub.HubAutoSequence;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubData;
import com.viaoa.hub.HubDataActive;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.hub.HubDataUnique;
import com.viaoa.hub.HubDetail;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubFilter;
import com.viaoa.hub.HubInternalBridge;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.hub.HubMerger;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectSerializer;
import com.viaoa.object.OASiblingHelper;
import com.viaoa.remote.OARemoteThread;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.thread.OARemoteThreadService;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OANullObject;
import com.viaoa.xml.OAXMLWriter;

/**
 * 
 * qqqqqqqqqqqq Parent that manages all subservices
 * 
 */
public abstract class HubParentService {
	private final Logger LOG = Logger.getLogger(HubParentService.class.getName());

	private final HubInternalBridge faBridge = new HubInternalBridge();
	private Hub.FriendAccess faHub;
	
	private OAObjectService srvcObject;
	private OASyncService srvcSync;
	private OAThreadLocalService srvcThreadLocal;
	private OARemoteThreadService srvcRemoteThread;
	
	private HubAddRemoveService srvcHubAddRemove;
	private HubAOService srvcHubAO;
	private HubCSService srvcHubCS;
	private HubDataService srvcHubData;
	private HubDeleteService srvcHubDelete;
	private HubDetailService srvcHubDetail;
	private HubDSService srvcHubDS;
	private HubEventService srvcHubEvent;
	private HubFindService srvcHubFind;
	private HubLinkService srvcHubLink;
	private HubRootService srvcHubRoot;
	private HubSaveService srvcHubSave;
	private HubSelectService srvcHubSelect;
	private HubSerializeService srvcHubSerialize;
	private HubShareService srvcHubShare;
	private HubSortService srvcHubSort;
	private HubXMLService srvcHubXML;
	
	private boolean bInitialized;
	public static final Boolean TRUE = Boolean.valueOf(true);
	public static final Boolean FALSE = Boolean.valueOf(false);
	
	public void initialize(OAObjectService srvcObject, OASyncService srvcSync, OAThreadLocalService srvcThreadLocal, OARemoteThreadService srvcRemoteThread) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	if (srvcSync == null) throw new IllegalArgumentException("OASyncService can not be null");
    	if (srvcThreadLocal == null) throw new IllegalArgumentException("OAThreadLocalService can not be null");
    	if (srvcRemoteThread == null) throw new IllegalArgumentException("OARemoteThreadService can not be null");
		
		this.faHub = faBridge.getHubFriendAccess();
    	
    	this.srvcObject = srvcObject; 
		this.srvcSync = srvcSync;
		this.srvcThreadLocal = srvcThreadLocal;
		this.srvcRemoteThread = srvcRemoteThread;
	}
	
	
	public HubAddRemoveService getHubAddRemoveService() {
		if (srvcHubAddRemove != null) return srvcHubAddRemove;
		
		srvcHubAddRemove = new HubAddRemoveService(faBridge.getHubFriendAccess()) {
			@Override
			public void callThreadLocalLock(Object object) {
				HubParentService.this.srvcThreadLocal.lock(object);;
			}
			@Override
			public void callThreadLocalUnlock(Object object) {
				HubParentService.this.srvcThreadLocal.unlock(object);;
			}
			@Override
			public boolean callThreadLocalIsLoading() {
				return HubParentService.this.srvcThreadLocal.isLoading();
			}
			@Override
			public boolean callThreadLocalIsDeleting(Object obj) {
				return HubParentService.this.srvcThreadLocal.isDeleting(obj);
			}
			@Override
			public void callRemoteThreadStartNextThread() {
				HubParentService.this.srvcRemoteThread.startNextThread();
			}
			@Override
			public boolean callRemoteThreadIsRemoteThread() {
				return HubParentService.this.srvcRemoteThread.isRemoteThread();
			}
			@Override
			public void callObjectReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				HubParentService.this.srvcObject.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);				
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public OALinkInfo callObjectInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getRecursiveLinkInfo(thisOI, type);
			}
			@Override
			public void callObjectHubRemoveHub(OAObject oaObj, Hub hub, boolean bIsOnHubFinalize) {
				HubParentService.this.srvcObject.getOAObjectHubService().removeHub(oaObj, hub, bIsOnHubFinalize);				
			}
			@Override
			public boolean callObjectHubAddHub(OAObject oaObj, Hub hub) {
				return HubParentService.this.srvcObject.getOAObjectHubService().addHub(oaObj, hub);
			}
			@Override
			public OAObjectCallback callObjectCallbackGetVerifyRemoveAllObjectCallback(Hub hub, int checkType) {
				return HubParentService.this.srvcObject.getOAObjectCallbackService().getVerifyRemoveAllObjectCallback(hub, checkType);
			}
			@Override
			public OAObjectCallback callObjectCallbackGetVerifyAddObjectCallback(Hub hub, OAObject oaObj, int checkType) {
				return HubParentService.this.srvcObject.getOAObjectCallbackService().getVerifyAddObjectCallback(hub, oaObj, checkType);
			}
			@Override
			public OAObjectCallback callObjectCallbackGetAllowRemoveObjectCallback(Hub hub, OAObject objRemove, int checkType) {
				return HubParentService.this.srvcObject.getOAObjectCallbackService().getAllowRemoveObjectCallback(hub, objRemove, checkType);
			}
			@Override
			public OAObjectCallback callObjectCallbackGetVerifyRemoveObjectCallback(Hub hub, OAObject objRemove, int checkType) {
				return HubParentService.this.srvcObject.getOAObjectCallbackService().getVerifyRemoveObjectCallback(hub, objRemove, checkType);
			}
			@Override
			public OAObjectCallback callObjectCallbackGetAllowRemoveAllObjectCallback(Hub hub, int checkType) {
				return HubParentService.this.srvcObject.getOAObjectCallbackService().getAllowRemoveAllObjectCallback(hub, checkType);
			}
			@Override
			public OAObjectCallback callObjectCallbackGetAllowAddObjectCallback(Hub hub, OAObject objAdd, int checkType) {
				return HubParentService.this.srvcObject.getOAObjectCallbackService().getAllowAddObjectCallback(hub, objAdd, checkType);
			}
			@Override
			public boolean callHubVerifyUniqueProperty(Hub thisHub, Object object) {
				return HubParentService.this.verifyUniqueProperty(thisHub, object);
			}
			@Override
			public void callHubShareSetSharedHubsAfterRemoveAll(Hub thisHub) {
				HubParentService.this.getHubShareService().setSharedHubsAfterRemoveAll(thisHub);
			}
			@Override
			public void callHubShareSetSharedHubsAfterRemove(Hub thisHub, Object objRemoved, int posRemoved) {
				HubParentService.this.getHubShareService().setSharedHubsAfterRemove(thisHub, objRemoved, posRemoved);				
			}
			@Override
			public void callHubSetReferenceable(Hub hub, boolean bReferenceable) {
				HubParentService.this.setReferenceable(hub, bReferenceable);				
			}
			@Override
			public void callHubSetObjectClass(Hub thisHub, Class objClass) {
				HubParentService.this.setObjectClass(thisHub, objClass);				
			}
			@Override
			public String callHubSelectGetSelectWhereHubPropertyPath(Hub thisHub) {
				return HubParentService.this.getHubSelectService().getSelectWhereHubPropertyPath(thisHub);
			}
			@Override
			public Hub callHubSelectGetSelectWhereHub(Hub thisHub) {
				return HubParentService.this.getHubSelectService().getSelectWhereHub(thisHub);
			}
			@Override
			public void callHubSelectCancelSelect(Hub thisHub, boolean bRemoveSelect) {
				HubParentService.this.getHubSelectService().cancelSelect(thisHub, bRemoveSelect);				
			}
			@Override
			public Object callHubGetRealObject(Hub hub, Object object) {
				return HubParentService.this.getRealObject(hub, object);
			}
			@Override
			public void callHubEventFireOnNewListEvent(Hub thisHub, boolean bAll) {
				HubParentService.this.getHubEventService().fireOnNewListEvent(thisHub, bAll);				
			}
			@Override
			public void callHubEventFireBeforeRemoveEvent(Hub thisHub, Object obj, int pos) {
				HubParentService.this.getHubEventService().fireBeforeRemoveEvent(thisHub, obj, pos);				
			}
			@Override
			public void callHubEventFireBeforeRemoveAllEvent(Hub thisHub) {
				HubParentService.this.getHubEventService().fireBeforeRemoveAllEvent(thisHub);				
			}
			@Override
			public void callHubEventFireBeforeMoveEvent(Hub thisHub, int fromPos, int toPos) {
				HubParentService.this.getHubEventService().fireBeforeMoveEvent(thisHub, fromPos, toPos);				
			}
			@Override
			public void callHubEventFireBeforeInsertEvent(Hub thisHub, Object obj, int pos) {
				HubParentService.this.getHubEventService().fireBeforeInsertEvent(thisHub, obj, pos);				
			}
			@Override
			public void callHubEventFireBeforeAddEvent(Hub thisHub, Object obj, int pos) {
				HubParentService.this.getHubEventService().fireBeforeAddEvent(thisHub, obj, pos);				
			}
			@Override
			public <T> void callHubEventFireAfterRemoveEvent(Hub<T> thisHub, T obj, int pos) {
				HubParentService.this.getHubEventService().fireAfterRemoveEvent(thisHub, obj, pos);				
			}
			@Override
			public void callHubEventFireAfterRemoveAllEvent(Hub thisHub) {
				HubParentService.this.getHubEventService().fireAfterRemoveAllEvent(thisHub);				
			}
			@Override
			public void callHubEventFireAfterMoveEvent(Hub thisHub, int fromPos, int toPos) {
				HubParentService.this.getHubEventService().fireAfterMoveEvent(thisHub, fromPos, toPos);				
			}
			@Override
			public <T> void callHubEventFireAfterInsertEvent(Hub<T> thisHub, T obj, int pos) {
				HubParentService.this.getHubEventService().fireAfterInsertEvent(thisHub, obj, pos);				
			}
			@Override
			public <T> void callHubEventFireAfterAddEvent(Hub<T> thisHub, T obj, int pos) {
				HubParentService.this.getHubEventService().fireAfterAddEvent(thisHub, obj, pos);				
			}
			@Override
			public void callHubDetailSetPropertyToMasterHub(Hub thisHub, Object detailObject, Object objMaster) {
				HubParentService.this.getHubDetailService().setPropertyToMasterHub(thisHub, detailObject, objMaster);				
			}
			@Override
			public boolean callHubDetailIsRecursiveMasterDetail(Hub thisHub) {
				return HubParentService.this.getHubDetailService().isRecursiveMasterDetail(thisHub);
			}
			@Override
			public OAObject callHubDetailGetMasterObject(Hub thisHub) {
				return HubParentService.this.getHubDetailService().getMasterObject(thisHub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub hub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public HubDataMaster callHubDetailGetDataMaster(Hub thisHub, boolean bIncludedFilteredHub) {
				return HubParentService.this.getHubDetailService().getDataMaster(thisHub, bIncludedFilteredHub);
			}
			@Override
			public int callHubData_remove(Hub thisHub, Object obj, boolean bDeleting, boolean bIsRemovingAll) {
				return HubParentService.this.getHubDataService()._remove(thisHub, obj, bDeleting, bIsRemovingAll);
			}
			@Override
			public void callHubData_move(Hub thisHub, Object obj, int posFrom, int posTo) {
				HubParentService.this.getHubDataService()._move(thisHub, obj, posFrom, posTo);				
			}
			@Override
			public boolean callHubData_insert(Hub thisHub, Object obj, int pos, boolean bIsLocked) {
				return HubParentService.this.getHubDataService()._insert(thisHub, obj, pos, bIsLocked);
			}
			@Override
			public boolean callHubData_add(Hub thisHub, Object obj, boolean bHasLock, boolean bCheckContains) {
				return HubParentService.this.getHubDataService()._add(thisHub, obj, bHasLock, bCheckContains);
			}
			@Override
			public void callHubDataSetChanged(Hub thisHub, boolean bChanged) {
				HubParentService.this.getHubDataService().setChanged(thisHub, bChanged);				
			}
			@Override
			public int callHubDataGetPos(Hub thisHub, Object object, boolean adjustMaster, boolean bUpdateLink) {
				return HubParentService.this.getHubDataService().getPos(thisHub, object, adjustMaster, bUpdateLink);
			}
			@Override
			public OAObject[] callHubDataGetRemovedObjects(Hub thisHub) {
				return HubParentService.this.getHubDataService().getRemovedObjects(thisHub);
			}
			@Override
			public Object callHubDataGetObjectAt(Hub thisHub, int pos) {
				return HubParentService.this.getHubDataService().getObjectAt(thisHub, pos);
			}
			@Override
			public OAObject[] callHubDataGetAddedObjects(Hub thisHub) {
				return HubParentService.this.getHubDataService().getAddedObjects(thisHub);
			}
			@Override
			public Vector callHubDataCreateVecRemove(Hub thisHub) {
				return HubParentService.this.getHubDataService().createVecRemove(thisHub);
			}
			@Override
			public boolean callHubDataContains(Hub hub, Object obj, boolean bJustAdded) {
				return HubParentService.this.getHubDataService().contains(hub, obj, bJustAdded);
			}
			@Override
			public void callHubCSRemoveFromHub(Hub thisHub, OAObject obj, int pos) {
				HubParentService.this.getHubCSService().removeFromHub(thisHub, obj, pos);				
			}
			@Override
			public void callHubCSRemoveAllFromHub(Hub thisHub) {
				HubParentService.this.getHubCSService().removeAllFromHub(thisHub);				
			}
			@Override
			public void callHubCSMoveObjectInHub(Hub thisHub, int posFrom, int posTo) {
				HubParentService.this.getHubCSService().moveObjectInHub(thisHub, posFrom, posTo);				
			}
			@Override
			public boolean callHubCSInsertInHub(Hub thisHub, OAObject obj, int pos) {
				return HubParentService.this.getHubCSService().insertInHub(thisHub, obj, pos);
			}
			@Override
			public void callHubCSAddToHub(Hub thisHub, OAObject thisObj) {
				HubParentService.this.getHubCSService().addToHub(thisHub, thisObj);;				
			}
			@Override
			public void callRemoteThreadSetStartedNextThread(boolean b) {
				Thread t = Thread.currentThread();
				if (t instanceof OARemoteThread) {
					OARemoteThread rt = (OARemoteThread) t;
					rt.setStartedNextThread(b);
				}
			}
		};
		return srvcHubAddRemove;
	}
	
	
	public HubAOService getHubAOService() {
		if (srvcHubAO != null) return srvcHubAO;
		
		srvcHubAO = new HubAOService(faBridge.getHubFriendAccess()) {
			@Override
			public void callThreadLocalUnlock(Object object) {
				HubParentService.this.srvcThreadLocal.unlock(object);				
			}
			@Override
			public void callThreadLocalLock(Object object) {
				HubParentService.this.srvcThreadLocal.lock(object);				
			}
			@Override
			public void callObjectReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				HubParentService.this.srvcObject.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);				
			}
			@Override
			public Object callObjectReflectGetProperty(OAObject oaObj, String propPath) {
				return HubParentService.this.srvcObject.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public Hub[] callHubShareGetAllSharedHubs(Hub thisHub, OAFilter<Hub> filter) {
				return HubParentService.this.getHubShareService().getAllSharedHubs(thisHub, filter);
			}
			@Override
			public void callHubLinkUpdateLinkProperty(Hub thisHub, Object fromObject, int pos) {
				HubParentService.this.getHubLinkService().updateLinkProperty(thisHub, fromObject, pos);				
			}
			@Override
			public Object callHubGetRealObject(Hub hub, Object object) {
				return HubParentService.this.getRealObject(hub, object);
			}
			@Override
			public void callHubEventFireAfterChangeActiveObjectEvent(Hub thisHub, Object obj, int pos, boolean bAllShared) {
				HubParentService.this.getHubEventService().fireAfterChangeActiveObjectEvent(thisHub, obj, pos, bAllShared);				
			}
			@Override
			public void callHubDetailUpdateAllDetail(Hub thisHub, boolean bUpdateLink) {
				HubParentService.this.getHubDetailService().updateAllDetail(thisHub, bUpdateLink);				
			}
			@Override
			public OAObject callHubDetailGetMasterObject(Hub thisHub) {
				return HubParentService.this.getHubDetailService().getMasterObject(thisHub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromMasterObjectToDetail(Hub thisDetailHub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromMasterObjectToDetail(thisDetailHub);
			}
			@Override
			public int callHubDataGetPos(Hub thisHub, Object object, boolean adjustMaster, boolean bUpdateLink) {
				return HubParentService.this.getHubDataService().getPos(thisHub, object, adjustMaster, bUpdateLink);
			}
			@Override
			public Object callHubDataGetObjectAt(Hub thisHub, int pos) {
				return HubParentService.this.getHubDataService().getObjectAt(thisHub, pos);
			}
		}; 
		return srvcHubAO;
	}

	public HubCSService getHubCSService() {
		if (srvcHubCS != null) return srvcHubCS;
		srvcHubCS = new HubCSService(faBridge.getHubFriendAccess()) {
			@Override
			public boolean callThreadLocalIsSuppressCSMessages() {
				return HubParentService.this.srvcThreadLocal.isSuppressCSMessages();
			}
			@Override
			public boolean callThreadLocalIsLoading() {
				return HubParentService.this.srvcThreadLocal.isLoading();
			}
			@Override
			public boolean callSyncClientIsObjectOnServer(OAObject obj) {
				return HubParentService.this.srvcSync.getSyncClient().isObjectOnServer(obj);
			}
			@Override
			public boolean callSyncSyncSort(Class objectClass, OAObjectKey objectKey, String hubPropertyName, String propertyPaths, boolean bAscending, Comparator comp) {
				return HubParentService.this.srvcSync.getRemoteSync().sort(objectClass, objectKey, hubPropertyName, propertyPaths, bAscending, comp);
			}
			@Override
			public boolean callSyncShouldSendMessages() {
				return HubParentService.this.srvcSync.shouldSendMessages();
			}
			@Override
			public boolean callSyncRemoteSyncRemoveFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName, Class objectClassX, OAObjectKey objectKeyX) {
				return HubParentService.this.srvcSync.getRemoteSync().removeFromHub(objectClass, objectKey, hubPropertyName, objectClassX, objectKeyX);
			}
			@Override
			public boolean callSyncRemoteSyncRemoveAllFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName) {
				return HubParentService.this.srvcSync.getRemoteSync().removeAllFromHub(objectClass, objectKey, hubPropertyName);
			}
			@Override
			public void callSyncSyncRefresh(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName) {
				HubParentService.this.srvcSync.getRemoteSync().refresh(masterObjectClass, masterObjectKey, hubPropertyName);
			}
			@Override
			public boolean callSyncSyncMoveObjectInHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName, int posFrom, int posTo) {
				return HubParentService.this.srvcSync.getRemoteSync().moveObjectInHub(objectClass, objectKey, hubPropertyName, posFrom, posTo);
			}
			@Override
			public boolean callSyncSyncInsertInHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj, int pos) {
				return HubParentService.this.srvcSync.getRemoteSync().insertInHub(masterObjectClass, masterObjectKey, hubPropertyName, obj, pos);
			}
			@Override
			public void callSyncSyncClearHubChanges(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName) {
				HubParentService.this.srvcSync.getRemoteSync().clearHubChanges(masterObjectClass, masterObjectKey, hubPropertyName);				
			}
			@Override
			public boolean callSyncSyncAddToHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj) {
				return HubParentService.this.srvcSync.getRemoteSync().addToHub(masterObjectClass, masterObjectKey, hubPropertyName, obj);
			}
			@Override
			public boolean callSyncSyncAddNewToHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, OAObjectSerializer obj) {
				return HubParentService.this.srvcSync.getRemoteSync().addNewToHub(masterObjectClass, masterObjectKey, hubPropertyName, obj);
			}
			@Override
			public boolean callSyncClientDeleteAll(Class objectClass, OAObjectKey objectKey, String hubPropertyName) {
				return HubParentService.this.srvcSync.getRemoteClient().deleteAll(objectClass, objectKey, hubPropertyName);
			}
			@Override
			public boolean callSyncIsSingleUser() {
				return HubParentService.this.srvcSync.isSingleUser();
			}
			@Override
			public boolean callSyncIsServer() {
				return HubParentService.this.srvcSync.isServer();
			}
			@Override
			public boolean callSyncIsClient() {
				return HubParentService.this.srvcSync.isClient();
			}
			@Override
			public boolean callSyncGetSuppressCSMessages() {
				return HubParentService.this.srvcSync.getSuppressCSMessages();
			}
			@Override
			public boolean callRemoteThreadShouldSendMessages() {
				return HubParentService.this.srvcRemoteThread.shouldSendMessages();
			}
			@Override
			public boolean callRemoteThreadIsRemoteThread() {
				return HubParentService.this.srvcRemoteThread.isRemoteThread();
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public OAObjectInfo callObjectInfoGetObjectInfo(Class c) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getOAObjectInfo(c);
			}
			@Override
			public OAObjectInfo callObjectInfoGetObjectInfo(OAObject obj) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getOAObjectInfo(obj);
			}
			@Override
			public boolean callObjectHubIsInHub(OAObject oaObj) {
				return HubParentService.this.srvcObject.getOAObjectHubService().isInHub(oaObj);
			}
			@Override
			public boolean callHubIsInHubWithMaster(OAObject oaObj, Hub hubIgnore) {
				return HubParentService.this.srvcObject.getOAObjectHubService().isInHubWithMaster(oaObj, hubIgnore);
			}
			@Override
			public boolean callHubIsInHubWithMaster(OAObject oaObj) {
				return HubParentService.this.srvcObject.getOAObjectHubService().isInHubWithMaster(oaObj);
			}
			@Override
			public String callHubDetailGetPropertyFromMasterToDetail(Hub thisHub) {
				return HubParentService.this.getHubDetailService().getPropertyFromMasterToDetail(thisHub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromMasterObjectToDetail(Hub thisDetailHub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromMasterObjectToDetail(thisDetailHub);
			}
		};
		return srvcHubCS;
	}
	
	public HubDataService getHubDataService() {
		if (srvcHubData != null) return srvcHubData;
		
		srvcHubData = new HubDataService(faBridge.getHubFriendAccess()) {
			@Override
			public OAObjectKey callObjectKeyGetKey(OAObject oaObj) {
				return HubParentService.this.srvcObject.getOAObjectKeyService().getKey(oaObj);
			}
			@Override
			public boolean callObjectKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return HubParentService.this.srvcObject.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}
			@Override
			public OAObject callObjectReflectGetObject(Class clazz, Object key) {
				return HubParentService.this.srvcObject.getOAObjectReflectService().getObject(clazz, key);
			}
			@Override
			public boolean callObjectHubAddHub(OAObject oaObj, Hub hub) {
				return HubParentService.this.srvcObject.getOAObjectHubService().addHub(oaObj, hub);
			}
			@Override
			public OALinkInfo callObjectInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getRecursiveLinkInfo(thisOI, type);
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public Object callObjectReflectGetProperty(OAObject oaObj, String propPath) {
				return HubParentService.this.srvcObject.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public <T extends OAObject> T callObjectCacheGet(Class<T> clazz, Object key) {
				return HubParentService.this.srvcObject.getOAObjectCacheService().get(clazz, key);
			}
			@Override
			public boolean callObjectHubIsAlreadyInHub(OAObject oaObj, Hub hubFind) {
				return HubParentService.this.srvcObject.getOAObjectHubService().isAlreadyInHub(oaObj, hubFind);
			}
			@Override
			public OAObjectKey callObjectKeyCreateObjectKey(Class c, Object... ids) {
				return HubParentService.this.srvcObject.getOAObjectKeyService().createObjectKey(c, ids);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub thisDetailHub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromMasterHubToDetail(thisDetailHub);
			}
			@Override
			public boolean callHubCSClearHubChanges(Hub thisHub) {
				return HubParentService.this.getHubCSService().clearHubChanges(thisHub);
			}
			@Override
			public void callHubdetailSetPropertyToMasterHub(Hub thisHub, Object detailObject, Object objMaster) {
				HubParentService.this.getHubDetailService().setPropertyToMasterHub(thisHub, detailObject, objMaster);				
			}
			@Override
			public boolean callHubSelectIsMoreData(Hub thisHub) {
				return HubParentService.this.getHubSelectService().isMoreData(thisHub);
			}
			@Override
			public int callHubSelectFetchMore(Hub thisHub) {
				return HubParentService.this.getHubSelectService().fetchMore(thisHub);
			}
			@Override
			public Object callHubGetRealObject(Hub hub, Object object) {
				return HubParentService.this.getRealObject(hub, object);
			}
			@Override
			public void callHubShareSetSharedHub(Hub thisHub, Hub sharedMasterHub, boolean shareActiveObject) {
				HubParentService.this.getHubShareService().setSharedHub(thisHub, sharedMasterHub, shareActiveObject);				
			}
			@Override
			public void callHubShareSetSharedHub(Hub thisHub, Hub sharedMasterHub, boolean shareActiveObject, Object newLinkValue) {
				HubParentService.this.getHubShareService().setSharedHub(thisHub, sharedMasterHub, shareActiveObject, newLinkValue);				
			}
			@Override
			public boolean callHubDetailSetMasterHubActiveObject(Hub thisHub, Object detailObject, boolean bUpdateLink) {
				return HubParentService.this.getHubDetailService().setMasterHubActiveObject(thisHub, detailObject, bUpdateLink);
			}
			@Override
			public void callThreadLocalLock(Object object) {
				HubParentService.this.srvcThreadLocal.lock(object);				
			}
			@Override
			public void callThreadLocalUnlock(Object object) {
				HubParentService.this.srvcThreadLocal.unlock(object);				
			}
			@Override
			public void callRemoteThreadStartNextThread() {
				HubParentService.this.srvcRemoteThread.startNextThread();				
			}
			@Override
			public boolean callThreadLocalIsLoading() {
				return HubParentService.this.srvcThreadLocal.isLoading();
			}
			@Override
			public boolean callSyncIsServer() {
				return HubParentService.this.srvcSync.isServer();
			}
		};
		return srvcHubData;
	}
	
	public HubDeleteService getHubDeleteService() {
		if (srvcHubDelete != null) return srvcHubDelete;
		
		srvcHubDelete =  new HubDeleteService(faBridge.getHubFriendAccess()) {
			@Override
			public void callObjectDeleteDelete(OAObject oaObj, OACascade cascade) {
				HubParentService.this.srvcObject.getOAObjectDeleteService().delete(oaObj, cascade);				
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public boolean callHubCSDeleteAll(Hub thisHub) {
				return HubParentService.this.getHubCSService().deleteAll(thisHub);
			}
			@Override
			public void callHubAddRemoveClear(Hub thisHub) {
				HubParentService.this.getHubAddRemoveService().clear(thisHub);				
			}
			@Override
			public void callHubDataClearHubChanges(Hub thisHub) {
				HubParentService.this.getHubDataService().clearHubChanges(thisHub);				
			}
			@Override
			public boolean callHubAddRemoveRemove(Hub thisHub, Object obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll) {
				return HubParentService.this.getHubAddRemoveService().remove(thisHub, obj, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub hub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public OAObject callHubDetailGetMasterObject(Hub thisHub) {
				return HubParentService.this.getHubDetailService().getMasterObject(thisHub);
			}
			@Override
			public Vector callHubDataCreateVecRemove(Hub thisHub) {
				return HubParentService.this.getHubDataService().createVecRemove(thisHub);
			}
			@Override
			public void callHubDataSetChanged(Hub thisHub, boolean bChanged) {
				HubParentService.this.getHubDataService().setChanged(thisHub, bChanged);				
			}
			@Override
			public void callHub_updateHubAddsAndRemoves(Hub thisHub, int iCascadeRule, OACascade cascade, boolean bIsSaving) {
				HubParentService.this._updateHubAddsAndRemoves(thisHub, iCascadeRule, cascade, bIsSaving);				
			}
			@Override
			public void callThreadLocalSetDeleting(Hub hub, boolean b) {
				HubParentService.this.srvcThreadLocal.setDeleting(hub, b);				
			}
			@Override
			public boolean callThreadLocalIsDeleting(Hub hub) {
				return HubParentService.this.srvcThreadLocal.isDeleting(hub);
			}
			@Override
			public void callThreadLocalLock(Hub hub) {
				HubParentService.this.srvcThreadLocal.lock(hub);				
			}
			@Override
			public void callThreadLocalUnlock(Hub hub) {
				HubParentService.this.srvcThreadLocal.unlock(hub);				
			}
			@Override
			public void callRemoteThreadSendMessages(boolean b) {
				HubParentService.this.srvcRemoteThread.sendMessages(b);				
			}
		};
		
		return srvcHubDelete;
	}

	public HubDetailService getHubDetailService() {
		if (srvcHubDetail != null) return srvcHubDetail;
		srvcHubDetail = new HubDetailService(faBridge.getHubFriendAccess()) {
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public Object callObjectReflectGetProperty(OAObject oaObj, String propPath) {
				return HubParentService.this.srvcObject.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public void callObjectReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				HubParentService.this.srvcObject.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);				
			}
			@Override
			public Method callObjectInfoGetMethod(Class clazz, String methodName) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getMethod(clazz, methodName);
			}
			@Override
			public boolean callObjectReflectIsReferenceHubLoaded(OAObject oaObj, String propertyName) {
				return HubParentService.this.srvcObject.getOAObjectReflectService().isReferenceHubLoaded(oaObj, propertyName);
			}
			@Override
			public void callObjectHubRemoveHub(OAObject oaObj, Hub hub, boolean bIsOnHubFinalize) {
				HubParentService.this.srvcObject.getOAObjectHubService().removeHub(oaObj, hub, bIsOnHubFinalize);				
			}
			@Override
			public OALinkInfo callObjectInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}
			@Override
			public OALinkInfo callObjectInfoGetLinkInfo(OAObjectInfo oi, OAObject fromObject, Hub hub) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getLinkInfo(oi, fromObject, hub);
			}
			@Override
			public OAObjectInfo callObjectInfoGetOAObjectInfo(Class clazz) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public boolean callSyncIsServer() {
				return HubParentService.this.srvcSync.isServer();
			}
			@Override
			public boolean callThreadLocalGetCanAdjustHub(Hub hub) {
				return HubParentService.this.srvcThreadLocal.getCanAdjustHub(hub);
			}
			@Override
			public Object callHubAOSetActiveObject(Hub thisHub, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
				return HubParentService.this.getHubAOService().setActiveObject(thisHub, pos, bUpdateLink, bForce, bCalledByShareHub);
			}
			@Override
			public void callHubAOSetActiveObject(Hub thisHub, Object object, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, pos, bUpdateLink, bForce, bCalledByShareHub);				
			}
			@Override
			public void callHubAOSetActiveObject(Hub thisHub, Object object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, adjustMaster, bUpdateLink, bForce);				
			}
			@Override
			public WeakReference<Hub>[] callHubShareGetSharedWeakHubs(Hub thisHub) {
				return HubParentService.this.getHubShareService().getSharedWeakHubs(thisHub);
			}
			@Override
			public Hub callHubShareGetFirstSharedHub(Hub thisHub, OAFilter<Hub> filter, boolean bIncludeFilteredHubs, boolean bOnlyIfSharedAO) {
				return HubParentService.this.getHubShareService().getFirstSharedHub(thisHub, filter, bIncludeFilteredHubs, bOnlyIfSharedAO);
			}
			@Override
			public String callHubGetPropertyPathforClasses(Hub hub, Class[] classes) {
				return HubParentService.this.getPropertyPathforClasses(hub, classes);
			}
			@Override
			public void callHubGetObjectClass(Hub thisHub, Class objClass) {
				HubParentService.this.setObjectClass(thisHub, objClass);				
			}
			@Override
			public Hub callHubShareGetMainSharedHub(Hub hub) {
				return HubParentService.this.getHubShareService().getMainSharedHub(hub);
			}
			@Override
			public void callHubShareSyncSharedHubs(Hub thisHub, boolean bShareActiveObject, HubDataActive daOld, HubDataActive daNew, boolean bUpdateLink) {
				HubParentService.this.getHubShareService().syncSharedHubs(thisHub, bShareActiveObject, daOld, daNew, bUpdateLink);				
			}
			@Override
			public void callHubShareRemoveSharedHub(Hub sharedHub, Hub hub) {
				HubParentService.this.getHubShareService()._removeSharedHub(sharedHub, hub);				
			}
			@Override
			public void callHubEventFireOnNewListEvent(Hub thisHub, boolean bAll) {
				HubParentService.this.getHubEventService().fireOnNewListEvent(thisHub, bAll);				
			}
			@Override
			public Object callHubDataGetObjectAt(Hub thisHub, int pos) {
				return HubParentService.this.getHubDataService().getObjectAt(thisHub, pos);
			}
			@Override
			public boolean callHubSortIsSorted(Hub thisHub) {
				return HubParentService.this.getHubSortService().isSorted(thisHub);
			}
			@Override
			public String callHubSortGetSortProperty(Hub thisHub) {
				return HubParentService.this.getHubSortService().getSortProperty(thisHub);
			}
			@Override
			public boolean callHubSortGetSortAsc(Hub thisHub) {
				return HubParentService.this.getHubSortService().getSortAsc(thisHub);
			}
			@Override
			public void callHubShareAddSharedHub(Hub thisHub, Hub hub) {
				HubParentService.this.getHubShareService().addSharedHub(thisHub, hub);				
			}
			@Override
			public boolean callHubAddRemoveInternalAdd(Hub thisHub, Object obj, boolean bHasLock, boolean bCheckContains) {
				return HubParentService.this.getHubAddRemoveService().internalAdd(thisHub, obj, bHasLock, bCheckContains);
			}
			@Override
			public void callHubDataIncChangeCount(Hub thisHub) {
				HubParentService.this.getHubDataService().incChangeCount(thisHub);				
			}
			@Override
			public Hub callHubLinkGetHubWithLink(Hub thisHub, boolean bIncludeCopiedHubs) {
				return HubParentService.this.getHubLinkService().getHubWithLink(thisHub, bIncludeCopiedHubs);
			}
		};
		return srvcHubDetail;
	}

	
	public HubDSService getHubDSService() {
		if (srvcHubDS != null) return srvcHubDS;
    	srvcHubDS = new HubDSService(faBridge.getHubFriendAccess()) {
			@Override
			public boolean callObjectInfoIsMany2Many(OALinkInfo thisLi) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().isMany2Many(thisLi);
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}

			@Override
			public OAObject[] callHubAddRemoveGetRemovedObjects(Hub thisHub) {
				return HubParentService.this.getHubAddRemoveService().getRemovedObjects(thisHub);
			}
    	};
		return srvcHubDS;
	}
	
	public HubEventService getHubEventService() {
		if (srvcHubEvent != null) return srvcHubEvent;
		
    	srvcHubEvent = new HubEventService(faBridge.getHubFriendAccess()) {
			@Override
			public OAObjectCallback callObjectCallbackGetVerifyRemoveObjectCallback(Hub hub, OAObject objRemove, int checkType) {
				return HubParentService.this.srvcObject.getOAObjectCallbackService().getVerifyRemoveObjectCallback(hub, objRemove, checkType);
			}
			@Override
			public <T extends OAObject> void callObjectCacheFireAfterRemoveEvent(Hub<T> hub, T obj) {
				HubParentService.this.srvcObject.getOAObjectCacheService().fireAfterRemoveEvent(hub, obj);
			}
			@Override
			public OAObjectInfo callObjectInfoGetOAObjectInfo(Class clazz) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject obj) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getOAObjectInfo(obj);
			}
			@Override
			public OAObjectCallback callObjectCallbackGetVerifyRemoveAllObjectCallback(Hub hub, int checkType) {
				return HubParentService.this.srvcObject.getOAObjectCallbackService().getVerifyRemoveAllObjectCallback(hub, checkType);
			}
			@Override
			public <T extends OAObject> void callObjectCacheFireAfterAddEvent(Hub<T> hub, T obj) {
				HubParentService.this.srvcObject.getOAObjectCacheService().fireAfterAddEvent(hub, obj);				
			}
			@Override
			public OAObjectCallback callObjectCallbackGetVerifyAddObjectCallback(Hub hub, OAObject oaObj, int checkType) {
				return HubParentService.this.srvcObject.getOAObjectCallbackService().getVerifyAddObjectCallback(hub, oaObj, checkType);
			}
			@Override
			public OALinkInfo callObjectInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}
			@Override
			public String callHubDetailGetPropertyFromMasterToDetail(Hub thisHub) {
				return HubParentService.this.getHubDetailService().getPropertyFromMasterToDetail(thisHub);
			}
			@Override
			public boolean callHubVerifyUniqueProperty(Hub thisHub, Object object) {
				return HubParentService.this.verifyUniqueProperty(thisHub, object);
			}
			@Override
			public void callHubDetailUpdateDetail(Hub thisHub, HubDetail detail, Hub detailHub, boolean bUpdateLink) {
				HubParentService.this.getHubDetailService().updateDetail(thisHub, detail, detailHub, bUpdateLink);				
			}
			@Override
			public WeakReference<Hub>[] callHubShareGetSharedWeakHubs(Hub thisHub) {
				return HubParentService.this.getHubShareService().getSharedWeakHubs(thisHub);
			}
			@Override
			public void callHubDataIncChangeCount(Hub thisHub) {
				HubParentService.this.getHubDataService().incChangeCount(thisHub);
			}
			@Override
			public boolean callRemoteThreadIsRemoteThread() {
				return HubParentService.this.srvcRemoteThread.isRemoteThread();
			}
			@Override
			public void callThreadLocalAddHubEvent(HubEvent he) {
				HubParentService.this.srvcThreadLocal.addHubEvent(he);
			}
			@Override
			public void callThreadLocalRemoveHubEvent(HubEvent he) {
				HubParentService.this.srvcThreadLocal.removeHubEvent(he);
			}
			@Override
			public boolean callThreadLocalIsLoading() {
				return HubParentService.this.srvcThreadLocal.isLoading();
			}
			@Override
			public boolean callRemoteThreadShouldEventsBeQueued() {
				return HubParentService.this.srvcRemoteThread.shouldEventsBeQueued();
			}
			@Override
			public boolean callRemoteThreadQueueEvent(Runnable r) {
				return HubParentService.this.srvcRemoteThread.queueEvent(r);
			}
			@Override
			public boolean callThreadLocalHasSentCalcPropertyChange(Hub thisHub, OAObject thisObj, String propertyName) {
				return HubParentService.this.srvcThreadLocal.hasSentCalcPropertyChange(thisHub, thisObj, propertyName);
			}
    	};
		return srvcHubEvent;
	}

	public HubFindService getHubFindService() {
		if (srvcHubFind != null) return srvcHubFind;
    	srvcHubFind = new HubFindService();
		return srvcHubFind;
	}
	
	public HubLinkService getHubLinkService() {
		if (srvcHubLink != null) return srvcHubLink;
    	srvcHubLink = new HubLinkService(faBridge.getHubFriendAccess()) {
			@Override
			public OAObjectInfo callObjectInfoGetOAObjectInfo(Class clazz) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public Method callObjectInfoGetMethod(Class clazz, String methodName) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getMethod(clazz, methodName);
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public void callHubEventRemoveHubListener(Hub thisHub, HubListener l) {
				HubParentService.this.getHubEventService().removeHubListener(thisHub, l);				
			}
			@Override
			public void callHubEventFireAfterPropertyChange(Hub thisHub, OAObject oaObj, String propertyName, Object oldValue, Object newValue, OALinkInfo linkInfo) {
				HubParentService.this.getHubEventService().fireAfterPropertyChange(thisHub, oaObj, propertyName, oldValue, newValue, linkInfo);				
			}
			@Override
			public void callHubEventAddHubListener(Hub thisHub, HubListener hl) {
				HubParentService.this.getHubEventService().addHubListener(thisHub, hl);				
			}
			@Override
			public void callHubEventFireAfterChangeActiveObjectEvent(Hub thisHub, Object obj, int pos, boolean bAllShared) {
				HubParentService.this.getHubEventService().fireAfterChangeActiveObjectEvent(thisHub, obj, pos, bAllShared);
			}
			@Override
			public Hub callHubShareGetFirstSharedHub(Hub thisHub, OAFilter<Hub> filter, boolean bIncludeFilteredHubs, boolean bOnlyIfSharedAO) {
				return HubParentService.this.getHubShareService().getFirstSharedHub(thisHub, filter, bIncludeFilteredHubs, bOnlyIfSharedAO);
			}
			@Override
			public void callHubAOSetActiveObject(Hub thisHub, Object object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, adjustMaster, bUpdateLink, bForce);
			}
			@Override
			public void callHubAOSetActiveObject(Hub thisHub, Object object, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, pos, bUpdateLink, bForce, bCalledByShareHub);
			}
			@Override
			public HubDataMaster callHubDetailGetDataMaster(Hub thisHub) {
				return HubParentService.this.getHubDetailService().getDataMaster(thisHub);
			}
			@Override
			public int callHubDataGetPos(Hub thisHub, Object object, boolean adjustMaster, boolean bUpdateLink) {
				return HubParentService.this.getHubDataService().getPos(thisHub, object, adjustMaster, bUpdateLink);
			}
			@Override
			public Hub[] callHubShareGetAllSharedHubs(Hub thisHub) {
				return HubParentService.this.getHubShareService().getAllSharedHubs(thisHub);
			}
			@Override
			public void callThreadLocalAddDontAdjustHub(Hub hub) {
				HubParentService.this.srvcThreadLocal.addDontAdjustHub(hub);
			}
			@Override
			public void callThreadLocalRemoveDontAdjustHub(Hub hub) {
				HubParentService.this.srvcThreadLocal.removeDontAdjustHub(hub);
			}
    	};
		return srvcHubLink;
	}

	public HubRootService getHubRootService() {
		if (srvcHubRoot != null) return srvcHubRoot;
		
    	srvcHubRoot = new HubRootService(faBridge.getHubFriendAccess()) {
			@Override
			public OALinkInfo callObjectInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getRecursiveLinkInfo(thisOI, type);
			}
			@Override
			public Hub callObjectInfoGetRootHub(OAObjectInfo thisOI) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getRootHub(thisOI);
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public Object callObjectReflectGetProperty(OAObject oaObj, String propPath) {
				return HubParentService.this.srvcObject.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public OALinkInfo callObjectInfoGetLinkToOwner(OAObjectInfo thisOI) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getLinkToOwner(thisOI);
			}
			@Override
			public void callObjectInfoSetRootHub(OAObjectInfo thisOI, Hub h) {
				HubParentService.this.srvcObject.getOAObjectInfoService().setRootHub(thisOI, h);
			}
			@Override
			public Hub[] callHubShareGetAllSharedHubs(Hub thisHub, OAFilter<Hub> filter) {
				return HubParentService.this.getHubShareService().getAllSharedHubs(thisHub, filter);
			}
    	};
		
		return srvcHubRoot;
	}

	public HubSaveService getHubSaveService() {
		if (srvcHubSave != null) return srvcHubSave;
		
    	srvcHubSave = new HubSaveService() {
			@Override
			public void callObjectSaveSave(OAObject oaObj, int iCascadeRule, OACascade cascade) {
				HubParentService.this.srvcObject.getOAObjectSaveService().save(oaObj, iCascadeRule, cascade);
			}
			@Override
			public boolean callObjectInfoIsMany2Many(OALinkInfo thisLi) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().isMany2Many(thisLi);
			}
			@Override
			public void callObjectSaveSaveObjectOnly(OAObject oaObj, OACascade cascade) {
				HubParentService.this.srvcObject.getOAObjectSaveService()._saveObjectOnly(oaObj, cascade);
			}
			@Override
			public HubDataMaster callHubDetailGetDataMaster(Hub thisHub) {
				return HubParentService.this.getHubDetailService().getDataMaster(thisHub);
			}
			@Override
			public OAObject[] callHubDataGetAddedObjects(Hub thisHub) {
				return HubParentService.this.getHubDataService().getAddedObjects(thisHub);
			}
			@Override
			public void callHub_updateHubAddsAndRemoves(Hub thisHub, int iCascadeRule, OACascade cascade, boolean bIsSaving) {
				HubParentService.this._updateHubAddsAndRemoves(thisHub, iCascadeRule, cascade, bIsSaving);
			}
			@Override
			public void callHubSetReferenceable(Hub hub, boolean bReferenceable) {
				HubParentService.this.setReferenceable(hub, bReferenceable);;
			}
    	};
		
		return srvcHubSave;
	}
	
	
	public HubSelectService getHubSelectService() {
		if (srvcHubSelect != null) return srvcHubSelect;
		
    	srvcHubSelect = new HubSelectService(faBridge.getHubFriendAccess()) {
			@Override
			public OAObjectInfo callObjectInfoGetObjectInfo(Class clazz) {
				return HubParentService.this.srvcObject.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public void callObjectHubRemoveHub(OAObject oaObj, Hub hub, boolean bIsOnHubFinalize) {
				HubParentService.this.srvcObject.getOAObjectHubService().removeHub(oaObj, hub, bIsOnHubFinalize);
			}
			@Override
			public void callObjectCacheSetSelectAllHub(Hub hub) {
				HubParentService.this.srvcObject.getOAObjectCacheService().setSelectAllHub(hub);
			}
			@Override
			public void callObjectCacheRemoveSelectAllHub(Hub hub) {
				HubParentService.this.srvcObject.getOAObjectCacheService().removeSelectAllHub(hub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub hub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public void callHubDataEnsureCapacity(Hub thisHub, int size) {
				HubParentService.this.getHubDataService().ensureCapacity(thisHub, size);
			}
			@Override
			public boolean callHubAddRemoveAdd(Hub thisHub, Object obj) {
				return HubParentService.this.getHubAddRemoveService().add(thisHub, obj);
			}
			@Override
			public void callHubEventFireBeforeSelectEvent(Hub thisHub) {
				HubParentService.this.getHubEventService().fireBeforeSelectEvent(thisHub);
			}
			@Override
			public void callHubDataIncChangeCount(Hub thisHub) {
				HubParentService.this.getHubDataService().incChangeCount(thisHub);
			}
			@Override
			public int callHubDataGetCurrentSize(Hub thisHub) {
				return HubParentService.this.getHubDataService().getCurrentSize(thisHub);
			}
			@Override
			public Object callHubDataGetObjectAt(Hub thisHub, int pos) {
				return HubParentService.this.getHubDataService().getObjectAt(thisHub, pos);
			}
			@Override
			public void callHubDataClearAllAndReset(Hub thisHub) {
				HubParentService.this.getHubDataService().clearAllAndReset(thisHub);
			}
			@Override
			public Hub<?>[] callHubShareGetAllSharedHubs(Hub<?> thisHub, OAFilter<Hub<?>> filter) {
				return HubParentService.this.getHubShareService().getAllSharedHubs(thisHub, filter);
			}
			@Override
			public void callHubEventFireOnNewListEvent(Hub thisHub, boolean bAll) {
				HubParentService.this.getHubEventService().fireOnNewListEvent(thisHub, bAll);
			}
			@Override
			public void callHubDataResizeToFit(Hub thisHub) {
				HubParentService.this.getHubDataService().resizeToFit(thisHub);
			}
			@Override
			public void callHubEventFireBeforeRefreshEvent(Hub thisHub) {
				HubParentService.this.getHubEventService().fireBeforeRefreshEvent(thisHub);
			}
			@Override
			public OAObject callHubDetailGetMasterObject(Hub thisHub) {
				return HubParentService.this.getHubDetailService().getMasterObject(thisHub);
			}
			@Override
			public String callHubDetailGetPropertyFromMasterToDetail(Hub thisHub) {
				return HubParentService.this.getHubDetailService().getPropertyFromMasterToDetail(thisHub);
			}
			@Override
			public boolean callThreadLocalSetLoading(boolean b) {
				return HubParentService.this.srvcThreadLocal.setLoading(b);
			}
			@Override
			public void callThreadLocalSetRefreshing(boolean b) {
				HubParentService.this.srvcThreadLocal.setRefreshing(b);
			}
    	};
		return srvcHubSelect;
	}

	
	public HubSerializeService getHubSerializeService() {
		if (srvcHubSerialize != null) return srvcHubSerialize;

    	srvcHubSerialize = new HubSerializeService(faBridge.getHubFriendAccess()) {
			@Override
			public boolean callObjectHubIsAlreadyInHub(OAObject oaObj, OALinkInfo li) {
				return HubParentService.this.srvcObject.getOAObjectHubService().isAlreadyInHub(oaObj, li);
			}
			@Override
			public boolean callObjectHubAddHub(OAObject oaObj, Hub hub) {
				return HubParentService.this.srvcObject.getOAObjectHubService().addHub(oaObj, hub);
			}
			@Override
			public boolean callHubSelectIsMoreData(Hub thisHub) {
				return HubParentService.this.getHubSelectService().isMoreData(thisHub);
			}
			@Override
			public void callHubSelectLoadAllData(Hub thisHub) {
				HubParentService.this.getHubSelectService().loadAllData(thisHub);	
			}
			@Override
			public void callThreadLocalSetSuppressCSMessages(boolean b) {
				HubParentService.this.srvcThreadLocal.setSuppressCSMessages(b);
			}
    	};
		return srvcHubSerialize;
	}


	public HubShareService getHubShareService() {
		if (srvcHubShare != null) return srvcHubShare;
		
    	srvcHubShare = new HubShareService(faBridge.getHubFriendAccess()) {
			@Override
			public void callObjectHubRemoveHub(OAObject oaObj, Hub hub, boolean bIsOnHubFinalize) {
				HubParentService.this.srvcObject.getOAObjectHubService().removeHub(oaObj, hub, bIsOnHubFinalize);
			}
			@Override
			public HubListener[] callHubEventGetHubListeners(Hub thisHub) {
				return HubParentService.this.getHubEventService().getHubListeners(thisHub);
			}
			@Override
			public void callHubAOSetActiveObject(Hub thisHub, Object object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, adjustMaster, bUpdateLink, bForce);
			}
			@Override
			public void callHubAOSetActiveObject(Hub thisHub, Object object, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, pos, bUpdateLink, bForce, bCalledByShareHub);
			}
			@Override
			public void callHubEventClearGetAllListenerCache(Hub hub) {
				HubParentService.this.getHubEventService().clearGetAllListenerCache(hub);
			}
			@Override
			public void callHubEventFireAfterChangeActiveObjectEvent(Hub thisHub, Object obj, int pos, boolean bAllShared) {
				HubParentService.this.getHubEventService().fireAfterChangeActiveObjectEvent(thisHub, obj, pos, bAllShared);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub hub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub thisDetailHub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromMasterHubToDetail(thisDetailHub);
			}
			@Override
			public void callHubDataIncChangeCount(Hub thisHub) {
				HubParentService.this.getHubDataService().incChangeCount(thisHub);
			}
			@Override
			public void callHubSetObjectClass(Hub thisHub, Class objClass) {
				HubParentService.this.setObjectClass(thisHub, objClass);
			}
			@Override
			public void callHubEventFireOnNewListEvent(Hub thisHub, boolean bAll) {
				HubParentService.this.getHubEventService().fireOnNewListEvent(thisHub, bAll);
			}
			@Override
			public boolean callRemoteThreadIsRemoteThread() {
				return HubParentService.this.srvcRemoteThread.isRemoteThread();
			}
    	};
		return srvcHubShare;
	}

	public HubSortService getHubSortService() {
		if (srvcHubSort != null) return srvcHubSort;
		
    	srvcHubSort = new HubSortService(faBridge.getHubFriendAccess()) {
			@Override
			public void callHubCSSort(Hub thisHub, String propertyPaths, boolean bAscending, Comparator comp) {
				HubParentService.this.getHubCSService().sort(thisHub, propertyPaths, bAscending, comp);				
			}
			@Override
			public void callHubSelectLoadAllData(Hub thisHub) {
				HubParentService.this.getHubSelectService().loadAllData(thisHub);				
			}
			@Override
			public void callHubEventFireAfterSortEvent(Hub thisHub) {
				HubParentService.this.getHubEventService().fireAfterSortEvent(thisHub);
			}
			@Override
			public void callThreadLocalLock(Object object) {
				HubParentService.this.srvcThreadLocal.lock(object);
			}
			@Override
			public void callThreadLocalUnlock(Object object) {
				HubParentService.this.srvcThreadLocal.unlock(object);
			}
			@Override
			public void callRemoteThreadStartNextThread() {
				HubParentService.this.srvcRemoteThread.startNextThread();
			}
			@Override
			public boolean callThreadLocalAddSiblingHelper(OASiblingHelper sh) {
				return HubParentService.this.srvcThreadLocal.addSiblingHelper(sh);
			}
			@Override
			public void callThreadLocalRemoveSiblingHelper(OASiblingHelper sh) {
				HubParentService.this.srvcThreadLocal.removeSiblingHelper(sh);
			}
    	};
		return srvcHubSort;
	}
	
	public HubXMLService getHubXMLService() {
		if (srvcHubXML != null) return srvcHubXML;
		
    	srvcHubXML = new HubXMLService() {
			@Override
			public void callObjectXMLWrite(OAObject oaObj, OAXMLWriter ow, String tagName, boolean bKeyOnly, OACascade cascade) {
				srvcObject.getOAObjectXMLService().write(oaObj, ow, tagName, bKeyOnly, cascade);
			}
    	};
		
		return srvcHubXML;
	}
	
	
/*	
	@OAParentProvided (example = "")
	public abstract ;
	
	@OAParentProvided (example = "")
	public abstract ;
	
	@OAParentProvided (example = "")
	public abstract ;
	
	@OAParentProvided (example = "")
	public abstract ;
	
	@OAParentProvided (example = "")
	public abstract ;
	
	@OAParentProvided (example = "")
	public abstract ;
*/	



	

	/**
	 * Determines whether this hub or any of its contained OAObjects are marked as
	 * changed according to the supplied cascade rules.
	 *
	 * <p>
	 * The method first checks whether this hub has already been processed in the
	 * current cascade; if so, it returns {@code false}. It then evaluates the hub’s
	 * own changed state. If cascade rules allow, it iterates through each object in
	 * the hub and checks whether any OAObject reports a changed state.
	 *
	 * @param thisHub      the hub being evaluated
	 * @param iCascadeRule the cascade rule used to determine how far change
	 *                     detection should propagate
	 * @param cascade      the cascade tracker used to prevent reprocessing
	 * @return {@code true} if the hub or any contained OAObject is changed;
	 *         otherwise {@code false}
	 */
	public boolean getChanged(Hub thisHub, int iCascadeRule, OACascade cascade) {
		if (cascade.wasCascaded(thisHub, true)) {
			return false;
		}

		final OAGraphImpl og = (OAGraphImpl) (OARuntime.graph(thisHub));
		if (srvcHubData.getChanged(thisHub)) {
			return true;
		}
		if (iCascadeRule == OAObject.CASCADE_NONE) {
			return false;
		}

		if (thisHub.isOAObject()) {
			for (int i = 0;; i++) {
				Object object = srvcHubData.getObjectAt(thisHub, i);
				if (object == null) {
					break;
				}
				if (object instanceof OAObject) {
					OAObject obj = (OAObject) object;
					if (srvcObject.getChanged(obj, iCascadeRule, cascade)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Verifies that the specified object's unique property value does not already
	 * exist in this hub. If the hub or object is null, or if the object is loading,
	 * uniqueness checking is bypassed. When a unique property is defined, its value
	 * is obtained either through a link property or a getter method. Null or blank
	 * values are not checked.
	 *
	 * <p>
	 * The method iterates through all hub elements and compares each object's
	 * unique property value to that of the given object. If an equal value is found
	 * on a different object, the uniqueness constraint fails.
	 *
	 * @param thisHub the hub in which uniqueness is validated
	 * @param object  the object whose property value is being checked
	 * @return {@code true} if the unique value does not conflict; otherwise
	 *         {@code false}
	 */
	public boolean verifyUniqueProperty(final Hub thisHub, final Object object) {
		if (thisHub == null || object == null) {
			return true;
		}

		if (object instanceof OAObject) {
			if (srvcThreadLocal.isLoading()) {
				return true;
			}
		}

		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		final HubDataMaster hdm = faBridge.getHubFriendAccess().getHubDataMaster(thisHub);
		
		Object object2;
		Method m = null;
		String uniqueLinkPropName;
		try {
			
			uniqueLinkPropName = hd.getUniqueProperty();
			if (uniqueLinkPropName == null) {
				uniqueLinkPropName = hdm.getUniqueProperty();
			}
			if (uniqueLinkPropName != null) {
				OAObjectInfo oi = thisHub.getOAObjectInfo();
				if (oi.getLinkInfo(uniqueLinkPropName) == null) {
					uniqueLinkPropName = null;
				}
			}

			if (uniqueLinkPropName != null) {
				object2 = srvcObject.getOAObjectPropertyService().getProperty((OAObject) object, uniqueLinkPropName);
			} else {
				m = hd.getUniquePropertyGetMethod();
				if (m == null) {
					m = hdm.getUniquePropertyGetMethod();
					if (m == null) {
						return true;
					}
				}
				object2 = m.invoke(object, (Object[]) null);
				if (object2 == null) {
					return true;
				}
				if (object2 instanceof String && ((String) object2).equals("")) {
					return true;
				}
			}
		} catch (Exception e) {
			String s = m == null ? "" : m.getName();
			throw new RuntimeException("Error invoking " + s, e);
		}

		for (int i = 0;; i++) {
			Object obj = thisHub.elementAt(i);
			if (obj == null) {
				break;
			}
			if (obj == object) {
				continue;
			}

			try {
				if (uniqueLinkPropName != null) {
					Object obj2 = srvcObject.getOAObjectPropertyService().getProperty((OAObject) obj, uniqueLinkPropName);
					if (OACompare.compare(obj2, object2) == 0) {
						return false;
					}
					continue;
				}

				Object obj2 = m.invoke(obj, (Object[]) null);
				if (obj2 == null) {
					continue;
				}
				if (obj2 == object2 || obj2.equals(object2)) {
					return false;
				}
			} catch (Exception e) {
				String s = m == null ? "" : m.getName();
				throw new RuntimeException("Error invoking " + s, e);
			}
		}
		return true;
	}

	/**
	 * Resolves the canonical instance of the given object for this hub. If the
	 * object's class does not match the hub's object class, the cache is queried
	 * first; if no cached instance exists, the hub is asked to resolve the object,
	 * potentially triggering data loading.
	 *
	 * @param hub    the hub providing the object class and lookup context
	 * @param object the object or key to resolve
	 * @return the resolved object instance, or the original value if no resolution
	 *         occurs
	 */
	public Object getRealObject(Hub hub, Object object) {
		if (object != null && !object.getClass().equals(hub.getObjectClass())) {
			Object objx = srvcObject.getOAObjectCacheService().get(hub.getObjectClass(), object);
			if (objx != null) {
				return objx;
			}
			object = srvcHubData.getObject(hub, object); // might not have loaded all data yet (fetchMore will be called)
		}
		return object;
	}

	/**
	 * Builds a property path linking the hub's object class through a sequence of
	 * classes. For each class in the array, the method locates a matching link
	 * property that targets that class. If multiple matching links are found, an
	 * exception is thrown. If no matching link exists, {@code null} is returned.
	 *
	 * @param hub     the starting hub whose object class defines the first segment
	 * @param classes array of classes describing the traversal path
	 * @return a dot-delimited property path, or {@code null} if a segment cannot be
	 *         resolved
	 */
	public String getPropertyPathforClasses(Hub hub, Class[] classes) {
		if (classes == null) {
			return null;
		}
		Class c = hub.getObjectClass();
		String path = null;
		int x = classes.length;
		for (int i = 0; i < x; i++) {
			OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(c); // this never returns null

			// find property to use
			List al = oi.getLinkInfos();
			OALinkInfo liFound = null;
			for (int ii = 0; ii < al.size(); ii++) {
				OALinkInfo li = (OALinkInfo) al.get(ii);
				if (classes[i].equals(li.getToClass())) {
					if (li.getToClass() == null) {
						if (liFound != null) {
							continue;
						}
					}
					if (liFound != null) {
						throw new RuntimeException("more then one link for hubClass=" + c + ", find linkClass=" + classes[i]);
					}
					liFound = li;
					// if (li.getType() == li.ONE) break;  // try to find ONE type, but will settle on MANY
				}
			}
			if (liFound == null) {
				return null;
			}
			if (path == null) {
				path = liFound.getName();
			} else {
				path += "." + liFound.getName();
			}
			c = classes[i];
		}
		return path;
	}

	/**
	 * Returns the master OAObject associated with this hub. If no master
	 * relationship exists or the hub is null, {@code null} is returned.
	 *
	 * @param hub the hub whose master object is requested
	 * @return the master OAObject, or {@code null} if none exists
	 */
	public OAObject getMasterObject(Hub hub) {
		if (hub == null) {
			return null;
		}
		HubDataMaster dm = srvcHubDetail.getDataMaster(hub, true);
		if (dm == null) {
			return null;
		}
		return dm.getMasterObject();
	}

	/**
	 * Returns the class of the hub's master OAObject. If the master object exists,
	 * its class is returned; otherwise, if a master hub exists, that hub's object
	 * class is used. If neither is available, {@code null} is returned.
	 *
	 * @param hub the hub whose master object's class is requested
	 * @return the master class, or {@code null} if unavailable
	 */
	public Class getMasterClass(Hub hub) {
		if (hub == null) {
			return null;
		}
		HubDataMaster dm = srvcHubDetail.getDataMaster(hub, true);
		Object obj = dm.getMasterObject();
		if (obj != null) {
			return obj.getClass();
		}
		if (dm.getMasterHub() != null) {
			return dm.getMasterHub().getObjectClass();
		}
		return null;
	}

	/**
	 * Assigns the object class for this hub. The class cannot be changed if the hub
	 * already contains objects, has detail hubs, has a master object, or is shared.
	 * If validation passes, the hub's object class is updated.
	 *
	 * @param thisHub  the hub whose object class is being changed
	 * @param objClass the new object class
	 * @throws RuntimeException if the object class cannot be changed due to
	 *                          existing state constraints
	 */
	public void setObjectClass(Hub thisHub, Class objClass) {
		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		final HubDataMaster hdm = faBridge.getHubFriendAccess().getHubDataMaster(thisHub);
		final HubDataUnique hdu = faBridge.getHubFriendAccess().getHubDataUnique(thisHub);
		
		Class cx = faBridge.getHubFriendAccess().getHubData(thisHub).getObjClass();
		
		if (cx != null && !cx.equals(objClass) && !cx.equals(OAObject.class)) {
			if (srvcHubData.getCurrentSize(thisHub) > 0
					|| (hdu.getVecHubDetail() != null && hdu.getVecHubDetail().size() > 0)) {
				throw new RuntimeException("cant change object class if objects are in hub");
			}
			if (hdm.getMasterHub() != null || hdm.getMasterObject() != null) {
				throw new RuntimeException("cant change object class if masterObject exists");
			}
			if (hdu.getSharedHub() != null || getHubShareService().getSharedWeakHubSize(thisHub) > 0) {
				throw new RuntimeException("cant change object class since this is a shared hub.");
			}
		}
		// 20141111 removed since the select could be valid
		// srvcHub.getHubSelectService().cancelSelect(thisHub, true);
		faBridge.getHubFriendAccess().getHubData(thisHub).setObjClass(objClass);

		/* 20141111 not needed here
		if (objClass != null) {
		    // find out if class is OAObject
			thisHub.data.setOAObjectFlag(OAObject.class.isAssignableFrom(objClass));
			// thisHub.data.setObjectInfo(srvcObject.getOAObjectInfoService().getOAObjectInfo(objClass));
		}
		else {
		    thisHub.data.setObjectInfo(null);
		    thisHub.data.setOAObjectFlag(false);
		}
		*/
	}

	/**
	 * Determines whether the hub is in a valid state. A hub is invalid if its
	 * master hub exists but has no active master object, or if any linked hub is
	 * invalid and cannot auto-create missing objects. If an addHub exists, its
	 * validity is also checked recursively.
	 *
	 * @param thisHub the hub being evaluated
	 * @return {@code true} if the hub is valid; otherwise {@code false}
	 */
	public boolean isValid(final Hub thisHub) {
		HubDataMaster dm = srvcHubDetail.getDataMaster(thisHub, true);
		if (dm.getMasterHub() != null && dm.getMasterObject() == null) {
			return false;
		}

		// 20181119 reworked to check other hubs for hubWithLink
		Hub h = srvcHubLink.getHubWithLink(thisHub, true);
		if (h != null) {
			Hub hx = faBridge.getHubFriendAccess().getHubDataUnique(h).getLinkToHub();
			if (hx != null) {
				if (!isValid(hx)) {
					return false;
				}
				
				if (faBridge.getHubFriendAccess().getHubDataActive(hx).getActiveObject() == null) {
					if (!faBridge.getHubFriendAccess().getHubDataUnique(h).isAutoCreate()) {
						return false;
					}
				}
			}
		}

		HubDataUnique hdu = faBridge.getHubFriendAccess().getHubDataUnique(thisHub);
		if (hdu.getAddHub() != null) {
			return isValid(hdu.getAddHub());
		}
		return true;
	}

	
	/**
	 * Evaluates the current synchronization state of the hub, optionally populating
	 * a replacement hub or list when a mismatch is detected. This is a wrapper that
	 * delegates to the internal recursive implementation.
	 *
	 * @param thisHub the hub being examined
	 * @param hubNew  optional hub to receive corrected state contents
	 * @param alNew   optional list to receive corrected state contents
	 * @return the hub’s synchronization status
	 */
    public <T> HubCurrentStateEnum getCurrentState(final Hub<T> thisHub, final Hub<T> hubNew, final ArrayList<T> alNew) {
        return _getCurrentState(thisHub, hubNew, alNew, new HashSet<Hub<T>>());
    }

    /**
     * Internal recursive implementation for evaluating hub synchronization state.
     * Prevents cyclic traversal using the provided hub set. Traverses master hubs,
     * shared hubs, mergers, combined hubs, and filters to determine whether the hub
     * is aligned with its correct source.
     *
     * @param thisHub the hub being evaluated
     * @param hubNew  optional hub for corrected content
     * @param alNew   optional list for corrected content
     * @param hmHub   set of hubs visited to prevent cycles
     * @return the computed synchronization status, or {@code null} when a cycle is
     *         detected
     */
    public <T> HubCurrentStateEnum _getCurrentState(final Hub<T> thisHub, final Hub<T> hubNew, final ArrayList<T> alNew, final Set<Hub<T>> hmHub) {
		if (thisHub == null) {
			return HubCurrentStateEnum.InSync;
		}
		if (hmHub.contains(thisHub)) {
            return null;
		}
		hmHub.add(thisHub);

		Hub hub = thisHub;
		Hub hubMaster;
		boolean bHasMaster = false;
		for (int i = 0;; i++, hub = hubMaster) {
			HubDataMaster dm = srvcHubDetail.getDataMaster(hub, true);

			hubMaster = dm.getMasterHub();
			if (hubMaster == null) {
				break; // check for hubMerger
			}
			bHasMaster = true;

			final Object objMaster = hubMaster.getAO();
			if (objMaster == dm.getMasterObject()) {
				if (objMaster == null && thisHub.getSize() > 0) {
					return HubCurrentStateEnum.DetailDisconnectedFromMaster;
				}
				continue;
			}

			if (i > 0) {
				return HubCurrentStateEnum.DetailDisconnectedFromMaster;
			}

			if (objMaster != null && (hubNew != null || alNew != null)) {
				// find correct hub
				OALinkInfo li = dm.getDetailToMasterLinkInfo();
				if (li != null) {
					Object value = li.getReverseLinkInfo().getValue(objMaster);
					if (value != null) {
						if (value instanceof Hub) {
							if (hubNew != null) {
								hubNew.setSharedHub((Hub<T>) value);
							}
							if (alNew != null) {
								for (T objNext : ((Hub<T>) value)) {
									alNew.add(objNext);
								}
							}
						} else {
							if (hubNew != null) {
								hubNew.add((T) value);
							}
							if (alNew != null) {
								alNew.add((T) value);
							}
						}
					}
				}
			}
			return HubCurrentStateEnum.DetailHubNotSameAsMasterObject;
		}

		// check to see if hub is derived from another Hub, and check it

		hub = getHubShareService().getMainSharedHub(hub);

		HubMerger hubMerger = null;
		HubCombined hubCombined = null;
		HubFilter hubFilter = null;

		HubListener[] hls = srvcHubEvent.getAllListeners(hub);

		if (hls != null) {
			for (HubListener hl : hls) {
				if (!(hl instanceof HubListenerAdapter)) {
					continue;
				}
				HubListenerAdapter hla = (HubListenerAdapter) hl;
				Object listener = hla.getListener();
				if (listener instanceof HubMerger) {
					hubMerger = (HubMerger) hla.getListener();
					Hub hubx = hubMerger.getCombinedHub();
					if (hubx == hub) {
						break;
					}
					hubMerger = null;
				} else if (listener instanceof HubCombined) {
					hubCombined = (HubCombined) hla.getListener();
					Hub hubx = hubCombined.getMasterHub();
					if (hubx == hub) {
						break;
					}
					hubCombined = null;
				} else if (listener instanceof HubFilter) {
					hubFilter = (HubFilter) hla.getListener();
					Hub hubx = hubFilter.getHub();
					if (hubx == hub) {
						break;
					}
					hubFilter = null;
				}

			}
		}

		if (hubFilter != null) {
			Hub hubx = hubFilter.getMasterHub();

			HubCurrentStateEnum hcs = _getCurrentState(hubx, null, null, hmHub);
			if (hcs == HubCurrentStateEnum.InSync) {
				return hcs;
			}
			if (hubNew == null && alNew == null) {
				return hcs;
			}

			Hub hubTemp = new Hub();
			_getCurrentState(hubx, hubTemp, null, hmHub);

			for (Object objx : hubTemp) {
				if (!hubFilter.isUsed(objx)) {
					continue;
				}
				if (hubNew != null) {
					hubNew.add((T) objx);
				}
				if (alNew != null) {
					alNew.add((T) objx);
				}
			}

		} else if (hubCombined != null) {
			ArrayList<Hub> al = hubCombined.getHubs();
			if (al != null) {
				HubCurrentStateEnum hcs = null;
				for (Hub hubx : al) {
					hcs = _getCurrentState(hubx, null, null, hmHub);
					if (hcs != HubCurrentStateEnum.InSync) {
						break;
					}
				}
				if (hcs == null) {
					return HubCurrentStateEnum.InSync;
				}
				if (hubNew == null && alNew == null) {
					return hcs;
				}

				for (Hub hubx : al) {
					hcs = _getCurrentState(hubx, hubNew, alNew, hmHub);
				}
				return hcs;
			}

		} else if (hubMerger != null) {
			Hub hubx = hubMerger.getRootHub();

			HubCurrentStateEnum hcs = _getCurrentState(hubx, null, null, hmHub);

			if (hcs == HubCurrentStateEnum.InSync) {
				if (!srvcThreadLocal.isHubMergerChanging() && !hubMerger.isLoadingCombinedHub()) {
					return hcs;
				}
			}

			if (hubNew == null && alNew == null) {
				return HubCurrentStateEnum.HubMergerNotUpdated;
			}

			Hub hubTemp = new Hub();

			_getCurrentState(hubx, hubTemp, null, hmHub);

			OAFinder finder = new OAFinder(hubMerger.getPath());

			ArrayList al;
			if (hubMerger.getUseAll()) {
				al = finder.find(hubTemp);
			} else {
				// ?? not sure that AO will be set
				al = finder.find((OAObject) hubTemp.getAO());
			}

			if (hubNew != null) {
				hubNew.add((List<T>) al);
			}
			if (alNew != null) {
				alNew.addAll((List<T>) al);
			}

			return HubCurrentStateEnum.HubMergerNotUpdated;
		}
		return HubCurrentStateEnum.InSync;
	}

    /**
     * Determines which hub controls this hub’s validity. If the hub has a master
     * hub, that master hub is returned. If a linked shared hub exists, its link
     * target or its controlling hub is returned. If an addHub is present, its
     * controlling hub is evaluated. Otherwise, this hub is returned.
     *
     * @param thisHub the hub whose controlling hub is requested
     * @return the controlling hub
     */
	public Hub getControllingHub(Hub thisHub) {
		HubDataMaster dm = srvcHubDetail.getDataMaster(thisHub, true);
		if (dm.getMasterHub() != null) {
			return dm.getMasterHub();
		}

		// 20181119 find shared hub with link
		Hub hubWithLink = srvcHubLink.getHubWithLink(thisHub, true);
		
		if (hubWithLink != null) {
			HubDataUnique hdu = faBridge.getHubFriendAccess().getHubDataUnique(hubWithLink);			
			if (hdu.getLinkToHub() != null) {
				if (hdu.isAutoCreate()) {
					return getControllingHub(hdu.getLinkToHub());
				}
				return hdu.getLinkToHub();
			}
		}
		HubDataUnique hdu = faBridge.getHubFriendAccess().getHubDataUnique(thisHub);			
		if (hdu.getAddHub() != null) {
			return getControllingHub(hdu.getAddHub());
		}
		return thisHub;
	}

	
	/**
	 * Returns this hub or any shared hub that has an addHub defined. Shared hubs
	 * are scanned using a filter to locate the first hub that supports additions.
	 *
	 * @param hub the hub to evaluate
	 * @return a hub with an addHub, or {@code null} if none exists
	 */
	public Hub getAnyAddHub(final Hub hub) {
		if (hub.getAddHub() != null) {
			return hub;
		}

		// 20120716
		OAFilter<Hub> filter = new OAFilter<Hub>() {
			@Override
			public boolean isUsed(Hub h) {
				return h.getAddHub() != null;
			}
		};
		Hub[] hubs = getHubShareService().getAllSharedHubs(hub, filter);

		// was: Hub[] hubs = getHubShareService().getAllSharedHubs(hub);
		for (int i = 0; i < hubs.length; i++) {
			if (hubs[i].getAddHub() != null) {
				return hubs[i];
			}
		}
		return null;
	}

	/**
	 * Updates link relationships for objects added to or removed from this hub.
	 * When objects are removed, the method determines whether the reverse link
	 * requires deletion, reference removal, or persistence based on the link type,
	 * master relationship, and cascade rules. Many-to-many links are updated when
	 * needed. New objects are skipped because they do not yet exist in the data
	 * source.
	 *
	 * @param thisHub       the hub whose add/remove state is being processed
	 * @param iCascadeRule  the cascade rule for save/delete operations
	 * @param cascade       the cascade tracker for preventing reprocessing
	 * @param bIsSaving     whether the caller is performing a save operation
	 */
	public void _updateHubAddsAndRemoves(final Hub thisHub, final int iCascadeRule, final OACascade cascade,
			final boolean bIsSaving) {
		//qqqqqqqq method was protected
		// removed Objects need to be saved if reference = null.
		HubDataMaster dm = srvcHubDetail.getDataMaster(thisHub);
		
		boolean bM2M = (dm != null && dm.getDetailToMasterLinkInfo() != null && dm.getDetailToMasterLinkInfo().getType() == OALinkInfo.MANY);
		OALinkInfo liRev = null;
		if (dm != null && dm.getDetailToMasterLinkInfo() != null) {
			liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(dm.getDetailToMasterLinkInfo());
		}

		boolean bHasMethod = true;
		if (dm == null) {
		} else if (bM2M) {
			bHasMethod = false;
			if (dm.getMasterObject() != null && dm.getDetailToMasterLinkInfo() != null) {
				updateMany2ManyLinks(thisHub, dm); // update any link tables
			}
		} else {
			// 20120907 cases where there is not a public method created, and would use a link table.
			Method method = srvcObject.getOAObjectInfoService().getMethod(dm.getDetailToMasterLinkInfo());
			if (method == null || ((method.getModifiers() & (Modifier.PRIVATE)) != 0)) {
				bHasMethod = false;
				updateMany2ManyLinks(thisHub, dm); // update any link tables
			}
		}

		Object[] objs = srvcHubData.getRemovedObjects(thisHub);
		if (objs == null) {
			return;
		}

		for (int i = 0; i < objs.length; i++) {
			OAObject obj = (OAObject) objs[i];
			if (obj.getNew()) {
				continue; // does not exist in DS
			}
			if (liRev != null && liRev.isOwner()) {
				if (dm.getDetailToMasterLinkInfo() != null) {
					Object ox = srvcObject.getOAObjectReflectService().getProperty(obj, dm.getDetailToMasterLinkInfo().getName());
					if (ox == null) {
						srvcObject.getOAObjectDeleteService().delete(obj, cascade);
					}
				}
			} else if (dm != null && dm.getDetailToMasterLinkInfo() != null && bHasMethod) {
				Object ox = srvcObject.getOAObjectReflectService().getProperty(obj, dm.getDetailToMasterLinkInfo().getName());
				if (ox == null) { // else property has been reassigned
					// 20120925
					srvcObject.getOAObjectDSService().removeReference(obj, dm.getDetailToMasterLinkInfo());
					//was: OAObjectSaveDelegate._saveObjectOnly(obj, cascade);
				}
			} else if (bIsSaving && dm != null && dm.getDetailToMasterLinkInfo() != null && !bHasMethod && srvcSync.isServer() && !obj.isDeleted()) {
				// 20181126 if it is a removed object from ServerRoot, need to save now
				srvcObject.getOAObjectSaveService().save(obj, iCascadeRule, cascade);
			}
		}
	}

	/**
	 * Synchronizes many-to-many link table entries for this hub. Added and removed
	 * objects are examined and cross-updated on the opposite hub. When changes
	 * occur, the link table is updated using the master object's reverse link
	 * property.
	 *
	 * @param thisHub the hub whose many-to-many links are being updated
	 * @param dm      the master relationship information for this hub
	 */
	private void updateMany2ManyLinks(Hub thisHub, HubDataMaster dm) {
		if (dm == null || dm.getDetailToMasterLinkInfo() == null) {
			return;
		}
		OAObject[] adds = getHubAddRemoveService().getAddedObjects(thisHub);
		OAObject[] removes = getHubAddRemoveService().getRemovedObjects(thisHub);

		boolean b = false;
		// cross update opposite hub vecAdd/Remove
		for (int i = 0; adds != null && i < adds.length; i++) {
			b = true;
			if (adds[i] == null) continue;
			OAObject obj = adds[i];
			if (obj.getNew()) continue;
			Object objx = srvcObject.getOAObjectReflectService().getRawReference(obj, dm.getDetailToMasterLinkInfo().getName());
			if (objx instanceof Hub) {
				srvcHubData.removeFromAddedList((Hub) objx, dm.getMasterObject());
			}
		}
		for (int i = 0; removes != null && i < removes.length; i++) {
			b = true;
			if (removes[i] == null) continue;
			OAObject obj = (OAObject) removes[i];
			Object objx = srvcObject.getOAObjectReflectService().getRawReference(obj, dm.getDetailToMasterLinkInfo().getName());
			if (objx instanceof Hub) {
				srvcHubData.removeFromRemovedList((Hub) objx, dm.getMasterObject());
			}
		}
		if (b) {
			String propFromMaster = srvcObject.getOAObjectInfoService().getReverseLinkInfo(dm.getDetailToMasterLinkInfo()).getName();
			srvcHubDS.updateMany2ManyLinks(dm.getMasterObject(), adds, removes, propFromMaster);
		}
	}

	/**
	 * Configures the hub to enforce uniqueness based on the specified property.
	 * Validates that the property is not nested, that a corresponding getter
	 * method exists, and that the getter accepts no parameters. When {@code null}
	 * is supplied, the unique property is cleared.
	 *
	 * @param thisHub      the hub whose unique property is being set
	 * @param propertyName the name of the property used for uniqueness, or
	 *                     {@code null} to clear
	 * @throws IllegalArgumentException if the property is nested, lacks a getter,
	 *                                  or the getter requires parameters
	 */
	public void setUniqueProperty(Hub thisHub, String propertyName) {
		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		
		if (propertyName == null) {
			hd.setUniqueProperty(null);
			hd.setUniquePropertyGetMethod(null);
			return;
		}
		if (propertyName.indexOf('.') >= 0) {
			throw new IllegalArgumentException(
					"Property " + propertyName + " can only be for a property in " + thisHub.getObjectClass().getName());
		}

		hd.setUniquePropertyGetMethod(srvcObject.getOAObjectInfoService().getMethod(thisHub.getObjectClass(), "get" + propertyName));
		if (hd.getUniquePropertyGetMethod() == null) {
			throw new IllegalArgumentException("Get Method for Property " + propertyName + " not found");
		}
		if (hd.getUniquePropertyGetMethod().getParameterTypes().length > 0) {
			throw new IllegalArgumentException("Get Method for Property " + propertyName + " expects parameters");
		}
		hd.setUniqueProperty(propertyName);
	}

	/**
	 * Enables automatic sequencing of objects in this hub by assigning sequential
	 * values to the specified property. Existing auto-sequence handlers are closed
	 * before creating a new one. Sorting is canceled to preserve sequence order.
	 * When the hub is a detail hub, sequencing is only enabled on the server side.
	 *
	 * @param thisHub     the hub whose objects will receive sequence values
	 * @param property    the property to update with sequence numbers
	 * @param startNumber the initial sequence number
	 * @param bKeepSeq    whether sequence values are preserved after removals
	 */
	public void setAutoSequence(Hub thisHub, String property, int startNumber, boolean bKeepSeq) {
		// 20091030 only set for server for detail hubs
		boolean bServerOnly = false;
		if (thisHub.getMasterObject() != null) {
			if (!getHubCSService().isServer(thisHub)) {
				return; // only set up for server
			}
			bServerOnly = true;
		}
		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		if (hd.getAutoSequence() != null) {
			hd.getAutoSequence().close();
		}
		thisHub.cancelSort(); // 20090801 need to remove any sorters
		hd.setAutoSequence(new HubAutoSequence(thisHub, property, startNumber, bKeepSeq, bServerOnly));
	}

	/**
	 * Returns the auto-sequence controller for this hub, or {@code null} if none is
	 * assigned.
	 *
	 * @param thisHub the hub whose auto-sequence handler is requested
	 * @return the auto-sequence object, or {@code null} if not configured
	 */
	public HubAutoSequence getAutoSequence(Hub thisHub) {
		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		return hd.getAutoSequence();
	}

	/**
	 * Recomputes sequence values for all objects in this hub when auto-sequence is
	 * enabled. If no auto-sequence handler exists, no action is taken.
	 *
	 * @param thisHub the hub whose sequence values will be recalculated
	 */
	public void resequence(Hub thisHub) {
		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		if (hd.getAutoSequence() != null) {
			hd.getAutoSequence().resequence();
		}
	}

	/**
	 * Ensures that for every object in the master hub, there is a corresponding
	 * object in this hub whose specified property points to that master object.
	 * Existing auto-match handlers are closed before creating a new one. The match
	 * logic supports server-side restriction.
	 *
	 * @param thisHub         the hub being synchronized
	 * @param property        the property on this hub's objects used to match
	 * @param hubMaster       the hub whose objects must be mirrored
	 * @param bServerSideOnly whether matching should only be enforced on the server
	 */
	public void setAutoMatch(Hub thisHub, String property, Hub hubMaster, boolean bServerSideOnly) {
		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		if (hd.getAutoMatch() != null) {
			hd.getAutoMatch().close();
		}
		// 20220802 now works with Enum (name/value) property
		// if (hubMaster != null) {
		HubAutoMatch am = new HubAutoMatch();
		hd.setAutoMatch(am);
		am.setServerSideOnly(bServerSideOnly);
		am.init(thisHub, property, hubMaster, null, null);
		// }
	}

	/**
	 * Variant of auto-match initialization that includes a stopping condition. For
	 * each object in the master hub, this hub ensures a corresponding object exists
	 * unless the match path encounters the specified stop object and property.
	 *
	 * @param thisHub         the hub being synchronized
	 * @param property        the property used to link to master hub objects
	 * @param hubMaster       the hub being mirrored
	 * @param bServerSideOnly whether matching is server-only
	 * @param objStop         optional object used to limit matching
	 * @param stopProperty    the property that defines the stopping condition
	 */
	public void setAutoMatch(Hub thisHub, String property, Hub hubMaster, boolean bServerSideOnly, OAObject objStop, String stopProperty) {
		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		if (hd.getAutoMatch() != null) {
			hd.getAutoMatch().close();
		}
		// 20220802 now works with Enum (name/value) property
		// if (hubMaster != null) {
		HubAutoMatch am = new HubAutoMatch();
		hd.setAutoMatch(am);
		am.setServerSideOnly(bServerSideOnly);
		am.init(thisHub, property, hubMaster, objStop, stopProperty);
		// }
	}

	/**
	 * Returns the auto-match controller for this hub, or {@code null} if no
	 * auto-match logic is configured.
	 *
	 * @param thisHub the hub whose auto-match handler is requested
	 * @return the auto-match object, or {@code null} if none exists
	 */
	public HubAutoMatch getAutoMatch(Hub thisHub) {
		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		return hd.getAutoMatch();
	}

	/**
	 * Returns the logical size of this hub. If the hub is backed by a select with
	 * more data available, counting and fetch operations are used to determine the
	 * full size. If no select applies, the in-memory object count is returned.
	 *
	 * @param thisHub the hub whose size is requested
	 * @return the number of objects the hub represents
	 */
	public int getSize(Hub thisHub) {
		if (getHubSelectService().isMoreData(thisHub)) {
			if (!getHubSelectService().isCounted(thisHub)) {
				if (srvcHubData.getCurrentSize(thisHub) == 0) {
					getHubSelectService().fetchMore(thisHub); // see if this will load it, before calling count on the select
					if (!getHubSelectService().isMoreData(thisHub)) {
						return getSize(thisHub);
					}
				}
			}
			int x = getHubSelectService().getCount(thisHub);
			if (x > 0) {
				return x;
			}
		}
		return srvcHubData.getCurrentSize(thisHub);
	}

	/**
	 * Ensures that all data is loaded into the hub and then returns its size. A
	 * {@code null} hub returns zero.
	 *
	 * @param thisHub the hub whose fully loaded size is requested
	 * @return the loaded size of the hub
	 */
	public int getLoadedSize(Hub thisHub) {
		if (thisHub == null) {
			return 0;
		}
		thisHub.loadAllData();
		return getSize(thisHub);
	}

	private int cntLoadedSizeError;

	/**
	 * Stores a named property value on the hub. Property names are normalized to
	 * uppercase. A {@link OANullObject} marker is stored when the value is
	 * {@code null}. A new property map is created on demand.
	 *
	 * @param thisHub the hub whose property map is updated
	 * @param name    the property name
	 * @param obj     the value to store, or {@code null}
	 */
	public void setProperty(Hub thisHub, String name, Object obj) {
		if (name == null) {
			return;
		}
		name = name.toUpperCase();
		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		if (hd.getHashProperty() == null) {
			hd.setHashProperty(new Hashtable(7));
		}
		hd.getHashProperty().put(name, (obj == null) ? OANullObject.instance : obj);
	}

	/**
	 * Retrieves a named property value previously stored on the hub. Property names
	 * are normalized to uppercase. A stored {@link OANullObject} resolves to
	 * {@code null}. If no property map exists, {@code null} is returned.
	 *
	 * @param thisHub the hub whose property is requested
	 * @param name    the property name
	 * @return the stored value, or {@code null} if not found
	 */
	public Object getProperty(Hub thisHub, String name) {
		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		if (hd.getHashProperty() == null) {
			return null;
		}

		name = name.toUpperCase();
		Object obj = hd.getHashProperty().get(name);
		if (obj instanceof OANullObject) {
			obj = null;
		}
		return obj;
	}

	/**
	 * Removes a property from the hub’s property map. Property names are converted
	 * to uppercase. If no property map exists, no action is taken.
	 *
	 * @param thisHub the hub whose property should be removed
	 * @param name    the name of the property to remove
	 */
	public void removeProperty(Hub thisHub, String name) {
		final HubData hd = faBridge.getHubFriendAccess().getHubData(thisHub);
		if (hd.getHashProperty() != null) {
			name = name.toUpperCase();
			hd.getHashProperty().remove(name);
		}
	}

	/**
	 * Updates referenceability settings for this hub and its parent objects. If the
	 * hub is server-side and the object class supports weak referencing, this method
	 * adjusts weak-reference behavior based on whether references should be
	 * maintained. When enabling referenceability, parent objects are also updated.
	 *
	 * @param hub            the hub whose referenceability is being updated
	 * @param bReferenceable whether objects referenced by this hub should remain
	 *                       strongly referenceable
	 */
	public void setReferenceable(Hub hub, boolean bReferenceable) {
		if (hub == null) {
			return;
		}
		if (!srvcSync.isServer()) {
			return;
		}

		OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(hub.getObjectClass());
		if (!srvcObject.getOAObjectInfoService().isWeakReferenceable(oi)) {
			return;
		}
		boolean bSupportStorage = oi.getSupportsStorage();

		Object master = getMasterObject(hub);
		if (master == null) return;

		OALinkInfo li = srvcHubDetail.getLinkInfoFromDetailToMaster(hub);
		if (li == null) {
			return;
		}
		OALinkInfo liRev = li.getReverseLinkInfo();
		if (liRev == null) {
			return;
		}

		if (liRev.getCacheSize() > 0) {
			if (bReferenceable || bSupportStorage) {
				boolean b = srvcObject.getOAObjectPropertyService().setPropertyWeakRef((OAObject) master, liRev.getName(), !bReferenceable, hub);
				if (!b) {
					return; // already done, dont need to check/change parents
				}
			}
		}

		if (bReferenceable) {
			// make parents referenceable
			srvcObject.getOAObjectPropertyService().setReferenceable((OAObject) master, bReferenceable);
		}
	}


	
	public void save(Hub hub, int iCascadeRule, OACascade cascade) {
		srvcHubSave.saveAll(hub, iCascadeRule, cascade);
	}
	
}
