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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectHubDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASync;
import com.viaoa.util.OAFilter;

/**
 * Manages “shared” Hubs that reference the same {@link HubData} or Active
 * Object. Enables coordinated views and reuse of loaded data across multiple
 * Hubs.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Locate and traverse all shared Hub relationships (parent, children, filters).</li>
 *   <li>Resolve {@link HubFilter}, {@link HubCopy}, and {@link HubShareAO} associations.</li>
 *   <li>Provide filtered enumeration and AO-sharing utilities.</li>
 * </ul>
 *
 * <p>Forms the foundation for Hub “sharing” patterns — allowing lists to be
 * reused across contexts while maintaining independent AO state when required.
 */
public class HubShareDelegate {
	private static Logger LOG = Logger.getLogger(HubShareDelegate.class.getName());

	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubShareService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubShareService().?(?);
    */
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	
	/**
	 * Returns all Hubs that share the same underlying HubData as {@code thisHub}.
	 *
	 * <p>Equivalent to calling {@link #getAllSharedHubs(Hub, boolean, OAFilter)}
	 * with {@code bChildrenOnly = false} and no filter.</p>
	 *
	 * @param thisHub the Hub whose shared group is requested
	 * @return array of shared Hubs (possibly size 0), including {@code thisHub}
	 */
	public static Hub[] getAllSharedHubs(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getAllSharedHubs(thisHub);
	}

	/**
	 * Returns all Hubs that share the same data as {@code thisHub}, optionally
	 * restricting results to children in the shared chain.
	 *
	 * @param thisHub        the Hub to evaluate
	 * @param bChildrenOnly  true to return only Hubs directly shared from thisHub
	 * @return array of shared Hubs
	 */
	public static Hub[] getAllSharedHubs(Hub thisHub, boolean bChildrenOnly) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getAllSharedHubs(thisHub, bChildrenOnly);
	}

	/**
	 * Returns all shared Hubs that satisfy the given filter.
	 *
	 * @param thisHub the Hub whose shared group is being enumerated
	 * @param filter  an OAFilter determining which Hubs to include
	 * @return array of filtered shared Hubs
	 */
	public static Hub[] getAllSharedHubs(Hub thisHub, OAFilter<Hub> filter) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getAllSharedHubs(thisHub, filter);
	}

	/**
	 * Returns all Hubs sharing the same HubData as {@code thisHub}, with
	 * optional child-only restriction and filtering.
	 *
	 * @param thisHub       the Hub whose relationships are examined
	 * @param bChildrenOnly true to restrict results to downstream shared Hubs
	 * @param filter        optional filter to select which Hubs to return
	 * @return array of shared Hubs
	 */
	public static Hub[] getAllSharedHubs(Hub thisHub, boolean bChildrenOnly, OAFilter<Hub> filter) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getAllSharedHubs(thisHub, bChildrenOnly, filter);
	}

	/**
	 * Core implementation for discovering all shared Hubs, including those
	 * linked through HubFilters or AO-sharing relationships.
	 *
	 * @param thisHub              base Hub
	 * @param bChildrenOnly        restrict to descendants only
	 * @param filter               filter applied to discovered Hubs
	 * @param bIncludeFilteredHubs whether HubFilter-based shared Hubs are included
	 * @param bOnlyIfSharedAO      true to include only Hubs sharing the AO source
	 * @return array of discovered shared Hubs
	 */
	protected static Hub[] getAllSharedHubs(Hub thisHub, boolean bChildrenOnly, OAFilter<Hub> filter, boolean bIncludeFilteredHubs,
			boolean bOnlyIfSharedAO) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getAllSharedHubs(thisHub, bChildrenOnly, filter, bIncludeFilteredHubs, bOnlyIfSharedAO);
	}


	/**
	 * Returns the HubCopy associated with the shared Hub group, if present.
	 * Searches the main shared Hub’s listeners for a HubCopy instance.
	 *
	 * @param thisHub the Hub to examine
	 * @return the HubCopy instance, or null if none exists
	 */
	public static HubCopy getHubCopy(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getHubCopy(thisHub);
	}

	/**
	 * Locates a HubFilter attached to the main shared Hub.
	 *
	 * @param thisHub the Hub to examine
	 * @return a HubFilter instance, or null if none is found
	 */
	public static HubFilter getHubFilter(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getHubFilter(thisHub);
	}

	/**
	 * Returns the HubShareAO listener (if any) that synchronizes Active Objects
	 * for the given Hub’s shared group.
	 *
	 * @param thisHub the Hub to inspect
	 * @return HubShareAO listener or null
	 */
	public static HubShareAO getHubShareAO(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getHubShareAO(thisHub);
	}

	/**
	 * Returns the Hub that {@code thisHub} is shared with, optionally
	 * including filtered or AO-sharing relationships.
	 *
	 * @param thisHub             the Hub whose shared parent is requested
	 * @param bIncludeFilteredHubs include HubFilters when resolving shared hubs
	 * @param bOnlyIfSharedAO     restrict results to AO-sharing cases
	 * @return the shared Hub or null
	 */
	public static Hub getSharedHub(final Hub thisHub, boolean bIncludeFilteredHubs, boolean bOnlyIfSharedAO) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getSharedHub(thisHub, bIncludeFilteredHubs, bOnlyIfSharedAO);
	}

	/**
	 * Traverses the shared-Hub graph to find the first Hub satisfying the
	 * given filter.
	 *
	 * @param thisHub              starting Hub
	 * @param filter               filter to test against
	 * @param bIncludeFilteredHubs include HubFilters in traversal
	 * @param bOnlyIfSharedAO      restrict traversal to AO-sharing Hubs
	 * @return the first matching Hub, or null if none found
	 */
	public static Hub getFirstSharedHub(Hub thisHub, OAFilter<Hub> filter, boolean bIncludeFilteredHubs, boolean bOnlyIfSharedAO) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getFirstSharedHub(thisHub, filter, bIncludeFilteredHubs, bOnlyIfSharedAO);
	}


	/**
	 * Returns the root of a shared-Hub chain by following the sharedHub links
	 * upward until no further parent exists.
	 *
	 * @param hub starting Hub
	 * @return the root shared Hub
	 */
	public static Hub getMainSharedHub(Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getMainSharedHub(hub);
	}

	/**
	 * Determines whether two Hubs share the same HubData instance.
	 *
	 * @param hub1 first Hub
	 * @param hub2 second Hub
	 * @return true if both use identical HubData
	 */
	public static boolean isUsingSameSharedHub(Hub hub1, Hub hub2) {
		OAGraph g = getGraph(hub1, null);
		if (g == null) return false;
		return g.hubs().getHubShareService().isUsingSameSharedHub(hub1, hub2);
	}

	/**
	 * Determines whether two Hubs share the same Active Object source.
	 *
	 * @param hub1 first Hub
	 * @param hub2 second Hub
	 * @return true if they use the same HubDataActive instance
	 */
	public static boolean isUsingSameSharedAO(Hub hub1, Hub hub2) {
		OAGraph g = getGraph(hub1, null);
		if (g == null) return false;
		return g.hubs().getHubShareService().isUsingSameSharedAO(hub1, hub2);
	}

	/**
	 * Determines AO-sharing equivalence, optionally including filtered Hub
	 * relationships.
	 *
	 * @param hub1 first Hub
	 * @param hub2 second Hub
	 * @param bIncludeFilteredHubs include HubFilter-based AO sharing
	 * @return true if the Hubs share an AO source
	 */
	public static boolean isUsingSameSharedAO(Hub hub1, Hub hub2, boolean bIncludeFilteredHubs) {
		OAGraph g = getGraph(hub1, null);
		if (g == null) return false;
		return g.hubs().getHubShareService().isUsingSameSharedAO(hub1, hub2, bIncludeFilteredHubs);
	}

	/**
	 * Synchronizes HubData and HubDataActive instances across all Hubs in the
	 * shared group. Updates AO state when required and ensures detail-Hub link
	 * consistency.
	 *
	 * @param thisHub            the Hub undergoing changes
	 * @param bShareActiveObject whether AO state should be shared
	 * @param daOld              prior HubDataActive instance
	 * @param daNew              new HubDataActive instance
	 * @param bUpdateLink        whether link-based AO adjustments should occur
	 */
	public static void syncSharedHubs(Hub thisHub, boolean bShareActiveObject, HubDataActive daOld, HubDataActive daNew,
			boolean bUpdateLink) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubShareService().syncSharedHubs(thisHub, bShareActiveObject, daOld, daNew, bUpdateLink);
	}

	/**
	 * Resets AO state in all shared Hubs after a remove-all operation and
	 * recursively propagates the change through the shared-Hub graph.
	 *
	 * @param thisHub the Hub where the remove-all occurred
	 */
	public static void setSharedHubsAfterRemoveAll(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubShareService().setSharedHubsAfterRemoveAll(thisHub);
	}

	/**
	 * Updates Active Object values across all shared Hubs after a single
	 * object removal. Ensures AO validity based on size, linkHub status,
	 * null-on-remove rules, and AO-sharing behavior.
	 *
	 * @param thisHub     the Hub where removal occurred
	 * @param objRemoved  the object removed
	 * @param posRemoved  its position within the Hub
	 */
	public static void setSharedHubsAfterRemove(Hub thisHub, Object objRemoved, int posRemoved) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubShareService().setSharedHubsAfterRemove(thisHub, objRemoved, posRemoved);
	}

	/**
	 * Creates a new Hub instance that shares its underlying data with the
	 * specified {@code thisHub}. The new Hub is initialized using the same
	 * object type as {@code thisHub} and is then configured to participate in
	 * the shared-Hub relationship.
	 *
	 * @param thisHub      the Hub whose data and shared group the new Hub will join
	 * @param bShareActive true to share the active object state with {@code thisHub};
	 *                     false to maintain a separate active object
	 * @return the newly created shared Hub
	 */
	public static Hub createSharedHub(Hub thisHub, boolean bShareActive) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().createSharedHub(thisHub, bShareActive);
	}

	/**
	 * Assigns {@code thisHub} to share data with the specified
	 * {@code sharedMasterHub}. Sharing may optionally include the active
	 * object state. This method performs the basic shared-Hub setup and
	 * delegates extended behavior to the 4-argument overload.
	 *
	 * @param thisHub           the Hub to configure for sharing
	 * @param sharedMasterHub   the Hub whose data will be shared
	 * @param shareActiveObject true to share active-object state, false for independent AO
	 */
	public static void setSharedHub(Hub thisHub, Hub sharedMasterHub, boolean shareActiveObject) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubShareService().setSharedHub(thisHub, sharedMasterHub, shareActiveObject);
	}

	/**
	 * Internal implementation for establishing a shared-Hub relationship.
	 * Performs full validation, detaches any previous shared configuration,
	 * aligns HubData/HubDataActive as required, updates shared children,
	 * recalculates active-object values, and fires list-reset events.
	 *
	 * @param thisHub           the Hub being configured
	 * @param sharedMasterHub   the Hub whose data is shared
	 * @param shareActiveObject true to share active-object state
	 * @param newLinkValue      optional pending link-value used during AO updates
	 */
	public static void setSharedHub(Hub thisHub, Hub sharedMasterHub, boolean shareActiveObject, Object newLinkValue) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubShareService().setSharedHub(thisHub, sharedMasterHub, shareActiveObject, newLinkValue);
	}

	/**
	 * Core worker that performs the detailed process of attaching
	 * {@code thisHub} to the shared-Hub graph. Handles recursive-Hub protection,
	 * compatibility checks, data replacement, AO alignment, listener updates,
	 * and propagation of active-object recalculation.
	 *
	 * @param thisHub           Hub being attached
	 * @param sharedMasterHub   Hub that supplies shared data
	 * @param shareActiveObject whether to share AO state
	 * @param newLinkValue      temporary link value used during AO recalculation
	 */
	protected static void _setSharedHub(Hub thisHub, Hub sharedMasterHub, boolean shareActiveObject, Object newLinkValue) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubShareService()._setSharedHub(thisHub, sharedMasterHub, shareActiveObject, newLinkValue);
	}

	/**
	 * Adds {@code hub} to the list of Hubs that are shared with {@code thisHub}.
	 * This operation records the shared relationship using a weak reference and
	 * then clears the cached listener map so that subsequent listener lookup
	 * reflects the updated shared-Hub configuration.
	 *
	 * @param thisHub the Hub whose shared-Hub list is being updated
	 * @param hub     the Hub to add as a shared participant
	 */
	public static void addSharedHub(Hub thisHub, Hub hub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubShareService().addSharedHub(thisHub, hub);
	}

	/**
	 * Internal worker that inserts {@code hub} into {@code thisHub}'s
	 * weak-shared-Hubs array. Expands the underlying array when full and
	 * reuses empty or garbage-collected slots when available.
	 *
	 * @param thisHub the Hub whose weak-shared-Hub list is being modified
	 * @param hub     the Hub to add as a shared reference
	 */
	protected static void _addSharedHub(Hub thisHub, Hub hub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubShareService()._addSharedHub(thisHub, hub);
	}

	/**
	 * Removes {@code hub} from {@code sharedHub}'s weak-shared-Hub list.
	 * Delegates structural cleanup to the internal worker and clears both
	 * Hubs’ cached listener data so the system will recalculate listeners
	 * the next time they are requested.
	 *
	 * @param sharedHub the Hub whose shared list is being updated
	 * @param hub       the Hub to remove
	 */
	public static void removeSharedHub(Hub sharedHub, Hub hub) {
		OAGraph g = getGraph(sharedHub, null);
		if (g == null) return;
		g.hubs().getHubShareService().removeSharedHub(sharedHub, hub);
	}

	/**
	 * Internal worker that removes {@code hub} from {@code sharedHub}'s
	 * weak-shared-Hub array. Handles compaction of the array, removal of
	 * null or garbage-collected references, and resizing when appropriate
	 * to reduce unused capacity.
	 *
	 * @param sharedHub the Hub whose stored references are modified
	 * @param hub       the Hub being removed
	 */
	protected static void _removeSharedHub(Hub sharedHub, Hub hub) {
		OAGraph g = getGraph(sharedHub, null);
		if (g == null) return;
		g.hubs().getHubShareService()._removeSharedHub(sharedHub, hub);
	}

	/**
	 * Returns an array of Hubs directly recorded as shared with {@code thisHub}.
	 * Uses the weak-shared-Hub array and trims trailing null or empty entries.
	 * This method is deprecated in favor of {@link #getAllSharedHubs}.
	 *
	 * @param thisHub the Hub to inspect
	 * @return array of directly shared Hubs (may contain null entries)
	 * @deprecated use {@link #getAllSharedHubs} instead
	 */
	protected static Hub[] getSharedHubs(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getSharedHubs(thisHub);
	}

	/**
	 * Returns the internal weak-reference array that stores Hubs sharing
	 * data with {@code thisHub}. May be null if no shared-Hub relationships
	 * have been established.
	 *
	 * @param thisHub the Hub whose shared structure is requested
	 * @return weak-reference array, or null if none exist
	 */
	public static WeakReference<Hub>[] getSharedWeakHubs(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubShareService().getSharedWeakHubs(thisHub);
	}

	/**
	 * Counts valid Hub references in {@code thisHub}'s weak-shared-Hub array.
	 * Entries that are null or whose referent has been garbage-collected
	 * are not included.
	 *
	 * @param thisHub the Hub to inspect
	 * @return number of active shared-Hub references
	 */
	public static int getSharedWeakHubSize(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubShareService().getSharedWeakHubSize(thisHub);
	}

}
