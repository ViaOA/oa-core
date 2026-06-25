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
 * 
 * - Internal Object Graph runtime contracts.
 * <p>
 * - This package defines the runtime-only collaboration interfaces used by
 * OA/OG - implementation classes to coordinate Object Graph behavior across
 * object, - Hub, synchronization, replication, and trigger services.
 * <p>
 * - These interfaces are not application-facing API. They are not part of the -
 * public {@code OA} surface, and they are not part of the future public OG
 * - Specification. They exist so runtime services can collaborate through named
 * - contracts without exposing concrete implementation classes or child-service
 * - machinery as supported public API.
 * <h2>Layering</h2>
 * <p>
 * - OA 4.0 separates graph access into distinct layers:
 * <ol>
 * <li>{@code OA}: small public application-facing graph verbs.</li>
 * <li>{@code com.viaoa.graph.api.services.*}: curated public advanced service
 * 
 * - contracts exposed through {@code graph.services()}.</li>
 * 
 * <li>{@code com.viaoa.graph.service.facade.*}: facade implementations of
 * 
 * - the public service contracts.</li>
 * 
 * <li>{@code com.viaoa.graph.api.internal.*}: internal runtime collaboration
 * 
 * - contracts.</li>
 * 
 * <li>{@code com.viaoa.graph.service.*} and child service packages:
 * 
 * - implementation machinery.</li>
 * 
 * </ol>
 * <h2>Purpose</h2>
 * <p>
 * - Internal contracts in this package describe lower-level operations needed
 * by - the graph runtime and OA framework internals, including:
 * <ul>
 * <li>object lifecycle, metadata, identity, cache, property, reflection,
 * 
 * - serialization, save/delete, and trigger coordination;</li>
 * 
 * <li>Hub membership, active object, detail, link, select, sort, share,
 * 
 * - serialization, status, and event coordination;</li>
 * 
 * <li>sync, replication, and trigger runtime hooks beyond the public graph
 * 
 * - surface.</li>
 * 
 * </ul>
 * <h2>Public API Boundary</h2>
 * <p>
 * - Application and external advanced access must go through -
 * {@code OA.services()} and the curated public contracts under -
 * {@code com.viaoa.graph.api.services.*}. If external or application code needs
 * - a capability, that capability belongs as a small explicit method on the -
 * facade API. If only OG runtime code needs a capability, it belongs in this -
 * internal API or in implementation services. If casts to known implementation
 * - classes become common, the facade API is missing a method.
 * <p>
 * - Internal services should implement {@code *InternalOps} contracts, not
 * public - facade contracts directly. Public facade implementations may
 * delegate to - internal services, but public {@code Ops} interfaces should not
 * depend on - internal contracts or implementation classes.
 * <h2>Dependency Direction</h2>
 * <p>
 * - Dependency direction is intentionally one-way:
 * <ul>
 * <li>public graph APIs do not depend on internal APIs;</li>
 * <li>public service APIs do not depend on internal services;</li>
 * <li>facade implementations may depend on internal runtime contracts and
 * 
 * - services;</li>
 * 
 * <li>internal contracts may expose runtime-level operations needed by OA/OG
 * 
 * - implementation code;</li>
 * 
 * <li>internal service implementations may evolve without changing the public
 * 
 * - graph API or public service facade API.</li>
 * 
 * </ul>
 * <h2>Implementation Naming</h2>
 * <p>
 * - Classes implementing interfaces from this package should generally include
 * - {@code Internal} in their implementation name, such as -
 * {@code OAObjectInternalService}, {@code HubInternalService}, -
 * {@code OASyncInternalService}, {@code OAReplicationInternalService}, or -
 * {@code OATriggerInternalService}. This makes the runtime boundary visible in
 * - code review and discourages application code from treating implementation -
 * services as public contracts.
 * <h2>CODEX Invariants</h2>
 * <h3>INTERNAL_CONTRACTS_ARE_NOT_PUBLIC_API</h3>
 * <p>
 * - Interfaces in {@code com.viaoa.graph.api.internal} are runtime-only -
 * collaboration contracts. They must not be documented, promoted, or relied on
 * - as application-facing API.
 * <h3>INTERNAL_CONTRACTS_ARE_NOT_OG_SPEC</h3>
 * <p>
 * - Internal contracts are not part of the future public OG Specification. -
 * Public OG specification work belongs in {@code OA} and -
 * {@code com.viaoa.graph.api.services.*}.
 * <h3>PUBLIC_ACCESS_GOES_THROUGH_GRAPH_SERVICES</h3>
 * <p>
 * - Advanced public access must flow through {@code graph.services()} and
 * curated - service interfaces under {@code com.viaoa.graph.api.services.*},
 * not through - casts to {@code OA}, implementation services, or
 * child services.
 * <h3>PUBLIC_OPS_DO_NOT_DEPEND_ON_INTERNAL_OPS</h3>
 * <p>
 * - Public service contracts must not import, extend, or expose internal -
 * contracts. Public APIs remain stable even when internal contracts change.
 * <h3>PUBLIC_OPS_DO_NOT_EXPOSE_INTERNAL_SERVICE_TYPES</h3>
 * <p>
 * - Public service contracts must not expose concrete implementation services,
 * - parent services, child services, or implementation-specific nested types in
 * - method signatures.
 * <h3>INTERNAL_CONTRACTS_DEFINE_RUNTIME_COLLABORATION</h3>
 * <p>
 * - Internal contracts exist to let OA/OG runtime services coordinate object, -
 * Hub, sync, replication, trigger, metadata, cache, serialization, and
 * lifecycle - behavior without turning implementation services into public API.
 * <h3>INTERNAL_SERVICES_IMPLEMENT_INTERNAL_OPS</h3>
 * <p>
 * - Runtime implementation services should implement {@code *InternalOps} -
 * contracts. They should not directly implement curated public facade -
 * contracts under {@code com.viaoa.graph.api.services.*}.
 * <h3>INTERNAL_IMPLEMENTATIONS_ARE_NAMED_INTERNAL</h3>
 * <p>
 * - Classes implementing internal contracts should use names that clearly mark
 * - them as internal runtime machinery, typically by including {@code Internal}
 * - in the class name.
 * <h3>FACADE_LAYER_IS_BOUNDARY_BETWEEN_PUBLIC_AND_INTERNAL</h3>
 * <p>
 * - {@code com.viaoa.graph.service.facade.*} is the adapter boundary between -
 * public service APIs and internal runtime services. Facades curate behavior; -
 * they must not become automatic pass-through delegates for every internal -
 * method.
 * <h3>NO_PUBLIC_BACK_DOOR_TO_OBJECT_OR_HUB_SERVICES</h3>
 * <p>
 * - OG must not expose public direct access to {@code OAObject*Service}, -
 * {@code Hub*Service}, or child implementation services. Repeated external -
 * need for such access indicates a missing facade API method.
 * <h3>INTERNAL_CONTRACTS_MAY_EVOLVE_WITHOUT_PUBLIC_API_CHANGE</h3>
 * <p>
 * - Internal contracts may change to support runtime implementation needs
 * without - forcing changes to {@code OA} or public service interfaces,
 * provided - the public contract behavior remains stable.
 * <h3>INTERNAL_API_DOES_NOT_IMPORT_IMPLEMENTATION_TYPES</h3>
 * <p>
 * - Internal API contracts should define shared runtime contract types in API
 * or - neutral runtime packages. They should avoid depending on concrete
 * service - implementation classes or implementation-owned nested types.
 * <h3>CASTS_ARE_CONTROLLED_IMPLEMENTATION_DETAILS</h3>
 * <p>
 * - Casts to internal graph or service implementation types are allowed only -
 * inside controlled OA/OG runtime implementation code. Application and -
 * developer code should not cast to known implementation classes to access -
 * behavior. - @see com.viaoa.graph.OA - @see
 * com.viaoa.graph.OA - @see com.viaoa.graph.api.services - @see
 * com.viaoa.graph.service.facade - @see com.viaoa.graph.service
 */
package com.viaoa.oa.api.internal;






















