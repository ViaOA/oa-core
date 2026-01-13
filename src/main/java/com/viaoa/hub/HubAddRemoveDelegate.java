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
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.*;
import com.viaoa.remote.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAString;

/**
 * Implements object add/remove operations for a {@link Hub}.
 * <p>
 * Handles insertion, deletion, and replacement with strict sequencing of
 * before/after events. Enforces integrity rules such as uniqueness,
 * master-detail consistency, and synchronization with shared or linked Hubs.
 *
 * <p><b>Key functions</b>
 * <ul>
 *   <li>Add or insert objects with duplicate and type checks.</li>
 *   <li>Remove objects while ensuring correct event propagation.</li>
 *   <li>Maintain {@code HubData} vector integrity and trigger cascade updates.</li>
 * </ul>
 */
public class HubAddRemoveDelegate {

	private static Logger LOG = Logger.getLogger(HubAddRemoveDelegate.class.getName());

	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubAddRemoveService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubAddRemoveService().?(?);
    */
	static OAGraph getGraph(Hub hub, Object obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	
	/**
	 * Removes the specified object from the hub using default options for force,
	 * event sending, deletion mode, active-object updates, master-reference updates,
	 * and remove-all behavior. Delegates to the full remove implementation.
	 *
	 * @param thisHub the hub from which the object will be removed
	 * @param obj     the object to remove
	 * @return {@code true} if the object was removed, otherwise {@code false}
	 */
	public static boolean remove(final Hub thisHub, final Object obj) {
		OAGraph g = getGraph(thisHub, obj);
		if (g == null) return false;
		return g.hubs().getHubAddRemoveService().remove(thisHub, obj);
	}

	/**
	 * Removes the object at the specified position using default force behavior.
	 * Delegates to the internal positional remove method.
	 *
	 * @param thisHub the hub containing the object
	 * @param pos     the position of the object to remove
	 * @return the removed object, or {@code null} if removal failed
	 */
	public static Object remove(final Hub thisHub, final int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubAddRemoveService().remove(thisHub, pos);
	}

	/**
	 * Removes the object at the specified position. Retrieves the object and
	 * delegates to the main remove implementation. Returns {@code null} if the
	 * object could not be removed.
	 *
	 * @param thisHub the hub containing the object
	 * @param pos     the position of the object
	 * @param bForce  whether to force removal
	 * @return the removed object, or {@code null} if removal failed
	 */
	protected static Object remove(final Hub thisHub, final int pos, final boolean bForce) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubAddRemoveService().remove(thisHub, pos, bForce);
	}

	/**
	 * Removes an object from the hub with full control over removal behavior.
	 * Performs validation, event notifications, server messaging, vector updates,
	 * reference cleanup, and master/detail property adjustments.
	 *
	 * @param thisHub         the hub from which the object will be removed
	 * @param obj             the object to remove
	 * @param bForce          whether to force removal
	 * @param bSendEvent      whether to send before/after remove events
	 * @param bDeleting       whether the removal is part of a delete operation
	 * @param bSetAO          whether to update active-object references
	 * @param bSetPropToMaster whether to clear master/detail references
	 * @param bIsRemovingAll  whether this is part of a bulk remove/all operation
	 * @return {@code true} if the object was removed, otherwise {@code false}
	 */
	public static boolean remove(final Hub thisHub, Object obj, final boolean bForce,
			final boolean bSendEvent, final boolean bDeleting, final boolean bSetAO,
			final boolean bSetPropToMaster, final boolean bIsRemovingAll) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubAddRemoveService().remove(thisHub, obj, bForce, 
				bSendEvent, bDeleting, bSetAO, 
				bSetPropToMaster, bIsRemovingAll);
	}

	/**
	 * Determines whether the specified object can be removed from the hub and
	 * returns a descriptive message if removal is not allowed.
	 *
	 * @param thisHub   the hub being evaluated
	 * @param obj       the object to check, or {@code null} to evaluate hub state
	 * @param checkType the callback check type
	 * @return a message describing why removal is not allowed, or {@code null} if allowed
	 */
	public static String getCantRemoveMessage(final Hub thisHub, final Object obj, final int checkType) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubAddRemoveService().getCantRemoveMessage(thisHub, obj, checkType);
	}

	/**
	 * Determines whether all objects can be removed from the hub and returns a
	 * descriptive message if removal is not permitted.
	 *
	 * @param thisHub   the hub being evaluated
	 * @param checkType the callback check type
	 * @return a message describing why remove-all is not allowed, or {@code null} if allowed
	 */
	public static String getCantRemoveAllMessage(final Hub thisHub, final int checkType) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubAddRemoveService().getCantRemoveAllMessage(thisHub, checkType);
	}

	/**
	 * Clears all objects from the hub using default options for resetting the
	 * active object and sending a new-list event. Delegates to the full clear method.
	 *
	 * @param thisHub the hub to clear
	 */
	public static void clear(final Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAddRemoveService().clear(thisHub);
	}

	/**
	 * Clears all objects from the hub. Performs callback checks, locking, event
	 * notifications, removal operations, and active-object updates.
	 *
	 * @param thisHub       the hub to clear
	 * @param bSetAOtoNull  whether to set the active object to {@code null}
	 * @param bSendNewList  whether to fire a new-list event
	 */
	public static void clear(final Hub thisHub, final boolean bSetAOtoNull, final boolean bSendNewList) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAddRemoveService().clear(thisHub, bSetAOtoNull, bSendNewList);
	}

	/**
	 * Determines whether the specified object can be added to the hub.
	 * Delegates to {@link #canAddMsg(Hub, Object)}.
	 *
	 * @param thisHub the hub to evaluate
	 * @param obj     the object to test
	 * @return {@code true} if the object can be added, otherwise {@code false}
	 */
	public static boolean canAdd(final Hub thisHub, final Object obj) {
		OAGraph g = getGraph(thisHub, obj);
		if (g == null) return false;
		return g.hubs().getHubAddRemoveService().canAdd(thisHub, obj);
	}

	/**
	 * Determines whether an object can be added to the hub. Delegates to
	 * {@link #canAddMsg(Hub, Object)} using {@code null}.
	 *
	 * @param thisHub the hub to evaluate
	 * @return {@code true} if adding is allowed, otherwise {@code false}
	 */
	public static boolean canAdd(final Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubAddRemoveService().canAdd(thisHub);
	}

	/**
	 * Returns a message describing why an object cannot be added to the hub.
	 * Delegates to {@link #canAddMsg(Hub, Object)} using {@code null}.
	 *
	 * @param thisHub the hub to evaluate
	 * @return a message describing the restriction, or {@code null} if allowed
	 */
	public static String canAddMsg(final Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubAddRemoveService().canAddMsg(thisHub);
	}

	// returns null if obj can be added; otherwise an error msg is returned.
	/**
	 * Determines whether an object can be added to the hub. Performs enabled,
	 * class, master/detail, uniqueness, callback, and recursion checks.
	 *
	 * @param thisHub the hub to evaluate
	 * @param obj     the object to test
	 * @return {@code null} if adding is allowed, otherwise an error message
	 */
	public static String canAddMsg(final Hub thisHub, final Object obj) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubAddRemoveService().canAddMsg(thisHub, obj);
	}
	
	/**
	 * Adds an object to the hub using default contains-check behavior.
	 * Delegates to {@link #add(Hub, Object, boolean)}.
	 *
	 * @param thisHub the hub receiving the object
	 * @param obj     the object to add
	 * @return {@code true} if the object was added, otherwise {@code false}
	 */
    public static boolean add(final Hub thisHub, final Object obj) {
		OAGraph g = getGraph(thisHub, obj);
		if (g == null) return false;
		return g.hubs().getHubAddRemoveService().add(thisHub, obj);
    }

    /**
     * Adds an object to the hub after performing validation, event dispatch,
     * locking, and link updates. Delegates to the internal add method.
     *
     * @param thisHub                the hub receiving the object
     * @param obj                    the object to add
     * @param bAlreadyCalledContains whether the caller has already checked contains()
     * @return {@code true} if the object was added
     */
    public static boolean add(final Hub thisHub, final Object obj, final boolean bAlreadyCalledContains) {
		OAGraph g = getGraph(thisHub, obj);
		if (g == null) return false;
		return g.hubs().getHubAddRemoveService().add(thisHub, obj, bAlreadyCalledContains);
	}

	/**
	 * Adds the object to the hub's internal vector and registers hub membership
	 * for OAObjects. Does not perform validation or event notifications.
	 *
	 * @param thisHub       the hub receiving the object
	 * @param obj           the object to add
	 * @param bHasLock      whether the caller holds the lock
	 * @param bCheckContains whether to check for existing membership
	 * @return {@code true} if the object was added
	 */
	public static boolean internalAdd(final Hub thisHub, final Object obj, final boolean bHasLock, final boolean bCheckContains) {
		//qqqqqq method was protected
		OAGraph g = getGraph(thisHub, obj);
		if (g == null) return false;
		return g.hubs().getHubAddRemoveService().internalAdd(thisHub, obj, bHasLock, bCheckContains);
	}

	/**
	 * Attempts to reposition an object within a sorted hub up to five times by
	 * retrieving its position and delegating to the move operation.
	 *
	 * @param thisHub the hub containing the object
	 * @param obj     the object to reposition
	 */
	protected static void sortMove(final Hub thisHub, final Object obj) {
		OAGraph g = getGraph(thisHub, obj);
		if (g == null) return;
		g.hubs().getHubAddRemoveService().sortMove(thisHub, obj);
	}

	/**
	 * Moves an object from one position to another within the hub. Adjusts the
	 * target position for sorted hubs, fires before/after move events, and
	 * updates the internal data structure. Sends server notifications when needed.
	 *
	 * @param thisHub the hub containing the object
	 * @param posFrom the original position of the object
	 * @param posTo   the target position for the object
	 */
	protected static void move(final Hub thisHub, final int posFrom, int posTo) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAddRemoveService().move(thisHub, posFrom, posTo);
	}

	/**
	 * Inserts an object into the hub at the specified position. If the hub is
	 * sorted, the object is inserted at the correct sorted position. Performs
	 * validation, locking, internal insertion, and event dispatch.
	 *
	 * @param thisHub the hub receiving the object
	 * @param obj     the object to insert
	 * @param pos     the requested insert position
	 * @return {@code true} if insertion succeeded, otherwise {@code false}
	 */
	public static boolean insert(final Hub thisHub, final Object obj, final int pos) {
		OAGraph g = getGraph(thisHub, obj);
		if (g == null) return false;
		return g.hubs().getHubAddRemoveService().insert(thisHub, obj, pos);
	}

	/**
	 * Swaps the positions of two objects within the hub. Uses the move operation
	 * to reposition each object. No operation is performed if either index is
	 * invalid or no object exists at the given positions.
	 *
	 * @param thisHub the hub containing the objects
	 * @param pos1    the position of the first object
	 * @param pos2    the position of the second object
	 */
	public static void swap(final Hub thisHub, int pos1, int pos2) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAddRemoveService().swap(thisHub, pos1, pos2);
	}

	/**
	 * Retrieves the list of objects tracked as added to the hub.
	 *
	 * @param thisHub the hub to inspect
	 * @return an array of added {@link OAObject} instances
	 */
	public static OAObject[] getAddedObjects(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubAddRemoveService().getAddedObjects(thisHub);
	}

	/**
	 * Retrieves the list of objects tracked as removed from the hub.
	 *
	 * @param thisHub the hub to inspect
	 * @return an array of removed {@link OAObject} instances
	 */
	public static OAObject[] getRemovedObjects(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubAddRemoveService().getRemovedObjects(thisHub);
	}

	/**
	 * Determines whether the hub permits duplicate add/remove operations based on
	 * its configuration.
	 *
	 * @param thisHub the hub to check
	 * @return {@code true} if duplicate add/remove operations are allowed
	 */
	public static boolean isAllowAddRemove(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubAddRemoveService().isAllowAddRemove(thisHub);
	}

	/**
	 * Determines whether objects can be removed from the hub. Considers duplicate
	 * add/remove configuration flags and foreign-key/primary-key constraints
	 * related to master/detail ONE-type links.
	 *
	 * @param thisHub the hub to evaluate
	 * @return {@code true} if removal is permitted
	 */
	public static boolean isAllowRemove(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubAddRemoveService().isAllowRemove(thisHub);
	}

	/**
	 * Adds all objects from the specified list to the hub without performing
	 * validation, event dispatch, or link updates. Directly modifies the hub's
	 * internal vector.
	 *
	 * @param hub  the hub to modify
	 * @param list the objects to add
	 */
	public static void unsafeAddAll(Hub hub, List list) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return;
		g.hubs().getHubAddRemoveService().unsafeAddAll(hub, list);
	}

	/**
	 * Replaces all objects in the hub with those contained in another hub.
	 * Clears internal structures, removes old hub references, adds new objects,
	 * and fires a new-list event.
	 *
	 * @param hub    the hub being updated
	 * @param hubNew the hub providing the new objects
	 */
	public static void refresh(Hub hub, Hub hubNew) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return;
		g.hubs().getHubAddRemoveService().refresh(hub, hubNew);
	}
}
