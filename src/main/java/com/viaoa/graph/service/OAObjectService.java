package com.viaoa.graph.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.annotation.OAMany;
import com.viaoa.graph.api.ObjectsOps;
import com.viaoa.graph.api.internal.ObjectsInternalOps;
import com.viaoa.graph.service.object.*;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubChangeListener;
import com.viaoa.model.oa.VEnum;
import com.viaoa.model.oa.VString;
import com.viaoa.object.OACalcInfo;
import com.viaoa.object.OACallback;
import com.viaoa.object.OACascade;
import com.viaoa.object.OACopyCallback;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheListener;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.scheduler.OAScheduler;
import com.viaoa.util.OADate;
import com.viaoa.util.OAFilter;
import com.viaoa.xml.OAXMLWriter;

public class OAObjectService extends OAObjectParentService implements ObjectsOps, ObjectsInternalOps {
	private static final Logger LOG = Logger.getLogger(OAObjectService.class.getName());

    
	@Override
	public void callObjectSetNew(OAObject oaObj, boolean bIsNew) {
	    setNew(oaObj, bIsNew);
	}

	@Override
	public boolean callObjectChangeGetChanged(OAObject oaObj, int cascadeRule) {
	    return getOAObjectChangeService().getChanged(oaObj, cascadeRule);
	}

	@Override
	public void callObjectSetAutoAdd(OAObject oaObj, boolean bAutoAdd) {
	     getOAObjectAutoAddService().setAutoAdd(oaObj, bAutoAdd);
	}

	@Override
	public boolean callObjectGetAutoAdd(OAObject oaObj) {
	    return getOAObjectAutoAddService().getAutoAdd(oaObj);
	}


	@Override
	public Object[] callObjectFind(OAObject oaObjBase, String propertyPath, Object findValue, boolean bFindAll) {
	    return getOAObjectFindService().find(oaObjBase, propertyPath, findValue, bFindAll);
	}

	
	// AnnotationService ======
	@Override
	public Class<? extends OAObject> callObjectAnnotationGetHubObjectClass(OAMany annotation, Method method) {
	    return getOAObjectAnnotationService().getHubObjectClass(annotation, method);
	}


	// CacheService ======
	@Override
	public void callObjectCacheFireAfterLoadEvent(OAObject oaObj) {
	    getOAObjectCacheService().fireAfterLoadEvent(oaObj);
	}

	@Override
	public Class<? extends OAObject>[] callObjectCacheGetClasses() {
	    return getOAObjectCacheService().getClasses();
	}

	@Override
	public <T extends OAObject> void callObjectCacheCallback(Class<T> clazz, OACallback<T> callback) {
	    getOAObjectCacheService().callback(clazz, callback);
	}

	@Override
	public int callObjectCacheGetTotal(Class<? extends OAObject> clazz) {
	    return getOAObjectCacheService().getTotal(clazz);
	}

	@Override
	public <T extends OAObject> void callObjectCacheAddListener(Class<T> clazz, OAObjectCacheListener<T> cachelistener) {
		getOAObjectCacheService().addListener(clazz, cachelistener);
	}

	@Override
	public <T extends OAObject> void callObjectCacheVisit(Class<T> clazz, OACallback<T> callback) {
	    getOAObjectCacheService().visit(clazz, callback);
	}

	@Override
	public <T extends OAObject> void callObjectCacheRemoveListener(Class<T> clazz, OAObjectCacheListener<T> cacheListener) {
	    getOAObjectCacheService().removeListener(clazz, cacheListener);
	}

	@Override
	public <T extends OAObject> Hub<T> callObjectCacheGetSelectAllHub(Class<T> clazz) {
	    return getOAObjectCacheService().getSelectAllHub(clazz);
	}

	@Override
	public <T extends OAObject> T callObjectCacheFind(Class<T> clazz, OAFinder<T, T> finder) {
	    return getOAObjectCacheService().find(clazz, finder);
	}

	@Override
	public <T extends OAObject> T callObjectCacheGet(Class<T> clazz, OAObjectKey objectKey) {
	    return getOAObjectCacheService().get(clazz, objectKey);
	}

	@Override
	public <T extends OAObject> T callObjectCacheGetObject(Class<T> clazz, Object object) {
	    return getOAObjectCacheService().getObject(clazz, object);
	}

	@Override
	public void callObjectCacheRemoveObject(OAObject oaObj) {
	    getOAObjectCacheService().removeObject(oaObj);
	}

	@Override
	public void callObjectCacheRefresh(Class<? extends OAObject> clazz) {
	    getOAObjectCacheService().refresh(clazz);
	}

	@Override
	public void callObjectCacheRemoveAllObjects(Class<? extends OAObject> clazz) {
	    getOAObjectCacheService().removeAllObjects(clazz);
	}

	@Override
	public <T extends OAObject> T callObjectCacheFind(T fromObject, Class<T> clazz, OAFilter<T> filter, boolean bSkipNew, boolean bThrowException, int fetchAmount, List<T> alResults) {
	    return getOAObjectCacheService().find(fromObject, clazz, filter, bSkipNew, bThrowException, fetchAmount, alResults);
	}

	@Override
	public <T extends OAObject> T callObjectCacheAdd(T oaObj, boolean bErrorIfExists, boolean bAddToSelectAll) {
	    return (T) getOAObjectCacheService().add(oaObj, bErrorIfExists, bAddToSelectAll);
	}


	// CallbackService ======
	@Override
	public boolean callObjectCallbackGetVerifyPropertyChange(int checkType, OAObject obj, String propertyName, Object oldValue, Object newValue) {
	    return getOAObjectCallbackService().getVerifyPropertyChange(checkType, obj, propertyName, oldValue, newValue);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetVerifyPropertyChangeObjectCallback(int checkType, OAObject oaObj, String propertyName, Object oldValue, Object newValue) {
	    return getOAObjectCallbackService().getVerifyPropertyChangeObjectCallback(checkType, oaObj, propertyName, oldValue, newValue);
	}

	@Override
	public <T extends OAObject> boolean callObjectCallbackGetAllowEnabled(int checkType, Hub<T> hub, T obj, String name) {
	    return getOAObjectCallbackService().getAllowEnabled(checkType, hub, obj, name);
	}

	@Override
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowEnabledObjectCallback(int checkType, Hub<T> hub, T oaObj, String name) {
		return getOAObjectCallbackService().getAllowEnabledObjectCallback(checkType, hub, oaObj, name);
	}

	@Override
	public <T extends OAObject> boolean callObjectCallbackGetAllowVisible(Hub<T> hub, T oaObj, String name) {
	    return getOAObjectCallbackService().getAllowVisible(hub, oaObj, name);
	}

	@Override
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowVisibleObjectCallback(Hub<T> hub, T oaObj, String name) {
	    return getOAObjectCallbackService().getAllowVisibleObjectCallback(hub, oaObj, name);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetVerifyCommandObjectCallback(OAObject oaObj, String methodName, int checkType) {
	    return getOAObjectCallbackService().getVerifyCommandObjectCallback(oaObj, methodName, checkType);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetAllowSubmitObjectCallback(OAObject oaObj) {
	    return getOAObjectCallbackService().getAllowSubmitObjectCallback(oaObj);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetVerifySaveObjectCallback(OAObject oaObj, int checkType) {
	    return getOAObjectCallbackService().getVerifySaveObjectCallback(oaObj, checkType);
	}

	@Override
	public boolean callObjectCallbackGetAllowSave(OAObject oaObj, int checkType) {
	    return getOAObjectCallbackService().getAllowSave(oaObj, checkType);
	}

	@Override
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetVerifyDeleteObjectCallback(Hub<T> hub, T objDelete, int checkType) {
	    return getOAObjectCallbackService().getVerifyDeleteObjectCallback(hub, objDelete, checkType);
	}

	@Override
	public <T extends OAObject>  boolean callObjectCallbackGetAllowDelete(Hub<T> hub, T oaObj) {
	    return getOAObjectCallbackService().getAllowDelete(hub, oaObj);
	}

	@Override
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowAddObjectCallback(Hub<T> hub, T objAdd, int checkType) {
	    return getOAObjectCallbackService().getAllowAddObjectCallback(hub, objAdd, checkType);
	}

	@Override
	public <T extends OAObject> void callObjectCallbackAddObjectCallbackChangeListeners(Hub<T> hub, Class<T> cz, String prop, String ppPrefix, HubChangeListener changeListener, boolean bEnabled) {
	    getOAObjectCallbackService().addObjectCallbackChangeListeners(hub, cz, prop, ppPrefix, changeListener, bEnabled);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetAllowNewObjectCallback(Hub<? extends OAObject> hub) {
	    return getOAObjectCallbackService().getAllowNewObjectCallback(hub);
	}
	
	@Override
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowDeleteObjectCallback(Hub<T> hub, T obj) {
	    return getOAObjectCallbackService().getAllowDeleteObjectCallback(hub, obj);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetAllowCopyObjectCallback(OAObject obj) {
	    return getOAObjectCallbackService().getAllowCopyObjectCallback(obj);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetAllowEnabledObjectCallback(Hub<? extends OAObject> hub) {
	    return getOAObjectCallbackService().getAllowEnabledObjectCallback(hub);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetAllowSaveObjectCallback(OAObject obj, int checkType) {
	    return getOAObjectCallbackService().getAllowSaveObjectCallback(obj, checkType);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetAllowDeleteObjectCallback(OAObject ao) {
	    return getOAObjectCallbackService().getAllowDeleteObjectCallback(ao);
	}

	@Override
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowRemoveObjectCallback(Hub<T> hub, T objRemove, int checkType) {
	    return getOAObjectCallbackService().getAllowRemoveObjectCallback(hub, objRemove, checkType);
	}
	@Override
	public OAObjectCallback callObjectCallbackGetAllowRemoveAllObjectCallback(Hub<? extends OAObject> hub, int checkType) {
	    return getOAObjectCallbackService().getAllowRemoveAllObjectCallback(hub, checkType);
	}

	@Override
	public <T extends OAObject> T callObjectCallbackGetCopy(T obj) {
	    return (T) getOAObjectCallbackService().getCopy(obj);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetConfirmPropertyChangeObjectCallback(OAObject oaObj, String property, Object newValue, String confirmMessage, String confirmTitle) {
	    return getOAObjectCallbackService().getConfirmPropertyChangeObjectCallback(oaObj, property, newValue, confirmMessage, confirmTitle);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetConfirmSaveObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle) {
	    return getOAObjectCallbackService().getConfirmSaveObjectCallback(oaObj, confirmMessage, confirmTitle);
	}
	@Override
	public OAObjectCallback callObjectCallbackGetConfirmDeleteObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle) {
	    return getOAObjectCallbackService().getConfirmDeleteObjectCallback(oaObj, confirmMessage, confirmTitle);
	}

	@Override
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetConfirmRemoveObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle) {
	    return getOAObjectCallbackService().getConfirmRemoveObjectCallback(hub, oaObj, confirmMessage, confirmTitle);
	}

	@Override
	public <T extends OAObject> boolean callObjectCallbackGetAllowRemove(Hub<T> hub, T obj, int checkType) {
		return getOAObjectCallbackService().getAllowRemove(hub, obj, checkType);
	}
	
	@Override
	public OAObjectCallback callObjectCallbackGetConfirmRemoveAllObjectCallback(Hub<? extends OAObject> hub, String confirmMessage, String confirmTitle) {
	    return getOAObjectCallbackService().getConfirmRemoveAllObjectCallback(hub, confirmMessage, confirmTitle);
	}

	@Override
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetConfirmAddObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle) {
	    return getOAObjectCallbackService().getConfirmAddObjectCallback(hub, oaObj, confirmMessage, confirmTitle);
	}

	@Override
	public String callObjectCallbackGetFormat(OAObject obj, String propertyName, String defaultFormat) {
	    return getOAObjectCallbackService().getFormat(obj, propertyName, defaultFormat);
	}

	@Override
	public String callObjectCallbackGetToolTip(OAObject obj, String propertyName, String defaultToolTip) {
	    return getOAObjectCallbackService().getToolTip(obj, propertyName, defaultToolTip);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetConfirmCommandObjectCallback(OAObject oaObj, String methodName, String confirmMessage, String confirmTitle) {
	    return getOAObjectCallbackService().getConfirmCommandObjectCallback(oaObj, methodName, confirmMessage, confirmTitle);
	}

	@Override
	public OAObjectCallback callObjectCallbackGetAllowVisibleObjectCallback(Hub<? extends OAObject> hub) {
	    return getOAObjectCallbackService().getAllowVisibleObjectCallback(hub);
	}

	@Override
	public <T extends OAObject> boolean callObjectCallbackGetAllowAdd(Hub<T> hub, T obj, int checkType) {
	    return getOAObjectCallbackService().getAllowAdd(hub, obj, checkType);
	}

	@Override
	public <T extends OAObject> boolean callObjectCallbackGetVerifyRemove(Hub<T> hub, T obj, int checkType) {
	    return getOAObjectCallbackService().getVerifyRemove(hub, obj, checkType);
	}

	// CSService ======
	@Override
	public void callObjectCSObjectFinalized(UUID guid) {
	    getOAObjectCSService().objectFinalized(guid);
	}

	@Override
	public <T extends OAObject> Hub<T> callObjectCSGetServerReferenceHub(T oaObj, String linkPropertyName) {
	    return getOAObjectCSService().getServerReferenceHub(oaObj, linkPropertyName);
	}

	@Override
	public boolean callObjectCSIsServer(OAObject oaObj) {
	    return getOAObjectCSService().isServer(oaObj);
	}

	@Override
	public void callObjectCSUpdateObjectsWithoutHubs(OAObject oaObj) {
	    getOAObjectCSService().updateObjectsWithoutHubs(oaObj);
	}

	// DeleteService ======
	@Override
	public void callObjectDeleteSetDeleted(OAObject oaObj, boolean bDeleted) {
	    getOAObjectDeleteService().setDeleted(oaObj, bDeleted);
	}

	@Override
	public void callObjectDeleteDelete(OAObject oaObj) {
	    getOAObjectDeleteService().delete(oaObj);
	}

	@Override
	public void callObjectDeleteSyncServerDelete(OAObject obj) {
	    getOAObjectDeleteService().syncServerDelete(obj);
	}

	@Override
	public void callObjectDeleteSyncClientDelete(OAObject obj) {
	    getOAObjectDeleteService().syncClientDelete(obj);
	}

	
	// DSService ======
	@Override
	public boolean callObjectDSGetAssignIdOnCreate(OAObject oaObj) {
	    return getOAObjectDSService().getAssignIdOnCreate(oaObj);
	}

	@Override
	public void callObjectDSAssignId(OAObject oaObj) {
	    getOAObjectDSService().assignId(oaObj);
	}

	@Override
	public void callObjectDSSetAssigningId(OAObject oaObj, boolean bIsAssigningId) {
	    getOAObjectDSService().setAssigningId(oaObj, bIsAssigningId);
	}

	// EmptyHubService ======
	@Override
	public void callObjectEmptyHubInitialize(OAObject oaObj) {
	    getOAObjectEmptyHubService().initialize(oaObj);
	}

	// EnumService ======
	@Override
	public Hub<VEnum> callObjectEnumGetVEnums(Class<? extends OAObject> clazz, String propertyName) {
	    return getOAObjectEnumService().getVEnums(clazz, propertyName);
	}

	// EventService ======
	@Override
	public void callObjectEventFireBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
	    getOAObjectEventService().fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
	}

	@Override
	public void callObjectEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
	    getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
	}

	@Override
	public void callObjectEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged, boolean bUnknownValues) {
	    getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged, bUnknownValues);
	}

	@Override
	public void callObjectEventFireAfterLoadEvent(OAObject oaObj) {
	    getOAObjectEventService().fireAfterLoadEvent(oaObj);
	}

	// GuidService ======
	@Override
	public void callObjectGuidSetGuid(OAObject oaObj, UUID iguid) {
	    getOAObjectGuidService().setGuid(oaObj, iguid);
	}

	@Override
	public UUID callObjectGuidGetGuid(OAObject oaObj) {
	    return getOAObjectGuidService().getGuid(oaObj);
	}

	
	// HubService ======
	@Override
	public WeakReference<Hub<? extends OAObject>>[] callObjectHubGetHubReferencesNoCopy(OAObject oaObj) {
	    return getOAObjectHubService().getHubReferencesNoCopy(oaObj);
	}

	@Override
	public <T extends OAObject> Hub<T>[] callObjectHubGetHubReferences(T oaObj) {
	    return getOAObjectHubService().getHubReferences(oaObj);
	}

	@Override
	public <T extends OAObject> boolean callObjectHubAddHub(T oaObj, Hub<T> hub, boolean bAlwaysAddIfM2M) {
	    return getOAObjectHubService().addHub(oaObj, hub, bAlwaysAddIfM2M);
	}

	@Override
	public boolean callObjectHubIsInHubWithMaster(OAObject obj) {
	    return getOAObjectHubService().isInHubWithMaster(obj);
	}

	@Override
	public <T extends OAObject> void callObjectHubRemoveHub(final T oaObj, Hub<T> hub, boolean bIsOnHubFinalize) {
	    getOAObjectHubService().removeHub(oaObj, hub, bIsOnHubFinalize);
	}

	// ObjectInfoService ======
	@Override
	public OAObjectInfo callObjectInfoGetOAObjectInfo(Class<?> clazz) {
	    return getOAObjectInfoService().getOAObjectInfo(clazz);
	}

	@Override
	public Class<? extends OAObject> callObjectInfoGetHubPropertyClass(Class<? extends OAObject> clazz, String propertyName) {
	    return getOAObjectInfoService().getHubPropertyClass(clazz, propertyName);
	}

	@Override
	public Class<?> callObjectInfoGetPropertyClass(Class<? extends OAObject> clazz, String propertyName) {
	    return getOAObjectInfoService().getPropertyClass(clazz, propertyName);
	}

	@Override
	public Class<?> callObjectInfoGetPropertyClass(OAObjectInfo oi, String propertyName) {
	    return getOAObjectInfoService().getPropertyClass(oi, propertyName);
	}

	@Override
	public boolean callObjectInfoIsHubProperty(OAObjectInfo oi, String propertyName) {
	    return getOAObjectInfoService().isHubProperty(oi, propertyName);
	}
	@Override
	public OACalcInfo callObjectInfoGetCalcInfo(OAObjectInfo oi, String name) {
	    return getOAObjectInfoService().getOACalcInfo(oi, name);
	}

	@Override
	public OALinkInfo callObjectInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
	    return getOAObjectInfoService().getLinkInfo(oi, propertyName);
	}

	@Override
	public OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject oaObj) {
	    return getOAObjectInfoService().getOAObjectInfo(oaObj);
	}

	@Override
	public boolean callObjectInfoCacheHub(OALinkInfo linkInfo, Hub<?> hub) {
	    return getOAObjectInfoService().cacheHub(linkInfo, hub);
	}

	@Override
	public Method callObjectInfoGetMethod(OAObjectInfo oi, String string) {
	    return getOAObjectInfoService().getMethod(oi, string);
	}

	@Override
	public OALinkInfo callObjectInfoGetRecursiveLinkInfo(OAObjectInfo oi, int type) {
	    return getOAObjectInfoService().getRecursiveLinkInfo(oi, type);
	}

	@Override
	public Method callObjectInfoGetMethod(Class<?> clazz, String methodName) {
	    return getOAObjectInfoService().getMethod(clazz, methodName);
	}

	@Override
	public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo li) {
	    return getOAObjectInfoService().getReverseLinkInfo(li);
	}

	@Override
	public OAObjectInfo callObjectInfoGetObjectInfo(Class<?> clazz) {
	    return getOAObjectInfoService().getObjectInfo(clazz);
	}

	@Override
	public boolean callObjectInfoIsMany2Many(OALinkInfo li) {
	    return getOAObjectInfoService().isMany2Many(li);
	}

	@Override
	public OALinkInfo callObjectInfoGetLinkInfo(Class<? extends OAObject> clazz, String property) {
	    return getOAObjectInfoService().getLinkInfo(clazz, property);
	}

	@Override
	public Method callObjectInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount) {
	    return getOAObjectInfoService().getMethod(oi, methodName, argumentCount);
	}

	@Override
	public OAPropertyInfo callObjectInfoGetPropertyInfo(OAObjectInfo oi, String propertyName) {
	    return getOAObjectInfoService().getPropertyInfo(oi, propertyName);
	}

	@Override
	public boolean callObjectInfoIsPojoSingleton(OAObjectInfo toObjectInfo) {
	    return getOAObjectInfoService().isPojoSingleton(toObjectInfo);
	}

	
	// InitializeService ======
	@Override
	public boolean callObjectInitializeInitialize(OAObject oaObj) {
	    return getOAObjectInitializeService().initialize(oaObj);
	}

	@Override
	public void callObjectInitializeInitializeAfterLoading(OAObject oaObj) {
	    getOAObjectInitializeService().initializeAfterLoading(oaObj);
	}

	@Override
	public void callObjectInitializeInitializeAfterLoading(OAObject oaObj, boolean bAssignNewId, boolean bInitializeNulls, boolean bSetChangedToFalse) {
	    getOAObjectInitializeService().initializeAfterLoading(oaObj, bAssignNewId, bInitializeNulls, bSetChangedToFalse);
	}

	@Override
	public void callObjectInitializeSetAsNewObject(OAObject oaObj, UUID guid) {
	    getOAObjectInitializeService().setAsNewObject(oaObj, guid);
	}

	@Override
	public void callObjectInitializeSetAsNewObject(OAObject oaObj) {
	    getOAObjectInitializeService().setAsNewObject(oaObj);
	}

	// KeyService ======
	@Override
	public OAObjectKey callObjectKeyGetKey(OAObject oaObj) {
	    return getOAObjectKeyService().getKey(oaObj);
	}

	@Override
	public OAObjectKey callObjectKeyCreateObjectKey(OAObject oaObj) {
	    return getOAObjectKeyService().createObjectKey(oaObj);
	}

	@Override
	public OAObjectKey callObjectKeyCreateObjectKey(Class<? extends OAObject> clazz, final Object ...ids) {
	    return getOAObjectKeyService().createObjectKey(clazz, ids);
	}

	@Override
	public boolean callObjectKeyIsForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey ok1, final OAObjectKey ok2) {
	    return getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
	}

	@Override
	public OAObjectKey callObjectKeyCreateObjectKey(Object id) {
	    return getOAObjectKeyService().createObjectKey(id);
	}

	// LockService ======
	@Override
	public void callObjectLockLock(OAObject oaObj) {
	    getOAObjectLockService().lock(oaObj);
	}

	@Override
	public void callObjectLockUnlock(OAObject oaObj) {
	    getOAObjectLockService().unlock(oaObj);
	}

	@Override
	public boolean callObjectLockIsLocked(OAObject oaObj) {
	    return getOAObjectLockService().isLocked(oaObj);
	}

	// PropertyService ======
	@Override
	public Object callObjectPropertyGetProperty(OAObject oaObj, String name) {
	    return getOAObjectPropertyService().getProperty(oaObj, name);
	}

	@Override
	public Object callObjectPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
	    return getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
	}

	@Override
	public void callObjectPropertySetProperty(OAObject oaObj, String name, Object value) {
	    getOAObjectPropertyService().setProperty(oaObj, name, value);
	}

	@Override
	public void callObjectPropertyRemoveProperty(OAObject oaObj, String name, boolean bFirePropertyChange) {
	    getOAObjectPropertyService().removeProperty(oaObj, name, bFirePropertyChange);
	}

	@Override
	public void callObjectPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist) {
	    getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue, bMustNotExist, bReturnNotExist);
	}

	@Override
	public String[] callObjectPropertyGetPropertyNames(OAObject oaObj) {
	    return getOAObjectPropertyService().getPropertyNames(oaObj);
	}

	@Override
	public boolean callObjectPropertyIsPropertyLoaded(OAObject oaObj, String prop) {
	    return getOAObjectPropertyService().isPropertyLoaded(oaObj, prop);
	}

	@Override
	public boolean callObjectPropertyIsReferenceNull(OAObject oaObj, String prop) {
	    return getOAObjectPropertyService().isReferenceNull(oaObj, prop);
	}

	@Override
	public boolean callObjectPropertyIsPropertyLocked(OAObject oaObj, String prop) {
	    return getOAObjectLockService().isPropertyLocked(oaObj, prop);
	}

	@Override
	public void callObjectPropertySetReferenceable(OAObject oaObj, boolean bIsReferenceable) {
	    getOAObjectPropertyService().setReferenceable(oaObj, bIsReferenceable);
	}

	@Override
	public void callObjectPropertyClearProperties(OAObject oaObj) {
	    getOAObjectPropertyService().clearProperties(oaObj);
	}

	
	// ReflectService ======
	@Override
	public void callObjectReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
	    getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);
	}

	@Override
	public Object callObjectReflectGetProperty(OAObject oaObj, String propName) {
	    return getOAObjectReflectService().getProperty(oaObj, propName);
	}

	@Override
	public OAObject callObjectReflectCreateCopy(OAObject oaObj, String[] excludeProperties) {
	    return getOAObjectReflectService().createCopy(oaObj, excludeProperties);
	}

	@Override
	public void callObjectReflectCopyInto(OAObject oaObj, OAObject newObject, String[] excludeProperties, OACopyCallback copyCallback) {
	    getOAObjectReflectService().copyInto(oaObj, newObject, excludeProperties, copyCallback);
	}

	@Override
	public <T extends OAObject> Hub<T> callObjectReflectGetReferenceHub(final OAObject oaObj, final String linkPropertyName, String sortOrder, boolean bSequence, Hub<T> hubMatch) {
	    return getOAObjectReflectService().getReferenceHub(oaObj, linkPropertyName, sortOrder, bSequence, hubMatch);
	}

	@Override
	public Object callObjectReflectGetReferenceObject(OAObject oaObj, String linkPropertyName) {
	    return getOAObjectReflectService().getReferenceObject(oaObj, linkPropertyName);
	}

	@Override
	public boolean callObjectReflectIsReferenceObjectNullOrEmpty(OAObject oaObj, String name) {
	    return getOAObjectReflectService().isReferenceObjectNullOrEmpty(oaObj, name);
	}

	@Override
	public byte[] callObjectReflectGetReferenceBlob(OAObject oaObj, String linkPropertyName) {
	    return getOAObjectReflectService().getReferenceBlob(oaObj, linkPropertyName);
	}

	@Override
	public boolean callObjectReflectGetPrimitiveNull(OAObject oaObj, String prop) {
	    return getOAObjectReflectService().getPrimitiveNull(oaObj, prop);
	}

	@Override
	public void callObjectReflectLoadAllReferences(OAObject oaObj, boolean bIncludeCalc) {
	    getOAObjectReflectService().loadAllReferences(oaObj, bIncludeCalc);
	}

	@Override
	public void callObjectReflectLoadAllReferences(OAObject oaObj, boolean bOne, boolean bMany, boolean bIncludeCalc) {
	    getOAObjectReflectService().loadAllReferences(oaObj, bOne, bMany, bIncludeCalc);
	}

	@Override
	public void callObjectReflectLoadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc) {
	    getOAObjectReflectService().loadAllReferences(oaObj, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc);
	}

	@Override
	public int callObjectReflectLoadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad) {
	    return getOAObjectReflectService().loadAllReferences(oaObj, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, maxRefsToLoad);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <T extends OAObject> T callObjectReflectGetObject(Class<T> clazz, Object keyValue) {
	    return (T) getOAObjectReflectService().getObject(clazz, keyValue);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <T extends OAObject> T callObjectReflectCreateNewObject(Class<T> clazz) {
	    return (T) getOAObjectReflectService().createNewObject(clazz);
	}

	@Override
	public boolean callObjectReflectAreAllReferencesLoaded(OAObject oaObj, boolean bIncludeCalc) {
	    return getOAObjectReflectService().areAllReferencesLoaded(oaObj, bIncludeCalc);
	}

	@Override
	public boolean callObjectReflectIsReferenceHubLoaded(OAObject oaObj, String hubPropertyName) {
	    return getOAObjectReflectService().isReferenceHubLoaded(oaObj, hubPropertyName);
	}

	@Override
	public String[] callObjectReflectGetUnloadedReferences(OAObject obj, boolean bIncludeCalc, String exceptPropertyName, boolean bIncludeLarge) {
	    return getOAObjectReflectService().getUnloadedReferences(obj, bIncludeCalc, exceptPropertyName, bIncludeLarge);
	}

	@Override
	public String callObjectReflectGetPropertyPathFromMaster(OAObject oaObjParent, Hub<?> hubChild) {
	    return getOAObjectReflectService().getPropertyPathFromMaster(oaObjParent, hubChild);
	}

	@Override
	public Object callObjectReflectGetProperty(Hub<?> hub, String propertyPath) {
	    return getOAObjectReflectService().getProperty(hub, propertyPath);
	}

	@Override
	public OAObjectKey callObjectReflectGetPropertyObjectKey(OAObject oaObj, String propertyName) {
	    return getOAObjectReflectService().getPropertyObjectKey(oaObj, propertyName);
	}

	@Override
	public Object callObjectReflectGetRawReference(OAObject oaObj, String name) {
	    return getOAObjectReflectService().getRawReference(oaObj, name);
	}

	@Override
	public void callObjectReflectLoadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad, long maxEndTime) {
	    getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, maxRefsToLoad, maxEndTime);
	}

	
	// SaveService ======
	@Override
	public void callObjectSaveSave(OAObject oaObj, int iCascadeRule) {
	    getOAObjectSaveService().save(oaObj, iCascadeRule);
	}

	@Override
	public void callObjectSaveSave(OAObject obj, int iCascadeRule, OACascade cascade) {
	    getOAObjectSaveService().save(obj, iCascadeRule, cascade);
	}

	
	// SchedulerService ======
	@Override
	public OAScheduler callObjectSchedulerGetScheduler(OAObject oaObj, String property, OADate date) {
	    return getOAObjectSchedulerService().getScheduler(oaObj, property, date);
	}

	
	// SerializeService ======
	@Override
	public void callObjectSerializeReadObject(OAObject oaObj, ObjectInputStream in) throws IOException, ClassNotFoundException {
	    getOAObjectSerializeService()._readObject(oaObj, in);
	}

	@Override
	public Object callObjectSerializeReadResolve(OAObject oaObj) throws ObjectStreamException {
	    return getOAObjectSerializeService()._readResolve(oaObj);
	}

	@Override
	public void callObjectSerializeWriteObject(OAObject oaObj, ObjectOutputStream stream) throws IOException {
	    getOAObjectSerializeService()._writeObject(oaObj, stream);
	}

	// SiblingService ======
	@Override
	public OAObjectKey[] callObjectSiblingGetSiblings(OAObject oaObj, String property, int maxAmount, ConcurrentHashMap<UUID, Boolean> hmIgnoreSibling) {
	    return getOAObjectSiblingService().getSiblings(oaObj, property, maxAmount, hmIgnoreSibling);
	}

	// UniqueService ======
	@Override
	public <T extends OAObject> T callObjectUniqueGetUnique(Class<T> clazz, String propertyName, Object uniqueKey, boolean bAutoCreate) {
	    return (T) getOAObjectUniqueService().getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
	}

	// XMLService ======
	@Override
	public void callObjectXMLWrite(OAObject obj, OAXMLWriter oaxmlWriter, String tagName, boolean bKeyOnly, OACascade cascade) {
	    getOAObjectXMLService().write(obj, oaxmlWriter, tagName, bKeyOnly, cascade);
	}

}


