package com.viaoa.graph.api.internal;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.annotation.OAMany;
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
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.scheduler.OAScheduler;
import com.viaoa.util.OADate;
import com.viaoa.util.OAFilter;
import com.viaoa.xml.OAXMLWriter;


/**
 * 
 *  methods needed internally by OA and OA tools.  Used by OG.Objects (OAObjectService)
 *  
 *  
 */
public interface ObjectsInternalOps {
	
	// Object (itself)
	public void callObjectSetNew(OAObject oaObj, boolean bIsNew);
	public boolean callObjectChangeGetChanged(OAObject oaObj, int cascadeRule);
	public void callObjectSetAutoAdd(OAObject oaObj, boolean bAutoAdd);
	public boolean callObjectGetAutoAdd(OAObject oaObj);
	public Object[] callObjectFind(OAObject oaObjBase, String propertyPath, Object findValue, boolean bFindAll);

	// Annotation
	public Class<? extends OAObject> callObjectAnnotationGetHubObjectClass(OAMany annotation, Method method);
	
	// Cache
	public void callObjectCacheFireAfterLoadEvent(OAObject oaObj);
	public Class<? extends OAObject>[] callObjectCacheGetClasses();
	public <T extends OAObject> void callObjectCacheCallback(Class<T> clazz, OACallback<T> callback);
	public int callObjectCacheGetTotal(Class<? extends OAObject> clazz);
	public <T extends OAObject> void callObjectCacheAddListener(Class<T> clazz, OAObjectCacheListener<T> cachelistener);
	public <T extends OAObject> void callObjectCacheVisit(Class<T> clazz, OACallback<T> callback);
	public <T extends OAObject> void callObjectCacheRemoveListener(Class<T> clazz, OAObjectCacheListener<T> cacheListener);
	public <T extends OAObject> Hub<T> callObjectCacheGetSelectAllHub(Class<T> clazz);
	public <T extends OAObject> T callObjectCacheFind(Class<T> clazz, OAFinder<T, T> finder);
	public <T extends OAObject> T callObjectCacheGet(Class<T> clazz, OAObjectKey objectKey);
	public <T extends OAObject> T callObjectCacheGetObject(Class<T> clazz, Object object);
	public void callObjectCacheRemoveObject(OAObject oaObj);
	public void callObjectCacheRefresh(Class<? extends OAObject> clazz);
	public void callObjectCacheRemoveAllObjects(Class<? extends OAObject> clazz);
	public <T extends OAObject> T callObjectCacheFind(T fromObject, Class<T> clazz, OAFilter<T> filter, boolean bSkipNew, boolean bThrowException, int fetchAmount, List<T> alResults);
	public <T extends OAObject> T callObjectCacheAdd(T oaObj, boolean bErrorIfExists, boolean bAddToSelectAll);

	
	// Callback
	public boolean callObjectCallbackGetVerifyPropertyChange(int checkType, OAObject obj, String propertyName, Object oldValue, Object newValue);
	public OAObjectCallback callObjectCallbackGetVerifyPropertyChangeObjectCallback(int checkType, OAObject oaObj, String propertyName, Object oldValue, Object newValue);
	public <T extends OAObject> boolean callObjectCallbackGetAllowEnabled(int checkType, Hub<T> hub, T obj, String name);
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowEnabledObjectCallback(int checkType, Hub<T> hub, T oaObj, String name);
	public <T extends OAObject> boolean callObjectCallbackGetAllowVisible(Hub<T> hub, T oaObj, String name);
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowVisibleObjectCallback(Hub<T> hub, T oaObj, String name);
	public OAObjectCallback callObjectCallbackGetVerifyCommandObjectCallback(OAObject oaObj, String methodName, int checkType);
	public OAObjectCallback callObjectCallbackGetAllowSubmitObjectCallback(OAObject oaObj);
	public OAObjectCallback callObjectCallbackGetVerifySaveObjectCallback(OAObject oaObj, int checkType);
	public boolean callObjectCallbackGetAllowSave(OAObject oaObj, int checkType);
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetVerifyDeleteObjectCallback(Hub<T> hub, T objDelete, int checkType);
	public <T extends OAObject> boolean callObjectCallbackGetAllowDelete(Hub<T> hub, T oaObj);
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowAddObjectCallback(Hub<T> hub, T objAdd, int checkType);
    public <T extends OAObject> void callObjectCallbackAddObjectCallbackChangeListeners(Hub<T> hub, Class<T> cz, String prop, String ppPrefix, HubChangeListener changeListener, boolean bEnabled);
	public OAObjectCallback callObjectCallbackGetAllowNewObjectCallback(Hub<? extends OAObject> hub);
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowDeleteObjectCallback(Hub<T> hub, T obj);
	public OAObjectCallback callObjectCallbackGetAllowCopyObjectCallback(OAObject obj);
	public OAObjectCallback callObjectCallbackGetAllowEnabledObjectCallback(Hub<? extends OAObject> hub);
	public OAObjectCallback callObjectCallbackGetAllowSaveObjectCallback(OAObject obj, int checkType);
	public OAObjectCallback callObjectCallbackGetAllowDeleteObjectCallback(OAObject ao);
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowRemoveObjectCallback(Hub<T> hub, T objRemove, int checkType);
	public OAObjectCallback callObjectCallbackGetAllowRemoveAllObjectCallback(Hub<? extends OAObject> hub, int checkType);
	public <T extends OAObject> T callObjectCallbackGetCopy(T obj);
	public OAObjectCallback callObjectCallbackGetConfirmPropertyChangeObjectCallback(OAObject oaObj, String property, Object newValue, String confirmMessage, String confirmTitle);
	public OAObjectCallback callObjectCallbackGetConfirmSaveObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle);
	public OAObjectCallback callObjectCallbackGetConfirmDeleteObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle);
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetConfirmRemoveObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle);
	public OAObjectCallback callObjectCallbackGetConfirmRemoveAllObjectCallback(Hub<? extends OAObject> hub, String confirmMessage, String confirmTitle);
	public <T extends OAObject> OAObjectCallback callObjectCallbackGetConfirmAddObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle);
	public String callObjectCallbackGetFormat(OAObject obj, String propertyName, String defaultFormat);
	public String callObjectCallbackGetToolTip(OAObject obj, String propertyName, String defaultToolTip);
	public OAObjectCallback callObjectCallbackGetConfirmCommandObjectCallback(OAObject oaObj, String methodName, String confirmMessage, String confirmTitle);
	public OAObjectCallback callObjectCallbackGetAllowVisibleObjectCallback(Hub<? extends OAObject> hub);
	public <T extends OAObject> boolean callObjectCallbackGetAllowAdd(Hub<T> hub, T obj, int checkType);
	public <T extends OAObject> boolean callObjectCallbackGetAllowRemove(Hub<T> hub, T obj, int checkType);
	public <T extends OAObject> boolean callObjectCallbackGetVerifyRemove(Hub<T> hub, T obj, int checkType);

	
	// CS
	public void callObjectCSObjectFinalized(UUID guid);
	public <T extends OAObject> Hub<T> callObjectCSGetServerReferenceHub(T oaObj, String linkPropertyName);
	public boolean callObjectCSIsServer(OAObject oaObj);
	public void callObjectCSUpdateObjectsWithoutHubs(OAObject oaObj);
	
	// Delete
	public void callObjectDeleteSetDeleted(OAObject oaObj, boolean bDeleted);
	public void callObjectDeleteDelete(OAObject oaObj);
	public void callObjectDeleteSyncServerDelete(OAObject obj);
	public void callObjectDeleteSyncClientDelete(OAObject obj);

	// DS
	public boolean callObjectDSGetAssignIdOnCreate(OAObject oaObj);
	public void callObjectDSAssignId(OAObject oaObj);
	public void callObjectDSSetAssigningId(OAObject oaObj, boolean bIsAssigningId);

	// Empty
	public void callObjectEmptyHubInitialize(OAObject oaObj);

	
	// Enum
	public Hub<VEnum> callObjectEnumGetVEnums(Class<? extends OAObject> clazz, String propertyName);
	
	// Event
	public void callObjectEventFireBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged);
	public void callObjectEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged);
	public void callObjectEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged, boolean bUnknownValues);
	public void callObjectEventFireAfterLoadEvent(OAObject oaObj);
	
	// GUID
	public void callObjectGuidSetGuid(OAObject oaObj, UUID iguid);
	public UUID callObjectGuidGetGuid(OAObject oaObj);
	
	// Hub
	public WeakReference<Hub<? extends OAObject>>[] callObjectHubGetHubReferencesNoCopy(OAObject oaObj);
	public <T extends OAObject> Hub<T>[] callObjectHubGetHubReferences(T oaObj);
	public <T extends OAObject> boolean callObjectHubAddHub(T oaObj, Hub<T> hub, boolean bAlwaysAddIfM2M);
	public boolean callObjectHubIsInHubWithMaster(OAObject obj);
	public <T extends OAObject> void callObjectHubRemoveHub(final T oaObj, Hub<T> hub, boolean bIsOnHubFinalize);
	
	// Info
	public OAObjectInfo callObjectInfoGetOAObjectInfo(Class<?> clazz);
	public Class<? extends OAObject> callObjectInfoGetHubPropertyClass(Class<? extends OAObject> clazz, String propertyName);
	public Class<?> callObjectInfoGetPropertyClass(Class<? extends OAObject> clazz, String propertyName);
	public Class<?> callObjectInfoGetPropertyClass(OAObjectInfo oi, String propertyName);
	public boolean callObjectInfoIsHubProperty(OAObjectInfo oi, String propertyName);
	public OACalcInfo callObjectInfoGetCalcInfo(OAObjectInfo oi, String name);
	public OALinkInfo callObjectInfoGetLinkInfo(OAObjectInfo oi, String propertyName);
	public OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject oaObj);
	public boolean callObjectInfoCacheHub(OALinkInfo linkInfo, Hub<?> hub);
	public Method callObjectInfoGetMethod(OAObjectInfo oi, String string);
	public OALinkInfo callObjectInfoGetRecursiveLinkInfo(OAObjectInfo oi, int type);
	public Method callObjectInfoGetMethod(Class<?> clazz, String methodName);
	public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo li);
	public OAObjectInfo callObjectInfoGetObjectInfo(Class<?> clazz);
	public boolean callObjectInfoIsMany2Many(OALinkInfo li);
	public OALinkInfo callObjectInfoGetLinkInfo(Class<? extends OAObject> clazz, String property);
	public Method callObjectInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount);
	public OAPropertyInfo callObjectInfoGetPropertyInfo(OAObjectInfo oi, String propertyName);
	public boolean callObjectInfoIsPojoSingleton(OAObjectInfo toObjectInfo);
	
	// Initialize
	public boolean callObjectInitializeInitialize(OAObject oaObj);
	public void callObjectInitializeInitializeAfterLoading(OAObject oaObj);
	public void callObjectInitializeInitializeAfterLoading(OAObject oaObj, boolean bAssignNewId, boolean bInitializeNulls, boolean bSetChangedToFalse);
/*qqqqqqq remove	
	public void callObjectInitializeSetAsNewObject(OAObject oaObj, UUID guid);
	public void callObjectInitializeSetAsNewObject(OAObject oaObj);
*/	
	
	// Key
	public OAObjectKey callObjectKeyGetKey(OAObject oaObj);
	public OAObjectKey callObjectKeyCreateObjectKey(OAObject oaObj);
	public OAObjectKey callObjectKeyCreateObjectKey(Class<? extends OAObject> clazz, final Object ...ids);
	public boolean callObjectKeyIsForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey ok1, final OAObjectKey ok2);
	public OAObjectKey callObjectKeyCreateObjectKey(Object id);
	
	// Lock
	public void callObjectLockLock(OAObject oaObj);
	public void callObjectLockUnlock(OAObject oaObj);
	public boolean callObjectLockIsLocked(OAObject oaObj);
	
	// Property
	public Object callObjectPropertyGetProperty(OAObject oaObj, String name);
	public Object callObjectPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef);
	public void callObjectPropertySetProperty(OAObject oaObj, String name, Object value);
	public void callObjectPropertyRemoveProperty(OAObject oaObj, String name, boolean bFirePropertyChange);
	public void callObjectPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist);
	
	public String[] callObjectPropertyGetPropertyNames(OAObject oaObj);
	public boolean callObjectPropertyIsPropertyLoaded(OAObject oaObj, String prop);
	public boolean callObjectPropertyIsReferenceNull(OAObject oaObj, String prop);
	public boolean callObjectPropertyIsPropertyLocked(OAObject oaObj, String prop);
	public void callObjectPropertySetReferenceable(OAObject oaObj, boolean bIsReferenceable);
	public void callObjectPropertyClearProperties(OAObject oaObj);

	// Reflect
	public void callObjectReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt);
	public Object callObjectReflectGetProperty(OAObject oaObj, String propName);
	public OAObject callObjectReflectCreateCopy(OAObject oaObj, String[] excludeProperties);
	public void callObjectReflectCopyInto(OAObject oaObj, OAObject newObject, String[] excludeProperties, OACopyCallback copyCallback);
	public <T extends OAObject> Hub<T> callObjectReflectGetReferenceHub(final OAObject oaObj, final String linkPropertyName, String sortOrder, boolean bSequence, Hub<T> hubMatch);
	public Object callObjectReflectGetReferenceObject(OAObject oaObj, String linkPropertyName);
	public boolean callObjectReflectIsReferenceObjectNullOrEmpty(OAObject oaObj, String name);
	public byte[] callObjectReflectGetReferenceBlob(OAObject oaObj, String linkPropertyName);
	public boolean callObjectReflectGetPrimitiveNull(OAObject oaObj, String prop);
	public void callObjectReflectLoadAllReferences(OAObject oaObj, boolean bIncludeCalc);
	public void callObjectReflectLoadAllReferences(OAObject oaObj, boolean bOne, boolean bMany, boolean bIncludeCalc);
	public void callObjectReflectLoadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc);
	public int callObjectReflectLoadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad);
	public <T extends OAObject> T callObjectReflectGetObject(Class<T> clazz, Object keyValue); //qqqqqqqqq add this to graph.objects().getObject(c, k)
	public <T extends OAObject> T callObjectReflectCreateNewObject(Class<T> clazz);
	public boolean callObjectReflectAreAllReferencesLoaded(OAObject oaObj, boolean bIncludeCalc);
	public boolean callObjectReflectIsReferenceHubLoaded(OAObject oaObj, String hubPropertyName);
	public String[] callObjectReflectGetUnloadedReferences(OAObject obj, boolean bIncludeCalc, String exceptPropertyName, boolean bIncludeLarge);
	public String callObjectReflectGetPropertyPathFromMaster(OAObject oaObjParent, Hub<?> hubChild);
	public Object callObjectReflectGetProperty(Hub<?> hub, String propertyPath);
	public OAObjectKey callObjectReflectGetPropertyObjectKey(OAObject oaObj, String propertyName);
	public Object callObjectReflectGetRawReference(OAObject oaObj, String name);
	public void callObjectReflectLoadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad, long maxEndTime);
	
	// Save
	public void callObjectSaveSave(OAObject oaObj, int iCascadeRule);
	public void callObjectSaveSave(OAObject obj, int iCascadeRule, OACascade cascade);

	// Scheduler
	public OAScheduler callObjectSchedulerGetScheduler(OAObject oaObj, String property, OADate date);
	
	// Serialize
	public void callObjectSerializeReadObject(OAObject oaObj, ObjectInputStream in) throws IOException, ClassNotFoundException;
	public Object callObjectSerializeReadResolve(OAObject oaObj) throws ObjectStreamException;
	public void callObjectSerializeWriteObject(OAObject oaObj, ObjectOutputStream stream) throws IOException;

	// Sibling
	public OAObjectKey[] callObjectSiblingGetSiblings(OAObject oaObj, String property, int maxAmount, ConcurrentHashMap<UUID, Boolean> hmIgnoreSibling);
	
	// Unique
	public <T extends OAObject> T callObjectUniqueGetUnique(Class<T> clazz, String propertyName, Object uniqueKey, boolean bAutoCreate);

	// XML
	public void callObjectXMLWrite(OAObject obj, OAXMLWriter oaxmlWriter, String tagName, boolean bKeyOnly, OACascade cascade);
}
