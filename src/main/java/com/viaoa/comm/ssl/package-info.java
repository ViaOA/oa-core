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
 * Provides SSL/TLS support for OA communication layers using Java’s
 * {@link javax.net.ssl.SSLEngine}. This package supplies the core framework
 * required to perform encrypted bidirectional communication over arbitrary
 * transports, including socket and multiplexed channels.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.viaoa.comm.ssl.OASslBase OASslBase} –
 *       Abstract foundation for SSL clients and servers. Manages:
 *       <ul>
 *         <li>SSLContext and SSLEngine creation</li>
 *         <li>Handshake coordination (wrap/unwrap cycles)</li>
 *         <li>Encryption and decryption</li>
 *         <li>Delegated SSL tasks</li>
 *         <li>Blocking input/output for secured channels</li>
 *       </ul>
 *   </li>
 *
 *   <li>{@link com.viaoa.comm.ssl.OASslClient OASslClient} –
 *       SSL client implementation that loads a truststore to authenticate
 *       remote servers and configures the engine in client mode.</li>
 *
 *   <li>{@link com.viaoa.comm.ssl.OASslServer OASslServer} –
 *       SSL server implementation that loads a private-key keystore and
 *       configures the SSLEngine in server mode.</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li>Transport-agnostic SSL encryption (usable over raw sockets, OA
 *       Multiplexer channels, or other streams)</li>
 *   <li>Full control over the handshake state machine</li>
 *   <li>Clean separation between SSL processing and I/O transport</li>
 *   <li>Consistent, reusable SSL utilities across all OA communication modules</li>
 * </ul>
 *
 * <h2>Intended Usage</h2>
 * <p>
 * Applications subclass {@link com.viaoa.comm.ssl.OASslBase} to provide the
 * actual transport mechanism for encrypted bytes, while OASslBase manages the
 * entire SSL lifecycle. The SSL client/server classes included here are ready
 * for use in systems requiring authenticated, encrypted connections.
 * </p>
 */
package com.viaoa.comm.ssl;

