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
 * Undo and redo support for OA applications.
 * <p>
 * This package provides an {@link javax.swing.undo.UndoManager}-compatible
 * implementation tailored to OA's domain model, Hubs, and property-change
 * architecture. It allows UI frameworks and controllers to perform
 * application-level undo/redo for:
 * <ul>
 *   <li>Hub operations (add, remove, insert, move),</li>
 *   <li>active-object changes,</li>
 *   <li>OAObject property changes,</li>
 *   <li>compound edit grouping,</li>
 *   <li>arbitrary custom undoable actions.</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 *
 * <h3>{@link com.viaoa.undo.OAUndoableEdit}</h3>
 * Represents a single reversible operation applied to a Hub or OAObject. It
 * implements {@link javax.swing.undo.UndoableEdit} and contains logic to undo
 * and redo:
 * <ul>
 *   <li>adding or removing an object from a Hub,</li>
 *   <li>moving or inserting objects,</li>
 *   <li>changing the active object of a Hub,</li>
 *   <li>changing an OAObject property.</li>
 * </ul>
 *
 * <h3>{@link com.viaoa.undo.OAUndoManager}</h3>
 * Extension of Swing's {@code UndoManager} with OA-specific features:
 * <ul>
 *   <li>thread-local ignore counters to suppress recursive edits,</li>
 *   <li>compound-edit support for grouping multiple operations,</li>
 *   <li>integration with {@code OAThreadLocalDelegate} to capture OAObject
 *       property changes automatically,</li>
 *   <li>global verbosity and ignore-all flags.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * Typical usage involves:
 * <ol>
 *   <li>creating a global OAUndoManager,</li>
 *   <li>wrapping user actions in undoable edits,</li>
 *   <li>using compound edits for multi-step changes,</li>
 *   <li>binding Undo/Redo menu items to the OAUndoManager.</li>
 * </ol>
 *
 * <p>
 * The undo subsystem is used extensively by OA GUI and OA Web controllers to
 * provide intuitive, reversible interactions with complex object graphs.
 */
package com.viaoa.undo;

/* CODEX Invariants

UNDO-STACK-001 — Undo Stack Integrity
Contract statement:
The undo/redo stack must preserve the committed sequence of undoable actions without silently losing, duplicating,
reordering, or corrupting entries.
Rationale:
UI, controller, and runtime undo depends on stack history matching the user-visible committed mutation history.
Source scope:
OAUndoManager.createUndoManager(), getUndoManager(), add(...), addEdit(...), undo(), redo behavior inherited from
UndoManager, compound edit handling.
Related CODEX findings:
singleton/global fields unsynchronized; add(UndoableEdit) silently drops edits before manager creation; static
global compound state can contaminate stack history.
Suggested unit tests:
undoStackPreservesCommittedOrder(), undoAddBeforeManagerCreationIsVisibleOrRejected(),
undoStackDoesNotDuplicateRedoAppliedEdit().
Spec target section:
Undo Runtime / Stack Integrity Semantics.

UNDO-ORDER-001 — Undo and Redo Ordering
Contract statement:
Undo must apply committed actions in reverse order, and redo must reapply undone actions in original committed
order.
Rationale:
Reversing graph mutations out of order can break properties, links, Hub membership, active object state, derived
values, and runtime event expectations.
Source scope:
OAUndoManager.undo(); OAUndoableEdit.undo(); OAUndoableEdit.redo(); compound edit handling.
Related CODEX findings:
compound boundaries can be split by nested starts; cross-thread compound contamination can mix action order.
Suggested unit tests:
undoAppliesCompoundChildrenInReverseOrder(), redoAppliesCompoundChildrenInOriginalOrder(),
nestedCompoundPolicyPreservesCommittedOrder().
Spec target section:
Undo Runtime / Ordering Semantics.

UNDO-RECORD-001 — Undo Record Lifecycle
Contract statement:
Undo records must have a deterministic lifecycle: created, active/on-stack, undoing, undone, redoing, redone,
failed/incomplete, dead/discarded.
Rationale:
Undo stacks can retain large OA object graphs; record lifecycle controls validity, retry behavior, and memory
retention.
Source scope:
OAUndoableEdit creation factories, canUndo(), undo(), canRedo(), redo(), die(), isSignificant(); OAUndoManager
cancel/discard/stack trimming behavior.
Related CODEX findings:
die() does not release strong references; cancelCompoundEdit() drops compound without calling die(); public undo/
redo sequencing concerns.
Suggested unit tests:
discardedUndoRecordCannotBeApplied(), undoDieReleasesStrongReferences(), cancelCompoundDiesChildEdits().
Spec target section:
Undo Runtime / Record Lifecycle Semantics.

UNDO-PROP-001 — Property Change Capture Timing
Contract statement:
Property undo records must capture old value, new value, property name, object identity, and changed-state context
at the correct lifecycle point for the committed property mutation.
Rationale:
Restoring the wrong property value corrupts OAObject state and can affect datasource saves, filters, templates,
sync, replication, and calculated runtime behavior.
Source scope:
OAUndoableEdit.createUndoablePropertyChange(...), prevValue/newValue fields, OAObject property-change capture paths,
OAObject.setProperty(...).
Related CODEX findings:
mutable value records are stored by reference; convenience property-change factory assumes wasChanged=true.
Suggested unit tests:
undoPropertyRestoresCapturedOldValue(), redoPropertyRestoresCapturedNewValue(),
undoPropertyChangedFlagRestoredByContract().
Spec target section:
Undo Runtime / Property Restoration Semantics.

UNDO-PROP-002 — Value Snapshot Versus Object Identity
Contract statement:
Undo records must distinguish OAObject reference identity from value-property snapshot semantics; reference
properties preserve canonical object identity, while value properties preserve the required value snapshot.
Rationale:
OA distinguishes object identity from value equality; undo must not replace canonical OAObjects incorrectly or
restore stale mutated value objects.
Source scope:
OAUndoableEdit.prevValue/newValue, createUndoablePropertyChange(...), OAObject.setProperty(...), metadata property/
link semantics.
Related CODEX findings:
mutable values captured by reference can drift before undo.
Suggested unit tests:
undoReferencePropertyPreservesOAObjectIdentity(), undoValuePropertyRestoresIndependentSnapshot(),
undoDateTimePropertyDoesNotDriftAfterOriginalValueMutation().
Spec target section:
Undo Runtime / Value and Identity Semantics.

UNDO-HUB-001 — Hub Membership Restoration
Contract statement:
Hub undo records must preserve affected Hub identity, object identity, membership state, and intended position/order
at the time of committed mutation.
Rationale:
Hub membership and ordering are core OA graph state; position-only or hub-agnostic records can restore the wrong
object or wrong Hub.
Source scope:
OAUndoableEdit.createUndoableAdd(...), createUndoableRemove(...), createUndoableInsert(...),
createUndoableMove(...), undo(), redo(), equals(), hashCode().
Related CODEX findings:
MOVE records only positions, not object identity; equals ignores Hub identity for Hub-scoped edits.
Suggested unit tests:
undoAddRemovesSameObjectFromSameHub(), undoRemoveReinsertsSameObjectAtOriginalPosition(),
undoMoveRestoresMovedObjectNotCurrentPositionOccupant(), hubEditEqualityIncludesHubIdentity().
Spec target section:
Undo Runtime / Hub Membership Semantics.

UNDO-HUB-002 — Hub Restore Failure Visibility
Contract statement:
Hub undo/redo must treat failed add, insert, remove, move, invalid position, missing object, or rejected operation
as failed undo/redo, not as success.
Rationale:
Ignoring Hub operation failure produces false-success undo records and leaves graph state different from UI/
controller state.
Source scope:
OAUndoableEdit.undo(), redo(); Hub.add(...), insert(...), remove(...), move(...).
Related CODEX findings:
Hub boolean restore results are ignored; undo/redo state flips before restoration succeeds.
Suggested unit tests:
undoAddFailsWhenRemoveReturnsFalse(), redoInsertFailsWhenInsertReturnsFalse(), failedHubUndoLeavesEditRetryable().
Spec target section:
Undo Runtime / Hub Failure Semantics.

UNDO-AO-001 — Active Object Restoration
Contract statement:
Active-object undo/redo must restore the intended active object only when that object is valid for the Hub, or must
fail visibly when the target cannot be restored.
Rationale:
Active object drives UI state, detail Hubs, link behavior, and controller state; pointing AO outside valid Hub
membership corrupts dependent runtime state.
Source scope:
OAUndoableEdit.createUndoableChangeAO(...), undo(), redo(); Hub.setAO(...), Hub active-object services.
Related CODEX findings:
active-object undo can set AO to an object no longer in the Hub.
Suggested unit tests:
undoAOChangeRestoresPreviousMember(), undoAOChangeFailsWhenPreviousObjectNoLongerInHub(),
redoAOChangeDoesNotSetNonMemberAO().
Spec target section:
Undo Runtime / Active Object Semantics.

UNDO-GROUP-001 — Compound Edit Boundaries
Contract statement:
Compound/grouped undo boundaries must represent one committed logical operation and must not silently include
unrelated actions or split a user/runtime operation.
Rationale:
Generated applications depend on predictable user-level undo behavior for multi-step changes.
Source scope:
OAUndoManager.startCompoundEdit(...), endCompoundEdit(), cancelCompoundEdit(), addEdit(...), add(UndoableEdit[]),
compoundEdit state.
Related CODEX findings:
compound edit state is static global; nested compound starts silently commit existing compound; endCompoundEdit can
leave compound open when ignore is active.
Suggested unit tests:
compoundEditContainsOnlyActionsInScope(), nestedCompoundPolicyIsDeterministic(),
compoundEndClosesEvenDuringCleanup().
Spec target section:
Undo Runtime / Compound Edit Semantics.

UNDO-GROUP-002 — Compound Restore Atomicity
Contract statement:
Compound/grouped undo and redo must be atomic or visibly incomplete; if any child action fails, caller/observer must
know the group did not fully restore state.
Rationale:
Partial grouped restoration can leave object graphs in mixed before/after state, which is acceptable only when
visible and recoverable.
Source scope:
OAUndoManager compound edit handling; OAUndoableEdit.undo()/redo(); add(UndoableEdit[]).
Related CODEX findings:
state flips before restore success; add(UndoableEdit[]) can create incomplete compound groups if child additions
fail.
Suggested unit tests:
compoundUndoFailureIsVisible(), compoundRedoFailureDoesNotMarkGroupComplete(),
compoundPartialFailurePreservesRecoveryState().
Spec target section:
Undo Runtime / Group Failure Semantics.

UNDO-IDENTITY-001 — Undo Identity Boundaries
Contract statement:
Undo records must preserve OA identity boundaries: object identity, Hub identity, object key identity, reference
identity, and value identity must not be conflated.
Rationale:
OA/OG correctness depends on stable identity routing through cache, Hub membership, link relationships, datasource
persistence, sync, and replication.
Source scope:
OAUndoableEdit.object, hub, prevValue, newValue, equals(...), hashCode(...), replaceEdit(...), move records.
Related CODEX findings:
Hub identity omitted from equality; MOVE lacks object identity; mutable values captured by reference.
Suggested unit tests:
undoRecordDistinguishesSameObjectInDifferentHubs(), undoRecordDistinguishesReferenceObjectFromValueSnapshot(),
moveUndoRecordStoresMovedObjectIdentity().
Spec target section:
Undo Runtime / Identity Semantics.

UNDO-LINK-001 — Link, Cache, and Graph Consistency
Contract statement:
Undo/redo must not corrupt cache identity, object keys, reverse links, Hub/detail relationships, metadata
cardinality, or graph ownership; restoration must use normal OA graph APIs or a documented authoritative path.
Rationale:
Undo is still runtime mutation and must obey the same object/cache/link invariants as user edits, sync replay,
datasource load, and graph services.
Source scope:
OAUndoableEdit.undo(), redo(); OAObject.setProperty(...); Hub add/remove/insert/move/setAO; graph/object/hub service
boundaries.
Related CODEX findings:
false-success Hub restore and AO restore can leave Hub/detail state inconsistent.
Suggested unit tests:
undoReferencePropertyMaintainsReverseLink(), undoHubRemoveMaintainsDetailRelationship(),
undoDoesNotCreateDuplicateCachedObject().
Spec target section:
Undo Runtime / Cache and Link Consistency Semantics.

UNDO-EVENT-001 — Event and Capture Semantics
Contract statement:
Event suppression, event publication, and undo-capture suppression during undo/redo must be explicit; undo/redo must
not accidentally record itself as new undoable work or suppress required OA runtime notifications unless contracted.
Rationale:
Undo changes may need UI refresh, Hub/detail updates, triggers, and sync decisions, but recursive undo capture
corrupts history.
Source scope:
OAUndoManager.undo(), redo behavior, setIgnore(...), ignore(...), setIgnoreAll(...), OAUndoableEdit.undo()/redo(),
OAObject property-change capture paths.
Related CODEX findings:
redo is not wrapped with undo-capture suppression; undo clobbers pre-existing global ignore state.
Suggested unit tests:
undoDoesNotCreateNewUndoRecord(), redoDoesNotCreateNewUndoRecord(), undoStillFiresRequiredPropertyEvents(),
undoRestoresPreviousIgnoreAllState().
Spec target section:
Undo Runtime / Event Publication and Capture Semantics.

UNDO-FAIL-001 — Undo/Redo Failure Visibility
Contract statement:
Undo/redo failure must be caller-visible or otherwise observable and must never silently appear successful.
Rationale:
Silent failed undo leaves UI/controller state believing old graph state was restored when object/Hub state differs.
Source scope:
OAUndoableEdit.undo(), redo(); OAUndoManager.undo(); OAUndoManager add/edit APIs.
Related CODEX findings:
Hub operation false returns ignored; no-op HOLDER significant by default; public undo/redo do not enforce
sequencing.
Suggested unit tests:
undoFailureThrowsOrRecordsFailure(), redoFailureThrowsOrRecordsFailure(),
noOpVisibleUndoRecordRejectedUnlessExplicit().
Spec target section:
Undo Runtime / Failure and False-Success Prevention.

UNDO-RETRY-001 — Retry After Failed Restore
Contract statement:
Retry after failed undo/redo must not reuse corrupted stack or action state; action state must advance only after
successful restoration or enter an explicit failed/incomplete state.
Rationale:
Visible failure is useful only if retry/recovery state remains meaningful.
Source scope:
OAUndoableEdit.bCanUndo, canUndo(), canRedo(), undo(), redo(); OAUndoManager stack behavior.
Related CODEX findings:
undo/redo flips state before restoration succeeds.
Suggested unit tests:
failedUndoCanBeRetried(), failedRedoCanBeRetried(), failedUndoDoesNotExposeCanRedoUntilUndoSucceeded().
Spec target section:
Undo Runtime / Retry Semantics.

UNDO-SEQUENCE-001 — Undo/Redo Sequencing Enforcement
Contract statement:
Public undo and redo entry points must enforce canUndo/canRedo sequencing and must reject invalid lifecycle
transitions visibly.
Rationale:
Applying redo before a successful undo or undoing a dead/invalid record can corrupt action state and Object Graph
state.
Source scope:
OAUndoableEdit.canUndo(), undo(), canRedo(), redo(); OAUndoManager undo/redo pathways.
Related CODEX findings:
OAUndoableEdit public undo() and redo() do not enforce canUndo/canRedo.
Suggested unit tests:
undoRejectsWhenCanUndoFalse(), redoRejectsWhenCanRedoFalse(), deadEditCannotBeUndoneOrRedone().
Spec target section:
Undo Runtime / Lifecycle Sequencing Semantics.

UNDO-TL-001 — ThreadLocal and Runtime Context Restoration
Contract statement:
ThreadLocal, ignore counters, undo-capture state, sync suppression, graph routing, transaction context, and runtime
context set during undo capture or undo/redo application must be restored with try/finally.
Rationale:
Undo capture often runs on UI, worker, event, remote, or sync threads that later process unrelated OA work.
Source scope:
OAUndoManager.setIgnore(...), ignore(...), getIgnore(), startCompoundEditForPropertyChanges(...),
endCompoundEditForPropertyChanges(), setIgnoreAll(...), OAThreadLocal undoable integration.
Related CODEX findings:
property-change capture lifecycle lacks scoped try/finally API; ignore counter map can leak thread state; undo
clobbers global ignore state.
Suggested unit tests:
undoCaptureThreadLocalRestoredAfterException(), ignoreScopeRestoredAfterException(),
undoDoesNotLeakThreadLocalState().
Spec target section:
Undo Runtime / ThreadLocal Cleanup Semantics.

UNDO-CONCURRENT-001 — Undo Scope Concurrency
Contract statement:
Concurrent undo/redo, edit capture, compound grouping, ignore scopes, and stack mutation must either be serialized
by contract or protected from corrupting stack and graph state.
Rationale:
OA runtime can involve UI, background, remote, sync, trigger, callback, and event threads; undo ownership must be
explicit.
Source scope:
OAUndoManager static fields, hmThreadCounter, compoundEdit, undoManager, addEdit(...), start/end/cancel compound
APIs, global flags.
Related CODEX findings:
static global compound edit state; unsynchronized ignore map; unsynchronized singleton/global flags.
Suggested unit tests:
concurrentAddAndUndoIsSerializedOrRejected(), concurrentCompoundEditsDoNotCrossContaminate(),
concurrentIgnoreScopesAreThreadIsolated().
Spec target section:
Undo Runtime / Concurrency Semantics.

UNDO-COALESCE-001 — Edit Coalescing Semantics
Contract statement:
Coalescing or replacement of undo records may occur only when the new record fully supersedes the old record for the
same object, property, Hub, operation type, and scope.
Rationale:
Incorrect coalescing loses history or replaces the wrong restorable action.
Source scope:
OAUndoableEdit.replaceEdit(...), addEdit(...), equals(...), hashCode(...), bAllowReplace.
Related CODEX findings:
replaceEdit checks the wrong edit’s allow flag; equality ignores Hub identity.
Suggested unit tests:
propertyEditCoalescesOnlySameObjectAndProperty(), hubEditCoalescesOnlySameHubAndObject(),
moveEditDoesNotCoalesceDifferentMoves().
Spec target section:
Undo Runtime / Coalescing Semantics.

UNDO-SIGNIFICANCE-001 — Significant Edit Semantics
Contract statement:
Undo records exposed to users as significant must perform a meaningful reversible operation or explicitly represent
a documented marker action.
Rationale:
Visible no-op undo records create false-success UI behavior and confuse command history.
Source scope:
OAUndoableEdit.createUndoable(...), isSignificant(), HOLDER edit type, presentation name APIs.
Related CODEX findings:
HOLDER edits are significant no-op undo records by default.
Suggested unit tests:
holderEditIsInsignificantUnlessCustomBehaviorDefined(), significantEditPerformsReversibleOperation(),
visibleNoOpUndoRecordRejectedUnlessExplicit().
Spec target section:
Undo Runtime / Significant Edit Semantics.

UNDO-PRESENT-001 — Presentation Name Stability
Contract statement:
Undo/redo presentation names must describe the committed action represented by the record or group and must not
drift across replacement, grouping, undo, or redo.
Rationale:
UI and tooling depend on presentation names to explain reversible graph operations to users.
Source scope:
OAUndoableEdit.getPresentationName(), setPresentationName(...), getUndoPresentationName(),
getRedoPresentationName(), setName(...), getName(); OAUndoManager compound presentation names.
Related CODEX findings:
none observed.
Suggested unit tests:
undoPresentationNameMatchesCommittedAction(), redoPresentationNameMatchesCommittedAction(),
compoundPresentationNameRepresentsGroupedOperation().
Spec target section:
Undo Runtime / User-Visible Command Semantics.

UNDO-LIFETIME-001 — Reference Release on Discard
Contract statement:
Dead, discarded, trimmed, or cancelled undo records must release strong references to OAObjects, Hubs, property
values, and presentation data when no longer applicable.
Rationale:
Undo stacks can retain large Object Graphs; discarded records must not leak runtime graph state.
Source scope:
OAUndoableEdit.die(); OAUndoManager.cancelCompoundEdit(); UndoManager trim/discard behavior.
Related CODEX findings:
die() does not release strong references; cancelCompoundEdit() drops compound without calling die().
Suggested unit tests:
dieReleasesObjectHubAndValueReferences(), cancelCompoundDiesChildEdits(), trimmedUndoRecordsReleaseReferences().
Spec target section:
Undo Runtime / Reference Lifecycle Semantics.

UNDO-PARTIAL-001 — Partial Restore Visibility
Contract statement:
If undo/redo applies some but not all intended graph restoration, partial progress must be observable and the
record/stack must not report complete success.
Rationale:
Partial restore can leave object graphs in mixed states affecting UI, events, triggers, datasource, sync, and
replication.
Source scope:
OAUndoableEdit.undo()/redo(); OAUndoManager compound edits; Hub/property/AO restore operations.
Related CODEX findings:
Hub operation false returns ignored; group partial failure concerns; state flips before restore success.
Suggested unit tests:
partialUndoFailureIsObservable(), partialRedoFailureDoesNotMarkComplete(),
partialCompoundRestorePreservesRecoveryState().
Spec target section:
Undo Runtime / Partial Progress Semantics.

UNDO-TRANSACTION-001 — Transaction and Persistence Boundary
Contract statement:
Undo/redo completion only means the in-memory reversible mutation was applied according to undo contract; it must
not imply datasource commit, transaction commit, save success, delete success, sync delivery, or replication
convergence.
Rationale:
Undo is a runtime mutation boundary, not the authority for persistence or distributed success.
Source scope:
OAUndoableEdit.undo()/redo(); OAUndoManager; boundaries with transaction, datasource, object, hub, sync,
replication, and graph packages.
Related CODEX findings:
none observed directly; false-success Hub/AO restore can affect downstream runtime state.
Suggested unit tests:
undoSuccessDoesNotImplyDatasourceCommit(), undoSuccessDoesNotImplySyncDelivery(),
undoPropertyRestoreInsideTransactionUsesTransactionContract().
Spec target section:
Undo Runtime / Transaction and Runtime Boundary Semantics.

UNDO-INTEGRATION-001 — Cross-Package Runtime Compatibility
Contract statement:
Undo behavior must remain compatible with object, Hub, cache, metadata, event, trigger, callback, transaction,
datasource, sync, replication, queue, remote, and graph contracts.
Rationale:
Undo is not isolated UI behavior in OA 4.0; it mutates live Object Graph state and can affect persistence,
distributed runtime behavior, events, and generated applications.
Source scope:
OAUndoableEdit.undo()/redo(); OAUndoManager capture/group/ignore APIs; OAObject property-change capture; Hub
mutation APIs; runtime service boundaries.
Related CODEX findings:
redo can recursively capture edits; Hub/AO false-success can corrupt downstream detail/runtime state.
Suggested unit tests:
undoPropertyChangeTriggersExpectedRuntimeNotifications(), undoHubChangeMaintainsSyncSuppressionPolicy(),
undoDuringReplayDoesNotCreateUserUndoRecord().
Spec target section:
Undo Runtime / Cross-Package Compatibility Semantics.

*/


