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

    /**
     * Constructs a FilteredHub backed by the specified master Hub. Creates an
     * internal {@link HubFilter} that delegates its {@code isUsed} evaluation
     * to {@link #isUsed(Object)} implemented by the subclass.
     *
     * @param hubMaster the source Hub whose objects are evaluated by this filter.
     */
    public FilteredHub(Hub<T> hubMaster) {
        super(hubMaster.getObjectClass());
    
        filter = new HubFilter<T>(hubMaster, this) {
            @Override
            public boolean isUsed(T object) {
                return FilteredHub.this.isUsed(object);
            }
        };
    }

    /**
     * Returns the internal {@link HubFilter} used to evaluate object inclusion
     * for this filtered Hub.
     *
     * @return the HubFilter associated with this FilteredHub.
     */
    public HubFilter<T> getFilter() {
        return filter;
    }
    
    /**
     * Registers a property name whose changes should cause the filter to
     * re-evaluate objects. Delegates to {@link HubFilter#addDependentProperty(String)}.
     *
     * @param prop the property whose updates can affect filter results.
     */
    public void addProperty(String prop) {
        filter.addDependentProperty(prop);
    }

    /**
     * Registers a property name as a dependency for filter evaluation. Changes
     * to this property trigger re-evaluation. Delegates to
     * {@link HubFilter#addDependentProperty(String)}.
     *
     * @param prop the property whose updates should trigger filter re-evaluation.
     */
    public void addDependentProperty(String prop) {
        filter.addDependentProperty(prop);
    }

    /**
     * Registers a dependent property on a specific object so that changes to
     * that property cause filter re-evaluation. Delegates to
     * {@link HubFilter#addDependentProperty(OAObject, String)}.
     *
     * @param obj  the specific object containing the dependent property.
     * @param prop the property name whose updates trigger re-evaluation.
     */
    public void addDependentProperty(OAObject obj, String prop) {
        filter.addDependentProperty(obj, prop);
    }

    /**
     * Registers a dependent property on all objects within the specified Hub.
     * Property changes trigger filter re-evaluation. Delegates to
     * {@link HubFilter#addDependentProperty(Hub, String)}.
     *
     * @param hub  the Hub whose objects contain the dependent property.
     * @param prop the property name that triggers re-evaluation when modified.
     */
    public void addDependentProperty(Hub hub, String prop) {
        filter.addDependentProperty(hub, prop);
    }

    /**
     * Forces a refresh of the filtered contents by invoking
     * {@link HubFilter#refresh()} on the underlying filter.
     */
    public void refresh() {
        getFilter().refresh();
    }
    
    /**
     * Determines whether the specified object should be included in this filtered
     * Hub. Subclasses must implement the predicate logic.
     *
     * @param obj the object to evaluate.
     * @return true if the object is included in the filtered Hub; false otherwise.
     */
    protected abstract boolean isUsed(T obj);
}

