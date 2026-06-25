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
package com.viaoa.trigger;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.callback.OACallback;
import com.viaoa.datasource.OADataSource;
import com.viaoa.find.OAFinder;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.lang.OAString;
import com.viaoa.log.OALogger;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;

/*qqqqqqqqqqqq
CODEX

1. file/class/method: src/main/java/com/viaoa/trigger/OATriggerMethodListener.java:151
     OATriggerMethodListener.onTrigger

  concrete bug: The object-cache fallback path swallows trigger method exceptions.

  runtime scenario: A trigger uses bOnlyUseLoadedData, or no datasource exists, or datasource storage is unsupported.
  The listener visits cached objects and invokes the annotated method. If the method throws, lines 156-161 only log
  and continue.

  why this violates OA/OG trigger semantics: The same trigger failure propagates in the direct path, selected-hub
  path, and datasource-select path. In this fallback path, the trigger appears successful even though derived state/
  business logic failed.

  minimal fix direction: Capture the first exception from the callback and rethrow it after callObjectCacheVisit, or
  otherwise make failure observable by the caller.

  suggested CODEX comment location: Before the catch (Exception e) at line 159.


2. file/class/method: src/main/java/com/viaoa/trigger/OATriggerMethodListener.java:169
     OATriggerMethodListener.onTrigger

  concrete bug: The datasource query computes objWhere for Hub add/insert/remove events, but ignores it for non-empty
  propertyPathFromRoot.

  runtime scenario: Root trigger path is something like departments.employees. An employee is added to a department
  hub. masterObject is the Department, hubEvent.getObject() is the Employee, and propertyPathFromRoot points from root
  to the department-side path. Code correctly sets objWhere = masterObject, but line 184 binds hubEvent.getObject()
  instead.

  why this violates OA/OG trigger semantics: The datasource query can search roots where departments = employee
  instead of departments = department, missing eligible root objects and silently skipping required trigger execution.

  minimal fix direction: Bind objWhere in the generated query, or split object-property events from Hub membership
  events so the query parameter matches the path target type.

  suggested CODEX comment location: Around lines 170-184, where objWhere is computed and then not used.
  
>> fix these 

3. file/class/method: src/main/java/com/viaoa/trigger/OATriggerMethodListener.java:192
     OATriggerMethodListener.onTrigger

  concrete bug: The datasource OASelect is not closed on success or failure.

  runtime scenario: Trigger fallback opens an OASelect, iterates selected roots, and invokes trigger methods. If
  iteration completes normally or method.invoke throws, there is no finally cleanup.

  why this violates OA/OG trigger semantics: Trigger execution can leak datasource iterator/result resources, and
  failures during trigger application can leave select resources open while the caller only sees the trigger
  exception.

  minimal fix direction: Wrap sel.select() / iteration in try/finally and close the select in the finally block.

  suggested CODEX comment location: Before sel.select() at line 192.




*/

/**
 * Reflection-based {@link OATriggerListener} that invokes a trigger method on
 * the root object. Used by the {@code @OATriggerMethod} annotation processor.
 * <p>
 * This listener locates all affected root objects (using {@link OAFinder} or
 * {@link OASelect}) and calls the annotated method for each when a dependent
 * property changes.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatically created by {@link OAAnnotationDelegate} for annotated methods.</li>
 *   <li>Optionally limited to loaded objects only.</li>
 *   <li>Falls back to DataSource queries when not fully loaded.</li>
 * </ul>
 *
 * @see OATrigger
 * @see OATriggerListener
 * @see com.viaoa.select.OASelect
 */
public class OATriggerMethodListener implements OATriggerListener {
	private static final Logger LOG = OALogger.getLogger(OATriggerMethodListener.class);
	
	/**
	 * The class that declares the trigger method.
	 * Used to resolve the root object type on which the reflective
	 * invocation will be performed.
	 */
    private final Class<? extends OAObject> clazz;
    
    /**
     * The reflective method to invoke when the trigger fires.
     * Represents the annotated trigger method defined on the root class.
     */
    private final Method method;
    
    /**
     * Flag indicating whether processing must be limited to already-loaded data.
     * When true, the listener avoids performing DataSource queries and uses only
     * in-memory objects.
     */
    private final boolean bOnlyUseLoadedData;
    
    /**
     * Metadata describing the root class associated with this trigger listener.
     * Retrieved from OAObjectInfoDelegate and used to support selection and lookup
     * of affected objects.
     */
    private final OAObjectInfo oi;
    
    /**
     * Creates a trigger listener that uses reflection to invoke the specified
     * method on all affected root objects when a dependent property changes.
     * The listener resolves all matching objects using either loaded data or
     * data source queries based on the supplied flag.
     *
     * @param clazz               the class containing the trigger method
     * @param method              the method to invoke when the trigger fires
     * @param bOnlyUseLoadedData  true to restrict processing to loaded objects
     */
    public OATriggerMethodListener(Class clazz, Method method, boolean bOnlyUseLoadedData) {
        this.clazz = clazz;
        this.method = method;
        this.bOnlyUseLoadedData = bOnlyUseLoadedData;
		final OAGraph og = OARuntime.graph(clazz);
        oi =  og.internal().objects().info().getOAObjectInfo(clazz);
    }
    
    /**
     * Handles a trigger event by invoking the configured method on all root
     * objects affected by the change. If a root object is supplied, the method
     * is invoked directly. Otherwise, matching objects are located using an
     * {@link OAFinder}, cached hubs, or data source queries depending on
     * loading constraints.
     *
     * @param objRoot               the root object to invoke the method on, or null to search
     * @param hubEvent              the event that caused the trigger
     * @param propertyPathFromRoot  the path from the root object to the event source
     * @throws Exception if the reflective invocation fails
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

		final OAGraph og = OARuntime.graph(clazz);
        
        Hub h = og.internal().objects().cache().getSelectAllHub(clazz);
        if (h != null && bOnlyUseLoadedData) {
            for (Object objx : h) {
                if (finder.findFirst((OAObject) objx) == null) continue;
                method.invoke(objx, new Object[] { hubEvent });
            }
            return;
        }
        
        
		OADataSource ds = OARuntime.datasource().get(clazz);
        
        if (bOnlyUseLoadedData || ds == null || !ds.supportsStorage()) {
        	og.internal().objects().cache().visit(clazz, new OACallback() {
                @Override
                public boolean updateObject(Object obj) {
                    if (finder.findFirst((OAObject) obj) == null) return true;
                    try {
                        method.invoke(obj, new Object[] { hubEvent });
                    }
                    catch (Exception e) {
                        LOG.log(Level.WARNING, "Exception calling updateObject for Trigger", e);
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
