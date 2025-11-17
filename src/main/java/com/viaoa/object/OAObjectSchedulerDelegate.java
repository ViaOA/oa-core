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
package com.viaoa.object;

import java.lang.reflect.Method;

import com.viaoa.scheduler.OAScheduler;
import com.viaoa.util.OADate;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Provides callback support for invoking {@link OAScheduler} methods
 * on linked {@link OAObject} relationships.
 *
 * <p>This delegate discovers scheduler callback methods defined on
 * {@link OALinkInfo} metadata and dynamically invokes them to populate
 * or refresh scheduler data.</p>
 *
 * <p><b>Responsibilities</b>:
 * <ul>
 *   <li>Locate the scheduler method via {@link OALinkInfo#getSchedulerMethod()}.</li>
 *   <li>Instantiate and pass an {@link OAScheduler} object covering the
 *       requested date range.</li>
 *   <li>Allow both direct property names and dot-notation property paths.</li>
 * </ul>
 *
 * <p>Used by OA scheduling and calendar integrations to retrieve events
 * or availability linked to an object graph.</p>
 */
public class OAObjectSchedulerDelegate {

    public static OAScheduler getScheduler(OAObject objThis, String property, OADate date) {
        return getScheduler(objThis, property, null, date);
    }
    
    public static OAScheduler getScheduler(OAObject objThis, String property, OAObject objSearch, OADate date) {
        if (objThis == null || OAString.isEmpty(property)) return null;

        OAObjectInfo oi = OAObjectInfoDelegate.getObjectInfo(objThis);
        if (oi == null) return null;
        OALinkInfo li = oi.getLinkInfo(property);
        if (li == null) {
            if (property.indexOf(".") < 0) return null;
            OAPropertyPath pp = new OAPropertyPath(objThis.getClass(), property);
            OALinkInfo[] lis = pp.getLinkInfos();
            if (lis == null || lis.length == 0) return null;
            li = lis[0];
        }
        
        Method method = li.getSchedulerMethod();
        if (method == null) return null;
        
        OAScheduler scheduler = new OAScheduler(objSearch, date, date);
        
        try {
            method.invoke(objThis, new Object[] {scheduler});
        }
        catch (Exception e) {
            throw new RuntimeException("exception while invoking scheduler callback method="+method+", for object="+objThis, e);
        }
        return scheduler;
    }
    
    public static void invokeCallback(OAScheduler scheduler, OAObject objThis, String property) {
        if (scheduler == null || objThis == null || OAString.isEmpty(property)) return;

        OAObjectInfo oi = OAObjectInfoDelegate.getObjectInfo(objThis);
        if (oi == null) return;
        OALinkInfo li = oi.getLinkInfo(property);
        if (li == null) return;
        
        Method method = li.getSchedulerMethod();
        if (method == null) return;
        
        try {
            method.invoke(objThis, new Object[] {scheduler});
        }
        catch (Exception e) {
            throw new RuntimeException("exception while invoking scheduler callback method="+method+", for object="+objThis, e);
        }
    }
}
