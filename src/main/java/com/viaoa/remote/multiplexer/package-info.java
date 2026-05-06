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
 * Provides the server–side implementation of OA’s remoting layer that runs on top of the
 * Multiplexer framework.  
 * <p>
 * This package enables full bidirectional remote method invocation (RMI-style) over a single
 * physical socket connection using virtual sockets. It is used by {@link com.viaoa.remote}
 * to expose server objects to remote clients, and to receive remote objects originating from clients.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Virtual Sockets</b> — Multiple independent request/response channels over one real TCP
 *       connection, enabling lightweight, concurrent remoting without socket explosion.</li>
 *   <li><b>Client-to-Server (CtoS) Remoting</b> — Clients can lookup, invoke, and stream method calls
 *       to server-hosted objects.</li>
 *   <li><b>Server-to-Client (StoC) Remoting</b> — Server can call methods on client-side remote
 *       objects through dynamically generated Java proxies.</li>
 *   <li><b>Asynchronous Queues</b> — Remote calls can be queued and processed using a circular queue,
 *       enabling high-throughput or broadcast workflows.</li>
 *   <li><b>Broadcast Remote Objects</b> — A single remote proxy can be shared by all connected clients
 *       to deliver fan-out messaging.</li>
 *   <li><b>Distributed Garbage Collection (DGC)</b> — Weak references are tracked so server-side
 *       remote objects can safely be released when clients no longer hold them.</li>
 *   <li><b>Compression / Class-Descriptor Caching</b> — Optimizations for payload size and
 *       serialization speed.</li>
 *   <li><b>Automatic Proxy Generation</b> — Java {@link java.lang.reflect.Proxy} is used to create
 *       server-side representations of client remote objects.</li>
 * </ul>
 *
 * <h2>How It Works</h2>
 * <ol>
 *   <li>Server creates a {@code OARemoteMultiplexerServer} bound to an
 *       {@link com.viaoa.comm.multiplexer.OAMultiplexerServer}.</li>
 *   <li>Clients connect through virtual sockets, using the multiplexer protocol.</li>
 *   <li>Remote lookups and method calls are serialized using
 *       {@link com.viaoa.remote.multiplexer.io.RemoteObjectInputStream} and
 *       {@link com.viaoa.remote.multiplexer.io.RemoteObjectOutputStream}.</li>
 *   <li>Server dispatches remote invocations to worker {@link com.viaoa.runtime.thread.OARemoteThread}s.</li>
 *   <li>Return values (including remote objects) are serialized back to the caller or routed using
 *       async queues when enabled.</li>
 * </ol>
 *
 * <h2>Intended Use</h2>
 * This package provides the low-level infrastructure for high-performance remoting, used internally
 * by OAObjectGraph synchronization, remote method calls, distributed messaging, server-side
 * callbacks, and broadcast channels.
 *
 * @author vvia
 */
package com.viaoa.remote.multiplexer;