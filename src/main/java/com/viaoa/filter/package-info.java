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
 * Provides a comprehensive set of {@link com.viaoa.util.OAFilter} subclasses
 * used to evaluate object-level conditions across Hubs, OASelect queries, and
 * the OA Object Graph.  Filters enable declarative, reusable, type-safe
 * selection logic without requiring SQL, reflection-based expression engines,
 * or custom comparator code.
 *
 * <p>
 * OA filters are used in multiple contexts:
 * </p>
 *
 * <ul>
 *   <li><b>Hub filtering</b> – dynamically include/exclude objects in a Hub.</li>
 *   <li><b>Derived Hubs</b> – apply filtering rules to detail collections,
 *       shared Hubs, or linked Hubs.</li>
 *   <li><b>OASelect queries</b> – optionally push filter logic down into the
 *       datasource via {@code updateSelect()}.</li>
 *   <li><b>Finder evaluation</b> – filters attached to an
 *       {@link com.viaoa.util.OAFinder} allow deep filtering across
 *       multi-valued property paths.</li>
 *   <li><b>UI controllers</b> – filter tables, type-ahead lists, and other
 *       interactive components.</li>
 * </ul>
 *
 * <h3>Core filter capabilities</h3>
 *
 * <p>
 * All filters share the following characteristics:
 * </p>
 *
 * <ul>
 *   <li><b>Serializable</b> – filters can be distributed between client and server.</li>
 *   <li><b>Property path aware</b> – supports nested paths via
 *       {@link com.viaoa.object.OAPropertyPath}, including many-relationships.</li>
 *   <li><b>Finder-enabled</b> – multi-valued segments automatically generate
 *       {@link com.viaoa.util.OAFinder} instances with embedded filters.</li>
 *   <li><b>Consistent comparison semantics</b> – all relational logic uses
 *       {@link com.viaoa.util.OACompare} for type-safe evaluation.</li>
 *   <li><b>Composable</b> – filters can be combined through logical AND, OR,
 *       XOR, and block aggregations.</li>
 * </ul>
 *
 * <h3>Operator-style filters</h3>
 *
 * <p>
 * The package contains a wide array of comparison filters:
 * </p>
 *
 * <ul>
 *   <li>Equality / Inequality – {@code OAEqualFilter}, {@code OANotEqualFilter}</li>
 *   <li>Relational – {@code OALessFilter}, {@code OAGreaterFilter}, etc.</li>
 *   <li>Between / BetweenOrEqual – range evaluations</li>
 *   <li>Null / NotNull / Empty / NotEmpty</li>
 *   <li>String pattern matching – {@code OALikeFilter}, {@code OANotLikeFilter},
 *       {@code OAStartsWithFilter}, {@code OAContainsFilter}</li>
 *   <li>Membership – {@code OAInFilter} for arrays, collections, and Hubs</li>
 * </ul>
 *
 * <h3>Composite and logical filters</h3>
 *
 * <ul>
 *   <li>{@code OAAndFilter}</li>
 *   <li>{@code OAOrFilter}</li>
 *   <li>{@code OAXorFilter}</li>
 *   <li>{@code OABlockFilter}</li>
 * </ul>
 *
 * These allow complex multi-condition expressions to be assembled easily.
 *
 * <h3>Expression-based filtering</h3>
 *
 * <p>
 * {@link com.viaoa.filter.OAQueryFilter} provides an OQL/SQL-style expression
 * language that compiles queries such as:
 * </p>
 *
 * <pre>
 *   "lastName LIKE 'S*' AND (age >= 18 OR status = 'VIP')"
 * </pre>
 *
 * <p>
 * The parser converts the expression into a tree of OAFilter objects, enabling
 * powerful declarative filtering directly against the OA Object Graph.
 * </p>
 *
 * <h3>Design philosophy</h3>
 *
 * <p>
 * The filter package is designed for:
 * </p>
 *
 * <ul>
 *   <li><b>Simplicity</b> – each filter performs one role and is easy to test.</li>
 *   <li><b>Reusability</b> – filters can be attached anywhere: Hubs, finders,
 *       selects, or custom logic.</li>
 *   <li><b>Predictability</b> – all comparisons use a unified comparison engine.</li>
 *   <li><b>Performance</b> – no reflection-based evaluation; filters run at
 *       in-memory speeds.</li>
 * </ul>
 *
 * <p>
 * Together, the filters in this package form a comprehensive selection and
 * rule-evaluation framework used throughout OA to shape object graphs, perform
 * searches, enforce constraints, and support dynamic UI behavior.
 * </p>
 */
package com.viaoa.filter;
