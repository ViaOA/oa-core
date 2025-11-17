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
 * Model classes used by the OA synchronization subsystem to describe the
 * state of connected clients and the server instance.
 * <p>
 * These classes do not perform synchronization logic themselves; instead,
 * they act as lightweight data carriers for:
 * <ul>
 *   <li>client identity,</li>
 *   <li>server identity,</li>
 *   <li>connection metadata,</li>
 *   <li>lifecycle timestamps,</li>
 *   <li>runtime statistics.</li>
 * </ul>
 *
 * <h2>Classes</h2>
 *
 * <h3>{@link com.viaoa.sync.model.ClientInfo}</h3>
 * Represents a single connected client. Tracks:
 * <ul>
 *   <li>connection ID and creation/disconnect times,</li>
 *   <li>host and network information,</li>
 *   <li>request counts and total request time,</li>
 *   <li>client memory usage,</li>
 *   <li>user identity (userId, userName, location),</li>
 *   <li>sync-thread counts and client version.</li>
 * </ul>
 * Instances of this class are updated by {@code OASyncClient} and
 * {@code OASyncServer} during connection negotiation and runtime.
 *
 * <h3>{@link com.viaoa.sync.model.ServerInfo}</h3>
 * Describes the running {@code OASyncServer} instance. Includes:
 * <ul>
 *   <li>start timestamp,</li>
 *   <li>server host and network metadata,</li>
 *   <li>server version,</li>
 *   <li>flags for start/suspend/discovery state.</li>
 * </ul>
 * Used primarily for diagnostics, discovery, and administrative APIs.
 *
 * <p>
 * Together these model classes provide introspection data for client/server
 * monitoring tools and remote management layers.
 */
package com.viaoa.sync.model;
