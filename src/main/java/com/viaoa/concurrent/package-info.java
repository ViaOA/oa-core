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


//CODEX unit tests <todo>

/* CODEX Invariants

CONC-RUNTIME-001 — OA Concurrent Runtime Primitives
Contract statement:
com.viaoa.concurrent defines lightweight OA concurrency primitives for task execution, scheduling, object pooling,
throttling, and coordinated runnable start; these primitives provide execution boundaries for higher-level OA
runtime services but do not themselves own OAObject, Hub, transaction, sync, replication, or graph semantics.
Rationale:
Higher-level OA systems rely on this package for background and concurrent execution. The package must make its
concurrency guarantees explicit so callers do not infer ordering, lifecycle, context propagation, or semantic
completion that the primitives do not provide.
Source scope:
OAExecutorService, OAScheduledExecutorService, OAPool, OAThrottle, OAConcurrent, package-info.java.
Related CODEX findings:
Existing package-info notes implicit lifecycle state, missing scheduler shutdown, pool capacity leaks, timeout
ambiguity, and ThreadLocal propagation boundaries.
Suggested unit tests:
testExecutorSchedulerPoolThrottleContractsAreExplicit(), testConcurrentPrimitivesDoNotImplyGraphSemanticSuccess(),
testRuntimeCallersMustOwnOAContextPropagation()
Spec target section:
Concurrent Runtime / Core Responsibility

CONC-LIFECYCLE-001 — Executor Lifecycle State
Contract statement:
OAExecutorService must have deterministic active, closing, and closed behavior: once closed, new work must be
rejected predictably before accepted-work state or metrics claim success.
Rationale:
Runtime services must not believe background work was accepted after shutdown or during partial closure.
Source scope:
OAExecutorService.submit(Runnable), submit(Callable), submitAndWait(...), close(), getExecutorService(),
getQueueSize(), getThreadPoolSize(), getActiveThreads().
Related CODEX findings:
close() delegates to shutdown() without explicit wrapper terminal state; submit counters can increment before
acceptance.
Suggested unit tests:
testOAExecutorSubmitAfterCloseRejectedBeforeCounterIncrement(),
testOAExecutorConcurrentSubmitCloseHasDeterministicOutcome(), testOAExecutorCloseRejectsNewWork()
Spec target section:
Concurrent Runtime / Executor Lifecycle

CONC-LIFECYCLE-002 — Scheduler Lifecycle Ownership
Contract statement:
OAScheduledExecutorService must have explicit shutdown/close ownership semantics for delayed and recurring tasks,
including whether pending and recurring tasks are cancelled, drained, rejected, or allowed to continue.
Rationale:
Scheduled tasks can retain runtime objects, keep daemon activity alive, or continue mutating state after the owner
is stopped.
Source scope:
OAScheduledExecutorService.schedule(...), scheduleEvery(...), getScheduledExecutorService().
Related CODEX findings:
OAScheduledExecutorService has no shutdown/close lifecycle.
Suggested unit tests:
testOASchedulerCloseCancelsRecurringTasksByContract(), testOASchedulerRejectsSchedulingAfterClose(),
testOASchedulerPendingTaskLifecycleIsExplicit()
Spec target section:
Concurrent Runtime / Scheduler Lifecycle

CONC-LIFECYCLE-003 — One-Shot Versus Reusable Helpers
Contract statement:
Helpers with mutable run coordination state must define whether instances are one-shot, reusable after completion,
or safe for concurrent run calls, and must enforce that lifecycle.
Rationale:
Reusing mutable coordination state ambiguously can corrupt barriers, latches, completion status, and failure
reporting.
Source scope:
OAConcurrent.run(), OAConcurrent constructor state, countDownLatch, barrier, runnable array.
Related CODEX findings:
OAConcurrent reuse/concurrent-run behavior is not synchronized or explicitly documented.
Suggested unit tests:
testOAConcurrentRunIsOneShotOrReusableByContract(), testOAConcurrentConcurrentRunRejectedOrSerialized(),
testOAConcurrentSecondRunHasDefinedBehavior()
Spec target section:
Concurrent Runtime / Helper Lifecycle

CONC-TASK-001 — Accepted Work Execution or Visible Failure
Contract statement:
A submitted or scheduled task that returns a Future/ScheduledFuture must either be accepted by the underlying
executor/scheduler or fail visibly to the caller; metrics and lifecycle state must not claim acceptance for rejected
work.
Rationale:
OA background work must not be silently lost, especially when used by load, sync, replication, remote, trigger, or
process services.
Source scope:
OAExecutorService.submit(...), submitAndWait(...), OAScheduledExecutorService.schedule(...), scheduleEvery(...).
Related CODEX findings:
OAExecutorService counters increment before acceptance; scheduler/executor closed-state ambiguity.
Suggested unit tests:
testAcceptedExecutorTaskRunsOrFutureReportsFailure(), testRejectedExecutorTaskDoesNotIncrementAcceptedCounter(),
testScheduledTaskRejectionIsVisible()
Spec target section:
Concurrent Runtime / Task Submission Semantics

CONC-TASK-002 — Task Completion Boundary
Contract statement:
Task completion, Future completion, transport completion, and semantic OA runtime completion must remain distinct;
this package only reports task execution completion unless an owning subsystem defines a stronger semantic boundary.
Rationale:
A Runnable can finish while graph mutation, sync delivery, replication capture, or callback semantics remain
incomplete or failed.
Source scope:
OAExecutorService, OAScheduledExecutorService, OAConcurrent, higher-level runtime callers.
Related CODEX findings:
Existing package-info notes distinction between concurrent task execution and higher-level OA runtime semantics.
Suggested unit tests:
testFutureCompletionDoesNotImplyGraphSemanticCommit(), testRuntimeOwnerDefinesSemanticCompletionSeparately(),
testTaskFailureDoesNotAppearAsOperationSuccess()
Spec target section:
Concurrent Runtime / Execution Boundary Semantics

CONC-TIMEOUT-001 — submitAndWait Timeout Semantics
Contract statement:
submitAndWait timeout behavior must explicitly define whether unfinished work is cancelled, left running, or
returned to the caller as an incomplete task; timeout must not be reported as successful completion.
Rationale:
Continuing timed-out work can conflict with caller retry, cancellation, transaction boundaries, or shutdown logic.
Source scope:
OAExecutorService.submitAndWait(Runnable, int, TimeUnit), submitAndWait(Callable, int, TimeUnit).
Related CODEX findings:
submitAndWait timeout does not cancel or classify unfinished task.
Suggested unit tests:
testOAExecutorSubmitAndWaitTimeoutDoesNotReportCompletion(),
testOAExecutorSubmitAndWaitTimeoutLeavesOrCancelsByContract(), testTimedOutTaskStateIsVisibleToCaller()
Spec target section:
Concurrent Runtime / Timeout Semantics

CONC-ORDER-001 — Executor Ordering Limits
Contract statement:
OAExecutorService must expose only the ordering guarantees provided by its configured executor mode: cached/
unbounded workers provide no global execution order, and fixed-size mode preserves queue order only up to concurrent
worker execution.
Rationale:
Event, sync, replication, object, and Hub services must not rely on ordering unless they explicitly serialize work.
Source scope:
OAExecutorService constructors, getExecutorService(), ThreadPoolExecutor configuration, submit methods.
Related CODEX findings:
Existing package-info notes ordering limits should be explicit.
Suggested unit tests:
testCachedExecutorDoesNotPromiseTaskOrdering(), testFixedExecutorQueueOrderDoesNotImplySerialExecution(),
testSerializedRuntimeWorkUsesSingleWorkerOrExternalOrdering()
Spec target section:
Concurrent Runtime / Ordering Semantics

CONC-SCHEDULE-001 — Scheduled Execution Semantics
Contract statement:
OAScheduledExecutorService must define delayed, fixed-period, and time-of-day scheduling in terms of initial delay,
period, clock basis, recurrence behavior, and drift when tasks run long.
Rationale:
Scheduled runtime behavior can affect polling, cleanup, sync, replication, and background maintenance. Time
ambiguity creates missed or duplicate work.
Source scope:
OAScheduledExecutorService.schedule(Runnable, OADateTime), schedule(Runnable, int, TimeUnit), schedule(Callable,
int, TimeUnit), scheduleEvery(Runnable, OATime), scheduleEvery(Runnable, int, int, TimeUnit).
Related CODEX findings:
Single-thread scheduler drift/starvation and daily time wall-clock/DST ambiguity are noted in package-info.
Suggested unit tests:
testOASchedulerDelayedTaskRunsAfterDelay(), testOASchedulerLongTaskDelaysFollowingTaskObservableByContract(),
testOASchedulerDailyTimeRecomputesAcrossDstIfWallClockContract()
Spec target section:
Concurrent Runtime / Scheduling Semantics

CONC-EXCEPTION-001 — Task Exception Visibility
Contract statement:
Exceptions thrown by submitted, scheduled, pooled, or coordinated callback tasks must be visible through Future
failure, caller-visible exception, logged diagnostic, or owner-defined aggregation; exceptions must not silently
kill required runtime work while reporting success.
Rationale:
Background task failures can stop recurring work, hide failed coordination, or leave runtime state partially
updated.
Source scope:
OAExecutorService submit methods, OAScheduledExecutorService scheduleEvery(...), OAConcurrent.run(),
OAPool.create(), OAPool.removed(...).
Related CODEX findings:
Recurring scheduled task exceptions can silently cancel future executions; OAConcurrent worker failures are logged
but not propagated; pool cleanup callback failures can escape after state mutation.
Suggested unit tests:
testScheduledRecurringTaskExceptionIsVisibleByContract(), testOAConcurrentWorkerExceptionPropagatesOrAggregates(),
testPoolRemovedFailureLeavesVisibleFailureState()
Spec target section:
Concurrent Runtime / Exception Semantics

CONC-INTERRUPT-001 — Interrupt Preservation
Contract statement:
Blocking waits and coordinated waits must either propagate InterruptedException or restore the interrupted status
and fail visibly; interrupts must not be swallowed as normal completion.
Rationale:
Shutdown, cancellation, failover, and timeout logic depend on Java interruption semantics.
Source scope:
OAPool.get(), OAConcurrent.run(), executor/scheduler waits owned by callers.
Related CODEX findings:
OAPool.get swallows InterruptedException; OAConcurrent does not cancel spawned workers if caller wait is
interrupted.
Suggested unit tests:
testOAPoolInterruptedGetRestoresInterrupt(), testOAConcurrentInterruptedRunFailsVisibly(),
testInterruptedWaitDoesNotReturnNormalSuccess()
Spec target section:
Concurrent Runtime / Interruption Semantics

CONC-CANCEL-001 — Cancellation Handle Semantics
Contract statement:
Returned Future and ScheduledFuture instances are the caller’s cancellation handles, and wrapper lifecycle behavior
must not hide cancellation state or imply cancellation of work that continues running.
Rationale:
Runtime owners need a reliable way to cancel delayed, recurring, or submitted work during shutdown, retry, or
failure.
Source scope:
OAExecutorService.submit(...), submitAndWait(...), OAScheduledExecutorService.schedule(...), scheduleEvery(...).
Related CODEX findings:
Scheduler lacks aggregate lifecycle cancellation; submitAndWait timeout does not define cancellation.
Suggested unit tests:
testSubmittedFutureCancellationPreventsOrReportsExecution(), testScheduledFutureCancellationStopsRecurringTask(),
testSubmitAndWaitTimeoutCancellationPolicyIsExplicit()
Spec target section:
Concurrent Runtime / Cancellation Semantics

CONC-POOL-001 — Pool Ownership State
Contract statement:
Each OAPool resource must be in exactly one ownership state at a time: available in the pool, checked out to one
caller, removed, or failed during creation; a resource must never be duplicated or both checked out and available.
Rationale:
Pooled resources often represent sockets or runtime handles. Duplicate ownership corrupts remote calls and shared
state.
Source scope:
OAPool.get(), release(TYPE), remove(TYPE), add(TYPE), getAllItems(), getCurrentSize(), getCurrentUsed().
Related CODEX findings:
OAPool.add can duplicate resources; double release is silently ignored; create-failure paths can leave reserved
slots/accounting inconsistent.
Suggested unit tests:
testOAPoolResourceHasSingleOwnerAtATime(), testOAPoolAddRejectsDuplicateResourceByContract(),
testOAPoolDoubleReleaseHasDefinedOutcome()
Spec target section:
Concurrent Runtime / Pool Ownership

CONC-POOL-002 — Pool Accounting Accuracy
Contract statement:
OAPool current size, current used count, available resources, waiters, and min/max capacity state must remain
consistent with actual resource ownership after get, release, add, remove, create failure, shrink, and load-minimum
operations.
Rationale:
Incorrect accounting can block callers permanently, exceed capacity, shrink live resources, or leak resources.
Source scope:
OAPool.getCurrentSize(), getCurrentUsed(), loadMinimum(), get(), release(TYPE), remove(TYPE), add(TYPE),
setMinimum(int), setMaximum(int).
Related CODEX findings:
Failed create() leaks pool capacity; shrink/remove path can miss notification; setMaximum capacity increases do not
notify waiters.
Suggested unit tests:
testOAPoolCreateFailureRollsBackReservedSlot(), testHighContentionPoolMaintainsCurrentUsedInvariant(),
testOAPoolSetMaximumWakesWaitingGetter()
Spec target section:
Concurrent Runtime / Pool Accounting

CONC-POOL-003 — Pool Callback Lock Boundary
Contract statement:
OAPool create() and removed(TYPE) callbacks must not execute while holding the pool state lock, and callback failure
must leave resource ownership and pool accounting in a defined state.
Rationale:
Pool callbacks can block, close sockets, create external resources, or invoke runtime services. Running them under
lock risks deadlock and stalls.
Source scope:
OAPool.create(), removed(TYPE), get(), loadMinimum(), release(TYPE), remove(TYPE), synchronized pool state.
Related CODEX findings:
Existing package-info notes current structure mostly honors no-callback-under-lock, but failure rollback needs care.
Suggested unit tests:
testOAPoolCreateRunsOutsidePoolLock(), testOAPoolRemovedRunsOutsidePoolLock(),
testPoolCallbackFailureDoesNotCorruptPoolAccounting()
Spec target section:
Concurrent Runtime / Pool Callback Semantics

CONC-POOL-004 — Pool Wait and Wake Semantics
Contract statement:
Any pool operation that makes a resource available, frees capacity, or changes capacity in a way that can satisfy a
waiter must notify blocked waiters; waiters must not remain blocked on stale capacity or availability state.
Rationale:
Missed wakeups create production stalls in remote/socket and background resource pools.
Source scope:
OAPool.get(), release(TYPE), remove(TYPE), add(TYPE), loadMinimum(), setMaximum(int), setMinimum(int).
Related CODEX findings:
Release shrink/remove path and setMaximum capacity increases need explicit wakeups.
Suggested unit tests:
testOAPoolReleaseNotifiesWaiter(), testOAPoolReleaseShrinkNotifiesWaiter(), testOAPoolSetMaximumWakesWaitingGetter()
Spec target section:
Concurrent Runtime / Pool Wait Semantics

CONC-VISIBILITY-001 — Shared State Publication
Contract statement:
Mutable state shared across threads must be safely published through synchronization, volatile, atomic variables,
immutable construction, or documented single-thread ownership.
Rationale:
Stale lifecycle, delay, pool, or executor state can produce duplicate resources, accepted work after close,
incorrect throttling, or blocked waiters.
Source scope:
OAExecutorService.executorService, OAScheduledExecutorService.scheduledExecutorService, OAPool mutable fields,
OAThrottle.msDelay, OAThrottle aiMsLast/aiCnt.
Related CODEX findings:
Executor/scheduler lazy fields are not explicitly synchronized/volatile; OAThrottle.msDelay is mutable non-volatile;
dynamic pool config writes are unsynchronized.
Suggested unit tests:
testExecutorServiceLazyInitSafelyPublishesExecutor(), testSchedulerLazyInitSafelyPublishesExecutor(),
testThrottleDelayUpdateVisibleToConcurrentCheck()
Spec target section:
Concurrent Runtime / Memory Visibility

CONC-THROTTLE-001 — Throttle Single-Pass Interval
Contract statement:
OAThrottle.check() must allow at most one successful pass per configured delay interval under concurrent access,
unless reset or configuration change explicitly defines otherwise.
Rationale:
Throttle is commonly used for event/log suppression or rate-limited runtime behavior; multiple concurrent passes
violate suppression guarantees.
Source scope:
OAThrottle.check(), now(), aiMsLast, aiCnt.
Related CODEX findings:
Check-then-set race allows multiple concurrent callers through for the same interval.
Suggested unit tests:
testOAThrottleAllowsOnlyOneConcurrentPassPerInterval(), testOAThrottleConcurrentCheckDoesNotDoublePass(),
testOAThrottleCheckCountMatchesSuccessfulPasses()
Spec target section:
Concurrent Runtime / Throttle Semantics

CONC-THROTTLE-002 — Throttle Delay Contract
Contract statement:
OAThrottle delay must be non-negative, visible to concurrent checkers, and evaluated without overflow-producing
elapsed-time logic; invalid delay values must fail visibly or be normalized by explicit contract.
Rationale:
Negative, stale, or overflowed delay values can silently disable throttling or suppress work incorrectly.
Source scope:
OAThrottle.OAThrottle(long), setDelay(long), getDelay(), check(), now().
Related CODEX findings:
msDelay is mutable but not volatile/atomic; negative delays are accepted and make check pass; aiMsLast.get() +
msDelay can overflow.
Suggested unit tests:
testOAThrottleRejectsOrNormalizesNegativeDelay(), testOAThrottleDelayUpdateVisibleAcrossThreads(),
testOAThrottleLargeDelayDoesNotOverflowToImmediatePass()
Spec target section:
Concurrent Runtime / Throttle Delay Semantics

CONC-THROTTLE-003 — Throttle Reset and Counter Semantics
Contract statement:
OAThrottle reset, count, check count, and last-throttle timestamp semantics must be deterministic under the
documented concurrency model and must not expose misleading values.
Rationale:
Throttle diagnostics and runtime suppression decisions depend on accurate counters and timestamps.
Source scope:
OAThrottle.reset(), getCheckCount(), getCount(), getLastThrottle(), check().
Related CODEX findings:
reset() is not atomic with respect to check(); getLastThrottle JavaDoc says total check count while implementation
returns timestamp.
Suggested unit tests:
testOAThrottleResetHasDefinedConcurrentBehavior(), testOAThrottleGetLastThrottleReturnsTimestampByContract(),
testOAThrottleCountersMatchSuccessfulChecks()
Spec target section:
Concurrent Runtime / Throttle Diagnostics

CONC-CLOCK-001 — Time Source Semantics
Contract statement:
Time-based concurrency behavior must define whether it uses wall-clock time or monotonic elapsed time, and must
document effects of clock changes, DST, and scheduling drift.
Rationale:
Wall-clock changes can alter throttling and scheduling behavior; runtime callers must know whether the primitive is
suitable for elapsed-time suppression or wall-clock scheduling.
Source scope:
OAThrottle.now(), OAThrottle.check(), OAScheduledExecutorService.schedule(Runnable, OADateTime),
scheduleEvery(Runnable, OATime).
Related CODEX findings:
OAThrottle uses System.currentTimeMillis for elapsed control; scheduleEvery(OATime) daily wall-clock semantics are
an open question.
Suggested unit tests:
testOAThrottleClockBackwardBehaviorIsDefined(), testOASchedulerTimeOfDayBehaviorIsDefinedAcrossClockBoundary(),
testTimeBasedConcurrencyUsesDocumentedClockSource()
Spec target section:
Concurrent Runtime / Time Source Semantics

CONC-TL-001 — No Implicit OAThreadLocal Propagation
Contract statement:
Tasks submitted to OAExecutorService or OAScheduledExecutorService do not automatically inherit, propagate, or clean
OAThreadLocal, transaction, sync, replication, security, user, or graph context unless the caller wraps the task.
Rationale:
Assuming implicit context propagation can leak or lose transaction, sync, replay, security, or runtime graph state
on pooled daemon threads.
Source scope:
OAExecutorService, OAScheduledExecutorService, higher-level callers in load, process, remote, sync, replication,
trigger, transaction, and runtime packages.
Related CODEX findings:
Existing package-info notes this package does not implicitly propagate OAThreadLocal state.
Suggested unit tests:
testTaskDoesNotInheritOAThreadLocalUnlessWrapped(), testExecutorTaskStartsWithoutCallerRuntimeContextByDefault(),
testSchedulerTaskStartsWithoutCallerRuntimeContextByDefault()
Spec target section:
Concurrent Runtime / ThreadLocal Context

CONC-TL-002 — Caller-Owned Context Cleanup
Contract statement:
Any task that installs OAThreadLocal, transaction, sync, replication, security, loading, or runtime context while
running through these primitives must restore the previous value with try/finally.
Rationale:
Pooled worker threads can otherwise leak context into later unrelated tasks.
Source scope:
Task execution through OAExecutorService, OAScheduledExecutorService, OAConcurrent; higher-level OA task wrappers
and callers.
Related CODEX findings:
Existing package-info notes enforcement is external to this package.
Suggested unit tests:
testCallerInstalledThreadLocalCleanedAfterExecutorTask(), testCallerInstalledThreadLocalCleanedAfterScheduledTask(),
testTaskExceptionDoesNotLeakRuntimeContext()
Spec target section:
Concurrent Runtime / ThreadLocal Cleanup

CONC-RESOURCE-001 — Owned Resource Cleanup
Contract statement:
Executors, schedulers, pool resources, background workers, and queued tasks owned by an OA runtime component must be
drainable, cancellable, closed, or explicitly transferred at that owner’s shutdown boundary.
Rationale:
Long-running OA runtimes must not retain threads, queued work, sockets, callbacks, or graph references after
shutdown.
Source scope:
OAExecutorService.close(), OAScheduledExecutorService scheduling methods, OAPool.remove/release/removed, OAConcurre
nt worker threads.
Related CODEX findings:
Scheduler has no close/shutdown; executor close does not await/drain/cancel; pool removed callback failure can leave
ownership ambiguous.
Suggested unit tests:
testOwnerShutdownClosesExecutorResources(), testSchedulerRecurringTasksDoNotLeakAfterOwnerShutdown(),
testPoolRemoveClosesOrTransfersResourceByContract()
Spec target section:
Concurrent Runtime / Resource Cleanup

CONC-DEADLOCK-001 — Lock and Callback Isolation
Contract statement:
This package must avoid invoking caller code while holding internal locks unless explicitly documented, and must
preserve a clear lock ownership boundary for pool state and coordination state.
Rationale:
Caller code can block, reenter OA services, close resources, or submit new tasks; running it under internal locks
risks deadlock, starvation, and lock amplification.
Source scope:
OAPool.get(), release(TYPE), remove(TYPE), loadMinimum(), OAConcurrent.run(), executor/scheduler task execution
boundaries.
Related CODEX findings:
OAPool mostly aligns with no-callback-under-lock, but failure rollback still needs care.
Suggested unit tests:
testPoolCreateCallbackCanReenterWithoutDeadlock(), testPoolRemovedCallbackCanBlockWithoutHoldingPoolLock(),
testInternalLocksReleasedWhenCallbackThrows()
Spec target section:
Concurrent Runtime / Locking Semantics

CONC-INTEGRATION-001 — Cross-Package Concurrency Boundary
Contract statement:
Object, Hub, cache, event, callback, trigger, sync, replication, transaction, remote, graph, and runtime packages
must not rely on these primitives for semantic ordering, transaction context, sync context, or graph authority
unless those guarantees are explicitly provided by the caller or owner.
Rationale:
These classes are concurrency primitives. Higher-level packages own OA semantic correctness and must define
serialization, context, ordering, and commit boundaries around concurrent execution.
Source scope:
com.viaoa.concurrent.*, callers across object, hub, cache, trigger, queue/process, remote, sync, replication,
transaction, datasource, graph, and runtime packages.
Related CODEX findings:
Existing package-info notes runtime packages must not rely on wrappers for transaction/sync context propagation and
that ordering guarantees are limited.
Suggested unit tests:
testRuntimePackageWrapsTaskWhenContextRequired(), testSyncReplicationWorkUsesExplicitOrderingBoundary(),
testConcurrentGraphMutationRequiresOwnerDefinedSerialization()
Spec target section:
Concurrent Runtime / Cross-Package Integration

*/
