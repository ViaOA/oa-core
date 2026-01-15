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
package com.viaoa.object;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAFilter;

//qqqqqqqqq PHASE 3: moved to OAObjectCacheService

/**
 * Internal delegate responsible for managing the OAObject runtime cache,
 * ensuring global identity consistency and fast lookup by either GUID or
 * business keys. All stored references are weak so that objects may be
 * reclaimed by the garbage collector when no longer referenced elsewhere.
 *
 * <p>Identity resolution is GUID-first: if both GUID and business keys are
 * supplied, any conflict is resolved in favor of GUID. Business key lookups
 * are used when the GUID is unknown (e.g., object created or referenced using
 * only persistent identity), allowing lazy loading and identity reconciliation.</p>
 *
 * <p>The delegate also cooperates with client-session tracking for distributed
 * UI operation. When a client receives objects over the network, the server
 * maintains a strong reference set of objects currently visible to that client.
 * When the client releases objects, the cache is updated so that objects are
 * eligible for GC if no other strong references exist.</p>
 *
 * <p>This design allows the OA framework to maintain a consistent and correct
 * object graph while efficiently supporting distributed, event-driven, and
 * offline-first application behavior.</p>
 *
 * @see OAObjectCache
 * @see OAObject
 * @see OAObjectKey
 * @see OAObjectKeyDelegate
 */
public class OAObjectCacheDelegate {
	public static final Logger LOG = Logger.getLogger(OAObjectCacheDelegate.class.getName());


	/**
	 * throw an exception if a duplicate object is added. This is Default. see HubController#setAddMode
	 */
	static private final int NO_DUPS = 1; // dont use 0

	/**
	 * dont store object if a duplicate is already stored. If the object is being deserialized (see OAObject.readResolve) then the object
	 * that is already loaded will be used. see HubController#setAddMode
	 *
	 * @see OAObject#readResolve
	 */
	static private final int IGNORE_DUPS = 2;

	/**
	 * store object even if another exists see HubController#setAddMode
	 */
	// static private final int OVERWRITE_DUPS = 3; // not used qqqqqqqqqqqqqq

	/**
	 * dont store objects. see HubController#setAddMode
	 */
	static private final int IGNORE_ALL = 4;
	static private final int MODE_MAX = 4;

	
	
	/**
	 * Returns all Hubs registered as “select all” Hubs for the specified class.
	 * These Hubs are maintained as weak references and automatically cleaned up
	 * when they are no longer strongly referenced elsewhere.
	 *
	 * @param clazz the class whose select-all Hubs are requested
	 * @return an array of matching Hubs, or {@code null} if none exist
	 */
	public static Hub[] getSelectAllHubs(Class clazz) {
		if (clazz == null) return null;
		return OARuntime.get().graph(clazz).objects().getOAObjectCacheService().getSelectAllHubs(clazz);
	}
	

	/**
	 * Returns the first Hub registered as a “select all” Hub for the
	 * specified class. If no such Hubs exist, this method returns {@code null}.
	 *
	 * @param clazz the class whose first select-all Hub is requested
	 * @return the first matching Hub, or {@code null} if none exist
	 */
	public static Hub getSelectAllHub(Class clazz) {
		if (clazz == null) return null;
		return OARuntime.get().graph(clazz).objects().getOAObjectCacheService().getSelectAllHub(clazz);
	}

	public static Class classSample;
	
	/**
	 * Registers the specified Hub as a “select all” Hub for its object class.
	 * A weak reference is stored so the Hub can be automatically cleared when
	 * no longer strongly referenced. If the Hub is already registered, the call
	 * is ignored.
	 *
	 * @param hub the Hub to register as a select-all Hub
	 */
	public static void setSelectAllHub(Hub hub) {
		if (hub == null) {
			return;
		}
		Class clazz = hub.getObjectClass();
		if (clazz == null) return;
		classSample = clazz;
		OAGraph og = OARuntime.get().graph(clazz);
		if (og != null) og.objects().getOAObjectCacheService().setSelectAllHub(hub);
	}

	/**
	 * Unregisters the specified Hub from the list of “select all” Hubs for
	 * its object class. If the Hub is the only entry, the class is removed
	 * entirely from the registry.
	 *
	 * @param hub the Hub to remove from the select-all list
	 */
	public static void removeSelectAllHub(Hub hub) {
		if (hub == null) {
			return;
		}
		Class clazz = hub.getObjectClass();
		if (clazz == null) return;
		OAGraph og = OARuntime.get().graph(clazz);
		if (og != null) og.objects().getOAObjectCacheService().removeSelectAllHub(hub);
	}

	/**
	 * Removes all registered “select all” Hubs across all classes. The
	 * underlying map is cleared, removing all weak references to Hubs.
	 */
	public static void removeAllSelectAllHubs() {
		// find one
		if (classSample == null) return;
		OAGraph og = OARuntime.get().graph(classSample);
		if (og != null) og.objects().getOAObjectCacheService().removeAllSelectAllHubs();
	}

	/**
	 * Stores the specified Hub under a global name using a weak reference.
	 * The name is treated case-insensitively. If either argument is null,
	 * the call is ignored.
	 *
	 * @param name the reference name (case-insensitive)
	 * @param hub  the Hub to associate with the name
	 */
	static private void setNamedHub(String name, Hub<? extends OAObject> hub) {
		LOG.fine("Hub=" + hub + ", name=" + name);
		if (name == null || hub == null) {
			return;
		}
		Class c = hub.getObjectClass();
		if (c == null) return;
		classSample = c;
		OAGraph og = OARuntime.get().graph(classSample);
		if (og != null) og.objects().getOAObjectCacheService().setNamedHub(name, hub);
	}

	/**
	 * Retrieves a Hub previously stored under the given name. The lookup
	 * is case-insensitive. If the weak reference has been cleared, the
	 * entry is removed and {@code null} is returned.
	 *
	 * @param name the name of the Hub to retrieve (case-insensitive)
	 * @return the Hub associated with the name, or {@code null} if not found
	 */
	public static Hub getNamedHub(String name) {
		if (name == null) return null;
		if (classSample == null) return null;
		OAGraph og = OARuntime.get().graph(classSample);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().getNamedHub(name);
	}


	/**
	 * Registers a cache listener for the specified class. The listener will
	 * receive events such as afterAdd, afterRemove, and afterPropertyChange
	 * for objects of that class. Duplicate registrations are ignored.
	 *
	 * @param clazz the class whose events the listener should receive
	 * @param l     the listener to register
	 */
	public static <T extends OAObject> void addListener(final Class<T> clazz, final OAObjectCacheListener<T> l) {
		if (clazz == null || l == null) return;
		OAGraph og = OARuntime.get().graph(clazz);
		if (og != null) og.objects().getOAObjectCacheService().addListener(clazz, l);
	}

	/**
	 * Enables or disables unit test mode. When enabled, certain operations
	 * such as {@link #resetCache()} are permitted; otherwise they will throw
	 * an exception. This flag is intended for internal testing only.
	 *
	 * @param b {@code true} to enable unit test mode, {@code false} to disable it
	 */
	public static void setUnitTestMode(boolean b) {
		 OARuntime.get().setUnitTestMode(b);
	}

	/**
	 * Clears all cache data, listeners, select-all Hubs, and named Hubs.
	 * This operation is permitted only when unit test mode is enabled;
	 * otherwise, a {@link RuntimeException} is thrown.
	 *
	 * @throws RuntimeException if unit test mode is not enabled
	 */
	public static void resetCache() {
		OARuntime.get().unitTestReset();
	}

	/**
	 * Unregisters the specified listener for the given class. If the listener
	 * is found and removed, the global listener count is decremented.
	 *
	 * @param clazz the class whose listener list should be modified
	 * @param l     the listener to remove
	 */
	public static void removeListener(Class clazz, OAObjectCacheListener l) {
		if (clazz == null || l == null) return;
		OAGraph og = OARuntime.get().graph(clazz);
		if (og != null) og.objects().getOAObjectCacheService().removeListener(clazz, l);
	}

	/**
	 * Returns all registered cache listeners for the specified class.
	 * If no listeners are registered globally or for the class, this
	 * method returns {@code null}. The returned array is a snapshot of
	 * the current listeners.
	 *
	 * @param c the class whose listeners should be retrieved
	 * @return an array of listeners for the class, or {@code null} if none exist
	 */
	@SuppressWarnings("unchecked")
	public static <T extends OAObject> OAObjectCacheListener<T>[] getListeners(final Class<T> c) {
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().getListeners(c);
	}
	
	/**
	 * Notifies all registered cache listeners for the object's class that a
	 * property value has changed. The event is sent only if listener count is
	 * nonzero and {@code bSendEvent} is {@code true}. The original and new
	 * values are forwarded for listener handling.
	 *
	 * @param obj          the object whose property changed
	 * @param origKey      the original object key (may include old primary key values)
	 * @param propertyName the name of the changed property
	 * @param oldValue     the prior value of the property
	 * @param newValue     the new value of the property
	 * @param bLocalOnly   unused indicator for local-only routing
	 * @param bSendEvent   whether to dispatch the event to listeners
	 */
	public static void fireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue,
			boolean bLocalOnly, boolean bSendEvent) {
		//qqqqqqqqq method was protected
		if (obj == null) return;
		Class c  = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().fireAfterPropertyChange(obj, origKey, propertyName, oldValue, newValue, bLocalOnly, bSendEvent);
	}

	/**
	 * Sends an after-load event to all registered listeners for the object's class.
	 * The event is triggered only if listeners exist. Each listener's
	 * {@code afterLoad} method is invoked.
	 *
	 * @param obj the object that has just been loaded
	 */
	public static <T extends OAObject> void fireAfterLoadEvent(T obj) {
		if (obj == null) return;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().fireAfterLoadEvent(obj);
	}

	/**
	 * Sends an after-add event to all registered listeners for the object's class.
	 * The event is dispatched only if listeners exist and both the Hub and object
	 * are non-null. Each listener's {@code afterAdd(Hub, T)} method is invoked.
	 *
	 * @param hub the Hub to which the object was added
	 * @param obj the object that was added
	 */
	public static <T extends OAObject> void fireAfterAddEvent(Hub<T> hub, T obj) {
		if (obj == null) return;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().fireAfterAddEvent(hub, obj);
	}

	/**
	 * Sends an after-remove event to all registered listeners for the object's class.
	 * The event is dispatched only if listeners exist and both the Hub and object
	 * are non-null. Each listener's {@code afterRemove(Hub, T)} method is invoked.
	 *
	 * @param hub the Hub from which the object was removed
	 * @param obj the object that was removed
	 */
	public static <T extends OAObject> void fireAfterRemoveEvent(Hub<T> hub, T obj) {
		if (obj == null) return;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().fireAfterRemoveEvent(hub, obj);
	}

	
	
	/**
	 * Removes all objects from the object cache across all OAObject classes.
	 * Each class registered in the cache is cleared in turn. This does not
	 * affect listeners or select-all/named Hub registrations.
	 */
	public static void removeAllObjects() {
		Class c = classSample;
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().removeAllObjects();
	}

	/**
	 * Removes all cached objects for the specified class. This clears only
	 * the cache entries for the class and does not affect listeners or other
	 * cache metadata.
	 *
	 * @param c the class whose cached objects should be removed
	 */
	public static void removeAllObjects(Class c) {
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().removeAllObjects(c);
	}

	/**
	 * Visits all cached objects across all classes by invoking the specified
	 * {@link OACallback}. This is a convenience method that delegates to
	 * {@link #visit(OACallback)}.
	 *
	 * @param callback the callback to be invoked for each cached object
	 */
	public static void callback(OACallback callback) {
		visit(callback);
	}

	/**
	 * Visits every cached object across all OAObject classes by delegating
	 * to the underlying {@link OAObjectCache}. Each object is passed to the
	 * supplied {@link OACallback}.
	 *
	 * @param callback the callback invoked for each cached object
	 */
	public static void visit(OACallback callback) {
		Class c = classSample;
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().visit(callback);
	}

	/**
	 * Invokes the specified {@link OACallback} for every cached object of
	 * the given class. This is a convenience wrapper that delegates to
	 * {@link #visit(Class, OACallback)}.
	 *
	 * @param clazz    the OAObject class whose cached instances should be processed
	 * @param callback the callback to invoke for each object
	 */
	public static void callback(Class<? extends OAObject> clazz, OACallback callback) {
		visit(clazz, callback);
	}

	/**
	 * Visits all cached objects of the specified class by delegating to the
	 * underlying {@link OAObjectCache}. Objects are passed to the supplied
	 * {@link OACallback}.
	 *
	 * @param clazz    the OAObject class to visit
	 * @param callback the callback invoked for each object
	 */
	public static void visit(Class clazz, OACallback callback) {
		Class c = clazz;
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().visit(c, callback);
	}

	/**
	 * Convenience wrapper that invokes the specified {@link OACallback} for
	 * every cached object of the given class. This delegates directly to
	 * {@link #callback(Class, OACallback)}.
	 *
	 * @param callback the callback to invoke for each cached object
	 * @param clazz    the OAObject class whose objects should be visited
	 */
	public static void callback(OACallback callback, Class clazz) {
		Class c = clazz;
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().callback(callback, c);
	}

	
	/**
	 * Convenience wrapper that visits all cached objects of the specified
	 * class by delegating to {@link #visit(Class, OACallback)}. Each object
	 * is passed to the supplied {@link OACallback}.
	 *
	 * @param callback the callback invoked for each cached object
	 * @param clazz    the OAObject class whose cached instances should be visited
	 */
	public static void visit(OACallback callback, Class clazz) {
		Class c = clazz;
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().visit(callback, c);
	}

	/**
	 * Populates the supplied list with cache summary information. The method
	 * adds a series of entries describing cache statistics, including:
	 * <ul>
	 *   <li>whether unit test mode is enabled</li>
	 *   <li>the total number of registered listeners</li>
	 *   <li>the count of select-all Hubs</li>
	 *   <li>the count of named Hubs</li>
	 * </ul>
	 *
	 * @param al the list to which cache information entries are added
	 */
	public static void getInfo(List al) {
		Class c = classSample;
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().getInfo(al);
	}

	/**
	 * Returns all classes currently registered in the object cache. This is
	 * a convenience wrapper around the underlying {@link OAObjectCache}
	 * implementation.
	 *
	 * @return an array of OAObject classes known to the cache
	 */
	public static Class[] getClasses() {
		Class c = classSample;
		// if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().getClasses();
	}

	/**
	 * Returns the number of cached objects for the specified class. This is a
	 * convenience wrapper that delegates to the underlying {@link OAObjectCache}.
	 *
	 * @param clazz the class whose cached object count is requested
	 * @return the number of cached objects for the class
	 */
	public static int getTotal(Class clazz) {
		Class c = clazz;
		if (c == null) return 0;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return 0;
		return og.objects().getOAObjectCacheService().getTotal(clazz);
	}


	/**
	 * Returns a newly created list containing cache summary information.
	 * This method constructs the list, populates it using
	 * {@link #getInfo(List)}, and returns the populated result.
	 *
	 * @return a list containing cache summary information
	 */
	public static List getInfo() {
		Class c = classSample;
		if (c == null) return new ArrayList<>();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().getInfo();
	}

	/**
	 * Sets the default add mode used by HubController operations for all threads
	 * that do not already have an assigned add mode. The mode controls how
	 * duplicate objects are handled when added to the cache.
	 *
	 * <p>If the supplied mode is outside the valid range {@code 0–4}, an
	 * {@link IllegalArgumentException} is thrown. Valid modes include
	 * {@link #NO_DUPS}, {@link #IGNORE_DUPS}, and {@link #IGNORE_ALL}.</p>
	 *
	 * @param mode the default add mode to assign
	 * @throws IllegalArgumentException if the mode is not between 0 and 4
	 */
	static private void setDefaultAddMode(int mode) {
		Class c = classSample;
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().setDefaultAddMode(mode);
	}

	/**
	 * Returns the current default add mode used for threads that do not have a
	 * thread-local add mode assigned. This setting determines how duplicate
	 * objects are handled when added to the cache.
	 *
	 * @return the default add mode value
	 */
	static private int getDefaultAddMode() {
		Class c = classSample;
		if (c == null) return 1;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return 1;
		return og.objects().getOAObjectCacheService().getDefaultAddMode();
	}

	/**
	 * Clears all cached objects for the specified class by delegating to the
	 * underlying {@link OAObjectCache}. Only the cache entries for the given
	 * class are removed; listeners and other cache metadata are unaffected.
	 *
	 * @param clazz the class whose cached objects should be cleared
	 */
	public static void clearCache(Class clazz) {
		Class c = clazz;
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().clearCache(c);
	}


	/**
	 * Adds the specified object to the cache using default behavior. This is a
	 * convenience wrapper that delegates to
	 * {@link #add(OAObject, boolean, boolean)} with duplicate-error checking
	 * disabled and automatic addition to select-all Hubs enabled.
	 *
	 * @param obj the object to add to the cache
	 * @return the existing cached object if one matches, otherwise the supplied object
	 */
	public static OAObject add(OAObject obj) {
		if (obj == null) return null;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().add(obj);
	}

	/**
	 * Adds the specified object to the cache with explicit control over duplicate
	 * handling and select-all Hub population. This method delegates to
	 * {@link #add(OAObject, boolean, boolean, boolean)} with event dispatching
	 * performed in the current thread.
	 *
	 * @param obj               the object to add to the cache
	 * @param bErrorIfExists    whether to throw an exception if a duplicate exists
	 * @param bAddToSelectAll   whether the object should be added to all select-all Hubs
	 * @return the existing cached object if one matches, otherwise the supplied object
	 */
	public static OAObject add(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll) {
		if (obj == null) return null;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().add(obj, bErrorIfExists, bAddToSelectAll);
	}

	/**
	 * Adds the specified object to the cache with full control over duplicate
	 * handling, select-all Hub population, and whether after-add events are
	 * dispatched asynchronously in another thread. If caching is disabled,
	 * the supplied object is returned unchanged.
	 *
	 * @param obj                             the object to add to the cache
	 * @param bErrorIfExists                  whether to throw an exception if a duplicate exists
	 * @param bAddToSelectAll                 whether the object should be added to all select-all Hubs
	 * @param bSendAddEventInAnotherThread    whether after-add events should be queued for asynchronous dispatch
	 * @return the existing cached object if one matches, otherwise the supplied object
	 */
	public static OAObject add(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll, boolean bSendAddEventInAnotherThread) {
		if (obj == null) return null;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().add(obj, bErrorIfExists, bAddToSelectAll, bSendAddEventInAnotherThread);
	}


	
	
	/**
	 * Dispatches the after-add event to all registered cache listeners for the
	 * object's class. If {@code bSendAddEventInAnotherThread} is {@code true},
	 * the events are queued and processed asynchronously; otherwise, they are
	 * invoked immediately in the current thread. No action is taken if the
	 * listener count is zero or the object is null.
	 *
	 * @param obj                             the object that was added to the cache
	 * @param bSendAddEventInAnotherThread    whether to dispatch events asynchronously
	 */
	public static <T extends OAObject> void fireAfterAddEvent(T obj, boolean bSendAddEventInAnotherThread) {
		if (obj == null) return;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().fireAfterAddEvent(obj, bSendAddEventInAnotherThread);
	}


	/**
	 * Adds the specified object to all registered select-all Hubs for its class.
	 * If a Hub already contains the object, it is skipped. This ensures that
	 * objects are automatically included in global views without creating duplicates.
	 *
	 * @param obj the object to add to all select-all Hubs
	 */
	public static void addToSelectAllHubs(OAObject obj) {
		if (obj == null) return;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().addToSelectAllHubs(obj);
	}

	/**
	 * Notifies the cache that a key property value of the specified object has
	 * changed. The object cache is updated to reflect the new key value, ensuring
	 * that future lookups using the updated key will succeed. No action is taken
	 * if caching is disabled.
	 *
	 * @param obj the object whose key property has changed
	 */
	public static void propertyKeyValueChanged(OAObject obj) {
		//qqqqqq method was protected
		if (obj == null) return;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().propertyKeyValueChanged(obj);
	}

	/**
	 * Removes the specified object from the cache. This operation delegates
	 * directly to the underlying {@link OAObjectCache} and does not affect
	 * listeners or select-all Hub registrations.
	 *
	 * @param obj the object to remove from the cache
	 */
	static private void removeObject(final OAObject obj) {
		if (obj == null) return;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().removeObject(obj);
	}
	

	/**
	 * Retrieves an object from the cache based on its object ID property value.
	 * This is a convenience wrapper around {@link #get(Class, Object)}.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param key   the object ID, an array of IDs, or an {@link OAObjectKey} representing the object
	 * @return the cached object matching the key, or {@code null} if not found
	 * @see OAObjectKey#OAObjectKey
	 * @see OAObject#equals
	 */
	public static <T extends OAObject> T getObject(Class<T> clazz, Object key) {
		Class<T> c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().getObject(c, key);
	}

	/**
	 * Retrieves an object from the cache using an integer ID. This method
	 * delegates to {@link #get(Class, Object)} by wrapping the integer in
	 * an {@link Integer} object.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param id    the integer ID of the object
	 * @return the cached object matching the ID, or {@code null} if not found
	 */
	public static <T extends OAObject> T get(Class<T> clazz, int id) {
		Class<T> c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().get(c, id);
	}

	/**
	 * Retrieves an object from the cache based on the provided key. If the key
	 * is not an {@link OAObjectKey}, it is converted appropriately. Delegates
	 * to {@link #get(Class, OAObjectKey)} for the final retrieval.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param key   the object, key value, array of key values, or {@link OAObjectKey}
	 * @return the cached object matching the key, or {@code null} if not found
	 */
	public static <T extends OAObject> T get(Class<T> clazz, Object key) {
		Class<T> c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().get(c, key);
	}
	
	/**
	 * Retrieves an object from the cache using its {@link OAObjectKey}. If either
	 * the class or key is null, {@code null} is returned. Delegates directly
	 * to the underlying {@link OAObjectCache} to fetch the object.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param ok    the {@link OAObjectKey} representing the object's identity
	 * @return the cached object matching the key, or {@code null} if not found
	 */
	public static <T extends OAObject> T get(Class<T> clazz, OAObjectKey ok) {
		Class<T> c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().get(c, ok);
	}

//qqqqqq remove this method ??
	/**
	 * Retrieves an object from the cache by its GUID. This method returns the
	 * object if it exists in the cache; it does not create a new instance.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param guid  the globally unique identifier of the object
	 * @return the cached object matching the GUID, or {@code null} if not found
	 */
	public static <T extends OAObject> T getNewObjectUsingGuid(Class<T> clazz, UUID guid) {
		Class<T> c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().getNewObjectUsingGuid(c, guid);
	}

	/**
	 * Retrieves an object from the cache based on its GUID. Delegates directly
	 * to the underlying {@link OAObjectCache} for the lookup.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param guid  the globally unique identifier of the object
	 * @return the cached object matching the GUID, or {@code null} if not found
	 */
	public static <T extends OAObject> T getUsingGuid(Class<T> clazz, UUID guid) {
		Class<T> c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().getUsingGuid(c, guid);
	}
	
	/**
	 * Retrieves the cached instance of the specified object based on its
	 * current key values. If caching is disabled or the object is null,
	 * {@code null} is returned.
	 *
	 * @param obj the object whose cached instance is requested
	 * @return the cached object matching the key, or {@code null} if not found or caching is disabled
	 */
	public static Object get(OAObject obj) {
		if (obj == null) return null;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().get(obj);
	}

	/**
	 * Finds the next object in the cache following the specified object.
	 * Delegates to the internal {@link #_find(Object, Class, String, Object, boolean, boolean)}
	 * method with default parameters.
	 *
	 * @param fromObject the object from which to start the search; if null, search starts at the beginning
	 * @return the next object in the cache, or {@code null} if none found
	 */
	public static Object findNext(Object fromObject) {
		Object obj = fromObject;
		if (obj == null) return null;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().findNext(obj);
	}

	/**
	 * Finds the next object in the cache after {@code fromObject} that matches
	 * the specified property path and value. Delegates to the internal
	 * {@link #_find(Object, Class, String, Object, boolean, boolean)} method
	 * with default skip-new and exception behavior.
	 *
	 * @param fromObject   the object from which to start the search; if null, search starts at the beginning
	 * @param propertyPath the property path to match
	 * @param findObject   the value to compare against
	 * @return the next matching object in the cache, or {@code null} if none found
	 */
	public static Object findNext(Object fromObject, String propertyPath, Object findObject) {
		Object obj = fromObject;
		if (obj == null) return null;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().findNext(obj, propertyPath, findObject);
	}

	/**
	 * Finds the next object in the cache after {@code fromObject} that matches
	 * the specified property path and value. Allows control over whether new
	 * objects should be skipped and whether exceptions should be thrown if
	 * no match is found. Delegates to the internal {@link #_find(Object, Class, String, Object, boolean, boolean)}.
	 *
	 * @param fromObject     the object from which to start the search; if null, search starts at the beginning
	 * @param propertyPath   the property path to match
	 * @param findObject     the value to compare against
	 * @param bSkipNew       whether newly added (unsaved) objects should be skipped
	 * @param bThrowException whether to throw an exception if no matching object is found
	 * @return the next matching object in the cache, or {@code null} if none found
	 */
	public static Object findNext(Object fromObject, String propertyPath, Object findObject, boolean bSkipNew, boolean bThrowException) {
		Object obj = fromObject;
		if (obj == null) return null;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().findNext(fromObject, propertyPath, findObject, bSkipNew, bThrowException);
	}

	/**
	 * Finds the next object in the cache of the specified class after
	 * {@code fromObject} that matches the given property path and value.
	 * If {@code fromClass} is null, the class of {@code fromObject} is used.
	 * Delegates to the internal {@link #_find(Object, Class, String, Object, boolean, boolean)}.
	 *
	 * @param fromObject   the object from which to start the search; if null, search starts at the beginning
	 * @param fromClass    the class of objects to search; if null, the class of {@code fromObject} is used
	 * @param propertyPath the property path to match
	 * @param findObject   the value to compare against
	 * @return the next matching object in the cache, or {@code null} if none found
	 */
	public static Object findNext(Object fromObject, Class fromClass, String propertyPath, Object findObject) {
		Object obj = fromObject;
		if (obj == null) return null;
		Class c = obj.getClass();
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().findNext(fromObject, fromClass, propertyPath, findObject);
	}

	/**
	 * Searches the cache for any object of the specified class. Delegates to
	 * the internal {@link #_find(Object, Class, String, Object, boolean, boolean)}
	 * method with default parameters.
	 *
	 * @param clazz the class of objects to search
	 * @return the first matching object in the cache, or {@code null} if none found
	 */
	public static Object find(Class clazz) {
		Class c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().find(c);
	}

	/**
	 * Searches the cache for an object of the specified class that satisfies
	 * the given {@link OAFinder}. Delegates to the internal
	 * {@link #_find(Object, Class, OAFinder, boolean, boolean)} method with
	 * default skip-new and exception behavior.
	 *
	 * @param clazz  the class of objects to search
	 * @param finder the finder specifying the search criteria
	 * @return the first matching object in the cache, or {@code null} if none found
	 */
	public static Object find(Class clazz, OAFinder finder) {
		Class c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().find(c, finder);
	}

	/**
	 * Searches the cache for an object of the specified class where the
	 * property at {@code propertyPath} equals {@code findObject}. Delegates
	 * to the internal {@link #_find(Object, Class, String, Object, boolean, boolean)}
	 * with default skip-new and exception behavior.
	 *
	 * @param clazz        the class of objects to search
	 * @param propertyPath the property path to match
	 * @param findObject   the value to compare against
	 * @return the first matching object in the cache, or {@code null} if none found
	 */
	public static Object find(Class clazz, String propertyPath, Object findObject) {
		Class c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().find(c, propertyPath, findObject);
	}

	/**
	 * Searches the cache for an object of the specified class where the
	 * property at {@code propertyPath} equals {@code findObject}, with
	 * control over skipping new objects and throwing exceptions if no match
	 * is found. Delegates to the internal
	 * {@link #_find(Object, Class, String, Object, boolean, boolean)} method.
	 *
	 * @param clazz         the class of objects to search
	 * @param propertyPath  the property path to match
	 * @param findObject    the value to compare against
	 * @param bSkipNew      whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no match is found
	 * @return the first matching object in the cache, or {@code null} if none found
	 */
	public static Object find(Class clazz, String propertyPath, Object findObject, boolean bSkipNew, boolean bThrowException) {
		Class c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().find(clazz, propertyPath, findObject, bSkipNew, bThrowException);
	}

	/**
	 * Searches the cache for an object of the specified class that satisfies
	 * the given {@link OAFinder}, with control over skipping new objects and
	 * throwing exceptions if no match is found. Delegates to the internal
	 * {@link #_find(Object, Class, OAFinder, boolean, boolean)} method.
	 *
	 * @param clazz          the class of objects to search
	 * @param finder         the finder specifying the search criteria
	 * @param bSkipNew       whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no match is found
	 * @return the first matching object in the cache, or {@code null} if none found
	 */
	public static Object find(Class clazz, OAFinder finder, boolean bSkipNew, boolean bThrowException) {
		Class c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().find(clazz, finder, bSkipNew, bThrowException);
	}

	
	/**
	 * Internal helper that searches the cache for an object of the specified class
	 * matching the given property path and value. Delegates to the more detailed
	 * {@link #_find(Object, Class, String, Object, boolean, boolean, int, List)}
	 * method with default fetch amount and result list.
	 *
	 * @param fromObject      the object from which to start the search; may be null
	 * @param clazz           the class of objects to search
	 * @param propertyPath    the property path to match; may be null
	 * @param findObject      the value to compare against
	 * @param bSkipNew        whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no match is found
	 * @return the next matching object in the cache, or {@code null} if none found
	 */
	public static Object _find(Object fromObject, Class clazz, String propertyPath, Object findObject, boolean bSkipNew,
			boolean bThrowException) {
		Class c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService()._find(fromObject, clazz, propertyPath, findObject, bSkipNew, bThrowException);
	}

	/**
	 * Internal helper that searches the cache for an object of the specified class
	 * using the provided {@link OAFinder}. Delegates to the more detailed
	 * {@link #_find(Object, Class, OAFinder, boolean, boolean, int, List)} method
	 * with default fetch amount and result list.
	 *
	 * @param fromObject      the object from which to start the search; may be null
	 * @param clazz           the class of objects to search
	 * @param finder          the finder specifying search criteria
	 * @param bSkipNew        whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no match is found
	 * @return the next matching object in the cache, or {@code null} if none found
	 */
	public static Object _find(Object fromObject, Class clazz, OAFinder finder, boolean bSkipNew, boolean bThrowException) {
		Class c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService()._find(fromObject, clazz, finder, bSkipNew, bThrowException);
	}

	public static Object find(Object fromObject, Class clazz, OAFinder finder, boolean bSkipNew, boolean bThrowException, int fetchAmount,
			List<OAObject> alResults) {
		Class c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().find(fromObject, clazz, finder, bSkipNew, bThrowException, fetchAmount, alResults);
	}

	/**
	 * Searches the cache for objects of the specified class that satisfy the given
	 * {@link OAFilter}. Converts the filter into an {@link OAFinder} internally
	 * and delegates to the core {@link #_find(Object, Class, OAFinder, boolean, boolean, int, List)} method.
	 *
	 * @param fromObject      the object from which to start the search; may be null
	 * @param clazz           the class of objects to search
	 * @param filter          the filter specifying search criteria; may be null
	 * @param bSkipNew        whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no match is found
	 * @param fetchAmount     maximum number of objects to return
	 * @param alResults       list to accumulate found objects; may be null
	 * @return the last matching object found, or {@code null} if none
	 */
	public static Object find(Object fromObject, Class clazz, OAFilter filter, boolean bSkipNew, boolean bThrowException, int fetchAmount,
			List<OAObject> alResults) {
		Class c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().find(fromObject, clazz, filter, bSkipNew, bThrowException, fetchAmount, alResults);
	}

	/**
	 * Searches the cache for objects of the specified class starting from
	 * {@code fromObject}, returning up to {@code fetchAmount} results.
	 * Delegates to the internal {@link #_find(Object, Class, String, Object, boolean, boolean, int, List)}
	 * method with default search parameters.
	 *
	 * @param fromObject  the object from which to start the search; may be null
	 * @param clazz       the class of objects to search
	 * @param fetchAmount maximum number of objects to return
	 * @param alResults   list to accumulate found objects; may be null
	 * @return the last object found, or {@code null} if none
	 */
	public static Object find(Object fromObject, Class clazz, int fetchAmount, List<OAObject> alResults) {
		Class c = clazz;
		if (c == null) return null;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().find(fromObject, clazz, fetchAmount, alResults);
	}


	/**
	 * Refreshes all objects of the specified class from the underlying
	 * {@link OADataSource}. If called on a client, the refresh is performed
	 * asynchronously on the server. Updates all relevant Hubs for the class.
	 * Objects already present in select-all or detail Hubs are checked to
	 * determine if they require refresh.
	 *
	 * @param clazz the class of objects to refresh; if null, no action is taken
	 */
	public static void refresh(Class clazz) {
		Class c = clazz;
		if (c == null) return;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return;
		og.objects().getOAObjectCacheService().refresh(clazz);
	}

	/*
	public static void updateClientInfo(OAClientInfo ci) {
		// LOG.fine("called");
		for (Class c : getOAObjectCache().getClasses()) {
	    	ci.getCacheHashMap().put(c, getOAObjectCache().getTotal(c));
		}
	}
	*/
	
//qqqqqqqqqq	
	
	/**
	 * Enables or disables the object cache globally. When caching is disabled,
	 * methods that would normally retrieve or store objects in the cache will
	 * bypass it, effectively returning objects directly without caching.
	 *
	 * @param b {@code true} to disable caching, {@code false} to enable it
	 */
	public static void setDisableCache(boolean b) {
//qqqqqqqqqqqqqqqqqqq		
//		bDisableCache = b;
	}

	/**
	 * Returns the underlying {@link OAObjectCache} instance used by this delegate.
	 * This provides direct access to low-level cache operations for advanced use cases.
	 *
	 * @return the global {@link OAObjectCache} instance
	 */
	public static OAObjectCache getOAObjectCache() {
//		return objectCache;
		
		return null;//qqqqqqqqqqqqqqqqqqqqqqqq
	}


	public static Object getRandom(Class<? extends OAObject> clazz, int i) {
		Class c = clazz;
		OAGraph og = OARuntime.get().graph(c);
		if (og == null) return null;
		return og.objects().getOAObjectCacheService().getRandom(clazz, i);
	}
	
}

