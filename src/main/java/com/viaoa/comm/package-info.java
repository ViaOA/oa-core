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
 * Core communication framework for OA applications. The {@code com.viaoa.comm}
 * package provides the foundational abstractions and transport mechanisms used
 * across OA’s distributed systems, enabling secure, discoverable, multiplexed,
 * and extensible communication channels between clients, servers, and
 * services.
 *
 * <h2>Architecture Overview</h2>
 * <p>
 * The communication layer is organized into specialized subpackages, each
 * responsible for a distinct capability:
 * </p>
 *
 * <ul>
 *   <li><strong>discovery</strong> –
 *       Lightweight UDP-based service discovery for locating OA endpoints on
 *       a network.</li>
 *
 *   <li><strong>http</strong> –
 *       Basic HTTP/HTTPS utilities and JSON-based request helpers.</li>
 *
 *   <li><strong>io</strong> –
 *       OA-enhanced serialization streams, including object-stream utilities
 *       used throughout OA’s distributed object graph.</li>
 *
 *   <li><strong>multiplexer</strong> –
 *       High-performance virtual-socket system that allows many independent
 *       logical channels to coexist on a single physical TCP connection. This
 *       includes input/output controllers, virtual sockets, and server/client
 *       socket controllers.</li>
 *
 *   <li><strong>ssl</strong> –
 *       Transport-agnostic SSL/TLS engine wrappers built on {@link
 *       javax.net.ssl.SSLEngine}, providing encrypted communication for TCP or
 *       multiplexed channels.</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Transport independence</strong> – Higher layers (e.g., OA
 *       Object Sync, distributed messaging) do not depend on specific socket
 *       or protocol implementations.</li>
 *
 *   <li><strong>Modularity</strong> – Components such as SSL, multiplexing,
 *       or discovery can be used individually or combined to build richer
 *       communication stacks.</li>
 *
 *   <li><strong>Performance</strong> – Multiplexed virtual channels eliminate
 *       the overhead of managing many physical connections.</li>
 *
 *   <li><strong>Security</strong> – Built-in SSL/TLS support ensures encrypted
 *       channels for any transport.</li>
 * </ul>
 *
 * <h2>Intended Usage</h2>
 * <p>
 * Applications may use the components in this package directly or rely on
 * higher-level OA subsystems that internally leverage these communication
 * layers to synchronize data, coordinate distributed processes, and perform
 * service-to-service interactions.
 * </p>
 */
package com.viaoa.comm;
