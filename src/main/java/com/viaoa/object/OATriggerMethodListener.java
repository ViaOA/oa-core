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

import java.lang.reflect.Method;

import com.viaoa.ds.OADataSource;
import com.viaoa.ds.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.util.OAString;

/**
 * used by OAAnnoationDelegate for OATriggerMethod annotation.
 * This will create the necessary triggers that will call the trigger method.
 * @author vvia
 */
public class OATriggerMethodListener implements OATriggerListener {
    private final Class<?> clazz;
    private final Method method;
    private final boolean bOnlyUseLoadedData;
    private final OAObjectInfo oi;
    
    public OATriggerMethodListener(Class clazz, Method method, boolean bOnlyUseLoadedData) {
        this.clazz = clazz;
        this.method = method;
        this.bOnlyUseLoadedData = bOnlyUseLoadedData;
        oi = OAObjectInfoDelegate.getObjectInfo(clazz);
    }
    
    /**
     * called by OAObjectInfo.onChange(..)
     * 
     */
    @Override
    public void onTrigger(OAObject objRoot, final HubEvent hubEvent, String propertyPathFromRoot) throws Exception {
        if (objRoot != null) {
            method.invoke(objRoot, new Object[] { hubEvent });
            return;
        }

        Hub hub = hubEvent.getHub();
        final OAObject masterObject = hub == null ? null : hub.getMasterObject();
        
        // the reverse property could not be used to get objRoot - need to find root objs and call trigger method
        final OAFinder finder = new OAFinder(propertyPathFromRoot) {
            protected boolean isUsed(OAObject obj) {
                if (obj == hubEvent.getObject()) return true;
                if (masterObject == obj) return true;
                return false;
            }
        };
        finder.setUseOnlyLoadedData(bOnlyUseLoadedData);

        Hub h = OAObjectCacheDelegate.getSelectAllHub(clazz);
        if (h != null && bOnlyUseLoadedData) {
            for (Object objx : h) {
                if (finder.findFirst((OAObject) objx) == null) continue;
                method.invoke(objx, new Object[] { hubEvent });
            }
            return;
        }
        
        
        OADataSource ds = OADataSource.getDataSource(clazz);
        
        if (bOnlyUseLoadedData || ds == null || !ds.supportsStorage()) {
            OAObjectCacheDelegate.visit(clazz, new OACallback() {
                @Override
                public boolean updateObject(Object obj) {
                    if (finder.findFirst((OAObject) obj) == null) return true;
                    try {
                        method.invoke(obj, new Object[] { hubEvent });
                    }
                    catch (Exception e) {
                        // TODO: handle exception
                    }
                    return true;
                }
            });
        }
        else {
            // see if a query can be used.
            OASelect sel = null;
            if (hubEvent.getObject() != null) {
                OAObject objWhere;
                if (OAString.isEmpty(hubEvent.getPropertyName()) && masterObject != null) {
                    // if hub add/insert/remove
                    objWhere = masterObject;
                }
                else {
                    objWhere = (OAObject) hubEvent.getObject();
                }

                if (OAString.isEmpty(propertyPathFromRoot)) {
                    sel = new OASelect(oi.getForClass(), objWhere, "");
                }
                else {
                    String query = propertyPathFromRoot + " = ?";
                    sel = new OASelect(oi.getForClass(), query, new Object[] { hubEvent.getObject() }, "");
                }
            }

            if (sel == null) {
                //qqq todo: ??? might want to try reverse search qqqq                            
                sel = new OASelect(oi.getForClass());
            }
            sel.select();
            for (;;) {
                Object objNext = sel.next();
                if (objNext == null) break;
                if (finder.findFirst((OAObject) objNext) != null) {
                    method.invoke(objNext, new Object[] { hubEvent });
                }
            }
        }
    }
}
