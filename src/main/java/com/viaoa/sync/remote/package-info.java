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
 * Remote method and synchronization interfaces used by OA's distributed
 * client/server architecture.
 * <p>
 * The classes and interfaces in this package define the RPC surface between
 * an {@link com.viaoa.sync.OASyncClient OASyncClient} and an
 * {@link com.viaoa.sync.OASyncServer OASyncServer}. They enable:
 * <ul>
 *   <li>remote invocation of server methods,</li>
 *   <li>per-client session management,</li>
 *   <li>datasource commands executed on the authoritative server,</li>
 *   <li>detail and reference loading for OAObject properties,</li>
 *   <li>live synchronization of object and hub changes,</li>
 *   <li>bidirectional propagation of edits, inserts, deletes, and hub updates.</li>
 * </ul>
 *
 * <h2>Key Interfaces</h2>
 *
 * <h3>{@link com.viaoa.sync.remote.RemoteServerInterface}</h3>
 * Represents the authoritative server-side OA model. Clients use this
 * interface to:
 * <ul>
 *   <li>fetch objects from cache or datasource,</li>
 *   <li>save changes with server-side cascades,</li>
 *   <li>obtain a new {@code RemoteSessionInterface} for the connection,</li>
 *   <li>obtain a {@code RemoteClientInterface} for detail loading and
 *       datasource work,</li>
 *   <li>invoke remote methods on OAObjects and Hubs.</li>
 * </ul>
 *
 * <h3>{@link com.viaoa.sync.remote.RemoteSessionInterface}</h3>
 * Represents server-side state for a single connected client. Tracks:
 * <ul>
 *   <li>GUIDs for objects present on the client,</li>
 *   <li>objects the client is still using (even if not in hubs),</li>
 *   <li>per-object locks,</li>
 *   <li>disconnect cleanup and cache persistence.</li>
 * </ul>
 *
 * <h3>{@link com.viaoa.sync.remote.RemoteClientInterface}</h3>
 * Exposes detail loading and datasource operations to the client. Executes:
 * <ul>
 *   <li>select and iterator-based queries,</li>
 *   <li>insert/update/delete operations,</li>
 *   <li>detail and sibling loading with depth constraints,</li>
 *   <li>object refreshes and lightweight operations on server-side entities.</li>
 * </ul>
 *
 * <h3>{@link com.viaoa.sync.remote.RemoteSyncInterface}</h3>
 * Defines the full set of broadcast synchronization events used to propagate
 * changes across the distributed graph:
 * <ul>
 *   <li>property changes,</li>
 *   <li>hub insert/remove/replace/move operations,</li>
 *   <li>detail and sibling refresh events,</li>
 *   <li>delete propagation,</li>
 *   <li>hub sorting and state resets.</li>
 * </ul>
 * A {@code RemoteSyncImpl} instance exists on both client and server to apply
 * these updates.
 *
 * <h2>Supporting Implementations</h2>
 *
 * <h3>{@link com.viaoa.sync.remote.RemoteServerImpl}</h3>
 * Server-side implementation responsible for:
 * <ul>
 *   <li>executing remote OAObject/Hub method calls,</li>
 *   <li>issuing GUID sequences,</li>
 *   <li>creating per-client sessions,</li>
 *   <li>routing server-to-client delete operations,</li>
 *   <li>capturing diagnostic thread dumps.</li>
 * </ul>
 *
 * <h3>{@link com.viaoa.sync.remote.RemoteSessionImpl}</h3>
 * Maintains all runtime state for a single client connection:
 * GUID registry, lock tracking, cache retention, and session cleanup.
 *
 * <h3>{@link com.viaoa.sync.remote.RemoteClientImpl}</h3>
 * The server-side implementation that executes detail loads and datasource
 * logic initiated by the client, using {@link com.viaoa.sync.remote.RemoteDataSource}.
 *
 * <h3>{@link com.viaoa.sync.remote.RemoteDataSource}</h3>
 * Executes datasource commands (select, insert, update, count, etc.) on the
 * authoritative datasource, maintaining per-client iterators and marking
 * objects as cached for sync purposes.
 *
 * <h3>{@link com.viaoa.sync.remote.ClientGetDetail}</h3>
 * Builds the object-graph payload returned to the client when requesting a
 * detail property. Manages sibling loading, object-depth rules, and selective
 * serialization with size limits.
 *
 * <h2>Routing and Queueing Semantics</h2>
 * The multiplexer layer distinguishes between:
 * <ul>
 *   <li>queued remote methods (ordered with sync events),</li>
 *   <li>non-queued methods (low-latency operations),</li>
 *   <li>methods executed in the server's remote thread,</li>
 *   <li>methods returned directly on the queue socket.</li>
 * </ul>
 * Correct selection ensures deterministic ordering of sync events relative to
 * hub operations and property changes.
 *
 * <p>
 * Together, the classes in this package form the complete remote API and
 * synchronization protocol used by OA's executable object-graph architecture.
 */
package com.viaoa.sync.remote;
