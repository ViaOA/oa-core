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

import com.viaoa.object.OAObject;


/*qqqqqqqqqqqqqqqq
CODEX


4. file/class/method: src/main/java/com/viaoa/trigger/OATrigger.java:121 OATrigger constructor / getPaths

  concrete bug: paths is externally mutable after registration, which can leave stale trigger registrations
  behind on unregister.

  runtime scenario: Caller constructs a trigger with a String[], registers it, then mutates the original array or the
  array returned by getPaths(). Later OAObjectInfo.removeTrigger traverses the current property paths to
  remove trigger registrations from linked OAObjectInfo instances. If the paths changed, linked registrations for the
  original paths can remain and still fire after unregister.

  why this violates OA/OG trigger semantics: Removing/unregistering a trigger must prevent future eligible executions.
  External mutation of the trigger’s dependency path array can make unregister incomplete without an obvious failure.

  minimal fix direction: Defensively copy paths in the constructor and return a copy from getPaths().
  Treat dependent trigger arrays similarly if they are intended to be externally visible.

  suggested CODEX comment location: At line 121 and/or line 197.


*/

/**
 * Defines a trigger that reacts to changes within an {@link OAObject} graph.
 * <p>
 * A trigger monitors one or more property paths relative to a root class and
 * executes an {@link OATriggerListener} whenever an event occurs anywhere along
 * those paths. This enables database-trigger-like logic at the object level,
 * supporting dependency propagation, automatic recalculation, or background
 * synchronization tasks.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Supports multiple dependent property paths per trigger.</li>
 *   <li>Can run server-side only or across distributed clients.</li>
 *   <li>Supports asynchronous execution via background threads.</li>
 *   <li>Integrates with OAObjectInfo for automatic event routing.</li>
 * </ul>
 *
 * @see OATriggerDelegate
 * @see OATriggerListener
 */
public class OATrigger {

	/**
	 * The name assigned to this trigger.
	 * Used to uniquely identify the trigger instance within the system.
	 */
	protected String name;
    
	/**
	 * The root class from which all property paths are evaluated.
	 * Serves as the reference type for resolving trigger dependencies.
	 */
	protected Class rootClass;
    
	/**
	 * The list of property paths that this trigger monitors relative to the root class.
	 * Each path represents a dependency that can cause the trigger to fire.
	 */
	protected String[] paths;
    
	/**
	 * The listener invoked when any monitored property path produces an event.
	 * Defines the behavior executed when the trigger fires.
	 */
	protected OATriggerListener triggerListener;
    
	/**
	 * Flag indicating whether trigger evaluation must rely only on already-loaded data.
	 * When true, the trigger will not cause additional data retrieval or loading.
	 */
	protected final boolean bOnlyUseLoadedData; 
    
	/**
	 * Indicates whether this trigger is restricted to server-side execution.
	 * Prevents execution on client environments when set to true.
	 */
	protected final boolean bServerSideOnly;
    
	/**
	 * Determines whether the trigger should always execute in a background thread.
	 * Provides asynchronous processing for trigger events.
	 */
	protected final boolean bUseBackgroundThread;
    
	/**
	 * Indicates that trigger execution may occur in a background thread only if required.
	 * Enables conditional asynchronous processing based on workload or event context.
	 */
	protected final boolean bUseBackgroundThreadIfNeeded;
    
	/**
	 * Optional array of triggers that depend on this trigger.
	 * Used to form dependency chains among multiple triggers.
	 */
	protected OATrigger[] dependentTriggers;
    
    /**
     * Creates a trigger that monitors one or more property paths relative to a
     * given root class and invokes the supplied listener when events occur.
     *
     * @param name                         the trigger name
     * @param rootClass                    the root class from which property paths are evaluated
     * @param triggerListener              the listener to invoke when the trigger fires
     * @param paths                the property paths that this trigger depends on
     * @param bOnlyUseLoadedData           true to restrict evaluation to already-loaded data
     * @param bServerSideOnly              true to limit execution to the server
     * @param bUseBackgroundThread         true to execute the trigger in a background thread
     * @param bUseBackgroundThreadIfNeeded true to run in a background thread only when required
     */
    public OATrigger(
        String name,
        Class rootClass,
        OATriggerListener triggerListener,
        String[] paths, 
        final boolean bOnlyUseLoadedData, 
        final boolean bServerSideOnly, 
        final boolean bUseBackgroundThread,
        final boolean bUseBackgroundThreadIfNeeded)
    {
        this.name = name;
        this.rootClass = rootClass;
        this.paths = paths;
        this.triggerListener = triggerListener;
        this.bOnlyUseLoadedData = bOnlyUseLoadedData;
        this.bServerSideOnly = bServerSideOnly;
        this.bUseBackgroundThread = bUseBackgroundThread;
        this.bUseBackgroundThreadIfNeeded = bUseBackgroundThreadIfNeeded;
    }
    
    /**
     * Creates a trigger that monitors a single property path relative to a root
     * class and invokes the supplied listener when events occur.
     *
     * @param name                         the trigger name
     * @param rootClass                    the root class from which the property path is evaluated
     * @param triggerListener              the listener to invoke when the trigger fires
     * @param path                 the dependent property path
     * @param bOnlyUseLoadedData           true to restrict evaluation to already-loaded data
     * @param bServerSideOnly              true to limit execution to the server
     * @param bUseBackgroundThread         true to execute the trigger in a background thread
     * @param bUseBackgroundThreadIfNeeded true to run in a background thread only when required
     */
    public OATrigger(
        String name,
        Class rootClass,
        OATriggerListener triggerListener,
        String path, 
        final boolean bOnlyUseLoadedData, 
        final boolean bServerSideOnly, 
        final boolean bUseBackgroundThread,
        final boolean bUseBackgroundThreadIfNeeded)
    {
        this.name = name;
        this.rootClass = rootClass;
        this.paths = new String[] {path};
        this.triggerListener = triggerListener;
        this.bOnlyUseLoadedData = bOnlyUseLoadedData;
        this.bServerSideOnly = bServerSideOnly;
        this.bUseBackgroundThread = bUseBackgroundThread;
        this.bUseBackgroundThreadIfNeeded = bUseBackgroundThreadIfNeeded;
    }

    public Class<?> getRootClass() {
    	return rootClass;
    }
    
    /**
     * Returns the triggers that depend on this trigger.
     *
     * @return an array of dependent triggers, or null if none are defined
     */
    public OATrigger[] getDependentTriggers() {
        return dependentTriggers;        
    }

    /**
     * Returns the listener associated with this trigger.
     *
     * @return the trigger listener
     */
    public OATriggerListener getTriggerListener() {
        return triggerListener;
    }
    
    public boolean getUseBackgroundThread() {
    	return bUseBackgroundThread;
    }
    public boolean getServerSideOnly() {
    	return bServerSideOnly;
    }
    public boolean getOnlyUseLoadedData() {
    	return bOnlyUseLoadedData;
    }
    public boolean getUseBackgroundThreadIfNeeded() {
    	return bUseBackgroundThreadIfNeeded;
    }

	public String[] getPaths() {
		return paths;
	}
	
	public String getName() {
		return name;
	}

	public OATrigger[] geDependentTriggers() {
		return  dependentTriggers;
	}
	public void setDependentTriggers(OATrigger[] triggers) {
		dependentTriggers = triggers;
	}
	
}
