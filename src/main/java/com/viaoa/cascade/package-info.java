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

//CODEX unit tests <todo>

/* CODEX Invariants

CASCADE-RUNTIME-001 — Graph Cascade Authority
Contract statement:
com.viaoa.cascade defines runtime cascade traversal state and guard semantics for OA graph-wide operations,
including save, delete, validation, changed-state detection, load/recurse traversal, serialization participation,
and graph visitation.
Rationale:
Cascade is graph-aware lifecycle coordination, not generic recursion. It determines which OAObjects and Hubs
participate in operations that affect persistence, validation, events, serialization, sync, replication, and runtime
graph consistency.
Source scope:
OACascade, consumers in object save/delete/change/recurse services, Hub save/delete/status services, loader/finder/
serialization/sync integration.
Related CODEX findings:
Existing package-info notes deep save overflow skip, Hub visited tracking failure, non-atomic locked visited checks,
and lock cleanup risks.
Suggested unit tests:
testCascadeVisitsAllRequiredReachableObjects(), testCascadeTraversalHonorsCascadeRule(),
testCascadeFailureDoesNotAppearComplete()
Spec target section:
Cascade Runtime / Core Responsibility

CASCADE-TRAVERSE-001 — Deterministic Traversal Scope
Contract statement:
A cascade operation must traverse the OAObject and Hub graph according to the active operation contract and
metadata-defined traversal scope; the same graph state, metadata, root, and cascade rule must produce deterministic
traversal eligibility.
Rationale:
Save, delete, validation, load, serialization, and graph traversal depend on reaching the same required objects
every time.
Source scope:
OACascade, OACascade.wasCascaded(OAObject, boolean), OACascade.wasCascaded(Hub, boolean), object/hub cascade
consumers.
Related CODEX findings:
Deep save overflow path can mark an object visited but never save/defer it.
Suggested unit tests:
testCascadeVisitsAllRequiredReachableObjects(), testCascadeTraversalRepeatableForSameGraphState(),
testDeepCascadeDoesNotSkipOverflowObject()
Spec target section:
Cascade Runtime / Traversal Semantics

CASCADE-METADATA-001 — Metadata-Driven Relationship Traversal
Contract statement:
Cascade traversal decisions must respect OA metadata, including OALinkInfo ownership, link type, cascade-save,
cascade-delete, transient, calculated, private, used, reverse-link, and relationship cardinality rules.
Rationale:
OA metadata is runtime truth for relationship traversal. Incorrect metadata handling can save/delete unrelated
objects or miss owned detail objects.
Source scope:
OACascade as traversal state; consumers in OAObjectSaveService, OAObjectDeleteService, OAObjectChangeService,
HubSaveService, HubDeleteService; OAObjectInfo and OALinkInfo metadata.
Related CODEX findings:
Related graph service notes illustrate save/delete partial-failure and ownership risks; no separate OACascade
metadata parser exists.
Suggested unit tests:
testCascadeSaveFollowsOwnedLinksOnlyWhenConfigured(), testCascadeDeleteDeletesOwnedChildrenAccordingToMetadata(),
testCascadeIgnoresTransientCalculatedPrivateUnusedLinks()
Spec target section:
Cascade Runtime / Metadata and Ownership Semantics

CASCADE-OWNERSHIP-001 — Owned Versus Referenced Boundaries
Contract statement:
Cascade must distinguish owned graph state from referenced graph state for each operation; owned/detail objects may
participate in save/delete/validation according to metadata, while non-owned references must not be cascaded as
owned state unless explicitly contracted.
Rationale:
Ownership determines persistence responsibility, delete reachability, validation scope, serialization depth, and
graph lifecycle boundaries.
Source scope:
OACascade, object save/delete/change consumers, Hub save/delete/status consumers, OALinkInfo owner and cascade
flags.
Related CODEX findings:
Existing package-info notes ownership/link semantics as core cascade risk area.
Suggested unit tests:
testCascadeDoesNotDeleteNonOwnedReference(), testCascadeSaveIncludesOwnedDetailObject(),
testReferencedObjectTraversalRequiresExplicitRule()
Spec target section:
Cascade Runtime / Ownership Boundaries

CASCADE-CARDINALITY-001 — One and Many Relationship Semantics
Contract statement:
ONE and MANY relationships must be cascaded according to their distinct runtime semantics: ONE links preserve
reference/parent ordering, while MANY links traverse Hub membership and preserve detail/link-table/change tracking
assumptions.
Rationale:
ONE and MANY links have different ordering, ownership, Hub membership, and datasource requirements.
Source scope:
OACascade as guard state; OAObjectSaveService, OAObjectDeleteService, HubSaveService, HubDeleteService, Hub add/
remove tracking consumers.
Related CODEX findings:
Existing package-info notes graph save/delete findings as examples of relationship failure risks.
Suggested unit tests:
testCascadeSaveNewOneReferenceBeforeOwnerWhenRequired(), testCascadeSaveManyHubMembersWhenCascadeEnabled(),
testCascadeDeleteOneToOneReferenceCleanup()
Spec target section:
Cascade Runtime / Relationship Cardinality Semantics

CASCADE-HUB-001 — Hub Traversal Identity
Contract statement:
Hub cascade tracking must use Hub instance identity and must preserve membership, detail-link consistency, ordering
assumptions, and many-to-many add/remove state for the owning operation.
Rationale:
Hubs are relationship containers and graph state. Cascade guards must not conflate distinct Hubs or fail before
traversal because a Hub lacks value ordering semantics.
Source scope:
OACascade.wasCascaded(Hub, boolean), HubSaveService, HubDeleteService, HubStatusService, Hub/detail traversal
consumers.
Related CODEX findings:
OACascade uses TreeSet<Hub> although Hub is not comparable; normal Hub cascade can throw before traversal starts.
Suggested unit tests:
testHubCascadeUsesIdentityTracking(), testHubCascadeAcceptsNonComparableHub(),
testCascadeHubVisitedStateDoesNotConflateDistinctHubs()
Spec target section:
Cascade Runtime / Hub Traversal Semantics

CASCADE-IDENTITY-001 — Object Identity Tracking
Contract statement:
Cascade object tracking must use stable OA runtime identity for visited/duplicate detection and must not conflate
GUID identity, datasource key identity, business key identity, or distinct live instances incorrectly.
Rationale:
Visited guards determine whether an object is skipped or processed. Wrong identity semantics can skip distinct
objects or process the same logical object twice.
Source scope:
OACascade.wasCascaded(OAObject, boolean), OAObject.getGuid(), cache/object identity consumers.
Related CODEX findings:
Existing package-info notes Hub tracking identity issues and object visited-state semantics.
Suggested unit tests:
testCascadeTracksOAObjectsByStableGuid(), testCascadeIdentityUnaffectedByBusinessKeyChange(),
testCascadeDoesNotConflateDistinctObjectInstancesWithDifferentGuid()
Spec target section:
Cascade Runtime / Identity and Cache Semantics

CASCADE-CYCLE-001 — Cycle Prevention Without Over-Suppression
Contract statement:
Recursive and cyclic graph protection must prevent infinite traversal while still allowing every legitimate
reachable object required by the active cascade operation to be processed.
Rationale:
OA graphs naturally include reverse links, parent-child loops, shared Hubs, and recursive relationships. Cycle
guards must be precise enough to avoid hiding required work.
Source scope:
OACascade.wasCascaded(OAObject, boolean), OACascade.wasCascaded(Hub, boolean), recursive object/hub traversal
consumers.
Related CODEX findings:
Deep save overflow path can suppress a required object after marking it visited.
Suggested unit tests:
testCascadeTerminatesOnParentChildCycle(), testCascadeStillVisitsDistinctReachableChildrenInCycle(),
testCascadeRecursivePathDoesNotSkipLegitimateBranch()
Spec target section:
Cascade Runtime / Recursive Protection

CASCADE-VISITED-001 — Visited-State Meaning
Contract statement:
Cascade visited state must distinguish already processed, newly claimed for processing, explicitly ignored,
deferred/overflow, and failed/incomplete state whenever the owning operation depends on that distinction.
Rationale:
A single visited boolean is not sufficient for all cascade lifecycle stages. Marking an object visited too early can
hide unsaved, undeleted, unvalidated, or unprocessed objects from retry.
Source scope:
OACascade.wasCascaded(OAObject, boolean), OACascade.wasCascaded(Hub, boolean), OACascade.ignore(Class),
OACascade.addToOverflow(Object), OACascade.getOverflowList().
Related CODEX findings:
Deep save overflow path marks object visited before deciding to defer it.
Suggested unit tests:
testVisitedSetNewlyClaimedObjectCanBeDeferred(), testIgnoredClassIsExplicitlyReportedAsSkipped(),
testVisitedStateDoesNotHideUnprocessedObject()
Spec target section:
Cascade Runtime / Visited-State Semantics

CASCADE-DUP-001 — Duplicate Processing Prevention
Contract statement:
Within one logical cascade operation, cascade guards must prevent duplicate processing of the same object or Hub
unless the owning operation explicitly permits revisiting through a distinct semantic path.
Rationale:
Duplicate processing can duplicate save/delete/validate side effects, fire events twice, corrupt Hub state, or
produce nondeterministic traversal results.
Source scope:
OACascade.wasCascaded(OAObject, boolean), OACascade.wasCascaded(Hub, boolean), OACascade(boolean bUseLocks), loader
and traversal consumers.
Related CODEX findings:
Locked wasCascaded(OAObject,true) performs check and add under separate locks, allowing two threads to both process
the same object.
Suggested unit tests:
testLockedCascadeAllowsOnlyOneFirstVisitorPerObject(), testLockedCascadeAllowsOnlyOneFirstVisitorPerHub(),
testDuplicateCascadeProcessingNotObservedUnderConcurrentLoad()
Spec target section:
Cascade Runtime / Duplicate Processing Prevention

CASCADE-DEPTH-001 — Depth and Overflow Semantics
Contract statement:
Cascade depth and overflow/deferred traversal state must preserve operation completeness: any object deferred
because of depth or overflow handling must remain visible and must be completed, retried, or reported incomplete by
the owning operation.
Rationale:
Depth limits can protect recursion, but they must not silently convert required work into skipped work.
Source scope:
OACascade.depthAdd(), OACascade.depthSubtract(), OACascade.getDepth(), OACascade.setDepth(int),
OACascade.addToOverflow(Object), OACascade.getOverflowList(), OACascade.clearOverflowList(), save/load consumers.
Related CODEX findings:
Deep cascade save can queue/skip incorrectly after object is marked visited; overflow object can be skipped while
operation appears successful.
Suggested unit tests:
testCascadeDepthRestoredAfterException(), testCascadeSaveCompletesOverflowQueue(),
testCascadeDoesNotClaimCompleteWhenOverflowUnprocessed()
Spec target section:
Cascade Runtime / Depth and Overflow Semantics

CASCADE-SAVE-001 — Cascade Save Completeness
Contract statement:
Cascade save must process all required reachable objects according to metadata and cascade rule, preserve required
reference ordering, and must not report success when required objects were skipped, deferred without completion, or
failed.
Rationale:
Save cascade controls persistence completeness and datasource referential integrity. Silent skips can leave unsaved
children or dangling references.
Source scope:
OACascade, OACascade.addToOverflow(Object), object save and Hub save consumers.
Related CODEX findings:
Deep cascade overflow object skipped; graph save notes on failure/depth cleanup.
Suggested unit tests:
testCascadeSavePersistsOwnedChildren(), testCascadeSaveCompletesOverflowQueue(),
testCascadeSaveFailureDoesNotReportSuccess()
Spec target section:
Cascade Runtime / Save Semantics

CASCADE-DELETE-001 — Cascade Delete Completeness
Contract statement:
Cascade delete must apply metadata-defined delete rules, preserve Hub/detail/link consistency, and keep failed or
incomplete deletes visible and retryable.
Rationale:
Delete cascade can remove child objects, Hub memberships, datasource rows, cache entries, sync state, and
replication records. Failure must not leave an object appearing successfully deleted when it was not.
Source scope:
OACascade, object delete and Hub delete consumers.
Related CODEX findings:
Existing package-info references graph delete CODEX comments for partial delete and retry semantics.
Suggested unit tests:
testCascadeDeleteDeletesOwnedChildrenAccordingToMetadata(), testCascadeDeleteFailureLeavesFailedObjectRetryable(),
testCascadeDeletePartialProgressIsVisible()
Spec target section:
Cascade Runtime / Delete Semantics

CASCADE-VALIDATE-001 — Validation and Changed-State Reachability
Contract statement:
Cascade validation and changed-state detection must inspect every required reachable object according to the active
operation rule and must not return clean/unchanged when a required reachable object is invalid, changed, or
unvisited due to guard state.
Rationale:
Validation and changed-state checks drive save decisions, UI enablement, runtime policy, and production data
integrity.
Source scope:
OACascade, object change/validation consumers, Hub status consumers, callback/validation consumers.
Related CODEX findings:
No direct source CODEX finding; existing package-info identifies this as a cascade contract.
Suggested unit tests:
testCascadeChangedDetectsChangedOwnedChild(), testCascadeChangedDoesNotSkipChangedHubMember(),
testCascadeValidationTerminatesOnCycleButReportsReachableInvalidChild()
Spec target section:
Cascade Runtime / Validation and Changed-State Semantics

CASCADE-NULL-001 — Null, Detached, Lazy, and Ignored State
Contract statement:
Cascade behavior for null references, unloaded/lazy references, transient links, deleted or detached objects, and
ignored classes must be explicit for each owning operation and must not be counted as successful traversal of
required runtime state unless that is the operation contract.
Rationale:
Cascade often crosses partially loaded graphs. Treating unavailable state as successful traversal can hide missing
validation, save, delete, or serialization work.
Source scope:
OACascade.ignore(Class), OACascade.wasCascaded(...), object/hub load/save/delete/validation/serialization consumers.
Related CODEX findings:
Existing package-info notes null, unloaded, lazy, transient, deleted, and detached behavior as package-specific
concerns.
Suggested unit tests:
testIgnoredClassIsSkippedOnlyByExplicitContract(), testNullReferenceDoesNotFailCascadeWhenOptional(),
testUnloadedRequiredLinkDoesNotAppearTraversed()
Spec target section:
Cascade Runtime / Availability and Ignored-State Semantics

CASCADE-FAIL-001 — Cascade Failure Visibility
Contract statement:
Cascade failure during traversal, save, delete, validation, locking, overflow/deferred processing, callback
execution, or metadata resolution must be caller-visible or operationally observable; cascade must not silently
report complete success after incomplete graph processing.
Rationale:
Cascade coordinates multiple runtime subsystems. Silent false-success can corrupt persistence, graph state, events,
sync, replication, or serialization.
Source scope:
OACascade lock/visited/depth/overflow methods and cascade consumers in object, Hub, loader, serialization, sync, and
replication services.
Related CODEX findings:
Non-finally write-lock release can turn failure into future blocked traversal; deep overflow skip can look
successful.
Suggested unit tests:
testCascadeLockFailureDoesNotLeaveHiddenStall(), testCascadeFailurePropagatesToCaller(),
testCascadeDoesNotClaimCompleteWhenOverflowUnprocessed()
Spec target section:
Cascade Runtime / Failure Visibility

CASCADE-RETRY-001 — Retry-Safe Traversal State
Contract statement:
Retry after failed cascade must not reuse corrupted visited sets, stale overflow lists, inflated depth, unreleased
locks, ignored-state leakage, or partially claimed objects that hide required work.
Rationale:
Partial cascade progress is acceptable only if retry remains safe and required incomplete work remains discoverable.
Source scope:
OACascade visited object/hub state, OACascade.depthAdd(), depthSubtract(), addToOverflow(Object),
clearOverflowList(), ignore(Class), save/delete/load consumers.
Related CODEX findings:
Overflow skip marks unsaved object visited; save depth cleanup related notes exist in graph services.
Suggested unit tests:
testFailedCascadeRetryUsesFreshState(), testFailedDeepSaveRetryDoesNotSkipOverflowObject(),
testCascadeDepthRestoredAfterException()
Spec target section:
Cascade Runtime / Retry Semantics

CASCADE-LOCK-001 — Locked Cascade Atomicity and Cleanup
Contract statement:
When OACascade is configured for locked/concurrent use, check-and-add visited transitions must be atomic, and every
acquired lock must be released with try/finally on success, failure, and early return.
Rationale:
Concurrent cascade is only safe if duplicate prevention and lock cleanup are deterministic under normal failure
conditions.
Source scope:
OACascade(boolean bUseLocks), OACascade.wasCascaded(OAObject, boolean), OACascade.wasCascaded(Hub, boolean),
OACascade.addToOverflow(Object), OACascade.ignore(Class).
Related CODEX findings:
Locked check/add is not atomic; write locks are not released in finally; lock failure can cause future blocked
traversal.
Suggested unit tests:
testConcurrentCascadeVisitedSetIsAtomic(), testLockedCascadeReleasesWriteLockWhenAddThrows(),
testConcurrentCascadeDoesNotDeadlockAfterException()
Spec target section:
Cascade Runtime / Locking and Concurrency

CASCADE-CONCURRENT-001 — Isolated Operation State
Contract statement:
Concurrent cascade operations must use isolated OACascade state per logical operation or explicitly synchronized
shared state that preserves visited, ignored, depth, overflow, and failure semantics.
Rationale:
Shared mutable cascade state can corrupt traversal determinism under multi-threaded loaders, background processing,
save-cache, or runtime graph services.
Source scope:
OACascade fields and public methods, multi-threaded loader/traversal consumers.
Related CODEX findings:
Locked check/add race; concurrent overflow/visited state risks.
Suggested unit tests:
testIndependentCascadeInstancesDoNotShareVisitedState(), testConcurrentOverflowAddIsSafeWhenLocked(),
testConcurrentCascadeDoesNotSkipRequiredObject()
Spec target section:
Cascade Runtime / Concurrency Semantics

CASCADE-TL-001 — Runtime Context Restoration
Contract statement:
Any ThreadLocal, OAThreadLocal, transaction, sync, loading, deleting, serializing, or runtime context set by cascade
consumers during cascade processing must be restored with try/finally; OACascade itself must remain traversal-state
oriented and context-neutral.
Rationale:
Cascade runs inside save/delete/load/sync/replication flows where context leakage can alter event, transaction,
sync, or loading behavior.
Source scope:
OACascade; consumers in object save/delete/load/change services, Hub services, loader services, sync/remote save-
cache paths, serialization consumers.
Related CODEX findings:
No direct OACascade ThreadLocal mutation; existing package-info notes related cleanup expectations in other
packages.
Suggested unit tests:
testCascadeDeleteRestoresDeletingThreadLocal(), testCascadeLoadRestoresLoadingContext(),
testCascadeFailureDoesNotLeakThreadLocalContext()
Spec target section:
Cascade Runtime / ThreadLocal Context

CASCADE-CALLBACK-001 — Callback and Operation Result Boundary
Contract statement:
Callbacks or visitors invoked during cascade must follow callback stop/failure semantics, and cascade traversal
success must remain distinct from the semantic success of the owning save, delete, validation, serialization, or
load operation.
Rationale:
A traversal can complete while the owning operation fails, and a callback can stop traversal intentionally. These
outcomes must not be conflated.
Source scope:
OACascade as traversal guard; callback/recurse/finder/loader/save/delete consumers.
Related CODEX findings:
Existing package-info notes callback/validation consumers using OACascade and partial-progress visibility
requirements.
Suggested unit tests:
testCascadeVisitorStopIsReportedByOwnerContract(), testCascadeTraversalSuccessDoesNotMaskSaveFailure(),
testCascadeCallbackExceptionIsVisible()
Spec target section:
Cascade Runtime / Callback and Result Boundaries

CASCADE-INTEGRATION-001 — Cross-Package Cascade Compatibility
Contract statement:
Cascade behavior must remain compatible with object, Hub, metadata, path, load, save, delete, datasource, cache,
transaction, serialization, sync, replication, callback, trigger, and graph/runtime contracts.
Rationale:
Cascade is cross-cutting runtime infrastructure. Incorrect traversal state can affect persistence, validation,
serialization, replay, cache identity, Hub/detail links, and observable graph state.
Source scope:
com.viaoa.cascade.OACascade and consumers across graph object/hub services, load/find/recurse services, datasource,
serialization, sync, replication, transaction, and callback packages.
Related CODEX findings:
Graph save/delete comments illustrate cascade interaction risks; cascade guard findings affect Hub and loader
consumers.
Suggested unit tests:
testCascadeSaveDeleteCompatibleWithDatasourceTransaction(),
testCascadeTraversalCompatibleWithSyncReplaySuppression(), testCascadeDoesNotCorruptHubDetailLinks()
Spec target section:
Cascade Runtime / Cross-Package Integration

*/

