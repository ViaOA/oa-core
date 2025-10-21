/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.hub;
import java.util.*;
import java.lang.ref.*; 

/** 
    Used by OA components to create temporary hubs when using Object without a Hub.
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

