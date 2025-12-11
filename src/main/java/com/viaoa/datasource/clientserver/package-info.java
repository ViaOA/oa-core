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
 * Provides client-side implementations for OA's distributed data-source layer.
 * <p>
 * Classes in this package enable OA applications to access remote
 * {@link com.viaoa.datasource.OADataSource} instances hosted on OA servers.
 * Communication occurs via {@link com.viaoa.sync.remote.RemoteClientInterface}
 * and the OA synchronization framework.
 *
 * <h2>Key Component</h2>
 * <ul>
 *   <li>{@link com.viaoa.datasource.clientserver.OADataSourceClient} —
 *       client-side proxy for remote OADataSource operations.</li>
 * </ul>
 */
package com.viaoa.datasource.clientserver;
