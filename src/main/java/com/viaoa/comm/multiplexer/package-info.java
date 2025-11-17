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
 * Provides the client- and server-side infrastructure for OA’s socket
 * multiplexer, a communication layer that allows many independent virtual
 * TCP connections to operate over a single physical network socket.
 *
 * <p>
 * The multiplexer enables distributed OA applications to maintain multiple
 * logical channels—each behaving like a normal {@link java.net.Socket} or
 * {@link java.net.ServerSocket}—while sharing one underlying TCP connection.
 * This reduces connection overhead, simplifies firewall/proxy traversal, and
 * centralizes routing and throttling through a compact and efficient I/O
 * controller.
 * </p>
 *
 * <h2>Key Concepts</h2>
 *
 * <ul>
 *   <li><b>Virtual Sockets</b> –
 *       Represented by {@code VirtualSocket} and {@code VirtualServerSocket}.
 *       These behave like ordinary sockets but are mapped onto logical
 *       channels managed by the multiplexer.</li>
 *
 *   <li><b>Single Physical Connection</b> –
 *       The {@link com.viaoa.comm.multiplexer.OAMultiplexerClient} and
 *       {@link com.viaoa.comm.multiplexer.OAMultiplexerServer} communicate
 *       using one real TCP socket.</li>
 *
 *   <li><b>Multiplexer Controllers</b> –
 *       I/O controllers such as
 *       {@code MultiplexerSocketController},
 *       {@code MultiplexerInputStreamController},
 *       and {@code MultiplexerOutputStreamController} handle framing, routing,
 *       buffering, fairness, and channel lifecycle management.</li>
 *
 *   <li><b>Named Virtual Endpoints</b> –
 *       Clients request connections by name
 *       (e.g., {@code "OrderService"}), and the server exposes matching
 *       {@code VirtualServerSocket} instances. This decouples logical services
 *       from physical ports.</li>
 *
 *   <li><b>High Throughput with Optional Throttling</b> –
 *       Both client and server support byte-level throttling to limit bursts
 *       and balance traffic across channels.</li>
 *
 *   <li><b>Connection Health</b> –
 *       Clients support optional keep-alive signaling for long-running
 *       distributed sessions.</li>
 * </ul>
 *
 * <h2>Typical Usage</h2>
 *
 * <h3>Client</h3>
 * <pre>
 * OAMultiplexerClient client = new OAMultiplexerClient("host", 9000);
 * client.start();
 * VirtualSocket vs = client.createSocket("MyService");
 * InputStream in  = vs.getInputStream();
 * OutputStream out = vs.getOutputStream();
 * </pre>
 *
 * <h3>Server</h3>
 * <pre>
 * OAMultiplexerServer server = new OAMultiplexerServer(9000);
 * server.start();
 * VirtualServerSocket vss = server.createServerSocket("MyService");
 * VirtualSocket vs = vss.accept();   // behaves like ServerSocket.accept()
 * </pre>
 *
 * <h2>Design Goals</h2>
 *
 * <ul>
 *   <li><b>Transparency</b> – Virtual sockets follow the same I/O patterns as
 *       traditional Java sockets.</li>
 *   <li><b>Performance</b> – Centralized I/O, minimal contention, and compact
 *       packet framing.</li>
 *   <li><b>Reliability</b> – Clean lifecycle, ordered delivery within channels,
 *       and controlled shutdown semantics.</li>
 *   <li><b>Scalability</b> – Many logical connections with only one real network
 *       connection per client.</li>
 * </ul>
 *
 * <p>
 * Together, these classes form a flexible, efficient communication layer used
 * throughout OA’s distributed messaging, event propagation, and multi-module
 * service communication.
 * </p>
 */
package com.viaoa.comm.multiplexer;
