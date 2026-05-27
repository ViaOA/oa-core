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
package com.viaoa.graph.service;

//CODEX unit tests <todo>


/* CODEX Invariants

GRAPH-SERVICE-001 — Parent Services Are Graph-Owned Runtime Coordinators
Contract statement: Parent services in com.viaoa.graph.service are owned by a single OAGraph and coordinate object,
Hub, sync, replication, and trigger behavior for that graph only.
Rationale: Runtime authority must remain graph-scoped so object identity, Hub state, sync role, replication,
triggers, events, and datasource coordination do not drift across graphs.
Source scope: OAObjectService, HubService, OASyncService, OAReplicationService, OATriggerService.
Related CODEX findings: Parent service ownership and graph target routing findings.
Suggested unit tests: testParentServicesUseOwningGraphOnly(), testForeignGraphOperationRejectedOrRoutedByContract(),
testParentServiceDoesNotCoordinateForeignGraphState()
Spec target section: OG Runtime Services / Graph Ownership

GRAPH-SERVICE-002 — Parent Service Initialization Is Single And Complete
Contract statement: A parent service must initialize once, publish child/dependent services only after required
dependencies are available, and reject or visibly fail duplicate or partial initialization.
Rationale: Partially initialized parent services can expose incomplete object, Hub, sync, replication, or trigger
behavior.
Source scope: OAObjectService, HubService, OASyncService, OAReplicationService, OATriggerService.
Related CODEX findings: GRAPH_PARENT_SERVICES_INITIALIZE_ONCE; child service creation after parent init; partial
sync startup failure.
Suggested unit tests: testParentServiceInitializeOnce(), testDuplicateParentInitFailsPredictably(),
testPartialInitDoesNotPublishReadyService()
Spec target section: OG Runtime Services / Service Initialization

GRAPH-SERVICE-003 — Child Service Creation Is Coordinated By Parent Services
Contract statement: Child services must be created, owned, and exposed only through their parent coordinator; parent
services remain responsible for cross-service dependency hooks and runtime authority boundaries.
Rationale: Child services implement focused behavior, while parent services preserve ordering, lifecycle,
dependency, and role semantics.
Source scope: OAObjectService, HubService, object child services, Hub child services.
Related CODEX findings: GRAPH_CHILD_SERVICE_CREATION_IS_SINGLE_AND_AFTER_PARENT_INIT;
GRAPH_CHILD_SERVICES_ARE_NOT_PUBLIC_APP_SURFACE.
Suggested unit tests: testChildServiceCreationRequiresParentInitialization(),
testConcurrentChildServiceAccessCreatesSingleInstance(), testExternalPackagesDoNotUseChildServiceAsPublicSurface()
Spec target section: OG Runtime Services / Parent-Child Service Boundary

GRAPH-SERVICE-004 — Parent Services Own Cross-Service Orchestration
Contract statement: Any operation requiring coordination across object, Hub, sync, replication, trigger, datasource,
cache, event, or metadata services must route through the appropriate parent service boundary.
Rationale: Cross-service behavior must be staged and ordered centrally to prevent sideways coupling and inconsistent
runtime state.
Source scope: OAObjectService, HubService, OASyncService, OAReplicationService, OATriggerService.
Related CODEX findings: Parent/child service orchestration and child sync hook role-guard findings.
Suggested unit tests: testCrossServiceOperationRoutesThroughParentCoordinator(),
testChildServiceDoesNotCallSiblingServiceDirectlyForRuntimeAuthority(), testParentHookPreservesServiceOrdering()
Spec target section: OG Runtime Services / Orchestration Semantics

GRAPH-ROLE-001 — Runtime Role Semantics Are Centralized
Contract statement: Single-user, server, client, and unconfigured runtime roles must be interpreted consistently by
parent services; child services should use parent-provided role guards instead of duplicating role decisions.
Rationale: Role drift causes object/Hub operations to choose the wrong local, remote, datasource, or sync path.
Source scope: OASyncService, OAObjectService, HubService, OAReplicationService, OATriggerService.
Related CODEX findings: GRAPH_CHILD_SYNC_HOOKS_USE_PARENT_ROLE_GUARDS; GRAPH_SYNC_CLIENT_OPS_REQUIRE_CLIENT_ROLE;
GRAPH_SYNC_SERVER_OPS_REQUIRE_SERVER_ROLE.
Suggested unit tests: testSingleUserServerClientRolesAreDistinct(), testChildSyncHooksUseParentRoleGuards(),
testWrongRoleOperationFailsPredictably()
Spec target section: OG Runtime Services / Runtime Role Semantics

GRAPH-SYNC-001 — Sync Role Transitions Are Atomic Or Visibly Incomplete
Contract statement: Sync service role creation, start, stop, and transition operations must leave exactly one valid
visible sync role/state, or fail visibly with cleanup of partially started resources.
Rationale: Sync state coordinates remote calls, cache save, client/server behavior, and graph-wide runtime
authority.
Source scope: OASyncService.
Related CODEX findings: GRAPH_SYNC_ROLE_TRANSITIONS_ARE_ATOMIC; partial sync/remote startup failure leaves resources
running while service reports not running.
Suggested unit tests: testConcurrentSyncRoleTransitionsLeaveOneValidState(),
testFailedSyncStartupCleansPartialResources(), testSyncStopLeavesNoStaleRunningResources()
Spec target section: OG Runtime Services / Sync Lifecycle

GRAPH-SYNC-002 — Sync Client And Server Operations Require Matching Role
Contract statement: Client-only sync operations must require client role, server-only sync operations must require
server role, and single-user/unconfigured roles must no-op or fail according to documented contract.
Rationale: Incorrect role acceptance can corrupt remote/session state, cache save behavior, and graph-wide client/
server coordination.
Source scope: OASyncService, SyncOps integration, sync hooks used by parent services.
Related CODEX findings: GRAPH_SYNC_CLIENT_OPS_REQUIRE_CLIENT_ROLE; GRAPH_SYNC_SERVER_OPS_REQUIRE_SERVER_ROLE.
Suggested unit tests: testGetClientInfoRequiresClientRole(), testSaveCacheRequiresServerRole(),
testSingleUserSyncOpsUseDocumentedNoopOrFailure()
Spec target section: OG Runtime Services / Sync Role Guards

GRAPH-REPL-001 — Replication Uses The Owning Sync Service
Contract statement: Replication service operations must use the owning graph’s sync service and must not attach to
or depend on a sync service from another graph.
Rationale: Replication depends on graph identity, sync role, object identity, Hub ordering, and runtime ownership.
Source scope: OAReplicationService, OASyncService.
Related CODEX findings: GRAPH_REPLICATION_USES_OWNING_SYNC_SERVICE.
Suggested unit tests: testReplicationStartRequiresOwningSyncServer(),
testReplicationCannotUseForeignGraphSyncService(), testReplicationFailsPredictablyWithoutRequiredSyncRole()
Spec target section: OG Runtime Services / Replication Coordination

GRAPH-REPL-002 — Replication Startup And Runtime State Are Explicit
Contract statement: Replication start/stop behavior must publish running state only after required sync/graph
dependencies are available and must clean up partial setup on failure.
Rationale: Replication false-starts can leave Store/Corp convergence, replay, and sync assumptions in inconsistent
state.
Source scope: OAReplicationService, OASyncService.
Related CODEX findings: Replication start-without-sync-server risk.
Suggested unit tests: testReplicationStartupDoesNotReportRunningBeforeDependenciesReady(),
testReplicationStartupFailureLeavesRetryableState(), testReplicationStopCleansRunningState()
Spec target section: OG Runtime Services / Replication Lifecycle

GRAPH-TRIGGER-001 — Trigger Registration Targets The Owning Graph Explicitly
Contract statement: Trigger registration, removal, and execution must target the intended owning graph and must not
silently register against a wrong graph or hidden service surface.
Rationale: Triggers react to graph-scoped object/class/property behavior and can produce runtime side effects.
Source scope: OATriggerService, TriggerOps integration, OAGraph trigger facade integration.
Related CODEX findings: GRAPH_TRIGGER_TARGET_GRAPH_IS_EXPLICIT; public trigger surface completeness findings.
Suggested unit tests: testTriggerRegistrationUsesOwningGraph(),
testForeignGraphTriggerRegistrationRejectedOrRoutedByContract(), testRemovedTriggerDoesNotFire()
Spec target section: OG Runtime Services / Trigger Coordination

GRAPH-TRIGGER-002 — Async Trigger Work Preserves Runtime Context
Contract statement: Async trigger execution must capture or establish the required graph/runtime context, preserve
relevant ThreadLocal flags, and restore prior state after execution.
Rationale: Trigger work can run outside the originating thread but must not leak or lose sync/event/security/runtime
state.
Source scope: OATriggerService async execution paths.
Related CODEX findings: GRAPH_ASYNC_TRIGGER_PRESERVES_RUNTIME_THREAD_FLAGS; send-sync preservation findings.
Suggested unit tests: testAsyncTriggerPreservesSendSyncFlagByContract(),
testAsyncTriggerRestoresRuntimeContextAfterFailure(), testAsyncTriggerDoesNotLeakThreadLocalState()
Spec target section: OG Runtime Services / Async Trigger Context

GRAPH-TRIGGER-003 — Async Trigger Failure Must Be Observable
Contract statement: Background trigger execution failure must be logged, surfaced, retained, or otherwise observable
according to contract; it must not disappear as silent false success.
Rationale: Trigger failures can leave derived state, business rules, sync side effects, or runtime observers
incomplete.
Source scope: OATriggerService runTrigger/executor submission paths.
Related CODEX findings: ExecutorService.submit captures background trigger failures without observable failure path.
Suggested unit tests: testAsyncTriggerFailureIsObservable(), testTriggerExecutorDoesNotHideRuntimeException(),
testFailedAsyncTriggerDoesNotReportSuccessSilently()
Spec target section: OG Runtime Services / Trigger Failure Semantics

GRAPH-TRIGGER-004 — Trigger Executor Lifecycle Is Explicit
Contract statement: Trigger executor ownership, shutdown, graph reset behavior, pending work, and stale graph-
reference cleanup must be explicit and deterministic.
Rationale: Stale trigger work can execute against old graph state or retain graph/runtime resources after shutdown.
Source scope: OATriggerService executor lifecycle, graph reset/shutdown integration.
Related CODEX findings: GRAPH_TRIGGER_EXECUTOR_LIFECYCLE_IS_EXPLICIT.
Suggested unit tests: testGraphShutdownStopsTriggerExecutorByContract(),
testPendingTriggerWorkAfterShutdownUsesDocumentedBehavior(), testTriggerExecutorDoesNotRetainClosedGraph()
Spec target section: OG Runtime Services / Trigger Executor Lifecycle

GRAPH-TL-001 — Parent Services Restore Runtime Context They Change
Contract statement: Parent services that set ThreadLocal or runtime context state for sync, trigger, replication,
object, Hub, datasource, event, or remote coordination must restore the previous state in finally.
Rationale: Runtime context leaks can suppress sync/events, apply wrong role assumptions, or contaminate later graph
operations.
Source scope: OAObjectService, HubService, OASyncService, OAReplicationService, OATriggerService.
Related CODEX findings: GRAPH_ASYNC_TRIGGER_PRESERVES_RUNTIME_THREAD_FLAGS; child sync hook/runtime flag findings.
Suggested unit tests: testParentServiceRestoresThreadLocalAfterException(),
testTriggerRuntimeFlagsRestoredAfterAsyncFailure(), testSyncContextDoesNotLeakAcrossOperations()
Spec target section: OG Runtime Services / Runtime Context Restoration

GRAPH-ORDER-001 — Cross-Service Side Effects Follow Defined Ordering
Contract statement: Parent services must coordinate cross-service side effects so lifecycle state, cache state,
events, sync messages, replication hooks, and trigger execution observe the intended completed order.
Rationale: Object/Hub child services define subsystem mutation details, but parent orchestration defines when cross-
service observers are allowed to see completed state.
Source scope: OAObjectService, HubService, OASyncService, OAReplicationService, OATriggerService.
Related CODEX findings: Cross-service failure, trigger, sync, and role-guard findings.
Suggested unit tests: testEventSyncReplicationTriggerOrderingForObjectMutation(),
testHubMutationCrossServiceOrderingIsDeterministic(), testCrossServiceObserversDoNotSeePreCommitState()
Spec target section: OG Runtime Services / Cross-Service Ordering

GRAPH-FAILURE-001 — Cross-Service Failure Propagates Visibly
Contract statement: Parent services must propagate failures from child services or dependent runtime services
visibly unless the operation has a documented fallback/no-op behavior.
Rationale: Silent parent-level success after child failure creates graph-wide false state across object, Hub, sync,
replication, or trigger behavior.
Source scope: OAObjectService, HubService, OASyncService, OAReplicationService, OATriggerService.
Related CODEX findings: Partial sync startup failure; trigger async hidden failure; remote/role operation failure
findings.
Suggested unit tests: testChildServiceFailurePropagatesThroughParent(),
testDependentRuntimeFailureDoesNotReturnParentSuccess(), testDocumentedNoopRolePathIsDistinguishableFromFailure()
Spec target section: OG Runtime Services / Failure Propagation

GRAPH-FAILURE-002 — Partial Cross-Service Progress Must Not Become False Success
Contract statement: If orchestration across services partially completes and then fails, parent services must not
publish graph-level completion, ready/running state, sync/replication success, or trigger success unless the
required stages completed.
Rationale: Cross-service orchestration can otherwise leave mixed runtime state that appears successful and blocks
retry.
Source scope: OASyncService startup/shutdown, OAReplicationService startup, OATriggerService async work,
OAObjectService/HubService orchestration hooks.
Related CODEX findings: Partial sync/remote resources running after failed startup; async trigger silent false-
success.
Suggested unit tests: testPartialSyncStartupDoesNotReportRunning(),
testPartialTriggerSubmissionFailureDoesNotReportSuccess(), testPartialCrossServiceFailureLeavesRetryableState()
Spec target section: OG Runtime Services / Partial Progress Semantics

GRAPH-RETRY-001 — Failed Orchestration Leaves Retryable State
Contract statement: After visible failure in parent service orchestration, graph service state must remain retryable
or explicitly terminal according to contract.
Rationale: Runtime services need safe recovery after failed sync startup, replication startup, trigger execution, or
service initialization.
Source scope: OASyncService, OAReplicationService, OATriggerService, OAObjectService, HubService.
Related CODEX findings: Partial startup failure refusing future starts; trigger executor lifecycle findings.
Suggested unit tests: testFailedSyncStartupCanRetryAfterCleanup(), testFailedReplicationStartupCanRetry(),
testFailedTriggerExecutorSetupDoesNotPoisonService()
Spec target section: OG Runtime Services / Retry Semantics

GRAPH-CONCURRENT-001 — Parent Service State Is Safely Published
Contract statement: Parent service lifecycle, child service references, role state, executor state, and running/
started flags must be thread-safe, safely published, or explicitly confined.
Rationale: Graph services are accessed from UI, datasource, remote, sync, trigger, and background paths.
Source scope: OAObjectService, HubService, OASyncService, OAReplicationService, OATriggerService.
Related CODEX findings: Concurrent init/getter access; concurrent sync role creation/start/stop.
Suggested unit tests: testConcurrentParentInitHasSingleOutcome(), testConcurrentSyncStartStopLeavesValidState(),
testConcurrentTriggerRegistrationDoesNotCorruptServiceState()
Spec target section: OG Runtime Services / Concurrency Semantics

GRAPH-TXN-001 — Parent Orchestration Respects Transaction Boundaries
Contract statement: When graph-level work participates in a transaction, parent services must coordinate child work
so committed semantics, events, sync/replication hooks, and triggers align with the transaction stage.
Rationale: Parent services are the boundary where object, Hub, event, sync, and trigger side effects meet
transaction semantics.
Source scope: OAObjectService, HubService, OASyncService, OAReplicationService, OATriggerService.
Related CODEX findings: No direct CODEX finding in this package; included as graph-level orchestration contract.
Suggested unit tests: testTransactionCommitPublishesCrossServiceSideEffectsInOrder(),
testTransactionRollbackSuppressesCommittedSyncAndTriggerSideEffects(),
testTransactionFailureDoesNotPublishGraphSuccess()
Spec target section: OG Runtime Services / Transaction Coordination

GRAPH-DETERMINISM-001 — Parent Orchestration Is Deterministic For The Same Runtime State
Contract statement: For the same graph state, service dependencies, runtime role, child-service outcomes, and
callback outcomes, parent service orchestration must produce the same visible service state and cross-service side
effects.
Rationale: Deterministic orchestration is required for testing, debugging, sync/replication correctness, and
generated application behavior.
Source scope: OAObjectService, HubService, OASyncService, OAReplicationService, OATriggerService.
Related CODEX findings: Package-wide role, lifecycle, replication, and trigger orchestration findings.
Suggested unit tests: testSameSyncStartupScenarioProducesSameState(),
testSameTriggerScenarioProducesSameExecutionOrdering(), testSameCrossServiceFailureProducesSameVisibleState()
Spec target section: OG Runtime Services / Deterministic Orchestration

*/

