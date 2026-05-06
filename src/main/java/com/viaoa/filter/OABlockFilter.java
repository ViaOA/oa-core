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
package com.viaoa.filter;

/**
 * Aggregates multiple {@link OAFilter} instances and applies them as a
 * logical AND block.  All contained filters must return {@code true} for
 * the object to be included.
 *
 * <p>
 * This is a convenience wrapper for grouping multiple conditions without
 * nesting several {@link OAAndFilter} objects.
 * </p>
 */
public class OABlockFilter implements OAFilter {
 
	/**
	 * The array of {@link OAFilter} instances that make up this block filter.
	 * All filters in this array must return {@code true} for an evaluated
	 * object to be accepted. May be {@code null} to indicate no filtering.
	 */
    private OAFilter[] filters;
    
    /**
     * Constructs a new block filter using the supplied {@link OAFilter}
     * instances. All provided filters will be evaluated using logical AND.
     *
     * @param filters the filters to aggregate; may be {@code null}
     */
    public OABlockFilter(OAFilter ... filters) {
        this.filters = filters;
    }

    /**
     * Evaluates the supplied object against all contained filters.
     * Returns {@code true} only if every filter returns {@code true}, or if
     * the filter array is {@code null}.
     *
     * @param obj the object to evaluate
     * @return {@code true} if all filters accept the object, otherwise {@code false}
     */
    @Override
    public boolean isUsed(Object obj) {
        if (filters == null) return true;
        for (OAFilter f : filters) {
            if (!f.isUsed(obj)) return false;
        }
        return true;
    }
}
