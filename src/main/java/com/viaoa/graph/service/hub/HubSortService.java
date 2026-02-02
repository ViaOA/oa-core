package com.viaoa.graph.service.hub;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.logging.Logger;

import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.hub.*;
import com.viaoa.object.OASiblingHelper;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OARemoteThreadService;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.util.OAComparator;
import com.viaoa.util.OAString;

public class HubSortService {
	private final Logger LOG = Logger.getLogger(HubSortService.class.getName());

	private final OAObjectService srvcObject;
	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;

	public HubSortService(OAObjectService srvcObject, HubService srvcHub, Hub.FriendAccess faHub) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
		if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
		this.srvcHub = srvcHub;
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
    public void sort(Hub thisHub, String propertyPaths, boolean bAscending, Comparator comp) {
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
    public void sort(Hub thisHub, String propertyPaths, boolean bAscending) {
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
    public void sort(Hub thisHub, String propertyPaths, boolean bAscending, Comparator comp, boolean bAlreadySortedAndLocalOnly) {
        if (thisHub == null) return;
        boolean b = false;
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        try {
            srvcOAThreadLocal.lock(thisHub);
            b = _sort(thisHub, propertyPaths, bAscending, comp, bAlreadySortedAndLocalOnly);
        }
        finally {
            srvcOAThreadLocal.unlock(thisHub);
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
    public HubSortListener getSortListener(Hub thisHub) {
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
    private boolean _sort(Hub thisHub, String propertyPaths, final boolean bAscending, Comparator comp, boolean bAlreadySortedAndLocalOnly) {
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
        srvcOARemoteThread.startNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message

        if (comp != null && !(comp instanceof Serializable)) {
            if (faHub.getHubDataMaster(thisHub).getMasterObject() != null) { 
                throw new RuntimeException("comparator is not Serializable");
            }
        }
        
        boolean bSame = false;
        HubSortListener hsl = faHub.getHubData(thisHub).getSortListener();
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
            	srvcHub.getHubCSService().sort(thisHub, propertyPaths, bAscending, comp);
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
	public void resort(Hub thisHub) {
		sort(thisHub);
	}
	
	/**
	 * Re-sorts {@code thisHub} using previously stored sort parameters.
	 * Loads all data if needed, performs the sort, and fires sort events.
	 *
	 * @param thisHub the Hub to sort
	 */
	public void sort(Hub thisHub) {
        if (thisHub == null) return;

		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        try {
            srvcOAThreadLocal.lock(thisHub);
            _performSort(thisHub);
        }
        finally {
            srvcOAThreadLocal.unlock(thisHub);
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
    private void _performSort(Hub thisHub) {
        OASiblingHelper siblingHelper = new OASiblingHelper(thisHub);
        siblingHelper.setUseSameThread(true);
        HubSortListener hsl = faHub.getHubData(thisHub).getSortListener();
        if (hsl != null) {
            String[] props = hsl.getPropeties();
            if (props != null) {
                for (String p : props) {
                    siblingHelper.add(p);
                }
            }
        }        
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        try {
            srvcOAThreadLocal.addSiblingHelper(siblingHelper);
            _performSortX(thisHub);
        }
        finally {
            srvcOAThreadLocal.removeSiblingHelper(siblingHelper);
        }
    }
	
    /**
     * Executes the actual sorting of the Hub's underlying vector using its
     * active HubSortListener comparator. Retries several times to tolerate
     * concurrent modifications.
     *
     * @param thisHub the Hub whose contents are sorted
     */
	private void _performSortX(Hub thisHub) {
		if (faHub.getHubData(thisHub).getSortListener() == null) return;
		srvcHub.getHubSelectService().loadAllData(thisHub);
	    faHub.getHubData(thisHub).incrementChangeCount();
	    
	    for (int i=0; i<5; i++) {
	        try {
    	        Collections.sort(faHub.getHubData(thisHub).getVector(), faHub.getHubData(thisHub).getSortListener().getComparator());
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
    private void afterPerformSort(Hub thisHub) {
    	srvcHub.getHubEventService().fireAfterSortEvent(thisHub);
    }
	
    /**
     * Cancels any existing sort on {@code thisHub}. If the Hub is currently
     * kept sorted, invokes the sort method with null parameters to reset
     * sort state.
     *
     * @param thisHub the Hub whose sort state is being cancelled
     */
	public void cancelSort(Hub thisHub) {
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
	public boolean isSorted(Hub thisHub) {
        return (faHub.getHubData(thisHub).getSortListener() != null);
    }

	/**
	 * Returns the property-path(s) used for sorting {@code thisHub}, checking
	 * current Hub data first and falling back to master data if required.
	 *
	 * @param thisHub the Hub to inspect
	 * @return the configured sort property path(s), or null
	 */
    public String getSortProperty(Hub thisHub) {
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
    public boolean getSortAsc(Hub thisHub) {
        boolean b = faHub.getHubData(thisHub).isSortAsc();
        b = b || faHub.getHubDataMaster(thisHub).isSortAsc();
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
    public String getSeqProperty(Hub thisHub) {
        String s = faHub.getHubDataMaster(thisHub).getSeqProperty();
        return s;
    }

}
