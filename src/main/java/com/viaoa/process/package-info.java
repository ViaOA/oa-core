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
 * Process-management utilities for event-driven, scheduled, and long-running
 * operations within OA applications. <p>
 *
 * The package includes:
 * <ul>
 *   <li>{@link com.viaoa.process.OAChangeProcessor} – triggers processing when
 *       Hub or property-path changes occur.</li>
 *   <li>{@link com.viaoa.process.OAChangeRefresher} – background refresher that
 *       coalesces multiple change events into serialized processing.</li>
 *   <li>{@link com.viaoa.process.OACron} – cron-style schedule definition and
 *       next-execution computation.</li>
 *   <li>{@link com.viaoa.process.OACronProcessor} – executes cron jobs using a
 *       background daemon thread.</li>
 *   <li>{@link com.viaoa.process.OAProcess} – tracks state and lifecycle of
 *       asynchronous or background processes.</li>
 *   <li>{@link com.viaoa.process.OAThreadMonitor} – diagnostic tool for thread
 *       inspection.</li>
 * </ul>
 *
 * These utilities integrate with OA's concurrency, Hub event stream, and
 * temporal classes to provide robust infrastructure for background processing.
 */
package com.viaoa.process;


/* CODEX Invariants


Process Invariants

  ID: PROC-LIFECYCLE-001
  Contract statement: Every process-like component must have a clear lifecycle state: not started, starting, running,
  stopping/canceling, completed, failed, canceled, or closed.
  Rationale: OA background work often coordinates Hub events, cache refreshes, sync/replication, and derived state.
  Consumers must know whether work is active, complete, failed, or abandoned.
  Source locations: OAProcess; OAChangeRefresher.start/stop/runThread; OACronProcessor.start/stop/runThread;
  OACron.setEnabled/getEnabled.
  Related CODEX findings: timeout state checks inverted; refresher/cron processor lifecycle state can lie; finalizer-
  only cleanup.
  Suggested unit tests: testProcessLifecycleStateTransitions, testRefresherStartStopStateIsCommitted,
  testCronProcessorStopDoesNotReportStoppedBeforeThreadExit.
  Spec target section: Process Runtime / Lifecycle Semantics

  ID: PROC-START-001
  Contract statement: Starting a process must either be idempotent or visibly reject duplicate/invalid starts. It must
  not silently create competing workers for the same process unless explicitly contracted.
  Rationale: Duplicate workers can process the same Hub event, refresh, or cron schedule more than once.
  Source locations: OAChangeRefresher.start; OACronProcessor.start.
  Related CODEX findings: OAChangeRefresher.start() always creates a new daemon thread; OACronProcessor.start() has
  similar duplicate-start exposure.
  Suggested unit tests: testRefresherDuplicateStartRejectedOrIdempotent,
  testCronProcessorDuplicateStartDoesNotCreateCompetingSchedulers.
  Spec target section: Process Runtime / Start Semantics

  ID: PROC-STOP-001
  Contract statement: Stop must be explicit, observable, and must not silently abandon required cleanup or leave
  lifecycle state ambiguous.
  Rationale: OA shutdown and restart flows need deterministic cleanup for listeners, workers, scheduled jobs, and
  pending refresh state.
  Source locations: OAChangeRefresher.stop; OACronProcessor.stop; OAChangeProcessor.finalize;
  OAChangeRefresher.finalize.
  Related CODEX findings: finalizer-only listener cleanup; OACronProcessor.isRunning() can lie during stop and after
  worker exit.
  Suggested unit tests: testStopWaitsOrSignalsWorkerExit, testStopRemovesHubListeners, testStopClosesOwnedExecutor.
  Spec target section: Process Runtime / Stop and Close Semantics

  ID: PROC-CANCEL-001
  Contract statement: Cancellation has two distinct states: cancellation requested and cancellation confirmed.
  Requesting cancellation must not falsely imply that work has stopped.
  Rationale: Long-running OA work may be cooperative. Callers need to distinguish “please cancel” from “cancel
  completed.”
  Source locations: OAProcess.requestCancel; OAProcess.confirmRequestToCancel; OAProcess.setWasCancelled;
  OAProcess.getRequestedToCancel.
  Related CODEX findings: none observed beyond cross-thread visibility concerns.
  Suggested unit tests: testCancelRequestDoesNotMarkDone, testConfirmCancelRequiresRequest,
  testCancelReasonAndTimesRemainConsistent.
  Spec target section: Process Runtime / Cancellation Semantics

  ID: PROC-CANCEL-002
  Contract statement: A canceled process must not also silently report successful completion unless the contract
  explicitly allows completed-after-cancel and records that distinction.
  Rationale: OA runtime decisions can depend on whether derived state, sync work, or background refresh actually
  completed.
  Source locations: OAProcess.setWasCancelled; OAProcess.setDone; OAProcess.getDone; OAProcess.getWasCancelled.
  Related CODEX findings: lifecycle state is implicit and independent booleans can become contradictory.
  Suggested unit tests: testCanceledProcessDoesNotReportSuccessfulDoneByDefault,
  testDoneAndCanceledStateIsExplicitlyDistinguishable.
  Spec target section: Process Runtime / Terminal State Semantics

  ID: PROC-STATUS-001
  Contract statement: Status, done, failure, cancellation, pause, blocking, timeout, and last-run state must reflect
  committed process state, not stale or transient state.
  Rationale: UI, monitoring, retry logic, and runtime orchestration use process status to decide whether to wait,
  cancel, retry, or proceed.
  Source locations: OAProcess.getStatus/setStatus; OAProcess.getDone/setDone; OAProcess.getException/setException; OA
  Process.isTimedout; OAProcess.isBlockTimedout; OACron.getLast/setLast.
  Related CODEX findings: timeout checks inverted; cron last-run timestamp is set before job success; non-volatile
  status/control fields.
  Suggested unit tests: testTimeoutOnlyTrueAfterMaxTimeElapsed, testBlockTimeoutOnlyTrueAfterBlockLimitElapsed,
  testCronLastSuccessUpdatedOnlyAfterSuccessfulProcess.
  Spec target section: Process Runtime / Status Accuracy

  ID: PROC-PROGRESS-001
  Contract statement: Progress step state must be internally consistent: current step must refer to a valid committed
  step, and total/current values must not expose impossible progress unless explicitly allowed.
  Rationale: Progress is used for runtime observability and user feedback. Invalid progress can mask stalled or failed
  work.
  Source locations: OAProcess.setSteps; OAProcess.getSteps; OAProcess.setCurrentStep; OAProcess.getCurrentStep;
  OAProcess.getTotalSteps.
  Related CODEX findings: none observed.
  Suggested unit tests: testProgressStepWithinDefinedRange, testProgressStateRemainsConsistentAfterStepsChanged.
  Spec target section: Process Runtime / Progress Semantics

  ID: PROC-FAIL-001
  Contract statement: Process failure must be visible and must not silently appear as successful completion.
  Rationale: Background failures can leave OA cache, Hub contents, datasource work, sync messages, or replication
  state incomplete.
  Source locations: OAProcess.setException; OAChangeRefresher.runThread; OAChangeProcessor.onProcess;
  OACronProcessor.callProcess/callProcessInAnotherThread.
  Related CODEX findings: async OAChangeProcessor.process exceptions are captured by discarded Future; cron job
  exceptions are captured by discarded Future; refresher logs but consumes failed change.
  Suggested unit tests: testAsyncChangeProcessorFailureIsObservable, testCronProcessFailureIsLoggedOrRecorded,
  testRefresherFailureDoesNotAppearSuccessful.
  Spec target section: Process Runtime / Failure Visibility

  ID: PROC-FAIL-002
  Contract statement: Coalesced/background work must not acknowledge or consume a unit of work before it has completed
  successfully, unless failed state remains visible and retryable.
  Rationale: Event coalescing is useful only if failures do not erase the pending need to refresh or recompute.
  Source locations: OAChangeRefresher.aiChange; OAChangeRefresher.lastChange; OAChangeRefresher.runThread.
  Related CODEX findings: lastChange advances before process() succeeds.
  Suggested unit tests: testRefresherFailedProcessKeepsChangePending, testRefresherRetriesAfterTransientFailure.
  Spec target section: Process Runtime / Work Acknowledgement

  ID: PROC-RETRY-001
  Contract statement: Retry/restart after failure must not reuse corrupted state or hide previous failure.
  Rationale: OA runtime failures may be transient. Retry must not skip work because state was prematurely marked
  complete.
  Source locations: OAChangeRefresher.runThread; OACronProcessor.callProcess; OAProcess.exception/doneTime; OACron.dt
  Last.
  Related CODEX findings: refresher consumes failed work; cron last is set before success.
  Suggested unit tests: testRetryAfterRefresherFailureProcessesSameChange,
  testCronRetryAfterFailureDoesNotUseFalseLastSuccess.
  Spec target section: Process Runtime / Retry Semantics

  ID: PROC-THREAD-001
  Contract statement: Worker threads created by process utilities must have explicit ownership, lifecycle, and
  shutdown behavior.
  Rationale: Daemon workers are convenient but can hide leaked runtime work, missed shutdown, or duplicate processing.
  Source locations: OAChangeRefresher.thread; OAChangeRefresher.start/stop/runThread; OACronProcessor.thread;
  OACronProcessor.start/stop/runThread; OAChangeProcessor.execService; OACronProcessor.execService.
  Related CODEX findings: no explicit close for owned executors/listeners; start can create duplicate workers; stop
  lifecycle state can lie.
  Suggested unit tests: testOwnedWorkerThreadTerminatesOnStop, testOwnedExecutorClosedOnClose,
  testNoOrphanWorkerAfterRepeatedStartStop.
  Spec target section: Process Runtime / Worker Lifecycle

  ID: PROC-THREAD-002
  Contract statement: Background callbacks must not rely on caller thread context unless the process contract
  explicitly captures and restores that context.
  Rationale: Hub events, sync replay, transaction state, and security/context flags may be ThreadLocal. Running work
  later on another daemon thread changes semantics unless context is controlled.
  Source locations: OAChangeProcessor.onProcess; OAChangeRefresher.runThread; OACronProcessor.callProcessInAnotherThr
  ead; OAThreadLocalService.setProcess/getProcess.
  Related CODEX findings: none observed directly in process package, but async dispatch discards contextual failure
  visibility.
  Suggested unit tests: testAsyncProcessDoesNotLeakCallerThreadLocal, testCronProcessRunsWithCleanThreadLocalBaseline,
  testChangeRefresherRestoresThreadContextAfterProcess.
  Spec target section: Process Runtime / Thread Context Semantics

  ID: PROC-INTERRUPT-001
  Contract statement: Interrupt handling must preserve Java interrupt semantics and OA lifecycle correctness.
  Interrupts must not be swallowed in a way that leaves workers running indefinitely or status misleading.
  Rationale: Stop/shutdown and enterprise runtime interruption depend on workers reacting predictably.
  Source locations: OAChangeRefresher.runThread; OACronProcessor.runThread; Thread.sleep/wait usage.
  Related CODEX findings: none observed specifically, but catch-all Exception loops can swallow InterruptedException.
  Suggested unit tests: testRefresherInterruptStopsOrRestoresInterrupt,
  testCronProcessorInterruptStopsOrRestoresInterrupt.
  Spec target section: Process Runtime / Interrupt Semantics

  ID: PROC-RESOURCE-001
  Contract statement: Resources acquired by a process must be released on success, failure, cancellation, and stop.
  Rationale: Process helpers register Hub listeners and own executors/threads. Leaks cause duplicate callbacks,
  retained object graphs, and stale background work.
  Source locations: OAChangeProcessor.addListener/finalize; OAChangeRefresher.addListener/finalize; OAChangeProcessor
  .execService; OACronProcessor.execService.
  Related CODEX findings: finalizer-only cleanup for listeners; no explicit close path for OAChangeProcessor; executor
  ownership not closed.
  Suggested unit tests: testCloseRemovesAllRegisteredHubListeners, testClosePreventsFutureCallbacks,
  testCloseReleasesExecutorThreads.
  Spec target section: Process Runtime / Resource Cleanup

  ID: PROC-TL-001
  Contract statement: Any process code that sets OAThreadLocal or runtime context state must restore it with try/
  finally.
  Rationale: Process workers commonly run on reusable background threads. Leaked context can corrupt sync, datasource,
  graph ownership, transaction, or trigger behavior.
  Source locations: OAThreadLocalService.getProcess/setProcess; process worker hooks in OAChangeRefresher.process, OA
  ChangeProcessor.process, OACron.process.
  Related CODEX findings: none observed directly in this package; invariant imported from OA runtime contract.
  Suggested unit tests: testProcessThreadLocalRestoredAfterSuccess, testProcessThreadLocalRestoredAfterException,
  testCronThreadLocalDoesNotLeakBetweenJobs.
  Spec target section: Process Runtime / ThreadLocal Cleanup

  ID: PROC-CONCURRENT-001
  Contract statement: Concurrent start, stop, cancel, status, enable/disable, and progress operations must not corrupt
  lifecycle state or produce contradictory observations.
  Rationale: Process objects are naturally accessed by runtime worker threads, UI/status threads, and shutdown
  threads.
  Source locations: OAProcess fields; OAChangeRefresher.aiStartStop/thread/lastChange;
  OACronProcessor.aiStartStop/thread; OACron.bEnabled/dtLast; OACron.findNext.
  Related CODEX findings: non-volatile process fields; OACron.findNext mutable shared dtFrom; cron enabled/last state
  visibility; start/stop state ambiguity.
  Suggested unit tests: testConcurrentStartStopDoesNotCreateContradictoryState,
  testConcurrentCronEnableDisableVisibleToScheduler, testConcurrentFindNextReturnsIndependentResults.
  Spec target section: Process Runtime / Concurrency Semantics

  ID: PROC-SCHEDULE-001
  Contract statement: A scheduled process must execute only when its schedule is valid and matches the current
  scheduling window. Invalid schedules must fail closed.
  Rationale: Cron-style processes are production automation; malformed schedules must not silently broaden into
  unintended execution.
  Source locations: OACron.getInts; OACron.isValid; OACron.findNext; OACronProcessor.runThread/add.
  Related CODEX findings: invalid findNext returning null can be wrapped as current time; cron parser can silently
  broaden malformed fields to wildcard.
  Suggested unit tests: testInvalidCronIsNeverExecuted, testMalformedStarFieldIsInvalidNotWildcard,
  testReversedCronRangeIsInvalidNotEveryValue.
  Spec target section: Process Runtime / Schedule Semantics

  ID: PROC-SCHEDULE-002
  Contract statement: A scheduled process must have an explicit overlap policy: skip, queue, reject, or allow
  concurrent executions.
  Rationale: Long-running cron jobs can otherwise duplicate side effects or corrupt shared runtime state.
  Source locations: OACronProcessor.runThread; OACronProcessor.callProcessInAnotherThread; OACron.process.
  Related CODEX findings: same cron can overlap itself across schedule ticks.
  Suggested unit tests: testCronDoesNotOverlapWhenPolicyIsNoOverlap, testLongRunningCronNextTickPolicyIsExplicit.
  Spec target section: Process Runtime / Schedule Execution Semantics

  ID: PROC-EVENT-001
  Contract statement: Hub/property-path change processors must register exactly the listener needed for the requested
  event scope, and must not silently miss eligible events.
  Rationale: Process helpers are often used to refresh filtered caches, derived views, or downstream runtime state
  from Hub events. Missed events produce stale state.
  Source locations: OAChangeProcessor.addListener; OAChangeRefresher.addListener; Hub listener registration calls.
  Related CODEX findings: OAChangeProcessor.addListener(Hub,String) simple-property branch constructs but does not
  register listener.
  Suggested unit tests: testChangeProcessorSimplePropertyListenerFires, testChangeProcessorPathListenerFires,
  testChangeProcessorHubMembershipListenerFiresWhenRequested.
  Spec target section: Process Runtime / Event Integration

  ID: PROC-EVENT-002
  Contract statement: Event-driven process dispatch must preserve the intended execution mode: synchronous callbacks
  may fail the caller, while asynchronous observer callbacks must make failures observable without blocking unrelated
  dispatch.
  Rationale: OA-wide listener policy distinguishes participants from observers. Background process dispatch usually
  acts as observer infrastructure.
  Source locations: OAChangeProcessor.onProcess; OAChangeRefresher.runThread;
  OACronProcessor.callProcessInAnotherThread.
  Related CODEX findings: async exceptions hidden by discarded Future; refresher logs and continues but consumes
  failed work.
  Suggested unit tests: testSynchronousChangeProcessorExceptionPropagates, testAsyncChangeProcessorExceptionRecorded,
  testObserverFailureDoesNotKillDispatcher.
  Spec target section: Process Runtime / Callback Semantics

  ID: PROC-INTEGRATION-001
  Contract statement: Process behavior must remain compatible with OA queue, runtime, object, Hub, sync, replication,
  datasource, load, and trigger contracts.
  Rationale: Process utilities often sit at boundaries between event detection, background work, cache refresh, and
  runtime propagation. They must not violate ordering, identity, ThreadLocal, or loaded-state expectations.
  Source locations: OAChangeRefresher usage in OAObjectCacheFilter; OAChangeProcessor; OACronProcessor;
  OAThreadLocalService.process.
  Related CODEX findings: failed refresh can leave derived/cache/filter state stale; listener cleanup leak can cause
  stale callbacks.
  Suggested unit tests: testCacheFilterRefresherFailureDoesNotMarkRefreshComplete,
  testBackgroundProcessDoesNotEmitSyncFromWrongContext, testProcessCleanupDoesNotLeaveStaleHubListener.
  Spec target section: Process Runtime / Cross-Package Compatibility

  Suggested Package-Level Spec Summary

  com.viaoa.process is responsible for lightweight OA process orchestration: observable long-running process state,
  event-driven background refresh, Hub/property-path change processing, cron-style scheduling, and diagnostic process/
  thread support.

  It must guarantee:

  - Process lifecycle state is explicit enough for callers to distinguish running, stopping, completed, failed,
    canceled, and closed.
  - Start, stop, cancel, retry, and restart behavior is deterministic.
  - Completion, failure, cancellation, timeout, and progress state do not lie.
  - Background work failure is visible and does not silently consume pending work.
  - Scheduled jobs execute only for valid schedules and matching windows.
  - Cron overlap policy is explicit.
  - Worker threads and owned executors do not leak or leave orphaned work.
  - Hub listeners registered by process helpers are removed by explicit cleanup.
  - ThreadLocal/runtime context used during background work is restored.
  - Concurrent lifecycle/status calls do not corrupt state or report impossible combinations.
  - Process utilities integrate safely with Hub events, cache refresh, datasource/load work, sync, replication,
    triggers, and runtime shutdown.

  It must never silently:

  - Treat failed work as completed.
  - Treat an invalid schedule as valid.
  - Consume a pending refresh before successful processing.
  - Report stopped while required cleanup is still running without making that transitional state explicit.
  - Leak listeners, workers, executors, or ThreadLocal context.
  - Run duplicate process workers unless that behavior is contracted.
  - Hide async callback failures in discarded futures.

  Likely unit-test categories:

  - OAProcess lifecycle, timeout, cancellation, failure, progress, and visibility tests.
  - OAChangeProcessor listener registration, synchronous/asynchronous exception, and cleanup tests.
  - OAChangeRefresher coalescing, failure retry, stop/start, and listener cleanup tests.
  - OACron parser validity, next-time determinism, malformed schedule, and concurrent findNext tests.
  - OACronProcessor invalid schedule, overlap policy, stop/restart, failure visibility, and last-success tests.
  - ThreadLocal/context cleanup tests for all background worker paths.

  Likely stress/failure-test categories:

  - Concurrent start/stop/refresh storms.
  - Hub event storms with failing refresh work.
  - Long-running cron job overlap under minute-boundary pressure.
  - Repeated schedule add/remove while processor runs.
  - Runtime shutdown while background process is executing.
  - Async callback exceptions under high event volume.
  - ThreadLocal leakage across reused executor threads.



qqqqqqqqqqqqqqqqqqq other

Architectural Assessment

  The package is conceptually useful but uneven. OACron and OACronProcessor form a basic scheduler-like subsystem;
  OAChangeRefresher and OAChangeProcessor are event-to-background-work adapters; OAProcess is a state holder. The main
  architectural weakness is that lifecycle is implicit: start/stop/close, success/failure, retry, and listener
  ownership are not represented as explicit states.

  Ownership boundaries are also fuzzy. Hub listeners are registered by process helpers but not explicitly
  deregistered. Executor ownership is created internally but not closed. Cron job success is mixed with attempt state.

  Invariant Risk Areas

  - Process timeout semantics are not enforced by tests or state names.
  - Listener registration must be symmetrical with deregistration, but cleanup is finalizer-based.
  - Background work failure must be observable, but several paths discard Future results.
  - Refresh coalescing must not consume failed work, but lastChange is committed before processing.
  - Cron last must represent a committed successful run, but it is written before job execution.
  - Cron next-time calculation assumes single-threaded access to mutable instance search state.

  Top Production Risks

  1. Missed change processing from the unregistered simple-property listener in OAChangeProcessor.
  2. Stale derived/cache/filter state from OAChangeRefresher consuming failed refreshes.
  3. Cron jobs silently failing while last reports success.
  4. Listener/executor leaks from finalizer-only cleanup.
  5. Wrong timeout decisions from inverted timeout checks.

  Hardening Recommendations

  - Add explicit lifecycle states to OAChangeRefresher, OAChangeProcessor, and possibly OAProcess.
  - Add explicit close() methods for listener deregistration and executor shutdown.
  - Treat submitted background task failures as observable: log, aggregate, or expose last failure.
  - Split cron timing into lastAttempt, lastSuccess, and lastFailure.
  - Make OACron.findNext stateless or synchronized.
  - Add focused tests for timeout checks, simple-property listener registration, failed refresh retry, cron failure
    not updating success state, and close removing listeners.


*/

