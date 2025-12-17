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
package com.viaoa.remote.multiplexer.annotation;
/**
 * Annotations that describe metadata for OA's Multiplexer-based remoting
 * system. These annotations are placed on <b>remote interfaces</b> and
 * <b>their methods and parameters</b>. They are read at runtime by
 * {@code OARemoteMultiplexerClient} and
 * {@code OARemoteMultiplexerServer} to determine how remote calls
 * should be transmitted, queued, compressed, and executed.
 *
 * <ul>
 *   <li>{@link OARemoteInterface} – Marks an interface as remotely accessible
 *       and optionally forces serialized execution.</li>
 *   <li>{@link OARemoteMethod} – Controls compression, timeout, queue usage,
 *       and return semantics on a per-method basis.</li>
 *   <li>{@link OARemoteParameter} – Controls compression and queue behavior
 *       for individual method parameters.</li>
 * </ul>
 *
 * <p>
 * These annotations allow OA remoting to be completely declarative: the
 * interface signature itself defines how remote invocation behaves,
 * without requiring configuration files or non-Java artifacts.
 * </p>
 */
