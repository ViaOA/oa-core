package com.viaoa.graph.service.object;

import java.util.UUID;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.context.OAContext;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.util.OAString;

public abstract class OAObjectInitializeService {
	private static final Logger LOG = Logger.getLogger(OAObjectInitializeService.class.getName());

	private final OAObject.FriendAccess faObject;

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

		OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());

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
		OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());

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
	        OAObject oaObj,
	        OAObjectInfo oi,
	        boolean bInitializeNulls,
	        boolean bInitializeWithDS,
	        boolean bAddToCache,
	        boolean bInitializeWithCS,
	        boolean bSetChangedToFalse) {
  
		final boolean bWasLoading = callThreadLocalSetLoading(true);
		try {
			if (oi == null) {
				oi = getOAObjectInfo(oaObj.getClass());
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
					if (li.getType() == li.TYPE_ONE && OAString.isNotEmpty(li.getDefaultContextPropertyPath())) {
						OAObject objx = OAContext.getContextObject();
						if (objx != null) {
							if (!li.getDefaultContextPropertyPath().equals(".")) {
								OAFinder hf = new OAFinder(li.getDefaultContextPropertyPath());
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
						callThreadLocalSetLoading(true);
					}
				}
			}
			if (bSetChangedToFalse) {
				oaObj.setChanged(false);
			}
		} finally {
			// note: this has to be false, not bWasLoading, since it also increments a counter in threadLocalDelegate
			callThreadLocalSetLoading(false);
		}
		if (!bWasLoading) {
			callCacheFireAfterLoadEvent(oaObj);
		}
		if (bAddToCache) { // needs to run after setLoadingObject(false), so that add event is handled correctly.
			callCacheAddToSelectAllHubs(oaObj);
		}
	}

	
	/**
	 * Convenience method that reinitializes the specified {@link OAObject} so it
	 * behaves as a newly created instance. This method simply allocates a new GUID
	 * and delegates to {@link #setAsNewObject(OAObject, long)}.
	 *
	 * @param oaObj the object to reinitialize; may be {@code null}.
	 */
	public void setAsNewObject(final OAObject oaObj) {
		if (oaObj == null) return;
		callGuidAssignNewGuid(oaObj);

		UUID guid = callGuidGetGuid(oaObj);
		setAsNewObject(oaObj, guid);
	}
	
	
	/**
	 * Reinitializes the specified {@link OAObject} so it behaves as a newly created
	 * instance. This resets identity, lifecycle flags, and primary-key fields while
	 * ensuring property-change and link events are suppressed during the transition.
	 *
	 * <p>Actions include:</p>
	 * <ul>
	 *   <li>Assigning the provided GUID.</li>
	 *   <li>Setting <code>newFlag</code> to {@code true}.</li>
	 *   <li>Clearing ID (primary-key) properties defined by the object's metadata.</li>
	 *   <li>Suppressing events while clearing ID values to avoid notification
	 *       during reinitialization.</li>
	 *   <li>Rebuilding the object's {@link OAObjectKey}.</li>
	 * </ul>
	 *
	 * @param oaObj the object to reset; may be {@code null}.
	 * @param guid  the GUID to assign.
	 */
	public void setAsNewObject(final OAObject oaObj, UUID guid) {
		if (oaObj == null) {
			return;
		}
		faObject.setNew(oaObj, true);
		faObject.setGuid(oaObj, guid); //qqqqqqq not a good idea (hashcode) ... will also need to update cache (key is guid

		OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());
		String[] ids = oi.getIdProperties();
		if (ids == null) {
			return;
		}

		callThreadLocalSetLoading(true);
		try {
			for (String id : ids) {
				callReflectSetProperty(oaObj, id, null, null);
			}
		} finally {
			callThreadLocalSetLoading(false);
		}
		if (callDSGetAssignIdOnCreate(oaObj)) {
			callDSAssignId(oaObj);
		}
		oaObj.getObjectKey();
	}

	/**
	 * Reassigns the GUID of the specified {@link OAObject} to match the GUID
	 * contained in the provided {@link OAObjectKey}. This is used when an object
	 * has been reloaded or reconstructed and must retain its original identity
	 * within the object graph.
	 *
	 * <p>If the object already has a GUID equal to the GUID in {@code origKey},
	 * the method returns immediately with no changes.</p>
	 *
	 * <p>If reassignment is necessary, the new GUID is extracted from the key and
	 * assigned using {@link #setObjectGuid(OAObject, long)}. This preserves the
	 * object's identity for cache consistency, link resolution, and distributed
	 * sync reconciliation.</p>
	 *
	 * @param obj the object whose GUID is being restored; may be {@code null}.
	 * @param origKey the key containing the original GUID to apply; must not be {@code null}.
	 */
	public void reassignGuid(OAObject oaObj, OAObjectKey origKey) {
		//qqqqqqqqqqqqqqq this is not be a good idea ... objectCache would need to be updated		
		if (oaObj != null && origKey != null) {
			faObject.setGuid(oaObj, origKey.getGuid()); // needs to re-cache
		}
	}

	@OAParentProvided (example = "srvcObject.getOAObjectCacheService().add")
	public abstract OAObject callCacheAdd(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll);
	
	@OAParentProvided (example = "srvcObject.getOAObjectCacheService().fireAfterLoadEvent")
	public abstract <T extends OAObject> void callCacheFireAfterLoadEvent(T obj);

	@OAParentProvided (example = "srvcObject.getOAObjectCacheService().addToSelectAllHubs")
	public abstract void callCacheAddToSelectAllHubs(OAObject obj);

	@OAParentProvided (example = "srvcObject.getOAObjectDSService().assignId")
	public abstract void callDSAssignId(OAObject oaObj);

	@OAParentProvided (example = "srvcObject.getOAObjectDSService().getAssignIdOnCreate")
	public abstract boolean callDSGetAssignIdOnCreate(OAObject oaObj);
	
	@OAParentProvided (example = "srvcObject.getOAObjectGuidService().getGuid")
	public abstract UUID callGuidGetGuid(OAObject oaObj);

	@OAParentProvided (example = "srvcObject.getOAObjectGuidService().assignNewGuid")
	public abstract void callGuidAssignNewGuid(OAObject obj);

	@OAParentProvided (example = "srvcObject.getOAObjectGuidService().assignGuid")
	public abstract void callGuidAssignGuid(OAObject obj);
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().isOne2One")
	public abstract boolean callInfoIsOne2One(OALinkInfo thisLi);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo(clazz)")
	public abstract OAObjectInfo getOAObjectInfo(Class clazz); 

	@OAParentProvided (example = "srvcObject.getOAObjectPropertyService().unsafeAddProperty")
	public abstract void callPropertyUnsafeAddProperty(OAObject oaObj, String name, Object value); 
	
	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().setProperty")
	public abstract void callReflectSetProperty(final OAObject oaObj, String propName, Object value, final String fmt);
	
	
	@OAParentProvided (example = "srvcSync.isClient")
	public abstract boolean callSyncIsClient();

	@OAParentProvided (example = "srvcSync.getSyncClient().objectCreated")
	public abstract void callSyncClientObjectCreated(OAObject obj);	

	
	@OAParentProvided (example = "srvcOAThreadLocal.isLoading()")
	public abstract boolean callThreadLocalIsLoading();

	@OAParentProvided (example = "srvcOAThreadLocal.setLoading(..)")
	public abstract boolean callThreadLocalSetLoading(boolean b);
}

