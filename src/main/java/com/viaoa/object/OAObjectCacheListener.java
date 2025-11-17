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
     * Called when there is a change to an object.
     */
    public void afterPropertyChange(T obj, String propertyName, Object oldValue, Object newValue);

    /** 
     * called when a new object is added to OAObjectCache, during the object construction. 
     */
    public void afterAdd(T obj);
    
    public void afterAdd(Hub<T> hub, T obj);
    
    public void afterRemove(Hub<T> hub, T obj);
    
    public void afterLoad(T obj);
    
}
