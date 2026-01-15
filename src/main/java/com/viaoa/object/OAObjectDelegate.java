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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.viaoa.context.OAContext;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAString;

//qqqqqqqqq PHASE 3: moved to OAObjectService, OAObjectInitializeService, OAObjectGuidService

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

	public static Logger LOG = Logger.getLogger(OAObjectDelegate.class.getName());

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

	/** Static global lock used when setting global properties (ex: guidCounter) */

	/**
	 * Global positive GUID generator used for locally created objects. Ensures
	 * unique, monotonically increasing identifiers.
	 */
	static protected final AtomicLong guidCounter = new AtomicLong(); // unique identifier needed for objects past from client/server

	/**
	 * Global negative GUID generator used for local-only classes to avoid
	 * collisions with server-issued GUIDs.
	 */
	static protected final AtomicLong localGuidCounter = new AtomicLong();

	/**
	 * Flag controlling whether objects that are garbage collected should be
	 * automatically saved. Default is disabled.
	 */
	protected static boolean bFinalizeSave = false;


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
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return false;
		og.objects().getOAObjectInitializeService().initialize(oaObj);
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
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return;
		og.objects().getOAObjectInitializeService().initializeAfterLoading(oaObj);
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
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return;
		og.objects().getOAObjectInitializeService().initializeAfterLoading(oaObj, bAssignNewId, bInitializeNulls, bSetChangedToFalse);
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
	public static void initialize(
	        OAObject oaObj,
	        OAObjectInfo oi,
	        boolean bInitializeNulls,
	        boolean bInitializeWithDS,
	        boolean bAddToCache,
	        boolean bInitializeWithCS,
	        boolean bSetChangedToFalse) {
		//qqqqqq method was protected
		final boolean bWasLoading = OARuntime.get().threadLocals().setLoading(true);

		if (oaObj == null) return;
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return;
		og.objects().getOAObjectInitializeService().initialize(oaObj, oi, bInitializeNulls, bInitializeWithDS, bAddToCache, bInitializeWithCS, bSetChangedToFalse);
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
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return;
		og.objects().setNew(oaObj, b);
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
		OAGraph og = OARuntime.get().graph(obj);
		if (og == null) return;
		og.objects().getOAObjectGuidService().assignGuid(obj);
	}
	
	/**
	 * Convenience method that reinitializes the specified {@link OAObject} so it
	 * behaves as a newly created instance. This method simply allocates a new GUID
	 * and delegates to {@link #setAsNewObject(OAObject, long)}.
	 *
	 * @param oaObj the object to reinitialize; may be {@code null}.
	 */
	public static void setAsNewObject(final OAObject oaObj) {
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return;
		og.objects().getOAObjectInitializeService().setAsNewObject(oaObj);
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
	public static void setAsNewObject(final OAObject oaObj, UUID guid) {
		if (oaObj == null) return;
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return;
		og.objects().getOAObjectInitializeService().setAsNewObject(oaObj, guid);
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
	public static void reassignGuid(OAObject oaObj, OAObjectKey origKey) {
		if (oaObj == null) return;
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return;
		og.objects().getOAObjectInitializeService().reassignGuid(oaObj, origKey);
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
	/*qqqqqqqqqqqqqqq
	public static long getNextGuid(Package p) {
		if (p == null) return 0l;
		OAGraph og = OARuntime.get().graph(p);
		if (og == null) return 0l;
		return og.objects().getOAObjectGuidService().getNextGuid();
	}
	*/

	
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
	/*qqqqqqqqqqqqqqq
	public static long getNextFiftyGuids(Package pkg) {
		if (pkg == null) return 0l;
		OAGraph og = OARuntime.get().graph(pkg);
		if (og == null) return 0l;
		return og.objects().getOAObjectGuidService().getNextFiftyGuids();
	}
	*/

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
	/*qqqqqqqqqqqqqqq
	public static void setNextGuid(Package pkg, long x) {
		if (pkg == null) return;
		OAGraph og = OARuntime.get().graph(pkg);
		if (og == null) return;
		og.objects().getOAObjectGuidService().setNextGuid(x);
	}
	*/

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
	/*qqqqqqqqqqqqqqq
	static void updateGuid(Package pkg, long guid) {
		if (pkg == null) return;
		OAGraph og = OARuntime.get().graph(pkg);
		if (og == null) return;
		og.objects().getOAObjectGuidService().updateGuid(guid);
	}
	*/
	
	/**
	 * This method prevents an {@link OAObject} instance from being
	 * cleaned up, by changing the guid to 0. 
	 */
	protected static void dontFinalize(OAObject obj) {
		OAGraph og = OARuntime.get().graph(obj);
		if (og == null) return;
		og.objects().dontFinalize(obj);
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
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return false;
		return og.objects().getChanged(oaObj, iCascadeRule);
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
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return false;
		return og.objects().getChanged(oaObj, iCascadeRule, cascade);
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
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return;
		og.objects().recurse(oaObj, callback);
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
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return;
		og.objects().recurse(oaObj, callback, cascade);
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
		OAGraph og = OARuntime.get().graph(base);
		if (og == null) return null;
		return og.objects().find(base, propertyPath, findValue, bFindAll);
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
	public static UUID getGuid(OAObject oaObj) {
		if (oaObj == null) return null;
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return null;
		return og.objects().getOAObjectGuidService().getGuid(oaObj);
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
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return;
		og.objects().setAutoAdd(oaObj, bEnabled);
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
		OAGraph og = OARuntime.get().graph(oaObj);
		if (og == null) return false;
		return og.objects().getAutoAdd(oaObj);
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
		if (obj == null) {
			return null;
		}
		OAGraph og = OARuntime.get().graph(obj);
		if (og == null) return null;
		return og.objects().getPropertyIdValues(obj);
	}
}
