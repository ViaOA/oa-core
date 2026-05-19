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
 * Automates how to make method calls remote, so that they are the same as if calling a local method.
 * <p>
 * Supports the following:
 * <ul>
 * <li>Client to Server
 * <li>Server to Client
 * <li>Broadcasting from server or clients to 1+/all others.
 * </ul>
 * Independent of communication layer, transmission layer, and serialization.
 */
package com.viaoa.remote;


/* CODEX Invariants

ID: REMOTE-DISPATCH-001
  Contract statement: Remote dispatch must resolve each request to the intended bind, method signature, target object,
  and argument list.
  Rationale: Wrong dispatch can execute the wrong business operation across sync, replication, or callbacks.
  Source locations: BindInfo.loadMethodInfo, getMethodInfo, OARemoteMultiplexerClient._processMessageForStoC,
  OARemoteMultiplexerServer._processSocketCtoSRequest, _invokeByRemoteThread.
  Related CODEX findings: method signature collision; annotation remote interface correction not applied.
  Suggested unit tests: remoteDispatchSelectsCorrectOverload, remoteDispatchFailsVisibleForMissingBind,
  remoteDispatchFailsVisibleForMissingMethod.
  Spec target section: Remote Runtime / Dispatch Semantics

  ID: REMOTE-PROXY-001
  Contract statement: A proxy bind name must identify one stable remote endpoint within its session and must not be
  resolved by semantic equals().
  Rationale: Remote identity is endpoint identity, not object equality.
  Source locations: client/server getProxyForCtoS, getProxyForStoC, getBindInfo(Object), Session.getBindInfo.
  Related CODEX findings: session bind lookup using equals; stale lookup/broadcast re-registration.
  Suggested unit tests: remoteProxyUsesIdentityNotEquals, reRegisterLookupDoesNotKeepStaleObjectSilently.
  Spec target section: Remote Runtime / Proxy Identity

  ID: REMOTE-CORRELATION-001
  Contract statement: Every request expecting a response must have one unique correlation ID, and pending state must
  be removed only after terminal completion.
  Rationale: Premature or missing cleanup causes timeouts, wrong responses, or stale pending maps.
  Source locations: hmAsyncRequestInfo, hmClientCallbackRequestInfo, queued response branches.
  Related CODEX findings: pending request inserted before send success; pending removed before conversion/queueing
  succeeds; close does not wake pending requests.
  Suggested unit tests: pendingRequestRemovedAfterSuccessfulResponse,
  pendingRequestRetainedOrFailedWhenResponseConversionFails, closeFailsAndWakesPendingRequests.
  Spec target section: Remote Runtime / Request Correlation

  ID: REMOTE-RESPONSE-001
  Contract statement: A request type with return value must always produce a terminal response, terminal exception,
  timeout, or disconnect failure visible to the caller.
  Rationale: Remote calls must not degrade into indefinite waits or false transport delays.
  Source locations: sendResponseForStoC, _writeOnQueueSocketX, _invokeByRemoteThread, onInvokeForCtoS,
  onInvokeForStoC.
  Related CODEX findings: queue socket missing causes no terminal response; bind/method errors for queued return paths
  fail to respond.
  Suggested unit tests: queuedReturnSocketMissingFailsCaller, queuedBindErrorReturnsRemoteError,
  stoCResponseWriteFailureIsVisible.
  Spec target section: Remote Runtime / Response Semantics

  ID: REMOTE-FAIL-001
  Contract statement: Remote failure must not silently appear as success or default return unless the method contract
  explicitly suppresses response handling.
  Rationale: Silent false-success corrupts sync, replication, UI callbacks, and server decisions.
  Source locations: broadcast proxy handler, no-response branches, exceptionMessage handling, afterInvokeForCtoS/StoC.
  Related CODEX findings: broadcast proxy returning default despite exception; dropped no-response stale bind
  failures.
  Suggested unit tests: broadcastProxyThrowsWhenDispatchFails, noResponseStaleBindIsLoggedOrFailedByContract.
  Spec target section: Remote Runtime / Silent Failure Prevention

  ID: REMOTE-TIMEOUT-001
  Contract statement: Timeout must mark the request as incomplete/failure and must not leave stale pending state or
  imply successful execution.
  Rationale: Timeout is a correlation/lifecycle state, not a normal successful return.
  Source locations: waitForMethodInvoked, waitForProcessedByServer, queued wait loops.
  Related CODEX findings: timed-out queued requests remain pending; sync queue continues while timed-out request still
  running.
  Suggested unit tests: timeoutRemovesPendingClientRequest, timeoutDoesNotReportSuccess,
  syncTimeoutDoesNotBreakOrderingSilently.
  Spec target section: Remote Runtime / Timeout Semantics

  ID: REMOTE-ORDER-001
  Contract statement: Queue-backed remote delivery must preserve OA-required ordering and must not replay, skip, or
  reorder messages unless explicitly contracted.
  Rationale: OA sync/replication depends on deterministic event order.
  Source locations: OACircularQueue use, setupBroadcastQueueReader, processQueueMessagesOnServer, _writeQueueMessages,
  sync request queues.
  Related CODEX findings: queue reader replay after exception; sync queue continues after timed-out in-flight request.
  Suggested unit tests: broadcastQueueDoesNotReplayAfterProcessorException,
  syncQueuePreservesRequestOrderUnderSlowHandler.
  Spec target section: Remote Runtime / Ordering Semantics

  ID: REMOTE-THREAD-001
  Contract statement: Remote worker threads must reset baseline request state before work and clear request state
  after completion or failure.
  Rationale: Reused remote threads must not leak prior request context into later work.
  Source locations: createRemoteThread, createRemoteClientThread, createSyncRunnableQueueThread, OARemoteThread.reset.
  Related CODEX findings: sync runnable worker leaves requestInfo; terminal exceptions outside guarded path leave
  request without terminal state.
  Suggested unit tests: remoteThreadClearsRequestInfoAfterSuccess, remoteThreadClearsRequestInfoAfterRunnableFailure,
  remoteThreadFailureNotifiesWaitingCaller.
  Spec target section: Remote Runtime / Worker Lifecycle

  ID: REMOTE-TL-001
  Contract statement: Any remote code that sets OAThreadLocal remote request, serializer, notify object, sync flags,
  or context must restore it with finally.
  Rationale: ThreadLocal leaks cause cross-request sync/serialization/context corruption.
  Source locations: setRemoteRequestInfo, add/removeObjectSerializer, sync runnable processing.
  Related CODEX findings: remote request info cleanup risks around non-finally paths.
  Suggested unit tests: remoteRequestInfoRestoredAfterInvocationException, objectSerializerRemovedAfterWriteFailure.
  Spec target section: Remote Runtime / ThreadLocal Semantics

  ID: REMOTE-SYNC-001
  Contract statement: Remote sync/replay/broadcast handling must preserve sendSyncMessages and replication-source
  semantics and must not echo messages unless intended.
  Rationale: Sync divergence or echo loops break OG distributed consistency.
  Source locations: RequestInfo.replicationSource, broadcast handling, OARemoteThread.setDefaultSendSyncMessages, OA
  sync queue branches.
  Related CODEX findings: client-originated broadcast args mutated before fan-out; sync queue ordering concerns.
  Suggested unit tests: clientBroadcastDoesNotEchoToOriginUnlessContracted,
  replicationSourcePropagatesThroughBroadcast, remoteThreadSendSyncDefaultMatchesRequestType.
  Spec target section: Remote Runtime / Sync Integration

  ID: REMOTE-SERIAL-001
  Contract statement: Remote serialization must preserve argument, return, exception, class-descriptor, compressed-
  value, and remote-reference semantics across all virtual sockets.
  Rationale: Remote execution correctness depends on exact wire value meaning.
  Source locations: RemoteObjectInputStream, RemoteObjectOutputStream, RemoteBufferedOutputStream, argument/return
  processing methods.
  Related CODEX findings: class descriptor ID race across virtual sockets; buffered close/flush buffer loss/leak;
  broadcast argument mutation before fan-out.
  Suggested unit tests: classDescriptorSharedAcrossSocketsDoesNotDeserializeMissingId,
  remoteBufferedCloseFlushesBytes, remoteBufferedFlushReleasesBufferOnIOException,
  broadcastFanoutUsesWireSafeArguments.
  Spec target section: Remote Runtime / Serialization Semantics

  ID: REMOTE-CONNECTION-001
  Contract statement: Disconnect/close must make all session-owned sockets, queue sockets, pending requests, and
  worker waiters terminal.
  Rationale: Stale sockets/pending requests cause leaks, stalls, and hidden remote failures.
  Source locations: close, removeSession, Session.onDisconnect, socket pools, async queue sockets.
  Related CODEX findings: client close does not wake pending requests; session disconnect does not close pooled
  sockets; StoC reader terminal exit does not close/recover socket.
  Suggested unit tests: clientCloseFailsPendingRequests, sessionDisconnectClosesPooledStoCSockets,
  stocReaderTerminalFailureForcesReconnectOrFailure.
  Spec target section: Remote Runtime / Connection Lifecycle

  ID: REMOTE-RETRY-001
  Contract statement: Retry/reconnect must not reuse corrupted stream state, stale pending state, or stale proxy/
  session routing.
  Rationale: Retrying after a visible remote failure must not create duplicate execution or wrong endpoint routing.
  Source locations: pending maps, proxy caches, session bind maps, stream reuse paths.
  Related CODEX findings: stale bind registrations; reused stream after exception; pending inserted before send
  success.
  Suggested unit tests: sendFailureDoesNotLeavePendingRequest, streamFailureDiscardsSocketBeforeReuse,
  lookupReplacementDoesNotRouteToOldObject.
  Spec target section: Remote Runtime / Retry Semantics

  ID: REMOTE-CONCURRENT-001
  Contract statement: Concurrent remote calls must not corrupt pending maps, queue positions, bind/proxy state,
  sequence counters, or response delivery.
  Rationale: OA remote is shared by sync, replication, callbacks, and application threads.
  Source locations: concurrent maps, AtomicInteger message/thread counters, queue readers/writers, bind maps.
  Related CODEX findings: thread index overflow; async queue sender marker prevents restart; queued callback state
  inserted before enqueue success.
  Suggested unit tests: remoteThreadIndexOverflowUsesNonNegativeIndex, asyncQueueSenderRestartsAfterFailure,
  concurrentQueuedCallbacksDoNotLoseCorrelation.
  Spec target section: Remote Runtime / Concurrency Semantics

  ID: REMOTE-INTERNAL-001
  Contract statement: Remote protocol validation focuses on OA-controlled transport correctness: valid OA-produced
  frames must be fully consumed, processed, failed, or connection-terminated.
  Rationale: This package is not a hostile public protocol, but internal frame corruption must not leave ambiguous
  state.
  Source locations: _processSocket, _processSocketCtoSRequest, stream/frame read methods.
  Related CODEX findings: discarded frame must fully consume bytes or fail connection; input stream close skipped on
  exception.
  Suggested unit tests: validFrameProcessingFailureTerminatesFrameState, c2sInputStreamClosedOnProcessingException.
  Spec target section: Remote Runtime / Internal Transport Contract

  Suggested Package-Level Spec Summary

  - com.viaoa.remote owns OA internal remote method invocation over the OA comm/multiplexer layer.
  - It is responsible for proxy creation, bind identity, method metadata, argument/return conversion, request
    correlation, queue routing, and remote worker lifecycle.
  - It must guarantee that every response-capable remote call reaches a visible terminal state: response, exception,
    timeout, disconnect, or explicit no-response contract.
  - It must never silently report success when dispatch, serialization, queueing, socket write/read, or response
    correlation failed.
  - It must preserve remote ordering where OA sync, replication, broadcast, and queued callback behavior depend on
    order.
  - It assumes com.viaoa.comm provides ordered internal virtual sockets, but remote must discard/fail corrupted stream
    state rather than reuse it.
  - It assumes serialization preserves OA object identity/reference semantics across arguments, return values, and
    exceptions.
  - It must keep OAThreadLocal remote context, serializer context, and sync behavior balanced across reused remote
    threads.
  - Main unit-test categories: dispatch/signature resolution, proxy identity, queued request correlation, timeout/
    disconnect cleanup, remote thread reuse, sync ordering, serialization frame behavior, socket failure cleanup, and
    concurrent callback delivery.

*/
