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

REMOTE-RUNTIME-001 — High-Level Remote Runtime Boundary
Contract statement:
com.viaoa.remote defines OA’s high-level remote invocation contract above communication and multiplexer transports,
including remote lookup, proxy invocation, callback participation, distributed context, and visible terminal
outcomes.
Rationale:
OA remote behavior is a callable Object Graph boundary, not ordinary RPC plumbing; distributed runtime correctness
depends on separating remote semantic success from lower-level transport delivery.
Source scope:
com.viaoa.remote package contract; com.viaoa.remote.multiplexer implementation; com.viaoa.comm transport
integration; sync/replication/runtime consumers.
Related CODEX findings:
Existing package-info notes remote dispatch, proxy identity, correlation, response, failure, ThreadLocal, sync,
serialization, connection, retry, and concurrency risks.
Suggested unit tests:
remoteRuntimeBoundaryDistinguishesTransportAndSemanticSuccess, remoteInvocationHasVisibleTerminalOutcome.
Spec target section:
Remote Runtime / Package Boundary

REMOTE-LIFECYCLE-001 — Remote Endpoint Lifecycle
Contract statement:
Remote client/server endpoints, sessions, lookup bindings, callbacks, queues, sockets, and remote worker state must
follow deterministic lifecycle stages: registered/created, active, closing, closed, failed, or reconnected as a new
lifecycle.
Rationale:
Remote runtime state crosses process boundaries; stale endpoint or session state can route calls to wrong objects,
leak callbacks, or hide disconnects.
Source scope:
com.viaoa.remote package contract; OARemoteMultiplexerClient close/lookup/bind paths; OARemoteMultiplexerServer
start/session/bind paths.
Related CODEX findings:
Client close does not wake pending requests; session disconnect does not close pooled sockets; StoC reader terminal
exit does not close/recover socket.
Suggested unit tests:
clientCloseFailsPendingRequests, sessionDisconnectClosesPooledStoCSockets, remoteReconnectCreatesNewLifecycle.
Spec target section:
Remote Runtime / Lifecycle Semantics

REMOTE-REG-001 — Remote Service Registration And Lookup
Contract statement:
Remote service/object registration and lookup must bind a lookup name to the intended object, interface, queue
settings, and session authority, and duplicate registration must be visibly rejected or update the existing binding
consistently.
Rationale:
Lookup names are distributed runtime authority boundaries; stale or ambiguous bindings can execute business methods
against the wrong object.
Source scope:
com.viaoa.remote package contract; server createLookup/removeLookup/getBindInfo; client lookup/lookupBroadcast/
registerBroadcast.
Related CODEX findings:
Stale lookup/broadcast re-registration; duplicate createLookup/createBroadcast behavior; re-register lookup can keep
stale object silently.
Suggested unit tests:
reRegisterLookupDoesNotKeepStaleObjectSilently, duplicateLookupRegistrationRejectedOrUpdatedByContract,
removedLookupFailsVisibleForFutureCalls.
Spec target section:
Remote Runtime / Registration And Lookup

REMOTE-PROXY-001 — Proxy Identity Semantics
Contract statement:
A remote proxy/bind name must identify one stable remote endpoint within its session and must not be resolved by
semantic equals() unless explicitly contracted.
Rationale:
Remote identity is endpoint identity, not object equality; equals-based routing can call the wrong remote object.
Source scope:
com.viaoa.remote package contract; client/server proxy creation; getBindInfo(Object); session bind lookup.
Related CODEX findings:
Session bind lookup using equals; stale proxy/bind registrations.
Suggested unit tests:
remoteProxyUsesIdentityNotEquals, equalButDistinctRemoteObjectsDoNotShareBind,
lookupReplacementDoesNotRouteToOldObject.
Spec target section:
Remote Runtime / Proxy Identity

REMOTE-DISPATCH-001 — Method Dispatch Resolution
Contract statement:
Remote dispatch must resolve each request to the intended bind, method signature, target object, argument list, and
invocation mode, or fail visibly when any component cannot be resolved.
Rationale:
Wrong dispatch can execute the wrong business operation across remote services, sync, replication, callbacks, or
graph runtime operations.
Source scope:
com.viaoa.remote package contract; BindInfo method metadata; remote multiplexer client/server request processing;
remote annotation metadata.
Related CODEX findings:
Method signature collision; annotation remote interface correction not applied; missing bind/method queued return
paths.
Suggested unit tests:
remoteDispatchSelectsCorrectOverload, remoteDispatchFailsVisibleForMissingBind,
remoteDispatchFailsVisibleForMissingMethod.
Spec target section:
Remote Runtime / Dispatch Semantics

REMOTE-INVOKE-001 — Invocation Mode Semantics
Contract statement:
Remote invocation modes must be explicit: synchronous calls require a correlated response or terminal failure;
asynchronous/queued calls require visible enqueue or queue failure; no-response calls must not imply remote semantic
success.
Rationale:
OA callers must know whether a remote method completed, was queued, failed, timed out, or was intentionally fire-
and-forget.
Source scope:
com.viaoa.remote package contract; client/server onInvoke paths; queued request/response branches; broadcast/no-
response branches.
Related CODEX findings:
Queue socket missing causes no terminal response; no-response stale bind failures can be dropped; broadcast proxy
can return default despite exception.
Suggested unit tests:
queuedReturnSocketMissingFailsCaller, noResponseStaleBindIsLoggedOrFailedByContract,
transportWriteSuccessDoesNotMeanRemoteMethodSucceeded.
Spec target section:
Remote Runtime / Invocation Modes

REMOTE-CORRELATION-001 — Request Response Correlation
Contract statement:
Every response-capable remote request must have one unique correlation identity, and pending state must remain until
a terminal response, terminal exception, timeout, disconnect, or cancellation has been recorded and waiters
notified.
Rationale:
Premature or missing cleanup causes wrong responses, stale pending maps, leaked waiters, or indefinite waits.
Source scope:
com.viaoa.remote package contract; pending request maps; queued response branches; callback request maps.
Related CODEX findings:
Pending request inserted before send success; pending removed before conversion/queueing succeeds; close does not
wake pending requests.
Suggested unit tests:
pendingRequestRemovedAfterSuccessfulResponse, pendingRequestRetainedOrFailedWhenResponseConversionFails,
closeFailsAndWakesPendingRequests.
Spec target section:
Remote Runtime / Request Correlation

REMOTE-RESPONSE-001 — Terminal Response Visibility
Contract statement:
A request type with a return value must always reach one caller-visible terminal state: value, remote exception,
local exception, timeout, disconnect failure, cancellation, or explicit no-response contract.
Rationale:
Remote calls must not degrade into indefinite waits, false transport success, or default return values.
Source scope:
com.viaoa.remote package contract; sendResponseForStoC; queue socket write paths; server invocation/return paths;
client response handling.
Related CODEX findings:
Queue socket missing causes no terminal response; bind/method errors for queued return paths fail to respond; StoC
response write failure visibility.
Suggested unit tests:
queuedReturnSocketMissingFailsCaller, queuedBindErrorReturnsRemoteError, stoCResponseWriteFailureIsVisible.
Spec target section:
Remote Runtime / Response Semantics

REMOTE-FAIL-001 — No Silent Remote False-Success
Contract statement:
Remote failure must not silently appear as success, null/default return, accepted callback, delivered broadcast,
completed lookup, or clean disconnect unless the method contract explicitly suppresses response handling and records
diagnostics as required.
Rationale:
Silent false-success corrupts sync, replication, UI callbacks, object graph updates, and server/client runtime
decisions.
Source scope:
com.viaoa.remote package contract; proxy invocation handlers; exceptionMessage handling; afterInvoke callbacks; no-
response and broadcast paths.
Related CODEX findings:
Broadcast proxy returning default despite exception; dropped no-response stale bind failures; silent
exceptionMessage paths.
Suggested unit tests:
broadcastProxyThrowsWhenDispatchFails, noResponseStaleBindIsLoggedOrFailedByContract,
failedLookupDoesNotReturnUsableProxy.
Spec target section:
Remote Runtime / False-Success Prevention

REMOTE-TIMEOUT-001 — Timeout Is Failure/Incomplete State
Contract statement:
Remote timeout must mark the affected request as incomplete or failed, must notify waiters, and must not leave stale
pending state or imply successful remote execution.
Rationale:
Timeout is a correlation/lifecycle outcome, not a successful remote return.
Source scope:
com.viaoa.remote package contract; waitForMethodInvoked; waitForProcessedByServer; queued wait loops; pending maps.
Related CODEX findings:
Timed-out queued requests remain pending; sync queue continues while timed-out request is still running.
Suggested unit tests:
timeoutRemovesPendingClientRequest, timeoutDoesNotReportSuccess, syncTimeoutDoesNotBreakOrderingSilently.
Spec target section:
Remote Runtime / Timeout Semantics

REMOTE-INTERRUPT-001 — Interruption Is Preserved And Visible
Contract statement:
Interrupted remote waits, queue puts, worker waits, and callback waits must restore interrupt status and fail or
cancel the affected remote operation visibly.
Rationale:
Ignoring interruption makes shutdown, cancellation, and thread-pool management unreliable.
Source scope:
com.viaoa.remote package contract; remote multiplexer wait/queue/worker paths.
Related CODEX findings:
InterruptedException handling risks in queue puts, remote thread waits, and server wait loops.
Suggested unit tests:
interruptedRemoteWaitRestoresInterrupt, interruptedQueuePutFailsRequest,
interruptedRemoteThreadWaitDoesNotReportSuccess.
Spec target section:
Remote Runtime / Interruption Semantics

REMOTE-ORDER-001 — Ordered Remote Delivery
Contract statement:
Queue-backed remote delivery, sync requests, replication-related calls, and ordered callback streams must preserve
the OA-required ordering contract and must not replay, skip, or reorder messages unless explicitly contracted.
Rationale:
OA sync and replication depend on deterministic event and method ordering across runtimes.
Source scope:
com.viaoa.remote package contract; OACircularQueue use; broadcast queue readers; sync request queues; remote
multiplexer queue senders/readers.
Related CODEX findings:
Queue reader replay after exception; sync queue continues after timed-out in-flight request.
Suggested unit tests:
broadcastQueueDoesNotReplayAfterProcessorException, syncQueuePreservesRequestOrderUnderSlowHandler,
reconnectStartsNewOrderingEpoch.
Spec target section:
Remote Runtime / Ordering Semantics

REMOTE-CALLBACK-001 — Callback Lifecycle And Routing
Contract statement:
Remote callbacks must be registered, routed, invoked, failed, and deregistered according to session and bind
identity, and stale callbacks must not receive future invocations.
Rationale:
Callbacks are distributed callable graph entry points and can mutate object, Hub, sync, UI, or business state.
Source scope:
com.viaoa.remote package contract; server-to-client and client-to-server proxy/bind/callback paths; broadcast
callback paths.
Related CODEX findings:
Stale bind failures in StoC queued no-response; callback request map cleanup risks; stale broadcast callback
registration.
Suggested unit tests:
callbackRoutesToCorrectSession, removedCallbackDoesNotReceiveInvocation, staleCallbackBindFailureIsObservable.
Spec target section:
Remote Runtime / Callback Semantics

REMOTE-BROADCAST-001 — Broadcast Fan-Out Semantics
Contract statement:
Broadcast remote calls must route to intended recipients according to the broadcast/queue contract, preserve
original wire-safe arguments for each recipient, and surface per-invocation failure according to the invocation
mode.
Rationale:
Broadcasts can affect many clients and object graph projections; argument mutation or hidden recipient failure
creates distributed divergence.
Source scope:
com.viaoa.remote package contract; broadcast proxy handling; queued broadcast paths; client-originated broadcast
fan-out.
Related CODEX findings:
Client-originated broadcast args mutated before fan-out; broadcast proxy returning response despite exception.
Suggested unit tests:
clientBroadcastDoesNotEchoToOriginUnlessContracted, broadcastFanoutUsesWireSafeArguments,
broadcastProxyThrowsWhenDispatchFails.
Spec target section:
Remote Runtime / Broadcast Semantics

REMOTE-THREAD-001 — Remote Worker Context Lifecycle
Contract statement:
Remote worker threads must reset baseline request state before work, set request context only for the active
invocation, and clear it after completion, cancellation, or failure.
Rationale:
Reused remote threads must not leak prior request context into later calls, sync behavior, serialization, or
diagnostics.
Source scope:
com.viaoa.remote package contract; remote thread creation; sync runnable queue threads; OARemoteThread.reset and
request info handling.
Related CODEX findings:
Sync runnable worker leaves requestInfo; terminal exceptions outside guarded paths can leave request without
terminal state.
Suggested unit tests:
remoteThreadClearsRequestInfoAfterSuccess, remoteThreadClearsRequestInfoAfterRunnableFailure,
remoteThreadFailureNotifiesWaitingCaller.
Spec target section:
Remote Runtime / Worker Lifecycle

REMOTE-TL-001 — ThreadLocal Runtime Context Restoration
Contract statement:
Remote code that sets OA ThreadLocal remote request, serializer, notify object, sync flags, sendSyncMessages, graph/
runtime context, security context, or transaction context must restore previous state with finally-style cleanup.
Rationale:
ThreadLocal leaks cause cross-request sync, serialization, transaction, security, and graph-context corruption.
Source scope:
com.viaoa.remote package contract; remote request info; object serializer context; sync runnable processing;
sendSyncMessages integration.
Related CODEX findings:
Remote request info cleanup risks around non-finally paths; sendSyncMessages context concerns.
Suggested unit tests:
remoteRequestInfoRestoredAfterInvocationException, objectSerializerRemovedAfterWriteFailure,
sendSyncMessagesRestoredAfterRemoteCall.
Spec target section:
Remote Runtime / ThreadLocal Semantics

REMOTE-SYNC-001 — Sync Replication Context Semantics
Contract statement:
Remote sync, replay, replication, broadcast, and callback handling must preserve origin/source context,
sendSyncMessages behavior, and replay/replication boundaries so messages do not echo, reorder, or apply under the
wrong source.
Rationale:
Distributed Object Graph consistency depends on remote preserving sync and replication context semantics.
Source scope:
com.viaoa.remote package contract; RequestInfo replication/source fields; OARemoteThread sendSync defaults; sync
queue and broadcast handling.
Related CODEX findings:
Client-originated broadcast args mutated before fan-out; sync queue ordering concerns; replicationSource propagation
noted.
Suggested unit tests:
clientBroadcastDoesNotEchoToOriginUnlessContracted, replicationSourcePropagatesThroughBroadcast,
remoteThreadSendSyncDefaultMatchesRequestType.
Spec target section:
Remote Runtime / Sync And Replication Integration

REMOTE-SERIAL-001 — Remote Serialization Semantics
Contract statement:
Remote serialization must preserve argument, return, exception, class-descriptor, compressed-value, remote-
reference, object identity, and proxy binding semantics across all remote transports.
Rationale:
Remote execution correctness depends on exact wire value meaning and OA object/reference identity preservation.
Source scope:
com.viaoa.remote package contract; remote.multiplexer.io streams; argument/return processing; OAObjectSerializer
integration.
Related CODEX findings:
Class descriptor ID race across virtual sockets; buffered close/flush buffer loss/leak; broadcast argument mutation
before fan-out.
Suggested unit tests:
classDescriptorSharedAcrossSocketsDoesNotDeserializeMissingId, remoteBufferedCloseFlushesBytes,
remoteBufferedFlushReleasesBufferOnIOException, broadcastFanoutUsesWireSafeArguments.
Spec target section:
Remote Runtime / Serialization Semantics

REMOTE-CONNECTION-001 — Disconnect And Shutdown Terminality
Contract statement:
Disconnect, close, shutdown, or session removal must make all session-owned sockets, queue sockets, pending
requests, callbacks, worker waiters, and async queues terminal or explicitly transferred to a new lifecycle.
Rationale:
Stale sockets and pending requests cause leaks, stalls, hidden remote failures, and wrong reconnect behavior.
Source scope:
com.viaoa.remote package contract; client close; server removeSession; session disconnect; socket pools; async queue
sockets.
Related CODEX findings:
Client close does not wake pending requests; session disconnect does not close pooled sockets; StoC reader terminal
exit does not close/recover socket.
Suggested unit tests:
clientCloseFailsPendingRequests, sessionDisconnectClosesPooledStoCSockets,
stocReaderTerminalFailureForcesReconnectOrFailure.
Spec target section:
Remote Runtime / Connection Lifecycle

REMOTE-RETRY-001 — Retry And Reconnect State Isolation
Contract statement:
Retry or reconnect after visible remote failure must not reuse corrupted stream state, dirty pooled sockets, stale
pending request state, stale proxy bindings, stale session maps, or prior ordering epoch state.
Rationale:
Retry after remote failure must not create duplicate execution, wrong endpoint routing, or hidden reorder.
Source scope:
com.viaoa.remote package contract; pending maps; proxy caches; session bind maps; stream/socket reuse paths.
Related CODEX findings:
Stale bind registrations; reused stream after exception; pending inserted before send success.
Suggested unit tests:
sendFailureDoesNotLeavePendingRequest, streamFailureDiscardsSocketBeforeReuse,
lookupReplacementDoesNotRouteToOldObject.
Spec target section:
Remote Runtime / Retry Semantics

REMOTE-CONCURRENT-001 — Concurrent Remote Operation Safety
Contract statement:
Concurrent remote calls, callbacks, lookups, broadcasts, queue processing, disconnects, DGC, and response handling
must not corrupt pending maps, queue positions, bind/proxy state, sequence counters, socket pools, or response
delivery.
Rationale:
OA remote is shared by sync, replication, callbacks, UI, and application threads.
Source scope:
com.viaoa.remote package contract; concurrent maps; atomic counters; queue readers/writers; bind maps; remote thread
pools.
Related CODEX findings:
Thread index overflow; async queue sender marker prevents restart; queued callback state inserted before enqueue
success.
Suggested unit tests:
remoteThreadIndexOverflowUsesNonNegativeIndex, asyncQueueSenderRestartsAfterFailure,
concurrentQueuedCallbacksDoNotLoseCorrelation.
Spec target section:
Remote Runtime / Concurrency

REMOTE-DGC-001 — Distributed Remote Object Lifecycle
Contract statement:
Distributed garbage collection must release remote bindings only after no live session, proxy, callback, queued
request, or runtime reference still owns them, and must not drop active callable objects.
Rationale:
Remote object lifecycle must balance leak prevention with callable graph correctness.
Source scope:
com.viaoa.remote package contract; remote multiplexer DGC; bind maps; weak references; session ownership.
Related CODEX findings:
Stale bind/session cleanup risks; package-info DGC responsibility.
Suggested unit tests:
dgcRemovesUnreferencedRemoteBind, dgcDoesNotRemoveActiveProxyBind, dgcScopedBySession.
Spec target section:
Remote Runtime / Distributed Garbage Collection

REMOTE-SECURITY-001 — Remote Authority Boundary
Contract statement:
Remote invocation may transport and execute callable graph requests, but object graph authority, security
authorization, transaction commit, sync/replication apply, and datasource durability remain owned by their
respective runtime packages unless explicitly delegated.
Rationale:
A remote call’s transport or dispatch success is not equivalent to semantic authorization, persistence, replication,
or graph commit success.
Source scope:
com.viaoa.remote package contract; integration with secure, transaction, sync, replication, datasource, object, hub,
graph packages.
Related CODEX findings:
Existing package-info distinguishes remote dispatch/correlation/failure from higher-level sync/replication
semantics.
Suggested unit tests:
remoteTransportSuccessDoesNotCommitTransaction, remoteInvocationRespectsSecurityBoundaryByContract,
remoteSuccessDoesNotMeanReplicationApplied.
Spec target section:
Remote Runtime / Runtime Authority Boundary

REMOTE-INTERNAL-001 — Internal Protocol Processing
Contract statement:
Remote protocol processing assumes OA-controlled frames, but any internal frame/request/state violation encountered
during normal OA operation must be fully consumed, processed, failed, or connection-terminated without leaving
ambiguous stream state.
Rationale:
This package is not a hostile public protocol, but internal corruption must not silently poison future remote calls.
Source scope:
com.viaoa.remote package contract; remote socket processing; request frame handling; stream read/close paths.
Related CODEX findings:
Discarded frame must fully consume bytes or fail connection; input stream close skipped on exception.
Suggested unit tests:
validFrameProcessingFailureTerminatesFrameState, c2sInputStreamClosedOnProcessingException,
unexpectedRemoteFrameStateClosesConnection.
Spec target section:
Remote Runtime / Internal Transport Contract

REMOTE-INTEGRATION-001 — Cross-Package Runtime Compatibility
Contract statement:
Remote behavior must remain compatible with remote.multiplexer, comm, comm.multiplexer, serialization, sync,
replication, transaction, object, Hub, graph, runtime, security, and queue contracts.
Rationale:
com.viaoa.remote is the high-level callable boundary for distributed OA/OG runtime behavior.
Source scope:
com.viaoa.remote package contract and all remote implementations/integrations.
Related CODEX findings:
Existing package-info maps dispatch, ordering, sync, serialization, connection, retry, ThreadLocal, and concurrency
findings to runtime invariants.
Suggested unit tests:
remotePayloadRoundTripsThroughCommTransport, syncMessagesPreserveRemoteOrdering,
remoteFailureDoesNotAppearAsGraphSuccess, remoteContextRestoredAcrossCallback.
Spec target section:
Remote Runtime / Cross-Package Integration

*/
