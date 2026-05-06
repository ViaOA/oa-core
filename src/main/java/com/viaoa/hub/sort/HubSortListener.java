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
package com.viaoa.hub.sort;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.compare.OAComparator;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.lang.OAArray;
import com.viaoa.object.*;
import com.viaoa.reflect.OAReflect;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;

/**
 * Listens for property and list changes on a {@link Hub} to keep it
 * sorted according to its current sort configuration.
 *
 * <p>This class is created internally by {@link Hub#sort(String, boolean)}
 * and maintains the Hub’s ordering when relevant properties change.
 * It detects both direct property changes on contained objects and
 * indirect updates through nested property paths.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Parse the property path string passed to {@link Hub#sort} and
 *       register listeners for all dependent properties.</li>
 *   <li>When a sort-related property changes, invoke
 *       {@link HubAddRemoveDelegate#sortMove(Hub, OAObject)} to reposition
 *       the object within the Hub.</li>
 *   <li>When the Hub’s entire list changes ({@code onNewList}), call
 *       {@link HubSortDelegate#resort(Hub)} to re-evaluate order.</li>
 *   <li>Support explicit {@link Comparator} sorting as well as automatic
 *       {@link OAComparator} based on property paths.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Used exclusively by {@link Hub} and not intended for direct use.</li>
 *   <li>Each client maintains its own sort order; server synchronization
 *       of sorting is intentionally suppressed.</li>
 *   <li>Implements {@link java.io.Serializable} for Hub graph persistence.</li>
 *   <li>Thread-safe under OA’s single-threaded event model; employs
 *       {@link OAThreadLocalDelegate#callThreadLocalSetSuppressCSMessages(boolean)} to
 *       prevent cross-client event storms.</li>
 * </ul>
 */
public class HubSortListener<TYPE extends OAObject> extends HubListenerAdapter<TYPE> implements java.io.Serializable {
    static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(HubSortListener.class.getName()); 
    
    /**
     * Internal identifier used by Hub listener registration to receive
     * notifications when sort-dependent properties change. Generated from
     * the parsed sort property paths.
     */
    String sortPropertyName = null;  // uniquely generated PropertyName used by hubListener(..,prop) based on property and sort properties 

    /**
     * Array of parsed property paths extracted from the sort expression.
     * Each entry represents a dependent property that triggers re-sorting
     * when modified.
     */
    private String[] sortPropertyPaths;  // parsed sort strings, used as dependent propertyPaths for hubListener
    
    /**
     * Original sort property-path string supplied when the listener was
     * created. May contain multiple properties separated by commas or
     * spaces, and may include optional "asc" or "desc" tokens.
     */
    String propertyPaths;  // orig sort string
    
    /**
     * The Hub whose ordering this listener is responsible for maintaining.
     * All property-change and list-change events are evaluated against this
     * Hub instance.
     */
    Hub<TYPE> hub;
    
    /**
     * Comparator used for sorting Hub contents. If null, an OAComparator
     * will be created automatically based on the property paths.
     */
    Comparator<TYPE> comparator;
    
    /**
     * Indicates whether sorting is ascending (true) or descending (false),
     * applied when OAComparator or property-path sorting is used.
     */
    boolean bAscending;

    /**
     * Creates a HubSortListener that uses property-path-based sorting.
     * Delegates to the full constructor with no explicit comparator.
     *
     * @param hub           the Hub to keep sorted
     * @param propertyPaths property path(s) used for sorting
     * @param bAscending    true for ascending order, false for descending
     */
    public HubSortListener(Hub<TYPE> hub, String propertyPaths, boolean bAscending) {
        this(hub, null, propertyPaths, bAscending);
    }

    /**
     * Creates a HubSortListener using the specified property paths and
     * defaulting to ascending sort order.
     *
     * @param hub           the Hub to keep sorted
     * @param propertyPaths property path(s) used for sorting
     */
    public HubSortListener(Hub<TYPE> hub, String propertyPaths) {
        this(hub, null, propertyPaths, true);
    }

    /**
     * Creates a HubSortListener that uses a custom comparator instead of
     * property-path sorting.
     *
     * @param hub        the Hub to keep sorted
     * @param comparator explicit Comparator to use for ordering
     * @param bAscending true if the comparator’s natural ordering should be
     *                   treated as ascending
     */
    public HubSortListener(Hub<TYPE> hub, Comparator<TYPE> comparator, boolean bAscending) {
        this(hub, comparator, null, bAscending);
    }

    /**
     * Creates a HubSortListener using the provided comparator and defaulting
     * to ascending sort order.
     *
     * @param hub        the Hub to keep sorted
     * @param comparator explicit Comparator for sorting
     */
    public HubSortListener(Hub<TYPE> hub, Comparator<TYPE> comparator) {
        this(hub, comparator, null, true);
    }

    /**
     * Core constructor that initializes sort configuration, parses property
     * paths when needed, creates an OAComparator if no comparator is
     * supplied, and registers the listener with the Hub.
     *
     * @param hub           the Hub whose sort order is maintained
     * @param comparator    custom comparator, or null for property-based sorting
     * @param propertyPaths property-path expression for sorting
     * @param bAscending    true for ascending sort, false for descending
     */
    public HubSortListener(Hub<TYPE> hub, Comparator<TYPE> comparator, String propertyPaths, boolean bAscending) {
        this.hub = hub;
        this.comparator = comparator;
        this.propertyPaths = propertyPaths;
        this.bAscending = bAscending;

        if (comparator == null) {
            setupPropertyPaths();
            if (this.comparator == null) {
                this.comparator = new OAComparator(hub.getObjectClass(), propertyPaths, bAscending);
            }
        }
        hub.addHubListener(this); 
    }

    /**
     * Ensures that listener cleanup occurs if this object is finalized.
     * Invokes {@link #close()} before garbage collection.
     *
     * @throws Throwable if superclass finalization fails
     */
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    /**
     * Returns the parsed list of property paths used for sorting. These
     * property names correspond to dependent values monitored for updates.
     *
     * @return array of sort-dependent property paths, or null
     */
    public String[] getPropeties() {
    	return sortPropertyPaths;
    }

    /**
     * Parses the sort expression into individual property paths, validates
     * their existence using reflection, builds the internal property list,
     * and registers Hub listeners for dependent-property change events.
     */
    protected void setupPropertyPaths() {
    	if (propertyPaths == null) return;

        final Class<? extends OAObject> clazz = hub.getObjectClass();

    	StringTokenizer st = new StringTokenizer(propertyPaths, ", ", true);
        
        sortPropertyPaths = null;
        sortPropertyName = null;
        boolean bAllowType = false;
        
        for ( ; st.hasMoreElements() ; ) {
            String prop = (String) st.nextElement();
            if (prop.equals(" ")) {
                bAllowType = true;
                continue;
            }
            if (prop.equals(",")) {
                bAllowType = false;
                continue;
            }
            if (prop.equalsIgnoreCase("desc")) {
                if (bAllowType) continue;
                // else could be property name
            }
            if (prop.equalsIgnoreCase("asc")) {
                if (bAllowType) continue;
                // else could be property name
            }
            try {
            	OAReflect.getMethods(clazz, prop);
            }
            catch (RuntimeException e) {
                // ignore
                LOG.log(Level.WARNING, "error getting method, will continue.  Class="+clazz+", prop="+prop, e);
                continue;
            }
            
            sortPropertyPaths = (String[]) OAArray.add(String.class, sortPropertyPaths, prop);
            
            if (sortPropertyName == null) sortPropertyName = "";
            else sortPropertyName += "_";
            sortPropertyName += prop.toUpperCase();
        }

        if (sortPropertyName != null) {
            if (sortPropertyPaths != null && sortPropertyPaths.length == 1 && sortPropertyName.indexOf('.') < 0) {
                hub.addHubListener(this, sortPropertyName); // only sorting on one property in the Hub
            }
            else {
                // use a "dummy" name that with get notified when one of the sortPropertyPaths change
                //   dont use '.' in name
                sortPropertyName = "HUBSORT_" + sortPropertyName;  
                sortPropertyName = sortPropertyName.replace('.', '_');  // cant have '.' in property name
                hub.addHubListener(this, sortPropertyName, sortPropertyPaths, false, true);
            }
        }
    }

    /**
     * Removes this listener from the Hub, stopping all sort-related event
     * monitoring and cleanup of listener registrations.
     */
    public void close() {
        hub.removeHubListener(this);
    }

    /**
     * Invoked when the Hub replaces its entire list.
     * Attempts to re-sort the Hub up to three times in case Hub changes
     * occur concurrently from another thread.
     *
     * @param e event describing the list replacement
     */
    public @Override void onNewList(HubEvent<TYPE> e) {
        Hub h = e.getHub();
        if (h == hub) {
    		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
            // 20101009 another thread could be making Hub changes, so this could fail - adding try..catch
            for (int i=0; i<3; i++) {
                try {
                    og.hubsInternal().callHubSortResort(hub);
                    break;
                }
                catch (Exception ex) {
                }
            }
        }
    }
    
    /**
     * Guard flag used to prevent recursive calls to sortMove during
     * property-change handling. Ensures that sort adjustments occur only
     * once per triggering event.
     */
    private boolean bCallingSortMove; // 20141205

    /**
     * Responds to property-change events affecting sort-dependent
     * properties. Temporarily suppresses cross-client messages while
     * repositioning the modified object using {@link HubAddRemoveDelegate#sortMove}.
     *
     * @param e property-change event
     */
    public @Override void afterPropertyChange(HubEvent<TYPE> e) {
        if (bCallingSortMove) return;
        String s = e.getPropertyName();
        if (s != null && s.equalsIgnoreCase(sortPropertyName)) {
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
			boolean bWas = srvcOAThreadLocal.getSendSyncMessages();
            try {
                bCallingSortMove = true;
                srvcOAThreadLocal.setSendSyncMessages(false);  // each client will handle it's own sorting
        		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hub);
                og.hubsInternal().callHubAddRemoveSortMove(hub, e.getObject());
            }
            finally {
                bCallingSortMove = false;
                srvcOAThreadLocal.setSendSyncMessages(bWas);
            }
        }
    }
    
    public Comparator<TYPE> getComparator() {
    	return comparator;
    }
}
