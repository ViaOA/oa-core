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

import java.lang.reflect.Method;

import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;
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

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectSchedulerService().??(oaObj);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.objects().getOAObjectSchedulerService().??(oaObj);
    */
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		// if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	
	/**
	 * Delegates to {@link #getScheduler(OAObject, String, OAObject, OADate)} using a
	 * {@code null} search object. This provides a convenience overload for retrieving
	 * a scheduler for the specified property and date without supplying a search
	 * filter object.
	 *
	 * @param objThis the object whose scheduler callback method should be invoked
	 * @param property the name of the link property containing the scheduler callback
	 * @param date the date used to initialize the scheduler range
	 * @return the created scheduler, or {@code null} if required arguments are missing
	 */
    public static OAScheduler getScheduler(OAObject objThis, String property, OADate date) {
    	OAGraph g = getGraph(null, objThis);
    	if (g == null) return null;
    	return g.objects().getOAObjectSchedulerService().getScheduler(objThis, property, date);
    }
    
    /**
     * Retrieves an {@link OAScheduler} for a linked property by invoking the scheduler
     * callback method defined in that property's {@link OALinkInfo}. The callback method,
     * if present, is invoked on {@code objThis} with a newly constructed scheduler
     * covering the supplied date.
     *
     * <p>This method performs several validations:</p>
     * <ul>
     *   <li>Ensures {@code objThis} and {@code property} are not null or empty.</li>
     *   <li>Locates the corresponding {@link OALinkInfo} either directly or via a
     *       dot-notation {@link OAPropertyPath}.</li>
     *   <li>Retrieves the scheduler callback {@link Method}, if defined.</li>
     * </ul>
     *
     * <p>If a callback method is found, it is invoked with a scheduler whose start and
     * end dates are both set to the supplied {@code date}. Exceptions during callback
     * execution are wrapped in a {@link RuntimeException}.</p>
     *
     * @param objThis    the object used to locate and invoke the scheduler callback
     * @param property   the link property or property path identifying the callback
     * @param objSearch  an optional object passed to the scheduler constructor
     * @param date       the date used to initialize the scheduler instance
     * @return the populated scheduler, or {@code null} if any required metadata is not found
     */
    public static OAScheduler getScheduler(OAObject objThis, String property, OAObject objSearch, OADate date) {
    	OAGraph g = getGraph(null, objThis);
    	if (g == null) return null;
    	return g.objects().getOAObjectSchedulerService().getScheduler(objThis, property, objSearch, date);
    }
    
    /**
     * Invokes the scheduler callback method defined in the {@link OALinkInfo} for the
     * specified property, passing in the supplied {@link OAScheduler}. If any required
     * argument is missing or if the property lacks a scheduler callback, this method
     * returns silently.
     *
     * <p>The method performs the following steps:</p>
     * <ul>
     *   <li>Validates arguments and retrieves {@link OAObjectInfo} for the object.</li>
     *   <li>Locates the {@link OALinkInfo} for the property.</li>
     *   <li>Retrieves the scheduler callback {@link Method}, if defined.</li>
     *   <li>Invokes the callback, wrapping any exceptions in a {@link RuntimeException}.</li>
     * </ul>
     *
     * @param scheduler the scheduler instance passed to the callback method
     * @param objThis   the object whose callback method should be invoked
     * @param property  the property identifying which scheduler callback to execute
     */
    public static void invokeCallback(OAScheduler scheduler, OAObject objThis, String property) {
    	OAGraph g = getGraph(null, objThis);
    	if (g == null) return;
    	g.objects().getOAObjectSchedulerService().invokeCallback(scheduler, objThis, property);
    }
}
