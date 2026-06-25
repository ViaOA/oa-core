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
package com.viaoa.oa.api;

//CODEX unit tests <todo>


/* CODEX Invariants

GRAPH-API-001 — Public Graph API Boundary
Contract statement:
com.viaoa.graph.api defines caller-visible Object Graph operation contracts; implementations may delegate to graph
services, but callers may rely only on the public API semantics exposed by this package.
Rationale:
The API package is the callable boundary into OA and must not leak internal service coordination details.
Source scope:
package-info.java, SyncOps, ReplOps, TriggerOps.
Related CODEX findings:
Existing package-info CODEX block is empty; ReplOps CODEX notes public API surface risk.
Suggested unit tests:
Verify public API methods enforce documented lifecycle and role boundaries without requiring callers to know
implementation classes.
Spec target section:
Public API boundary semantics.

GRAPH-API-002 — Graph-Scoped Runtime Authority
Contract statement:
Every API operation acts within the authority of its owning graph instance and must not apply triggers,
synchronization state, replication state, or distributed runtime behavior to another graph scope.
Rationale:
The public API is graph-facing; cross-graph authority leaks would violate Object Graph ownership semantics.
Source scope:
SyncOps, ReplOps, TriggerOps.
Related CODEX findings:
None beyond empty package invariant section.
Suggested unit tests:
Verify operations obtained from one graph affect only that graph’s sync, replication, and trigger state.
Spec target section:
Graph-scoped authority.

GRAPH-API-003 — Deterministic API Lifecycle State
Contract statement:
For the same graph state and same API calls, externally visible lifecycle state must be deterministic, including
configured role, running state, single-user state, registered trigger state, and exposed distributed-runtime state.
Rationale:
Callers need repeatable API behavior for generated blueprints, OAi/MCP access, runtime verification, and distributed
graph coordination.
Source scope:
SyncOps, ReplOps, TriggerOps.
Related CODEX findings:
ReplOps CODEX notes missing guarded public lifecycle contract.
Suggested unit tests:
Verify repeated role/configuration/start/stop/trigger operations expose stable state transitions and predictable
query results.
Spec target section:
Deterministic public API behavior.

GRAPH-API-004 — Sync Role Configuration Contract
Contract statement:
A sync API instance has at most one configured synchronization role at a time: single-user/unconfigured, server, or
client. Creating a server or client role must fail visibly when incompatible with the current configured or running
state.
Rationale:
Public sync behavior is role-based; callers must not be able to silently configure contradictory distributed graph
roles.
Source scope:
SyncOps.createServer, SyncOps.createClient, SyncOps.isSingleUser, SyncOps.isServer, SyncOps.isClient.
Related CODEX findings:
SyncOps CODEX placeholder indicates missing invariant coverage; method Javadocs define role constraints.
Suggested unit tests:
Verify server/client role creation, conflicting role creation, repeated role creation, and role query methods.
Spec target section:
Sync API role semantics.

GRAPH-API-005 — Sync Start Stop Lifecycle
Contract statement:
start must activate only a valid configured synchronization role, and stop must deactivate active synchronization
without erasing the configured role unless the public contract explicitly says otherwise. Starting without a role or
while already running must fail visibly.
Rationale:
Callers need a clear lifecycle boundary between configuration, active distributed coordination, and stopped-but-
configured state.
Source scope:
SyncOps.start, SyncOps.stop, SyncOps.isRunning.
Related CODEX findings:
SyncOps Javadocs state start/stop lifecycle requirements.
Suggested unit tests:
Verify start-before-configure failure, start-after-create success, duplicate start behavior, stop behavior, and
restart-after-stop behavior.
Spec target section:
Sync API lifecycle semantics.

GRAPH-API-006 — Distributed Success Boundary
Contract statement:
A public API call returning normally may indicate only the success boundary promised by that API method; it must not
imply datasource durability, remote semantic success, synchronized graph convergence, or replication convergence
unless that method explicitly defines such a boundary.
Rationale:
The graph API bridges local graph behavior and distributed runtime behavior; callers must not confuse API invocation
success with deeper semantic success.
Source scope:
SyncOps, ReplOps, TriggerOps.
Related CODEX findings:
ReplOps CODEX notes mismatch between advertised replication API and implementation lifecycle behavior.
Suggested unit tests:
Verify failures from sync/replication start/stop and trigger registration are surfaced rather than treated as
semantic graph success.
Spec target section:
API success and distributed boundary semantics.

GRAPH-API-007 — False-Success Prevention
Contract statement:
Invalid API usage, unsupported lifecycle transitions, missing configuration, failed startup, failed shutdown, failed
trigger registration, or failed trigger removal must be reported through the public API and must not be silently
treated as successful graph behavior.
Rationale:
Public API calls are the external contract into OA; silent success would corrupt caller assumptions and runtime
orchestration.
Source scope:
SyncOps, ReplOps, TriggerOps.
Related CODEX findings:
ReplOps CODEX notes start can be called without represented guarded public configuration.
Suggested unit tests:
Verify invalid sync role transitions, start without configuration, failed start, failed stop, null trigger
registration, duplicate trigger handling, and removal of missing triggers.
Spec target section:
Failure semantics and false-success prevention.

GRAPH-API-008 — Partial-Progress Visibility
Contract statement:
When an API operation fails after partial runtime work, the API must preserve enough observable state, exception
context, or queryable lifecycle state for callers to distinguish full success, full failure, and partially completed
work.
Rationale:
Graph API calls can cross service, listener, transport, and distributed boundaries where partial progress is
externally meaningful.
Source scope:
SyncOps.start, SyncOps.stop, ReplOps, TriggerOps.
Related CODEX findings:
ReplOps CODEX notes public contract does not expose lifecycle guard semantics.
Suggested unit tests:
Verify failed start/stop and failed trigger registration leave queryable state consistent with the actual lifecycle
boundary reached.
Spec target section:
Partial-progress visibility.

GRAPH-API-009 — Trigger Registration Semantics
Contract statement:
Adding a trigger through the graph API must either make that trigger active for the graph according to trigger
metadata and path semantics, or fail visibly without registering a partial trigger. Removing a trigger must report
whether an active registration was removed.
Rationale:
TriggerOps exposes reactive graph behavior; callers need atomic registration and deterministic removal visibility.
Source scope:
TriggerOps.addTrigger, TriggerOps.removeTrigger.
Related CODEX findings:
No direct CODEX comment in TriggerOps.
Suggested unit tests:
Verify add, add with skip-first-non-many-property, remove existing trigger, remove missing trigger, null trigger
behavior, and duplicate registration behavior.
Spec target section:
Trigger API semantics.

GRAPH-API-010 — Observable Graph API Semantics
Contract statement:
API operations that expose live graph behavior, including sync coordination and triggers, must publish or make
observable only graph state that is internally consistent for the lifecycle stage being observed.
Rationale:
The public API participates in observable/callable graph semantics and must not expose impossible intermediate state
to callers, listeners, OAi/MCP tools, or distributed peers.
Source scope:
SyncOps, ReplOps, TriggerOps.
Related CODEX findings:
Existing package-info CODEX block is empty; root and graph invariants identify observable/callable graph semantics
as a platform theme.
Suggested unit tests:
Verify observers triggered through API-managed behavior see consistent graph state during sync lifecycle and trigger
execution scenarios.
Spec target section:
Observable/callable graph semantics.

GRAPH-API-011 — Metadata-Driven Runtime Behavior
Contract statement:
API operations that depend on generated model classes, object metadata, paths, links, triggers, sync roles, or
replication roles must use runtime metadata as the semantic authority and must fail visibly when required metadata
is missing or invalid.
Rationale:
The graph API is the public entry point for executable blueprint behavior; syntactic Java availability is not enough
for semantic graph correctness.
Source scope:
SyncOps, ReplOps, TriggerOps.
Related CODEX findings:
No direct metadata CODEX comment in graph.api; package-info block is empty.
Suggested unit tests:
Verify API behavior against valid generated metadata and invalid or incomplete trigger/path/graph metadata.
Spec target section:
Metadata-driven public API behavior.

GRAPH-API-012 — Replication API Contract Completeness
Contract statement:
If replication is exposed through graph.api, its public interface must describe the caller-visible lifecycle and
role semantics needed to use it safely; if not yet supported, the API must fail explicitly rather than advertising
silent or empty usable behavior.
Rationale:
An empty public replication contract creates ambiguity between unsupported behavior, internal-only behavior, and
incomplete public API behavior.
Source scope:
ReplOps.
Related CODEX findings:
ReplOps CODEX finding GRAPH_REPLICATION_PUBLIC_CONTRACT_MATCHES_IMPLEMENTATION identifies an empty public contract
while implementation has lifecycle behavior.
Suggested unit tests:
Verify replication API either exposes guarded role/start/stop lifecycle behavior or clearly reports unsupported/
incomplete public operation.
Spec target section:
Replication public API boundary.

GRAPH-API-013 — Runtime Context Restoration
Contract statement:
Any public API operation that temporarily changes runtime context, graph role, sync-message behavior, trigger
execution context, or distributed invocation context must restore prior context after normal or exceptional
completion.
Rationale:
Public API calls can be used by application code, remote runtimes, and AI/MCP tools; leaked runtime context would
corrupt later graph operations.
Source scope:
SyncOps, ReplOps, TriggerOps.
Related CODEX findings:
None specific in graph.api; root/platform invariants identify ThreadLocal/runtime-context restoration as a cross-
package requirement.
Suggested unit tests:
Verify context-sensitive sync and trigger API operations restore prior runtime state after success and thrown
exceptions.
Spec target section:
ThreadLocal/runtime-context restoration.

GRAPH-API-014 — AI-Readable Callable Runtime Contract
Contract statement:
The public graph API must remain semantically explicit enough for generated code, tests, documentation, OAi/MCP
clients, and runtime verification tools to infer valid operations, invalid operations, lifecycle stages, and success
boundaries without depending on implementation internals.
Rationale:
graph.api is the public callable layer over executable enterprise blueprints and must be usable as an AI-readable
runtime contract.
Source scope:
package-info.java, SyncOps, ReplOps, TriggerOps.
Related CODEX findings:
Existing package-info CODEX block is empty; ReplOps CODEX notes incomplete public contract.
Suggested unit tests:
Verify public API documentation and behavior expose enough state and failure information for lifecycle validation
and generated test planning.
Spec target section:
OAi/MCP callable runtime readiness.

*/

