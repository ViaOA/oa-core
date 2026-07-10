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
 * Core OA runtime services.
 * <p>
 * This package owns process-level runtime access to OA instances, datasource
 * registration, runtime thread services, remote-thread state, and thread-local
 * execution context. It is the kernel layer used by OA object, Hub, datasource,
 * synchronization, replication, and generated-application code.
 * <p>
 * The {@link com.viaoa.runtime.OARuntime} entry point resolves the current
 * {@link com.viaoa.oa.OA} runtime for model packages, classes, objects, and
 * Hubs. Runtime thread services coordinate scoped flags such as loading,
 * saving, deleting, remote-thread execution, synchronization-message sending,
 * model-user state, session-user state, transactions, and sibling helpers.
 *
 * @see com.viaoa.runtime.OARuntime
 * @see com.viaoa.runtime.OAThreadService
 * @see com.viaoa.runtime.OAThreadLocalService
 * @see com.viaoa.runtime.OADataSourceService
 * @see com.viaoa.oa.OA
 */
package com.viaoa.runtime;
