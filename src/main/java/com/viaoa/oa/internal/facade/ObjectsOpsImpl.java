package com.viaoa.oa.internal.facade;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.annotation.OAMany;
import com.viaoa.cache.OAObjectCacheListener;
import com.viaoa.callback.OACallback;
import com.viaoa.callback.OACallbackLabel;
import com.viaoa.callback.OACopyCallback;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.datetime.OADate;
import com.viaoa.filter.OAFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.hub.listener.HubChangeListener;
import com.viaoa.lang.oa.VEnum;
import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAObjectModel;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.oa.api.internal.ObjectsOps;
import com.viaoa.oa.api.internal.objects.OAObjectAnnotationOps;
import com.viaoa.oa.api.internal.objects.OAObjectAutoAddOps;
import com.viaoa.oa.api.internal.objects.OAObjectCSOps;
import com.viaoa.oa.api.internal.objects.OAObjectCacheOps;
import com.viaoa.oa.api.internal.objects.OAObjectRulesOps;
import com.viaoa.oa.api.internal.objects.OAObjectChangeOps;
import com.viaoa.oa.api.internal.objects.OAObjectDSOps;
import com.viaoa.oa.api.internal.objects.OAObjectDeleteOps;
import com.viaoa.oa.api.internal.objects.OAObjectEnumOps;
import com.viaoa.oa.api.internal.objects.OAObjectEventOps;
import com.viaoa.oa.api.internal.objects.OAObjectFindOps;
import com.viaoa.oa.api.internal.objects.OAObjectGuidOps;
import com.viaoa.oa.api.internal.objects.OAObjectHubOps;
import com.viaoa.oa.api.internal.objects.OAObjectInfoOps;
import com.viaoa.oa.api.internal.objects.OAObjectInitializeOps;
import com.viaoa.oa.api.internal.objects.OAObjectKeyOps;
import com.viaoa.oa.api.internal.objects.OAObjectLockOps;
import com.viaoa.oa.api.internal.objects.OAObjectPropertyOps;
import com.viaoa.oa.api.internal.objects.OAObjectRecurseOps;
import com.viaoa.oa.api.internal.objects.OAObjectReflectOps;
import com.viaoa.oa.api.internal.objects.OAObjectSaveOps;
import com.viaoa.oa.api.internal.objects.OAObjectSchedulerOps;
import com.viaoa.oa.api.internal.objects.OAObjectSerializeOps;
import com.viaoa.oa.api.internal.objects.OAObjectSiblingOps;
import com.viaoa.oa.api.internal.objects.OAObjectStateOps;
import com.viaoa.oa.api.internal.objects.OAObjectUniqueOps;
import com.viaoa.oa.service.object.OAObjectParentService;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.schedule.OAScheduler;

/**
 * Internal facade implementation for OAObject operation families exposed through {@code OA.internal().objects()}.
 */
public class ObjectsOpsImpl implements ObjectsOps {

	private OAObjectParentService srvc;
	
	private OAObjectAnnotationOps opsAnnotation;
	private OAObjectAutoAddOps opsAutoAdd;
	private OAObjectCacheOps opsCache;
	private OAObjectReflectOps opsReflect;
	private OAObjectRulesOps opsCallback;
	private OAObjectChangeOps opsChange;
	private OAObjectDeleteOps opsDelete;
	private OAObjectCSOps opsCS;
	private OAObjectDSOps opsDS;
	private OAObjectEnumOps opsEnum;
	private OAObjectEventOps opsEvent;
	private OAObjectFindOps opsFind;
	private OAObjectGuidOps opsGuid;
	private OAObjectHubOps opsHub;
	private OAObjectInfoOps opsInfo;
	private OAObjectInitializeOps opsInitialize;
	private OAObjectKeyOps opsKey;
	private OAObjectLockOps opsLock;
	private OAObjectPropertyOps opsProperty;
	private OAObjectSaveOps opsSave;
	private OAObjectSchedulerOps opsScheduler;
	private OAObjectSerializeOps opsSerialize;
	private OAObjectSiblingOps opsSibling;
	private OAObjectStateOps opsState;
	private OAObjectUniqueOps opsUnique;
	private OAObjectRecurseOps opsRecurse;
	
	
	/**
	 * Creates the internal OAObject facade backed by an object parent service.
	 *
	 * @param srvcObjectInternal the object parent service that owns the concrete object services
	 */
	public ObjectsOpsImpl(OAObjectParentService srvcObjectInternal) {
		this.srvc = srvcObjectInternal;
	}

	@Override
	/**
	 * Returns the internal OAObject annotation facade.
	 *
	 * @return the annotation operations facade
	 */
	public OAObjectAnnotationOps annotation() {
		if (opsAnnotation != null) return opsAnnotation;
		opsAnnotation = new OAObjectAnnotationOps() {
			@Override
			public Class<? extends OAObject> getHubObjectClass(OAMany annotation, Method method) {
				return srvc.getOAObjectAnnotationService().getHubObjectClass(annotation, method);
			}
		};
		return opsAnnotation;
	}
	
	
	@Override
	/**
	 * Returns the internal OAObject auto-add facade.
	 *
	 * @return the auto-add operations facade
	 */
	public OAObjectAutoAddOps autoAdd() {
		if (opsAutoAdd != null) return opsAutoAdd;
		opsAutoAdd = new OAObjectAutoAddOps() {
			@Override
			public void setAutoAdd(OAObject oaObj, boolean bAutoAdd) {
				srvc.getOAObjectAutoAddService().setAutoAdd(oaObj, bAutoAdd);
			}
			
			@Override
			public boolean getAutoAdd(OAObject oaObj) {
				return srvc.getOAObjectAutoAddService().getAutoAdd(oaObj);
			}
		};
		return opsAutoAdd;
	}
	
	@Override
	/**
	 * Returns the internal OAObject cache facade.
	 *
	 * @return the cache operations facade
	 */
	public OAObjectCacheOps cache() {
		if (opsCache != null) return opsCache;
		
		opsCache = new OAObjectCacheOps() {
			@Override
			public void fireAfterLoadEvent(OAObject oaObj) {
				srvc.getOAObjectCacheService().fireAfterLoadEvent(oaObj);
			}

			@Override
			public Class<? extends OAObject>[] getClasses() {
				return srvc.getOAObjectCacheService().getClasses();
			}

			@Override
			public <T extends OAObject> void callback(Class<T> clazz, OACallback<T> callback) {
				srvc.getOAObjectCacheService().callback(clazz, callback);
			}

			@Override
			public int getTotal(Class<? extends OAObject> clazz) {
				return srvc.getOAObjectCacheService().getTotal(clazz);
			}

			@Override
			public <T extends OAObject> void addListener(Class<T> clazz, OAObjectCacheListener<T> cachelistener) {
				srvc.getOAObjectCacheService().addListener(clazz, cachelistener);
			}

			@Override
			public <T extends OAObject> void visit(Class<T> clazz, OACallback<T> callback) {
				srvc.getOAObjectCacheService().visit(clazz, callback);
			}

			@Override
			public <T extends OAObject> void removeListener(Class<T> clazz, OAObjectCacheListener<T> cacheListener) {
				srvc.getOAObjectCacheService().removeListener(clazz, cacheListener);
			}

			@Override
			public <T extends OAObject> Hub<T> getSelectAllHub(Class<T> clazz) {
				return srvc.getOAObjectCacheService().getSelectAllHub(clazz);
			}

			@Override
			public <T extends OAObject> T find(Class<T> clazz, OAFinder<T, T> finder) {
				return srvc.getOAObjectCacheService().find(clazz, finder);
			}

			@Override
			public <T extends OAObject> T get(Class<T> clazz, OAObjectKey objectKey) {
				return srvc.getOAObjectCacheService().get(clazz, objectKey);
			}

			@Override
			public <T extends OAObject> T getObject(Class<T> clazz, Object object) {
				return srvc.getOAObjectCacheService().getObject(clazz, object);
			}

			@Override
			public void removeObject(OAObject oaObj) {
				srvc.getOAObjectCacheService().removeObject(oaObj);
			}

			@Override
			public void refresh(Class<? extends OAObject> clazz) {
				srvc.getOAObjectCacheService().refresh(clazz);
			}

			@Override
			public void removeAllObjects(Class<? extends OAObject> clazz) {
				srvc.getOAObjectCacheService().removeAllObjects(clazz);
			}

			@Override
			public <T extends OAObject> T find(T fromObject, Class<T> clazz, OAFilter<T> filter, boolean bSkipNew, boolean bThrowException, int fetchAmount, List<T> alResults) {
				return srvc.getOAObjectCacheService().find(fromObject, clazz, filter, bSkipNew, bThrowException, fetchAmount, alResults);
			}

			@Override
			public <T extends OAObject> T add(T oaObj, boolean bErrorIfExists, boolean bAddToSelectAll) {
				return srvc.getOAObjectCacheService().add(oaObj, bErrorIfExists, bAddToSelectAll);
			}

			@Override
			public <T extends OAObject> T find(T fromObject, Class<T> clazz, int fetchAmount, List<T> alResults) {
				return srvc.getOAObjectCacheService().find(fromObject, clazz, fetchAmount, alResults);
			}

			@Override
			public <T extends OAObject> void setSelectAllHub(Hub<T> hub) {
				srvc.getOAObjectCacheService().setSelectAllHub(hub);
			}

			@Override
			public <T extends OAObject> void removeSelectAllHub(Hub<T> hub) {
				srvc.getOAObjectCacheService().removeSelectAllHub(hub);
			}

			@Override
			public void getInfo(List<String> al) {
				srvc.getOAObjectCacheService().getInfo(al);
			}

			@Override
			public OAObject getRandom(Class<? extends OAObject> clazz, int max) {
				return srvc.getOAObjectCacheService().getRandom(clazz, max);
			}
		};
		return opsCache;
	}

	@Override
	/**
	 * Returns the internal OAObject reflection facade.
	 *
	 * @return the reflection operations facade
	 */
	public OAObjectReflectOps reflect() {
		if (opsReflect != null) return opsReflect;
		
		opsReflect = new OAObjectReflectOps() {
			@Override
			public String getPathFromMaster(OAObject objParent, Hub<?> hubChild) {
				return srvc.getOAObjectReflectService().getPropertyFromMaster(objParent, hubChild);
			}

			@Override
			public Object getProperty(OAObject oaObj, String propPath) {
				return srvc.getOAObjectReflectService().getProperty(oaObj, propPath);
			}

			@Override
			public Object getProperty(Hub<?> hub, String propPath) {
				return srvc.getOAObjectReflectService().getProperty(hub, propPath);
			}

			@Override
			public void setProperty(OAObject oaObj, String propName, Object value, String fmt) {
				srvc.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);
			}

			@Override
			public OAObject createCopy(OAObject oaObj, String[] excludeProperties) {
				return srvc.getOAObjectReflectService().createCopy(oaObj, excludeProperties);
			}

			public OAObject _createCopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback,
					Map<UUID, OAObject> hmNew) {
				return srvc.getOAObjectReflectService()._createCopy(oaObj, excludeProperties, copyCallback, hmNew);
			}
			
			
			@Override
			public void copyInto(OAObject oaObj, OAObject newObject, String[] excludeProperties, OACopyCallback copyCallback) {
				srvc.getOAObjectReflectService().copyInto(oaObj, newObject, excludeProperties, copyCallback);
			}

			@Override
			public <T extends OAObject> Hub<T> getReferenceHub(OAObject oaObj, String linkPropertyName, String sortOrder, boolean bSequence, Hub<T> hubMatch) {
				return srvc.getOAObjectReflectService().getReferenceHub(oaObj, linkPropertyName, sortOrder, bSequence, hubMatch);
			}

			@Override
			public Object getReferenceObject(OAObject oaObj, String linkPropertyName) {
				return srvc.getOAObjectReflectService().getReferenceObject(oaObj, linkPropertyName);
			}

			@Override
			public boolean isReferenceObjectNullOrEmpty(OAObject oaObj, String name) {
				return srvc.getOAObjectReflectService().isReferenceObjectNullOrEmpty(oaObj, name);
			}

			@Override
			public byte[] getReferenceBlob(OAObject oaObj, String linkPropertyName) {
				return srvc.getOAObjectReflectService().getReferenceBlob(oaObj, linkPropertyName);
			}

			@Override
			public boolean getPrimitiveNull(OAObject oaObj, String prop) {
				return srvc.getOAObjectReflectService().getPrimitiveNull(oaObj, prop);
			}

			@Override
			public void setPrimitiveNull(OAObject oaObj, String prop, boolean b) {
				srvc.getOAObjectReflectService().setPrimitiveNull(oaObj, prop, b);
			}

			@Override
			public int loadAllReferences(OAObject oaObj, boolean bIncludeCalc) {
				return srvc.getOAObjectReflectService().loadAllReferences(oaObj, bIncludeCalc);
			}

			@Override
			public int loadAllReferences(OAObject oaObj, boolean bOne, boolean bMany, boolean bIncludeCalc) {
				return srvc.getOAObjectReflectService().loadAllReferences(oaObj, bOne, bMany, bIncludeCalc);
			}

			@Override
			public int loadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc) {
				return srvc.getOAObjectReflectService().loadAllReferences(oaObj, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc);
			}

			@Override
			public int loadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad) {
				return srvc.getOAObjectReflectService().loadAllReferences(oaObj, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, maxRefsToLoad);
			}

			@Override
			public <T extends OAObject> T getObject(Class<T> clazz, Object keyValue) {
				return srvc.getOAObjectReflectService().getObject(clazz, keyValue);
			}

			@Override
			public <T extends OAObject> T createNewObject(Class<T> clazz) {
				return srvc.getOAObjectReflectService().createNewObject(clazz);
			}

			@Override
			public boolean areAllReferencesLoaded(OAObject oaObj, boolean bIncludeCalc) {
				return srvc.getOAObjectReflectService().areAllReferencesLoaded(oaObj, bIncludeCalc);
			}

			@Override
			public boolean isReferenceHubLoaded(OAObject oaObj, String hubPropertyName) {
				return srvc.getOAObjectReflectService().isReferenceHubLoaded(oaObj, hubPropertyName);
			}

			@Override
			public String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc, String exceptPropertyName, boolean bIncludeLarge) {
				return srvc.getOAObjectReflectService().getUnloadedReferences(obj, bIncludeCalc, exceptPropertyName, bIncludeLarge);
			}

			@Override
			public OAObjectKey getPropertyObjectKey(OAObject oaObj, String propertyName) {
				return srvc.getOAObjectReflectService().getPropertyObjectKey(oaObj, propertyName);
			}

			@Override
			public Object getRawReference(OAObject oaObj, String name) {
				return srvc.getOAObjectReflectService().getRawReference(oaObj, name);
			}

			@Override
			public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad, long maxEndTime) {
				return srvc.getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, maxRefsToLoad, maxEndTime);
			}

			@Override
			public String getPathBetweenHubs(Hub<?> hubParent, Hub<?> hubChild) {
				return srvc.getOAObjectReflectService().getPathBetweenHubs(hubParent, hubChild);
			}

			@Override
			public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, OACascade cascade, int maxRefsToLoad) {
				return srvc.getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, cascade, maxRefsToLoad);
			}

			@Override
			public <T extends OAObject> void _copyInto(T oaObj, T newObject, String[] excludeProperties, OACopyCallback copyCallback, Map<UUID, OAObject> hmNew) {
				srvc.getOAObjectReflectService()._copyInto(oaObj, newObject, excludeProperties, copyCallback, hmNew);
			}

			@Override
			public OAObject createCopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback) {
				return srvc.getOAObjectReflectService().createCopy(oaObj, excludeProperties, copyCallback);
			}
		};
		return opsReflect;
	}

	@Override
	/**
	 * Returns the internal OAObject rules facade backed by {@code OAObjectRulesService}.
	 *
	 * @return the rules operations facade
	 */
	public OAObjectRulesOps rules() {
		if (opsCallback != null) return opsCallback;
		opsCallback = new OAObjectRulesOps() {
			@Override
			public <T extends OAObject> void addObjectCallbackChangeListeners(Hub<T> hub, Class<T> cz, String prop, String ppPrefix, HubChangeListener changeListener, boolean bEnabled) {
				srvc.getOAObjectRulesService().addObjectCallbackChangeListeners(hub, cz, prop, ppPrefix, changeListener, bEnabled);
			}

			@Override
			public OAObjectCallback getConfirmPropertyChangeObjectCallback(OAObject oaObj, String property, Object newValue, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectRulesService().getConfirmPropertyChangeObjectCallback(oaObj, property, newValue, confirmMessage, confirmTitle);
			}

			@Override
			public OAObjectCallback getConfirmDeleteObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectRulesService().getConfirmDeleteObjectCallback(oaObj, confirmMessage, confirmTitle);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getConfirmRemoveObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectRulesService().getConfirmRemoveObjectCallback(hub, oaObj, confirmMessage, confirmTitle);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getConfirmAddObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectRulesService().getConfirmAddObjectCallback(hub, oaObj, confirmMessage, confirmTitle);
			}

			@Override
			public OAObjectCallback getAllowNewObjectCallback(Hub<? extends OAObject> hub) {
				return srvc.getOAObjectRulesService().getAllowNewObjectCallback(hub);
			}

			@Override
			public OAObjectCallback getVerifySaveObjectCallback(OAObject oaObj) {
				return srvc.getOAObjectRulesService().getVerifySaveObjectCallback(oaObj);
			}

			@Override
			public OAObjectCallback getConfirmSaveObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectRulesService().getConfirmSaveObjectCallback(oaObj, confirmMessage, confirmTitle);
			}

			@Override
			public OAObjectCallback getVerifyCommandObjectCallback(OAObject oaObj, String methodName) {
				return srvc.getOAObjectRulesService().getVerifyCommandObjectCallback(oaObj, methodName);
			}

			@Override
			public OAObjectCallback getConfirmCommandObjectCallback(OAObject oaObj, String methodName, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectRulesService().getConfirmCommandObjectCallback(oaObj, methodName, confirmMessage, confirmTitle);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getVerifyDeleteObjectCallback(Hub<T> hub, T objDelete) {
				return srvc.getOAObjectRulesService().getVerifyDeleteObjectCallback(hub, objDelete);
			}

			@Override
			public OAObjectCallback getVerifyPropertyChangeCallbackOnlyObjectCallback(OAObject oaObj, String propertyName, Object oldValue, Object newValue) {
				return srvc.getOAObjectRulesService().getVerifyPropertyChangeCallbackOnlyObjectCallback(oaObj, propertyName, oldValue, newValue);
			}

			@Override
			public <T extends OAObject> T getCopy(T oaObj) {
				return srvc.getOAObjectRulesService().getCopy(oaObj);
			}

			@Override
			public <T extends OAObject> boolean getAllowRemove(Hub<T> hub, T obj) {
				return srvc.getOAObjectRulesService().getAllowRemove(hub, obj);
			}
			@Override
			public <T extends OAObject> boolean getAllowRemoveCallbackOnly(Hub<T> hub, T obj) {
				return srvc.getOAObjectRulesService().getAllowRemoveCallbackOnly(hub, obj);
			}
			@Override
			public <T extends OAObject> boolean getAllowRemoveIgnoreProcessed(Hub<T> hub, T obj) {
				return srvc.getOAObjectRulesService().getAllowRemoveIgnoreProcessed(hub, obj);
			}

			
			@Override
			public <T extends OAObject> boolean getAllowDelete(Hub<T> hub, T obj) {
				return srvc.getOAObjectRulesService().getAllowDelete(hub, obj);
			}

			@Override
			public <T extends OAObject> boolean getAllowDelete(T obj) {
				return srvc.getOAObjectRulesService().getAllowDelete(obj);
			}
			
			@Override
			public <T extends OAObject> boolean getAllowAdd(Hub<T> hub, T obj) {
				return srvc.getOAObjectRulesService().getAllowAdd(hub, obj);
			}
			@Override
			public <T extends OAObject> boolean getAllowAddIgnoreProcessed(Hub<T> hub, T obj) {
				return srvc.getOAObjectRulesService().getAllowAddIgnoreProcessed(hub, obj);
			}

			
			@Override
			public <T extends OAObject> OAObjectCallback getAllowRemoveObjectCallback(Hub<T> hub, T objRemove) {
				return srvc.getOAObjectRulesService().getAllowRemoveObjectCallback(hub, objRemove);
			}

			@Override
			public OAObjectCallback getAllowCopyObjectCallback(OAObject oaObj) {
				return srvc.getOAObjectRulesService().getAllowCopyObjectCallback(oaObj);
			}

			@Override
			public String getToolTip(OAObject obj, String propertyName, String defaultToolTip) {
				return srvc.getOAObjectRulesService().getToolTip(obj, propertyName, defaultToolTip);
			}

			@Override
			public boolean getVerifyPropertyChangeCallbackOnly(OAObject obj, String propertyName, Object oldValue, Object newValue) {
				return srvc.getOAObjectRulesService().getVerifyPropertyChangeCallbackOnly(obj, propertyName, oldValue, newValue);
			}

			@Override
			public <T extends OAObject> boolean getAllowEnabled(Hub<T> hub, T obj, String name) {
				return srvc.getOAObjectRulesService().getAllowEnabled(hub, obj, name);
			}
			@Override
			public <T extends OAObject> boolean getAllowEnabledCallbackOnly(Hub<T> hub, T obj, String name) {
				return srvc.getOAObjectRulesService().getAllowEnabledCallbackOnly(hub, obj, name);
			}
			
			
			
			@Override
			public <T extends OAObject> OAObjectCallback getAllowEnabledObjectCallback(Hub<T> hub, T oaObj, String name) {
				return srvc.getOAObjectRulesService().getAllowEnabledObjectCallback(null, hub, oaObj, name);
			}

			@Override
			public <T extends OAObject> boolean getAllowVisible(Hub<T> hub, T oaObj, String name) {
				return srvc.getOAObjectRulesService().getAllowVisible(hub, oaObj, name);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getAllowVisibleObjectCallback(Hub<T> hub, T oaObj, String name) {
				return srvc.getOAObjectRulesService().getAllowVisibleObjectCallback(hub, oaObj, name);
			}

			@Override
			public OAObjectCallback getAllowSubmitObjectCallback(OAObject oaObj) {
				return srvc.getOAObjectRulesService().getAllowSubmitObjectCallback(oaObj);
			}

			@Override
			public boolean getAllowSave(OAObject oaObj) {
				return srvc.getOAObjectRulesService().getAllowSave(oaObj);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getAllowAddObjectCallback(Hub<T> hub, T objAdd) {
				return srvc.getOAObjectRulesService().getAllowAddObjectCallback(hub, objAdd);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getAllowDeleteObjectCallback(Hub<T> hub, T obj) {
				return srvc.getOAObjectRulesService().getAllowDeleteObjectCallback(hub, obj);
			}

			@Override
			public OAObjectCallback getAllowEnabledObjectCallback(Hub<? extends OAObject> hub) {
				return srvc.getOAObjectRulesService().getAllowEnabledObjectCallback(hub);
			}

			@Override
			public OAObjectCallback getAllowSaveObjectCallback(OAObject obj) {
				return srvc.getOAObjectRulesService().getAllowSaveObjectCallback(obj);
			}

			@Override
			public OAObjectCallback getAllowDeleteObjectCallback(OAObject ao) {
				return srvc.getOAObjectRulesService().getAllowDeleteObjectCallback(ao);
			}

			@Override
			public OAObjectCallback getAllowRemoveAllObjectCallback(Hub<? extends OAObject> hub) {
				return srvc.getOAObjectRulesService().getAllowRemoveAllObjectCallback(hub);
			}

			@Override
			public OAObjectCallback getConfirmRemoveAllObjectCallback(Hub<? extends OAObject> hub, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectRulesService().getConfirmRemoveAllObjectCallback(hub, confirmMessage, confirmTitle);
			}

			@Override
			public String getFormat(OAObject obj, String propertyName, String defaultFormat) {
				return srvc.getOAObjectRulesService().getFormat(obj, propertyName, defaultFormat);
			}

			@Override
			public OAObjectCallback getAllowVisibleObjectCallback(Hub<? extends OAObject> hub) {
				return srvc.getOAObjectRulesService().getAllowVisibleObjectCallback(hub, null, null);
			}

			@Override
			public <T extends OAObject> boolean getVerifyRemove(Hub<T> hub, T obj) {
				return srvc.getOAObjectRulesService().getVerifyRemove(hub, obj);
			}

			@Override
			public <T extends OAObject> boolean getVerifyRemoveCallbackOnly(Hub<T> hub, T obj) {
				return srvc.getOAObjectRulesService().getVerifyRemoveCallbackOnly(hub, obj);
			}

			@Override
			public <T extends OAObject> boolean getVerifyRemoveIgnoreProcessed(Hub<T> hub, T obj) {
				return srvc.getOAObjectRulesService().getVerifyRemoveIgnoreProcessed(hub, obj);
			}

			
			@Override
			public void onObjectCallbackModel(Class<? extends OAObject> clazz, String property, OAObjectModel model) {
				srvc.getOAObjectRulesService().onObjectCallbackModel(clazz, property, model);
				
			}

			@Override
			public OAObjectCallback getVerifyPropertyChangeObjectCallback(OAObject obj, String propertyName, Object oldValue, Object newValue) {
				return srvc.getOAObjectRulesService().getVerifyPropertyChangeObjectCallback(obj, propertyName, oldValue, newValue);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getVerifyAddObjectCallback(Hub<T> hub, T objAdd) {
				return srvc.getOAObjectRulesService().getVerifyAddObjectCallback(hub, objAdd, null);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getVerifyRemoveObjectCallback(Hub<T> hub, T obj) {
				return srvc.getOAObjectRulesService().getVerifyRemoveObjectCallback(hub, obj, null);
			}

			@Override
			public void updateLabel(OAObject obj, String propertyName, OACallbackLabel lbl) {
				srvc.getOAObjectRulesService().updateLabel(obj, propertyName, lbl);
			}

			@Override
			public void renderLabel(OAObject obj, String propertyName, OACallbackLabel lbl) {
				srvc.getOAObjectRulesService().renderLabel(obj, propertyName, lbl);
			}
		};
		return opsCallback;
	}

	/**
	 * Returns the internal OAObject changed-state facade.
	 *
	 * @return the change operations facade
	 */
	public OAObjectChangeOps change() {
		if (opsChange != null) return opsChange;
		opsChange = new OAObjectChangeOps() {
			@Override
			public boolean getChanged(OAObject oaObj, int cascadeRule) {
				return srvc.getOAObjectChangeService().getChanged(oaObj, cascadeRule);
			}
		};
		return opsChange;
	}
	
	@Override
	/**
	 * Returns the internal delete facade for the current operation family.
	 *
	 * @return the delete operations facade
	 */
	public OAObjectDeleteOps delete() {
		if (opsDelete != null) return opsDelete;
		opsDelete = new OAObjectDeleteOps() {
			@Override
			public OALinkInfo[] getMustBeEmptyBeforeDelete(OAObject oaObj) {
				return srvc.getOAObjectDeleteService().getMustBeEmptyBeforeDelete(oaObj);
			}

			@Override
			public void setDeleted(OAObject oaObj, boolean bDeleted) {
				srvc.getOAObjectDeleteService().setDeleted(oaObj, bDeleted);
			}

			@Override
			public void delete(OAObject oaObj) {
				srvc.getOAObjectDeleteService().delete(oaObj);
			}

			@Override
			public void syncServerDelete(OAObject obj) {
				srvc.getOAObjectDeleteService().syncServerDelete(obj);
			}

			@Override
			public void syncClientDelete(OAObject obj) {
				srvc.getOAObjectDeleteService().syncClientDelete(obj);
			}
		}; 
		return opsDelete;
	}

	
	
	@Override
	/**
	 * Returns the internal client/server facade for the current operation family.
	 *
	 * @return the client/server operations facade
	 */
	public OAObjectCSOps cs() {
		if (opsCS != null) return opsCS;
		opsCS = new OAObjectCSOps() {
			
			@Override
			public void updateObjectsWithoutHubs(OAObject oaObj) {
				srvc.getOAObjectCSService().updateObjectsWithoutHubs(oaObj);
			}
			
			@Override
			public void objectFinalized(UUID guid) {
				srvc.getOAObjectCSService().objectFinalized(guid);
			}
			
			@Override
			public boolean isServer(OAObject oaObj) {
				return srvc.getOAObjectCSService().isServer(oaObj);
			}
			
			@Override
			public <T extends OAObject> Hub<T> getServerReferenceHub(T oaObj, String linkPropertyName) {
				return srvc.getOAObjectCSService().getServerReferenceHub(oaObj, linkPropertyName);
			}
		};
		return opsCS;
	}

	@Override
	/**
	 * Returns the internal OAObject datasource facade.
	 *
	 * @return the datasource operations facade
	 */
	public OAObjectDSOps ds() {
		if (opsDS != null) return opsDS;
		opsDS = new OAObjectDSOps() {
			
			@Override
			public void setAssigningId(OAObject oaObj, boolean bIsAssigningId) {
				srvc.getOAObjectDSService().setAssigningId(oaObj, bIsAssigningId);
			}
			
			@Override
			public boolean getAssignIdOnCreate(OAObject oaObj) {
				return srvc.getOAObjectDSService().getAssignIdOnCreate(oaObj);
			}
			
			@Override
			public void assignId(OAObject oaObj) {
				srvc.getOAObjectDSService().assignId(oaObj);
			}
		};
		return opsDS;
	}

	@Override
	/**
	 * Returns the internal OAObject enum facade.
	 *
	 * @return the enum operations facade
	 */
	public OAObjectEnumOps enumx() {
		if (opsEnum != null) return opsEnum;
		opsEnum = new OAObjectEnumOps() {
			@Override
			public Hub<VEnum> getVEnums(Class<? extends OAObject> clazz, String propertyName) {
				return srvc.getOAObjectEnumService().getVEnums(clazz, propertyName);
			}
		};
		return opsEnum;
	}

	@Override
	/**
	 * Returns the internal OAObject event facade.
	 *
	 * @return the event operations facade
	 */
	public OAObjectEventOps event() {
		if (opsEvent != null) return opsEvent;
		opsEvent = new OAObjectEventOps() {
			
			@Override
			public void firePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged, boolean bUnknownValues) {
				srvc.getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged, bUnknownValues);
			}
			
			@Override
			public void firePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				srvc.getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}
			
			@Override
			public void fireBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				srvc.getOAObjectEventService().fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}
			
			@Override
			public void fireAfterLoadEvent(OAObject oaObj) {
				srvc.getOAObjectEventService().fireAfterLoadEvent(oaObj);
			}
		};
		return opsEvent;
	}

	@Override
	/**
	 * Returns the internal find facade for the current operation family.
	 *
	 * @return the find operations facade
	 */
	public OAObjectFindOps find() {
		if (opsFind != null) return opsFind;
		opsFind = new OAObjectFindOps() {
			@Override
			public OAObject[] find(OAObject base, String path, Object findValue, boolean bFindAll) {
				return srvc.getOAObjectFindService().find(base, path, findValue, bFindAll);
			}
		};
		return opsFind;
	}
	
	
	@Override
	/**
	 * Returns the internal OAObject GUID facade.
	 *
	 * @return the GUID operations facade
	 */
	public OAObjectGuidOps guid() {
		if (opsGuid != null) return opsGuid;
		opsGuid = new OAObjectGuidOps() {
			
			@Override
			public void setGuid(OAObject oaObj, UUID iguid) {
				srvc.getOAObjectGuidService().setGuid(oaObj, iguid);
			}
			
			@Override
			public UUID getGuid(OAObject oaObj) {
				return srvc.getOAObjectGuidService().getGuid(oaObj);
			}
		};
		return opsGuid;
	}

	@Override
	/**
	 * Returns the internal OAObject Hub-reference facade.
	 *
	 * @return the object-Hub operations facade
	 */
	public OAObjectHubOps hub() {
		if (opsHub != null) return opsHub;
		opsHub = new OAObjectHubOps() {
			
			@Override
			public <T extends OAObject> void removeHub(T oaObj, Hub<T> hub, boolean bIsOnHubFinalize) {
				srvc.getOAObjectHubService().removeHub(oaObj, hub, bIsOnHubFinalize);
			}
			
			@Override
			public boolean isInHubWithMaster(OAObject obj) {
				return srvc.getOAObjectHubService().isInHubWithMaster(obj);
			}
			
			@Override
			public WeakReference<Hub<? extends OAObject>>[] getHubReferencesNoCopy(OAObject oaObj) {
				return srvc.getOAObjectHubService().getHubReferencesNoCopy(oaObj);
			}
			
			@Override
			public <T extends OAObject> Hub<T>[] getHubReferences(T oaObj) {
				return srvc.getOAObjectHubService().getHubReferences(oaObj);
			}
			
			@Override
			public <T extends OAObject> boolean addHub(T oaObj, Hub<T> hub, boolean bAlwaysAddIfM2M) {
				return srvc.getOAObjectHubService().addHub(oaObj, hub, bAlwaysAddIfM2M);
			}
		};
		return opsHub;
	}

	@Override
	/**
	 * Returns the internal OAObject metadata facade.
	 *
	 * @return the metadata operations facade
	 */
	public OAObjectInfoOps info() {
		if (opsInfo != null) return opsInfo;
		opsInfo = new OAObjectInfoOps() {
			
			@Override
			public boolean isPojoSingleton(OAObjectInfo toObjectInfo) {
				return srvc.getOAObjectInfoService().isPojoSingleton(toObjectInfo);
			}
			
			@Override
			public boolean isMany2Many(OALinkInfo li) {
				return srvc.getOAObjectInfoService().isMany2Many(li);
			}
			
			@Override
			public boolean isHubProperty(OAObjectInfo oi, String propertyName) {
				return srvc.getOAObjectInfoService().isHubProperty(oi, propertyName);
			}
			
			@Override
			public OALinkInfo getReverseLinkInfo(OALinkInfo li) {
				return srvc.getOAObjectInfoService().getReverseLinkInfo(li);
			}
			
			@Override
			public OALinkInfo getRecursiveLinkInfo(OAObjectInfo oi, int type) {
				return srvc.getOAObjectInfoService().getRecursiveLinkInfo(oi, type);
			}
			
			@Override
			public OAPropertyInfo getPropertyInfo(OAObjectInfo oi, String propertyName) {
				return srvc.getOAObjectInfoService().getPropertyInfo(oi, propertyName);
			}
			
			@Override
			public Class<?> getPropertyClass(OAObjectInfo oi, String propertyName) {
				return srvc.getOAObjectInfoService().getPropertyClass(oi, propertyName);
			}
			
			@Override
			public Class<?> getPropertyClass(Class<? extends OAObject> clazz, String propertyName) {
				return srvc.getOAObjectInfoService().getPropertyClass(clazz, propertyName);
			}
			
			@Override
			public OAObjectInfo getObjectInfo(Class<?> clazz) {
				return srvc.getOAObjectInfoService().getObjectInfo(clazz);
			}
			
			@Override
			public OAObjectInfo getOAObjectInfo(OAObject oaObj) {
				return srvc.getOAObjectInfoService().getOAObjectInfo(oaObj);
			}
			
			@Override
			public OAObjectInfo getOAObjectInfo(Class<?> clazz) {
				return srvc.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
			
			@Override
			public Method getMethod(OAObjectInfo oi, String methodName, int argumentCount) {
				return srvc.getOAObjectInfoService().getMethod(oi, methodName, argumentCount);
			}
			
			@Override
			public Method getMethod(Class<?> clazz, String methodName) {
				return srvc.getOAObjectInfoService().getMethod(clazz, methodName);
			}
			
			@Override
			public Method getMethod(OAObjectInfo oi, String string) {
				return srvc.getOAObjectInfoService().getMethod(oi, string);
			}
			
			@Override
			public OALinkInfo getLinkInfo(Class<? extends OAObject> clazz, String property) {
				return srvc.getOAObjectInfoService().getLinkInfo(clazz, property);
			}
			
			@Override
			public OALinkInfo getLinkInfo(OAObjectInfo oi, String propertyName) {
				return srvc.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}
			
			@Override
			public Class<? extends OAObject> getHubPropertyClass(Class<? extends OAObject> clazz, String propertyName) {
				return srvc.getOAObjectInfoService().getHubPropertyClass(clazz, propertyName);
			}
			
			@Override
			public OACalcInfo getCalcInfo(OAObjectInfo oi, String name) {
				return srvc.getOAObjectInfoService().getOACalcInfo(oi, name);
			}
			
			@Override
			public boolean cacheHub(OALinkInfo linkInfo, Hub<?> hub) {
				return srvc.getOAObjectInfoService().cacheHub(linkInfo, hub);
			}

			@Override
			public Class<? extends OAObject>[] getAllClasses() {
				return srvc.getOAObjectInfoService().getAllClasses();
			}

			@Override
			public OALinkInfo getLinkInfo(Class<? extends OAObject> fromClass, Class<? extends OAObject> toClass) {
				return srvc.getOAObjectInfoService().getLinkInfo(fromClass, toClass);
			}
		};
		return opsInfo;
	}

	@Override
	/**
	 * Returns the internal OAObject initialization facade.
	 *
	 * @return the initialization operations facade
	 */
	public OAObjectInitializeOps initialize() {
		if (opsInitialize != null) return opsInitialize;
		opsInitialize = new OAObjectInitializeOps() {
			
			@Override
			public void initializeAfterLoading(OAObject oaObj, boolean bAssignNewId, boolean bInitializeNulls, boolean bSetChangedToFalse) {
				srvc.getOAObjectInitializeService().initializeAfterLoading(oaObj, bAssignNewId, bInitializeNulls, bSetChangedToFalse);
			}
			
			@Override
			public void initializeAfterLoading(OAObject oaObj) {
				srvc.getOAObjectInitializeService().initializeAfterLoading(oaObj);
			}
			
			@Override
			public boolean initialize(OAObject oaObj) {
				return srvc.getOAObjectInitializeService().initialize(oaObj);
			}
		};
		return opsInitialize;
	}

	@Override
	/**
	 * Returns the internal OAObject key facade.
	 *
	 * @return the key operations facade
	 */
	public OAObjectKeyOps key() {
		if (opsKey != null) return opsKey;
		opsKey = new OAObjectKeyOps() {
			
			@Override
			public boolean isForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return srvc.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}
			
			@Override
			public OAObjectKey getKey(OAObject oaObj) {
				return srvc.getOAObjectKeyService().getKey(oaObj);
			}
			
			@Override
			public OAObjectKey createObjectKey(Object id) {
				return srvc.getOAObjectKeyService().createObjectKey(id);
			}
			
			@Override
			public OAObjectKey createObjectKey(Class<? extends OAObject> clazz, Object... ids) {
				return srvc.getOAObjectKeyService().createObjectKey(clazz, ids);
			}
			
			@Override
			public OAObjectKey createObjectKey(OAObject oaObj) {
				return srvc.getOAObjectKeyService().createObjectKey(oaObj);
			}
		};
		return opsKey;
	}

	@Override
	/**
	 * Returns the internal OAObject lock facade.
	 *
	 * @return the lock operations facade
	 */
	public OAObjectLockOps lock() {
		if (opsLock != null) return opsLock;
		opsLock = new OAObjectLockOps() {
			
			@Override
			public void unlock(OAObject oaObj) {
				srvc.getOAObjectLockService().unlock(oaObj);
			}
			
			@Override
			public void lock(OAObject oaObj) {
				srvc.getOAObjectLockService().lock(oaObj);
			}
			
			@Override
			public boolean isLocked(OAObject oaObj) {
				return srvc.getOAObjectLockService().isLocked(oaObj);
			}

			@Override
			public boolean isPropertyLocked(OAObject oaObj, String name) {
				return srvc.getOAObjectLockService().isPropertyLocked(oaObj, name);
			}
		}; 
				
		return opsLock;
	}

	@Override
	/**
	 * Returns the internal property facade for the current operation family.
	 *
	 * @return the property operations facade
	 */
	public OAObjectPropertyOps property() {
		if (opsProperty != null) return opsProperty;
		opsProperty = new OAObjectPropertyOps() {
			
			@Override
			public void setReferenceable(OAObject oaObj, boolean bIsReferenceable) {
				srvc.getOAObjectPropertyService().setReferenceable(oaObj, bIsReferenceable);
			}
			
			@Override
			public void setPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist) {
				srvc.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue, bMustNotExist, bReturnNotExist);
			}
			
			@Override
			public void removeProperty(OAObject oaObj, String name, boolean bFirePropertyChange) {
				srvc.getOAObjectPropertyService().removeProperty(oaObj, name, bFirePropertyChange);
			}
			
			@Override
			public boolean isReferenceNull(OAObject oaObj, String prop) {
				return srvc.getOAObjectPropertyService().isReferenceNull(oaObj, prop);
			}
			
			@Override
			public boolean isPropertyLoaded(OAObject oaObj, String prop) {
				return srvc.getOAObjectPropertyService().isPropertyLoaded(oaObj, prop);
			}
			
			@Override
			public String[] getPropertyNames(OAObject oaObj) {
				return srvc.getOAObjectPropertyService().getPropertyNames(oaObj);
			}
			
			@Override
			public Object getProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return srvc.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}
			
			@Override
			public Object getProperty(OAObject oaObj, String name) {
				return srvc.getOAObjectPropertyService().getProperty(oaObj, name);
			}
			
			@Override
			public void clearProperties(OAObject oaObj) {
				srvc.getOAObjectPropertyService().clearProperties(oaObj);
			}
			
			@Override
			public void setProperty(OAObject oaObj, String name, Object value) { 
				srvc.getOAObjectPropertyService().setProperty(oaObj, name, value);
			}
		};
		return opsProperty;
	}

	@Override
	/**
	 * Returns the internal save facade for the current operation family.
	 *
	 * @return the save operations facade
	 */
	public OAObjectSaveOps save() {
		if (opsSave != null) return opsSave;
		opsSave = new OAObjectSaveOps() {
			
			@Override
			public void save(OAObject obj, int iCascadeRule, OACascade cascade) {
				srvc.getOAObjectSaveService().save(obj, iCascadeRule, cascade);
			}
			
			@Override
			public void save(OAObject oaObj, int iCascadeRule) {
				srvc.getOAObjectSaveService().save(oaObj, iCascadeRule);
			}
		};
		return opsSave;
	}

	@Override
	/**
	 * Returns the internal OAObject scheduler facade.
	 *
	 * @return the scheduler operations facade
	 */
	public OAObjectSchedulerOps scheduler() {
		if (opsScheduler != null) return opsScheduler;
		opsScheduler = new OAObjectSchedulerOps() {
			@Override
			public OAScheduler getScheduler(OAObject oaObj, String property, OADate date) {
				return srvc.getOAObjectSchedulerService().getScheduler(oaObj, property, date);
			}
		}; 
				
		return opsScheduler;
	}

	@Override
	/**
	 * Returns the internal serialization facade for the current operation family.
	 *
	 * @return the serialization operations facade
	 */
	public OAObjectSerializeOps serialize() {
		if (opsSerialize != null) return opsSerialize;
		opsSerialize = new OAObjectSerializeOps() {
			
			@Override
			public void writeObject(OAObject oaObj, ObjectOutputStream stream) throws IOException {
				srvc.getOAObjectSerializeService()._writeObject(oaObj, stream);
			}
			
			@Override
			public Object readResolve(OAObject oaObj) throws ObjectStreamException {
				return srvc.getOAObjectSerializeService()._readResolve(oaObj);
			}
			
			@Override
			public void readObject(OAObject oaObj, ObjectInputStream in) throws IOException, ClassNotFoundException {
				srvc.getOAObjectSerializeService()._readObject(oaObj, in);
			}
		};
		return opsSerialize;
	}

	@Override
	/**
	 * Returns the internal OAObject sibling facade.
	 *
	 * @return the sibling operations facade
	 */
	public OAObjectSiblingOps sibling() {
		if (opsSibling != null) return opsSibling;
		opsSibling = new OAObjectSiblingOps() {
			@Override
			public OAObjectKey[] getSiblings(OAObject oaObj, String property, int maxAmount, ConcurrentHashMap<UUID, Boolean> hmIgnoreSibling) {
				return srvc.getOAObjectSiblingService().getSiblings(oaObj, property, maxAmount, hmIgnoreSibling);
			}
		};
		return opsSibling;
	}

	@Override
	/**
	 * Returns the internal OAObject lifecycle-state facade.
	 *
	 * @return the state operations facade
	 */
	public OAObjectStateOps state() {
		if (opsState != null) return opsState;
		opsState = new OAObjectStateOps() {
			@Override
			public void setNew(OAObject oaObj, boolean bIsNew) {
				srvc.getOAObjectStateService().setNew(oaObj, bIsNew);
			}
		};
		return opsState;
	}
	
	@Override
	/**
	 * Returns the internal unique-object facade.
	 *
	 * @return the unique operations facade
	 */
	public OAObjectUniqueOps unique() {
		if (opsUnique != null) return opsUnique;
		opsUnique = new OAObjectUniqueOps() {
			@Override
			public OAObject getUnique(final Class<? extends OAObject> clazz, final String propertyName, final Object uniqueKey, final boolean bAutoCreate) {
				return srvc.getOAObjectUniqueService().getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
			}
		};
		return opsUnique;
	}

	@Override
	public OAObjectRecurseOps recurse() {
		if (opsRecurse != null) return opsRecurse;
		opsRecurse = new OAObjectRecurseOps() {
			@Override
			public <T extends OAObject> void recurse(T oaObj, OACallback<OAObject> callback, OACascade cascade) {
				srvc.getOAObjectRecurseService().recurse(oaObj, callback, cascade);
			}
		};
		return opsRecurse;
	}

}
