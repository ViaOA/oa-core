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
 * Core collection and event framework for OA — the {@code Hub}.
 *
 * <p>The {@code com.viaoa.hub} package defines OA’s reactive data layer,
 * centered around the {@link com.viaoa.hub.Hub Hub} class. A Hub acts as a
 * dynamic, observable collection of {@link com.viaoa.object.OAObject OAObject}
 * instances, maintaining both content and state (such as the active object)
 * and serving as the foundation for binding, synchronization, and messaging
 * across the OA runtime.</p>
 *
 * <h2>Overview</h2>
 * <p>Hubs are lightweight, observable, and linkable collections that enable
 * the OAObject Graph to connect data, UI, and services. They provide:</p>
 * <ul>
 *   <li>Master–detail relationships between Hubs (via
 *       {@link com.viaoa.hub.HubDetailDelegate HubDetailDelegate}).</li>
 *   <li>Linking to reference properties of other Hubs (via
 *       {@link com.viaoa.hub.HubLinkDelegate HubLinkDelegate}).</li>
 *   <li>Shared collections and shared active objects (via
 *       {@link com.viaoa.hub.HubShareDelegate HubShareDelegate}).</li>
 *   <li>Automated observability and event dispatching for all object and
 *       Hub-level changes (through {@link com.viaoa.hub.HubEvent HubEvent}
 *       and {@link com.viaoa.hub.HubListener HubListener}).</li>
 *   <li>Filtering, sorting, grouping, merging, and sampling mechanisms to
 *       transform and synchronize Hub contents in real time.</li>
 * </ul>
 *
 * <h2>Internal Architecture</h2>
 * <p>Each Hub maintains a set of delegate components that separate behavior
 * into specialized responsibilities:</p>
 * <ul>
 *   <li>{@link com.viaoa.hub.HubData HubData} — holds the core collection and
 *       metadata such as the active object and listener lists.</li>
 *   <li>{@link com.viaoa.hub.HubDataUnique HubDataUnique} and
 *       {@link com.viaoa.hub.HubDataActive HubDataActive} — manage Hub
 *       identity and active-object tracking across shared and detail Hubs.</li>
 *   <li>{@link com.viaoa.hub.HubDelegate HubDelegate} — the primary façade
 *       coordinating operations across all delegates.</li>
 *   <li>Other delegates provide specialized services (selecting, linking,
 *       saving, serialization, etc.), allowing modular composition of
 *       functionality.</li>
 * </ul>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>Master–Detail Wiring:</b> Detail Hubs automatically mirror the
 *       collection from the master’s active object reference.</li>
 *   <li><b>Shared Hubs:</b> Multiple Hubs can share the same data and events
 *       for synchronized UIs or parallel processing.</li>
 *   <li><b>Observability:</b> All Hub and OAObject events propagate upward
 *       through listeners, enabling reactive updates and distributed
 *       synchronization.</li>
 *   <li><b>Temporary and Recursive Support:</b>
 *       {@link com.viaoa.hub.util.HubTemp HubTemp} provides lightweight
 *       one-object contexts, and {@link com.viaoa.hub.HubRootDelegate
 *       HubRootDelegate} manages recursion roots.</li>
 * </ul>
 *
 * <h2>Design Philosophy</h2>
 * <p>The Hub framework encapsulates the "observable object graph" pattern
 * central to OA. It separates collection management, event propagation,
 * and synchronization logic from the domain model while remaining fully
 * type-safe and reflection-aware. Its design emphasizes:</p>
 * <ul>
 *   <li>Minimal overhead and explicit visibility of relationships.</li>
 *   <li>Loose coupling between model, UI, and data source layers.</li>
 *   <li>Distributed, event-driven synchronization with optional persistence.</li>
 * </ul>
 *
 * @author ViaOA
 * @see com.viaoa.hub.Hub
 * @see com.viaoa.hub.HubDelegate
 * @see com.viaoa.object.OAObject
 */
package com.viaoa.hub;


/* CODEX Invariants

1. Hub Membership Contracts

  HUB-MEMBERSHIP-001 — Hub Contains Only Valid Objects For Its Object Class
  Contract statement: A Hub may contain only objects assignable to its configured object class, unless it is
  explicitly untyped by contract.
  Rationale: Hub services, UI binding, sorting, filtering, detail linkage, and save/delete assume a stable element
  type.
  Source locations: Hub, HubData, HubDataService, HubAddRemoveService.
  Known related CODEX findings: none observed.
  Suggested unit tests: testHubRejectsWrongObjectClass(), testHubAcceptsSubclassWhenAllowed()
  Spec target section: Hub Runtime / Membership Semantics

  HUB-MEMBERSHIP-002 — Hub Membership Must Be Graph-Consistent
  Contract statement: Adding or removing an OAObject from a Hub must keep object-Hub membership tracking consistent
  with the owning graph.
  Rationale: Save/delete, weak membership tracking, sync, and event propagation depend on knowing which Hubs contain
  an object.
  Source locations: Hub, HubData, HubAddRemoveService, object-Hub membership services.
  Known related CODEX findings: Hub membership tracking issues were reviewed in graph/hub scans.
  Suggested unit tests: testObjectKnowsHubAfterAdd(), testObjectHubMembershipRemovedAfterRemove()
  Spec target section: Hub Runtime / Object-Hub Membership

  HUB-MEMBERSHIP-003 — Duplicate Policy Must Be Consistent
  Contract statement: Hub duplicate behavior must follow the configured add mode and must not create accidental
  duplicate membership.
  Rationale: Duplicate objects corrupt position lookup, AO behavior, saveAll/deleteAll, and UI selection.
  Source locations: HubAddRemoveService, HubDataService, Hub.
  Known related CODEX findings: duplicate/add behavior reviewed.
  Suggested unit tests: testHubRejectsDuplicateWhenNoDups(), testHubDuplicatePolicyConsistentAcrossAddAndInsert()
  Spec target section: Hub Runtime / Duplicate Semantics

  2. Add / Remove / Insert / Move Contracts

  HUB-ADDREMOVE-001 — Successful Add Inserts Object At One Defined Position
  Contract statement: A successful add/insert must place the object in exactly one defined Hub position and update
  Hub membership once.
  Rationale: Prevents duplicate events, duplicate membership, and index drift.
  Source locations: HubAddRemoveService, HubDataService, HubEventService.
  Known related CODEX findings: add/insert correctness issues reviewed.
  Suggested unit tests: testAddPlacesObjectAtEndOnce(), testInsertPlacesObjectAtRequestedPositionOnce()
  Spec target section: Hub Runtime / Add Semantics

  HUB-ADDREMOVE-002 — Remove Must Remove The Intended Object Only
  Contract statement: Remove operations must remove the intended object/position and must not disturb unrelated
  objects.
  Rationale: Hub membership order and detail/view Hubs depend on precise removal.
  Source locations: HubAddRemoveService, HubDataService.
  Known related CODEX findings: remove/change tracking issues reviewed.
  Suggested unit tests: testRemoveObjectRemovesOnlyThatObject(), testRemoveAtRemovesOnlyThatPosition()
  Spec target section: Hub Runtime / Remove Semantics

  HUB-MOVE-001 — Move/Swap Must Preserve Membership Set
  Contract statement: Move and swap operations may change order but must not add, drop, or duplicate objects.
  Rationale: Sorting, UI reorder, and replication require order changes without membership corruption.
  Source locations: HubAddRemoveService, HubDataService, HubEventService.
  Known related CODEX findings: move/swap correctness risks reviewed.
  Suggested unit tests: testMovePreservesMembershipSet(), testSwapPreservesMembershipSetAndPositions()
  Spec target section: Hub Runtime / Order Mutation Semantics

  3. Active Object Contracts

  HUB-AO-001 — Active Object Must Be Null Or A Hub Member
  Contract statement: A Hub active object must either be null or currently contained in that Hub, unless an explicit
  loading/detail transition temporarily defines otherwise.
  Rationale: UI binding, detail Hubs, and generated controllers depend on AO membership.
  Source locations: HubAOService, HubDataService, HubDetailService.
  Known related CODEX findings: AO/detail behavior reviewed.
  Suggested unit tests: testSetAOToMemberSucceeds(), testRemoveActiveObjectClearsOrMovesAOByContract()
  Spec target section: Hub Runtime / Active Object Semantics

  HUB-AO-002 — AO Change Events Observe New Authoritative AO
  Contract statement: After-AO-change listeners must observe the new authoritative active object.
  Rationale: Detail Hub refresh and UI binding often run from AO listeners.
  Source locations: HubAOService, HubEventService, HubDetailService.
  Known related CODEX findings: event ordering risks mapped here.
  Suggested unit tests: testAfterAOChangeListenerSeesNewAO(), testDetailHubUsesNewAOWhenAOEventFires()
  Spec target section: Hub Runtime / AO Event Ordering

  4. Master / Detail / Link Contracts

  HUB-DETAIL-001 — Detail Hub Membership Mirrors Master Link State
  Contract statement: A detail Hub must contain exactly the objects linked to its current master object through its
  configured link path.
  Rationale: Master/detail is a core OA model and UI contract.
  Source locations: HubDetailService, HubLinkService, HubDataService.
  Known related CODEX findings: detail/lazy-load issues covered in graph scans.
  Suggested unit tests: testDetailHubContainsOnlyCurrentMasterChildren(), testChangingMasterRefreshesDetailHub()
  Spec target section: Hub Runtime / Detail Hub Semantics

  HUB-LINK-001 — Hub Add/Remove Updates Reverse Link When Link-Bound
  Contract statement: For link-bound Hubs, add/remove operations must update the corresponding object reference or
  reverse Hub relationship.
  Rationale: Graph relationships must stay bidirectionally consistent.
  Source locations: HubLinkService, HubAddRemoveService, object reference services.
  Known related CODEX findings: link consistency findings reviewed.
  Suggested unit tests: testAddingChildSetsParentReference(),
  testRemovingChildClearsParentReferenceWhenContractRequires()
  Spec target section: Hub Runtime / Link Semantics

  HUB-MASTER-001 — Master Object Changes Must Rebind Detail State
  Contract statement: When a Hub’s master object changes, dependent detail/link state must be rebound before
  observable after-events.
  Rationale: Listeners and UI must not see stale master/detail state.
  Source locations: HubDetailService, HubAOService, HubEventService.
  Known related CODEX findings: AO/detail ordering risks reviewed.
  Suggested unit tests: testMasterChangeRebindsDetailBeforeEvent(),
  testOldMasterChildrenNotVisibleAfterMasterChange()
  Spec target section: Hub Runtime / Master-Detail Semantics

  5. Shared / Filtered / Sorted / View Hub Contracts

  HUB-SHARED-001 — Shared Hubs Reflect Same Underlying Membership
  Contract statement: Shared Hubs must observe the same underlying membership state while preserving their own
  allowed local view state such as AO when applicable.
  Rationale: Shared Hubs are used to project one collection into multiple UI/controllers without copying membership.
  Source locations: HubShareService, HubDataService, HubAOService.
  Known related CODEX findings: shared Hub risks reviewed.
  Suggested unit tests: testSharedHubSeesSourceAdd(), testSharedHubSeesSourceRemove()
  Spec target section: Hub Runtime / Shared Hub Semantics

  HUB-FILTER-001 — Filtered/View Hubs Must Not Mutate Source Membership Incorrectly
  Contract statement: Filtered/view Hubs must represent a constrained view and must not add/remove source membership
  except through explicit source-authoritative operations.
  Rationale: View projections must not corrupt real graph collections.
  Source locations: filter/view Hub services, HubAddRemoveService, HubDataService.
  Known related CODEX findings: filtered/view behavior reviewed.
  Suggested unit tests: testFilteredHubExcludesNonMatchingObject(),
  testFilteredHubRemoveDoesNotSilentlyCorruptSource()
  Spec target section: Hub Runtime / View Hub Semantics

  HUB-SORT-001 — Sorted Hub Order Must Match Comparator/Sort Contract
  Contract statement: Sorted Hubs must maintain order according to configured sort/comparator and must update order
  when relevant sort properties change.
  Rationale: UI, select order, and generated screens depend on stable sort behavior.
  Source locations: sorting services, HubDataService, comparator integration.
  Known related CODEX findings: sorting setup issues reviewed.
  Suggested unit tests: testSortedHubAddPlacesObjectInSortedPosition(), testSortedHubReordersOnSortPropertyChange()
  Spec target section: Hub Runtime / Sort Semantics

  6. Auto-Match / Auto-Sequence / Auto-Add Contracts

  HUB-AUTOMATCH-001 — Auto-Match Must Preserve Link Consistency
  Contract statement: Auto-match behavior must match objects according to configured properties and update links
  only when a valid match is found.
  Rationale: Auto-match wires object graphs automatically; wrong matches corrupt relationships.
  Source locations: auto-match Hub services/controllers, link services.
  Known related CODEX findings: auto-match SingleUser/routing risks reviewed.
  Suggested unit tests: testAutoMatchLinksMatchingObject(), testAutoMatchDoesNotLinkNonMatchingObject()
  Spec target section: Hub Runtime / Auto-Match Semantics

  HUB-AUTOSEQ-001 — Auto-Sequence Must Produce Stable Sequence Values
  Contract statement: Auto-sequence behavior must assign deterministic, unique sequence positions within the Hub
  scope.
  Rationale: Generated apps rely on ordered child collections.
  Source locations: auto-sequence Hub services/controllers, property change services.
  Known related CODEX findings: auto-sequence setup risks reviewed.
  Suggested unit tests: testAutoSequenceAssignsIncreasingValues(), testAutoSequenceReordersAfterMoveByContract()
  Spec target section: Hub Runtime / Auto-Sequence Semantics

  HUB-AUTOADD-001 — Auto-Add Must Not Create False Membership
  Contract statement: Auto-add behavior may create/add objects only when the configured trigger condition is
  satisfied and must not create duplicates.
  Rationale: Auto-add is model-driven automation; false additions create unexpected persistent objects.
  Source locations: auto-add Hub services/controllers, add/remove services.
  Known related CODEX findings: none observed.
  Suggested unit tests: testAutoAddCreatesObjectWhenTriggerSatisfied(), testAutoAddDoesNotCreateDuplicateObject()
  Spec target section: Hub Runtime / Auto-Add Semantics

  7. SaveAll / DeleteAll Contracts

  HUB-SAVEALL-001 — SaveAll Saves Current Hub Graph Scope By Contract
  Contract statement: saveAll must save the intended Hub contents/detail scope and must not silently skip changed
  members.
  Rationale: Hub saveAll is a high-level generated-app persistence operation.
  Source locations: HubSaveService, object save/datasource services.
  Known related CODEX findings: saveAll behavior reviewed.
  Suggested unit tests: testSaveAllSavesChangedHubMembers(), testSaveAllPropagatesSaveFailure()
  Spec target section: Hub Runtime / SaveAll Semantics

  HUB-DELETEALL-001 — DeleteAll Deletes Intended Members Only
  Contract statement: deleteAll must delete the intended Hub members/scope and must not report success while leaving
  authoritative members undeleted.
  Rationale: DeleteAll has high blast radius and affects cache, datasource, sync, and UI.
  Source locations: HubDeleteService, object delete service, datasource service.
  Known related CODEX findings: deleteAll correctness issues found in graph/hub scans.
  Suggested unit tests: testDeleteAllDeletesEveryCurrentMember(), testDeleteAllFailureIsVisibleToCaller()
  Spec target section: Hub Runtime / DeleteAll Semantics

  8. Event / Listener Contracts

  HUB-EVENT-001 — Add/Remove/Move Events Fire After Completed Mutation
  Contract statement: Hub after-events must fire only after the corresponding membership/order mutation has
  completed.
  Rationale: Listeners must observe authoritative Hub state.
  Source locations: HubEventService, HubAddRemoveService, HubDataService.
  Known related CODEX findings: event ordering findings reviewed.
  Suggested unit tests: testAfterAddEventSeesObjectInHub(), testAfterRemoveEventSeesObjectRemoved(),
  testAfterMoveEventSeesNewOrder()
  Spec target section: Hub Runtime / Event Ordering

  HUB-EVENT-002 — Listener Failure Must Not Produce False Success
  Contract statement: If listener failure aborts an operation by contract, the operation must not fire completion
  events or report success.
  Rationale: Generated business logic may use listeners as validation or side-effect gates.
  Source locations: HubEventService, HubAddRemoveService, callback/trigger services.
  Known related CODEX findings: listener/false-success risks reviewed.
  Suggested unit tests: testListenerExceptionPreventsFalseAddSuccess(), testListenerExceptionDoesNotLeakEventState()
  Spec target section: Hub Runtime / Listener Failure

  9. Change Tracking Contracts

  HUB-CHANGE-001 — Added/Removed Lists Reflect Completed Changes
  Contract statement: Hub change tracking must record only completed adds/removes and must clear only when
  explicitly requested or after successful save semantics.
  Rationale: Persistence and sync use change lists to determine relationship updates.
  Source locations: HubDataService, HubAddRemoveService, HubSaveService.
  Known related CODEX findings: added/removed list issues reviewed.
  Suggested unit tests: testAddedListRecordsSuccessfulAddOnly(), testRemovedListRecordsSuccessfulRemoveOnly(),
  testClearChangesClearsBothLists()
  Spec target section: Hub Runtime / Change Tracking

  HUB-CHANGE-002 — Move/Sort Must Not Be Misclassified As Add/Remove
  Contract statement: Reordering operations must not create added/removed change entries unless membership actually
  changes.
  Rationale: Relationship persistence must not treat order changes as membership changes.
  Source locations: HubDataService, move/sort services.
  Known related CODEX findings: move/sort behavior reviewed.
  Suggested unit tests: testMoveDoesNotAddChangeEntry(), testSortDoesNotCreateRemovedEntries()
  Spec target section: Hub Runtime / Change Tracking

  10. Lazy Load / Select Contracts

  HUB-LOAD-001 — Loaded/Empty State Requires Authoritative Load Completion
  Contract statement: A Hub may be marked loaded or empty only after a successful authoritative load/select proves
  that state.
  Rationale: False loaded-empty state prevents retry and hides data.
  Source locations: HubSelectService, HubDataService, datasource interaction.
  Known related CODEX findings: lazy-load state corruption issues covered.
  Suggested unit tests: testFailedHubLoadDoesNotMarkLoaded(), testEmptyHubMarkedLoadedOnlyAfterSuccessfulSelect()
  Spec target section: Hub Runtime / Lazy Load Semantics

  HUB-SELECT-001 — Hub Select Must Preserve Datasource Iterator Lifecycle
  Contract statement: Hub select/loading must close datasource iterators according to OA iterator lifecycle rules.
  Rationale: Remote/datasource resources must not leak.
  Source locations: HubSelectService, datasource iterator use.
  Known related CODEX findings: datasource iterator lifecycle findings mapped here.
  Suggested unit tests: testHubSelectClosesIteratorOnCompletion(), testHubSelectClosesIteratorOnException()
  Spec target section: Hub Runtime / Select Resource Semantics

  11. Sync / Replication Interaction Contracts

  HUB-SYNC-001 — Hub Mutations Emit Sync Only For Completed Semantic Changes
  Contract statement: Add/remove/move/sort/deleteAll must emit sync/replication messages only for completed semantic
  mutations.
  Rationale: Prevents client/server and replication divergence.
  Source locations: HubAddRemoveService, HubDeleteService, sync/replication services.
  Known related CODEX findings: sync/replication false-success issues reviewed.
  Suggested unit tests: testFailedHubAddDoesNotEmitSync(), testCompletedHubRemoveEmitsSyncWhenEnabled()
  Spec target section: Hub Runtime / Sync Semantics

  HUB-SYNC-002 — Client Hub Mutations Respect Server Authority
  Contract statement: In client/server mode, operations requiring server authority must not locally claim success
  before authoritative server acceptance when the contract requires gating.
  Rationale: Prevents silent local/server Hub divergence.
  Source locations: Hub add/remove/delete services, sync services.
  Known related CODEX findings: CS-authoritative ordering clarified in graph scans.
  Suggested unit tests: testClientHubAddRejectedByServerDoesNotRemainLocalSuccess(),
  testClientHubRemoveUsesAuthoritativeServerResult()
  Spec target section: Hub Runtime / Client-Server Authority

  12. ThreadLocal / Context Contracts

  HUB-TL-001 — Hub Operations Respect Loading/Saving/Deleting Flags
  Contract statement: Hub operations must honor OAThreadLocal loading/saving/deleting/sync flags and restore any
  flags they set.
  Rationale: Prevents recursion, unwanted sync, and leaked operation context.
  Source locations: Hub services, runtime thread-local service.
  Known related CODEX findings: thread-local restoration findings covered in runtime/graph scans.
  Suggested unit tests: testHubLoadRestoresLoadingFlagAfterException(),
  testHubDeleteAllRestoresDeletingFlagAfterException()
  Spec target section: Hub Runtime / ThreadLocal Semantics

  HUB-CONTEXT-001 — Hub Context/User Access Applies Consistently
  Contract statement: Hub mutations and visibility decisions that depend on context/user access must use the active
  graph/runtime context.
  Rationale: Generated applications depend on context-aware access and UI behavior.
  Source locations: Hub services, graph context services.
  Known related CODEX findings: context/user-access behavior reviewed.
  Suggested unit tests: testUserAccessBlocksUnauthorizedHubAdd(), testContextDoesNotLeakAcrossHubOperations()
  Spec target section: Hub Runtime / Context Semantics

  13. Failure / Partial-Progress / Retry Contracts

  HUB-FAILURE-001 — Partial Progress Is Allowed But Must Be Visible
  Contract statement: Hub operations are not automatically atomic, but caller-visible exceptions must signal
  incomplete operations.
  Rationale: OA allows partial progress outside transactions while preserving caller ability to retry/reconcile.
  Source locations: Hub add/remove/delete/save/select services.
  Known related CODEX findings: partial-progress semantics clarified during graph scans.
  Suggested unit tests: testHubOperationExceptionVisibleToCaller(), testPartialHubProgressCanBeReconciled()
  Spec target section: Hub Runtime / Failure Semantics

  HUB-FAILURE-002 — Failed Operations Must Not Produce Completion Signals
  Contract statement: Failed Hub operations must not fire after-events, emit sync/replication messages, or mark
  loaded/deleted/saved state as complete.
  Rationale: False success is the dangerous failure mode.
  Source locations: Hub event, sync, delete, save, select services.
  Known related CODEX findings: false-success bugs found/fixed/commented.
  Suggested unit tests: testFailedAddDoesNotFireAfterAdd(), testFailedDeleteAllDoesNotEmitCompletedSync(),
  testFailedLoadDoesNotMarkHubLoaded()
  Spec target section: Hub Runtime / False Success Prevention

  HUB-RETRY-001 — Retry Must Remain Possible After Visible Failure
  Contract statement: After a visible Hub operation failure, state must remain retryable unless the operation
  explicitly documents terminal partial state.
  Rationale: Applications can retry, refresh, reconcile, or use transactions.
  Source locations: Hub add/remove/delete/select/save services.
  Known related CODEX findings: retry correctness reviewed.
  Suggested unit tests: testFailedHubLoadCanRetry(), testFailedHubSaveAllCanRetry(),
  testFailedHubDeleteAllCanRetryOrReconcile()
  Spec target section: Hub Runtime / Retry Semantics

  14. Test Coverage Matrix

  Membership/add/remove:

  - testHubRejectsWrongObjectClass
  - testObjectKnowsHubAfterAdd
  - testObjectHubMembershipRemovedAfterRemove
  - testHubRejectsDuplicateWhenNoDups
  - testAddPlacesObjectAtEndOnce
  - testInsertPlacesObjectAtRequestedPositionOnce
  - testRemoveObjectRemovesOnlyThatObject
  - testMovePreservesMembershipSet

  AO/master/detail/link:

  - testSetAOToMemberSucceeds
  - testAfterAOChangeListenerSeesNewAO
  - testDetailHubContainsOnlyCurrentMasterChildren
  - testChangingMasterRefreshesDetailHub
  - testAddingChildSetsParentReference
  - testRemovingChildClearsParentReferenceWhenContractRequires

  Shared/view/sort:

  - testSharedHubSeesSourceAdd
  - testSharedHubSeesSourceRemove
  - testFilteredHubExcludesNonMatchingObject
  - testFilteredHubRemoveDoesNotSilentlyCorruptSource
  - testSortedHubAddPlacesObjectInSortedPosition
  - testSortedHubReordersOnSortPropertyChange

  Automation:

  - testAutoMatchLinksMatchingObject
  - testAutoMatchDoesNotLinkNonMatchingObject
  - testAutoSequenceAssignsIncreasingValues
  - testAutoAddCreatesObjectWhenTriggerSatisfied
  - testAutoAddDoesNotCreateDuplicateObject

  Save/delete/change tracking:

  - testSaveAllSavesChangedHubMembers
  - testSaveAllPropagatesSaveFailure
  - testDeleteAllDeletesEveryCurrentMember
  - testDeleteAllFailureIsVisibleToCaller
  - testAddedListRecordsSuccessfulAddOnly
  - testRemovedListRecordsSuccessfulRemoveOnly
  - testMoveDoesNotAddChangeEntry

  Events/select/sync:

  - testAfterAddEventSeesObjectInHub
  - testAfterRemoveEventSeesObjectRemoved
  - testListenerExceptionPreventsFalseAddSuccess
  - testFailedHubLoadDoesNotMarkLoaded
  - testHubSelectClosesIteratorOnCompletion
  - testFailedHubAddDoesNotEmitSync
  - testClientHubAddRejectedByServerDoesNotRemainLocalSuccess

  Thread/failure/retry:

  - testHubLoadRestoresLoadingFlagAfterException
  - testHubDeleteAllRestoresDeletingFlagAfterException
  - testUserAccessBlocksUnauthorizedHubAdd
  - testHubOperationExceptionVisibleToCaller
  - testFailedAddDoesNotFireAfterAdd
  - testFailedHubLoadCanRetry

*/



