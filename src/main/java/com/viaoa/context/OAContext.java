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
package com.viaoa.context;

import java.util.concurrent.ConcurrentHashMap;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.sync.OASync;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAString;

/**
 * Allows storing an object that is associated with a context, that can be used 
 * by EditQuery and other code to work with OAObjects. 
 * 
 * set the JVM default (null) context, by calling OAContext.setContextObject(null, user);
 * set the JSP/Servlet context using oasession, by calling OAContext.setContextHub(oasession, hubLoginUser);
 * set the current threads context, using OAThreadLocalDelegate.setContext(oasession)
 * 
 * @author vvia
 */
public class OAContext {
    private static final ConcurrentHashMap<Object, Hub<? extends OAObject>> hmContextHub = new ConcurrentHashMap<>();

    private static final Object NullContext = new Object();

    private static String adminPropertyPath = "Admin"; 
    private static String allowEditProcessedPropertyPath = "EditProcessed"; 
    
    /**
     * Property path used to find the user property for allowing users to edit objects/properties/etc that are annotatied as processed.
     * Defaults to "EditProcessed"
     */
    public static void setAllowEditProcessedPropertyPath(String pp) {
        OAContext.allowEditProcessedPropertyPath = pp;
    }
    public static String getAllowEditProcessedPropertyPath() {
        return OAContext.allowEditProcessedPropertyPath;
    }

    /**
     * Does the current thread have rights to edit processed objects.
     */
    public static boolean getAllowEditProcessed() {
        Object context = OAThreadLocalDelegate.getContext();
        return getAllowEditProcessed(context);
    }
    /**
     * Does the context have rights to edit processed objects.
     */
    public static boolean getAllowEditProcessed(Object context) {
        if (context == null) context = NullContext;

        // default for main server thread (context=null) is always true
        if (context == NullContext && OASync.isServer()) return true;
        
        if (OAThreadLocalDelegate.getAlwaysAllowEditProcessed()) {
            return true;
        }
        
        if (OAString.isEmpty(allowEditProcessedPropertyPath)) return true;
        OAObject oaObj = getContextObject(context);
        if (oaObj == null) return false;
        
        Object val = oaObj.getProperty(OAContext.allowEditProcessedPropertyPath);
        boolean b = OAConv.toBoolean(val);
        return b;
    }
    

    /**
     * Property path used to find the user property for allowing users to have admin rights.
     * Defaults to "EditProcessed"
     */
    public static void setAdminPropertyPath(String pp) {
        OAContext.adminPropertyPath = pp;
    }
    public static String getAdminPropertyPath() {
        return OAContext.adminPropertyPath;
    }
    /**
     * Does the context have admin rights.
     */
    public static boolean isAdmin() {
        Object context = OAThreadLocalDelegate.getContext();
        return isAdmin(context);
    }
    public static boolean isAdmin(Object context) {
        if (context == null) context = NullContext;

        // default for main server thread (context=null) is always true
        if (context == NullContext && OASync.isServer()) return true;
        
        if (OAThreadLocalDelegate.isAdmin()) {
            return true;
        }
        
        if (OAString.isEmpty(adminPropertyPath)) return true;
        OAObject oaObj = getContextObject(context);
        if (oaObj == null) return false;
        
        Object val = oaObj.getProperty(OAContext.adminPropertyPath);
        boolean b = OAConv.toBoolean(val);
        return b;
    }
    
    
    /**
     * Check to see if context (user) property path is equal to bEqualTo.
     */
    public static boolean isEnabled(final String pp, final boolean bEqualTo) {
        Object context = OAThreadLocalDelegate.getContext();
        return isEnabled(context, pp, bEqualTo);
    }
    public static boolean isEnabled(Object context, final String pp, final boolean bEqualTo) {
        if (context == null) context = NullContext;

        // default for main server thread (context=null) is always true
        if (context == NullContext && OASync.isServer()) return true;
        
        if (OAThreadLocalDelegate.getAlwaysAllowEnabled()) {
            return true;
        }
        
        if (OAString.isEmpty(pp)) return true;
        OAObject oaObj = getContextObject(context);
        if (oaObj == null) return false;
        
        Object val = oaObj.getProperty(pp);
        boolean b = OAConv.toBoolean(val);
        return b == bEqualTo;
    }
    
    
    /**
     * Associated an object value with a context. 
     * @param context is value used to lookup obj
     * @param obj object that is associated with context.
     * @see OAThreadLocalDelegate#getContext()
     */
    public static void setContextObject(Object context, OAObject obj) {
        if (obj == null) return;
        if (context == null) context = NullContext;
        Hub h = new Hub();
        h.add(obj);
        h.setAO(obj);
        setContextHub(context, h);
    }
    public static void setContext(Object context, OAObject obj) {
        setContextObject(context, obj);
    }
    
    /**
     * Returns the object associated with the current thread local (or null) context.
     */
    public static OAObject getContextObject() {
        Object context = OAThreadLocalDelegate.getContext();
        return getContextObject(context);
    }
    public static OAObject getContextObject(Object context) {
        Hub<? extends OAObject> hub = getContextHub(context);
        if (hub == null) return null;
        return hub.getAO();
    }

    /**
     * Allows the value to be associated with a context, to be the ActiveObject in a hub.
     */
    public static void setContextHub(Object context, Hub<? extends OAObject> hub) {
        if (hub == null) return;
        if (context == null) context = NullContext;
        if (hub.getAO() == null) hub.setPos(0);
        hmContextHub.put(context, hub);
    }
    public static void removeContextHub() {
        removeContextHub(NullContext);
    }
    public static void removeContextHub(Object context) {
        if (context == null) context = NullContext;
        hmContextHub.remove(context);
    }
    public static void removeContext(Object context) {
        removeContextHub(context);
    }
    public static void removeContext() {
        removeContextHub(null);
    }

    public static Hub<? extends OAObject> getContextHub() {
        return getContextHub(null);
    }
    public static Hub<? extends OAObject> getContextHub(Object context) {
        // if (context == null)  //todo: qqqqqq look in threadLocal            
        if (context == null) context = NullContext;
        return hmContextHub.get(context); 
    }

    
    
    
}
