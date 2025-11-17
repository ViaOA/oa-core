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

import com.viaoa.object.OAObject;

/**
 * A derived Hub backed by a source Hub and a {@link HubFilter} that defines inclusion via
 * {@code isUsed(T)}. Membership reflects the filtered subset and updates incrementally as the
 * source changes.
 *
 * <p><b>Behavior from implementation:</b>
 * <ul>
 *   <li>Wraps a {@code HubFilter<T>} bound to a master Hub and delegates {@code isUsed} to the
 *       subclass’s predicate.</li>
 *   <li>Exposes helper methods to add dependent properties on objects or Hubs that affect filter
 *       evaluation, and a {@code refresh()} hook to force re-evaluation.</li>
 * </ul>
 *
 * <p><b>Notes:</b> Retains its own AO/order; composes cleanly with sorting and grouping delegates.
 */
public abstract class FilteredHub<T> extends Hub<T> {
    
    private HubFilter<T> filter;

    public FilteredHub(Hub<T> hubMaster) {
        super(hubMaster.getObjectClass());
    
        filter = new HubFilter<T>(hubMaster, this) {
            @Override
            public boolean isUsed(T object) {
                return FilteredHub.this.isUsed(object);
            }
        };
    }

    public HubFilter<T> getFilter() {
        return filter;
    }
    
    public void addProperty(String prop) {
        filter.addDependentProperty(prop);
    }
    public void addDependentProperty(String prop) {
        filter.addDependentProperty(prop);
    }
    public void addDependentProperty(OAObject obj, String prop) {
        filter.addDependentProperty(obj, prop);
    }
    public void addDependentProperty(Hub hub, String prop) {
        filter.addDependentProperty(hub, prop);
    }

    public void refresh() {
        getFilter().refresh();
    }
    
    protected abstract boolean isUsed(T obj);
    
    
}

