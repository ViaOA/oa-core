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
package com.viaoa.datasource;

import java.util.List;

/**
 * {@link OADataSourceIterator} implementation backed by a {@link List}.
 * Provides simple, forward-only iteration over the supplied list. This is
 * typically used by DataSource implementations that already have results
 * materialized in memory and do not require streaming or cursor-based access.
 * <p>
 * Behavior characteristics:
 * <ul>
 *   <li>Iteration stops when the current position exceeds the list size.</li>
 *   <li>Returned objects are retrieved using {@link List#get(int)}.</li>
 *   <li>No removal or mutation operations are supported.</li>
 * </ul>
 */
public class OADataSourceListIterator implements OADataSourceIterator {
	/**
	 * The underlying list being iterated. Elements are returned sequentially
	 * using {@link List#get(int)}. May be null if no results are available.
	 */
    private List al;

    /**
     * The zero-based index of the next element to be returned from the list.
     * Incremented after each successful {@link #next()} call.
     */
    private int pos;
    
    /**
     * Creates a new iterator over the specified list.
     *
     * @param list the list to iterate over; may be null
     */
    public OADataSourceListIterator(List list) {
        this.al = list;
    }
    
    /**
     * Indicates whether additional elements are available in the underlying list.
     * Returns {@code true} when the list is non-null and the current position is
     * less than the list size.
     *
     * @return true if another element can be returned
     */
    @Override
    public boolean hasNext() {
        return al != null && pos < al.size();
    }
    
    /**
     * Returns the next element in the list and advances the iterator position.
     * If no further elements are available, the method returns {@code null}.
     *
     * @return the next list element, or null if iteration has ended
     */
    @Override
    public Object next() {
        if (!hasNext()) return null;
        return al.get(pos++);
    }
}
