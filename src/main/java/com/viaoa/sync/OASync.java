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
package com.viaoa.sync;

/**
 * Facade access point for OA's distributed synchronization subsystem.
 * <p>
 * {@code OASync} inherits all behavior from {@link OASyncDelegate} and
 * provides a concise type used throughout OA's object, hub, and datasource
 * layers when interacting with remote synchronization.
 * <p>
 * Through this class, application code can:
 * <ul>
 *   <li>resolve the current {@link OASyncServer} or {@link OASyncClient}
 *       associated with a model package,</li>
 *   <li>obtain remote interfaces such as
 *       {@code RemoteSyncInterface}, {@code RemoteServerInterface},
 *       {@code RemoteClientInterface}, and {@code RemoteSessionInterface},</li>
 *   <li>determine whether the current execution context is client-side or
 *       server-side, and</li>
 *   <li>access thread-local request metadata for remote method calls.</li>
 * </ul>
 * {@code OASync} does not add new functionality on top of
 * {@link OASyncDelegate}; it exists as a user-friendly public entry point.
 */
public class OASync extends OASyncDelegate {

}
