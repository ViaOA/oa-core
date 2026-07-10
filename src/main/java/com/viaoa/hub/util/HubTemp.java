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
package com.viaoa.hub.util;

import java.util.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import java.lang.ref.*; 

/**
 * Provides lightweight temporary {@link Hub} instances for single
 * {@link OAObject} references that are not currently contained in
 * any Hub.
 *
 * <p>OA components and delegates occasionally require a Hub context
 * to perform operations—such as binding, event firing, or property
 * path resolution—even when an object has not yet been added to a Hub.
 * {@code HubTemp} creates and caches such temporary Hubs as needed.</p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Each distinct object instance is associated with a temporary
 *       Hub stored in a weakly referenced cache keyed by its class
 *       and object identity.</li>
 *   <li>Subsequent calls to {@link #createHub(Object)} for the same
 *       object return the same temporary Hub while incrementing an
 *       internal reference counter.</li>
 *   <li>When {@link #deleteHub(Object)} is called enough times to
 *       reduce the count to zero, the entry is removed and eligible
 *       for garbage collection.</li>
 *   <li>All mappings use {@link WeakReference} so that both the Hub
 *       and object can be reclaimed when no longer used.</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * Customer c = new Customer();
 * Hub<Customer> hub = HubTemp.createHub(c);
 * // hub contains only c and has c as its active object
 * ...
 * HubTemp.deleteHub(c); // release when done
 * }</pre>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Intended only for internal OA use; not for general application logic.</li>
 *   <li>Provides thread-safe creation and removal of cached temporary Hubs.</li>
 *   <li>Ensures that object–Hub identity integrity is maintained for
 *       operations requiring a Hub context.</li>
 * </ul>
 */
public class HubTemp<TYPE extends OAObject> {

	/**
	 * The temporary Hub instance associated with a single object. Created on
	 * demand and used to provide a Hub context for operations requiring one.
	 */
    Hub<TYPE> hub;

    /**
     * The underlying object represented by this temporary Hub. Used as the key
     * for cached lookup and identity validation.
     */
    TYPE object;

    /**
     * Reference count indicating how many callers currently require this
     * temporary Hub. When decremented to zero, the HubTemp becomes eligible
     * for removal from the cache.
     */
    int cnt;

    /**
     * Global cache mapping each object type to a per-object map of weakly
     * referenced HubTemp instances. Ensures reuse of temporary Hubs while
     * allowing garbage collection when no longer referenced.
     */
    private static final Map<Class, Map<Object, WeakReference<HubTemp>>> hmClass = new HashMap<>();

    /**
     * Retrieves (or lazily creates) the per-class map used to store weak
     * references to HubTemp instances. Thread-safe through synchronization
     * on the shared class map.
     *
     * @param c the class whose temporary-Hub map is requested
     * @return the map associated with that class, or null if class is null
     */
    static Map getMap(Class c) {
        if (c == null) return null;
        Map hm = (Map) hmClass.get(c);
        if (hm == null) {
            synchronized (hmClass) {
                hm = hmClass.get(c);
                if (hm == null) {
                    hm = new HashMap();
                    hmClass.put(c, hm);
                }
            }
        }
        return hm;
    }

    /**
     * Creates or retrieves a temporary Hub for the specified object. If a
     * HubTemp already exists for the object, increments its reference count.
     * Otherwise creates a new Hub containing the object as its sole member
     * and sets it as the active object.
     *
     * @param hubObject the object requiring a temporary Hub
     * @return the associated Hub, or null if {@code hubObject} is null
     */
    public static Hub createHub(Object hubObject) {
        if (hubObject == null) return null;

        Map<Object, WeakReference<HubTemp>> hm = getMap(hubObject.getClass());

        HubTemp ht = null;
        synchronized (hm) {
            WeakReference ref = hm.get(hubObject);
            if (ref != null) ht = (HubTemp) ref.get();
            if (ht != null) ht.cnt++;
            else {
                ht = new HubTemp();
                ht.hub = new Hub(hubObject.getClass());
                ht.object = (OAObject) hubObject;
                ht.cnt = 1;
                ht.hub.add( (OAObject) hubObject);
                ht.hub.setActiveObject(0);
                hm.put(hubObject, new WeakReference(ht));
            }
        }
        return ht.hub;
    }

    /**
     * Returns the reference count of the temporary Hub associated with the
     * given object. If no temporary Hub exists or if it has been reclaimed,
     * returns zero.
     *
     * @param hubObject the object whose HubTemp reference count is requested
     * @return the number of outstanding references
     */
    public static int getCount(Object hubObject) {
        if (hubObject == null) return 0;

        Map hm = getMap(hubObject.getClass());
        synchronized (hm) {
            WeakReference ref = (WeakReference) hm.get(hubObject);
            if (ref == null) return 0;
            HubTemp ht = (HubTemp) ref.get();
            if (ht == null) return 0;
            return ht.cnt;
        }
    }

    /**
     * Decrements the reference count of the temporary Hub associated with the
     * specified object. Once the count reaches zero (or the HubTemp has been
     * GC-collected), removes the entry from the per-class map.
     *
     * @param hubObject the object whose temporary Hub should be released
     */
    public static void deleteHub(Object hubObject) {
        if (hubObject == null) return;

        Map<Object, WeakReference> hm = getMap(hubObject.getClass());
        if (hm == null) return;

        WeakReference<HubTemp> ref;
        synchronized (hm) {
        	ref = hm.get(hubObject); 
        	if (ref == null) return;
            HubTemp ht = ref.get(); 
            if (ht == null || (ht.object == hubObject && (--ht.cnt) == 0) ) hm.remove(hubObject);
        }
    }

    /**
     * Returns the total number of active temporary Hub entries across all
     * classes in the global cache.
     *
     * @return count of active temporary Hub mappings
     */
    public static int getCount() {
    	int cnt = 0;
        for (Class c : hmClass.keySet()) {
            Map h = getMap(c); 
            if (h != null) cnt += h.size();
        }
        return cnt;
    }
}

