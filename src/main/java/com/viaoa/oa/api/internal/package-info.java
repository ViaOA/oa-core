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
 * Internal OA runtime collaboration contracts exposed through {@code OA.internal()}.
 * <p>
 * This package defines the top-level internal facade used by OA framework and
 * runtime code to reach lower-level object, Hub, synchronization, replication,
 * and trigger operation families. These contracts are implementation-level
 * boundaries for OA libraries; they are not application-facing APIs and are not
 * part of the public OA service surface.
 * <p>
 * Public and advanced application access should use {@code OA.services()} and
 * the curated public service contracts. Internal OA libraries use this package
 * when they need behavior that must preserve lower-level {@code OAObject} and
 * {@code Hub} runtime semantics.
 * <h2>Layering</h2>
 * <ul>
 * <li>{@code OA} exposes the runtime instance.</li>
 * <li>{@code OA.services()} exposes curated public and advanced services.</li>
 * <li>{@code OA.internal()} exposes OA-library/runtime-only operation families.</li>
 * <li>Concrete service implementations remain behind these facade contracts.</li>
 * </ul>
 * <h2>Boundary Rules</h2>
 * <ul>
 * <li>Application code should not depend on this package.</li>
 * <li>Public service APIs should not expose internal operation types.</li>
 * <li>Internal operation interfaces may evolve with OA runtime needs.</li>
 * <li>Repeated external need for an internal operation usually indicates that a
 * curated public service method should be considered.</li>
 * </ul>
 *
 * @see com.viaoa.oa.OA
 * @see com.viaoa.oa.api.services
 * @see com.viaoa.oa.api.internal.objects
 * @see com.viaoa.oa.api.internal.hubs
 */
package com.viaoa.oa.api.internal;
