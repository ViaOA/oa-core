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
package com.viaoa.object;

import java.util.HashSet;

import com.viaoa.hub.Hub;

public class OAObjectAnalyzer {

    
    HashSet<Hub> hsHub = new HashSet<Hub>();

    
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


