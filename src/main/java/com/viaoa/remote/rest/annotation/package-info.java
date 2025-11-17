/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
 * Defines the annotation model used by OA's REST remoting subsystem. The
 * annotations in this package allow a normal Java interface to be declared as
 * a remote REST API contract. At runtime, {@link com.viaoa.remote.rest.OARestClient}
 * uses these annotations to build metadata, generate dynamic proxies, and
 * translate method calls into HTTP requests with strongly typed return values.
 *
 * <h2>Purpose</h2>
 * <p>
 * The annotations in this package form a complete, declarative description
 * of a REST API. Instead of defining external specifications (Swagger/OpenAPI)
 * or manually constructing HTTP requests, the developer writes a plain Java
 * interface and annotates:
 * </p>
 * <ul>
 *   <li>the interface itself – specifying the root REST context,</li>
 *   <li>each method – specifying HTTP method, URL template, OA operation type,</li>
 *   <li>each parameter – specifying how its value is inserted into the request.</li>
 * </ul>
 *
 * <p>
 * This design allows the REST interface to be:
 * </p>
 * <ul>
 *   <li><b>Typed</b> – method signatures define exact return types,</li>
 *   <li><b>Self-contained</b> – no external configuration files,</li>
 *   <li><b>Declarative</b> – remoting rules are written directly in code,</li>
 *   <li><b>OAObject-aware</b> – methods can operate on full object graphs.</li>
 * </ul>
 *
 * <h2>Annotation Summary</h2>
 *
 * <h3>{@link com.viaoa.remote.rest.annotation.OARestClass}</h3>
 * <p>
 * Marks an interface as remote-accessible and defines the base URL context
 * under which all declared methods will be exposed. This is the top-level
 * metadata entry for the interface.
 * </p>
 *
 * <h3>{@link com.viaoa.remote.rest.annotation.OARestMethod}</h3>
 * <p>
 * Provides the complete definition for how an interface method maps to a
 * remote operation. This includes:
 * </p>
 * <ul>
 *   <li>HTTP verb (GET, POST, PUT, PATCH, DELETE),</li>
 *   <li>URL template and query-string composition,</li>
 *   <li>OAObjectGraph operations such as select, get, insert, update, delete,</li>
 *   <li>include-property-path rules for graph expansion,</li>
 *   <li>paging, sorting, and return-type selection,</li>
 *   <li>body formats and JSON serialization behavior.</li>
 * </ul>
 *
 * <p>
 * {@code OARestMethod} is the central description of request routing and
 * response handling. It allows the interface to express REST semantics without
 * any implementation code.
 * </p>
 *
 * <h3>{@link com.viaoa.remote.rest.annotation.OARestParam}</h3>
 * <p>
 * Describes how each parameter is applied to the request. Parameters can be
 * used as:
 * </p>
 * <ul>
 *   <li>path variables,</li>
 *   <li>query parameters,</li>
 *   <li>form fields,</li>
 *   <li>JSON body fragments or complete bodies,</li>
 *   <li>OA search criteria,</li>
 *   <li>OAObjectGraph identifiers,</li>
 *   <li>HTTP headers or cookies,</li>
 *   <li>paging controls (page size, page number),</li>
 *   <li>include-property-path rules for response shaping.</li>
 * </ul>
 *
 * <p>
 * {@code OARestParam} allows fine-grained control over how values are inserted
 * into a REST request and how OAObjectGraph behavior is applied.
 * </p>
 *
 * <h2>How the Annotation Subsystem Works</h2>
 * <ol>
 *   <li>Developer defines a Java interface and annotates it with these
 *       annotations.</li>
 *   <li>{@code OARestClient} scans the interface using reflection, building
 *       a metadata model ({@code OARestClassInfo}, {@code OARestMethodInfo},
 *       {@code OARestParamInfo}).</li>
 *   <li>A dynamic proxy is created for the interface.</li>
 *   <li>When a method is invoked:
 *     <ul>
 *       <li>parameters are bound to URL, query, form, or body fields,</li>
 *       <li>the HTTP request is constructed,</li>
 *       <li>the server response is parsed as JSON,</li>
 *       <li>return value is mapped to the method’s declared type.</li>
 *     </ul>
 *   </li>
 *   <li>OAObjectGraph operations are handled through a compatible REST servlet
 *       on the server, allowing full OA graph navigation.</li>
 * </ol>
 *
 * <h2>Usage Goals</h2>
 * <p>
 * These annotations enable OA applications to use strongly typed remote
 * interfaces for REST communication, without needing:
 * </p>
 * <ul>
 *   <li>Spring controllers,</li>
 *   <li>OpenAPI specifications,</li>
 *   <li>manual HTTP client code,</li>
 *   <li>hand-written JSON parsing.</li>
 * </ul>
 *
 * <p>
 * Instead, developers declare a simple Java interface with annotations, and
 * {@code OARestClient} handles the entire remoting pipeline.
 * </p>
 *
 * @author vvia
 */
package com.viaoa.remote.rest.annotation;
