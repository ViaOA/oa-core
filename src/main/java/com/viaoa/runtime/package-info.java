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

/* CODEX Invariants

1. Runtime Lifecycle Contracts

  RT-LIFECYCLE-001 — OARuntime Is The Runtime Entry Point

  Contract statement:
  All runtime-wide access to graph, datasource, thread, thread-local, remote-thread, and sync-related services must
  go through OARuntime.

  Rationale:
  OA 4.0 replaces scattered static delegate assumptions with a runtime/service model. A single entry point keeps
  lifecycle and ownership testable.

  Source locations:
  OARuntime
  OADataSourceService
  OAThreadService
  OAThreadLocalService
  OARemoteThreadService

  Known related CODEX findings:
  Runtime lifecycle/reset risks were previously noted.

  Suggested unit tests:
  testOARuntimeExposesCoreRuntimeServices()
  testRuntimeServicesAreStableAcrossRepeatedAccess()

  Spec target section:
  Runtime / Entry Point Semantics

  RT-LIFECYCLE-002 — Runtime Service Instances Are Owned By Runtime

  Contract statement:
  Runtime services must be created, owned, and reused by the active OARuntime instance/context.

  Rationale:
  Graph lookup, datasource routing, and thread-local behavior must not drift across unrelated static instances.

  Source locations:
  OARuntime service fields/accessors
  OADataSourceService
  OAThreadService
  OAThreadLocalService

  Known related CODEX findings:
  Hidden global/static state and reset risks noted.

  Suggested unit tests:
  testRuntimeOwnsSingleDatasourceServiceInstance()
  testRuntimeOwnsSingleThreadLocalServiceInstance()

  Spec target section:
  Runtime / Service Lifecycle

  2. Default Graph / Graph Lookup Contracts

  RT-GRAPH-001 — Default Graph Is Stable Within Runtime

  Contract statement:
  The default graph must be lazily created once and reused for the lifetime of the runtime unless an explicit
  runtime reset/lifecycle operation replaces it.

  Rationale:
  Default graph instability would corrupt object identity, datasource ownership, Hub state, and sync routing.

  Source locations:
  OARuntime.graph()
  OARuntime graph fields/lookup methods
  OAGraphImpl

  Known related CODEX findings:
  Default graph creation/lifecycle risks were reviewed.

  Suggested unit tests:
  testDefaultGraphIsSameInstanceAcrossCalls()
  testDefaultGraphRecreatedOnlyAfterExplicitRuntimeReset()

  Spec target section:
  Runtime / Default Graph Semantics

  RT-GRAPH-002 — Graph Lookup By Class/Object/Package Must Agree

  Contract statement:
  Graph lookup by model class, OAObject instance, package name, or graph name must resolve to the same intended
  graph for that model package.

  Rationale:
  Object services, Hub services, datasource routing, sync, and metadata lookup all assume consistent graph
  ownership.

  Source locations:
  OARuntime.graph(Class<?>)
  OARuntime.graph(Object)
  OARuntime.graph(String)
  OAGraphImpl

  Known related CODEX findings:
  Graph routing and package/name assumptions noted.

  Suggested unit tests:
  testGraphLookupByClassAndPackageMatch()
  testGraphLookupByObjectUsesObjectClassGraph()
  testUnknownPackageUsesDefinedFallbackBehavior()

  Spec target section:
  Runtime / Graph Lookup Semantics

  RT-GRAPH-003 — Graph Creation Must Not Silently Split A Package

  Contract statement:
  Runtime must not create multiple independent graphs for the same package/name unless explicitly requested and
  documented.

  Rationale:
  Two graphs for the same model package would split object identity and runtime services.

  Source locations:
  OARuntime graph registry/lookup
  OAGraphImpl

  Known related CODEX findings:
  none observed beyond lifecycle/routing notes.

  Suggested unit tests:
  testRepeatedPackageLookupDoesNotCreateDuplicateGraph()
  testExplicitNamedGraphCreationDoesNotHijackDefaultGraph()

  Spec target section:
  Runtime / Graph Registry

  3. Runtime Service Ownership Contracts

  RT-SERVICE-001 — Runtime Services Are Shared Kernel Services

  Contract statement:
  Datasource, thread, thread-local, and remote-thread services are runtime kernel services and must not depend on
  UI, JDBC, Jackson, XML, web, or external modules.

  Rationale:
  oa-core is the zero-external-dependency runtime kernel.

  Source locations:
  com.viaoa.runtime.*
  com.viaoa.runtime.thread.*

  Known related CODEX findings:
  Boundary scans found runtime structurally clean.

  Suggested unit tests:
  testRuntimePackageHasNoForbiddenModuleReferences()
  testRuntimeServicesInstantiateWithoutExternalModules()

  Spec target section:
  Runtime / Core Boundary

  RT-SERVICE-002 — Runtime Services Must Not Create Cyclic Ownership With Graph Services

  Contract statement:
  Runtime may own graph and runtime services, but graph services must not redefine runtime ownership or create
  alternate runtime roots.

  Rationale:
  Avoids split lifecycle and hidden service duplication.

  Source locations:
  OARuntime
  OAGraphImpl
  runtime service accessors

  Known related CODEX findings:
  Graph/runtime ownership risks reviewed.

  Suggested unit tests:
  testGraphUsesRuntimeServicesFromOwningRuntime()
  testRuntimeAndGraphDoNotCreateAlternateServiceInstances()

  Spec target section:
  Runtime / Service Ownership

  4. Datasource Service Contracts

  RT-DATASOURCE-001 — Datasource Registry Is Runtime-Owned

  Contract statement:
  Datasource registration, unregistration, ordering, and lookup belong to OADataSourceService under OARuntime.

  Rationale:
  Persistence routing must be runtime-wide and deterministic.

  Source locations:
  OADataSourceService.register(...)
  OADataSourceService.unregister(...)
  OADataSourceService.get(...)
  OADataSourceService.setPosition(...)

  Known related CODEX findings:
  Registry order concurrency and lifecycle/reset risks are CODEX-commented.

  Suggested unit tests:
  testDatasourceRegisterMakesDatasourceDiscoverable()
  testDatasourceUnregisterRemovesDatasource()
  testDatasourceSetPositionChangesRoutingOrder()

  Spec target section:
  Runtime / Datasource Registry

  RT-DATASOURCE-002 — Datasource Lookup Must Respect Enabled And Last Semantics

  Contract statement:
  Datasource lookup must skip disabled datasources and use getLast() datasources only as fallback behind non-last
  supported datasources.

  Rationale:
  Object-cache, autonumber, client, and real storage datasources rely on deterministic priority.

  Source locations:
  OADataSourceService.get(Class, OAFilter)
  OADataSource.getEnabled()
  OADataSource.getLast()

  Known related CODEX findings:
  none observed beyond datasource invariant extraction.

  Suggested unit tests:
  testDisabledDatasourceSkipped()
  testLastDatasourceUsedOnlyAsFallback()
  testNonLastDatasourceBeatsLastDatasource()

  Spec target section:
  Runtime / Datasource Routing

  RT-DATASOURCE-003 — Datasource Registry Lifecycle Must Be Explicit

  Contract statement:
  Runtime must define whether datasource registry survives reset/test lifecycle or is cleared explicitly.

  Rationale:
  Unit tests and modular app bootstraps must not inherit stale datasource registrations.

  Source locations:
  OADataSourceService
  OARuntime lifecycle/reset hooks

  Known related CODEX findings:
  Datasource registry lacks explicit lifecycle/reset API; CODEX-commented in runtime.

  Suggested unit tests:
  testRuntimeResetClearsDatasourceRegistryWhenConfigured()
  testDatasourceRegistryPersistenceAcrossResetIsExplicitIfAllowed()

  Spec target section:
  Runtime / Datasource Lifecycle

  5. Thread Service Contracts

  RT-THREAD-001 — OAThreadService Owns Runtime Thread Creation Semantics

  Contract statement:
  Runtime-managed thread creation and thread behavior must be centralized through OAThreadService.

  Rationale:
  OA thread behavior affects thread-local baseline, sync-send state, remote-thread handling, and context
  propagation.

  Source locations:
  OAThreadService
  OAThread
  OARuntime.thread()

  Known related CODEX findings:
  Thread package review covered OAThread/OARemoteThread issues.

  Suggested unit tests:
  testOAThreadServiceCreatesRuntimeAwareThread()
  testRuntimeThreadHasExpectedThreadLocalBaseline()

  Spec target section:
  Runtime / Thread Ownership

  RT-THREAD-002 — Runtime Thread Behavior Must Not Duplicate ThreadLocal Authority

  Contract statement:
  Thread classes may initialize or reset runtime context, but authoritative thread-local state belongs to
  OAThreadLocalService.

  Rationale:
  Avoids duplicate flags and inconsistent cleanup.

  Source locations:
  OAThreadService
  OAThreadLocalService
  OARemoteThreadService

  Known related CODEX findings:
  Remote-thread reset is not a substitute for balanced cleanup.

  Suggested unit tests:
  testThreadServiceUsesThreadLocalServiceForFlags()
  testThreadCreationDoesNotBypassThreadLocalService()

  Spec target section:
  Runtime / Thread Service Boundary

  6. OAThreadLocal Contracts

  RT-THREADLOCAL-001 — ThreadLocal Baseline State Must Be Deterministic

  Contract statement:
  A new OA runtime thread/request must start with a defined baseline for loading, deleting, saving,
  sendSyncMessages, admin, context, and transaction flags.

  Rationale:
  Leaked or undefined flags can suppress events, sync, loads, or verification.

  Source locations:
  OAThreadLocalService
  OAThread
  OARemoteThread

  Known related CODEX findings:
  Thread-local baseline and cleanup risks were reviewed.

  Suggested unit tests:
  testNewRuntimeThreadHasDefaultThreadLocalState()
  testRemoteThreadResetEstablishesBaselineState()

  Spec target section:
  Runtime / ThreadLocal Baseline

  RT-THREADLOCAL-002 — Code That Sets ThreadLocal State Must Restore It

  Contract statement:
  Any runtime or graph code that changes OAThreadLocal state must restore the previous state using try/finally.

  Rationale:
  Flag leaks create cross-operation corruption.

  Source locations:
  OAThreadLocalService
  graph/object/hub services that set flags
  OARemoteThreadService

  Known related CODEX findings:
  Balanced cleanup expectation accepted; reset is diagnostic/baseline, not cleanup substitute.

  Suggested unit tests:
  testSendSyncMessagesRestoredAfterException()
  testAdminFlagRestoredAfterException()
  testLoadingFlagRestoredAfterException()

  Spec target section:
  Runtime / ThreadLocal Cleanup

  RT-THREADLOCAL-003 — sendSyncMessages Is The Authoritative Sync-Send Flag

  Contract statement:
  Sync message emission must be controlled by OAThreadLocalService.sendSyncMessages semantics, not duplicated flags.

  Rationale:
  Sync/replication correctness depends on predictable suppression and restoration.

  Source locations:
  OAThreadLocalService
  OASyncService
  graph object/hub mutation services

  Known related CODEX findings:
  Sync-send behavior and thread-local restoration risks reviewed.

  Suggested unit tests:
  testSendSyncMessagesFalseSuppressesSyncMessage()
  testSendSyncMessagesRestoresPreviousValueAfterNestedScope()

  Spec target section:
  Runtime / Sync ThreadLocal Semantics

  7. OARemoteThread Contracts

  RT-REMOTE-001 — RemoteThread Reset Establishes Baseline, Not Cleanup Substitute

  Contract statement:
  OARemoteThread.reset() may establish request baseline and detect/report leaked state, but callers that set state
  remain responsible for balanced cleanup.

  Rationale:
  Remote thread reuse must be safe without hiding bugs.

  Source locations:
  OARemoteThread
  OARemoteThreadService
  OAThreadLocalService

  Known related CODEX findings:
  Clarified and accepted during runtime review.

  Suggested unit tests:
  testRemoteThreadResetRestoresBaselineBeforeRequest()
  testRemoteThreadResetReportsLeakedStateWhenConfigured()

  Spec target section:
  Runtime / Remote Thread Reuse

  RT-REMOTE-002 — RemoteThread Reuse Must Not Carry Request Context Forward

  Contract statement:
  A reused remote thread must not retain prior request context, admin flags, transaction, sync-send state, or graph
  context.

  Rationale:
  Remote/server operations must be isolated per request.

  Source locations:
  OARemoteThread
  OARemoteThreadService
  OAThreadLocalService

  Known related CODEX findings:
  Remote-thread reuse/progression risks reviewed.

  Suggested unit tests:
  testRemoteThreadDoesNotLeakAdminFlagAcrossRequests()
  testRemoteThreadDoesNotLeakTransactionAcrossRequests()
  testRemoteThreadDoesNotLeakContextAcrossRequests()

  Spec target section:
  Runtime / Remote Request Isolation

  8. Context / Admin Contracts

  RT-CONTEXT-001 — Runtime Context Is Thread-Scoped Unless Explicitly Propagated

  Contract statement:
  Context/user/access state must be scoped to the current thread/request unless an explicit runtime propagation
  mechanism is used.

  Rationale:
  Generated apps and server requests require isolation between users and operations.

  Source locations:
  OAThreadLocalService
  graph.context.OAContext
  OAUserAccess

  Known related CODEX findings:
  Context propagation risks reviewed in graph context pass.

  Suggested unit tests:
  testContextDoesNotLeakAcrossThreads()
  testExplicitContextPropagationCopiesExpectedStateOnly()

  Spec target section:
  Runtime / Context Propagation

  RT-ADMIN-001 — Admin/System Flags Must Be Scoped And Restored

  Contract statement:
  Admin/system execution flags must be temporary, thread-scoped, and restored after privileged operations.

  Rationale:
  Leaked admin state can bypass access or verification rules.

  Source locations:
  OAThreadLocalService
  graph context/access services

  Known related CODEX findings:
  Thread-local restoration risks cover this.

  Suggested unit tests:
  testAdminFlagScopedToOperation()
  testAdminFlagRestoredAfterException()

  Spec target section:
  Runtime / Admin Context

  9. Sync Role Contracts

  RT-SYNC-001 — Sync Roles Are Mutually Semantic, Not Synonyms

  Contract statement:
  isServer() means actual sync server, isClient() means actual sync client, and isSingleUser() means standalone
  local runtime.

  Rationale:
  Old “server == not client” assumptions can break SingleUser behavior.

  Source locations:
  OASyncService
  runtime/graph sync role accessors
  sync-dependent graph services

  Known related CODEX findings:
  OASync role-semantics review converged.

  Suggested unit tests:
  testServerRoleIsActualServerOnly()
  testSingleUserIsNotServerAndNotClient()
  testClientRoleIsActualClientOnly()

  Spec target section:
  Runtime / Sync Role Semantics

  RT-SYNC-002 — Local Runtime Behavior Uses Not-Client Semantics When Intended

  Contract statement:
  Code requiring local datasource/cache/runtime behavior must use !isClient() or explicit server-or-single-user
  logic.

  Rationale:
  SingleUser must not skip local persistence, loading, or cache behavior.

  Source locations:
  runtime sync role checks
  graph services using role checks

  Known related CODEX findings:
  Role semantic bugs found/fixed/commented during graph review.

  Suggested unit tests:
  testSingleUserUsesLocalDatasourcePath()
  testSingleUserDoesNotAttemptClientServerRouting()

  Spec target section:
  Runtime / SingleUser Semantics

  10. Failure / Initialization / Retry Contracts

  RT-INIT-001 — Partial Runtime Initialization Failure Must Be Detectable

  Contract statement:
  If runtime/graph/service initialization fails, callers must not receive a silently usable but partially
  initialized runtime.

  Rationale:
  Partial initialization can corrupt global services and make retries unreliable.

  Source locations:
  OARuntime constructors/init paths
  graph creation paths
  service construction

  Known related CODEX findings:
  Runtime initialization/retry risks reviewed; one accepted/deferred item noted by user.

  Suggested unit tests:
  testFailedGraphInitializationIsVisibleToCaller()
  testFailedRuntimeInitializationDoesNotPublishBrokenGraph()

  Spec target section:
  Runtime / Initialization Failure

  RT-RETRY-001 — Retry After Initialization Failure Must Be Defined

  Contract statement:
  After failed runtime or graph initialization, retry must either create a clean runtime/graph or explicitly reject
  retry with a clear error.

  Rationale:
  Tests, tools, and application bootstraps need deterministic recovery.

  Source locations:
  OARuntime graph creation/lookup
  runtime service lifecycle

  Known related CODEX findings:
  Retry after initialization failure was reviewed.

  Suggested unit tests:
  testGraphCreationRetryAfterFailureUsesCleanState()
  testRuntimeRejectsRetryAfterTerminalInitializationFailure()

  Spec target section:
  Runtime / Retry Semantics

  RT-FAILURE-001 — Runtime Must Not Convert Service Failure Into Silent Success

  Contract statement:
  Runtime service operations that fail must return a clear failure result or throw; they must not silently no-op
  when the caller expects work to occur.

  Rationale:
  Silent failure causes false success in graph, datasource, sync, and thread behavior.

  Source locations:
  OARuntime service accessors
  OADataSourceService
  OAThreadService
  OARemoteThreadService

  Known related CODEX findings:
  Silent false-success was a review priority; no remaining concrete runtime closure bugs.

  Suggested unit tests:
  testMissingRequiredServiceFailsClearly()
  testDatasourceLookupNoMatchReturnsDefinedNullResultOnly()

  Spec target section:
  Runtime / Failure Semantics

  11. Shutdown / Cleanup Contracts

  RT-SHUTDOWN-001 — Runtime Cleanup Must Release Or Reset Runtime-Owned Services

  Contract statement:
  Runtime shutdown/reset must define cleanup behavior for graphs, datasource registry, thread services, remote-
  thread baselines, and thread-local state.

  Rationale:
  Long-running tools and test suites must not inherit stale runtime state.

  Source locations:
  OARuntime lifecycle/reset hooks
  OADataSourceService
  thread services

  Known related CODEX findings:
  Runtime reset/datasource lifecycle risks noted.

  Suggested unit tests:
  testRuntimeResetClearsOrPreservesDatasourceRegistryByContract()
  testRuntimeResetClearsGraphRegistryByContract()
  testRuntimeResetDoesNotLeaveThreadLocalStateDirty()

  Spec target section:
  Runtime / Shutdown Cleanup

  RT-STATIC-001 — Hidden Static State Must Have Runtime Boundary Semantics

  Contract statement:
  Any static runtime state must be either immutable, process-global by design, or resettable/test-isolated by
  explicit runtime lifecycle.

  Rationale:
  OA tools, generated apps, tests, and multiple graph contexts need predictable global state.

  Source locations:
  OARuntime static accessors/state
  OADataSourceService via runtime singleton
  thread-local/static helpers

  Known related CODEX findings:
  Hidden global/static state drift was reviewed.

  Suggested unit tests:
  testStaticRuntimeStateDoesNotLeakAcrossRuntimeReset()
  testProcessGlobalStateIsExplicitlyPreservedWhenContractRequires()

  Spec target section:
  Runtime / Static State Boundaries

  12. Test Coverage Matrix

  Runtime lifecycle:

  - testOARuntimeExposesCoreRuntimeServices
  - testRuntimeServicesAreStableAcrossRepeatedAccess
  - testRuntimeOwnsSingleDatasourceServiceInstance
  - testRuntimeOwnsSingleThreadLocalServiceInstance

  Graph lookup:

  - testDefaultGraphIsSameInstanceAcrossCalls
  - testDefaultGraphRecreatedOnlyAfterExplicitRuntimeReset
  - testGraphLookupByClassAndPackageMatch
  - testGraphLookupByObjectUsesObjectClassGraph
  - testRepeatedPackageLookupDoesNotCreateDuplicateGraph

  Service ownership:

  - testRuntimePackageHasNoForbiddenModuleReferences
  - testRuntimeServicesInstantiateWithoutExternalModules
  - testGraphUsesRuntimeServicesFromOwningRuntime

  Datasource service:

  - testDatasourceRegisterMakesDatasourceDiscoverable
  - testDatasourceUnregisterRemovesDatasource
  - testDatasourceSetPositionChangesRoutingOrder
  - testDisabledDatasourceSkipped
  - testLastDatasourceUsedOnlyAsFallback

  Thread service:

  - testOAThreadServiceCreatesRuntimeAwareThread
  - testRuntimeThreadHasExpectedThreadLocalBaseline
  - testThreadCreationDoesNotBypassThreadLocalService

  ThreadLocal:

  - testNewRuntimeThreadHasDefaultThreadLocalState
  - testSendSyncMessagesRestoredAfterException
  - testAdminFlagRestoredAfterException
  - testLoadingFlagRestoredAfterException
  - testSendSyncMessagesFalseSuppressesSyncMessage

  Remote thread:

  - testRemoteThreadResetRestoresBaselineBeforeRequest
  - testRemoteThreadDoesNotLeakAdminFlagAcrossRequests
  - testRemoteThreadDoesNotLeakTransactionAcrossRequests
  - testRemoteThreadDoesNotLeakContextAcrossRequests

  Context/admin:

  - testContextDoesNotLeakAcrossThreads
  - testExplicitContextPropagationCopiesExpectedStateOnly
  - testAdminFlagScopedToOperation

  Sync roles:

  - testServerRoleIsActualServerOnly
  - testSingleUserIsNotServerAndNotClient
  - testClientRoleIsActualClientOnly
  - testSingleUserUsesLocalDatasourcePath

  Failure/retry:

  - testFailedGraphInitializationIsVisibleToCaller
  - testFailedRuntimeInitializationDoesNotPublishBrokenGraph
  - testGraphCreationRetryAfterFailureUsesCleanState
  - testMissingRequiredServiceFailsClearly

  Shutdown/static state:

  - testRuntimeResetClearsOrPreservesDatasourceRegistryByContract
  - testRuntimeResetClearsGraphRegistryByContract
  - testRuntimeResetDoesNotLeaveThreadLocalStateDirty
  - testStaticRuntimeStateDoesNotLeakAcrossRuntimeReset


*/


/*qqqqqqqq other
CODEX

  3. Top Runtime Invariants Excluding Thread-Local Details
  - OARuntime is the only public runtime service entry point.
  - The default graph is a single instance for the lifetime of the runtime.
  - createGraph(pkg) is idempotent.
  - graph(pkg) never silently returns default after package graph creation has failed.
  - Graph lookup by class follows a documented class-canonicalization rule.
  - Runtime graph helper caches are invalidated on all graph lifecycle changes, including failures/resets.
  - Datasource registry order is deterministic.
  - Datasource registry lifecycle is explicit and test-resettable.
  - Runtime direct package has no JDBC/Jackson/REST/Web/vendor dependencies.
  - UI-thread detection remains a replaceable hook, not a hard runtime dependency.

  4. Runtime Test Plan

  - OARuntimeDefaultGraphTest: default graph singleton across graph(), defaultGraph(), graph(""), createGraph("").
  - OARuntimeGraphFailureTest: failed createGraph(pkg) cannot be bypassed by prior helper-cache fallback.
  - OARuntimeGraphRetryResetTest: graph exception cache behavior is either permanent by contract or resettable.
  - OARuntimeGraphCanonicalClassTest: direct OAObject class, subclass, proxy/helper subclass, and cross-package
    subclass route correctly.
  - OARuntimeGraphNullContractTest: all null overloads behave consistently.
  - OADataSourceServiceOrderTest: register order, getLast behavior, disabled datasource skipping, setPosition.
  - OADataSourceServiceConcurrencyTest: concurrent register/unregister/reorder does not corrupt deterministic
    registry state.
  - OADataSourceServiceLifecycleTest: runtime/test reset clears or preserves datasource registry by explicit
    contract.


*/
