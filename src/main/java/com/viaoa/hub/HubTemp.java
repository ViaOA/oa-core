/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
public class HubTemp {
    Hub hub;
    Object object;
    int cnt;

    /** 
        Temp Hub objects used when a Hub is needed for an OAObject that does not have a Hub.
    */
    private static final Map<Class, Map<Object, WeakReference<HubTemp>>> hmClass = new HashMap<>();
    
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
                ht.object = hubObject;
                ht.cnt = 1;
                ht.hub.add(hubObject);
                ht.hub.setActiveObject(0);
                hm.put(hubObject, new WeakReference(ht));
            }
        }
        return ht.hub;
    }
    
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
    
    public static int getCount() {
    	int cnt = 0;
        for (Class c : hmClass.keySet()) {
            Map h = getMap(c); 
            if (h != null) cnt += h.size();
        }    
        return cnt;
    }
}

