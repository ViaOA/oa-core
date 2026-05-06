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
package com.viaoa.hub.filter;

import com.viaoa.filter.OAFilter;
import com.viaoa.object.OAObject;

/**
 * Defines a customizable filter interface for Hub data sets.
 * <p>
 * This interface extends {@link com.viaoa.filter.OAFilter} to allow additional Hub-aware
 * logic, providing access to the underlying {@link HubFilter} that owns or delegates
 * filtering decisions.
 *
 * <h3>Purpose</h3>
 * <ul>
 *   <li>Enables compound or decorator filters that can evaluate both object
 *       attributes and Hub context.</li>
 *   <li>Allows injection of dynamic filter behavior (e.g., for UI or cascading
 *       filters) without subclassing {@code HubFilter} itself.</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * hub.setFilter(new CustomHubFilter<Employee>() {
 *     public boolean isUsed(Employee emp) {
 *         return emp.isActive();
 *     }
 *     public HubFilter<Employee> getHubFilter() {
 *         return hub.getFilter();
 *     }
 * });
 * }</pre>
 */
public interface CustomHubFilter<TYPE extends OAObject> extends OAFilter<TYPE> {
	
	/**
	 * Returns the {@link HubFilter} associated with this filter, allowing access
	 * to the owning or delegating Hub-level filter logic.
	 *
	 * @return the HubFilter instance used for filtering, or {@code null} if none
	 *         is assigned.
	 */
    HubFilter<TYPE> getHubFilter();
}


