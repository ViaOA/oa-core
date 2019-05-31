package com.viaoa.object;

import java.lang.reflect.Method;

import com.viaoa.scheduler.OAScheduler;
import com.viaoa.util.OADate;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Used to call Scheduler methods on OAObject link properties.
 * @author vvia
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
            method.invoke(objThis, scheduler);
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
            method.invoke(objThis, scheduler);
        }
        catch (Exception e) {
            throw new RuntimeException("exception while invoking scheduler callback method="+method+", for object="+objThis, e);
        }
    }
}
