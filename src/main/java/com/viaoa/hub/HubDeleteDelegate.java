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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;

import com.viaoa.datasource.OADataSource;
import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.sync.*;

/**
 * Delegate that manages deleting an object from a Hub.
 * @author vvia
 *
 */
public class HubDeleteDelegate {

    public static void deleteAll(Hub thisHub) {
        // 20150206 send to server
        if (thisHub.getSize() == 0) return;
        if (!HubCSDelegate.deleteAll(thisHub)) {
            return;  // Done on server.
        }

        try {
            OAThreadLocalDelegate.setDeleting(thisHub, true);
            OARemoteThreadDelegate.sendMessages(true);
            _runDeleteAll(thisHub);
        }
        finally {
            OARemoteThreadDelegate.sendMessages(false);
            OAThreadLocalDelegate.setDeleting(thisHub, false);
        }
    }
    // only runs on the server
    private static void _runDeleteAll(Hub thisHub) {
        Object[] objs;
        if (thisHub.isOAObject()) objs = thisHub.toArray();
        else objs = null;
        
        HubAddRemoveDelegate.clear(thisHub); // single event to remove all from hub (sent to clients)
        HubDataDelegate.clearHubChanges(thisHub);
        
        if (objs != null) {
            OACascade cascade = new OACascade();
            for (Object obj : objs) {
                OAObjectDeleteDelegate.delete((OAObject)obj, cascade);
            }
            for (Object obj : objs) {
                HubAddRemoveDelegate.remove(thisHub, obj, false, false, true, false, false, true); 
            }
        }
    }
    
    
    public static boolean isDeletingAll(Hub thisHub) {
        return OAThreadLocalDelegate.isDeleting(thisHub);
    }
    
    public static void deleteAll(Hub thisHub, OACascade cascade) {
        if (cascade.wasCascaded(thisHub, true)) return;
        try {
            OAThreadLocalDelegate.setDeleting(thisHub, true);
            OAThreadLocalDelegate.lock(thisHub);
            _deleteAll(thisHub, cascade);
        }
        finally {
            OAThreadLocalDelegate.unlock(thisHub);
            OAThreadLocalDelegate.setDeleting(thisHub, false);
        }
    }

    private static void _deleteAll(Hub thisHub, OACascade cascade) {
        boolean bIsOa = thisHub.isOAObject();
        Object objLast = null;

        // 20121005 need to check to see if a link table was used for a 1toM, where createMethod for One is false
        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(thisHub);
        OALinkInfo liRev = null;
        OAObject masterObj = null;
        OADataSource dataSource = null;
        if (bIsOa && li != null && li.getType() == li.ONE) {
            Method method = OAObjectInfoDelegate.getMethod(li);
            if (method == null || ((method.getModifiers() & Modifier.PRIVATE) == 2) ) {
                // uses a link table, need to delete from link table first
                liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);

                masterObj = HubDetailDelegate.getMasterObject(thisHub);
                if (masterObj != null) dataSource = OADataSource.getDataSource(masterObj.getClass());
            }
        }        


        // 20160615 
        Object[] objs = thisHub.toArray();
        thisHub.data.vector.removeAllElements();
        
        if ((thisHub.datam.getTrackChanges() || thisHub.data.getTrackChanges()) && thisHub.isOAObject()) {
            Vector vecRemove = thisHub.data.getVecRemove();
            int x = vecRemove==null ? 0 : vecRemove.size(); 
            for (Object obj : objs) {
                if (thisHub.data.getVecAdd() != null && thisHub.data.getVecAdd().removeElement(obj)) {
                    // no-op
                }
                else {
                    boolean b = false;
                    for (int i=0; i<x; i++) {
                        if (obj == vecRemove.elementAt(i)) {
                            b = true;
                            break;
                        }
                    }
                    if (!b) {
                        if (vecRemove == null) vecRemove = HubDataDelegate.createVecRemove(thisHub);
                        vecRemove.addElement(obj);
                    }
                }
            }
            HubDataDelegate.setChanged(thisHub, (thisHub.data.getVecAdd() != null && thisHub.data.getVecAdd().size() > 0) || (thisHub.data.getVecRemove() != null && thisHub.data.getVecRemove().size() > 0) );
        }
        else {
            HubDataDelegate.setChanged(thisHub, true);
        }

        for (Object obj : objs) {
            HubAddRemoveDelegate.remove(thisHub, obj, false, true, true, true, true, true);

            if (dataSource != null) {
                dataSource.updateMany2ManyLinks(masterObj, null, new OAObject[] {(OAObject)obj}, liRev.getName());
            }
            
            if (bIsOa) {
                OAObjectDeleteDelegate.delete((OAObject)obj, cascade);
            }
            else {
                if (thisHub.isOAObject() && obj instanceof OAObject) OAObjectDSDelegate.delete((OAObject)obj);
            }
        }
        
        
        
        /* was
        int pos = 0;
        for (; ; ) {
            Object obj = thisHub.elementAt(pos);
            if (obj == null) break;

            if (obj == objLast) {
                // object was not deleted
                pos++;
                continue;
            }
            objLast = obj;
            
            // 20150107
            HubAddRemoveDelegate.remove(thisHub, obj, false, true, true, true, true, true);
            //was:thisHub.remove(obj);  // oaobject.delete will remove object from hubs, this "remove" will make sure that recursive owner reference is removed. 
       
            // 20121005
            if (dataSource != null) {
                dataSource.updateMany2ManyLinks(masterObj, null, new OAObject[] {(OAObject)obj}, liRev.getName());
            }
            
            if (bIsOa) {
                OAObjectDeleteDelegate.delete((OAObject)obj, cascade);
            }
            else {
            	if (thisHub.isOAObject() && obj instanceof OAObject) OAObjectDSDelegate.delete((OAObject)obj);
            }
        }
        */
    	HubDelegate._updateHubAddsAndRemoves(thisHub, -1, cascade, false);
     	
    	thisHub.setChanged(false); // removes all vecAdd, vecRemove objects
    }
}



