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
package com.viaoa.transaction;

import java.util.ArrayList;
import java.util.HashMap;

import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;

/**
 * Thread-local transaction mechanism used by OA datasources and other
 * subsystems that require coordinated commit/rollback sequencing.
 * <p>
 * An {@code OATransaction} is explicitly started and then becomes associated
 * with the current thread via
 * {@link com.viaoa.object.OAThreadLocalDelegate#setTransaction}. Code running
 * in that thread can retrieve the active transaction and register listeners
 * that should be notified at commit or rollback time.
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>define a JDBC-style isolation level for the transaction,</li>
 *   <li>coordinate batch-mode operations across datasources,</li>
 *   <li>optionally allow writes even when datasources are marked read-only,</li>
 *   <li>notify registered {@link OATransactionListener} instances,</li>
 *   <li>provide a simple key/value map for listeners to store temporary data.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link #start()} — associates this transaction with the current thread,</li>
 *   <li>application performs work (e.g., datasource operations),</li>
 *   <li>{@link #commit()} or {@link #rollback()} — notifies all listeners and
 *       clears thread-local state.</li>
 * </ol>
 *
 * <h2>Listeners</h2>
 * Registered listeners (datasources, batching subsystems, etc.) receive:
 * <ul>
 *   <li>{@code commit(this)}</li>
 *   <li>{@code rollback(this)}</li>
 *   <li>{@code executeOpenBatches(this)}</li>
 * </ul>
 * providing a consistent transaction boundary.
 *
 * <h2>Thread Association</h2>
 * Transactions never span threads. Each thread must start and end its own
 * transaction using {@link #start()}, and {@link #isStarted()} determines
 * whether this transaction is the one currently bound to the thread.
 */
public class OATransaction {
	/*
	CODEX

	Transaction hardening findings for OA 4.0:

	1. OATransaction.start()/commit()/rollback()
	   Severity: Critical
	   Bug/risk: transaction lifecycle has no ownership/completion state and no nesting model. start() overwrites any
	   transaction already bound to the thread, while commit()/rollback() unconditionally clear the thread-local
	   transaction instead of verifying that this transaction is still the active owner. Double commit/rollback will
	   re-run listeners and decrement OAThreadLocalService's transaction counter again.
	   Production impact: nested transactions can lose the outer transaction; commit/rollback on a stale transaction can
	   clear another active transaction; double completion can duplicate datasource commits/rollbacks and corrupt
	   transaction diagnostics/counters.
	   Minimal fix: add lifecycle state (NEW/ACTIVE/COMMITTING/COMMITTED/ROLLING_BACK/ROLLED_BACK), verify active
	   thread ownership before completion, reject double completion, and either implement a transaction stack or reject
	   nested start when another transaction is active.
	
	2. OATransaction.commit()
	   Severity: High
	   Bug/risk: commit() does not call executeOpenBatches() even though OATransactionListener documents that pending
	   batches are executed prior to commit.
	   Production impact: datasource/batch listeners can have pending work that is never flushed before commit, causing
	   false-success transaction completion with missing writes.
	   Minimal fix: define the contract explicitly and, if batching is part of commit, run executeOpenBatches() before
	   listener commit with clear exception/rollback behavior.
	
	3. OATransaction.commit()/rollback()/executeOpenBatches()
	   Severity: High
	   Bug/risk: listener exceptions stop iteration, so later listeners are not notified. The first exception wins and
	   later cleanup/rollback failures are not collected or suppressed.
	   Production impact: one datasource/listener failure can leave other registered resources active or uncommitted,
	   while the transaction is removed from thread-local state and appears complete/inactive.
	   Minimal fix: iterate over a snapshot, attempt all required listener cleanup/rollback calls, collect failures with
	   suppressed exceptions, and define whether commit failure should trigger rollback for listeners not committed.
	
	4. OATransaction.addTransactionListener()/removeTransactionListener()/listener iteration
	   Severity: Medium
	   Bug/risk: the listener list is a mutable ArrayList with no synchronization or snapshot semantics. A listener that
	   adds/removes listeners during commit/rollback can cause ConcurrentModificationException or skipped/duplicate
	   callbacks.
	   Production impact: datasource participation can become nondeterministic under reentrant listener behavior.
	   Minimal fix: disallow listener mutation after start/while completing, or use a snapshot/CopyOnWriteArrayList.
	
	5. OATransaction key/value map and listener list
	   Severity: Medium
	   Bug/risk: completion does not clear listener references or transaction-scoped stored values.
	   Production impact: long-lived/reused transaction objects can retain datasource/object/Hub state after completion,
	   and reuse after completion can observe stale state.
	   Minimal fix: clear transaction-scoped storage/listeners after terminal completion, or explicitly forbid reuse and
	   assert when a completed transaction is used.
	   
	   
1. Critical: no lifecycle ownership/completion state
     Class/method: src/main/java/com/viaoa/transaction/OATransaction.java:223
     Bug/risk: start() overwrites any existing thread transaction; commit()/rollback() clear thread-local transaction
     without verifying that this is still active; double commit/rollback re-runs listeners.
     Impact: nested transactions can lose the outer transaction, stale transactions can clear current work, and
     duplicate completion can duplicate datasource actions.
     Fix: add transaction state, active-owner checks, double-completion rejection, and either a transaction stack or
     explicit nested-start rejection.
  2. High: commit does not execute open batches
     Class/method: src/main/java/com/viaoa/transaction/OATransaction.java:261, src/main/java/com/viaoa/transaction/
     OATransactionListener.java:60
     Bug/risk: listener contract says batches execute prior to commit, but commit() only calls tl.commit(this).
     Impact: pending datasource batch work can be skipped while commit appears successful.
     Fix: define and enforce commit sequencing: execute batches before commit, with clear rollback/error behavior.
  3. High: listener exception stops remaining listeners
     Class/method: src/main/java/com/viaoa/transaction/OATransaction.java:261, src/main/java/com/viaoa/transaction/
     OATransaction.java:245, src/main/java/com/viaoa/transaction/OATransaction.java:281
     Bug/risk: first listener exception aborts the loop. Later listeners are not committed/rolled back/flushed.
     Impact: one datasource failure can leave other resources active or uncompleted while thread-local state is
     cleared.
     Fix: iterate a snapshot, attempt all required cleanup, collect suppressed exceptions, and define commit-failure
     rollback semantics.
  4. Medium: listener list mutation is unsafe during transaction callbacks
     Class/method: src/main/java/com/viaoa/transaction/OATransaction.java:277 and commit/rollback iteration
     Bug/risk: ArrayList is mutated/iterated without synchronization or snapshot semantics.
     Impact: reentrant listener add/remove can cause ConcurrentModificationException, skipped callbacks, or duplicate
     behavior.
     Fix: disallow mutation while completing or use snapshot/CopyOnWriteArrayList.
  5. Medium: transaction-scoped state retained after completion
     Class/method: src/main/java/com/viaoa/transaction/OATransaction.java:297, listener list
     Bug/risk: hm and listeners remain after commit/rollback.
     Impact: completed transaction objects retain datasource/object/Hub state; reuse after completion can observe
     stale state.
     Fix: clear state on terminal completion or forbid reuse with lifecycle assertions.
	 
	   
 High: transaction ThreadLocal counter can drift from real active transaction state
  OATransaction.start()/commit()/rollback() use OAThreadLocalService.setTransaction(this/null) as raw set/clear calls.
  That service increments on any non-null set and decrements on any null set, without checking previous state. So
  replacing an active transaction increments twice, and double/stale clear decrements again.

  Production impact: OAThreadLocalService.getTransaction() short-circuits to null when the global counter is zero, so
  counter drift can make active transactions invisible to datasource transaction checks, batch mode checks, and read-
  only write override checks.

  Minimal fix: make transaction binding transition-aware: increment only null -> non-null, decrement only non-null ->
  null, and verify active owner before clear/replace.

	   

1. No enforced transaction lifecycle state

  - Class/method: OATransaction.start, commit, rollback around src/main/java/com/viaoa/transaction/
    OATransaction.java:223, src/main/java/com/viaoa/transaction/OATransaction.java:245, src/main/java/com/viaoa/
    transaction/OATransaction.java:261
  - Severity: Critical
  - Bug/risk: There is no NEW/ACTIVE/COMMITTED/ROLLED_BACK state. A transaction can be started multiple times,
    committed multiple times, rolled back after commit, or committed after rollback.
  - Production impact: duplicate datasource commits/rollbacks, stale listener execution, transaction counter drift,
    and false completion semantics.
  - Minimal hardening: add terminal lifecycle state; reject double completion and reuse after terminal state.

  2. Nested transaction overwrites outer transaction

  - Class/method: OATransaction.start around src/main/java/com/viaoa/transaction/OATransaction.java:223
  - Severity: Critical
  - Bug/risk: start() blindly sets this transaction into OAThreadLocalService. If an outer transaction is already
    active, it is replaced without stack/restore behavior.
  - Production impact: outer transaction participants become orphaned; inner commit/rollback clears the thread-local
    and leaves outer work outside transaction control.
  - Minimal hardening: either reject nested start() when a transaction is active, or implement a real transaction
    stack with defined nested commit/rollback semantics.

  3. Commit/rollback can clear another transaction

  - Class/method: commit, rollback around src/main/java/com/viaoa/transaction/OATransaction.java:245, src/main/java/
    com/viaoa/transaction/OATransaction.java:261
  - Severity: Critical
  - Bug/risk: completion methods do not verify OARuntime.thread().getTransaction() == this before clearing thread-
    local state.
  - Production impact: stale transaction references can clear a newer active transaction, breaking datasource/save/
    delete transaction participation.
  - Minimal hardening: verify active owner before completion; throw if completing a non-current transaction.

  4. ThreadLocal transaction counter can drift

  - Class/method: OATransaction.start/commit/rollback; OAThreadLocalService.setTransaction/getTransaction around src/
    main/java/com/viaoa/runtime/OAThreadLocalService.java:401
  - Severity: High
  - Bug/risk: setTransaction(t) increments for every non-null set and decrements for every null set, regardless of
    previous thread state.
  - Production impact: active transactions can become invisible because getTransaction() short-circuits when global
    count is zero; datasource isInTransaction, batch mode, and read-only override checks can be wrong.
  - Minimal hardening: make transaction assignment transition-aware: increment only null -> non-null, decrement only
    non-null -> null.

  5. Commit does not execute open batches

  - Class/method: OATransaction.commit, executeOpenBatches; src/main/java/com/viaoa/transaction/
    OATransaction.java:261, src/main/java/com/viaoa/transaction/OATransaction.java:371; listener contract at src/main/
    java/com/viaoa/transaction/OATransactionListener.java:60
  - Severity: High
  - Bug/risk: Listener docs say open batches execute prior to commit, but commit() only invokes tl.commit(this).
  - Production impact: batched datasource writes can be skipped while transaction commit appears successful.
  - Minimal hardening: define commit sequence and call executeOpenBatches() before commit() if that is the intended
    contract.

  6. Listener exception stops remaining listeners

  - Class/method: commit, rollback, executeOpenBatches; src/main/java/com/viaoa/transaction/OATransaction.java:245,
    src/main/java/com/viaoa/transaction/OATransaction.java:261, src/main/java/com/viaoa/transaction/
    OATransaction.java:371
  - Severity: High
  - Bug/risk: first listener exception aborts iteration.
  - Production impact: later datasource/listener participants may never commit, rollback, flush, or clean up.
  - Minimal hardening: define listener phases. For cleanup/finalization observers, continue through all listeners and
    aggregate exceptions with suppressed failures.

  7. No rollback-only semantics

  - Class/method: package-level lifecycle model; absent from OATransaction
  - Severity: High
  - Bug/risk: Once a participant fails during work or batch execution, there is no way to mark the transaction as
    rollback-only.
  - Production impact: caller can still call commit() after an earlier recoverable-looking failure, causing false-
    success or partial completion.
  - Minimal hardening: add setRollbackOnly, isRollbackOnly; make commit() fail or route to rollback when set.

  8. Listener mutation during callbacks is unsafe

  - Class/method: addTransactionListener, removeTransactionListener, callback loops; src/main/java/com/viaoa/
    transaction/OATransaction.java:277
  - Severity: Medium
  - Bug/risk: ArrayList is iterated directly while listeners can add/remove listeners reentrantly.
  - Production impact: ConcurrentModificationException, skipped callbacks, or nondeterministic participant ordering.
  - Minimal hardening: snapshot listener list before callback loops or use CopyOnWriteArrayList; optionally reject
    mutation once completing.

  9. Transaction-scoped state retained after completion

  - Class/method: listener list and hm; src/main/java/com/viaoa/transaction/OATransaction.java:297
  - Severity: Medium
  - Bug/risk: listeners and key/value state are not cleared after commit/rollback.
  - Production impact: retained datasource/object/Hub references; accidental transaction reuse observes stale state.
  - Minimal hardening: clear transaction-scoped state at terminal completion, or make completed transactions unusable.

  10. No thread ownership assertion

  - Class/method: start, isStarted, commit, rollback
  - Severity: Medium
  - Bug/risk: docs say transactions never span threads, but the object does not record owner thread or reject cross-
    thread completion.
  - Production impact: one thread can commit/rollback a transaction object started by another thread, invoking
    listeners in the wrong runtime context.
  - Minimal hardening: store owner thread on start() and assert current thread on listener mutation and completion.

  11. BEFORE/DURING/AFTER listener semantics are not represented

  - Class/method: OATransactionListener
  - Severity: Medium
  - Bug/risk: only one listener interface exists for commit/rollback/batch. There is no phase distinction for fail-
    fast participants vs observers vs cleanup/finalization observers.
  - Production impact: observer-style listeners can prevent remaining cleanup; participant failures are not
    semantically separated from notification failures.
  - Minimal hardening: split phases or document listener methods as participant/fail-fast vs cleanup/continue;
    implement aggregation where appropriate.

	   
12. Commit failure clears the transaction before caller can perform meaningful rollback

  - Class/method: OATransaction.commit around src/main/java/com/viaoa/transaction/OATransaction.java:261
  - Severity: High
  - Bug/risk: If a listener throws during commit(), the finally block clears the thread-local transaction. The package
    usage pattern shows catch (Exception e) { tx.rollback(); }, but after a commit failure the transaction is no
    longer active.
  - Production impact: rollback listeners may execute outside active transaction context, and datasource code checking
    OARuntime.thread().getTransaction() will see no transaction during rollback cleanup.
  - Minimal hardening: distinguish commit-failed state from completed state. If commit throws, preserve enough
    transaction context for rollback/recovery, or explicitly mark commit failure as terminal and prohibit caller
    rollback with a clear exception.

  13. executeOpenBatches() can be called independently and repeatedly

  - Class/method: OATransaction.executeOpenBatches around src/main/java/com/viaoa/transaction/OATransaction.java:371
  - Severity: Medium
  - Bug/risk: executeOpenBatches() is public and has no active-owner, lifecycle, or idempotency guard.
  - Production impact: batches can be flushed outside an active transaction, after rollback, after commit, or multiple
    times. That can duplicate writes or execute pending datasource work outside the intended boundary.
  - Minimal hardening: make batch execution part of controlled commit flow, or guard it with active transaction state
    and a “batches executed” flag.

  14. Mutable transaction options can change mid-transaction

  - Class/method: setUseBatch, setAllowWritesIfDsIsReadonly around src/main/java/com/viaoa/transaction/
    OATransaction.java:222, src/main/java/com/viaoa/transaction/OATransaction.java:241
  - Severity: Medium
  - Bug/risk: batch mode and read-only override can be changed after start() and even while listeners/datasources are
    participating.
  - Production impact: different datasource operations in the same transaction can observe different transaction
    options, causing inconsistent batching or write permission behavior.
  - Minimal hardening: freeze transaction options at start(), or document and enforce that options must be set before
    start.

  15. Rollback ordering is same as commit ordering

  - Class/method: rollback around src/main/java/com/viaoa/transaction/OATransaction.java:245
  - Severity: Medium
  - Bug/risk: listeners are rolled back in registration order. If listeners represent nested resources or dependent
    participants, rollback often needs reverse registration order.
  - Production impact: dependent resources can be rolled back before the resources that depend on them, leaving
    cleanup or compensation behavior inconsistent.
  - Minimal hardening: define participant ordering explicitly. If registration order is commit order, consider reverse
    order for rollback, or add listener priority/phase semantics.

  16. Null listener registration causes delayed failure at transaction boundary

  - Class/method: addTransactionListener, callback loops around src/main/java/com/viaoa/transaction/
    OATransaction.java:277
  - Severity: Low
  - Bug/risk: addTransactionListener(null) is accepted. The failure occurs later during commit/rollback/open-batch
    iteration.
  - Production impact: transaction can fail at boundary time with an avoidable NullPointerException, after work has
    already been performed.
  - Minimal hardening: reject null listeners immediately.

  17. No timeout/cancellation/abandonment detection

  - Class/method: package-level transaction model
  - Severity: Low / Medium depending on intended scope
  - Bug/risk: If caller starts a transaction and never commits/rolls back, there is no timeout, diagnostic owner
    stack, or abandonment warning beyond indirect ThreadLocal counter logging.
  - Production impact: pooled threads can retain transaction context indefinitely, causing unrelated later work on the
    same thread to join stale transaction state.
  - Minimal hardening: record start timestamp/owner stack for diagnostics; add optional stale transaction logging or
    runtime assertion hooks.

  Additional Assessment

  The remaining issues all come from the same core gap: the class is not stateful enough for infrastructure semantics.
  Once lifecycle state, owner checks, and commitAdditional findings beyond the ones already noted:

  12. Commit failure clears the active transaction before caller can rollback

  - Class/method: OATransaction.commit around src/main/java/com/viaoa/transaction/OATransaction.java:261
  - Severity: High
  - Bug/risk: If a listener throws during commit(), the finally clears the thread-local transaction before the
    caller’s catch block can call rollback(). The package usage example suggests catch -> rollback, but after a commit
    failure the transaction is already detached.
  - Production impact: rollback listeners may run outside active transaction context, datasource participants may not
    see isInTransaction(), and rollback-after-commit-failure semantics become undefined.
  - Minimal hardening: define commit-failure state. Either keep the transaction active until caller rollback, or mark
    terminal failed and provide an internal compensation/cleanup phase that still runs with transaction context.

  13. Rollback after failed commit can run rollback listeners after some listeners already committed

  - Class/method: commit, rollback
  - Severity: High
  - Bug/risk: Listener A can successfully commit, Listener B throws, caller catches and calls rollback(), then
    Listener A receives rollback after already committing.
  - Production impact: participants can receive impossible lifecycle sequences: commit then rollback for the same
    transaction. That can corrupt datasource state or cause duplicate/contradictory cleanup.
  - Minimal hardening: track per-listener phase outcome during commit. On commit failure, only rollback uncommitted/
    prepared participants, or define commit as non-recoverable once any participant commits.

  14. Transaction flags are mutable during an active transaction

  - Class/method: setUseBatch, setAllowWritesIfDsIsReadonly around src/main/java/com/viaoa/transaction/
    OATransaction.java:222
  - Severity: Medium
  - Bug/risk: Batch mode and read-only override can be changed after start() and even during listener callbacks.
  - Production impact: different datasource operations inside the same transaction can observe different transaction
    policy. A save early in the transaction might not batch, while a later save does; read-only override can change
    mid-flight.
  - Minimal hardening: freeze transaction options at start(), or document them as dynamic and add diagnostics when
    changed while active.

  15. Rollback callback order is same as commit order

  - Class/method: rollback around src/main/java/com/viaoa/transaction/OATransaction.java:245
  - Severity: Medium
  - Bug/risk: Rollback iterates listeners in registration order. If listeners represent nested resources or dependent
    participants, rollback commonly needs reverse order.
  - Production impact: dependent resources can be rolled back before the resources that depend on them, causing
    cleanup/order-sensitive inconsistencies.
  - Minimal hardening: define ordering contract. If listeners are ordered participants, rollback should likely use
    reverse registration order; if not, document that listeners must be independent.

  16. Public executeOpenBatches() can be called outside active transaction

  - Class/method: executeOpenBatches around src/main/java/com/viaoa/transaction/OATransaction.java:371
  - Severity: Medium
  - Bug/risk: There is no active-owner check, lifecycle state check, or idempotency guard. It can be called before
    start(), after commit(), after rollback(), or multiple times.
  - Production impact: pending datasource batch work can execute outside intended transaction context or execute more
    than once.
  - Minimal hardening: require active owner and legal state; track whether open batches have already been executed for
    the current transaction phase.

  17. Null listeners are accepted

  - Class/method: addTransactionListener around src/main/java/com/viaoa/transaction/OATransaction.java:277
  - Severity: Low
  - Bug/risk: addTransactionListener(null) succeeds if null is not already present. Later commit/rollback/execute
    batches will throw NullPointerException.
  - Production impact: a bad registration creates a delayed failure at transaction completion instead of failing at
    the source.
  - Minimal hardening: reject null listeners immediately with IllegalArgumentException.

	
	 
Transaction code must be stage-aware, and each participant must only receive lifecycle calls that are valid for its current state.	 
VV> create callbacks to run each transaction stage	   
Make the transaction stage explicit, then make each stage callback responsible for only what is valid in that stage.


> "Isolation Levels"
Clean mental model:
ACTIVE       -> normal work may register participants, mutate state, mark rollback-only
COMMITTING   -> participants finalize; no new normal work should begin
COMMITTED    -> success terminal state
ROLLING_BACK -> undo active/uncommitted work
ROLLED_BACK  -> rollback terminal state
CLOSED       -> cleanup complete; no lifecycle operations allowed	   
	   
- no rollback after a participant has already completed commit
  - no double commit / double rollback
  - no listener skipped because an earlier listener failed
  - no participant removed after it has accepted transactional work
  - no executeOpenBatches outside the legal commit-preparation phase
  - no clearing thread-local state until the transaction has reached a valid terminal state
  - no nested or stale transaction completing another transaction’s work

  A minimal participant state model could be:

  ENLISTED -> PREPARED -> COMMITTED

  or

  ENLISTED -> PREPARED -> ROLLED_BACK

  with failure states like:

  PREPARE_FAILED, COMMIT_FAILED, ROLLBACK_FAILED

  Then the transaction coordinator can decide what cleanup calls are legal per participant instead of broadcasting the
  same method to everyone blindly.
	   
	   
	   
*/

	/**
	 * Isolation level assigned to this transaction. Corresponds to one of the
	 * standard {@link java.sql.Connection} isolation constants and determines
	 * visibility and locking behavior for transactional operations.
	 */
	private final int transactionLevel;
	
	/**
	 * Collection of registered {@link OATransactionListener} instances that will
	 * be notified on commit, rollback, and open-batch execution events.
	 */
	private final ArrayList<OATransactionListener> al = new ArrayList<OATransactionListener>();

	/**
	 * Flag indicating whether batch mode is enabled for this transaction. When
	 * true, datasources and listeners may defer writes until batch execution.
	 */
	private boolean bUseBatch;

	/**
	 * Indicates whether write operations should be allowed even when a datasource
	 * is marked read-only. Used to override default safety checks.
	 */
	private boolean bAllowWritesIfDsIsReadonly;

	/*  java.sql.Connection isolation levels
	    java.sql.Connection.X<br>
	    TRANSACTION_NONE - level not set - some databases (ex: Derby) will throw and exception<br>
	    TRANSACTION_READ_UNCOMMITTED - data changed by transaction will be used by other transactions that read<br>
	    TRANSACTION_READ_COMMITTED - data changed by transaction is not "seen" until commited.  Other transactions will read "old" data.<br>
	    TRANSACTION_REPEATABLE_READ - prevents others from writing<br>
	    TRANSACTION_SERIALIZABLE - prevents others from reading & writing<br>
	*/
	/**
	 * Creates a new transaction with the specified JDBC-style isolation level.
	 *
	 * @param transactionLevel the desired transaction isolation level as defined
	 *        by {@link java.sql.Connection} constants.
	 */
	public OATransaction(int transactionLevel) {
		this.transactionLevel = transactionLevel;
	}

	/**
	 * Creates a new transaction using {@link java.sql.Connection#TRANSACTION_READ_COMMITTED}
	 * as the default isolation level.
	 */
	public OATransaction() {
		this(java.sql.Connection.TRANSACTION_READ_COMMITTED);
	}

	/**
	 * Returns the configured transaction isolation level.
	 *
	 * @return the JDBC-style isolation level assigned to this transaction.
	 */
	public int getTransactionIsolationLevel() {
		return transactionLevel;
	}

	/**
	 * Sets whether batch mode should be used for this transaction.
	 *
	 * @param b true to enable batch mode; false to disable it.
	 */
	public void setUseBatch(boolean b) {
		this.bUseBatch = b;
	}

	/**
	 * Indicates whether batch mode is enabled for this transaction.
	 *
	 * @return true if batch mode is enabled; otherwise false.
	 */
	public boolean getUseBatch() {
		return this.bUseBatch;
	}

	/**
	 * Specifies whether write operations should be permitted even when datasources
	 * are configured as read-only.
	 *
	 * @param b true to allow writes; false to prevent them.
	 */
	public void setAllowWritesIfDsIsReadonly(boolean b) {
		bAllowWritesIfDsIsReadonly = b;
	}

	/**
	 * Returns whether write operations are allowed when datasources are marked
	 * read-only.
	 *
	 * @return true if writes are permitted; otherwise false.
	 */
	public boolean getAllowWritesIfDsIsReadonly() {
		return bAllowWritesIfDsIsReadonly;
	}

	/**
	 * Associates this transaction with the current thread by placing it in the
	 * thread-local storage used by OA. Marks the beginning of a transactional
	 * context for the calling thread.
	 */
	public void start() {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		srvcOAThreadLocal.setTransaction(this);
	}

	/**
	 * Determines whether this transaction is currently active on the calling
	 * thread.
	 *
	 * @return true if this transaction is the one bound to the thread; otherwise
	 *         false.
	 */
	public boolean isStarted() {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		return srvcOAThreadLocal.getTransaction() == this;
	}

	/**
	 * Performs a rollback of the transaction. All registered listeners are
	 * notified via {@code rollback(this)}. The transaction is then cleared from
	 * thread-local storage.
	 */
	public void rollback() {
		try {
			for (OATransactionListener tl : al) {
				tl.rollback(this);
			}
		} finally {
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
			srvcOAThreadLocal.setTransaction(null);
		}
	}

	/**
	 * Commits the transaction. All registered listeners are notified via
	 * {@code commit(this)}. Regardless of listener behavior, the transaction is
	 * removed from thread-local storage.
	 */
	public void commit() {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		try {
			for (OATransactionListener tl : al) {
				tl.commit(this);
			}
		} finally {
			srvcOAThreadLocal.setTransaction(null);
		}
	}

	/**
	 * Registers a transaction listener if it is not already present.
	 *
	 * @param tl the listener to add.
	 */
	public void addTransactionListener(OATransactionListener tl) {
		if (!al.contains(tl)) {
			al.add(tl);
		}
	}

	/**
	 * Removes a previously registered transaction listener.
	 *
	 * @param tl the listener to remove.
	 */
	public void removeTransactionListener(OATransactionListener tl) {
		al.remove(tl);
	}

	// used by TransactionListeners to "store" information.
	/**
	 * Internal key/value store used by listeners to associate temporary data with
	 * the lifespan of this transaction.
	 */
	private HashMap<Object, Object> hm = new HashMap();

	/**
	 * Stores a key/value pair in the transaction's internal map.
	 *
	 * @param key the lookup key.
	 * @param value the associated value.
	 */
	public void put(Object key, Object value) {
		hm.put(key, value);
	}

	/**
	 * Retrieves a value from the transaction's internal map.
	 *
	 * @param key the lookup key.
	 * @return the stored value associated with the key, or null if none exists.
	 */
	public Object get(Object key) {
		return hm.get(key);
	}

	/**
	 * Removes the entry associated with the given key from the transaction's
	 * internal map.
	 *
	 * @param key the lookup key whose entry should be removed.
	 * @return the value previously associated with the key, or null if none existed.
	 */
	public Object remove(Object key) {
		return hm.remove(key);
	}

	/**
	 * Notifies all registered {@link OATransactionListener} instances to execute
	 * any batches they have open by invoking {@code executeOpenBatches(this)}.
	 */
	public void executeOpenBatches() {
		for (OATransactionListener tl : al) {
			tl.executeOpenBatches(this);
		}
	}

}
