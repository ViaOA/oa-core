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

import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;
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
        sort(thisHub, propertyPaths, bAscending, comp, false);
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
        sort(thisHub, propertyPaths, bAscending, null, false);
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
        if (thisHub == null) return;
        boolean b = false;
        try {
            OAThreadLocalDelegate.lock(thisHub);
            b = _sort(thisHub, propertyPaths, bAscending, comp, bAlreadySortedAndLocalOnly);
        }
        finally {
            OAThreadLocalDelegate.unlock(thisHub);
        }
        if (b) afterPerformSort(thisHub); // outside of lock
    }
    
    /**
     * Returns the current HubSortListener used by {@code thisHub} to
     * maintain sorted order, or null if no active sort exists.
     *
     * @param thisHub the Hub to inspect
     * @return the active HubSortListener, or null
     */
    public static HubSortListener getSortListener(Hub thisHub) {
        if (thisHub == null) return null;
        return thisHub.data.getSortListener();
    }
    
    /**
     * Internal worker that prepares and configures sorting for {@code thisHub}.
     * Ensures comparator validity, manages existing sort listeners, updates
     * sort attributes, and triggers client-side sort propagation when needed.
     *
     * @param thisHub                     the Hub being sorted
     * @param propertyPaths               property path(s) used for sorting
     * @param bAscending                  true for ascending order
     * @param comp                        optional Comparator
     * @param bAlreadySortedAndLocalOnly  true to skip performing sort
     * @return true if sorting parameters changed
     */
    private static boolean _sort(Hub thisHub, String propertyPaths, final boolean bAscending, Comparator comp, boolean bAlreadySortedAndLocalOnly) {
        OARemoteThreadDelegate.startNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message

        if (comp != null && !(comp instanceof Serializable)) {
            if (thisHub.datam.getMasterObject() != null) { 
                throw new RuntimeException("comparator is not Serializable");
            }
        }
        
        boolean bSame = false;
        HubSortListener hsl = thisHub.data.getSortListener();
        if (OAString.isEqual(propertyPaths, thisHub.data.getSortProperty(),true)) {
            if (bAscending == thisHub.data.isSortAsc()) {
                bSame = true;
            }
        }
        
        if (hsl != null) {
            if (bSame) {
                // make sure that comparator is same
                if (hsl.comparator == null) return false;
                if (hsl.comparator instanceof OAComparator) {
                    OAComparator compx = (OAComparator) hsl.comparator;
                    if (OAString.isEqual(propertyPaths, compx.getPropertyPaths(),true)) {
                        if (bAscending == compx.getAsc()) {
                            return false;
                        }
                    }
                }
                bSame = false;
            }
            hsl.close();
            thisHub.data.setSortListener(null);
        }
        else {
            if (bSame) {
                if (OAString.isEmpty(propertyPaths) && comp == null) return false;
            }
        }
        
        thisHub.data.setSortProperty(propertyPaths);
        thisHub.data.setSortAsc(bAscending);
        
        if (propertyPaths != null || comp != null) {
            thisHub.data.setSortListener(new HubSortListener(thisHub, comp, propertyPaths, bAscending));
            if (!bAlreadySortedAndLocalOnly) _performSort(thisHub);
        }
        else { // cancel sort
            thisHub.data.setSortAsc(true);
        }
        
        if (!bAlreadySortedAndLocalOnly) {  // otherwise, no other client has this hub yet
            if (thisHub.datam.getMasterObject() != null) {
                // 20171028 need to send if sort is cancelled
                //was: if (propertyPaths != null || comp != null) { // otherwise it was a cancel
                    HubCSDelegate.sort(thisHub, propertyPaths, bAscending, comp);
                //}
            }
        }
        return true;
    }
    
    /**
     * Re-sorts the Hub using the last sort or select parameters. Equivalent to
     * calling {@link #sort(Hub)}.
     *
     * @param thisHub the Hub to re-sort
     */
	public static void resort(Hub thisHub) {
		sort(thisHub);
	}
	
	/**
	 * Re-sorts {@code thisHub} using previously stored sort parameters.
	 * Loads all data if needed, performs the sort, and fires sort events.
	 *
	 * @param thisHub the Hub to sort
	 */
	public static void sort(Hub thisHub) {
        if (thisHub == null) return;

        try {
            OAThreadLocalDelegate.lock(thisHub);
            _performSort(thisHub);
        }
        finally {
            OAThreadLocalDelegate.unlock(thisHub);
        }
        afterPerformSort(thisHub); // outside of lock
	}

	/**
	 * Prepares and performs a sort operation using a sibling-helper structure
	 * to manage multi-property dependencies. Delegates actual sorting to
	 * {@link #_performSortX(Hub)}.
	 *
	 * @param thisHub the Hub to sort
	 */
    private static void _performSort(Hub thisHub) {
        OASiblingHelper siblingHelper = new OASiblingHelper(thisHub);
        siblingHelper.setUseSameThread(true);
        HubSortListener hsl = thisHub.data.getSortListener();
        if (hsl != null) {
            String[] props = hsl.getPropeties();
            if (props != null) {
                for (String p : props) {
                    siblingHelper.add(p);
                }
            }
        }        
        try {
            OAThreadLocalDelegate.addSiblingHelper(siblingHelper);
            _performSortX(thisHub);
        }
        finally {
            OAThreadLocalDelegate.removeSiblingHelper(siblingHelper);
        }
    }
	
    /**
     * Executes the actual sorting of the Hub's underlying vector using its
     * active HubSortListener comparator. Retries several times to tolerate
     * concurrent modifications.
     *
     * @param thisHub the Hub whose contents are sorted
     */
	private static void _performSortX(Hub thisHub) {
		if (thisHub.data.getSortListener() == null) return;
		HubSelectDelegate.loadAllData(thisHub);
	    thisHub.data.changeCount++;
	    
	    for (int i=0; i<5; i++) {
	        try {
    	        Collections.sort(thisHub.data.vector, thisHub.data.getSortListener().comparator);
    	        break;
	        }
	        catch (ConcurrentModificationException e) {
	        }
	    }
	}
	
	/**
	 * Fires the post-sort event notifying listeners that sorting has completed.
	 *
	 * @param thisHub the Hub that was sorted
	 */
    private static void afterPerformSort(Hub thisHub) {
        HubEventDelegate.fireAfterSortEvent(thisHub);
    }
	
    /**
     * Cancels any existing sort on {@code thisHub}. If the Hub is currently
     * kept sorted, invokes the sort method with null parameters to reset
     * sort state.
     *
     * @param thisHub the Hub whose sort state is being cancelled
     */
	public static void cancelSort(Hub thisHub) {
	    if (isSorted(thisHub)) {
	        sort(thisHub, null, false, null);
	    }
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
        return (thisHub.data.getSortListener() != null);
    }

	/**
	 * Returns the property-path(s) used for sorting {@code thisHub}, checking
	 * current Hub data first and falling back to master data if required.
	 *
	 * @param thisHub the Hub to inspect
	 * @return the configured sort property path(s), or null
	 */
    public static String getSortProperty(Hub thisHub) {
        String s = thisHub.data.getSortProperty();
        if (s == null) s = thisHub.datam.getSortProperty();
        return s;
    }

    /**
     * Returns whether {@code thisHub} is sorted in ascending order.
     * Evaluates local Hub data first, then master data.
     *
     * @param thisHub the Hub to inspect
     * @return true if ascending, otherwise false
     */
    public static boolean getSortAsc(Hub thisHub) {
        boolean b = thisHub.data.isSortAsc();
        b = b || thisHub.datam.isSortAsc();
        return b;
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
        String s = thisHub.datam.getSeqProperty();
        return s;
    }
}


