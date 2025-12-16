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
 * Core synchronization layer for distributed OA models.
 * <p>
 * This package integrates {@link com.viaoa.object.OAObject OAObject} /
 * {@link com.viaoa.hub.Hub Hub} observability with the multiplexer-based
 * remoting classes to keep models synchronized across JVMs.
 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li>Managing {@link OASyncServer} instances that host remote sync endpoints,
 *       sessions and file transfer.</li>
 *   <li>Managing {@link OASyncClient} instances that connect to remote servers,
 *       obtain {@code RemoteServer}, {@code RemoteSession}, {@code RemoteClient}
 *       and {@code RemoteSync} proxies, and keep the local object graph in sync.</li>
 *   <li>Providing static helpers in {@link OASyncDelegate} to resolve the
 *       appropriate server, client and remote interfaces for a given model
 *       package and to determine whether the current code path is executing
 *       on a client or on a server.</li>
 *   <li>Optionally combining multiple servers into a single logical model via
 *       the experimental {@link OASyncCombinedClient}.</li>
 * </ul>
 * The typical usage pattern is:
 * <ol>
 *   <li>Create and start an {@link OASyncServer} on the server JVM.</li>
 *   <li>Create and start one {@link OASyncClient} per application JVM.</li>
 *   <li>Allow {@link OASyncDelegate} / {@link OASync} to route sync operations
 *       for {@link com.viaoa.object.OAObject} and {@link com.viaoa.hub.Hub} so that
 *       changes are automatically propagated between client and server.</li>
 * </ol>
 */
package com.viaoa.sync;
