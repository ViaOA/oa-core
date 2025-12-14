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
 * Provides runtime metadata structures used by the OA REST remoting subsystem.
 * <p>
 * This package contains the information objects produced when OA parses and
 * introspects {@code @OARestClass}, {@code @OARestMethod}, and
 * {@code @OARestParam} annotations. These objects are used at runtime to
 * construct HTTP requests, bind method parameters, interpret results, and
 * supply diagnostics to the calling code.
 *
 * <h2>Purpose</h2>
 * The classes in this package separate low-level REST invocation details from
 * the higher-level proxy mechanism. Each remote call captures its inputs,
 * resolved parameter types, URL routing, serialization options, and HTTP
 * timing. This allows OA to:
 * <ul>
 *   <li>Dynamically generate request URLs and payloads.</li>
 *   <li>Support multiple parameter styles (query, path, body, form, JSON).</li>
 *   <li>Record timing, headers, response codes, and exceptions.</li>
 *   <li>Surface the invocation context back to user methods when requested.</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 *
 * <h3>{@link com.viaoa.remote.rest.info.OARestClassInfo}</h3>
 * Captures metadata for a remote interface annotated with {@code @OARestClass}.
 * This includes the interface’s REST context name, base URL information,
 * default serializer options, and the set of discovered remote methods.
 *
 * <h3>{@link com.viaoa.remote.rest.info.OARestMethodInfo}</h3>
 * Describes a single REST-accessible method, including:
 * <ul>
 *   <li>HTTP verb (GET, POST, etc.).</li>
 *   <li>URL path, query construction rules, and serialization mode.</li>
 *   <li>Return type information and optional wrapper semantics.</li>
 *   <li>Associated {@link OARestParamInfo} objects for method parameters.</li>
 * </ul>
 *
 * <h3>{@link com.viaoa.remote.rest.info.OARestParamInfo}</h3>
 * Holds per-parameter metadata derived from {@code @OARestParam} annotations.
 * This includes:
 * <ul>
 *   <li>Resolved parameter class or container type.</li>
 *   <li>Formatting directives.</li>
 *   <li>Inclusion of property paths for serialized OAObjects.</li>
 *   <li>Parameter role (query, path, body, form, etc.).</li>
 * </ul>
 *
 * <h3>{@link com.viaoa.remote.rest.info.OARestInvokeInfo}</h3>
 * A runtime data object representing a single executed remote call. This is
 * created for each method invocation and contains:
 * <ul>
 *   <li>Timestamps (start, send, end) for performance metrics.</li>
 *   <li>Fully resolved URL and HTTP method.</li>
 *   <li>Serialized JSON or form data request body.</li>
 *   <li>Incoming and outgoing headers and cookies.</li>
 *   <li>HTTP response code, message, and response body.</li>
 *   <li>Deserialized return value or any encountered exception.</li>
 * </ul>
 * When a user method declares a parameter or return type of
 * {@code OARestInvokeInfo}, OA passes the constructed runtime info directly
 * back to application code for diagnostics or custom handling.
 *
 * <h2>Usage</h2>
 * The OA REST client stack automatically creates and populates these info
 * objects while invoking interface proxies. Application code does not create
 * them manually but may access them when debugging, logging, or inspecting REST
 * behavior.
 *
 * <h2>Design Notes</h2>
 * These classes are intentionally simple data holders with minimal logic. They
 * reflect OA’s design philosophy of separating concerns:
 * <ul>
 *   <li>Annotation scanning builds the metadata.</li>
 *   <li>The proxy layer performs invocation.</li>
 *   <li>The info objects record what happened.</li>
 * </ul>
 *
 * @author vvia
 */
package com.viaoa.remote.rest.info;
