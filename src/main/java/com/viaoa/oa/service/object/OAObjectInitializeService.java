package com.viaoa.oa.service.object;

import java.util.UUID;
import java.util.logging.Logger;

import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;
import com.viaoa.session.OASessionUser;


/*qqqqqqqqqqqqqq
CODEX

#2
  File/Class/Method: src/main/java/com/viaoa/oa/service/object/OAObjectInitializeService.java, initialize(...)

  Exact execution path: with bAddToCache=true, initialize(...) assigns GUID/default links and calls
  callCacheAdd(...), then later runs client-sync initialization and datasource ID assignment. If
  callSyncClientObjectCreated(...) or callDSAssignId(...) throws, the object has already been inserted into cache
  with partial initialization.

  Why it is a correctness bug: failed initialization can leave a runtime-visible cached object that lacks
  authoritative CS/DS initialization, making identity lookup and later retries ambiguous.

  Semantic/invariant violated: cache publication must happen after authoritative initialization succeeds, or must be
  rolled back on failure.

  Minimal fix: move cache add until after CS/DS success, or remove the object from cache in the failure path.

  Suggested test: datasource assigns IDs on create and throws from assignId; call initialize with bAddToCache=true;
  assert object is not findable from cache afterward.


*/



public abstract class OAObjectInitializeService {
	private static final Logger LOG = Logger.getLogger(OAObjectInitializeService.class.getName());

	private final OAObject.FriendAccess faObject;

	/**
	 * Performs OAObjectInitializeService behavior for the OA object service.
	 *
	 * @param oaObjectFriendAccess method input
	 */
	public OAObjectInitializeService(OAObject.FriendAccess oaObjectFriendAccess) {
    	if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
    	this.faObject = oaObjectFriendAccess;
	}
    
	/**
	 * Initializes the specified {@link OAObject} by assigning a GUID, allocating its
	 * primitive null-mask array, and invoking the full initialization pipeline when
	 * not running under a loading context.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>If {@code oaObj} is {@code null}, returns {@code false}.</li>
	 *   <li>Ensures the object has a GUID using {@link #assignGuid(OAObject)}.</li>
	 *   <li>Retrieves the object's {@link OAObjectInfo} and allocates the
	 *       {@code nulls} array based on its primitive properties.</li>
	 *   <li>If the thread-local loading flag is set, initialization is deferred and
	 *       the method returns {@code false}.</li>
	 *   <li>Otherwise, computes whether client-sync initialization is required and
	 *       calls the multi-argument {@code initialize(...)} method to perform full
	 *       setup.</li>
	 * </ul>
	 *
	 * <p>This method performs only basic pre-initialization and does not reset
	 * lifecycle flags, clear ID properties, or configure links; those actions occur
	 * in the full initializer.</p>
	 *
	 * @param oaObj the object to initialize; may be {@code null}.
	 * @return {@code true} if full initialization was performed; {@code false} if
	 *         initialization was skipped.
	 */
	public boolean initialize(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		//20260108 was:  srvcObject.getOAObjectGuidService().assignGuid(oaObj);

		OAObjectInfo oi = callInfoGetObjectInfo(oaObj.getClass());

		String[] ps = oi.getPrimitiveProperties();
		int x = (ps == null) ? 0 : ((int) Math.ceil(ps.length / 8.0d));
		
		faObject.setNulls(oaObj, new byte[x]);

		if (callThreadLocalIsLoading()) {
			return false; // dont initialize. Whatever is loading should call initialize below directly
		}

		
		boolean bInitializeWithCS = !oi.getLocalOnly() && callSyncIsClient();
		

		// useDataSource needs to be true ... since other DS (ex: autonumber) might be used
		initialize(oaObj, oi, oi.getInitializeNewObjects(), true, oi.getAddToCache(), bInitializeWithCS, true);

		return true;
	}

	
	/**
	 * Convenience method that performs after-load initialization using default
	 * settings. This method delegates to
	 * {@link #initializeAfterLoading(OAObject, boolean, boolean, boolean)} with
	 * all flags set to {@code false}.
	 *
	 * @param oaObj the object to initialize; may be {@code null}.
	 */
	public void initializeAfterLoading(OAObject oaObj) {
		initializeAfterLoading(oaObj, false, false, false);
	}
	
	/**
	 * Performs after-load initialization for the specified {@link OAObject}. This
	 * method finalizes the object's state after it has been populated, preparing it
	 * for normal runtime usage.
	 *
	 * <p>Behavior includes:</p>
	 * <ul>
	 *   <li>Obtaining {@link OAObjectInfo} for the object.</li>
	 *   <li>Determining whether client-sync initialization is required.</li>
	 *   <li>Delegating to the full initialization pipeline via the multi-argument
	 *       {@code initialize(...)} method using the supplied flags.</li>
	 * </ul>
	 *
	 * <p>This method does not perform the initial GUID assignment or metadata setup;
	 * those actions occur during creation or in the primary initializer.</p>
	 *
	 * @param oaObj the object being finalized; may be {@code null}.
	 * @param bAssignNewId whether the full initializer should request DataSource ID assignment.
	 * @param bInitializeNulls whether primitive null-mask bytes should be reset.
	 * @param bSetChangedToFalse whether the object's changed flag should be cleared.
	 */
	public void initializeAfterLoading(OAObject oaObj, boolean bAssignNewId, boolean bInitializeNulls, boolean bSetChangedToFalse) {
		if (oaObj == null) {
			return;
		}
		OAObjectInfo oi = callInfoGetObjectInfo(oaObj.getClass());

		boolean bInitializeWithCS = !oi.getLocalOnly() && callSyncIsClient();

		initialize(oaObj, oi, bInitializeNulls, bAssignNewId, oi.getAddToCache(), bInitializeWithCS, bSetChangedToFalse);
	}

	
	/**
	 * Executes the full internal initialization pipeline for a newly constructed or
	 * freshly loaded {@link OAObject}. This method configures null-mask bytes,
	 * default link values, cache participation, client-sync initialization, optional
	 * DataSource ID assignment, and the object's changed state.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>Temporarily sets the thread-local loading flag and restores it on exit.</li>
	 *   <li>If {@code oi} is {@code null}, resolves metadata for the object.</li>
	 *   <li>If {@code bInitializeNulls} is true, updates primitive null-mask bytes.</li>
	 *   <li>When not already loading, initializes default link-one values and assigns
	 *       {@code null} to other link references as appropriate.</li>
	 *   <li>If {@code bAddToCache} is true, adds the object to the cache.</li>
	 *   <li>If {@code bInitializeWithCS} is true, performs client-sync initialization.</li>
	 *   <li>If {@code bInitializeWithDS} is true and the DataSource assigns IDs on
	 *       create, temporarily clears the loading flag and invokes ID assignment.</li>
	 *   <li>If {@code bSetChangedToFalse} is true, clears the object's changed flag.</li>
	 * </ul>
	 *
	 * <p>After the loading flag is cleared, if not previously loading, the method
	 * fires the “after load” cache event. If {@code bAddToCache} is true, the object
	 * is then added to all SelectAll hubs.</p>
	 *
	 * @param oaObj the object being initialized.
	 * @param oi optional metadata; if {@code null}, metadata is looked up.
	 * @param bInitializeNulls whether primitive null-mask bytes should be reset.
	 * @param bInitializeWithDS whether DataSource initialization should run.
	 * @param bAddToCache whether the object should be inserted into the cache.
	 * @param bInitializeWithCS whether client-sync initialization should run.
	 * @param bSetChangedToFalse whether the object's changed flag should be cleared.
	 */
	public void initialize(
	        final OAObject oaObj,
	        OAObjectInfo oi,
	        final boolean bInitializeNulls,
	        final boolean bInitializeWithDS,
	        final boolean bAddToCache,
	        final boolean bInitializeWithCS,
	        final boolean bSetChangedToFalse) {
  
		final boolean bWasLoading = callThreadLocalSetLoading(true);
		try {
			if (oi == null) {
				oi = callInfoGetObjectInfo(oaObj.getClass());
			}

			// 20260108
			if (callGuidGetGuid(oaObj) == null) {
				callGuidAssignGuid(oaObj);
			}
			
			if (bInitializeNulls) {
				byte[] bs = faObject.getNulls(oaObj);
				for (int i = 0; i < bs.length; i++) {
					bs[i] = (byte) ~bs[i];
				}
			}

			if (!bWasLoading) {
				for (OALinkInfo li : oi.getLinkInfos()) {
					if (li.getCalculated()) {
						continue;
					}
					if (li.getPrivateMethod()) {
						continue;
					}
					if (!li.getUsed()) {
						continue;
					}
					if (li.getMatchProperty() != null) {
						// dont set to null, so that it will have to call oaObject.getHub(), which will then create hubAutoMatch
						continue;
					}
					// 20140409 added check for 1to1, in which case one side will not have an
					//    fkey, since it uses it's own pkey as the fkey

					// 20190205 set default linkOne
					if (li.getType() == li.TYPE_ONE && OAString.isNotEmpty(li.getDefaultModelUserPath())) {
						OA oa = OARuntime.oa(oaObj);
						
						Hub<?> hub = oa.modelUser().getCalc();
						OAObject objx = hub == null ? null : hub.getAO();
						if (objx != null) {
							if (!li.getDefaultModelUserPath().equals(".")) {
								OAFinder hf = new OAFinder(li.getDefaultModelUserPath());
								objx = hf.findFirst(objx);
							}
							callPropertyUnsafeAddProperty(oaObj, li.getName(), objx);
						}
					} else {
						if (!callInfoIsOne2One(li)) {
							callPropertyUnsafeAddProperty(oaObj, li.getName(), null);
						}
					}
				}
			}

			if (bAddToCache) { // needs to run before any property could be set, so that OACS changes will find this new object.
				callCacheAdd(oaObj, false, false); //  was true,true:  dont add to selectAllHub until after loadingObject is false
			}

			if (bInitializeWithCS) {
				// must be before DS init, since it could add to local client cache
				callSyncClientObjectCreated(oaObj);
			}
			if (!bWasLoading && bInitializeWithDS) {
				if (callDSGetAssignIdOnCreate(oaObj)) {
					callThreadLocalSetLoading(false); 
					try {
						callDSAssignId(oaObj);
					} finally {
						callThreadLocalSetLoading(true); // outer code has it set to true
					}
				}
			}
			if (bSetChangedToFalse) {
				oaObj.setChanged(false);
			}
		} finally {
			// note: this has to be false, not bWasLoading, since it also increments a counter in threadLocalDelegate
			callThreadLocalSetLoading(bWasLoading);
		}
		if (!bWasLoading) {
			callCacheFireAfterLoadEvent(oaObj);
		}
		if (bAddToCache) { // needs to run after setLoadingObject(false), so that add event is handled correctly.
			callCacheAddToSelectAllHubs(oaObj);
		}
	}

	
	public abstract OAObject callCacheAdd(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll);
	/**
	 * Dependency hook used by this service to cacheFireAfterLoadEvent.
	 *
	 * @param obj method input
	 */
	public abstract <T extends OAObject> void callCacheFireAfterLoadEvent(T obj);
	/**
	 * Dependency hook used by this service to cacheAddToSelectAllHubs.
	 *
	 * @param obj method input
	 */
	public abstract void callCacheAddToSelectAllHubs(OAObject obj);
	/**
	 * Dependency hook used by this service to dSAssignId.
	 *
	 * @param oaObj method input
	 */
	public abstract void callDSAssignId(OAObject oaObj);
	/**
	 * Dependency hook used by this service to dSGetAssignIdOnCreate.
	 *
	 * @param oaObj method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callDSGetAssignIdOnCreate(OAObject oaObj);
	/**
	 * Dependency hook used by this service to guidGetGuid.
	 *
	 * @param oaObj method input
	 * @return result value
	 */
	public abstract UUID callGuidGetGuid(OAObject oaObj);
	/**
	 * Dependency hook used by this service to guidAssignGuid.
	 *
	 * @param obj method input
	 */
	public abstract void callGuidAssignGuid(OAObject obj);
	/**
	 * Dependency hook used by this service to infoIsOne2One.
	 *
	 * @param thisLi method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callInfoIsOne2One(OALinkInfo thisLi);
	/**
	 * Dependency hook used by this service to infoGetObjectInfo.
	 *
	 * @param clazz method input
	 * @return result value
	 */
	public abstract OAObjectInfo callInfoGetObjectInfo(Class<?> clazz);
	/**
	 * Dependency hook used by this service to propertyUnsafeAddProperty.
	 *
	 * @param oaObj method input
	 * @param name method input
	 * @param value method input
	 */
	public abstract void callPropertyUnsafeAddProperty(OAObject oaObj, String name, Object value);
	/**
	 * Dependency hook used by this service to reflectSetProperty.
	 *
	 * @param oaObj method input
	 * @param propName method input
	 * @param value method input
	 * @param fmt method input
	 */
	public abstract void callReflectSetProperty(final OAObject oaObj, String propName, Object value, final String fmt);
	/**
	 * Dependency hook used by this service to syncIsClient.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callSyncIsClient();
	/**
	 * Dependency hook used by this service to syncClientObjectCreated.
	 *
	 * @param obj method input
	 */
	public abstract void callSyncClientObjectCreated(OAObject obj);
	/**
	 * Dependency hook used by this service to threadLocalIsLoading.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callThreadLocalIsLoading();
	/**
	 * Dependency hook used by this service to threadLocalSetLoading.
	 *
	 * @param b method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callThreadLocalSetLoading(boolean b);
}

