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


/* CODEX Invariants

1. Communication Runtime Contracts

  COMM-RUNTIME-001 — Comm Is An Internal OA-Controlled Transport
  Contract statement: com.viaoa.comm assumes valid OA-produced frames/messages and is not a hostile public protocol
  surface.
  Rationale: Correctness focus is OA internal ordering, framing, lifecycle, and cleanup, not arbitrary malformed
  external input hardening.
  Source locations: com.viaoa.comm.*, multiplexer, socket, stream, SSL classes.
  Known related CODEX findings: hostile-input-only concerns were explicitly treated as out of scope.
  Suggested unit tests: testValidOAFrameRoundTrip(),
  testInternalTransportRejectsBrokenConnectionOnInvalidInternalFrame()
  Spec target section: Communication Runtime / Internal Transport Contract

  COMM-RUNTIME-002 — Transport Failure Must Be Visible Or Disconnecting
  Contract statement: A failed read/write/frame operation must either throw/return a clear failure result or
  transition the connection toward disconnect/cleanup.
  Rationale: Silent comm failure causes remote/sync/replication divergence.
  Source locations: socket connection classes, multiplexer read/write loops, stream wrappers.
  Known related CODEX findings: silent false-success and blocked-thread issues found/fixed/commented during comm
  scans.
  Suggested unit tests: testWriteFailureTransitionsConnectionToDisconnecting(),
  testReadFailureDoesNotReturnFalseSuccess()
  Spec target section: Communication Runtime / Failure Visibility

  2. Multiplexer Frame / Virtual Socket Contracts

  COMM-MUX-001 — Frame Boundaries Are Authoritative
  Contract statement: Each multiplexer frame must be read, written, skipped, and dispatched as one complete frame
  boundary.
  Rationale: Virtual sockets share one physical connection; losing boundaries corrupts every stream above it.
  Source locations: multiplexer frame reader/writer classes, virtual socket input/output streams.
  Known related CODEX findings: frame discard/skip correctness issues were found and fixed/noted.
  Suggested unit tests: testMuxReadsExactlyOneFramePerDispatch(),
  testMuxPreservesFrameBoundaryAcrossMultipleVirtualSockets()
  Spec target section: Communication Runtime / Multiplexer Framing

  COMM-MUX-002 — Discarded Frames Must Be Fully Consumed Or Fail Connection
  Contract statement: If a frame is discarded, all remaining bytes in that frame must be consumed, or the physical
  connection must be failed.
  Rationale: Leaving frame bytes in the stream misaligns the next frame and corrupts transport.
  Source locations: multiplexer discard/skip/read methods.
  Known related CODEX findings: discard/skip partial-frame issues were found in comm scans.
  Suggested unit tests: testDiscardFullyConsumesFrameBytes(), testFailedDiscardClosesConnection()
  Spec target section: Communication Runtime / Frame Discard Semantics

  COMM-MUX-003 — Virtual Socket IDs Route Frames To Exactly One Recipient
  Contract statement: A frame for a virtual socket ID must be delivered to exactly that virtual socket or rejected/
  cleaned up if the socket is closed.
  Rationale: Cross-delivery breaks request/response pairing and stream isolation.
  Source locations: multiplexer socket registry, virtual socket dispatch logic.
  Known related CODEX findings: virtual socket routing risks reviewed.
  Suggested unit tests: testFrameDeliveredToMatchingVirtualSocketOnly(),
  testFrameForClosedVirtualSocketIsDiscardedSafely()
  Spec target section: Communication Runtime / Virtual Socket Routing

  COMM-MUX-004 — Virtual Sockets Preserve Per-Socket FIFO Order
  Contract statement: Frames for the same virtual socket must be delivered to that socket in send order.
  Rationale: Remote calls, object streams, and sync messages assume stream order.
  Source locations: multiplexer queue/dispatch logic, virtual socket input stream.
  Known related CODEX findings: ordering risks reviewed during comm scan.
  Suggested unit tests: testVirtualSocketFramesDeliveredInFifoOrder(),
  testInterleavedVirtualSocketsPreservePerSocketOrder()
  Spec target section: Communication Runtime / Virtual Socket Ordering

  3. Input / Output Stream Contracts

  COMM-STREAM-001 — Stream Read Must Not Cross Frame Boundaries Incorrectly
  Contract statement: Virtual input streams must expose bytes in the correct sequence for their virtual socket and
  must not leak bytes from another frame/socket.
  Rationale: ObjectInputStream and remote call protocols depend on byte stream correctness.
  Source locations: virtual socket input stream classes, frame queues.
  Known related CODEX findings: stream read/frame boundary issues reviewed.
  Suggested unit tests: testVirtualInputStreamReadsOnlyOwnSocketBytes(), testPartialReadsResumeWithinSameFrame()
  Spec target section: Communication Runtime / Input Stream Semantics

  COMM-STREAM-002 — Stream Write Must Preserve Message Byte Order
  Contract statement: Bytes written to a virtual output stream must be framed and sent in the same logical order.
  Rationale: Object serialization and remote command streams are order-sensitive.
  Source locations: virtual socket output stream classes, multiplexer writer.
  Known related CODEX findings: write ordering issues reviewed.
  Suggested unit tests: testVirtualOutputStreamPreservesByteOrder(), testMultipleWritesArriveInSameLogicalOrder()
  Spec target section: Communication Runtime / Output Stream Semantics

  COMM-STREAM-003 — EOF And Close Must Be Distinguishable From Empty Read
  Contract statement: Stream close/disconnect/EOF must be reported distinctly from temporary no-data conditions.
  Rationale: Reader threads must know whether to wait, retry, or terminate.
  Source locations: input stream read methods, virtual socket close handling.1. Communication Runtime Contracts

  COMM-RUNTIME-001 — Comm Is An Internal OA-Controlled Transport
  Contract statement: com.viaoa.comm assumes valid OA-produced protocol messages and is not required to harden
  against arbitrary hostile external protocol input unless that malformed state can occur during normal OA usage.
  Rationale: This keeps the layer focused on fast internal remote/sync/replication transport correctness.
  Source locations: com.viaoa.comm.*, multiplexer/socket/stream classes.
  Known related CODEX findings: hostile/public protocol hardening findings were accepted as out of scope.
  Suggested unit tests: testValidOAMessageRoundTrip(), testInternalProtocolAssumptionsDocumented()
  Spec target section: Communication Runtime / Internal Transport Contract

  COMM-RUNTIME-002 — Communication Failure Must Be Visible To Higher Layers
  Contract statement: A failed send, receive, frame parse, or connection transition must not be reported as
  successful to remote/sync/replication callers.
  Rationale: Silent communication failure causes message loss and graph divergence.
  Source locations: socket client/server classes, multiplexer classes, input/output stream wrappers.
  Known related CODEX findings: false-success/discard/partial-frame issues reviewed during comm scans.
  Suggested unit tests: testSendFailurePropagatesToCaller(), testReceiveFailureClosesOrMarksConnectionFailed()
  Spec target section: Communication Runtime / Failure Semantics

  2. Multiplexer Frame / Virtual Socket Contracts

  COMM-MUX-001 — Frame Boundaries Are Preserved
  Contract statement: Every logical message/frame sent through the multiplexer must be read as exactly one complete
  frame by the receiver.
  Rationale: Remote calls, sync events, and replication messages depend on unambiguous frame boundaries.
  Source locations: multiplexer frame reader/writer classes, virtual socket streams.
  Known related CODEX findings: frame discard/skip and partial-frame issues found/fixed/commented.
  Suggested unit tests: testMultiplexerPreservesSingleFrameBoundary(), testBackToBackFramesReadSeparately()
  Spec target section: Communication Runtime / Multiplexer Framing

  COMM-MUX-002 — Virtual Socket Streams Are Ordered Per Virtual Connection
  Contract statement: Bytes/messages written to a virtual socket must be read in the same order on that virtual
  socket.
  Rationale: Request/response and object stream protocols require per-channel ordering.
  Source locations: virtual socket input/output stream classes, multiplexer queue handling.
  Known related CODEX findings: queue ordering risks reviewed.
  Suggested unit tests: testVirtualSocketPreservesWriteOrder(),
  testInterleavedVirtualSocketsPreservePerSocketOrder()
  Spec target section: Communication Runtime / Virtual Socket Ordering

  COMM-MUX-003 — Discarded Frame Must Be Fully Consumed Or Connection Failed
  Contract statement: If a frame is discarded, the comm layer must either consume the entire frame payload or fail/
  close the connection.
  Rationale: Partial discard corrupts the next frame boundary and breaks all subsequent messages.
  Source locations: multiplexer frame reader/discard paths, input stream skip/read logic.
  Known related CODEX findings: discard/skip correctness bugs were found and fixed/commented.
  Suggested unit tests: testDiscardFrameConsumesEntirePayload(), testPartialDiscardFailureClosesConnection()
  Spec target section: Communication Runtime / Frame Discard Semantics

  COMM-MUX-004 — Multiplexer Close Must Wake Dependent Virtual Sockets
  Contract statement: Closing the physical/multiplexer connection must wake blocked virtual socket readers/writers
  with a clear closed/failure state.
  Rationale: Shutdown must not leave blocked threads waiting forever.
  Source locations: multiplexer close/shutdown paths, virtual socket wait/notify paths.
  Known related CODEX findings: blocked-thread/shutdown risks reviewed.
  Suggested unit tests: testMultiplexerCloseWakesBlockedVirtualReader(),
  testMultiplexerCloseWakesBlockedVirtualWriter()
  Spec target section: Communication Runtime / Multiplexer Shutdown

  3. Input / Output Stream Contracts

  COMM-STREAM-001 — Read Must Not Return Partial Logical Object As Success
  Contract statement: Stream/object read paths must either produce a complete logical value/frame or fail visibly.
  Rationale: Remote invocation and sync deserialize logical objects; partial reads cannot be accepted as valid data.
  Source locations: OA object input/output stream wrappers, socket input stream classes.
  Known related CODEX findings: partial-frame/read issues reviewed.
  Suggested unit tests: testPartialObjectReadThrowsOrDisconnects(), testCompleteObjectReadSucceeds()
  Spec target section: Communication Runtime / Stream Read Semantics

  COMM-STREAM-002 — Write Completion Means Bytes Are Accepted By Transport Layer
  Contract statement: A successful write/flush must mean the bytes were accepted by the underlying stream/queue or
  failure was reported.
  Rationale: Callers use successful write as message publication to the transport.
  Source locations: output stream wrappers, multiplexer send queues, socket write loops.
  Known related CODEX findings: false-success send risks reviewed.
  Suggested unit tests: testWriteFailureVisibleToCaller(), testFlushFailureMarksConnectionFailed()
  Spec target section: Communication Runtime / Stream Write Semantics

  COMM-STREAM-003 — Skip Must Respect Requested Byte Count
  Contract statement: Stream skip/discard logic must continue until the requested number of bytes is skipped/read or
  the connection fails.
  Rationale: Java InputStream.skip is not guaranteed to skip all bytes in one call; assuming it does corrupts
  framing.
  Source locations: frame discard and skip logic.
  Known related CODEX findings: skip/read/discard issues found in comm scans.
  Suggested unit tests: testSkipLoopHandlesShortSkip(), testSkipZeroProgressFallsBackOrFails()
  Spec target section: Communication Runtime / Stream Skip Semantics

  4. Blocking / Wakeup / Threading Contracts

  COMM-BLOCK-001 — Blocking Reads Must Wake On Data, Close, Or Failure
  Contract statement: Any thread blocked waiting for input must wake when data arrives, the virtual/physical
  connection closes, or failure occurs.
  Rationale: Remote/sync threads must not stall permanently.
  Source locations: virtual input stream queues, multiplexer reader thread, wait/notify paths.
  Known related CODEX findings: blocked reader issues reviewed.
  Suggested unit tests: testBlockedReadWakesOnData(), testBlockedReadWakesOnClose(), testBlockedReadWakesOnFailure()
  Spec target section: Communication Runtime / Blocking Read Semantics

  COMM-BLOCK-002 — Blocking Writes Must Wake On Capacity, Close, Or Failure
  Contract statement: Any thread blocked waiting to write/enqueue must wake when capacity is available, the
  connection closes, or failure occurs.
  Rationale: Writer stalls can deadlock remote/sync/replication.
  Source locations: output queues, socket writer threads, multiplexer send queues.
  Known related CODEX findings: blocked writer/stall risks reviewed.
  Suggested unit tests: testBlockedWriteWakesOnCapacity(), testBlockedWriteWakesOnClose()
  Spec target section: Communication Runtime / Blocking Write Semantics

  COMM-THREAD-001 — Reader/Writer Threads Must Have Single Ownership Of Physical Socket Streams
  Contract statement: The physical socket input/output streams must be read/written by their designated comm threads
  or synchronized ownership paths only.
  Rationale: Concurrent unsynchronized physical reads/writes break frame ordering.
  Source locations: socket client/server classes, multiplexer reader/writer loops.
  Known related CODEX findings: read/write ordering risks reviewed.
  Suggested unit tests: testSingleReaderOwnsPhysicalInputStream(), testConcurrentVirtualWritesSerializeThroughMux()
  Spec target section: Communication Runtime / Thread Ownership

  5. Connection Lifecycle Contracts

  COMM-CONN-001 — Connection State Transitions Are Monotonic Per Lifecycle
  Contract statement: A connection must move through open, active, closing, closed/failed states without reverting
  to active after closed unless explicitly reconnected as a new lifecycle.
  Rationale: Prevents stale socket reuse and false send/receive success.
  Source locations: socket client/server lifecycle classes, multiplexer close/reconnect paths.
  Known related CODEX findings: reconnect/disconnect risks reviewed.
  Suggested unit tests: testClosedConnectionDoesNotAcceptSend(), testReconnectCreatesNewLifecycleState()
  Spec target section: Communication Runtime / Connection Lifecycle

  COMM-CONN-002 — Disconnect Must Be Observed By All Dependent Layers
  Contract statement: Physical disconnect must propagate to virtual sockets, blocking streams, remote call waiters,
  and sync/replication queues.
  Rationale: Higher layers need visible failure to retry/reconcile.
  Source locations: multiplexer, virtual sockets, remote invocation transport.
  Known related CODEX findings: shutdown/blocked-thread issues reviewed.
  Suggested unit tests: testPhysicalDisconnectClosesVirtualSockets(), testDisconnectWakesRemoteCallWaiter()
  Spec target section: Communication Runtime / Disconnect Propagation

  6. Shutdown / Resource Cleanup Contracts

  COMM-RESOURCE-001 — Close Releases Socket And Stream Resources
  Contract statement: Closing a comm connection must close underlying sockets, input/output streams, and owned
  worker threads/queues.
  Rationale: Resource leaks can stall long-running OA servers and clients.
  Source locations: socket close paths, multiplexer shutdown paths, stream close methods.
  Known related CODEX findings: resource cleanup issues reviewed.
  Suggested unit tests: testCloseClosesPhysicalSocket(), testCloseReleasesVirtualSocketResources()
  Spec target section: Communication Runtime / Resource Cleanup

  COMM-RESOURCE-002 — Close Is Idempotent
  Contract statement: Calling close/shutdown multiple times must not throw unexpected errors or resurrect connection
  state.
  Rationale: Error paths may call cleanup from multiple layers.
  Source locations: close/shutdown methods across comm classes.
  Known related CODEX findings: cleanup path risks reviewed.
  Suggested unit tests: testCloseIsIdempotent(), testConcurrentCloseIsSafe()
  Spec target section: Communication Runtime / Idempotent Shutdown

  COMM-RESOURCE-003 — Failed Frame/Stream Processing Cleans Up Or Fails Connection
  Contract statement: If frame processing fails mid-frame, the connection must be marked failed/closed unless
  recovery can preserve exact frame alignment.
  Rationale: Continuing after unknown stream position corrupts all later messages.
  Source locations: multiplexer reader loop, frame parsing, object stream read paths.
  Known related CODEX findings: partial-frame corruption findings reviewed.
  Suggested unit tests: testMidFrameReadFailureClosesConnection(),
  testFrameParseFailureDoesNotAllowNextFrameAsValid()
  Spec target section: Communication Runtime / Failure Cleanup

  7. SSL Contracts

  COMM-SSL-001 — SSL Streams Must Preserve Same Framing Contract As Plain Streams
  Contract statement: SSL input/output streams must expose the same complete-byte stream semantics required by the
  multiplexer and object streams.
  Rationale: Secure transport must not change OA frame/message semantics.
  Source locations: SSL socket/stream classes, multiplexer stream integration.
  Known related CODEX findings: none observed.
  Suggested unit tests: testSslFrameRoundTripPreservesBoundary(), testSslBackToBackFramesPreserveOrder()
  Spec target section: Communication Runtime / SSL Stream Semantics

  COMM-SSL-002 — SSL Handshake Failure Must Fail Connection Clearly
  Contract statement: SSL handshake/setup failure must not leave the connection appearing open or usable.
  Rationale: Remote/sync callers must not publish messages into a failed secure channel.
  Source locations: SSL socket creation/initialization paths.
  Known related CODEX findings: none observed.
  Suggested unit tests: testSslHandshakeFailureMarksConnectionFailed(), testSslHandshakeFailureWakesWaiters()
  Spec target section: Communication Runtime / SSL Lifecycle

  8. Remote / Sync / Replication Ordering Contracts

  COMM-ORDER-001 — Remote/Sync/Replication Messages Preserve Send Order Per Channel
  Contract statement: Messages sent on a logical comm channel must be delivered to remote/sync/replication consumers
  in send order.
  Rationale: Object graph replication and event replay require deterministic ordering.
  Source locations: multiplexer queueing, remote transport, sync message sender.
  Known related CODEX findings: ordering issues were a primary comm review focus.
  Suggested unit tests: testRemoteMessagesDeliveredInSendOrder(), testReplicationMessagesDeliveredInSendOrder()
  Spec target section: Communication Runtime / Message Ordering

  COMM-ORDER-002 — Request/Response Matching Must Be One-To-One
  Contract statement: A response must be delivered to the requester/call associated with its request and must not be
  consumed by another caller.
  Rationale: Remote method calls depend on exact request/response pairing.
  Source locations: remote call transport, request id handling, response wait queues.
  Known related CODEX findings: request/response matching risks reviewed.
  Suggested unit tests: testConcurrentRemoteCallsReceiveCorrectResponses(),
  testLateResponseDoesNotSatisfyWrongRequest()
  Spec target section: Communication Runtime / Request Response Semantics

  COMM-ORDER-003 — Disconnect Must Not Reorder Already Accepted Messages
  Contract statement: Messages accepted for ordered delivery before disconnect must either be delivered in order or
  the failure must be visible; they must not be silently reordered across reconnect.
  Rationale: Sync/replication cannot tolerate hidden reorder across reconnect.
  Source locations: reconnect/disconnect handling, send queues, multiplexer lifecycle.
  Known related CODEX findings: reconnect/disconnect ordering risks reviewed.
  Suggested unit tests: testDisconnectDuringSendDoesNotSilentlyReorderMessages(),
  testReconnectStartsNewOrderingEpoch()
  Spec target section: Communication Runtime / Reconnect Ordering

  9. Failure / Retry / Disconnect Contracts

  COMM-FAILURE-001 — Partial Send Failure Is Not Success
  Contract statement: If a frame/message is only partially sent and cannot complete, the send operation must fail
  visibly or close the connection.
  Rationale: Partial message publication is indistinguishable from corruption to the receiver.
  Source locations: socket output stream, multiplexer frame writer, send queue handling.
  Known related CODEX findings: partial send/false-success risks reviewed.
  Suggested unit tests: testPartialSendFailureVisibleToCaller(), testPartialSendFailureClosesConnection()
  Spec target section: Communication Runtime / Send Failure

  COMM-FAILURE-002 — Retry Occurs At A Defined Layer
  Contract statement: The comm layer may reconnect/retry only according to its explicit contract; otherwise higher
  remote/sync layers must observe failure and decide retry.
  Rationale: Hidden retry can duplicate or reorder messages.
  Source locations: reconnect logic, remote transport, sync/replication callers.
  Known related CODEX findings: retry/reconnect risks reviewed.
  Suggested unit tests: testCommFailureDoesNotSilentlyDuplicateMessage(), testReconnectRetryPreservesOrderingEpoch()
  Spec target section: Communication Runtime / Retry Semantics

  COMM-FAILURE-003 — Internal Contract Violations Fail Fast Enough To Protect Ordering
  Contract statement: If a normal OA-controlled path violates expected frame/connection state, the comm layer must
  fail the connection or operation rather than continue with unknown ordering.
  Rationale: Protects remote/sync/replication from silent divergence.
  Source locations: multiplexer frame parser, virtual socket dispatch, remote call transport.
  Known related CODEX findings: internal malformed/public hostile cases classified out of scope unless normal OA
  usage can produce them.
  Suggested unit tests: testUnexpectedFrameStateClosesConnection(), testInvalidVirtualSocketStateFailsOperation()
  Spec target section: Communication Runtime / Internal Contract Failure

  10. Test Coverage Matrix

  Runtime/internal transport:

  - testValidOAMessageRoundTrip
  - testInternalProtocolAssumptionsDocumented
  - testSendFailurePropagatesToCaller
  - testReceiveFailureClosesOrMarksConnectionFailed

  Multiplexer/frame/virtual socket:

  - testMultiplexerPreservesSingleFrameBoundary
  - testBackToBackFramesReadSeparately
  - testVirtualSocketPreservesWriteOrder
  - testInterleavedVirtualSocketsPreservePerSocketOrder
  - testDiscardFrameConsumesEntirePayload
  - testPartialDiscardFailureClosesConnection
  - testMultiplexerCloseWakesBlockedVirtualReader
  - testMultiplexerCloseWakesBlockedVirtualWriter

  Streams:

  - testPartialObjectReadThrowsOrDisconnects
  - testCompleteObjectReadSucceeds
  - testWriteFailureVisibleToCaller
  - testFlushFailureMarksConnectionFailed
  - testSkipLoopHandlesShortSkip
  - testSkipZeroProgressFallsBackOrFails

  Blocking/threading:

  - testBlockedReadWakesOnData
  - testBlockedReadWakesOnClose
  - testBlockedReadWakesOnFailure
  - testBlockedWriteWakesOnCapacity
  - testBlockedWriteWakesOnClose
  - testSingleReaderOwnsPhysicalInputStream
  - testConcurrentVirtualWritesSerializeThroughMux

  Connection/shutdown:

  - testClosedConnectionDoesNotAcceptSend
  - testReconnectCreatesNewLifecycleState
  - testPhysicalDisconnectClosesVirtualSockets
  - testDisconnectWakesRemoteCallWaiter
  - testCloseClosesPhysicalSocket
  - testCloseReleasesVirtualSocketResources
  - testCloseIsIdempotent
  - testConcurrentCloseIsSafe
  - testMidFrameReadFailureClosesConnection

  SSL:

  - testSslFrameRoundTripPreservesBoundary
  - testSslBackToBackFramesPreserveOrder
  - testSslHandshakeFailureMarksConnectionFailed
  - testSslHandshakeFailureWakesWaiters

  Remote/sync/replication ordering:

  - testRemoteMessagesDeliveredInSendOrder
  - testReplicationMessagesDeliveredInSendOrder
  - testConcurrentRemoteCallsReceiveCorrectResponses
  - testLateResponseDoesNotSatisfyWrongRequest
  - testDisconnectDuringSendDoesNotSilentlyReorderMessages
  - testReconnectStartsNewOrderingEpoch

  Failure/retry:

  - testPartialSendFailureVisibleToCaller
  - testPartialSendFailureClosesConnection
  - testCommFailureDoesNotSilentlyDuplicateMessage
  - testReconnectRetryPreservesOrderingEpoch
  - testUnexpectedFrameStateClosesConnection
  - testInvalidVirtualSocketStateFailsOperation


*/








