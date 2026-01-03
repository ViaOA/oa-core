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
package com.viaoa.hub;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectDSDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectDeleteDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAObjectSaveDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OANullObject;

/**
 * Primary internal delegate that implements the operational logic of {@link Hub}.
 * <p>
 * The HubDelegate encapsulates all shared algorithms required by Hubs—object
 * identity resolution, uniqueness validation, master/detail synchronization,
 * and cascade-based change detection—so that the {@code Hub} class itself
 * remains a thin facade.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Evaluate Hub-level and object-level “changed” state using {@link com.viaoa.object.OACascade}.</li>
 *   <li>Enforce per-Hub uniqueness constraints via reflection-based property evaluation.</li>
 *   <li>Resolve canonical object identities through {@link com.viaoa.object.OAObjectCacheDelegate}.</li>
 *   <li>Maintain class and master-relationship metadata used by Hub detail wiring.</li>
 *   <li>Compute Hub validity and synchronization state across shared, linked, and merged graphs.</li>
 *   <li>Support safe re-linking and refresh detection through {@code HubCurrentStateEnum} logic.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * All methods are static and stateless; the delegate acts as a functional
 * utility layer shared by every Hub instance.  Internal helpers such as
 * {@code _getCurrentState} and {@code getControllingHub} are recursive graph
 * evaluators used by detail, merger, and filter Hubs to maintain coherence.
 *
 * <h3>Threading and Reentrancy</h3>
 * No mutable static state is maintained; all Hub instance data is passed in via
 * parameters.  Cascades, recursion guards, and {@link java.util.HashSet}
 * tracking prevent infinite traversal through cyclic Hub graphs.
 */
public class HubDelegate {
	private static Logger LOG = Logger.getLogger(HubDelegate.class.getName());

	
	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubService().?(?);
    */
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	
	
	public static final Boolean TRUE = Boolean.valueOf(true);
	public static final Boolean FALSE = Boolean.valueOf(false);

	/**
	 * Determines whether this hub or any of its contained OAObjects are marked as
	 * changed according to the supplied cascade rules.
	 *
	 * <p>
	 * The method first checks whether this hub has already been processed in the
	 * current cascade; if so, it returns {@code false}. It then evaluates the hub’s
	 * own changed state. If cascade rules allow, it iterates through each object in
	 * the hub and checks whether any OAObject reports a changed state.
	 *
	 * @param thisHub      the hub being evaluated
	 * @param iCascadeRule the cascade rule used to determine how far change
	 *                     detection should propagate
	 * @param cascade      the cascade tracker used to prevent reprocessing
	 * @return {@code true} if the hub or any contained OAObject is changed;
	 *         otherwise {@code false}
	 */
	public static boolean getChanged(Hub thisHub, int iCascadeRule, OACascade cascade) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getChanged(thisHub, iCascadeRule, cascade);
	}

	/**
	 * Verifies that the specified object's unique property value does not already
	 * exist in this hub. If the hub or object is null, or if the object is loading,
	 * uniqueness checking is bypassed. When a unique property is defined, its value
	 * is obtained either through a link property or a getter method. Null or blank
	 * values are not checked.
	 *
	 * <p>
	 * The method iterates through all hub elements and compares each object's
	 * unique property value to that of the given object. If an equal value is found
	 * on a different object, the uniqueness constraint fails.
	 *
	 * @param thisHub the hub in which uniqueness is validated
	 * @param object  the object whose property value is being checked
	 * @return {@code true} if the unique value does not conflict; otherwise
	 *         {@code false}
	 */
	public static boolean verifyUniqueProperty(final Hub thisHub, final Object object) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().verifyUniqueProperty(thisHub, object);
	}

	/**
	 * Resolves the canonical instance of the given object for this hub. If the
	 * object's class does not match the hub's object class, the cache is queried
	 * first; if no cached instance exists, the hub is asked to resolve the object,
	 * potentially triggering data loading.
	 *
	 * @param hub    the hub providing the object class and lookup context
	 * @param object the object or key to resolve
	 * @return the resolved object instance, or the original value if no resolution
	 *         occurs
	 */
	public static Object getRealObject(Hub hub, Object object) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.hubs().getRealObject(hub, object);
	}

	/**
	 * Builds a property path linking the hub's object class through a sequence of
	 * classes. For each class in the array, the method locates a matching link
	 * property that targets that class. If multiple matching links are found, an
	 * exception is thrown. If no matching link exists, {@code null} is returned.
	 *
	 * @param hub     the starting hub whose object class defines the first segment
	 * @param classes array of classes describing the traversal path
	 * @return a dot-delimited property path, or {@code null} if a segment cannot be
	 *         resolved
	 */
	public static String getPropertyPathforClasses(Hub hub, Class[] classes) {
		//qqqqqqqqq method was protected
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.hubs().getPropertyPathforClasses(hub, classes);
	}

	/**
	 * Returns the master OAObject associated with this hub. If no master
	 * relationship exists or the hub is null, {@code null} is returned.
	 *
	 * @param hub the hub whose master object is requested
	 * @return the master OAObject, or {@code null} if none exists
	 */
	public static OAObject getMasterObject(Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.hubs().getMasterObject(hub);
	}

	/**
	 * Returns the class of the hub's master OAObject. If the master object exists,
	 * its class is returned; otherwise, if a master hub exists, that hub's object
	 * class is used. If neither is available, {@code null} is returned.
	 *
	 * @param hub the hub whose master object's class is requested
	 * @return the master class, or {@code null} if unavailable
	 */
	public static Class getMasterClass(Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.hubs().getMasterClass(hub);
	}

	/**
	 * Assigns the object class for this hub. The class cannot be changed if the hub
	 * already contains objects, has detail hubs, has a master object, or is shared.
	 * If validation passes, the hub's object class is updated.
	 *
	 * @param thisHub  the hub whose object class is being changed
	 * @param objClass the new object class
	 * @throws RuntimeException if the object class cannot be changed due to
	 *                          existing state constraints
	 */
	public static void setObjectClass(Hub thisHub, Class objClass) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().setObjectClass(thisHub, objClass);
	}

	/**
	 * Determines whether the hub is in a valid state. A hub is invalid if its
	 * master hub exists but has no active master object, or if any linked hub is
	 * invalid and cannot auto-create missing objects. If an addHub exists, its
	 * validity is also checked recursively.
	 *
	 * @param thisHub the hub being evaluated
	 * @return {@code true} if the hub is valid; otherwise {@code false}
	 */
	public static boolean isValid(final Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().isValid(thisHub);
	}

	/**
	 * Enumeration describing the synchronization state of a hub during updates.
	 *
	 * <ul>
	 *   <li>{@code InSync} – the hub is correctly aligned with its master or linked
	 *       state.</li>
	 *   <li>{@code DetailDisconectedFromMaster} – the detail hub does not match its
	 *       expected master state.</li>
	 *   <li>{@code DetailHubNotSameAsMasterObject} – the detail hub contains a
	 *       different object than the master hub’s active object.</li>
	 *   <li>{@code HubMergerNotUpdated} – a hub merger is not in sync with its
	 *       source hubs.</li>
	 * </ul>
	 */
	public static enum HubCurrentStateEnum {
		InSync,
		DetailDisconectedFromMaster,
		DetailHubNotSameAsMasterObject, // caused when object/hubs are in flux (hub event that is calling listeners and changing linkages)
		HubMergerNotUpdated
	}

	/**
	 * Evaluates the current synchronization state of the hub, optionally populating
	 * a replacement hub or list when a mismatch is detected. This is a wrapper that
	 * delegates to the internal recursive implementation.
	 *
	 * @param thisHub the hub being examined
	 * @param hubNew  optional hub to receive corrected state contents
	 * @param alNew   optional list to receive corrected state contents
	 * @return the hub’s synchronization status
	 */
    public static <T> HubCurrentStateEnum getCurrentState(final Hub<T> thisHub, final Hub<T> hubNew, final ArrayList<T> alNew) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getCurrentState(thisHub, hubNew, alNew);
    }

    /**
     * Internal recursive implementation for evaluating hub synchronization state.
     * Prevents cyclic traversal using the provided hub set. Traverses master hubs,
     * shared hubs, mergers, combined hubs, and filters to determine whether the hub
     * is aligned with its correct source.
     *
     * @param thisHub the hub being evaluated
     * @param hubNew  optional hub for corrected content
     * @param alNew   optional list for corrected content
     * @param hmHub   set of hubs visited to prevent cycles
     * @return the computed synchronization status, or {@code null} when a cycle is
     *         detected
     */
    protected static <T> HubCurrentStateEnum _getCurrentState(final Hub<T> thisHub, final Hub<T> hubNew, final ArrayList<T> alNew, final Set<Hub> hmHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs()._getCurrentState(thisHub, hubNew, alNew, hmHub);
	}

    /**
     * Determines which hub controls this hub’s validity. If the hub has a master
     * hub, that master hub is returned. If a linked shared hub exists, its link
     * target or its controlling hub is returned. If an addHub is present, its
     * controlling hub is evaluated. Otherwise, this hub is returned.
     *
     * @param thisHub the hub whose controlling hub is requested
     * @return the controlling hub
     */
	public static Hub getControllingHub(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getControllingHub(thisHub);
	}

	/**
	 * Returns this hub or any shared hub that has an addHub defined. Shared hubs
	 * are scanned using a filter to locate the first hub that supports additions.
	 *
	 * @param hub the hub to evaluate
	 * @return a hub with an addHub, or {@code null} if none exists
	 */
	public static Hub getAnyAddHub(final Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.hubs().getAnyAddHub(hub);
	}

	/**
	 * Updates link relationships for objects added to or removed from this hub.
	 * When objects are removed, the method determines whether the reverse link
	 * requires deletion, reference removal, or persistence based on the link type,
	 * master relationship, and cascade rules. Many-to-many links are updated when
	 * needed. New objects are skipped because they do not yet exist in the data
	 * source.
	 *
	 * @param thisHub       the hub whose add/remove state is being processed
	 * @param iCascadeRule  the cascade rule for save/delete operations
	 * @param cascade       the cascade tracker for preventing reprocessing
	 * @param bIsSaving     whether the caller is performing a save operation
	 */
	public static void _updateHubAddsAndRemoves(final Hub thisHub, final int iCascadeRule, final OACascade cascade,
			final boolean bIsSaving) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs()._updateHubAddsAndRemoves(thisHub, iCascadeRule, cascade, bIsSaving);
	}

	/**
	 * Configures the hub to enforce uniqueness based on the specified property.
	 * Validates that the property is not nested, that a corresponding getter
	 * method exists, and that the getter accepts no parameters. When {@code null}
	 * is supplied, the unique property is cleared.
	 *
	 * @param thisHub      the hub whose unique property is being set
	 * @param propertyName the name of the property used for uniqueness, or
	 *                     {@code null} to clear
	 * @throws IllegalArgumentException if the property is nested, lacks a getter,
	 *                                  or the getter requires parameters
	 */
	public static void setUniqueProperty(Hub thisHub, String propertyName) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().setUniqueProperty(thisHub, propertyName);
	}

	/**
	 * Enables automatic sequencing of objects in this hub by assigning sequential
	 * values to the specified property. Existing auto-sequence handlers are closed
	 * before creating a new one. Sorting is canceled to preserve sequence order.
	 * When the hub is a detail hub, sequencing is only enabled on the server side.
	 *
	 * @param thisHub     the hub whose objects will receive sequence values
	 * @param property    the property to update with sequence numbers
	 * @param startNumber the initial sequence number
	 * @param bKeepSeq    whether sequence values are preserved after removals
	 */
	public static void setAutoSequence(Hub thisHub, String property, int startNumber, boolean bKeepSeq) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().setAutoSequence(thisHub, property, startNumber, bKeepSeq);
	}

	/**
	 * Returns the auto-sequence controller for this hub, or {@code null} if none is
	 * assigned.
	 *
	 * @param thisHub the hub whose auto-sequence handler is requested
	 * @return the auto-sequence object, or {@code null} if not configured
	 */
	public static HubAutoSequence getAutoSequence(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getAutoSequence(thisHub);
	}

	/**
	 * Recomputes sequence values for all objects in this hub when auto-sequence is
	 * enabled. If no auto-sequence handler exists, no action is taken.
	 *
	 * @param thisHub the hub whose sequence values will be recalculated
	 */
	public static void resequence(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().resequence(thisHub);
	}

	/**
	 * Ensures that for every object in the master hub, there is a corresponding
	 * object in this hub whose specified property points to that master object.
	 * Existing auto-match handlers are closed before creating a new one. The match
	 * logic supports server-side restriction.
	 *
	 * @param thisHub         the hub being synchronized
	 * @param property        the property on this hub's objects used to match
	 * @param hubMaster       the hub whose objects must be mirrored
	 * @param bServerSideOnly whether matching should only be enforced on the server
	 */
	public static void setAutoMatch(Hub thisHub, String property, Hub hubMaster, boolean bServerSideOnly) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().setAutoMatch(thisHub, property, hubMaster, bServerSideOnly);
	}

	/**
	 * Variant of auto-match initialization that includes a stopping condition. For
	 * each object in the master hub, this hub ensures a corresponding object exists
	 * unless the match path encounters the specified stop object and property.
	 *
	 * @param thisHub         the hub being synchronized
	 * @param property        the property used to link to master hub objects
	 * @param hubMaster       the hub being mirrored
	 * @param bServerSideOnly whether matching is server-only
	 * @param objStop         optional object used to limit matching
	 * @param stopProperty    the property that defines the stopping condition
	 */
	public static void setAutoMatch(Hub thisHub, String property, Hub hubMaster, boolean bServerSideOnly, OAObject objStop, String stopProperty) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().setAutoMatch(thisHub, property, hubMaster, bServerSideOnly, objStop, stopProperty);
	}

	/**
	 * Returns the auto-match controller for this hub, or {@code null} if no
	 * auto-match logic is configured.
	 *
	 * @param thisHub the hub whose auto-match handler is requested
	 * @return the auto-match object, or {@code null} if none exists
	 */
	public static HubAutoMatch getAutoMatch(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getAutoMatch(thisHub);
	}

	/**
	 * Returns the logical size of this hub. If the hub is backed by a select with
	 * more data available, counting and fetch operations are used to determine the
	 * full size. If no select applies, the in-memory object count is returned.
	 *
	 * @param thisHub the hub whose size is requested
	 * @return the number of objects the hub represents
	 */
	public static int getSize(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getSize(thisHub);
	}

	/**
	 * Ensures that all data is loaded into the hub and then returns its size. A
	 * {@code null} hub returns zero.
	 *
	 * @param thisHub the hub whose fully loaded size is requested
	 * @return the loaded size of the hub
	 */
	public static int getLoadedSize(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getLoadedSize(thisHub);
	}

	/**
	 * Stores a named property value on the hub. Property names are normalized to
	 * uppercase. A {@link OANullObject} marker is stored when the value is
	 * {@code null}. A new property map is created on demand.
	 *
	 * @param thisHub the hub whose property map is updated
	 * @param name    the property name
	 * @param obj     the value to store, or {@code null}
	 */
	protected static void setProperty(Hub thisHub, String name, Object obj) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().setProperty(thisHub, name, obj);
	}

	/**
	 * Retrieves a named property value previously stored on the hub. Property names
	 * are normalized to uppercase. A stored {@link OANullObject} resolves to
	 * {@code null}. If no property map exists, {@code null} is returned.
	 *
	 * @param thisHub the hub whose property is requested
	 * @param name    the property name
	 * @return the stored value, or {@code null} if not found
	 */
	protected static Object getProperty(Hub thisHub, String name) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getProperty(thisHub, name);
	}

	/**
	 * Removes a property from the hub’s property map. Property names are converted
	 * to uppercase. If no property map exists, no action is taken.
	 *
	 * @param thisHub the hub whose property should be removed
	 * @param name    the name of the property to remove
	 */
	protected static void removeProperty(Hub thisHub, String name) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().removeProperty(thisHub, name);
	}

	/**
	 * Updates referenceability settings for this hub and its parent objects. If the
	 * hub is server-side and the object class supports weak referencing, this method
	 * adjusts weak-reference behavior based on whether references should be
	 * maintained. When enabling referenceability, parent objects are also updated.
	 *
	 * @param hub            the hub whose referenceability is being updated
	 * @param bReferenceable whether objects referenced by this hub should remain
	 *                       strongly referenceable
	 */
	public static void setReferenceable(Hub hub, boolean bReferenceable) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return;
		g.hubs().setReferenceable(hub, bReferenceable);
	}
}
