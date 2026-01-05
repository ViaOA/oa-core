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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASync;

/**
 * Core delegate for internal Hub data operations.
 * <p>
 * Directly manipulates the underlying {@code HubData.vector} and change-tracking
 * structures. Provides thread-safe add/remove/insert operations and maintains
 * Hub-level change counters.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Manage object storage within the Hub’s {@code Vector}.</li>
 *   <li>Track added and removed objects for persistence and synchronization.</li>
 *   <li>Coordinate with {@link HubCSDelegate} and {@link HubAddRemoveDelegate}
 *       for client/server propagation.</li>
 *   <li>Maintain Hub change counters and dirty-state flags.</li>
 * </ul>
 *
 * <p>Called by most other Hub delegates; low-level changes should only be made
 * through these internal APIs to guarantee event and cache consistency.
 */
public class HubDataDelegate {
	
    private static Logger LOG = Logger.getLogger(HubDataDelegate.class.getName());
    
	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubDataService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubDataService().?(?);
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
     * Clears all internal Hub data structures and resets tracking state.
     * Removes all elements from the Hub’s vector and clears add/remove
     * tracking lists. Drops the {@code hubDatax} extension if it is no
     * longer needed, resets the changed flag, and increments the change
     * counter.
     *
     * @param thisHub the hub whose internal state is being reset
     */
	public static void clearAllAndReset(Hub thisHub) {
		//qqqqqq method was protected
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService().clearAllAndReset(thisHub);
	}
	
	/**
	 * Ensures that the Hub’s underlying vector has capacity for at least
	 * the specified number of elements.
	 *
	 * @param thisHub the hub whose vector capacity is being checked
	 * @param size    the minimum capacity required
	 */
	public static void ensureCapacity(Hub thisHub, int size) {
		//qqqqqqqqqqq method was protected
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService().ensureCapacity(thisHub, size);
	}
	
	/**
	 * Trims the Hub’s underlying vector to its current size. No-op when
	 * serialization is in progress and the vector is null.
	 *
	 * @param thisHub the hub whose vector should be trimmed
	 */
	public static void resizeToFit(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService().resizeToFit(thisHub);
	}

	/**
	 * Updates the Hub’s changed flag and increments its change counter
	 * when the value transitions. Clearing the changed flag also clears
	 * tracked add/remove lists. When marking as changed, the master
	 * object may also be marked changed based on link metadata.
	 *
	 * @param thisHub   the hub whose changed state is being updated
	 * @param bChanged  the new changed value
	 */
	public static void setChanged(Hub thisHub, boolean bChanged) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService().setChanged(thisHub, bChanged);
    }
	
	/**
	 * Clears the Hub’s add/remove tracking lists and resets the changed
	 * flag when those lists become empty. Drops {@code hubDatax} if it is
	 * no longer required. Sends a remote clear-changes event when any
	 * tracked changes existed.
	 *
	 * @param thisHub the hub whose change tracking is being cleared
	 */
	public static void clearHubChanges(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService().clearHubChanges(thisHub);
	}
	
	/**
	 * Copies all elements from the Hub’s vector into the supplied array.
	 * The copy is performed under synchronization for thread safety.
	 *
	 * @param thisHub the hub whose elements are being copied
	 * @param anArray the destination array
	 */
    protected static void copyInto(Hub thisHub, Object anArray[]) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService().copyInto(thisHub, anArray);
    }

    /**
     * Returns an array containing all elements in the Hub. Retries the
     * copy operation if the Hub’s contents change during the attempt.
     * Ensures that the Hub’s size is known before copying.
     *
     * @param thisHub the hub whose elements are being converted to an array
     * @return an array containing the Hub’s elements
     */
	public static Object[] toArray(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDataService().toArray(thisHub);
	}

	/**
	 * Returns the current number of elements in the Hub.
	 *
	 * @param thisHub the hub whose size is being retrieved
	 * @return the number of elements in the Hub
	 */
    public static int getCurrentSize(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubDataService().getCurrentSize(thisHub);
    }
	

    /**
     * Copies the underlying vector from one Hub to another as part of
     * cloning operations.
     *
     * @param thisHub the source hub
     * @param newHub  the destination hub
     */
    public static void _clone(Hub thisHub, Hub newHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService()._clone(thisHub, newHub);
    }
    
    /**
     * Removes an object from the Hub while managing locking and remote
     * thread coordination. Delegates to the internal removal method and
     * returns the removed position unless removing all elements.
     *
     * @param thisHub        the hub from which the object is removed
     * @param obj            the object being removed
     * @param bDeleting      whether the object is being permanently deleted
     * @param bIsRemovingAll whether the entire Hub is being cleared
     * @return the position from which the object was removed, or -1
     */
    public static int _remove(Hub thisHub, Object obj, boolean bDeleting, boolean bIsRemovingAll) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubDataService()._remove(thisHub, obj, bDeleting, bIsRemovingAll);
    }
    
    /**
     * Adds an object to the Hub's vector, optionally acquiring a lock
     * unless already held. Delegates to the internal add method and
     * triggers remote thread processing. Returns whether the object
     * was added.
     *
     * @param thisHub        the hub receiving the object
     * @param obj            the object to add
     * @param bHasLock       whether the calling thread already holds the lock
     * @param bCheckContains whether to skip the add if the object is already present
     * @return {@code true} if the object was added; otherwise {@code false}
     */
    public static boolean _add(Hub thisHub, Object obj, boolean bHasLock, boolean bCheckContains) {
    	//qqqqqqqq method was protected
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDataService()._add(thisHub, obj, bHasLock, bCheckContains);
    }
    
    /**
     * Inserts an object at the specified position in the Hub's vector,
     * optionally acquiring a lock when not already held. Delegates to
     * the internal insert method and triggers remote thread processing.
     *
     * @param thisHub   the hub receiving the insertion
     * @param obj       the object to insert
     * @param pos       the index at which to insert the object
     * @param bIsLocked whether the calling thread already holds the lock
     * @return {@code true} if the object was inserted
     */
    public static boolean _insert(Hub thisHub, Object obj, int pos, boolean bIsLocked) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDataService()._insert(thisHub, obj, pos, bIsLocked);
    }
   
	/**
	 * Moves an object within the Hub from one index to another.
	 * Performs the vector operations under lock, increments the
	 * change counter, and triggers remote thread processing.
	 *
	 * @param thisHub the hub containing the object
	 * @param obj     the object to move
	 * @param posFrom the original index
	 * @param posTo   the destination index
	 */
	public static void _move(Hub thisHub, Object obj, int posFrom, int posTo) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService()._move(thisHub, obj, posFrom, posTo);
	}
	
	/**
	 * Adds all objects currently in the Hub to the add-tracking vector,
	 * creating the vector when necessary. No-op when the hub is null.
	 *
	 * @param thisHub the hub whose contents are being tracked as added
	 */
	public static void addAllToAddVector(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService().addAllToAddVector(thisHub);
	}
	
	/**
	 * Creates the Hub’s add-tracking vector if it does not already exist.
	 * Ensures thread-safe initialization using synchronization.
	 *
	 * @param thisHub the hub whose add-tracking vector is being created
	 * @return the add-tracking vector
	 */
	protected static Vector createVecAdd(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDataService().createVecAdd(thisHub);
	}
	
	/**
	 * Creates the Hub’s remove-tracking vector if it does not already exist.
	 * Ensures thread-safe initialization using synchronization.
	 *
	 * @param thisHub the hub whose remove-tracking vector is being created
	 * @return the remove-tracking vector
	 */
	public static Vector createVecRemove(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDataService().createVecRemove(thisHub);
	}
	
	/**
	 * Returns an array of objects that have been added to the Hub but not yet cleared
	 * from the Hub’s change-tracking “added” list.
	 *
	 * <p>The method checks the Hub’s internal {@code vecAdd} list and, if it contains
	 * entries, copies them into a new {@code OAObject[]} array under synchronization.</p>
	 *
	 * @param thisHub the Hub whose added-object list is being queried
	 * @return an array of added {@link OAObject} instances, or {@code null} if none
	 *         have been recorded
	 */
	public static OAObject[] getAddedObjects(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDataService().getAddedObjects(thisHub);
	}

	/**
	 * Returns an array of objects that have been removed from the Hub but not yet
	 * cleared from the Hub’s change-tracking “removed” list.
	 *
	 * <p>The method checks the Hub’s internal {@code vecRemove} list and, if it
	 * contains entries, copies them into a new {@code OAObject[]} array under
	 * synchronization.</p>
	 *
	 * @param thisHub the Hub whose removed-object list is being queried
	 * @return an array of removed {@link OAObject} instances, or {@code null} if
	 *         none have been recorded
	 */
	public static OAObject[] getRemovedObjects(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDataService().getRemovedObjects(thisHub);
	}

	/**
	 * Returns whether the Hub is marked as changed.
	 *
	 * @param thisHub the Hub to check
	 * @return {@code true} if the Hub is marked as changed, otherwise {@code false}
	 */
	public static boolean getChanged(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDataService().getChanged(thisHub);
	}
	
	/**
	 * Returns the object in the Hub matching the supplied key.
	 *
	 * <p>The key is converted to an {@link OAObjectKey} if needed, and the Hub is
	 * searched sequentially. If the Hub supports loading additional data, more
	 * records are fetched until a match is found or no more records are available.</p>
	 *
	 * @param thisHub the Hub to search
	 * @param key the key or object used to identify the desired element
	 * @return the matching object, or {@code null} if not found
	 */
	public static Object getObject(final Hub thisHub, Object key) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDataService().getObject(thisHub, key);
	}
	
	/**
	 * Retrieves the object at the specified position within the Hub.
	 *
	 * <p>If the object is an {@link OAObjectKey}, it is resolved to its underlying
	 * {@link OAObject}. If the Hub supports incremental loading, additional data
	 * may be fetched to satisfy the request.</p>
	 *
	 * @param thisHub the Hub containing the data
	 * @param pos the index to retrieve
	 * @return the object at the position, or {@code null} if not available
	 */
	public static Object getObjectAt(Hub thisHub, int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDataService().getObjectAt(thisHub, pos);
	}
	
	/**
	 * Finds the position of the given object within the Hub.
	 *
	 * <p>If not present, optional master adjustment and recursive-hub logic may be
	 * applied to locate and reposition the Hub within a recursive hierarchy.</p>
	 *
	 * @param thisHub the Hub to search
	 * @param object the object to locate
	 * @param adjustMaster whether to adjust the master Hub relationship if needed
	 * @param bUpdateLink whether to update link relationships during the search
	 * @return the position of the object, or {@code -1} if not found
	 */
	public static int getPos(final Hub thisHub, Object object, final boolean adjustMaster, final boolean bUpdateLink) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubDataService().getPos(thisHub, object, adjustMaster, bUpdateLink);
	}
	
    /**
     * Removes the given object from the Hub’s change-tracking “added” list.
     *
     * <p>Also updates the Hub’s change flag if the removal results in no remaining
     * tracked additions or removals.</p>
     *
     * @param thisHub the Hub whose added list is modified
     * @param obj the object to remove from the added list
     */
	public static void removeFromAddedList(Hub thisHub, Object obj) {
		//qqqqq method was protected
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService().removeFromAddedList(thisHub, obj);
	}
	
	/**
	 * Removes the given object from the Hub’s change-tracking “removed” list.
	 *
	 * <p>Also updates the Hub’s change flag if the removal results in no remaining
	 * tracked additions or removals.</p>
	 *
	 * @param thisHub the Hub whose removed list is modified
	 * @param obj the object to remove from the removed list
	 */
	public static void removeFromRemovedList(Hub thisHub, Object obj) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService().removeFromRemovedList(thisHub, obj);
	}

	/**
	 * Returns the Hub’s change counter value.
	 *
	 * @param thisHub the Hub whose change count is requested
	 * @return the current change count
	 */
	public static int getChangeCount(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubDataService().getChangeCount(thisHub);
	}
	
	/**
	 * Increments the Hub’s change counter.
	 *
	 * @param thisHub the Hub whose counter is incremented
	 */
	public static void incChangeCount(Hub thisHub) {
		//qqqqqqq metod was protected
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService().incChangeCount(thisHub);
	}

	/**
	 * Determines whether the Hub contains the specified object.
	 *
	 * <p>Equivalent to calling {@link #contains(Hub, Object, boolean)} with
	 * {@code false} for the {@code bJustAdded} flag.</p>
	 *
	 * @param hub the Hub to search
	 * @param obj the object to look for
	 * @return {@code true} if the Hub contains the object, otherwise {@code false}
	 */
	public static boolean contains(Hub hub, Object obj) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return false;
		return g.hubs().getHubDataService().contains(hub, obj);
	}
	
	/**
	 * Determines whether the Hub contains the specified object, with an option to
	 * restrict the check to recently added objects.
	 *
	 * <p>This method uses direct lookup, key resolution, or Hub-level OAObject
	 * presence checks depending on the Hub configuration and object type.</p>
	 *
	 * @param hub the Hub to search
	 * @param obj the object to check for
	 * @param bJustAdded if {@code true}, only checks the most recently added items
	 * @return {@code true} if the Hub contains the object, otherwise {@code false}
	 */
	public static boolean contains(Hub hub, Object obj, final boolean bJustAdded) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return false;
		return g.hubs().getHubDataService().contains(hub, obj, bJustAdded);
    }
	
	/**
	 * Performs a direct lookup to determine whether the Hub contains the object.
	 *
	 * <p>For large Hubs with a sort listener, a quicksort-based search may be used.</p>
	 *
	 * @param hub the Hub to search
	 * @param obj the object to check for
	 * @return {@code true} if the object is present, otherwise {@code false}
	 */
    public static boolean containsDirect(Hub hub, Object obj) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return false;
		return g.hubs().getHubDataService().containsDirect(hub, obj);
    }
    
    /**
     * Determines whether the Hub participates in a recursive link structure.
     *
     * @param thisHub the Hub to check
     * @return {@code true} if this Hub is used recursively, otherwise {@code false}
     */
    public static boolean isHubBeingUsedAsRecursive(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDataService().isHubBeingUsedAsRecursive(thisHub);
    }
    
    /**
     * Sets the Hub’s track-changes flag.
     *
     * @param thisHub the Hub to update
     * @param b the new track-changes state
     */
    public static void setTrackChanges(Hub thisHub, boolean b) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDataService().setTrackChanges(thisHub, b);
    }

    /**
     * Returns the Hub’s track-changes flag.
     *
     * @param thisHub the Hub to query
     * @return {@code true} if track changes is enabled, otherwise {@code false}
     */
    public static boolean getTrackChanges(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDataService().getTrackChanges(thisHub);
    }
    
    /**
     * Returns whether the Hub is in a load-all-data state.
     *
     * @param thisHub the Hub to check
     * @return {@code true} if loading all data, otherwise {@code false}
     */
    public static boolean isLoadingAllData(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDataService().isLoadingAllData(thisHub);
    }
    
    /**
     * Sets the Hub’s load-all-data flag.
     *
     * @param thisHub the Hub to update
     * @param b the new load-all-data state
     * @return the previous state
     */
    public static boolean setLoadingAllData(Hub thisHub, boolean b) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDataService().setLoadingAllData(thisHub, b);
    }
    
    /**
     * Sets the Hub’s load-all-data flag and associates it with a thread.
     *
     * @param thisHub the Hub to update
     * @param b the new state
     * @param thread the thread to associate with the load-all flag
     * @return the previous state
     */
    public static boolean setLoadingAllData(Hub thisHub, boolean b, Thread thread) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDataService().setLoadingAllData(thisHub, b, thread);
    }
}
