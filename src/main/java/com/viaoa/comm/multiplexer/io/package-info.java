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
 * Contains the low-level I/O components used by the OA Multiplexer to create
 * and manage many logical {@link java.net.Socket} and
 * {@link java.net.ServerSocket} connections over a single physical TCP
 * connection.
 *
 * <p>
 * These classes implement the framing protocol, I/O scheduling, buffering,
 * fairness, throttling, and lifecycle coordination required to multiplex
 * multiple virtual channels across one shared network stream. They are not
 * typically used directly by application code; instead, applications interact
 * through high-level abstractions such as
 * {@link com.viaoa.comm.multiplexer.OAMultiplexerClient},
 * {@link com.viaoa.comm.multiplexer.OAMultiplexerServer},
 * {@link VirtualSocket}, and {@link VirtualServerSocket}.
 * </p>
 *
 * <h2>Key Components</h2>
 *
 * <ul>
 *   <li><b>MultiplexerSocketController</b> –
 *       The core controller for each real socket connection. It performs the
 *       multiplexer handshake, reads frame headers, dispatches payloads to
 *       the correct virtual socket, and manages all virtual channel creation
 *       and teardown.</li>
 *
 *   <li><b>MultiplexerInputStreamController</b> –
 *       Owns the real {@link java.io.DataInputStream} for the physical socket.
 *       A single reader thread processes incoming frames and assigns them to
 *       the appropriate {@link VirtualSocket}. It enforces strict ordering,
 *       frame-size validation, and per-socket timeouts.</li>
 *
 *   <li><b>MultiplexerOutputStreamController</b> –
 *       Owns the real {@link java.io.DataOutputStream}. Provides exclusive,
 *       fair access to the write stream; implements chunked writes and
 *       optional throughput throttling; and serializes command frames and
 *       virtual socket payloads.</li>
 *
 *   <li><b>VirtualSocket</b> –
 *       Acts as a logical {@link java.net.Socket}. Its input and output
 *       streams delegate to the shared multiplexer controllers. Each virtual
 *       socket is identified by a channel id and maintains its own timeout and
 *       close semantics.</li>
 *
 *   <li><b>VirtualServerSocket</b> –
 *       Represents a logical {@link java.net.ServerSocket}. It does not bind
 *       to a physical port; instead, it is registered by name with the
 *       {@link com.viaoa.comm.multiplexer.MultiplexerServerSocketController}
 *       and receives virtual connections via {@link java.net.ServerSocket#accept()}.</li>
 *
 *   <li><b>MultiplexerServerSocketController</b> –
 *       Server-side manager responsible for accepting real client connections,
 *       creating a {@link MultiplexerSocketController} for each, and routing
 *       virtual socket open requests to the correct {@link VirtualServerSocket}.</li>
 * </ul>
 *
 * <h2>Design Characteristics</h2>
 *
 * <ul>
 *   <li><b>Single real socket per client</b> – All virtual channels share one
 *       underlying TCP connection.</li>
 *
 *   <li><b>Frame-based protocol</b> – Each message begins with a virtual
 *       socket id and payload length, providing consistent routing and error
 *       detection.</li>
 *
 *   <li><b>Cooperative fairness</b> – Writers coordinate through a
 *       scheduling/lock mechanism to prevent starvation and ensure smooth
 *       throughput across all channels.</li>
 *
 *   <li><b>Error containment</b> – Corrupted frames or timeouts cause only the
 *       affected virtual socket (or the entire connection, if necessary) to be
 *       closed cleanly.</li>
 *
 *   <li><b>High performance</b> – No reflection, minimal synchronization, and
 *       careful buffer management support large numbers of concurrent virtual
 *       connections.</li>
 * </ul>
 *
 * <p>
 * Collectively, these classes form the high-performance transport layer for
 * OA’s distributed communication system, allowing complex multi-channel
 * messaging architectures to run over simple, firewall-friendly TCP endpoints.
 * </p>
 */
package com.viaoa.comm.multiplexer.io;
