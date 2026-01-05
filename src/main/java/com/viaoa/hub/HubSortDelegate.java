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

import java.io.Serializable;
import java.util.*;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAComparator;
import com.viaoa.util.OAString;

/**
 * Provides sorting and ordering logic for {@link Hub} contents.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Apply and maintain property-path or custom comparator sorting.</li>
 *   <li>Reorder Hub membership efficiently on property changes.</li>
 *   <li>Integrate with event sequencing to notify listeners post-sort.</li>
 * </ul>
 *
 * <p>Supports both ascending and descending order, local sorting, and
 * comparator replacement while preserving selection state and data stability.
 */
public class HubSortDelegate {

	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubSortService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubSortService().?(?);
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
	 * Sorts the contents of {@code thisHub} using the specified property paths,
	 * direction, and comparator. Delegates to the extended sort method with
	 * {@code bAlreadySortedAndLocalOnly=false}.
	 *
	 * @param thisHub       the Hub to sort
	 * @param propertyPaths property path(s) used for sorting
	 * @param bAscending    true for ascending order, false for descending
	 * @param comp          optional Comparator; if null, property-based sorting is used
	 */
    public static void sort(Hub thisHub, String propertyPaths, boolean bAscending, Comparator comp) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubSortService().sort(thisHub, propertyPaths, bAscending, comp);
    }

    /**
     * Sorts {@code thisHub} using the given property paths and direction,
     * without specifying a custom comparator. The comparator defaults to
     * property-based sorting.
     *
     * @param thisHub       the Hub to sort
     * @param propertyPaths property path(s) used for sorting
     * @param bAscending    true for ascending order, false for descending
     */
    public static void sort(Hub thisHub, String propertyPaths, boolean bAscending) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubSortService().sort(thisHub, propertyPaths, bAscending);
    }
    
    /**
     * Main entry point for sorting a Hub. Validates comparator usage,
     * detects no-op sort conditions, updates the HubSortListener, and
     * performs the actual sort unless marked as already sorted locally.
     *
     * @param thisHub                     the Hub to sort
     * @param propertyPaths               property path(s) used for sorting
     * @param bAscending                  true for ascending order
     * @param comp                        optional Comparator; must be Serializable if used on a masterHub
     * @param bAlreadySortedAndLocalOnly  true to skip performing sort (local-only sorted Hub)
     * @return true if sort parameters changed and sorting should be performed
     */
    public static void sort(Hub thisHub, String propertyPaths, boolean bAscending, Comparator comp, boolean bAlreadySortedAndLocalOnly) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubSortService().sort(thisHub, propertyPaths, bAscending, comp, bAlreadySortedAndLocalOnly);
    }
    
    /**
     * Returns the current HubSortListener used by {@code thisHub} to
     * maintain sorted order, or null if no active sort exists.
     *
     * @param thisHub the Hub to inspect
     * @return the active HubSortListener, or null
     */
    public static HubSortListener getSortListener(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return null;
    	return g.hubs().getHubSortService().getSortListener(thisHub);
    }
    
  
    /**
     * Re-sorts the Hub using the last sort or select parameters. Equivalent to
     * calling {@link #sort(Hub)}.
     *
     * @param thisHub the Hub to re-sort
     */
	public static void resort(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubSortService().resort(thisHub);
	}
	
	/**
	 * Re-sorts {@code thisHub} using previously stored sort parameters.
	 * Loads all data if needed, performs the sort, and fires sort events.
	 *
	 * @param thisHub the Hub to sort
	 */
	public static void sort(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubSortService().sort(thisHub);
	}

    /**
     * Cancels any existing sort on {@code thisHub}. If the Hub is currently
     * kept sorted, invokes the sort method with null parameters to reset
     * sort state.
     *
     * @param thisHub the Hub whose sort state is being cancelled
     */
	public static void cancelSort(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubSortService().cancelSort(thisHub);
	}
	
	/**
	    Used to keep objects sorted based on last call to select method.  By default, the sort order
	    used in a select is not maintained within the Hub.  This method will keep the objects sorted
	    using the same property paths used by select.
	*/
/**qqqqqqqqqqq  20150810 removed, sort will keepSorted by default	
	public static void keepSorted(Hub thisHub) {
	    // 20090801 cant have sorter if a AutoSequence is being used
	    if (thisHub.data.getAutoSequence() != null) {
	        return;
	    }
	    if (thisHub.data.getSortListener() != null) return;
	    if (HubSelectDelegate.getSelect(thisHub) == null) return;
	    String s = HubSelectDelegate.getSelect(thisHub).getOrder();
	    if (s == null || s.length() == 0) return;
	    sort(thisHub, s, true, null, true);
	}
*/

	/**
	 * Indicates whether {@code thisHub} is currently kept sorted through an
	 * active HubSortListener.
	 *
	 * @param thisHub the Hub to check
	 * @return true if the Hub is maintained in sorted order
	 */
	public static boolean isSorted(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return false;
    	return g.hubs().getHubSortService().isSorted(thisHub);
    }

	/**
	 * Returns the property-path(s) used for sorting {@code thisHub}, checking
	 * current Hub data first and falling back to master data if required.
	 *
	 * @param thisHub the Hub to inspect
	 * @return the configured sort property path(s), or null
	 */
    public static String getSortProperty(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return null;
    	return g.hubs().getHubSortService().getSortProperty(thisHub);
    }

    /**
     * Returns whether {@code thisHub} is sorted in ascending order.
     * Evaluates local Hub data first, then master data.
     *
     * @param thisHub the Hub to inspect
     * @return true if ascending, otherwise false
     */
    public static boolean getSortAsc(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return false;
    	return g.hubs().getHubSortService().getSortAsc(thisHub);
    }

    /**
     * Returns the sequence property used by the Hub's master data. This does
     * not necessarily indicate that the Hub is sorted, only that a sequence
     * property exists.
     *
     * @param thisHub the Hub to inspect
     * @return the sequence property name, or null
     */
    public static String getSeqProperty(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return null;
    	return g.hubs().getHubSortService().getSeqProperty(thisHub);
    }
}


