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
 * OA date, time, and date-time value types.
 * <p>
 * This package provides OA-compatible temporal classes for date-only
 * ({@link com.viaoa.datetime.OADate}), time-only
 * ({@link com.viaoa.datetime.OATime}), full date-time
 * ({@link com.viaoa.datetime.OADateTime}), and timezone lookup
 * ({@link com.viaoa.datetime.OATimeZone}) behavior.
 * <p>
 * The classes are backed by Java {@code java.time} concepts while preserving
 * compatibility with legacy OA applications and Java date APIs such as
 * {@link java.util.Date}, {@link java.util.Calendar}, {@link java.sql.Date},
 * {@link java.sql.Time}, and {@link java.sql.Timestamp}. They are used by OA
 * model properties, datasource conversion, filtering, comparison, scheduling,
 * serialization, UI formatting, and template/report output.
 *
 * @see com.viaoa.datetime.OADateTime
 * @see com.viaoa.datetime.OADate
 * @see com.viaoa.datetime.OATime
 * @see com.viaoa.datetime.OATimeZone
 */
package com.viaoa.datetime;
