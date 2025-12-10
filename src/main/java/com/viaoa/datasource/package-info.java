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
/**
 * Core persistence abstraction for the OA framework.
 * <p>
 * The {@code com.viaoa.datasource} package defines OA's unified DataSource
 * architecture — a flexible, pluggable layer that allows {@link com.viaoa.object.OAObject}
 * models to work seamlessly with any persistence provider, including JDBC,
 * REST services, distributed servers, or in-memory caches.
 * <p>
 * The design goal is to completely decouple business models and object graphs
 * from the underlying storage mechanism, while preserving full CRUD semantics,
 * transaction control, and identity consistency across all backends.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.viaoa.datasource.OADataSource} — abstract base class defining the
 *       CRUD and query contract for persistence providers.</li>
 *   <li>{@link com.viaoa.datasource.OADataSourceInterface} — formal interface used by
 *       all implementations to ensure compatibility.</li>
 *   <li>{@link com.viaoa.datasource.OASelect} — executes object-based queries and
 *       streams results through {@link com.viaoa.datasource.OADataSourceIterator}.</li>
 *   <li>{@link com.viaoa.datasource.OADataSourceDelegate} — utility for locating and
 *       managing registered DataSources.</li>
 *   <li>{@link com.viaoa.datasource.OASelectManager} — background manager that monitors
 *       and cleans up active query iterators.</li>
 *   <li>{@link com.viaoa.datasource.OASelectFilter} — filter bridge for in-memory and
 *       DataSource-level selection logic.</li>
 * </ul>
 *
 * <h2>Design Highlights</h2>
 * <ul>
 *   <li>Supports any persistence type (SQL, REST, distributed, cache, custom).</li>
 *   <li>Object-graph queries automatically translated into native query syntax.</li>
 *   <li>Full CRUD lifecycle integration with {@link com.viaoa.object.OAObject}.</li>
 *   <li>Thread-safe registration, iteration, and transaction participation.</li>
 *   <li>Zero code changes required when switching DataSource implementations.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * OADataSource ds = new OADataSourceJDBC("jdbc:mysql://...");
 * OADataSource.register(ds, Customer.class);
 * OASelect<Customer> select = new OASelect<>(Customer.class, "lastName = ?", new Object[]{"Smith"});
 * for (Customer c : select) {
 *     System.out.println(c.getFirstName());
 * }
 * select.close();
 * }</pre>
 *
 * @see com.viaoa.object.OAObject
 * @see com.viaoa.datasource.OADataSource
 * @see com.viaoa.datasource.OASelect
 */
package com.viaoa.datasource;
