/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.object;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.viaoa.context.OAContext;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDelegate;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAString;

/**
 * Provides core operational utilities for {@link OAObject} that are not specific
 * to property handling, link management, metadata, ClientSync, or DataSource
 * operations. All methods in this class are static and stateless, and serve as
 * common execution points for identity management, change-state evaluation,
 * recursive graph traversal, and several object-lifecycle support functions.
 *
 * <p>This delegate works in conjunction with the other OA *Delegate classes:</p>
 * <ul>
 *   <li>{@link OAObjectPropertyDelegate} – manages property access and
 *       change events</li>
 *   <li>{@link OAObjectLinkDelegate} – manages link-one and link-many
 *       relationships</li>
 *   <li>{@link OAObjectInfoDelegate} – resolves metadata, ID properties,
 *       and type information</li>
 *   <li>{@link OAObjectDSDelegate} – integrates with the DataSource layer</li>
 *   <li>{@link OAObjectCSDelegate} – integrates with the ClientSync layer</li>
 * </ul>
 *
 * <p>The responsibilities implemented here include:</p>
 * <ul>
 *   <li><b>GUID and identity lifecycle</b>  
 *       Assigning and updating GUID values, reserving GUID ranges,
 *       and reinitializing objects as “new.”</li>
 *
 *   <li><b>Change-state evaluation</b>  
 *       Determining whether an object or any related objects are marked
 *       changed, including cascade-aware recursive evaluation.</li>
 *
 *   <li><b>Object graph traversal</b>  
 *       Recursively visiting reachable objects and invoking caller-supplied
 *       callbacks, with loop prevention via {@link OACascade}.</li>
 *
 *   <li><b>Property-path search utilities</b>  
 *       Locating objects or values using dot-notation property paths
 *       beginning from a root object.</li>
 *
 *   <li><b>Auto-add lifecycle</b>  
 *       Enabling or disabling automatic participation in reverse-link hubs
 *       when new link relationships are established.</li>
 *
 *   <li><b>Initialization helpers</b>  
 *       Supporting basic setup for OAObject instances immediately after 
 *       load or creation.</li>
 * </ul>
 *
 * <p>None of the methods in this class modify link relationships, raise
 * property-change events, or access DataSource records directly. Those behaviors
 * reside in the corresponding delegate classes. Instead, this class centralizes
 * general-purpose functionality used across the OAObject lifecycle.</p>
 */

public class OAObjectDelegate {

	private static Logger LOG = Logger.getLogger(OAObjectDelegate.class.getName());

	public static final String WORD_New = "NEW";
	public static final String WORD_Changed = "CHANGED";
	public static final String WORD_Deleted = "DELETED";
	public static final String WORD_AutoAdd = "AutoAdd";

	public static final Boolean TRUE = Boolean.TRUE;
	public static final Boolean FALSE = Boolean.FALSE;

	/** Static global lock used when setting global properties (ex: guidCounter) */

	/** global counter used for local objects. Value is positive */
	static protected final AtomicLong guidCounter = new AtomicLong(); // unique identifier needed for objects past from client/server

	/** global counter used for local objects. Value is negative */
	static protected final AtomicLong localGuidCounter = new AtomicLong();

	/** Flag to know if finalized objects should be automatically saved. Default is false. */
	protected static boolean bFinalizeSave = false;

	/** tracks which OAObjects should not automatically add themself to a detailHub when an oaObj property is set. */
	private static final ConcurrentHashMap<Long, Long> hmAutoAdd = new ConcurrentHashMap<Long, Long>();

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
	protected static boolean initialize(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		assignGuid(oaObj); // must get a guid before calling setInConstructor, so that it will have a valid hash key

		/**
		 * set OAObject.nulls to know if a primitive property is null or not. All "bits" are flagged/set to 1. Ordering and positions are
		 * set by the position of uppercase/sorted property in array. See: OAObjectInfoDelegate.initialize()
		 */
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);

		String[] ps = oi.getPrimitiveProperties();
		int x = (ps == null) ? 0 : ((int) Math.ceil(ps.length / 8.0d));
		oaObj.nulls = new byte[x];

		if (OAThreadLocalDelegate.isLoading()) {
			return false; // dont initialize. Whatever is loading should call initialize below directly
		}

		boolean bInitializeWithCS = !oi.getLocalOnly() && OASync.isClient(oaObj.getClass());

		// 20200910 useDataSource needs to be true ... since other DS (ex: autonumber) might be used
		initialize(oaObj, oi, oi.getInitializeNewObjects(), true, oi.getAddToCache(), bInitializeWithCS, true);
		//was: initialize(oaObj, oi, oi.getInitializeNewObjects(), oi.getUseDataSource(), oi.getAddToCache(), bInitializeWithCS, true);
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
	public static void initializeAfterLoading(OAObject oaObj) {
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
	public static void initializeAfterLoading(OAObject oaObj, boolean bAssignNewId, boolean bInitializeNulls, boolean bSetChangedToFalse) {
		if (oaObj == null) {
			return;
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);

		boolean bInitializeWithCS = !oi.getLocalOnly() && OASync.isClient(oaObj.getClass());

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
	protected static void initialize(
	        OAObject oaObj,
	        OAObjectInfo oi,
	        boolean bInitializeNulls,
	        boolean bInitializeWithDS,
	        boolean bAddToCache,
	        boolean bInitializeWithCS,
	        boolean bSetChangedToFalse) {
		final boolean bWasLoading = OAThreadLocalDelegate.setLoading(true);
		try {
			if (oi == null) {
				oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
			}

			if (bInitializeNulls) {
				/* 20180325 20180403 removed,not used
				byte[] bsMask = oi.getPrimitiveMask();
				for (int i=0; i<oaObj.nulls.length; i++) {
				    oaObj.nulls[i] |= (byte) bsMask[i];
				}
				*/
				// put this back
				for (int i = 0; i < oaObj.nulls.length; i++) {
					oaObj.nulls[i] = (byte) ~oaObj.nulls[i];
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
							OAObjectPropertyDelegate.unsafeAddProperty(oaObj, li.getName(), objx);
						}
					} else {
						if (!OAObjectInfoDelegate.isOne2One(li)) {
							OAObjectPropertyDelegate.unsafeAddProperty(oaObj, li.getName(), null);
						}
					}
				}
			}

			if (bAddToCache) { // needs to run before any property could be set, so that OACS changes will find this new object.
				OAObjectCacheDelegate.add(oaObj, false, false); // 20090525, was true,true:  dont add to selectAllHub until after loadingObject is false
			}

			if (bInitializeWithCS) {
				// must be before DS init, since it could add to local client cache
				OAObjectCSDelegate.objectCreated(oaObj);
			}
			if (!bWasLoading && bInitializeWithDS) {
				if (OAObjectDSDelegate.getAssignIdOnCreate(oaObj)) {
					OAThreadLocalDelegate.setLoading(false);
					try {
						OAObjectDSDelegate.assignId(oaObj);
					} finally {
						OAThreadLocalDelegate.setLoading(true);
					}
				}
			}
			if (bSetChangedToFalse) {
				oaObj.setChanged(false);
			}
			/*
			OAObjectKey key = OAObjectKeyDelegate.getKey(oaObj);
			String s = String.format("New, class=%s, id=%s",
			        OAString.getClassName(oaObj.getClass()),
			        key.toString()
			);
			if (oi.bUseDataSource) {
			    OAObject.OALOG.fine(s);
			}
			*/
		} finally {
			// note: this has to be false, not bWasLoading, since it also increments a counter in threadLocalDelegate
			OAThreadLocalDelegate.setLoading(false);
		}
		if (!bWasLoading) {
			OAObjectCacheDelegate.fireAfterLoadEvent(oaObj);
		}
		if (bAddToCache) { // 20090525 needs to run after setLoadingObject(false), so that add event is handled correctly.
			OAObjectCacheDelegate.addToSelectAllHubs(oaObj);
		}
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
	public static void setNew(final OAObject oaObj, final boolean b) {
		if (b == oaObj.newFlag) {
			return;
		}
		boolean old = oaObj.newFlag;
		OAObjectEventDelegate.fireBeforePropertyChange(oaObj, WORD_New, old ? TRUE : FALSE, b ? TRUE : FALSE, false, false);

		oaObj.newFlag = b;
		
		OAObjectEventDelegate.firePropertyChange(oaObj, WORD_New, old ? TRUE : FALSE, b ? TRUE : FALSE, false, false);
		if (!b) {
			setAutoAdd(oaObj, true);
		}
	}

	/**
	 * Ensures that the specified {@link OAObject} has a valid globally unique
	 * identifier (GUID). If the object already has a non-zero GUID, the method
	 * returns immediately without modification.
	 *
	 * <p>The GUID assignment strategy depends on the object's metadata and the
	 * current client/server execution context:</p>
	 *
	 * <h3>Assignment Rules</h3>
	 * <ul>
	 *   <li><b>Local-only classes</b>  
	 *       Use a negative, decrementing counter to ensure the GUID does not
	 *       overlap with server-issued identifiers.</li>
	 *
	 *   <li><b>Client-side execution</b>  
	 *       Attempt to obtain a server-issued GUID via
	 *       {@code OAObjectCSDelegate.getGuidFromServer(obj)}.  
	 *       If the server does not provide one (returns {@code 0}), a new GUID is
	 *       generated locally using {@link #getNextGuid()}.</li>
	 *
	 *   <li><b>Server-side execution</b>  
	 *       Always generates a new positive GUID using {@link #getNextGuid()}.</li>
	 * </ul>
	 *
	 * <p>This method must be invoked before any operations that rely on the object's
	 * identity, including hashing, cache insertion, linking, or client/server sync.</p>
	 *
	 * @param obj the object requiring GUID assignment; may be {@code null}.
	 */
	protected static void assignGuid(OAObject obj) {
		if (obj == null) {
			return;
		}
		if (obj.guid != 0) {
			return;
		}
		if (OAObjectInfoDelegate.getOAObjectInfo(obj).getLocalOnly()) {
			obj.guid = localGuidCounter.decrementAndGet();
		} else {
			if (!OASyncDelegate.isServer(obj)) {
				obj.guid = OAObjectCSDelegate.getGuidFromServer(obj);
				if (obj.guid == 0) {
					obj.guid = getNextGuid();
				}
			} else {
				obj.guid = getNextGuid();
			}
		}
	}

	
	/**
	 * Convenience method that reinitializes the specified {@link OAObject} so it
	 * behaves as a newly created instance. This method simply allocates a new GUID
	 * and delegates to {@link #setAsNewObject(OAObject, long)}.
	 *
	 * @param oaObj the object to reinitialize; may be {@code null}.
	 */
	public static void setAsNewObject(final OAObject oaObj) {
		if (oaObj == null) {
			return;
		}
		long guid = OAObjectCSDelegate.getGuidFromServer(oaObj);
		if (oaObj.guid == 0) {
			oaObj.guid = getNextGuid();
		}
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
	public static void setAsNewObject(final OAObject oaObj, long guid) {
		if (oaObj == null) {
			return;
		}
		oaObj.newFlag = true;
//qqqqqq was:		oaObj.objectKey = null;
		oaObj.guid = guid;

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());
		String[] ids = oi.getIdProperties();
		if (ids == null) {
			return;
		}

		OAThreadLocalDelegate.setLoading(true);
		try {
			for (String id : ids) {
				OAObjectReflectDelegate.setProperty(oaObj, id, null, null);
			}
		} finally {
			OAThreadLocalDelegate.setLoading(false);
		}

		//OAObjectCSDelegate.initialize(oaObj);
		if (OAObjectDSDelegate.getAssignIdOnCreate(oaObj)) {
			OAObjectDSDelegate.assignId(oaObj);
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
	public static void reassignGuid(OAObject obj, OAObjectKey origKey) {
		if (obj != null && origKey != null) {
			obj.guid = origKey.getGuid();
		}
	}

	/**
	 * Returns the next available positive GUID value and increments the internal
	 * GUID counter. This method is used for generating new unique identifiers for
	 * {@link OAObject} instances created on the local JVM.
	 *
	 * <p>The method delegates to {@link OAGuid#getNextGuid()} to retrieve and
	 * increment the global counter. The value returned is always positive and
	 * monotonically increasing, ensuring uniqueness across all locally created
	 * objects and preventing collisions with previously assigned GUIDs.</p>
	 *
	 * @return the next positive GUID value.
	 */
	public static long getNextGuid() {
		return guidCounter.incrementAndGet(); // cant be 0
	}

	
	/**
	 * Reserves the next fifty GUID values in the global GUID counter and returns the
	 * first GUID in that reserved block.
	 *
	 * <p>The method atomically adds {@code 50} to the internal counter and returns
	 * the first value in the allocated range. This is useful when a caller needs to
	 * preallocate a contiguous block of GUIDs, such as for batching or distributed
	 * assignment scenarios.</p>
	 *
	 * <p>Only the first GUID is returned. The remaining forty-nine GUIDs are
	 * implicitly reserved and may be obtained by incrementing sequentially from the
	 * returned value.</p>
	 *
	 * @return the first GUID in the next reserved block of fifty GUIDs.
	 */
	public static long getNextFiftyGuids() {
		return guidCounter.getAndAdd(50) + 1;
	}

	/**
	 * Sets the global GUID counter to the specified value.
	 *
	 * <p>This method directly assigns the internal {@code guidCounter} to the provided
	 * value {@code x}. It is used to advance or reset the positive GUID sequence for
	 * newly created {@link OAObject} instances. No validation is performed, and the
	 * next generated GUID will be {@code x + 1}.</p>
	 *
	 * <p>This is typically invoked when loading serialized graphs or synchronizing
	 * GUID state between distributed systems to ensure that locally generated GUIDs
	 * do not collide with previously issued ones.</p>
	 *
	 * @param x the new value of the global GUID counter.
	 */
	public static void setNextGuid(long x) {
		guidCounter.set(x);
	}

	/**
	 * Ensures that the global GUID counter is at least as large as the specified
	 * value. If {@code guid} is greater than the current counter, the internal
	 * counter is updated so that future GUID allocations will not overlap with the
	 * provided value.
	 *
	 * <p>This method does not assign a GUID to any object. Instead, it synchronizes
	 * the global GUID generator with an externally supplied value—typically one that
	 * originated from a DataSource, remote system, or merged object graph—so that
	 * locally generated GUIDs remain globally unique.</p>
	 *
	 * @param guid the GUID value that the global counter must reach or exceed.
	 */
	protected static void updateGuid(long guid) {
		for (;;) {
			long g = guidCounter.get();
			if (g >= guid) {
				break;
			}
			if (guidCounter.compareAndSet(g, guid)) {
				break;
			}
		}
	}
	
	/**
	 * Deprecated no-op method retained for backward compatibility.
	 *
	 * <p>This method previously prevented an {@link OAObject} instance from being
	 * finalized by the garbage collector. It no longer performs any operation and
	 * is maintained solely to preserve binary compatibility with older OA versions
	 * that may still invoke it.</p>
	 *
	 * @param obj the object referenced by the legacy call; ignored.
	 *
	 * @deprecated no longer required; method performs no action.
	 */
	@Deprecated
	protected static void dontFinalize(OAObject obj) {
		if (obj != null) {
			obj.guid = 0; // flag so that OAObject.finalize should ignore this object.
		}
	}


	/**
	 * Removes object from HubController and calls super.finalize().
	 */
/*qqqqqqqqqqq 20251105 can be removed	
	public static void finalizeObject(OAObject oaObj) {
		//System.out.println((++qq)+" finalizeObject: "+oaObj);
		if (oaObj.guid == 0) {
			return; // set to 0 by readResolve or ObjectCacheDelegate.add() to ignore finalization
		}
		if (oaObj.guid > 0 && !oaObj.deletedFlag) { // set to 0 by readResolve or ObjectCacheDelegate.add() to ignore finalization
			if ((oaObj.changedFlag || oaObj.newFlag) && !OAObjectCSDelegate.isWorkstation(oaObj)) {

				// 20131128 added autoAttach check
				if (OAObjectDelegate.getAutoAdd(oaObj)) {
					OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());
					if (oi != null && (oi.getUseDataSource() || OADataSource.getDataSource(oaObj.getClass()) != null)) {
						LOG.finer("object was not saved, object=" + oaObj.getClass().getName() + ", key="
								+ OAObjectKeyDelegate.getKey(oaObj)
								+ ", willSaveNow=" + bFinalizeSave);
						if (bFinalizeSave) {
							try {
								oaObj.save(OAObject.CASCADE_NONE);
							} catch (Exception e) {
								LOG.log(Level.WARNING, "object had error while saving, object=" + oaObj.getClass().getName() + ", key="
										+ OAObjectKeyDelegate.getKey(oaObj), e);
							}
						}
					}
				}
			}
		}
		OAObjectCacheDelegate.removeObject(oaObj); // remove from class cache

		hmAutoAdd.remove(oaObj.guid);
		oaObj.weakhubs = null;
	}
*/	

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
	public static boolean getChanged(OAObject oaObj, int iCascadeRule) {
		if (iCascadeRule == OAObject.CASCADE_NONE) {
			return (oaObj.changedFlag || oaObj.newFlag);
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
	public static boolean getChanged(final OAObject oaObj, int iCascadeRule, OACascade cascade) {
		if (oaObj.changedFlag || oaObj.newFlag) {
			return true;
		}
		if (iCascadeRule == oaObj.CASCADE_NONE) {
			return false;
		}
		if (cascade.wasCascaded(oaObj, true)) {
			return false;
		}

		if (oaObj.properties == null) {
			return false;
		}

		// check link cascade objects
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
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
			if (OAObjectReflectDelegate.isReferenceNullOrNotLoaded(oaObj, prop)) {
				continue;
			}

			boolean bValidCascade = false;
			if (iCascadeRule == OAObject.CASCADE_LINK_RULES && li.cascadeSave) {
				bValidCascade = true;
			} else if (iCascadeRule == OAObject.CASCADE_OWNED_LINKS && li.getOwner()) {
				bValidCascade = true;
			} else if (iCascadeRule == OAObject.CASCADE_ALL_LINKS) {
				bValidCascade = true;
			}

			if (OAObjectInfoDelegate.isMany2Many(li)) {
				Hub hub = (Hub) OAObjectReflectDelegate.getRawReference(oaObj, prop);
				if (HubDelegate.getChanged(hub, OAObject.CASCADE_NONE, cascade)) {
					return true;
				}
			}
			if (!bValidCascade) {
				continue;
			}

			Object obj = OAObjectReflectDelegate.getProperty(oaObj, li.name); // if Hub with Keys, then this will load the correct objects to check
			if (obj == null) {
				continue;
			}

			if (obj instanceof Hub) {
				if (OAObjectHubDelegate.getChanged((Hub) obj, iCascadeRule, cascade)) {
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
	public static void recurse(OAObject oaObj, OACallback callback) {
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
	public static void recurse(OAObject oaObj, OACallback callback, OACascade cascade) {
		if (cascade.wasCascaded(oaObj, true)) {
			return;
		}

		if (callback != null) {
			callback.updateObject(oaObj);
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);

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
			String prop = li.name;

			Object obj = OAObjectReflectDelegate.getProperty(oaObj, li.name); // select all
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
	protected static Object[] find(OAObject base, String propertyPath, Object findValue, boolean bFindAll) {
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
	 * Central method that is used when the object property Key is changed (OAObjectKey) and needs to be rehashed in all Hashtables that it
	 * could exist in.
	 *
	 * @param oaObj
	 * @param oldKey
	 */
/*20251015 not used anymore 
 * REMOVE THIS code	
	protected static void rehash(OAObject oaObj, OAObjectKey oldKey) {
		// Need to rehash all Hashtables that OAObject is stored in:
		// 1: CacheDelegate hashtable
		// 2: obj.Hubs - NOTE: not needed, since Hubs dont use hashtables anymore
		// 3: HashDelegate hashtables

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		if (oi.getAddToCache()) {
			OAObjectCacheDelegate.propertyKeyValueChanged(oaObj);
		}
		OAObjectHashDelegate.rehash(oaObj, oldKey);
	}
**/
	
	/**
	 * Returns the GUID assigned to the specified {@link OAObject}. If the object is
	 * {@code null}, the method returns {@code 0}.
	 *
	 * <p>This method does not generate or assign a GUID; it only returns the
	 * object's current GUID value. GUID assignment occurs during initialization or
	 * through explicit calls to methods such as {@link #assignGuid(OAObject)} or
	 * {@link #setObjectGuid(OAObject, long)}.</p>
	 *
	 * @param obj the object whose GUID is requested; may be {@code null}.
	 * @return the object's GUID, or {@code 0} if the object is {@code null}.
	 */
	public static long getGuid(OAObject obj) {
		if (obj == null) {
			return -1;
		}
		return obj.guid;
	}

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
	public static void setAutoAdd(OAObject oaObj, boolean bEnabled) {
		if (oaObj == null) {
			return;
		}
		if (!bEnabled && !oaObj.isNew()) {
			return;
		}

		boolean bOld = !hmAutoAdd.containsKey(oaObj.guid);
		if (bOld == bEnabled) {
			return;
		}

		if (!bEnabled) {
			hmAutoAdd.put(oaObj.guid, oaObj.guid);
		} else {
			hmAutoAdd.remove(oaObj.guid);
		}
		OAObjectEventDelegate.firePropertyChange(oaObj, WORD_AutoAdd, bOld ? TRUE : FALSE, bEnabled ? TRUE : FALSE, false, false);

		if (!bEnabled || oaObj.deletedFlag) {
			return;
		}

		try {
			OAThreadLocalDelegate.setSuppressCSMessages(true);
			// need to see if object should be put into linkOne/masterObject hub(s)
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
			for (OALinkInfo li : oi.getLinkInfos()) {
				if (!li.getUsed()) {
					continue;
				}
				if (li.getType() != li.ONE) {
					continue;
				}
				Object objx = OAObjectReflectDelegate.getRawReference(oaObj, li.getName());
				if (!(objx instanceof OAObject)) {
					continue;
				}

				OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
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

				Object objz = OAObjectReflectDelegate.getProperty((OAObject) objx, liRev.getName());
				if (objz instanceof Hub) {
					((Hub) objz).add(oaObj);
				}
			}
		} finally {
			OAThreadLocalDelegate.setSuppressCSMessages(false);
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
	public static boolean getAutoAdd(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		return !hmAutoAdd.containsKey(oaObj.guid);
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
	public static Object[] getPropertyIdValues(OAObject obj) {
		if (obj == null) return null;
		return OAObjectInfoDelegate.getPropertyIdValues(obj);
	}
}
