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
 * Provides the server–side implementation of OA’s remoting layer that runs on top of the
 * Multiplexer framework.  
 * <p>
 * This package enables full bidirectional remote method invocation (RMI-style) over a single
 * physical socket connection using virtual sockets. It is used by {@link com.viaoa.remote}
 * to expose server objects to remote clients, and to receive remote objects originating from clients.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Virtual Sockets</b> — Multiple independent request/response channels over one real TCP
 *       connection, enabling lightweight, concurrent remoting without socket explosion.</li>
 *   <li><b>Client-to-Server (CtoS) Remoting</b> — Clients can lookup, invoke, and stream method calls
 *       to server-hosted objects.</li>
 *   <li><b>Server-to-Client (StoC) Remoting</b> — Server can call methods on client-side remote
 *       objects through dynamically generated Java proxies.</li>
 *   <li><b>Asynchronous Queues</b> — Remote calls can be queued and processed using a circular queue,
 *       enabling high-throughput or broadcast workflows.</li>
 *   <li><b>Broadcast Remote Objects</b> — A single remote proxy can be shared by all connected clients
 *       to deliver fan-out messaging.</li>
 *   <li><b>Distributed Garbage Collection (DGC)</b> — Weak references are tracked so server-side
 *       remote objects can safely be released when clients no longer hold them.</li>
 *   <li><b>Compression / Class-Descriptor Caching</b> — Optimizations for payload size and
 *       serialization speed.</li>
 *   <li><b>Automatic Proxy Generation</b> — Java {@link java.lang.reflect.Proxy} is used to create
 *       server-side representations of client remote objects.</li>
 * </ul>
 *
 * <h2>How It Works</h2>
 * <ol>
 *   <li>Server creates a {@code OARemoteMultiplexerServer} bound to an
 *       {@link com.viaoa.comm.multiplexer.OAMultiplexerServer}.</li>
 *   <li>Clients connect through virtual sockets, using the multiplexer protocol.</li>
 *   <li>Remote lookups and method calls are serialized using
 *       {@link com.viaoa.remote.multiplexer.io.RemoteObjectInputStream} and
 *       {@link com.viaoa.remote.multiplexer.io.RemoteObjectOutputStream}.</li>
 *   <li>Server dispatches remote invocations to worker {@link com.viaoa.runtime.thread.OARemoteThread}s.</li>
 *   <li>Return values (including remote objects) are serialized back to the caller or routed using
 *       async queues when enabled.</li>
 * </ol>
 *
 * <h2>Intended Use</h2>
 * This package provides the low-level infrastructure for high-performance remoting, used internally
 * by OAObjectGraph synchronization, remote method calls, distributed messaging, server-side
 * callbacks, and broadcast channels.
 *
 * @author vvia
 */
package com.viaoa.remote.multiplexer;


/* CODEX Invariants

REMOTE-MUX-LIFECYCLE-001 — Remote Endpoint Lifecycle
Contract statement:
A remote multiplexer client or server endpoint must move through deterministic lifecycle states: configured,
starting, active, closing/disconnecting, closed, failed, or reconnected as a new endpoint/session.
Rationale:
Remote invocation, callbacks, async queues, sync, and replication must not run through stale or ambiguously closed
endpoint state.
Source scope:
OARemoteMultiplexerClient constructor, close(), getMultiplexerClient(); OARemoteMultiplexerServer constructor,
start(), getMultiplexerServer(), createSession(...), removeSession(...), getSession(...).
Related CODEX findings:
Client close can leave pending async requests waiting; session/socket cleanup findings in client/server CODEX notes.
Suggested unit tests:
testRemoteClientCloseFailsPendingRequests, testRemoteServerSessionLifecycleIsDeterministic,
testReconnectStartsNewRemoteSession.
Spec target section:
Remote Multiplexer / Endpoint Lifecycle

REMOTE-MUX-SESSION-001 — Session Ownership
Contract statement:
Each remote session must be owned by one multiplexer connection id and must keep its bind registry, callback
sockets, queues, DGC state, and serialization context scoped to that session.
Rationale:
Session state crossing connection boundaries can route callbacks or remote objects to the wrong client.
Source scope:
OARemoteMultiplexerServer.Session; createSession(...); removeSession(...); getSession(...);
Session.getBindInfo(...); Session.getSocketForStoC(); Session.releaseSocketForStoC(...).
Related CODEX findings:
Server CODEX notes session socket pool cleanup, async queue sender exit, and bind lookup identity risks.
Suggested unit tests:
testSessionStateScopedByConnectionId, testRemovedSessionDoesNotReceiveCallbacks,
testSessionSocketPoolNotSharedAcrossConnections.
Spec target section:
Remote Multiplexer / Session Ownership

REMOTE-MUX-LOOKUP-001 — Remote Lookup Registration Semantics
Contract statement:
Remote lookup and broadcast names must bind deterministically to the intended object, interface, queue settings, and
callback semantics; duplicate registration must either be visibly rejected or update the existing binding
consistently.
Rationale:
Lookup names are remote service authority boundaries; stale or inconsistent binds can execute methods on the wrong
object or proxy.
Source scope:
OARemoteMultiplexerServer.createLookup(...), removeLookup(...), getBindInfo(...), createBroadcast(...);
OARemoteMultiplexerClient.lookup(...), lookupBroadcast(...), registerBroadcast(...), getBindInfo(...).
Related CODEX findings:
Duplicate createLookup/createBroadcast and lookupBroadcast registrations can silently retain stale bind/callback
references.
Suggested unit tests:
testDuplicateLookupRegistrationRejectedOrUpdatedByContract, testDuplicateBroadcastRegistrationUpdatesOrFailsVisibly,
testRemovedLookupCannotBeInvoked.
Spec target section:
Remote Multiplexer / Lookup Binding

REMOTE-MUX-BIND-001 — Remote Object Identity And Bind Semantics
Contract statement:
Remote object/proxy binding must use the intended identity semantics for local objects, broadcast objects, server
objects, and client callback objects, and must not route by value equality unless that is explicitly contracted.
Rationale:
Remote invocation identity is a runtime authority boundary; equals-based routing can bind calls to the wrong object.
Source scope:
OARemoteMultiplexerClient.getBindInfo(...), getBindInfoForObject(...), getProxyForCtoS(...),
getProxyForBroadcast(...); OARemoteMultiplexerServer.getBindInfo(Object), Session.getBindInfo(Object).
Related CODEX findings:
Server Session.getBindInfo(Object) uses obj.equals(...) where identity semantics are expected.
Suggested unit tests:
testRemoteBindUsesObjectIdentityByContract, testEqualButDistinctObjectsDoNotShareBind,
testBroadcastBindIdentityStable.
Spec target section:
Remote Multiplexer / Bind Identity

REMOTE-MUX-PROXY-001 — Proxy Invocation Boundary
Contract statement:
Generated remote proxies must convert a Java method invocation into exactly one remote request according to
synchronous, queued, broadcast, or no-response semantics, and must not report success until that invocation mode’s
completion contract is satisfied.
Rationale:
Remote proxies are callable graph/runtime surfaces; callers rely on them as executable semantic contracts.
Source scope:
OARemoteMultiplexerClient.getProxyForCtoS(...), getProxyForBroadcast(...), InvocationHandler.invoke(...),
onInvokeForCtoS(...); OARemoteMultiplexerServer.createProxyForStoC(...), onInvokeForStoC(...), createBroadcast(...),
onInvokeBroadcast(...).
Related CODEX findings:
Broadcast proxy can return response without throwing recorded exception; no-response and queued branches can hide
failure.
Suggested unit tests:
testSynchronousProxyWaitsForCorrelatedResponse, testNoResponseProxyDoesNotClaimRemoteCommit,
testBroadcastProxyThrowsRemoteExceptionWhenInvocationFailed.
Spec target section:
Remote Multiplexer / Proxy Invocation

REMOTE-MUX-REQUEST-001 — Request Response Correlation
Contract statement:
Every remote request expecting a response must have a stable message id/correlation record, and only the matching
terminal response may satisfy the waiting caller.
Rationale:
Concurrent remote calls must not consume each other’s responses or leave stale waiter state.
Source scope:
RequestInfo handling; OARemoteMultiplexerClient.onInvokeForCtoS(...), _onInvokeForCtoS(...), _processSocket(...),
hmAsyncRequestInfo; OARemoteMultiplexerServer callback request maps and wait/notify methods.
Related CODEX findings:
Client pending request can remain after timeout/disconnect; response conversion after pending-map removal can lose
waiter notification; server queued callback pending map can leak on enqueue/wait failure.
Suggested unit tests:
testConcurrentRemoteCallsReceiveCorrectResponses, testLateResponseDoesNotSatisfyTimedOutRequest,
testResponseConversionFailureNotifiesWaitingRequest.
Spec target section:
Remote Multiplexer / Request Correlation

REMOTE-MUX-REQUEST-002 — Pending Request Terminal Cleanup
Contract statement:
Pending request records must be removed or marked terminal exactly once on success, timeout, disconnect,
interruption, conversion failure, send failure, or close, and waiters must be notified.
Rationale:
Stale pending requests leak memory and can make future late responses corrupt diagnostics or request state.
Source scope:
OARemoteMultiplexerClient.hmAsyncRequestInfo, close(), onInvokeForCtoS(...), _onInvokeForCtoS(...),
_processSocket(...); OARemoteMultiplexerServer.hmClientCallbackRequestInfo, waitForMethodInvoked(...),
notifyMethodInvoked(...).
Related CODEX findings:
Client close does not fail pending async requests; timeout leaves pending entry; failure after
hmAsyncRequestInfo.put can leave stale request; server callback maps can retain stale correlation state.
Suggested unit tests:
testClientCloseFailsAndRemovesPendingAsyncRequests, testTimeoutRemovesPendingRequest,
testSendFailureAfterPendingPutRemovesRequest.
Spec target section:
Remote Multiplexer / Pending Request Cleanup

REMOTE-MUX-TRANSPORT-001 — Transport Success Versus Remote Success
Contract statement:
Successful socket write or queue enqueue means transport acceptance only; remote semantic success requires the
matching remote execution, response, ack, or no-response contract.
Rationale:
Transport delivery is not the same as method execution, sync apply, replication apply, or transaction commit.
Source scope:
OARemoteMultiplexerClient._onInvokeForCtoS(...), sendResponseForStoC(...);
OARemoteMultiplexerServer._invokeByRemoteThread(...), Session.writeOnQueueSocket(...), OACircularQueue integration.
Related CODEX findings:
Several queued/no-response branches can return without sending correlated failure or ack.
Suggested unit tests:
testTransportWriteDoesNotMeanRemoteMethodSucceeded, testQueuedRequestFailureReturnsCorrelatedFailure,
testNoResponseFailureObservableByDiagnostics.
Spec target section:
Remote Multiplexer / Transport Boundary

REMOTE-MUX-ORDER-001 — Ordered Remote Execution Where Required
Contract statement:
Remote calls using ordered queue or sync-queue semantics must preserve the documented request order, and any timeout
or failure that permits later work to continue must make the ordering boundary explicit and visible.
Rationale:
Sync and replication rely on deterministic ordered delivery and execution; silent out-of-order continuation can
diverge object graphs.
Source scope:
OARemoteMultiplexerClient.setupRequestQueueThread(), setupSyncRequestQueueThread(), putQueSyncRequestInfo(...),
addSyncRunnable(...); OARemoteMultiplexerServer queue readers and async queue senders.
Related CODEX findings:
Client sync queue thread continues after timeout before methodInvoked; interrupted enqueue handling can treat failed
queueing as success.
Suggested unit tests:
testSyncQueuePreservesRequestOrder, testSyncQueueTimeoutDoesNotSilentlyContinueOutOfOrder,
testInterruptedQueuePutFailsRequestVisibly.
Spec target section:
Remote Multiplexer / Ordered Execution

REMOTE-MUX-ASYNC-001 — Async Queue Lifecycle
Contract statement:
Async queue sender/reader state must be created, marked active, failed, and removed deterministically; exiting queue
threads must clear queue/socket markers or fail the owning session.
Rationale:
Callers must not believe queued remote delivery is still active after queue transport has failed.
Source scope:
OARemoteMultiplexerServer.Session.setupAsyncQueueSender(...), writeQueueMessages(...), getCircularQueue(...);
OARemoteMultiplexerClient setupRequestQueueThread(), setupSyncRequestQueueThread().
Related CODEX findings:
Server async queue sender thread exit can leave async queue marker/socket entry active; broadcast queue retry can
reuse stale qPos.
Suggested unit tests:
testAsyncQueueSenderExitMarksQueueFailed, testAsyncQueueMarkerRemovedOnSenderFailure,
testBroadcastQueueReaderResumesFromCommittedPosition.
Spec target section:
Remote Multiplexer / Async Queue Lifecycle

REMOTE-MUX-BROADCAST-001 — Broadcast Invocation Semantics
Contract statement:
Broadcast remote objects must fan out to intended sessions/callbacks according to queue policy and must preserve
original invocation arguments, exception state, and per-session delivery result.
Rationale:
Broadcast is multi-recipient remote execution; one mutation or failure path must not corrupt all client fan-out.
Source scope:
OARemoteMultiplexerServer.createBroadcast(...), onInvokeBroadcast(...), setupBroadcastQueueReader(...),
_writeQueueMessages(...); OARemoteMultiplexerClient.lookupBroadcast(...), getProxyForBroadcast(...).
Related CODEX findings:
Server broadcast can ignore exception before returning response; processCtoSArguments can mutate args before client
fan-out; duplicate broadcast registration can retain stale callback.
Suggested unit tests:
testBroadcastExceptionPropagatesToCallerByContract, testBroadcastPreservesOriginalArgumentsForFanOut,
testDuplicateBroadcastRegistrationContract.
Spec target section:
Remote Multiplexer / Broadcast Semantics

REMOTE-MUX-CALLBACK-001 — Callback Routing And Lifecycle
Contract statement:
Server-to-client and client-to-server callback proxies must route through the correct session, bind, socket, and
response channel, and callback registration/removal must prevent stale callbacks from receiving future invocations.
Rationale:
Callbacks are distributed runtime entry points and can affect object graph state, sync, UI, and business behavior.
Source scope:
OARemoteMultiplexerServer.createProxyForStoC(...), onInvokeForStoC(...), Session.getSocketForStoC(...);
OARemoteMultiplexerClient.createSocketForStoC(), processStoCSocket(...), sendResponseForStoC(...),
afterInvokForStoC(...).
Related CODEX findings:
Client stale bind in StoC queued no-response can fail silently; server callback request map cleanup and socket
pooling findings.
Suggested unit tests:
testCallbackRoutesToCorrectClientSession, testRemovedCallbackBindDoesNotReceiveInvocation,
testStaleCallbackBindFailureIsObservable.
Spec target section:
Remote Multiplexer / Callback Routing

REMOTE-MUX-SOCKET-001 — Remote Socket Pool Ownership
Contract statement:
Virtual sockets borrowed for remote calls, lookups, broadcasts, and responses must be returned to the pool only
after a complete clean frame exchange; on partial write/read/exception they must be closed or discarded.
Rationale:
Returning a dirty socket to the pool can corrupt framing, response correlation, and remote call ordering for later
calls.
Source scope:
OARemoteMultiplexerClient.getSocketForCtoS(), releaseSocketForCtoS(...), lookup(...), lookupBroadcast(...),
onInvokeForCtoS(...), sendResponseForStoC(...); OARemoteMultiplexerServer.Session.getSocketForStoC(),
releaseSocketForStoC(...), addSocketForStoC(...).
Related CODEX findings:
Client lookup and broadcast socket use lack try/finally cleanup; onInvokeForCtoS can double release; async response
socket path can release dirty socket; server outer finally can return failed StoC socket to pool.
Suggested unit tests:
testLookupReleasesSocketOnlyAfterCleanExchange, testFailedRemoteCallDiscardsSocketFromPool,
testSocketNotReleasedTwice.
Spec target section:
Remote Multiplexer / Socket Pool Semantics

REMOTE-MUX-SOCKET-002 — Reader Exit Is Terminal Or Recoverable
Contract statement:
When a client or server remote reader loop exits because of repeated errors, EOF, invalid frame, or disconnect, the
socket/session must be closed, reset, or marked failed so pending and future remote work cannot silently continue on
a dead path.
Rationale:
A remote endpoint cannot safely keep accepting work if its response/callback reader is gone.
Source scope:
OARemoteMultiplexerClient.createSocketForStoC(), processStoCSocket(...);
OARemoteMultiplexerServer.processSocketCtoS(...), processSocketCtoS request loops.
Related CODEX findings:
Client StoC reader can break after repeated errors without resetting socket-created state or failing session;
processStoCSocket exception may continue on corrupted stream.
Suggested unit tests:
testStoCReaderExitFailsPendingWork, testRepeatedReaderErrorsCloseRemotePath, testCorruptReaderStreamNotReused.
Spec target section:
Remote Multiplexer / Reader Lifecycle

REMOTE-MUX-EXCEPTION-001 — Remote Exception Visibility
Contract statement:
Exceptions thrown by remote method invocation, argument conversion, return conversion, queue write, response write,
callback execution, and serialization must be propagated to the caller, encoded in a correlated response, or logged
through an explicit failure channel.
Rationale:
Remote failures must not appear as successful method calls or silently skipped callbacks.
Source scope:
OARemoteMultiplexerClient.onInvokeForCtoS(...), _processSocket(...), processMessageForStoC(...),
sendResponseForStoC(...); OARemoteMultiplexerServer._invokeByRemoteThread(...), processSocketCtoS(...),
afterInvokeForCtoS(...), onException(...).
Related CODEX findings:
Client response conversion failure after map removal can leave waiter unnotified; server CtoS_ReturnOnQueueSocket
and queued broadcast branches can skip correlated error response; _writeOnQueueSocketX can set exceptionMessage then
return silently.
Suggested unit tests:
testRemoteMethodExceptionReturnedToCaller, testArgumentConversionFailureSendsCorrelatedFailure,
testQueueSocketMissingFailsRequestVisibly.
Spec target section:
Remote Multiplexer / Exception Semantics

REMOTE-MUX-THREAD-001 — Remote Thread Ownership
Contract statement:
Remote method execution must occur on an OA remote thread or documented execution context that owns the RequestInfo
for the duration of invocation and clears that context when invocation completes.
Rationale:
Remote thread context can affect sync, transaction, security, graph, and diagnostics behavior.
Source scope:
OARemoteMultiplexerClient.getRemoteThread(...), addSyncRunnable(...), remote worker creation;
OARemoteMultiplexerServer.invokeUsingRemoteThread(...), _invokeByRemoteThread(...), notifyMethodInvoked(...),
waitForMethodInvoked(...).
Related CODEX findings:
Client worker runnable does not clear requestInfo after r.run(); getRemoteThread InterruptedException handling can
lose interrupt/cancellation.
Suggested unit tests:
testRemoteThreadRequestInfoClearedAfterInvocation, testRemoteThreadInterruptedWaitRestoresInterrupt,
testRemoteThreadContextDoesNotLeakBetweenRequests.
Spec target section:
Remote Multiplexer / Remote Thread Context

REMOTE-MUX-TL-001 — Runtime Context Restoration
Contract statement:
Remote invocation must restore OA ThreadLocal/runtime context after send, receive, callback, argument conversion,
execution, response processing, and failure, including sendSyncMessages and request-specific runtime flags.
Rationale:
Remote calls participate in distributed object graph behavior; leaked context can corrupt sync, replication,
transaction, and trigger behavior.
Source scope:
OARemoteMultiplexerClient request processing and remote thread setup; OARemoteMultiplexerServer
_invokeByRemoteThread(...), shouldSendSyncMessageToClient(...), session serialization callback; OA runtime/thread
services.
Related CODEX findings:
Client requestInfo context leak; prompt specifically calls out sendSyncMessages and runtime-context boundaries.
Suggested unit tests:
testSendSyncMessagesRestoredAfterRemoteCall, testRuntimeContextRestoredAfterRemoteException,
testCallbackContextDoesNotLeakToNextInvocation.
Spec target section:
Remote Multiplexer / ThreadLocal Context

REMOTE-MUX-INTERRUPT-001 — Interrupt And Timeout Semantics
Contract statement:
Interrupted waits and queue puts must restore interrupt status and fail or cancel the affected remote request
visibly; timeouts must remove or terminally mark pending state.
Rationale:
Interrupted remote operations must not continue as successful or leave request maps waiting forever.
Source scope:
OARemoteMultiplexerClient.getRemoteThread(...), putQueSyncRequestInfo(...), addSyncRunnable(...),
onInvokeForCtoS(...); OARemoteMultiplexerServer waitForMethodInvoked(...), wait loops, queue puts.
Related CODEX findings:
InterruptedException is swallowed in addSyncRunnable, getRemoteThread, putQueSyncRequestInfo, and server wait loops;
timeout does not remove pending request.
Suggested unit tests:
testInterruptedQueuePutRestoresInterruptAndFailsRequest, testRemoteCallTimeoutRemovesPendingRequest,
testInterruptedRemoteWaitDoesNotReportSuccess.
Spec target section:
Remote Multiplexer / Timeout And Interruption

REMOTE-MUX-DGC-001 — Distributed Garbage Collection Semantics
Contract statement:
Remote weak-reference/DGC state must release remote bindings only when no live session/proxy/bind still owns them,
and DGC must not remove active callable objects.
Rationale:
Remote object lifecycle must not leak abandoned remote objects or drop still-reachable callbacks/proxies.
Source scope:
OARemoteMultiplexerClient.performDGC(), getBindInfoForObject(...); OARemoteMultiplexerServer.performDGC(),
Session.getGuidHashMap(), bind maps.
Related CODEX findings:
Package-info describes distributed garbage collection; CODEX findings include stale bind/session cleanup risks.
Suggested unit tests:
testDgcRemovesUnreferencedRemoteBind, testDgcDoesNotRemoveActiveProxyBind, testDgcScopedBySession.
Spec target section:
Remote Multiplexer / Distributed Garbage Collection

REMOTE-MUX-SERIAL-001 — Serialization Boundary
Contract statement:
Remote argument and return serialization must preserve OA object identity/reference semantics, remote object proxy
binding, class-descriptor/cache assumptions, and failure visibility.
Rationale:
Remote calls carry object graph state and proxies across process boundaries; serialization corruption becomes
distributed graph corruption.
Source scope:
RemoteObjectInputStream/RemoteObjectOutputStream integration; OARemoteMultiplexerClient and
OARemoteMultiplexerServer argument/return processing; OAObjectSerializer callback usage.
Related CODEX findings:
CODEX findings mention argument conversion, return conversion, and serialization stream cleanup failures.
Suggested unit tests:
testRemoteObjectArgumentBecomesProxyOrSerializedValueByContract, testRemoteReturnValueSerializationFailureFailsCall,
testRemoteObjectIdentityPreservedAcrossCall.
Spec target section:
Remote Multiplexer / Serialization Boundary

REMOTE-MUX-RESOURCE-001 — Stream Resource Cleanup
Contract statement:
Remote object input/output streams and virtual sockets opened for lookup, invocation, callback processing, queued
responses, and broadcast processing must be closed, released, or discarded according to ownership on success and
failure.
Rationale:
Remote runtime is long-lived; stream and socket leaks or dirty pooled streams corrupt future invocations.
Source scope:
OARemoteMultiplexerClient.lookup(...), lookupBroadcast(...), processStoCSocket(...), _processSocketCtoSRequest(...),
sendResponseForStoC(...); OARemoteMultiplexerServer.processSocketCtoS(...), Session.writeOnQueueSocket(...), queue
sender/reader paths.
Related CODEX findings:
Client _processSocketCtoSRequest ois close finding; lookup/broadcast/socket cleanup findings; server socket pool and
queue socket cleanup findings.
Suggested unit tests:
testRemoteObjectInputStreamClosedAfterRequestProcessing, testLookupSocketClosedOnFailure,
testQueueSocketDiscardedAfterWriteFailure.
Spec target section:
Remote Multiplexer / Resource Cleanup

REMOTE-MUX-CONCURRENT-001 — Concurrent Remote Calls
Contract statement:
Concurrent remote calls, callbacks, lookups, broadcasts, DGC, close, and session removal must not corrupt request
maps, bind maps, socket pools, queue state, or response routing.
Rationale:
Remote runtime is concurrent by design; races can route results to the wrong caller or leak distributed state.
Source scope:
OARemoteMultiplexerClient hmAsyncRequestInfo, bind maps, socket pool, queues; OARemoteMultiplexerServer session
maps, bind maps, callback maps, circular queues, remote thread pools.
Related CODEX findings:
Request map cleanup, bind duplication, async queue, session/socket, and remote thread position overflow findings.
Suggested unit tests:
testConcurrentRemoteCallsReturnToCorrectWaiters, testConcurrentCloseFailsAllPendingRequestsOnce,
testRemoteClientThreadIndexOverflowDoesNotBreakSelection.
Spec target section:
Remote Multiplexer / Concurrency

REMOTE-MUX-FAIL-001 — No Silent Remote False-Success
Contract statement:
Failed remote calls, callbacks, broadcasts, lookups, registrations, queue writes, disconnects, and close operations
must not silently appear successful to the initiating runtime participant.
Rationale:
Remote false-success causes distributed object graph divergence, missed sync/replication updates, and incorrect
callable graph behavior.
Source scope:
OARemoteMultiplexerClient; OARemoteMultiplexerServer; RequestInfo; BindInfo; queue and socket paths.
Related CODEX findings:
Multiple client/server CODEX findings identify silent returns after exceptionMessage, stale bind no-response,
missing queue socket, and swallowed callback exceptions.
Suggested unit tests:
testLookupFailureVisibleToCaller, testCallbackFailureVisibleToInitiator, testQueuedBroadcastFailureNotSilent.
Spec target section:
Remote Multiplexer / False-Success Prevention

REMOTE-MUX-INTEGRATION-001 — Sync Replication And Graph Runtime Compatibility
Contract statement:
Remote multiplexer behavior must preserve the ordering, context restoration, failure visibility, object identity,
callback routing, and transport/runtime boundary assumptions required by sync, replication, transaction, object,
Hub, graph, and runtime packages.
Rationale:
This package is the executable remote boundary for distributed OA/OG behavior.
Source scope:
OARemoteMultiplexerClient; OARemoteMultiplexerServer; comm.multiplexer integration; remote, sync, replication,
transaction, object, hub, graph, runtime integration.
Related CODEX findings:
Prompt and CODEX findings call out sendSyncMessages, queued ordering, request correlation, remote context, and
transport false-success risks.
Suggested unit tests:
testSyncRemoteCallPreservesSendSyncContext, testReplicationRemoteFailureDoesNotAppearApplied,
testRemoteInvocationPreservesGraphObjectIdentity.
Spec target section:
Remote Multiplexer / Cross-Package Runtime Integration

*/
































