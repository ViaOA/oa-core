package com.viaoa.oa.service.hub;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Vector;
import java.util.logging.Logger;

import com.viaoa.callback.OAObjectCallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDataActive;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubInternalBridge;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.detail.HubDetail;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.oa.service.OASyncService;
import com.viaoa.oa.service.object.OAObjectParentService;
import com.viaoa.oa.sibling.OASiblingHelper;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.path.OAPathDelegate;
import com.viaoa.runtime.OARemoteThreadService;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.thread.OARemoteThread;
import com.viaoa.serialize.OAObjectSerializer;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;


/*qqqqqqqqqqqqqqqqqqq
CODEX

 #1 — invariant risk
  File/class/method: src/main/java/com/viaoa/oa/service/hub/HubParentService.java:76, initialize(...)
  Exact concern: HubParentService.initialize has no “already initialized” guard, while
  OAObjectParentService.initialize does.
  Why it matters: a second initialize call can replace parent service references after child services already exist.
  The anonymous child hooks close over HubParentService.this, so later calls may use a new srvcObject, srvcSync, or
  thread service unexpectedly.
  Minimal fix: add the same one-shot initialization guard used by OAObjectParentService.
  Suggested invariant: GRAPH_PARENT_SERVICES_INITIALIZE_ONCE
  Suggested test coverage: call HubService.initialize twice and verify the second call fails.

#2 — boundary risk
  File/class/method: src/main/java/com/viaoa/oa/service/hub/HubParentService.java:113, public child getters; src/
  main/java/com/viaoa/oa/service/object/OAObjectParentService.java:156, public child getters
  Exact concern: parent services expose concrete child services through public inherited getters.
  Why it matters: callers with HubService or OAObjectService can bypass the parent orchestration API and call deep
  child services directly. That weakens the “parent owns child services” boundary.
  Minimal fix: make child getters protected/package-private where possible, or explicitly mark them unsupported
  internal wiring.
  Suggested invariant: GRAPH_CHILD_SERVICES_ARE_NOT_PUBLIC_APP_SURFACE
  Suggested test coverage: architecture test that non-OA-runtime packages do not call child-service getters.

 #3 — invariant risk
  File/class/method: src/main/java/com/viaoa/oa/service/hub/HubParentService.java:113, child-service lazy
  getters; src/main/java/com/viaoa/oa/service/object/OAObjectParentService.java:156, child-service lazy getters
  Exact concern: child service creation is lazy, mutable, and unsynchronized.
  Why it matters: initialization eagerly creates the services, but public getters can still be called before/during
  initialization or concurrently on directly constructed services. That can create duplicate child instances or
  child services with incomplete parent dependencies.
  Minimal fix: enforce parent initialization before child access and/or synchronize child creation.
  Suggested invariant: GRAPH_CHILD_SERVICE_CREATION_IS_SINGLE_AND_AFTER_PARENT_INIT
  Suggested test coverage: concurrent getter access around initialization creates exactly one child service and
  never observes null parent dependencies.

#3 — boundary risk
  file/class/method: src/main/java/com/viaoa/oa/service/hub/HubParentService.java:540, HubCS remote hooks
  exact concern: HubCS parent hooks directly call srvcSync.getRemoteSync() / getRemoteClient() without null guards
  across add/remove/insert/move/sort/refresh/clear-changes/delete-all.
  why it matters: Child Hub services already decide whether sync should happen, but the parent boundary should still
  preserve runtime invariants. A stale role branch becomes an NPE instead of a no-op/local path.
  severity: invariant risk
  minimal fix: Parent hooks should return false/no-op if the required sync endpoint is absent, or HubCS should
  expose explicit client/server-only methods.
  suggested invariant ID/name: HUB-CS-HOOK-001: remote sync hooks are null-safe
  suggested test coverage: Each HubCS operation in single-user, server-without-remoteSync, and client-without-
  remoteClient setup.


*/

/**
 *
 * qqqqqqqqqqqq Parent that manages all subservices
 *
 */
public class HubParentService {
	private final Logger LOG = Logger.getLogger(HubParentService.class.getName());

	private final HubInternalBridge faBridge = new HubInternalBridge();
	private final Hub.FriendAccess faHub;
	
	private OAObjectParentService srvcObjectParent;
	private OASyncService srvcSync;
	private OAThreadLocalService srvcThreadLocal;
	private OARemoteThreadService srvcRemoteThread;

	private HubAddRemoveService srvcHubAddRemove;
	private HubAOService srvcHubAO;
	private HubAutoMatchService srvcHubAutoMatch;
	private HubCSService srvcHubCS;
	private HubDataService srvcHubData;
	private HubDeleteService srvcHubDelete;
	private HubDetailService srvcHubDetail;
	private HubDSService srvcHubDS;
	private HubEventService srvcHubEvent;
	private HubFindService srvcHubFind;
	private HubLinkService srvcHubLink;
	private HubMasterService srvcHubMaster;
	private HubPropertyService srvcHubProperty;
	private HubRootService srvcHubRoot;
	private HubSaveService srvcHubSave;
	private HubSelectService srvcHubSelect;
	private HubSequenceService srvcHubSequence;
	private HubSerializeService srvcHubSerialize;
	private HubShareService srvcHubShare;
	private HubSizeService srvcHubSize;
	private HubSortService srvcHubSort;
	private HubStatusService srvcHubStatus;
	
	public HubParentService() {
		this.faHub = faBridge.getHubFriendAccess();
	}


	/**
	 * Performs initialize behavior for the Hub service.
	 *
	 * @param srvcObjectParent method input
	 * @param srvcSync method input
	 * @param srvcThreadLocal method input
	 * @param srvcRemoteThread method input
	 */


	public void initialize(OAObjectParentService srvcObjectParent, OASyncService srvcSync, OAThreadLocalService srvcThreadLocal, OARemoteThreadService srvcRemoteThread) {
    	if (srvcObjectParent == null) throw new IllegalArgumentException("OAObjectParentService can not be null");
    	if (srvcSync == null) throw new IllegalArgumentException("OASyncService can not be null");
    	if (srvcThreadLocal == null) throw new IllegalArgumentException("OAThreadLocalService can not be null");
    	if (srvcRemoteThread == null) throw new IllegalArgumentException("OARemoteThreadService can not be null");
    	
    	this.srvcObjectParent = srvcObjectParent; 
		this.srvcSync = srvcSync;
		this.srvcThreadLocal = srvcThreadLocal;
		this.srvcRemoteThread = srvcRemoteThread;
		
		// create all Hub sub-Services
		getHubAddRemoveService();
		getHubAOService();
		getHubAutoMatchService();
		getHubCSService();
		getHubDataService();
		getHubDeleteService();
		getHubDetailService();
		getHubDSService();
		getHubEventService();
		getHubFindService();
		getHubLinkService();
		getHubMasterService();
		getHubPropertyService();
		getHubRootService();
		getHubSaveService();
		getHubSelectService();
		getHubSequenceService();
		getHubSerializeService();
		getHubShareService();
		getHubSizeService();
		getHubSortService();
		getHubStatusService();
	}


	/**
	 * Returns the hubAddRemoveService for the supplied Hub context.
	 *
	 * @return result value
	 */


	public HubAddRemoveService getHubAddRemoveService() {
		if (srvcHubAddRemove != null) return srvcHubAddRemove;
		
		srvcHubAddRemove = new HubAddRemoveService(faHub) {
			@Override
			public void callThreadLocalLock(Object object) {
				HubParentService.this.srvcThreadLocal.lock(object);
			}
			@Override
			public void callThreadLocalUnlock(Object object) {
				HubParentService.this.srvcThreadLocal.unlock(object);
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
				HubParentService.this.srvcObjectParent.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);				
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public OALinkInfo callObjectInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getRecursiveLinkInfo(thisOI, type);
			}
			@Override
			public <T extends OAObject> void callObjectHubRemoveHub(T oaObj, Hub<T> hub, boolean bIsOnHubFinalize) {
				HubParentService.this.srvcObjectParent.getOAObjectHubService().removeHub(oaObj, hub, bIsOnHubFinalize);				
			}
			@Override
			public <T extends OAObject> boolean callObjectHubAddHub(T oaObj, Hub<T> hub) {
				return HubParentService.this.srvcObjectParent.getOAObjectHubService().addHub(oaObj, hub);
			}
			@Override
			public OAObjectCallback callObjectCallbackGetVerifyRemoveAllObjectCallback(Hub<?> hub, OAObjectCallback.CheckType[] onlyCheckTypes) {
				return HubParentService.this.srvcObjectParent.getOAObjectRulesService().getVerifyRemoveAllObjectCallback(hub, onlyCheckTypes);
			}
			@Override
			public <T extends OAObject> OAObjectCallback callObjectCallbackGetVerifyAddObjectCallback(Hub<T> hub, T oaObj, OAObjectCallback.CheckType[] onlyCheckTypes) {
				return HubParentService.this.srvcObjectParent.getOAObjectRulesService().getVerifyAddObjectCallback(hub, oaObj, onlyCheckTypes);
			}
			@Override
			public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowRemoveObjectCallback(Hub<T> hub, T objRemove, OAObjectCallback.CheckType[] onlyCheckTypes) {
				return HubParentService.this.srvcObjectParent.getOAObjectRulesService().getAllowRemoveObjectCallback(hub, objRemove, onlyCheckTypes);
			}
			@Override
			public <T extends OAObject> OAObjectCallback callObjectCallbackGetVerifyRemoveObjectCallback(Hub<T> hub, T objRemove, OAObjectCallback.CheckType[] onlyCheckTypes) {
				return HubParentService.this.srvcObjectParent.getOAObjectRulesService().getVerifyRemoveObjectCallback(hub, objRemove, onlyCheckTypes);
			}
			@Override
			public OAObjectCallback callObjectCallbackGetAllowRemoveAllObjectCallback(Hub<?> hub, OAObjectCallback.CheckType[] onlyCheckTypes) {
				return HubParentService.this.srvcObjectParent.getOAObjectRulesService().getAllowRemoveAllObjectCallback(hub, onlyCheckTypes);
			}
			@Override
			public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowAddObjectCallback(Hub<T> hub, T objAdd, OAObjectCallback.CheckType[] onlyCheckTypes) {
				return HubParentService.this.srvcObjectParent.getOAObjectRulesService().getAllowAddObjectCallback(hub, objAdd, onlyCheckTypes);
			}
			@Override
			public <T extends OAObject> boolean callHubVerifyUniqueProperty(Hub<T> thisHub, T object) {
				return HubParentService.this.getHubPropertyService().verifyUniqueProperty(thisHub, object);
			}
			@Override
			public void callHubShareSetSharedHubsAfterRemoveAll(Hub<?> thisHub) {
				HubParentService.this.getHubShareService().setSharedHubsAfterRemoveAll(thisHub);
			}
			@Override
			public <T extends OAObject> void callHubShareSetSharedHubsAfterRemove(Hub<T> thisHub, T objRemoved, int posRemoved) {
				HubParentService.this.getHubShareService().setSharedHubsAfterRemove(thisHub, objRemoved, posRemoved);				
			}
			@Override
			public void callHubStatusSetReferenceable(Hub<?> hub, boolean bReferenceable) {
				HubParentService.this.getHubStatusService().setReferenceable(hub, bReferenceable);				
			}
			@Override
			public <T extends OAObject> void callHubDataSetObjectClass(Hub<T> thisHub, Class<T> objClass) {
				HubParentService.this.getHubDataService().setObjectClass(thisHub, objClass);				
			}
			@Override
			public String callHubSelectGetSelectWhereHubPath(Hub<?> thisHub) {
				return HubParentService.this.getHubSelectService().getSelectWhereHubPath(thisHub);
			}
			@Override
			public <T extends OAObject> Hub<T> callHubSelectGetSelectWhereHub(Hub<T> thisHub) {
				return HubParentService.this.getHubSelectService().getSelectWhereHub(thisHub);
			}
			@Override
			public void callHubSelectCancelSelect(Hub<?> thisHub, boolean bRemoveSelect) {
				HubParentService.this.getHubSelectService().cancelSelect(thisHub, bRemoveSelect);				
			}
			@Override
			public <T extends OAObject> T callHubFindGetRealObject(Hub<T> hub, Object object) {
				return HubParentService.this.getHubFindService().getRealObject(hub, object);
			}
			@Override
			public void callHubEventFireOnNewListEvent(Hub<?> thisHub, boolean bAll) {
				HubParentService.this.getHubEventService().fireOnNewListEvent(thisHub, bAll);				
			}
			@Override
			public <T extends OAObject> void callHubEventFireBeforeRemoveEvent(Hub<T> thisHub, T obj, int pos) {
				HubParentService.this.getHubEventService().fireBeforeRemoveEvent(thisHub, obj, pos);				
			}
			@Override
			public void callHubEventFireBeforeRemoveAllEvent(Hub<?> thisHub) {
				HubParentService.this.getHubEventService().fireBeforeRemoveAllEvent(thisHub);				
			}
			@Override
			public void callHubEventFireBeforeMoveEvent(Hub<?> thisHub, int fromPos, int toPos) {
				HubParentService.this.getHubEventService().fireBeforeMoveEvent(thisHub, fromPos, toPos);				
			}
			@Override
			public <T extends OAObject> void callHubEventFireBeforeInsertEvent(Hub<T> thisHub, T obj, int pos) {
				HubParentService.this.getHubEventService().fireBeforeInsertEvent(thisHub, obj, pos);				
			}
			@Override
			public <T extends OAObject> void callHubEventFireBeforeAddEvent(Hub<T> thisHub, T obj, int pos) {
				HubParentService.this.getHubEventService().fireBeforeAddEvent(thisHub, obj, pos);				
			}
			@Override
			public <T extends OAObject> void callHubEventFireAfterRemoveEvent(Hub<T> thisHub, T obj, int pos) {
				HubParentService.this.getHubEventService().fireAfterRemoveEvent(thisHub, obj, pos);				
			}
			@Override
			public void callHubEventFireAfterRemoveAllEvent(Hub<?> thisHub) {
				HubParentService.this.getHubEventService().fireAfterRemoveAllEvent(thisHub);				
			}
			@Override
			public void callHubEventFireAfterMoveEvent(Hub<?> thisHub, int fromPos, int toPos) {
				HubParentService.this.getHubEventService().fireAfterMoveEvent(thisHub, fromPos, toPos);				
			}
			@Override
			public <T extends OAObject> void callHubEventFireAfterInsertEvent(Hub<T> thisHub, T obj, int pos) {
				HubParentService.this.getHubEventService().fireAfterInsertEvent(thisHub, obj, pos);				
			}
			@Override
			public <T extends OAObject>  void callHubEventFireAfterAddEvent(Hub<T> thisHub, T obj, int pos) {
				HubParentService.this.getHubEventService().fireAfterAddEvent(thisHub, obj, pos);				
			}
			@Override
			public <T extends OAObject> void callHubDetailSetPropertyToMasterHub(Hub<T> thisHub, T detailObject, OAObject objMaster) {
				HubParentService.this.getHubDetailService().setPropertyToMasterHub(thisHub, detailObject, objMaster);				
			}
			@Override
			public boolean callHubDetailIsRecursiveMasterDetail(Hub<?> thisHub) {
				return HubParentService.this.getHubDetailService().isRecursiveMasterDetail(thisHub);
			}
			@Override
			public OAObject callHubDetailGetMasterObject(Hub<?> thisHub) {
				return HubParentService.this.getHubDetailService().getMasterObject(thisHub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public HubDataMaster callHubDetailGetDataMaster(Hub<?> thisHub, boolean bIncludedFilteredHub) {
				return HubParentService.this.getHubDetailService().getDataMaster(thisHub, bIncludedFilteredHub);
			}
			@Override
			public <T extends OAObject> int callHubData_remove(Hub<T> thisHub, T obj, boolean bDeleting, boolean bIsRemovingAll) {
				return HubParentService.this.getHubDataService()._remove(thisHub, obj, bDeleting, bIsRemovingAll);
			}
			@Override
			public <T extends OAObject> void callHubData_move(Hub<T> thisHub, T obj, int posFrom, int posTo) {
				HubParentService.this.getHubDataService()._move(thisHub, obj, posFrom, posTo);				
			}
			@Override
			public <T extends OAObject> boolean callHubData_insert(Hub<T> thisHub, T obj, int pos, boolean bIsLocked) {
				return HubParentService.this.getHubDataService()._insert(thisHub, obj, pos, bIsLocked);
			}
			@Override
			public <T extends OAObject> boolean callHubData_add(Hub<T> thisHub, T obj, boolean bHasLock, boolean bCheckContains) {
				return HubParentService.this.getHubDataService()._add(thisHub, obj, bHasLock, bCheckContains);
			}
			@Override
			public void callHubStatusSetChanged(Hub<?> thisHub, boolean bChanged) {
				HubParentService.this.getHubStatusService().setChanged(thisHub, bChanged);				
			}
			@Override
			public <T extends OAObject> int callHubDataGetPos(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink) {
				return HubParentService.this.getHubDataService().getPos(thisHub, object, adjustMaster, bUpdateLink);
			}
			@Override
			public <T extends OAObject> T[] callHubDataGetRemovedObjects(Hub<T> thisHub) {
				return HubParentService.this.getHubDataService().getRemovedObjects(thisHub);
			}
			@Override
			public <T extends OAObject> T callHubDataGetObjectAt(Hub<T> thisHub, int pos) {
				return HubParentService.this.getHubDataService().getObjectAt(thisHub, pos);
			}
			@Override
			public <T extends OAObject> T[] callHubDataGetAddedObjects(Hub<T> thisHub) {
				return HubParentService.this.getHubDataService().getAddedObjects(thisHub);
			}
			@Override
			public <T extends OAObject> Vector<T> callHubDataCreateVecRemove(Hub<T> thisHub) {
				return HubParentService.this.getHubDataService().createVecRemove(thisHub);
			}
			@Override
			public <T extends OAObject> boolean callHubDataContains(Hub<T> hub, T obj, boolean bJustAdded) {
				return HubParentService.this.getHubDataService().contains(hub, obj, bJustAdded);
			}
			@Override
			public <T extends OAObject> void callHubCSRemoveFromHub(Hub<T> thisHub, T obj, int pos) {
				HubParentService.this.getHubCSService().removeFromHub(thisHub, obj, pos);				
			}
			@Override
			public void callHubCSRemoveAllFromHub(Hub<?> thisHub) {
				HubParentService.this.getHubCSService().removeAllFromHub(thisHub);				
			}
			@Override
			public void callHubCSMoveObjectInHub(Hub<?> thisHub, int posFrom, int posTo) {
				HubParentService.this.getHubCSService().moveObjectInHub(thisHub, posFrom, posTo);				
			}
			@Override
			public <T extends OAObject> boolean callHubCSInsertInHub(Hub<T> thisHub, T obj, int pos) {
				return HubParentService.this.getHubCSService().insertInHub(thisHub, obj, pos);
			}
			@Override
			public <T extends OAObject> void callHubCSAddToHub(Hub<T> thisHub, T thisObj) {
				HubParentService.this.getHubCSService().addToHub(thisHub, thisObj);
			}
			@Override
			public void callRemoteThreadSetStartedNextThread(boolean b) {
				Thread t = Thread.currentThread();
				if (t instanceof OARemoteThread) {
					OARemoteThread rt = (OARemoteThread) t;
					rt.setStartedNextThread(b);
				}
			}
			@Override
			public HubDataMaster callHubDetailGetDataMaster(Hub<?> thisHub) {
				return HubParentService.this.getHubDetailService().getDataMaster(thisHub);
			}
			@Override
			public Method callObjectInfoGetMethod(OALinkInfo li) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getMethod(li);
			}
			@Override
			public Method callObjectInfoGetMethod(Class<?> clazz, String methodName) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getMethod(clazz, methodName);
			}
			@Override
			public Object callObjectReflectGetProperty(OAObject oaObj, String propPath) {
				return HubParentService.this.srvcObjectParent.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public void callObjectDSRemoveReference(OAObject oaObj, OALinkInfo li) {
				HubParentService.this.srvcObjectParent.getOAObjectDSService().removeReference(oaObj, li);
			}
			@Override
			public void callObjectDeleteDelete(OAObject oaObj, OACascade cascade) {
				HubParentService.this.srvcObjectParent.getOAObjectDeleteService().delete(oaObj, cascade);
			}
			@Override
			public void callObjectSaveSave(OAObject oaObj, int iCascadeRule, OACascade cascade) {
				HubParentService.this.srvcObjectParent.getOAObjectSaveService().save(oaObj, iCascadeRule, cascade);
			}
			@Override
			public boolean callSyncIsClient() {
				return HubParentService.this.srvcSync.isClient();
			}
			@Override
			public <T extends OAObject> T[] callHubAddRemoveGetAddedObjects(Hub<T> thisHub) {
				return HubParentService.this.getHubAddRemoveService().getAddedObjects(thisHub);
			}
			@Override
			public <T extends OAObject> T[] callHubAddRemoveGetRemovedObjects(Hub<T> thisHub) {
				return HubParentService.this.getHubAddRemoveService().getRemovedObjects(thisHub);
			}
			@Override
			public Object callObjectReflectGetRawReference(OAObject oaObj, String name) {
				return HubParentService.this.srvcObjectParent.getOAObjectReflectService().getRawReference(oaObj, name);
			}
			@Override
			public <T extends OAObject> void callHubDataRemoveFromAddedList(Hub<T> thisHub, T obj) {
				HubParentService.this.getHubDataService().removeFromAddedList(thisHub, obj);
			}
			@Override
			public <T extends OAObject> void callHubDataRemoveFromRemovedList(Hub<T> thisHub, T obj) {
				HubParentService.this.getHubDataService().removeFromRemovedList(thisHub, obj);
			}
			@Override
			public void callHubDSUpdateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster) {
				HubParentService.this.getHubDSService().updateMany2ManyLinks(masterObject, adds, removes, propFromMaster);
			}
		};
		return srvcHubAddRemove;
	}

	/**
	 * Returns the hubAOService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubAOService getHubAOService() {
		if (srvcHubAO != null) return srvcHubAO;
		
		srvcHubAO = new HubAOService(faHub) {
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
				HubParentService.this.srvcObjectParent.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);				
			}
			@Override
			public Object callObjectReflectGetProperty(OAObject oaObj, String propPath) {
				return HubParentService.this.srvcObjectParent.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public <T extends OAObject> Hub<T>[] callHubShareGetAllSharedHubs(Hub<T> thisHub, OAFilter<Hub<T>> filter) {
				return HubParentService.this.getHubShareService().getAllSharedHubs(thisHub, filter);
			}
			@Override
			public <T extends OAObject> void callHubLinkUpdateLinkProperty(Hub<T> hubFrom, T objFrom, int pos) {
				HubParentService.this.getHubLinkService().updateLinkedToHubProperty(hubFrom, objFrom, pos);				
			}
			@Override
			public <T extends OAObject> T callHubFindGetRealObject(Hub<T> hub, Object object) {
				return HubParentService.this.getHubFindService().getRealObject(hub, object);
			}
			@Override
			public <T extends OAObject> void callHubEventFireAfterChangeActiveObjectEvent(Hub<T> thisHub, T obj, int pos, boolean bAllShared) {
				HubParentService.this.getHubEventService().fireAfterChangeActiveObjectEvent(thisHub, obj, pos, bAllShared);				
			}
			@Override
			public void callHubDetailUpdateAllDetail(Hub<?> thisHub, boolean bUpdateLink) {
				HubParentService.this.getHubDetailService().updateAllDetail(thisHub, bUpdateLink);				
			}
			@Override
			public OAObject callHubDetailGetMasterObject(Hub<?> thisHub) {
				return HubParentService.this.getHubDetailService().getMasterObject(thisHub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromMasterObjectToDetail(Hub<?> thisDetailHub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromMasterObjectToDetail(thisDetailHub);
			}
			@Override
			public <T extends OAObject> int callHubDataGetPos(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink) {
				return HubParentService.this.getHubDataService().getPos(thisHub, object, adjustMaster, bUpdateLink);
			}
			@Override
			public <T extends OAObject> T callHubDataGetObjectAt(Hub<T> thisHub, int pos) {
				return HubParentService.this.getHubDataService().getObjectAt(thisHub, pos);
			}
		}; 
		return srvcHubAO;
	}

	/**
	 * Returns the hubAutoMatchService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubAutoMatchService getHubAutoMatchService() {
		if (srvcHubAutoMatch != null) return srvcHubAutoMatch;
		srvcHubAutoMatch = new HubAutoMatchService(faHub) {
		};
		return srvcHubAutoMatch;
	}

	/**
	 * Returns the hubCSService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubCSService getHubCSService() {
		if (srvcHubCS != null) return srvcHubCS;
		srvcHubCS = new HubCSService(faHub) {
			@Override
			public boolean callThreadLocalIsLoading() {
				return HubParentService.this.srvcThreadLocal.isLoading();
			}
			@Override
			public boolean callSyncClientIsObjectOnServer(OAObject obj) {
				OASyncClient sc = HubParentService.this.srvcSync.getClient();
				if (sc == null) return false;  
				return sc.isObjectOnServer(obj);
			}
			@Override
			public boolean callSyncSyncSort(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName, String paths, boolean bAscending, Comparator<?> comp) {
				RemoteSyncInterface rsi = HubParentService.this.srvcSync.getRemoteSync(); 
				if (rsi == null) return false;
				return rsi.sort(objectClass, objectKey, hubPropertyName, paths, bAscending, comp);
			}
			@Override
			public boolean callThreadLocalGetSendSyncMessages() {
				return HubParentService.this.srvcThreadLocal.getSendSyncMessages();
			}
			@Override
			public boolean callSyncRemoteSyncRemoveFromHub(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName, Class<? extends OAObject> objectClassX, OAObjectKey objectKeyX) {
				RemoteSyncInterface rsi = HubParentService.this.srvcSync.getRemoteSync();
				if (rsi == null) return false;
				return rsi.removeFromHub(objectClass, objectKey, hubPropertyName, objectClassX, objectKeyX);
			}
			@Override
			public boolean callSyncRemoteSyncRemoveAllFromHub(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName) {
				RemoteSyncInterface rsi = HubParentService.this.srvcSync.getRemoteSync();
				if (rsi == null) return false;
				return rsi.removeAllFromHub(objectClass, objectKey, hubPropertyName);
			}
			@Override
			public void callSyncSyncRefresh(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName) {
				RemoteSyncInterface rsi = HubParentService.this.srvcSync.getRemoteSync();
				if (rsi == null) return;
				rsi.refresh(masterObjectClass, masterObjectKey, hubPropertyName);
			}
			@Override
			public boolean callSyncSyncMoveObjectInHub(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName, int posFrom, int posTo) {
				RemoteSyncInterface rsi = HubParentService.this.srvcSync.getRemoteSync();
				if (rsi == null) return false;
				return rsi.moveObjectInHub(objectClass, objectKey, hubPropertyName, posFrom, posTo);
			}
			@Override
			public boolean callSyncSyncInsertInHub(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj, int pos) {
				RemoteSyncInterface rsi = HubParentService.this.srvcSync.getRemoteSync();
				if (rsi == null) return false;
				return rsi.insertInHub(masterObjectClass, masterObjectKey, hubPropertyName, obj, pos);
			}
			@Override
			public void callSyncSyncClearHubChanges(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName) {
				RemoteSyncInterface rsi = HubParentService.this.srvcSync.getRemoteSync();
				if (rsi == null) return;
				rsi.clearHubChanges(masterObjectClass, masterObjectKey, hubPropertyName);				
			}
			@Override
			public boolean callSyncSyncAddToHub(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj) {
				RemoteSyncInterface rsi = HubParentService.this.srvcSync.getRemoteSync();
				if (rsi == null) return false;
				return rsi.addToHub(masterObjectClass, masterObjectKey, hubPropertyName, obj);
			}
			
			@Override
			public void callSyncSyncAddNewToCache(OAObjectSerializer obj) {
				RemoteSyncInterface rsi = HubParentService.this.srvcSync.getRemoteSync();
				if (rsi == null) return;
				rsi.addNewToCache(obj);
			}
			@Override
			public boolean callSyncClientDeleteAll(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName) {
				RemoteClientInterface rci = HubParentService.this.srvcSync.getRemoteClient();
				if (rci == null) return false;
				return rci.deleteAll(objectClass, objectKey, hubPropertyName);
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
			public boolean callRemoteThreadIsRemoteThread() {
				return HubParentService.this.srvcRemoteThread.isRemoteThread();
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public OAObjectInfo callObjectInfoGetObjectInfo(Class<? extends OAObject> c) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getOAObjectInfo(c);
			}
			@Override
			public OAObjectInfo callObjectInfoGetObjectInfo(OAObject obj) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getOAObjectInfo(obj);
			}
			@Override
			public boolean callObjectHubIsInHub(OAObject oaObj) {
				return HubParentService.this.srvcObjectParent.getOAObjectHubService().isInHub(oaObj);
			}
			@Override
			public <T extends OAObject> boolean callHubIsInHubWithMaster(T oaObj, Hub<T> hubIgnore) {
				return HubParentService.this.srvcObjectParent.getOAObjectHubService().isInHubWithMaster(oaObj, hubIgnore);
			}
			@Override
			public boolean callHubIsInHubWithMaster(OAObject oaObj) {
				return HubParentService.this.srvcObjectParent.getOAObjectHubService().isInHubWithMaster(oaObj);
			}
			@Override
			public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> thisHub) {
				return HubParentService.this.getHubDetailService().getPropertyFromMasterToDetail(thisHub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromMasterObjectToDetail(Hub<?> thisDetailHub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromMasterObjectToDetail(thisDetailHub);
			}
		};
		return srvcHubCS;
	}

	/**
	 * Returns the hubDataService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubDataService getHubDataService() {
		if (srvcHubData != null) return srvcHubData;
		
		srvcHubData = new HubDataService(faHub) {
			@Override
			public OAObjectKey callObjectKeyGetKey(OAObject oaObj) {
				return HubParentService.this.srvcObjectParent.getOAObjectKeyService().getKey(oaObj);
			}
			@Override
			public boolean callObjectKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return HubParentService.this.srvcObjectParent.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}
			@Override
			public <T extends OAObject> T callObjectReflectGetObject(Class<T> clazz, Object key) {
				return HubParentService.this.srvcObjectParent.getOAObjectReflectService().getObject(clazz, key);
			}
			@Override
			public <T extends OAObject> boolean callObjectHubAddHub(T oaObj, Hub<T> hub) {
				return HubParentService.this.srvcObjectParent.getOAObjectHubService().addHub(oaObj, hub);
			}
			@Override
			public OALinkInfo callObjectInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getRecursiveLinkInfo(thisOI, type);
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public Object callObjectReflectGetProperty(OAObject oaObj, String propPath) {
				return HubParentService.this.srvcObjectParent.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public <T extends OAObject> T callObjectCacheGet(Class<T> clazz, Object key) {
				return HubParentService.this.srvcObjectParent.getOAObjectCacheService().get(clazz, key);
			}
			@Override
			public <T extends OAObject> boolean callObjectHubIsAlreadyInHub(T oaObj, Hub<T> hubFind) {
				return HubParentService.this.srvcObjectParent.getOAObjectHubService().isAlreadyInHub(oaObj, hubFind);
			}
			@Override
			public OAObjectKey callObjectKeyCreateObjectKey(Class<? extends OAObject> c, Object... ids) {
				return HubParentService.this.srvcObjectParent.getOAObjectKeyService().createObjectKey(c, ids);
			}
			@Override
			public boolean callHubCSClearHubChanges(Hub<?> thisHub) {
				return HubParentService.this.getHubCSService().clearHubChanges(thisHub);
			}
			@Override
			public <T extends OAObject> void callHubDetailSetPropertyToMasterHub(Hub<T> thisHub, T detailObject, OAObject objMaster) {
				HubParentService.this.getHubDetailService().setPropertyToMasterHub(thisHub, detailObject, objMaster);				
			}
			@Override
			public boolean callHubSelectIsMoreData(Hub<?> thisHub) {
				return HubParentService.this.getHubSelectService().isMoreData(thisHub);
			}
			@Override
			public int callHubSelectFetchMore(Hub<?> thisHub) {
				return HubParentService.this.getHubSelectService().fetchMore(thisHub);
			}
			@Override
			public <T extends OAObject> T callHubFindGetRealObject(Hub<T> hub, Object object) {
				return HubParentService.this.getHubFindService().getRealObject(hub, object);
			}
			@Override
			public <T extends OAObject> void callHubShareSetSharedHub(Hub<T> thisHub, Hub<T> sharedMasterHub, boolean shareActiveObject) {
				HubParentService.this.getHubShareService().setSharedHub(thisHub, sharedMasterHub, shareActiveObject);				
			}
			@Override
			public <T extends OAObject> void callHubShareSetSharedHub(Hub<T> thisHub, Hub<T> sharedMasterHub, boolean shareActiveObject, Object newLinkValue) {
				HubParentService.this.getHubShareService().setSharedHub(thisHub, sharedMasterHub, shareActiveObject, newLinkValue);				
			}
			@Override
			public <T extends OAObject> boolean callHubDetailSetMasterHubActiveObject(Hub<T> thisHub, T detailObject, boolean bUpdateLink) {
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
			public boolean callSyncIsSingleUserOrServer() {
				return HubParentService.this.srvcSync.isSingleUserOrServer();
			}
			@Override
			public int callHubDataGetCurrentSize(Hub<?> thisHub) {
				return HubParentService.this.getHubDataService().getCurrentSize(thisHub);
			}
			@Override
			public int callHubShareGetSharedWeakHubSize(Hub<?> thisHub) {
				return HubParentService.this.getHubShareService().getSharedWeakHubSize(thisHub);
			}
			@Override
			public void callHubStatusSetChanged(Hub<?> thisHub, boolean b) {
				HubParentService.this.getHubStatusService().setChanged(thisHub, b);
			}
		};
		return srvcHubData;
	}




	/**
	 * Returns the hubDeleteService for the supplied Hub context.
	 *
	 * @return result value
	 */




	public HubDeleteService getHubDeleteService() {
		if (srvcHubDelete != null) return srvcHubDelete;
		
		srvcHubDelete =  new HubDeleteService(faHub) {
			@Override
			public void callObjectDeleteDelete(OAObject oaObj, OACascade cascade) {
				HubParentService.this.srvcObjectParent.getOAObjectDeleteService().delete(oaObj, cascade);				
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public boolean callHubCSDeleteAll(Hub<?> thisHub) {
				return HubParentService.this.getHubCSService().deleteAll(thisHub);
			}
			@Override
			public void callHubAddRemoveClear(Hub<?> thisHub) {
				HubParentService.this.getHubAddRemoveService().clear(thisHub);				
			}
			@Override
			public void callHubDataClearHubChanges(Hub<?> thisHub) {
				HubParentService.this.getHubDataService().clearHubChanges(thisHub);				
			}
			@Override
			public <T extends OAObject> boolean callHubAddRemoveRemove(Hub<T> thisHub, T obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll) {
				T t = HubParentService.this.getHubAddRemoveService().remove(thisHub, obj, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
				return t != null;
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public OAObject callHubDetailGetMasterObject(Hub<?> thisHub) {
				return HubParentService.this.getHubDetailService().getMasterObject(thisHub);
			}
			@Override
			public <T extends OAObject> Vector<T> callHubDataCreateVecRemove(Hub<T> thisHub) {
				return HubParentService.this.getHubDataService().createVecRemove(thisHub);
			}
			@Override
			public void callHubStatusSetChanged(Hub<?> thisHub, boolean bChanged) {
				HubParentService.this.getHubStatusService().setChanged(thisHub, bChanged);				
			}
			@Override
			public void callHub_updateHubAddsAndRemoves(Hub<?> thisHub, int iCascadeRule, OACascade cascade, boolean bIsSaving) {
				HubParentService.this.getHubAddRemoveService()._updateHubAddsAndRemoves(thisHub, iCascadeRule, cascade, bIsSaving);				
			}
			@Override
			public void callThreadLocalSetDeleting(Hub<?> hub, boolean b) {
				HubParentService.this.srvcThreadLocal.setDeleting(hub, b);				
			}
			@Override
			public boolean callThreadLocalIsDeleting(Hub<?> hub) {
				return HubParentService.this.srvcThreadLocal.isDeleting(hub);
			}
			@Override
			public void callThreadLocalLock(Hub<?> hub) {
				HubParentService.this.srvcThreadLocal.lock(hub);				
			}
			@Override
			public void callThreadLocalUnlock(Hub<?> hub) {
				HubParentService.this.srvcThreadLocal.unlock(hub);				
			}
			@Override
			public boolean callThreadLocalGetSendSyncMessages() {
				return HubParentService.this.srvcThreadLocal.getSendSyncMessages();
			}
			@Override
			public void callThreadLocalSetSendSyncMessages(boolean b) {
				HubParentService.this.srvcThreadLocal.setSendSyncMessages(b);
			}
		};
		
		return srvcHubDelete;
	}

	/**
	 * Returns the hubDetailService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubDetailService getHubDetailService() {
		if (srvcHubDetail != null) return srvcHubDetail;
		srvcHubDetail = new HubDetailService(faHub) {
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public Object callObjectReflectGetProperty(OAObject oaObj, String propPath) {
				return HubParentService.this.srvcObjectParent.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public void callObjectReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				HubParentService.this.srvcObjectParent.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);				
			}
			@Override
			public Method callObjectInfoGetMethod(Class<?> clazz, String methodName) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getMethod(clazz, methodName);
			}
			@Override
			public boolean callObjectReflectIsReferenceHubLoaded(OAObject oaObj, String propertyName) {
				return HubParentService.this.srvcObjectParent.getOAObjectReflectService().isReferenceHubLoaded(oaObj, propertyName);
			}
			@Override
			public <T extends OAObject> void callObjectHubRemoveHub(T oaObj, Hub<T> hub, boolean bIsOnHubFinalize) {
				HubParentService.this.srvcObjectParent.getOAObjectHubService().removeHub(oaObj, hub, bIsOnHubFinalize);				
			}
			@Override
			public OALinkInfo callObjectInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}
			@Override
			public OALinkInfo callObjectInfoGetLinkInfo(OAObjectInfo oi, OAObject fromObject, Hub<?> hub) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getLinkInfo(oi, fromObject, hub);
			}
			@Override
			public OAObjectInfo callObjectInfoGetObjectInfo(Class<? extends OAObject> clazz) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getOAObjectInfo(clazz);
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
			public boolean callThreadLocalGetCanAdjustHub(Hub<?> hub) {
				return HubParentService.this.srvcThreadLocal.getCanAdjustHub(hub);
			}
			@Override
			public <T extends OAObject> T callHubAOSetActiveObject(Hub<T> thisHub, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
				return HubParentService.this.getHubAOService().setActiveObject(thisHub, pos, bUpdateLink, bForce, bCalledByShareHub);
			}
			@Override
			public <T extends OAObject> void callHubAOSetActiveObject(Hub<T> thisHub, T object, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, pos, bUpdateLink, bForce, bCalledByShareHub);				
			}
			@Override
			public <T extends OAObject> void callHubAOSetActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, adjustMaster, bUpdateLink, bForce);				
			}
			
			@Override
			public <T extends OAObject> WeakReference<Hub<T>>[] callHubShareGetSharedWeakHubs(Hub<T> thisHub) {
				return HubParentService.this.getHubShareService().getSharedWeakHubs(thisHub);
			}
			@Override
			public <T extends OAObject> Hub<T> callHubShareGetFirstSharedHub(Hub<T> thisHub, OAFilter<Hub<T>> filter, boolean bIncludeFilteredHubs, boolean bOnlyIfSharedAO) {
				return HubParentService.this.getHubShareService().getFirstSharedHub(thisHub, filter, bIncludeFilteredHubs, bOnlyIfSharedAO);
			}
			@Override
			public String callHubGetPathforClasses(Hub<?> hub, Class<? extends OAObject>[] classes) {
				return OAPathDelegate.getPathforClasses(hub, classes);
			}
			@Override
			public <T extends OAObject> void callHubDataSetObjectClass(Hub<T> thisHub, Class<T> objClass) {
				HubParentService.this.getHubDataService().setObjectClass(thisHub, objClass);				
			}
			@Override
			public <T extends OAObject> Hub<T> callHubShareGetMainSharedHub(Hub<T> hub) {
				return HubParentService.this.getHubShareService().getMainSharedHub(hub);
			}
			@Override
			public void callHubShareSyncSharedHubs(Hub<?> thisHub, boolean bShareActiveObject, HubDataActive daOld, HubDataActive daNew, boolean bUpdateLink) {
				HubParentService.this.getHubShareService().syncSharedHubs(thisHub, bShareActiveObject, daOld, daNew, bUpdateLink);				
			}
			
			@Override
			public <T extends OAObject> void callHubShareRemoveSharedHub(Hub<T> sharedHub, Hub<T> hub) {
				HubParentService.this.getHubShareService()._removeSharedHub(sharedHub, hub);				
			}
			@Override
			public void callHubEventFireOnNewListEvent(Hub<?> thisHub, boolean bAll) {
				HubParentService.this.getHubEventService().fireOnNewListEvent(thisHub, bAll);				
			}
			@Override
			public <T extends OAObject> T callHubDataGetObjectAt(Hub<T> thisHub, int pos) {
				return HubParentService.this.getHubDataService().getObjectAt(thisHub, pos);
			}
			@Override
			public boolean callHubSortIsSorted(Hub<?> thisHub) {
				return HubParentService.this.getHubSortService().isSorted(thisHub);
			}
			@Override
			public String callHubSortGetSortProperty(Hub<?> thisHub) {
				return HubParentService.this.getHubSortService().getSortProperty(thisHub);
			}
			@Override
			public boolean callHubSortGetSortAsc(Hub<?> thisHub) {
				return HubParentService.this.getHubSortService().getSortAsc(thisHub);
			}
			@Override
			public <T extends OAObject> void callHubShareAddSharedHub(Hub<T> thisHub, Hub<T> hub) {
				HubParentService.this.getHubShareService().addSharedHub(thisHub, hub);				
			}
			@Override
			public <T extends OAObject> boolean callHubAddRemoveInternalAdd(Hub<T> thisHub, T obj, boolean bHasLock, boolean bCheckContains) {
				return HubParentService.this.getHubAddRemoveService().internalAdd(thisHub, obj, bHasLock, bCheckContains);
			}
			@Override
			public void callHubDataIncChangeCount(Hub<?> thisHub) {
				HubParentService.this.getHubDataService().incChangeCount(thisHub);				
			}
			@Override
			public <T extends OAObject> Hub<T> callHubLinkGetHubWithLink(Hub<T> thisHub, boolean bIncludeCopiedHubs) {
				return HubParentService.this.getHubLinkService().getHubWithLink(thisHub, bIncludeCopiedHubs);
			}
		};
		return srvcHubDetail;
	}

	/**
	 * Returns the hubDSService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubDSService getHubDSService() {
		if (srvcHubDS != null) return srvcHubDS;
    	srvcHubDS = new HubDSService(faHub) {
			@Override
			public boolean callObjectInfoIsMany2Many(OALinkInfo thisLi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().isMany2Many(thisLi);
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}

			@Override
			public <T extends OAObject> T[] callHubAddRemoveGetRemovedObjects(Hub<T> thisHub) {
				return HubParentService.this.getHubAddRemoveService().getRemovedObjects(thisHub);
			}
    	};
		return srvcHubDS;
	}


	/**
	 * Returns the hubEventService for the supplied Hub context.
	 *
	 * @return result value
	 */


	public HubEventService getHubEventService() {
		if (srvcHubEvent != null) return srvcHubEvent;
		
    	srvcHubEvent = new HubEventService(faHub) {
			@Override
			public <T extends OAObject> OAObjectCallback callObjectCallbackGetVerifyRemoveObjectCallback(Hub<T> hub, T objRemove, OAObjectCallback.CheckType[] onlyCheckTypes) {
				return HubParentService.this.srvcObjectParent.getOAObjectRulesService().getVerifyRemoveObjectCallback(hub, objRemove, onlyCheckTypes);
			}
			@Override
			public <T extends OAObject> void callObjectCacheFireAfterRemoveEvent(Hub<T> hub, T obj) {
				HubParentService.this.srvcObjectParent.getOAObjectCacheService().fireAfterRemoveEvent(hub, obj);
			}
			@Override
			public OAObjectInfo callObjectInfoGetObjectInfo(Class<?> clazz) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public OAObjectInfo callObjectInfoGetObjectInfo(OAObject obj) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getOAObjectInfo(obj);
			}
			@Override
			public OAObjectCallback callObjectCallbackGetVerifyRemoveAllObjectCallback(Hub<?> hub, OAObjectCallback.CheckType[] onlyCheckTypes) {
				return HubParentService.this.srvcObjectParent.getOAObjectRulesService().getVerifyRemoveAllObjectCallback(hub, onlyCheckTypes);
			}
			@Override
			public <T extends OAObject> void callObjectCacheFireAfterAddEvent(Hub<T> hub, T obj) {
				HubParentService.this.srvcObjectParent.getOAObjectCacheService().fireAfterAddEvent(hub, obj);				
			}
			@Override
			public <T extends OAObject> OAObjectCallback callObjectCallbackGetVerifyAddObjectCallback(Hub<T> hub, T oaObj, OAObjectCallback.CheckType[] onlyCheckTypes) {
				return HubParentService.this.srvcObjectParent.getOAObjectRulesService().getVerifyAddObjectCallback(hub, oaObj, onlyCheckTypes);
			}
			@Override
			public OALinkInfo callObjectInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}
			@Override
			public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> thisHub) {
				return HubParentService.this.getHubDetailService().getPropertyFromMasterToDetail(thisHub);
			}
			@Override
			public <T extends OAObject> boolean callHubVerifyUniqueProperty(Hub<T> thisHub, T object) {
				return HubParentService.this.getHubPropertyService().verifyUniqueProperty(thisHub, object);
			}
			@Override
			public void callHubDetailUpdateDetail(Hub<?> thisHub, HubDetail detail, Hub<?> detailHub, boolean bUpdateLink) {
				HubParentService.this.getHubDetailService().updateDetail(thisHub, detail, detailHub, bUpdateLink);				
			}
			@Override
			public <T extends OAObject> WeakReference<Hub<T>>[] callHubShareGetSharedWeakHubs(Hub<T> thisHub) {
				return HubParentService.this.getHubShareService().getSharedWeakHubs(thisHub);
			}
			@Override
			public void callHubDataIncChangeCount(Hub<?> thisHub) {
				HubParentService.this.getHubDataService().incChangeCount(thisHub);
			}
			@Override
			public boolean callRemoteThreadIsRemoteThread() {
				return HubParentService.this.srvcRemoteThread.isRemoteThread();
			}
			@Override
			public boolean callThreadLocalAddHubEvent(HubEvent he) {
				return HubParentService.this.srvcThreadLocal.addHubEvent(he);
			}
			@Override
			public void callThreadLocalRemoveHubEvent(HubEvent<?> he) {
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
			public <T extends OAObject> boolean callThreadLocalHasSentCalcPropertyChange(Hub<T> thisHub, T thisObj, String propertyName) {
				return HubParentService.this.srvcThreadLocal.hasSentCalcPropertyChange(thisHub, thisObj, propertyName);
			}
    	};
		return srvcHubEvent;
	}

	/**
	 * Returns the hubFindService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubFindService getHubFindService() {
		if (srvcHubFind != null) return srvcHubFind;
    	srvcHubFind = new HubFindService() {
			@Override
			public <T extends OAObject> T callObjectCacheGet(Class<T> clazz, Object key) {
				return HubParentService.this.srvcObjectParent.getOAObjectCacheService().get(clazz, key);
			}
			@Override
			public <T extends OAObject> T callHubDataGetObject(Hub<T> thisHub, Object key) {
				return HubParentService.this.getHubDataService().getObject(thisHub, key);
			}
    	};
		return srvcHubFind;
	}


	/**
	 * Returns the hubLinkService for the supplied Hub context.
	 *
	 * @return result value
	 */


	public HubLinkService getHubLinkService() {
		if (srvcHubLink != null) return srvcHubLink;
    	srvcHubLink = new HubLinkService(faHub) {
			@Override
			public OAObjectInfo callObjectInfoGetObjectInfo(Class<?> clazz) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public Method callObjectInfoGetMethod(Class<? extends OAObject> clazz, String methodName) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getMethod(clazz, methodName);
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public <T extends OAObject> void callHubEventRemoveHubListener(Hub<T> thisHub, HubListener<T> l) {
				HubParentService.this.getHubEventService().removeHubListener(thisHub, l);				
			}
			@Override
			public <T extends OAObject> void callHubEventFireAfterPropertyChange(Hub<T> thisHub, T oaObj, String propertyName, Object oldValue, Object newValue, OALinkInfo linkInfo) {
				HubParentService.this.getHubEventService().fireAfterPropertyChange(thisHub, oaObj, propertyName, oldValue, newValue, linkInfo);				
			}
			@Override
			public <T extends OAObject> void callHubEventAddHubListener(Hub<T> thisHub, HubListener<T> hl) {
				HubParentService.this.getHubEventService().addHubListener(thisHub, hl);				
			}
			@Override
			public <T extends OAObject> void callHubEventFireAfterChangeActiveObjectEvent(Hub<T> thisHub, T obj, int pos, boolean bAllShared) {
				HubParentService.this.getHubEventService().fireAfterChangeActiveObjectEvent(thisHub, obj, pos, bAllShared);
			}
			@Override
			public <T extends OAObject> Hub<T> callHubShareGetFirstSharedHub(Hub<T> thisHub, OAFilter<Hub<T>> filter, boolean bIncludeFilteredHubs, boolean bOnlyIfSharedAO) {
				return HubParentService.this.getHubShareService().getFirstSharedHub(thisHub, filter, bIncludeFilteredHubs, bOnlyIfSharedAO);
			}
			@Override
			public <T extends OAObject> void callHubAOSetActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, adjustMaster, bUpdateLink, bForce);
			}
			@Override
			public <T extends OAObject> void callHubAOSetActiveObject(Hub<T> thisHub, T object, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, pos, bUpdateLink, bForce, bCalledByShareHub);
			}
			@Override
			public HubDataMaster callHubDetailGetDataMaster(Hub<?> thisHub) {
				return HubParentService.this.getHubDetailService().getDataMaster(thisHub);
			}
			@Override
			public <T extends OAObject> int callHubDataGetPos(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink) {
				return HubParentService.this.getHubDataService().getPos(thisHub, object, adjustMaster, bUpdateLink);
			}
			@Override
			public <T extends OAObject> Hub<T>[] callHubShareGetAllSharedHubs(Hub<T> thisHub) {
				return HubParentService.this.getHubShareService().getAllSharedHubs(thisHub);
			}
			@Override
			public void callThreadLocalAddDontAdjustHub(Hub<?> hub) {
				HubParentService.this.srvcThreadLocal.addDontAdjustHub(hub);
			}
			@Override
			public void callThreadLocalRemoveDontAdjustHub(Hub<?> hub) {
				HubParentService.this.srvcThreadLocal.removeDontAdjustHub(hub);
			}
    	};
		return srvcHubLink;
	}

	/**
	 * Returns the hubMasterService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubMasterService getHubMasterService() {
		if (srvcHubMaster != null) return srvcHubMaster;
		srvcHubMaster = new HubMasterService(faHub) {
			@Override
			public <T extends OAObject> Hub<?> callHubLinkGetHubWithLink(Hub<T> thisHub, boolean bIncludeCopiedHubs) {
				return HubParentService.this.getHubLinkService().getHubWithLink(thisHub, bIncludeCopiedHubs);
			}
			@Override
			public HubDataMaster callHubDetailGetDataMaster(Hub<?> thisHub, boolean bIncludedFilteredHub) {
				return HubParentService.this.getHubDetailService().getDataMaster(thisHub, bIncludedFilteredHub);
			}
		};
		
		return srvcHubMaster;
	}


	/**
	 * Returns the hubPropertyService for the supplied Hub context.
	 *
	 * @return result value
	 */


	public HubPropertyService getHubPropertyService() {
		if (srvcHubProperty != null) return srvcHubProperty;
		
		srvcHubProperty = new HubPropertyService(faHub) {
			@Override
			public Method callObjectInfoGetMethod(OALinkInfo li) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getMethod(li);
			}
			@Override
			public Method callObjectInfoGetMethod(Class<?> clazz, String methodName) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getMethod(clazz, methodName);
			}
			@Override
			public Object callObjectPropertyGetProperty(OAObject oaObj, String name) {
				return HubParentService.this.srvcObjectParent.getOAObjectPropertyService().getProperty(oaObj, name);
			}
			@Override
			public boolean callThreadLocalIsLoading() {
				return HubParentService.this.srvcThreadLocal.isLoading();
			}
			@Override
			public OAObjectInfo callObjectInfoGetOAObjectInfo(Hub hub) {
				if (hub == null) return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getOAObjectInfo(String.class);
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getOAObjectInfo(hub.getObjectClass());
			}
		};
		
		return srvcHubProperty;
	}


	/**
	 * Returns the hubRootService for the supplied Hub context.
	 *
	 * @return result value
	 */


	public HubRootService getHubRootService() {
		if (srvcHubRoot != null) return srvcHubRoot;
		
    	srvcHubRoot = new HubRootService(faHub) {
			@Override
			public OALinkInfo callObjectInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getRecursiveLinkInfo(thisOI, type);
			}
			@Override
			public <T extends OAObject> Hub<T> callObjectInfoGetRootHub(OAObjectInfo thisOI, Hub<T> hub) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getRootHub(thisOI);
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public Object callObjectReflectGetProperty(OAObject oaObj, String propPath) {
				return HubParentService.this.srvcObjectParent.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public OALinkInfo callObjectInfoGetLinkToOwner(OAObjectInfo thisOI) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getLinkToOwner(thisOI);
			}
			@Override
			public void callObjectInfoSetRootHub(OAObjectInfo thisOI, Hub<?> h) {
				HubParentService.this.srvcObjectParent.getOAObjectInfoService().setRootHub(thisOI, h);
			}
			@Override
			public <T extends OAObject> Hub<T>[] callHubShareGetAllSharedHubs(Hub<T> thisHub, OAFilter<Hub<T>> filter) {
				return HubParentService.this.getHubShareService().getAllSharedHubs(thisHub, filter);
			}
    	};
		
		return srvcHubRoot;
	}

	/**
	 * Returns the hubSaveService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubSaveService getHubSaveService() {
		if (srvcHubSave != null) return srvcHubSave;
		
    	srvcHubSave = new HubSaveService() {
			@Override
			public void callObjectSaveSave(OAObject oaObj, int iCascadeRule, OACascade cascade) {
				HubParentService.this.srvcObjectParent.getOAObjectSaveService().save(oaObj, iCascadeRule, cascade);
			}
			@Override
			public boolean callObjectInfoIsMany2Many(OALinkInfo thisLi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().isMany2Many(thisLi);
			}
			@Override
			public void callObjectSaveSaveObjectOnly(OAObject oaObj, OACascade cascade) {
				HubParentService.this.srvcObjectParent.getOAObjectSaveService()._saveObjectOnly(oaObj, cascade);
			}
			@Override
			public HubDataMaster callHubDetailGetDataMaster(Hub<?> thisHub) {
				return HubParentService.this.getHubDetailService().getDataMaster(thisHub);
			}
			@Override
			public <T extends OAObject> T[] callHubDataGetAddedObjects(Hub<T> thisHub) {
				return HubParentService.this.getHubDataService().getAddedObjects(thisHub);
			}
			@Override
			public void callHub_updateHubAddsAndRemoves(Hub<?> thisHub, int iCascadeRule, OACascade cascade, boolean bIsSaving) {
				HubParentService.this.getHubAddRemoveService()._updateHubAddsAndRemoves(thisHub, iCascadeRule, cascade, bIsSaving);
			}
			@Override
			public void callHubStatusSetReferenceable(Hub<?> hub, boolean bReferenceable) {
				HubParentService.this.getHubStatusService().setReferenceable(hub, bReferenceable);
			}
    	};
		
		return srvcHubSave;
	}

	/**
	 * Returns the hubSequenceService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubSequenceService getHubSequenceService() {
		if (srvcHubSequence != null) return srvcHubSequence;
		srvcHubSequence = new HubSequenceService(faHub) {
			@Override
			public boolean callHubCSIsClient() {
				return HubParentService.this.getHubCSService().callSyncIsClient();
			}
			@Override
			public void callHubSortCancelSort(Hub<?> hub) {
				HubParentService.this.getHubSortService().cancelSort(hub);
			}
		};
		return srvcHubSequence;
		
	}


	/**
	 * Returns the hubSelectService for the supplied Hub context.
	 *
	 * @return result value
	 */
	public HubSelectService getHubSelectService() {
		if (srvcHubSelect != null) return srvcHubSelect;
		
    	srvcHubSelect = new HubSelectService(faHub) {
			@Override
			public OAObjectInfo callObjectInfoGetObjectInfo(Class<?> clazz) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public <T extends OAObject> void callObjectHubRemoveHub(T oaObj, Hub<T> hub, boolean bIsOnHubFinalize) {
				HubParentService.this.srvcObjectParent.getOAObjectHubService().removeHub(oaObj, hub, bIsOnHubFinalize);
			}
			@Override
			public void callObjectCacheSetSelectAllHub(Hub<?> hub) {
				HubParentService.this.srvcObjectParent.getOAObjectCacheService().setSelectAllHub(hub);
			}
			@Override
			public void callObjectCacheRemoveSelectAllHub(Hub<?> hub) {
				HubParentService.this.srvcObjectParent.getOAObjectCacheService().removeSelectAllHub(hub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public void callHubDataEnsureCapacity(Hub<?> thisHub, int size) {
				HubParentService.this.getHubDataService().ensureCapacity(thisHub, size);
			}
			@Override
			public <T extends OAObject> boolean callHubAddRemoveAdd(Hub<T> thisHub, T obj) {
				return HubParentService.this.getHubAddRemoveService().add(thisHub, obj);
			}
			@Override
			public void callHubEventFireBeforeSelectEvent(Hub<?> thisHub) {
				HubParentService.this.getHubEventService().fireBeforeSelectEvent(thisHub);
			}
			@Override
			public void callHubDataIncChangeCount(Hub<?> thisHub) {
				HubParentService.this.getHubDataService().incChangeCount(thisHub);
			}
			@Override
			public int callHubDataGetCurrentSize(Hub<?> thisHub) {
				return HubParentService.this.getHubDataService().getCurrentSize(thisHub);
			}
			@Override
			public <T extends OAObject> T callHubDataGetObjectAt(Hub<T> thisHub, int pos) {
				return HubParentService.this.getHubDataService().getObjectAt(thisHub, pos);
			}
			@Override
			public void callHubDataClearAllAndReset(Hub<?> thisHub) {
				HubParentService.this.getHubDataService().clearAllAndReset(thisHub);
			}
			@Override
			public <T extends OAObject> Hub<T>[] callHubShareGetAllSharedHubs(Hub<T> thisHub, OAFilter<Hub<T>> filter) {
				return HubParentService.this.getHubShareService().getAllSharedHubs(thisHub, filter);
			}
			@Override
			public void callHubEventFireOnNewListEvent(Hub<?> thisHub, boolean bAll) {
				HubParentService.this.getHubEventService().fireOnNewListEvent(thisHub, bAll);
			}
			@Override
			public void callHubDataResizeToFit(Hub<?> thisHub) {
				HubParentService.this.getHubDataService().resizeToFit(thisHub);
			}
			@Override
			public void callHubEventFireBeforeRefreshEvent(Hub<?> thisHub) {
				HubParentService.this.getHubEventService().fireBeforeRefreshEvent(thisHub);
			}
			@Override
			public OAObject callHubDetailGetMasterObject(Hub<?> thisHub) {
				return HubParentService.this.getHubDetailService().getMasterObject(thisHub);
			}
			@Override
			public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> thisHub) {
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


	/**
	 * Returns the hubSerializeService for the supplied Hub context.
	 *
	 * @return result value
	 */


	public HubSerializeService getHubSerializeService() {
		if (srvcHubSerialize != null) return srvcHubSerialize;

    	srvcHubSerialize = new HubSerializeService(faHub) {
			@Override
			public boolean callObjectHubIsAlreadyInHub(OAObject oaObj, OALinkInfo li) {
				return HubParentService.this.srvcObjectParent.getOAObjectHubService().isAlreadyInHub(oaObj, li);
			}
			@Override
			public <T extends OAObject> boolean callObjectHubAddHub(T oaObj, Hub<T> hub) {
				return HubParentService.this.srvcObjectParent.getOAObjectHubService().addHub(oaObj, hub);
			}
			@Override
			public boolean callHubSelectIsMoreData(Hub<?> thisHub) {
				return HubParentService.this.getHubSelectService().isMoreData(thisHub);
			}
			@Override
			public void callHubSelectLoadAllData(Hub<?> thisHub) {
				HubParentService.this.getHubSelectService().loadAllData(thisHub);	
			}
			@Override
			public boolean callThreadLocalGetSendSyncMessages() {
				return HubParentService.this.srvcThreadLocal.getSendSyncMessages();
			}
			@Override
			public void callThreadLocalSetSendSyncMessages(boolean b) {
				HubParentService.this.srvcThreadLocal.setSendSyncMessages(b);
			}
    	};
		return srvcHubSerialize;
	}

	/**
	 * Returns the hubShareService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubShareService getHubShareService() {
		if (srvcHubShare != null) return srvcHubShare;
		
    	srvcHubShare = new HubShareService(faHub) {
			@Override
			public <T extends OAObject> void callObjectHubRemoveHub(T oaObj, Hub<T> hub, boolean bIsOnHubFinalize) {
				HubParentService.this.srvcObjectParent.getOAObjectHubService().removeHub(oaObj, hub, bIsOnHubFinalize);
			}
			@Override
			public <T extends OAObject> HubListener<T>[] callHubEventGetHubListeners(Hub<T> thisHub) {
				return HubParentService.this.getHubEventService().getHubListeners(thisHub);
			}
			@Override
			public <T extends OAObject> void callHubAOSetActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, adjustMaster, bUpdateLink, bForce);
			}
			@Override
			public <T extends OAObject> void callHubAOSetActiveObject(Hub<T> thisHub, T object, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, object, pos, bUpdateLink, bForce, bCalledByShareHub);
			}
			@Override
			public void callHubAOSetActiveObject(Hub<?> thisHub, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
				HubParentService.this.getHubAOService().setActiveObject(thisHub, pos, bUpdateLink, bForce, bCalledByShareHub);
			}
			@Override
			public void callHubEventClearGetAllListenerCache(Hub<?> hub) {
				HubParentService.this.getHubEventService().clearGetAllListenerCache(hub);
			}
			@Override
			public <T extends OAObject> void callHubEventFireAfterChangeActiveObjectEvent(Hub<T> thisHub, T obj, int pos, boolean bAllShared) {
				HubParentService.this.getHubEventService().fireAfterChangeActiveObjectEvent(thisHub, obj, pos, bAllShared);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<?> thisDetailHub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromMasterToDetail(thisDetailHub);
			}
			@Override
			public void callHubDataIncChangeCount(Hub<?> thisHub) {
				HubParentService.this.getHubDataService().incChangeCount(thisHub);
			}
			@Override
			public <T extends OAObject> void callHubDataSetObjectClass(Hub<T> thisHub, Class<T> objClass) {
				HubParentService.this.getHubDataService().setObjectClass(thisHub, objClass);
			}
			@Override
			public void callHubEventFireOnNewListEvent(Hub<?> thisHub, boolean bAll) {
				HubParentService.this.getHubEventService().fireOnNewListEvent(thisHub, bAll);
			}
			@Override
			public boolean callRemoteThreadIsRemoteThread() {
				return HubParentService.this.srvcRemoteThread.isRemoteThread();
			}
    	};
		return srvcHubShare;
	}

	/**
	 * Returns the hubSizeService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubSizeService getHubSizeService() {
		if (srvcHubSize != null) return srvcHubSize;
    	srvcHubSize = new HubSizeService(faHub) {
			@Override
			public boolean callHubSelectIsMoreData(Hub<?> thisHub) {
				return HubParentService.this.getHubSelectService().isMoreData(thisHub);
			}
			@Override
			public <T extends OAObject> boolean callHubSelectIsCounted(Hub<T> thisHub) {
				return HubParentService.this.getHubSelectService().isCounted(thisHub);
			}
			@Override
			public int callHubDataGetCurrentSize(Hub<?> thisHub) {
				return HubParentService.this.getHubDataService().getCurrentSize(thisHub);
			}
			@Override
			public <T extends OAObject> int callHubSelectFetchMore(Hub<T> thisHub) {
				return HubParentService.this.getHubSelectService().fetchMore(thisHub);
			}
			@Override
			public <T extends OAObject> int callHubSelectGetCount(Hub<T> thisHub) {
				return HubParentService.this.getHubSelectService().getCount(thisHub);
			}
			@Override
			public void callHubSelectLoadAllData(Hub<?> hub) {
				HubParentService.this.getHubSelectService().loadAllData(hub);
			}
    	};
		return srvcHubSize;
	}

	/**
	 * Returns the hubSortService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubSortService getHubSortService() {
		if (srvcHubSort != null) return srvcHubSort;
		
    	srvcHubSort = new HubSortService(faHub) {
			@Override
			public void callHubCSSort(Hub<?> thisHub, String paths, boolean bAscending, Comparator<?> comp) {
				HubParentService.this.getHubCSService().sort(thisHub, paths, bAscending, comp);				
			}
			@Override
			public void callHubSelectLoadAllData(Hub<?> thisHub) {
				HubParentService.this.getHubSelectService().loadAllData(thisHub);				
			}
			@Override
			public void callHubEventFireAfterSortEvent(Hub<?> thisHub) {
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
			public boolean callThreadLocalAddSiblingHelper(OASiblingHelper<?> sh) {
				return HubParentService.this.srvcThreadLocal.addSiblingHelper(sh);
			}
			@Override
			public void callThreadLocalRemoveSiblingHelper(OASiblingHelper<?> sh) {
				HubParentService.this.srvcThreadLocal.removeSiblingHelper(sh);
			}
    	};
		return srvcHubSort;
	}

	/**
	 * Returns the hubStatusService for the supplied Hub context.
	 *
	 * @return result value
	 */

	public HubStatusService getHubStatusService() {
		if (srvcHubStatus != null) return srvcHubStatus;
		srvcHubStatus = new HubStatusService(faHub) {
			@Override
			public HubDataMaster callHubDetailGetDataMaster(Hub<?> thisHub, boolean bIncludedFilteredHub) {
				return HubParentService.this.getHubDetailService().getDataMaster(thisHub, bIncludedFilteredHub);
			}
			@Override
			public <T extends OAObject> Hub<T> callHubShareGetMainSharedHub(Hub<T> hub) {
				return HubParentService.this.getHubShareService().getMainSharedHub(hub);
			}
			@Override
			public <T extends OAObject> HubListener<T>[] callHubEventGetAllListeners(Hub<T> thisHub) {
				return HubParentService.this.getHubEventService().getAllListeners(thisHub);
			}
			@Override
			public boolean callThreadLocalIsHubMergerChanging() {
				return HubParentService.this.srvcThreadLocal.isHubMergerChanging();
			}
			@Override
			public <T extends OAObject> Hub<T> callHubLinkGetHubWithLink(Hub<T> thisHub, boolean bIncludeCopiedHubs) {
				return HubParentService.this.getHubLinkService().getHubWithLink(thisHub, bIncludeCopiedHubs);
			}
			@Override
			public boolean callObjectChangeGetChanged(OAObject oaObj, int iCascadeRule, OACascade cascade) {
				return HubParentService.this.srvcObjectParent.getOAObjectChangeService().getChanged(oaObj, iCascadeRule, cascade);
			}
			@Override
			public boolean callSyncIsClient() {
				return HubParentService.this.srvcSync.isClient();
			}
			@Override
			public OAObjectInfo callObjectInfoGetOAObjectInfo(Class<?> clazz) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public boolean callObjectInfoIsWeakReferenceable(OAObjectInfo oi) {
				return HubParentService.this.srvcObjectParent.getOAObjectInfoService().isWeakReferenceable(oi);
			}
			@Override
			public OAObject callHubMasterGetMasterObject(Hub<?> hub) {
				return HubParentService.this.getHubMasterService().getMasterObject(hub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public boolean callObjectPropertySetPropertyWeakRef(OAObject oaObj, String name, boolean bToWeakRef, Object value) {
				return HubParentService.this.srvcObjectParent.getOAObjectPropertyService().setPropertyWeakRef(oaObj, name, bToWeakRef, value);
			}
			@Override
			public void callObjectPropertySetReferenceable(OAObject obj, boolean bReferenceable) {
				HubParentService.this.srvcObjectParent.getOAObjectPropertyService().setReferenceable(obj, bReferenceable);
			}
			@Override
			public void callHubDataClearHubChanges(Hub<?> thisHub) {
				HubParentService.this.getHubDataService().clearHubChanges(thisHub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<?> thisDetailHub) {
				return HubParentService.this.getHubDetailService().getLinkInfoFromMasterToDetail(thisDetailHub);
			}
		};
		return srvcHubStatus;
	}
}
