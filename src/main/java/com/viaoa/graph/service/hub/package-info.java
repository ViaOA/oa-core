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

package com.viaoa.graph.service.hub;


/* CODEX Invariants

HUB-SERVICE-001 — Hub Services Are Graph-Owned Runtime Services
Contract statement: Hub service classes are owned, created, wired, and coordinated by the owning HubService/OAGraph
and must not act as independent application-level runtime authorities.
Rationale: Hub membership, active object, detail linkage, sync, events, sorting, selection, and serialization must
remain graph-scoped and centrally coordinated.
Source scope: HubService parent coordinator; all classes in com.viaoa.graph.service.hub; dependency hooks
implemented by the owning graph service.
Related CODEX findings: HubParentService initialize-once, child-service surface, child-service creation, and remote
hook role findings.
Suggested unit tests: testHubSubServicesAreCreatedByParentService(),
testHubParentServiceInitializeTwiceFailsPredictably(), testHubChildServicesAreNotPublicAppSurface()
Spec target section: OG Hub Runtime / Service Ownership

HUB-SERVICE-002 — Child Service Creation Is Single, Ordered, And Safely Published
Contract statement: Hub child services must be created once, after parent initialization, safely published, and
reused for the lifetime of the owning HubService unless explicit graph reset/shutdown occurs.
Rationale: Duplicate or partially initialized services can split Hub state, event routing, sync hooks, and lifecycle
behavior.
Source scope: HubParentService, HubService, all Hub child service getter/initialization paths.
Related CODEX findings: HubParentService parent initialize-once and concurrent child service creation findings.
Suggested unit tests: testHubChildServiceCreatedOnce(), testConcurrentHubChildServiceGetterCreatesSingleInstance(),
testHubChildServiceUnavailableBeforeParentInitByContract()
Spec target section: OG Hub Runtime / Service Lifecycle

HUB-ROLE-001 — Hub Runtime Role Routing Is Explicit
Contract statement: Hub operations must distinguish single-user, server, client, and unconfigured roles; local Hub
behavior must not require remote sync services, and remote/client operations must require the correct role.
Rationale: Add/remove/deleteAll/refresh/sort behavior must not silently choose the wrong local or remote path.
Source scope: HubCSService, HubParentService, HubAddRemoveService, HubDeleteService, HubSelectService,
HubSaveService.
Related CODEX findings: HubCS remote false return handling; HubParentService remote sync hooks null-safety; role
matrix test outline.
Suggested unit tests: testSingleUserHubAddDoesNotRequireRemoteSync(), testHubCSHooksAreNullSafeByRole(),
testClientHubOperationRequiresClientAuthority()
Spec target section: OG Hub Runtime / Role Semantics

HUB-OWNERSHIP-001 — Hub Operations Use The Owning Graph And Hub State
Contract statement: Hub operations must execute against the intended owning graph, Hub data structures, metadata,
master/detail state, and object identity scope.
Rationale: Hubs are graph-scoped semantic collections and must not mix objects, metadata, or services from another
graph.
Source scope: HubParentService, HubDataService, HubRootService, HubMasterService, HubLinkService, HubDetailService,
HubAddRemoveService.
Related CODEX findings: Package-level graph ownership and service orchestration findings.
Suggested unit tests: testHubOperationUsesOwningGraph(), testForeignGraphObjectRejectedOrRoutedByContract(),
testHubRootAndMasterStateMatchOwningGraph()
Spec target section: OG Hub Runtime / Graph Ownership

HUB-MEMBERSHIP-001 — Membership Mirrors Completed Vector State
Contract statement: Hub membership APIs must reflect the committed contents of the Hub vector and must not expose
objects as added, removed, present, or absent until the corresponding mutation is completed.
Rationale: Hub contents drive UI, object graph traversal, save/delete, sync, selection, and event behavior.
Source scope: HubDataService, HubAddRemoveService, HubSizeService, HubFindService, HubStatusService.
Related CODEX findings: HubAddRemoveService local/server divergence and local vector failure findings.
Suggested unit tests: testHubContainsOnlyCommittedAdds(), testFailedAddDoesNotPublishMembership(),
testFailedRemoveDoesNotPublishAbsence()
Spec target section: OG Hub Runtime / Membership Semantics

HUB-MEMBERSHIP-002 — Membership And Object Hub References Stay Balanced
Contract statement: Adding, inserting, removing, clearing, or deleting Hub members must keep Hub vector membership
and OAObject hub-reference state balanced.
Rationale: Object-to-Hub references are used by lifecycle, events, delete, save, sync, detail Hubs, and graph
traversal.
Source scope: HubAddRemoveService, HubDataService, HubDeleteService, HubLinkService, HubDetailService.
Related CODEX findings: Package-info HUB-MEMBERSHIP invariant; failed add/remove/deleteAll state findings.
Suggested unit tests: testHubAddUpdatesObjectHubReference(), testHubRemoveClearsObjectHubReferenceWhenApplicable(),
testFailedMutationDoesNotLeaveUnbalancedHubReference()
Spec target section: OG Hub Runtime / Object-Hub Reference Semantics

HUB-MEMBERSHIP-003 — Membership Uniqueness And Duplicate Policy Are Deterministic
Contract statement: Hub add/insert operations must apply the Hub’s duplicate policy deterministically and must not
create duplicate membership unless explicitly allowed by contract.
Rationale: Duplicate Hub entries corrupt UI order, save/delete traversal, sync, and event counts.
Source scope: HubAddRemoveService, HubDataService, HubFindService, HubAutoMatchService.
Related CODEX findings: Lifecycle test outline for owned, M2M, detail, shared, filtered, sorted, and recursive Hubs.
Suggested unit tests: testHubAddDuplicateUsesDefinedPolicy(), testHubInsertDuplicateDoesNotCorruptIndexes(),
testAutoMatchDoesNotCreateDuplicateMembership()
Spec target section: OG Hub Runtime / Membership Uniqueness

HUB-ORDER-001 — Positional Operations Preserve Deterministic Ordering
Contract statement: Insert, move, sort, remove, refresh, and selection reconciliation must preserve deterministic
object order and valid indexes according to Hub contract.
Rationale: Hub order is externally observable in UI, reports, iteration, active-object position, sync, and
serialization.
Source scope: HubAddRemoveService, HubSortService, HubSequenceService, HubSelectService, HubAOService,
HubDataService.
Related CODEX findings: HubSortService afterSort after failed sort; select/refresh reconciliation findings.
Suggested unit tests: testHubInsertMaintainsRequestedPosition(), testHubMoveUpdatesOrderAndAOPosition(),
testRefreshPreservesDefinedSelectOrder()
Spec target section: OG Hub Runtime / Ordering Semantics

HUB-AO-001 — Active Object Transitions Publish Only Completed AO State
Contract statement: Active object changes must stage AO value, AO position, detail Hub updates, link effects, and
events so listeners observe only completed authoritative AO state.
Rationale: UI binding, detail Hubs, selection, event listeners, and generated controllers depend on correct active
object state.
Source scope: HubAOService, HubDetailService, HubEventService, HubDataService.
Related CODEX findings: HubAOService failure during detail update leaves AO changed without rollback.
Suggested unit tests: testActiveObjectAfterEventSeesNewAO(), testFailedActiveObjectChangeRestoresPriorAO(),
testActiveObjectChangeUpdatesDetailHubBeforeAfterEvent()
Spec target section: OG Hub Runtime / Active Object Semantics

HUB-AO-002 — Active Object Position Matches Membership
Contract statement: Active object and active position must remain consistent with committed Hub membership after
add, remove, clear, move, sort, refresh, and detail-master changes.
Rationale: Active object drives UI current row, detail Hubs, property binding, and controller state.
Source scope: HubAOService, HubAddRemoveService, HubDeleteService, HubSortService, HubSelectService,
HubDetailService.
Related CODEX findings: HubSelectService refresh failure AO restoration finding.
Suggested unit tests: testRemoveActiveObjectChoosesDocumentedNextAO(), testSortUpdatesActivePositionForSameAO(),
testRefreshFailurePreservesPriorAOAndPosition()
Spec target section: OG Hub Runtime / Active Object Position Semantics

HUB-DETAIL-001 — Detail Hubs Reflect Current Master And Link Path Only
Contract statement: A detail Hub must expose only objects belonging to the current master object and metadata link
path, and must refresh deterministically when the master AO or link path changes.
Rationale: Master/detail behavior is core OA graph and UI semantics.
Source scope: HubDetailService, HubMasterService, HubLinkService, HubAOService, HubSelectService.
Related CODEX findings: Package-info HUB-DETAIL invariant and AO/detail failure findings.
Suggested unit tests: testDetailHubLoadsOnlyCurrentMasterChildren(), testDetailHubRefreshesWhenMasterAOChanges(),
testDetailHubDoesNotExposePreviousMasterChildren()
Spec target section: OG Hub Runtime / Master Detail Semantics

HUB-LINK-001 — Link-Bound Hubs Maintain Relationship Consistency
Contract statement: Link-bound Hub add/remove/clear operations must update corresponding object link, reverse link,
master/detail, and ownership state according to OA metadata.
Rationale: A Hub can represent object graph relationships; vector mutation without link consistency corrupts
traversal, save/delete, sync, and serialization.
Source scope: HubLinkService, HubDetailService, HubAddRemoveService, HubMasterService, HubDataService.
Related CODEX findings: Package-info detail/master consistency; lifecycle tests for owned and M2M Hubs.
Suggested unit tests: testHubAddUpdatesReverseLinkWhenApplicable(), testHubRemoveClearsLinkWhenApplicable(),
testManyToManyHubMutationUsesMetadataContract()
Spec target section: OG Hub Runtime / Link Relationship Semantics

HUB-SHARE-001 — Shared Hubs Share Only Intended Runtime Structures
Contract statement: Shared Hubs must share only the intended data and data-active structures, and must not
accidentally share unrelated listener, master/detail, filter, sort, or lifecycle state.
Rationale: Shared Hub behavior must be predictable without cross-contaminating independent Hub views.
Source scope: HubShareService, HubDataService, HubAOService, HubEventService, HubDetailService.
Related CODEX findings: Package-info HUB-SHARE invariant and lifecycle tests for shared Hubs.
Suggested unit tests: testSharedHubSharesMembershipByContract(), testSharedHubActiveObjectSharingFollowsContract(),
testSharedHubDoesNotShareUnintendedListenersOrDetailState()
Spec target section: OG Hub Runtime / Shared Hub Semantics

HUB-FILTER-001 — Filtered And Auto-Matched Hubs Reflect Their Source Contract
Contract statement: Filtered, matched, and auto-match Hub behavior must include only objects that satisfy the
configured source/filter/match contract and must update deterministically as source membership changes.
Rationale: Filtered/matched Hubs are runtime views used by UI, queries, and graph operations.
Source scope: HubAutoMatchService, HubFindService, HubAddRemoveService, HubDataService.
Related CODEX findings: Lifecycle test outline for filtered and recursive Hubs.
Suggested unit tests: testAutoMatchIncludesOnlyMatchingObjects(), testAutoMatchRemovesObjectWhenItNoLongerMatches(),
testFilteredHubSourceMutationUpdatesViewByContract()
Spec target section: OG Hub Runtime / Filtered Hub Semantics

HUB-SORT-001 — Sort State And Events Require Completed Sort Order
Contract statement: Hub sorted state and after-sort events must be published only after the Hub has a completed
deterministic order.
Rationale: UI, reports, listeners, active-position logic, and sync can treat after-sort as authoritative order.
Source scope: HubSortService, HubEventService, HubAOService, HubDataService.
Related CODEX findings: HubSortService afterSort fires after all sort retries fail.
Suggested unit tests: testAfterSortFiresOnlyAfterCompletedSort(), testSortFailureDoesNotPublishSortedState(),
testComparatorConcurrentModificationFailureIsVisible()
Spec target section: OG Hub Runtime / Sort Semantics

HUB-SELECT-001 — Select, Fetch, Load, And Refresh Preserve Retryable State
Contract statement: Hub select/fetch/load/refresh operations must update loaded, dirty, AO, membership, and ordering
state only after completion, or preserve retryable state on failure.
Rationale: Failed refresh must not make a Hub appear clean, loaded, or authoritatively refreshed with stale data.
Source scope: HubSelectService, HubDSService, HubDataService, HubAOService, HubStatusService.
Related CODEX findings: HubSelectService failed refresh dirty flag and AO restoration findings.
Suggested unit tests: testFailedRefreshRestoresSelectDirtyFlag(), testFailedRefreshPreservesActiveObject(),
testSuccessfulRefreshClearsDirtyOnlyAfterCompletion()
Spec target section: OG Hub Runtime / Select Refresh Semantics

HUB-LOAD-001 — Loaded, Loading, Empty, And Unloaded States Are Distinct
Contract statement: Hub runtime state must distinguish unloaded, loading, loaded-empty, loaded-with-values, and
failed/incomplete states where behavior depends on that distinction.
Rationale: Selection, lazy loading, serialization, size, find, refresh, and retry behavior require explicit Hub
load-state semantics.
Source scope: HubSelectService, HubDSService, HubStatusService, HubSizeService, HubSerializeService.
Related CODEX findings: Select tests for partially loaded Hubs and fetch/load/refresh failure.
Suggested unit tests: testLoadedEmptyOnlyAfterAuthoritativeLoad(), testFailedLoadDoesNotMarkHubLoaded(),
testPartiallyLoadedHubReportsStateByContract()
Spec target section: OG Hub Runtime / Load State Semantics

HUB-EVENT-001 — After Events Fire Only After Successful State Mutation
Contract statement: Hub after-events must not fire until the Hub mutation, relationship update, active-object
update, and relevant change tracking have completed.
Rationale: Listeners, UI, triggers, sync, and replication treat after-events as completed semantic facts.
Source scope: HubEventService, HubAddRemoveService, HubDeleteService, HubAOService, HubSortService,
HubSelectService.
Related CODEX findings: Package-info HUB-EVENT invariant; failed mutation and afterSort findings.
Suggested unit tests: testAfterAddNotFiredWhenAddFails(), testAfterRemoveNotFiredWhenRemoveFails(),
testAfterSortNotFiredWhenSortFails()
Spec target section: OG Hub Runtime / Event Publication Semantics

HUB-EVENT-002 — Before Events May Cancel Without Publishing Partial Success
Contract statement: Before listeners/callbacks may cancel or fail-fast according to contract; canceled mutations
must not publish after-events, sync messages, completed membership, or changed-state success.
Rationale: Before events are participants in the mutation, not observers of completed state.
Source scope: HubEventService, HubAddRemoveService, HubDeleteService, HubAOService, HubSortService.
Related CODEX findings: HubAddRemoveService before-remove-all listener failure and before-add failure paths.
Suggested unit tests: testBeforeAddCancelPreventsMembershipAndAfterEvent(),
testBeforeRemoveAllExceptionRestoresRuntimeFlags(), testBeforeListenerFailureDoesNotPublishSyncSuccess()
Spec target section: OG Hub Runtime / Before Event Semantics

HUB-EVENT-003 — Event Observation Order Matches Hub Mutation Order
Contract statement: Listeners must observe Hub membership, AO, detail, sort, select, and changed-state updates in
the same semantic order in which the mutation completed.
Rationale: UI, bindings, triggers, sync, and business listeners depend on deterministic event ordering.
Source scope: HubEventService, HubAddRemoveService, HubAOService, HubDetailService, HubSortService,
HubSelectService.
Related CODEX findings: Package-info event-order tests and AO/detail findings.
Suggested unit tests: testHubAddEventSeesObjectInHub(), testAOAfterEventSeesDetailUpdated(),
testRefreshEventOrderMatchesMembershipReconciliation()
Spec target section: OG Hub Runtime / Event Ordering Semantics

HUB-REENTRANT-001 — Reentrant And Recursive Hub Mutations Are Bounded And Restore State
Contract statement: Reentrant listener/callback mutations, recursive Hub changes, and runtime guards must remain
bounded and restore prior state on success or failure.
Rationale: Hub events can trigger additional graph mutations; leaked guards or unbounded recursion can suppress
valid events or create event storms.
Source scope: HubEventService, HubAddRemoveService, HubAOService, HubDetailService, HubParentService.
Related CODEX findings: Listener reentrancy, remote-thread suppression, and runtime flag restoration findings.
Suggested unit tests: testReentrantAddDoesNotCorruptMembership(), testRecursiveEventGuardRestoredAfterException(),
testSecondIndependentEventNotSuppressedAfterFailedFirstEvent()
Spec target section: OG Hub Runtime / Reentrancy Semantics

HUB-CS-001 — Client/Server Hub Mutations Must Reflect Authoritative Completion
Contract statement: Client/server Hub mutation return values and local state updates must reflect whether the
authoritative remote/server operation completed; remote failure must be caller-visible or leave documented
incomplete state.
Rationale: Silent local/server divergence corrupts Hub contents, object graph links, sync, and replication.
Source scope: HubCSService, HubAddRemoveService, HubDeleteService, HubParentService.
Related CODEX findings: HubCSService addToHub/insertInHub remote false hidden; deleteAll false means not delegated;
remote hooks null-safety.
Suggested unit tests: testRemoteAddToHubFalseIsVisible(), testRemoteInsertInHubFalseDoesNotPublishLocalSuccess(),
testClientDeleteAllRemoteFalseIsVisible()
Spec target section: OG Hub Runtime / Client Server Authority

HUB-CS-002 — Remote Sync Hooks Are Null-Safe By Role
Contract statement: Hub remote sync hooks must no-op or fail predictably when remote sync is absent for the current
role; they must not throw accidental NPEs from stale role assumptions.
Rationale: Single-user, server-without-remote, and unconfigured modes must remain valid runtime states.
Source scope: HubParentService remote hook methods, HubCSService, HubAddRemoveService, HubDeleteService.
Related CODEX findings: HubParentService remote sync hooks null-safety finding.
Suggested unit tests: testHubCSOperationsInSingleUserWithNoRemoteSync(),
testHubCSOperationsInServerWithoutRemoteSync(), testHubCSOperationsInClientWithoutRemoteSyncUseDocumentedBehavior()
Spec target section: OG Hub Runtime / Sync Hook Semantics

HUB-DELETE-001 — DeleteAll Coordinates Membership, Object Delete, Links, And Change State
Contract statement: Hub deleteAll must coordinate Hub membership, object delete/cascade, relationship cleanup,
change tracking, events, sync, and cache/object state according to contract.
Rationale: deleteAll is a graph mutation spanning Hub and object lifecycle; partial cleanup can silently corrupt the
object graph.
Source scope: HubDeleteService, HubAddRemoveService, HubDataService, HubLinkService, object delete hooks.
Related CODEX findings: HubDeleteService clears membership before object delete failure; second-object delete
failure partial state.
Suggested unit tests: testDeleteAllDeletesObjectsAndClearsHubAfterCompletion(),
testDeleteAllFailurePreservesOrMarksIncompleteMembership(), testDeleteAllFailureDoesNotLoseChangeStateSilently()
Spec target section: OG Hub Runtime / DeleteAll Semantics

HUB-DELETE-002 — Failed Hub Delete/Clear Must Not Publish False Success
Contract statement: Failed clear, removeAll, deleteAll, or delete cascade operations must not silently lose
membership, clear change state, fire after-events, or emit sync/replication success.
Rationale: False success makes Hub state unretryable and can create object graph divergence.
Source scope: HubDeleteService, HubAddRemoveService, HubEventService, HubCSService.
Related CODEX findings: HubDeleteService failed deleteAll state; HubCSService deleteAll false result;
HubAddRemoveService clear flag restoration.
Suggested unit tests: testFailedDeleteAllDoesNotFireAfterRemoveAll(), testFailedClearDoesNotEmitSyncSuccess(),
testDeleteAllFailureLeavesRetryableState()
Spec target section: OG Hub Runtime / Delete Failure Semantics

HUB-SAVE-001 — Hub Save Coordinates Membership And Object Save Semantics
Contract statement: Hub save/saveAll behavior must persist required Hub members and relationship changes according
to metadata, graph role, and object save semantics.
Rationale: Hub membership can represent graph relationships that must be persisted consistently with object state.
Source scope: HubSaveService, HubAddRemoveService, HubLinkService, HubDataService, object save hooks.
Related CODEX findings: Package-info lifecycle tests for owned, M2M, detail, shared, filtered, sorted, and recursive
Hubs.
Suggested unit tests: testHubSavePersistsAddedMembersByContract(),
testHubSavePersistsRemovedRelationshipByContract(), testHubSaveFailurePreservesChangeState()
Spec target section: OG Hub Runtime / Hub Save Semantics

HUB-SERIALIZE-001 — Hub Serialization Has Deterministic Load And Side-Effect Semantics
Contract statement: Hub serialization must define whether it materializes unloaded contents, preserve membership/
load state consistently, and avoid unintended event/sync side effects.
Rationale: Serialization must not accidentally mutate runtime graph state, emit sync/events, or duplicate Hub
membership.
Source scope: HubSerializeService, HubDataService, HubSelectService, HubEventService.
Related CODEX findings: HubSerializeService partially loaded Hub materialization side-effect finding.
Suggested unit tests: testSerializePartiallyLoadedHubHasDefinedMaterialization(),
testHubSerializationDoesNotEmitMutationEvents(), testHubSerializationRestoresSyncFlags()
Spec target section: OG Hub Runtime / Serialization Semantics

HUB-SERIALIZE-002 — Deserialized Hubs Resolve Membership Deterministically
Contract statement: Hub deserialization/readResolve behavior must reconstruct or resolve Hub membership using graph
identity rules without duplicating objects or losing ordering/load-state semantics.
Rationale: Serialized Hubs are used across persistence, remoting, sync, and tooling boundaries.
Source scope: HubSerializeService, HubDataService, HubLinkService, HubSelectService.
Related CODEX findings: Package-info serialization tests for duplicate Hub readResolve membership.
Suggested unit tests: testDeserializedHubUsesAuthoritativeObjectIdentity(),
testDeserializedHubPreservesOrderByContract(), testReadResolveDoesNotDuplicateMembership()
Spec target section: OG Hub Runtime / Deserialization Semantics

HUB-SIZE-001 — Hub Size And Empty Status Reflect Contracted Load State
Contract statement: Hub size, empty, and status operations must reflect the documented relationship between in-
memory membership, loaded/unloaded state, selected count, and datasource-backed count.
Rationale: UI, pagination, lazy loading, and selection behavior depend on accurate Hub size/status semantics.
Source scope: HubSizeService, HubStatusService, HubSelectService, HubDataService.
Related CODEX findings: Select tests for partially loaded Hubs and selectAll cache registration.
Suggested unit tests: testSizeForLoadedHubUsesMembershipCount(),
testSizeForUnloadedSelectedHubUsesDocumentedSource(), testEmptyStatusDistinguishesLoadedEmptyFromUnloaded()
Spec target section: OG Hub Runtime / Size And Status Semantics

HUB-FIND-001 — Hub Find Traversal Uses Committed Membership And Metadata Semantics
Contract statement: Hub find/search operations must use committed membership, metadata path semantics, object
identity, and active filters/sorts according to contract.
Rationale: Hub find results drive UI selection, object traversal, filtering, and runtime decisions.
Source scope: HubFindService, HubDataService, HubLinkService, HubDetailService, HubSortService.
Related CODEX findings: Package focus on deterministic Hub behavior; no direct CODEX finding observed.
Suggested unit tests: testHubFindUsesCommittedMembershipOnly(), testHubFindUsesObjectIdentityByContract(),
testHubFindPathMatchesMetadataSemantics()
Spec target section: OG Hub Runtime / Find Semantics

HUB-LIFECYCLE-001 — Hub Lifecycle Cleanup Releases Runtime Links And Listeners
Contract statement: Hub lifecycle cleanup must release graph-owned references, listeners, shared data links, detail/
master bindings, and runtime flags according to ownership contract.
Rationale: Stale Hub references can leak graph/object state and continue receiving events after intended disposal.
Source scope: HubShareService, HubDetailService, HubMasterService, HubEventService, HubDataService,
HubParentService.
Related CODEX findings: Package-info lifecycle cleanup and shared/detail test focus.
Suggested unit tests: testHubCleanupReleasesDetailBinding(), testSharedHubCleanupDoesNotBreakOwnerByContract(),
testDisposedHubDoesNotReceiveFutureEvents()
Spec target section: OG Hub Runtime / Lifecycle Cleanup Semantics

HUB-TL-001 — Hub Runtime ThreadLocal State Must Be Restored
Contract statement: Hub services that set runtime ThreadLocal/context flags for remote threads, sync suppression,
loading, selecting, deleting, serializing, or event dispatch must restore prior state in finally.
Rationale: Leaked runtime flags can suppress events, sync, selection, deletes, or remote behavior across unrelated
operations.
Source scope: HubAddRemoveService, HubDeleteService, HubSelectService, HubSerializeService, HubCSService,
HubEventService.
Related CODEX findings: HubAddRemoveService remote-thread suppression flag restoration; serialization sync
suppression findings.
Suggested unit tests: testRemoteThreadFlagRestoredWhenBeforeRemoveAllThrows(),
testSerializationRestoresSyncSuppressionFlag(), testRefreshRestoresRuntimeFlagsAfterFailure()
Spec target section: OG Hub Runtime / ThreadLocal Semantics

HUB-FAILURE-001 — Hub Operations Must Not Publish False Success
Contract statement: Hub services must not mark state complete, clear dirty/change flags, fire after-events, emit
sync/replication hooks, or publish sorted/loaded/deleted state when the authoritative operation failed.
Rationale: False success creates silent Hub corruption and unretryable graph state.
Source scope: HubAddRemoveService, HubDeleteService, HubSelectService, HubSortService, HubCSService,
HubEventService.
Related CODEX findings: Remote false hidden; failed refresh clears dirty; afterSort after failed sort; failed
deleteAll loses membership; local vector failure after CS success.
Suggested unit tests: testFailedHubAddDoesNotPublishAfterEventOrSync(), testFailedRefreshDoesNotClearDirtyState(),
testFailedSortDoesNotFireAfterSort(), testFailedDeleteAllDoesNotClearMembershipAsSuccess()
Spec target section: OG Hub Runtime / Failure Semantics

HUB-FAILURE-002 — Partial Hub Progress Must Be Visible And Retryable
Contract statement: Hub operations may make partial progress only when caller-visible failure or observable
incomplete state signals that completion did not occur; retry or reconciliation state must remain valid unless
failure is terminal by contract.
Rationale: Add/remove/deleteAll/refresh/sort can involve multiple object, link, listener, datasource, and sync
stages.
Source scope: HubAddRemoveService, HubDeleteService, HubSelectService, HubSortService, HubCSService.
Related CODEX findings: Partial deleteAll, refresh failure, local/server divergence, listener exception, and local
vector failure findings.
Suggested unit tests: testPartialAddFailureLeavesRetryableHubState(),
testPartialDeleteAllFailureIsVisibleAndRecoverable(), testPartialRefreshFailureCanRetry()
Spec target section: OG Hub Runtime / Partial Progress Semantics

HUB-DETERMINISM-001 — Observable Hub Mutations Are Deterministic For The Same Runtime State
Contract statement: For the same graph state, Hub metadata, role, datasource result, listener outcome, and object
identity state, Hub services must produce the same membership, AO, order, events, change tracking, and sync side
effects.
Rationale: Deterministic Hub behavior is required for UI binding, sync/replication, testing, debugging, and
generated application semantics.
Source scope: All com.viaoa.graph.service.hub services.
Related CODEX findings: Package-wide membership, AO, select, sort, deleteAll, CS, serialization, and event findings.
Suggested unit tests: testSameAddScenarioProducesSameMembershipAndEvents(),
testSameRefreshScenarioProducesSameOrderAndAO(), testSameDeleteAllFailureProducesSameRetryableState()
Spec target section: OG Hub Runtime / Deterministic Mutation Semantics

*/
