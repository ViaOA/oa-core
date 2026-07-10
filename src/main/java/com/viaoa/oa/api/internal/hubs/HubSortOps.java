package com.viaoa.oa.api.internal.hubs;

import java.util.Comparator;

import com.viaoa.hub.Hub;
import com.viaoa.hub.sort.HubSortListener;

/**
 * Internal sorting operations for Hubs.
 */
public interface HubSortOps {

	/**
	 * Returns the sort listener for a Hub.
	 *
	 * @param hub the Hub to inspect
	 * @return the sort listener, or {@code null}
	 */
 	public HubSortListener getSortListener(Hub<?> hub);
	/**
	 * Sorts a Hub by property paths or comparator.
	 *
	 * @param hub the Hub to sort
	 * @param paths property paths used for sorting
	 * @param bAscending {@code true} for ascending order
	 * @param comp optional comparator
	 */
	public void sort(Hub<?> hub, String paths, boolean bAscending, Comparator<?> comp);
	/**
	 * Returns whether a Hub is sorted.
	 *
	 * @param hub the Hub to inspect
	 * @return {@code true} if sorted
	 */
	public boolean isSorted(Hub<?> hub);
	/**
	 * Cancels Hub sorting.
	 *
	 * @param hub the Hub to update
	 */
	public void cancelSort(Hub<?> hub);
	/**
	 * Sorts a Hub using its configured sort settings.
	 *
	 * @param hub the Hub to sort
	 */
	public void sort(Hub<?> hub);
	/**
	 * Reapplies Hub sorting.
	 *
	 * @param hub the Hub to resort
	 */
	public void resort(Hub<?> hub);

}
