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
 * Client/server synchronization for distributed OA runtimes.
 * <p>
 * The sync package keeps OAObject, Hub, cache, property, lifecycle, and remote datasource state coordinated between an
 * authoritative {@link com.viaoa.sync.OASyncServer} and one or more {@link com.viaoa.sync.OASyncClient} instances. It
 * uses the multiplexer remote layer to expose server, session, client, and sync endpoints while preserving OA runtime
 * semantics for object identity, Hub membership, lazy detail loading, locking, and remote method invocation.
 * </p>
 * <p>
 * Sync is the live connected transport layer. Durable reconnect/replay behavior is handled by the replication package.
 * </p>
 *
 * @see com.viaoa.sync.OASyncServer
 * @see com.viaoa.sync.OASyncClient
 * @see com.viaoa.sync.remote.RemoteSyncInterface
 */
package com.viaoa.sync;
