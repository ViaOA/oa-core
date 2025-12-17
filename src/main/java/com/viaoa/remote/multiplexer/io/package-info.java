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
 * Low-level I/O classes used by OA's Multiplexer-based remoting system.
 * These classes implement OA’s high-performance serialization protocol,
 * replacing Java’s default {@link java.io.ObjectOutputStream} /
 * {@link java.io.ObjectInputStream} behavior with a compact, bandwidth-efficient
 * format tailored for distributed OA applications.
 *
 * <h2>Purpose</h2>
 * <p>
 * This package provides the encoding and decoding layer for all remote
 * method calls, remote object references, and broadcast messages flowing
 * through the Multiplexer. The classes here are responsible for:
 * </p>
 * <ul>
 *   <li>Efficient buffering of outbound data</li>
 *   <li>Custom class-descriptor serialization and caching</li>
 *   <li>Compact ASCII protocol strings</li>
 *   <li>Supporting nested and compressed serialization streams</li>
 *   <li>Cooperating with session-wide caches maintained by the remote server
 *       and client implementations</li>
 * </ul>
 *
 * <h2>Major Components</h2>
 *
 * <ul>
 *   <li><b>{@link RemoteBufferedOutputStream}</b> – A pooled, single-threaded,
 *       high-throughput buffered stream optimized for remoting workloads.
 *       Eliminates unnecessary allocations and significantly reduces GC pressure.</li>
 *
 *   <li><b>{@link RemoteObjectOutputStream}</b> – Replacement for
 *       {@code ObjectOutputStream}. Removes stream headers, writes class
 *       descriptors only once, assigns small integer IDs for later use,
 *       and serializes objects in a form suitable for Multiplexer transport.</li>
 *
 *   <li><b>{@link RemoteObjectInputStream}</b> – Counterpart to
 *       {@code RemoteObjectOutputStream}. Reads compact descriptors, resolves
 *       them through shared session caches, and reconstructs objects using the
 *       same optimized protocol.</li>
 * </ul>
 *
 * <h2>Design Characteristics</h2>
 *
 * <ul>
 *   <li><b>Compact Protocol</b> – Class descriptors are transmitted only once.
 *       Subsequent references use small integer IDs, reducing payload size.</li>
 *
 *   <li><b>Nested Streams Support</b> – When a remote call contains embedded
 *       remote objects, nested serialization streams reuse the same session-level
 *       descriptor caches, ensuring consistency and efficiency.</li>
 *
 *   <li><b>High Performance</b> – Buffer pooling, ASCII-optimized string
 *       routines, and a reduced serialization header make this protocol much
 *       faster and smaller than standard Java serialization.</li>
 *
 *   <li><b>Session-Aware</b> – These streams cooperate with server and client
 *       session managers so descriptor caches are shared, synchronized, and
 *       reused across the lifetime of a Multiplexer connection.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>
 * These classes are used internally by:
 * </p>
 * <ul>
 *   <li>{@link com.viaoa.remote.multiplexer.OARemoteMultiplexerServer}</li>
 *   <li>{@link com.viaoa.remote.multiplexer.OARemoteMultiplexerClient}</li>
 * </ul>
 *
 * <p>
 * Application code does not interact with this package directly. It is a core
 * subsystem of OA’s remoting infrastructure and is designed to be transparent
 * to developers building distributed OA applications.
 * </p>
 *
 * @author vvia
 */
package com.viaoa.remote.multiplexer.io;
