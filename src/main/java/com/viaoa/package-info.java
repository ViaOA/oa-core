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
 * Core package for OA (Object Automation), a model-driven, executable
 * object-graph framework designed for building large-scale, distributed,
 * real-time enterprise applications.
 * <p>
 * OA provides a complete end-to-end architecture centered around a richly
 * instrumented domain model. Rather than assembling dozens of external
 * frameworks, OA offers a coherent, tightly integrated platform where:
 * <ul>
 *   <li>the domain model is the application,</li>
 *   <li>object graphs are live, observable, and distributed,</li>
 *   <li>UI, server, datasource, and remote layers are all synchronized
 *       automatically,</li>
 *   <li>application logic emerges naturally through object relationships,
 *       Hubs, and metadata.</li>
 * </ul>
 *
 * <h2>Core Architectural Components</h2>
 *
 * <h3>OAObject / OAObjectGraph</h3>
 * Rich domain objects with:
 * <ul>
 *   <li>identity and GUID management,</li>
 *   <li>change tracking and edit state,</li>
 *   <li>lazy loading,</li>
 *   <li>metadata (properties, links, calculations),</li>
 *   <li>serialization with property-path control,</li>
 *   <li>graph traversal and visiting.</li>
 * </ul>
 *
 * <h3>Hub&lt;T&gt;</h3>
 * OA’s observable collection:
 * <ul>
 *   <li>master/detail relationships,</li>
 *   <li>active object (cursor) tracking,</li>
 *   <li>sharing and linking between controllers,</li>
 *   <li>filters, sorters, matchers, and live indexing,</li>
 *   <li>distributed sync of collection changes.</li>
 * </ul>
 *
 * <h3>Property Paths</h3>
 * A uniform dot-notation language used everywhere, including:
 * <ul>
 *   <li>filters and queries,</li>
 *   <li>templates,</li>
 *   <li>detail/sibling loading,</li>
 *   <li>JSON/XML serialization,</li>
 *   <li>UI binding,</li>
 *   <li>datasource column mapping.</li>
 * </ul>
 *
 * <h3>Datasources</h3>
 * Pluggable datasource implementations:
 * <ul>
 *   <li>JDBC (SQL databases),</li>
 *   <li>REST,</li>
 *   <li>Client/Server,</li>
 *   <li>ObjectCache,</li>
 *   <li>Multiplexer remote datasource,</li>
 *   <li>in-memory and hybrid combinations.</li>
 * </ul>
 * All datasources follow a unified API for select, iterator, insert, update,
 * and delete operations with cascade-aware behavior.
 *
 * <h3>Distributed Sync</h3>
 * OA includes a full multiplexer-based remote method invocation system:
 * <ul>
 *   <li>server → client broadcast of object and hub changes,</li>
 *   <li>client → server updates with edit-level granularity,</li>
 *   <li>remote object loading with depth/sibling rules,</li>
 *   <li>per-client sessions tracking GUIDs and locks,</li>
 *   <li>file transfer subsystem,</li>
 *   <li>real-time conflict detection.</li>
 * </ul>
 *
 * <h3>Templates</h3>
 * {@code OATemplate} provides a lightweight templating engine based on
 * property paths to generate:
 * <ul>
 *   <li>HTML,</li>
 *   <li>emails,</li>
 *   <li>documents,</li>
 *   <li>custom text formats.</li>
 * </ul>
 *
 * <h3>UI Framework Integration</h3>
 * {@code com.viaoa.uicontroller} provides MVC binding between:
 * <ul>
 *   <li>domain objects (OAObject),</li>
 *   <li>Hubs (collections),</li>
 *   <li>UI widgets across different frameworks.</li>
 * </ul>
 * Hubs define the live state, controllers simply bind UI widgets to hubs.
 *
 * <h3>JSON Serialization</h3>
 * {@code com.viaoa.json} and {@code com.viaoa.json.jackson} integrate with
 * Jackson to provide object-graph-aware serialization with identity and depth
 * management. Supports:
 * <ul>
 *   <li>full graph,</li>
 *   <li>partial graph,</li>
 *   <li>property-path-driven serialization,</li>
 *   <li>OA temporal types.</li>
 * </ul>
 *
 * <h2>Design Philosophy</h2>
 * OA is intentionally:
 * <ul>
 *   <li><b>minimal</b> – few classes, little configuration, no XML, no heavy
 *       frameworks;</li>
 *   <li><b>model-driven</b> – the domain model defines behavior through
 *       metadata;</li>
 *   <li><b>executable</b> – the architecture is embodied in live objects and
 *       Hubs, not code generation glue;</li>
 *   <li><b>deterministic</b> – consistent object identity, consistent ordering,
 *       predictable sync behavior;</li>
 *   <li><b>observable</b> – changes flow automatically through the system;</li>
 *   <li><b>distributed-ready</b> – built from day one for multi-client sync.</li>
 * </ul>
 *
 * <p>
 * OA’s goal is to turn domain modeling into application logic, and application
 * logic into a live, distributed, synchronized object graph with minimal code.
 */
package com.viaoa;

/* CODEX Invariants

VIAOA-ROOT-001 — Platform Runtime Authority
Contract statement:
The root com.viaoa package defines OA as a metadata-driven executable Object Graph runtime platform; child packages
own subsystem-specific mechanics, but their public behavior must remain consistent with the root platform contract.
Rationale:
The root package is the top-level semantic boundary for OA 4.0/OAGraph and should describe platform-wide authority
without duplicating object, hub, graph, datasource, sync, or other subsystem invariants.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root CODEX block is placeholder-style and duplicates many child-package invariants.
Suggested unit tests:
Verify root/package-level documentation and child package invariant sections do not contradict root platform
authority.
Spec target section:
Root platform authority and package hierarchy.

VIAOA-ROOT-002 — Executable Blueprint Alignment
Contract statement:
Generated model blueprints, annotations, metadata, OAObjects, Hubs, paths, queries, datasources, serialization,
sync, and replication must be interpreted as one coherent executable runtime model.
Rationale:
OA treats model structure as executable semantic input, not passive documentation or disconnected helper metadata.
Source scope:
com.viaoa package-info.java and child package contracts as context.
Related CODEX findings:
Existing root block mentions model-driven and executable behavior but does not state the cross-package alignment
contract precisely.
Suggested unit tests:
Verify a representative generated model has consistent metadata, path/query behavior, datasource identity,
serialization identity, and graph ownership assumptions.
Spec target section:
Blueprint-to-runtime semantic correctness.

VIAOA-ROOT-003 — Object Graph Runtime Abstraction
Contract statement:
The Object Graph is the platform-level abstraction for semantic runtime behavior; object lifecycle, hub membership,
metadata resolution, persistence, serialization, sync, replication, callbacks, triggers, and observation must be
coordinated through graph-compatible runtime contracts.
Rationale:
OA runtime behavior is meaningful only when subsystem operations preserve graph semantics across package boundaries.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root block lists many graph-related subsystem details but lacks a single root-level graph abstraction
contract.
Suggested unit tests:
Verify a graph-level lifecycle scenario keeps object, hub, cache, datasource, event, and serialization views
semantically aligned.
Spec target section:
Object Graph platform semantics.

VIAOA-ROOT-004 — Runtime Authority Boundaries
Contract statement:
Each OA subsystem has a defined authority boundary, and root-level behavior must not treat local object state, cache
state, datasource state, remote delivery, sync application, replication replay, or transaction completion as
interchangeable forms of success.
Rationale:
OA operations often cross in-memory, persistence, remote, and distributed boundaries; root semantics must preserve
the distinction between transport, storage, and semantic Object Graph success.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root notes mention save/delete/remote success but mix subsystem details with root-level concepts.
Suggested unit tests:
Verify failure scenarios distinguish local mutation success, datasource success, remote invocation success, sync
success, replication success, and final semantic graph success.
Spec target section:
Runtime authority and success-boundary semantics.

VIAOA-ROOT-005 — Deterministic Platform Behavior
Contract statement:
For the same runtime metadata, graph state, inputs, runtime role, and execution context, OA platform behavior must
be deterministic in object identity, metadata interpretation, lifecycle ordering, path/query interpretation,
serialization boundaries, and distributed message semantics where ordering is externally observable.
Rationale:
Determinism is required for executable blueprints, reproducible tests, distributed runtime correctness, and AI-
readable semantic contracts.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root block identifies determinism broadly but scatters it across child-package checklists.
Suggested unit tests:
Verify repeatable outcomes for representative metadata resolution, object graph mutation, serialization, and
distributed operation scenarios.
Spec target section:
Deterministic platform runtime behavior.

VIAOA-ROOT-006 — Identity And Cache Coherence
Contract statement:
Across OA runtime subsystems, class/key identity, GUID identity, cache authority, serialization identity, datasource
identity, sync identity, and replication identity must resolve to compatible semantic object identity within the
relevant graph/runtime scope.
Rationale:
OA depends on one coherent identity model even though identity is exposed through multiple packages and runtime
boundaries.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root block duplicates detailed cache/object identity rules that belong to child packages.
Suggested unit tests:
Verify a representative object maintains coherent identity through cache lookup, datasource load/save, serialization
round trip, and distributed propagation.
Spec target section:
Platform identity/cache authority.

VIAOA-ROOT-007 — Lifecycle Stage Ordering
Contract statement:
Platform-visible lifecycle stages must occur in a consistent semantic order: metadata interpretation before runtime
use, graph/runtime ownership before graph-scoped mutation, mutation before after-observation, durable success before
durable-success reporting, and cleanup after lifecycle completion or failure.
Rationale:
Cross-package lifecycle ordering prevents observers, callbacks, persistence layers, and distributed runtimes from
seeing impossible or prematurely committed state.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root block contains many child-specific lifecycle rules but not a root-level ordering contract.
Suggested unit tests:
Verify create/load/save/delete/serialize/sync/replicate scenarios expose lifecycle state only at the appropriate
semantic stage.
Spec target section:
Lifecycle coordination semantics.

VIAOA-ROOT-008 — Observable And Callable Graph Semantics
Contract statement:
OA platform operations that publish events, callbacks, triggers, remote calls, bindings, projections, or diagnostics
must expose graph state that is internally consistent for the lifecycle stage being observed or invoked.
Rationale:
OA is an observable/callable graph runtime; observers and callers must not receive false semantic signals about
graph state.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root block lists events/listeners/triggers separately but does not define the platform-wide observable
graph contract.
Suggested unit tests:
Verify observers, callbacks, triggers, and remote-facing calls see consistent state during representative object and
Hub lifecycle operations.
Spec target section:
Observable/callable graph semantics.

VIAOA-ROOT-009 — False-Success Prevention
Contract statement:
No root-level OA operation or cross-package orchestration path may report semantic success when required runtime
metadata, graph ownership, identity reconciliation, persistence, communication, serialization, sync, replication, or
cleanup work failed or was not attempted.
Rationale:
False success is more dangerous than visible failure in a metadata-driven distributed runtime because it corrupts
caller assumptions and downstream graph behavior.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root block contains many failure examples but not a unified root false-success principle.
Suggested unit tests:
Verify representative cross-package failures are reported or exposed instead of returning successful semantic
completion.
Spec target section:
Failure semantics and false-success prevention.

VIAOA-ROOT-010 — Partial-Progress Visibility
Contract statement:
When a platform-level operation partially completes before failure, OA must preserve enough observable state,
exception context, lifecycle state, diagnostics, or recovery boundary information for callers and runtime services
to distinguish full success, full failure, and partial progress.
Rationale:
OA operations can span graphs, caches, datasources, transactions, remote endpoints, sync, and replication; partial
progress must not be hidden as ordinary completion.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root block discusses many failed operations but does not define partial-progress visibility as a root
invariant.
Suggested unit tests:
Verify failed multi-stage load/save/delete/serialize/sync/replication scenarios expose the stage and semantic
boundary of failure.
Spec target section:
Partial-progress visibility.

VIAOA-ROOT-011 — Runtime Context Restoration
Contract statement:
ThreadLocal and runtime-scoped context changes used for graph ownership, server/client role, sync suppression,
remote execution, transaction participation, loading state, event suppression, or diagnostic context must be
restored after the scoped operation completes, including exceptional completion.
Rationale:
OA uses runtime context across many subsystem boundaries; leaked context can cause cross-request, cross-thread, or
cross-graph semantic corruption.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root block includes ThreadLocal and runtime-context notes but mixes them with runtime-package
implementation details.
Suggested unit tests:
Verify representative scoped runtime-context operations restore prior context after normal return and thrown
exceptions.
Spec target section:
ThreadLocal/runtime-context restoration.

VIAOA-ROOT-012 — Distributed Runtime Boundary Correctness
Contract statement:
Remote, communication, sync, and replication behavior must preserve the distinction between local runtime mutation,
transport delivery, remote invocation completion, synchronized graph application, replicated replay, and eventual
semantic convergence.
Rationale:
OA distributed runtime correctness depends on clear boundaries between immediate connected sync, remote callable
behavior, and eventual/offline replication.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root block mentions remote/sync/replication behavior but duplicates child-package ordering and retry
details.
Suggested unit tests:
Verify representative distributed scenarios expose correct success/failure boundaries and do not conflate transport
success with semantic graph success.
Spec target section:
Distributed runtime correctness.

VIAOA-ROOT-013 — Metadata-Driven Runtime Validity
Contract statement:
A Java class, annotation, model definition, path, query, template, or datasource mapping is not semantically valid
merely because it exists syntactically; OA runtime behavior requires metadata interpretation that is complete,
consistent, and sufficient for the requested platform operation.
Rationale:
OA’s executable blueprint model depends on metadata validity, not just Java reflection or string parsing success.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root block emphasizes model-driven behavior but does not distinguish syntactic existence from runtime-
semantic validity.
Suggested unit tests:
Verify invalid or incomplete metadata prevents semantic runtime success in representative graph, path/query,
persistence, and serialization operations.
Spec target section:
Metadata-driven runtime behavior.

VIAOA-ROOT-014 — Cross-Package Contract Consistency
Contract statement:
Root and child package invariants must form a non-contradictory hierarchy: root invariants define platform-wide
semantics, parent packages define orchestration contracts, and leaf packages define detailed subsystem behavior.
Rationale:
The root package must keep OA’s architecture AI-readable and test-ready without duplicating or contradicting
subsystem-specific contracts.
Source scope:
com.viaoa package-info.java and child package invariant sections as context.
Related CODEX findings:
Existing root CODEX block duplicates many detailed child-package invariants and includes test-plan/commentary text.
Suggested unit tests:
Verify package invariant inventory reports no duplicate root/child responsibility conflicts and all packages use the
standardized invariant structure.
Spec target section:
AI-readable architecture readiness.

VIAOA-ROOT-015 — Digital Twin Runtime Semantics
Contract statement:
OA platform behavior must support runtime graph state as a digital twin of executable enterprise blueprints: object
identity, relationships, lifecycle state, metadata, persistence state, observable state, and distributed state must
remain semantically aligned within documented runtime boundaries.
Rationale:
The root package frames OA as a live semantic runtime engine, not a collection of independent utilities.
Source scope:
com.viaoa package-info.java.
Related CODEX findings:
Existing root documentation describes live distributed object graphs but the CODEX block does not state the digital-
twin platform contract.
Suggested unit tests:
Verify an end-to-end representative scenario keeps blueprint metadata, object graph state, datasource state,
serialization state, and distributed visibility aligned.
Spec target section:
Digital twin runtime semantics.

*/



