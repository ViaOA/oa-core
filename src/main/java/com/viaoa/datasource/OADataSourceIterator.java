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

import java.util.Iterator;

import com.viaoa.object.OASiblingHelper;

/**
 * Iterator abstraction for streaming objects returned from a
 * {@link OADataSource#select} operation.
 * <p>
 * {@code OADataSourceIterator} provides a uniform way to traverse query results
 * regardless of the underlying persistence provider.  It typically wraps a
 * JDBC {@link java.sql.ResultSet}, a REST response stream, or an in-memory
 * collection.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Forward-only, read-once iteration of {@link OAObject} results.</li>
 *   <li>Optional access to the originating query string or filter.</li>
 *   <li>Safe no-op default implementations for unsupported operations.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * OADataSourceIterator it = ds.select(...);
 * while (it.hasNext()) {
 *     OAObject obj = it.next();
 *     ...
 * }
 * it.remove();  // no-op by default
 * }</pre>
 *
 * @see OADataSource
 * @see OASelect
 */
public interface OADataSourceIterator extends Iterator {
    
	/**
	 * Returns the primary query expression used to produce this iterator,
	 * if available. Default implementation returns {@code null}.
	 *
	 * @return the primary query string, or null if not available
	 */
    public default String getQuery() {
        return null;
    }

    /**
     * Returns a secondary or translated query string associated with this
     * iterator. Default implementation returns {@code null}.
     *
     * @return secondary query string, or null if none exists
     */
    public default String getQuery2() {
        return null;
    }
    
    /**
     * Returns an optional {@link OASiblingHelper} used for tracking sibling
     * relationships within the result set. Default implementation returns
     * {@code null}.
     *
     * @return an OASiblingHelper instance, or null if unsupported
     */
    public default OASiblingHelper getSiblingHelper() {
        return null;
    }
    
    /**
     * No-operation implementation of {@link Iterator#remove()}. Iterators
     * produced by OADataSource typically do not support element removal,
     * and this default method silently ignores the request.
     */
    @Override
    default void remove() {
    }
}
