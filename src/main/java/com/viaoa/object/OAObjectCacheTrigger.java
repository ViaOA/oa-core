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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.service.object.OAObjectCacheService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OARemoteThreadService;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAFilter;

/**
 * Reactive trigger that monitors the global {@link OAObjectCacheDelegate}
 * and invokes {@link #onTrigger(OAObject)} when an object of the target
 * class satisfies specified filter and property-path conditions.
 *
 * <p>OAObjectCacheTrigger generalizes {@link OAObjectCacheFilter} by
 * attaching {@link OATrigger}s to dependent property paths, allowing
 * automatic response when any linked property changes, even across
 * associations.</p>
 *
 * <p><b>Core Behavior</b>:
 * <ul>
 *   <li>Registers a class-level {@link OAObjectCacheListener}.</li>
 *   <li>Evaluates one or more {@link com.viaoa.util.OAFilter}s.</li>
 *   <li>Creates dynamic {@link OATrigger}s for dependent property paths.</li>
 *   <li>Calls {@link #onTrigger(OAObject)} when criteria are met.</li>
 * </ul>
 *
 * <p><b>Usage Example</b>:</p>
 * <pre>
 * new OAObjectCacheTrigger&lt;Invoice&gt;(Invoice.class, inv -&gt; inv.getTotal() &gt; 1000,
 *                                    "customer.region")
 * {
 *     public void onTrigger(Invoice inv) {
 *         alertHighValue(inv);
 *     }
 * };
 * </pre>
 *
 * @param <T> OAObject subtype observed
 */
public abstract class OAObjectCacheTrigger<T extends OAObject> implements OAFilter<T> {
    // Note: this code very similar to OAObjectCacheFilter
    private static final long serialVersionUID = 1L;
    
    /**
     * The class type of objects monitored by this trigger. Determines which cached
     * objects are evaluated for trigger activation.
     */
    private Class<T> clazz;
 
    /**
     * Optional name assigned to the trigger and used when creating the underlying
     * {@link OATrigger}. A unique name is generated if not explicitly set.
     */
    private String name;

    /**
     * Listener registered with {@link OAObjectCacheDelegate} to receive cache-level
     * notifications such as adds, loads, and property changes for monitored objects.
     */
    private OAObjectCacheListener cacheListener;    
    
    /**
     * List of property paths whose changes should cause monitored objects to be
     * re-evaluated for trigger activation.
     */
    private String[] dependentPropertyPaths;
    
    
    /**
     * Counter used to generate a unique identifier for dynamically created trigger
     * names when none is explicitly provided.
     */
    private static final AtomicInteger aiUnique = new AtomicInteger();  

    /**
     * The underlying {@link OATrigger} instance used to observe dependent property
     * paths and route change events back to this trigger.
     */
    private OATrigger trigger;
    
    /**
     * Collection of filters used to determine whether a cached object qualifies for
     * trigger activation. All filters must accept the object.
     */
    private ArrayList<OAFilter<T>> alFilter;

    /**
     * Flag indicating whether trigger execution should temporarily suspend outbound
     * remote message delivery, enabling server-side-only behavior.
     */
    protected boolean bServerSideOnly;
    
    
    
    /**
     * Creates a new cache trigger that monitors all objects of the specified
     * class and triggers {@link #onTrigger(Object)} when matching objects
     * appear in the cache.
     *
     * @param clazz the class to monitor; must not be {@code null}
     * @throws RuntimeException if {@code clazz} is {@code null}
     */
    public OAObjectCacheTrigger(Class clazz) {
        this(clazz, null);
    }
    
    /**
     * Creates a new cache trigger with an initial filter applied. Objects
     * that satisfy the filter and cache-level rules will invoke
     * {@link #onTrigger(Object)}.
     *
     * @param clazz the class to monitor; must not be {@code null}
     * @param filter optional filter applied to determine whether cached
     *               objects should trigger; may be {@code null}
     * @throws RuntimeException if {@code clazz} is {@code null}
     */
    public OAObjectCacheTrigger(Class clazz, OAFilter<T> filter) {
        this(clazz, filter, null);
    }
    
    /**
     * Creates a new cache trigger with an optional filter and dependent
     * property paths. When an object satisfies the filter and a monitored
     * property path changes, {@link #onTrigger(Object)} is invoked.
     *
     * @param clazz the class to monitor; must not be {@code null}
     * @param filter optional filter applied to determine trigger eligibility
     * @param dependentPropPaths property paths whose changes should trigger
     *                           re-evaluation; may be {@code null}
     * @throws RuntimeException if {@code clazz} is {@code null}
     */
    public OAObjectCacheTrigger(Class clazz, OAFilter<T> filter, String ... dependentPropPaths) {
        if (clazz == null) throw new RuntimeException("class can not be null");
        this.clazz = clazz;
 
        if (filter != null) addFilter(filter);
        
        if (dependentPropPaths != null) {
            for (String pp : dependentPropPaths) {
                addDependentProperty(pp);
            }
        }
        
        cacheListener = new OAObjectCacheListener<T>() {
            @Override
            public void afterPropertyChange(T obj, String propertyName, Object oldValue, Object newValue) {
            }
            @Override
            public void afterAdd(T obj) {
                if (obj.isLoading()) return;
                //removed, since this is when it is added to objCache
                //if (OAThreadLocalDelegate.isLoadingObject()) return;
                                
                // new object is created
                if (isUsed((T) obj)) {
                    callOnTrigger(obj);
                }
            }
            @Override
            public void afterAdd(Hub<T> hub, T obj) {
            }
            @Override
            public void afterRemove(Hub<T> hub, T obj) {
            }
            @Override
            public void afterLoad(T obj) {
                afterAdd(obj);
            }
        };        

		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(clazz);
    	final OAObjectCacheService srvcObjectCache = og.getOAObjectService().getOAObjectCacheService();
        
    	srvcObjectCache.addListener(clazz, cacheListener);
        refresh();
    }

    /**
     * Creates a cache trigger based on the class of the supplied Hub's
     * object type. When objects in the cache satisfy the filter and property
     * path conditions, {@link #onTrigger(Object)} is invoked.
     *
     * @param hub the Hub providing the target class; must not be {@code null}
     * @param filter optional filter applied to determine trigger eligibility
     * @param dependentPropPaths property paths whose changes should trigger
     *                           re-evaluation; may be {@code null}
     * @throws RuntimeException if {@code hub} is {@code null}
     */
    public OAObjectCacheTrigger(Hub<T> hub, OAFilter<T> filter, String ... dependentPropPaths) {
        if (hub == null) throw new RuntimeException("hub can not be null");
        clazz = hub.getObjectClass();
 
        if (dependentPropPaths != null) {
            for (String pp : dependentPropPaths) {
                addDependentProperty(pp);
            }
        }
        
        if (filter != null) addFilter(filter, false);
        if (hub.getSize() == 0) {
            refresh();
        }  // else the hub must have been preselected
    }

    
    /**
     * Enables or disables server-side-only behavior for trigger execution.
     * <p>
     * When enabled, outbound remote messages are temporarily suspended
     * during {@link #onTrigger(Object)} execution so that updates initiated
     * from an {@code OAClientThread} propagate correctly.
     * </p>
     *
     * @param b {@code true} to enable server-side-only behavior
     */
    public void setServerSideOnly(boolean b) {
        bServerSideOnly = b;
    }
    
    /**
     * Adds a filter used to determine whether cached objects qualify for
     * triggering and then invokes {@link #refresh()}.
     *
     * @param f the filter to add; ignored if {@code null}
     */
    public void addFilter(OAFilter<T> f) {
        addFilter(f, true); // filter changes what objs are selected, need to refresh
    }

    /**
     * Adds a filter and registers additional dependent property paths. After
     * the filter is added, {@link #refresh()} is invoked to evaluate all
     * cached objects.
     *
     * @param f the filter to add; ignored if {@code null}
     * @param dependentPropPaths additional property paths to monitor
     */
    public void addFilter(OAFilter<T> f, String ... dependentPropPaths) {
        addFilter(f, true);
        if (dependentPropPaths == null) return;
        for (String pp : dependentPropPaths) {
            addDependentProperty(pp);
        }
    }
    
    /**
     * Adds a filter used to determine trigger eligibility.
     * <p>
     * If {@code bCallRefresh} is {@code true}, {@link #refresh()} is invoked
     * after adding the filter.
     * </p>
     *
     * @param f the filter to add; ignored if {@code null}
     * @param bCallRefresh whether to perform an immediate refresh
     */
    public void addFilter(OAFilter<T> f, boolean bCallRefresh) {
        if (f == null) return;
        if (alFilter == null) alFilter = new ArrayList<OAFilter<T>>();
        alFilter.add(f);
        if (bCallRefresh) refresh();
    }

    
    /**
     * Iterates through all cached objects of the monitored class and invokes
     * {@link #onTrigger(Object)} for each object satisfying {@link #isUsed(Object)}.
     */
    public void refresh() {
        // need to check loaded objects 

		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(clazz);
    	final OAObjectCacheService srvcObjectCache = og.getOAObjectService().getOAObjectCacheService();
    	
    	srvcObjectCache.visit(clazz, new OACallback() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean updateObject(Object obj) {
                if (isUsed((T) obj)) {
                    callOnTrigger((T)obj);
                }
                return true;
            }
        });
    }
    
    
    /**
     * Registers a dependent property path whose changes should cause objects
     * to be re-evaluated for triggering. This method updates internal state
     * and installs or reconfigures the underlying trigger.
     *
     * @param prop the dependent property path; ignored if {@code null} or empty
     */
    public void addDependentProperty(final String prop) {
        if (prop == null || prop.length() == 0) return;
        
        dependentPropertyPaths = (String[]) OAArray.add(String.class, dependentPropertyPaths, prop);
        
        // need to recheck in case there was previous changes for the newly added dependentProp that was never checked.  
        setupTrigger();
    }
    
    /**
     * Creates or replaces the underlying {@link OATrigger} used to monitor
     * the configured dependent property paths. When a relevant property
     * change occurs, affected objects are evaluated and may trigger
     * {@link #onTrigger(Object)}.
     */
    protected void setupTrigger() {
        if (trigger != null) {
            OATriggerDelegate.removeTrigger(trigger);
        }

        OATriggerListener<T> triggerListener = new OATriggerListener<T>() {
            @Override
            public void onTrigger(final T rootObject, final HubEvent hubEvent, final String propertyPathFromRoot) throws Exception {
                if (rootObject == null) {
                    Hub hubx = hubEvent.getHub();
                    final OAObject masterObject = hubx == null ? null : hubx.getMasterObject();
                    
                    // the reverse property could not be used to get objRoot 
                    // - need see if any of the rootObjs + pp used the changed obj
                    final OAFinder finder = new OAFinder(propertyPathFromRoot) {
                        protected boolean isUsed(OAObject obj) {
                            if (obj == hubEvent.getObject()) return true;
                            if (masterObject == obj) return true;
                            return false;
                        }
                    };
                    finder.setUseOnlyLoadedData(false);

            		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(clazz);
                	final OAObjectCacheService srvcObjectCache = og.getOAObjectService().getOAObjectCacheService();
                    
                	srvcObjectCache.visit(clazz, new OACallback() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public boolean updateObject(Object obj) {
                            if (finder.findFirst((OAObject) obj) == null) return true;
                            
                            if (isUsed((T) obj)) {
                                callOnTrigger((T) obj);
                            }
                            return true;
                        }
                    });
                }
                else {
                    if (isUsed((T) rootObject)) {
                        callOnTrigger((T) rootObject);
                    }
                }
            }
        };
        
        if (name == null) {
            name = "OAObjectCacheTrigger" + (aiUnique.incrementAndGet());
        }
        
        trigger = new OATrigger(name, clazz, triggerListener, dependentPropertyPaths, true, false, false, true);
        OATriggerDelegate.createTrigger(trigger);
    }
    
    
    /**
     * Removes this trigger's listeners and releases associated resources.
     * After closing, no further trigger notifications will occur.
     */
    public void close() {
        if (trigger == null) {
            OATriggerDelegate.removeTrigger(trigger);
            trigger = null;
        }
        if (cacheListener == null) {
    		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(clazz);
        	final OAObjectCacheService srvcObjectCache = og.getOAObjectService().getOAObjectCacheService();
        	srvcObjectCache.removeListener(clazz, cacheListener);
            cacheListener = null;
        }
    }
    
    /**
     * Ensures that listeners are removed before garbage collection by calling
     * {@link #close()} and then delegating to {@code super.finalize()}.
     */
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
    /**
     * Evaluates whether the specified object satisfies all registered
     * filters. If no filters exist, this method returns {@code true}.
     *
     * @param obj the object to evaluate
     * @return {@code true} if all filters accept the object
     */
    @Override
    public boolean isUsed(T obj) {
        if (alFilter != null) {
            for (OAFilter<T> f : alFilter) {
                if (!f.isUsed(obj)) return false;
            }
        }
        return true;
    }
    
    
    /**
     * Invokes {@link #onTrigger(Object)} for the specified object while
     * applying server-side-only messaging rules if enabled.
     *
     * @param obj the object to trigger on
     */
    private void callOnTrigger(T obj) {
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
        try {
            if (bServerSideOnly) { 
            	srvcOARemoteThread.sendMessages(true);
            }
            onTrigger(obj);
        }
        finally {
            if (bServerSideOnly) {
            	srvcOARemoteThread.sendMessages(false);
            }
        }
    }
    
    /**
     * Invoked when an object satisfies all trigger conditions, including
     * cache-level and filter-level rules.
     *
     * @param obj the object that triggered the event
     */
    public abstract void onTrigger(T obj);
}
