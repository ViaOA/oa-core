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
package com.viaoa.hub.trigger;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.filter.HubFilter;
import com.viaoa.object.OAObject;

/**
 * Watches a {@link Hub} and invokes a trigger callback whenever an
 * object enters the filtered view defined by this {@code HubFilter}.
 *
 * <p>{@code HubTrigger} extends {@link HubFilter} and behaves like a
 * transient rule or alert mechanism: when an object becomes visible in
 * the filtered list (i.e., satisfies the {@link OAFilter} criteria),
 * {@link #onTrigger(OAObject)} is called immediately.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * Hub<Order> hubAll = new Hub<>(Order.class);
 *
 * new HubTrigger<>(hubAll, order -> order.getTotal() > 1000) {
 *     @Override
 *     public void onTrigger(Order order) {
 *         System.out.println("High-value order: " + order.getId());
 *     }
 * };
 * }</pre>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Monitor its master Hub and maintain a filtered view using
 *       {@link HubFilter} semantics.</li>
 *   <li>Invoke {@link #onTrigger(OAObject)} when a new object appears
 *       in the filter result after Hub updates.</li>
 *   <li>Ignore notifications during initialization to prevent duplicate
 *       triggers when rebuilding the list.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Lightweight, stateless utility for rule-based Hub monitoring.</li>
 *   <li>Supports property-path dependencies so triggers fire when any
 *       dependent property changes.</li>
 *   <li>Safe under OA’s single-threaded event model.</li>
 * </ul>
 */
public abstract class HubTrigger<T extends OAObject> extends HubFilter<T> {
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a HubTrigger that monitors the given master Hub using no filter
     * and no dependent property paths. Delegates to the superclass constructor
     * to initialize filtered-Hub behavior.
     *
     * @param hubMaster the master Hub to observe for trigger events
     */
    public HubTrigger(Hub<T> hubMaster) {
        super(hubMaster, null);
    }
    
    /**
     * Creates a HubTrigger with an explicit filter and optional dependent
     * property paths. When objects satisfy the filter and enter the filtered
     * view, {@link #onTrigger(OAObject)} is invoked.
     *
     * @param hubMaster               the Hub being monitored
     * @param filter                  filter determining which objects enter the trigger view
     * @param dependentPropertyPaths  optional property paths whose changes may affect filtering
     */
    public HubTrigger(Hub<T> hubMaster, OAFilter filter, String ... dependentPropertyPaths) {
        super(hubMaster, null, filter, dependentPropertyPaths);
    }

    /**
     * Adds an object to the filtered view. If this is not part of initial
     * HubFilter population, invokes {@link #onTrigger(OAObject)} to signal
     * that the object newly satisfies the trigger criteria.
     *
     * @param obj             the object entering the filtered view
     * @param bIsInitialzing  true if called during initialization, false otherwise
     */
    @Override
    protected void addObject(T obj, boolean bIsInitialzing) {
        super.addObject(obj, bIsInitialzing);
        if (bIsInitialzing) return;
        onTrigger(obj);
    }
    
    /**
     * Removes the object from the filtered view. Delegates entirely to the
     * superclass removal logic without additional trigger behavior.
     *
     * @param obj the object being removed from the filtered view
     */
    @Override
    protected void removeObject(T obj) {
        super.removeObject(obj);
    }
    
    /**
     * Callback invoked whenever an object newly satisfies the trigger criteria
     * and enters the filtered view. Subclasses implement application-specific
     * response logic.
     *
     * @param obj the object causing the trigger event
     */
    public abstract void onTrigger(T obj);
}
