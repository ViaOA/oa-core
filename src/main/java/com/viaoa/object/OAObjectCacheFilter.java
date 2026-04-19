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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectCacheService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.process.OAChangeRefresher;
import com.viaoa.runtime.OARemoteThreadService;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAFilter;

/**
 * Utility filter applied to the global {@link OAObjectCacheDelegate}
 * to automatically add or remove objects from a target {@link com.viaoa.hub.Hub}
 * as they appear, are loaded, or change state.
 *
 * <p>OAObjectCacheFilter monitors all objects of a specified type that
 * match supplied {@link com.viaoa.util.OAFilter} conditions.  Whenever
 * an object enters or leaves the matching set, the filter updates the
 * bound Hub automatically.</p>
 *
 * <p><b>Responsibilities</b>:
 * <ul>
 *   <li>Registers an {@link OAObjectCacheListener} for the class type.</li>
 *   <li>Evaluates one or more filters through {@link #isUsed(OAObject)}.</li>
 *   <li>Adds or removes objects from the Hub in real-time.</li>
 * </ul>
 *
 * Typical usage: a background view Hub that always reflects objects
 * matching live filter criteria, without performing manual selects.
 *
 * @param <T> OAObject subtype managed by this filter
 */
public class OAObjectCacheFilter<T extends OAObject> implements OAFilter<T> {
    private static final long serialVersionUID = 1L;
    
    /**
     * The OAObject class type monitored by this cache filter.
     */
    private Class<T> clazz;
    
    /**
     * Weak reference to the target Hub that receives matching cached objects.
     * Allows the Hub to be garbage-collected without leaking this filter.
     */
    private WeakReference<Hub<T>> wrHub;

    /**
     * Optional internal name used when creating trigger instances.
     */
    private String name;

    /**
     * Listener registered with the global OAObjectCache to monitor add, load,
     * and property-change events for this class type.
     */
    private OAObjectCacheListener cacheListener;    
    
    // list of propPaths to listen for
    /**
     * List of dependent property paths whose changes trigger re-evaluation of
     * cached objects for possible inclusion in the Hub.
     */
    private String[] dependentPropertyPaths;
    
    /**
     * When true, Hub update operations triggered by this filter are treated
     * as server-side only, temporarily suppressing remote messaging.
     */
    protected boolean bServerSideOnly;
    
    /**
     * Counter used to generate unique internal names for triggers.
     */
    private static AtomicInteger aiUnique = new AtomicInteger();  

    /**
     * Trigger object used to monitor dependent property paths and refresh the
     * Hub when affected objects change.
     */
    private OATrigger trigger;
    
    /**
     * Collection of filters that must all return true for an object to be
     * included in the Hub.
     */
    private ArrayList<OAFilter<T>> alFilter;

    /**
     * Background refresher used to batch or delay filter updates when needed,
     * particularly during large or asynchronous change events.
     */
    private volatile OAChangeRefresher changeRefresher;

    
    
	/**
	 * Constructs a new OAObjectCacheFilter that monitors the specified Hub
	 * and automatically updates it with objects from the cache. All cached
	 * objects that satisfy {@link #isUsed(OAObject)} will be added to the Hub.
	 * Delegates to {@link #OAObjectCacheFilter(Hub, OAFilter)} with a null filter.
	 *
	 * @param hub the Hub to be automatically updated; must not be null
	 * @throws RuntimeException if the hub is null
	 */
    public OAObjectCacheFilter(Hub<T> hub) {
        this(hub, null);
    }
    
    /**
     * Constructs a new cache filter bound to the supplied Hub and optional filter.
     * <p>
     * The Hub is updated automatically as objects in the global object cache
     * are added, loaded, or otherwise encountered. Objects are included only
     * when both {@code isUsedFromObjectCache()} (implicit through cache events)
     * and {@link #isUsed(Object)} evaluate to {@code true}.
     * </p>
     *
     * <p>If the Hub is initially empty, a full {@link #reselectAndRefresh()}
     * is performed. Otherwise the Hub is assumed to contain a preselected set
     * and no initial refresh occurs.</p>
     *
     * @param hub the target Hub receiving matching cached objects; must not be {@code null}
     * @param filter optional filter applied to determine whether an object
     *               should be added to the Hub; may be {@code null}
     * @throws RuntimeException if {@code hub} is {@code null}
     */
    public OAObjectCacheFilter(Hub<T> hub, OAFilter<T> filter) {
        if (hub == null) throw new RuntimeException("hub can not be null");
        clazz = hub.getObjectClass();
        wrHub = new WeakReference<Hub<T>>(hub);
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);

        final boolean bEmptyHub = (hub.getSize() == 0);
        
        if (filter != null) addFilter(filter, false);
        
        cacheListener = new OAObjectCacheListener<T>() {
            @Override
            public void afterPropertyChange(T obj, String propertyName, Object oldValue, Object newValue) {
            }
            @Override
            public void afterAdd(T obj) {
                if (obj.isLoading()) return;
                // new object is created
                final Hub<T> hub = wrHub.get();
                if (hub == null) return;
                if (isUsed((T) obj)) {
        			final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
                    if (bServerSideOnly) { 
                        srvcOARemoteThread.sendMessages(true);
                    }
                    hub.add((T) obj);
                    if (bServerSideOnly) { 
                    	srvcOARemoteThread.sendMessages(false);
                    }
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

        og.objectsInternal().callObjectCacheAddListener(clazz, cacheListener);
        
        if (bEmptyHub) {
            reselectAndRefresh();            
        }  // else the hub must have been preselected
    }

    
    /**
     * Constructs a new cache filter bound to the supplied Hub, applying the
     * optional filter and any number of dependent property paths.
     * <p>
     * Each dependent property path is registered so that when the property
     * (or path) changes on any cached object, its eligibility for inclusion
     * in the Hub is re-evaluated. Matching objects are added to the Hub and
     * non-matching ones removed.
     * </p>
     *
     * <p>If the Hub is initially empty, a full {@link #reselectAndRefresh()}
     * is performed. Otherwise the Hub is assumed to be preselected.</p>
     *
     * @param hub the target Hub that will receive matching cached objects;
     *            must not be {@code null}
     * @param filter optional filter used to determine object inclusion;
     *               may be {@code null}
     * @param dependentPropPaths optional property paths that, when changed,
     *                           trigger re-evaluation of affected objects
     * @throws RuntimeException if {@code hub} is {@code null}
     */
    public OAObjectCacheFilter(Hub<T> hub, OAFilter<T> filter, String ... dependentPropPaths) {
        if (hub == null) throw new RuntimeException("hub can not be null");
        clazz = hub.getObjectClass();
        wrHub = new WeakReference<Hub<T>>(hub);
 
        final boolean bEmptyHub = (hub.getSize() == 0);
        
        if (dependentPropPaths != null) {
            for (String pp : dependentPropPaths) {
                addDependentProperty(pp, false);
            }
        }
        
        if (filter != null) addFilter(filter, false);
        if (bEmptyHub) {
            reselectAndRefresh();            
        }  // else the hub must have been preselected
    }
    
    /**
     * Specifies whether Hub update operations triggered by this cache filter
     * should be treated as server-side only.
     * <p>
     * When enabled, outbound remote messages are temporarily suspended and
     * resumed around Hub modifications so that client updates are still
     * published even when initiated on an {@code OAClientThread}.
     * </p>
     *
     * @param b {@code true} to enable server-side-only behavior, {@code false} otherwise
     */
    public void setServerSideOnly(boolean b) {
        bServerSideOnly = b;
    }
    
    
    /**
     * Adds a filter used to determine whether cached objects should be
     * included in the Hub, and then performs a refresh.
     * <p>
     * This method appends the filter to the internal filter list. An object
     * must satisfy all added filters for {@link #isUsed(Object)} to return
     * {@code true}.
     * </p>
     *
     * <p>A full {@link #refresh()} is automatically performed since the
     * selected set may change when a new filter is added.</p>
     *
     * @param f the filter to add; ignored if {@code null}
     */
    public void addFilter(OAFilter<T> f) {
        addFilter(f, true); // filter changes what objs are selected, need to refresh
    }

    /**
     * Adds a filter used to determine whether cached objects should be
     * included in the Hub, and registers additional dependent property paths.
     * <p>
     * This method appends the filter to the internal list and immediately
     * calls {@link #refresh()} to update the Hub. Any supplied dependent
     * property paths are also registered so that changes to those properties
     * trigger re-evaluation of affected objects.
     * </p>
     *
     * @param f the filter to add; ignored if {@code null}
     * @param dependentPropPaths optional property paths that, when changed,
     *                           should cause matching objects to be rechecked
     */
    public void addFilter(OAFilter<T> f, String ... dependentPropPaths) {
        addFilter(f, true);
        if (dependentPropPaths == null) return;
        for (String pp : dependentPropPaths) {
            addDependentProperty(pp);
        }
    }
    
    /**
     * Adds a filter used to determine whether cached objects should be
     * included in the Hub.
     * <p>
     * The filter is appended to the internal filter list. An object must
     * satisfy all added filters for {@link #isUsed(Object)} to return
     * {@code true}.
     * </p>
     *
     * <p>If {@code bCallRefresh} is {@code true}, a {@link #refresh()}
     * is performed to update the Hub based on the expanded filter set.</p>
     *
     * @param f the filter to add; ignored if {@code null}
     * @param bCallRefresh {@code true} to immediately refresh the Hub after
     *                     adding the filter, {@code false} otherwise
     */
    public void addFilter(OAFilter<T> f, boolean bCallRefresh) {
        if (f == null) return;
        if (alFilter == null) alFilter = new ArrayList<OAFilter<T>>();
        alFilter.add(f);
        if (bCallRefresh) refresh();
    }

    /**
     * Reselects data and refreshes the Hub based on current filter settings.
     * <p>
     * If the target Hub is no longer available, this method calls {@link #close()}
     * and returns. Otherwise, the Hub is temporarily placed in a loading state
     * while data is reselected and refreshed.
     * </p>
     *
     * <p>The operation:
     * <ol>
     *   <li>Marks the Hub as loading all data.</li>
     *   <li>Invokes {@link #reselect()} to allow subclasses to repopulate from a data source.</li>
     *   <li>Invokes {@link #refresh()} to apply all filters to cached objects.</li>
     * </ol>
     * </p>
     */
    public void reselectAndRefresh() {
        final Hub<T> hub = wrHub.get();
        if (hub == null) {
            close();
            return;
        }
        // 20190925 dont clear since it removes all in hub.  refresh will remove any that are not needed
        // hub.clear();
        
        
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
        boolean b = og.hubsInternal().callHubDataSetLoadingAllData(hub, true);
        try {
            hub.setLoading(true);
            if (changeRefresher != null && changeRefresher.hasChanged()) return;
            reselect();
            if (changeRefresher != null && changeRefresher.hasChanged()) return;
            refresh();
        }
        finally {
            hub.setLoading(false);
            if (!b) og.hubsInternal().callHubDataSetLoadingAllData(hub, false);
        }
    }
    
    /**
     * Placeholder method invoked during {@link #reselectAndRefresh()} to
     * allow subclasses to reselect data from an external data source.
     * <p>
     * The base implementation performs no work.
     * </p>
     */
    protected void reselect() {
    }
    
    /**
     * Refreshes the Hub by removing objects that no longer satisfy
     * {@link #isUsed(Object)} and by checking cached objects for inclusion.
     * <p>
     * This is a convenience method equivalent to calling
     * {@link #refresh(boolean)} with {@code true}.
     * </p>
     */
    public void refresh() {
        refresh(true);
    }

    /**
     * Refreshes the Hub by synchronizing its contents with the cached objects
     * that satisfy {@link #isUsed(Object)}.
     * <p>
     * The method first removes any objects already in the Hub that no longer
     * meet the filter criteria. It then iterates over all cached objects and
     * adds those that qualify.
     * </p>
     *
     * <p>If {@code bSetLoading} is {@code true}, the Hub is temporarily placed
     * in a loading state for the duration of the refresh. The method also
     * manages the Hub's “load all data” flag and fires a
     * {@link com.viaoa.hub.HubEventDelegate#fireOnNewListEvent} upon
     * completion.</p>
     *
     * @param bSetLoading whether to set the Hub in a loading state during refresh
     */
    public void refresh(final boolean bSetLoading) {
        final Hub<T> hub = wrHub.get();
        if (hub == null) {
            close();
            return;
        }

        // 20191002 
        //was: hub.clear();
        for (T obj : hub) {
            if (!isUsed(obj)) {
                hub.remove(obj);
            }
        }
        if (changeRefresher != null && changeRefresher.hasChanged()) {
            return;
        }
        
        if (bSetLoading) hub.setLoading(true);

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
        boolean b = og.hubsInternal().callHubDataSetLoadingAllData(hub, true);
    	
        try {
            // need to check loaded objects 
        	og.objectsInternal().callObjectCacheVisit(clazz, new OACallback() {
                @SuppressWarnings("unchecked")
                @Override
                public boolean updateObject(Object obj) {
                    if (changeRefresher != null && changeRefresher.hasChanged()) {
                        return false;
                    }
    
                    if (isUsed((T) obj)) {
                        hub.add((T) obj);
                    }
                    return true;
                }
            });
        }
        finally {
            if (bSetLoading) hub.setLoading(false);
            if (!b) og.hubsInternal().callHubDataSetLoadingAllData(hub, false);
            og.hubsInternal().callHubEventFireOnNewListEvent(hub, false);
        }
    }
    
    
    /**
     * Registers a dependent property path and triggers an immediate
     * re-evaluation of cached objects.
     * <p>
     * This is a convenience method equivalent to calling
     * {@link #addDependentProperty(String, boolean)} with {@code true}.
     * </p>
     *
     * @param prop the property path to register; ignored if {@code null} or empty
     */
    public void addDependentProperty(final String prop) {
        addDependentProperty(prop, true);
    }
    
    /**
     * Registers a dependent property path that should trigger re-evaluation
     * of cached objects when the property (or path) changes.
     * <p>
     * The property path is added to the list of dependent paths, a trigger is
     * set up via {@link #setupTrigger()}, and—if {@code bRefresh} is
     * {@code true}—all cached objects are checked to determine whether they
     * should be added to or removed from the Hub.
     * </p>
     *
     * @param prop the property path to register; ignored if {@code null} or empty
     * @param bRefresh {@code true} to immediately recheck cached objects for
     *                 inclusion or removal, {@code false} to skip rechecking
     */
    public void addDependentProperty(final String prop, final boolean bRefresh) {
        if (prop == null || prop.length() == 0) return;
        
        dependentPropertyPaths = (String[]) OAArray.add(String.class, dependentPropertyPaths, prop);
        
        // need to recheck in case there was previous changes for the newly added dependentProp that was never checked.  
        final Hub<T> hub = wrHub.get();
        if (hub == null) {
            close();
            return;
        }
        
        
        setupTrigger();

        if (!bRefresh) return;
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
        if (bServerSideOnly) { 
        	srvcOARemoteThread.sendMessages(true);
        }
        
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
		og.objectsInternal().callObjectCacheVisit(clazz, new OACallback() {
            @Override
            public boolean updateObject(Object obj) {
                if (isUsed((T) obj)) hub.add((T) obj);
                else hub.remove((T) obj);
                return true;
            }
        });
        if (bServerSideOnly) { 
            srvcOARemoteThread.sendMessages(false);
        }
    }

    /**
     * Configures or replaces the internal trigger used to listen for changes
     * on registered dependent property paths.
     * <p>
     * A new {@link OATrigger} is created with a listener that determines
     * whether objects should be added to or removed from the Hub whenever a
     * dependent property (or path) changes. If a previous trigger exists, it
     * is removed before the new one is installed.
     * </p>
     *
     * <p>The listener handles two cases:
     * <ul>
     *   <li>When a root object cannot be determined from the event, a full
     *       {@link #reselectAndRefresh()} or background refresh is performed.</li>
     *   <li>When the root object is available, its inclusion is re-evaluated
     *       using {@link #isUsed(Object)}.</li>
     * </ul>
     * </p>
     */
    protected void setupTrigger() {
        OATriggerListener<T> triggerListener = new OATriggerListener<T>() {
            
            @Override
            public void onTrigger(final T rootObject, final HubEvent hubEvent, final String propertyPathFromRoot) throws Exception {
                final Hub<T> hub = wrHub.get();
                if (hub == null) {
                    return;
                }
                
                if (rootObject == null) {
                    // could not get from event object to T object(s)
                    if (trigger != null && (trigger.bUseBackgroundThread || trigger.bUseBackgroundThreadIfNeeded) ) {
                        if (changeRefresher == null) {
                            synchronized (this) {
                                if (changeRefresher == null) {
                                    changeRefresher = new OAChangeRefresher() {
                                        @Override
                                        protected void process() throws Exception {
                                    		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
                                            boolean bWasLoadingAllData = og.hubsInternal().callHubDataSetLoadingAllData(hub, true);
                                            try {
                                                reselectAndRefresh();
                                            }
                                            finally {
                                                if (!hasChanged()) {
                                                    if (!bWasLoadingAllData) og.hubsInternal().callHubDataSetLoadingAllData(hub, false);
                                                }
                                            }
                                        }
                                    };
                                    changeRefresher.start();
                                }
                            }
                        }
                        // need to flag that all data will be loaded in another thread
                		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
                        og.hubsInternal().callHubDataSetLoadingAllData(hub, true, changeRefresher.getThread());
                        changeRefresher.refresh();
                    }
                    else {
                        reselectAndRefresh();            
                    }
                    
                    /* was:
                    Hub hubx = hubEvent.getHub();
                    final OAObject masterObject = hubx == null ? null : hubx.getMasterObject();
                    
                    // the reverse property could not be used to get objRoot 
                    // - need to see if any of the rootObjs + pp used the changed obj
                    final OAFinder finder = new OAFinder(propertyPathFromRoot) {
                        protected boolean isUsed(OAObject obj) {
                            if (obj == hubEvent.getObject()) return true;
                            if (masterObject == obj) return true;
                            return false;
                        }
                    };
                    finder.setUseOnlyLoadedData(false);

                    if (bServerSideOnly) { 
                        OARemoteThreadDelegate.sendMessages(true);
                    }
                    OAObjectCacheDelegate.visit(clazz, new OACallback() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public boolean updateObject(Object obj) {
                            if (finder.findFirst((OAObject) obj) == null) return true;
                            
                            if (isUsed((T) obj)) {
                                hub.add((T) obj);
                            }
                            else {
                                hub.remove((T) obj);
                            }
                            return true;
                        }
                    });
                    if (bServerSideOnly) { 
                        OARemoteThreadDelegate.sendMessages(false);
                    }
                    */
                }
                else {
        			final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
                    if (bServerSideOnly) { 
                    	srvcOARemoteThread.sendMessages(true);
                    }
                    if (isUsed((T) rootObject)) hub.add((T) rootObject);
                    else hub.remove((T) rootObject);
                    if (bServerSideOnly) { 
                    	srvcOARemoteThread.sendMessages(false);
                    }
                }
            }
        };
        
        if (trigger != null) {
            OATriggerDelegate.removeTrigger(trigger);
        }
        
        if (name == null) {
            name = "OAObjectCacheFilter" + (aiUnique.incrementAndGet());
        }
        
        trigger = new OATrigger(name, clazz, triggerListener, dependentPropertyPaths, true, false, false, true);
        OATriggerDelegate.createTrigger(trigger);
    }
    
    
    /**
     * Closes this cache filter by removing its trigger and cache listener.
     * <p>
     * If the trigger exists, it is removed from {@link OATriggerDelegate}.
     * If the cache listener exists, it is removed from
     * {@link OAObjectCacheDelegate}. After removal, references are cleared.
     * </p>
     * <p>
     * Once closed, the filter will no longer update the Hub in response to
     * cache or property change events.
     * </p>
     */
    public void close() {
        if (trigger == null) {
            OATriggerDelegate.removeTrigger(trigger);
            trigger = null;
        }
        if (cacheListener == null) {
    		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
        	        	
    		og.objectsInternal().callObjectCacheRemoveListener(clazz, cacheListener);
            cacheListener = null;
        }
    }
    
    /**
     * Ensures that this filter is closed before garbage collection.
     * <p>
     * Invokes {@link #close()} and then delegates to {@code super.finalize()}.
     * </p>
     */
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
    /**
     * Determines whether the specified object satisfies all registered
     * filters and should therefore be included in the Hub.
     * <p>
     * If no filters have been added, this method returns {@code false}.
     * Otherwise, each filter in the internal list is evaluated, and the
     * object is considered usable only if every filter returns {@code true}.
     * </p>
     *
     * @param obj the object to evaluate
     * @return {@code true} if all filters accept the object, otherwise {@code false}
     */
    @Override
    public boolean isUsed(T obj) {
        if (alFilter == null) {
            return false;
        }
        
        for (OAFilter<T> f : alFilter) {
            if (!f.isUsed(obj)) return false;
        }
        return true;
    }
}
