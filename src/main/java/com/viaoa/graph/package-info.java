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


1. OG Runtime / Graph Ownership Contracts

  GRAPH-OWNERSHIP-001 — Every OAObject Belongs To A Runtime Graph Context

  Contract statement:
  Every OAObject operation that needs metadata, identity, datasource, sync, or Hub behavior must resolve through the
  owning OAGraph.

  Rationale:
  OA identity, lifecycle, save/delete, Hub membership, and sync behavior are graph-scoped. Bypassing graph ownership
  risks cross-runtime identity drift.

  Source locations:
  OAGraph, OAGraphImpl, OARuntime.graph(...), OAObjectService, HubService

  Known related CODEX findings:
  Prior graph routing and ownership findings mapped to this invariant.

  Suggested unit tests:
  testObjectOperationUsesOwningGraph()
  testCrossGraphObjectDoesNotUseWrongGraphServices()

  Spec target section:
  OG Runtime / Graph Ownership Semantics

  GRAPH-OWNERSHIP-002 — OAGraph Is Public Facade, OAGraphInternal Is Internal Runtime Surface

  Contract statement:
  Application code should use OAGraph; internal object/hub/runtime services may use OAGraphInternal.

  Rationale:
  Keeps public OG verbs stable while preserving internal service hooks.

  Source locations:
  OAGraph, OAGraphInternal, OAGraphImpl, com.viaoa.graph.api.internal.*

  Known related CODEX findings:
  Internal API leakage risks noted during facade review.

  Suggested unit tests:
  testPublicGraphApiDoesNotExposeInternalServices()
  testInternalServicesUseInternalGraphSurfaceOnly()

  Spec target section:
  OG Runtime / Public-Internal API Boundary

  2. Graph Routing / Default Graph Contracts

  GRAPH-ROUTE-001 — Class/Object/Package Routing Must Be Deterministic

  Contract statement:
  OARuntime.graph(class), OARuntime.graph(object), and OARuntime.graph(packageName) must consistently resolve the
  same intended graph for a model package.

  Rationale:
  Datasource routing, metadata lookup, sync package routing, and cache identity depend on stable graph resolution.

  Source locations:
  OARuntime, OAGraphImpl, OAGraphInternal

  Known related CODEX findings:
  Runtime graph creation/default graph findings covered earlier.

  Suggested unit tests:
  testGraphLookupByClassMatchesPackageGraph()
  testGraphLookupByObjectUsesObjectClassGraph()
  testDefaultGraphIsStableAcrossCalls()

  Spec target section:
  OG Runtime / Graph Routing

  GRAPH-ROUTE-002 — Default Graph Must Be Explicitly Stable

  Contract statement:
  The default graph must be created once per runtime context and reused until explicit runtime reset.

  Rationale:
  Accidental default graph churn breaks cache, datasource, metadata, and sync ownership.

  Source locations:
  OARuntime.graph(), OARuntime.graph(String), OAGraphImpl

  Known related CODEX findings:
  Runtime lifecycle/reset risks noted in runtime review.

  Suggested unit tests:
  testDefaultGraphCreatedOnce()
  testRuntimeResetCreatesFreshDefaultGraphWhenIntended()

  Spec target section:
  OG Runtime / Default Graph Lifecycle

  3. Service Ownership / Parent-Child Contracts

  GRAPH-SERVICE-001 — Parent Services Own Child Service Orchestration

  Contract statement:
  OAObjectService, HubService, OASyncService, OAReplicationService, and OATriggerService coordinate child services;
  graph facade should call parent services, not deep child services directly.

  Rationale:
  Preserves clear orchestration points and prevents sideways coupling.

  Source locations:
  com.viaoa.graph.service.*
  graph.service.object.*
  graph.service.hub.*

  Known related CODEX findings:
  Parent/child service boundary risks noted in package review.

  Suggested unit tests:
  testGraphFacadeDelegatesThroughParentServices()
  testChildServiceDoesNotBypassParentForCrossServiceOperation()

  Spec target section:
  OG Runtime / Service Ownership

  GRAPH-SERVICE-002 — Child Services Must Not Invent Runtime Authority

  Contract statement:
  Child services may execute their responsibility, but graph/runtime authority remains with parent services and
  OAGraphImpl.

  Rationale:
  Avoids fragmented lifecycle, sync, and datasource behavior.

  Source locations:
  OAObjectParentService, HubParentService, OAObjectService, HubService

  Known related CODEX findings:
  Sideways service coupling risks noted.

  Suggested unit tests:
  testChildServiceUsesParentHookForCrossServiceState()
  testServiceInitializationOrderIsDeterministic()

  Spec target section:
  OG Runtime / Service Layering

  4. Object Identity / Key / Lifecycle Contracts

  OBJ-IDENTITY-001 — One Graph Identity Per Persistent Key

  Contract statement:
  Within one graph, the same class/key pair must resolve to one authoritative OAObject instance.

  Rationale:
  Hub membership, links, serialization, sync, and replication all require identity stability.

  Source locations:
  OAObjectCacheService, OAObjectKeyService, OAObjectGuidService, OAObjectDSService

  Known related CODEX findings:
  Identity/cache drift findings from graph adversarial scans.

  Suggested unit tests:
  testSameKeyReturnsSameObjectIdentity()
  testDatasourceLoadMergesWithExistingCachedObject()

  Spec target section:
  OG Runtime / Object Identity

  OBJ-KEY-001 — Object Key Changes Must Preserve Cache Index Consistency

  Contract statement:
  When an object key changes, all graph cache indexes must be updated consistently or the change must be rejected.

  Rationale:
  A stale key index can make the same object unreachable or duplicated.

  Source locations:
  OAObjectKeyService, OAObjectCacheService, OAObjectPropertyService

  Known related CODEX findings:
  Key/cache drift risks noted in adversarial graph scans.

  Suggested unit tests:
  testKeyChangeUpdatesObjectCacheIndex()
  testRejectedKeyChangeLeavesCacheIndexUnchanged()

  Spec target section:
  OG Runtime / Key Semantics

  OBJ-LIFECYCLE-001 — New/Changed/Deleted Flags Must Reflect Completed Semantic State

  Contract statement:
  Lifecycle flags must not claim successful persistence, deletion, or load completion unless the authoritative
  operation completed.

  Rationale:
  False lifecycle state breaks retry, save/delete, replication, and UI state.

  Source locations:
  OAObjectLifecycleService, OAObjectDSService, OAObjectDeleteService, OAObjectSaveService

  Known related CODEX findings:
  Multiple lifecycle false-success bugs found and fixed/commented.

  Suggested unit tests:
  testFailedSaveDoesNotClearChangedFlagAsSuccess()
  testFailedDeleteDoesNotMarkDeletedAsCompleted()
  testSuccessfulInsertClearsNewFlagOnlyAfterDatasourceSuccess()

  Spec target section:
  OG Runtime / Object Lifecycle

  5. Object Save/Delete Contracts

  OBJ-SAVE-001 — Save Delegates To Authoritative Datasource Path

  Contract statement:
  Object save must route through the graph/object datasource service and selected datasource for the object class.

  Rationale:
  Save behavior must be consistent across SingleUser, Server, and Client modes.

  Source locations:
  OAObjectDSService, OAObjectSaveService, OADataSource

  Known related CODEX findings:
  Datasource routing and save/delete issues mapped here.

  Suggested unit tests:
  testObjectSaveUsesClassDatasource()
  testClientObjectSaveDelegatesThroughClientDatasource()
  testSingleUserObjectSaveUsesLocalDatasource()

  Spec target section:
  OG Runtime / Save Semantics

  OBJ-DELETE-001 — Delete Must Coordinate Object, Hub, Datasource, And Sync State

  Contract statement:
  Deleting an object must coordinate object lifecycle, Hub membership, datasource delete, cache removal, and sync/
  replication hooks according to graph role.

  Rationale:
  Partial delete coordination can leave stale Hub members, ghost cache entries, or replication divergence.

  Source locations:
  OAObjectDeleteService, HubDeleteService, OAObjectCacheService, OASyncService

  Known related CODEX findings:
  Delete lifecycle and Hub deleteAll issues found during graph scans.

  Suggested unit tests:
  testDeleteRemovesObjectFromOwningHubs()
  testDeleteRemovesObjectFromCacheAfterAuthoritativeCompletion()
  testDeleteSendsSyncOnlyWhenAuthoritativeDeleteCompletes()

  Spec target section:
  OG Runtime / Delete Semantics

  6. Hub Membership Contracts

  HUB-MEMBERSHIP-001 — Hub Membership Must Match Link/Ownership Semantics

  Contract statement:
  Adding/removing an object from a Hub must update the corresponding link/master/detail state when the Hub is link-
  bound.

  Rationale:
  Hubs are semantic graph collections, not plain lists.

  Source locations:
  HubAddRemoveService, HubLinkService, HubDetailService, HubDataService

  Known related CODEX findings:
  Hub membership corruption risks found/fixed in graph and hub scans.

  Suggested unit tests:
  testHubAddUpdatesReverseLinkWhenApplicable()
  testHubRemoveClearsLinkWhenApplicable()
  testDetailHubMembershipMatchesMasterLink()

  Spec target section:
  OG Runtime / Hub Membership

  HUB-MEMBERSHIP-002 — Hub Change Tracking Must Match Actual Membership Mutations

  Contract statement:
  Hub added/removed tracking must reflect completed membership changes and must not record false adds/removes.

  Rationale:
  SaveAll, deleteAll, sync, and UI state depend on accurate change lists.

  Source locations:
  HubDataService, HubAddRemoveService, HubSaveService, HubDeleteService

  Known related CODEX findings:
  Hub added/removed list correctness issues noted during scans.

  Suggested unit tests:
  testHubAddedListTracksSuccessfulAddOnly()
  testHubRemovedListTracksSuccessfulRemoveOnly()
  testFailedAddDoesNotRecordAddedObject()

  Spec target section:
  OG Runtime / Hub Change Tracking

  7. Hub Active Object / Detail / Link Contracts

  HUB-AO-001 — Active Object Changes Must Be Ordered And Observable

  Contract statement:
  Changing a Hub active object must update AO state before after-events, and listeners must observe the new
  authoritative AO.

  Rationale:
  Generated UI, detail Hubs, and property binding depend on AO correctness.

  Source locations:
  HubAOService, HubEventService, HubDetailService

  Known related CODEX findings:
  Event ordering risks noted in Hub scans.

  Suggested unit tests:
  testActiveObjectAfterEventSeesNewAO()
  testActiveObjectChangeUpdatesDetailHubBeforeEvent()

  Spec target section:
  OG Runtime / Active Object Semantics

  HUB-DETAIL-001 — Detail Hub Must Reflect Master Object And Link Path

  Contract statement:
  A detail Hub must load and expose objects that belong to its current master object and link path only.

  Rationale:
  Master/detail is core OA UI and model wiring behavior.

  Source locations:
  HubDetailService, HubLinkService, HubSelectService, OAObjectReflectService

  Known related CODEX findings:
  Lazy-load/detail scope bugs mapped here.

  Suggested unit tests:
  testDetailHubLoadsOnlyCurrentMasterChildren()
  testDetailHubRefreshesWhenMasterAOChanges()
  testDetailHubDoesNotExposePreviousMasterChildren()

  Spec target section:
  OG Runtime / Detail Hub Semantics

  8. Reference / Lazy Load Contracts

  REF-LAZY-001 — Lazy Reference Must Not Be Marked Loaded On Failed Load

  Contract statement:
  A reference or Hub must not be marked loaded/empty if the load failed or authoritative source was unavailable.

  Rationale:
  False loaded-empty state prevents retry and causes silent missing data.

  Source locations:
  OAObjectPropertyService, OAObjectReflectService, HubSelectService, HubDataService

  Known related CODEX findings:
  Lazy-load loaded/empty corruption bugs found in graph scans.

  Suggested unit tests:
  testFailedReferenceLoadDoesNotMarkLoaded()
  testFailedDetailHubLoadRemainsRetryable()
  testEmptyLoadedStateOnlyAfterSuccessfulAuthoritativeLoad()

  Spec target section:
  OG Runtime / Lazy Load Semantics

  REF-LAZY-002 — Unresolved Key References Must Remain Distinguishable From Null

  Contract statement:
  An unresolved object key/reference must not be collapsed into a real null reference unless the authoritative
  source confirms absence.

  Rationale:
  Sync, replication, and lazy loading need to distinguish “not loaded yet” from “does not exist.”

  Source locations:
  OAObjectKeyService, OAObjectPropertyService, OAObjectReflectService, OAObjectDSService

  Known related CODEX findings:
  Unresolved reference handling risks noted.

  Suggested unit tests:
  testUnresolvedReferenceKeyDoesNotBecomeNullOnCacheMiss()
  testMissingDatasourceObjectCanBeRetriedLater()

  Spec target section:
  OG Runtime / Reference Resolution

  9. Event Publication Contracts

  EVENT-ORDER-001 — After Events Fire Only After Authoritative State Transition

  Contract statement:
  After-events must not fire for operations that did not complete their authoritative state transition.

  Rationale:
  Listeners, UI, sync, triggers, and replication treat after-events as completed semantic facts.

  Source locations:
  HubEventService, OAObjectEventService, OATriggerService

  Known related CODEX findings:
  After-event on incomplete operation issues found/fixed.

  Suggested unit tests:
  testAfterAddNotFiredWhenAddFails()
  testAfterRemoveNotFiredWhenRemoveFails()
  testAfterDeleteNotFiredWhenDeleteFailsBeforeCompletion()

  Spec target section:
  OG Runtime / Event Ordering

  EVENT-ORDER-002 — Event Ordering Must Match Object/Hub Mutation Order

  Contract statement:
  Listeners must observe object and Hub state in the same semantic order the operation completed.

  Rationale:
  Generated UI and business rules rely on deterministic event-driven behavior.

  Source locations:
  HubEventService, HubAddRemoveService, OAObjectPropertyService

  Known related CODEX findings:
  Event ordering and missing/duplicate event risks noted.

  Suggested unit tests:
  testHubAddEventSeesObjectInHub()
  testHubRemoveEventSeesObjectRemoved()
  testPropertyChangeEventOrderIsDeterministic()

  Spec target section:
  OG Runtime / Event Ordering

  10. ThreadLocal / Context Contracts

  GRAPH-TL-001 — ThreadLocal Flags Must Be Restored By The Setter

  Contract statement:
  Any graph/runtime service that sets OAThreadLocal state must restore it in finally.

  Rationale:
  Flag leaks can suppress sync, events, loading, or verification across unrelated operations.

  Source locations:
  OAThreadLocalService, OAObjectDSService, HubSelectService, sync/remote thread services

  Known related CODEX findings:
  Thread-local restoration issues found in graph/runtime scans.

  Suggested unit tests:
  testSendSyncMessagesRestoredAfterException()
  testLoadingFlagRestoredAfterFailedLoad()
  testDeletingFlagRestoredAfterFailedDelete()

  Spec target section:
  OG Runtime / ThreadLocal Semantics

  GRAPH-CONTEXT-001 — OAContext Must Be Explicitly Scoped

  Contract statement:
  Context/user-access overrides must be scoped to the current operation/thread and restored after use.

  Rationale:
  Context leakage can apply one user’s access rules or graph assumptions to another request.

  Source locations:
  graph.context.OAContext, OAUserAccess, runtime thread/context services

  Known related CODEX findings:
  Context propagation risks noted in context/sibling review.

  Suggested unit tests:
  testContextOverrideRestoredAfterOperation()
  testUserAccessDoesNotLeakAcrossThreads()

  Spec target section:
  OG Runtime / Context Semantics

  11. Sync Role / CS Authority Contracts

  SYNC-ROLE-001 — isServer Means Actual Sync Server Only

  Contract statement:
  isServer() means actual sync server. Code requiring local/non-client behavior must use !isClient() or server-or-
  single-user semantics.

  Rationale:
  SingleUser must not lose local datasource/cache/load behavior after role semantics changed.

  Source locations:
  OASyncService, graph service sync checks, hub/object services

  Known related CODEX findings:
  OASync role-semantics pass found and fixed/commented old server==not-client assumptions.

  Suggested unit tests:
  testSingleUserDoesNotSkipLocalDatasourceLoad()
  testServerOnlySyncPublishDoesNotRunInSingleUser()
  testClientOnlyRoutingDoesNotRunInSingleUser()

  Spec target section:
  OG Runtime / Sync Role Semantics

  SYNC-CS-001 — Client/Server Authoritative Calls Must Not Produce Silent Local Divergence

  Contract statement:
  Client-side operations that require server authority must either receive/perform authoritative completion or leave
  caller-visible incomplete state.

  Rationale:
  Silent local/server divergence breaks sync and replication correctness.

  Source locations:
  OASyncService, HubAddRemoveService, OAObjectDSService, OADataSourceClient

  Known related CODEX findings:
  CS delegation and false-success paths found during graph scans.

  Suggested unit tests:
  testClientHubAddFailsVisibleWhenServerRejects()
  testClientDeleteDoesNotMarkCompleteWhenServerFails()
  testCSAuthoritativeCallDoesNotEmitLocalSuccessOnFailure()

  Spec target section:
  OG Runtime / Client-Server Authority

  12. Replication Hook Contracts

  SYNC-REPL-001 — Replication Hooks Observe Completed Semantic Changes

  Contract statement:
  Replication hooks must be invoked only for completed graph semantic changes, or must explicitly mark incomplete/
  retryable operations.

  Rationale:
  Replication divergence is worse than local failure because another runtime may accept false state.

  Source locations:
  OAReplicationService, OASyncService, object/hub save/delete/add/remove services

  Known related CODEX findings:
  Replication/sync divergence risks noted in graph scans.

  Suggested unit tests:
  testReplicationMessageAfterCompletedObjectChangeOnly()
  testFailedDeleteDoesNotEmitReplicationDelete()
  testHubMutationReplicationPreservesOrder()

  Spec target section:
  OG Runtime / Replication Semantics

  SYNC-REPL-002 — Replication Must Preserve Graph Identity And Ordering

  Contract statement:
  Replicated operations must preserve object identity, object keys, Hub membership order, and dependency ordering.

  Rationale:
  Replication target must reconstruct the same semantic Object Graph.

  Source locations:
  OAReplicationService, OASyncService, object cache/key services, Hub services

  Known related CODEX findings:
  Ordering and identity risks noted.

  Suggested unit tests:
  testReplicationCreatesSameObjectIdentityByKey()
  testReplicationHubAddOrderMatchesSource()
  testReplicationReferenceResolutionHandlesOutOfOrderObjectArrival()

  Spec target section:
  OG Runtime / Replication Ordering

  13. Failure / Partial-Progress / Retry Contracts

  GRAPH-FAILURE-001 — OG Operations Are Not Automatically Atomic

  Contract statement:
  Multi-step OG operations may make partial progress unless explicitly documented as atomic. Caller-visible
  exceptions signal incomplete operation.

  Rationale:
  OA supports runtime graph operations outside transactions. Correctness requires clear partial-progress semantics,
  not implicit rollback assumptions.

  Source locations:
  Graph/object/hub services broadly, OATransaction

  Known related CODEX findings:
  Clarified during adversarial graph review.

  Suggested unit tests:
  testExceptionFromMultiStepOperationIsVisibleToCaller()
  testPartialProgressOperationCanBeReconciledOrRetried()

  Spec target section:
  OG Runtime / Partial Progress Semantics

  GRAPH-FAILURE-002 — Partial Progress Must Not Become False Success

  Contract statement:
  Even when partial progress is allowed, graph services must not fire completion events, emit sync/replication
  messages, or mark lifecycle completed for an operation that failed.

  Rationale:
  False success causes silent corruption and unretryable state.

  Source locations:
  OAObjectSaveService, OAObjectDeleteService, HubAddRemoveService, HubDeleteService, OASyncService

  Known related CODEX findings:
  Core adversarial graph bugs mapped here.

  Suggested unit tests:
  testFailedOperationDoesNotFireAfterEvent()
  testFailedOperationDoesNotEmitSyncMessage()
  testFailedOperationDoesNotClearRetryState()

  Spec target section:
  OG Runtime / Failure Semantics

  GRAPH-FAILURE-003 — Retry Must Remain Possible After Visible Failure

  Contract statement:
  If an operation fails visibly, graph state must not be mutated into an unretryable state unless the failure is
  documented as terminal.

  Rationale:
  Applications need to refresh, retry, reconcile, or use transactions after failure.

  Source locations:
  Object lifecycle, Hub membership, lazy-load, datasource, sync services

  Known related CODEX findings:
  Retry/state-corruption issues found in graph scans.

  Suggested unit tests:
  testFailedLazyLoadCanRetry()
  testFailedSaveCanRetry()
  testFailedHubLoadCanRetry()

  Spec target section:
  OG Runtime / Retry Semantics

  14. Instrumentation / Event-Storm Detection Contracts

  EVENT-STORM-001 — Event Storm Detection Must Not Change Runtime Semantics

  Contract statement:
  Instrumentation/throttling/event-storm detection may warn or suppress diagnostic noise, but must not alter graph
  semantic state or event correctness.

  Rationale:
  Diagnostics must not become part of business behavior.

  Source locations:
  Event services, trigger services, throttling hooks

  Known related CODEX findings:
  none observed.

  Suggested unit tests:
  testEventStormInstrumentationDoesNotSuppressRequiredEvent()
  testThrottleWarningDoesNotChangeHubState()

  Spec target section:
  OG Runtime / Instrumentation Semantics

  EVENT-STORM-002 — Recursive Event Protection Must Restore State

  Contract statement:
  Any recursion/depth guard used during event publication or graph traversal must restore its prior state after the
  operation.

  Rationale:
  Leaked recursion guards can suppress later valid events or traversal.

  Source locations:
  HubEventService, OAObjectEventService, OATriggerService, thread-local services

  Known related CODEX findings:
  Thread-local/flag restoration risks mapped here.

  Suggested unit tests:
  testRecursiveEventGuardRestoredAfterException()
  testSecondIndependentEventNotSuppressedAfterFailedFirstEvent()

  Spec target section:
  OG Runtime / Event Instrumentation

  15. Test Coverage Matrix

  Graph ownership/routing:

  - testObjectOperationUsesOwningGraph
  - testCrossGraphObjectDoesNotUseWrongGraphServices
  - testGraphLookupByClassMatchesPackageGraph
  - testDefaultGraphCreatedOnce

  Service boundaries:

  - testGraphFacadeDelegatesThroughParentServices
  - testChildServiceUsesParentHookForCrossServiceState
  - testServiceInitializationOrderIsDeterministic

  Object identity/lifecycle:

  - testSameKeyReturnsSameObjectIdentity
  - testDatasourceLoadMergesWithExistingCachedObject
  - testKeyChangeUpdatesObjectCacheIndex
  - testFailedSaveDoesNotClearChangedFlagAsSuccess
  - testFailedDeleteDoesNotMarkDeletedAsCompleted

  Save/delete:

  - testObjectSaveUsesClassDatasource
  - testSingleUserObjectSaveUsesLocalDatasource
  - testDeleteRemovesObjectFromOwningHubs
  - testDeleteSendsSyncOnlyWhenAuthoritativeDeleteCompletes

  Hub membership/detail/AO:

  - testHubAddUpdatesReverseLinkWhenApplicable
  - testHubRemoveClearsLinkWhenApplicable
  - testHubAddedListTracksSuccessfulAddOnly
  - testActiveObjectAfterEventSeesNewAO
  - testDetailHubLoadsOnlyCurrentMasterChildren

  Reference/lazy load:

  - testFailedReferenceLoadDoesNotMarkLoaded
  - testFailedDetailHubLoadRemainsRetryable
  - testUnresolvedReferenceKeyDoesNotBecomeNullOnCacheMiss

  Events:

  - testAfterAddNotFiredWhenAddFails
  - testHubAddEventSeesObjectInHub
  - testPropertyChangeEventOrderIsDeterministic

  Thread/context:

  - testSendSyncMessagesRestoredAfterException
  - testLoadingFlagRestoredAfterFailedLoad
  - testContextOverrideRestoredAfterOperation
  - testUserAccessDoesNotLeakAcrossThreads

  Sync/replication:

  - testSingleUserDoesNotSkipLocalDatasourceLoad
  - testClientHubAddFailsVisibleWhenServerRejects
  - testReplicationMessageAfterCompletedObjectChangeOnly
  - testReplicationHubAddOrderMatchesSource

  Failure/retry:

  - testExceptionFromMultiStepOperationIsVisibleToCaller
  - testFailedOperationDoesNotFireAfterEvent
  - testFailedOperationDoesNotEmitSyncMessage
  - testFailedLazyLoadCanRetry
  - testFailedSaveCanRetry

  Instrumentation:

  - testEventStormInstrumentationDoesNotSuppressRequiredEvent
  - testRecursiveEventGuardRestoredAfterException

*/


/*qqqqqqqqqqqqqqqqqqq other
CODEX


 F. Top Graph Invariants

  - OAGraph verbs operate on objects/classes/Hubs owned by that graph, or explicitly reroute by runtime graph
    lookup.
  - OAGraphInternal is not application API.
  - Graph initialization is atomic.
  - Sync role transitions are atomic and contract-consistent.
  - Null-context server authority is deliberate and tested.
  - Context removal clears Hub and user-access state.
  - Trigger registration target and async lifecycle are explicit.

  G. Test Plan Outline

  - Graph lifecycle: successful init, failed init, package scan failure, repeated init.
  - Multi-graph ownership: create/get/save/delete/select/trigger with matching and foreign model classes.
  - Sync lifecycle: unconfigured/start, server/client create races, start/stop/restart, role predicates.
  - Context/user access: null context, server/client authority, remove context, Hub-rooted access rules.
  - Trigger behavior: explicit target graph, async context propagation, executor lifecycle/overload.
  - Replication: public contract decision, invalid start, role setup.

  H. Looks Sound
  The package split itself looks structurally sound. I found no direct JSON/XML/YAML/Jackson/JDBC/REST/UI
  contamination in com.viaoa.graph. The graph package mostly depends on legitimate OA kernel pieces: runtime,
  metadata, object, hub, sync/replication/remoting, serialization contracts, traversal, query/find/filter/select.
  The remaining risk is not module purity; it is tightening graph ownership, lifecycle, sync semantics, and context/
  trigger invariants.





G. Top Context/Sibling Invariants

  CTX-REMOVE-NULL-CLEARS-ALL
  CTX-ACCESS-LIFETIME-DETERMINISTIC
  UA-PACKAGE-SCOPE-CONSISTENT
  UA-EMPTY-PATH-NO-THROW
  UA-REVERSE-PATH-BOUNDS
  UA-CONFIGURE-BEFORE-PUBLISH
  SIB-SAME-THREAD-ENFORCED

  H. Test Plan Outline

  Add focused tests for null-context registration/removal, weak-vs-strong context lifetime contract, package-scoped
  enabled/visible behavior, empty/scalar access paths, reverse traversal bounds, configure-before-publish access
  rules, and cross-thread sibling-helper use.









 C. Consolidated Graph-Level Invariants

  GRAPH-OWNERSHIP-ROUTING-IS-EXPLICIT
  GRAPH-LIFECYCLE-IS-ATOMIC-AND-BOUNDED
  GRAPH-INTERNAL-APIS-ARE-NOT-APPLICATION-SURFACE
  GRAPH-SYNC-ROLE-GUARDS-ARE-CENTRALIZED
  GRAPH-ASYNC-WORK-PRESERVES-RUNTIME-CONTEXT
  GRAPH-OBJECT-HUB-MUTATIONS-STAY-BALANCED
  GRAPH-SERIALIZATION-PRESERVES-AUTHORITATIVE-IDENTITY
  GRAPH-TRIGGER-TARGET-AND-EXECUTOR-LIFECYCLE-ARE-EXPLICIT

  D. Consolidated Object-Service Invariants

  OBJ-GUID-STABLE-AFTER-INIT
  OBJ-KEY-COMPARISON-DETERMINISTIC
  OBJ-CACHE-HAS-ONE-AUTHORITATIVE-INSTANCE
  OBJ-SAVE-FAILURE-PRESERVES-STATE
  OBJ-DELETE-CLEANS-CACHE-KEYS-HUB-REFS
  OBJ-SER-READ-RESOLVE-USES-CACHE-AUTHORITY
  OBJ-SYNC-HOOKS-REQUIRE-VALID-ROLE
  OBJ-PRIMITIVE-NULL-MASKS-INITIALIZED

  E. Consolidated Hub-Service Invariants

  HUB-MEMBERSHIP-AND-OBJECT-REFS-BALANCED
  HUB-AO-DETAIL-ORDER-DETERMINISTIC
  HUB-DETAIL-MASTER-LINKS-CONSISTENT
  HUB-SHARE-LINK-SCOPE-EXPLICIT
  HUB-EVENTS-FIRE-AFTER-SUCCESSFUL-MUTATION
  HUB-SELECT-STATE-RESTORED-ON-FAILURE
  HUB-DELETEALL-FAILURE-DOES-NOT-CORRUPT-GRAPH
  HUB-SER-SIDE-EFFECTS-BOUNDED

  F. Context/Sibling Invariants

  CTX-REMOVE-NULL-CLEARS-ALL
  CTX-ACCESS-LIFETIME-DETERMINISTIC
  UA-PACKAGE-SCOPE-CONSISTENT
  UA-EMPTY-PATH-NO-THROW
  UA-REVERSE-PATH-BOUNDS
  UA-CONFIGURE-BEFORE-PUBLISH
  SIB-SAME-THREAD-ENFORCED




*/