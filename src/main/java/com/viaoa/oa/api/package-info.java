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
 * Public OA runtime API contracts.
 * <p>
 * This package defines the top-level operation interfaces exposed by the OA
 * runtime for model-user state, session-user state, synchronization,
 * replication, and runtime configuration. These contracts are public runtime
 * boundaries; implementation services and lower-level object/Hub machinery are
 * intentionally kept behind {@code OA.services()} and {@code OA.internal()}.
 * <h2>API Boundary</h2>
 * <p>
 * APIs in this package should describe caller-visible behavior without exposing
 * concrete implementation services. Public application and generated-code usage
 * should depend on these contracts and the curated service contracts under
 * {@code com.viaoa.oa.api.services}. OA-library-only behavior belongs under
 * {@code com.viaoa.oa.api.internal}.
 * <h2>Runtime Scope</h2>
 * <p>
 * Operations obtained from an OA runtime instance apply to that runtime's model
 * state and distributed-runtime configuration. Synchronization and replication
 * methods expose lifecycle and role state; rule evaluation and low-level model
 * machinery remain in their dedicated services.
 *
 * @see com.viaoa.oa.OA
 * @see com.viaoa.oa.api.services
 * @see com.viaoa.oa.api.internal
 */
package com.viaoa.oa.api;
