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
package com.viaoa.select;

import com.viaoa.filter.OAQueryFilter;

/**
 * Specialized {@link com.viaoa.filter.OAFilter} used during
 * {@link OASelect} processing.
 * <p>
 * {@code OASelectFilter} wraps user-defined filters or query conditions
 * so they can be applied consistently across all DataSource types.
 * It can represent both pre-fetch (native query) and post-fetch
 * (in-memory) filtering logic.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Encapsulate the predicate used to include or exclude objects
 *       returned from a DataSource.</li>
 *   <li>Provide a lightweight bridge between object-graph filters and
 *       native query conditions.</li>
 * </ul>
 *
 * @param <T> the {@link OAObject} type being filtered
 *
 * @see OASelect
 * @see com.viaoa.filter.OAFilter
 */
public class OASelectFilter<T> extends OAQueryFilter<T> {

	/**
	 * Creates a new filter using the specified class, property-path query,
	 * and parameter values. This constructor is used when a query contains
	 * one or more '?' substitution markers.
	 *
	 * @param clazz the object type being filtered
	 * @param query the property-path or native query expression
	 * @param args  parameter values substituted into the query
	 */
    public OASelectFilter(Class<T> clazz, String query, Object[] args) {
        super(clazz, query, args);
    }
    
    /**
     * Creates a new filter using the specified class and query expression.
     * This version is used when filtering does not require parameter
     * substitution.
     *
     * @param clazz the object type being filtered
     * @param query the property-path or native query expression
     */
    public OASelectFilter(Class<T> clazz, String query) {
        super(clazz, query);
    }

}
