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
 * Transaction management for OA's thread-local execution model.
 * <p>
 * The classes in this package provide lightweight transactional semantics for
 * operations performed within a single thread. A transaction:
 * <ul>
 *   <li>is created and started explicitly,</li>
 *   <li>is associated with the current thread via
 *       {@link com.viaoa.object.OAThreadLocalDelegate},</li>
 *   <li>notifies registered {@link com.viaoa.transaction.OATransactionListener}
 *       instances on commit or rollback,</li>
 *   <li>supports batch modes for datasources that optimize multi-row
 *       operations,</li>
 *   <li>can override datasource read-only restrictions when appropriate.</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * OATransaction tx = new OATransaction();
 * tx.setUseBatch(true);
 * tx.start();
 * try {
 *     // perform work that involves datasources or OAObjects
 *     tx.commit();
 * }
 * catch (Exception e) {
 *     tx.rollback();
 * }
 * }</pre>
 *
 * <h2>Listeners</h2>
 * Datasources and other subsystems register a listener with the active
 * transaction. Listeners receive:
 * <ul>
 *   <li>{@code commit}</li>
 *   <li>{@code rollback}</li>
 *   <li>{@code executeOpenBatches}</li>
 * </ul>
 * allowing them to finalize their work atomically.
 *
 * <p>
 * Transactions in OA are intentionally simple: they track only in-memory
 * behavior and event sequencing. Database-level transaction boundaries are
 * controlled by the underlying datasource implementations.
 */
package com.viaoa.transaction;

/* CODEX Invariants

1. Package Summary

  com.viaoa.transaction is a lightweight thread-local transaction coordination package. In OA 4.0 terms, it should
  define the runtime boundary for grouped datasource/object/Hub work, participant enlistment, batch flushing, commit/
  rollback sequencing, and cleanup of transaction-scoped state.

  Current implementation is small:

  - OATransaction: transaction context, thread-local binding, listener list, batch/write flags, key/value participant
    storage.
  - OATransactionListener: participant callback interface with commit, rollback, and executeOpenBatches.

  The package currently behaves more like an advisory enlistment context than a strict transaction coordinator. The
  invariants below describe what must be true for production-grade OA 4.0 semantics.

  2. Core Concepts

  - transaction: one runtime boundary for coordinated work on one thread.
  - participant: datasource/subsystem/listener enlisted through OATransactionListener.
  - transaction stage/state: explicit lifecycle phase such as ACTIVE, COMMITTING, COMMITTED, etc.
  - active work: OAObject/Hub/datasource operations while transaction is current.
  - commit phase: stage where participants finalize uncommitted work.
  - rollback phase: stage where participants undo/discard active uncommitted work.
  - completion phase: transition into successful or failed terminal outcome.
  - terminal state: no further commit/rollback allowed.
  - close/cleanup: release transaction references and clear thread-local regardless of outcome.
  - rollback-only: active transaction marker that forbids commit.
  - nested transaction behavior: either explicitly rejected or stack-based.
  - thread ownership: transaction belongs to the thread that started it.
  - listener/callback behavior: participant lifecycle callbacks, not casual app events.

  3. Transaction Stage Invariants

  A. ACTIVE

  Allowed:

  - enlist participants
  - perform OAObject/Hub/datasource work
  - store participant-scoped transaction data
  - mark rollback-only
  - execute rollback

  Forbidden:

  - commit after rollback-only
  - replace another active transaction unless nested semantics are explicit
  - complete from non-owner thread
  - mutate immutable options if frozen at start

  State may change:

  - participants may move from not enlisted to enlisted
  - transaction may move to COMMITTING or ROLLING_BACK

  Current confidence: Low
  Code: OATransaction.start, addTransactionListener, put/get/remove.

  B. COMMITTING

  Allowed:

  - execute commit preparation/open batches
  - commit eligible participants
  - record per-participant commit success/failure
  - enter COMMITTED or COMMIT_FAILED

  Forbidden:

  - new active work
  - listener mutation unless explicitly allowed
  - normal rollback of already-committed participants
  - clearing thread-local before commit outcome is known

  Participant state must be tracked:

  - ENLISTED
  - BATCH_EXECUTED or PREPARED
  - COMMITTED
  - COMMIT_FAILED

  Current confidence: Low
  Code: commit, executeOpenBatches.

  C. COMMITTED

  Rules:

  - successful terminal transaction outcome
  - no further commit/rollback
  - participant commit callbacks must not repeat
  - cleanup/close must release thread-local and transaction-scoped references

  Current confidence: Low
  Gap: no terminal state.

  D. COMMIT_FAILED

  Rules:

  - diagnostic/recovery state, not normal rollback state
  - participants already committed must not receive ordinary rollback
  - participants not yet committed may need recovery/cleanup
  - errors must preserve root cause and participant status

  Current confidence: Low
  Gap: no distinction between commit failure and active rollback.

  E. ROLLING_BACK

  Allowed:

  - rollback participants that have uncommitted active work
  - continue cleanup callbacks even after failures
  - aggregate rollback failures

  Forbidden:

  - rollback already-committed participants as if active work still exists
  - commit after rollback begins

  Current confidence: Low
  Code: rollback.

  F. ROLLED_BACK

  Rules:

  - terminal rollback outcome
  - no further commit/rollback
  - cleanup/close must run
  - transaction should not be reusable

  Current confidence: Low

  G. ROLLBACK_FAILED

  Rules:

  - terminal or recovery-needed failure state
  - retain diagnostics and participant failure status
  - cleanup still required where safe

  Current confidence: Low

  H. CLOSED

  Rules:

  - no lifecycle operations
  - thread-local must not reference transaction
  - transaction-scoped listener/map references should be cleared or intentionally retained for diagnostics
  - closed transaction cannot be restarted

  Current confidence: Low

  4. General Invariants

  A. Lifecycle / State Machine

  TX-LIFE-001: Transaction lifecycle must be explicit and monotonic.
  Why: prevents double commit, rollback after commit, and reuse.
  Locations: OATransaction.start, commit, rollback.
  Confidence: Low.
  Gap: no state field.

  TX-LIFE-002: Completion must be exactly once.
  Why: participant side effects must not repeat.
  Locations: commit, rollback.
  Confidence: Low.
  Gap: double calls allowed.

  TX-LIFE-003: A transaction must be active before commit/rollback.
  Why: prevents stale object completion.
  Locations: isStarted, commit, rollback.
  Confidence: Low.
  Gap: commit/rollback do not call isStarted.

  B. Participant Coordination

  TX-PART-001: Each participant must have its own lifecycle state.
  Why: prevents contradictory commit then rollback.
  Locations: OATransactionListener, commit, rollback.
  Confidence: Low.
  Gap: no per-listener state.

  TX-PART-002: Participant enlistment must be stable once commit/rollback begins.
  Why: prevents missed or duplicate callbacks.
  Locations: addTransactionListener, removeTransactionListener.
  Confidence: Low.
  Gap: mutable ArrayList.

  TX-PART-003: Null participants must be rejected immediately.
  Why: delayed NPE at boundary is avoidable.
  Locations: addTransactionListener.
  Confidence: Low.

  C. Commit / Rollback Correctness

  TX-COMMIT-001: Commit must execute required preparation/batch phase before final commit.
  Why: prevents skipped batched writes.
  Locations: commit, executeOpenBatches, OATransactionListener.executeOpenBatches.
  Confidence: Low.
  Gap: commit does not call executeOpenBatches.

  TX-COMMIT-002: Commit failure must not be treated as ordinary active rollback.
  Why: some participants may already be committed.
  Locations: commit, rollback.
  Confidence: Low.

  TX-ROLLBACK-001: Rollback applies only to participants with rollback-eligible uncommitted work.
  Why: prevents rollback after commit.
  Locations: rollback.
  Confidence: Low.

  D. Rollback-Only

  TX-RBONLY-001: Any participant/work failure during ACTIVE must be able to mark transaction rollback-only.
  Why: prevents false-success commit after known failure.
  Locations: absent.
  Confidence: Low.

  TX-RBONLY-002: commit() must fail or route to rollback when rollback-only is set.
  Why: rollback-only must be enforceable.
  Locations: absent.
  Confidence: Low.

  E. Nested Transactions

  TX-NEST-001: Nested transaction behavior must be explicitly rejected or stack-managed.
  Why: current start() overwrites thread-local.
  Locations: start, OAThreadLocalService.setTransaction.
  Confidence: Low.

  TX-NEST-002: Inner completion must not clear parent transaction state.
  Why: parent work must remain transactional.
  Locations: commit, rollback.
  Confidence: Low.

  F. ThreadLocal / Thread Ownership

  TX-TL-001: Only the owner thread may commit/rollback a transaction.
  Why: OA transaction context is documented as single-threaded.
  Locations: start, commit, rollback.
  Confidence: Low.
  Gap: owner thread not recorded.

  TX-TL-002: A transaction may clear thread-local only if it is the active transaction.
  Why: stale transactions must not clear newer ones.
  Locations: commit, rollback.
  Confidence: Low.

  TX-TL-003: ThreadLocal transaction counter must reflect actual active bindings.
  Why: getTransaction() short-circuits on global count.
  Locations: OAThreadLocalService.setTransaction/getTransaction.
  Confidence: Low.

  G. Listener / Callback Exceptions

  TX-CB-001: Participant/fail-fast callbacks may cancel before state commits.
  Why: BEFORE semantics require visible cancellation.
  Locations: no explicit BEFORE phase.
  Confidence: Low.

  TX-CB-002: Observer/cleanup callbacks must not prevent remaining observers from running.
  Why: cleanup/finalization must be best-effort complete.
  Locations: commit, rollback, executeOpenBatches.
  Confidence: Low.
  Gap: first exception stops loop.

  TX-CB-003: Multiple listener failures must be aggregated.
  Why: root cause plus cleanup failures must remain visible.
  Locations: commit, rollback, executeOpenBatches.
  Confidence: Low.

  H. Ordering / Determinism

  TX-ORDER-001: Commit participant order must be deterministic.
  Why: datasource dependencies may rely on order.
  Locations: ArrayList registration order.
  Confidence: Medium.

  TX-ORDER-002: Rollback order must be explicitly defined, likely reverse participant order if dependencies exist.
  Why: cleanup usually unwinds dependencies.
  Locations: rollback.
  Confidence: Low.
  Gap: same order as commit, no contract.

  I. Error Handling / Aggregation

  TX-ERR-001: Commit failure must leave transaction in COMMIT_FAILED, not silently closed as success.
  Why: recovery semantics differ from rollback.
  Locations: commit.
  Confidence: Low.

  TX-ERR-002: Rollback failure must leave transaction in ROLLBACK_FAILED with diagnostics.
  Why: hidden rollback failure is production-critical.
  Locations: rollback.
  Confidence: Low.

  J. Resource Cleanup

  TX-CLEAN-001: Cleanup must run regardless of commit/rollback outcome.
  Why: thread-local and participant resources must not leak.
  Locations: commit, rollback.
  Confidence: Medium for thread-local clear, Low for participant cleanup.

  TX-CLEAN-002: Transaction-scoped map/listeners should be cleared or terminally retained by explicit diagnostic
  policy.
  Why: prevents memory leaks and stale reuse.
  Locations: hm, al.
  Confidence: Low.

  K. Integration

  TX-INT-001: Datasource transaction checks must see consistent active transaction state.
  Why: OADataSource.isInTransaction, batching, write override depend on thread-local.
  Locations: OADataSource.isAllowingBatch, isInTransaction, getIgnoreWrites.
  Confidence: Low due counter drift.

  TX-INT-002: Save/delete cascades that claim transaction cooperation must not rely on advisory-only semantics.
  Why: partial save/delete consistency depends on actual participant coordination.
  Locations: transaction package plus object/datasource integration.
  Confidence: Low.

  TX-INT-003: Sync/replication replay must not inherit stale transaction context from pooled/remote threads.
  Why: replay boundaries must be deterministic.
  Locations: OAThreadLocal.transaction, OATransaction.start/commit/rollback.
  Confidence: Low.

  5. Listener / Callback Semantics

  Current package conflicts with the proposed OA-wide policy:

  - BEFORE participants: not represented as a phase. executeOpenBatches appears preparation-like, but is public and
    not integrated into commit.
  - DURING observers: not represented. All listeners are treated as fail-fast participants.
  - AFTER cleanup/finalization: not represented. commit/rollback loops stop on first exception, so cleanup observers
    can be skipped.

  Current behavior:

  - commit: fail-fast loop, clears thread-local in finally.
  - rollback: fail-fast loop, clears thread-local in finally.
  - executeOpenBatches: fail-fast loop, no lifecycle guard.

  Spec direction:

  - beforeCommit / prepare participants may fail-fast.
  - commit participant completion must track participant state.
  - afterCommit / cleanup observers must continue after exceptions.
  - rollback must be participant-state-aware.
  - close cleanup must always execute and aggregate failures.

  6. Failure Modes

  - double commit: listeners receive duplicate commit.
  - double rollback: listeners receive duplicate rollback.
  - rollback after partial commit: committed participant receives rollback.
  - participant contradictory signals: commit then rollback.
  - commit succeeds but transaction reports rollback: possible under caller-managed catch/rollback without state.
  - rollback-only ignored: no rollback-only state.
  - stale ThreadLocal transaction: missing owner/state cleanup.
  - nested transaction corrupts parent state: inner start overwrites parent.
  - cleanup skipped after exception: first listener exception stops loop.
  - listener exception prevents cleanup: no after/cleanup phase.
  - datasource participant left open: later listeners skipped.
  - sync/replication sees inconsistent boundary: stale or missing thread-local transaction.
  - transaction reused after completion: no terminal guard.
  - open batches execute outside transaction: public unguarded executeOpenBatches.

  7. Test Recommendations

  Lifecycle:

  - testCommitRequiresActiveTransactionOwner
  - testDoubleCommitRejected
  - testRollbackAfterCommitRejected
  - testCommitAfterRollbackRejected
  - testTransactionCannotRestartAfterClosed

  Commit failure:

  - testCommitFailureAfterFirstParticipantCommittedCreatesCommitFailedState
  - testRollbackAfterPartialCommitDoesNotRollbackCommittedParticipant
  - testCommitFailurePreservesRootCauseAndParticipantStates

  Rollback:

  - testRollbackDuringActiveCallsOnlyRollbackEligibleParticipants
  - testRollbackFailureAggregatesAllParticipantFailures
  - testRollbackUsesDocumentedOrdering

  Rollback-only:

  - testRollbackOnlyPreventsCommit
  - testParticipantCanMarkRollbackOnly
  - testRollbackOnlyCommitRoutesToRollbackOrThrows

  Nested:

  - testNestedStartRejected
  - testNestedStackRestoresParentAfterInnerCompletion if stack semantics chosen

  ThreadLocal:

  - testCommitDoesNotClearDifferentActiveTransaction
  - testTransactionCounterTracksNullToNonNullTransitions
  - testCrossThreadCommitRejected
  - testThreadLocalClearedAfterCommitFailureClose

  Listener semantics:

  - testBeforeParticipantFailureCancelsCommit
  - testAfterCleanupListenersContinueAfterException
  - testDuringObserversAggregateFailures
  - testListenerMutationDuringCompletionRejectedOrSnapshotSafe

  Batches:

  - testCommitExecutesOpenBatchesBeforeCommit
  - testExecuteOpenBatchesCannotRunAfterRollback
  - testExecuteOpenBatchesIdempotency

  Cleanup:

  - testCloseClearsListenersAndTransactionMap
  - testCompletedTransactionCannotStoreNewParticipantState

  8. Hardening Recommendations

  - Add explicit TransactionStage enum.
  - Add owner thread field.
  - Add per-participant state object instead of raw listener list.
  - Add rollbackOnly.
  - Split phases: prepare/before, commit, afterCommit, rollback, afterRollback, close.
  - Make executeOpenBatches internal to commit or guard it by stage.
  - Add active-owner checks before commit/rollback/cleanup.
  - Add terminal-state enforcement.
  - Snapshot participant list or lock participant mutation during completion.
  - Aggregate exceptions using suppressed exceptions.
  - Clear ThreadLocal only in controlled close/cleanup.
  - Make OAThreadLocalService.setTransaction transition-aware.
  - Add diagnostics: transaction id, owner thread, start stack, stage, participant count, participant states.
  - Add stale transaction detection for long-lived active transactions.

  9. Open Questions

  - Is OATransaction intended to be strict transaction coordinator or advisory enlistment context?
  - Should nested transactions be rejected or stack-based?
  - Should commit failure allow rollback, or enter terminal COMMIT_FAILED recovery state?
  - Are datasource batch operations supposed to be automatically executed by commit()?
  - Should rollback order be reverse enlistment order?
  - Are listeners participants only, or are observer/cleanup callbacks also expected?
  - Should completed transaction objects retain diagnostic state or clear all references?
  - Should setAllowWritesIfDsIsReadonly be immutable after start?
  - Should sync/replication replay suppress or participate in active transactions?
  - Should OATransactionListener be expanded or replaced with stage-aware participant callbacks?


qqqqqqq othere codex ...
A. Architectural Assessment

  The model is conceptually sound as a lightweight thread-local transaction coordinator, but the lifecycle is not
  production-hard yet. Ownership is implicit, nesting is undefined, completion is not terminal, and listener semantics
  are too coarse for infrastructure code. Boundaries are not deterministic enough for save/delete cascades, datasource
  batching, sync/replay, or replication readiness.

  B. Invariant Risk Areas

  - A transaction must be active before commit/rollback.
  - A transaction must complete exactly once.
  - Only the active owner transaction may clear thread-local state.
  - Nested transaction behavior must be explicit.
  - Batch execution ordering must be part of commit or removed from the contract.
  - Rollback-only state must prevent commit.
  - All cleanup/finalization listeners must run even after earlier failures.
  - Transaction-scoped state must not outlive terminal completion unless explicitly retained.

  C. Top Production Risks

  1. Outer transaction lost by nested start().
  2. Active transaction hidden by ThreadLocal counter drift.
  3. Batched writes skipped on commit.
  4. Partial listener commit/rollback leaves datasources inconsistent.
  5. Double completion duplicates side effects.
  6. Cross-thread completion executes transaction participants in the wrong runtime context.

  D. Hardening Recommendations

  - Add State enum and owner-thread field.
  - Add rollbackOnly.
  - Add active-owner assertions in commit/rollback.
  - Define nested behavior: reject or stack.
  - Execute open batches in a documented commit phase.
  - Snapshot listeners before iteration.
  - Aggregate exceptions for cleanup/after phases.
  - Clear listener/map state on terminal completion.
  - Add diagnostics for stale completion, nested start, double completion, counter drift, and skipped batch execution.

*/









