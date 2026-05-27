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
 * Core communication framework for OA applications. The {@code com.viaoa.comm}
 * package provides the foundational abstractions and transport mechanisms used
 * across OA’s distributed systems, enabling secure, discoverable, multiplexed,
 * and extensible communication channels between clients, servers, and
 * services.
 *
 * <h2>Architecture Overview</h2>
 * <p>
 * The communication layer is organized into specialized subpackages, each
 * responsible for a distinct capability:
 * </p>
 *
 * <ul>
 *   <li><strong>discovery</strong> –
 *       Lightweight UDP-based service discovery for locating OA endpoints on
 *       a network.</li>
 *
 *   <li><strong>http</strong> –
 *       Basic HTTP/HTTPS utilities and JSON-based request helpers.</li>
 *
 *   <li><strong>io</strong> –
 *       OA-enhanced serialization streams, including object-stream utilities
 *       used throughout OA’s distributed object graph.</li>
 *
 *   <li><strong>multiplexer</strong> –
 *       High-performance virtual-socket system that allows many independent
 *       logical channels to coexist on a single physical TCP connection. This
 *       includes input/output controllers, virtual sockets, and server/client
 *       socket controllers.</li>
 *
 *   <li><strong>ssl</strong> –
 *       Transport-agnostic SSL/TLS engine wrappers built on {@link
 *       javax.net.ssl.SSLEngine}, providing encrypted communication for TCP or
 *       multiplexed channels.</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Transport independence</strong> – Higher layers (e.g., OA
 *       Object Sync, distributed messaging) do not depend on specific socket
 *       or protocol implementations.</li>
 *
 *   <li><strong>Modularity</strong> – Components such as SSL, multiplexing,
 *       or discovery can be used individually or combined to build richer
 *       communication stacks.</li>
 *
 *   <li><strong>Performance</strong> – Multiplexed virtual channels eliminate
 *       the overhead of managing many physical connections.</li>
 *
 *   <li><strong>Security</strong> – Built-in SSL/TLS support ensures encrypted
 *       channels for any transport.</li>
 * </ul>
 *
 * <h2>Intended Usage</h2>
 * <p>
 * Applications may use the components in this package directly or rely on
 * higher-level OA subsystems that internally leverage these communication
 * layers to synchronize data, coordinate distributed processes, and perform
 * service-to-service interactions.
 * </p>
 */
package com.viaoa.comm;

//CODEX unit tests <todo>

/* CODEX Invariants

COMM-RUNTIME-001 — Internal OA Communication Boundary
Contract statement:
com.viaoa.comm defines internal OA-controlled communication semantics for distributed runtime participants; it
assumes OA-produced protocol messages and focuses on lifecycle, ordering, framing, delivery, and failure correctness
rather than hostile public-protocol hardening.
Rationale:
OA communication supports remote execution, sync, replication, object graph coordination, and distributed runtime
behavior where correctness depends on trusted protocol participants and explicit failure boundaries.
Source scope:
com.viaoa.comm package; com.viaoa.comm.discovery, http, io, multiplexer, ssl subpackages; remote/sync/replication
consumers.
Related CODEX findings:
Existing package-info notes hostile/public protocol hardening is out of scope unless malformed state can occur
during normal OA usage.
Suggested unit tests:
testValidOAMessageRoundTrip, testInternalProtocolAssumptionsDocumented, testInvalidInternalStateFailsConnection.
Spec target section:
Communication Runtime / Internal Transport Contract

COMM-LIFECYCLE-001 — Communication Session Lifecycle
Contract statement:
Each communication session or endpoint must move through deterministic lifecycle states: configured, connecting/
starting, active, closing/disconnecting, closed, failed, or reconnected-as-new-session.
Rationale:
Distributed OA callers must not send, receive, sync, replicate, or perform remote work through stale or ambiguously
closed communication state.
Source scope:
Top-level comm contract; multiplexer client/server lifecycle; discovery/http/io/ssl lifecycle; remote/sync/
replication transport boundaries.
Related CODEX findings:
Existing package-info reconnect/disconnect and stale socket lifecycle risks.
Suggested unit tests:
testConnectionStateTransitionsAreMonotonic, testClosedConnectionDoesNotAcceptSend,
testReconnectCreatesNewLifecycleState.
Spec target section:
Communication Runtime / Session Lifecycle

COMM-LIFECYCLE-002 — Disconnect Propagation
Contract statement:
A physical or logical disconnect must propagate to dependent streams, virtual channels, blocked readers/writers,
remote call waiters, sync queues, replication queues, and runtime observers through a visible closed or failed
state.
Rationale:
Higher layers need visible failure to retry, reconcile, or abort distributed graph work.
Source scope:
comm lifecycle contract; multiplexer virtual sockets; IO streams; remote invocation transport; sync/replication send
queues.
Related CODEX findings:
Existing package-info notes shutdown and blocked-thread risks.
Suggested unit tests:
testPhysicalDisconnectClosesVirtualSockets, testDisconnectWakesRemoteCallWaiter,
testDisconnectWakesSyncReplicationQueues.
Spec target section:
Communication Runtime / Disconnect Propagation

COMM-FAIL-001 — Communication Failure Visibility
Contract statement:
A failed send, receive, frame parse, handshake, flush, close, timeout, EOF, or connection transition must be
observable through exception, failure result, callback, diagnostic, or closed/failed state; it must not appear as
successful delivery.
Rationale:
Silent communication failure causes remote call hangs, sync message loss, replication divergence, and stale
distributed object graph state.
Source scope:
comm package-level contract; socket clients/servers; multiplexer; SSL; stream wrappers; remote/sync/replication
integration.
Related CODEX findings:
Existing package-info false-success, discard, partial-frame, and blocked-thread findings.
Suggested unit tests:
testSendFailurePropagatesToCaller, testReceiveFailureClosesOrMarksConnectionFailed,
testWriteFailureTransitionsConnectionToDisconnecting.
Spec target section:
Communication Runtime / Failure Visibility

COMM-FAIL-002 — Partial Progress Is Visible
Contract statement:
Partial sends, partial reads, incomplete frames, incomplete object reads, failed skips, or interrupted message
publication must either complete according to contract or fail visibly; they must not be reported as complete
logical messages.
Rationale:
Remote invocation, serialization, sync, and replication cannot safely consume partial protocol messages as valid
data.
Source scope:
comm stream contract; com.viaoa.comm.io; multiplexer frame read/write/discard paths; remote object streams.
Related CODEX findings:
Existing package-info partial-frame/read/write/skip findings.
Suggested unit tests:
testPartialObjectReadThrowsOrDisconnects, testPartialSendFailureVisibleToCaller,
testMidFrameReadFailureClosesConnection.
Spec target section:
Communication Runtime / Partial Progress

COMM-FRAME-001 — Frame Boundaries Are Authoritative
Contract statement:
Every framed communication message must be written, read, skipped, discarded, and dispatched as one complete frame
boundary, or the connection must be failed when frame alignment cannot be preserved.
Rationale:
Losing frame boundaries corrupts every higher-level protocol sharing the connection.
Source scope:
multiplexer frame reader/writer classes; virtual socket streams; comm stream wrappers; SSL-framed transport
integration.
Related CODEX findings:
Existing package-info frame discard/skip and partial-frame findings.
Suggested unit tests:
testMultiplexerPreservesSingleFrameBoundary, testBackToBackFramesReadSeparately,
testFrameParseFailureDoesNotAllowNextFrameAsValid.
Spec target section:
Communication Runtime / Frame Integrity

COMM-FRAME-002 — Discard And Skip Consume Exact Bytes
Contract statement:
Discard and skip operations must consume exactly the intended frame/message bytes, looping through short skips/reads
as needed, or fail/close the connection when exact consumption cannot be guaranteed.
Rationale:
Java stream skip/read calls are not guaranteed to complete the requested amount in one call; assuming they do
corrupts subsequent frames.
Source scope:
multiplexer discard/skip/read paths; stream wrappers; input stream discard helpers.
Related CODEX findings:
Existing package-info skip/read/discard findings.
Suggested unit tests:
testDiscardFrameConsumesEntirePayload, testSkipLoopHandlesShortSkip, testSkipZeroProgressFallsBackOrFails.
Spec target section:
Communication Runtime / Frame Discard Semantics

COMM-ORDER-001 — Ordered Delivery Within A Channel
Contract statement:
Messages or bytes written on one logical communication channel must be observed by its peer in the same order unless
the channel has failed or closed visibly.
Rationale:
Object streams, remote requests, sync events, and replication logs require per-channel FIFO ordering.
Source scope:
comm package-level ordering contract; multiplexer virtual sockets; stream wrappers; remote/sync/replication
transports.
Related CODEX findings:
Existing package-info virtual socket and message ordering findings.
Suggested unit tests:
testVirtualSocketPreservesWriteOrder, testRemoteMessagesDeliveredInSendOrder,
testReplicationMessagesDeliveredInSendOrder.
Spec target section:
Communication Runtime / Ordered Delivery

COMM-ORDER-002 — Cross-Channel Ordering Boundary
Contract statement:
Ordering across independent logical channels must be explicitly documented as either unordered, controller-
scheduled, or globally ordered; higher layers must not infer global order unless comm explicitly provides it.
Rationale:
Multiplexed and asynchronous communication can preserve per-channel order while allowing inter-channel reordering.
Source scope:
comm parent contract; multiplexer subpackage; remote/sync/replication transport selection.
Related CODEX findings:
Existing package-info queue ordering and reconnect ordering findings.
Suggested unit tests:
testPerChannelOrderingDoesNotImplyGlobalOrdering, testInterleavedVirtualSocketsPreservePerSocketOrder.
Spec target section:
Communication Runtime / Ordering Boundary

COMM-REQUEST-001 — Request Response Pairing
Contract statement:
Synchronous request/response communication must correlate each response to the initiating request and must not allow
late, duplicate, failed, or unrelated responses to satisfy the wrong waiter.
Rationale:
Remote method calls and distributed runtime operations depend on exact request/response pairing under concurrency.
Source scope:
comm parent contract; remote multiplexer integration; request id and response wait queues in higher communication
layers.
Related CODEX findings:
Existing package-info request/response matching risks.
Suggested unit tests:
testConcurrentRemoteCallsReceiveCorrectResponses, testLateResponseDoesNotSatisfyWrongRequest,
testDisconnectWakesRemoteCallWaiter.
Spec target section:
Communication Runtime / Request Response Semantics

COMM-ASYNC-001 — Async Versus Sync Boundary
Contract statement:
The comm contract must distinguish synchronous request completion, asynchronous send acceptance, transport delivery,
and higher-level semantic completion; success at one layer must not be reported as success at another layer.
Rationale:
A successful write does not mean a remote method committed, a sync event applied, or a replication message became
durable.
Source scope:
comm parent contract; multiplexer; remote; sync; replication; transaction integration.
Related CODEX findings:
Existing package-info retry/reconnect and false-success findings.
Suggested unit tests:
testTransportWriteSuccessDoesNotImplyRemoteSemanticAck, testAsyncSendFailureObservable,
testReplicationTransportSuccessDoesNotMeanApplyCommitted.
Spec target section:
Communication Runtime / Semantic Boundary

COMM-MUX-001 — Multiplexer Channel Isolation
Contract statement:
When multiplexing is used, each logical virtual socket/channel must preserve its own identity, byte stream, close
state, ordering, and failure state without cross-delivering data to another channel.
Rationale:
Many OA runtime services share one physical connection; cross-channel contamination breaks remote calls, sync,
replication, and service isolation.
Source scope:
com.viaoa.comm.multiplexer; com.viaoa.comm.multiplexer.io; VirtualSocket; VirtualServerSocket; multiplexer
controllers.
Related CODEX findings:
Existing package-info virtual socket routing and isolation risks.
Suggested unit tests:
testFrameDeliveredToMatchingVirtualSocketOnly, testFrameForClosedVirtualSocketIsDiscardedSafely,
testTwoVirtualSocketsDoNotCrossDeliverBytes.
Spec target section:
Communication Runtime / Multiplexer Channel Isolation

COMM-MUX-002 — Multiplexer Close Wakes Dependents
Contract statement:
Closing or failing a multiplexed physical connection must wake blocked virtual socket readers and writers with a
clear closed/failure state.
Rationale:
Shutdown must not leave remote, sync, or replication worker threads blocked forever.
Source scope:
com.viaoa.comm.multiplexer and io subpackage; virtual socket wait/notify paths; multiplexer close/shutdown paths.
Related CODEX findings:
Existing package-info blocked reader/writer shutdown findings.
Suggested unit tests:
testMultiplexerCloseWakesBlockedVirtualReader, testMultiplexerCloseWakesBlockedVirtualWriter.
Spec target section:
Communication Runtime / Multiplexer Shutdown

COMM-STREAM-001 — Stream Read Semantics
Contract statement:
Stream and object-read paths must either produce a complete logical byte sequence/object/frame for the active
channel or fail visibly; EOF and close must be distinguishable from temporary no-data conditions.
Rationale:
ObjectInputStream, remote calls, and sync/replay deserialization require complete logical input.
Source scope:
com.viaoa.comm.io; multiplexer virtual input streams; remote object streams.
Related CODEX findings:
Existing package-info stream read, partial object read, EOF/close, and blocked read findings.
Suggested unit tests:
testCompleteObjectReadSucceeds, testPartialObjectReadThrowsOrDisconnects,
testEOFAndCloseDistinguishableFromEmptyRead.
Spec target section:
Communication Runtime / Stream Read Semantics

COMM-STREAM-002 — Stream Write Semantics
Contract statement:
A successful stream write or flush means bytes were accepted by the transport queue/stream according to the layer
contract, or failure was reported; writes must preserve byte order for their channel.
Rationale:
Callers use successful write/flush as transport publication, and higher-level protocols depend on ordered bytes.
Source scope:
com.viaoa.comm.io; multiplexer virtual output streams; socket write loops; remote object streams.
Related CODEX findings:
Existing package-info write ordering and false-success send findings.
Suggested unit tests:
testWriteFailureVisibleToCaller, testFlushFailureMarksConnectionFailed, testVirtualOutputStreamPreservesByteOrder.
Spec target section:
Communication Runtime / Stream Write Semantics

COMM-BLOCK-001 — Blocking Operations Wake Correctly
Contract statement:
Threads blocked on communication reads, writes, accepts, request waits, queue capacity, or response waits must wake
when data/capacity arrives, close occurs, timeout expires, interruption is honored, or failure occurs.
Rationale:
Blocked communication threads can stall OA distributed runtime, sync, replication, and remote processing.
Source scope:
comm parent contract; multiplexer virtual sockets; server accept loops; remote call waiters; sync/replication
queues.
Related CODEX findings:
Existing package-info blocked reader/writer and shutdown risks.
Suggested unit tests:
testBlockedReadWakesOnData, testBlockedReadWakesOnClose, testBlockedWriteWakesOnCapacity,
testBlockedWriteWakesOnClose.
Spec target section:
Communication Runtime / Blocking And Wakeup

COMM-THREAD-001 — Transport Thread Ownership
Contract statement:
Physical socket input and output streams must have clear owner threads or synchronized access paths, and concurrent
logical send/receive operations must serialize through the transport contract.
Rationale:
Unsynchronized concurrent physical stream access breaks framing, ordering, and request/response pairing.
Source scope:
comm parent contract; multiplexer reader/writer controllers; stream wrappers; socket client/server implementations.
Related CODEX findings:
Existing package-info read/write ordering and thread ownership findings.
Suggested unit tests:
testSingleReaderOwnsPhysicalInputStream, testConcurrentVirtualWritesSerializeThroughMux, testConcurrentCloseIsSafe.
Spec target section:
Communication Runtime / Thread Ownership

COMM-CONTEXT-001 — Runtime Context Propagation Boundary
Contract statement:
Any ThreadLocal or runtime context propagated for remote execution, sync, replication, or communication callbacks
must be explicitly scoped and restored after send, receive, callback, failure, and disconnect handling.
Rationale:
Distributed communication must not leak transaction, sync, security, graph, or replay context across pooled/runtime
threads.
Source scope:
comm parent contract; remote/sync/replication integration; runtime ThreadLocal interaction.
Related CODEX findings:
Existing package-info focuses on comm failure/order; ThreadLocal restoration remains a cross-package boundary.
Suggested unit tests:
testRemoteReceiveRestoresThreadLocalContext, testSyncSendFailureRestoresRuntimeContext,
testCommCallbackDoesNotLeakTransactionContext.
Spec target section:
Communication Runtime / Runtime Context

COMM-SECURE-001 — SSL Preserves Communication Semantics
Contract statement:
SSL/TLS wrapping must preserve the same message boundaries, ordering, lifecycle, failure, and wakeup semantics as
unencrypted communication, while handshake/setup failure must leave the connection visibly failed or closed.
Rationale:
Secure transport is a channel wrapper, not a semantic change to OA remote/sync/replication behavior.
Source scope:
com.viaoa.comm.ssl; SSL socket/stream classes; multiplexer stream integration.
Related CODEX findings:
Existing package-info SSL frame and handshake lifecycle notes.
Suggested unit tests:
testSslFrameRoundTripPreservesBoundary, testSslBackToBackFramesPreserveOrder,
testSslHandshakeFailureMarksConnectionFailed, testSslHandshakeFailureWakesWaiters.
Spec target section:
Communication Runtime / SSL Boundary

COMM-RECONNECT-001 — Reconnect Starts New Ordering Epoch
Contract statement:
Reconnect after disconnect or failure must create a new communication lifecycle and ordering epoch; unsent, partial,
or in-flight messages from the prior epoch must not be silently reordered, duplicated, or reported as delivered.
Rationale:
Sync and replication cannot tolerate hidden reorder or duplicate delivery across reconnect.
Source scope:
comm parent contract; reconnect/disconnect handling; send queues; multiplexer lifecycle; remote/sync/replication
callers.
Related CODEX findings:
Existing package-info reconnect/disconnect ordering and retry risks.
Suggested unit tests:
testReconnectStartsNewOrderingEpoch, testDisconnectDuringSendDoesNotSilentlyReorderMessages,
testCommFailureDoesNotSilentlyDuplicateMessage.
Spec target section:
Communication Runtime / Reconnect Semantics

COMM-RETRY-001 — Retry Is Owned Above Transport Unless Contracted
Contract statement:
The comm layer must not silently retry failed semantic messages in a way that can duplicate, reorder, or hide
delivery failure; higher-level remote/sync/replication code owns semantic retry unless comm explicitly documents a
transport-only retry.
Rationale:
Retries across distributed object graph operations require idempotency and ordering rules that transport alone
cannot infer.
Source scope:
comm parent contract; reconnect/retry paths; remote/sync/replication integration.
Related CODEX findings:
Existing package-info retry/reconnect risks.
Suggested unit tests:
testCommFailureDoesNotSilentlyDuplicateMessage, testReconnectRetryPreservesOrderingEpoch,
testTransportRetryDoesNotClaimSemanticSuccess.
Spec target section:
Communication Runtime / Retry Boundary

COMM-RESOURCE-001 — Resource Cleanup
Contract statement:
Closing or failing communication must release owned sockets, server sockets, input/output streams, virtual sockets,
queues, controllers, accept threads, reader/writer threads, SSL resources, and waiters according to ownership.
Rationale:
Long-running OA clients and servers must not leak transport resources across disconnects, reconnects, shutdown, or
failure paths.
Source scope:
comm parent contract; comm.io, multiplexer, ssl, discovery/http resources.
Related CODEX findings:
Existing package-info resource cleanup risks.
Suggested unit tests:
testCloseClosesPhysicalSocket, testCloseReleasesVirtualSocketResources, testCloseReleasesOwnedThreadsAndQueues.
Spec target section:
Communication Runtime / Resource Cleanup

COMM-RESOURCE-002 — Idempotent Shutdown
Contract statement:
close, disconnect, stop, shutdown, and cleanup operations must be idempotent and must not resurrect active
communication state, duplicate close notifications, or hide cleanup failure.
Rationale:
Failure paths often cleanup from multiple layers and callbacks.
Source scope:
comm parent contract; multiplexer client/server close/stop; stream close methods; SSL close paths.
Related CODEX findings:
Existing package-info idempotent shutdown and cleanup path risks.
Suggested unit tests:
testCloseIsIdempotent, testConcurrentCloseIsSafe, testRepeatedShutdownDoesNotResurrectConnection.
Spec target section:
Communication Runtime / Idempotent Shutdown

COMM-METRICS-001 — Communication State And Metrics Are Observable
Contract statement:
Connection state, read/write counts, byte counts, created/live connection counts, and failure/close state must
reflect communication controller state consistently enough for runtime diagnostics.
Rationale:
Production diagnostics need trustworthy observability for distributed runtime failures.
Source scope:
comm parent contract; multiplexer client/server metrics; transport controllers; logging integration.
Related CODEX findings:
Existing package-info diagnostics and false-success concerns.
Suggested unit tests:
testMetricsZeroBeforeStart, testMetricsIncreaseAfterTraffic, testLiveConnectionCountUpdatesOnDisconnect.
Spec target section:
Communication Runtime / Observability

COMM-AUTHORITY-001 — Distributed Runtime Authority Boundary
Contract statement:
Communication transports may carry remote, sync, replication, and graph-runtime messages, but they must not decide
object graph authority, transaction commit, replication merge, security authorization, or semantic success unless
explicitly delegated by the owning package.
Rationale:
Transport success and distributed runtime semantic success are different contracts.
Source scope:
comm parent contract; remote, sync, replication, graph, transaction, secure integration.
Related CODEX findings:
Existing package-info retry and semantic delivery notes imply this boundary.
Suggested unit tests:
testTransportSuccessDoesNotCommitTransaction, testTransportSuccessDoesNotMeanReplicationApplied,
testRemoteAuthorityDecisionOwnedOutsideComm.
Spec target section:
Communication Runtime / Runtime Authority Boundary

COMM-INTEGRATION-001 — Remote Sync Replication Compatibility
Contract statement:
Communication behavior must remain compatible with remote invocation, sync message ordering, replication replay/
transport, serialization boundaries, transaction context, graph runtime ownership, and security/session context.
Rationale:
OA distributed graph correctness depends on communication preserving ordering, isolation, failure visibility, and
context boundaries while higher layers enforce semantic contracts.
Source scope:
com.viaoa.comm package and subpackages; com.viaoa.remote; com.viaoa.sync; com.viaoa.replication; serialization;
runtime/graph/transaction/security integration.
Related CODEX findings:
Existing package-info remote/sync/replication ordering, request/response, reconnect, and failure findings.
Suggested unit tests:
testRemotePayloadRoundTripsOverCommTransport, testSyncMessagesDeliveredInSendOrder,
testReplicationTransportFailureDoesNotAppearCommitted, testRemoteCallContextRestoredAfterFailure.
Spec target section:
Communication Runtime / Cross-Package Integration

*/



