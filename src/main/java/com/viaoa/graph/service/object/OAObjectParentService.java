package com.viaoa.graph.service.object;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.annotation.OAMany;
import com.viaoa.callback.OACallback;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.datasource.OADataSource;
import com.viaoa.filter.OAFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.graph.context.OAContext;
import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.OASyncService;
import com.viaoa.graph.service.hub.HubParentService;
import com.viaoa.graph.sibling.OASiblingHelper;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.auto.HubAutoMatch;
import com.viaoa.hub.auto.HubAutoSequence;
import com.viaoa.hub.sort.HubSortListener;
import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInternalBridge;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARemoteThreadService;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.select.OASelect;
import com.viaoa.serialize.OAObjectSerializer;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.sync.remote.RemoteSessionInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;

/*qqqqqqqqqq
CODEX

 #7 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/service/object/OAObjectParentService.java:373, sync child hooks;
  src/main/java/com/viaoa/graph/service/hub/HubParentService.java:496, sync child hooks
  Exact concern: child-service hooks frequently call srvcSync.getClient(), getRemoteSync(), getRemoteClient(), or
  getRemoteServer() directly. Some calls guard role/null, others do not.
  Why it matters: parent service orchestration should centralize sync role validation. Current hooks rely on each
  child call site to know the correct mode, which is fragile for OA 4.0 runtime invariants.
  Minimal fix: add guarded parent-level helper methods for required sync roles and use them consistently.
  Suggested invariant: GRAPH_CHILD_SYNC_HOOKS_USE_PARENT_ROLE_GUARDS
  Suggested test coverage: child sync hooks in single-user/server/client modes either no-op or fail with documented
  role errors.



*/


/**
 * 
 * qqqqqqqqqqqq Parent that manages all subservices
 * 
 */
public abstract class OAObjectParentService {
	private static final Logger LOG = Logger.getLogger(OAObjectParentService.class.getName());

	private HubService srvcHub;
	private OASyncService srvcSync;
	private OAThreadLocalService srvcThreadLocal;
	private OARemoteThreadService srvcRemoteThread;
	private OAContext context;
	private final OAObjectInternalBridge faBridge = new OAObjectInternalBridge();

    private OAObjectAnnotationService srvcOAObjectAnnotation;
    private OAObjectAutoAddService srvcOAObjectAutoAdd;
    private OAObjectCacheService srvcOAObjectCache;
    private OAObjectChangeService srvcOAObjectChange;
    private OAObjectCallbackService srvcOAObjectCallback;
    private OAObjectCSService srvcOAObjectCS;
    // private OAObjectDatabaseService srvcOAObjectDatabase;
    private OAObjectDeleteService srvcOAObjectDelete;
    private OAObjectDSService srvcOAObjectDS;
    private OAObjectEnumService srvcOAObjectEnum;
    private OAObjectEmptyHubService srvcOAObjectEmptyHub;
    private OAObjectEventService srvcOAObjectEvent;
    private OAObjectFindService srvcOAObjectFind;
    private OAObjectGuidService srvcOAObjectGuid;
    private OAObjectHubService srvcOAObjectHub;
    private OAObjectImportMatchService srvcOAObjectImportMatch;
    private OAObjectInfoService srvcOAObjectInfo; 
    private OAObjectInitializeService srvcOAObjectInitialize; 
    private OAObjectKeyService srvcOAObjectKey;
    private OAObjectLockService srvcOAObjectLock;
//    private OAObjectLogService srvcOAObjectLog;
    private OAObjectPropertyService srvcOAObjectProperty;
    private OAObjectRecurseService srvcOAObjectRecurse;
    private OAObjectReflectService srvcOAObjectReflect;
    private OAObjectSaveService srvcOAObjectSave;
    private OAObjectSchedulerService srvcOAObjectScheduler;
    private OAObjectSerializeService srvcOAObjectSerialize;
    private OAObjectSiblingService srvcOAObjectSibling;
    private OAObjectUniqueService srvcOAObjectUnique;
    
	/**
	 * Reserved property name representing an object's "new" lifecycle state.
	 */
	public static final String WORD_New = "NEW";

	/**
	 * Reserved property name representing an object's "changed" lifecycle state.
	 */
	public static final String WORD_Changed = "CHANGED";
	
	/**
	 * Reserved property name representing an object's "deleted" lifecycle state.
	 */
	public static final String WORD_Deleted = "DELETED";
	

	public void initialize(HubService srvcHub, OASyncService srvcSync, OAThreadLocalService srvcThreadLocal, OARemoteThreadService srvcRemoteThread, OAContext context) {
		if (this.srvcHub != null) throw new IllegalArgumentException("initialize already called");
    	if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
    	if (srvcSync == null) throw new IllegalArgumentException("OASyncService can not be null");
    	if (srvcThreadLocal == null) throw new IllegalArgumentException("OAThreadLocalService can not be null");
    	if (srvcRemoteThread == null) throw new IllegalArgumentException("OARemoteThreadService can not be null");
    	if (context == null) throw new IllegalArgumentException("OAContext can not be null");

		this.srvcHub = srvcHub;
		this.srvcSync = srvcSync;
		this.srvcThreadLocal = srvcThreadLocal;
		this.srvcRemoteThread = srvcRemoteThread;
		this.context = context;
		
	    getOAObjectAnnotationService();
	    getOAObjectAutoAddService();
	    getOAObjectCacheService();
	    getOAObjectChangeService();
	    getOAObjectCallbackService();
	    getOAObjectCSService();
	    // getOAObjectDatabaseService();
	    getOAObjectDeleteService();
	    getOAObjectDSService();
	    getOAObjectEnumService();
	    getOAObjectEmptyHubService();
	    getOAObjectEventService();
	    getOAObjectFindService();
	    getOAObjectGuidService();
	    getOAObjectHubService();
	    getOAObjectImportMatchService();
	    getOAObjectInfoService(); 
	    getOAObjectInitializeService(); 
	    getOAObjectKeyService();
	    getOAObjectLockService();
//	    getOAObjectLogService();
	    getOAObjectPropertyService();
	    getOAObjectRecurseService();
	    getOAObjectReflectService();
	    getOAObjectSaveService();
	    getOAObjectSchedulerService();
	    getOAObjectSerializeService();
	    getOAObjectSiblingService();
	    getOAObjectUniqueService();
	}
	
	protected HubService getHubService() {
		return this.srvcHub;
	}

    public OAObjectAnnotationService getOAObjectAnnotationService() {
    	if (srvcOAObjectAnnotation != null) return srvcOAObjectAnnotation;
		srvcOAObjectAnnotation = new OAObjectAnnotationService(faBridge.getObjectInfoFriendAccess()) {
			@Override
			public Class<?> callReflectGetHubObjectClass(Method method) {
				return OAObjectParentService.this.getOAObjectReflectService().getHubObjectClass(method);
			}

			@Override
			public OACalcInfo callInfoGetCalcInfo(OAObjectInfo oi, String name) {
				return OAObjectParentService.this.getOAObjectInfoService().getOACalcInfo(oi, name);
			}

			@Override
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String name) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(oi, name);
			}
		};
    	return srvcOAObjectAnnotation;
    }

    public OAObjectAutoAddService getOAObjectAutoAddService() {
    	if (srvcOAObjectAutoAdd != null) return srvcOAObjectAutoAdd;
    	
    	srvcOAObjectAutoAdd = new OAObjectAutoAddService(faBridge.getObjectFriendAccess()) {
			@Override
			public Object callObjectReflectGetRawReference(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectReflectService().getRawReference(oaObj, name);
			}
			@Override
			public Object callObjectReflectGetProperty(OAObject obj, String name) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(obj, name);
			}
			@Override
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().getReverseLinkInfo(li);
			}
			@Override
			public OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(oaObj);
			}
			@Override
			public void callObjectEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}
			@Override
			public boolean callThreadLocalGetSendSyncMessages() {
				return OAObjectParentService.this.srvcThreadLocal.getSendSyncMessages();
			}
			@Override
			public void callThreadLocalSetSendSyncMessages(boolean b) {
				OAObjectParentService.this.srvcThreadLocal.setSendSyncMessages(b);
			}
		};
    	return srvcOAObjectAutoAdd;
    }

    
    public OAObjectCacheService getOAObjectCacheService() {
    	if (srvcOAObjectCache != null) return srvcOAObjectCache;
		srvcOAObjectCache = new OAObjectCacheService() {
			@Override
			public OAObjectKey callKeyCreateObjectKey(OAObject obj) {
				return OAObjectParentService.this.getOAObjectKeyService().createObjectKey(obj);
			}
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class<? extends OAObject> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}
			@Override
			public OAObjectKey callKeyCreateObjectKey(Class<? extends OAObject> c, Object... ids) {
				return OAObjectParentService.this.getOAObjectKeyService().createObjectKey(c, ids);
			}
			@Override
			public <T extends OAObject> Hub<T>[] callHubGetHubReferences(T oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}
			@Override
			public OALinkInfo callDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return OAObjectParentService.this.getHubService().getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public boolean callHubSelectRefreshSelect(Hub<?> hub) {
				return OAObjectParentService.this.getHubService().getHubSelectService().refreshSelect(hub);
			}
			@Override
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}
			@Override
			public void callSyncRemoteServerRefreshCache(Class<? extends OAObject> clazz) {
				OAObjectParentService.this.srvcSync.getRemoteServer().refreshCache(clazz);
			}
			@Override
			public int callThreadLocalGetObjectCacheAddMode() {
				return OAObjectParentService.this.srvcThreadLocal.getObjectCacheAddMode();
			}
		};
    	return srvcOAObjectCache; 
    }

    public OAObjectCallbackService getOAObjectCallbackService() {
    	if (srvcOAObjectCallback != null) return srvcOAObjectCallback;
    	
    	srvcOAObjectCallback = new OAObjectCallbackService() {
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class<?>  clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, propertyName);
			}
			@Override
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public <T extends OAObject> Hub<T>[] callHubGetHubReferences(T oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}
			@Override
			public Method callInfoGetMethod(OAObjectInfo oi, String methodName, Class<?> classParam) {
				return OAObjectParentService.this.getOAObjectInfoService().getMethod(oi, methodName, classParam);
			}
			@Override
			public Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount) {
				return OAObjectParentService.this.getOAObjectInfoService().getMethod(oi, methodName, argumentCount);
			}
			@Override
			public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> hub) {
				return OAObjectParentService.this.getHubService().getHubDetailService().getPropertyFromMasterToDetail(hub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return OAObjectParentService.this.getHubService().getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<?> hub) {
				return OAObjectParentService.this.getHubService().getHubDetailService().getLinkInfoFromMasterHubToDetail(hub);
			}
			@Override
			public <T extends OAObject> HubListener<T>[] callHubEventGetAllListeners(Hub<T> hub) {
				return OAObjectParentService.this.getHubService().getHubEventService().getAllListeners(hub);
			}
			@Override
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}
			@Override
			protected OAContext callContextGetContext() {
				return OAObjectParentService.this.context;
			}
    	};
    	return srvcOAObjectCallback;
    }
    
    public OAObjectChangeService getOAObjectChangeService() {
    	if (srvcOAObjectChange != null) return srvcOAObjectChange;
		srvcOAObjectChange = new OAObjectChangeService(faBridge.getObjectFriendAccess()) {
			@Override
			public OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(oaObj);
			}
			@Override
			public boolean callObjectInfoIsMany2Many(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().isMany2Many(li);
			}
			@Override
			public boolean callHubStatusGetChanged(Hub<?> hub, int type, OACascade cascade) {
				return OAObjectParentService.this.srvcHub.getHubStatusService().getChanged(hub, type, cascade);
			}
			@Override
			public Object callObjectReflectGetRawReference(OAObject oaObj, String prop) {
				return OAObjectParentService.this.getOAObjectReflectService().getRawReference(oaObj, prop);
			}
			@Override
			public Object callObjectReflectGetProperty(OAObject oaObj, String prop) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, prop);
			}
			@Override
			public boolean callObjectHubGetChanged(Hub<?> hub, int cascadeRule, OACascade cascade) {
				return OAObjectParentService.this.getOAObjectHubService().getChanged(hub, cascadeRule, cascade);
			}
			@Override
			public boolean callObjectReflectIsReferenceNullOrNotLoaded(OAObject oaObj, String prop) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceNullOrNotLoaded(oaObj, prop);
			}
		};
		return srvcOAObjectChange;
    }
    
    public OAObjectCSService getOAObjectCSService() {
    	if (srvcOAObjectCS != null) return srvcOAObjectCS;
    	
    	srvcOAObjectCS = new OAObjectCSService() {
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class<?> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public boolean callSyncIsSingleUser() {
				return OAObjectParentService.this.srvcSync.isSingleUser();
			}
			@Override
			public boolean callSyncIsServer() {
				return OAObjectParentService.this.srvcSync.isServer();
			}
			@Override
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}
			@Override
			public Object callSyncClientGetDetail(OAObject masterObject, String propertyName) {
				return OAObjectParentService.this.srvcSync.getClient().getDetail(masterObject, propertyName);
			}
			@Override
			public boolean callRemoteSyncPropertyChange(Class<? extends OAObject> objectClass, OAObjectKey origKey, String propertyName, Object newValue, boolean bIsBlob) {
				return OAObjectParentService.this.srvcSync.getRemoteSync().propertyChange(objectClass, origKey, propertyName, newValue, bIsBlob);
			}
			@Override
			public void callSyncClientObjectCreated(OAObject obj) {
				OAObjectParentService.this.srvcSync.getClient().objectCreated(obj);
			}
			@Override
			public void callSyncClientObjectFinalized(UUID guid) {
				OAObjectParentService.this.srvcSync.getClient().objectFinalized(guid);
			}
			@Override
			public <T extends OAObject> void callHubSelectLoadAllData(Hub<T> thisHub, OASelect<T> select) {
				OAObjectParentService.this.srvcHub.getHubSelectService().loadAllData(thisHub, select);
			}
			@Override
			public void callSyncClientUpdateObjectsWithoutHubs(OAObject obj) {
				OASyncClient sc = OAObjectParentService.this.srvcSync.getClient();
				if (sc != null) sc.updateObjectsWithoutHubs(obj);
			}
			@Override
			public <T extends OAObject> T callSyncClientCreateCopy(Class<T> objectClass, OAObjectKey objectKey, String[] excludeProperties) {
				return OAObjectParentService.this.srvcSync.getRemoteClient().createCopy(objectClass, objectKey, excludeProperties);
			}
			@Override
			public boolean callSyncServerSave(Class<? extends OAObject> objectClass, OAObjectKey objectKey, int iCascadeRule) {
				return OAObjectParentService.this.srvcSync.getRemoteServer().save(objectClass, objectKey, iCascadeRule);
			}

			@Override
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String name) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(oi, name);
			}
			@Override
			public boolean callThreadLocalGetSendSyncMessages() {
				return OAObjectParentService.this.srvcThreadLocal.getSendSyncMessages();
			}
			@Override
			public void callThreadLocalSetSendSyncMessages(boolean b) {
		        srvcThreadLocal.setSendSyncMessages(b);
			}
			@Override
			public boolean callThreadLocalIsLoading() {
		        return srvcThreadLocal.isLoading();
			}
			@Override
			public <T extends OAObject> T callSyncServerGetObject(Class<T> clazz, OAObjectKey key) {
				RemoteServerInterface rsi = OAObjectParentService.this.srvcSync.getRemoteServer();
				if (rsi != null) return rsi.getObject(clazz, key);
				return null;
			}
			@Override
			public boolean callSyncSyncServerDelete(Class<? extends OAObject> clazz, OAObjectKey key) {
				RemoteSyncInterface rsi = OAObjectParentService.this.srvcSync.getRemoteSync();
				if (rsi == null) return false;
				rsi.serverDelete(clazz, key);
				return true;
			}
			@Override
			public boolean callSyncSyncClientDelete(Class<? extends OAObject> clazz, OAObjectKey key) {
				RemoteSyncInterface rsi = OAObjectParentService.this.srvcSync.getRemoteSync();
				if (rsi == null) return false;
				rsi.clientDelete(clazz, key);
				return true;
			}
    	};
    	
    	return srvcOAObjectCS;
    }
    
/*qqqqqq    
    public OAObjectDatabaseService getOAObjectDatabaseService() {
    	if (srvcOAObjectDatabase != null) return srvcOAObjectDatabase;
    	srvcOAObjectDatabase = new OAObjectDatabaseService() {
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class<?> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public Class<? extends OAObject> callAnnotationGetHubObjectClass(OAMany annotation, Method method) {
				return OAObjectParentService.this.getOAObjectAnnotationService().getHubObjectClass(annotation, method);
			}
    	};
    	return srvcOAObjectDatabase;
    }
*/    
    public OAObjectDeleteService getOAObjectDeleteService() {
    	if (srvcOAObjectDelete != null) return srvcOAObjectDelete;
        srvcOAObjectDelete = new OAObjectDeleteService(faBridge.getObjectFriendAccess()) {
			@Override
			public OAObjectInfo getOAObjectInfo(Class<?> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public boolean callCSDelete(OAObject obj) {
				return OAObjectParentService.this.getOAObjectCSService().delete(obj);
			}
			@Override
			public void callCSSendDeleteToClients(OAObject obj) {
				OAObjectParentService.this.getOAObjectCSService().sendDeleteToClients(obj);
			}
			@Override
			public void callEventFireBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}
			@Override
			public void callEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}
			@Override
			public String callKeyVerifyKeyChange(OAObject oaObj, OAObjectKey newObjectKey) {
				return OAObjectParentService.this.getOAObjectKeyService().verifyKeyChange(oaObj, newObjectKey);
			}
			@Override
			public OAObject callCacheAdd(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll) {
				return OAObjectParentService.this.getOAObjectCacheService().add(obj, bErrorIfExists, bAddToSelectAll);
			}
			@Override
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}
			@Override
			public Hub[] callObjectHubGetHubReferences(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}
			@Override
			public <T extends OAObject> void callHubEventFireBeforeDeleteEvent(Hub<T> hub, T obj) {
				OAObjectParentService.this.getHubService().getHubEventService().fireBeforeDeleteEvent(hub, obj);
			}
			@Override
			public void callLocalThreadSetDeleting(Object obj, boolean b) {
				OAObjectParentService.this.srvcThreadLocal.setDeleting(obj, b);
			}
			@Override
			public boolean callSyncIsServer() {
				return OAObjectParentService.this.srvcSync.isServer();
			}
			@Override
			public void callHubCSRemoveAllFromHub(Hub<?> thisHub) {
				OAObjectParentService.this.srvcHub.getHubCSService().removeAllFromHub(thisHub);
			}
			@Override
			public void callObjectHubDeleteAll(Hub<?> hub, OACascade cascade) {
				OAObjectParentService.this.getOAObjectHubService().deleteAll(hub, cascade);
			}
			@Override
			public void callHubDSRemoveMany2ManyLinks(Hub<?> hub) {
				OAObjectParentService.this.getHubService().getHubDSService().removeMany2ManyLinks(hub);
			}
			@Override
			public <T extends OAObject> void callCacheCallback(OACallback<T> callback, Class<T> clazz) {
				OAObjectParentService.this.getOAObjectCacheService().callback(callback, clazz);
			}
			@Override
			public boolean callReflectIsReferenceNullOrNotLoadedOrEmptyHub(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceNullOrNotLoadedOrEmptyHub(oaObj, propertyName);
			}
			@Override
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectParentService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}
			@Override
			public void callPropertyRemoveProperty(OAObject oaObj, String name, boolean bFirePropertyChange) {
				OAObjectParentService.this.getOAObjectPropertyService().removeProperty(oaObj, name, bFirePropertyChange);
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}
			@Override
			public void callObjectSetNew(OAObject oaObj, boolean b) {
				OAObjectParentService.this.setNew(oaObj, b);;
			}
			@Override
			public <T extends OAObject> void callHubEventFireAfterDeleteEvent(Hub<T> thisHub, T obj) {
				OAObjectParentService.this.srvcHub.getHubEventService().fireAfterDeleteEvent(thisHub, obj);
			}
			
			public <T extends OAObject> T callHubRemove(Hub<T> thisHub, Object obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll) {
				return OAObjectParentService.this.srvcHub.getHubAddRemoveService().remove(thisHub, obj, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
			}
			@Override
			public void callRemoteTheadStartNextThread() {
				OAObjectParentService.this.srvcRemoteThread.startNextThread();
			}
			@Override
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public OAObject callHubMasterGetMasterObject(Hub<?> hub) {
				return OAObjectParentService.this.getHubService().getHubMasterService().getMasterObject(hub);
			}
			@Override
			public Object callReflectGetReferenceObject(OAObject oaObj, String linkPropertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().getReferenceObject(oaObj, linkPropertyName);
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name);
			}
			@Override
			public boolean callDSSupportsStorage(OAObject obj) {
				OADataSource ds = OAObjectParentService.this.getOAObjectDSService().getDataSource(obj);
				if (ds == null) return false;
				return ds.supportsStorage();
			}
			@Override
			public void callDSUpdateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster) {
				OADataSource ds = OAObjectParentService.this.getOAObjectDSService().getDataSource(masterObject);
				if (ds == null) return;
				ds.updateMany2ManyLinks(masterObject, adds, removes, propFromMaster);
			}
			@Override
			public <T extends OAObject> void callHubDataRemoveFromRemovedList(Hub<T> thisHub, T obj) {
				OAObjectParentService.this.getHubService().getHubDataService().removeFromRemovedList(thisHub, obj);
			}
			@Override
			public <T extends OAObject> Hub<T> callReflectGetReferenceHub(final OAObject oaObj, final String linkPropertyName, String sortOrder, boolean bSequence, Hub<T> hubMatch) {
				return OAObjectParentService.this.getOAObjectReflectService().getReferenceHub(oaObj, linkPropertyName, sortOrder, bSequence, hubMatch);
			}
			@Override
			public void callDSDelete(OAObject obj) {
				OAObjectParentService.this.getOAObjectDSService().delete(obj);
			}
			@Override
			public void callDSRemoveReference(OAObject oaObj, OALinkInfo li) {
				OAObjectParentService.this.getOAObjectDSService().removeReference(oaObj, li);
			}
			@Override
			public Hub<?> callHubGetHub(OAObject oaObj, OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectHubService().getHub(oaObj, li);
			}
			@Override
			public boolean callInfoIsMany2Many(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().isMany2Many(li);
			}
			@Override
			public OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().getReverseLinkInfo(li);
			}
			/*qqqqqqqqq
			@Override
			public void callLogToXmlFile(OAObject oaObj, boolean bSave) {
				OAObjectParentService.this.getOAObjectLogService().logToXmlFile(oaObj, bSave);
			}
			*/
			@Override
			public void callReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				OAObjectParentService.this.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);
			}
        };
    	return srvcOAObjectDelete;
    }
    
    
    public OAObjectDSService getOAObjectDSService() {
    	if (srvcOAObjectDS != null) return srvcOAObjectDS;
    	srvcOAObjectDS = new OAObjectDSService() {
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class<?> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public OAObjectKey callKeyGetKey(OAObject obj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(obj);
			}
			@Override
			public UUID callGuidGetGuid(OAObject obj) {
				return OAObjectParentService.this.getOAObjectGuidService().getGuid(obj);
			}
			@Override
			public OAObjectKey callKeyCreateObjectKey(Class<? extends OAObject> c, Object... ids) {
				return OAObjectParentService.this.getOAObjectKeyService().createObjectKey(c, ids);
			}
		}; 
    	return srvcOAObjectDS;
    }
    
    public OAObjectEmptyHubService getOAObjectEmptyHubService() {
    	if (srvcOAObjectEmptyHub != null) return srvcOAObjectEmptyHub;
    	srvcOAObjectEmptyHub = new OAObjectEmptyHubService() {
			@Override
			public OAObjectKey callKeyGetKey(OAObject obj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(obj);
			}
			@Override
			public void callPropertySetProperty(OAObject obj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().setProperty(obj, name, value);
			}
			@Override
			public void callCacheCallback(OACallback callback) {
				final OAObjectCacheService srvcCache = OAObjectParentService.this.getOAObjectCacheService();
				for (Class<? extends OAObject> c : srvcCache.getClasses()) {
					srvcCache.callback(c, callback);
				}
			}
			@Override
			public String[] callPropertyGetPropertyNames(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectPropertyService().getPropertyNames(oaObj);
			}
			@Override
			public boolean callReflectIsReferenceHubLoadedAndEmpty(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceHubLoadedAndEmpty(oaObj, propertyName);
			}
    	};
    	return srvcOAObjectEmptyHub;
    }
    
    public OAObjectEnumService getOAObjectEnumService() {
    	if (srvcOAObjectEnum != null) return srvcOAObjectEnum;
    	srvcOAObjectEnum = new OAObjectEnumService() {
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
		};
    	return srvcOAObjectEnum;
    }

    public OAObjectEventService getOAObjectEventService() {
    	if (srvcOAObjectEvent != null) return srvcOAObjectEvent;
    	
    	srvcOAObjectEvent = new OAObjectEventService(faBridge.getObjectFriendAccess()) {
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class<?> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public OAObject callUniqueGetUnique(Class<? extends OAObject> clazz, String propertyName, Object uniqueKey, boolean bAutoCreate) {
				return OAObjectParentService.this.getOAObjectUniqueService().getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
			}
			@Override
			public boolean callSyncIsServer() {
				return OAObjectParentService.this.srvcSync.isServer();
			}
			@Override
			public boolean callSyncIsObjectOnServer(OAObject obj) {
				OASyncClient sc = srvcSync.getClient();
				return (sc != null && sc.isObjectOnServer(obj));
			}
			@Override
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}
			@Override
			public void callObjectSetAutoAdd(OAObject oaObj, boolean bEnabled) {
				OAObjectParentService.this.getOAObjectAutoAddService().setAutoAdd(oaObj, bEnabled);
			}
			@Override
			public void callRemoteThreadStartNextThread() {
				OAObjectParentService.this.srvcRemoteThread.startNextThread();
			}
			@Override
			public void callReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				OAObjectParentService.this.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);
			}
			@Override
			public void callReflectSetPrimitiveNull(OAObject oaObj, String propertyName, boolean bNull) {
				OAObjectParentService.this.getOAObjectReflectService().setPrimitiveNull(oaObj, propertyName, bNull);
			}
			@Override
			public boolean callReflectIsReferenceHubLoadedAndNotEmpty(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceHubLoadedAndNotEmpty(oaObj, propertyName);
			}
			@Override
			public boolean callReflectIsReferenceHubLoaded(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceHubLoaded(oaObj, propertyName);
			}
			@Override
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public boolean callReflectGetPrimitiveNull(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().getPrimitiveNull(oaObj, propertyName);
			}
			@Override
			public OAObject callReflectGetObject(Class<? extends OAObject> clazz, Object key) {
				return OAObjectParentService.this.getOAObjectReflectService().getObject(clazz, key);
			}
			@Override
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue, bMustNotExist, bReturnNotExist);
			}
			@Override
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue);
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name);
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}
			@Override
			public void callThreadLocalSetDeleting(Object obj, boolean b) {
				OAObjectParentService.this.srvcThreadLocal.setDeleting(obj, b);
			}
			@Override
			public void callThreadLocalRemoveHubEvent(HubEvent<?> he) {
				OAObjectParentService.this.srvcThreadLocal.removeHubEvent(he);
			}
			@Override
			public boolean callThreadLocalIsLoading() {
				// TODO Auto-generated method stub
				return OAObjectParentService.this.srvcThreadLocal.isLoading();
			}
			@Override
			public boolean callThreadLocalIsDeleting(OAObject obj) {
				return OAObjectParentService.this.srvcThreadLocal.isDeleting(obj);
			}
			@Override
			public boolean callThreadLocalIsDeleting() {
				return OAObjectParentService.this.srvcThreadLocal.isDeleting();
			}
			@Override
			public boolean callThreadLocalGetCreateUndoablePropertyChanges() {
				return OAObjectParentService.this.srvcThreadLocal.getCreateUndoablePropertyChanges();
			}
			@Override
			public void callThreadLocalAddHubEvent(HubEvent<?> he) {
				OAObjectParentService.this.srvcThreadLocal.addHubEvent(he);
			}
			@Override
			public String callKeyVerifyKeyChange(OAObject oaObj, OAObjectKey newObjectKey) {
				return OAObjectParentService.this.getOAObjectKeyService().verifyKeyChange(oaObj, newObjectKey);
			}
			@Override
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectParentService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}
			@Override
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}
			@Override
			public OAObjectKey callKeyCreateChangedObjectKey(Class<? extends OAObject> clazz, OAObjectKey objKey, String propertyName, Object newValue) {
				return OAObjectParentService.this.getOAObjectKeyService().createChangedObjectKey(clazz, objKey, propertyName, newValue);
			}
			@Override
			public boolean callKeyAfterChangedObjectKeyProperty(OAObject oaObj, OAObjectKey okOrig, boolean bVerify) {
				return OAObjectParentService.this.getOAObjectKeyService().afterChangedObjectKeyProperty(oaObj, okOrig, bVerify);
			}
			@Override
			public boolean callRemoteThreadIsRemoteThread() {
				return OAObjectParentService.this.srvcRemoteThread.isRemoteThread();
			}
			@Override
			public Hub<?> callInfoGetRootHub(OAObjectInfo oi) {
				return OAObjectParentService.this.getOAObjectInfoService().getRootHub(oi);
			}
			@Override
			public OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().getReverseLinkInfo(li);
			}
			@Override
			public OALinkInfo callInfoGetRecursiveLinkInfo(OAObjectInfo oi, int type) {
				return OAObjectParentService.this.getOAObjectInfoService().getRecursiveLinkInfo(oi, type);
			}
			@Override
			public OAPropertyInfo callInfoGetPropertyInfo(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getPropertyInfo(oi, propertyName);
			}
			@Override
			public OACalcInfo callInfoGetCalcInfo(OAObjectInfo thisOI, String name) {
				return OAObjectParentService.this.getOAObjectInfoService().getOACalcInfo(thisOI, name);
			}
			@Override
			public Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount) {
				return OAObjectParentService.this.getOAObjectInfoService().getMethod(oi, methodName, argumentCount);
			}
			@Override
			public OALinkInfo callInfoGetLinkToOwner(OAObjectInfo oi) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkToOwner(oi);
			}
			@Override
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}
			@Override
			public <T extends OAObject> Hub<T>[] callHubShareGetAllSharedHubs(Hub<T> thisHub, OAFilter<Hub<T>> filter) {
				return OAObjectParentService.this.srvcHub.getHubShareService().getAllSharedHubs(thisHub, filter);
			}
			@Override
			public boolean callHubIsInHub(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().isInHub(oaObj);
			}
			@Override
			public Hub[] callHubGetHubReferences(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}
			@Override
			public <T extends OAObject> void callHubEventFireBeforePropertyChange(Hub<T> thisHub, T oaObj, String propertyName, Object oldValue, Object newValue) {
				OAObjectParentService.this.srvcHub.getHubEventService().fireBeforePropertyChange(thisHub, oaObj, propertyName, oldValue, newValue);
			}
			@Override
			public <T extends OAObject> void callHubEventFireAfterPropertyChange(Hub<T> thisHub, T oaObj, String propertyName, Object oldValue, Object newValue, OALinkInfo linkInfo) {
				OAObjectParentService.this.srvcHub.getHubEventService().fireAfterPropertyChange(thisHub, oaObj, propertyName, oldValue, newValue, linkInfo);
			}
			@Override
			public <T extends OAObject> void callHubEventFireAfterLoadEvent(Hub<T> thisHub, T oaObj) {
				OAObjectParentService.this.srvcHub.getHubEventService().fireAfterLoadEvent(thisHub, oaObj);
			}
			@Override
			public <T extends OAObject> Hub<T> callHubDetailGetHubWithMasterHub(Hub<T> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubDetailService().getHubWithMasterHub(thisHub);
			}
			@Override
			public <T extends OAObject> T callHubAddRemoveRemove(Hub<T> thisHub, Object obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll) {
				return OAObjectParentService.this.srvcHub.getHubAddRemoveService().remove(thisHub, obj, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
			}
			@Override
			public <T extends OAObject> void callHubAOSetActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
				OAObjectParentService.this.srvcHub.getHubAOService().setActiveObject(thisHub, object, adjustMaster, bUpdateLink, bForce);
			}
			@Override
			public boolean callObjectGetAutoAdd(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectAutoAddService().getAutoAdd(oaObj);
			}
			@Override
			public boolean callDSIsAssigningId(OAObject obj) {
				return OAObjectParentService.this.getOAObjectDSService().isAssigningId(obj);
			}
			@Override
			public OAObjectCallback callCallbackGetVerifyPropertyChangeObjectCallback(int checkType, OAObject oaObj, String propertyName, Object oldValue, Object newValue) {
				return OAObjectParentService.this.getOAObjectCallbackService().getVerifyPropertyChangeObjectCallback(checkType, oaObj, propertyName, oldValue, newValue);
			}
			@Override
			public OAObjectCallback callCallbackGetAllowSubmitObjectCallback(OAObject obj) {
				return OAObjectParentService.this.getOAObjectCallbackService().getAllowSubmitObjectCallback(obj);
			}
			@Override
			public <T extends OAObject> T callCacheGet(Class<T> clazz, OAObjectKey ok) {
				return OAObjectParentService.this.getOAObjectCacheService().get(clazz, ok);
			}
			@Override
			public void callCacheFireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue, boolean bLocalOnly, boolean bSendEvent) {
				OAObjectParentService.this.getOAObjectCacheService().fireAfterPropertyChange(obj, origKey, propertyName, oldValue, newValue, bLocalOnly, bSendEvent);
			}
			@Override
			public boolean callCSIsServer(OAObject obj) {
				return OAObjectParentService.this.getOAObjectCSService().callSyncIsServer();
			}
			@Override
			public void callCSFireBeforePropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue) {
				OAObjectParentService.this.getOAObjectCSService().fireBeforePropertyChange(obj, propertyName, oldValue, newValue);
			}
			@Override
			public boolean callThreadLocalGetSendSyncMessages() {
				return OAObjectParentService.this.srvcThreadLocal.getSendSyncMessages();
			}
			@Override
			public void callThreadLocalSetSendSyncMessages(boolean b) {
				OAObjectParentService.this.srvcThreadLocal.setSendSyncMessages(b);
			}
		}; 
    	
    	return srvcOAObjectEvent;
    }

    public OAObjectFindService getOAObjectFindService() {
    	if (srvcOAObjectFind != null) return srvcOAObjectFind;
    	srvcOAObjectFind = new OAObjectFindService();
    	return srvcOAObjectFind;
    }
    
    public OAObjectGuidService getOAObjectGuidService() {
    	if (srvcOAObjectGuid != null) return srvcOAObjectGuid;
    	srvcOAObjectGuid = new OAObjectGuidService(faBridge.getObjectFriendAccess()) {
		};
    	return srvcOAObjectGuid;
    }

    
    public OAObjectHubService getOAObjectHubService() {
    	if (srvcOAObjectHub != null) return srvcOAObjectHub;
    	
    	srvcOAObjectHub = new OAObjectHubService(faBridge.getObjectFriendAccess()) {
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}
			@Override
			public boolean callThreadLocalGetSendSyncMessages() {
				return OAObjectParentService.this.srvcThreadLocal.getSendSyncMessages();
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name);
			}
			@Override
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}
			@Override
			public boolean callInfoIsMany2Many(OALinkInfo thisLi) {
				return OAObjectParentService.this.getOAObjectInfoService().isMany2Many(thisLi);
			}
			@Override
			public OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return OAObjectParentService.this.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}
			@Override
			public void callHubSaveSaveAll(Hub<?> thisHub, int iCascadeRule, OACascade cascade) {
				OAObjectParentService.this.srvcHub.getHubSaveService().saveAll(thisHub, iCascadeRule, cascade);
			}
			@Override
			public OAObject callHubMasterGetMasterObject(Hub<?> hub) {
				return OAObjectParentService.this.srvcHub.getHubMasterService().getMasterObject(hub);
			}
			@Override
			public boolean callHubStatusGetChanged(Hub<?> thisHub, int iCascadeRule, OACascade cascade) {
				return OAObjectParentService.this.srvcHub.getHubStatusService().getChanged(thisHub, iCascadeRule, cascade);
			}
			@Override
			public void callHubDetailSetMasterObject(Hub<?> thisHub, OAObject masterObject, OALinkInfo liDetailToMaster) {
				// TODO Auto-generated method stub
				OAObjectParentService.this.srvcHub.getHubDetailService().setMasterObject(thisHub, masterObject, liDetailToMaster);
			}
			@Override
			public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub);
			}
			@Override
			public OAObject callHubDetailGetMasterObject(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubDetailService().getMasterObject(thisHub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return OAObjectParentService.this.srvcHub.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public void callHubDeleteDeleteAll(Hub<?> thisHub, OACascade cascade) {
				OAObjectParentService.this.srvcHub.getHubDeleteService().deleteAll(thisHub, cascade);
			}
			@Override
			public boolean callHubDataContainsDirect(Hub<?> hub, Object obj) {
				return OAObjectParentService.this.srvcHub.getHubDataService().containsDirect(hub, obj);
			}
			@Override
			public void callEventSendHubPropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, OALinkInfo linkInfo) {
				OAObjectParentService.this.getOAObjectEventService().sendHubPropertyChange(oaObj, propertyName, oldObj, newObj, linkInfo);
			}
			@Override
			public void callCacheFireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue, boolean bLocalOnly, boolean bSendEvent) {
				OAObjectParentService.this.getOAObjectCacheService().fireAfterPropertyChange(obj, origKey, propertyName, oldValue, newValue, bLocalOnly, bSendEvent);
			}
			@Override
			public void callCSUpdateObjectsWithoutHubs(OAObject obj) {
				OAObjectParentService.this.getOAObjectCSService().updateObjectsWithoutHubs(obj);
			}
		};
    	
    	return srvcOAObjectHub;
    }
    
    public OAObjectImportMatchService getOAObjectImportMatchService() {
    	if (srvcOAObjectImportMatch != null) return srvcOAObjectImportMatch;
    	
    	srvcOAObjectImportMatch = new OAObjectImportMatchService() {
			@Override
			public OAObjectInfo callInfogetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public boolean callThreadLocalSetLoading(boolean b) {
				return OAObjectParentService.this.srvcThreadLocal.setLoading(b);
			}
			@Override
			public boolean callThreadLocalIsLoading() {
				return OAObjectParentService.this.srvcThreadLocal.isLoading();
			}
			@Override
			public <T extends OAObject> T callReflectCreateNewObject(Class<T> clazz) {
				return OAObjectParentService.this.getOAObjectReflectService().createNewObject(clazz);
			}
			@Override
			public <T extends OAObject> T callCacheFind(Class<T> clazz, OAFinder<T, T> finder) {
				return OAObjectParentService.this.getOAObjectCacheService().find(clazz, finder);
			}
		}; 
    	return srvcOAObjectImportMatch;
    }
    
    public OAObjectInfoService getOAObjectInfoService() {
    	if (srvcOAObjectInfo != null) return srvcOAObjectInfo;
    	
    	srvcOAObjectInfo = new OAObjectInfoService(faBridge.getObjectFriendAccess(), faBridge.getObjectInfoFriendAccess()) {
			@Override
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}
			@Override
			public Object callReflectGetRawReference(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectReflectService().getRawReference(oaObj, name);
			}
			@Override
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public void callAnnotationUpdateLinkFkeys(OAObjectInfo oi) {
				OAObjectParentService.this.getOAObjectAnnotationService().updateLinkFkeys(oi);
			}
			@Override
			public void callAnnotationUpdateImportMatches(OAObjectInfo oi) {
				OAObjectParentService.this.getOAObjectAnnotationService().updateImportMatches(oi);
			}
			@Override
			public void callAnnotationUpdate2(OAObjectInfo oi, Class clazz) {
				OAObjectParentService.this.getOAObjectAnnotationService().update2(oi, clazz);
			}
			@Override
			public void callAnnotationUpdate(OAObjectInfo oi, Class clazz) {
				OAObjectParentService.this.getOAObjectAnnotationService().update(oi, clazz);
			}
		}; 
    	return srvcOAObjectInfo;
    }
    
    public OAObjectInitializeService getOAObjectInitializeService() {
    	if (srvcOAObjectInitialize != null) return srvcOAObjectInitialize;
    	
    	srvcOAObjectInitialize = new OAObjectInitializeService(faBridge.getObjectFriendAccess()) {
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public boolean callThreadLocalSetLoading(boolean b) {
				return OAObjectParentService.this.srvcThreadLocal.setLoading(b);
			}
			@Override
			public boolean callThreadLocalIsLoading() {
				return OAObjectParentService.this.srvcThreadLocal.isLoading();
			}
			@Override
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}
			@Override
			public void callSyncClientObjectCreated(OAObject obj) {
				OAObjectParentService.this.srvcSync.getClient().objectCreated(obj);
			}
			@Override
			public void callReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				OAObjectParentService.this.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);
			}
			@Override
			public void callPropertyUnsafeAddProperty(OAObject oaObj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().unsafeAddProperty(oaObj, name, value);
			}
			@Override
			public boolean callInfoIsOne2One(OALinkInfo thisLi) {
				return OAObjectParentService.this.getOAObjectInfoService().isOne2One(thisLi);
			}
			@Override
			public UUID callGuidGetGuid(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectGuidService().getGuid(oaObj);
			}
			@Override
			public void callGuidAssignNewGuid(OAObject obj) {
				OAObjectParentService.this.getOAObjectGuidService().assignNewGuid(obj);
			}
			@Override
			public void callGuidAssignGuid(OAObject obj) {
				OAObjectParentService.this.getOAObjectGuidService().assignGuid(obj);
			}
			@Override
			public boolean callDSGetAssignIdOnCreate(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectDSService().getAssignIdOnCreate(oaObj);
			}
			@Override
			public void callDSAssignId(OAObject oaObj) {
				OAObjectParentService.this.getOAObjectDSService().assignId(oaObj);
			}
			
			@Override
			public <T extends OAObject> void callCacheFireAfterLoadEvent(T obj) {
				OAObjectParentService.this.getOAObjectCacheService().fireAfterLoadEvent(obj);
			}
			@Override
			public void callCacheAddToSelectAllHubs(OAObject obj) {
				OAObjectParentService.this.getOAObjectCacheService().addToSelectAllHubs(obj);
			}
			@Override
			public OAObject callCacheAdd(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll) {
				return OAObjectParentService.this.getOAObjectCacheService().add(obj, bErrorIfExists, bAddToSelectAll);
			}
			@Override
			public OAObject callContextGetContextObject() {
				OAObject objx = context.getContextObject();
				return objx;
			}
		};
    	return srvcOAObjectInitialize;
    }

    public OAObjectKeyService getOAObjectKeyService() {
    	if (srvcOAObjectKey != null) return srvcOAObjectKey;
    	
    	srvcOAObjectKey = new OAObjectKeyService() {
			@Override
			public OAObjectInfo callInfogetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public boolean callThreadLocalIsLoading() {
				return OAObjectParentService.this.srvcThreadLocal.isLoading();
			}
			@Override
			public int callThreadLocalGetObjectCacheAddMode() {
				return OAObjectParentService.this.srvcThreadLocal.getObjectCacheAddMode();
			}
			@Override
			public boolean callReflectIsReferenceObjectLoadedAndNotEmpty(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceObjectLoadedAndNotEmpty(oaObj, propertyName);
			}
			@Override
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public boolean callIsRemoteThread() {
				return OAObjectParentService.this.srvcRemoteThread.isRemoteThread();
			}
			@Override
			public boolean callInfoIsIdProperty(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().isIdProperty(oi, propertyName);
			}
			@Override
			public Class callInfoGetPropertyClass(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getPropertyClass(oi, propertyName);
			}
			@Override
			public Object[] callObjectInfoGetPropertyIdValues(OAObject obj) {
				return OAObjectParentService.this.getOAObjectInfoService().getPropertyIdValues(obj);
			}
			@Override
			public boolean callDSIsAssigningId(OAObject obj) {
				return OAObjectParentService.this.getOAObjectDSService().isAssigningId(obj);
			}
			@Override
			public <T extends OAObject> T callDSGetObject(OAObjectInfo oi, Class<T> clazz, OAObjectKey key) {
				return OAObjectParentService.this.getOAObjectDSService().getObject(oi, clazz, key);
			}
			@Override
			public boolean callDSAllowIdChange(Class<? extends OAObject> c) {
				return OAObjectParentService.this.getOAObjectDSService().allowIdChange(c);
			}
			@Override
			public void callCacheRemoveObject(OAObject obj) {
				OAObjectParentService.this.getOAObjectCacheService().removeObject(obj);;
			}
			@Override
			public void callCachePropertyKeyValueChanged(OAObject obj) {
				OAObjectParentService.this.getOAObjectCacheService().propertyKeyValueChanged(obj);;
			}
			@Override
			public <T extends OAObject> T callCacheGet(Class<T> clazz, Object key) {
				return OAObjectParentService.this.getOAObjectCacheService().get(clazz, key);
			}
			@Override
			public <T extends OAObject> T callCacheGet(Class<T> clazz, OAObjectKey ok) {
				return OAObjectParentService.this.getOAObjectCacheService().get(clazz, ok);
			}
			@Override
			public boolean callCSIsSingleUser(OAObject obj) {
				return OAObjectParentService.this.getOAObjectCSService().isSingleUser(obj);
			}
			@Override
			public boolean callCSIsServer(OAObject obj) {
				return OAObjectParentService.this.getOAObjectCSService().isServer(obj);
			}
			@Override
			public boolean callCSIsClient(OAObject obj) {
				return OAObjectParentService.this.getOAObjectCSService().isClient(obj);
			}
			@Override
			public OAObject callCSGetServerObject(Class clazz, OAObjectKey key) {
				return OAObjectParentService.this.getOAObjectCSService().callSyncServerGetObject(clazz, key);
			}
		};
    	return srvcOAObjectKey;
    }
    
    public OAObjectLockService getOAObjectLockService() {
    	if (srvcOAObjectLock != null) return srvcOAObjectLock;
    	
    	srvcOAObjectLock = new OAObjectLockService() {
			@Override
			public boolean callSyncIsServer() {
				return OAObjectParentService.this.srvcSync.isServer();
			}
			@Override
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}
			@Override
			public boolean callSyncSetLock(Class<? extends OAObject> objectClass, OAObjectKey objectKey, boolean bLock) {
				RemoteSessionInterface rs = OAObjectParentService.this.srvcSync.getRemoteSession();
				if (rs == null) return false;
				return rs.setLock(objectClass, objectKey, bLock);
			}
			@Override
			public boolean callSyncIsLocked(Class<? extends OAObject> objectClass, OAObjectKey objectKey) {
				RemoteSessionInterface rs = OAObjectParentService.this.srvcSync.getRemoteSession();
				if (rs == null) return false;
				return rs.isLocked(objectClass, objectKey);
			}
			@Override
			public void callRemoteThreadStartNextThread() {
				OAObjectParentService.this.srvcRemoteThread.startNextThread();
			}
		};
    	return srvcOAObjectLock;
    }

/*qqqqqqq    
    public OAObjectLogService getOAObjectLogService() {
    	if (srvcOAObjectLog != null) return srvcOAObjectLog;
    	srvcOAObjectLog = new OAObjectLogService() {
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().getReverseLinkInfo(li);
			}
			@Override
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}
		};
    	return srvcOAObjectLog;
    }
*/    

    public OAObjectPropertyService getOAObjectPropertyService() {
    	if (srvcOAObjectProperty != null) return srvcOAObjectProperty; 
    	srvcOAObjectProperty = new OAObjectPropertyService(faBridge.getObjectFriendAccess()) {
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public boolean callSyncIsServer() {
				return OAObjectParentService.this.srvcSync.isServer();
			}
			@Override
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectParentService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}
			@Override
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}
			@Override
			public boolean callInfoIsWeakReferenceable(OAObjectInfo oi) {
				return OAObjectParentService.this.getOAObjectInfoService().isWeakReferenceable(oi);
			}
			@Override
			public OALinkInfo callInfoGetLinkInfo(Class clazz, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(clazz, propertyName);
			}
			@Override
			public void callHubSetMasterObject(Hub<?> hub, OAObject oaObj, String nameFromMasterToDetail) {
				OAObjectParentService.this.getOAObjectHubService().setMasterObject(hub, oaObj, nameFromMasterToDetail);
			}
			@Override
			public <T extends OAObject> T callCacheGet(Class<T> clazz, OAObjectKey ok) {
				return OAObjectParentService.this.getOAObjectCacheService().get(clazz, ok);
			}
		};
    	return srvcOAObjectProperty;
    }

    public OAObjectRecurseService getOAObjectRecurseService() {
    	if (srvcOAObjectRecurse != null) return srvcOAObjectRecurse;
    	
    	srvcOAObjectRecurse = new OAObjectRecurseService() {
			@Override
			public Object callObjectReflectGetProperty(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, name);
			}
			@Override
			public OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(oaObj);
			}
		};
    	
    	return srvcOAObjectRecurse;
    }
    
    public OAObjectReflectService getOAObjectReflectService() {
    	if (srvcOAObjectReflect != null) return srvcOAObjectReflect;
    	srvcOAObjectReflect = new OAObjectReflectService(faBridge.getObjectFriendAccess()) {
			@Override
			public OAObjectInfo getOAObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public Hub<?> getCSGetServerReferenceHub(OAObject oaObj, String linkPropertyName) {
				return OAObjectParentService.this.getOAObjectCSService().getServerReferenceHub(oaObj, linkPropertyName);
			}
			@Override
			public boolean callThreadLocalSetLoading(boolean b) {
				return OAObjectParentService.this.srvcThreadLocal.setLoading(b);
			}
			@Override
			public void callThreadLocalRemoveSiblingHelper(OASiblingHelper sh) {
				OAObjectParentService.this.srvcThreadLocal.removeSiblingHelper(sh);;
			}
			@Override
			public boolean callThreadLocalIsLoading() {
				return OAObjectParentService.this.srvcThreadLocal.isLoading();
			}
			@Override
			public boolean callThreadLocalIsDeleting() {
				return OAObjectParentService.this.srvcThreadLocal.isDeleting();
			}
			@Override
			public int callThreadLocalGetObjectCacheAddMode() {
				return OAObjectParentService.this.srvcThreadLocal.getObjectCacheAddMode();
			}
			@Override
			public boolean callThreadLocalAddSiblingHelper(OASiblingHelper sh) {
				return OAObjectParentService.this.srvcThreadLocal.addSiblingHelper(sh);
			}
			@Override
			public boolean callSyncIsObjectOnServer(OAObject obj) {
				return OAObjectParentService.this.srvcSync.getClient().isObjectOnServer(obj);
			}
			@Override
			public void callSiblingOnGetObjectReference(OAObject obj, String linkPropertyName) {
				OAObjectParentService.this.getOAObjectSiblingService().onGetObjectReference(obj, linkPropertyName);;
			}
			@Override
			public OAObjectKey[] callSiblingGetSiblings(OAObject mainObject, String property, int maxAmount, ConcurrentHashMap<UUID, Boolean> hmIgnore) {
				return OAObjectParentService.this.getOAObjectSiblingService().getSiblings(mainObject, property, maxAmount, hmIgnore);
			}
			@Override
			public void callPropertyUnsafeSetProperty(OAObject oaObj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().unsafeSetProperty(oaObj, name, value);
			}
			@Override
			public boolean callLockSetPropertyLock(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectLockService().setPropertyLock(oaObj, name);
			}
			@Override
			public void callPropertySetPropertyHubIfNotSet(OAObject oaObj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().setPropertyHubIfNotSet(oaObj, name, value);			
			}
			@Override
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue, bMustNotExist, bReturnNotExist);
			}
			@Override
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue);
			}
			@Override
			public void callPropertySetProperty(OAObject oaObj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().setProperty(oaObj, name, value);
			}
			@Override
			public void callLockReleasePropertyLock(OAObject oaObj, String name) {
				OAObjectParentService.this.getOAObjectLockService().releasePropertyLock(oaObj, name);
			}
			@Override
			public boolean callLockIsPropertyLocked(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectLockService().isPropertyLocked(oaObj, name);
			}
			@Override
			public boolean callPropertyIsPropertyLoaded(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectPropertyService().isPropertyLoaded(oaObj, name);
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}
			@Override
			public boolean callLockAttemptPropertyLock(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectLockService().attemptPropertyLock(oaObj, name);
			}
			@Override
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectParentService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}
			@Override
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}
			@Override
			public OAObjectKey callKeyCreateObjectKey(Class c, Object... ids) {
				return OAObjectParentService.this.getOAObjectKeyService().createObjectKey(c, ids);
			}
			@Override
			public boolean callRemoteThreadIsRemoteThread() {
				return OAObjectParentService.this.srvcRemoteThread.isRemoteThread();
			}
			@Override
			public void callInitializeInitialize(OAObject oaObj, OAObjectInfo oi, boolean bInitializeNulls, boolean bInitializeWithDS, boolean bAddToCache, boolean bInitializeWithCS, boolean bSetChangedToFalse) {
				OAObjectParentService.this.getOAObjectInitializeService().initialize(oaObj, oi, bInitializeNulls, bInitializeWithDS, bAddToCache, bInitializeWithCS, bSetChangedToFalse);
			}
			@Override
			public void callInfoSetPrimitiveNull(OAObject oaObj, String propertyName, boolean bSetToNull) {
				OAObjectParentService.this.getOAObjectInfoService().setPrimitiveNull(oaObj, propertyName, bSetToNull);				
			}
			@Override
			public boolean callInfoIsPrimitiveNull(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().isPrimitiveNull(oaObj, propertyName);
			}
			@Override
			public boolean callInfoIsOne2One(OALinkInfo thisLi) {
				return OAObjectParentService.this.getOAObjectInfoService().isOne2One(thisLi);
			}
			@Override
			public OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().getReverseLinkInfo(li);
			}
			@Override
			public OALinkInfo callInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type) {
				return OAObjectParentService.this.getOAObjectInfoService().getRecursiveLinkInfo(thisOI, type);
			}
			@Override
			public Method callInfoGetMethod(OAObjectInfo oi, String methodName, Class classParam) {
				return getOAObjectInfoService().getMethod(oi, methodName, classParam);
			}
			@Override
			public Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount) {
				return OAObjectParentService.this.getOAObjectInfoService().getMethod(oi, methodName, argumentCount);
			}
			@Override
			public OALinkInfo callInfoGetLinkInfo(Class clazz, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(clazz, propertyName);
			}
			@Override
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}
			@Override
			public boolean callInfoCacheHub(OALinkInfo li, Hub<?> hub) {
				return OAObjectParentService.this.getOAObjectInfoService().cacheHub(li, hub);
			}
			@Override
			public void callHubSortSort(Hub<?> thisHub, String propertyPaths, boolean bAscending, Comparator comp, boolean bAlreadySortedAndLocalOnly) {
				OAObjectParentService.this.srvcHub.getHubSortService().sort(thisHub, propertyPaths, bAscending, comp, bAlreadySortedAndLocalOnly);				
			}
			@Override
			public boolean callHubSortIsSorted(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubSortService().isSorted(thisHub);
			}
			@Override
			public String callHubSortGetSortProperty(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubSortService().getSortProperty(thisHub);
			}
			@Override
			public HubSortListener callHubSortGetSortListener(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubSortService().getSortListener(thisHub);
			}
			@Override
			public boolean callHubSortGetSortAsc(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubSortService().getSortAsc(thisHub);
			}
			@Override
			public boolean callHubShareIsUsingSameSharedHub(Hub<?> hub1, Hub<?> hub2) {
				return OAObjectParentService.this.srvcHub.getHubShareService().isUsingSameSharedHub(hub1, hub2);
			}
			@Override
			public boolean callHubShareIsUsingSameSharedAO(Hub<?> hub1, Hub<?> hub2, boolean bIncludeFilteredHubs) {
				return OAObjectParentService.this.srvcHub.getHubShareService().isUsingSameSharedAO(hub1, hub2, bIncludeFilteredHubs);
			}
			@Override
			public void callHubSelectLoadAllData(Hub<?> thisHub, OASelect select) {
				OAObjectParentService.this.srvcHub.getHubSelectService().loadAllData(thisHub, select);				
			}
			@Override
			public boolean callHubLinkGetLinkedOnPos(Hub<?> thisHub, boolean bIncludeCopiedHubs) {
				return OAObjectParentService.this.srvcHub.getHubLinkService().getLinkedOnPos(thisHub, bIncludeCopiedHubs);
			}
			@Override
			public String callHubLinkGetLinkToProperty(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubLinkService().getLinkToProperty(thisHub);
			}
			@Override
			public Hub<?> callHubLinkGetLinkToHub(Hub<?> thisHub, boolean bIncludeCopiedHubs) {
				return OAObjectParentService.this.srvcHub.getHubLinkService().getLinkToHub(thisHub, bIncludeCopiedHubs);
			}
			@Override
			public String callHubLinkGetLinkHubPath(Hub<?> thisHub, boolean bIncludeCopiedHubs) {
				return OAObjectParentService.this.srvcHub.getHubLinkService().getLinkHubPath(thisHub, bIncludeCopiedHubs);
			}
			@Override
			public String callHubLinkGetLinkFromProperty(Hub<?> thisHub, boolean bIncludeCopiedHubs) {
				return OAObjectParentService.this.srvcHub.getHubLinkService().getLinkFromProperty(thisHub, bIncludeCopiedHubs);
			}
			@Override
			public OAObject callHubMasterGetMasterObject(Hub<?> hub) {
				return OAObjectParentService.this.srvcHub.getHubMasterService().getMasterObject(hub);
			}
			@Override
			public Hub[] callHubGetHubReferences(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}
			@Override
			public Hub<?> callHubGetHub(OAObject oaObj, OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectHubService().getHub(oaObj, li);
			}
			@Override
			public HubAutoSequence callHubGetAutoSequence(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubSequenceService().getAutoSequence(thisHub);
			}
			@Override
			public HubAutoMatch callHubGetAutoMatch(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubAutoMatchService().getAutoMatch(thisHub);
			}
			@Override
			public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub);
			}
			@Override
			public String callHubDetailGetPropertyFromDetailToMaster(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubDetailService().getPropertyFromDetailToMaster(thisHub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return OAObjectParentService.this.srvcHub.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public void callHubDataResizeToFit(Hub<?> thisHub) {
				OAObjectParentService.this.srvcHub.getHubDataService().resizeToFit(thisHub);				
			}
			@Override
			public UUID callGuidGetGuid(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectGuidService().getGuid(oaObj);
			}
			@Override
			public void callEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);				
			}
			@Override
			public void callEventFireBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);				
			}
			@Override
			public <T extends OAObject> T callDSGetObject(OAObjectInfo oi, Class<T> clazz, OAObjectKey key) {
				return OAObjectParentService.this.getOAObjectDSService().getObject(oi, clazz, key);
			}
			@Override
			public <T extends OAObject> T callDSGetObject(Class<T> clazz, OAObjectKey key) {
				return OAObjectParentService.this.getOAObjectDSService().getObject(clazz, key);
			}
			@Override
			public OADataSource callDSGetDataSource(Class c) {
				return OAObjectParentService.this.getOAObjectDSService().getDataSource(c);
			}
			@Override
			public <T extends OAObject> T callCacheGet(Class<T> clazz, OAObjectKey ok) {
				return OAObjectParentService.this.getOAObjectCacheService().get(clazz, ok);
			}
			@Override
			public OAObject callCacheAdd(OAObject obj) {
				return OAObjectParentService.this.getOAObjectCacheService().add(obj);
			}
			@Override
			public boolean callCSLoadReferenceHubDataOnServer(Hub<?> thisHub, OASelect select) {
				return OAObjectParentService.this.getOAObjectCSService().loadReferenceHubDataOnServer(thisHub, select);
			}
			@Override
			public boolean callCSIsServer() {
				return OAObjectParentService.this.getOAObjectCSService().callSyncIsServer();
			}
			@Override
			public boolean callCSIsClient() {
				return OAObjectParentService.this.getOAObjectCSService().callSyncIsClient();
			}
			@Override
			public byte[] callCSGetServerReferenceBlob(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectCSService().getServerReferenceBlob(oaObj, propertyName);
			}
			@Override
			public Object callCSGetServerReference(OAObject oaObj, String linkPropertyName) {
				return OAObjectParentService.this.getOAObjectCSService().getServerReference(oaObj, linkPropertyName);
			}
			@Override
			public OAObject callCSGetServerObject(Class clazz, OAObjectKey key) {
				return OAObjectParentService.this.getOAObjectCSService().callSyncServerGetObject(clazz, key);
			}
			@Override
			public OAObject callCSCreateCopy(OAObject oaObj, String[] excludeProperties) {
				return OAObjectParentService.this.getOAObjectCSService().createCopy(oaObj, excludeProperties);
			}
			@Override
			public boolean callThreadLocalGetSendSyncMessages() {
				return OAObjectParentService.this.srvcThreadLocal.getSendSyncMessages();
			}
			@Override
			public void callThreadLocalSetSendSyncMessages(boolean b) {
				OAObjectParentService.this.srvcThreadLocal.setSendSyncMessages(b);
			}
			@Override
			public boolean callThreadLocalGetLoading() {
    			return	OAObjectParentService.this.srvcThreadLocal.isLoading();
			}
		}; 
    	return srvcOAObjectReflect;
    }

    public OAObjectSaveService getOAObjectSaveService() {
    	if (srvcOAObjectSave != null) return srvcOAObjectSave;
    	srvcOAObjectSave = new OAObjectSaveService(faBridge.getObjectFriendAccess()) {
			@Override
			public boolean callCSIsClient(OAObject obj) {
				return OAObjectParentService.this.getOAObjectCSService().isClient(obj);
			}
			@Override
			public boolean callCSSave(OAObject oaObj, int iCascadeRule) {
				return OAObjectParentService.this.getOAObjectCSService().save(oaObj, iCascadeRule);
			}
			@Override
			public Hub[] callHubGetHubReferences(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}
			@Override
			public OAObjectInfo callInfoGetObjectInfo(OAObject obj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(obj);
			}
			@Override
			public boolean callReflectIsReferenceNullOrNotLoaded(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceNullOrNotLoaded(oaObj, propertyName);
			}
			@Override
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public OAObjectInfo callInfoGetOAObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(getClass());
			}
			@Override
			public void callDSSaveWithoutReferences(OAObject oaObj) {
				OAObjectParentService.this.getOAObjectDSService().saveWithoutReferences(oaObj);
			}
			@Override
			public void callObjectSetNew(OAObject oaObj, boolean b) {
				OAObjectParentService.this.setNew(oaObj, b);
			}
			@Override
			public void callHubSaveAll(Hub<?> hub, int iCascadeRule, OACascade cascade) {
				OAObjectParentService.this.getOAObjectHubService().saveAll(hub, iCascadeRule, cascade);
			}
			@Override
			public Object callReflectGetRawReference(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectReflectService().getRawReference(oaObj, name);
			}
			@Override
			public void callDSSave(OAObject oaObj) {
				OAObjectParentService.this.getOAObjectDSService().save(oaObj);
			}
/*qqqqq			
			@Override
			public void callLogLogToXmlFile(OAObject oaObj, boolean bSave) {
				OAObjectParentService.this.getOAObjectLogService().logToXmlFile(oaObj, bSave);
			}
*/			
			@Override
			public <T extends OAObject> void callHubEventFireBeforeSaveEvent(Hub<T> thisHub, T obj) {
				OAObjectParentService.this.srvcHub.getHubEventService().fireBeforeSaveEvent(thisHub, obj);
			}
			@Override
			public <T extends OAObject> void callHubEventFireAfterSaveEvent(Hub<T> thisHub, T obj) {
				OAObjectParentService.this.srvcHub.getHubEventService().fireAfterSaveEvent(thisHub, obj);
			}
			@Override
			public boolean callThreadLocalIsDeleting() {
				return OAObjectParentService.this.srvcThreadLocal.isDeleting();
			}
			@Override
			protected boolean callHubIsInHubWithMaster(OAObject thisObj) {
				return OAObjectParentService.this.getOAObjectHubService().isInHubWithMaster(thisObj);
			}
			@Override
			protected void callRemoteSyncAddNewToCache(OAObjectSerializer<? extends OAObject> oos) {
				OAObjectParentService.this.srvcSync.getRemoteSync().addNewToCache(oos);
			}
    	};
    	return srvcOAObjectSave;
    }
    
    public OAObjectSchedulerService getOAObjectSchedulerService() {
    	if (srvcOAObjectScheduler != null) return srvcOAObjectScheduler;
    	srvcOAObjectScheduler = new OAObjectSchedulerService() {
			@Override
			public OAObjectInfo callInfoGetObjectInfo(OAObject obj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(obj);
			}
		};
    	return srvcOAObjectScheduler;
    }
    

    public OAObjectSerializeService getOAObjectSerializeService() {
    	if (srvcOAObjectSerialize != null) return srvcOAObjectSerialize;
    	
    	srvcOAObjectSerialize = new OAObjectSerializeService(faBridge.getObjectSerializerFriendAccess()) {
			@Override
			public void callGuidSetGuid(OAObject oaObj, UUID guid) {
				OAObjectParentService.this.getOAObjectGuidService().setGuid(oaObj, guid);
			}
			@Override
			public OAObjectInfo callInfoGetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public void callPropertyUnsafeSetPropertyIfEmpty(OAObject oaObj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().unsafeSetPropertyIfEmpty(oaObj, name, value);				
			}
			@Override
			public UUID callGuidGetGuid(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectGuidService().getGuid(oaObj);
			}
			@Override
			public void callGuiAssignGuid(OAObject obj) {
				OAObjectParentService.this.getOAObjectGuidService().assignGuid(obj);				
			}
			@Override
			public OAObjectInfo callInfoGetObjectInfo(OAObject obj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(obj);
			}
			@Override
			public OAObject callCacheAdd(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll, boolean bSendAddEventInAnotherThread) {
				return OAObjectParentService.this.getOAObjectCacheService().add(obj, bErrorIfExists, bAddToSelectAll, bSendAddEventInAnotherThread);
			}
			@Override
			public Object[] callGetProperties(OAObject obj) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperties(obj);
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}
			@Override
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}
			@Override
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectParentService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}
			@Override
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue);
			}
			@Override
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}
			@Override
			public boolean callPropertyAttemptPropertyLock(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectLockService().attemptPropertyLock(oaObj, name);
			}
			@Override
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue, bMustNotExist, bReturnNotExist);
			}
			@Override
			public void callPropertyReleasePropertyLock(OAObject oaObj, String name) {
				OAObjectParentService.this.getOAObjectLockService().releasePropertyLock(oaObj, name);				
			}
			@Override
			public boolean callInfoCacheHub(OALinkInfo li, Hub<?> hub) {
				return OAObjectParentService.this.getOAObjectInfoService().cacheHub(li, hub);
			}
			@Override
			public boolean callCSIsClient() {
				return OAObjectParentService.this.getOAObjectCSService().callSyncIsClient();
			}
			@Override
			public int callHubSerializeReplaceObject(Hub<?> thisHub, OAObject objFrom, OAObject objTo) {
				return OAObjectParentService.this.getOAObjectSerializeService().callHubSerializeReplaceObject(thisHub, objFrom, objTo);
			}
			@Override
			public boolean callHubSerializeIsResolved(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubSerializeService().isResolved(thisHub);
			}
			@Override
			public <T extends OAObject> void callHubSerializeReplaceMasterObject(Hub<T> thisHub, T objFrom, T objTo) {
				OAObjectParentService.this.srvcHub.getHubSerializeService().replaceMasterObject(thisHub, objFrom, objTo);				
			}
			@Override
			public HubAutoMatch callHubGetAutoMatch(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHub.getHubAutoMatchService().getAutoMatch(thisHub);
			}
			@Override
			public boolean callSyncClientIsObjectOnServer(OAObject obj) {
				OASyncClient sc = OAObjectParentService.this.srvcSync.getClient();
				if (sc == null) return false;
				return sc.isObjectOnServer(obj);
			}
			@Override
			public void callSyncClientObjectSentToServer(OAObject obj) {
				OASyncClient sc = OAObjectParentService.this.srvcSync.getClient();
				if (sc != null) sc.objectSentToServer(obj);				
			}
			@Override
			public OAObjectSerializer callThreadLocalGetCurrentObjectSerializer() {
				return OAObjectParentService.this.srvcThreadLocal.getCurrentObjectSerializer();
			}
    	};
    	return srvcOAObjectSerialize;
    }
    
    
    public OAObjectSiblingService getOAObjectSiblingService() {
    	if (srvcOAObjectSibling != null) return srvcOAObjectSibling;
    	
    	srvcOAObjectSibling = new OAObjectSiblingService() {
			@Override
			public List<OASiblingHelper<?>> callThreadLocalGetSiblingHelpers() {
				return OAObjectParentService.this.srvcThreadLocal.getSiblingHelpers();
			}
			@Override
			public int callThreadLocalGetAndIncrementGetSiblingCalledCount() {
				return OAObjectParentService.this.srvcThreadLocal.getAndIncrementGetSiblingCalledCount();
			}
			@Override
			public void callThreadLocalClearGetSiblingCalledCount() {
				OAObjectParentService.this.srvcThreadLocal.clearGetSiblingCalledCount();
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}
			@Override
			public OALinkInfo callInfoGetLinkInfo(Class fromClass, Class toClass) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(fromClass, toClass);
			}
			@Override
			public OALinkInfo callInfoGetLinkInfo(Class clazz, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(clazz, propertyName);
			}
			@Override
			public Hub[] callHubGetHubReferences(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromMasterToDetail(Hub<?> thisDetailHub) {
				return OAObjectParentService.this.srvcHub.getHubDetailService().getLinkInfoFromMasterToDetail(thisDetailHub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<?> thisDetailHub) {
				return OAObjectParentService.this.srvcHub.getHubDetailService().getLinkInfoFromMasterHubToDetail(thisDetailHub);
			}
			@Override
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return OAObjectParentService.this.srvcHub.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public <T extends OAObject> T callCacheGet(Class<T> clazz, OAObjectKey ok) {
				return OAObjectParentService.this.getOAObjectCacheService().get(clazz, ok);
			}
		}; 
    	return srvcOAObjectSibling;
    }
    
    public OAObjectUniqueService getOAObjectUniqueService() {
    	if (srvcOAObjectUnique != null) return srvcOAObjectUnique;
    	srvcOAObjectUnique = new OAObjectUniqueService() {
			@Override
			public boolean callThreadLocalSetLoading(boolean b) {
				return OAObjectParentService.this.srvcThreadLocal.setLoading(b);				
			}
			@Override
			public OAObject callSyncClientGetUnique(Class<? extends OAObject> clazz, String propertyName, Object uniqueKey, boolean bAutoCreate) {
				OASyncClient sc = OAObjectParentService.this.srvcSync.getClient();
				RemoteServerInterface rsi;
				try {
					rsi = sc.getRemoteServer();
				}
				catch (Exception e) {
					throw new RuntimeException("Could not get remote server ", e);
				}
				return rsi.getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
			}
			@Override
			public Object callReflectCreateNewObject(Class clazz) {
				return OAObjectParentService.this.getOAObjectReflectService().createNewObject(clazz);
			}
			@Override
			public Object callCacheFind(Class clazz, String propertyPath, Object findObject) {
				return OAObjectParentService.this.getOAObjectCacheService().find(clazz, propertyPath, findObject);
			}
			@Override
			public boolean callCSIsClient() {
				return OAObjectParentService.this.getOAObjectCSService().callSyncIsClient();
			}
		};
    	return srvcOAObjectUnique;
    }
    
	/**
	 * Updates the {@code newFlag} of the specified {@link OAObject} and fires the
	 * corresponding before/after property-change events for the reserved property
	 * name {@code "NEW"}.
	 *
	 * <p>This method controls the object's lifecycle state with respect to creation
	 * and persistence. When the flag transitions from {@code true} to {@code false},
	 * automatic reverse-link insertion is enabled so that the object can be added to
	 * owning Hub relationships when applicable.</p>
	 *
	 * <h3>Behavior</h3>
	 * <ul>
	 *   <li>Ignores the call if the requested value equals the current value.</li>
	 *   <li>Fires a {@code beforePropertyChange} event with the old and new values.</li>
	 *   <li>Updates the internal {@code newFlag} field.</li>
	 *   <li>Fires an {@code afterPropertyChange} event.</li>
	 *   <li>If switching from new → not-new, invokes {@link #setAutoAdd(OAObject, boolean)}
	 *       to enable automatic reverse-link population.</li>
	 * </ul>
	 *
	 * @param oaObj the object whose new-state is being modified; may be {@code null}.
	 * @param b {@code true} to mark the object as newly created,
	 *          {@code false} to clear the new-state flag.
	 */
	public void setNew(final OAObject oaObj, final boolean b) {
		boolean old = faBridge.getObjectFriendAccess().getNewFlag(oaObj);
		if (b == old) {
			return;
		}
		getOAObjectEventService().fireBeforePropertyChange(oaObj, WORD_New, old, b, false, false);

		faBridge.getObjectFriendAccess().setNew(oaObj, b);
		
		getOAObjectEventService().firePropertyChange(oaObj, WORD_New, old, b, false, false);
		if (!b) {
			getOAObjectAutoAddService().setAutoAdd(oaObj, true);
		}
	}
    	
	// flag so that OAObject.finalize should ignore this object.	
	//qqqqqqqqqqqq make sure other code looks for guid=0, and ignore default cleanup (cached, etc)
	public void dontFinalize(OAObject obj) {
		if (obj != null) {
			getOAObjectGuidService().setGuid(obj, null);
		}
	}

}


