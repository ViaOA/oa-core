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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASync;
import com.viaoa.util.OAString;

/**
 * Creates and dispatches {@link HubEvent}s for structural and selection
 * operations, routing them through registered {@link HubListener}s.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Construct and fire before/after events for add, remove, clear, and AO changes.</li>
 *   <li>Coordinate verification callbacks using {@code OAObjectCallbackDelegate}.</li>
 *   <li>Support queued dispatch for remote or asynchronous event contexts.</li>
 *   <li>Trigger OAObject triggers and maintain referential updates for master/detail links.</li>
 * </ul>
 *
 * <p>Ensures event ordering and prevents reentrancy issues by maintaining
 * per-thread event stacks in {@code OAThreadLocalDelegate}.
 */
public class HubEventDelegate {

	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubEventService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubEventService().?(?);
    */
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	
	// 20120827 might be used later, if we need to have hub changes notify masterobject
	/**
	 * Placeholder for future support to notify that a Hub's master object
	 * has changed. Currently unused and contains no implementation.
	 *
	 * @param thisHub       the Hub whose master object would be reported
	 * @param bRefreshFlag  whether the change is associated with a refresh
	 */
	protected static void fireMasterObjectChangeEvent(Hub thisHub, boolean bRefreshFlag) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireMasterObjectChangeEvent(thisHub, bRefreshFlag);
	}

	/**
	 * Fires a before-remove event for the specified object and position.
	 * Verifies removal through {@code OAObjectCallbackDelegate} and then
	 * notifies all registered listeners via their {@code beforeRemove} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object being removed
	 * @param pos     the position of the object within the Hub
	 */
	public static void fireBeforeRemoveEvent(Hub thisHub, Object obj, int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireBeforeRemoveEvent(thisHub, obj, pos);
	}

	/**
	 * Fires an after-remove event for the specified object and position.
	 * Supports queued delivery for remote threads and triggers referential
	 * updates and OAObject triggers when appropriate.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the removed object
	 * @param pos     the position the object occupied
	 */
	public static <T> void fireAfterRemoveEvent(Hub<T> thisHub, final T obj, int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireAfterRemoveEvent(thisHub, obj, pos);
	}

	/**
	 * Fires a before-remove-all event for the Hub. Verifies permission via
	 * {@code OAObjectCallbackDelegate} and notifies listeners through their
	 * {@code beforeRemoveAll} method.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public static void fireBeforeRemoveAllEvent(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireBeforeRemoveAllEvent(thisHub);
	}

	/**
	 * Fires an after-remove-all event for the Hub. Supports queued delivery,
	 * notifies listeners through {@code afterRemoveAll}, and triggers master
	 * object onChange processing when applicable.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public static void fireAfterRemoveAllEvent(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireAfterRemoveAllEvent(thisHub);
	}

	/**
	 * Fires a before-add event for an object being added to the Hub. Verifies
	 * the addition through {@code OAObjectCallbackDelegate} and notifies all
	 * listeners via {@code beforeAdd}.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object being added
	 * @param pos     the position at which the object will be added
	 */
	public static void fireBeforeAddEvent(Hub thisHub, Object obj, int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireBeforeAddEvent(thisHub, obj, pos);
	}

	/**
	 * Fires an after-add event for an object added to the Hub. Supports queued
	 * event dispatch, triggers OAObject cache notifications, and processes
	 * master-object triggers when applicable.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the added object
	 * @param pos     the position of the added object
	 */
	public static <T> void fireAfterAddEvent(Hub<T> thisHub, final T obj, int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireAfterAddEvent(thisHub, obj, pos);
	}

	/**
	 * Fires a before-insert event for an object being inserted into the Hub.
	 * Performs callback verification and notifies listeners via
	 * {@code beforeInsert}.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object being inserted
	 * @param pos     the target insertion position
	 */
	public static void fireBeforeInsertEvent(Hub thisHub, Object obj, int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireBeforeInsertEvent(thisHub, obj, pos);
	}

	/**
	 * Fires an after-insert event for an object inserted into the Hub.
	 * Supports queued event dispatch, updates OAObject caches, and triggers
	 * master-object change processing when appropriate.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the inserted object
	 * @param pos     the position of the inserted object
	 */
	public static <T> void fireAfterInsertEvent(Hub<T> thisHub, final T obj, int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireAfterInsertEvent(thisHub, obj, pos);
	}

	/**
	 * Fires an after-change-active-object event for the Hub. Notifies all
	 * applicable listeners and propagates any exceptions encountered.
	 *
	 * @param thisHub    the Hub generating the event
	 * @param obj        the new active object
	 * @param pos        the position associated with the change
	 * @param bAllShared whether shared Hubs should also receive the event
	 */
	public static void fireAfterChangeActiveObjectEvent(Hub thisHub, Object obj, int pos, boolean bAllShared) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireAfterChangeActiveObjectEvent(thisHub, obj, pos, bAllShared);
	}

	/**
	 * Fires a before-refresh event for the Hub. Notifies all registered
	 * listeners via their {@code beforeRefresh} method.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public static void fireBeforeRefreshEvent(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireBeforeRefreshEvent(thisHub);
	}

	/**
	 * Fires a before-select event for the Hub. Notifies all registered
	 * listeners via their {@code beforeSelect} method.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public static void fireBeforeSelectEvent(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireBeforeSelectEvent(thisHub);
	}

	/**
	 * Fires an after-sort event for the Hub. Notifies all registered listeners
	 * via their {@code afterSort} method.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public static void fireAfterSortEvent(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireAfterSortEvent(thisHub);
	}

	/**
	 * Fires a before-delete event for the specified object. Notifies listeners
	 * via their {@code beforeDelete} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object being deleted
	 */
	public static void fireBeforeDeleteEvent(Hub thisHub, Object obj) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireBeforeDeleteEvent(thisHub, obj);
	}

	/**
	 * Fires an after-delete event for the specified object. Supports queued
	 * dispatch when remote threading is active and notifies listeners via
	 * their {@code afterDelete} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the deleted object
	 */
	public static void fireAfterDeleteEvent(Hub thisHub, Object obj) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireAfterDeleteEvent(thisHub, obj);
	}

	/**
	 * Fires a before-save event for the specified object. Notifies listeners
	 * via their {@code beforeSave} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object being saved
	 */
	public static void fireBeforeSaveEvent(Hub thisHub, OAObject obj) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireBeforeSaveEvent(thisHub, obj);
	}

	/**
	 * Fires an after-save event for the specified object. Notifies all
	 * registered listeners via their {@code afterSave} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object that was saved
	 */
	public static void fireAfterSaveEvent(Hub thisHub, OAObject obj) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireAfterSaveEvent(thisHub, obj);
	}

	/**
	 * Fires a before-move event for an object being repositioned within the Hub.
	 *
	 * @param thisHub the Hub generating the event
	 * @param fromPos the original position
	 * @param toPos   the destination position
	 */
	public static void fireBeforeMoveEvent(Hub thisHub, int fromPos, int toPos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireBeforeMoveEvent(thisHub, fromPos, toPos);
	}

	/**
	 * Fires an after-move event for an object repositioned within the Hub.
	 * Notifies listeners via their {@code afterMove} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param fromPos the original position
	 * @param toPos   the new position
	 */
	public static void fireAfterMoveEvent(Hub thisHub, int fromPos, int toPos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireAfterMoveEvent(thisHub, fromPos, toPos);
	}

	/**
	 * Fires a calculated-property-change event for the given object and
	 * property. Updates affected detail Hubs when the property corresponds
	 * to a link and notifies all listeners via {@code afterPropertyChange}.
	 *
	 * @param thisHub      the Hub generating the event
	 * @param object       the object whose property changed
	 * @param propertyName the name of the property that changed
	 */
	public static void fireCalcPropertyChange(Hub thisHub, final Object object, final String propertyName) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireCalcPropertyChange(thisHub, object, propertyName);
	}

	/**
	 * Fires a before-property-change event for the given object. Notifies all
	 * listeners via their {@code beforePropertyChange} method.
	 *
	 * @param thisHub      the Hub generating the event
	 * @param oaObj        the object whose property is changing
	 * @param propertyName the name of the property
	 * @param oldValue     the previous value
	 * @param newValue     the new value
	 */
	public static void fireBeforePropertyChange(Hub thisHub, OAObject oaObj, String propertyName, Object oldValue, Object newValue) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireBeforePropertyChange(thisHub, oaObj, propertyName, oldValue, newValue);
	}

	/**
	 * Fires an after-property-change event for the given object and property.
	 * Updates detail Hubs when the property corresponds to a link, validates
	 * unique-property constraints, and notifies all listeners via their
	 * {@code afterPropertyChange} method. Supports queued dispatch when
	 * remote-thread queuing is enabled.
	 *
	 * @param thisHub      the Hub generating the event
	 * @param oaObj        the object whose property changed
	 * @param propertyName the name of the changed property
	 * @param oldValue     the previous value
	 * @param newValue     the new value
	 * @param linkInfo     link metadata for the property, or null
	 */
	public static void fireAfterPropertyChange(final Hub thisHub, final OAObject oaObj, final String propertyName, final Object oldValue,
			final Object newValue, final OALinkInfo linkInfo) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireAfterPropertyChange(thisHub, oaObj, propertyName, oldValue, newValue, linkInfo);
	}


	/**
	 * Fires new-list and after-new-list events for the Hub to notify listeners
	 * that a new collection has been established. Also increments the Hub’s
	 * change count.
	 *
	 * @param thisHub the Hub generating the event
	 * @param bAll    whether to notify all listeners or a subset
	 */
	public static void fireOnNewListEvent(Hub thisHub, boolean bAll) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().fireOnNewListEvent(thisHub, bAll);
	}

	/**
	 * Registers a HubListener for a specific property name with optional
	 * dependent property paths. Clears the listener-cache afterward.
	 *
	 * @param thisHub                 the Hub to attach the listener to
	 * @param hl                      the listener to add
	 * @param property                the property name
	 * @param dependentPropertyPaths  dependent properties to monitor
	 */
	public static void addHubListener(Hub thisHub, HubListener hl, String property, String[] dependentPropertyPaths) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().addHubListener(thisHub, hl, property, dependentPropertyPaths);
	}

	/**
	 * Registers a HubListener for a property with optional dependent paths
	 * and an option to receive events only for the active object. Clears the
	 * listener cache after registration.
	 *
	 * @param thisHub                 the Hub to attach the listener to
	 * @param hl                      the listener to add
	 * @param property                the property name
	 * @param dependentPropertyPaths  dependent properties to monitor
	 * @param bActiveObjectOnly       true to notify only for active-object events
	 */
	public static void addHubListener(Hub thisHub, HubListener hl, String property, String[] dependentPropertyPaths,
			boolean bActiveObjectOnly) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().addHubListener(thisHub, hl, property, dependentPropertyPaths, bActiveObjectOnly);
	}

	/**
	 * Registers a HubListener for a property with options for dependent
	 * paths, active-object filtering, and background-thread execution.
	 * Clears the listener cache after registration.
	 *
	 * @param thisHub                 the Hub to attach the listener to
	 * @param hl                      the listener to add
	 * @param property                the property name
	 * @param dependentPropertyPaths  dependent properties to monitor
	 * @param bActiveObjectOnly       true for active-object-only events
	 * @param bUseBackgroundThread    true to dispatch events in a background thread
	 */
	public static void addHubListener(Hub thisHub, HubListener hl, String property, String[] dependentPropertyPaths,
			boolean bActiveObjectOnly, boolean bUseBackgroundThread) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().addHubListener(thisHub, hl, property, dependentPropertyPaths, bActiveObjectOnly, bUseBackgroundThread);
	}

	/**
	 * Registers a HubListener for a specific property. Clears the listener
	 * cache after registration.
	 *
	 * @param thisHub the Hub to attach the listener to
	 * @param hl      the listener to add
	 * @param property the property name
	 */
	public static void addHubListener(Hub thisHub, HubListener hl, String property) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().addHubListener(thisHub, hl, property);
	}

	/**
	 * Registers a listener for a property with an option to receive only
	 * active-object-related events. Clears the listener cache afterward.
	 *
	 * @param thisHub           the Hub to attach the listener to
	 * @param hl                the listener to add
	 * @param property          the property name
	 * @param bActiveObjectOnly true to restrict events to active-object changes
	 */
	public static void addHubListener(Hub thisHub, HubListener hl, String property, boolean bActiveObjectOnly) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().addHubListener(thisHub, hl, property, bActiveObjectOnly);
	}

	/**
	 * Registers a listener for all properties with an option to restrict
	 * events to active-object changes. Clears the listener cache afterward.
	 *
	 * @param thisHub           the Hub to attach the listener to
	 * @param hl                the listener to add
	 * @param bActiveObjectOnly true to restrict notifications
	 */
	public static void addHubListener(Hub thisHub, HubListener hl, boolean bActiveObjectOnly) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().addHubListener(thisHub, hl, bActiveObjectOnly);
	}

	/**
	 * Registers a listener to receive all Hub and OAObject events. Clears
	 * the listener cache afterward.
	 *
	 * @param thisHub the Hub to attach the listener to
	 * @param hl      the listener to add
	 */
	public static void addHubListener(Hub thisHub, HubListener hl) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().addHubListener(thisHub, hl);
	}

	public static int TotalHubListeners;

	/**
	 * Removes a HubListener from the Hub’s listener tree if present. Clears
	 * the listener cache afterward.
	 *
	 * @param thisHub the Hub to remove the listener from
	 * @param l       the listener to remove
	 */
	public static void removeHubListener(Hub thisHub, HubListener l) {
		//qqqqqq method was protected
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubEventService().removeHubListener(thisHub, l);
	}

	/**
	 * Returns all listeners registered directly on this Hub (not including
	 * shared or duplicate Hubs). Returns an empty array when none exist.
	 *
	 * @param thisHub the Hub whose direct listeners are requested
	 * @return an array of HubListeners
	 */
	public static HubListener[] getHubListeners(Hub thisHub) {
		//qqqqqqq method was protected
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubEventService().getHubListeners(thisHub);
	}

	/**
	 * Returns the count of listeners registered for this Hub, including
	 * shared and duplicate Hubs.
	 *
	 * @param thisHub the Hub to inspect
	 * @return the total listener count
	 */
	public static int getListenerCount(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return 0;
		return g.hubs().getHubEventService().getListenerCount(thisHub);
	}

	/**
	 * Returns all listeners for this Hub, delegating to the type-0
	 * variant of {@code getAllListeners(Hub,int)}.
	 *
	 * @param thisHub the Hub to inspect
	 * @return all listeners associated with the Hub
	 */
	public static HubListener[] getAllListeners(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubEventService().getAllListeners(thisHub);
	}


	/**
	 * Returns listeners for the Hub according to the lookup type, which
	 * controls whether shared or duplicate Hubs are included. Uses a small
	 * cache for type-0 lookups.
	 *
	 * @param thisHub the Hub to inspect
	 * @param type    lookup type selector
	 * @return an array of listeners matching the lookup criteria
	 */
	protected static HubListener[] getAllListeners(final Hub thisHub, int type) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubEventService().getAllListeners(thisHub, type);
	}

	/**
	 * Clears cached results used for {@code getAllListeners}. Removes cache
	 * entries for the specified Hub or all entries matching its object class.
	 *
	 * @param hub the Hub whose cache entries should be invalidated
	 */
	public static void clearGetAllListenerCache(Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return;
		g.hubs().getHubEventService().clearGetAllListenerCache(hub);
	}

	/**
	 * Recursively collects listeners from this Hub and appropriate shared or
	 * duplicate Hubs based on the lookup type.
	 *
	 * @param thisHub the Hub whose listeners are being collected
	 * @param hub     the reference Hub used for comparison
	 * @param type    lookup type selector
	 * @return an array of collected listeners
	 */
	protected static HubListener[] getAllListenersRecursive(Hub thisHub, Hub hub, int type) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubEventService().getAllListenersRecursive(thisHub, hub, type);
	}


	/**
	 * Fires an after-load event for an OAObject added to the Hub's data.
	 * Notifies listeners via their {@code afterLoad} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param oaObj   the loaded object
	 */
	public static void fireAfterLoadEvent(Hub thisHub, OAObject oaObj) {
		OAGraph g = getGraph(thisHub, oaObj);
		if (g == null) return;
		g.hubs().getHubEventService().fireAfterLoadEvent(thisHub, oaObj);
	}

	/**
	 * qqqqq these are in Hub public static boolean canAdd(Hub thisHub) { return canAdd(thisHub, null); } public static boolean canAdd(Hub
	 * thisHub, OAObject obj) { if (obj == null) return OAObjectCallbackDelegate.getAllowAdd(thisHub); return
	 * OAObjectCallbackDelegate.getVerifyAdd(thisHub, obj); } public static boolean canRemove(Hub thisHub) { return canRemove(thisHub,
	 * null); } public static boolean canRemove(Hub thisHub, OAObject obj) { if (obj == null) return
	 * OAObjectCallbackDelegate.getAllowRemove(thisHub); return OAObjectCallbackDelegate.getVerifyRemove(thisHub, obj); } public static
	 * boolean canRemoveAll(Hub thisHub) { return OAObjectCallbackDelegate.getAllowRemoveAll(thisHub); }
	 */
}
