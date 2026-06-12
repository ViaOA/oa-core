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
 * Core OA date, time, and date-time value types.
 *
 * <p>
 * This package provides the primary temporal abstractions used throughout the
 * OA runtime, including date-only ({@link com.viaoa.datetime.OADate}),
 * time-only ({@link com.viaoa.datetime.OATime}), and full date-time
 * ({@link com.viaoa.datetime.OADateTime}) values.
 * </p>
 *
 * <p>
 * OA 4.0 datetime classes are implemented on top of the Java {@code java.time}
 * API while preserving the OA programming model and interoperability with
 * legacy APIs such as {@link java.util.Date}, {@link java.util.Calendar},
 * {@link java.sql.Date}, {@link java.sql.Time}, and
 * {@link java.sql.Timestamp}.
 * </p>
 *
 * <h2>Semantic Types</h2>
 *
 * <ul>
 *   <li>
 *   <b>OADate</b> represents a calendar date and maintains date-only semantics.
 *   </li>
 *   <li>
 *   <b>OATime</b> represents a clock time and maintains time-only semantics.
 *   </li>
 *   <li>
 *   <b>OADateTime</b> represents a complete date-time value and supports
 *   timezone-aware operations, conversion, formatting, parsing, and temporal
 *   arithmetic.
 *   </li>
 * </ul>
 *
 * <h2>Timezone Support</h2>
 *
 * <p>
 * OA datetime values support explicit timezone handling through
 * {@link java.time.ZoneId}. Applications may define a default OA timezone while
 * individual instances can preserve their own timezone context when required.
 * </p>
 *
 * <h2>Runtime Usage</h2>
 *
 * <p>
 * These classes are used throughout the OA platform for:
 * </p>
 *
 * <ul>
 *   <li>Object property values</li>
 *   <li>Datasource persistence and queries</li>
 *   <li>Filtering and comparison operations</li>
 *   <li>Scheduling and temporal calculations</li>
 *   <li>Serialization and replication</li>
 *   <li>User interface formatting and parsing</li>
 *   <li>Reporting and template generation</li>
 * </ul>
 *
 * <h2>OA 4.0 Design Goals</h2>
 *
 * <ul>
 *   <li>Deterministic temporal behavior</li>
 *   <li>Consistent semantic type boundaries</li>
 *   <li>Explicit timezone handling</li>
 *   <li>Modern {@code java.time} integration</li>
 *   <li>Backward compatibility with existing OA applications</li>
 * </ul>
 *
 * <p>
 * Detailed behavioral contracts, invariants, and validation requirements are
 * documented in the accompanying CODEX Invariants section within this package.
 * </p>
 *
 * @since OA 4.0
 */
package com.viaoa.datetime;

// CODEX unit tests 20260611

/* CODEX Invariants

*/
