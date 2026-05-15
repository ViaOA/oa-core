package com.viaoa.graph.service.hub;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.logging.Logger;

import com.viaoa.compare.OAComparator;
import com.viaoa.graph.sibling.OASiblingHelper;
import com.viaoa.hub.*;
import com.viaoa.hub.sort.HubSortListener;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.thread.OAThread;

/*qqqqqqq
CODEX

#9 — invariant risk
  file/class/method: src/main/java/com/viaoa/graph/service/hub/HubSortService.java:648, _performSortX
  exact concern: Sort retries ConcurrentModificationException up to 25 times, then silently exits and afterSort
  still fires.
  why it matters: Listeners and callers can observe “sort complete” while the Hub remains unsorted. That violates
  event semantics.
  severity: invariant risk
  minimal fix: Track sort success; if all retries fail, throw or suppress afterSort and leave an explicit failure
  path.
  suggested invariant ID/name: HUB-SORT-ORDER-001: afterSort fires only after a completed sort
  suggested test coverage: Comparator/listener-induced concurrent modification during sort.



*/

public abstract class HubSortService {
	private final Logger LOG = Logger.getLogger(HubSortService.class.getName());

	private final Hub.FriendAccess faHub;

	public HubSortService(Hub.FriendAccess faHub) {
		if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
		this.faHub = faHub;
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
    public void sort(Hub<?> thisHub, String propertyPaths, boolean bAscending, Comparator<?> comp) {
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
    public void sort(Hub<?> thisHub, String propertyPaths, boolean bAscending) {
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
    public void sort(Hub<?> thisHub, String propertyPaths, boolean bAscending, Comparator<?> comp, boolean bAlreadySortedAndLocalOnly) {
        if (thisHub == null) return;
        boolean b = false;
        try {
            callThreadLocalLock(thisHub);
            b = _sort(thisHub, propertyPaths, bAscending, comp, bAlreadySortedAndLocalOnly);
        }
        finally {
            callThreadLocalUnlock(thisHub);
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
    public <T extends OAObject> HubSortListener<T> getSortListener(Hub<T> thisHub) {
        if (thisHub == null) return null;
        return faHub.getHubData(thisHub).getSortListener();
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
    private <T extends OAObject> boolean _sort(Hub<T> thisHub, String propertyPaths, final boolean bAscending, Comparator<?> comp, boolean bAlreadySortedAndLocalOnly) {
        callRemoteThreadStartNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message

        if (comp != null && !(comp instanceof Serializable)) {
            if (faHub.getHubDataMaster(thisHub).getMasterObject() != null) { 
                throw new RuntimeException("comparator is not Serializable");
            }
        }
        
        boolean bSame = false;
        HubSortListener<T> hsl = faHub.getHubData(thisHub).getSortListener();
        if (OAString.isEqual(propertyPaths, faHub.getHubData(thisHub).getSortProperty(),true)) {
            if (bAscending == faHub.getHubData(thisHub).isSortAsc()) {
                bSame = true;
            }
        }
        
        if (hsl != null) {
            if (bSame) {
                // make sure that comparator is same
                if (hsl.getComparator() == null) return false;
                if (hsl.getComparator() instanceof OAComparator) {
                    OAComparator compx = (OAComparator) hsl.getComparator();
                    if (OAString.isEqual(propertyPaths, compx.getPropertyPaths(),true)) {
                        if (bAscending == compx.getAsc()) {
                            return false;
                        }
                    }
                }
                bSame = false;
            }
            hsl.close();
            faHub.getHubData(thisHub).setSortListener(null);
        }
        else {
            if (bSame) {
                if (OAString.isEmpty(propertyPaths) && comp == null) return false;
            }
        }
        
        faHub.getHubData(thisHub).setSortProperty(propertyPaths);
        faHub.getHubData(thisHub).setSortAsc(bAscending);
        
        if (propertyPaths != null || comp != null) {
            faHub.getHubData(thisHub).setSortListener(new HubSortListener(thisHub, comp, propertyPaths, bAscending));
            if (!bAlreadySortedAndLocalOnly) _performSort(thisHub);
        }
        else { // cancel sort
            faHub.getHubData(thisHub).setSortAsc(true);
        }
        
        if (!bAlreadySortedAndLocalOnly) {  // otherwise, no other client has this hub yet
            if (faHub.getHubDataMaster(thisHub).getMasterObject() != null) {
                // 20171028 need to send if sort is cancelled
                //was: if (propertyPaths != null || comp != null) { // otherwise it was a cancel
            	callHubCSSort(thisHub, propertyPaths, bAscending, comp);
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
	public <T extends OAObject> void resort(Hub<T> thisHub) {
		sort(thisHub);
	}
	
	/**
	 * Re-sorts {@code thisHub} using previously stored sort parameters.
	 * Loads all data if needed, performs the sort, and fires sort events.
	 *
	 * @param thisHub the Hub to sort
	 */
	public <T extends OAObject> void sort(Hub<T> thisHub) {
        if (thisHub == null) return;

        try {
            callThreadLocalLock(thisHub);
            _performSort(thisHub);
        }
        finally {
            callThreadLocalUnlock(thisHub);
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
    private <T extends OAObject> void _performSort(Hub<T> thisHub) {
        OASiblingHelper<T> siblingHelper = new OASiblingHelper<>(thisHub);
        siblingHelper.setUseSameThread(true);
        HubSortListener<T> hsl = faHub.getHubData(thisHub).getSortListener();
        if (hsl != null) {
            String[] props = hsl.getPropeties();
            if (props != null) {
                for (String p : props) {
                    siblingHelper.add(p);
                }
            }
        }        
        try {
            callThreadLocalAddSiblingHelper(siblingHelper);
            _performSortX(thisHub);
        }
        finally {
            callThreadLocalRemoveSiblingHelper(siblingHelper);
        }
    }
	
    /**
     * Executes the actual sorting of the Hub's underlying vector using its
     * active HubSortListener comparator. Retries several times to tolerate
     * concurrent modifications.
     *
     * @param thisHub the Hub whose contents are sorted
     */
    @SuppressWarnings("unchecked")
	private <T extends OAObject> void _performSortX(Hub<T> thisHub) {
		if (faHub.getHubData(thisHub).getSortListener() == null) return;
		callHubSelectLoadAllData(thisHub);
		
		final HubData<T> hd = faHub.getHubData(thisHub);
	    hd.incrementChangeCount();
	    
	    ConcurrentModificationException ex = null;
	    for (int i=0; i<25; i++) {
	    	ex = null;
	        try {
    	        Collections.sort(hd.getVector(), hd.getSortListener().getComparator());
    	        break;
	        }
	        catch (ConcurrentModificationException e) {
	        	ex = e;
	        	OAThread.delay(1);
	        }
	    }
	    if (ex != null) throw ex;
	}
	
	/**
	 * Fires the post-sort event notifying listeners that sorting has completed.
	 *
	 * @param thisHub the Hub that was sorted
	 */
    private void afterPerformSort(Hub<?> thisHub) {
    	callHubEventFireAfterSortEvent(thisHub);
    }
	
    /**
     * Cancels any existing sort on {@code thisHub}. If the Hub is currently
     * kept sorted, invokes the sort method with null parameters to reset
     * sort state.
     *
     * @param thisHub the Hub whose sort state is being cancelled
     */
	public void cancelSort(Hub<?> thisHub) {
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
	public void keepSorted(Hub thisHub) {
	    // 20090801 cant have sorter if a AutoSequence is being used
	    if (faHub.getHubData(thisHub).getAutoSequence() != null) {
	        return;
	    }
	    if (faHub.getHubData(thisHub).getSortListener() != null) return;
	    if (srvcHub.getHubSelectService().getSelect(thisHub) == null) return;
	    String s = srvcHub.getHubSelectService().getSelect(thisHub).getOrder();
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
	public boolean isSorted(Hub<?> thisHub) {
        return (faHub.getHubData(thisHub).getSortListener() != null);
    }

	/**
	 * Returns the property-path(s) used for sorting {@code thisHub}, checking
	 * current Hub data first and falling back to master data if required.
	 *
	 * @param thisHub the Hub to inspect
	 * @return the configured sort property path(s), or null
	 */
    public String getSortProperty(Hub<?> thisHub) {
        String s = faHub.getHubData(thisHub).getSortProperty();
        if (s == null) s = faHub.getHubDataMaster(thisHub).getSortProperty();
        return s;
    }

    /**
     * Returns whether {@code thisHub} is sorted in ascending order.
     * Evaluates local Hub data first, then master data.
     *
     * @param thisHub the Hub to inspect
     * @return true if ascending, otherwise false
     */
    public <T extends OAObject> boolean getSortAsc(Hub<T> thisHub) {
    	if (thisHub == null) return false;
		final HubData<T> hd = faHub.getHubData(thisHub);
        boolean b = hd != null && hd.isSortAsc();
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
    public String getSeqProperty(Hub<?> thisHub) {
    	if (thisHub == null) return null;
        String s = faHub.getHubDataMaster(thisHub).getSeqProperty();
        return s;
    }

	public abstract void callHubCSSort(Hub<?> thisHub, String propertyPaths, boolean bAscending, Comparator<?> comp);
	public abstract void callHubSelectLoadAllData(Hub<?> thisHub);
	public abstract void callHubEventFireAfterSortEvent(Hub<?> thisHub);
	public abstract void callThreadLocalLock(Object object);
	public abstract void callThreadLocalUnlock(Object object);
	public abstract void callRemoteThreadStartNextThread();
	public abstract boolean callThreadLocalAddSiblingHelper(OASiblingHelper<?> sh);
	public abstract void callThreadLocalRemoveSiblingHelper(OASiblingHelper<?> sh);
    
}



