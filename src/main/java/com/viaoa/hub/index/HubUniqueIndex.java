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
package com.viaoa.hub.index;

import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;

/**
 * Maintains a live, thread-safe unique index for a {@link Hub} based on a single
 * property path.
 *
 * <p><b>Responsibilities</b>:
 * <ul>
 *   <li>Listens to Hub add/remove/property-change events to update a concurrent map.</li>
 *   <li>Optionally enforces case sensitivity for String keys.</li>
 *   <li>Supports {@link #get(Object)} lookup in O(1) time.</li>
 * </ul>
 *
 * <p>Used to prevent duplicate keys or perform fast lookups by business identifier.</p>
 */
public class HubUniqueIndex<TYPE extends OAObject> {
    
	/**
	 * The Hub instance whose objects are tracked and indexed. All add/remove
	 * and property-change events originate from this Hub.
	 */
    private final Hub<TYPE> hub;

    /**
     * The name of the property used as the unique index key. Resolved through
     * an {@link OAPath} to support nested property references.
     */
    private final String property;
    
    /**
     * Indicates whether String-based keys should preserve case. If false,
     * all String keys are normalized to uppercase for case-insensitive matching.
     */
    private final boolean bCaseSensitive;
    
    /**
     * Internal HubListener that updates the index in response to Hub events
     * including add, remove, insert, new-list, and property changes.
     */
    private final HubListener<TYPE> listener;
    
    /**
     * Concurrent map maintaining the live unique index. Keys represent the
     * property-path value, and values are the corresponding Hub objects.
     */
    private final ConcurrentHashMap<Object, TYPE> hm = new ConcurrentHashMap<Object, TYPE>();
    
    /**
     * Precompiled property path used to extract index key values from Hub
     * objects efficiently and without reflection overhead during updates.
     */
    private final OAPath<TYPE> propertyPath;
    
    /**
     * Creates a case-insensitive unique index for the specified Hub and
     * property path. Delegates to the full constructor with case-sensitivity
     * disabled.
     *
     * @param hub  the Hub whose objects will be indexed
     * @param prop the property path used as the unique key
     */
    public HubUniqueIndex(Hub<TYPE> hub, String prop) {
        this(hub, prop, false);
    }
    
    /**
     * Creates and initializes a unique index for the specified Hub and property.
     * Installs an internal listener to monitor and update the index whenever
     * items are added, removed, or when the indexed property changes.
     *
     * @param hub            the Hub to monitor
     * @param prop           the property path used as the index key
     * @param bCaseSensitive true to preserve String case, false for case-insensitive keys
     */
    public HubUniqueIndex(Hub<TYPE> hub, String prop, boolean bCaseSensitive) {
        this.hub = hub;
        this.property = prop;
        this.bCaseSensitive = bCaseSensitive;
        this.propertyPath = new OAPath(hub.getObjectClass(), prop);
        
        listener = new HubListenerAdapter<TYPE>() {
            @Override
            public void afterPropertyChange(HubEvent<TYPE> e) {
                if (e == null || !property.equalsIgnoreCase(e.getPropertyName())) return;
                TYPE object = e.getObject();
                if (object == null) return;
                Object old = e.getOldValue();
                if (old != null) {
                    if (!HubUniqueIndex.this.bCaseSensitive && old instanceof String) old = ((String)old).toUpperCase(); 
                    hm.remove(old);
                }
                
                Object value = propertyPath.getValue(object);
                if (value != null) {
                    if (!HubUniqueIndex.this.bCaseSensitive && value instanceof String) value = ((String)value).toUpperCase(); 
                    hm.put(value, object);
                }
            }
            @Override
            public void afterAdd(HubEvent<TYPE> e) {
                add(e.getObject());
            }
            @Override
            public void afterInsert(HubEvent<TYPE> e) {
                add(e.getObject());
            }
            @Override
            public void afterRemove(HubEvent<TYPE> e) {
                if (e == null) return;
                TYPE object = e.getObject();
                if (object == null) return;
                Object value = propertyPath.getValue(object);
                if (value != null) {
                    if (!HubUniqueIndex.this.bCaseSensitive && value instanceof String) value = ((String)value).toUpperCase(); 
                    hm.remove(value);
                }
            }
            @Override
            public void onNewList(HubEvent<TYPE> e) {
                HubUniqueIndex.this.onNewList();
            }
            @Override
            public void afterRemoveAll(HubEvent<TYPE> e) {
                hm.clear();
            }
        };
        hub.addHubListener(listener);
    }

    /**
     * Adds the specified object to the unique index by extracting its property
     * value and storing it in the internal map. Case normalization is applied
     * when required.
     *
     * @param object the object being indexed
     */
    private void add(TYPE object) {
        if (object == null) return;
        Object value = propertyPath.getValue(object);
        if (value != null) {
            if (!HubUniqueIndex.this.bCaseSensitive && value instanceof String) value = ((String)value).toUpperCase(); 
            hm.put(value, object);
        }
    }

    /**
     * Rebuilds the index when the Hub fires a new-list event. Clears the map
     * and re-adds all objects currently in the Hub to maintain consistency.
     */
    private void onNewList() {
        hm.clear();
        for (TYPE object : HubUniqueIndex.this.hub) {
            add(object);
        }
    }
    
    /**
     * Ensures that index cleanup occurs during garbage collection by invoking
     * {@link #close()}. Delegates finalization to the superclass afterward.
     *
     * @throws Throwable if superclass finalization fails
     */
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Unregisters the internal HubListener to stop all indexing activity.
     * Should be called when the index is no longer needed.
     */
    public void close() {
        hub.removeHubListener(listener);
    }

    /**
     * Retrieves the object associated with the given key value from the
     * unique index. Applies case-normalization when appropriate.
     *
     * @param id the key to look up
     * @return the matching object, or null if not found
     */
    public TYPE get(Object id) {
        if (id == null) return null;
        if (!bCaseSensitive && id instanceof String) id = ((String)id).toUpperCase(); 
        return hm.get(id);
    }
    
}
