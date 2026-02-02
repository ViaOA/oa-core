package com.viaoa.graph.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.annotation.OAMany;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OASelect;
import com.viaoa.graph.api.internal.ObjectsInternalOps;
import com.viaoa.graph.service.object.*;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.object.OACalcInfo;
import com.viaoa.object.OACallback;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectInternalBridge;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OARemoteThreadService;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;

public class OAObjectService implements ObjectsInternalOps {
	private static final Logger LOG = Logger.getLogger(OAObjectService.class.getName());

	private HubService srvcHub;
	private OASyncService srvcSync;
	private OAThreadLocalService srvcOAThreadLocal;
	private OARemoteThreadService srvcOARemoteThread;
	
	private final OAObjectInternalBridge faBridge = new OAObjectInternalBridge();

	
    private OAObjectAnnotationService srvcOAObjectAnnotation;
    private OAObjectCacheService srvcOAObjectCache;
    private OAObjectCallbackService srvcOAObjectCallback;
    private OAObjectCSService srvcOAObjectCS;
    private OAObjectDatabaseService srvcOAObjectDatabase;
    private OAObjectDeleteService srvcOAObjectDelete;
    private OAObjectDSService srvcOAObjectDS;
    private OAObjectEnumService srvcOAObjectEnum;
    private OAObjectEmptyHubService srvcOAObjectEmptyHub;
    private OAObjectEventService srvcOAObjectEvent;
    private OAObjectGuidService srvcGuid;
    private OAObjectHubService srvcOAObjectHub;
    
    
    private OAObjectImportMatchService srvcOAObjectImportMatch;
    private OAObjectInfoService srvcOAObjectInfo; 
    private OAObjectInitializeService srvcOAObjectInitialize; 
    private OAObjectKeyService srvcOAObjectKey;
    private OAObjectLockService srvcOAObjectLock;
    private OAObjectLogService srvcOAObjectLog;
    private OAObjectPropertyService srvcOAObjectProperty;
    private OAObjectReflectService srvcOAObjectReflect;
    private OAObjectSaveService srvcOAObjectSave;
    private OAObjectSchedulerService srvcOAObjectScheduler;
    private OAObjectSerializeService srvcOAObjectSerialize;
    private OAObjectSiblingService srvcOAObjectSibling;
    private OAObjectUniqueService srvcOAObjectUnique;
    private OAObjectXMLService srvcOAObjectXML;
    
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
	
	/**
	 * Reserved property name representing whether auto-add behavior is enabled
	 * for reverse-link insertion.
	 */
	public static final String WORD_AutoAdd = "AutoAdd";

//qqqqqqqq remove these 2 boolean	
	/**
	 * Shared Boolean constant used when firing lifecycle-related property-change
	 * events.
	 */
	public static final Boolean TRUE = Boolean.TRUE;
	
	/**
	 * Shared Boolean constant used when firing lifecycle-related property-change
	 * events.
	 */
	public static final Boolean FALSE = Boolean.FALSE;
    
	
    
	public OAObjectService() {
    }
	

	private boolean bInitialized;
	public void initialize(HubService srvcHub, OASyncService srvcSync, OAThreadLocalService srvcOAThreadLocal, OARemoteThreadService srvcOARemoteThread) {
		if (bInitialized) return;
		this.srvcHub = srvcHub;
		this.srvcSync = srvcSync;
		if (srvcHub == null) return;
		this.srvcOAThreadLocal = srvcOAThreadLocal;
		this.srvcOARemoteThread = srvcOARemoteThread;
		bInitialized = true;
		
		getOAObjectAnnotationService();
		getOAObjectCacheService();
        getOAObjectCallbackService();
        getOAObjectCSService();
        getOAObjectDatabaseService();
        getOAObjectDeleteService();
        getOAObjectDSService();
        getOAObjectEmptyHubService();
        getOAObjectEnumService();
        getOAObjectEventService();
        getOAObjectGuidService();
        getOAObjectHubService();
        
    	srvcOAObjectImportMatch = new OAObjectImportMatchService(this, faBridge.getObjectFriendAccess());
    	srvcOAObjectInfo = new OAObjectInfoService(this, faBridge.getObjectFriendAccess(), faBridge.getObjectInfoFriendAccess());
    	srvcOAObjectInitialize = new OAObjectInitializeService(this, faBridge.getObjectFriendAccess(), srvcSync);
    	srvcOAObjectKey = new OAObjectKeyService(this, faBridge.getObjectFriendAccess(), faBridge.getObjectInfoFriendAccess());
    	srvcOAObjectLock = new OAObjectLockService(this, faBridge.getObjectFriendAccess(), srvcSync);
    	srvcOAObjectLog = new OAObjectLogService(this, faBridge.getObjectFriendAccess());
    	srvcOAObjectProperty = new OAObjectPropertyService(this, faBridge.getObjectFriendAccess());
    	srvcOAObjectReflect = new OAObjectReflectService(this, faBridge.getObjectFriendAccess(), srvcHub, srvcSync);
    	srvcOAObjectSave = new OAObjectSaveService(this, faBridge.getObjectFriendAccess(), srvcHub);
    	srvcOAObjectScheduler = new OAObjectSchedulerService(this, faBridge.getObjectFriendAccess());
    	srvcOAObjectSerialize = new OAObjectSerializeService(this, faBridge.getObjectFriendAccess(), faBridge.getObjectSerializerFriendAccess(), srvcHub, srvcSync);
    	srvcOAObjectSibling = new OAObjectSiblingService(this, faBridge.getObjectFriendAccess(), srvcHub);
    	srvcOAObjectUnique = new OAObjectUniqueService(this, faBridge.getObjectFriendAccess(), srvcSync);
    	srvcOAObjectXML = new OAObjectXMLService(this, faBridge.getObjectFriendAccess(), srvcHub);
	}

	//qqqqqq remove, keep internal ?? qqqqq
	public HubService getHubService() {
		return this.srvcHub;
	}

//qqqqqqqqq make all getter methods for sub Services package protected	
	
    public OAObjectAnnotationService getOAObjectAnnotationService() {
    	if (srvcOAObjectAnnotation != null) return srvcOAObjectAnnotation;
		srvcOAObjectAnnotation = new OAObjectAnnotationService(faBridge.getObjectInfoFriendAccess()) {
			@Override
			public Class<?> getHubObjectClass(Method method) {
				return OAObjectService.this.getOAObjectReflectService().getHubObjectClass(method);
			}

			@Override
			public OACalcInfo getOACalcInfo(OAObjectInfo oi, String name) {
				return OAObjectService.this.getOAObjectInfoService().getOACalcInfo(oi, name);
			}

			@Override
			public OALinkInfo getLinkInfo(OAObjectInfo oi, String name) {
				return OAObjectService.this.getOAObjectInfoService().getLinkInfo(oi, name);
			}
		};
    	return srvcOAObjectAnnotation;
    }
	
    public OAObjectCacheService getOAObjectCacheService() {
    	if (srvcOAObjectCache != null) return srvcOAObjectCache;
		srvcOAObjectCache = new OAObjectCacheService() {
			@Override
			public OAObjectKey createObjectKey(OAObject obj) {
				return OAObjectService.this.getOAObjectKeyService().createObjectKey(obj);
			}
			@Override
			public OAObjectInfo getOAObjectInfo(Class clazz) {
				return OAObjectService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public OAObjectKey getKey(OAObject oaObj) {
				return OAObjectService.this.getOAObjectKeyService().getKey(oaObj);
			}
			@Override
			public OAObjectKey createObjectKey(Class c, Object... ids) {
				return OAObjectService.this.getOAObjectKeyService().createObjectKey(c, ids);
			}
			@Override
			public Hub[] getHubReferences(OAObject oaObj) {
				return OAObjectService.this.getOAObjectHubService().getHubReferences(oaObj);
			}
			@Override
			public OALinkInfo getLinkInfoFromDetailToMaster(Hub hub) {
				return OAObjectService.this.getHubService().getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public boolean refreshSelect(Hub hub) {
				return OAObjectService.this.getHubService().getHubSelectService().refreshSelect(hub);
			}
			@Override
			public boolean isServer() {
				return OAObjectService.this.srvcSync.isServer();
			}
			@Override
			public void refreshRemoteServerCache(Class clazz) {
				OAObjectService.this.srvcSync.getRemoteServer().refreshCache(clazz);
			}
			@Override
			public int getObjectCacheAddMode() {
				return OAObjectService.this.srvcOAThreadLocal.getObjectCacheAddMode();
			}
		};
    	return srvcOAObjectCache; 
    }

    public OAObjectCallbackService getOAObjectCallbackService() {
    	if (srvcOAObjectCallback != null) return srvcOAObjectCallback;
    	
    	srvcOAObjectCallback = new OAObjectCallbackService() {
			@Override
			public OAObjectInfo getOAObjectInfo(Class clazz) {
				return OAObjectService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public Object getInternalProperty(OAObject oaObj, String propertyName) {
				return OAObjectService.this.getOAObjectPropertyService().getProperty(oaObj, propertyName);
			}
			@Override
			public Object getReflectProperty(OAObject oaObj, String propPath) {
				return OAObjectService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public Hub[] getHubReferences(OAObject oaObj) {
				return OAObjectService.this.getOAObjectHubService().getHubReferences(oaObj);
			}
			@Override
			public Method getMethod(OAObjectInfo oi, String methodName, Class classParam) {
				return OAObjectService.this.getOAObjectInfoService().getMethod(oi, methodName, classParam);
			}
			@Override
			public Method getMethod(OAObjectInfo oi, String methodName, int argumentCount) {
				return OAObjectService.this.getOAObjectInfoService().getMethod(oi, methodName, argumentCount);
			}
			@Override
			public String getPropertyFromMasterToDetail(Hub hub) {
				return OAObjectService.this.getHubService().getHubDetailService().getPropertyFromMasterToDetail(hub);
			}
			@Override
			public OALinkInfo getLinkInfoFromDetailToMaster(Hub hub) {
				return OAObjectService.this.getHubService().getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}
			@Override
			public OALinkInfo getLinkInfoFromMasterHubToDetail(Hub hub) {
				return OAObjectService.this.getHubService().getHubDetailService().getLinkInfoFromMasterHubToDetail(hub);
			}
			@Override
			public HubListener[] getAllListeners(Hub hub) {
				return OAObjectService.this.getHubService().getHubEventService().getAllListeners(hub);
			}
			@Override
			public boolean isServer() {
				return OAObjectService.this.srvcSync.isServer();
			}
    	};
    	return srvcOAObjectCallback;
    }
    
    public OAObjectCSService getOAObjectCSService() {
    	if (srvcOAObjectCS != null) return srvcOAObjectCS;
    	
    	srvcOAObjectCS = new OAObjectCSService() {
			@Override
			public OAObjectInfo getOAObjectInfo(Class clazz) {
				return OAObjectService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public boolean isSingleUser() {
				return OAObjectService.this.srvcSync.isSingleUser();
			}
			@Override
			public boolean isServer() {
				return OAObjectService.this.srvcSync.isServer();
			}
			@Override
			public boolean isClient() {
				return OAObjectService.this.srvcSync.isClient();
			}
			@Override
			public Object getDetail(OAObject masterObject, String propertyName) {
				return OAObjectService.this.srvcSync.getSyncClient().getDetail(masterObject, propertyName);
			}
			@Override
			public boolean propertyChange(Class objectClass, OAObjectKey origKey, String propertyName, Object newValue, boolean bIsBlob) {
				return OAObjectService.this.srvcSync.getRemoteSync().propertyChange(objectClass, origKey, propertyName, newValue, bIsBlob);
			}
			@Override
			public void callObjectCreated(OAObject obj) {
				OAObjectService.this.srvcSync.getSyncClient().objectCreated(obj);
			}
			@Override
			public void callObjectFinalized(UUID guid) {
				OAObjectService.this.srvcSync.getSyncClient().objectFinalized(guid);
			}
			@Override
			public void loadAllData(Hub thisHub, OASelect select) {
				OAObjectService.this.srvcHub.getHubSelectService().loadAllData(thisHub, select);
			}
			@Override
			public void callUpdateObjectsWithoutHubs(OAObject obj) {
				OASyncClient sc = OAObjectService.this.srvcSync.getSyncClient();
				if (sc != null) sc.updateObjectsWithoutHubs(obj);
			}
			@Override
			public OAObject createCopy(Class objectClass, OAObjectKey objectKey, String[] excludeProperties) {
				return OAObjectService.this.srvcSync.getRemoteClient().createCopy(objectClass, objectKey, excludeProperties);
			}
			@Override
			public boolean syncServerSave(Class objectClass, OAObjectKey objectKey, int iCascadeRule) {
				return OAObjectService.this.srvcSync.getRemoteServer().save(objectClass, objectKey, iCascadeRule);
			}

			@Override
			public OALinkInfo getLinkInfo(OAObjectInfo oi, String name) {
				return OAObjectService.this.getOAObjectInfoService().getLinkInfo(oi, name);
			}
			@Override
			public boolean shouldSendMessages() {
				OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();
				return srvcOARemoteThread.shouldSendMessages();
			}
			@Override
			public boolean isSuppressCSMessages() {
		        return srvcOAThreadLocal.isSuppressCSMessages();
			}
			@Override
			public void setSuppressCSMessages(boolean b) {
		        srvcOAThreadLocal.setSuppressCSMessages(b);
			}
			@Override
			public boolean isLoading() {
		        return srvcOAThreadLocal.isLoading();
			}
			@Override
			public OAObject getServerObject(Class clazz, OAObjectKey key) {
				RemoteServerInterface rsi = OAObjectService.this.srvcSync.getRemoteServer();
				if (rsi != null) return rsi.getObject(clazz, key);
				return null;
			}
			@Override
			public boolean serverDelete(Class clazz, OAObjectKey key) {
				RemoteSyncInterface rsi = OAObjectService.this.srvcSync.getRemoteSync();
				if (rsi != null) rsi.serverDelete(clazz, key);
				return true;
			}
			@Override
			public boolean clientDelete(Class clazz, OAObjectKey key) {
				RemoteSyncInterface rsi = OAObjectService.this.srvcSync.getRemoteSync();
				if (rsi != null) rsi.clientDelete(clazz, key);
				return true;
			}
    	};
    	
    	return srvcOAObjectCS;
    }
    
    public OAObjectDatabaseService getOAObjectDatabaseService() {
    	if (srvcOAObjectDatabase != null) return srvcOAObjectDatabase;
    	srvcOAObjectDatabase = new OAObjectDatabaseService() {
			@Override
			public OAObjectInfo getOAObjectInfo(Class clazz) {
				return OAObjectService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public Class getHubObjectClass(OAMany annotation, Method method) {
				return OAObjectService.this.getOAObjectAnnotationService().getHubObjectClass(annotation, method);
			}
    	};
    	return srvcOAObjectDatabase;
    }
    
    public OAObjectDeleteService getOAObjectDeleteService() {
    	if (srvcOAObjectDelete != null) return srvcOAObjectDelete;
        srvcOAObjectDelete = new OAObjectDeleteService(faBridge.getObjectFriendAccess()) {
			@Override
			public OAObjectInfo getOAObjectInfo(Class clazz) {
				return OAObjectService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public boolean callCSDelete(OAObject obj) {
				return OAObjectService.this.getOAObjectCSService().delete(obj);
			}
			@Override
			public void callCSSendDeleteToClients(OAObject obj) {
				OAObjectService.this.getOAObjectCSService().sendDeleteToClients(obj);
			}
			@Override
			public void callEventFireBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectService.this.getOAObjectEventService().fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}
			@Override
			public void callEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectService.this.getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}
			@Override
			public String callKeyVerifyKeyChange(OAObject oaObj, OAObjectKey newObjectKey) {
				return OAObjectService.this.getOAObjectKeyService().verifyKeyChange(oaObj, newObjectKey);
			}
			@Override
			public OAObject callCacheAdd(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll) {
				return OAObjectService.this.getOAObjectCacheService().add(obj, bErrorIfExists, bAddToSelectAll);
			}
			@Override
			public boolean callSyncIsClient() {
				return OAObjectService.this.srvcSync.isClient();
			}
			@Override
			public Hub[] callObjectHubGetHubReferences(OAObject oaObj) {
				return OAObjectService.this.getOAObjectHubService().getHubReferences(oaObj);
			}
			@Override
			public void callHubEventFireBeforeDeleteEvent(Hub hub, Object obj) {
				OAObjectService.this.getHubService().getHubEventService().fireBeforeDeleteEvent(hub, obj);
			}
			@Override
			public void callLocalThreadSetDeleting(Object obj, boolean b) {
				OAObjectService.this.srvcOAThreadLocal.setDeleting(obj, b);
			}
			@Override
			public boolean callSyncIsServer() {
				return OAObjectService.this.srvcSync.isServer();
			}
			@Override
			public void callHubCSRemoveAllFromHub(Hub thisHub) {
				OAObjectService.this.srvcHub.getHubCSService().removeAllFromHub(thisHub);
			}
			@Override
			public void callObjectHubDeleteAll(Hub hub, OACascade cascade) {
				OAObjectService.this.getOAObjectHubService().deleteAll(hub, cascade);
			}
			@Override
			public void callHubDSRemoveMany2ManyLinks(Hub hub) {
				OAObjectService.this.getHubService().getHubDSService().removeMany2ManyLinks(hub);
			}
			@Override
			public void callCacheCallback(OACallback callback, Class clazz) {
				OAObjectService.this.getOAObjectCacheService().callback(callback, clazz);
			}
			@Override
			public boolean callReflectIsReferenceNullOrNotLoadedOrEmptyHub(OAObject oaObj, String propertyName) {
				return OAObjectService.this.getOAObjectReflectService().isReferenceNullOrNotLoadedOrEmptyHub(oaObj, propertyName);
			}
			@Override
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}
			@Override
			public void callPropertyRemoveProperty(OAObject oaObj, String name, boolean bFirePropertyChange) {
				OAObjectService.this.getOAObjectPropertyService().removeProperty(oaObj, name, bFirePropertyChange);
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}
			@Override
			public void callSetNew(OAObject oaObj, boolean b) {
				OAObjectService.this.setNew(oaObj, b);;
			}
			@Override
			public void callHubEventFireAfterDeleteEvent(Hub thisHub, Object obj) {
				OAObjectService.this.srvcHub.getHubEventService().fireAfterDeleteEvent(thisHub, obj);
			}
			@Override
			public boolean callHubRemove(Hub thisHub, Object obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll) {
				return OAObjectService.this.srvcHub.getHubAddRemoveService().remove(thisHub, obj, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
			}
			@Override
			public void callRemoteTheadStartNextThread() {
				OAObjectService.this.srvcOARemoteThread.startNextThread();
			}
			@Override
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public OAObject callHubGetMasterObject(Hub hub) {
				return OAObjectService.this.getHubService().getMasterObject(hub);
			}
			@Override
			public Object callReflectGetReferenceObject(OAObject oaObj, String linkPropertyName) {
				return OAObjectService.this.getOAObjectReflectService().getReferenceObject(oaObj, linkPropertyName);
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name) {
				return OAObjectService.this.getOAObjectPropertyService().getProperty(oaObj, name);
			}
			@Override
			public boolean callDSSupportsStorage(OAObject obj) {
				OADataSource ds = OAObjectService.this.getOAObjectDSService().getDataSource(obj);
				if (ds == null) return false;
				return ds.supportsStorage();
			}
			@Override
			public void callDSUpdateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster) {
				OADataSource ds = OAObjectService.this.getOAObjectDSService().getDataSource(masterObject);
				if (ds == null) return;
				ds.updateMany2ManyLinks(masterObject, adds, removes, propFromMaster);
			}
			@Override
			public void callHubDataRemoveFromRemovedList(Hub thisHub, Object obj) {
				OAObjectService.this.getHubService().getHubDataService().removeFromRemovedList(thisHub, obj);
			}
			@Override
			public Hub callReflectGetReferenceHub(OAObject oaObj, String linkPropertyName, String sortOrder, boolean bSequence, Hub hubMatch) {
				return OAObjectService.this.getOAObjectReflectService().getReferenceHub(oaObj, linkPropertyName, sortOrder, bSequence, hubMatch);
			}
			@Override
			public void callDSDelete(OAObject obj) {
				OAObjectService.this.getOAObjectDSService().delete(obj);
			}
			@Override
			public void callDSRemoveReference(OAObject oaObj, OALinkInfo li) {
				OAObjectService.this.getOAObjectDSService().removeReference(oaObj, li);
			}
			@Override
			public Hub callHubGetHub(OAObject oaObj, OALinkInfo li) {
				return OAObjectService.this.getOAObjectHubService().getHub(oaObj, li);
			}
			@Override
			public boolean callInfoIsMany2Many(OALinkInfo li) {
				return OAObjectService.this.getOAObjectInfoService().isMany2Many(li);
			}
			@Override
			public OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li) {
				return OAObjectService.this.getOAObjectInfoService().getReverseLinkInfo(li);
			}
			@Override
			public void callLogToXmlFile(OAObject oaObj, boolean bSave) {
				OAObjectService.this.getOAObjectLogService().logToXmlFile(oaObj, bSave);
			}
			@Override
			public void callReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				OAObjectService.this.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);
			}
        };
    	return srvcOAObjectDelete;
    }
    
    public OAObjectDSService getOAObjectDSService() {
    	if (srvcOAObjectDS != null) return srvcOAObjectDS;
    	srvcOAObjectDS = new OAObjectDSService() {
			@Override
			public OAObjectInfo getOAObjectInfo(Class clazz) {
				return OAObjectService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public OAObjectKey callGetKey(OAObject obj) {
				return OAObjectService.this.getOAObjectKeyService().getKey(obj);
			}
			@Override
			public UUID callGetGuid(OAObject obj) {
				return OAObjectService.this.getOAObjectGuidService().getGuid(obj);
			}
			@Override
			public OAObjectKey callCreateObjectKey(Class c, Object... ids) {
				return OAObjectService.this.getOAObjectKeyService().createObjectKey(c, ids);
			}
		}; 
    	return srvcOAObjectDS;
    }
    
    public OAObjectEmptyHubService getOAObjectEmptyHubService() {
    	if (srvcOAObjectEmptyHub != null) return srvcOAObjectEmptyHub;
    	srvcOAObjectEmptyHub = new OAObjectEmptyHubService() {
			@Override
			public OAObjectKey callGetKey(OAObject obj) {
				return OAObjectService.this.getOAObjectKeyService().getKey(obj);
			}
			@Override
			public void callPropertySetProperty(OAObject obj, String name, Object value) {
				OAObjectService.this.getOAObjectPropertyService().setProperty(obj, name, value);
			}
			@Override
			public void callCacheCallback(OACallback callback) {
				OAObjectService.this.getOAObjectCacheService().callback(callback);
			}
			@Override
			public String[] callPropertyGetPropertyNames(OAObject oaObj) {
				return OAObjectService.this.getOAObjectPropertyService().getPropertyNames(oaObj);
			}
			@Override
			public boolean callReflectIsReferenceHubLoadedAndEmpty(OAObject oaObj, String propertyName) {
				return OAObjectService.this.getOAObjectReflectService().isReferenceHubLoadedAndEmpty(oaObj, propertyName);
			}
    	};
    	return srvcOAObjectEmptyHub;
    }
    
    public OAObjectEnumService getOAObjectEnumService() {
    	if (srvcOAObjectEnum != null) return srvcOAObjectEnum;
    	srvcOAObjectEnum = new OAObjectEnumService() {
			@Override
			public OAObjectInfo getOAObjectInfo(Class clazz) {
				return OAObjectService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
		};
    	return srvcOAObjectEnum;
    }

    public OAObjectEventService getOAObjectEventService() {
    	if (srvcOAObjectEvent != null) return srvcOAObjectEvent;
    	
    	srvcOAObjectEvent = new OAObjectEventService(faBridge.getObjectFriendAccess()) {
			@Override
			public OAObjectInfo getOAObjectInfo(Class clazz) {
				return OAObjectService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			@Override
			public OAObject callUniqueGetUnique(Class<? extends OAObject> clazz, String propertyName, Object uniqueKey, boolean bAutoCreate) {
				return OAObjectService.this.getOAObjectUniqueService().getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
			}
			@Override
			public boolean callSyncIsServer() {
				return OAObjectService.this.srvcSync.isServer();
			}
			@Override
			public boolean callSyncIsObjectOnServer(OAObject obj) {
				OASyncClient sc = srvcSync.getSyncClient();
				return (sc != null && sc.isObjectOnServer(obj));
			}
			@Override
			public boolean callSyncIsClient() {
				return OAObjectService.this.srvcSync.isClient();
			}
			@Override
			public void callSetAutoAdd(OAObject oaObj, boolean bEnabled) {
				OAObjectService.this.setAutoAdd(oaObj, bEnabled);
			}
			@Override
			public void callRemoteThreadStartNextThread() {
				OAObjectService.this.srvcOARemoteThread.startNextThread();
			}
			@Override
			public void callReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				OAObjectService.this.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);
			}
			@Override
			public void callReflectSetPrimitiveNull(OAObject oaObj, String propertyName, boolean bNull) {
				OAObjectService.this.getOAObjectReflectService().setPrimitiveNull(oaObj, propertyName, bNull);
			}
			@Override
			public boolean callReflectIsReferenceHubLoadedAndNotEmpty(OAObject oaObj, String propertyName) {
				return OAObjectService.this.getOAObjectReflectService().isReferenceHubLoadedAndNotEmpty(oaObj, propertyName);
			}
			@Override
			public boolean callReflectIsReferenceHubLoaded(OAObject oaObj, String propertyName) {
				return OAObjectService.this.getOAObjectReflectService().isReferenceHubLoaded(oaObj, propertyName);
			}
			@Override
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}
			@Override
			public boolean callReflectGetPrimitiveNull(OAObject oaObj, String propertyName) {
				return OAObjectService.this.getOAObjectReflectService().getPrimitiveNull(oaObj, propertyName);
			}
			@Override
			public OAObject callReflectGetObject(Class clazz, Object key) {
				return OAObjectService.this.getOAObjectReflectService().getObject(clazz, key);
			}
			@Override
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist) {
				return OAObjectService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue, bMustNotExist, bReturnNotExist);
			}
			@Override
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue) {
				return OAObjectService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue);
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name) {
				return OAObjectService.this.getOAObjectPropertyService().getProperty(oaObj, name);
			}
			@Override
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}
			@Override
			public void callLocalThreadSetSuppressCSMessages(boolean b) {
				OAObjectService.this.srvcOAThreadLocal.setSuppressCSMessages(b);
			}
			@Override
			public void callLocalThreadSetDeleting(Object obj, boolean b) {
				OAObjectService.this.srvcOAThreadLocal.setDeleting(obj, b);
			}
			@Override
			public void callLocalThreadRemoveHubEvent(HubEvent he) {
				OAObjectService.this.srvcOAThreadLocal.removeHubEvent(he);
			}
			@Override
			public boolean callLocalThreadIsLoading() {
				// TODO Auto-generated method stub
				return OAObjectService.this.srvcOAThreadLocal.isLoading();
			}
			@Override
			public boolean callLocalThreadIsDeleting(OAObject obj) {
				return OAObjectService.this.srvcOAThreadLocal.isDeleting(obj);
			}
			@Override
			public boolean callLocalThreadIsDeleting() {
				return OAObjectService.this.srvcOAThreadLocal.isDeleting();
			}
			@Override
			public boolean callLocalThreadGetCreateUndoablePropertyChanges() {
				return OAObjectService.this.srvcOAThreadLocal.getCreateUndoablePropertyChanges();
			}
			@Override
			public void callLocalThreadAddHubEvent(HubEvent he) {
				OAObjectService.this.srvcOAThreadLocal.addHubEvent(he);
			}
			@Override
			public String callKeyVerifyKeyChange(OAObject oaObj, OAObjectKey newObjectKey) {
				return OAObjectService.this.getOAObjectKeyService().verifyKeyChange(oaObj, newObjectKey);
			}
			@Override
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}
			@Override
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectService.this.getOAObjectKeyService().getKey(oaObj);
			}
			@Override
			public OAObjectKey callKeyCreateChangedObjectKey(Class<? extends OAObject> clazz, OAObjectKey objKey, String propertyName, Object newValue) {
				return OAObjectService.this.getOAObjectKeyService().createChangedObjectKey(clazz, objKey, propertyName, newValue);
			}
			@Override
			public boolean callKeyAfterChangedObjectKeyProperty(OAObject oaObj, OAObjectKey okOrig, boolean bVerify) {
				return OAObjectService.this.getOAObjectKeyService().afterChangedObjectKeyProperty(oaObj, okOrig, bVerify);
			}
			@Override
			public boolean callIsRemoteThread() {
				return OAObjectService.this.srvcOARemoteThread.isRemoteThread();
			}
			@Override
			public Hub callInfoGetRootHub(OAObjectInfo oi) {
				return OAObjectService.this.getOAObjectInfoService().getRootHub(oi);
			}
			@Override
			public OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li) {
				return OAObjectService.this.getOAObjectInfoService().getReverseLinkInfo(li);
			}
			@Override
			public OALinkInfo callInfoGetRecursiveLinkInfo(OAObjectInfo oi, int type) {
				return OAObjectService.this.getOAObjectInfoService().getRecursiveLinkInfo(oi, type);
			}
			@Override
			public OAPropertyInfo callInfoGetPropertyInfo(OAObjectInfo oi, String propertyName) {
				return OAObjectService.this.getOAObjectInfoService().getPropertyInfo(oi, propertyName);
			}
			@Override
			public OACalcInfo callInfoGetOACalcInfo(OAObjectInfo thisOI, String name) {
				return OAObjectService.this.getOAObjectInfoService().getOACalcInfo(thisOI, name);
			}
			@Override
			public Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount) {
				return OAObjectService.this.getOAObjectInfoService().getMethod(oi, methodName, argumentCount);
			}
			@Override
			public OALinkInfo callInfoGetLinkToOwner(OAObjectInfo oi) {
				return OAObjectService.this.getOAObjectInfoService().getLinkToOwner(oi);
			}
			@Override
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return OAObjectService.this.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}
			@Override
			public Hub[] callHubShareGetAllSharedHubs(Hub thisHub, OAFilter<Hub> filter) {
				return OAObjectService.this.srvcHub.getHubShareService().getAllSharedHubs(thisHub, filter);
			}
			@Override
			public boolean callHubIsInHub(OAObject oaObj) {
				return OAObjectService.this.getOAObjectHubService().isInHub(oaObj);
			}
			@Override
			public Hub[] callHubGetHubReferences(OAObject oaObj) {
				return OAObjectService.this.getOAObjectHubService().getHubReferences(oaObj);
			}
			@Override
			public void callHubEventFireBeforePropertyChange(Hub thisHub, OAObject oaObj, String propertyName, Object oldValue, Object newValue) {
				OAObjectService.this.srvcHub.getHubEventService().fireBeforePropertyChange(thisHub, oaObj, propertyName, oldValue, newValue);
			}
			@Override
			public void callHubEventFireAfterPropertyChange(Hub thisHub, OAObject oaObj, String propertyName, Object oldValue, Object newValue, OALinkInfo linkInfo) {
				OAObjectService.this.srvcHub.getHubEventService().fireAfterPropertyChange(thisHub, oaObj, propertyName, oldValue, newValue, linkInfo);
			}
			@Override
			public void callHubEventFireAfterLoadEvent(Hub thisHub, OAObject oaObj) {
				OAObjectService.this.srvcHub.getHubEventService().fireAfterLoadEvent(thisHub, oaObj);
			}
			@Override
			public Hub callHubDetailGetHubWithMasterHub(Hub thisHub) {
				return OAObjectService.this.srvcHub.getHubDetailService().getHubWithMasterHub(thisHub);
			}
			@Override
			public boolean callHubAddRemoveRemove(Hub thisHub, Object obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll) {
				return OAObjectService.this.srvcHub.getHubAddRemoveService().remove(thisHub, obj, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
			}
			@Override
			public void callHubAOSetActiveObject(Hub thisHub, Object object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
				OAObjectService.this.srvcHub.getHubAOService().setActiveObject(thisHub, object, adjustMaster, bUpdateLink, bForce);
			}
			@Override
			public boolean callGetAutoAdd(OAObject oaObj) {
				return OAObjectService.this.getAutoAdd(oaObj);
			}
			@Override
			public boolean callDSIsAssigningId(OAObject obj) {
				return OAObjectService.this.getOAObjectDSService().isAssigningId(obj);
			}
			@Override
			public OAObjectCallback callCallbackGetVerifyPropertyChangeObjectCallback(int checkType, OAObject oaObj, String propertyName, Object oldValue, Object newValue) {
				return OAObjectService.this.getOAObjectCallbackService().getVerifyPropertyChangeObjectCallback(checkType, oaObj, propertyName, oldValue, newValue);
			}
			@Override
			public OAObjectCallback callCallbackGetAllowSubmitObjectCallback(OAObject obj) {
				return OAObjectService.this.getOAObjectCallbackService().getAllowSubmitObjectCallback(obj);
			}
			@Override
			public <T extends OAObject> T callCacheGet(Class<T> clazz, OAObjectKey ok) {
				return OAObjectService.this.getOAObjectCacheService().get(clazz, ok);
			}
			@Override
			public void callCacheFireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue, boolean bLocalOnly, boolean bSendEvent) {
				OAObjectService.this.getOAObjectCacheService().fireAfterPropertyChange(obj, origKey, propertyName, oldValue, newValue, bLocalOnly, bSendEvent);
			}
			@Override
			public boolean callCSIsServer(OAObject obj) {
				return OAObjectService.this.getOAObjectCSService().isServer();
			}
			@Override
			public void callCSFireBeforePropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue) {
				OAObjectService.this.getOAObjectCSService().fireBeforePropertyChange(obj, propertyName, oldValue, newValue);
			}
		}; 
    	
    	return srvcOAObjectEvent;
    }
    
    public OAObjectGuidService getOAObjectGuidService() {
    	if (srvcGuid != null) return srvcGuid;
    	srvcGuid = new OAObjectGuidService(faBridge.getObjectFriendAccess()) {
		};
    	return srvcGuid;
    }

    public OAObjectHubService getOAObjectHubService() {
    	if (srvcOAObjectHub != null) return srvcOAObjectHub;
    	
    	
    	
    	return srvcOAObjectHub;
    }
    
    
    
//qqqqqqqqqqqqqqq next    
    /*	
	@OAParentProvided (example = "")
	public abstract 

	@OAParentProvided (example = "")
	public abstract

	@OAParentProvided (example = "")
	public abstract 

	@OAParentProvided (example = "")
	public abstract
*/	 

    
    
    public OAObjectInitializeService getOAObjectInitializeService() {
    	return srvcOAObjectInitialize;
    }

    

    public OAObjectInfoService getOAObjectInfoService() {
    	return srvcOAObjectInfo;
    }

    public OAObjectPropertyService getOAObjectPropertyService() {
    	return srvcOAObjectProperty;
    }
    




    
    public OAObjectReflectService getOAObjectReflectService() {
    	return srvcOAObjectReflect;
    }

    public OAObjectImportMatchService getOAObjectImportMatchService() {
    	return srvcOAObjectImportMatch;
    }

    public OAObjectKeyService getOAObjectKeyService() {
    	return srvcOAObjectKey;
    }

    public OAObjectLockService getOAObjectLockService() {
    	return srvcOAObjectLock;
    }

    public OAObjectLogService getOAObjectLogService() {
    	return srvcOAObjectLog;
    }

    public OAObjectSaveService getOAObjectSaveService() {
    	return srvcOAObjectSave;
    }
    
    public OAObjectSchedulerService getOAObjectSchedulerService() {
    	return srvcOAObjectScheduler;
    }

    public OAObjectUniqueService getOAObjectUniqueService() {
    	return srvcOAObjectUnique;
    }

    public OAObjectSerializeService getOAObjectSerializeService() {
    	return srvcOAObjectSerialize;
    }
    
    public OAObjectSiblingService getOAObjectSiblingService() {
    	return srvcOAObjectSibling;
    }
    
    public OAObjectXMLService getOAObjectXMLService() {
    	return srvcOAObjectXML;
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
		getOAObjectEventService().fireBeforePropertyChange(oaObj, WORD_New, old ? TRUE : FALSE, b ? TRUE : FALSE, false, false);

		faBridge.getObjectFriendAccess().setNew(oaObj, b);
		
		getOAObjectEventService().firePropertyChange(oaObj, WORD_New, old ? TRUE : FALSE, b ? TRUE : FALSE, false, false);
		if (!b) {
			setAutoAdd(oaObj, true);
		}
	}
    
	/**
	 * Convenience method that determines whether the specified {@link OAObject} is
	 * considered changed according to the supplied rule. This method allocates a
	 * new {@link OACascade} instance and delegates to
	 * {@link #getChanged(OAObject, int, OACascade)}.
	 *
	 * @param oaObj       the object to evaluate; may be {@code null}.
	 * @param iCascadeRule the rule controlling change evaluation.
	 * @return {@code true} if the object or any related object is considered
	 *         changed; otherwise {@code false}.
	 */
	public boolean getChanged(OAObject oaObj, int iCascadeRule) {
		if (oaObj == null) return false;
		if (iCascadeRule == OAObject.CASCADE_NONE) {
			OAObject.FriendAccess fa = faBridge.getObjectFriendAccess();
			return (fa.getChangedFlag(oaObj) || fa.getNewFlag(oaObj));
		}
		OACascade cascade = new OACascade();
		boolean b = getChanged(oaObj, iCascadeRule, cascade);
		return b;
	}

	
	/**
	 * Determines whether the specified {@link OAObject} is considered changed based
	 * on the supplied cascade rule and {@link OACascade} context. This variant is
	 * used when change detection must be coordinated with an active cascade
	 * operation, ensuring that objects are not visited more than once during a
	 * recursive evaluation.
	 *
	 * <p>If the object is {@code null}, the method returns {@code false}. Otherwise,
	 * the object's change status is evaluated according to the cascade rule:</p>
	 *
	 * <ul>
	 *   <li><b>OAObjectInfo.CHANGED_NONE</b>  
	 *       Always returns {@code false}.</li>
	 *
	 *   <li><b>OAObjectInfo.CHANGED_LOCAL</b>  
	 *       Returns the object's own {@code changedFlag} value.</li>
	 *
	 *   <li><b>OAObjectInfo.CHANGED_ALL</b>  
	 *       Performs a recursive scan of related objects using the provided
	 *       {@link OACascade} instance to track visited objects and prevent loops.</li>
	 *
	 *   <li><b>Depth-based rules</b>  
	 *       Interprets {@code iCascadeRule} as a maximum recursion depth and checks
	 *       linked objects up to that depth.</li>
	 * </ul>
	 *
	 * <p>The recursion is delegated to
	 * {@link #getChanged(OAObject, int, int, OALinkInfo[])} after the cascade context
	 * registers the root object to ensure it is not revisited. If any reachable
	 * object is marked changed, the method returns {@code true}; otherwise it
	 * returns {@code false}.</p>
	 *
	 * @param oaObj the object to evaluate; may be {@code null}.
	 * @param iCascadeRule the rule controlling how far recursive change detection
	 *                     should propagate.
	 * @param cascade the active {@link OACascade} used to record visited objects and
	 *                prevent infinite recursion.
	 * @return {@code true} if the object or any reachable related object is changed
	 *         according to the rule; {@code false} otherwise.
	 */
	public boolean getChanged(final OAObject oaObj, int iCascadeRule, OACascade cascade) {
		if (oaObj == null) return false;
		
		OAObject.FriendAccess fa = faBridge.getObjectFriendAccess();
		if (fa.getChangedFlag(oaObj)) return true;
		if (fa.getNewFlag(oaObj)) return true;

		if (iCascadeRule == oaObj.CASCADE_NONE) {
			return false;
		}
		if (cascade.wasCascaded(oaObj, true)) {
			return false;
		}

		if (fa.getProperties(oaObj) == null) return false;

		// check link cascade objects
		OAObjectInfo oi = getOAObjectInfoService().getOAObjectInfo(oaObj);
		List al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);
			String prop = li.getName();
			if (prop == null || prop.length() < 1) {
				continue;
			}
			if (li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}

			// same as OAObjectSaveDelegate.cascadeSave()
			if (getOAObjectReflectService().isReferenceNullOrNotLoaded(oaObj, prop)) {
				continue;
			}

			boolean bValidCascade = false;
			if (iCascadeRule == OAObject.CASCADE_LINK_RULES && li.getCascadeSave()) {
				bValidCascade = true;
			} else if (iCascadeRule == OAObject.CASCADE_OWNED_LINKS && li.getOwner()) {
				bValidCascade = true;
			} else if (iCascadeRule == OAObject.CASCADE_ALL_LINKS) {
				bValidCascade = true;
			}

			
			if (getOAObjectInfoService().isMany2Many(li)) {
				Hub hub = (Hub) getOAObjectReflectService().getRawReference(oaObj, prop);
				if (getHubService().getChanged(hub, OAObject.CASCADE_NONE, cascade)) {
					return true;
				}
			}
			
			if (!bValidCascade) {
				continue;
			}

			Object obj = getOAObjectReflectService().getProperty(oaObj, li.getName()); // if Hub with Keys, then this will load the correct objects to check
			if (obj == null) {
				continue;
			}

			if (obj instanceof Hub) {
				if (getOAObjectHubService().getChanged((Hub) obj, iCascadeRule, cascade)) {
					return true; //  if there have been adds/removes to hub
				}
			} else {
				if (obj instanceof OAObject) { // 20110420 could be OANullObject
					if (getChanged((OAObject) obj, iCascadeRule, cascade)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Convenience method that initiates a recursive traversal of the object graph
	 * starting from the specified {@link OAObject}. This variant simply allocates a
	 * new {@link OACascade} instance and delegates all traversal logic to
	 * {@link #recurse(OAObject, OACallback, OACascade)}.
	 *
	 * <p>This method exists for callers that do not need to manage or reuse an
	 * {@link OACascade} context. See the cascade-enabled variant for the full
	 * traversal behavior and callback invocation rules.</p>
	 *
	 * @param oaObj the root object to traverse; may be {@code null}.
	 * @param callback the callback invoked for each visited object; must not be {@code null}.
	 */
	public void recurse(OAObject oaObj, OACallback callback) {
		OACascade cascade = new OACascade();
		recurse(oaObj, callback, cascade);
	}

	/**
	 * Recursively traverses the reachable object graph beginning at the specified
	 * {@link OAObject}, invoking the provided {@link OACallback} for the root object
	 * and for each subsequently visited object. The supplied {@link OACascade}
	 * tracks visited objects to ensure each instance is processed at most once and
	 * to prevent infinite loops when cycles exist in the graph.
	 *
	 * <p>If {@code oaObj} is {@code null}, the method returns immediately. Otherwise,
	 * the object is registered with the {@code cascade} and the callback is invoked
	 * for it. The method then retrieves all link relationships from the object's
	 * metadata and recursively visits referenced objects according to the link type:
	 * </p>
	 *
	 * <ul>
	 *   <li><b>One-to-one links</b> — the referenced object is visited if present
	 *       and has not already been processed by the cascade.</li>
	 *   <li><b>One-to-many or many-to-many links</b> — each object in the associated
	 *       hub is visited, again subject to cascade loop-prevention.</li>
	 * </ul>
	 *
	 * <p>The traversal continues until all reachable related objects have been
	 * processed or the cascade prevents further descent. The method performs no
	 * depth limiting; callers wishing to restrict traversal depth must enforce such
	 * behavior externally.</p>
	 *
	 * @param oaObj   the root or current object being processed; may be {@code null}.
	 * @param callback the callback to invoke for each visited object; must not be {@code null}.
	 * @param cascade  the cascade context used to record visited objects and prevent
	 *                 revisiting or infinite recursion; must not be {@code null}.
	 */
	public void recurse(OAObject oaObj, OACallback callback, OACascade cascade) {
		if (cascade.wasCascaded(oaObj, true)) {
			return;
		}

		if (callback != null) {
			callback.updateObject(oaObj);
		}
		OAObjectInfo oi = getOAObjectInfoService().getOAObjectInfo(oaObj);

		List al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);
			if (li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			String prop = li.getName();

			final OAObjectReflectService srvcOAObjectReflect = getOAObjectReflectService();
			Object obj = srvcOAObjectReflect.getProperty(oaObj, li.getName()); // select all
			if (obj == null) {
				continue;
			}

			if (obj instanceof Hub) {
				Hub h = (Hub) obj;
				for (int j = 0;; j++) {
					Object o = h.elementAt(j);
					if (o == null) {
						break;
					}
					if (o instanceof OAObject) {
						recurse((OAObject) o, callback, cascade);
					} else {
						if (callback != null) {
							callback.updateObject(o);
						}
					}
					Object o2 = h.elementAt(j);
					if (o != o2) {
						j--;
					}
				}
			} else {
				if (obj instanceof OAObject) {
					recurse((OAObject) obj, callback, cascade);
				} else {
					if (callback != null) {
						callback.updateObject(obj);
					}
				}
			}
		}
	}

	/**
	 * Searches the object graph beginning at the specified {@link OAObject} for
	 * objects whose property value matches the supplied {@code findValue}, following
	 * the navigation defined by the {@code propertyPath}. This method implements
	 * the full recursive search logic for all {@code find(...)} overloads.
	 *
	 * <p>The {@code propertyPath} is a dot-separated sequence of property or link
	 * names beginning at {@code base}. Each segment may refer to either a simple
	 * property or a relationship link (one-to-one or one-to-many). The method
	 * traverses the path step by step and evaluates the final property value(s)
	 * against the provided {@code findValue}. If {@code bFindAll} is {@code false},
	 * the search stops as soon as the first match is found; otherwise, all matches
	 * reachable along the path are collected.</p>
	 *
	 * <h3>Traversal Behavior</h3>
	 * <ul>
	 *   <li>If {@code base} is {@code null} or the {@code propertyPath} is empty,
	 *       an empty result array is returned.</li>
	 *   <li>The method resolves each segment in the {@code propertyPath} using
	 *       {@link OAPropertyPath} metadata provided by {@code base}'s
	 *       {@link OAObjectInfo}.</li>
	 *   <li>For link segments:
	 *     <ul>
	 *       <li>One-to-one links: the referenced object becomes the next traversal node.</li>
	 *       <li>One-to-many or many-to-many links: each object in the associated hub
	 *           is recursively processed for the remaining path.</li>
	 *     </ul>
	 *   </li>
	 *   <li>For the final segment:
	 *     <ul>
	 *       <li>If it is a property, its value is retrieved via the object's getter.</li>
	 *       <li>A match occurs if {@code findValue == null} and the property value is {@code null},
	 *           or if {@code findValue.equals(propertyValue)} is {@code true}.</li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 *
	 * <h3>Results</h3>
	 * <ul>
	 *   <li>Returns an array of all matching values if {@code bFindAll} is {@code true}.</li>
	 *   <li>Returns a single-element array containing the first match if
	 *       {@code bFindAll} is {@code false}.</li>
	 *   <li>Returns an empty array if no matches are found.</li>
	 * </ul>
	 *
	 * @param base         the root object from which the property path traversal
	 *                     begins; may be {@code null}.
	 * @param propertyPath the dot-separated property or link path to follow; must
	 *                     not be {@code null}.
	 * @param findValue    the value to compare against the resolved property value.
	 * @param bFindAll     if {@code true}, collect all matches; otherwise stop at the first match.
	 * @return an array containing matched values (or objects), never {@code null}.
	 */
	public Object[] find(OAObject base, String propertyPath, Object findValue, boolean bFindAll) {
		if (propertyPath == null || propertyPath.length() == 0) {
			return null;
		}
		StringTokenizer st = new StringTokenizer(propertyPath, ".");
		Object result = base;
		for (; st.hasMoreTokens();) {
			String s = st.nextToken();
			base = (OAObject) result; // previous object
			result = base.getProperty(s);

			if (!st.hasMoreTokens()) {
				// last property, check against findValue
				if (result == findValue || (result != null && OACompare.compare(result, findValue) == 0)) {
					Object[] objs = new Object[] { base };
					return objs;
				}
				return null;
			}

			if (result == null) {
				return null;
			}

			if (result instanceof Hub) {
				String pp = null;
				for (; st.hasMoreTokens();) {
					s = st.nextToken();
					if (pp == null) {
						pp = s;
					} else {
						pp += "." + s;
					}
				}
				ArrayList al = null;
				Hub h = (Hub) result;
				for (int ii = 0;; ii++) {
					Object obj = h.elementAt(ii);
					if (obj == null) {
						break;
					}
					Object[] objs = find((OAObject) obj, pp, findValue, bFindAll);
					if (objs != null) {
						if (!bFindAll) {
							return objs;
						}
						if (al == null) {
							al = new ArrayList(10);
						}
						for (int i3 = 0; i3 < objs.length; i3++) {
							al.add(objs[i3]);
						}
					}
				}
				if (al == null) {
					return null;
				}
				Object[] objs = new Object[al.size()];
				objs = al.toArray(objs);
				return objs;
			}
			if (!(result instanceof OAObject)) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Tracks OAObjects for which automatic reverse-link insertion is disabled.
	 * Presence of a GUID in this map indicates auto-add is turned off.
	 */
	private static final ConcurrentHashMap<UUID, Long> hmAutoAdd = new ConcurrentHashMap();
	
	/**
	 * Enables or disables automatic reverse-link insertion for the specified
	 * {@link OAObject}. When enabled, the object is eligible to be added to
	 * reverse-link hubs when link-one assignments occur.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>If {@code oaObj} is {@code null}, no action is taken.</li>
	 *   <li>Disabling auto-add is ignored if the object is not new.</li>
	 *   <li>Updates the internal auto-add state stored in the {@code hmAutoAdd} map.</li>
	 *   <li>Fires a property-change event for the reserved {@code "AutoAdd"} property.</li>
	 *   <li>When enabling auto-add and the object is not deleted, temporarily
	 *       suppresses client-sync messages and ensures the object is added to any
	 *       applicable reverse-link hubs.</li>
	 * </ul>
	 *
	 * @param oaObj the object whose auto-add behavior is being modified; may be {@code null}.
	 * @param bEnabled {@code true} to enable auto-add; {@code false} to disable it.
	 */
	public void setAutoAdd(final OAObject oaObj, boolean bEnabled) {
		if (oaObj == null) {
			return;
		}
		if (!bEnabled && !oaObj.isNew()) {
			return;
		}

		OAObject.FriendAccess fa = faBridge.getObjectFriendAccess();
		boolean bOld = !hmAutoAdd.containsKey(fa.getGuid(oaObj));
		if (bOld == bEnabled) {
			return;
		}

		UUID guid = fa.getGuid(oaObj);
		if (!bEnabled) {
			hmAutoAdd.put(guid, 0L);
		} else {
			hmAutoAdd.remove(guid);
		}
		getOAObjectEventService().firePropertyChange(oaObj, WORD_AutoAdd, bOld ? TRUE : FALSE, bEnabled ? TRUE : FALSE, false, false);

		if (!bEnabled || faBridge.getObjectFriendAccess().getDeleteFlag(oaObj)) {
			return;
		}

		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
		try {
			srvcOAThreadLocal.setSuppressCSMessages(true);
			// need to see if object should be put into linkOne/masterObject hub(s)
			OAObjectInfo oi = getOAObjectInfoService().getOAObjectInfo(oaObj);
			for (OALinkInfo li : oi.getLinkInfos()) {
				if (!li.getUsed()) {
					continue;
				}
				if (li.getType() != li.ONE) {
					continue;
				}
				final OAObjectReflectService srvcOAObjectReflect = getOAObjectReflectService();
				Object objx = srvcOAObjectReflect.getRawReference(oaObj, li.getName());
				if (!(objx instanceof OAObject)) {
					continue;
				}

				OALinkInfo liRev = getOAObjectInfoService().getReverseLinkInfo(li);
				if (liRev == null) {
					continue;
				}
				if (!liRev.getUsed()) {
					continue;
				}
				if (liRev.getType() != li.MANY) {
					continue;
				}
				if (liRev.getPrivateMethod()) {
					continue;
				}

				Object objz = srvcOAObjectReflect.getProperty((OAObject) objx, liRev.getName());
				if (objz instanceof Hub) {
					((Hub) objz).add(oaObj);
				}
			}
		} finally {
			srvcOAThreadLocal.setSuppressCSMessages(false);
		}
	}

	/**
	 * Returns whether automatic reverse-link insertion is enabled for the specified
	 * {@link OAObject}. If the object is {@code null}, the method returns
	 * {@code false}.
	 *
	 * <p>This method simply returns the value of the object's internal
	 * {@code autoAddEnabled} flag. It does not evaluate any link relationships or
	 * perform any side effects. The flag determines whether the object should be
	 * automatically inserted into reverse-link Hubs when link assignments occur.</p>
	 *
	 * @param oaObj the object whose auto-add setting is queried; may be {@code null}.
	 * @return {@code true} if automatic reverse-link insertion is enabled,
	 *         {@code false} otherwise.
	 */
	public boolean getAutoAdd(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		return !hmAutoAdd.containsKey(faBridge.getObjectFriendAccess().getGuid(oaObj));
	}

	/**
	 * Convenience method that returns the ID (primary-key) property values of the
	 * specified {@link OAObject}. This method simply delegates to
	 * {@link OAObjectInfoDelegate#getPropertyIdValues(OAObjectInfo, OAObject, String[])}
	 * using the object's {@link OAObjectInfo} metadata.
	 *
	 * <p>If {@code obj} is {@code null}, this method returns {@code null}. Otherwise,
	 * all ID property names defined in the model are resolved through the metadata
	 * and their values are retrieved. For composite keys, all ID components are
	 * returned in the order specified by the model.</p>
	 *
	 * <p>See the delegate method for full details on ID resolution behavior.</p>
	 *
	 * @param obj the object whose ID property values are requested; may be {@code null}.
	 * @return an array of ID values, or {@code null} if {@code obj} is {@code null}.
	 */
	public Object[] getPropertyIdValues(OAObject obj) {
		if (obj == null) return null;
		return getOAObjectInfoService().getPropertyIdValues(obj);
	}

	//qqqqqqqqq this was created/added ... needs to be more protected ?? 
	public Object[] getProperties(OAObject obj) {
		if (obj == null) return null;
		return faBridge.getObjectFriendAccess().getProperties(obj);
	}
	
	// flag so that OAObject.finalize should ignore this object.	
	//qqqqqqqqqqqq make sure other code looks for guid=0, and ignore default cleanup (cached, etc)
	public void dontFinalize(OAObject obj) {
		if (obj != null) {
			getOAObjectGuidService().setGuid(obj, null);
		}
	}

	
	//qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq ObjectsOps, ObjectInternalOps
	
	
	@Override
	public void save(OAObject oaObj, int iCascadeRule, OACascade cascade) {
		getOAObjectSaveService().save(oaObj, iCascadeRule, cascade);
		
	}
	
	@Override
	public OAObjectInfo getOAObjectInfo(Class<?> c) {
		return getOAObjectInfoService().getObjectInfo(c);
	}

	@Override
	public OAObjectInfo getOAObjectInfo(OAObject obj) {
		if (obj == null) return null;
		return getOAObjectInfo(obj.getClass());
	}

	@Override
	public void setSelectAllHub(Hub hub) {
		srvcOAObjectCache.setSelectAllHub(hub);
	}

	@Override
	public void removeSelectAllHub(Hub hub) {
		srvcOAObjectCache.removeSelectAllHub(hub);
	}

	@Override
	public <T extends OAObject> T getObject(Class<T> clazz, Object key) {
		return srvcOAObjectCache.getObject(clazz, key);
	}
	
}


