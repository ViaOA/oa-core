/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
package com.viaoa.hub;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.object.*;
import com.viaoa.util.*;

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
 *       {@link OAThreadLocalDelegate#setSuppressCSMessages(boolean)} to
 *       prevent cross-client event storms.</li>
 * </ul>
 */
public class HubSortListener extends HubListenerAdapter implements java.io.Serializable {
    static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(HubSortListener.class.getName()); 
    
    String sortPropertyName = null;  // uniquely generated PropertyName used by hubListener(..,prop) based on property and sort properties 
    private String[] sortPropertyPaths;  // parsed sort strings, used as dependent propertyPaths for hubListener
    String propertyPaths;  // orig sort string
    
    Hub hub;
    Comparator comparator;
    boolean bAscending;

    /**
      Used by Hub for sorting objects.
      @param propertyPaths list of property paths ( comma or space delimited).  Can include "asc" or "desc" after
      a propertyPath name.
      All property paths will be listened to, so that changes to them will updated the sorted Hub.
      @see OAComparator#OAComparator
      @see Hub#sort instead of using this object directly.
    */
    public HubSortListener(Hub hub, String propertyPaths, boolean bAscending) {
        this(hub, null, propertyPaths, bAscending);
    }
    public HubSortListener(Hub hub, String propertyPaths) {
        this(hub, null, propertyPaths, true);
    }


    public HubSortListener(Hub hub, Comparator comparator, boolean bAscending) {
        this(hub, comparator, null, bAscending);
    }
    public HubSortListener(Hub hub, Comparator comparator) {
        this(hub, comparator, null, true);
    }


    public HubSortListener(Hub hub, Comparator comparator, String propertyPaths, boolean bAscending) {
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

    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    public String[] getPropeties() {
    	return sortPropertyPaths;
    }

    
    protected void setupPropertyPaths() {
    	if (propertyPaths == null) return;

        final Class clazz = hub.getObjectClass();

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

    public void close() {
        hub.removeHubListener(this);
    }

    // if detail hub changes then one of the properties has changed
    public @Override void onNewList(HubEvent e) {
        Hub h = (Hub) e.getSource();
        if (h == hub) {
            // 20101009 another thread could be making Hub changes, so this could fail - adding try..catch
            for (int i=0; i<3; i++) {
                try {
                    HubSortDelegate.resort(hub);
                    break;
                }
                catch (Exception ex) {
                }
            }
        }
    }
    
    private boolean bCallingSortMove; // 20141205
    public @Override void afterPropertyChange(HubEvent e) {
        if (bCallingSortMove) return;
        String s = e.getPropertyName();
        if (s != null && s.equalsIgnoreCase(sortPropertyName)) {
            try {
                bCallingSortMove = true;
                OAThreadLocalDelegate.setSuppressCSMessages(true);  // each client will handle it's own sorting
                HubAddRemoveDelegate.sortMove(hub, e.getObject());
            }
            finally {
                bCallingSortMove = false;
                OAThreadLocalDelegate.setSuppressCSMessages(false);
            }
        }
    }

}



