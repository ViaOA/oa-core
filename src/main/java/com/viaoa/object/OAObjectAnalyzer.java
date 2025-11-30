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

import java.util.HashSet;

import com.viaoa.hub.Hub;

/**
 * Diagnostic utility that traverses all cached {@link OAObject}s and
 * analyzes their {@link com.viaoa.hub.Hub} memberships.
 *
 * <p>Primarily used for debugging or memory-analysis scenarios to identify
 * objects participating in excessive Hub references or cyclic graphs.</p>
 *
 * <p><b>Functions</b>:
 * <ul>
 *   <li>Iterates over all registered classes in
 *       {@link OAObjectCacheDelegate#getClasses()}.</li>
 *   <li>For each object, collects the set of all Hubs referencing it using
 *       {@link OAObjectHubDelegate#getHubReferences(OAObject)}.</li>
 *   <li>Prints summary output for objects associated with many Hubs.</li>
 * </ul>
 */
public class OAObjectAnalyzer {

    
    HashSet<Hub> hsHub = new HashSet<Hub>();

    
    /**
     * Scans all cached {@link OAObject} instances and reports their
     * {@link com.viaoa.hub.Hub} memberships for diagnostic analysis.
     *
     * <p>The method iterates through all classes registered in the object
     * cache, invoking a callback for each object to count and record the
     * hubs referencing it. Objects associated with more than ten hubs are
     * printed to standard output. A running set of all discovered hubs is
     * maintained for summary inspection.</p>
     */
    public void load() {

        for (Class cs : OAObjectCacheDelegate.getClasses()) {
            System.out.println("Starting class="+cs.getSimpleName()+", total="+OAObjectCacheDelegate.getTotal(cs));
            
            OACallback cb = new OACallback() {
                @Override
                public boolean updateObject(Object object) {
                    OAObject obj = (OAObject) object;
                    Hub[] hubs = OAObjectHubDelegate.getHubReferences(obj);
                    if (hubs == null) return true;
                    int cnt = 0;
                    for (Hub h : hubs) {
                        if (h == null) continue;
                        cnt++;
                        hsHub.add(h);
                    }
                    if (cnt > 10) {
                        System.out.println("   guid="+obj.getObjectKey().getGuid()+", cntHubs="+cnt);
                    }
                    return true;
                }
            };
            OAObjectCacheDelegate.callback(cs, cb);
        }    
        int xx = hsHub.size();
        xx++;
    }

    
    
    
}


