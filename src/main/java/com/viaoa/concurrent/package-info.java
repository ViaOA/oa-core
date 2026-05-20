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
 * Concurrency utilities used throughout the OA framework. <p>
 *
 * The classes in this package provide lightweight wrappers around Java
 * concurrency primitives, integrating them with OA's execution model where
 * necessary. The package includes:
 *
 * <ul>
 *   <li><b>OAConcurrent</b>: launches a group of runnables with synchronized
 *       start timing.</li>
 *   <li><b>OAExecutorService</b>: thread-pool executor with named, daemon
 *       threads for background processing.</li>
 *   <li><b>OAScheduledExecutorService</b>: scheduler for tasks that run at
 *       specific OA temporal values or at fixed intervals.</li>
 * </ul>
 *
 * These classes do not manage OAObject or Hub behavior directly, but they
 * provide foundational concurrency tools used by higher-level OA subsystems
 * such as remoting, caching, background updates, and object graph traversal.
 */
package com.viaoa.concurrent;

/* CODEX Invariants

1. Package Summary

  com.viaoa.concurrent provides OA’s lightweight concurrency primitives:

  - OAExecutorService: daemon-thread executor wrapper for background task execution.
  - OAScheduledExecutorService: single-thread scheduled executor wrapper for delayed and periodic work.
  - OAPool: synchronized bounded/unbounded object pool with high-water shrink behavior.
  - OAThrottle: time-based suppression/throttling helper.
  - OAConcurrent: helper for starting multiple runnables at the same barrier.

  In OA 4.0 terms, this package is foundational support infrastructure. It does not itself own OAObject, Hub, sync,
  replication, transaction, or trigger semantics, but higher-level packages may rely on it for background work,
  polling, remoting socket reuse, logging suppression, preload/load traversal, and event processing.

  2. Core Concepts

  - Task: a Runnable or Callable submitted to OAExecutorService or OAScheduledExecutorService.
  - Worker thread: daemon thread created by wrapper thread factories to execute tasks.
  - Executor/queue: ThreadPoolExecutor or ScheduledExecutorService backing task execution.
  - Lifecycle state: whether executor/scheduler/pool is active, closing, closed, or reusable.
  - Start/shutdown/close: creation and termination of worker infrastructure.
  - Cancellation: explicit stop/cancel of submitted or scheduled work.
  - Locking: synchronized ownership around shared structures such as OAPool.alResource.
  - Memory visibility: guarantee that state changes by one thread are visible to others.
  - Ordering: execution order guarantees, if any, for queued/scheduled tasks.
  - Callback/listener dispatch: execution of caller-supplied runnables/callables and pool callbacks.
  - ThreadLocal/OAThreadLocal ownership: this package does not automatically propagate or clean OA thread context.
  - Interruption/timeout handling: behavior when blocked waits, task waits, or scheduled work are interrupted or time
    out.

  3. Invariants

  A. Thread-Safety Invariants

  1. CONC-THREAD-001: Shared mutable state must be guarded consistently
     Invariant: mutable state shared across threads must be synchronized, volatile, atomic, or safely published.
     Why it matters: stale executor/pool/scheduler state can create duplicate resources, rejected work, or false
     lifecycle state.
     Locations: OAExecutorService.executorService, OAScheduledExecutorService.scheduledExecutorService, OAPool.alReso
     urce, OAThrottle.aiMsLast/aiCnt/msDelay.
     Confidence: Medium.
     Gaps: executor/scheduler lazy fields are not volatile/synchronized; OAThrottle.msDelay is mutable non-volatile.
  2. CONC-THREAD-002: Pool accounting must match actual checked-out resources
     Invariant: OAPool.currentUsed must equal the number of entries marked used=true that represent real checked-out
     resources.
     Why it matters: incorrect accounting can block callers or shrink/remove resources incorrectly.
     Locations: OAPool.get, release, remove, loadMinimum.
     Confidence: Medium.
     Gaps: create-failure paths can leave reserved slots/accounting inconsistent.

  B. Memory Visibility Invariants

  3. CONC-VIS-001: Lifecycle state must be visible before accepting work
     Invariant: submit/schedule calls must observe current active/closed state before recording or accepting work.
     Why it matters: prevents false accepted metrics and tasks entering closed infrastructure.
     Locations: OAExecutorService.submit, submitAndWait, close; OAScheduledExecutorService.schedule*.
     Confidence: Low/Medium.
     Gaps: no explicit closed state; close() leaves executor reference non-null.
  4. CONC-VIS-002: Dynamic pool configuration must be visible to waiters
     Invariant: changes to min/max/high-water configuration must be visible to pool users and must wake waiters when
     capacity increases.
     Why it matters: runtime pool-size tuning must not leave callers blocked on stale limits.
     Locations: OAPool.setMinimum, setMaximum, setHighMarkTimeLimit, get.
     Confidence: Low.
     Gaps: config writes are unsynchronized and do not notify.

  C. Locking / Deadlock Invariants

  5. CONC-LOCK-001: Pool callbacks must not run while holding the pool lock
     Invariant: create() and removed() callbacks must execute outside synchronized(alResource).
     Why it matters: callbacks can block, close sockets, or call external code; running them under lock risks deadlock
     and stalls.
     Locations: OAPool.get, loadMinimum, release, remove.
     Confidence: High.
     Gaps: current structure mostly honors this; failure rollback still needs care.
  6. CONC-LOCK-002: Waiters must be notified after availability or capacity changes
     Invariant: any operation that makes a resource available or frees capacity must notify blocked waiters.
     Why it matters: missed wakeups can create permanent stalls.
     Locations: OAPool.release, remove, add, loadMinimum, setMaximum.
     Confidence: Medium.
     Gaps: shrink/remove path in release and setMaximum capacity increases need explicit wakeups.

  D. Lifecycle / Start / Stop Invariants

  7. CONC-LIFE-001: Executors must have explicit active/closed semantics
     Invariant: once closed, an executor wrapper must reject new work predictably before updating accepted-work state.
     Why it matters: avoids false success, false metrics, and ambiguous shutdown behavior.
     Locations: OAExecutorService.close, submit, submitAndWait.
     Confidence: Medium.
     Gaps: close delegates to shutdown() only; wrapper lacks terminal state.
  8. CONC-LIFE-002: Schedulers must expose shutdown ownership
     Invariant: a scheduler that owns a ScheduledExecutorService must expose idempotent close/shutdown semantics.
     Why it matters: recurring tasks can otherwise retain runtime objects indefinitely.
     Locations: OAScheduledExecutorService.getScheduledExecutorService, scheduleEvery.
     Confidence: Low.
     Gaps: no close/shutdown method.
  9. CONC-LIFE-003: Helper classes must state whether they are one-shot or reusable
     Invariant: classes with mutable run-state such as OAConcurrent must define and enforce reuse/concurrent-run
     behavior.
     Why it matters: reusing the same helper concurrently can corrupt barrier/latch state.
     Locations: OAConcurrent.run, fields countDownLatch, barrier.
     Confidence: Low/Medium.
     Gaps: not synchronized and not documented as one-shot.

  E. Task Submission / Execution Invariants

  10. CONC-TASK-001: Accepted work must either execute or fail visibly
     Invariant: if submit/schedule returns a Future, the task must be accepted by the underlying executor; if not
     accepted, failure must be visible and metrics must not claim acceptance.
     Why it matters: runtime services depend on background work not being silently lost.
     Locations: OAExecutorService.submit*, OAScheduledExecutorService.schedule*.
     Confidence: Medium.
     Gaps: counters increment before acceptance.
  11. CONC-TASK-002: submitAndWait timeout semantics must be explicit
     Invariant: timeout must define whether unfinished work continues or is cancelled.
     Why it matters: continuing timed-out work can conflict with caller retry/abort logic.
     Locations: OAExecutorService.submitAndWait.
     Confidence: Low.
     Gaps: current method leaves task running after timeout.

  F. Ordering / Determinism Invariants

  12. CONC-ORDER-001: Ordering guarantees must match executor mode
     Invariant: cached/unbounded executor mode provides no ordering guarantee; fixed-size mode provides queue order
     only up to concurrent worker execution.
     Why it matters: event/sync/replication code must not rely on ordering unless explicitly serialized.
     Locations: OAExecutorService.getExecutorService.
     Confidence: High.
     Gaps: package-level docs should make ordering limits explicit.
  13. CONC-ORDER-002: Single-thread scheduler serializes execution but long tasks delay following tasks
     Invariant: OAScheduledExecutorService task ordering is single-threaded; long-running tasks delay later tasks.
     Why it matters: scheduled runtime work can drift under load.
     Locations: OAScheduledExecutorService.getScheduledExecutorService.
     Confidence: High.
     Gaps: behavior is documented, but no diagnostics for drift/starvation.

  G. Cancellation / Interruption Invariants

  14. CONC-INT-001: Interrupted waits must preserve interruption semantics
     Invariant: a blocking utility must either propagate interruption or restore the interrupted status and fail
     visibly.
     Why it matters: shutdown/cancel logic depends on interrupts.
     Locations: OAPool.get, OAConcurrent.run.
     Confidence: Low/Medium.
     Gaps: OAPool.get swallows InterruptedException; OAConcurrent does not cancel spawned workers if caller wait is
     interrupted.
  15. CONC-CANCEL-001: Scheduled and submitted work must expose cancellation through returned futures
     Invariant: returned Future/ScheduledFuture must be the caller’s cancellation handle; wrapper lifecycle must not
     hide cancellation state.
     Why it matters: runtime owners need a way to stop delayed/recurring work.
     Locations: OAExecutorService.submit*, OAScheduledExecutorService.schedule*.
     Confidence: Medium.
     Gaps: scheduler lacks aggregate lifecycle cancellation.

  H. Exception-Handling Invariants

  16. CONC-EXC-001: Recurring scheduled task exceptions must not silently kill recurring runtime work unless
     contracted
     Invariant: recurring task failure must be logged/visible and either continue or intentionally cancel.
     Why it matters: JDK scheduled executors suppress future executions after thrown exceptions.
     Locations: OAScheduledExecutorService.scheduleEvery.
     Confidence: Low.
     Gaps: no wrapper catches/logs recurring task exceptions.
  17. CONC-EXC-002: Pool cleanup callback failure must not leave ownership ambiguous
     Invariant: if removed(resource) fails, pool ownership and resource state must still be defined.
     Why it matters: failed socket/resource cleanup can leave stale external resources.
     Locations: OAPool.remove, release, OARemoteMultiplexerClient pool removed.
     Confidence: Medium.
     Gaps: callbacks can throw after state mutation.
  18. CONC-EXC-003: Concurrent helper task failures must be observable by caller when caller expects coordinated
     success
     Invariant: batch helpers should aggregate worker failures or document log-only behavior.
     Why it matters: false success hides failed concurrent work.
     Locations: OAConcurrent.run.
     Confidence: Low/Medium.
     Gaps: worker exceptions are logged only.

  I. ThreadLocal / OAThreadLocal Cleanup Invariants

  19. CONC-TL-001: This package does not implicitly propagate OAThreadLocal state
     Invariant: tasks submitted to OAExecutorService or OAScheduledExecutorService run without automatic
     OAThreadLocal/context propagation unless caller wraps them.
     Why it matters: transaction, sync, replay, user context, and graph context must not leak or be assumed.
     Locations: OAExecutorService docs, OAScheduledExecutorService docs, higher-level callers such as OALoader.
     Confidence: High.
     Gaps: package-info references OAThread, which could confuse the contract.
  20. CONC-TL-002: Callers that install OAThreadLocal state inside tasks must clean it in finally
     Invariant: any task that adds sibling helpers, transaction state, sync suppression, or runtime context must
     restore it with try/finally.
     Why it matters: pooled worker threads can otherwise leak context across tasks.
     Locations: higher-level callers, e.g. OALoader worker runnables.
     Confidence: Medium.
     Gaps: enforcement is external to this package.

  J. Resource Cleanup Invariants

  21. CONC-RESOURCE-001: Pool resources must have exactly one owner at a time
     Invariant: a pooled resource must be either available in the pool, checked out to one caller, or removed; never
     duplicated or both checked out and available.
     Why it matters: duplicate socket/resource use corrupts remote calls and shared state.
     Locations: OAPool.get, release, remove, add.
     Confidence: Medium.
     Gaps: add can duplicate resources; double release is silently ignored.
  22. CONC-RESOURCE-002: Scheduler/executor daemon threads must not retain unbounded runtime state after owner
     shutdown
     Invariant: background threads and queued tasks must be drainable or cancellable by the owning runtime component.
     Why it matters: long-running OA servers need deterministic cleanup.
     Locations: OAExecutorService.close, OAScheduledExecutorService.
     Confidence: Low/Medium.
     Gaps: scheduler has no owner cleanup; executor close does not await/drain/cancel.

  K. Integration Invariants

  23. CONC-INTEGRATION-001: Runtime packages must not rely on these wrappers for transaction/sync context propagation
     Invariant: transaction, sync, replication, trigger, and remote code must explicitly capture/restore context when
     dispatching tasks through these wrappers.
     Why it matters: wrong context causes sync echo, transaction leaks, or graph routing errors.
     Locations: OAExecutorService, OAScheduledExecutorService, users in load, process, remote, replication.
     Confidence: Medium.
     Gaps: no common task wrapper for OA context.
  24. CONC-INTEGRATION-002: Remote/socket pooling must be robust to transient create/close failure
     Invariant: concurrency primitives used by remoting must not corrupt pool capacity or leave blocked callers after
     socket failures.
     Why it matters: remote/sync/replication depend on socket pool availability.
     Locations: OAPool, OARemoteMultiplexerClient.getVirtualSocketCtoSPool.
     Confidence: Low/Medium.
     Gaps: create-failure and notify paths need hardening.

  4. Listener / Callback Semantics

  The package does not define OA business listeners, but it executes caller-supplied callbacks:

  - OAExecutorService: executes Runnable/Callable in worker threads.
  - OAScheduledExecutorService: executes scheduled Runnable/Callable.
  - OAPool: invokes create() and removed() callbacks.
  - OAConcurrent: executes supplied runnables.

  Alignment:

  - OAPool mostly aligns with “no callbacks while locks are held” by running create() and removed() outside the
    synchronized block.
  - OAExecutorService and OAScheduledExecutorService delegate exception semantics to Future/JDK executor behavior.

  Conflicts/gaps:

  - Recurring scheduled callbacks can stop after one exception without package-level aggregation/reporting.
  - OAConcurrent observer-style runnable failures are logged and not aggregated to the caller.
  - OAPool.removed() exception handling is not aggregated and can escape after pool state mutation.
  - No built-in BEFORE/DURING/AFTER distinction exists; higher-level packages must apply OA listener policy before
    dispatching into these primitives.

  5. Failure Modes

  - Race condition from unsafely published executor/scheduler fields.
  - Stale read of pool maximum/minimum after runtime tuning.
  - Deadlock/stall from missed pool notification.
  - Starvation from single-thread scheduler blocked by long-running task.
  - Callback failure after pool state mutation leaving resource ownership ambiguous.
  - Worker thread task failure hidden in Future or logged only.
  - Recurring scheduled task silently stops after exception.
  - Task executes after caller times out and retries.
  - Task submitted after shutdown is rejected after metrics are incremented.
  - Duplicate pooled resource through external add.
  - Lost interrupt in OAPool.get.
  - Caller interrupted while OAConcurrent workers keep running.
  - ThreadLocal or transaction context leaks across pooled executor workers if caller fails cleanup.
  - Sync/replay context leakage if task wrappers do not restore OAThreadLocal state.
  - Runtime shutdown leaves scheduled recurring tasks alive.

  6. Test Recommendations

  - testOAPoolCreateFailureRollsBackReservedSlot
  - testOAPoolReleaseShrinkNotifiesWaiter
  - testOAPoolInterruptedGetRestoresInterrupt
  - testOAPoolSetMaximumWakesWaitingGetter
  - testOAPoolAddRejectsDuplicateResource
  - testOAExecutorSubmitAfterCloseRejectedBeforeCounterIncrement
  - testOAExecutorSubmitAndWaitTimeoutLeavesOrCancelsByContract
  - testOAExecutorConcurrentSubmitCloseHasDeterministicOutcome
  - testOASchedulerRecurringTaskExceptionLoggedAndContinuesOrCancelsByContract
  - testOASchedulerCloseCancelsRecurringTasks
  - testOASchedulerDailyTimeRecomputesAcrossDstIfWallClockContract
  - testOAConcurrentWorkerExceptionPropagatesOrAggregates
  - testOAConcurrentConcurrentRunRejectedOrSerialized
  - testTaskDoesNotInheritOAThreadLocalUnlessWrapped
  - testCallerInstalledThreadLocalCleanedAfterExecutorTask
  - testLongRunningScheduledTaskDelaysNextTaskIsObservable
  - testHighContentionPoolMaintainsCurrentUsedInvariant

  7. Hardening Recommendations

  - Add explicit lifecycle state to OAExecutorService and OAScheduledExecutorService: ACTIVE, SHUTTING_DOWN, CLOSED.
  - Add close()/shutdown() to OAScheduledExecutorService.
  - Track accepted vs attempted task submissions separately.
  - Wrap recurring scheduled tasks in exception-isolating runners.
  - Define submitAndWait timeout semantics and offer cancel-on-timeout option.
  - Fix OAPool create-failure rollback and notify-on-capacity-change rules.
  - Preserve interrupt status in blocking waits.
  - Synchronize or make volatile dynamic pool configuration fields.
  - Enforce or document OAPool.add capacity/duplicate behavior.
  - Make OAConcurrent.run one-shot, synchronized, or local-state-only.
  - Clarify package-info: no OAThread class exists here, and executor wrappers do not propagate OAThreadLocal.
  - Add package-level diagnostics counters for rejected tasks, task exceptions, pool waits, create failures, and
    scheduled task failures.
  - Document lock ownership: alResource protects pool list, used flags, wait count, current used, and capacity
    decisions.
  - Add a standard OA task wrapper in a higher-level runtime package for OAThreadLocal capture/restore if desired.

  8. Open Questions

  - Should OAExecutorService.close() drain, await, cancel, or only reject new work?
  - Should submitAndWait cancel unfinished work on timeout?
  - Should OAScheduledExecutorService be owned here or folded into com.viaoa.schedule lifecycle semantics?
  - Is scheduleEvery(OATime) intended to mean fixed 24-hour interval or local wall-clock daily execution?
  - Should recurring scheduled task exceptions cancel by design, or should OA default to log-and-continue?
  - Is OAPool.add intended as an override that can exceed max, or should it obey max?
  - Should OAPool.get expose checked exceptions/interruption, or keep unchecked runtime semantics?
  - Should this package provide OAThreadLocal-aware executor wrappers, or should all context propagation remain in
    runtime services?
  - Is OAConcurrent intended for tests only, or production batch coordination too?
  - Should daemon-thread use be universal here, or should production runtime services be able to request non-daemon
    ownership?




qqqqqqqqqqqqqqqq other

A. Architectural Assessment

  The package is conceptually simple and useful: small wrappers over JDK concurrency primitives plus a reusable pool
  and throttle. It is not yet production-infrastructure complete. The strongest class is the general shape of OAPool,
  but its failure paths need hardening. The executor wrappers need explicit lifecycle and failure semantics if they
  are used by runtime services.

  Ownership boundaries are mostly clear, but lifecycle semantics are implicit. OAExecutorService.close() exists but
  does not define drain/await/cancel behavior. OAScheduledExecutorService lacks lifecycle ownership entirely.

  Synchronization responsibilities are mostly local, but failure cleanup and interrupt handling are weak.

  B. Invariant Risk Areas

  - Reserved pool slots must be released if resource creation fails.
  - Blocking waits must preserve interruption semantics.
  - Scheduled recurring work must not disappear silently after one exception.
  - Scheduler/executor ownership must include shutdown and task cancellation rules.
  - Timeout from a submitted task must define whether unfinished work continues.
  - Background workers must not silently report success after task failure.
  - OAThreadLocal propagation/cleanup is explicitly not handled; runtime callers must not assume it is.

  C. Top Production Risks

  1. Pool capacity leak after failed create() causing permanent blocked remote/socket callers.
  2. Recurring scheduled runtime work stopping silently after an uncaught exception.
  3. Scheduler tasks leaking because OAScheduledExecutorService has no close/shutdown lifecycle.
  4. Timed executor work continuing after timeout and applying changes after caller retry/abort.
  5. Interrupted pool waiters ignoring shutdown/cancellation.

  D. Hardening Recommendations

  - Add explicit lifecycle state to executor/scheduler wrappers: active, closing, closed.
  - Add close() to OAScheduledExecutorService, cancel recurring tasks where owned, and reject scheduling after close.
  - Wrap scheduled recurring tasks with exception logging/containment.
  - Fix OAPool.get/loadMinimum to roll back reserved slots on create failure.
  - Restore interrupt status and fail visibly on interrupted pool waits.
  - Define timeout semantics for submitAndWait: cancel-on-timeout or wait-only.
  - Add diagnostics counters: pool create failures, pool wait count, scheduled task failures, rejected submissions.
  - Add comments/spec stating that OAThreadLocal context is not propagated by these wrappers unless explicitly wrapped
    by the caller.


Updated Top Risk Areas

  - OAPool bounded-pool wakeup/capacity semantics are the most important remaining issue.
  - Executor/scheduler lifecycle state is implicit and should be made explicit before these wrappers carry critical
    sync/replication work.
  - OAThreadLocal propagation is not part of this package, so callers must wrap tasks intentionally.

*/
