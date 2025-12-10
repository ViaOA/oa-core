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
    

    public default String getQuery() {
        return null;
    }
    public default String getQuery2() {
        return null;
    }
    
    public default OASiblingHelper getSiblingHelper() {
        return null;
    }
    
    @Override
    default void remove() {
    }
}
