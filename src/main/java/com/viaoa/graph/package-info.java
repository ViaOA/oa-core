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
package com.viaoa.graph;


/* CODEX Invariants

GRAPH-OWNERSHIP-001 — OAGraph Is The Runtime Ownership Boundary
Contract statement: Every OAObject, Hub, runtime service, metadata binding, datasource route, sync/replication path,
trigger registration, and serialization boundary that belongs to a graph must resolve through that graph’s
authority.
Rationale: OA/OG runtime correctness depends on graph-scoped identity, lifecycle, Hub membership, cache, metadata,
datasource, sync, replication, and event semantics.
Source scope: OAGraph, OAGraphImpl, OAGraphInternal, direct graph facade methods, parent service access points.
Related CODEX findings: Graph ownership/routing findings; foreign class/object/Hub operation findings.
Suggested unit tests: testObjectOperationUsesOwningGraph(), testHubOperationUsesOwningGraph(),
testForeignGraphStateDoesNotUseWrongGraphAuthority()
Spec target section: OG Runtime / Graph Ownership

GRAPH-OWNERSHIP-002 — Cross-Graph Authority Leaks Are Forbidden
Contract statement: A graph must not silently operate on classes, objects, Hubs, services, metadata, datasource
state, sync state, or trigger state owned by another graph.
Rationale: Cross-graph leaks corrupt object identity, cache indexes, Hub links, datasource routing, sync messages,
and metadata assumptions.
Source scope: OAGraphImpl graph verbs, graph routing methods, object/Hub/service delegation boundaries.
Related CODEX findings: OAGraphImpl foreign class/object/Hub verb findings.
Suggested unit tests: testGraphVerbRejectsForeignClassByContract(), testGraphVerbRejectsForeignObjectByContract(),
testGraphVerbRejectsForeignHubByContract()
Spec target section: OG Runtime / Cross-Graph Isolation

GRAPH-APIX-001 — OAGraph Public Surface Is The Supported Runtime API
Contract statement: Application-facing graph operations must be exposed through OAGraph; implementation and internal
service surfaces are not application contracts.
Rationale: The public graph API must remain stable while internal orchestration and service implementation details
can evolve.
Source scope: OAGraph, OAGraphImpl, OAGraphInternal, graph.api., graph.api.internal..
Related CODEX findings: OAGraph trigger public surface completeness; OAGraphInternal internal API boundary findings.
Suggested unit tests: testPublicGraphApiExposesIntendedRuntimeVerbs(),
testPublicTriggerOperationsReachableThroughOAGraph(), testApplicationCodeDoesNotDependOnInternalGraphApis()
Spec target section: OG Runtime / Public API Boundary

GRAPH-INTERNAL-001 — Internal Graph APIs Are Runtime-Only
Contract statement: OAGraphInternal and com.viaoa.graph.api.internal APIs may be used by graph/runtime services but
must not define public application semantics.
Rationale: Internal APIs can expose staging, service, or lifecycle hooks that are unsafe as external contracts.
Source scope: OAGraphInternal, graph.api.internal.*, OAGraphImpl internal delegation.
Related CODEX findings: OAGraphInternal internal ops are not app contract; internal API import-boundary findings.
Suggested unit tests: testAppFacingPackagesDoNotImportGraphApiInternal(), testInternalGraphApisRemainRuntimeScoped()
Spec target section: OG Runtime / Internal API Boundary

GRAPH-LIFECYCLE-001 — Graph Initialization Publishes Ready State Only After Required Runtime Setup
Contract statement: A graph may report initialized or ready only after required package scanning, metadata binding,
parent services, child service dependencies, runtime role state, and graph routing state are complete.
Rationale: Partially initialized graph state can route object, Hub, datasource, sync, trigger, or serialization work
into incomplete runtime services.
Source scope: OAGraphImpl initialization, OARuntime graph creation/registration, parent service initialization.
Related CODEX findings: OAGraphImpl partial initialization after package scan failure;
GRAPH_INITIALIZED_MEANS_ALL_SERVICES_READY.
Suggested unit tests: testGraphInitFailureDoesNotReportInitialized(),
testInitializedGraphHasRequiredParentServicesReady(), testGraphInitFailureDoesNotPublishPartialGraphAsReady()
Spec target section: OG Runtime / Graph Lifecycle

GRAPH-LIFECYCLE-002 — Graph Shutdown And Reset Have Explicit Runtime Semantics
Contract statement: Graph shutdown/reset must define what happens to services, triggers, live views, sync/
replication state, caches, contexts, and pending work, and must not leave stale authoritative runtime state.
Rationale: Graph lifecycle boundaries must be safe for long-running servers, tests, tooling, and generated
applications.
Source scope: OAGraphImpl lifecycle/reset/shutdown behavior, service lifecycle boundaries, trigger/live-view/sync
integration.
Related CODEX findings: Trigger executor lifecycle; live view controller lifecycle; default graph reset findings.
Suggested unit tests: testGraphResetClearsOrPreservesStateByContract(),
testGraphShutdownDoesNotLeaveStaleTriggerWork(), testGraphResetCreatesFreshDefaultGraphWhenIntended()
Spec target section: OG Runtime / Graph Shutdown And Reset

GRAPH-ROUTE-001 — Graph Routing Is Deterministic
Contract statement: Graph lookup by package, class, object, Hub, and default runtime context must consistently
resolve the intended graph for the same runtime state.
Rationale: Metadata lookup, datasource routing, cache identity, sync package routing, and trigger registration
require stable graph resolution.
Source scope: OAGraphImpl routing methods, OARuntime graph lookup, direct graph facade routing.
Related CODEX findings: Class/object/package routing and default graph stability findings.
Suggested unit tests: testGraphLookupByClassMatchesPackageGraph(), testGraphLookupByObjectUsesObjectClassGraph(),
testGraphLookupByHubUsesOwningGraph()
Spec target section: OG Runtime / Graph Routing

GRAPH-ROUTE-002 — Default Graph Lifecycle Is Explicit
Contract statement: The default graph must be created, reused, reset, and replaced only according to explicit
runtime lifecycle rules.
Rationale: Accidental default graph churn splits metadata, cache, datasource, sync, context, and service ownership.
Source scope: OAGraphImpl default graph integration, OARuntime graph/default graph behavior.
Related CODEX findings: Default graph creation/reset findings.
Suggested unit tests: testDefaultGraphCreatedOncePerRuntimeContext(), testDefaultGraphStableAcrossCalls(),
testRuntimeResetCreatesFreshDefaultGraphOnlyWhenIntended()
Spec target section: OG Runtime / Default Graph Semantics

GRAPH-METADATA-001 — Metadata Binding Is Graph-Scoped And Complete Before Runtime Use
Contract statement: Package/class metadata must be registered, resolved, and bound to the owning graph before graph
operations depend on it for object, Hub, datasource, serialization, path, sync, or trigger behavior.
Rationale: Metadata is the semantic map between Java classes and runtime object graph behavior.
Source scope: OAGraphImpl package/class registration, metadata service boundaries, object/Hub service delegation.
Related CODEX findings: Annotation metadata and package scan initialization findings.
Suggested unit tests: testGraphClassRegistrationCreatesMetadataBeforeObjectUse(),
testMetadataLookupUsesOwningGraphPackage(), testFailedMetadataScanDoesNotPublishReadyGraph()
Spec target section: OG Runtime / Metadata Binding

GRAPH-IDENTITY-001 — Identity And Cache Authority Are Graph-Scoped
Contract statement: Object identity, key resolution, GUID identity, id-only references, cache membership, and
authoritative object instances must be scoped to one graph.
Rationale: Object identity is not global across independent graph runtimes; cache and serialization correctness
depend on graph authority.
Source scope: OAGraphImpl identity/cache delegation, object service boundary, serialization boundary.
Related CODEX findings: Cache identity drift, duplicate authoritative object, and graph ownership findings.
Suggested unit tests: testSameClassKeyInSameGraphResolvesSameObject(),
testSameClassKeyAcrossDifferentGraphsDoesNotShareAuthority(), testDeserializationUsesOwningGraphIdentity()
Spec target section: OG Runtime / Identity And Cache Authority

GRAPH-HUB-001 — Hub Runtime Authority Is Graph-Scoped
Contract statement: Hub membership, active object, master/detail state, link state, sorting, selection, and event
publication must be coordinated through the owning graph’s Hub runtime services.
Rationale: Hubs are semantic graph collections and must not be treated as graph-neutral lists.
Source scope: OAGraph Hub facade methods, HubService delegation, direct graph-to-Hub service boundaries.
Related CODEX findings: Child Hub service invariants for membership, AO, detail, sort, select, and failure
semantics.
Suggested unit tests: testGraphHubAddRoutesThroughOwningHubService(),
testGraphHubOperationDoesNotUseForeignHubService(), testGraphHubEventOrderingUsesOwningGraph()
Spec target section: OG Runtime / Hub Authority Boundary

GRAPH-OBJECT-001 — Object Runtime Authority Is Graph-Scoped
Contract statement: Object lifecycle, property mutation, load/save/delete, identity, callbacks, locks,
serialization, and traversal must be coordinated through the owning graph’s object runtime services.
Rationale: Object runtime behavior is the foundation for graph consistency, persistence, sync, replication, and
generated application semantics.
Source scope: OAGraph object facade methods, OAObjectService delegation, direct graph-to-object service boundaries.
Related CODEX findings: Child object service invariants for lifecycle, identity, save/delete, metadata,
serialization, and failure semantics.
Suggested unit tests: testGraphObjectSaveRoutesThroughOwningObjectService(),
testGraphObjectDeleteUsesOwningGraphAuthority(), testGraphObjectMutationDoesNotBypassObjectService()
Spec target section: OG Runtime / Object Authority Boundary

GRAPH-SERVICE-001 — Parent Services Own Cross-Service Orchestration
Contract statement: Graph-level operations that require object, Hub, datasource, cache, sync, replication, trigger,
serialization, event, or context coordination must route through parent service boundaries.
Rationale: Parent services define orchestration and prevent child service details from becoming graph-level
authority.
Source scope: OAGraphImpl, OAObjectService, HubService, OASyncService, OAReplicationService, OATriggerService.
Related CODEX findings: Parent service ownership, child service boundary, and orchestration findings.
Suggested unit tests: testGraphFacadeDelegatesThroughParentServices(),
testCrossServiceOperationRoutesThroughParentCoordinator(), testChildServicesDoNotBecomeGraphPublicSurface()
Spec target section: OG Runtime / Service Orchestration Boundary

GRAPH-ROLE-001 — Graph Runtime Role Semantics Are Explicit
Contract statement: Single-user, server, client, replication, and unconfigured graph roles must be distinct and
consistently interpreted at graph boundaries.
Rationale: Role ambiguity causes graph operations to use the wrong local, remote, datasource, sync, or replication
path.
Source scope: OAGraphImpl, OASyncService boundary, OAReplicationService boundary, object/Hub parent service
coordination.
Related CODEX findings: isServer vs !isClient findings; sync client/server operation guard findings; child sync hook
role guard findings.
Suggested unit tests: testSingleUserServerClientRolesAreDistinctAtGraphBoundary(),
testGraphServerOnlyOperationRequiresServerRole(), testGraphClientOnlyOperationRequiresClientRole()
Spec target section: OG Runtime / Runtime Role Semantics

GRAPH-DATASOURCE-001 — Datasource Resolution Is Graph-Scoped
Contract statement: Datasource lookup, load, save, refresh, delete, and select operations initiated through a graph
must resolve to the datasource authority configured for that graph and model class.
Rationale: Persisted state must match graph ownership, metadata, runtime role, cache identity, and sync semantics.
Source scope: OAGraphImpl datasource delegation, object and Hub service boundaries, metadata package/class routing.
Related CODEX findings: Save/load/delete datasource routing and graph role findings from child services.
Suggested unit tests: testGraphDatasourceLookupUsesOwningGraphClassRoute(),
testSingleUserGraphUsesLocalDatasource(), testClientGraphDatasourceOperationUsesClientAuthorityByContract()
Spec target section: OG Runtime / Datasource Authority

GRAPH-CONTEXT-001 — Graph Context And User Access Are Explicitly Scoped
Contract statement: Graph context, user access, context Hub, and runtime access overrides must be scoped to the
intended graph/thread/context object and restored or cleared according to contract.
Rationale: Context leakage can apply one user’s access rules, Hub scope, or graph assumptions to another operation.
Source scope: graph.context.OAContext, OAUserAccess, OAGraphImpl context integration, runtime context boundaries.
Related CODEX findings: OAContext.removeContext(null) semantics; weak access lifetime; OAUserAccess package/path/
configure-before-publish findings.
Suggested unit tests: testContextOverrideRestoredAfterGraphOperation(), testUserAccessDoesNotLeakAcrossContexts(),
testGraphContextLookupUsesExpectedContextScope()
Spec target section: OG Runtime / Context And Access Semantics

GRAPH-TL-001 — Runtime ThreadLocal State Must Be Restored At Graph Boundaries
Contract statement: Any graph operation that sets ThreadLocal or runtime context state for loading, saving,
deleting, sync, remote, trigger, serialization, traversal, context, or event suppression must restore prior state in
finally.
Rationale: Runtime flag leaks can suppress or misroute later graph operations.
Source scope: OAGraphImpl graph boundary methods, parent service delegation, context/sync/trigger/object/Hub
integration.
Related CODEX findings: ThreadLocal restoration, async trigger context, remote-thread suppression, and sync flag
findings.
Suggested unit tests: testGraphOperationRestoresThreadLocalAfterException(),
testAsyncGraphWorkRestoresRuntimeContext(), testSyncSuppressionDoesNotLeakAcrossGraphOperations()
Spec target section: OG Runtime / Runtime Context Restoration

GRAPH-SYNC-001 — Sync And Replication Are Graph-Level Authority Boundaries
Contract statement: Sync and replication operations must use the owning graph’s role, services, object identity, Hub
ordering, and runtime context; they must not attach to foreign graph state.
Rationale: Sync/replication divergence can corrupt distributed object graph state.
Source scope: OAGraphImpl sync/replication facade methods, OASyncService, OAReplicationService, object/Hub service
boundaries.
Related CODEX findings: Replication uses owning sync service; sync role transition and client/server guard findings.
Suggested unit tests: testReplicationUsesOwningGraphSyncService(), testSyncOperationDoesNotUseForeignGraphState(),
testGraphSyncRoleTransitionLeavesOneValidRole()
Spec target section: OG Runtime / Sync Replication Boundary

GRAPH-SERIAL-001 — Serialization Preserves Graph Identity Boundaries
Contract statement: Serialization and deserialization initiated through graph runtime must preserve graph identity,
object keys, Hub membership/order, reference/load state, and role-specific remote semantics.
Rationale: Serialized graph state crosses persistence, remote, sync, tooling, and cache boundaries.
Source scope: OAGraphImpl serialization boundaries, object/Hub serialization service boundaries.
Related CODEX findings: Object and Hub serialization identity/load-state findings.
Suggested unit tests: testDeserializationUsesOwningGraphIdentity(),
testSerializedHubPreservesGraphMembershipSemantics(), testRemoteSerializationPreservesGraphRoleSemantics()
Spec target section: OG Runtime / Serialization Boundary

GRAPH-TRIGGER-001 — Trigger Operations Are Graph-Scoped
Contract statement: Trigger registration, removal, and execution exposed through graph APIs must target the intended
graph and preserve graph runtime context.
Rationale: Triggers observe and mutate graph-scoped object/class/property behavior.
Source scope: OAGraph trigger facade methods, OATriggerService boundary, TriggerOps integration.
Related CODEX findings: Trigger public surface completeness; trigger target graph explicit; async trigger context/
failure findings.
Suggested unit tests: testAllGraphTriggerOperationsUseOwningGraph(),
testTriggerRegisteredThroughWrongGraphRejectedOrRoutedByContract(), testAsyncTriggerPreservesGraphRuntimeContext()
Spec target section: OG Runtime / Trigger Boundary

GRAPH-LIVEVIEW-001 — Live View And Convenience Controllers Have Explicit Ownership
Contract statement: Graph convenience APIs that create live views, combined Hubs, controllers, or managed runtime
views must define whether the graph or caller owns their lifecycle.
Rationale: Live views can retain listeners, Hubs, graph references, and object state beyond intended use.
Source scope: OAGraph/OAGraphImpl convenience/live view APIs and Hub composition boundaries.
Related CODEX findings: OAGraphImpl live view controller lifecycle and convenience live view ownership findings.
Suggested unit tests: testGraphLiveViewOwnershipIsDocumentedByBehavior(), testCallerOwnedLiveViewCanBeClosed(),
testGraphResetCleansGraphOwnedLiveViews()
Spec target section: OG Runtime / Live View Lifecycle

GRAPH-EVENT-001 — Graph-Level Events Represent Completed Runtime State
Contract statement: Graph-level orchestration must ensure events, triggers, sync messages, replication hooks, and
observable service state represent completed runtime transitions unless explicitly documented as before/participant
stages.
Rationale: Cross-service observers depend on graph-level event order and completion semantics.
Source scope: OAGraphImpl orchestration boundaries, parent service delegation, object/Hub/sync/trigger integration.
Related CODEX findings: Child package after-event, failed mutation, trigger, and sync false-success findings.
Suggested unit tests: testGraphAfterEventPublishedOnlyAfterCompletedMutation(),
testGraphSyncMessageAfterCompletedStateOnly(), testGraphTriggerObservesCompletedMutationByContract()
Spec target section: OG Runtime / Graph Event Semantics

GRAPH-FAILURE-001 — Graph Boundary Failures Are Visible
Contract statement: Failures in graph-level routing, initialization, service orchestration, datasource, sync,
replication, trigger, serialization, object, or Hub operations must be caller-visible or observable unless a
documented fallback/no-op applies.
Rationale: Silent graph-level failure produces corrupt object graph state that downstream services treat as
authoritative.
Source scope: OAGraphImpl facade methods, parent service boundaries, direct graph lifecycle operations.
Related CODEX findings: Partial initialization, partial sync startup, async trigger hidden failure, false-success
child service findings.
Suggested unit tests: testGraphInitFailureIsVisible(), testGraphOperationFailurePropagatesFromParentService(),
testDocumentedGraphNoopIsDistinguishableFromFailure()
Spec target section: OG Runtime / Failure Visibility

GRAPH-FAILURE-002 — Partial Progress Must Not Become Graph-Level False Success
Contract statement: If a graph-level operation partially completes and then fails, the graph must not publish ready/
running/completed state, events, sync/replication success, cache authority, or trigger success unless required
stages completed.
Rationale: Graph-level false success is the highest-risk silent corruption path.
Source scope: OAGraphImpl lifecycle/facade methods, parent service orchestration, sync/replication/trigger
integration.
Related CODEX findings: OAGraphImpl partial initialization; OASync partial startup; OATrigger async false-success;
child package partial mutation findings.
Suggested unit tests: testPartialGraphInitDoesNotReportReady(), testPartialSyncStartupDoesNotReportRunning(),
testPartialGraphMutationDoesNotPublishCompletedState()
Spec target section: OG Runtime / Partial Progress Semantics

GRAPH-RETRY-001 — Failed Graph Operations Leave Retryable Or Explicitly Terminal State
Contract statement: After visible graph-level failure, runtime state must remain retryable, refreshable, or
explicitly terminal according to contract.
Rationale: Applications, tests, tooling, and servers need safe recovery after failed initialization, sync startup,
datasource work, trigger execution, or graph mutations.
Source scope: OAGraphImpl, parent service lifecycle, sync/replication/trigger boundaries.
Related CODEX findings: Partial initialization/retry, sync startup retry, trigger executor lifecycle findings.
Suggested unit tests: testFailedGraphInitializationCanRetryOrFailsTerminallyByContract(),
testFailedSyncStartupCanRetryAfterCleanup(), testFailedTriggerSetupDoesNotPoisonGraph()
Spec target section: OG Runtime / Retry Semantics

GRAPH-CONCURRENT-001 — Shared Graph Runtime State Must Be Safely Published
Contract statement: Graph lifecycle state, routing maps, parent services, role state, context state, executors, live
views, and graph-owned caches must be thread-safe, safely published, or explicitly confined.
Rationale: Graph runtime is used by UI, datasource, remote, sync, trigger, background, and tooling flows.
Source scope: OAGraphImpl, graph context, parent service fields, sync/trigger lifecycle state.
Related CODEX findings: Concurrent service initialization, sync role transition, context publish, and trigger
executor findings.
Suggested unit tests: testConcurrentGraphLookupReturnsSameGraph(), testConcurrentGraphInitHasSingleOutcome(),
testConcurrentRoleTransitionLeavesValidGraphState()
Spec target section: OG Runtime / Concurrency Semantics

GRAPH-DETERMINISM-001 — Same Graph State Produces Same Runtime Behavior
Contract statement: For the same graph state, metadata, role, datasource result, context, object/Hub inputs, and
callback outcomes, graph APIs must produce deterministic routing, lifecycle state, events, sync/replication hooks,
and service side effects.
Rationale: Deterministic graph behavior is required for debugging, generated application semantics, sync/replication
correctness, and unit/stress testing.
Source scope: OAGraph, OAGraphImpl, OAGraphInternal boundaries, parent service delegation.
Related CODEX findings: Package-wide routing, initialization, role, context, service orchestration, and failure
findings.
Suggested unit tests: testSameGraphInputsProduceSameRouting(),
testSameMutationScenarioProducesSameGraphSideEffects(), testSameFailureScenarioProducesSameVisibleGraphState()
Spec target section: OG Runtime / Deterministic Graph Semantics

*/

