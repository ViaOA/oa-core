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
 * Provides a lightweight REST remoting layer for OA, enabling Java-to-Java remote
 * method invocation, OAObject graph access, and general HTTP endpoint communication
 * using a simple annotation-driven model.
 *
 * <p>
 * The REST client is designed around a small set of annotations
 * ({@link com.viaoa.remote.rest.annotation.OARestClass},
 * {@link com.viaoa.remote.rest.annotation.OARestMethod},
 * {@link com.viaoa.remote.rest.annotation.OARestParam}) that allow a normal Java
 * interface to be “remoted.”  At runtime, {@link com.viaoa.remote.rest.OARestClient}
 * creates a dynamic proxy for the interface and translates each method invocation
 * into an HTTP request.
 *
 * <h2>Primary Capabilities</h2>
 * <ul>
 *   <li><b>Java Interface Remoting</b> –
 *       Any annotated interface can be turned into a remote service proxy.
 *       Method calls are translated into HTTP GET/POST/PUT/PATCH/DELETE requests
 *       as defined by {@link com.viaoa.remote.rest.annotation.OARestMethod}.</li>
 *
 *   <li><b>OAObject Graph Access</b> –
 *       Convenience methods ({@code callOAGet}, {@code callOASelect}, etc.)
 *       allow remote access to an OAObject graph through an OARestServlet,
 *       including selecting, inserting, updating, and deleting OAObjects.</li>
 *
 *   <li><b>Automatic JSON Handling</b> –
 *       JSON serialization/deserialization is handled internally
 *       using {@link com.viaoa.json.OAJson}.  Return values may be
 *       OAObjects, lists, arrays, hubs, primitive types, or JSON nodes.</li>
 *
 *   <li><b>Detailed Invocation Metadata</b> –
 *       Every request produces an {@link com.viaoa.remote.rest.info.OARestInvokeInfo}
 *       that tracks timing, request/response headers, HTTP status codes, JSON payloads,
 *       and error diagnostics.</li>
 *
 *   <li><b>Error Reporting</b> –
 *       Any call failure throws {@link com.viaoa.remote.rest.OARestClientException},
 *       which contains the full {@code OARestInvokeInfo} for troubleshooting.</li>
 *
 *   <li><b>Flexible Parameter Handling</b> –
 *       Annotations allow parameters to be treated as path variables,
 *       query parameters, form data, JSON bodies, byte streams, or
 *       OA-specific “include property path” requests for graph expansion.</li>
 *
 *   <li><b>HTTPS Support</b> –
 *       The client includes a permissive SSL configuration for simplified
 *       testing against development and self-signed servers.</li>
 * </ul>
 *
 * <h2>Intended Usage</h2>
 * <p>
 * This REST layer is ideal when OA-based applications need:
 * </p>
 * <ul>
 *   <li>a simple remote API for mobile or web clients,</li>
 *   <li>lightweight Java-to-Java remote invocation without heavier frameworks,</li>
 *   <li>a way to expose OAObject graphs or execute remote OA methods,</li>
 *   <li>a controlled and strongly typed contract via Java interfaces.</li>
 * </ul>
 *
 * <h2>Relationship to OA Distributed Messaging</h2>
 * <p>
 * While OA already provides a powerful distributed messaging system through
 * the multiplexer and remote thread subsystems, this REST layer is intended
 * for simpler synchronous integrations and HTTP-centric environments where a
 * servlet-based remote API is preferred.
 * </p>
 *
 * @author vvia
 */
package com.viaoa.remote.rest;
