package com.viaoa.oa.service.object;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.callback.OACallback;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.datasource.OADataSource;
import com.viaoa.filter.OAFilter;
import com.viaoa.find.OAFinder;
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
import com.viaoa.oa.service.OASyncService;
import com.viaoa.oa.service.OATriggerService;
import com.viaoa.oa.service.hub.HubParentService;
import com.viaoa.oa.sibling.OASiblingHelper;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInternalBridge;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARemoteThreadService;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.select.OASelect;
import com.viaoa.serialize.OAObjectSerializer;
import com.viaoa.session.OASessionUser;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.sync.remote.RemoteSessionInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;
import com.viaoa.trigger.OATrigger;

/*qqqqqqqqqq
CODEX

 #7 — invariant risk
  File/class/method: src/main/java/com/viaoa/oa/service/object/OAObjectParentService.java:373, sync child hooks;
  src/main/java/com/viaoa/oa/service/hub/HubParentService.java:496, sync child hooks
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
public class OAObjectParentService {
	private static final Logger LOG = Logger.getLogger(OAObjectParentService.class.getName());

	private HubParentService srvcHubParent;
	private OASyncService srvcSync;
	private OAThreadLocalService srvcThreadLocal;
	private OARemoteThreadService srvcRemoteThread;
	private final OAObjectInternalBridge faBridge = new OAObjectInternalBridge();
	private OATriggerService srvcTrigger;

	private OAObjectAnnotationService srvcOAObjectAnnotation;
	private OAObjectAutoAddService srvcOAObjectAutoAdd;
	private OAObjectCacheService srvcOAObjectCache;
	private OAObjectChangeService srvcOAObjectChange;
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
	private OAObjectStateService srvcOAObjectState;
	private OAObjectUniqueService srvcOAObjectUnique;
	private OAObjectRulesService srvcOAObjectRules;

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
	 * Performs initialize behavior for the OA object service.
	 *
	 * @param srvcHubParent    method input
	 * @param srvcSync         method input
	 * @param srvcThreadLocal  method input
	 * @param srvcRemoteThread method input
	 * @param srvcTrigger      method input
	 */
	public void initialize(HubParentService srvcHubParent, OASyncService srvcSync, OAThreadLocalService srvcThreadLocal, OARemoteThreadService srvcRemoteThread, OATriggerService srvcTrigger) {
		if (this.srvcHubParent != null)
			throw new IllegalArgumentException("initialize already called");
		if (srvcHubParent == null)
			throw new IllegalArgumentException("HubParentService can not be null");
		if (srvcSync == null)
			throw new IllegalArgumentException("OASyncService can not be null");
		if (srvcThreadLocal == null)
			throw new IllegalArgumentException("OAThreadLocalService can not be null");
		if (srvcRemoteThread == null)
			throw new IllegalArgumentException("OARemoteThreadService can not be null");
		if (srvcTrigger == null)
			throw new IllegalArgumentException("OATriggerService can not be null");

		this.srvcHubParent = srvcHubParent;
		this.srvcSync = srvcSync;
		this.srvcThreadLocal = srvcThreadLocal;
		this.srvcRemoteThread = srvcRemoteThread;
		this.srvcTrigger = srvcTrigger;

		getOAObjectAnnotationService();
		getOAObjectAutoAddService();
		getOAObjectCacheService();
		getOAObjectChangeService();
		getOAObjectCSService();
		// getOAObjectDatabaseService();
		getOAObjectRulesService();
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

	/**
	 * Returns the hubParentService value.
	 *
	 * @return result value
	 */
	protected HubParentService getHubParentService() {
		return this.srvcHubParent;
	}

	/**
	 * Returns the oAObjectAnnotationService value.
	 *
	 * @return result value
	 */
	public OAObjectAnnotationService getOAObjectAnnotationService() {
		if (srvcOAObjectAnnotation != null)
			return srvcOAObjectAnnotation;
		srvcOAObjectAnnotation = new OAObjectAnnotationService(faBridge.getObjectInfoFriendAccess()) {
			@Override
			/**
			 * Performs callReflectGetHubObjectClass behavior for the OA object service.
			 *
			 * @param method method input
			 * @return result value
			 */
			public Class<?> callReflectGetHubObjectClass(Method method) {
				return OAObjectParentService.this.getOAObjectReflectService().getHubObjectClass(method);
			}

			@Override
			/**
			 * Performs callInfoGetCalcInfo behavior for the OA object service.
			 *
			 * @param oi   method input
			 * @param name method input
			 * @return result value
			 */
			public OACalcInfo callInfoGetCalcInfo(OAObjectInfo oi, String name) {
				return OAObjectParentService.this.getOAObjectInfoService().getOACalcInfo(oi, name);
			}

			@Override
			/**
			 * Performs callInfoGetLinkInfo behavior for the OA object service.
			 *
			 * @param oi   method input
			 * @param name method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String name) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(oi, name);
			}

			@Override
			/**
			 * Performs callAddTrigger behavior for the OA object service.
			 *
			 * @param trigger method input
			 */
			public void callAddTrigger(OATrigger trigger) {
				srvcTrigger.addTrigger(trigger);
			}
		};
		return srvcOAObjectAnnotation;
	}

	/**
	 * Returns the oAObjectAutoAddService value.
	 *
	 * @return result value
	 */
	public OAObjectAutoAddService getOAObjectAutoAddService() {
		if (srvcOAObjectAutoAdd != null)
			return srvcOAObjectAutoAdd;

		srvcOAObjectAutoAdd = new OAObjectAutoAddService(faBridge.getObjectFriendAccess()) {
			@Override
			/**
			 * Performs callObjectReflectGetRawReference behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return result value
			 */
			public Object callObjectReflectGetRawReference(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectReflectService().getRawReference(oaObj, name);
			}

			@Override
			/**
			 * Performs callObjectReflectGetProperty behavior for the OA object service.
			 *
			 * @param obj  method input
			 * @param name method input
			 * @return result value
			 */
			public Object callObjectReflectGetProperty(OAObject obj, String name) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(obj, name);
			}

			@Override
			/**
			 * Performs callObjectInfoGetReverseLinkInfo behavior for the OA object service.
			 *
			 * @param li method input
			 * @return result value
			 */
			public OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().getReverseLinkInfo(li);
			}

			@Override
			/**
			 * Performs callObjectInfoGetOAObjectInfo behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(oaObj);
			}

			@Override
			/**
			 * Performs callObjectEventFirePropertyChange behavior for the OA object
			 * service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param oldObj       method input
			 * @param newObj       method input
			 * @param bLocalOnly   method input
			 * @param bSetChanged  method input
			 */
			public void callObjectEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}

			@Override
			/**
			 * Performs callThreadLocalGetSendSyncMessages behavior for the OA object
			 * service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalGetSendSyncMessages() {
				return OAObjectParentService.this.srvcThreadLocal.getSendSyncMessages();
			}

			@Override
			/**
			 * Performs callThreadLocalSetSendSyncMessages behavior for the OA object
			 * service.
			 *
			 * @param b method input
			 */
			public void callThreadLocalSetSendSyncMessages(boolean b) {
				OAObjectParentService.this.srvcThreadLocal.setSendSyncMessages(b);
			}
		};
		return srvcOAObjectAutoAdd;
	}

	/**
	 * Returns the oAObjectCacheService value.
	 *
	 * @return result value
	 */
	public OAObjectCacheService getOAObjectCacheService() {
		if (srvcOAObjectCache != null)
			return srvcOAObjectCache;
		srvcOAObjectCache = new OAObjectCacheService() {
			@Override
			/**
			 * Performs callKeyCreateObjectKey behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return result value
			 */
			public OAObjectKey callKeyCreateObjectKey(OAObject obj) {
				return OAObjectParentService.this.getOAObjectKeyService().createObjectKey(obj);
			}

			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(Class<? extends OAObject> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callKeyGetKey behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}

			@Override
			/**
			 * Performs callKeyCreateObjectKey behavior for the OA object service.
			 *
			 * @param c   method input
			 * @param ids method input
			 * @return result value
			 */
			public OAObjectKey callKeyCreateObjectKey(Class<? extends OAObject> c, Object... ids) {
				return OAObjectParentService.this.getOAObjectKeyService().createObjectKey(c, ids);
			}

			@Override
			/**
			 * Performs callHubGetHubReferences behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public <T extends OAObject> Hub<T>[] callHubGetHubReferences(T oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}

			@Override
			/**
			 * Performs callDetailGetLinkInfoFromDetailToMaster behavior for the OA object
			 * service.
			 *
			 * @param hub method input
			 * @return result value
			 */
			public OALinkInfo callDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return OAObjectParentService.this.getHubParentService().getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}

			@Override
			/**
			 * Performs callHubSelectRefreshSelect behavior for the OA object service.
			 *
			 * @param hub method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callHubSelectRefreshSelect(Hub<?> hub) {
				return OAObjectParentService.this.getHubParentService().getHubSelectService().refreshSelect(hub);
			}

			@Override
			/**
			 * Performs callSyncIsClient behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callSyncRemoteServerRefreshCache behavior for the OA object service.
			 *
			 * @param clazz method input
			 */
			public void callSyncRemoteServerRefreshCache(Class<? extends OAObject> clazz) {
				OAObjectParentService.this.srvcSync.getRemoteServer().refreshCache(clazz);
			}

			@Override
			/**
			 * Performs callThreadLocalGetObjectCacheAddMode behavior for the OA object
			 * service.
			 *
			 * @return result value
			 */
			public int callThreadLocalGetObjectCacheAddMode() {
				return OAObjectParentService.this.srvcThreadLocal.getObjectCacheAddMode();
			}
		};
		return srvcOAObjectCache;
	}

	/**
	 * Returns the oAObjectChangeService value.
	 *
	 * @return result value
	 */
	public OAObjectChangeService getOAObjectChangeService() {
		if (srvcOAObjectChange != null)
			return srvcOAObjectChange;
		srvcOAObjectChange = new OAObjectChangeService(faBridge.getObjectFriendAccess()) {
			@Override
			/**
			 * Performs callObjectInfoGetOAObjectInfo behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(oaObj);
			}

			@Override
			/**
			 * Performs callObjectInfoIsMany2Many behavior for the OA object service.
			 *
			 * @param li method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callObjectInfoIsMany2Many(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().isMany2Many(li);
			}

			@Override
			/**
			 * Performs callHubStatusGetChanged behavior for the OA object service.
			 *
			 * @param hub     method input
			 * @param type    method input
			 * @param cascade method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callHubStatusGetChanged(Hub<?> hub, int type, OACascade cascade) {
				return OAObjectParentService.this.srvcHubParent.getHubStatusService().getChanged(hub, type, cascade);
			}

			@Override
			/**
			 * Performs callObjectReflectGetRawReference behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param prop  method input
			 * @return result value
			 */
			public Object callObjectReflectGetRawReference(OAObject oaObj, String prop) {
				return OAObjectParentService.this.getOAObjectReflectService().getRawReference(oaObj, prop);
			}

			@Override
			/**
			 * Performs callObjectReflectGetProperty behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param prop  method input
			 * @return result value
			 */
			public Object callObjectReflectGetProperty(OAObject oaObj, String prop) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, prop);
			}

			@Override
			/**
			 * Performs callObjectHubGetChanged behavior for the OA object service.
			 *
			 * @param hub         method input
			 * @param cascadeRule method input
			 * @param cascade     method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callObjectHubGetChanged(Hub<?> hub, int cascadeRule, OACascade cascade) {
				return OAObjectParentService.this.getOAObjectHubService().getChanged(hub, cascadeRule, cascade);
			}

			@Override
			/**
			 * Performs callObjectReflectIsReferenceNullOrNotLoaded behavior for the OA
			 * object service.
			 *
			 * @param oaObj method input
			 * @param prop  method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callObjectReflectIsReferenceNullOrNotLoaded(OAObject oaObj, String prop) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceNullOrNotLoaded(oaObj, prop);
			}
		};
		return srvcOAObjectChange;
	}

	/**
	 * Returns the oAObjectCSService value.
	 *
	 * @return result value
	 */
	public OAObjectCSService getOAObjectCSService() {
		if (srvcOAObjectCS != null)
			return srvcOAObjectCS;

		srvcOAObjectCS = new OAObjectCSService() {
			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(Class<?> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callSyncIsSingleUser behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsSingleUser() {
				return OAObjectParentService.this.srvcSync.isSingleUser();
			}

			@Override
			/**
			 * Performs callSyncIsServer behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsServer() {
				return OAObjectParentService.this.srvcSync.isServer();
			}

			@Override
			/**
			 * Performs callSyncIsClient behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callSyncClientGetDetail behavior for the OA object service.
			 *
			 * @param masterObject method input
			 * @param propertyName method input
			 * @return result value
			 */
			public Object callSyncClientGetDetail(OAObject masterObject, String propertyName) {
				return OAObjectParentService.this.srvcSync.getClient().getDetail(masterObject, propertyName);
			}

			@Override
			/**
			 * Performs callRemoteSyncPropertyChange behavior for the OA object service.
			 *
			 * @param objectClass  method input
			 * @param origKey      method input
			 * @param propertyName method input
			 * @param newValue     method input
			 * @param bIsBlob      method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callRemoteSyncPropertyChange(Class<? extends OAObject> objectClass, OAObjectKey origKey, String propertyName, Object newValue, boolean bIsBlob) {
				return OAObjectParentService.this.srvcSync.getRemoteSync().propertyChange(objectClass, origKey, propertyName, newValue, bIsBlob);
			}

			@Override
			/**
			 * Performs callSyncClientObjectCreated behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callSyncClientObjectCreated(OAObject obj) {
				OAObjectParentService.this.srvcSync.getClient().objectCreated(obj);
			}

			@Override
			/**
			 * Performs callSyncClientObjectFinalized behavior for the OA object service.
			 *
			 * @param guid method input
			 */
			public void callSyncClientObjectFinalized(UUID guid) {
				OAObjectParentService.this.srvcSync.getClient().objectFinalized(guid);
			}

			@Override
			/**
			 * Performs callHubSelectLoadAllData behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @param select  method input
			 */
			public <T extends OAObject> void callHubSelectLoadAllData(Hub<T> thisHub, OASelect<T> select) {
				OAObjectParentService.this.srvcHubParent.getHubSelectService().loadAllData(thisHub, select);
			}

			@Override
			/**
			 * Performs callSyncClientUpdateObjectsWithoutHubs behavior for the OA object
			 * service.
			 *
			 * @param obj method input
			 */
			public void callSyncClientUpdateObjectsWithoutHubs(OAObject obj) {
				OASyncClient sc = OAObjectParentService.this.srvcSync.getClient();
				if (sc != null)
					sc.updateObjectsWithoutHubs(obj);
			}

			@Override
			/**
			 * Performs callSyncClientCreateCopy behavior for the OA object service.
			 *
			 * @param objectClass       method input
			 * @param objectKey         method input
			 * @param excludeProperties method input
			 * @return result value
			 */
			public <T extends OAObject> T callSyncClientCreateCopy(Class<T> objectClass, OAObjectKey objectKey, String[] excludeProperties) {
				return OAObjectParentService.this.srvcSync.getRemoteClient().createCopy(objectClass, objectKey, excludeProperties);
			}

			@Override
			/**
			 * Performs callSyncServerSave behavior for the OA object service.
			 *
			 * @param objectClass  method input
			 * @param objectKey    method input
			 * @param iCascadeRule method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncServerSave(Class<? extends OAObject> objectClass, OAObjectKey objectKey, int iCascadeRule) {
				return OAObjectParentService.this.srvcSync.getRemoteServer().save(objectClass, objectKey, iCascadeRule);
			}

			@Override
			/**
			 * Performs callInfoGetLinkInfo behavior for the OA object service.
			 *
			 * @param oi   method input
			 * @param name method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String name) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(oi, name);
			}

			@Override
			/**
			 * Performs callThreadLocalGetSendSyncMessages behavior for the OA object
			 * service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalGetSendSyncMessages() {
				return OAObjectParentService.this.srvcThreadLocal.getSendSyncMessages();
			}

			@Override
			/**
			 * Performs callThreadLocalSetSendSyncMessages behavior for the OA object
			 * service.
			 *
			 * @param b method input
			 */
			public void callThreadLocalSetSendSyncMessages(boolean b) {
				srvcThreadLocal.setSendSyncMessages(b);
			}

			@Override
			/**
			 * Performs callThreadLocalIsLoading behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalIsLoading() {
				return srvcThreadLocal.isLoading();
			}

			@Override
			/**
			 * Performs callSyncServerGetObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param key   method input
			 * @return result value
			 */
			public <T extends OAObject> T callSyncServerGetObject(Class<T> clazz, OAObjectKey key) {
				RemoteServerInterface rsi = OAObjectParentService.this.srvcSync.getRemoteServer();
				if (rsi != null)
					return rsi.getObject(clazz, key);
				return null;
			}

			@Override
			/**
			 * Performs callSyncSyncServerDelete behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param key   method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncSyncServerDelete(Class<? extends OAObject> clazz, OAObjectKey key) {
				RemoteSyncInterface rsi = OAObjectParentService.this.srvcSync.getRemoteSync();
				if (rsi == null)
					return false;
				rsi.serverDelete(clazz, key);
				return true;
			}

			@Override
			/**
			 * Performs callSyncSyncClientDelete behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param key   method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncSyncClientDelete(Class<? extends OAObject> clazz, OAObjectKey key) {
				RemoteSyncInterface rsi = OAObjectParentService.this.srvcSync.getRemoteSync();
				if (rsi == null)
					return false;
				rsi.clientDelete(clazz, key);
				return true;
			}

			@Override
			/**
			 * Performs callSyncIsRunning behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			protected boolean callSyncIsRunning() {
				return OAObjectParentService.this.srvcSync.isRunning();
			}
		};

		return srvcOAObjectCS;
	}

	/*
	 * qqqqqq
	 * 
	 * 
	 * public OAObjectDatabaseService getOAObjectDatabaseService() { if
	 * (srvcOAObjectDatabase != null) return srvcOAObjectDatabase;
	 * srvcOAObjectDatabase = new OAObjectDatabaseService() {
	 * 
	 * @Override
	 * 
	 * 
	 * public OAObjectInfo callInfoGetObjectInfo(Class<?> clazz) { return
	 * OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz); }
	 * 
	 * @Override
	 * 
	 * 
	 * public Class<? extends OAObject> callAnnotationGetHubObjectClass(OAMany
	 * annotation, Method method) { return
	 * OAObjectParentService.this.getOAObjectAnnotationService().getHubObjectClass(
	 * annotation, method); } }; return srvcOAObjectDatabase; }
	 */
	public OAObjectDeleteService getOAObjectDeleteService() {
		if (srvcOAObjectDelete != null)
			return srvcOAObjectDelete;
		srvcOAObjectDelete = new OAObjectDeleteService(faBridge.getObjectFriendAccess()) {
			@Override
			/**
			 * Returns the oAObjectInfo value.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo getOAObjectInfo(Class<?> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callCSDelete behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callCSDelete(OAObject obj) {
				return OAObjectParentService.this.getOAObjectCSService().delete(obj);
			}

			@Override
			/**
			 * Performs callCSSendDeleteToClients behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callCSSendDeleteToClients(OAObject obj) {
				OAObjectParentService.this.getOAObjectCSService().sendDeleteToClients(obj);
			}

			@Override
			/**
			 * Performs callEventFireBeforePropertyChange behavior for the OA object
			 * service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param oldObj       method input
			 * @param newObj       method input
			 * @param bLocalOnly   method input
			 * @param bSetChanged  method input
			 */
			public void callEventFireBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}

			@Override
			/**
			 * Performs callEventFirePropertyChange behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param oldObj       method input
			 * @param newObj       method input
			 * @param bLocalOnly   method input
			 * @param bSetChanged  method input
			 */
			public void callEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}

			@Override
			/**
			 * Performs callKeyVerifyKeyChange behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param newObjectKey method input
			 * @return result value
			 */
			public String callKeyVerifyKeyChange(OAObject oaObj, OAObjectKey newObjectKey) {
				return OAObjectParentService.this.getOAObjectKeyService().verifyKeyChange(oaObj, newObjectKey);
			}

			@Override
			/**
			 * Performs callCacheAdd behavior for the OA object service.
			 *
			 * @param obj             method input
			 * @param bErrorIfExists  method input
			 * @param bAddToSelectAll method input
			 * @return result value
			 */
			public OAObject callCacheAdd(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll) {
				return OAObjectParentService.this.getOAObjectCacheService().add(obj, bErrorIfExists, bAddToSelectAll);
			}

			@Override
			/**
			 * Performs callSyncIsClient behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callObjectHubGetHubReferences behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public Hub[] callObjectHubGetHubReferences(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}

			@Override
			/**
			 * Performs callHubEventFireBeforeDeleteEvent behavior for the OA object
			 * service.
			 *
			 * @param hub method input
			 * @param obj method input
			 */
			public <T extends OAObject> void callHubEventFireBeforeDeleteEvent(Hub<T> hub, T obj) {
				OAObjectParentService.this.getHubParentService().getHubEventService().fireBeforeDeleteEvent(hub, obj);
			}

			@Override
			/**
			 * Performs callLocalThreadSetDeleting behavior for the OA object service.
			 *
			 * @param obj method input
			 * @param b   method input
			 */
			public void callLocalThreadSetDeleting(Object obj, boolean b) {
				OAObjectParentService.this.srvcThreadLocal.setDeleting(obj, b);
			}

			@Override
			/**
			 * Performs callSyncIsServer behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsServer() {
				return OAObjectParentService.this.srvcSync.isServer();
			}

			public boolean callSyncIsSingleUserOrServer() {
				return OAObjectParentService.this.srvcSync.isSingleUserOrServer();
			}
			
			@Override
			/**
			 * Performs callHubCSRemoveAllFromHub behavior for the OA object service.
			 *
			 * @param thisHub method input
			 */
			public void callHubCSRemoveAllFromHub(Hub<?> thisHub) {
				OAObjectParentService.this.srvcHubParent.getHubCSService().removeAllFromHub(thisHub);
			}

			@Override
			/**
			 * Performs callObjectHubDeleteAll behavior for the OA object service.
			 *
			 * @param hub     method input
			 * @param cascade method input
			 */
			public void callObjectHubDeleteAll(Hub<?> hub, OACascade cascade) {
				OAObjectParentService.this.getOAObjectHubService().deleteAll(hub, cascade);
			}

			@Override
			/**
			 * Performs callHubDSRemoveMany2ManyLinks behavior for the OA object service.
			 *
			 * @param hub method input
			 */
			public void callHubDSRemoveMany2ManyLinks(Hub<?> hub) {
				OAObjectParentService.this.getHubParentService().getHubDSService().removeMany2ManyLinks(hub);
			}

			@Override
			/**
			 * Performs callCacheCallback behavior for the OA object service.
			 *
			 * @param callback method input
			 * @param clazz    method input
			 */
			public <T extends OAObject> void callCacheCallback(OACallback<T> callback, Class<T> clazz) {
				OAObjectParentService.this.getOAObjectCacheService().callback(callback, clazz);
			}

			@Override
			/**
			 * Performs callReflectIsReferenceNullOrNotLoadedOrEmptyHub behavior for the OA
			 * object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callReflectIsReferenceNullOrNotLoadedOrEmptyHub(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceNullOrNotLoadedOrEmptyHub(oaObj, propertyName);
			}

			@Override
			/**
			 * Performs callKeyIsForSameOAObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param ok1   method input
			 * @param ok2   method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectParentService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}

			@Override
			/**
			 * Performs callPropertyRemoveProperty behavior for the OA object service.
			 *
			 * @param oaObj               method input
			 * @param name                method input
			 * @param bFirePropertyChange method input
			 */
			public void callPropertyRemoveProperty(OAObject oaObj, String name, boolean bFirePropertyChange) {
				OAObjectParentService.this.getOAObjectPropertyService().removeProperty(oaObj, name, bFirePropertyChange);
			}

			@Override
			/**
			 * Performs callPropertyGetProperty behavior for the OA object service.
			 *
			 * @param oaObj           method input
			 * @param name            method input
			 * @param bReturnNotExist method input
			 * @param bConvertWeakRef method input
			 * @return result value
			 */
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}

			@Override
			/**
			 * Performs callObjectSetNew behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param b     method input
			 */
			public void callObjectSetNew(OAObject oaObj, boolean b) {
				OAObjectParentService.this.getOAObjectStateService().setNew(oaObj, b);
			}

			@Override
			/**
			 * Performs callHubEventFireAfterDeleteEvent behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @param obj     method input
			 */
			public <T extends OAObject> void callHubEventFireAfterDeleteEvent(Hub<T> thisHub, T obj) {
				OAObjectParentService.this.srvcHubParent.getHubEventService().fireAfterDeleteEvent(thisHub, obj);
			}

			/**
			 * Performs callHubRemove behavior for the OA object service.
			 *
			 * @param thisHub          method input
			 * @param obj              method input
			 * @param bForce           method input
			 * @param bSendEvent       method input
			 * @param bDeleting        method input
			 * @param bSetAO           method input
			 * @param bSetPropToMaster method input
			 * @param bIsRemovingAll   method input
			 * @return result value
			 */
			public <T extends OAObject> T callHubRemove(Hub<T> thisHub, Object obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll) {
				return OAObjectParentService.this.srvcHubParent.getHubAddRemoveService().remove(thisHub, obj, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
			}

			@Override
			/**
			 * Performs callRemoteTheadStartNextThread behavior for the OA object service.
			 */
			public void callRemoteTheadStartNextThread() {
				OAObjectParentService.this.srvcRemoteThread.startNextThread();
			}

			@Override
			/**
			 * Performs callReflectGetProperty behavior for the OA object service.
			 *
			 * @param oaObj    method input
			 * @param propPath method input
			 * @return result value
			 */
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}

			@Override
			/**
			 * Performs callHubMasterGetMasterObject behavior for the OA object service.
			 *
			 * @param hub method input
			 * @return result value
			 */
			public OAObject callHubMasterGetMasterObject(Hub<?> hub) {
				return OAObjectParentService.this.getHubParentService().getHubMasterService().getMasterObject(hub);
			}

			@Override
			/**
			 * Performs callReflectGetReferenceObject behavior for the OA object service.
			 *
			 * @param oaObj            method input
			 * @param linkPropertyName method input
			 * @return result value
			 */
			public Object callReflectGetReferenceObject(OAObject oaObj, String linkPropertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().getReferenceObject(oaObj, linkPropertyName);
			}

			@Override
			/**
			 * Performs callPropertyGetProperty behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return result value
			 */
			public Object callPropertyGetProperty(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name);
			}

			@Override
			/**
			 * Performs callDSSupportsStorage behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callDSSupportsStorage(OAObject obj) {
				OADataSource ds = OAObjectParentService.this.getOAObjectDSService().getDataSource(obj);
				if (ds == null)
					return false;
				return ds.supportsStorage();
			}

			@Override
			/**
			 * Performs callDSUpdateMany2ManyLinks behavior for the OA object service.
			 *
			 * @param masterObject   method input
			 * @param adds           method input
			 * @param removes        method input
			 * @param propFromMaster method input
			 */
			public void callDSUpdateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster) {
				OADataSource ds = OAObjectParentService.this.getOAObjectDSService().getDataSource(masterObject);
				if (ds == null)
					return;
				ds.updateMany2ManyLinks(masterObject, adds, removes, propFromMaster);
			}

			@Override
			/**
			 * Performs callHubDataRemoveFromRemovedList behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @param obj     method input
			 */
			public <T extends OAObject> void callHubDataRemoveFromRemovedList(Hub<T> thisHub, T obj) {
				OAObjectParentService.this.getHubParentService().getHubDataService().removeFromRemovedList(thisHub, obj);
			}

			@Override
			/**
			 * Performs callReflectGetReferenceHub behavior for the OA object service.
			 *
			 * @param oaObj            method input
			 * @param linkPropertyName method input
			 * @param sortOrder        method input
			 * @param bSequence        method input
			 * @param hubMatch         method input
			 */
			public <T extends OAObject> Hub<T> callReflectGetReferenceHub(final OAObject oaObj, final String linkPropertyName, String sortOrder, boolean bSequence, Hub<T> hubMatch) {
				return OAObjectParentService.this.getOAObjectReflectService().getReferenceHub(oaObj, linkPropertyName, sortOrder, bSequence, hubMatch);
			}

			@Override
			/**
			 * Performs callDSDelete behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callDSDelete(OAObject obj) {
				OAObjectParentService.this.getOAObjectDSService().delete(obj);
			}

			@Override
			/**
			 * Performs callDSRemoveReference behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param li    method input
			 */
			public void callDSRemoveReference(OAObject oaObj, OALinkInfo li) {
				OAObjectParentService.this.getOAObjectDSService().removeReference(oaObj, li);
			}

			@Override
			/**
			 * Performs callHubGetHub behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param li    method input
			 * @return result value
			 */
			public Hub<?> callHubGetHub(OAObject oaObj, OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectHubService().getHub(oaObj, li);
			}

			@Override
			/**
			 * Performs callInfoIsMany2Many behavior for the OA object service.
			 *
			 * @param li method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callInfoIsMany2Many(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().isMany2Many(li);
			}

			@Override
			/**
			 * Performs callInfoGetReverseLinkInfo behavior for the OA object service.
			 *
			 * @param li method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().getReverseLinkInfo(li);
			}

			/*
			 * qqqqqqqqq
			 * 
			 * @Override
			 * 
			 * 
			 * public void callLogToXmlFile(OAObject oaObj, boolean bSave) {
			 * OAObjectParentService.this.getOAObjectLogService().logToXmlFile(oaObj,
			 * bSave); }
			 */
			@Override
			/**
			 * Performs callReflectSetProperty behavior for the OA object service.
			 *
			 * @param oaObj    method input
			 * @param propName method input
			 * @param value    method input
			 * @param fmt      method input
			 */
			public void callReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				OAObjectParentService.this.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);
			}
		};
		return srvcOAObjectDelete;
	}

	/**
	 * Returns the oAObjectDSService value.
	 *
	 * @return result value
	 */
	public OAObjectDSService getOAObjectDSService() {
		if (srvcOAObjectDS != null)
			return srvcOAObjectDS;
		srvcOAObjectDS = new OAObjectDSService() {
			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(Class<?> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callKeyGetKey behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return result value
			 */
			public OAObjectKey callKeyGetKey(OAObject obj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(obj);
			}

			@Override
			/**
			 * Performs callGuidGetGuid behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return result value
			 */
			public UUID callGuidGetGuid(OAObject obj) {
				return OAObjectParentService.this.getOAObjectGuidService().getGuid(obj);
			}

			@Override
			/**
			 * Performs callKeyCreateObjectKey behavior for the OA object service.
			 *
			 * @param c   method input
			 * @param ids method input
			 * @return result value
			 */
			public OAObjectKey callKeyCreateObjectKey(Class<? extends OAObject> c, Object... ids) {
				return OAObjectParentService.this.getOAObjectKeyService().createObjectKey(c, ids);
			}
		};
		return srvcOAObjectDS;
	}

	/**
	 * Returns the oAObjectEmptyHubService value.
	 *
	 * @return result value
	 */
	public OAObjectEmptyHubService getOAObjectEmptyHubService() {
		if (srvcOAObjectEmptyHub != null)
			return srvcOAObjectEmptyHub;
		srvcOAObjectEmptyHub = new OAObjectEmptyHubService() {
			@Override
			/**
			 * Performs callKeyGetKey behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return result value
			 */
			public OAObjectKey callKeyGetKey(OAObject obj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(obj);
			}

			@Override
			/**
			 * Performs callPropertySetProperty behavior for the OA object service.
			 *
			 * @param obj   method input
			 * @param name  method input
			 * @param value method input
			 */
			public void callPropertySetProperty(OAObject obj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().setProperty(obj, name, value);
			}

			@Override
			/**
			 * Performs callCacheCallback behavior for the OA object service.
			 *
			 * @param callback method input
			 */
			public void callCacheCallback(OACallback callback) {
				final OAObjectCacheService srvcCache = OAObjectParentService.this.getOAObjectCacheService();
				for (Class<? extends OAObject> c : srvcCache.getClasses()) {
					srvcCache.callback(c, callback);
				}
			}

			@Override
			/**
			 * Performs callPropertyGetPropertyNames behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public String[] callPropertyGetPropertyNames(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectPropertyService().getPropertyNames(oaObj);
			}

			@Override
			/**
			 * Performs callReflectIsReferenceHubLoadedAndEmpty behavior for the OA object
			 * service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callReflectIsReferenceHubLoadedAndEmpty(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceHubLoadedAndEmpty(oaObj, propertyName);
			}
		};
		return srvcOAObjectEmptyHub;
	}

	/**
	 * Returns the oAObjectEnumService value.
	 *
	 * @return result value
	 */
	public OAObjectEnumService getOAObjectEnumService() {
		if (srvcOAObjectEnum != null)
			return srvcOAObjectEnum;
		srvcOAObjectEnum = new OAObjectEnumService() {
			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}
		};
		return srvcOAObjectEnum;
	}

	/**
	 * Returns the oAObjectEventService value.
	 *
	 * @return result value
	 */
	public OAObjectEventService getOAObjectEventService() {
		if (srvcOAObjectEvent != null)
			return srvcOAObjectEvent;

		srvcOAObjectEvent = new OAObjectEventService(faBridge.getObjectFriendAccess()) {
			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(Class<?> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callUniqueGetUnique behavior for the OA object service.
			 *
			 * @param clazz        method input
			 * @param propertyName method input
			 * @param uniqueKey    method input
			 * @param bAutoCreate  method input
			 * @return result value
			 */
			public OAObject callUniqueGetUnique(Class<? extends OAObject> clazz, String propertyName, Object uniqueKey, boolean bAutoCreate) {
				return OAObjectParentService.this.getOAObjectUniqueService().getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
			}

			@Override
			/**
			 * Performs callSyncIsServer behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsServer() {
				return OAObjectParentService.this.srvcSync.isServer();
			}

			@Override
			/**
			 * Performs callSyncIsObjectOnServer behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsObjectOnServer(OAObject obj) {
				OASyncClient sc = srvcSync.getClient();
				return (sc != null && sc.isObjectOnServer(obj));
			}

			@Override
			/**
			 * Performs callSyncIsClient behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callObjectSetAutoAdd behavior for the OA object service.
			 *
			 * @param oaObj    method input
			 * @param bEnabled method input
			 */
			public void callObjectSetAutoAdd(OAObject oaObj, boolean bEnabled) {
				OAObjectParentService.this.getOAObjectAutoAddService().setAutoAdd(oaObj, bEnabled);
			}

			@Override
			/**
			 * Performs callRemoteThreadStartNextThread behavior for the OA object service.
			 */
			public void callRemoteThreadStartNextThread() {
				OAObjectParentService.this.srvcRemoteThread.startNextThread();
			}

			@Override
			/**
			 * Performs callReflectSetProperty behavior for the OA object service.
			 *
			 * @param oaObj    method input
			 * @param propName method input
			 * @param value    method input
			 * @param fmt      method input
			 */
			public void callReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				OAObjectParentService.this.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);
			}

			@Override
			/**
			 * Performs callReflectSetPrimitiveNull behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param bNull        method input
			 */
			public void callReflectSetPrimitiveNull(OAObject oaObj, String propertyName, boolean bNull) {
				OAObjectParentService.this.getOAObjectReflectService().setPrimitiveNull(oaObj, propertyName, bNull);
			}

			@Override
			/**
			 * Performs callReflectIsReferenceHubLoadedAndNotEmpty behavior for the OA
			 * object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callReflectIsReferenceHubLoadedAndNotEmpty(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceHubLoadedAndNotEmpty(oaObj, propertyName);
			}

			@Override
			/**
			 * Performs callReflectIsReferenceHubLoaded behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callReflectIsReferenceHubLoaded(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceHubLoaded(oaObj, propertyName);
			}

			@Override
			/**
			 * Performs callReflectGetProperty behavior for the OA object service.
			 *
			 * @param oaObj    method input
			 * @param propPath method input
			 * @return result value
			 */
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}

			@Override
			/**
			 * Performs callReflectGetPrimitiveNull behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callReflectGetPrimitiveNull(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().getPrimitiveNull(oaObj, propertyName);
			}

			@Override
			/**
			 * Performs callReflectGetObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param key   method input
			 * @return result value
			 */
			public OAObject callReflectGetObject(Class<? extends OAObject> clazz, Object key) {
				return OAObjectParentService.this.getOAObjectReflectService().getObject(clazz, key);
			}

			@Override
			/**
			 * Performs callPropertySetPropertyCAS behavior for the OA object service.
			 *
			 * @param oaObj           method input
			 * @param name            method input
			 * @param newValue        method input
			 * @param matchValue      method input
			 * @param bMustNotExist   method input
			 * @param bReturnNotExist method input
			 * @return result value
			 */
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue, bMustNotExist, bReturnNotExist);
			}

			@Override
			/**
			 * Performs callPropertySetPropertyCAS behavior for the OA object service.
			 *
			 * @param oaObj      method input
			 * @param name       method input
			 * @param newValue   method input
			 * @param matchValue method input
			 * @return result value
			 */
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue);
			}

			@Override
			/**
			 * Performs callPropertyGetProperty behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return result value
			 */
			public Object callPropertyGetProperty(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name);
			}

			@Override
			/**
			 * Performs callPropertyGetProperty behavior for the OA object service.
			 *
			 * @param oaObj           method input
			 * @param name            method input
			 * @param bReturnNotExist method input
			 * @param bConvertWeakRef method input
			 * @return result value
			 */
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}

			@Override
			/**
			 * Performs callThreadLocalSetDeleting behavior for the OA object service.
			 *
			 * @param obj method input
			 * @param b   method input
			 */
			public void callThreadLocalSetDeleting(Object obj, boolean b) {
				OAObjectParentService.this.srvcThreadLocal.setDeleting(obj, b);
			}

			@Override
			/**
			 * Performs callThreadLocalRemoveHubEvent behavior for the OA object service.
			 *
			 * @param he method input
			 */
			public void callThreadLocalRemoveHubEvent(HubEvent<?> he) {
				OAObjectParentService.this.srvcThreadLocal.removeHubEvent(he);
			}

			@Override
			/**
			 * Performs callThreadLocalIsLoading behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalIsLoading() {
				// TODO Auto-generated method stub
				return OAObjectParentService.this.srvcThreadLocal.isLoading();
			}

			@Override
			/**
			 * Performs callThreadLocalIsDeleting behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalIsDeleting(OAObject obj) {
				return OAObjectParentService.this.srvcThreadLocal.isDeleting(obj);
			}

			@Override
			/**
			 * Performs callThreadLocalIsDeleting behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalIsDeleting() {
				return OAObjectParentService.this.srvcThreadLocal.isDeleting();
			}

			@Override
			/**
			 * Performs callThreadLocalGetCreateUndoablePropertyChanges behavior for the OA
			 * object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalGetCreateUndoablePropertyChanges() {
				return OAObjectParentService.this.srvcThreadLocal.getCreateUndoablePropertyChanges();
			}

			@Override
			/**
			 * Performs callThreadLocalAddHubEvent behavior for the OA object service.
			 *
			 * @param he method input
			 */
			public void callThreadLocalAddHubEvent(HubEvent<?> he) {
				OAObjectParentService.this.srvcThreadLocal.addHubEvent(he);
			}

			@Override
			/**
			 * Performs callKeyVerifyKeyChange behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param newObjectKey method input
			 * @return result value
			 */
			public String callKeyVerifyKeyChange(OAObject oaObj, OAObjectKey newObjectKey) {
				return OAObjectParentService.this.getOAObjectKeyService().verifyKeyChange(oaObj, newObjectKey);
			}

			@Override
			/**
			 * Performs callKeyIsForSameOAObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param ok1   method input
			 * @param ok2   method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectParentService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}

			@Override
			/**
			 * Performs callKeyGetKey behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}

			@Override
			/**
			 * Performs callKeyCreateChangedObjectKey behavior for the OA object service.
			 *
			 * @param clazz        method input
			 * @param objKey       method input
			 * @param propertyName method input
			 * @param newValue     method input
			 * @return result value
			 */
			public OAObjectKey callKeyCreateChangedObjectKey(Class<? extends OAObject> clazz, OAObjectKey objKey, String propertyName, Object newValue) {
				return OAObjectParentService.this.getOAObjectKeyService().createChangedObjectKey(clazz, objKey, propertyName, newValue);
			}

			@Override
			/**
			 * Performs callKeyAfterChangedObjectKeyProperty behavior for the OA object
			 * service.
			 *
			 * @param oaObj   method input
			 * @param okOrig  method input
			 * @param bVerify method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callKeyAfterChangedObjectKeyProperty(OAObject oaObj, OAObjectKey okOrig, boolean bVerify) {
				return OAObjectParentService.this.getOAObjectKeyService().afterChangedObjectKeyProperty(oaObj, okOrig, bVerify);
			}

			@Override
			/**
			 * Performs callRemoteThreadIsRemoteThread behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callRemoteThreadIsRemoteThread() {
				return OAObjectParentService.this.srvcRemoteThread.isRemoteThread();
			}

			@Override
			/**
			 * Performs callInfoGetRootHub behavior for the OA object service.
			 *
			 * @param oi method input
			 * @return result value
			 */
			public Hub<?> callInfoGetRootHub(OAObjectInfo oi) {
				return OAObjectParentService.this.getOAObjectInfoService().getRootHub(oi);
			}

			@Override
			/**
			 * Performs callInfoGetReverseLinkInfo behavior for the OA object service.
			 *
			 * @param li method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().getReverseLinkInfo(li);
			}

			@Override
			/**
			 * Performs callInfoGetRecursiveLinkInfo behavior for the OA object service.
			 *
			 * @param oi   method input
			 * @param type method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetRecursiveLinkInfo(OAObjectInfo oi, int type) {
				return OAObjectParentService.this.getOAObjectInfoService().getRecursiveLinkInfo(oi, type);
			}

			@Override
			/**
			 * Performs callInfoGetPropertyInfo behavior for the OA object service.
			 *
			 * @param oi           method input
			 * @param propertyName method input
			 * @return result value
			 */
			public OAPropertyInfo callInfoGetPropertyInfo(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getPropertyInfo(oi, propertyName);
			}

			@Override
			/**
			 * Performs callInfoGetCalcInfo behavior for the OA object service.
			 *
			 * @param thisOI method input
			 * @param name   method input
			 * @return result value
			 */
			public OACalcInfo callInfoGetCalcInfo(OAObjectInfo thisOI, String name) {
				return OAObjectParentService.this.getOAObjectInfoService().getOACalcInfo(thisOI, name);
			}

			@Override
			/**
			 * Performs callInfoGetMethod behavior for the OA object service.
			 *
			 * @param oi            method input
			 * @param methodName    method input
			 * @param argumentCount method input
			 * @return result value
			 */
			public Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount) {
				return OAObjectParentService.this.getOAObjectInfoService().getMethod(oi, methodName, argumentCount);
			}

			@Override
			/**
			 * Performs callInfoGetLinkToOwner behavior for the OA object service.
			 *
			 * @param oi method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetLinkToOwner(OAObjectInfo oi) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkToOwner(oi);
			}

			@Override
			/**
			 * Performs callInfoGetLinkInfo behavior for the OA object service.
			 *
			 * @param oi           method input
			 * @param propertyName method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}

			@Override
			/**
			 * Performs callHubShareGetAllSharedHubs behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @param filter  method input
			 * @return result value
			 */
			public <T extends OAObject> Hub<T>[] callHubShareGetAllSharedHubs(Hub<T> thisHub, OAFilter<Hub<T>> filter) {
				return OAObjectParentService.this.srvcHubParent.getHubShareService().getAllSharedHubs(thisHub, filter);
			}

			@Override
			/**
			 * Performs callHubIsInHub behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callHubIsInHub(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().isInHub(oaObj);
			}

			@Override
			/**
			 * Performs callHubGetHubReferences behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public Hub[] callHubGetHubReferences(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}

			@Override
			/**
			 * Performs callHubEventFireBeforePropertyChange behavior for the OA object
			 * service.
			 *
			 * @param thisHub      method input
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param oldValue     method input
			 * @param newValue     method input
			 */
			public <T extends OAObject> void callHubEventFireBeforePropertyChange(Hub<T> thisHub, T oaObj, String propertyName, Object oldValue, Object newValue) {
				OAObjectParentService.this.srvcHubParent.getHubEventService().fireBeforePropertyChange(thisHub, oaObj, propertyName, oldValue, newValue);
			}

			@Override
			/**
			 * Performs callHubEventFireAfterPropertyChange behavior for the OA object
			 * service.
			 *
			 * @param thisHub      method input
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param oldValue     method input
			 * @param newValue     method input
			 * @param linkInfo     method input
			 */
			public <T extends OAObject> void callHubEventFireAfterPropertyChange(Hub<T> thisHub, T oaObj, String propertyName, Object oldValue, Object newValue, OALinkInfo linkInfo) {
				OAObjectParentService.this.srvcHubParent.getHubEventService().fireAfterPropertyChange(thisHub, oaObj, propertyName, oldValue, newValue, linkInfo);
			}

			@Override
			/**
			 * Performs callHubEventFireAfterLoadEvent behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @param oaObj   method input
			 */
			public <T extends OAObject> void callHubEventFireAfterLoadEvent(Hub<T> thisHub, T oaObj) {
				OAObjectParentService.this.srvcHubParent.getHubEventService().fireAfterLoadEvent(thisHub, oaObj);
			}

			@Override
			/**
			 * Performs callHubDetailGetHubWithMasterHub behavior for the OA object service.
			 *
			 * @param thisHub method input
			 */
			public <T extends OAObject> Hub<T> callHubDetailGetHubWithMasterHub(Hub<T> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubDetailService().getHubWithMasterHub(thisHub);
			}

			@Override
			/**
			 * Performs callHubAddRemoveRemove behavior for the OA object service.
			 *
			 * @param thisHub          method input
			 * @param obj              method input
			 * @param bForce           method input
			 * @param bSendEvent       method input
			 * @param bDeleting        method input
			 * @param bSetAO           method input
			 * @param bSetPropToMaster method input
			 * @param bIsRemovingAll   method input
			 * @return result value
			 */
			public <T extends OAObject> T callHubAddRemoveRemove(Hub<T> thisHub, Object obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll) {
				return OAObjectParentService.this.srvcHubParent.getHubAddRemoveService().remove(thisHub, obj, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
			}

			@Override
			/**
			 * Performs callHubAOSetActiveObject behavior for the OA object service.
			 *
			 * @param thisHub      method input
			 * @param object       method input
			 * @param adjustMaster method input
			 * @param bUpdateLink  method input
			 * @param bForce       method input
			 */
			public <T extends OAObject> void callHubAOSetActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
				OAObjectParentService.this.srvcHubParent.getHubAOService().setActiveObject(thisHub, object, adjustMaster, bUpdateLink, bForce);
			}

			@Override
			/**
			 * Performs callObjectGetAutoAdd behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callObjectGetAutoAdd(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectAutoAddService().getAutoAdd(oaObj);
			}

			@Override
			/**
			 * Performs callDSIsAssigningId behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callDSIsAssigningId(OAObject obj) {
				return OAObjectParentService.this.getOAObjectDSService().isAssigningId(obj);
			}

			@Override
			/**
			 * Performs callRulesGetVerifyPropertyChangeCallbackOnlyObjectCallback behavior
			 * for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param oldValue     method input
			 * @param newValue     method input
			 * @return result value
			 */
			public OAObjectCallback callRulesGetVerifyPropertyChangeCallbackOnlyObjectCallback(OAObject oaObj, String propertyName, Object oldValue, Object newValue) {
				return OAObjectParentService.this.getOAObjectRulesService().getVerifyPropertyChangeCallbackOnlyObjectCallback(oaObj, propertyName, oldValue, newValue);
			}

			@Override
			/**
			 * Performs callRulesGetAllowSubmitObjectCallback behavior for the OA object
			 * service.
			 *
			 * @param obj method input
			 * @return result value
			 */
			public OAObjectCallback callRulesGetAllowSubmitObjectCallback(OAObject obj) {
				return OAObjectParentService.this.getOAObjectRulesService().getAllowSubmitObjectCallback(obj);
			}

			@Override
			/**
			 * Performs callCacheGet behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param ok    method input
			 * @return result value
			 */
			public <T extends OAObject> T callCacheGetUsingKey(Class<T> clazz, OAObjectKey ok) {
				return OAObjectParentService.this.getOAObjectCacheService().getUsingKey(clazz, ok);
			}

			@Override
			/**
			 * Performs callCacheFireAfterPropertyChange behavior for the OA object service.
			 *
			 * @param obj          method input
			 * @param origKey      method input
			 * @param propertyName method input
			 * @param oldValue     method input
			 * @param newValue     method input
			 * @param bLocalOnly   method input
			 * @param bSendEvent   method input
			 */
			public void callCacheFireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue, boolean bLocalOnly, boolean bSendEvent) {
				OAObjectParentService.this.getOAObjectCacheService().fireAfterPropertyChange(obj, origKey, propertyName, oldValue, newValue, bLocalOnly, bSendEvent);
			}

			@Override
			/**
			 * Performs callCSFireBeforePropertyChange behavior for the OA object service.
			 *
			 * @param obj          method input
			 * @param propertyName method input
			 * @param oldValue     method input
			 * @param newValue     method input
			 */
			public void callCSFireBeforePropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue) {
				OAObjectParentService.this.getOAObjectCSService().fireBeforePropertyChange(obj, propertyName, oldValue, newValue);
			}

			@Override
			/**
			 * Performs callThreadLocalGetSendSyncMessages behavior for the OA object
			 * service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalGetSendSyncMessages() {
				return OAObjectParentService.this.srvcThreadLocal.getSendSyncMessages();
			}

			@Override
			/**
			 * Performs callThreadLocalSetSendSyncMessages behavior for the OA object
			 * service.
			 *
			 * @param b method input
			 */
			public void callThreadLocalSetSendSyncMessages(boolean b) {
				OAObjectParentService.this.srvcThreadLocal.setSendSyncMessages(b);
			}
		};
		return srvcOAObjectEvent;
	}

	/**
	 * Returns the oAObjectFindService value.
	 *
	 * @return result value
	 */
	public OAObjectFindService getOAObjectFindService() {
		if (srvcOAObjectFind != null)
			return srvcOAObjectFind;
		srvcOAObjectFind = new OAObjectFindService();
		return srvcOAObjectFind;
	}

	/**
	 * Returns the oAObjectGuidService value.
	 *
	 * @return result value
	 */
	public OAObjectGuidService getOAObjectGuidService() {
		if (srvcOAObjectGuid != null)
			return srvcOAObjectGuid;
		srvcOAObjectGuid = new OAObjectGuidService(faBridge.getObjectFriendAccess()) {
		};
		return srvcOAObjectGuid;
	}

	/**
	 * Returns the oAObjectHubService value.
	 *
	 * @return result value
	 */
	public OAObjectHubService getOAObjectHubService() {
		if (srvcOAObjectHub != null)
			return srvcOAObjectHub;

		srvcOAObjectHub = new OAObjectHubService(faBridge.getObjectFriendAccess()) {
			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callSyncIsClient behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callThreadLocalGetSendSyncMessages behavior for the OA object
			 * service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalGetSendSyncMessages() {
				return OAObjectParentService.this.srvcThreadLocal.getSendSyncMessages();
			}

			@Override
			/**
			 * Performs callPropertyGetProperty behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return result value
			 */
			public Object callPropertyGetProperty(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name);
			}

			@Override
			/**
			 * Performs callKeyGetKey behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}

			@Override
			/**
			 * Performs callInfoIsMany2Many behavior for the OA object service.
			 *
			 * @param thisLi method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callInfoIsMany2Many(OALinkInfo thisLi) {
				return OAObjectParentService.this.getOAObjectInfoService().isMany2Many(thisLi);
			}

			@Override
			/**
			 * Performs callInfoGetReverseLinkInfo behavior for the OA object service.
			 *
			 * @param thisLi method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo thisLi) {
				return OAObjectParentService.this.getOAObjectInfoService().getReverseLinkInfo(thisLi);
			}

			@Override
			/**
			 * Performs callHubSaveSaveAll behavior for the OA object service.
			 *
			 * @param thisHub      method input
			 * @param iCascadeRule method input
			 * @param cascade      method input
			 */
			public void callHubSaveSaveAll(Hub<?> thisHub, int iCascadeRule, OACascade cascade) {
				OAObjectParentService.this.srvcHubParent.getHubSaveService().saveAll(thisHub, iCascadeRule, cascade);
			}

			@Override
			/**
			 * Performs callHubMasterGetMasterObject behavior for the OA object service.
			 *
			 * @param hub method input
			 * @return result value
			 */
			public OAObject callHubMasterGetMasterObject(Hub<?> hub) {
				return OAObjectParentService.this.srvcHubParent.getHubMasterService().getMasterObject(hub);
			}

			@Override
			/**
			 * Performs callHubStatusGetChanged behavior for the OA object service.
			 *
			 * @param thisHub      method input
			 * @param iCascadeRule method input
			 * @param cascade      method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callHubStatusGetChanged(Hub<?> thisHub, int iCascadeRule, OACascade cascade) {
				return OAObjectParentService.this.srvcHubParent.getHubStatusService().getChanged(thisHub, iCascadeRule, cascade);
			}

			@Override
			/**
			 * Performs callHubDetailSetMasterObject behavior for the OA object service.
			 *
			 * @param thisHub          method input
			 * @param masterObject     method input
			 * @param liDetailToMaster method input
			 */
			public void callHubDetailSetMasterObject(Hub<?> thisHub, OAObject masterObject, OALinkInfo liDetailToMaster) {
				// TODO Auto-generated method stub
				OAObjectParentService.this.srvcHubParent.getHubDetailService().setMasterObject(thisHub, masterObject, liDetailToMaster);
			}

			@Override
			/**
			 * Performs callHubDetailGetPropertyFromMasterToDetail behavior for the OA
			 * object service.
			 *
			 * @param thisHub method input
			 * @return result value
			 */
			public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubDetailService().getPropertyFromMasterToDetail(thisHub);
			}

			@Override
			/**
			 * Performs callHubDetailGetMasterObject behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @return result value
			 */
			public OAObject callHubDetailGetMasterObject(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubDetailService().getMasterObject(thisHub);
			}

			@Override
			/**
			 * Performs callHubDetailGetLinkInfoFromDetailToMaster behavior for the OA
			 * object service.
			 *
			 * @param hub method input
			 * @return result value
			 */
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return OAObjectParentService.this.srvcHubParent.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}

			@Override
			/**
			 * Performs callHubDeleteDeleteAll behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @param cascade method input
			 */
			public void callHubDeleteDeleteAll(Hub<?> thisHub, OACascade cascade) {
				OAObjectParentService.this.srvcHubParent.getHubDeleteService().deleteAll(thisHub, cascade);
			}

			@Override
			/**
			 * Performs callHubDataContainsDirect behavior for the OA object service.
			 *
			 * @param hub method input
			 * @param obj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callHubDataContainsDirect(Hub<?> hub, Object obj) {
				return OAObjectParentService.this.srvcHubParent.getHubDataService().containsDirect(hub, obj);
			}

			@Override
			/**
			 * Performs callEventSendHubPropertyChange behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param oldObj       method input
			 * @param newObj       method input
			 * @param linkInfo     method input
			 */
			public void callEventSendHubPropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, OALinkInfo linkInfo) {
				OAObjectParentService.this.getOAObjectEventService().sendHubPropertyChange(oaObj, propertyName, oldObj, newObj, linkInfo);
			}

			@Override
			/**
			 * Performs callCacheFireAfterPropertyChange behavior for the OA object service.
			 *
			 * @param obj          method input
			 * @param origKey      method input
			 * @param propertyName method input
			 * @param oldValue     method input
			 * @param newValue     method input
			 * @param bLocalOnly   method input
			 * @param bSendEvent   method input
			 */
			public void callCacheFireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue, boolean bLocalOnly, boolean bSendEvent) {
				OAObjectParentService.this.getOAObjectCacheService().fireAfterPropertyChange(obj, origKey, propertyName, oldValue, newValue, bLocalOnly, bSendEvent);
			}

			@Override
			/**
			 * Performs callCSUpdateObjectsWithoutHubs behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callCSUpdateObjectsWithoutHubs(OAObject obj) {
				OAObjectParentService.this.getOAObjectCSService().updateObjectsWithoutHubs(obj);
			}
		};

		return srvcOAObjectHub;
	}

	/**
	 * Returns the oAObjectImportMatchService value.
	 *
	 * @return result value
	 */
	public OAObjectImportMatchService getOAObjectImportMatchService() {
		if (srvcOAObjectImportMatch != null)
			return srvcOAObjectImportMatch;

		srvcOAObjectImportMatch = new OAObjectImportMatchService() {
			@Override
			/**
			 * Performs callInfogetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfogetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callThreadLocalSetLoading behavior for the OA object service.
			 *
			 * @param b method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalSetLoading(boolean b) {
				return OAObjectParentService.this.srvcThreadLocal.setLoading(b);
			}

			@Override
			/**
			 * Performs callThreadLocalIsLoading behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalIsLoading() {
				return OAObjectParentService.this.srvcThreadLocal.isLoading();
			}

			@Override
			/**
			 * Performs callReflectCreateNewObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public <T extends OAObject> T callReflectCreateNewObject(Class<T> clazz) {
				return OAObjectParentService.this.getOAObjectReflectService().createNewObject(clazz);
			}

			@Override
			/**
			 * Performs callCacheFind behavior for the OA object service.
			 *
			 * @param clazz  method input
			 * @param finder method input
			 * @return result value
			 */
			public <T extends OAObject> T callCacheFind(Class<T> clazz, OAFinder<T, T> finder) {
				return OAObjectParentService.this.getOAObjectCacheService().find(clazz, finder);
			}
		};
		return srvcOAObjectImportMatch;
	}

	/**
	 * Returns the oAObjectInfoService value.
	 *
	 * @return result value
	 */
	public OAObjectInfoService getOAObjectInfoService() {
		if (srvcOAObjectInfo != null)
			return srvcOAObjectInfo;

		srvcOAObjectInfo = new OAObjectInfoService(faBridge.getObjectFriendAccess(), faBridge.getObjectInfoFriendAccess()) {
			@Override
			/**
			 * Performs callSyncIsClient behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callReflectGetRawReference behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return result value
			 */
			public Object callReflectGetRawReference(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectReflectService().getRawReference(oaObj, name);
			}

			@Override
			/**
			 * Performs callReflectGetProperty behavior for the OA object service.
			 *
			 * @param oaObj    method input
			 * @param propPath method input
			 * @return result value
			 */
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return getOAObjectReflectService().getProperty(oaObj, propPath);
			}

			@Override
			/**
			 * Performs callAnnotationUpdateLinkFkeys behavior for the OA object service.
			 *
			 * @param oi method input
			 */
			public void callAnnotationUpdateLinkFkeys(OAObjectInfo oi) {
				OAObjectParentService.this.getOAObjectAnnotationService().updateLinkFkeys(oi);
			}

			@Override
			/**
			 * Performs callAnnotationUpdateImportMatches behavior for the OA object
			 * service.
			 *
			 * @param oi method input
			 */
			public void callAnnotationUpdateImportMatches(OAObjectInfo oi) {
				OAObjectParentService.this.getOAObjectAnnotationService().updateImportMatches(oi);
			}

			@Override
			/**
			 * Performs callAnnotationUpdate2 behavior for the OA object service.
			 *
			 * @param oi    method input
			 * @param clazz method input
			 */
			public void callAnnotationUpdate2(OAObjectInfo oi, Class clazz) {
				OAObjectParentService.this.getOAObjectAnnotationService().update2(oi, clazz);
			}

			@Override
			/**
			 * Performs callAnnotationUpdate behavior for the OA object service.
			 *
			 * @param oi    method input
			 * @param clazz method input
			 */
			public void callAnnotationUpdate(OAObjectInfo oi, Class clazz) {
				OAObjectParentService.this.getOAObjectAnnotationService().update(oi, clazz);
			}
		};
		return srvcOAObjectInfo;
	}

	/**
	 * Returns the oAObjectInitializeService value.
	 *
	 * @return result value
	 */
	public OAObjectInitializeService getOAObjectInitializeService() {
		if (srvcOAObjectInitialize != null)
			return srvcOAObjectInitialize;

		srvcOAObjectInitialize = new OAObjectInitializeService(faBridge.getObjectFriendAccess()) {
			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callThreadLocalSetLoading behavior for the OA object service.
			 *
			 * @param b method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalSetLoading(boolean b) {
				return OAObjectParentService.this.srvcThreadLocal.setLoading(b);
			}

			@Override
			/**
			 * Performs callThreadLocalIsLoading behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalIsLoading() {
				return OAObjectParentService.this.srvcThreadLocal.isLoading();
			}

			@Override
			/**
			 * Performs callSyncIsClient behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callSyncClientObjectCreated behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callSyncClientObjectCreated(OAObject obj) {
				OAObjectParentService.this.srvcSync.getClient().objectCreated(obj);
			}

			@Override
			/**
			 * Performs callReflectSetProperty behavior for the OA object service.
			 *
			 * @param oaObj    method input
			 * @param propName method input
			 * @param value    method input
			 * @param fmt      method input
			 */
			public void callReflectSetProperty(OAObject oaObj, String propName, Object value, String fmt) {
				OAObjectParentService.this.getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);
			}

			@Override
			/**
			 * Performs callPropertyUnsafeAddProperty behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @param value method input
			 */
			public void callPropertyUnsafeAddProperty(OAObject oaObj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().unsafeAddProperty(oaObj, name, value);
			}

			@Override
			/**
			 * Performs callInfoIsOne2One behavior for the OA object service.
			 *
			 * @param thisLi method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callInfoIsOne2One(OALinkInfo thisLi) {
				return OAObjectParentService.this.getOAObjectInfoService().isOne2One(thisLi);
			}

			@Override
			/**
			 * Performs callGuidGetGuid behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public UUID callGuidGetGuid(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectGuidService().getGuid(oaObj);
			}

			@Override
			/**
			 * Performs callGuidAssignNewGuid behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callGuidAssignNewGuid(OAObject obj) {
				OAObjectParentService.this.getOAObjectGuidService().assignNewGuid(obj);
			}

			@Override
			/**
			 * Performs callGuidAssignGuid behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callGuidAssignGuid(OAObject obj) {
				OAObjectParentService.this.getOAObjectGuidService().assignGuid(obj);
			}

			@Override
			/**
			 * Performs callDSGetAssignIdOnCreate behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callDSGetAssignIdOnCreate(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectDSService().getAssignIdOnCreate(oaObj);
			}

			@Override
			/**
			 * Performs callDSAssignId behavior for the OA object service.
			 *
			 * @param oaObj method input
			 */
			public void callDSAssignId(OAObject oaObj) {
				OAObjectParentService.this.getOAObjectDSService().assignId(oaObj);
			}

			@Override
			/**
			 * Performs callCacheFireAfterLoadEvent behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public <T extends OAObject> void callCacheFireAfterLoadEvent(T obj) {
				OAObjectParentService.this.getOAObjectCacheService().fireAfterLoadEvent(obj);
			}

			@Override
			/**
			 * Performs callCacheAddToSelectAllHubs behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callCacheAddToSelectAllHubs(OAObject obj) {
				OAObjectParentService.this.getOAObjectCacheService().addToSelectAllHubs(obj);
			}

			@Override
			/**
			 * Performs callCacheAdd behavior for the OA object service.
			 *
			 * @param obj             method input
			 * @param bErrorIfExists  method input
			 * @param bAddToSelectAll method input
			 * @return result value
			 */
			public OAObject callCacheAdd(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll) {
				return OAObjectParentService.this.getOAObjectCacheService().add(obj, bErrorIfExists, bAddToSelectAll);
			}
		};
		return srvcOAObjectInitialize;
	}

	/**
	 * Returns the oAObjectKeyService value.
	 *
	 * @return result value
	 */
	public OAObjectKeyService getOAObjectKeyService() {
		if (srvcOAObjectKey != null)
			return srvcOAObjectKey;

		srvcOAObjectKey = new OAObjectKeyService() {
			@Override
			/**
			 * Performs callInfogetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfogetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callThreadLocalIsLoading behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalIsLoading() {
				return OAObjectParentService.this.srvcThreadLocal.isLoading();
			}

			@Override
			/**
			 * Performs callThreadLocalGetObjectCacheAddMode behavior for the OA object
			 * service.
			 *
			 * @return result value
			 */
			public int callThreadLocalGetObjectCacheAddMode() {
				return OAObjectParentService.this.srvcThreadLocal.getObjectCacheAddMode();
			}

			@Override
			/**
			 * Performs callReflectIsReferenceObjectLoadedAndNotEmpty behavior for the OA
			 * object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callReflectIsReferenceObjectLoadedAndNotEmpty(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceObjectLoadedAndNotEmpty(oaObj, propertyName);
			}

			@Override
			/**
			 * Performs callReflectGetProperty behavior for the OA object service.
			 *
			 * @param oaObj    method input
			 * @param propPath method input
			 * @return result value
			 */
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}

			@Override
			/**
			 * Performs callIsRemoteThread behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callIsRemoteThread() {
				return OAObjectParentService.this.srvcRemoteThread.isRemoteThread();
			}

			@Override
			/**
			 * Performs callInfoIsIdProperty behavior for the OA object service.
			 *
			 * @param oi           method input
			 * @param propertyName method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callInfoIsIdProperty(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().isIdProperty(oi, propertyName);
			}

			@Override
			/**
			 * Performs callInfoGetPropertyClass behavior for the OA object service.
			 *
			 * @param oi           method input
			 * @param propertyName method input
			 * @return result value
			 */
			public Class callInfoGetPropertyClass(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getPropertyClass(oi, propertyName);
			}

			@Override
			/**
			 * Performs callObjectInfoGetPropertyIdValues behavior for the OA object
			 * service.
			 *
			 * @param obj method input
			 * @return result value
			 */
			public Object[] callObjectInfoGetPropertyIdValues(OAObject obj) {
				return OAObjectParentService.this.getOAObjectInfoService().getPropertyIdValues(obj);
			}

			@Override
			/**
			 * Performs callDSIsAssigningId behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callDSIsAssigningId(OAObject obj) {
				return OAObjectParentService.this.getOAObjectDSService().isAssigningId(obj);
			}

			@Override
			/**
			 * Performs callDSGetObject behavior for the OA object service.
			 *
			 * @param oi    method input
			 * @param clazz method input
			 * @param key   method input
			 * @return result value
			 */
			public <T extends OAObject> T callDSGetObject(OAObjectInfo oi, Class<T> clazz, OAObjectKey key) {
				return OAObjectParentService.this.getOAObjectDSService().getObject(oi, clazz, key);
			}

			@Override
			/**
			 * Performs callDSAllowIdChange behavior for the OA object service.
			 *
			 * @param c method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callDSAllowIdChange(Class<? extends OAObject> c) {
				return OAObjectParentService.this.getOAObjectDSService().allowIdChange(c);
			}

			@Override
			/**
			 * Performs callCacheRemoveObject behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callCacheRemoveObject(OAObject obj) {
				OAObjectParentService.this.getOAObjectCacheService().removeObject(obj);
				;
			}

			@Override
			/**
			 * Performs callCachePropertyKeyValueChanged behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callCachePropertyKeyValueChanged(OAObject obj) {
				OAObjectParentService.this.getOAObjectCacheService().propertyKeyValueChanged(obj);
				;
			}

			@Override
			/**
			 * Performs callCacheGet behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param key   method input
			 * @return result value
			 */
			public <T extends OAObject> T callCacheGetUsingKey(Class<T> clazz, Object key) {
				return OAObjectParentService.this.getOAObjectCacheService().getUsingKey(clazz, key);
			}

			@Override
			/**
			 * Performs callCacheGet behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param ok    method input
			 * @return result value
			 */
			public <T extends OAObject> T callCacheGetUsingGuid(Class<T> clazz, UUID guid) {
				return OAObjectParentService.this.getOAObjectCacheService().getUsingGuid(clazz, guid);
			}

			@Override
			/**
			 * Performs callCSIsClient behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callCSGetServerObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param key   method input
			 * @return result value
			 */
			public OAObject callCSGetServerObject(Class clazz, OAObjectKey key) {
				return OAObjectParentService.this.getOAObjectCSService().callSyncServerGetObject(clazz, key);
			}
		};
		return srvcOAObjectKey;
	}

	/**
	 * Returns the oAObjectLockService value.
	 *
	 * @return result value
	 */
	public OAObjectLockService getOAObjectLockService() {
		if (srvcOAObjectLock != null)
			return srvcOAObjectLock;

		srvcOAObjectLock = new OAObjectLockService() {
			@Override
			public boolean callSyncIsClientOrServer() {
				return OAObjectParentService.this.srvcSync.isClientOrServer();
			}

			@Override
			/**
			 * Performs callSyncSetLock behavior for the OA object service.
			 *
			 * @param objectClass method input
			 * @param objectKey   method input
			 * @param bLock       method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncSetLock(Class<? extends OAObject> objectClass, OAObjectKey objectKey, boolean bLock) {
				RemoteSessionInterface rs = OAObjectParentService.this.srvcSync.getRemoteSession();
				if (rs == null)
					return false;
				return rs.setLock(objectClass, objectKey, bLock);
			}

			@Override
			/**
			 * Performs callSyncIsLocked behavior for the OA object service.
			 *
			 * @param objectClass method input
			 * @param objectKey   method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsLocked(Class<? extends OAObject> objectClass, OAObjectKey objectKey) {
				RemoteSessionInterface rs = OAObjectParentService.this.srvcSync.getRemoteSession();
				if (rs == null)
					return false;
				return rs.isLocked(objectClass, objectKey);
			}

			@Override
			/**
			 * Performs callRemoteThreadStartNextThread behavior for the OA object service.
			 */
			public void callRemoteThreadStartNextThread() {
				OAObjectParentService.this.srvcRemoteThread.startNextThread();
			}
		};
		return srvcOAObjectLock;
	}


	public OAObjectPropertyService getOAObjectPropertyService() {
		if (srvcOAObjectProperty != null)
			return srvcOAObjectProperty;
		srvcOAObjectProperty = new OAObjectPropertyService(faBridge.getObjectFriendAccess()) {
			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callSyncIsServer behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callKeyIsForSameOAObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param ok1   method input
			 * @param ok2   method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectParentService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}

			@Override
			/**
			 * Performs callKeyGetKey behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}

			@Override
			/**
			 * Performs callInfoIsWeakReferenceable behavior for the OA object service.
			 *
			 * @param oi method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callInfoIsWeakReferenceable(OAObjectInfo oi) {
				return OAObjectParentService.this.getOAObjectInfoService().isWeakReferenceable(oi);
			}

			@Override
			/**
			 * Performs callInfoGetLinkInfo behavior for the OA object service.
			 *
			 * @param clazz        method input
			 * @param propertyName method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetLinkInfo(Class clazz, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(clazz, propertyName);
			}

			@Override
			/**
			 * Performs callHubSetMasterObject behavior for the OA object service.
			 *
			 * @param hub                    method input
			 * @param oaObj                  method input
			 * @param nameFromMasterToDetail method input
			 */
			public void callHubSetMasterObject(Hub<?> hub, OAObject oaObj, String nameFromMasterToDetail) {
				OAObjectParentService.this.getOAObjectHubService().setMasterObject(hub, oaObj, nameFromMasterToDetail);
			}

			@Override
			/**
			 * Performs callCacheGet behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param ok    method input
			 * @return result value
			 */
			public <T extends OAObject> T callCacheGetUsingKey(Class<T> clazz, Object id) {
				return OAObjectParentService.this.getOAObjectCacheService().getUsingKey(clazz, id);
			}
		};
		return srvcOAObjectProperty;
	}

	/**
	 * Returns the oAObjectRecurseService value.
	 *
	 * @return result value
	 */
	public OAObjectRecurseService getOAObjectRecurseService() {
		if (srvcOAObjectRecurse != null)
			return srvcOAObjectRecurse;

		srvcOAObjectRecurse = new OAObjectRecurseService() {
			@Override
			/**
			 * Performs callObjectReflectGetProperty behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return result value
			 */
			public Object callObjectReflectGetProperty(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, name);
			}

			@Override
			/**
			 * Performs callObjectInfoGetOAObjectInfo behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(oaObj);
			}
		};

		return srvcOAObjectRecurse;
	}

	/**
	 * Returns the oAObjectReflectService value.
	 *
	 * @return result value
	 */
	public OAObjectReflectService getOAObjectReflectService() {
		if (srvcOAObjectReflect != null)
			return srvcOAObjectReflect;
		srvcOAObjectReflect = new OAObjectReflectService(faBridge.getObjectFriendAccess()) {
			@Override
			/**
			 * Returns the oAObjectInfo value.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo getOAObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Returns the cSGetServerReferenceHub value.
			 *
			 * @param oaObj            method input
			 * @param linkPropertyName method input
			 * @return result value
			 */
			public Hub<?> getCSGetServerReferenceHub(OAObject oaObj, String linkPropertyName) {
				return OAObjectParentService.this.getOAObjectCSService().getServerReferenceHub(oaObj, linkPropertyName);
			}

			@Override
			/**
			 * Performs callThreadLocalSetLoading behavior for the OA object service.
			 *
			 * @param b method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalSetLoading(boolean b) {
				return OAObjectParentService.this.srvcThreadLocal.setLoading(b);
			}

			@Override
			/**
			 * Performs callThreadLocalRemoveSiblingHelper behavior for the OA object
			 * service.
			 *
			 * @param sh method input
			 */
			public void callThreadLocalRemoveSiblingHelper(OASiblingHelper sh) {
				OAObjectParentService.this.srvcThreadLocal.removeSiblingHelper(sh);
			}

			@Override
			/**
			 * Performs callThreadLocalIsLoading behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalIsLoading() {
				return OAObjectParentService.this.srvcThreadLocal.isLoading();
			}

			@Override
			/**
			 * Performs callThreadLocalIsDeleting behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalIsDeleting() {
				return OAObjectParentService.this.srvcThreadLocal.isDeleting();
			}

			@Override
			/**
			 * Performs callThreadLocalGetObjectCacheAddMode behavior for the OA object
			 * service.
			 *
			 * @return result value
			 */
			public int callThreadLocalGetObjectCacheAddMode() {
				return OAObjectParentService.this.srvcThreadLocal.getObjectCacheAddMode();
			}

			@Override
			/**
			 * Performs callThreadLocalAddSiblingHelper behavior for the OA object service.
			 *
			 * @param sh method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalAddSiblingHelper(OASiblingHelper sh) {
				return OAObjectParentService.this.srvcThreadLocal.addSiblingHelper(sh);
			}

			@Override
			/**
			 * Performs callSyncIsObjectOnServer behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsObjectOnServer(OAObject obj) {
				return OAObjectParentService.this.srvcSync.getClient().isObjectOnServer(obj);
			}

			@Override
			/**
			 * Performs callSiblingOnGetObjectReference behavior for the OA object service.
			 *
			 * @param obj              method input
			 * @param linkPropertyName method input
			 */
			public void callSiblingOnGetObjectReference(OAObject obj, String linkPropertyName) {
				OAObjectParentService.this.getOAObjectSiblingService().onGetObjectReference(obj, linkPropertyName);
				;
			}

			@Override
			/**
			 * Performs callSiblingGetSiblings behavior for the OA object service.
			 *
			 * @param mainObject method input
			 * @param property   method input
			 * @param maxAmount  method input
			 * @param hmIgnore   method input
			 * @return result value
			 */
			public OAObjectKey[] callSiblingGetSiblings(OAObject mainObject, String property, int maxAmount, ConcurrentHashMap<UUID, Boolean> hmIgnore) {
				return OAObjectParentService.this.getOAObjectSiblingService().getSiblings(mainObject, property, maxAmount, hmIgnore);
			}

			@Override
			/**
			 * Performs callPropertyUnsafeSetProperty behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @param value method input
			 */
			public void callPropertyUnsafeSetProperty(OAObject oaObj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().unsafeSetProperty(oaObj, name, value);
			}

			@Override
			/**
			 * Performs callLockSetPropertyLock behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callLockSetPropertyLock(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectLockService().setPropertyLock(oaObj, name);
			}

			@Override
			/**
			 * Performs callPropertySetPropertyHubIfNotSet behavior for the OA object
			 * service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @param value method input
			 */
			public void callPropertySetPropertyHubIfNotSet(OAObject oaObj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().setPropertyHubIfNotSet(oaObj, name, value);
			}

			@Override
			/**
			 * Performs callPropertySetPropertyCAS behavior for the OA object service.
			 *
			 * @param oaObj           method input
			 * @param name            method input
			 * @param newValue        method input
			 * @param matchValue      method input
			 * @param bMustNotExist   method input
			 * @param bReturnNotExist method input
			 * @return result value
			 */
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue, bMustNotExist, bReturnNotExist);
			}

			@Override
			/**
			 * Performs callPropertySetPropertyCAS behavior for the OA object service.
			 *
			 * @param oaObj      method input
			 * @param name       method input
			 * @param newValue   method input
			 * @param matchValue method input
			 * @return result value
			 */
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue);
			}

			@Override
			/**
			 * Performs callPropertySetProperty behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @param value method input
			 */
			public void callPropertySetProperty(OAObject oaObj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().setProperty(oaObj, name, value);
			}

			@Override
			/**
			 * Performs callLockReleasePropertyLock behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 */
			public void callLockReleasePropertyLock(OAObject oaObj, String name) {
				OAObjectParentService.this.getOAObjectLockService().releasePropertyLock(oaObj, name);
			}

			@Override
			/**
			 * Performs callLockIsPropertyLocked behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callLockIsPropertyLocked(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectLockService().isPropertyLocked(oaObj, name);
			}

			@Override
			/**
			 * Performs callPropertyIsPropertyLoaded behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callPropertyIsPropertyLoaded(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectPropertyService().isPropertyLoaded(oaObj, name);
			}

			@Override
			/**
			 * Performs callPropertyGetProperty behavior for the OA object service.
			 *
			 * @param oaObj           method input
			 * @param name            method input
			 * @param bReturnNotExist method input
			 * @param bConvertWeakRef method input
			 * @return result value
			 */
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}

			@Override
			/**
			 * Performs callLockAttemptPropertyLock behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callLockAttemptPropertyLock(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectLockService().attemptPropertyLock(oaObj, name);
			}

			@Override
			/**
			 * Performs callKeyIsForSameOAObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param ok1   method input
			 * @param ok2   method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectParentService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}

			@Override
			/**
			 * Performs callKeyGetKey behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}

			@Override
			/**
			 * Performs callKeyCreateObjectKey behavior for the OA object service.
			 *
			 * @param c   method input
			 * @param ids method input
			 * @return result value
			 */
			public OAObjectKey callKeyCreateObjectKey(Class c, Object... ids) {
				return OAObjectParentService.this.getOAObjectKeyService().createObjectKey(c, ids);
			}

			@Override
			/**
			 * Performs callRemoteThreadIsRemoteThread behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callRemoteThreadIsRemoteThread() {
				return OAObjectParentService.this.srvcRemoteThread.isRemoteThread();
			}

			@Override
			/**
			 * Performs callInitializeInitialize behavior for the OA object service.
			 *
			 * @param oaObj              method input
			 * @param oi                 method input
			 * @param bInitializeNulls   method input
			 * @param bInitializeWithDS  method input
			 * @param bAddToCache        method input
			 * @param bInitializeWithCS  method input
			 * @param bSetChangedToFalse method input
			 */
			public void callInitializeInitialize(OAObject oaObj, OAObjectInfo oi, boolean bInitializeNulls, boolean bInitializeWithDS, boolean bAddToCache, boolean bInitializeWithCS, boolean bSetChangedToFalse) {
				OAObjectParentService.this.getOAObjectInitializeService().initialize(oaObj, oi, bInitializeNulls, bInitializeWithDS, bAddToCache, bInitializeWithCS, bSetChangedToFalse);
			}

			@Override
			/**
			 * Performs callInfoSetPrimitiveNull behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param bSetToNull   method input
			 */
			public void callInfoSetPrimitiveNull(OAObject oaObj, String propertyName, boolean bSetToNull) {
				OAObjectParentService.this.getOAObjectInfoService().setPrimitiveNull(oaObj, propertyName, bSetToNull);
			}

			@Override
			/**
			 * Performs callInfoIsPrimitiveNull behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callInfoIsPrimitiveNull(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().isPrimitiveNull(oaObj, propertyName);
			}

			@Override
			/**
			 * Performs callInfoIsOne2One behavior for the OA object service.
			 *
			 * @param thisLi method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callInfoIsOne2One(OALinkInfo thisLi) {
				return OAObjectParentService.this.getOAObjectInfoService().isOne2One(thisLi);
			}

			@Override
			/**
			 * Performs callInfoGetReverseLinkInfo behavior for the OA object service.
			 *
			 * @param li method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectInfoService().getReverseLinkInfo(li);
			}

			@Override
			/**
			 * Performs callInfoGetRecursiveLinkInfo behavior for the OA object service.
			 *
			 * @param thisOI method input
			 * @param type   method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type) {
				return OAObjectParentService.this.getOAObjectInfoService().getRecursiveLinkInfo(thisOI, type);
			}

			@Override
			/**
			 * Performs callInfoGetMethod behavior for the OA object service.
			 *
			 * @param oi         method input
			 * @param methodName method input
			 * @param classParam method input
			 * @return result value
			 */
			public Method callInfoGetMethod(OAObjectInfo oi, String methodName, Class classParam) {
				return getOAObjectInfoService().getMethod(oi, methodName, classParam);
			}

			@Override
			/**
			 * Performs callInfoGetMethod behavior for the OA object service.
			 *
			 * @param oi            method input
			 * @param methodName    method input
			 * @param argumentCount method input
			 * @return result value
			 */
			public Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount) {
				return OAObjectParentService.this.getOAObjectInfoService().getMethod(oi, methodName, argumentCount);
			}

			@Override
			/**
			 * Performs callInfoGetLinkInfo behavior for the OA object service.
			 *
			 * @param clazz        method input
			 * @param propertyName method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetLinkInfo(Class clazz, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(clazz, propertyName);
			}

			@Override
			/**
			 * Performs callInfoGetLinkInfo behavior for the OA object service.
			 *
			 * @param oi           method input
			 * @param propertyName method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}

			@Override
			/**
			 * Performs callInfoCacheHub behavior for the OA object service.
			 *
			 * @param li  method input
			 * @param hub method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callInfoCacheHub(OALinkInfo li, Hub<?> hub) {
				return OAObjectParentService.this.getOAObjectInfoService().cacheHub(li, hub);
			}

			@Override
			/**
			 * Performs callHubSortSort behavior for the OA object service.
			 *
			 * @param thisHub                    method input
			 * @param paths                      method input
			 * @param bAscending                 method input
			 * @param comp                       method input
			 * @param bAlreadySortedAndLocalOnly method input
			 */
			public void callHubSortSort(Hub<?> thisHub, String paths, boolean bAscending, Comparator comp, boolean bAlreadySortedAndLocalOnly) {
				OAObjectParentService.this.srvcHubParent.getHubSortService().sort(thisHub, paths, bAscending, comp, bAlreadySortedAndLocalOnly);
			}

			@Override
			/**
			 * Performs callHubSortIsSorted behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callHubSortIsSorted(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubSortService().isSorted(thisHub);
			}

			@Override
			/**
			 * Performs callHubSortGetSortProperty behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @return result value
			 */
			public String callHubSortGetSortProperty(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubSortService().getSortProperty(thisHub);
			}

			@Override
			/**
			 * Performs callHubSortGetSortListener behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @return result value
			 */
			public HubSortListener callHubSortGetSortListener(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubSortService().getSortListener(thisHub);
			}

			@Override
			/**
			 * Performs callHubSortGetSortAsc behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callHubSortGetSortAsc(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubSortService().getSortAsc(thisHub);
			}

			@Override
			/**
			 * Performs callHubShareIsUsingSameSharedHub behavior for the OA object service.
			 *
			 * @param hub1 method input
			 * @param hub2 method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callHubShareIsUsingSameSharedHub(Hub<?> hub1, Hub<?> hub2) {
				return OAObjectParentService.this.srvcHubParent.getHubShareService().isUsingSameSharedHub(hub1, hub2);
			}

			@Override
			/**
			 * Performs callHubShareIsUsingSameSharedAO behavior for the OA object service.
			 *
			 * @param hub1                 method input
			 * @param hub2                 method input
			 * @param bIncludeFilteredHubs method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callHubShareIsUsingSameSharedAO(Hub<?> hub1, Hub<?> hub2, boolean bIncludeFilteredHubs) {
				return OAObjectParentService.this.srvcHubParent.getHubShareService().isUsingSameSharedAO(hub1, hub2, bIncludeFilteredHubs);
			}

			@Override
			/**
			 * Performs callHubSelectLoadAllData behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @param select  method input
			 */
			public void callHubSelectLoadAllData(Hub<?> thisHub, OASelect select) {
				OAObjectParentService.this.srvcHubParent.getHubSelectService().loadAllData(thisHub, select);
			}

			@Override
			/**
			 * Performs callHubLinkGetLinkedOnPos behavior for the OA object service.
			 *
			 * @param thisHub            method input
			 * @param bIncludeCopiedHubs method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callHubLinkGetLinkedOnPos(Hub<?> thisHub, boolean bIncludeCopiedHubs) {
				return OAObjectParentService.this.srvcHubParent.getHubLinkService().getLinkedOnPos(thisHub, bIncludeCopiedHubs);
			}

			@Override
			/**
			 * Performs callHubLinkGetLinkToProperty behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @return result value
			 */
			public String callHubLinkGetLinkToProperty(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubLinkService().getLinkToProperty(thisHub);
			}

			@Override
			/**
			 * Performs callHubLinkGetLinkToHub behavior for the OA object service.
			 *
			 * @param thisHub            method input
			 * @param bIncludeCopiedHubs method input
			 * @return result value
			 */
			public Hub<?> callHubLinkGetLinkToHub(Hub<?> thisHub, boolean bIncludeCopiedHubs) {
				return OAObjectParentService.this.srvcHubParent.getHubLinkService().getLinkToHub(thisHub, bIncludeCopiedHubs);
			}

			@Override
			/**
			 * Performs callHubLinkGetLinkHubPath behavior for the OA object service.
			 *
			 * @param thisHub            method input
			 * @param bIncludeCopiedHubs method input
			 * @return result value
			 */
			public String callHubLinkGetLinkHubPath(Hub<?> thisHub, boolean bIncludeCopiedHubs) {
				return OAObjectParentService.this.srvcHubParent.getHubLinkService().getLinkHubPath(thisHub, bIncludeCopiedHubs);
			}

			@Override
			/**
			 * Performs callHubLinkGetLinkFromProperty behavior for the OA object service.
			 *
			 * @param thisHub            method input
			 * @param bIncludeCopiedHubs method input
			 * @return result value
			 */
			public String callHubLinkGetLinkFromProperty(Hub<?> thisHub, boolean bIncludeCopiedHubs) {
				return OAObjectParentService.this.srvcHubParent.getHubLinkService().getLinkFromProperty(thisHub, bIncludeCopiedHubs);
			}

			@Override
			/**
			 * Performs callHubMasterGetMasterObject behavior for the OA object service.
			 *
			 * @param hub method input
			 * @return result value
			 */
			public OAObject callHubMasterGetMasterObject(Hub<?> hub) {
				return OAObjectParentService.this.srvcHubParent.getHubMasterService().getMasterObject(hub);
			}

			@Override
			/**
			 * Performs callHubGetHubReferences behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public Hub[] callHubGetHubReferences(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}

			@Override
			/**
			 * Performs callHubGetHub behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param li    method input
			 * @return result value
			 */
			public Hub<?> callHubGetHub(OAObject oaObj, OALinkInfo li) {
				return OAObjectParentService.this.getOAObjectHubService().getHub(oaObj, li);
			}

			@Override
			/**
			 * Performs callHubGetAutoSequence behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @return result value
			 */
			public HubAutoSequence callHubGetAutoSequence(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubSequenceService().getAutoSequence(thisHub);
			}

			@Override
			/**
			 * Performs callHubGetAutoMatch behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @return result value
			 */
			public HubAutoMatch callHubGetAutoMatch(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubAutoMatchService().getAutoMatch(thisHub);
			}

			@Override
			/**
			 * Performs callHubDetailGetPropertyFromMasterToDetail behavior for the OA
			 * object service.
			 *
			 * @param thisHub method input
			 * @return result value
			 */
			public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubDetailService().getPropertyFromMasterToDetail(thisHub);
			}

			@Override
			/**
			 * Performs callHubDetailGetPropertyFromDetailToMaster behavior for the OA
			 * object service.
			 *
			 * @param thisHub method input
			 * @return result value
			 */
			public String callHubDetailGetPropertyFromDetailToMaster(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubDetailService().getPropertyFromDetailToMaster(thisHub);
			}

			@Override
			/**
			 * Performs callHubDetailGetLinkInfoFromDetailToMaster behavior for the OA
			 * object service.
			 *
			 * @param hub method input
			 * @return result value
			 */
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return OAObjectParentService.this.srvcHubParent.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}

			@Override
			/**
			 * Performs callHubDataResizeToFit behavior for the OA object service.
			 *
			 * @param thisHub method input
			 */
			public void callHubDataResizeToFit(Hub<?> thisHub) {
				OAObjectParentService.this.srvcHubParent.getHubDataService().resizeToFit(thisHub);
			}

			@Override
			/**
			 * Performs callGuidGetGuid behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public UUID callGuidGetGuid(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectGuidService().getGuid(oaObj);
			}

			@Override
			/**
			 * Performs callEventFirePropertyChange behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param oldObj       method input
			 * @param newObj       method input
			 * @param bLocalOnly   method input
			 * @param bSetChanged  method input
			 */
			public void callEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}

			@Override
			/**
			 * Performs callEventFireBeforePropertyChange behavior for the OA object
			 * service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param oldObj       method input
			 * @param newObj       method input
			 * @param bLocalOnly   method input
			 * @param bSetChanged  method input
			 */
			public void callEventFireBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}

			@Override
			/**
			 * Performs callDSGetObject behavior for the OA object service.
			 *
			 * @param oi    method input
			 * @param clazz method input
			 * @param key   method input
			 * @return result value
			 */
			public <T extends OAObject> T callDSGetObject(OAObjectInfo oi, Class<T> clazz, OAObjectKey key) {
				return OAObjectParentService.this.getOAObjectDSService().getObject(oi, clazz, key);
			}

			@Override
			/**
			 * Performs callDSGetObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param key   method input
			 * @return result value
			 */
			public <T extends OAObject> T callDSGetObject(Class<T> clazz, OAObjectKey key) {
				return OAObjectParentService.this.getOAObjectDSService().getObject(clazz, key);
			}

			@Override
			/**
			 * Performs callDSGetDataSource behavior for the OA object service.
			 *
			 * @param c method input
			 * @return result value
			 */
			public OADataSource callDSGetDataSource(Class c) {
				return OAObjectParentService.this.getOAObjectDSService().getDataSource(c);
			}

			@Override
			/**
			 * Performs callCacheGet behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param ok    method input
			 * @return result value
			 */
			public <T extends OAObject> T callCacheGetUsingKey(Class<T> clazz, Object key) {
				return OAObjectParentService.this.getOAObjectCacheService().getUsingKey(clazz, key);
			}

			public <T extends OAObject> T callCacheGetUsingGuid(Class<T> clazz, UUID guid) {
				return OAObjectParentService.this.getOAObjectCacheService().getUsingGuid(clazz, guid);
			}
			
			@Override
			/**
			 * Performs callCacheAdd behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return result value
			 */
			public OAObject callCacheAdd(OAObject obj) {
				return OAObjectParentService.this.getOAObjectCacheService().add(obj);
			}

			@Override
			/**
			 * Performs callCSLoadReferenceHubDataOnServer behavior for the OA object
			 * service.
			 *
			 * @param thisHub method input
			 * @param select  method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callCSLoadReferenceHubDataOnServer(Hub<?> thisHub, OASelect select) {
				return OAObjectParentService.this.getOAObjectCSService().loadReferenceHubDataOnServer(thisHub, select);
			}

			@Override
			/**
			 * Performs callCSIsClient behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callCSGetServerReferenceBlob behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @return result value
			 */
			public byte[] callCSGetServerReferenceBlob(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectCSService().getServerReferenceBlob(oaObj, propertyName);
			}

			@Override
			/**
			 * Performs callCSGetServerReference behavior for the OA object service.
			 *
			 * @param oaObj            method input
			 * @param linkPropertyName method input
			 * @return result value
			 */
			public Object callCSGetServerReference(OAObject oaObj, String linkPropertyName) {
				return OAObjectParentService.this.getOAObjectCSService().getServerReference(oaObj, linkPropertyName);
			}

			@Override
			/**
			 * Performs callCSGetServerObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param key   method input
			 * @return result value
			 */
			public OAObject callCSGetServerObject(Class clazz, OAObjectKey key) {
				return OAObjectParentService.this.getOAObjectCSService().callSyncServerGetObject(clazz, key);
			}

			@Override
			/**
			 * Performs callCSCreateCopy behavior for the OA object service.
			 *
			 * @param oaObj             method input
			 * @param excludeProperties method input
			 * @return result value
			 */
			public OAObject callCSCreateCopy(OAObject oaObj, String[] excludeProperties) {
				return OAObjectParentService.this.getOAObjectCSService().createCopy(oaObj, excludeProperties);
			}

			@Override
			/**
			 * Performs callThreadLocalGetSendSyncMessages behavior for the OA object
			 * service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalGetSendSyncMessages() {
				return OAObjectParentService.this.srvcThreadLocal.getSendSyncMessages();
			}

			@Override
			/**
			 * Performs callThreadLocalSetSendSyncMessages behavior for the OA object
			 * service.
			 *
			 * @param b method input
			 */
			public void callThreadLocalSetSendSyncMessages(boolean b) {
				OAObjectParentService.this.srvcThreadLocal.setSendSyncMessages(b);
			}

			@Override
			/**
			 * Performs callThreadLocalGetLoading behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalGetLoading() {
				return OAObjectParentService.this.srvcThreadLocal.isLoading();
			}
		};
		return srvcOAObjectReflect;
	}

	/**
	 * Returns the oAObjectSaveService value.
	 *
	 * @return result value
	 */
	public OAObjectSaveService getOAObjectSaveService() {
		if (srvcOAObjectSave != null)
			return srvcOAObjectSave;
		srvcOAObjectSave = new OAObjectSaveService(faBridge.getObjectFriendAccess()) {
			@Override
			/**
			 * Performs callCSIsClient behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callCSServerSave behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param iCascadeRule method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callCSServerSave(OAObject oaObj, int iCascadeRule) {
				return OAObjectParentService.this.getOAObjectCSService().save(oaObj, iCascadeRule);
			}

			@Override
			/**
			 * Performs callHubGetHubReferences behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public Hub[] callHubGetHubReferences(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}

			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(OAObject obj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(obj);
			}

			@Override
			/**
			 * Performs callReflectIsReferenceNullOrNotLoaded behavior for the OA object
			 * service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callReflectIsReferenceNullOrNotLoaded(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectReflectService().isReferenceNullOrNotLoaded(oaObj, propertyName);
			}

			@Override
			/**
			 * Performs callReflectGetProperty behavior for the OA object service.
			 *
			 * @param oaObj    method input
			 * @param propPath method input
			 * @return result value
			 */
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}

			@Override
			/**
			 * Performs callInfoGetOAObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetOAObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(getClass());
			}

			@Override
			/**
			 * Performs callDSSaveWithoutReferences behavior for the OA object service.
			 *
			 * @param oaObj method input
			 */
			public void callDSSaveWithoutReferences(OAObject oaObj) {
				OAObjectParentService.this.getOAObjectDSService().saveWithoutReferences(oaObj);
			}

			@Override
			/**
			 * Performs callObjectSetNew behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param b     method input
			 */
			public void callObjectSetNew(OAObject oaObj, boolean b) {
				OAObjectParentService.this.getOAObjectStateService().setNew(oaObj, b);
			}

			@Override
			/**
			 * Performs callHubSaveAll behavior for the OA object service.
			 *
			 * @param hub          method input
			 * @param iCascadeRule method input
			 * @param cascade      method input
			 */
			public void callHubSaveAll(Hub<?> hub, int iCascadeRule, OACascade cascade) {
				OAObjectParentService.this.getOAObjectHubService().saveAll(hub, iCascadeRule, cascade);
			}

			@Override
			/**
			 * Performs callReflectGetRawReference behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return result value
			 */
			public Object callReflectGetRawReference(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectReflectService().getRawReference(oaObj, name);
			}

			@Override
			/**
			 * Performs callDSSave behavior for the OA object service.
			 *
			 * @param oaObj method input
			 */
			public void callDSSave(OAObject oaObj) {
				OAObjectParentService.this.getOAObjectDSService().save(oaObj);
			}

			/*
			 * qqqqq
			 * 
			 * @Override
			 * 
			 * 
			 * public void callLogLogToXmlFile(OAObject oaObj, boolean bSave) {
			 * OAObjectParentService.this.getOAObjectLogService().logToXmlFile(oaObj,
			 * bSave); }
			 */
			@Override
			/**
			 * Performs callHubEventFireBeforeSaveEvent behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @param obj     method input
			 */
			public <T extends OAObject> void callHubEventFireBeforeSaveEvent(Hub<T> thisHub, T obj) {
				OAObjectParentService.this.srvcHubParent.getHubEventService().fireBeforeSaveEvent(thisHub, obj);
			}

			@Override
			/**
			 * Performs callHubEventFireAfterSaveEvent behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @param obj     method input
			 */
			public <T extends OAObject> void callHubEventFireAfterSaveEvent(Hub<T> thisHub, T obj) {
				OAObjectParentService.this.srvcHubParent.getHubEventService().fireAfterSaveEvent(thisHub, obj);
			}

			@Override
			/**
			 * Performs callThreadLocalIsDeleting behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalIsDeleting() {
				return OAObjectParentService.this.srvcThreadLocal.isDeleting();
			}

			@Override
			/**
			 * Performs callHubIsInHubWithMaster behavior for the OA object service.
			 *
			 * @param thisObj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			protected boolean callHubIsInHubWithMaster(OAObject thisObj) {
				return OAObjectParentService.this.getOAObjectHubService().isInHubWithMaster(thisObj);
			}

			@Override
			/**
			 * Performs callRemoteSyncAddNewToCache behavior for the OA object service.
			 *
			 * @param oos method input
			 */
			protected void callRemoteSyncAddNewToCache(OAObjectSerializer<? extends OAObject> oos) {
				OAObjectParentService.this.srvcSync.getRemoteSync().addNewToCache(oos);
			}

			@Override
			/**
			 * Performs callCSSyncIsRunning behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			protected boolean callCSSyncIsRunning() {
				return OAObjectParentService.this.getOAObjectCSService().callSyncIsRunning();
			}
		};
		return srvcOAObjectSave;
	}

	/**
	 * Returns the oAObjectSchedulerService value.
	 *
	 * @return result value
	 */
	public OAObjectSchedulerService getOAObjectSchedulerService() {
		if (srvcOAObjectScheduler != null)
			return srvcOAObjectScheduler;
		srvcOAObjectScheduler = new OAObjectSchedulerService() {
			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(OAObject obj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(obj);
			}
		};
		return srvcOAObjectScheduler;
	}

	/**
	 * Returns the oAObjectSerializeService value.
	 *
	 * @return result value
	 */
	public OAObjectSerializeService getOAObjectSerializeService() {
		if (srvcOAObjectSerialize != null)
			return srvcOAObjectSerialize;

		srvcOAObjectSerialize = new OAObjectSerializeService(faBridge.getObjectSerializerFriendAccess()) {
			@Override
			/**
			 * Performs callGuidSetGuid behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param guid  method input
			 */
			public void callGuidSetGuid(OAObject oaObj, UUID guid) {
				OAObjectParentService.this.getOAObjectGuidService().setGuid(oaObj, guid);
			}

			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(Class clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callPropertyUnsafeSetPropertyIfEmpty behavior for the OA object
			 * service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @param value method input
			 */
			public void callPropertyUnsafeSetPropertyIfEmpty(OAObject oaObj, String name, Object value) {
				OAObjectParentService.this.getOAObjectPropertyService().unsafeSetPropertyIfEmpty(oaObj, name, value);
			}

			@Override
			/**
			 * Performs callGuidGetGuid behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public UUID callGuidGetGuid(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectGuidService().getGuid(oaObj);
			}

			@Override
			/**
			 * Performs callGuiAssignGuid behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callGuiAssignGuid(OAObject obj) {
				OAObjectParentService.this.getOAObjectGuidService().assignGuid(obj);
			}

			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(OAObject obj) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(obj);
			}

			@Override
			/**
			 * Performs callCacheAdd behavior for the OA object service.
			 *
			 * @param obj                          method input
			 * @param bErrorIfExists               method input
			 * @param bAddToSelectAll              method input
			 * @param bSendAddEventInAnotherThread method input
			 * @return result value
			 */
			public OAObject callCacheAdd(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll, boolean bSendAddEventInAnotherThread) {
				return OAObjectParentService.this.getOAObjectCacheService().add(obj, bErrorIfExists, bAddToSelectAll, bSendAddEventInAnotherThread);
			}

			@Override
			/**
			 * Performs callGetProperties behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return result value
			 */
			public Object[] callGetProperties(OAObject obj) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperties(obj);
			}

			@Override
			/**
			 * Performs callPropertyGetProperty behavior for the OA object service.
			 *
			 * @param oaObj           method input
			 * @param name            method input
			 * @param bReturnNotExist method input
			 * @param bConvertWeakRef method input
			 * @return result value
			 */
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}

			@Override
			/**
			 * Performs callKeyGetKey behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public OAObjectKey callKeyGetKey(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectKeyService().getKey(oaObj);
			}

			@Override
			/**
			 * Performs callKeyIsForSameOAObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param ok1   method input
			 * @param ok2   method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callKeyIsForSameOAObject(Class<? extends OAObject> clazz, OAObjectKey ok1, OAObjectKey ok2) {
				return OAObjectParentService.this.getOAObjectKeyService().isForSameOAObject(clazz, ok1, ok2);
			}

			@Override
			/**
			 * Performs callPropertySetPropertyCAS behavior for the OA object service.
			 *
			 * @param oaObj      method input
			 * @param name       method input
			 * @param newValue   method input
			 * @param matchValue method input
			 * @return result value
			 */
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue);
			}

			@Override
			/**
			 * Performs callInfoGetLinkInfo behavior for the OA object service.
			 *
			 * @param oi           method input
			 * @param propertyName method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(oi, propertyName);
			}

			@Override
			/**
			 * Performs callPropertyAttemptPropertyLock behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callPropertyAttemptPropertyLock(OAObject oaObj, String name) {
				return OAObjectParentService.this.getOAObjectLockService().attemptPropertyLock(oaObj, name);
			}

			@Override
			/**
			 * Performs callPropertySetPropertyCAS behavior for the OA object service.
			 *
			 * @param oaObj           method input
			 * @param name            method input
			 * @param newValue        method input
			 * @param matchValue      method input
			 * @param bMustNotExist   method input
			 * @param bReturnNotExist method input
			 * @return result value
			 */
			public Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist) {
				return OAObjectParentService.this.getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue, bMustNotExist, bReturnNotExist);
			}

			@Override
			/**
			 * Performs callPropertyReleasePropertyLock behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @param name  method input
			 */
			public void callPropertyReleasePropertyLock(OAObject oaObj, String name) {
				OAObjectParentService.this.getOAObjectLockService().releasePropertyLock(oaObj, name);
			}

			@Override
			/**
			 * Performs callInfoCacheHub behavior for the OA object service.
			 *
			 * @param li  method input
			 * @param hub method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callInfoCacheHub(OALinkInfo li, Hub<?> hub) {
				return OAObjectParentService.this.getOAObjectInfoService().cacheHub(li, hub);
			}

			@Override
			/**
			 * Performs callCSIsClient behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}

			@Override
			/**
			 * Performs callHubSerializeReplaceObject behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @param objFrom method input
			 * @param objTo   method input
			 * @return result value
			 */
			public int callHubSerializeReplaceObject(Hub<?> thisHub, OAObject objFrom, OAObject objTo) {
				return OAObjectParentService.this.getOAObjectSerializeService().callHubSerializeReplaceObject(thisHub, objFrom, objTo);
			}

			@Override
			/**
			 * Performs callHubSerializeIsResolved behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callHubSerializeIsResolved(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubSerializeService().isResolved(thisHub);
			}

			@Override
			/**
			 * Performs callHubSerializeReplaceMasterObject behavior for the OA object
			 * service.
			 *
			 * @param thisHub method input
			 * @param objFrom method input
			 * @param objTo   method input
			 */
			public <T extends OAObject> void callHubSerializeReplaceMasterObject(Hub<T> thisHub, T objFrom, T objTo) {
				OAObjectParentService.this.srvcHubParent.getHubSerializeService().replaceMasterObject(thisHub, objFrom, objTo);
			}

			@Override
			/**
			 * Performs callHubGetAutoMatch behavior for the OA object service.
			 *
			 * @param thisHub method input
			 * @return result value
			 */
			public HubAutoMatch callHubGetAutoMatch(Hub<?> thisHub) {
				return OAObjectParentService.this.srvcHubParent.getHubAutoMatchService().getAutoMatch(thisHub);
			}

			@Override
			/**
			 * Performs callSyncClientIsObjectOnServer behavior for the OA object service.
			 *
			 * @param obj method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncClientIsObjectOnServer(OAObject obj) {
				OASyncClient sc = OAObjectParentService.this.srvcSync.getClient();
				if (sc == null)
					return false;
				return sc.isObjectOnServer(obj);
			}

			@Override
			/**
			 * Performs callSyncClientObjectSentToServer behavior for the OA object service.
			 *
			 * @param obj method input
			 */
			public void callSyncClientObjectSentToServer(OAObject obj) {
				OASyncClient sc = OAObjectParentService.this.srvcSync.getClient();
				if (sc != null)
					sc.objectSentToServer(obj);
			}

			@Override
			/**
			 * Performs callThreadLocalGetCurrentObjectSerializer behavior for the OA object
			 * service.
			 *
			 * @return result value
			 */
			public OAObjectSerializer callThreadLocalGetCurrentObjectSerializer() {
				return OAObjectParentService.this.srvcThreadLocal.getCurrentObjectSerializer();
			}
		};
		return srvcOAObjectSerialize;
	}

	/**
	 * Returns the oAObjectSiblingService value.
	 *
	 * @return result value
	 */
	public OAObjectSiblingService getOAObjectSiblingService() {
		if (srvcOAObjectSibling != null)
			return srvcOAObjectSibling;

		srvcOAObjectSibling = new OAObjectSiblingService() {
			@Override
			/**
			 * Performs callThreadLocalGetSiblingHelpers behavior for the OA object service.
			 *
			 * @return result value
			 */
			public List<OASiblingHelper<?>> callThreadLocalGetSiblingHelpers() {
				return OAObjectParentService.this.srvcThreadLocal.getSiblingHelpers();
			}

			@Override
			/**
			 * Performs callThreadLocalGetAndIncrementGetSiblingCalledCount behavior for the
			 * OA object service.
			 *
			 * @return result value
			 */
			public int callThreadLocalGetAndIncrementGetSiblingCalledCount() {
				return OAObjectParentService.this.srvcThreadLocal.getAndIncrementGetSiblingCalledCount();
			}

			@Override
			/**
			 * Performs callThreadLocalClearGetSiblingCalledCount behavior for the OA object
			 * service.
			 */
			public void callThreadLocalClearGetSiblingCalledCount() {
				OAObjectParentService.this.srvcThreadLocal.clearGetSiblingCalledCount();
			}

			@Override
			/**
			 * Performs callPropertyGetProperty behavior for the OA object service.
			 *
			 * @param oaObj           method input
			 * @param name            method input
			 * @param bReturnNotExist method input
			 * @param bConvertWeakRef method input
			 * @return result value
			 */
			public Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
			}

			@Override
			/**
			 * Performs callInfoGetLinkInfo behavior for the OA object service.
			 *
			 * @param fromClass method input
			 * @param toClass   method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetLinkInfo(Class fromClass, Class toClass) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(fromClass, toClass);
			}

			@Override
			/**
			 * Performs callInfoGetLinkInfo behavior for the OA object service.
			 *
			 * @param clazz        method input
			 * @param propertyName method input
			 * @return result value
			 */
			public OALinkInfo callInfoGetLinkInfo(Class clazz, String propertyName) {
				return OAObjectParentService.this.getOAObjectInfoService().getLinkInfo(clazz, propertyName);
			}

			@Override
			/**
			 * Performs callHubGetHubReferences behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public Hub[] callHubGetHubReferences(OAObject oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}

			@Override
			/**
			 * Performs callHubDetailGetLinkInfoFromMasterToDetail behavior for the OA
			 * object service.
			 *
			 * @param thisDetailHub method input
			 * @return result value
			 */
			public OALinkInfo callHubDetailGetLinkInfoFromMasterToDetail(Hub<?> thisDetailHub) {
				return OAObjectParentService.this.srvcHubParent.getHubDetailService().getLinkInfoFromMasterToDetail(thisDetailHub);
			}

			@Override
			/**
			 * Performs callHubDetailGetLinkInfoFromMasterHubToDetail behavior for the OA
			 * object service.
			 *
			 * @param thisDetailHub method input
			 * @return result value
			 */
			public OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<?> thisDetailHub) {
				return OAObjectParentService.this.srvcHubParent.getHubDetailService().getLinkInfoFromMasterToDetail(thisDetailHub);
			}

			@Override
			/**
			 * Performs callHubDetailGetLinkInfoFromDetailToMaster behavior for the OA
			 * object service.
			 *
			 * @param hub method input
			 * @return result value
			 */
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return OAObjectParentService.this.srvcHubParent.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}

			@Override
			/**
			 * Performs callCacheGet behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @param ok    method input
			 * @return result value
			 */
			public <T extends OAObject> T callCacheGetUsingKey(Class<T> clazz, OAObjectKey ok) {
				return OAObjectParentService.this.getOAObjectCacheService().getUsingKey(clazz, ok);
			}
		};
		return srvcOAObjectSibling;
	}

	/**
	 * Returns the oAObjectStateService value.
	 *
	 * @return result value
	 */
	public OAObjectStateService getOAObjectStateService() {
		if (srvcOAObjectState != null)
			return srvcOAObjectState;
		srvcOAObjectState = new OAObjectStateService() {

			@Override
			/**
			 * Performs callEventFirePropertyChange behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param oldObj       method input
			 * @param newObj       method input
			 * @param bLocalOnly   method input
			 * @param bSetChanged  method input
			 */
			public void callEventFirePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().firePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}

			@Override
			/**
			 * Performs callEventFireBeforePropertyChange behavior for the OA object
			 * service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @param oldObj       method input
			 * @param newObj       method input
			 * @param bLocalOnly   method input
			 * @param bSetChanged  method input
			 */
			public void callEventFireBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged) {
				OAObjectParentService.this.getOAObjectEventService().fireBeforePropertyChange(oaObj, propertyName, oldObj, newObj, bLocalOnly, bSetChanged);
			}

			@Override
			/**
			 * Performs callAutoAddSetAutoAdd behavior for the OA object service.
			 *
			 * @param oaObj    method input
			 * @param bEnabled method input
			 */
			public void callAutoAddSetAutoAdd(OAObject oaObj, boolean bEnabled) {
				OAObjectParentService.this.getOAObjectAutoAddService().setAutoAdd(oaObj, bEnabled);
			}
		};
		return srvcOAObjectState;
	}

	/**
	 * Returns the oAObjectUniqueService value.
	 *
	 * @return result value
	 */
	public OAObjectUniqueService getOAObjectUniqueService() {
		if (srvcOAObjectUnique != null)
			return srvcOAObjectUnique;
		srvcOAObjectUnique = new OAObjectUniqueService() {
			@Override
			/**
			 * Performs callThreadLocalSetLoading behavior for the OA object service.
			 *
			 * @param b method input
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callThreadLocalSetLoading(boolean b) {
				return OAObjectParentService.this.srvcThreadLocal.setLoading(b);
			}

			@Override
			/**
			 * Performs callSyncClientGetUnique behavior for the OA object service.
			 *
			 * @param clazz        method input
			 * @param propertyName method input
			 * @param uniqueKey    method input
			 * @param bAutoCreate  method input
			 * @return result value
			 */
			public OAObject callSyncClientGetUnique(Class<? extends OAObject> clazz, String propertyName, Object uniqueKey, boolean bAutoCreate) {
				OASyncClient sc = OAObjectParentService.this.srvcSync.getClient();
				RemoteServerInterface rsi;
				try {
					rsi = sc.getRemoteServer();
				} catch (Exception e) {
					throw new RuntimeException("Could not get remote server ", e);
				}
				return rsi.getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
			}

			@Override
			/**
			 * Performs callReflectCreateNewObject behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public Object callReflectCreateNewObject(Class clazz) {
				return OAObjectParentService.this.getOAObjectReflectService().createNewObject(clazz);
			}

			@Override
			/**
			 * Performs callCacheFind behavior for the OA object service.
			 *
			 * @param clazz      method input
			 * @param path       method input
			 * @param findObject method input
			 * @return result value
			 */
			public Object callCacheFind(Class clazz, String path, Object findObject) {
				return OAObjectParentService.this.getOAObjectCacheService().find(clazz, path, findObject);
			}

			@Override
			/**
			 * Performs callCSIsClient behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}
		};
		return srvcOAObjectUnique;
	}

	/**
	 * Returns the oAObjectRulesService value.
	 *
	 * @return result value
	 */
	public OAObjectRulesService getOAObjectRulesService() {
		if (srvcOAObjectRules != null)
			return srvcOAObjectRules;
		srvcOAObjectRules = new OAObjectRulesService() {
			@Override
			/**
			 * Performs callInfoGetObjectInfo behavior for the OA object service.
			 *
			 * @param clazz method input
			 * @return result value
			 */
			public OAObjectInfo callInfoGetObjectInfo(Class<?> clazz) {
				return OAObjectParentService.this.getOAObjectInfoService().getOAObjectInfo(clazz);
			}

			@Override
			/**
			 * Performs callPropertyGetProperty behavior for the OA object service.
			 *
			 * @param oaObj        method input
			 * @param propertyName method input
			 * @return result value
			 */
			public Object callPropertyGetProperty(OAObject oaObj, String propertyName) {
				return OAObjectParentService.this.getOAObjectPropertyService().getProperty(oaObj, propertyName);
			}

			@Override
			/**
			 * Performs callReflectGetProperty behavior for the OA object service.
			 *
			 * @param oaObj    method input
			 * @param propPath method input
			 * @return result value
			 */
			public Object callReflectGetProperty(OAObject oaObj, String propPath) {
				return OAObjectParentService.this.getOAObjectReflectService().getProperty(oaObj, propPath);
			}

			@Override
			/**
			 * Performs callHubGetHubReferences behavior for the OA object service.
			 *
			 * @param oaObj method input
			 * @return result value
			 */
			public <T extends OAObject> Hub<T>[] callHubGetHubReferences(T oaObj) {
				return OAObjectParentService.this.getOAObjectHubService().getHubReferences(oaObj);
			}

			@Override
			/**
			 * Performs callInfoGetMethod behavior for the OA object service.
			 *
			 * @param oi         method input
			 * @param methodName method input
			 * @param classParam method input
			 * @return result value
			 */
			public Method callInfoGetMethod(OAObjectInfo oi, String methodName, Class<?> classParam) {
				return OAObjectParentService.this.getOAObjectInfoService().getMethod(oi, methodName, classParam);
			}

			@Override
			/**
			 * Performs callInfoGetMethod behavior for the OA object service.
			 *
			 * @param oi            method input
			 * @param methodName    method input
			 * @param argumentCount method input
			 * @return result value
			 */
			public Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount) {
				return OAObjectParentService.this.getOAObjectInfoService().getMethod(oi, methodName, argumentCount);
			}

			@Override
			/**
			 * Performs callHubDetailGetPropertyFromMasterToDetail behavior for the OA
			 * object service.
			 *
			 * @param hub method input
			 * @return result value
			 */
			public String callHubDetailGetPropertyFromMasterToDetail(Hub<?> hub) {
				return OAObjectParentService.this.getHubParentService().getHubDetailService().getPropertyFromMasterToDetail(hub);
			}

			@Override
			/**
			 * Performs callHubDetailGetLinkInfoFromDetailToMaster behavior for the OA
			 * object service.
			 *
			 * @param hub method input
			 * @return result value
			 */
			public OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub) {
				return OAObjectParentService.this.getHubParentService().getHubDetailService().getLinkInfoFromDetailToMaster(hub);
			}

			@Override
			/**
			 * Performs callHubDetailGetLinkInfoFromMasterHubToDetail behavior for the OA
			 * object service.
			 *
			 * @param hub method input
			 * @return result value
			 */
			public OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<?> hub) {
				return OAObjectParentService.this.getHubParentService().getHubDetailService().getLinkInfoFromMasterToDetail(hub);
			}

			@Override
			/**
			 * Performs callHubEventGetAllListeners behavior for the OA object service.
			 *
			 * @param hub method input
			 * @return result value
			 */
			public <T extends OAObject> HubListener<T>[] callHubEventGetAllListeners(Hub<T> hub) {
				return OAObjectParentService.this.getHubParentService().getHubEventService().getAllListeners(hub);
			}

			@Override
			/**
			 * Performs callSyncIsClient behavior for the OA object service.
			 *
			 * @return {@code true} when the operation succeeds or condition is met
			 */
			public boolean callSyncIsClient() {
				return OAObjectParentService.this.srvcSync.isClient();
			}
		};
		return srvcOAObjectRules;
	}

	// flag so that OAObject.finalize should ignore this object.
	// qqqqqqqqqqqq make sure other code looks for guid=0, and ignore default
	// cleanup (cached, etc)
	/**
	 * Performs dontFinalize behavior for the OA object service.
	 *
	 * @param obj method input
	 */
	public void dontFinalize(OAObject obj) {
		if (obj != null) {
			getOAObjectGuidService().setGuid(obj, null);
		}
	}

}
