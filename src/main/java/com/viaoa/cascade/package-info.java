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
 * <p>
 */
package com.viaoa.cascade;

/* CODEX Invariants

com.viaoa.cascade Invariants

  ID: CASCADE-TRAVERSE-001
  Contract statement: Cascade traversal must follow OA metadata-defined object graph semantics and must visit every
  reachable object required by the active cascade operation.
  Rationale: Save, delete, validate, load, serialization, and graph traversal all rely on cascade behavior to reach
  the correct related objects. Silent traversal gaps create missing persistence, validation, or graph updates.
  Source locations: OACascade; consumers in OAObjectSaveService, OAObjectDeleteService, OAObjectChangeService,
  OAObjectRecurseService, HubSaveService, HubDeleteService, OAFinder, OALoader.
  Related CODEX findings: deep save overflow path can mark an object visited but never save/defer it.
  Suggested unit tests: testCascadeVisitsAllRequiredReachableObjects, testDeepCascadeDoesNotSkipOverflowObject,
  testCascadeTraversalHonorsCascadeRule.
  Spec target section: Cascade Runtime / Traversal Semantics.

  ID: CASCADE-OWNERSHIP-001
  Contract statement: Cascade decisions must respect OALinkInfo ownership, cascade-save, cascade-delete, link type,
  transient, calculated, private, and used flags.
  Rationale: OA metadata is runtime truth for relationship traversal. Incorrect ownership/link handling can save/
  delete unrelated objects or miss owned detail objects.
  Source locations: OAObjectSaveService._save; OAObjectDeleteService.deleteChildren; OAObjectChangeService.getChanged;
  HubSaveService.saveAll; HubDeleteService.deleteAll; OALinkInfo.
  Related CODEX findings: none specific to package-level OACascade; related save/delete partial-failure notes exist in
  graph services.
  Suggested unit tests: testCascadeSaveFollowsOwnedLinksOnlyWhenConfigured, testCascadeDeleteDeletesOwnedChildren,
  testCascadeIgnoresTransientCalculatedPrivateUnusedLinks.
  Spec target section: Cascade Runtime / Ownership and Link Semantics.

  ID: CASCADE-RELATION-001
  Contract statement: One-to-one and one-to-many relationships must be cascaded according to their distinct metadata
  semantics. ONE links may require parent/reference ordering; MANY links require Hub traversal and link-table/change
  tracking cooperation.
  Rationale: ONE and MANY relationships have different persistence ordering, ownership, and Hub/detail consistency
  requirements.
  Source locations: OAObjectSaveService._save; OAObjectDeleteService.deleteChildren; HubSaveService.saveAll;
  HubDeleteService.deleteAll; HubAddRemoveService._updateHubAddsAndRemoves.
  Related CODEX findings: none observed directly in com.viaoa.cascade; graph save/delete notes illustrate failure
  risks.
  Suggested unit tests: testCascadeSaveNewOneReferenceBeforeOwner, testCascadeSaveManyHubMembersWhenCascadeEnabled,
  testCascadeDeleteOneToOneReferenceCleanup.
  Spec target section: Cascade Runtime / Relationship Cardinality Semantics.

  ID: CASCADE-HUB-001
  Contract statement: Hub cascade tracking must use Hub instance identity and must preserve Hub membership, detail-
  link consistency, ordering assumptions, and many-to-many add/remove tracking.
  Rationale: Hubs are object graph containers and relationship state. Cascade guards must not fail or conflate Hubs
  while save/delete/status operations depend on them.
  Source locations: OACascade.wasCascaded(Hub, boolean); HubSaveService.saveAll; HubDeleteService.deleteAll;
  HubStatusService.getChanged.
  Related CODEX findings: OACascade uses TreeSet<Hub> although Hub is not comparable; normal Hub cascade can throw
  before traversal starts.
  Suggested unit tests: testHubCascadeUsesIdentityTracking, testHubCascadeAcceptsNonComparableHub,
  testCascadeHubVisitedStateDoesNotConflateDistinctHubs.
  Spec target section: Cascade Runtime / Hub Traversal Semantics.

  ID: CASCADE-RECURSIVE-001
  Contract statement: Recursive and cyclic graph protection must prevent infinite traversal without suppressing
  legitimate reachable objects required by the operation.
  Rationale: OA graphs naturally contain reverse links, parent-child loops, shared Hubs, and recursive relationships.
  Cascade protection must be precise, not overbroad.
  Source locations: OACascade.wasCascaded(OAObject, boolean); OACascade.wasCascaded(Hub, boolean);
  OAObjectRecurseService; OAFinder; OALoader; save/delete/change services.
  Related CODEX findings: deep save overflow path can suppress a required object after marking it visited.
  Suggested unit tests: testCascadeTerminatesOnParentChildCycle,
  testCascadeStillVisitsDistinctReachableChildrenInCycle, testCascadeRecursivePathDoesNotSkipLegitimateBranch.
  Spec target section: Cascade Runtime / Recursive Protection.

  ID: CASCADE-VISITED-001
  Contract statement: Visited-set semantics must distinguish “already processed,” “newly claimed for processing,”
  “deferred for overflow,” and “ignored by explicit contract” where the owning operation depends on that distinction.
  Rationale: A single boolean is not enough for all cascade stages. Marking an object visited too early can hide
  unsaved/unvalidated/unprocessed objects from retry or overflow handling.
  Source locations: OACascade.wasCascaded(OAObject, boolean); OACascade.ignore; OACascade.addToOverflow;
  OAObjectSaveService.save.
  Related CODEX findings: deep save overflow path marks object visited before deciding to defer it.
  Suggested unit tests: testVisitedSetNewlyClaimedObjectCanBeDeferred, testIgnoredClassIsExplicitlyReportedAsSkipped,
  testVisitedStateDoesNotHideUnprocessedObject.
  Spec target section: Cascade Runtime / Visited-State Semantics.

  ID: CASCADE-DUP-001
  Contract statement: Cascade guards must prevent duplicate processing for the same object/Hub within a logical
  cascade operation, including under locked/concurrent use.
  Rationale: Duplicate cascade processing can duplicate save/delete/validate side effects, fire events twice, or
  produce nondeterministic traversal results.
  Source locations: OACascade.wasCascaded(OAObject, boolean); OACascade.wasCascaded(Hub, boolean); OALoader locked
  cascade usage.
  Related CODEX findings: locked wasCascaded(OAObject,true) performs check and add under separate locks, allowing two
  threads to both process the same object.
  Suggested unit tests: testLockedCascadeAllowsOnlyOneFirstVisitorPerObject,
  testLockedCascadeAllowsOnlyOneFirstVisitorPerHub, testDuplicateCascadeProcessingNotObservedUnderConcurrentLoad.
  Spec target section: Cascade Runtime / Duplicate Processing Prevention.

  ID: CASCADE-SAVE-001
  Contract statement: Cascade save must process all required reachable objects according to the cascade rule, preserve
  required parent/reference ordering, and never report success when required objects were skipped, deferred but not
  completed, or failed.
  Rationale: Save cascade controls persistence completeness and datasource referential integrity. Silent skips can
  leave unsaved children or dangling references.
  Source locations: OAObjectSaveService.save; OAObjectSaveService._save; HubSaveService.saveAll;
  OACascade.addToOverflow.
  Related CODEX findings: deep cascade overflow object skipped; existing graph CODEX notes on save failure/depth
  cleanup.
  Suggested unit tests: testCascadeSavePersistsOwnedChildren, testCascadeSaveCompletesOverflowQueue,
  testCascadeSaveFailureDoesNotReportSuccess.
  Spec target section: Cascade Runtime / Save Semantics.

  ID: CASCADE-DELETE-001
  Contract statement: Cascade delete must apply metadata-defined delete rules and must preserve retry visibility for
  any object whose delete fails. Partial progress may be allowed only when failure is visible and state remains
  coherent.
  Rationale: Delete cascade can remove children, Hub memberships, datasource rows, and sync state. Failed deletes must
  not leave the failed object appearing successfully deleted.
  Source locations: OAObjectDeleteService.delete; OAObjectDeleteService.deleteChildren; HubDeleteService.deleteAll;
  OACascade.
  Related CODEX findings: graph delete CODEX comments already document partial delete and retry semantics.
  Suggested unit tests: testCascadeDeleteDeletesOwnedChildrenAccordingToMetadata,
  testCascadeDeleteFailureLeavesFailedObjectRetryable, testCascadeDeletePartialProgressIsVisible.
  Spec target section: Cascade Runtime / Delete Semantics.

  ID: CASCADE-VALIDATE-001
  Contract statement: Cascade validation/change detection must inspect all required reachable objects according to the
  active rule and must not return false when a reachable required object is changed, invalid, or unvisited due to
  guard state.
  Rationale: Validation and changed-state checks drive save decisions, UI enablement, and runtime policy. False
  negatives can skip required persistence or validation.
  Source locations: OAObjectChangeService.getChanged; HubStatusService.getChanged; callback/validation consumers using
  OACascade.
  Related CODEX findings: none observed directly.
  Suggested unit tests: testCascadeChangedDetectsChangedOwnedChild, testCascadeChangedDoesNotSkipChangedHubMember,
  testCascadeValidationTerminatesOnCycleButReportsReachableInvalidChild.
  Spec target section: Cascade Runtime / Validation and Changed-State Semantics.

  ID: CASCADE-FAIL-001
  Contract statement: Cascade failure must be caller-visible or observable. A cascade operation must not silently
  appear complete after traversal, save, delete, validation, lock, overflow, or callback failure.
  Rationale: Cascade operations coordinate multiple runtime subsystems. Silent false-success can corrupt persistence,
  graph state, events, sync, or replication.
  Source locations: OACascade lock/visited methods; OAObjectSaveService; OAObjectDeleteService; HubSaveService;
  HubDeleteService; OALoader.
  Related CODEX findings: non-finally write-lock release can turn failure into future blocked traversal; deep overflow
  skip can look successful.
  Suggested unit tests: testCascadeLockFailureDoesNotLeaveHiddenStall, testCascadeFailurePropagatesToCaller,
  testCascadeDoesNotClaimCompleteWhenOverflowUnprocessed.
  Spec target section: Cascade Runtime / Failure Visibility.

  ID: CASCADE-RETRY-001
  Contract statement: Retry after failed cascade must not reuse corrupted traversal state, stale visited sets, stale
  overflow lists, inflated depth, or partially claimed objects that hide required work.
  Rationale: Cascade failures are allowed to be partial only if retry remains safe and visible. Stale guard state can
  make retry skip objects that were never successfully processed.
  Source locations: OACascade.treeObject, treeHub, depth, alOverflow; OAObjectSaveService.save; delete/save/load
  consumers.
  Related CODEX findings: save depth cleanup already CODEX-commented in graph service; overflow skip marks unsaved
  object visited.
  Suggested unit tests: testFailedCascadeRetryUsesFreshState, testFailedDeepSaveRetryDoesNotSkipOverflowObject,
  testCascadeDepthRestoredAfterException.
  Spec target section: Cascade Runtime / Retry Semantics.

  ID: CASCADE-TL-001
  Contract statement: Any ThreadLocal/OAThreadLocal/runtime context set during cascade processing must be restored
  with try/finally. OACascade itself should remain context-neutral.
  Rationale: Cascade runs inside save/delete/load/sync/replication flows where context leakage can alter event, sync,
  transaction, or loading behavior.
  Source locations: OACascade; consumers in OAObjectSaveService, OAObjectDeleteService, HubDeleteService, OALoader,
  sync/remote save-cache paths.
  Related CODEX findings: none directly in OACascade; related cleanup expectations exist in other packages.
  Suggested unit tests: testCascadeDeleteRestoresDeletingThreadLocal, testCascadeLoadRestoresSiblingHelper,
  testCascadeFailureDoesNotLeakThreadLocalContext.
  Spec target section: Cascade Runtime / ThreadLocal Context Semantics.

  ID: CASCADE-CONCURRENT-001
  Contract statement: Concurrent cascade operations must either use isolated cascade state per operation or
  synchronized atomic state transitions that preserve visited/overflow/depth correctness.
  Rationale: Shared cascade guards can be used by multi-threaded loaders and runtime services. Non-atomic state
  corrupts traversal determinism.
  Source locations: OACascade(boolean bUseLocks); wasCascaded; addToOverflow; ignore; OALoader multi-threaded
  traversal.
  Related CODEX findings: locked check/add is not atomic; write locks are not released in finally.
  Suggested unit tests: testConcurrentCascadeVisitedSetIsAtomic, testConcurrentOverflowAddIsSafe,
  testConcurrentCascadeDoesNotDeadlockAfterException.
  Spec target section: Cascade Runtime / Concurrency Semantics.

  ID: CASCADE-IDENTITY-001
  Contract statement: Cascade traversal must track OAObject identity using stable runtime identity semantics and must
  not conflate GUID identity, business-key identity, or distinct Hub instances.
  Rationale: Cascade guards decide whether processing is skipped. Wrong identity semantics can skip distinct objects
  or process the same logical object twice.
  Source locations: OACascade.wasCascaded(OAObject, boolean) using OAObject.getGuid; wasCascaded(Hub, boolean); cache/
  object identity services.
  Related CODEX findings: Hub tracking currently requires comparable ordering rather than identity tracking.
  Suggested unit tests: testCascadeTracksOAObjectsByStableGuid, testCascadeDoesNotConflateDistinctHubs,
  testCascadeIdentityUnaffectedByBusinessKeyChange.
  Spec target section: Cascade Runtime / Identity and Cache Semantics.

  ID: CASCADE-INTEGRATION-001
  Contract statement: Cascade behavior must remain compatible with object, Hub, metadata, path, load, save, delete,
  datasource, serialization, sync, replication, runtime, and transaction contracts.
  Rationale: Cascade is cross-cutting infrastructure. Incorrect cascade behavior can affect persistence, validation,
  serialization, sync/replay, and runtime graph consistency.
  Source locations: package com.viaoa.cascade; consumers across graph object/hub services, load, find, sync, remote/
  session save-cache, serialization/recurse paths.
  Related CODEX findings: graph save/delete comments illustrate cascade interaction risks; cascade guard findings
  affect Hub and loader consumers.
  Suggested unit tests: testCascadeSaveDeleteCompatibleWithDatasourceTransaction,
  testCascadeTraversalCompatibleWithSyncReplaySuppression, testCascadeDoesNotCorruptHubDetailLinks.
  Spec target section: Cascade Runtime / Cross-Package Integration.

  Suggested Package-Level Spec Summary

  - com.viaoa.cascade provides runtime traversal guard/state for OA object graph cascade operations.
  - It is responsible for preventing infinite recursion and duplicate processing while allowing every required
    reachable object/Hub to be processed according to the operation contract.
  - Cascade traversal must follow OA metadata ownership, link type, cascade-save, cascade-delete, detail, Hub, and
    relationship semantics.
  - Cascade guards must distinguish already processed state from newly claimed, deferred, ignored, or failed/
    incomplete state when the owning operation depends on that distinction.
  - Cascade save/delete/validate/load/recurse operations must not silently skip required children, details, Hubs, or
    references.
  - Cascade failure must be caller-visible or observable; silent false-success is not allowed.
  - Retry after cascade failure must use valid traversal state and must not skip objects because of stale visited/
    depth/overflow data.
  - Concurrent cascade use must perform atomic check/add state transitions and release locks reliably on success and
    failure.
  - Cascade processing must preserve OAObject identity, Hub membership, detail links, cache consistency, datasource
    persistence semantics, and sync/replication expectations.
  - OACascade itself should remain a lightweight traversal-state object and should not mutate object graph state
    directly.

  Likely unit-test categories:

  - metadata ownership/link traversal tests
  - one-to-one and one-to-many cascade tests
  - Hub/detail traversal and identity tests
  - recursive/cyclic graph traversal tests
  - visited-set and duplicate prevention tests
  - deep cascade overflow/deferred processing tests
  - save/delete/validate cascade completeness tests
  - failure and retry tests
  - concurrency/locked cascade tests
  - ThreadLocal cleanup integration tests
  - datasource/transaction/sync/replication cascade integration tests


*/


