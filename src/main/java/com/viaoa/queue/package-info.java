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
 * 
 * </p>
 */
package com.viaoa.queue;

//CODEX unit tests <todo>

/* CODEX Invariants

QUEUE-ENQUEUE-001 — Enqueue Visibility
Contract statement:
A successful enqueue must make the queued item visible to every eligible consumer according to the queue’s delivery
model and retention policy.
Rationale:
OA queues carry async runtime work, remote messages, sync/replication events, callbacks, and background tasks;
returning success before work is reachable can silently drop runtime state changes.
Source scope:
OACircularQueue.addMessageToQueue(...), OACircularQueue.addMessage(...), OACircularQueue.getMessage(...),
OACircularQueue.getMessages(...).
Related CODEX findings:
live resize can discard queued messages; interrupted producer throttle can still report enqueue success.
Suggested unit tests:
successfulEnqueueIsVisibleToRegisteredSession(), successfulEnqueueIsVisibleToRawPositionConsumer(),
interruptedEnqueueDoesNotReportFalseSuccess().
Spec target section:
Queue Runtime / Enqueue Visibility Semantics.

QUEUE-ORDER-001 — Logical Stream Ordering
Contract statement:
Queue head and consumer positions must form a monotonically increasing logical message stream independent of
circular-array wraparound.
Rationale:
Remote calls, sync messages, replication changes, and event delivery are order-sensitive; physical storage
wraparound must not alter logical delivery order.
Source scope:
OACircularQueue.getHeadPostion(), OACircularQueue.addMessageToQueue(...), OACircularQueue.getMessages(...),
queueHeadPosition, queueLowPosition.
Related CODEX findings:
addMessageToQueue return value can be confused with physical array index; logical position overflow recovery can
collapse ordering.
Suggested unit tests:
logicalHeadIncreasesAcrossWraparound(), physicalIndexIsNotUsedAsLogicalPosition(),
logicalPositionOrderingSurvivesWraparound().
Spec target section:
Queue Runtime / Ordering Semantics.

QUEUE-POSITION-001 — Logical Position Versus Physical Slot
Contract statement:
Queue APIs must clearly preserve the distinction between logical stream positions and physical circular-array
indexes.
Rationale:
Callers that use a physical slot as a replay, ordering, or acknowledgment position will break after wraparound and
can skip or duplicate queued work.
Source scope:
OACircularQueue.addMessageToQueue(...), OACircularQueue.getHeadPostion(), OACircularQueue.getMessagesAtPos(int).
Related CODEX findings:
addMessageToQueue returns a physical array index while documentation suggests a message position.
Suggested unit tests:
addMessageReturnValueContractAfterWraparound(), logicalPositionApiIsDistinctFromPhysicalSlotApi().
Spec target section:
Queue Runtime / Position Semantics.

QUEUE-DEQUEUE-001 — Consumer Progress Acknowledgement
Contract statement:
A dequeue/read operation must advance registered consumer progress only for messages actually delivered to that
consumer.
Rationale:
Consumer position is the queue’s delivery acknowledgement; advancing it incorrectly causes silent loss, while
failing to advance can cause duplicate delivery.
Source scope:
OACircularQueue.getMessages(int, long, int, int), OACircularQueue.getMessages(long,...),
OACircularQueue.getMessage(...), Session.queuePos.
Related CODEX findings:
future posTail normalization can advance session.queuePos from caller-supplied position instead of actual read
position.
Suggested unit tests:
sessionPositionAdvancesOnlyByDeliveredMessages(), futureTailDoesNotAdvanceSessionPastHead(),
timedOutReadDoesNotAdvanceSessionPosition().
Spec target section:
Queue Runtime / Dequeue Progress Semantics.

QUEUE-SESSION-001 — Registered Session Delivery
Contract statement:
Registered-session reads must use tracked session semantics, and a missing or unregistered session id must fail
visibly or return an explicit “not registered” result rather than silently degrading to raw positional reads.
Rationale:
OA remote, sync, and replication consumers rely on session state for ordering, overrun detection, throttling,
cleanup, and delivery guarantees.
Source scope:
OACircularQueue.registerSession(int), OACircularQueue.unregisterSession(int), OACircularQueue.getMessages(int, long,
int, int), OACircularQueue.keepAlive(int).
Related CODEX findings:
missing session id can be treated as an untracked read.
Suggested unit tests:
missingRegisteredSessionFailsVisibly(), unregisteredSessionCannotSilentlyConsumeTrackedQueue(),
registerSessionInitializesTrackedPosition().
Spec target section:
Queue Runtime / Session Delivery Semantics.

QUEUE-SESSION-002 — Session State Truthfulness
Contract statement:
Session state must accurately represent whether the session is active, current, inactive, overrun, or unregistered.
Rationale:
Producer throttling, cleanup, overrun handling, and consumer recovery all depend on truthful session flags and
timestamps.
Source scope:
OACircularQueue.registerSession(int), OACircularQueue.unregisterSession(int), OACircularQueue.keepAlive(int),
Session.queuePos, Session.bInactive, Session.bOverrun, Session.msLastRead.
Related CODEX findings:
new sessions can have stale last-read state; slow-session retry can proceed without explicit session state
transition.
Suggested unit tests:
registeredSessionStartsActiveAndCurrent(), keepAliveUpdatesSessionLiveness(),
slowSessionTransitionIsExplicitWhenProducerStopsWaiting().
Spec target section:
Queue Runtime / Session State Semantics.

QUEUE-DELIVERY-001 — Ordered Delivery Per Consumer
Contract statement:
Each consumer must receive retained messages in enqueue order unless an explicit overrun, reset, unregistration, or
failure state is reported.
Rationale:
Out-of-order queue delivery can corrupt Object Graph state, remote invocation order, replication replay, and sync
event sequencing.
Source scope:
OACircularQueue.addMessageToQueue(...), OACircularQueue.getMessages(...), OACircularQueue.getMessage(...),
Session.queuePos.
Related CODEX findings:
consumer copy can race producer overwrite if overrun checks, availability calculations, and slot copying are not
under a consistent boundary.
Suggested unit tests:
sessionReceivesMessagesInEnqueueOrder(), concurrentProducerCannotOverwriteBetweenCheckAndCopy(),
rawPositionReadsPreserveOrder().
Spec target section:
Queue Runtime / Ordered Delivery Semantics.

QUEUE-DELIVERY-002 — Lost Work Visibility
Contract statement:
Queued work must not be silently lost; if a consumer falls behind beyond retained capacity, the queue must report
explicit overrun, reset, inactive-session, or documented discard behavior.
Rationale:
Silent message loss can create divergent distributed runtimes and stale graph state.
Source scope:
OACircularQueue.getMessages(...), OACircularQueue.getAmountAvailable(long), OACircularQueue.addMessageToQueue(...),
Session.bOverrun, Session.bInactive.
Related CODEX findings:
inconsistent exact-full overrun checks; should-wait hook can be overridden by retry cap; new sessions can be marked
inactive immediately.
Suggested unit tests:
slowSessionGetsOverrunSignalWhenMessagesLost(), exactlyFullQueueBoundaryIsConsistent(),
newSessionIsProtectedUntilFirstRead().
Spec target section:
Queue Runtime / Lost-Work Prevention.

QUEUE-DUP-001 — Duplicate Delivery Boundary
Contract statement:
Duplicate delivery must occur only under an explicitly contracted retry or replay mode, not because acknowledgement,
session position, or queue state is stale.
Rationale:
Duplicate remote calls, callbacks, sync events, or replication messages can repeat side effects.
Source scope:
OACircularQueue.getMessages(...), OACircularQueue.getMessage(...), Session.queuePos, consumer retry boundaries.
Related CODEX findings:
session-position concerns illustrate duplicate/loss risk.
Suggested unit tests:
sessionDoesNotReceiveSameMessageTwiceAfterPositionAdvance(), retryPolicyDocumentsDuplicateDeliveryBehavior().
Spec target section:
Queue Runtime / Duplicate Delivery Semantics.

QUEUE-CAPACITY-001 — Capacity and Configuration Consistency
Contract statement:
Queue capacity, backing storage, logical head position, low retained position, and session positions must remain
mutually consistent after construction or configuration changes.
Rationale:
The queue is both physical storage and a logical stream; if those diverge, consumers can read null, wrong, skipped,
or duplicate work.
Source scope:
OACircularQueue constructors, OACircularQueue.setSize(int), OACircularQueue.getSize(), msgQueue, queueHeadPosition,
queueLowPosition, Session.queuePos.
Related CODEX findings:
live setSize can replace backing array without preserving queued messages or logical state.
Suggested unit tests:
resizeBeforeUseIsAllowed(), resizeAfterEnqueueFailsOrPreservesMessages(),
resizeDoesNotInvalidateSessionPositionsSilently().
Spec target section:
Queue Runtime / Capacity and Configuration Semantics.

QUEUE-BACKPRESSURE-001 — Slow Consumer and Backpressure Semantics
Contract statement:
Bounded queues must define deterministic behavior when consumers lag: wait, throttle, mark overrun, mark inactive,
reject enqueue, or fail visibly.
Rationale:
Slow-consumer behavior determines whether OA preserves ordered delivery or intentionally sacrifices a session;
either outcome must be observable.
Source scope:
OACircularQueue.addMessageToQueue(...), OACircularQueue.shouldWaitOnSlowSession(...), throttleAmount, MS_Wait,
Session.bInactive, Session.bOverrun.
Related CODEX findings:
should-wait hook can be overridden by retry cap; slow sessions can be marked inactive based on elapsed read time.
Suggested unit tests:
backpressureWaitsForProtectedSlowSession(), backpressureMarksOverrunOrInactiveByContract(),
throttleDoesNotBreakOrdering().
Spec target section:
Queue Runtime / Backpressure Semantics.

QUEUE-OVERRUN-001 — Consistent Overrun Boundaries
Contract statement:
Overrun detection must be consistent across producer-side session scanning, availability checks, single-message
reads, and batch reads.
Rationale:
Queue APIs must not disagree about whether a retained message is still available; inconsistent boundaries cause
false empty reads, lost work, or duplicate recovery.
Source scope:
OACircularQueue.getAmountAvailable(long), OACircularQueue.getMessage(...), OACircularQueue.getMessages(...),
OACircularQueue.addMessageToQueue(...), queueLowPosition.
Related CODEX findings:
getAmountAvailable and _getMessages use different exact-boundary comparisons.
Suggested unit tests:
overrunBoundaryConsistentAcrossAvailabilityAndReads(), producerMarksSessionOverrunAtSameBoundaryAsConsumer(),
amountAvailableMatchesReadableMessagesAtCapacityBoundary().
Spec target section:
Queue Runtime / Overrun Semantics.

QUEUE-FAIL-001 — Failure and False-Success Prevention
Contract statement:
Queue states that affect delivery correctness must be caller-visible or observable and must not appear as successful
empty reads, successful enqueues, or successful delivery.
Rationale:
False-success queue behavior hides production data loss, stalled runtime processing, and distributed graph
divergence.
Source scope:
OACircularQueue.addMessageToQueue(...), OACircularQueue.getMessages(...), OACircularQueue.getMessage(...),
OACircularQueue.getAmountAvailable(long), OACircularQueue.registerSession(int).
Related CODEX findings:
missing session can become untracked read; negative availability can hide invalid position; interrupted enqueue can
continue as success.
Suggested unit tests:
overrunThrowsVisibleFailure(), invalidSessionDoesNotReturnFalseSuccess(),
invalidTailDoesNotReturnMisleadingAvailability(), interruptedProducerDoesNotSilentlySucceed().
Spec target section:
Queue Runtime / Failure Visibility Semantics.

QUEUE-RETRY-001 — Retry and Requeue Correctness
Contract statement:
Retry after failed read, timeout, interruption, or consumer processing failure must not corrupt session progress,
duplicate completion accounting, or skip queued work.
Rationale:
Remote, sync, replication, callback, and process workers must recover from transient failures without breaking
message sequence.
Source scope:
OACircularQueue.getMessages(...), OACircularQueue.getMessage(...), Session.queuePos, consumer retry boundaries.
Related CODEX findings:
future-tail normalization can advance session progress incorrectly.
Suggested unit tests:
retryAfterTimedOutReadKeepsSessionPosition(), retryAfterConsumerExceptionCanResumeFromLastDeliveredPosition(),
retryDoesNotSkipQueuedMessages().
Spec target section:
Queue Runtime / Retry Semantics.

QUEUE-SHUTDOWN-001 — Shutdown, Drain, and Cancellation Policy
Contract statement:
Shutdown, stop, cancellation, and drain behavior must be explicit: queued work is either drained, rejected, retained
for retry, or discarded under a documented policy.
Rationale:
OA production shutdown must not silently lose remote, sync, replication, event, or background process work.
Source scope:
OACircularQueue.addMessageToQueue(...), OACircularQueue.getMessages(...), queue consumer boundaries in process,
remote, sync, and replication packages.
Related CODEX findings:
producer throttle interruption can be swallowed and enqueue can still succeed.
Suggested unit tests:
shutdownPolicyRejectsOrDrainsQueuedWorkExplicitly(), drainPreservesEnqueueOrder(),
interruptedProducerRestoresInterruptStatus().
Spec target section:
Queue Runtime / Shutdown and Drain Semantics.

QUEUE-LOCK-001 — Atomic Queue State Transitions
Contract statement:
State transitions that couple overrun checks, message storage, message visibility, availability calculation,
consumer copying, and cleanup must be protected by a consistent synchronization boundary.
Rationale:
Volatile fields alone cannot make multi-field queue state atomic under concurrent producers and consumers.
Source scope:
OACircularQueue.addMessageToQueue(...), OACircularQueue.getMessages(...), OACircularQueue.getAmountAvailable(long),
OACircularQueue.cleanupQueue(), LOCKQueue, msgQueue, queueHeadPosition, queueLowPosition.
Related CODEX findings:
overrun check, available calculation, and copying are not consistently protected by one LOCKQueue critical section.
Suggested unit tests:
concurrentOverwriteCannotPassOverrunCheckThenReadWrongSlot(), producerConsumerVisibilityUnderContention(),
cleanupDoesNotRaceUnreadMessageCopy().
Spec target section:
Queue Runtime / Locking and Atomicity Semantics.

QUEUE-WAIT-001 — Wait/Notify Correctness
Contract statement:
Blocking queue reads and producer notifications must represent actual waiting consumers well enough to avoid missed
wakeups, masked waiters, or indefinite stalls under normal OA usage.
Rationale:
Async runtime delivery cannot depend on later unrelated messages to wake consumers that should have been notified.
Source scope:
OACircularQueue.getMessages(...), OACircularQueue.addMessageToQueue(...), bWaitingToGet, LOCKQueue wait/notify
behavior.
Related CODEX findings:
bWaitingToGet is a single boolean for multiple waiters.
Suggested unit tests:
multipleWaitingConsumersWakeOnEnqueue(), timedAndUntimedWaitersDoNotMaskEachOther(),
enqueueNotifiesEligibleWaitingConsumers().
Spec target section:
Queue Runtime / Wait-Notify Semantics.

QUEUE-HOOK-001 — Queue Hook Reentrancy Boundary
Contract statement:
Queue hooks invoked while queue state is locked must not block indefinitely, reenter the same queue, or depend on
work requiring the same lock unless that behavior is explicitly contracted.
Rationale:
Hook reentrancy or blocking while holding the queue lock can stall all producers and consumers.
Source scope:
OACircularQueue.shouldWaitOnSlowSession(...), OACircularQueue.addMessageToQueue(...), LOCKQueue.
Related CODEX findings:
shouldWaitOnSlowSession() is invoked while holding LOCKQueue.
Suggested unit tests:
slowSessionHookCannotDeadlockQueue(), hookContractRejectsReentrantQueueCall(),
slowSessionHookDoesNotBlockQueueProgress().
Spec target section:
Queue Runtime / Hook and Reentrancy Semantics.

QUEUE-INTERRUPT-001 — Interrupt Semantics
Contract statement:
Queue methods that block using sleep or wait must preserve Java interrupt semantics unless explicitly documented as
uninterruptible.
Rationale:
Runtime shutdown, cancellation, and process control depend on interrupt visibility.
Source scope:
OACircularQueue.addMessageToQueue(...), OACircularQueue.getMessages(...), OACircularQueue.getMessage(...).
Related CODEX findings:
producer throttle sleep catches InterruptedException as a generic Exception and continues.
Suggested unit tests:
producerInterruptRestoresInterruptStatus(), consumerWaitInterruptPropagatesOrIsContracted(),
interruptedQueueOperationDoesNotReportFalseSuccess().
Spec target section:
Queue Runtime / Interrupt Semantics.

QUEUE-STATE-001 — Queue Status Accuracy
Contract statement:
Queue status APIs must report committed queue state and must not expose impossible values such as negative
availability for valid caller state.
Rationale:
Flow control, monitoring, and runtime recovery use queue status to decide whether work is pending, lost, current, or
overrun.
Source scope:
OACircularQueue.getAmountAvailable(long), OACircularQueue.getHeadPostion(), OACircularQueue.getSize(),
OACircularQueue.getName()/setName(...).
Related CODEX findings:
negative availability when posTail is ahead of queueHeadPosition; exact-full overrun mismatch.
Suggested unit tests:
amountAvailableNeverNegativeForDefinedPositions(), amountAvailableMatchesReadableMessages(),
queueHeadPositionReportsCommittedLogicalHead().
Spec target section:
Queue Runtime / Status Reporting Semantics.

QUEUE-CLEANUP-001 — Retention Cleanup Safety
Contract statement:
Cleanup must clear only messages that every active protected session has advanced past, or that are explicitly
outside the retention contract.
Rationale:
Premature cleanup creates null reads or lost delivery; delayed cleanup is acceptable if it does not break
correctness.
Source scope:
OACircularQueue.cleanupQueue(), OACircularQueue.addMessageToQueue(...), queueLowPosition, lastUsedPos,
Session.queuePos.
Related CODEX findings:
session queue position update outside queue lock can interact incorrectly with cleanup.
Suggested unit tests:
cleanupDoesNotClearUnreadMessage(), cleanupClearsOnlyAfterAllSessionsAdvance(), cleanupRespectsProtectedSessions().
Spec target section:
Queue Runtime / Cleanup and Retention Semantics.

QUEUE-CONTEXT-001 — Context-Neutral Queue Primitive
Contract statement:
The queue primitive must remain neutral to OA ThreadLocal and runtime context unless an API explicitly captures,
propagates, or restores context.
Rationale:
Low-level queue storage must not accidentally bind message delivery to caller thread state; context-sensitive
execution belongs to the consumer or higher-level runtime package.
Source scope:
OACircularQueue; integration boundaries with process, callback, remote, sync, replication, transaction, object, hub,
and graph packages.
Related CODEX findings:
none observed in queue package.
Suggested unit tests:
queuePrimitiveDoesNotModifyOAThreadLocalState(), queuedWorkConsumerRestoresContextBetweenMessages().
Spec target section:
Queue Runtime / Runtime Context Semantics.

QUEUE-CONCURRENT-001 — Concurrent Producer and Consumer Correctness
Contract statement:
Concurrent producers and consumers must observe deterministic queue correctness: no lost retained messages, no out-
of-order delivery, no duplicate acknowledgement, and no corrupted session state outside explicit overrun/reset/
failure contracts.
Rationale:
OA queues are runtime coordination infrastructure for distributed and asynchronous graph behavior.
Source scope:
OACircularQueue.addMessageToQueue(...), OACircularQueue.getMessages(...), OACircularQueue.registerSession(int),
OACircularQueue.unregisterSession(int), OACircularQueue.keepAlive(int).
Related CODEX findings:
multi-field queue state atomicity concerns; wait/notify concerns; session state visibility concerns.
Suggested unit tests:
concurrentProducersPreserveLogicalOrder(), concurrentConsumersMaintainIndependentSessionPositions(),
producerConsumerContentionDoesNotCorruptQueueState().
Spec target section:
Queue Runtime / Concurrency Semantics.

QUEUE-CROSS-001 — Cross-Package Sequencing Contract
Contract statement:
OA event, callback, process, remote, sync, replication, datasource, cache, object, Hub, and graph code may rely on
queue delivery preserving committed enqueue order until explicit overrun, reset, shutdown, or failure is reported.
Rationale:
These packages use queue behavior as correctness infrastructure, not merely as an optimization.
Source scope:
OACircularQueue; integration boundaries with com.viaoa.process, callback, trigger, remote, sync, replication,
transaction, object, hub, cache, graph, and datasource packages.
Related CODEX findings:
multiple CODEX findings around ordering, overrun, session state, and false success illustrate this contract.
Suggested unit tests:
replicationQueuePreservesSyncMessageOrder(), remoteAsyncQueueSignalsOverrunInsteadOfSilentLoss(),
eventQueuePreservesCommittedEnqueueOrder().
Spec target section:
Queue Runtime / Cross-Package Runtime Sequencing.

QUEUE-BOUNDARY-001 — Queue Delivery Versus Semantic Operation Success
Contract statement:
Successful queue enqueue, visibility, or delivery only establishes queue-level success; it must not imply successful
task execution, callback completion, remote invocation, sync application, replication replay, transaction commit, or
Object Graph mutation.
Rationale:
Queue semantics are a runtime-buffer boundary; semantic success belongs to the consuming runtime package and must
remain separately observable.
Source scope:
OACircularQueue public API; cross-package boundaries with process, callback, trigger, remote, sync, replication,
transaction, object, hub, and graph packages.
Related CODEX findings:
none observed beyond queue false-success and acknowledgement concerns.
Suggested unit tests:
queueDeliveryDoesNotImplyTaskExecutionSuccess(), queueReadFailureDoesNotAdvanceSemanticCompletion(),
queuedSyncMessageRequiresConsumerApplySuccess().
Spec target section:
Queue Runtime / Runtime Boundary Semantics.

*/

