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
 * Contains the runtime metadata structures used by OA's multiplexer remoting
 * system. These classes capture binding definitions, method routing rules,
 * and per-request diagnostic information for all remote method invocations.
 *
 * <p>
 * When a remote interface is exported or imported through the OA multiplexer
 * layer, the remoting runtime scans the interface and builds a set of metadata
 * objects describing its structure. During execution of a remote call, this
 * metadata is combined with invocation-specific details to form a complete
 * representation of the request and response cycle.
 * </p>
 *
 * <h2>Purpose</h2>
 * The classes in this package provide:
 * <ul>
 *   <li><b>Binding metadata</b> for mapping bind-names to actual or proxied objects.</li>
 *   <li><b>Method metadata</b> describing how each method is invoked remotely,
 *       including serialization rules, routing flags, and return-type handling.</li>
 *   <li><b>Request metadata</b> that records the full lifecycle of each remote
 *       invocation for diagnostics, timing, queuing, and error handling.</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 *
 * <h3>{@link com.viaoa.remote.info.BindInfo}</h3>
 * Represents a single bind entry for a remote interface or object. It maintains
 * a weak reference to the underlying implementation, tracks the set of
 * remotely invocable methods, and holds {@link MethodInfo} metadata for
 * dispatch. BindInfo is the anchor point for all object-level remoting.
 *
 * <h3>{@link com.viaoa.remote.info.MethodInfo}</h3>
 * Captures all routing and serialization details for a single remotely
 * invocable method. This metadata includes:
 * <ul>
 *   <li>the reflected Java {@code Method},</li>
 *   <li>a unique signature for resolving overloads,</li>
 *   <li>queueing and broadcast rules,</li>
 *   <li>compression settings,</li>
 *   <li>whether the method returns another remote interface,</li>
 *   <li>whether the result must be returned on a queue thread.</li>
 * </ul>
 * {@code MethodInfo} is immutable after creation and is consulted for every
 * remote call routed through the multiplexer.
 *
 * <h3>{@link com.viaoa.remote.info.RequestInfo}</h3>
 * Records complete diagnostic information for a single remote method call,
 * including:
 * <ul>
 *   <li>timestamps for start, queue entry, send, receive, and finish,</li>
 *   <li>the request type (queued, unqueued, broadcast, etc.),</li>
 *   <li>method signature, argument preview, and thread routing,</li>
 *   <li>connection identifiers and socket routing information,</li>
 *   <li>return value or remote exception,</li>
 *   <li>formatted log output for debugging high-volume traffic.</li>
 * </ul>
 * {@code RequestInfo} instances are created per invocation and discarded when
 * the request completes, providing full visibility into the flow of every
 * remote call.
 *
 * <h2>Design Notes</h2>
 * <p>
 * The metadata in this package follows OA's core design principles:
 * </p>
 * <ul>
 *   <li><b>Separation of concerns</b> – annotation parsing and invocation
 *       logic are separated from request diagnostics.</li>
 *   <li><b>Lightweight structures</b> – classes contain only the information
 *       required for routing and logging, avoiding heavy abstractions.</li>
 *   <li><b>GC-aware design</b> – remote object bindings are weakly referenced
 *       to prevent memory leaks.</li>
 *   <li><b>High transparency</b> – timestamps, request types, and call details
 *       are fully captured for analysis and debugging.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>
 * Application code does not directly instantiate or manipulate these classes.
 * They are created and used internally by the OA multiplexer server and
 * client, but are exposed through logs, diagnostics, and introspection tools
 * to provide complete visibility into the remote call pipeline.
 * </p>
 *
 * @author vvia
 */
package com.viaoa.remote.info;
