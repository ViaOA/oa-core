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

/* CODEX Invariants

Queue Runtime Contracts

  ID: QUEUE-ENQUEUE-001
  Contract statement: A successful enqueue must make the queued item visible to all eligible consumers according to
  the queue’s delivery model.
  Rationale: OA uses queueing for async remote delivery, replication capture, and ordered background work. Returning
  success before the item is reachable can silently drop runtime work.
  Source locations: OACircularQueue.addMessageToQueue(...), OACircularQueue._addMessage(...)
  Related CODEX findings: live resize can discard queued messages; interrupt handling can return success after
  cancellation intent.
  Suggested unit tests: testSuccessfulEnqueueIsVisibleToRegisteredSession,
  testSuccessfulEnqueueIsVisibleToRawPositionConsumer
  Spec target section: Queue Runtime / Enqueue Visibility

  ID: QUEUE-ENQUEUE-002
  Contract statement: Queue capacity, backing storage, and logical head/session positions must remain mutually
  consistent after any queue configuration change.
  Rationale: A queue is both an array and a logical position stream. If those diverge, consumers can read null/wrong
  work or skip work.
  Source locations: OACircularQueue.setSize(int), OACircularQueue.queueHeadPosition, Session.queuePos
  Related CODEX findings: live setSize can replace backing array without resetting/copying logical state.
  Suggested unit tests: testResizeBeforeUseAllowed, testResizeAfterEnqueueFailsOrPreservesMessages
  Spec target section: Queue Runtime / Configuration Consistency

  ID: QUEUE-DEQUEUE-001
  Contract statement: A dequeue/read operation must only advance consumer/session progress for messages actually made
  available to that consumer.
  Rationale: Session position is the queue’s delivery acknowledgment. Advancing it incorrectly causes silent loss.
  Source locations: OACircularQueue.getMessages(int,long,int,long), OACircularQueue._getMessages(...)
  Related CODEX findings: future posTail normalization can advance session.queuePos from caller-supplied position
  instead of actual read position.
  Suggested unit tests: testSessionPositionAdvancesOnlyByDeliveredMessages,
  testFutureTailDoesNotAdvanceSessionPastHead
  Spec target section: Queue Runtime / Dequeue Progress

  ID: QUEUE-DEQUEUE-002
  Contract statement: Registered-session reads and raw positional reads must have distinct, explicit semantics; a
  registered-session call must not silently degrade into untracked raw reading.
  Rationale: OA remote/sync consumers rely on session state for overrun detection, cleanup, throttling, and delivery
  guarantees.
  Source locations: OACircularQueue.registerSession(int), OACircularQueue.getMessages(int,long,int,long),
  OACircularQueue.unregisterSession(int)
  Related CODEX findings: missing session id can be treated as untracked read.
  Suggested unit tests: testMissingRegisteredSessionFailsVisibly,
  testUnregisteredSessionCannotSilentlyConsumeTrackedQueue
  Spec target section: Queue Runtime / Session Delivery

  Ordering / Delivery Contracts

  ID: QUEUE-ORDER-001
  Contract statement: Queue head positions must form a monotonically increasing logical stream, independent of
  physical array wraparound.
  Rationale: OA sync/replication and async remote delivery depend on deterministic sequence ordering across consumers.
  Source locations: OACircularQueue.queueHeadPosition, OACircularQueue.addMessageToQueue(...),
  OACircularQueue.getHeadPostion()
  Related CODEX findings: return value is physical index, not logical queue position; long overflow recovery collapses
  logical positions.
  Suggested unit tests: testLogicalHeadIncreasesAcrossWraparound, testPhysicalIndexIsNotUsedAsLogicalPosition
  Spec target section: Queue Runtime / Ordering Semantics

  ID: QUEUE-ORDER-002
  Contract statement: Consumers must receive messages in enqueue order for their session, unless an overrun or
  explicit reset is reported.
  Rationale: Remote calls, sync messages, and replication events are order-sensitive. Out-of-order delivery can
  corrupt object graph state.
  Source locations: OACircularQueue._addMessage(...), OACircularQueue._getMessages(...), Session.queuePos
  Related CODEX findings: consumer copy can race producer overwrite outside one critical section.
  Suggested unit tests: testSessionReceivesMessagesInEnqueueOrder,
  testConcurrentProducerCannotOverwriteBetweenCheckAndCopy
  Spec target section: Queue Runtime / Ordered Delivery

  ID: QUEUE-DELIVERY-001
  Contract statement: Queued work must not be silently lost; if a consumer falls behind beyond retention capacity,
  overrun must be visible.
  Rationale: Silent loss of sync/replication messages can create divergent runtimes.
  Source locations: OACircularQueue._addMessage(...), OACircularQueue._getMessages(...),
  OACircularQueue.getAmountAvailable(long)
  Related CODEX findings: inconsistent exact-full overrun checks; should-wait hook overridden by retry cap; new
  sessions can be marked inactive immediately.
  Suggested unit tests: testSlowSessionGetsOverrunSignalWhenMessagesLost, testExactlyFullQueueRemainsReadable,
  testNewSessionIsProtectedUntilFirstRead
  Spec target section: Queue Runtime / Lost-Work Prevention

  ID: QUEUE-DELIVERY-002
  Contract statement: Duplicate delivery must only occur under an explicitly contracted retry/at-least-once mode, not
  because queue progress or acknowledgment state is stale.
  Rationale: Duplicate event or remote-call delivery can repeat side effects.
  Source locations: OACircularQueue.getMessages(...), Session.queuePos, remote/replication queue consumers
  Related CODEX findings: none observed beyond session-position concerns.
  Suggested unit tests: testSessionDoesNotReceiveSameMessageTwiceAfterPositionAdvance,
  testRetryPolicyDocumentsDuplicateDeliveryBehavior
  Spec target section: Queue Runtime / Duplicate Prevention

  Worker / Processing Contracts

  ID: QUEUE-WORKER-001
  Contract statement: Queue worker loops must keep processing future work after recoverable failures, or fail visibly
  when the queue can no longer make progress.
  Rationale: A stalled async queue can silently stop remote delivery, sync propagation, replication capture, or
  background processing.
  Source locations: OACircularQueue as queue primitive; consumers in OAReplicationBase, OARemoteMultiplexerServer
  Related CODEX findings: none observed in queue package itself.
  Suggested unit tests: testWorkerExceptionDoesNotCorruptQueueState, testFatalWorkerFailureIsObservable
  Spec target section: Queue Runtime / Worker Lifecycle

  ID: QUEUE-WORKER-002
  Contract statement: Processing completion must not be acknowledged before the queued work has actually completed or
  failed visibly.
  Rationale: OA cannot treat a message as delivered if execution failed silently after dequeue.
  Source locations: OACircularQueue.getMessages(...), consumer-managed session position updates
  Related CODEX findings: none observed in queue primitive; depends on consumer code.
  Suggested unit tests: testWorkerDoesNotAdvanceAckBeforeSuccessfulProcessingWhenContractRequiresAck,
  testProcessingFailureIsLoggedOrPropagated
  Spec target section: Queue Runtime / Processing Acknowledgment

  Shutdown / Drain / Retry Contracts

  ID: QUEUE-SHUTDOWN-001
  Contract statement: Shutdown, stop, and cancellation behavior must be explicit: queued work is either drained,
  rejected, or discarded under a documented policy.
  Rationale: OA production shutdown must not silently lose replication/sync/event work.
  Source locations: OACircularQueue.addMessageToQueue(...); consumer shutdown in remote/replication callers
  Related CODEX findings: interrupted producer sleep is swallowed and enqueue can still succeed.
  Suggested unit tests: testInterruptedProducerRestoresInterruptStatus,
  testShutdownPolicyRejectsOrDrainsQueuedWorkExplicitly
  Spec target section: Queue Runtime / Shutdown Semantics

  ID: QUEUE-DRAIN-001
  Contract statement: Drain behavior must preserve order and must define whether producers are still allowed during
  drain.
  Rationale: Draining mixed with active producers can create ambiguous completion state.
  Source locations: queue consumers; no explicit drain API in OACircularQueue
  Related CODEX findings: none observed.
  Suggested unit tests: testDrainPreservesEnqueueOrder, testDrainRejectsOrIncludesConcurrentEnqueuesByContract
  Spec target section: Queue Runtime / Drain Semantics

  ID: QUEUE-RETRY-001
  Contract statement: Retry after failed read or processing must not corrupt session progress, duplicate completion
  accounting, or skip queued work.
  Rationale: Remote and replication workers must recover from transient errors without breaking message sequence.
  Source locations: OACircularQueue.getMessages(...), Session.queuePos, consumer retry loops
  Related CODEX findings: future-tail normalization can advance session progress incorrectly.
  Suggested unit tests: testRetryAfterTimedOutReadKeepsSessionPosition,
  testRetryAfterConsumerExceptionCanResumeFromLastDeliveredPosition
  Spec target section: Queue Runtime / Retry Semantics

  ID: QUEUE-FAIL-001
  Contract statement: Queue failure states that affect delivery correctness must be caller-visible or logged through
  OA diagnostics; they must not look like successful empty reads or successful enqueues.
  Rationale: False success hides production data loss.
  Source locations: OACircularQueue._getMessages(...), OACircularQueue.getAmountAvailable(...),
  OACircularQueue.addMessageToQueue(...)
  Related CODEX findings: missing session can become untracked read; negative availability can hide invalid position.
  Suggested unit tests: testOverrunThrowsVisibleFailure, testInvalidSessionDoesNotReturnFalseSuccess,
  testInvalidTailDoesNotReturnMisleadingAvailability
  Spec target section: Queue Runtime / Failure Visibility

  Concurrency / Locking Contracts

  ID: QUEUE-LOCK-001
  Contract statement: Queue state transitions that couple overrun checks, message visibility, and consumer copying
  must be protected by a consistent synchronization boundary.
  Rationale: Volatile fields alone do not make multi-field queue state atomic.
  Source locations: OACircularQueue._addMessage(...), OACircularQueue._getMessages(...), LOCKQueue, msgQueue,
  queueHeadPosition
  Related CODEX findings: overrun check, available calculation, and copying are not under one LOCKQueue critical
  section.
  Suggested unit tests: testConcurrentOverwriteCannotPassOverrunCheckThenReadWrongSlot,
  testProducerConsumerVisibilityUnderContention
  Spec target section: Queue Runtime / Locking Semantics

  ID: QUEUE-LOCK-002
  Contract statement: Wait/notify state must represent actual waiting consumers well enough to avoid missed wakeups or
  indefinite stalls under normal OA usage.
  Rationale: Async delivery cannot rely on later unrelated messages to wake stuck consumers.
  Source locations: OACircularQueue.bWaitingToGet, OACircularQueue._getMessages(...), OACircularQueue._addMessage(...)
  Related CODEX findings: bWaitingToGet is a single boolean for multiple waiters.
  Suggested unit tests: testMultipleWaitingConsumersWakeOnEnqueue, testTimedAndUntimedWaitersDoNotMaskEachOther
  Spec target section: Queue Runtime / Wait-Notify Semantics

  ID: QUEUE-LOCK-003
  Contract statement: Queue callbacks/hooks invoked under the queue lock must not block, reenter the queue, or depend
  on work that requires the same lock unless explicitly contracted.
  Rationale: Hook reentrancy can stall all producers and consumers.
  Source locations: OACircularQueue.shouldWaitOnSlowSession(...), OACircularQueue._addMessage(...)
  Related CODEX findings: shouldWaitOnSlowSession() is invoked while holding LOCKQueue.
  Suggested unit tests: testSlowSessionHookCannotDeadlockQueue, testHookContractRejectsReentrantQueueCall
  Spec target section: Queue Runtime / Hook Locking

  ID: QUEUE-INTERRUPT-001
  Contract statement: Queue methods that block via sleep or wait must preserve Java interrupt semantics unless
  explicitly documented as uninterruptible.
  Rationale: Runtime shutdown and cancellation depend on interrupt visibility.
  Source locations: OACircularQueue.addMessageToQueue(...), OACircularQueue._getMessages(...)
  Related CODEX findings: producer throttle sleep catches InterruptedException as generic Exception and continues.
  Suggested unit tests: testProducerInterruptRestoresInterruptStatus, testConsumerWaitInterruptPropagates
  Spec target section: Queue Runtime / Interrupt Semantics

  State / Status Contracts

  ID: QUEUE-STATE-001
  Contract statement: Queue size/count/status APIs must report committed queue state, not impossible transient values.
  Rationale: Flow control and monitoring can make wrong decisions from negative or stale counts.
  Source locations: OACircularQueue.getAmountAvailable(long), OACircularQueue.getHeadPostion(),
  OACircularQueue.getSize()
  Related CODEX findings: negative availability when posTail > queueHeadPosition; exact-full overrun mismatch.
  Suggested unit tests: testAmountAvailableNeverNegative, testAmountAvailableMatchesReadableMessagesAtCapacityBoundary
  Spec target section: Queue Runtime / Status Reporting

  ID: QUEUE-STATE-002
  Contract statement: Session state must accurately represent whether the session is active, inactive, overrun, or
  current.
  Rationale: Producer throttling, cleanup, and delivery safety all depend on session flags being truthful.
  Source locations: Session.bInactive, Session.bOverrun, Session.msLastRead, Session.queuePos, registerSession(...),
  keepAlive(...)
  Related CODEX findings: new session has msLastRead == 0; slow-session retry can proceed without explicit session
  state transition.
  Suggested unit tests: testRegisterSessionInitializesActiveReadTimestamp,
  testSlowSessionTransitionIsExplicitWhenProducerStopsWaiting
  Spec target section: Queue Runtime / Session State

  ID: QUEUE-STATE-003
  Contract statement: Cleanup must only clear messages that every active protected session has advanced past.
  Rationale: Premature cleanup creates null reads or lost delivery; delayed cleanup is acceptable but must not break
  correctness.
  Source locations: OACircularQueue.cleanupQueue(), lastUsedPos, queueLowPosition, Session.queuePos
  Related CODEX findings: session queue position update is outside LOCKQueue; cleanup interacts with session progress.
  Suggested unit tests: testCleanupDoesNotClearUnreadMessage, testCleanupClearsOnlyAfterAllSessionsAdvance
  Spec target section: Queue Runtime / Cleanup Semantics

  Backpressure / Overflow Contracts

  ID: QUEUE-BACKPRESSURE-001
  Contract statement: Bounded queues must define what happens when consumers lag: wait, throttle, mark overrun, mark
  inactive, or fail visibly.
  Rationale: Slow consumer behavior determines whether OA preserves delivery or sacrifices a session.
  Source locations: OACircularQueue._addMessage(...), shouldWaitOnSlowSession(...), throttleAmount, MS_Wait
  Related CODEX findings: should-wait hook can be overridden by retry cap; slow sessions can be marked inactive based
  on elapsed read time.
  Suggested unit tests: testBackpressureWaitsForProtectedSlowSession,
  testBackpressureMarksOverrunOrInactiveByContract, testThrottleDoesNotBreakOrdering
  Spec target section: Queue Runtime / Backpressure Semantics

  ID: QUEUE-BACKPRESSURE-002
  Contract statement: Overrun detection must be consistent across read, availability, and producer-side session
  scanning.
  Rationale: Different APIs must not disagree about whether a session can still read retained messages.
  Source locations: OACircularQueue.getAmountAvailable(...), OACircularQueue._getMessages(...),
  OACircularQueue._addMessage(...)
  Related CODEX findings: getAmountAvailable uses <= while _getMessages uses <.
  Suggested unit tests: testOverrunBoundaryConsistentAcrossGetMessagesAndAvailability,
  testProducerMarksSessionOverrunAtSameBoundaryAsConsumer
  Spec target section: Queue Runtime / Overrun Semantics

  ThreadLocal / Context Contracts

  ID: QUEUE-TL-001
  Contract statement: Queue execution must not leak OAThreadLocal or runtime context state across queued work items or
  worker iterations.
  Rationale: OA event, sync, serialization, datasource, and remote behavior can change based on thread-local context.
  Source locations: queue consumers in remote/replication layers; OACircularQueue does not set OAThreadLocal directly
  Related CODEX findings: none observed in queue package.
  Suggested unit tests: testQueuedWorkRestoresThreadLocalContext, testWorkerLoopDoesNotLeakContextBetweenMessages
  Spec target section: Queue Runtime / ThreadLocal Context

  ID: QUEUE-TL-002
  Contract statement: Queue primitives should remain context-neutral unless explicitly designed to capture or
  propagate OA runtime context.
  Rationale: Low-level queue code should not accidentally bind work delivery to caller thread state.
  Source locations: OACircularQueue
  Related CODEX findings: none observed.
  Suggested unit tests: testQueuePrimitiveDoesNotModifyOAThreadLocalState
  Spec target section: Queue Runtime / Context Neutrality

  Cross-Package Reliability Contracts

  ID: QUEUE-CROSS-001
  Contract statement: OA event, sync, replication, datasource, cache, and object lifecycle code may assume queue
  delivery preserves committed enqueue order until explicit overrun/reset/failure.
  Rationale: These systems use queues as correctness infrastructure, not just optimization.
  Source locations: OACircularQueue; callers in OAReplicationBase, OARemoteMultiplexerServer
  Related CODEX findings: multiple findings around ordering, overrun, and session state illustrate this invariant.
  Suggested unit tests: testReplicationQueuePreservesSyncMessageOrder,
  testRemoteAsyncQueueSignalsOverrunInsteadOfSilentLoss
  Spec target section: Queue Runtime / Cross-Package Contract

  ID: QUEUE-CROSS-002
  Contract statement: Queue APIs that return positions must clearly distinguish logical stream position from physical
  array index.
  Rationale: Callers using physical index as replay/order position will break after wraparound.
  Source locations: OACircularQueue.addMessageToQueue(...), OACircularQueue.getHeadPostion(),
  OACircularQueue.getMessagesAtPos(int)
  Related CODEX findings: addMessageToQueue returns physical array index while JavaDoc says position.
  Suggested unit tests: testAddMessageReturnValueContractAfterWraparound,
  testLogicalPositionApiSeparateFromPhysicalIndexApi
  Spec target section: Queue Runtime / Position Semantics

  Suggested package-level spec summary

  - com.viaoa.queue provides OA’s low-level ordered queue primitive for async messaging, fan-out, and internal runtime
    delivery.
  - A successful enqueue must make work visible to all eligible consumers.
  - Queue ordering is logical-position based and must not be confused with circular-array indexes.
  - Registered sessions are protected consumers; their progress, inactivity, and overrun state must be truthful.
  - Message loss is only acceptable when the queue reports explicit overrun, reset, inactive-session, or documented
    discard behavior.
  - Worker and consumer failures must be visible enough for OA remote/sync/replication code to recover or stop
    intentionally.
  - Blocking queue paths must preserve interrupt semantics unless explicitly documented as uninterruptible.
  - Backpressure and slow-consumer behavior must be deterministic and testable.
  - Queue cleanup must never clear messages still needed by active protected sessions.
  - Queue code should remain context-neutral; any OAThreadLocal context used by workers must be restored by the worker
    layer.
  - Cross-package tests should cover replication order, remote async fan-out, slow-client overrun, reconnect/session
    behavior, and high-contention producer/consumer races.

*/



