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

Undo Invariants

  ID: UNDO-STACK-001
  Contract statement: The undo/redo stack must preserve a committed sequence of undoable actions without silently
  losing, duplicating, reordering, or corrupting entries.
  Rationale: UI/controller/runtime undo depends on stack history matching the user-visible committed mutation history.
  Source locations: OAUndoManager.createUndoManager, OAUndoManager.add, OAUndoManager.addEdit, OAUndoManager.undo,
  future OA-native undo manager.
  Related CODEX findings: singleton/global fields unsynchronized; add(UndoableEdit) silently drops edits before
  manager creation; static global compound state can contaminate stack history.
  Suggested unit tests: testUndoStackPreservesCommittedOrder, testAddBeforeManagerCreationIsVisibleOrRejected,
  testUndoStackDoesNotDuplicateRedoAppliedEdit.
  Spec target section: Undo Runtime / Stack Integrity

  ID: UNDO-ORDER-001
  Contract statement: Undo must apply actions in reverse committed order, and redo must reapply undone actions in the
  original committed order.
  Rationale: Reversing an object graph mutation sequence out of order can break links, Hub membership, active object
  state, and derived/runtime state.
  Source locations: OAUndoManager.undo, OAUndoableEdit.undo, OAUndoableEdit.redo, compound edit handling.
  Related CODEX findings: compound boundaries can be split by nested starts; cross-thread compound contamination can
  mix action order.
  Suggested unit tests: testUndoAppliesCompoundChildrenInReverseOrder, testRedoAppliesCompoundChildrenInOriginalOrder,
  testNestedCompoundDoesNotSplitOuterOrder.
  Spec target section: Undo Runtime / Ordering Semantics

  ID: UNDO-PROP-001
  Contract statement: Property undo records must capture the old value and new value at the correct lifecycle moment:
  after old value is known, before the previous value is overwritten, and after the committed new value is known.
  Rationale: Restoring the wrong property value corrupts OAObject state and can affect datasource saves, filters,
  templates, sync, and replication.
  Source locations: OAUndoableEdit.createUndoablePropertyChange; OAObjectEventService undoable property capture path;
  future property-change capture service.
  Related CODEX findings: mutable value records are stored by reference; convenience property-change factory assumes
  wasChanged=true.
  Suggested unit tests: testPropertyUndoRestoresCapturedOldValue, testPropertyRedoRestoresCapturedNewValue,
  testMutableValuePropertyUndoUsesSnapshotWhenRequired.
  Spec target section: Undo Runtime / Property Restoration

  ID: UNDO-PROP-002
  Contract statement: Property undo must restore value semantics without changing object identity incorrectly.
  OAObject reference properties preserve identity; value properties preserve value snapshots according to their
  metadata/value contract.
  Rationale: OA distinguishes object identity from value equality. Undo must not replace canonical OAObjects
  incorrectly or restore stale mutated value objects.
  Source locations: OAUndoableEdit.prevValue/newValue; OAObject.setProperty; metadata property/link information.
  Related CODEX findings: mutable values captured by reference can drift before undo.
  Suggested unit tests: testReferencePropertyUndoPreservesOAObjectIdentity,
  testValuePropertyUndoRestoresIndependentSnapshot, testDateTimePropertyUndoDoesNotDriftAfterOriginalValueMutation.
  Spec target section: Undo Runtime / Value and Identity Semantics

  ID: UNDO-HUB-001
  Contract statement: Hub undo records must preserve the affected Hub, object identity, membership state, and intended
  position/order at the time of the committed change.
  Rationale: Hub membership/order is core OA graph state. Position-only or hub-agnostic records can restore the wrong
  object or wrong Hub.
  Source locations: OAUndoableEdit.createUndoableAdd, createUndoableRemove, createUndoableInsert, createUndoableMove,
  undo, redo.
  Related CODEX findings: MOVE records only positions, not object identity; equals ignores Hub identity for Hub-scoped
  edits.
  Suggested unit tests: testUndoAddRemovesSameObjectFromSameHub, testUndoRemoveReinsertsSameObjectAtOriginalPosition,
  testUndoMoveRestoresMovedObjectNotCurrentPositionOccupant, testHubEditEqualityIncludesHubIdentity.
  Spec target section: Undo Runtime / Hub Membership Semantics

  ID: UNDO-HUB-002
  Contract statement: Hub undo/redo must treat failed Hub operations as failed undo/redo, not as success. Boolean
  failure, invalid positions, missing objects, and rejected inserts/removes must be visible.
  Rationale: Hub APIs can return false/no-op when restoration does not apply. Ignoring that produces false-success
  undo records.
  Source locations: OAUndoableEdit.undo, OAUndoableEdit.redo; Hub.add, Hub.insert, Hub.remove, Hub.move.
  Related CODEX findings: Hub boolean restore results are ignored; undo/redo state flips before restoration succeeds.
  Suggested unit tests: testUndoAddFailsWhenRemoveReturnsFalse, testRedoInsertFailsWhenInsertReturnsFalse,
  testFailedHubUndoLeavesEditRetryable.
  Spec target section: Undo Runtime / Hub Failure Semantics

  ID: UNDO-AO-001
  Contract statement: Active-object undo/redo must restore the intended active object only if that object is still
  valid for the Hub, or must fail visibly when the target cannot be restored.
  Rationale: Active object drives UI state, detail Hubs, link behavior, and controller state. AO pointing outside Hub
  membership can corrupt detail state.
  Source locations: OAUndoableEdit.createUndoableChangeAO, OAUndoableEdit.undo, OAUndoableEdit.redo, Hub.setAO,
  HubAOService.
  Related CODEX findings: active-object undo can set AO to an object no longer in the Hub.
  Suggested unit tests: testUndoAOChangeRestoresPreviousMember, testUndoAOChangeFailsWhenPreviousObjectNoLongerInHub,
  testRedoAOChangeDoesNotSetNonMemberAO.
  Spec target section: Undo Runtime / Active Object Semantics

  ID: UNDO-GROUP-001
  Contract statement: Compound/grouped undo boundaries must represent one committed logical operation and must not
  silently include unrelated actions or split a user operation.
  Rationale: Generated applications depend on predictable user-level undo behavior for multi-step changes.
  Source locations: OAUndoManager.startCompoundEdit, endCompoundEdit, cancelCompoundEdit, addEdit,
  add(UndoableEdit[]); future grouped edit implementation.
  Related CODEX findings: compound edit state is static global; nested compound starts silently commit existing
  compound; endCompoundEdit can leave compound open when ignore is active.
  Suggested unit tests: testCompoundEditContainsOnlyActionsInScope, testNestedCompoundPolicyIsDeterministic,
  testCompoundEndClosesEvenDuringCleanup.
  Spec target section: Undo Runtime / Compound Edit Semantics

  ID: UNDO-GROUP-002
  Contract statement: Compound/grouped undo must be atomic or visibly incomplete on failure. If one child action
  fails, caller/observer must know the group did not fully restore state, and retry/recovery state must be valid.
  Rationale: Partial grouped restoration can leave object graphs in mixed before/after state. That is acceptable only
  when visible and recoverable.
  Source locations: compound edit handling in OAUndoManager; OAUndoableEdit.undo/redo; future OA-native group action.
  Related CODEX findings: state flips before restore success; add(UndoableEdit[]) can create incomplete compound
  groups if child additions fail.
  Suggested unit tests: testCompoundUndoFailureIsVisible, testCompoundRedoFailureDoesNotMarkGroupComplete,
  testCompoundPartialFailurePreservesRecoveryState.
  Spec target section: Undo Runtime / Group Failure Semantics

  ID: UNDO-IDENTITY-001
  Contract statement: Undo records must preserve OA identity boundaries: object identity, Hub identity, object key
  identity, and value identity must not be conflated.
  Rationale: OA/OG correctness depends on stable identity routing through cache, Hub membership, link relationships,
  and datasource persistence.
  Source locations: OAUndoableEdit.object, hub, prevValue/newValue, equality/coalescing logic; future undo record
  model.
  Related CODEX findings: Hub identity omitted from equality; MOVE lacks object identity; mutable values captured by
  reference.
  Suggested unit tests: testUndoRecordDistinguishesSameObjectInDifferentHubs,
  testUndoRecordDistinguishesReferenceObjectFromValueSnapshot, testMoveUndoRecordStoresMovedObjectIdentity.
  Spec target section: Undo Runtime / Identity Semantics

  ID: UNDO-CACHE-001
  Contract statement: Undo/redo must not corrupt cache identity, object keys, link consistency, or Hub/detail
  relationships. Restoration must use normal OA graph APIs or a documented authoritative path.
  Rationale: Undo is still a runtime mutation. It must obey object/cache/link invariants just like user edits, sync
  replay, or datasource load.
  Source locations: OAUndoableEdit.undo/redo; OAObject.setProperty; Hub.add/remove/insert/move/setAO; OAGraph object/
  hub services.
  Related CODEX findings: false-success Hub restore and AO restore can leave Hub/detail state inconsistent.
  Suggested unit tests: testUndoReferencePropertyMaintainsReverseLink, testUndoHubRemoveMaintainsDetailRelationship,
  testUndoDoesNotCreateDuplicateCachedObject.
  Spec target section: Undo Runtime / Cache and Link Consistency

  ID: UNDO-EVENT-001
  Contract statement: Event suppression or replay during undo/redo must be explicit. Undo/redo must not accidentally
  record itself as new undoable work, and must not suppress required OA runtime notifications unless contracted.
  Rationale: Undo changes may need UI refresh, Hub/detail updates, triggers, and sync decisions, but recursive undo
  capture corrupts history.
  Source locations: OAUndoManager.undo; missing redo suppression path; OAUndoManager.bIgnoreAll; OAObjectEventService
  undoable capture path.
  Related CODEX findings: redo is not wrapped with undo-capture suppression; undo clobbers pre-existing global ignore
  state.
  Suggested unit tests: testUndoDoesNotCreateNewUndoRecord, testRedoDoesNotCreateNewUndoRecord,
  testUndoStillFiresRequiredPropertyEvents, testUndoRestoresPreviousIgnoreAllState.
  Spec target section: Undo Runtime / Event and Capture Semantics

  ID: UNDO-FAIL-001
  Contract statement: Undo/redo failure must be caller-visible or otherwise observable, and must never silently appear
  successful.
  Rationale: Silent failed undo leaves UI/controller state believing old state was restored when object/Hub graph
  state differs.
  Source locations: OAUndoableEdit.undo, redo; OAUndoManager.undo/redo; future OA-native action apply methods.
  Related CODEX findings: Hub operation false returns ignored; no-op HOLDER significant by default; public undo/redo
  do not enforce sequencing.
  Suggested unit tests: testUndoFailureThrowsOrRecordsFailure, testRedoFailureThrowsOrRecordsFailure,
  testNoOpVisibleUndoRecordRejectedUnlessExplicit.
  Spec target section: Undo Runtime / Failure Visibility

  ID: UNDO-RETRY-001
  Contract statement: Retry after failed undo/redo must not reuse corrupted stack/action state. Action state must
  advance only after successful restoration or enter an explicit failed/incomplete state.
  Rationale: Caller-visible exception is acceptable incomplete-operation signaling only if retry remains meaningful.
  Source locations: OAUndoableEdit.bCanUndo; undo, redo; future edit state enum.
  Related CODEX findings: undo/redo flips state before restoration succeeds.
  Suggested unit tests: testFailedUndoCanBeRetried, testFailedRedoCanBeRetried,
  testFailedUndoDoesNotExposeCanRedoUntilUndoSucceeded.
  Spec target section: Undo Runtime / Retry Semantics

  ID: UNDO-TL-001
  Contract statement: ThreadLocal/context state set during undo capture or undo/redo application must be restored with
  try/finally.
  Rationale: Undo capture often runs on UI or worker threads that can later process unrelated OA work. Leaked context
  corrupts future undo capture, sync suppression, graph routing, or transaction state.
  Source locations: OAUndoManager.setIgnore, ignore, startCompoundEditForPropertyChanges, endCompoundEditForPropertyC
  hanges; OAThreadLocalService.startUndoable/endUndoable.
  Related CODEX findings: property-change capture lifecycle lacks scoped try/finally API; ignore counter map can leak
  thread state.
  Suggested unit tests: testUndoCaptureThreadLocalRestoredAfterException, testIgnoreScopeRestoredAfterException,
  testUndoDoesNotLeakThreadLocalState.
  Spec target section: Undo Runtime / ThreadLocal Cleanup

  ID: UNDO-CONCURRENT-001
  Contract statement: Concurrent undo/redo, edit capture, compound grouping, and stack mutation must either be
  serialized by contract or protected from corrupting stack/object graph state.
  Rationale: OA runtime has UI, background, remote, sync, and event threads. Undo scope must be explicitly owned.
  Source locations: OAUndoManager static fields; hmThreadCounter; compoundEdit; undoManager; addEdit; future manager
  ownership model.
  Related CODEX findings: static global compound edit state; unsynchronized ignore map; unsynchronized singleton/
  global flags.
  Suggested unit tests: testConcurrentAddAndUndoIsSerializedOrRejected,
  testConcurrentCompoundEditsDoNotCrossContaminate, testConcurrentIgnoreScopesAreThreadIsolated.
  Spec target section: Undo Runtime / Concurrency Semantics

  ID: UNDO-LIFECYCLE-001
  Contract statement: Undo records have a lifecycle: created, active/on-stack, undoing, undone, redoing, redone,
  failed, dead/discarded. Dead records must release references and must not be applied again.
  Rationale: Undo stacks can retain large OA object graphs. Lifecycle controls memory, retry, and validity.
  Source locations: OAUndoableEdit.die; OAUndoManager.cancelCompoundEdit; stack trimming/discard behavior inherited
  from current manager; future OA-native stack.
  Related CODEX findings: die() does not release strong references; cancelCompoundEdit() drops compound without
  calling die().
  Suggested unit tests: testDiscardedUndoRecordCannotBeApplied, testDieReleasesStrongReferences,
  testCancelCompoundDiesChildEdits.
  Spec target section: Undo Runtime / Record Lifecycle

  ID: UNDO-COALESCE-001
  Contract statement: Coalescing/replacement of undo records must only occur when the new record fully supersedes the
  old record for the same object, property, Hub, and operation scope.
  Rationale: Incorrect coalescing loses history or replaces the wrong restorable action.
  Source locations: OAUndoableEdit.replaceEdit, equals, hashCode; future coalescing policy.
  Related CODEX findings: replaceEdit checks wrong edit’s allow flag; equality ignores Hub identity.
  Suggested unit tests: testPropertyEditCoalescesOnlySameObjectAndProperty, testHubEditCoalescesOnlySameHubAndObject,
  testMoveEditDoesNotCoalesceDifferentMoves.
  Spec target section: Undo Runtime / Coalescing Semantics

  ID: UNDO-INTEGRATION-001
  Contract statement: Undo behavior must remain compatible with object, Hub, cache, metadata, sync, replication,
  queue, and runtime contracts. Undo/redo must use sanctioned mutation paths or explicitly document any bypass.
  Rationale: Undo is not isolated UI behavior in OA 4.0; it can affect generated app state, graph state, persistence,
  sync, and replication.
  Source locations: OAUndoableEdit.undo/redo; OAObjectEventService capture; OAThreadLocalService capture; OAGraph obj
  ect/hub services.
  Related CODEX findings: redo can recursively capture edits; Hub/AO false-success can corrupt downstream detail/
  runtime state.
  Suggested unit tests: testUndoPropertyChangeTriggersExpectedRuntimeNotifications,
  testUndoHubChangeMaintainsSyncSuppressionPolicy, testUndoDuringReplayDoesNotCreateUserUndoRecord.
  Spec target section: Undo Runtime / Cross-Package Compatibility

  Suggested Package-Level Spec Summary

  com.viaoa.undo is responsible for OA-native undo/redo semantics for generated and runtime applications. It captures
  restorable actions for OAObject property changes, Hub membership/order changes, active-object changes, and grouped
  user/runtime operations.

  It must guarantee:

  - Undo/redo restores the intended committed OA object and Hub state.
  - Undo records capture old/new property values, object identity, Hub identity, order, membership, and active-object
    state at the correct lifecycle moment.
  - Stack ordering is deterministic: undo reverses committed order, redo reapplies original order.
  - Compound/grouped edits preserve operation boundaries and do not mix unrelated work.
  - Failures are visible and do not advance action/stack state as if successful.
  - Retry after failed undo/redo remains possible or enters an explicit unretryable failed state.
  - Undo/redo event behavior is explicit: recursive undo capture is suppressed, while required OA runtime
    notifications are preserved according to contract.
  - ThreadLocal/context state is restored with try/finally.
  - Concurrent access is serialized, scoped, or visibly rejected.
  - Discarded/dead undo records release object graph references.
  - Undo behavior remains compatible with OAObject, Hub, cache, metadata, sync, replication, queue, datasource, and
    runtime invariants.

  It must never silently:

  - Drop undo records that should be captured.
  - Add duplicate undo records from undo/redo application itself.
  - Mark undo/redo successful when a property or Hub restore failed.
  - Restore the wrong object, wrong Hub, wrong property value, or wrong active object.
  - Leave compound edits open after cleanup.
  - Cross-contaminate compound edits across threads or operations.
  - Leak ThreadLocal undo capture or ignore state.
  - Retain discarded object graphs indefinitely.
  - Corrupt cache identity, reverse links, Hub/detail relationships, or sync/replication event semantics.

  Likely unit-test categories:

  - Stack push/undo/redo ordering.
  - Property old/new value capture and restoration.
  - Mutable value snapshot behavior.
  - Hub add/remove/insert/move restoration.
  - Active-object restoration and invalid target handling.
  - Compound/grouped edit ordering, nesting, cancellation, and failure.
  - Undo/redo recursive capture suppression.
  - ThreadLocal cleanup and scoped ignore behavior.
  - Retry after failed undo/redo.
  - Dead/discarded edit cleanup.
  - Cross-package integration with object events, Hub detail links, sync/replay suppression, and cache identity.

  Likely stress/failure-test categories:

  - Concurrent edit capture from UI/background threads.
  - Undo/redo while Hub membership changes externally.
  - Compound undo failure after several child edits applied.
  - Repeated property edits with coalescing.
  - Large object graph undo history trimming.
  - Undo/redo during sync/replay/load contexts.
  - Exception-throwing listeners during property or Hub restoration.


qqqqqqqqqqqqqq other

Architectural Assessment

  The package is conceptually useful and compact: one manager, one edit type, Swing-compatible contracts. The main
  production gap is that it still assumes mostly single-threaded UI-style usage, while OA 4.0 runtime semantics
  include Hub/object events, sync/replication, background work, and ThreadLocal capture. For 4.0, the undo package
  needs explicit ownership and state contracts more than more features.

  Invariant Risk Areas

  - Undo/redo application state is implicit in bCanUndo.
  - Compound edit scope is global instead of thread/operation scoped.
  - Ignore state is implemented as a static map instead of actual thread-local scoped state.
  - Undo capture cleanup relies on callers balancing start/end manually.
  - Hub move undo records position but not object identity.
  - Failure during undo/redo corrupts retry state.

  Top Production Risks

  1. Failed undo/redo corrupts edit state and prevents correct retry.
  2. Redo recursively records new undo edits.
  3. Cross-thread compound edit contamination.
  4. Unsynchronized ignore map/global state corrupts suppression behavior.
  5. Move undo restores the wrong Hub object after intervening changes.

  Hardening Recommendations

  - Flip bCanUndo only after successful undo/redo.
  - Override redo() in OAUndoManager with the same suppression discipline as undo().
  - Replace hmThreadCounter with ThreadLocal<Integer> and add scoped ignore helpers.
  - Make compound edit scope thread-owned or explicitly single-thread enforced.
  - Store object identity for move edits and verify before moving.
  - Add a scoped property-change capture API that guarantees cleanup in finally.
  - Add sequencing checks inside OAUndoableEdit.undo/redo.



*/


