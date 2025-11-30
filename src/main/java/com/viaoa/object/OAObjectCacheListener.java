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

import com.viaoa.hub.Hub;

/**
 * Listener interface for receiving cache-level change notifications
 * from the {@link OAObjectCacheDelegate}.
 *
 * <p>Implementations receive callbacks when OAObjects are added,
 * removed, or modified within the global cache.  This enables
 * frameworks and tools to maintain secondary views, analytic hubs,
 * or reactive indexes without scanning the entire cache.</p>
 *
 * <p><b>Events</b>:
 * <ul>
 *   <li>{@link #afterAdd(OAObject)} – object constructed and inserted.</li>
 *   <li>{@link #afterRemove(com.viaoa.hub.Hub, OAObject)} – object removed from a Hub.</li>
 *   <li>{@link #afterPropertyChange(OAObject, String, Object, Object)} – property value changed.</li>
 *   <li>{@link #afterLoad(OAObject)} – object fully loaded.</li>
 * </ul>
 *
 * @param <T> OAObject subtype
 */
public interface OAObjectCacheListener<T extends OAObject> {
    
	/**
	 * Invoked when a property value on the specified object has changed.
	 *
	 * @param obj the object whose property changed
	 * @param propertyName the name of the property that changed
	 * @param oldValue the previous value of the property
	 * @param newValue the new value of the property
	 */
    public void afterPropertyChange(T obj, String propertyName, Object oldValue, Object newValue);

    /**
     * Invoked when a new object is added to the global object cache,
     * typically during its construction.
     *
     * @param obj the newly added object
     */
    public void afterAdd(T obj);
    
    /**
     * Invoked when an object is added to the specified Hub.
     *
     * @param hub the Hub the object was added to
     * @param obj the object that was added
     */
    public void afterAdd(Hub<T> hub, T obj);
    
    /**
     * Invoked when an object is removed from the specified Hub.
     *
     * @param hub the Hub the object was removed from
     * @param obj the object that was removed
     */
    public void afterRemove(Hub<T> hub, T obj);
    
    /**
     * Invoked when an object has finished loading its state.
     *
     * @param obj the object that completed loading
     */
    public void afterLoad(T obj);
    
}
