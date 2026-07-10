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
 * Remote interfaces and implementations used by OA client/server synchronization.
 * <p>
 * This package defines the RPC surface between {@link com.viaoa.sync.OASyncClient} and
 * {@link com.viaoa.sync.OASyncServer}: server lookup, per-client sessions, detail loading, remote datasource access,
 * client callbacks, and bidirectional sync event application. Implementations coordinate with OA runtime services to
 * apply property changes, Hub operations, object lifecycle changes, locks, refreshes, and cache updates.
 * </p>
 *
 * @see com.viaoa.sync.remote.RemoteServerInterface
 * @see com.viaoa.sync.remote.RemoteSessionInterface
 * @see com.viaoa.sync.remote.RemoteClientInterface
 * @see com.viaoa.sync.remote.RemoteSyncInterface
 */
package com.viaoa.sync.remote;
