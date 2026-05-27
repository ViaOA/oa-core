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

//CODEX unit tests <todo>

/* CODEX Invariants

PROC-LIFECYCLE-001 — Managed Process Lifecycle
Contract statement:
Process-like components must expose deterministic lifecycle semantics for initialization, start, running, stop/
cancel request, completion, failure, cancellation, and cleanup.
Rationale:
OA background work can coordinate Hub events, cache refresh, scheduled automation, datasource work, sync/
replication, and derived graph state; callers must be able to distinguish active, terminal, failed, and abandoned
work.
Source scope:
OAProcess; OAChangeProcessor; OAChangeRefresher; OACron; OACronProcessor; OAThreadMonitor.
Related CODEX findings:
OAProcess timeout state checks; OAChangeRefresher and OACronProcessor lifecycle state ambiguity; finalizer-only
cleanup references.
Suggested unit tests:
processLifecycleStateTransitionsAreObservable(), refresherStartStopLifecycleIsCommitted(),
cronProcessorStopStateReflectsWorkerExit().
Spec target section:
Process Runtime / Lifecycle Semantics.

PROC-START-001 — Start Idempotency and Worker Ownership
Contract statement:
Starting a managed process or processor must either be idempotent or visibly reject invalid duplicate starts; it
must not silently create competing workers for the same process scope.
Rationale:
Duplicate workers can process the same event, refresh request, or schedule tick more than once and can corrupt
derived runtime state.
Source scope:
OAChangeRefresher.start(); OACronProcessor.start(); worker thread ownership in OAChangeRefresher and
OACronProcessor.
Related CODEX findings:
OAChangeRefresher.start duplicate daemon thread exposure; OACronProcessor.start duplicate scheduler exposure.
Suggested unit tests:
refresherDuplicateStartIsIdempotentOrRejected(), cronProcessorDuplicateStartDoesNotCreateCompetingSchedulers().
Spec target section:
Process Runtime / Start Semantics.

PROC-STOP-001 — Stop and Cleanup Visibility
Contract statement:
Stop, cancel, close, or cleanup operations must be explicit and observable, and must not silently leave owned
listeners, workers, executors, or pending process state active.
Rationale:
Runtime shutdown and restart require deterministic release of process resources so stale callbacks and background
work do not affect later Object Graph state.
Source scope:
OAChangeProcessor listener registration/finalize; OAChangeRefresher.stop(), finalize(), listener registration;
OACronProcessor.stop(); executor and worker ownership.
Related CODEX findings:
finalizer-only listener cleanup; executor ownership not explicitly closed; OACronProcessor stop/running state
ambiguity.
Suggested unit tests:
stopRemovesOwnedHubListeners(), stopTerminatesOwnedWorkerThread(), stopReleasesOwnedExecutorResources().
Spec target section:
Process Runtime / Stop and Cleanup Semantics.

PROC-CANCEL-001 — Cancellation Request Versus Cancellation Completion
Contract statement:
Cancellation request and cancellation completion are distinct process states; requesting cancellation must not by
itself imply that process work has stopped or been safely cleaned up.
Rationale:
Long-running OA work may be cooperative, and callers need to know whether cancellation is pending, confirmed, or
terminal.
Source scope:
OAProcess.requestCancel(...), OAProcess.confirmRequestToCancel(), OAProcess.setWasCancelled(...),
OAProcess.getRequestedToCancel(), OAProcess.getWasCancelled().
Related CODEX findings:
cross-thread visibility and independent lifecycle-flag concerns.
Suggested unit tests:
cancelRequestDoesNotMarkProcessDone(), confirmCancelRequiresRequest(), cancelReasonAndTimesRemainConsistent().
Spec target section:
Process Runtime / Cancellation Semantics.

PROC-TERMINAL-001 — Terminal State Consistency
Contract statement:
Completed, failed, and canceled terminal states must remain distinguishable and must not silently report
contradictory success.
Rationale:
OA orchestration, retry, UI status, and runtime decisions depend on whether work finished successfully, failed, or
was canceled before completion.
Source scope:
OAProcess.setDone(), OAProcess.getDone(), OAProcess.setException(...), OAProcess.getException(),
OAProcess.setWasCancelled(...), OAProcess.getWasCancelled(), OAProcess done/cancel/failure fields.
Related CODEX findings:
implicit lifecycle state represented by independent booleans can become contradictory.
Suggested unit tests:
canceledProcessDoesNotReportSuccessfulCompletionByDefault(), failedProcessDoesNotReportCleanSuccess(),
terminalStatesAreExplicitlyDistinguishable().
Spec target section:
Process Runtime / Terminal State Semantics.

PROC-STATUS-001 — Status, Timeout, and Progress Accuracy
Contract statement:
Status, done time, failure state, timeout state, block state, cancellation state, pause state, and progress state
must reflect committed process state, not stale, inverted, or transient observations.
Rationale:
UI, monitoring, retry, scheduling, and runtime orchestration use process status to decide whether to wait, retry,
cancel, or proceed.
Source scope:
OAProcess status/done/exception/cancel/pause/progress APIs; OAProcess.isTimedout(); OAProcess.isBlockTimedout();
OACron.getLast(); OACron.setLast(...).
Related CODEX findings:
OAProcess timeout checks; cron last-run timestamp success ambiguity; non-volatile process status/control field
concerns.
Suggested unit tests:
timeoutOnlyTrueAfterMaxTimeElapsed(), blockTimeoutOnlyTrueAfterBlockLimitElapsed(),
progressStepRemainsWithinDefinedRange(), cronLastSuccessReflectsCommittedRun().
Spec target section:
Process Runtime / Status and Progress Semantics.

PROC-FAIL-001 — Process Failure Visibility
Contract statement:
Process execution failure must be caller-visible or observable and must not silently appear as successful
completion, successful refresh, or successful schedule execution.
Rationale:
Background failures can leave cache, Hub contents, datasource work, sync messages, replication state, or derived
runtime views incomplete.
Source scope:
OAProcess.setException(...); OAChangeProcessor.onProcess(...); OAChangeRefresher.runThread();
OACronProcessor.callProcess(...); OACronProcessor.callProcessInAnotherThread(...).
Related CODEX findings:
async OAChangeProcessor.process exceptions captured by discarded Future; cron job exceptions captured by discarded
Future; refresher logs but consumes failed change.
Suggested unit tests:
asyncChangeProcessorFailureIsObservable(), cronProcessFailureIsRecordedOrObservable(),
refresherFailureDoesNotAppearSuccessful().
Spec target section:
Process Runtime / Failure Visibility.

PROC-WORK-001 — Work Acknowledgement After Completion
Contract statement:
Coalesced or background work must not be acknowledged, consumed, or marked current before the required process
action has completed successfully unless failed state remains visible and retryable.
Rationale:
Event coalescing and background refresh are safe only when failures do not erase the pending need to recompute or
refresh.
Source scope:
OAChangeRefresher.refresh(); OAChangeRefresher.hasChanged(); OAChangeRefresher.isChanged();
OAChangeRefresher.runThread(); change counters and last processed state.
Related CODEX findings:
OAChangeRefresher lastChange advances before process() succeeds.
Suggested unit tests:
refresherFailedProcessKeepsChangePending(), refresherRetriesPendingChangeAfterTransientFailure(),
refresherAcknowledgesChangeOnlyAfterSuccess().
Spec target section:
Process Runtime / Work Acknowledgement Semantics.

PROC-RETRY-001 — Retry and Restart Correctness
Contract statement:
Retry, restart, or subsequent scheduled execution after failure must not reuse corrupted process state, skip
required work, or hide the prior failure.
Rationale:
OA process failures can be transient; retry must preserve correctness of pending work, schedule state, and
observability.
Source scope:
OAChangeRefresher.runThread(); OACronProcessor.callProcess(...); OACron.getLast()/setLast(...); OAProcess exception
and done state.
Related CODEX findings:
refresher consumes failed work; cron last timestamp set before job success.
Suggested unit tests:
retryAfterRefresherFailureProcessesSameChange(), cronRetryAfterFailureDoesNotUseFalseLastSuccess(),
processRetryPreservesPriorFailureVisibility().
Spec target section:
Process Runtime / Retry Semantics.

PROC-SCHEDULE-001 — Schedule Validity and Deterministic Next Run
Contract statement:
Cron-style process schedules must parse and evaluate deterministically, execute only when valid, and fail closed for
invalid or unresolvable schedules.
Rationale:
Scheduled OA processes can automate production state changes; malformed schedules must not silently broaden into
unintended execution.
Source scope:
OACron constructor; OACron.getMinutes(); OACron.getHours(); OACron.getMonthDays(); OACron.getDaysOfWeek();
OACron.getMonths(); OACron.isValid(); OACron.getIsValid(); OACron.findNext(...); OACron.getNext(...);
OACronProcessor.runThread().
Related CODEX findings:
invalid findNext returning null can be wrapped as current time; cron parser can silently broaden malformed fields to
wildcard; reversed range concerns.
Suggested unit tests:
invalidCronIsNeverExecuted(), malformedCronFieldIsInvalidNotWildcard(), reversedCronRangeIsInvalid(),
cronFindNextIsDeterministicForSameInput().
Spec target section:
Process Runtime / Schedule Semantics.

PROC-SCHEDULE-002 — Scheduled Execution Overlap Policy
Contract statement:
Scheduled process execution must have explicit behavior for overlapping runs: skip, queue, reject, or allow
concurrent execution; the selected policy must be observable and deterministic.
Rationale:
Long-running cron jobs can otherwise duplicate side effects, corrupt shared runtime state, or hide missed schedule
windows.
Source scope:
OACron.process(...); OACronProcessor.runThread(); OACronProcessor.callProcess(...);
OACronProcessor.callProcessInAnotherThread(...).
Related CODEX findings:
same cron can overlap itself across schedule ticks.
Suggested unit tests:
cronLongRunningJobOverlapPolicyIsExplicit(), cronDoesNotSilentlyRunCompetingExecutionsWhenOverlapDisallowed().
Spec target section:
Process Runtime / Scheduled Execution Semantics.

PROC-EVENT-001 — Hub and Property Event Scope
Contract statement:
Event-driven process components must register exactly the listener scope requested for Hub membership, property, and
property-path changes, and must not silently miss eligible events.
Rationale:
Process helpers often refresh filtered caches, derived views, or downstream runtime state from Hub events; missed
events produce stale runtime behavior.
Source scope:
OAChangeProcessor.addListener(...); OAChangeRefresher.addListener(...); HubListener registration for Hub and
property-path events.
Related CODEX findings:
OAChangeProcessor.addListener(Hub, String) simple-property branch constructs but does not register a listener.
Suggested unit tests:
changeProcessorSimplePropertyListenerFires(), changeProcessorPropertyPathListenerFires(),
changeProcessorHubMembershipListenerFiresWhenRequested().
Spec target section:
Process Runtime / Event Integration Semantics.

PROC-CALLBACK-001 — Synchronous and Asynchronous Callback Boundaries
Contract statement:
Process callbacks must preserve their intended execution mode: synchronous callbacks may fail the caller directly,
while asynchronous callbacks must make failures observable without blocking unrelated dispatch.
Rationale:
OA process utilities sit between event detection and background work; callback failure semantics must not be lost
when work moves across threads.
Source scope:
OAChangeProcessor.process(...); OAChangeProcessor.onProcess(...); OAChangeRefresher.process();
OAChangeRefresher.runThread(); OACron.process(...); OACronProcessor.callProcessInAnotherThread(...).
Related CODEX findings:
async exceptions hidden by discarded Future; refresher logs and continues after failed work.
Suggested unit tests:
synchronousChangeProcessorExceptionPropagates(), asynchronousChangeProcessorExceptionIsObservable(),
observerFailureDoesNotKillUnrelatedDispatch().
Spec target section:
Process Runtime / Callback Execution Semantics.

PROC-THREAD-001 — Worker Thread Lifecycle and Ownership
Contract statement:
Worker threads and executors created by process utilities must have explicit ownership, lifecycle, shutdown, and
leak-prevention behavior.
Rationale:
Daemon workers and background executors can retain object graphs, listeners, runtime context, or stale work if their
lifecycle is not controlled.
Source scope:
OAChangeProcessor executor; OAChangeRefresher worker thread; OACronProcessor worker thread and executor;
OAThreadMonitor diagnostic thread inspection.
Related CODEX findings:
duplicate worker creation; no explicit executor close; finalizer-only cleanup; orphan worker risk.
Suggested unit tests:
ownedWorkerThreadTerminatesOnStop(), ownedExecutorDoesNotLeakAfterCleanup(),
repeatedStartStopLeavesNoOrphanWorkers().
Spec target section:
Process Runtime / Worker Ownership Semantics.

PROC-INTERRUPT-001 — Interrupt and Shutdown Semantics
Contract statement:
InterruptedException and thread interruption during process waits, sleeps, and worker loops must preserve OA
lifecycle correctness and Java interrupt semantics according to the process contract.
Rationale:
Shutdown, cancellation, and container-managed interruption require workers to react predictably instead of silently
continuing with misleading status.
Source scope:
OAChangeRefresher.runThread(); OACronProcessor.runThread(); wait/sleep usage in process workers.
Related CODEX findings:
catch-all loop concerns can swallow InterruptedException.
Suggested unit tests:
refresherInterruptStopsOrRestoresInterrupt(), cronProcessorInterruptStopsOrRestoresInterrupt(),
interruptedWorkerDoesNotReportFalseSuccess().
Spec target section:
Process Runtime / Interrupt Semantics.

PROC-TL-001 — Runtime Context Restoration
Contract statement:
Any process code that sets OA ThreadLocal or runtime context state must restore prior state with try/finally,
including success, failure, cancellation, and interruption paths.
Rationale:
Process workers often run on reusable background threads; leaked context can corrupt graph ownership, transaction,
sync, datasource, security, trigger, or callback behavior.
Source scope:
OAThreadLocalService process context; OAChangeProcessor.process(...); OAChangeRefresher.process();
OACron.process(...); OACronProcessor callback execution boundaries.
Related CODEX findings:
no direct process-specific ThreadLocal bug noted; invariant follows OA runtime context contract.
Suggested unit tests:
processThreadLocalRestoredAfterSuccess(), processThreadLocalRestoredAfterException(),
cronThreadLocalDoesNotLeakBetweenJobs().
Spec target section:
Process Runtime / ThreadLocal and Runtime Context Semantics.

PROC-CONCURRENT-001 — Concurrent State Visibility
Contract statement:
Concurrent start, stop, cancel, enable/disable, schedule lookup, status, progress, and failure operations must not
corrupt shared process state or expose impossible state combinations.
Rationale:
Process state is commonly read and written by worker threads, UI/status threads, shutdown threads, and runtime event
threads.
Source scope:
OAProcess fields and accessors; OAChangeRefresher start/stop/change state; OACronProcessor start/stop state; OACron
enabled/last/next state; OACron.findNext(...).
Related CODEX findings:
non-volatile process fields; mutable OACron findNext search state; cron enabled/last visibility concerns; start/stop
state ambiguity.
Suggested unit tests:
concurrentStartStopDoesNotCreateContradictoryState(), concurrentCronEnableDisableVisibleToScheduler(),
concurrentFindNextReturnsIndependentResults().
Spec target section:
Process Runtime / Concurrency Semantics.

PROC-RESOURCE-001 — Listener and Resource Cleanup
Contract statement:
Resources acquired by process utilities, including Hub listeners, executors, worker threads, and retained process
references, must be released on explicit cleanup and must not depend on finalization for correctness.
Rationale:
Resource leaks can cause duplicate callbacks, retained Object Graphs, stale cache refreshes, and unexpected
background work.
Source scope:
OAChangeProcessor.addListener(...), finalize(); OAChangeRefresher.addListener(...), finalize(); OAChangeProcessor
executor; OACronProcessor executor.
Related CODEX findings:
finalizer-only listener cleanup; no explicit close path for OAChangeProcessor; executor ownership not closed.
Suggested unit tests:
closeRemovesAllRegisteredHubListeners(), closePreventsFutureCallbacks(), closeReleasesExecutorThreads().
Spec target section:
Process Runtime / Resource Cleanup Semantics.

PROC-MANUAL-001 — Manual Versus Scheduled Execution Semantics
Contract statement:
Manual process execution and scheduled process execution must be distinguishable when exposed by the API, and must
follow the same failure visibility and lifecycle rules.
Rationale:
Manual intervention and scheduled automation can have different operational meaning, but neither may bypass process
correctness contracts.
Source scope:
OACron.process(boolean bManuallyCalled); OACronProcessor.callProcess(...);
OACronProcessor.callProcessInAnotherThread(...).
Related CODEX findings:
cron job failures can be hidden by asynchronous execution path.
Suggested unit tests:
manualCronExecutionPassesManualFlag(), scheduledCronExecutionPassesScheduledFlag(), manualCronFailureIsObservable().
Spec target section:
Process Runtime / Manual and Scheduled Execution Semantics.

PROC-OBSERVE-001 — Runtime Process Observability
Contract statement:
Process state, progress, scheduling, failure, cancellation, and cleanup outcomes must be observable enough for OA
runtime monitoring, UI, diagnostics, and operational support to distinguish current state from stale or unknown
state.
Rationale:
OA processes can run long-lived background work; runtime operators and higher-level systems need accurate visibility
into what work happened and what remains pending.
Source scope:
OAProcess status/progress/cancel/failure APIs; OACron name/description/created/last/next APIs; OACronProcessor
running state; OAThreadMonitor.
Related CODEX findings:
status/control visibility concerns; timeout checks; cron last-success ambiguity.
Suggested unit tests:
processStatusReflectsCommittedStateTransitions(), cronDescriptionAndNextRunReflectCurrentSchedule(),
threadMonitorProvidesObservableThreadState().
Spec target section:
Process Runtime / Observability Semantics.

PROC-INTEGRATION-001 — Cross-Package Runtime Compatibility
Contract statement:
Process utilities must preserve OA runtime contracts when coordinating with Hub events, callbacks, concurrency,
transactions, datasource work, cache refresh, object graph mutation, sync, replication, remote execution, and
triggers.
Rationale:
com.viaoa.process often sits at boundaries between event detection, background execution, derived state refresh, and
runtime propagation; it must not violate ordering, identity, ThreadLocal, loaded-state, or failure semantics owned
by those packages.
Source scope:
OAChangeProcessor; OAChangeRefresher; OACron; OACronProcessor; OAProcess; OAThreadMonitor; integration with hub,
object, cache, callback, concurrent, transaction, datasource, sync, replication, remote, trigger, and graph
packages.
Related CODEX findings:
failed refresh can leave derived/cache/filter state stale; listener cleanup leak can cause stale callbacks; async
process failure visibility concerns.
Suggested unit tests:
cacheFilterRefresherFailureDoesNotMarkRefreshComplete(), backgroundProcessDoesNotEmitSyncFromWrongContext(),
processCleanupDoesNotLeaveStaleHubListener().
Spec target section:
Process Runtime / Cross-Package Compatibility.

PROC-BOUNDARY-001 — Process Success Versus Object Graph Success
Contract statement:
Successful process execution only establishes that the managed process action completed according to its process
contract; it must not imply successful Object Graph mutation, datasource commit, serialization, sync, replication,
or transaction completion unless the owning runtime package reports that success.
Rationale:
Process orchestration is an execution boundary, not the authority for semantic success of graph, persistence, or
distributed runtime operations.
Source scope:
OAProcess; OAChangeProcessor; OAChangeRefresher; OACron; OACronProcessor; cross-package boundaries with graph,
object, hub, datasource, transaction, sync, replication, and remote packages.
Related CODEX findings:
none observed beyond failure visibility and false-success process findings.
Suggested unit tests:
processSuccessDoesNotImplyDatasourceCommitSuccess(), processFailureDoesNotPublishSemanticGraphSuccess(),
scheduledTaskCompletionDoesNotHideInnerRuntimeFailure().
Spec target section:
Process Runtime / Runtime Operation Boundary Semantics.

*/

