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
package com.viaoa.runtime;

//CODEX unit tests <todo>

/* CODEX Invariants

RT-AUTHORITY-001 — OARuntime Is The Runtime Authority Entry Point
Contract statement: Runtime-wide access to graph, datasource, thread, thread-local, remote-thread, sync-role, and
runtime-owned services must go through OARuntime or services owned by OARuntime.
Rationale: OA 4.0 runtime behavior depends on a single authority boundary instead of scattered static delegate
assumptions.
Source scope: OARuntime, OADataSourceService, OAThreadService, OAThreadLocalService, OARemoteThreadService.
Related CODEX findings: Runtime lifecycle/reset risks; hidden global/static state findings.
Suggested unit tests: testOARuntimeExposesCoreRuntimeServices(), testRuntimeServicesAreStableAcrossRepeatedAccess(),
testRuntimeServiceAccessUsesRuntimeAuthority()
Spec target section: Runtime / Entry Point Semantics

RT-AUTHORITY-002 — Runtime-Owned Services Are Stable Within A Runtime Context
Contract statement: Runtime services must be created, owned, reused, and safely published by the active runtime
context until explicit reset/shutdown semantics replace them.
Rationale: Graph lookup, datasource routing, ThreadLocal behavior, and remote-thread handling must not drift across
unrelated service instances.
Source scope: OARuntime service fields/accessors, OADataSourceService, OAThreadService, OAThreadLocalService,
OARemoteThreadService.
Related CODEX findings: Hidden global/static state and reset risks.
Suggested unit tests: testRuntimeOwnsSingleDatasourceServiceInstance(),
testRuntimeOwnsSingleThreadLocalServiceInstance(), testRuntimeOwnsSingleThreadServiceInstance()
Spec target section: Runtime / Service Ownership

RT-BOUNDARY-001 — Runtime Kernel Has No External Module Dependency
Contract statement: Runtime services must remain kernel-level OA services and must not depend on UI, JDBC, XML,
JSON, web, or other optional/external modules.
Rationale: oa-core runtime must be usable as the zero-external-dependency foundation for graph, datasource, sync,
and tooling behavior.
Source scope: com.viaoa.runtime.*, service imports and construction paths.
Related CODEX findings: Runtime package boundary scans found structure clean; UI thread provider optionality.
Suggested unit tests: testRuntimePackageHasNoForbiddenModuleReferences(),
testRuntimeServicesInstantiateWithoutExternalModules(), testDefaultUiThreadProviderIsOptional()
Spec target section: Runtime / Core Boundary

RT-GRAPH-001 — Default Graph Is Stable Within Runtime
Contract statement: The default graph must be lazily created once and reused for the lifetime of the runtime unless
explicit runtime reset/lifecycle rules replace it.
Rationale: Default graph instability splits object identity, datasource ownership, Hub state, metadata, and sync
routing.
Source scope: OARuntime.graph(), graph fields/registry, OAGraphImpl creation boundary.
Related CODEX findings: Default graph lifecycle and reset findings.
Suggested unit tests: testDefaultGraphIsSameInstanceAcrossCalls(),
testDefaultGraphRecreatedOnlyAfterExplicitRuntimeReset(), testDefaultGraphDoesNotChangeDuringPackageLookup()
Spec target section: Runtime / Default Graph Semantics

RT-GRAPH-002 — Graph Lookup By Class/Object/Package Must Agree
Contract statement: Graph lookup by model class, OAObject instance, package name, or graph name must resolve to the
same intended graph for that model package according to documented canonicalization rules.
Rationale: Object services, Hub services, datasource routing, sync, metadata lookup, and serialization assume
consistent graph ownership.
Source scope: OARuntime.graph(Class<?>), graph(Object), graph(String), class/package graph helper registry.
Related CODEX findings: Graph routing/package assumptions; GRAPH_CLASS_CANONICALIZATION_IS_EXPLICIT.
Suggested unit tests: testGraphLookupByClassAndPackageMatch(), testGraphLookupByObjectUsesObjectClassGraph(),
testSubclassAndProxyClassCanonicalizationByContract()
Spec target section: Runtime / Graph Lookup Semantics

RT-GRAPH-003 — Graph Creation Must Not Silently Split A Package
Contract statement: Runtime must not create multiple independent authoritative graphs for the same package/name
unless explicitly requested and documented.
Rationale: Two authoritative graphs for the same model package split object identity, metadata, cache, datasource,
and sync services.
Source scope: OARuntime graph registry/lookup/create paths, OAGraphImpl creation boundary.
Related CODEX findings: Repeated package lookup and default graph split risks.
Suggested unit tests: testRepeatedPackageLookupDoesNotCreateDuplicateGraph(),
testExplicitNamedGraphCreationDoesNotHijackDefaultGraph(), testPackageGraphRegistryKeepsSingleAuthority()
Spec target section: Runtime / Graph Registry

RT-GRAPH-004 — Failed Graph Creation Must Not Fall Back To The Default Graph
Contract statement: If graph creation or initialization for a package fails, later lookup for that package must fail
visibly or follow an explicit failure-cache lifecycle; it must not silently return the default graph.
Rationale: Silent fallback routes objects, Hubs, metadata, and datasource work to the wrong graph after
initialization failure.
Source scope: OARuntime graph(String), createGraph/package graph helper cache, graph initialization failure
handling.
Related CODEX findings: FAILED_GRAPH_CREATION_NEVER_FALLS_BACK_TO_DEFAULT;
GRAPH_INIT_FAILURE_CACHE_HAS_EXPLICIT_LIFECYCLE.
Suggested unit tests: testFailedPackageGraphCreationDoesNotReturnDefaultGraph(),
testGraphInitFailureCacheLifecycleByContract(), testGraphCreationRetryAfterFailureUsesCleanStateOrClearError()
Spec target section: Runtime / Graph Initialization Failure

RT-GRAPH-005 — Runtime And Graph Ownership Must Not Become Cyclic
Contract statement: OARuntime may own graph and runtime services, and graphs may use runtime services, but graph
services must not create alternate runtime roots or redefine runtime ownership.
Rationale: Cyclic or duplicate ownership creates hidden service duplication and split lifecycle state.
Source scope: OARuntime, OAGraphImpl, runtime service accessors, graph service integration.
Related CODEX findings: Graph/runtime ownership risks.
Suggested unit tests: testGraphUsesRuntimeServicesFromOwningRuntime(),
testRuntimeAndGraphDoNotCreateAlternateServiceInstances(), testRuntimeServiceIdentityStableFromGraphAccess()
Spec target section: Runtime / Runtime-Graph Ownership

RT-DATASOURCE-001 — Datasource Registry Is Runtime-Owned
Contract statement: Datasource registration, unregistration, ordering, positioning, and lookup belong to the
runtime-owned OADataSourceService.
Rationale: Persistence routing must be runtime-wide, deterministic, and visible to graph/object/Hub services.
Source scope: OADataSourceService.register, unregister, get, setPosition, runtime datasource accessors.
Related CODEX findings: Datasource registry order and lifecycle risks.
Suggested unit tests: testDatasourceRegisterMakesDatasourceDiscoverable(),
testDatasourceUnregisterRemovesDatasource(), testDatasourceSetPositionChangesRoutingOrder()
Spec target section: Runtime / Datasource Registry

RT-DATASOURCE-002 — Datasource Lookup Respects Enabled, Last, Class, And Filter Semantics
Contract statement: Datasource lookup must skip disabled datasources, prefer non-last supported datasources before
last/fallback datasources, and apply class/filter matching deterministically.
Rationale: Object cache, autonumber, client, and real storage datasources rely on stable priority and fallback
behavior.
Source scope: OADataSourceService.get(Class, OAFilter), datasource enabled/last support checks.
Related CODEX findings: Datasource invariant extraction and routing priorities.
Suggested unit tests: testDisabledDatasourceSkipped(), testLastDatasourceUsedOnlyAsFallback(),
testDatasourceFilterAffectsLookupByContract()
Spec target section: Runtime / Datasource Routing

RT-DATASOURCE-003 — Datasource Registry Order Changes Are Serialized
Contract statement: Concurrent register, unregister, and setPosition operations must preserve deterministic final
registry order and must not expose corrupt intermediate registry state.
Rationale: Datasource ordering determines persistence routing and fallback behavior.
Source scope: OADataSourceService registry mutation methods.
Related CODEX findings: DATASOURCE_REGISTRY_ORDER_CHANGES_ARE_SERIALIZED.
Suggested unit tests: testConcurrentDatasourceRegisterUnregisterSetPositionDeterministic(),
testDatasourceRegistryOrderStableAfterConcurrentMutation()
Spec target section: Runtime / Datasource Registry Concurrency

RT-DATASOURCE-004 — Datasource Registry Lifecycle Is Explicit
Contract statement: Runtime reset/shutdown/test lifecycle must explicitly define whether datasource registry state
is cleared, preserved, or reinitialized.
Rationale: Tests, tools, and modular application bootstraps must not inherit stale datasource registrations
accidentally.
Source scope: OADataSourceService, OARuntime reset/shutdown/test helpers.
Related CODEX findings: DATASOURCE_REGISTRY_HAS_EXPLICIT_RUNTIME_LIFECYCLE.
Suggested unit tests: testRuntimeResetClearsDatasourceRegistryWhenConfigured(),
testDatasourceRegistryPersistenceAcrossResetIsExplicitIfAllowed()
Spec target section: Runtime / Datasource Lifecycle

RT-THREAD-001 — OAThreadService Owns Runtime Thread Creation Semantics
Contract statement: Runtime-managed thread creation and thread behavior must be centralized through OAThreadService,
including baseline runtime context expectations.
Rationale: OA thread behavior affects ThreadLocal baseline, sync-send state, remote-thread handling, context
propagation, and transaction assumptions.
Source scope: OAThreadService, OAThread, OARuntime.thread().
Related CODEX findings: Runtime thread package review; UI thread provider optionality.
Suggested unit tests: testOAThreadServiceCreatesRuntimeAwareThread(),
testRuntimeThreadHasExpectedThreadLocalBaseline(), testDefaultCoreRuntimeHasOptionalUiThreadProvider()
Spec target section: Runtime / Thread Ownership

RT-THREAD-002 — Thread Classes Must Not Duplicate ThreadLocal Authority
Contract statement: Thread services and thread classes may initialize or reset runtime context, but authoritative
mutable ThreadLocal state belongs to OAThreadLocalService.
Rationale: Duplicate ThreadLocal authority creates inconsistent cleanup and sync/context behavior.
Source scope: OAThreadService, OAThreadLocalService, OARemoteThreadService, OAThread, OARemoteThread.
Related CODEX findings: Remote-thread reset is not cleanup substitute; THREAD_LOCAL_STATE_MUTATED_ONLY_BY_SERVICE.
Suggested unit tests: testThreadServiceUsesThreadLocalServiceForFlags(),
testThreadCreationDoesNotBypassThreadLocalService(), testRuntimeThreadFlagsMutatedOnlyThroughService()
Spec target section: Runtime / Thread Service Boundary

RT-THREADLOCAL-001 — ThreadLocal Baseline State Is Deterministic
Contract statement: A new runtime thread/request must start with a defined baseline for loading, refreshing,
deleting, saving, sendSyncMessages, admin, context, transaction, sibling, undo, and remote request state.
Rationale: Undefined or leaked flags can suppress events, sync, loads, validation, undo, or access checks.
Source scope: OAThreadLocalService, OAThread, OARemoteThread, OARemoteThreadService.
Related CODEX findings: Thread-local baseline and cleanup risks; remote request info canonical source.
Suggested unit tests: testNewRuntimeThreadHasDefaultThreadLocalState(),
testRemoteThreadResetEstablishesBaselineState(), testRemoteRequestInfoHasSingleCanonicalSource()
Spec target section: Runtime / ThreadLocal Baseline

RT-THREADLOCAL-002 — ThreadLocal State Is Mutated Only Through Runtime Service APIs
Contract statement: Runtime ThreadLocal flags, counters, scoped state, sibling helpers, undo scopes, request info,
and sync-send state must be mutated through OAThreadLocalService or documented runtime APIs.
Rationale: Centralized mutation is required to keep scoped counters, cleanup, diagnostics, and nested behavior
consistent.
Source scope: OAThreadLocalService, callers in graph/object/hub/sync/remote runtime paths.
Related CODEX findings: THREAD_LOCAL_STATE_MUTATED_ONLY_BY_SERVICE;
SIBLING_HELPER_GLOBAL_COUNT_EQUALS_REGISTERED_HELPERS; REMOTE_REQUEST_INFO_HAS_SINGLE_CANONICAL_SOURCE.
Suggested unit tests: testThreadLocalCountersRemainConsistentThroughServiceApis(),
testSiblingHelperCountMatchesRegisteredHelpers(), testRemoteRequestInfoSameThroughAllRuntimeApis()
Spec target section: Runtime / ThreadLocal Authority

RT-THREADLOCAL-003 — Code That Sets ThreadLocal State Must Restore It
Contract statement: Any runtime or graph code that changes OAThreadLocal state must restore the previous state using
try/finally unless ownership is explicitly transferred.
Rationale: Flag leaks create cross-operation corruption in graph, datasource, sync, event, remote, undo, and context
behavior.
Source scope: OAThreadLocalService; runtime/graph/object/hub/sync/remote services that set flags.
Related CODEX findings: Balanced cleanup expectation; sendSync/admin/loading restoration risks.
Suggested unit tests: testSendSyncMessagesRestoredAfterException(), testAdminFlagRestoredAfterException(),
testLoadingFlagRestoredAfterException()
Spec target section: Runtime / ThreadLocal Cleanup

RT-THREADLOCAL-004 — Scoped ThreadLocal Counters Must Be Balanced And Non-Negative
Contract statement: Scoped ThreadLocal counters and nested enter/exit state must support balanced nesting and must
reject, ignore, or visibly report extra exits without corrupting future scope state.
Rationale: Counter underflow can permanently alter runtime behavior across later operations.
Source scope: OAThreadLocalService scoped counters for loading, events, sibling helpers, server-only scope, undoable
scope, and related flags.
Related CODEX findings: THREAD_LOCAL_SCOPED_COUNTERS_NEVER_NEGATIVE;
SERVER_ONLY_SCOPE_BALANCED_AND_RESTORES_SEND_SYNC; UNDOABLE_SCOPE_IS_BALANCED_AND_DEPTH_SAFE.
Suggested unit tests: testScopedCountersNeverBecomeNegative(), testNestedServerOnlyScopeRestoresSendSync(),
testNestedUndoableScopeRestoresOriginalState()
Spec target section: Runtime / Scoped ThreadLocal Semantics

RT-THREADLOCAL-005 — sendSyncMessages Is The Authoritative Sync-Send Flag
Contract statement: Sync message emission must be controlled by OAThreadLocalService sendSyncMessages semantics,
with nested suppression/restoration handled consistently.
Rationale: Sync and replication correctness depends on predictable suppression and restoration.
Source scope: OAThreadLocalService, graph sync-dependent mutation services.
Related CODEX findings: Sync-send behavior and ThreadLocal restoration risks.
Suggested unit tests: testSendSyncMessagesFalseSuppressesSyncMessage(),
testSendSyncMessagesRestoresPreviousValueAfterNestedScope(), testServerOnlyScopeRestoresSendSync()
Spec target section: Runtime / Sync ThreadLocal Semantics

RT-REMOTE-001 — RemoteThread Reset Establishes Baseline, Not Cleanup Substitute
Contract statement: OARemoteThread.reset may establish request baseline and detect/report leaked state, but callers
that set runtime state remain responsible for balanced cleanup.
Rationale: Remote thread reuse must be safe without hiding cleanup bugs.
Source scope: OARemoteThread, OARemoteThreadService, OAThreadLocalService.
Related CODEX findings: Remote-thread reset clarification; remote-thread reuse/progression risks.
Suggested unit tests: testRemoteThreadResetRestoresBaselineBeforeRequest(),
testRemoteThreadResetReportsLeakedStateWhenConfigured(), testRemoteThreadResetDoesNotMaskMissingFinallyCleanup()
Spec target section: Runtime / Remote Thread Reuse

RT-REMOTE-002 — RemoteThread Reuse Must Not Carry Request Context Forward
Contract statement: A reused remote thread must not retain prior request context, admin flags, transaction state,
sync-send state, sibling/undo state, graph context, or remote request info.
Rationale: Remote/server operations must be isolated per request and user/session context.
Source scope: OARemoteThread, OARemoteThreadService, OAThreadLocalService.
Related CODEX findings: Remote-thread request isolation and request-info canonical-source findings.
Suggested unit tests: testRemoteThreadDoesNotLeakAdminFlagAcrossRequests(),
testRemoteThreadDoesNotLeakTransactionAcrossRequests(), testRemoteThreadDoesNotLeakContextAcrossRequests()
Spec target section: Runtime / Remote Request Isolation

RT-CONTEXT-001 — Runtime Context Is Thread-Scoped Unless Explicitly Propagated
Contract statement: Context, user, access, transaction, and graph state must be scoped to the current thread/request
unless an explicit runtime propagation mechanism is used.
Rationale: Generated apps and server requests require isolation between users, operations, and graph contexts.
Source scope: OAThreadLocalService, graph.context.OAContext, OAUserAccess.
Related CODEX findings: Context propagation risks reviewed in graph context pass.
Suggested unit tests: testContextDoesNotLeakAcrossThreads(),
testExplicitContextPropagationCopiesExpectedStateOnly(), testGraphContextNotInheritedWithoutContract()
Spec target section: Runtime / Context Propagation

RT-ADMIN-001 — Admin And System Flags Are Scoped And Restored
Contract statement: Admin/system execution flags must be temporary, thread-scoped, nest-safe, and restored after
privileged operations.
Rationale: Leaked admin state can bypass access, validation, sync, or verification rules.
Source scope: OAThreadLocalService, graph context/access services.
Related CODEX findings: Thread-local restoration risks; admin flag restoration.
Suggested unit tests: testAdminFlagScopedToOperation(), testAdminFlagRestoredAfterException(),
testNestedAdminScopesRestorePriorValue()
Spec target section: Runtime / Admin Context

RT-SYNC-001 — Sync Roles Are Distinct Runtime Semantics
Contract statement: isServer means actual sync server, isClient means actual sync client, and isSingleUser means
standalone local runtime; these roles must not be treated as synonyms.
Rationale: Old server-equals-not-client assumptions can break single-user datasource/cache/load behavior.
Source scope: Runtime sync role accessors, graph sync role services, OASyncService integration.
Related CODEX findings: OASync role-semantics review; graph role fixes.
Suggested unit tests: testServerRoleIsActualServerOnly(), testSingleUserIsNotServerAndNotClient(),
testClientRoleIsActualClientOnly()
Spec target section: Runtime / Sync Role Semantics

RT-SYNC-002 — Local Runtime Behavior Uses Explicit Non-Client Semantics
Contract statement: Code requiring local datasource, cache, load, save, or runtime behavior must use explicit local/
server-or-single-user logic, not accidental server-only checks.
Rationale: Single-user mode must not skip local persistence, loading, or cache behavior.
Source scope: Runtime sync role checks, graph services using role checks.
Related CODEX findings: Single-user role semantic bugs found/fixed/commented during graph review.
Suggested unit tests: testSingleUserUsesLocalDatasourcePath(), testSingleUserDoesNotAttemptClientServerRouting(),
testServerOnlyPathDoesNotRunInSingleUser()
Spec target section: Runtime / SingleUser Semantics

RT-INIT-001 — Partial Runtime Initialization Failure Is Visible
Contract statement: If runtime, graph, or service initialization fails, callers must not receive a silently usable
but partially initialized runtime/graph/service.
Rationale: Partial initialization can corrupt global services, graph ownership, and retry behavior.
Source scope: OARuntime constructors/init paths, graph creation paths, service construction and registration.
Related CODEX findings: Runtime initialization/retry risks; failed graph creation fallback; graph init failure cache
lifecycle.
Suggested unit tests: testFailedGraphInitializationIsVisibleToCaller(),
testFailedRuntimeInitializationDoesNotPublishBrokenGraph(),
testFailedServiceInitializationDoesNotPublishReadyRuntime()
Spec target section: Runtime / Initialization Failure

RT-RETRY-001 — Retry After Initialization Failure Is Defined
Contract statement: After failed runtime, graph, or service initialization, retry must either create clean state or
explicitly reject retry with a clear error.
Rationale: Tests, tools, and application bootstraps need deterministic recovery after startup failure.
Source scope: OARuntime graph creation/lookup, graph helper cache, runtime service lifecycle.
Related CODEX findings: GRAPH_INIT_FAILURE_CACHE_HAS_EXPLICIT_LIFECYCLE;
RUNTIME_TEST_RESET_RESTORES_CORE_SINGLETON_STATE.
Suggested unit tests: testGraphCreationRetryAfterFailureUsesCleanState(),
testRuntimeRejectsRetryAfterTerminalInitializationFailure(), testRuntimeTestResetClearsFailureStateByContract()
Spec target section: Runtime / Retry Semantics

RT-FAILURE-001 — Runtime Services Must Not Convert Failure Into Silent Success
Contract statement: Runtime service operations that cannot satisfy their contract must return a defined failure
result or throw; they must not silently no-op when the caller expects work.
Rationale: Silent runtime failure causes false success in graph, datasource, sync, thread, and context behavior.
Source scope: OARuntime service accessors, OADataSourceService, OAThreadService, OARemoteThreadService,
OAThreadLocalService.
Related CODEX findings: Silent false-success review priority; failed graph creation fallback.
Suggested unit tests: testMissingRequiredServiceFailsClearly(),
testDatasourceLookupNoMatchReturnsDefinedNullResultOnly(), testFailedGraphLookupDoesNotReturnMisleadingDefault()
Spec target section: Runtime / Failure Semantics

RT-SHUTDOWN-001 — Runtime Reset And Cleanup Have Explicit Boundaries
Contract statement: Runtime shutdown/reset/test cleanup must define behavior for graphs, graph helper caches,
datasource registry, thread services, remote-thread baselines, ThreadLocal state, and known runtime singletons.
Rationale: Long-running tools and test suites must not inherit stale runtime state.
Source scope: OARuntime lifecycle/reset hooks, OADataSourceService, OAThreadLocalService, OARemoteThreadService.
Related CODEX findings: RUNTIME_TEST_RESET_RESTORES_CORE_SINGLETON_STATE; datasource registry lifecycle; graph
helper cache lifecycle.
Suggested unit tests: testRuntimeResetClearsOrPreservesDatasourceRegistryByContract(),
testRuntimeResetClearsGraphRegistryByContract(), testRuntimeResetDoesNotLeaveThreadLocalStateDirty()
Spec target section: Runtime / Shutdown Cleanup

RT-STATIC-001 — Hidden Static State Has Explicit Runtime Boundary Semantics
Contract statement: Static runtime state must be immutable, process-global by design, or resettable/test-isolated by
explicit runtime lifecycle.
Rationale: OA tools, generated apps, tests, and multiple graph contexts need predictable global state boundaries.
Source scope: OARuntime static accessors/state, graph helper caches, runtime singleton services, thread-local/static
helpers.
Related CODEX findings: Hidden global/static state drift; test reset restores core singleton state.
Suggested unit tests: testStaticRuntimeStateDoesNotLeakAcrossRuntimeReset(),
testProcessGlobalStateIsExplicitlyPreservedWhenContractRequires(), testGraphHelperCacheResetBehaviorByContract()
Spec target section: Runtime / Static State Boundaries

RT-CONCURRENT-001 — Shared Runtime State Is Safely Published And Mutated
Contract statement: Runtime-owned graph registries, datasource registries, service instances, ThreadLocal service
state, and remote/thread services must be thread-safe, safely published, or explicitly confined.
Rationale: OA runtime services are used by UI, background, datasource, remote, sync, trigger, and tooling flows.
Source scope: OARuntime, OADataSourceService, OAThreadLocalService, OAThreadService, OARemoteThreadService.
Related CODEX findings: Datasource registry order concurrency; runtime service singleton state; scoped counter
consistency.
Suggested unit tests: testConcurrentGraphLookupReturnsSingleGraph(),
testConcurrentDatasourceRegistryMutationDeterministic(), testConcurrentThreadLocalScopesRemainBalanced()
Spec target section: Runtime / Concurrency Semantics

RT-DETERMINISM-001 — Runtime Lookup And Service Behavior Are Deterministic
Contract statement: For the same runtime state, graph registry, datasource registry, role state, ThreadLocal state,
and inputs, runtime lookups and service operations must produce the same observable results.
Rationale: Deterministic runtime behavior is required for graph ownership, persistence routing, sync, testing,
debugging, and generated application behavior.
Source scope: OARuntime, OADataSourceService, OAThreadLocalService, OAThreadService, OARemoteThreadService.
Related CODEX findings: Package-wide graph lookup, datasource order, ThreadLocal counter, and reset lifecycle
findings.
Suggested unit tests: testSameGraphLookupInputsProduceSameGraph(),
testSameDatasourceRegistryStateProducesSameLookup(), testSameThreadLocalScopeSequenceProducesSameFinalState()
Spec target section: Runtime / Deterministic Runtime Semantics

*/
