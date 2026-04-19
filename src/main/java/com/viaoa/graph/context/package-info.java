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
 * Provides the core context and permission-management subsystem for OA-based
 * applications. <p>
 *
 * The classes in this package define how an application's thread-local context
 * is associated with a logged-in OAObject, a Hub representing the active user,
 * and an {@link com.viaoa.context.OAUserAccess} instance that governs visibility
 * and enabled/disabled access across an OAObject graph. <p>
 *
 * Features include:
 * <ul>
 *   <li>Thread-local context identity.</li>
 *   <li>Context-bound user object and user Hub.</li>
 *   <li>Context-specific permission rules using OAUserAccess.</li>
 *   <li>Admin, super-admin, and “edit processed” rule resolution.</li>
 *   <li>Property-path–based inclusion testing for complex graphs.</li>
 * </ul>
 *
 * This package is used throughout OA's object-graph traversal, UI binding,
 * callbacks, and security enforcement layers to define how objects and
 * properties behave relative to user or system context.
 */

package com.viaoa.graph.context;