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

//CODEX unit tests 20260528

/* CODEX Invariants

HUB-MEMBERSHIP-001 — Hub Contains Only Valid Objects For Its Object Class
Contract statement: A Hub may contain only objects assignable to its configured object class, unless it is
explicitly untyped or otherwise documented by contract.
Rationale: Hub services, UI binding, sorting, filtering, detail linkage, save/delete, and serialization assume a
stable element type.
Source scope: Hub constructors, Hub.getObjectClass, Hub.add, insert, replace, addAll-style collection APIs, HubData
object class state.
Related CODEX findings: none observed.
Suggested unit tests: testHubRejectsWrongObjectClass(), testHubAcceptsSubclassWhenAllowed(),
testUntypedHubBehaviorByContract()
Spec target section: Hub Runtime / Membership Type Semantics

HUB-MEMBERSHIP-002 — Hub Membership Is Graph And Object Identity Consistent
Contract statement: Hub membership must use OA object identity semantics and keep object-Hub membership tracking
consistent with the owning graph.
Rationale: Save/delete, weak membership tracking, event propagation, sync, replication, and graph traversal depend
on knowing which Hubs contain an object.
Source scope: Hub.add, remove, contains, indexOf, getObject, HubData, HubDataUnique, object-Hub membership
integration.
Related CODEX findings: Hub membership tracking issues from graph/hub scans.
Suggested unit tests: testObjectKnowsHubAfterAdd(), testObjectHubMembershipRemovedAfterRemove(),
testContainsUsesObjectIdentityContract()
Spec target section: Hub Runtime / Object-Hub Membership

HUB-MEMBERSHIP-003 — Duplicate Policy Is Deterministic
Contract statement: Hub duplicate behavior must follow the configured duplicate/add mode and must not create
accidental duplicate membership.
Rationale: Duplicate entries corrupt position lookup, active object behavior, saveAll/deleteAll, event counts, and
UI selection.
Source scope: Hub.add, insert, addElement, replace, HubData.isDupAllowAddRemove, HubDataUnique auto-create duplicate
flags.
Related CODEX findings: Duplicate/add behavior reviewed in Hub scans.
Suggested unit tests: testHubRejectsDuplicateWhenNoDups(), testHubDuplicatePolicyConsistentAcrossAddAndInsert(),
testReplaceDoesNotCreateDuplicateMembership()
Spec target section: Hub Runtime / Duplicate Semantics

HUB-MEMBERSHIP-004 — Collection Mutation Return Values Reflect Actual Change
Contract statement: Collection-style mutation methods must return whether Hub membership actually changed according
to the delegated operation contract.
Rationale: Callers use boolean mutation results for control flow, retry, UI state, and synchronization decisions.
Source scope: Hub.add, remove, addAll, removeAll, retainAll, collection-style mutation APIs.
Related CODEX findings: Hub.addAll/removeAll/retainAll return value finding: mutation result must reflect actual ch
anges.
Suggested unit tests: testAddAllEmptyReturnsFalse(), testAddAllExistingObjectsReturnsFalse(),
testRemoveAllNonMembersReturnsFalse(), testRetainAllNoChangeReturnsFalse()
Spec target section: Hub Runtime / Collection Mutation Semantics

HUB-ADDREMOVE-001 — Successful Add Or Insert Places Object Once At A Defined Position
Contract statement: A successful add/insert must place the object in exactly one defined Hub position and update
membership, indexes, change tracking, and events once.
Rationale: Prevents duplicate membership, duplicate events, index drift, and relationship persistence errors.
Source scope: Hub.add, addElement, insert, HubData vector state, HubEvent insert/add events.
Related CODEX findings: Add/insert correctness risks reviewed in graph/hub scans.
Suggested unit tests: testAddPlacesObjectAtEndOnce(), testInsertPlacesObjectAtRequestedPositionOnce(),
testAddDoesNotDoubleFireEvents()
Spec target section: Hub Runtime / Add Semantics

HUB-ADDREMOVE-002 — Remove Removes The Intended Object Or Position Only
Contract statement: Remove operations must remove the intended object or position and must not disturb unrelated
objects or indexes.
Rationale: Hub membership, order, active object, detail Hubs, and view Hubs depend on precise removal.
Source scope: Hub.remove overloads, removeAt, getAt/getObjectAt, indexOf, HubData vector state.
Related CODEX findings: Remove/change tracking issues reviewed.
Suggested unit tests: testRemoveObjectRemovesOnlyThatObject(), testRemoveAtRemovesOnlyThatPosition(),
testRemoveNonMemberLeavesHubUnchanged()
Spec target section: Hub Runtime / Remove Semantics

HUB-ADDREMOVE-003 — Clear And RemoveAll Have Defined Scope
Contract statement: Clear/removeAll operations must define whether they affect only Hub membership, linked
relationship state, change tracking, active object, datasource state, and/or object delete state.
Rationale: Clear-like operations have high blast radius and must not silently behave like deleteAll or vice versa.
Source scope: Hub.removeAll/clear-equivalent APIs, Hub.deleteAll, HubData change vectors, HubAO state.
Related CODEX findings: deleteAll/removeAll failure and false-success findings from graph/hub scans.
Suggested unit tests: testRemoveAllClearsMembershipWithoutDeletingObjectsByContract(),
testRemoveAllUpdatesAOByContract(), testRemoveAllFailureDoesNotPublishFalseSuccess()
Spec target section: Hub Runtime / Clear And RemoveAll Semantics

HUB-MOVE-001 — Move And Swap Preserve Membership Set
Contract statement: Move and swap operations may change order and positions but must not add, drop, or duplicate
objects.
Rationale: UI reorder, sort, sequence, and replication require order changes without membership corruption.
Source scope: Hub.move, swap, setPos, getPos, HubData vector state, HubEvent move events.
Related CODEX findings: Move/swap correctness risks reviewed.
Suggested unit tests: testMovePreservesMembershipSet(), testSwapPreservesMembershipSetAndPositions(),
testMoveUpdatesActivePositionByContract()
Spec target section: Hub Runtime / Order Mutation Semantics

HUB-ORDER-001 — Position And Index APIs Reflect Committed Hub Order
Contract statement: Position/index APIs must reflect committed Hub order and define behavior for out-of-range,
missing object, active position, and default position cases.
Rationale: UI controllers, detail Hubs, iteration, events, move/sort, and selection depend on stable indexing.
Source scope: Hub.getAt, getObjectAt, elementAt, getLast, indexOf, getPos overloads, setPos, getDefaultPos,
setDefaultPos.
Related CODEX findings: Ordering/index correctness reviewed in Hub scans.
Suggested unit tests: testGetAtReturnsObjectAtCommittedIndex(), testIndexOfMissingObjectUsesDefinedValue(),
testSetPosUpdatesActiveObjectByContract()
Spec target section: Hub Runtime / Position Semantics

HUB-AO-001 — Active Object Is Null Or A Current Hub Member
Contract statement: A Hub active object must be null or currently contained in that Hub, unless an explicit loading/
detail transition temporarily defines otherwise.
Rationale: UI binding, detail Hubs, generated controllers, active position, and property binding depend on AO
membership.
Source scope: Hub.getActiveObject, getAO, setActiveObject overloads, setAO overloads, resetAO, HubDataActive.
Related CODEX findings: AO/detail behavior reviewed.
Suggested unit tests: testSetAOToMemberSucceeds(), testSetAOToNonMemberUsesDefinedBehavior(),
testRemoveActiveObjectClearsOrMovesAOByContract()
Spec target section: Hub Runtime / Active Object Semantics

HUB-AO-002 — Active Object Position Matches Membership
Contract statement: Active object and active position must remain consistent with committed membership after add,
remove, clear, move, sort, refresh, and detail-master changes.
Rationale: Active position drives UI current row, detail Hub state, listeners, and controller behavior.
Source scope: Hub.getPos, setPos, getAO, setAO, resetAO, move, remove, sort/select integration.
Related CODEX findings: AO/detail and refresh failure risks from graph service scans.
Suggested unit tests: testRemoveActiveObjectMovesAOByContract(), testMoveActiveObjectUpdatesPosition(),
testRefreshFailurePreservesAOAndPosition()
Spec target section: Hub Runtime / Active Object Position Semantics

HUB-AO-003 — AO Events Observe New Authoritative AO
Contract statement: After-AO-change listeners must observe the new authoritative active object and dependent detail/
link state.
Rationale: Detail Hub refresh and UI binding often run from AO listeners.
Source scope: HubEvent, HubListener.afterChangeActiveObject, HubDataActive, HubDetail/master data.
Related CODEX findings: AO event ordering and detail update findings from graph/hub scans.
Suggested unit tests: testAfterAOChangeListenerSeesNewAO(), testDetailHubUsesNewAOWhenAOEventFires(),
testAOEventNotFiredForRejectedAOChange()
Spec target section: Hub Runtime / AO Event Ordering

HUB-DETAIL-001 — Detail Hub Membership Mirrors Master Link State
Contract statement: A detail Hub must contain exactly the objects linked to its current master object through its
configured link path and current master active object.
Rationale: Master/detail behavior is core OA model and UI semantics.
Source scope: Hub constructors with master Hub/object/link, HubDataMaster, HubDataUnique detail/shared state, root/
master/detail APIs.
Related CODEX findings: Detail/lazy-load and AO/detail findings from graph scans.
Suggested unit tests: testDetailHubContainsOnlyCurrentMasterChildren(), testChangingMasterRefreshesDetailHub(),
testOldMasterChildrenNotVisibleAfterMasterChange()
Spec target section: Hub Runtime / Detail Hub Semantics

HUB-MASTER-001 — Master Object Changes Rebind Detail State Before Observers
Contract statement: When a Hub’s master object or master active object changes, dependent detail/link state must be
rebound before observable after-events.
Rationale: Listeners and UI must not see stale master/detail state.
Source scope: HubDataMaster, HubDataActive, HubEvent AO/new-list events, master/detail constructors and binding
state.
Related CODEX findings: AO/detail ordering risks reviewed.
Suggested unit tests: testMasterChangeRebindsDetailBeforeEvent(), testDetailAfterMasterChangeUsesNewMaster(),
testOldMasterChildrenNotVisibleAfterMasterChange()
Spec target section: Hub Runtime / Master-Detail Semantics

HUB-LINK-001 — Link-Bound Hubs Maintain Reverse-Link Consistency
Contract statement: For link-bound Hubs, add/remove/clear operations must update the corresponding object reference,
reverse Hub relationship, or master/detail relationship according to metadata.
Rationale: Graph relationships must stay bidirectionally consistent for traversal, save/delete, sync, and
serialization.
Source scope: Hub link-related data in HubDataUnique/HubDataMaster, Hub.add/remove/setHub integration, HubData link
methods.
Related CODEX findings: Link/reverse-link consistency findings from graph/object/hub scans.
Suggested unit tests: testAddingChildSetsParentReference(),
testRemovingChildClearsParentReferenceWhenContractRequires(), testLinkBoundHubClearUpdatesReverseLinksByContract()
Spec target section: Hub Runtime / Link Semantics

HUB-SHARED-001 — Shared Hubs Share Only Intended Runtime State
Contract statement: Shared Hubs must share the intended membership/data-active structures while preserving any
contractually independent local state such as listeners, detail bindings, sort/filter views, or active object where
applicable.
Rationale: Shared Hubs support synchronized views without accidental cross-contamination of unrelated Hub behavior.
Source scope: Hub.getRealHub, clone, copyInto, HubDataUnique sharedHub/weakSharedHubs, HubData/HubDataActive.
Related CODEX findings: Shared Hub behavior reviewed in Hub and graph service scans.
Suggested unit tests: testSharedHubSeesSourceAdd(), testSharedHubSeesSourceRemove(),
testSharedHubDoesNotShareUnintendedListenerState()
Spec target section: Hub Runtime / Shared Hub Semantics

HUB-VIEW-001 — View And Filter Hubs Do Not Corrupt Source Membership
Contract statement: Filtered/view/projected Hubs must represent constrained views and must not mutate source
membership except through explicit source-authoritative operations.
Rationale: View projections must not silently alter real graph collections.
Source scope: Hub addHub/source view behavior, HubData selectWhereHub/selectWhereHubPropertyPath, filtered/view
integrations.
Related CODEX findings: Filtered/view behavior reviewed.
Suggested unit tests: testFilteredHubExcludesNonMatchingObject(),
testFilteredHubRemoveDoesNotSilentlyCorruptSource(), testSourceMutationUpdatesFilteredViewByContract()
Spec target section: Hub Runtime / View Hub Semantics

HUB-SORT-001 — Sorted Hub Order Matches Sort Contract
Contract statement: Sorted Hubs must maintain order according to configured sort property, comparator/listener,
ascending flag, and relevant property changes.
Rationale: UI, select order, reports, generated screens, and active position depend on stable sort behavior.
Source scope: HubData sortProperty/sortAsc/sortListener, HubListener.afterSort, Hub move/sort integrations.
Related CODEX findings: Sorting setup and after-sort failure findings from graph service scans.
Suggested unit tests: testSortedHubAddPlacesObjectInSortedPosition(), testSortedHubReordersOnSortPropertyChange(),
testAfterSortFiresOnlyAfterCompletedSort()
Spec target section: Hub Runtime / Sort Semantics

HUB-AUTOMATION-001 — Auto-Match Auto-Sequence And Auto-Add Preserve Graph Semantics
Contract statement: Auto-match, auto-sequence, and auto-add behavior must update only according to configured
properties, link metadata, duplicate policy, and Hub scope.
Rationale: Model-driven automation must not create wrong relationships, duplicate objects, or unstable sequence
values.
Source scope: HubData autoSequence/autoMatch, HubDataUnique autoCreate flags, Hub.setUniqueProperty, unique property
methods.
Related CODEX findings: Auto-match, auto-sequence, auto-add behavior reviewed.
Suggested unit tests: testAutoMatchLinksMatchingObject(), testAutoMatchDoesNotLinkNonMatchingObject(),
testAutoSequenceAssignsStableValues(), testAutoAddDoesNotCreateDuplicateObject()
Spec target section: Hub Runtime / Automation Semantics

HUB-SAVE-001 — SaveAll Saves The Intended Hub Graph Scope
Contract statement: saveAll must save the intended Hub contents, detail scope, and relationship changes, and must
not silently skip changed members.
Rationale: Hub saveAll is a high-level generated-application persistence operation.
Source scope: Hub.saveAll overloads, Hub.getChanged, Hub.setChanged, HubData change tracking.
Related CODEX findings: saveAll behavior reviewed in graph/hub scans.
Suggested unit tests: testSaveAllSavesChangedHubMembers(), testSaveAllPropagatesSaveFailure(),
testSaveAllHonorsCascadeRuleByContract()
Spec target section: Hub Runtime / SaveAll Semantics

HUB-DELETE-001 — DeleteAll Deletes Intended Members Only
Contract statement: deleteAll must delete the intended Hub members/scope and must not report success while leaving
authoritative members undeleted or silently deleting unrelated objects.
Rationale: deleteAll has high blast radius and affects cache, datasource, sync, Hub membership, and UI state.
Source scope: Hub.deleteAll, isDeletingAll, HubData membership/change state.
Related CODEX findings: deleteAll correctness and partial-failure findings from graph/hub scans.
Suggested unit tests: testDeleteAllDeletesEveryCurrentMember(), testDeleteAllDoesNotDeleteObjectsOutsideScope(),
testDeleteAllFailureIsVisibleToCaller()
Spec target section: Hub Runtime / DeleteAll Semantics

HUB-EVENT-001 — Hub After Events Fire After Completed Mutation
Contract statement: Hub after-events must fire only after the corresponding membership, order, active object, load,
select, save, delete, or property mutation has completed.
Rationale: Listeners must observe authoritative Hub state.
Source scope: HubEvent, HubListener, HubListenerAdapter, HubValidateListener, Hub add/remove/move/AO/save/delete/
select operations.
Related CODEX findings: Event ordering findings reviewed in Hub and graph scans.
Suggested unit tests: testAfterAddEventSeesObjectInHub(), testAfterRemoveEventSeesObjectRemoved(),
testAfterMoveEventSeesNewOrder()
Spec target section: Hub Runtime / Event Ordering

HUB-EVENT-002 — Before Events And Validation May Cancel By Contract
Contract statement: Before listeners, validation listeners, and allow/isValid callbacks may cancel or reject
operations according to contract, and rejected operations must not publish completed mutation state.
Rationale: Generated business rules may use listeners as validation or side-effect gates.
Source scope: HubListener before/allow/isValid methods, HubValidateListener, HubEvent cancel/response state.
Related CODEX findings: Listener failure/false-success risks reviewed.
Suggested unit tests: testBeforeAddValidationCanCancelAdd(), testBeforeRemoveValidationCanCancelRemove(),
testRejectedMutationDoesNotFireAfterEvent()
Spec target section: Hub Runtime / Listener Validation Semantics

HUB-EVENT-003 — Listener Failure Must Not Produce False Success
Contract statement: If listener failure aborts an operation by contract, the operation must not fire completion
events, report success, emit sync, or clear retry/change state.
Rationale: Listener failures can occur inside graph mutations and must not create false committed Hub state.
Source scope: HubEvent, HubListener, HubListenerAdapter, HubValidateListener, Hub mutation APIs.
Related CODEX findings: Listener/false-success risks from graph/hub scans.
Suggested unit tests: testListenerExceptionPreventsFalseAddSuccess(), testListenerExceptionDoesNotLeakEventState(),
testListenerExceptionDoesNotEmitCompletionSync()
Spec target section: Hub Runtime / Listener Failure Semantics

HUB-EVENT-004 — HubEvent Data Matches The Event Type
Contract statement: HubEvent fields such as Hub, object, object2, propertyName, oldValue, newValue, position, from/
to positions, cancel, and response must describe the event being delivered.
Rationale: Listeners depend on event payloads for validation, UI, sync, logging, and application behavior.
Source scope: HubEvent constructors/getters/setters/isProperty, HubListener event methods.
Related CODEX findings: Event payload correctness reviewed as part of event ordering scans.
Suggested unit tests: testAddEventContainsHubObjectAndPosition(), testPropertyEventContainsOldAndNewValue(),
testMoveEventContainsFromAndToPositions()
Spec target section: Hub Runtime / Event Payload Semantics

HUB-CHANGE-001 — Added And Removed Lists Reflect Completed Changes
Contract statement: Hub change tracking must record only completed adds/removes and must clear only when explicitly
requested or after successful save semantics.
Rationale: Persistence and sync use change lists to determine relationship updates.
Source scope: Hub.getChanged, setChanged, HubData vecAdd/vecRemove/getChanged/changeCount/trackChanges.
Related CODEX findings: Added/removed list issues reviewed.
Suggested unit tests: testAddedListRecordsSuccessfulAddOnly(), testRemovedListRecordsSuccessfulRemoveOnly(),
testClearChangesClearsBothListsByContract()
Spec target section: Hub Runtime / Change Tracking

HUB-CHANGE-002 — Reordering Is Not Membership Change
Contract statement: Move, swap, sort, active-object changes, and position changes must not create added/removed
change entries unless membership actually changes.
Rationale: Relationship persistence must not treat order-only changes as membership changes.
Source scope: Hub.move, swap, setPos, setAO, sort integrations, HubData change tracking.
Related CODEX findings: Move/sort behavior reviewed.
Suggested unit tests: testMoveDoesNotAddChangeEntry(), testSortDoesNotCreateRemovedEntries(),
testAOChangeDoesNotMarkMembershipChanged()
Spec target section: Hub Runtime / Change Tracking

HUB-LOAD-001 — Loaded Empty State Requires Authoritative Load Completion
Contract statement: A Hub may be marked loaded, loaded-empty, or select-all only after a successful authoritative
load/select proves that state.
Rationale: False loaded-empty state prevents retry and hides data.
Source scope: Hub.isMoreData, loadAllData, getCurrentSize, getSize, size, getLoadedSize, HubData select/loading/
selectAll state.
Related CODEX findings: Lazy-load and select-state corruption findings from graph/hub scans.
Suggested unit tests: testFailedHubLoadDoesNotMarkLoaded(), testEmptyHubMarkedLoadedOnlyAfterSuccessfulSelect(),
testPartiallyLoadedHubReportsLoadedSizeByContract()
Spec target section: Hub Runtime / Lazy Load Semantics

HUB-SELECT-001 — Hub Select Preserves Datasource Iterator Lifecycle
Contract statement: Hub select/loading must close datasource iterators and release select resources according to OA
iterator lifecycle rules on success and failure.
Rationale: Datasource and remote resources must not leak during Hub loading.
Source scope: Hub.loadAllData, HubData select state, select integration.
Related CODEX findings: Datasource iterator lifecycle findings mapped from datasource/select scans.
Suggested unit tests: testHubSelectClosesIteratorOnCompletion(), testHubSelectClosesIteratorOnException()
Spec target section: Hub Runtime / Select Resource Semantics

HUB-SERIAL-001 — Hub Serialization Preserves Membership Identity And Load Semantics
Contract statement: Hub serialization/readResolve must preserve membership object identity, ordering, shared/detail
semantics, active object state, and loaded/unloaded state according to contract.
Rationale: Serialized Hubs cross persistence, remote, sync, and tooling boundaries.
Source scope: Hub.readResolve, clone, toArray/toList, HubData/HubDataUnique/HubDataActive serialization-related
state.
Related CODEX findings: Hub serialization side-effect and duplicate readResolve findings from graph service scans.
Suggested unit tests: testSerializedHubPreservesMembershipOrder(), testHubReadResolveDoesNotDuplicateMembership(),
testSerializePartiallyLoadedHubHasDefinedLoadSemantics()
Spec target section: Hub Runtime / Serialization Semantics

HUB-SYNC-001 — Hub Mutations Emit Sync Only For Completed Semantic Changes
Contract statement: Add, insert, remove, move, sort, clear, saveAll, and deleteAll must emit sync/replication
messages only for completed semantic mutations.
Rationale: Prevents client/server and replication divergence.
Source scope: Hub mutation APIs, HubEvent/HubListener mutation events, graph sync integration.
Related CODEX findings: Sync/replication false-success issues reviewed in graph/hub scans.
Suggested unit tests: testFailedHubAddDoesNotEmitSync(), testCompletedHubRemoveEmitsSyncWhenEnabled(),
testFailedDeleteAllDoesNotEmitCompletedSync()
Spec target section: Hub Runtime / Sync Semantics

HUB-SYNC-002 — Client Hub Mutations Respect Server Authority
Contract statement: In client/server mode, operations requiring server authority must not locally claim
authoritative success before server acceptance when the contract requires gating.
Rationale: Prevents silent local/server Hub divergence.
Source scope: Hub add/remove/delete/save/select APIs through graph sync/client-server integration.
Related CODEX findings: Client/server authoritative ordering clarified in graph/hub service scans.
Suggested unit tests: testClientHubAddRejectedByServerDoesNotRemainLocalSuccess(),
testClientHubRemoveUsesAuthoritativeServerResult(), testClientDeleteAllRemoteFalseIsVisible()
Spec target section: Hub Runtime / Client Server Authority

HUB-TL-001 — Hub Operations Respect And Restore Runtime Context
Contract statement: Hub operations must honor OAThreadLocal loading, saving, deleting, sync, remote-thread, and
context flags and must restore any flags they set.
Rationale: Prevents recursion, unwanted sync, leaked event state, and cross-operation context contamination.
Source scope: Hub load/save/delete/mutation APIs, HubEvent dispatch, graph runtime integration.
Related CODEX findings: ThreadLocal restoration findings from runtime/graph/hub service scans.
Suggested unit tests: testHubLoadRestoresLoadingFlagAfterException(),
testHubDeleteAllRestoresDeletingFlagAfterException(), testHubEventDoesNotLeakRuntimeContext()
Spec target section: Hub Runtime / ThreadLocal Semantics

HUB-CONTEXT-001 — Hub Context And User Access Apply Consistently
Contract statement: Hub mutations, visibility, enablement, validation, and listener decisions that depend on
context/user access must use the active graph/runtime context.
Rationale: Generated applications depend on context-aware Hub access and UI behavior.
Source scope: HubListener allow/isValid methods, HubValidateListener, graph context/user-access integration.
Related CODEX findings: Context/user-access behavior reviewed in graph context and secure scans.
Suggested unit tests: testUserAccessBlocksUnauthorizedHubAdd(), testContextDoesNotLeakAcrossHubOperations(),
testHubListenerValidationUsesActiveContext()
Spec target section: Hub Runtime / Context Semantics

HUB-LIFECYCLE-001 — Hub Lifecycle Cleanup Releases Runtime Links And Listeners
Contract statement: Hub lifecycle cleanup must release or invalidate graph-owned references, listeners, shared data
links, detail/master bindings, select resources, and runtime flags according to ownership contract.
Rationale: Stale Hub references can leak graph/object state and continue receiving events after intended disposal.
Source scope: Hub.finalize/readResolve/clone, HubDataUnique weak shared/detail references, HubListener registration
state, select/shared/detail state.
Related CODEX findings: Shared/detail lifecycle cleanup risks from graph/hub service scans.
Suggested unit tests: testHubCleanupReleasesDetailBinding(), testDisposedHubDoesNotReceiveFutureEventsByContract(),
testSharedHubWeakReferencesDoNotRetainClosedHub()
Spec target section: Hub Runtime / Lifecycle Cleanup

HUB-INTERNAL-001 — Internal Bridge Access Does Not Become Public Hub Semantics
Contract statement: FriendAccess and internal bridge APIs may expose Hub internals to runtime infrastructure but
must not define application-facing Hub behavior.
Rationale: Internal access preserves performance and encapsulation while keeping the public Hub contract stable.
Source scope: HubInternalBridge, Hub.FriendAccess, HubData/FriendAccess classes, internal data classes.
Related CODEX findings: Internal surface and package-boundary findings from graph reviews.
Suggested unit tests: testApplicationSurfaceDoesNotRequireHubFriendAccess(),
testFriendAccessMutatesOnlyDocumentedInternalState(), testInternalBridgeDoesNotBypassGraphAuthority()
Spec target section: Hub Runtime / Internal API Boundary

HUB-FAILURE-001 — Partial Progress Is Allowed But Must Be Visible
Contract statement: Hub operations are not automatically atomic, but caller-visible exceptions or observable
incomplete state must signal incomplete operations.
Rationale: OA allows partial progress outside transactions while preserving caller ability to retry or reconcile.
Source scope: Hub add/remove/move/save/delete/select/sort/AO operations.
Related CODEX findings: Partial-progress semantics clarified during graph/hub scans.
Suggested unit tests: testHubOperationExceptionVisibleToCaller(), testPartialHubProgressCanBeReconciled(),
testFailedMutationLeavesDocumentedIncompleteState()
Spec target section: Hub Runtime / Failure Semantics

HUB-FAILURE-002 — Failed Operations Must Not Produce Completion Signals
Contract statement: Failed Hub operations must not fire after-events, emit sync/replication messages, clear retry/
change state, or mark loaded/deleted/saved/sorted state as complete.
Rationale: False success is the dangerous failure mode for Hub runtime behavior.
Source scope: Hub mutation/save/delete/load/sort/AO APIs, HubEvent/HubListener behavior, graph integration.
Related CODEX findings: False-success findings from graph/hub service scans; collection mutation return-value
finding.
Suggested unit tests: testFailedAddDoesNotFireAfterAdd(), testFailedDeleteAllDoesNotEmitCompletedSync(),
testFailedLoadDoesNotMarkHubLoaded()
Spec target section: Hub Runtime / False Success Prevention

HUB-RETRY-001 — Retry Remains Possible After Visible Failure
Contract statement: After visible Hub operation failure, state must remain retryable, refreshable, or explicitly
terminal according to contract.
Rationale: Applications can retry, refresh, reconcile, or use transaction boundaries after partial Hub failure.
Source scope: Hub load/save/delete/mutation/sort/select operations, change tracking, loaded state.
Related CODEX findings: Retry correctness reviewed in graph/hub scans.
Suggested unit tests: testFailedHubLoadCanRetry(), testFailedHubSaveAllCanRetry(),
testFailedHubDeleteAllCanRetryOrReconcile()
Spec target section: Hub Runtime / Retry Semantics

HUB-DETERMINISM-001 — Same Hub State Produces Same Observable Behavior
Contract statement: For the same graph, metadata, Hub state, runtime role, context, listener outcomes, and inputs,
Hub APIs must produce deterministic membership, active object, order, events, change tracking, load state, and sync
behavior.
Rationale: Deterministic Hub behavior is required for UI binding, generated apps, sync/replication, debugging, and
tests.
Source scope: Hub public/protected APIs, HubData, HubDataActive, HubDataMaster, HubDataUnique, HubEvent,
HubListener.
Related CODEX findings: Package-wide membership, AO, detail, event, change, sync, failure, and collection mutation
findings.
Suggested unit tests: testSameAddScenarioProducesSameMembershipAndEvents(),
testSameAOScenarioProducesSameDetailState(), testSameFailureScenarioProducesSameRetryableState()
Spec target section: Hub Runtime / Deterministic Hub Semantics

*/


