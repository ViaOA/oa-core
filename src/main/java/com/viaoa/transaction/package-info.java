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

TX-LIFECYCLE-001 — Explicit Transaction Lifecycle
Contract statement:
A transaction must move through an explicit monotonic lifecycle: new/not-started, active, committing, committed,
commit-failed, rolling-back, rolled-back, rollback-failed, and closed or equivalent documented states.
Rationale:
Transaction lifecycle state prevents double commit, double rollback, rollback after commit, commit after rollback,
stale completion, and reuse after terminal completion.
Source scope:
OATransaction.start(), commit(), rollback(), isStarted().
Related CODEX findings:
OATransaction lifecycle has no ownership/completion state; double commit/rollback can re-run listeners.
Suggested unit tests:
testTransactionLifecycleIsMonotonic, testDoubleCommitRejected, testCommitAfterRollbackRejected,
testRollbackAfterCommitRejected.
Spec target section:
Transaction Runtime / Lifecycle State

TX-LIFECYCLE-002 — Active Before Completion
Contract statement:
commit(), rollback(), executeOpenBatches(), participant mutation, and transaction-scoped state mutation must require
a valid transaction stage according to the package lifecycle contract.
Rationale:
Completing or mutating an inactive, stale, failed, or closed transaction creates false boundaries and corrupts
participant coordination.
Source scope:
OATransaction.start(), commit(), rollback(), executeOpenBatches(), addTransactionListener(...),
removeTransactionListener(...), put(...), get(...), remove(...), isStarted().
Related CODEX findings:
commit()/rollback() do not enforce active state; completed transaction state can be reused.
Suggested unit tests:
testCommitRequiresActiveTransaction, testRollbackRequiresActiveTransaction,
testCompletedTransactionRejectsNewStateMutation.
Spec target section:
Transaction Runtime / Valid Stage Operations

TX-OWNER-001 — Thread Ownership
Contract statement:
A transaction belongs to the thread that starts it, and only the owning active thread may commit, roll back, close,
or clear its thread-local binding unless an explicit cross-thread transfer contract exists.
Rationale:
OA transactions are thread-local runtime boundaries; cross-thread completion can run participants in the wrong
object, datasource, sync, or runtime context.
Source scope:
OATransaction.start(), isStarted(), commit(), rollback(); OAThreadLocalService transaction binding.
Related CODEX findings:
No owner thread is recorded; cross-thread or stale completion can clear the wrong transaction context.
Suggested unit tests:
testCrossThreadCommitRejected, testCrossThreadRollbackRejected, testOwnerThreadCanCompleteActiveTransaction.
Spec target section:
Transaction Runtime / Thread Ownership

TX-TL-001 — ThreadLocal Binding Integrity
Contract statement:
Starting a transaction must bind it to the current thread according to the nested-transaction policy, and
completion/cleanup must clear thread-local state only if this transaction is still the active bound transaction.
Rationale:
Stale transactions must not clear newer transactions, and nested or replacement transactions must not corrupt
runtime visibility.
Source scope:
OATransaction.start(), commit(), rollback(), isStarted(); OAThreadLocalService.setTransaction/getTransaction.
Related CODEX findings:
start() overwrites active transaction; commit()/rollback() unconditionally clear thread-local; transaction counter
can drift.
Suggested unit tests:
testStaleTransactionDoesNotClearActiveTransaction, testThreadLocalTransactionCounterTracksActualBinding,
testThreadLocalClearedOnlyByActiveOwner.
Spec target section:
Transaction Runtime / ThreadLocal Semantics

TX-NEST-001 — Nested Transaction Policy
Contract statement:
Nested transaction behavior must be explicit: either nested start is visibly rejected, or nested transactions are
stack-managed and restore the parent transaction after inner completion.
Rationale:
Implicit overwrite semantics lose outer transaction boundaries and can make parent work non-transactional.
Source scope:
OATransaction.start(), commit(), rollback(); OAThreadLocalService transaction binding.
Related CODEX findings:
start() overwrites an existing transaction bound to the thread.
Suggested unit tests:
testNestedStartRejectedByContract, testNestedStackRestoresParentIfSupported,
testInnerCompletionDoesNotClearParentIfNestedSupported.
Spec target section:
Transaction Runtime / Nesting Semantics

TX-COMPLETE-001 — Completion Exactly Once
Contract statement:
A transaction may produce exactly one terminal outcome: committed, commit-failed, rolled-back, rollback-failed, or
closed-after-failure; participant commit/rollback side effects must not run more than once for the same transaction.
Rationale:
Duplicate completion can duplicate datasource commits, rollbacks, batch execution, cache visibility transitions,
event publication, and diagnostics.
Source scope:
OATransaction.commit(), rollback(), executeOpenBatches(); OATransactionListener.
Related CODEX findings:
Double commit/rollback can re-run listeners and decrement thread-local counters again.
Suggested unit tests:
testCommitRunsParticipantsOnce, testRollbackRunsParticipantsOnce,
testDoubleCompletionDoesNotRepeatParticipantCallbacks.
Spec target section:
Transaction Runtime / Completion Semantics

TX-PARTICIPANT-001 — Participant Enlistment Visibility
Contract statement:
A transaction participant/listener must become visible for commit, rollback, and batch callbacks only after
registration is fully committed, and participant mutation during completion must follow a documented snapshot or
rejection policy.
Rationale:
Participant lists define transaction side effects; mid-iteration mutation can skip, duplicate, or disorder critical
callbacks.
Source scope:
OATransaction.addTransactionListener(...), removeTransactionListener(...), commit(), rollback(),
executeOpenBatches(); OATransactionListener.
Related CODEX findings:
Mutable ArrayList has no synchronization or snapshot semantics; listener mutation during callbacks can cause
nondeterministic behavior.
Suggested unit tests:
testParticipantRegistrationVisibleAfterAddReturns, testListenerMutationDuringCommitRejectedOrSnapshotSafe,
testRemovePreventsFutureEligibleCallbacks.
Spec target section:
Transaction Runtime / Participant Enlistment

TX-PARTICIPANT-002 — Participant State Awareness
Contract statement:
Each participant must receive only lifecycle callbacks valid for its current transaction-participation state,
including batch/prepared, committed, commit-failed, rollback-eligible, rolled-back, and cleanup states.
Rationale:
A participant that has already committed must not later receive ordinary rollback as if its work were still
uncommitted.
Source scope:
OATransactionListener.commit(...), rollback(...), executeOpenBatches(...); OATransaction.commit(), rollback(),
executeOpenBatches().
Related CODEX findings:
No per-listener state exists; rollback after partial commit can send contradictory signals.
Suggested unit tests:
testCommittedParticipantDoesNotReceiveOrdinaryRollbackAfterPartialCommitFailure,
testRollbackOnlyRollbackEligibleParticipants, testParticipantStateRecordedAcrossFailure.
Spec target section:
Transaction Runtime / Participant State

TX-BATCH-001 — Batch Execution Before Commit
Contract statement:
If batch mode or open batches are part of the transaction contract, pending batches must execute before final
participant commit, and failure during batch execution must prevent false-success commit.
Rationale:
Skipping batched writes while reporting commit success loses datasource work and breaks save/delete/cascade
boundaries.
Source scope:
OATransaction.getUseBatch(), setUseBatch(...), executeOpenBatches(), commit();
OATransactionListener.executeOpenBatches(...).
Related CODEX findings:
commit() does not call executeOpenBatches() even though listener documentation says pending batches execute prior to
commit.
Suggested unit tests:
testCommitExecutesOpenBatchesBeforeCommit, testBatchFailurePreventsCommitSuccess,
testExecuteOpenBatchesOrderingBeforeCommit.
Spec target section:
Transaction Runtime / Batch Commit Semantics

TX-COMMIT-001 — Commit Boundary Correctness
Contract statement:
A successful commit means all commit-required participants have completed their required prepare/batch/finalize work
according to the transaction’s stage policy, and object/cache/datasource visibility can be treated as committed.
Rationale:
OA object graph, datasource, cache, Hub, sync, and replication flows rely on commit as the point where grouped work
becomes durable or observable by contract.
Source scope:
OATransaction.commit(), executeOpenBatches(); OATransactionListener.commit(...), executeOpenBatches(...).
Related CODEX findings:
Skipped batch execution and fail-fast listener loops can make commit appear successful while participant work is
incomplete.
Suggested unit tests:
testCommitSuccessRequiresAllParticipantsCommitted, testCommitFailureDoesNotReportCommitted,
testCommitVisibilityOccursAfterParticipantCompletion.
Spec target section:
Transaction Runtime / Commit Boundary

TX-COMMIT-FAIL-001 — Commit Failure Is Distinct From Rollback
Contract statement:
A failure after commit processing begins must enter a commit-failed/recovery-visible state and must not be treated
as a normal active rollback unless participant state proves rollback is still valid.
Rationale:
Once any participant has committed, ordinary rollback may be impossible or harmful; recovery needs diagnostics and
participant-state awareness.
Source scope:
OATransaction.commit(), rollback(); OATransactionListener.commit(...), rollback(...).
Related CODEX findings:
Commit failure has no distinct state; caller-managed rollback after partial commit can send contradictory callbacks.
Suggested unit tests:
testCommitFailureAfterOneParticipantCommittedCreatesCommitFailedState,
testRollbackAfterPartialCommitDoesNotRollbackCommittedParticipant, testCommitFailurePreservesRootCause.
Spec target section:
Transaction Runtime / Commit Failure Semantics

TX-ROLLBACK-001 — Rollback Boundary Correctness
Contract statement:
Rollback must undo, discard, or mark incomplete only active uncommitted work that is rollback-eligible, and the
transaction outcome must be visibly rolled-back or rollback-failed.
Rationale:
Rollback is the undo/discard boundary for active transaction work; it must not silently appear complete when
participant rollback failed or was ineligible.
Source scope:
OATransaction.rollback(); OATransactionListener.rollback(...).
Related CODEX findings:
Rollback has no stage or participant eligibility tracking; listener exception can skip later rollback participants.
Suggested unit tests:
testRollbackDuringActiveCallsRollbackEligibleParticipants, testRollbackFailureCreatesRollbackFailedState,
testRollbackDoesNotReportSuccessWhenParticipantFails.
Spec target section:
Transaction Runtime / Rollback Boundary

TX-ROLLBACK-ONLY-001 — Rollback-Only Enforcement
Contract statement:
A transaction marked rollback-only must not commit successfully; commit must visibly fail or route through the
documented rollback path.
Rationale:
Known failures during active work must not be hidden by a later successful commit.
Source scope:
OATransaction lifecycle contract; future rollback-only state; participant and datasource integration.
Related CODEX findings:
Existing invariant notes identify absent rollback-only semantics.
Suggested unit tests:
testRollbackOnlyPreventsCommit, testParticipantCanMarkRollbackOnly,
testRollbackOnlyCommitFailsOrRollsBackByContract.
Spec target section:
Transaction Runtime / Rollback-Only Semantics

TX-LISTENER-001 — Listener Ordering
Contract statement:
Participant callback ordering must be deterministic for registration, batch execution, commit, rollback, and cleanup
phases, including whether rollback unwinds in registration order or reverse order.
Rationale:
Datasource, cascade, cache, event, and sync participants can have ordering dependencies.
Source scope:
OATransaction.addTransactionListener(...), removeTransactionListener(...), executeOpenBatches(), commit(),
rollback().
Related CODEX findings:
ArrayList registration order exists implicitly, but rollback order and mutation semantics are not explicit.
Suggested unit tests:
testCommitUsesRegistrationOrder, testExecuteOpenBatchesUsesRegistrationOrder, testRollbackUsesDocumentedOrder.
Spec target section:
Transaction Runtime / Callback Ordering

TX-LISTENER-002 — Listener Exception Semantics
Contract statement:
Participant/preparation callbacks may fail-fast before commit is finalized, while cleanup/finalization observer
phases must continue through remaining participants and aggregate failures according to the OA-wide callback policy.
Rationale:
One listener failure must not leave other transaction participants permanently open, unrolled-back, or unreported.
Source scope:
OATransaction.commit(), rollback(), executeOpenBatches(); OATransactionListener.
Related CODEX findings:
First listener exception stops iteration; later listeners are not committed/rolled back/flushed and failures are not
aggregated.
Suggested unit tests:
testPrepareFailureCancelsCommit, testRollbackAttemptsAllParticipantsAfterFailure,
testCleanupFailuresAreAggregatedWithSuppressedExceptions.
Spec target section:
Transaction Runtime / Listener Exception Policy

TX-ERROR-001 — Exception Propagation And Aggregation
Contract statement:
Commit, rollback, and batch failures must preserve the root cause and aggregate additional participant failures
without hiding them or replacing them with misleading success.
Rationale:
Production recovery depends on knowing which participant failed, which participants completed, and whether cleanup
also failed.
Source scope:
OATransaction.commit(), rollback(), executeOpenBatches(); OATransactionListener.
Related CODEX findings:
First exception wins and later failures are lost.
Suggested unit tests:
testCommitFailurePreservesRootCauseAndSuppressedFailures, testRollbackFailureAggregatesParticipantFailures,
testExecuteOpenBatchesFailureReportsParticipant.
Spec target section:
Transaction Runtime / Error Reporting

TX-CLEANUP-001 — Cleanup Always Runs
Contract statement:
Thread-local cleanup and transaction-owned cleanup must run after commit, rollback, commit failure, rollback
failure, and cancellation according to a finally-style cleanup policy.
Rationale:
Stale transaction context on pooled/runtime threads can corrupt later datasource, object, sync, replication, and
event behavior.
Source scope:
OATransaction.commit(), rollback(); OAThreadLocalService transaction binding; transaction listener/list/map
ownership.
Related CODEX findings:
Thread-local is cleared in finally, but active-owner and terminal cleanup semantics are missing.
Suggested unit tests:
testThreadLocalClearedAfterCommitFailureWhenActiveOwner, testThreadLocalClearedAfterRollbackFailureWhenActiveOwner,
testCleanupRunsAfterParticipantException.
Spec target section:
Transaction Runtime / Cleanup

TX-RESOURCE-001 — Transaction-Scoped State Lifetime
Contract statement:
Transaction-scoped listener references and key/value state must be cleared on terminal completion or intentionally
retained only under an explicit diagnostic policy; completed transactions must not expose stale participant state as
reusable active state.
Rationale:
Transaction maps and listeners can retain datasource, object, Hub, cache, and graph references after completion.
Source scope:
OATransaction.addTransactionListener(...), removeTransactionListener(...), put(...), get(...), remove(...),
commit(), rollback().
Related CODEX findings:
hm and listener list remain after commit/rollback.
Suggested unit tests:
testTransactionStateClearedAfterTerminalCompletion, testCompletedTransactionCannotReuseStoredParticipantState,
testDiagnosticRetentionPolicyIsExplicit.
Spec target section:
Transaction Runtime / State Lifetime

TX-OPTIONS-001 — Transaction Options Freeze By Stage
Contract statement:
Transaction options that affect participant behavior, including isolation level, batch mode, and read-only
datasource write override, must have documented mutability and must not change after the stage where participants
rely on them unless explicitly allowed.
Rationale:
Changing runtime transaction options after work begins can make datasource behavior inconsistent within one
transaction boundary.
Source scope:
OATransaction(int), getTransactionIsolationLevel(), setUseBatch(...), getUseBatch(),
setAllowWritesIfDsIsReadonly(...), getAllowWritesIfDsIsReadonly().
Related CODEX findings:
Existing package-info open question notes option mutability after start.
Suggested unit tests:
testTransactionOptionsStableAfterStartByContract, testUseBatchVisibleToParticipants,
testAllowWritesOverrideVisibleToDatasourceChecks.
Spec target section:
Transaction Runtime / Transaction Options

TX-CONCURRENT-001 — Transaction Instance Concurrency Boundary
Contract statement:
An OATransaction instance is a single-thread-owned coordination object; concurrent mutation or completion from
multiple threads must be rejected or explicitly synchronized by contract.
Rationale:
Listener lists, transaction maps, lifecycle stage, and thread-local ownership are not safe if multiple threads
mutate or complete the same transaction concurrently.
Source scope:
OATransaction.start(), commit(), rollback(), addTransactionListener(...), removeTransactionListener(...), put(...),
get(...), remove(...), executeOpenBatches().
Related CODEX findings:
Listener list and transaction map are mutable without synchronization; owner thread is not recorded.
Suggested unit tests:
testConcurrentCompletionRejected, testConcurrentListenerMutationRejectedOrSnapshotSafe,
testTransactionMapMutationFollowsOwnershipContract.
Spec target section:
Transaction Runtime / Concurrency

TX-INTEGRATION-001 — Datasource Boundary Consistency
Contract statement:
Datasource transaction checks, batching, isolation level, and read-only write override must observe the same active
transaction state for the duration of the transaction boundary.
Rationale:
OA datasource work must not see a transaction as active in one call and invisible in another because of thread-local
counter drift or stale binding.
Source scope:
OATransaction.start(), isStarted(), getUseBatch(), getAllowWritesIfDsIsReadonly(), executeOpenBatches();
OAThreadLocalService transaction binding; datasource integration.
Related CODEX findings:
ThreadLocal transaction counter can drift from real active state, making active transactions invisible to datasource
checks.
Suggested unit tests:
testDatasourceSeesActiveTransactionAfterStart, testDatasourceNoLongerSeesTransactionAfterCompletion,
testCounterDriftDoesNotHideActiveTransaction.
Spec target section:
Transaction Runtime / Datasource Integration

TX-INTEGRATION-002 — Object Graph Mutation Boundary
Contract statement:
Object, Hub, cascade, save, delete, cache, and event behavior inside a transaction must observe a consistent
transaction boundary and must not publish committed success before the transaction outcome is known.
Rationale:
OA transactions coordinate runtime graph mutations, not only database calls; cache visibility and event publication
must align with commit/rollback semantics.
Source scope:
OATransaction lifecycle; OATransactionListener participant callbacks; integration with object, hub, cascade, cache,
datasource, and event packages.
Related CODEX findings:
Existing package-info notes identify save/delete/cascade and event ordering as boundary risk areas.
Suggested unit tests:
testObjectSaveVisibilityAfterCommitBoundary, testDeleteRollbackDoesNotPublishCommittedSuccess,
testCacheVisibilityFollowsTransactionOutcome.
Spec target section:
Transaction Runtime / Object Graph Boundary

TX-INTEGRATION-003 — Sync Replication And Replay Context Boundary
Contract statement:
Sync, replication, remote, replay, and background runtime flows must not inherit stale transaction context, and any
transaction participation during these flows must be explicit and stage-valid.
Rationale:
Replay and remote execution require deterministic boundaries; stale ThreadLocal transaction state can cause false
batching, skipped writes, or wrong visibility.
Source scope:
OATransaction.start(), commit(), rollback(); OAThreadLocalService transaction binding; integration with sync,
replication, remote, queue, and runtime packages.
Related CODEX findings:
Existing package-info notes identify stale transaction context for sync/replication replay as a risk.
Suggested unit tests:
testReplayThreadDoesNotInheritStaleTransaction, testRemoteCallTransactionContextIsExplicit,
testBackgroundThreadTransactionCleanupAfterFailure.
Spec target section:
Transaction Runtime / Sync Replay Boundary

TX-OBSERVABLE-001 — Transaction Outcome Visibility
Contract statement:
Callers and runtime observers must be able to distinguish committed, rolled-back, commit-failed, rollback-failed,
rollback-only, closed, and incomplete transaction outcomes.
Rationale:
Silent false-success at a transaction boundary corrupts production recovery, diagnostics, cache state, event
handling, and downstream sync/replication behavior.
Source scope:
OATransaction lifecycle; commit(), rollback(), executeOpenBatches(); OATransactionListener.
Related CODEX findings:
Commit, rollback, and batch failures can leave partial participant work while thread-local is cleared and the
transaction appears inactive.
Suggested unit tests:
testCommitFailedOutcomeIsObservable, testRollbackFailedOutcomeIsObservable,
testIncompleteTransactionDoesNotAppearSuccessful.
Spec target section:
Transaction Runtime / Outcome Visibility

*/







