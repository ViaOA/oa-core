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
 * Provides the client- and server-side infrastructure for OA’s socket
 * multiplexer, a communication layer that allows many independent virtual
 * TCP connections to operate over a single physical network socket.
 *
 * <p>
 * The multiplexer enables distributed OA applications to maintain multiple
 * logical channels—each behaving like a normal {@link java.net.Socket} or
 * {@link java.net.ServerSocket}—while sharing one underlying TCP connection.
 * This reduces connection overhead, simplifies firewall/proxy traversal, and
 * centralizes routing and throttling through a compact and efficient I/O
 * controller.
 * </p>
 *
 * <h2>Key Concepts</h2>
 *
 * <ul>
 *   <li><b>Virtual Sockets</b> –
 *       Represented by {@code VirtualSocket} and {@code VirtualServerSocket}.
 *       These behave like ordinary sockets but are mapped onto logical
 *       channels managed by the multiplexer.</li>
 *
 *   <li><b>Single Physical Connection</b> –
 *       The {@link com.viaoa.comm.multiplexer.OAMultiplexerClient} and
 *       {@link com.viaoa.comm.multiplexer.OAMultiplexerServer} communicate
 *       using one real TCP socket.</li>
 *
 *   <li><b>Multiplexer Controllers</b> –
 *       I/O controllers such as
 *       {@code MultiplexerSocketController},
 *       {@code MultiplexerInputStreamController},
 *       and {@code MultiplexerOutputStreamController} handle framing, routing,
 *       buffering, fairness, and channel lifecycle management.</li>
 *
 *   <li><b>Named Virtual Endpoints</b> –
 *       Clients request connections by name
 *       (e.g., {@code "OrderService"}), and the server exposes matching
 *       {@code VirtualServerSocket} instances. This decouples logical services
 *       from physical ports.</li>
 *
 *   <li><b>High Throughput with Optional Throttling</b> –
 *       Both client and server support byte-level throttling to limit bursts
 *       and balance traffic across channels.</li>
 *
 *   <li><b>Connection Health</b> –
 *       Clients support optional keep-alive signaling for long-running
 *       distributed sessions.</li>
 * </ul>
 *
 * <h2>Typical Usage</h2>
 *
 * <h3>Client</h3>
 * <pre>
 * OAMultiplexerClient client = new OAMultiplexerClient("host", 9000);
 * client.start();
 * VirtualSocket vs = client.createSocket("MyService");
 * InputStream in  = vs.getInputStream();
 * OutputStream out = vs.getOutputStream();
 * </pre>
 *
 * <h3>Server</h3>
 * <pre>
 * OAMultiplexerServer server = new OAMultiplexerServer(9000);
 * server.start();
 * VirtualServerSocket vss = server.createServerSocket("MyService");
 * VirtualSocket vs = vss.accept();   // behaves like ServerSocket.accept()
 * </pre>
 *
 * <h2>Design Goals</h2>
 *
 * <ul>
 *   <li><b>Transparency</b> – Virtual sockets follow the same I/O patterns as
 *       traditional Java sockets.</li>
 *   <li><b>Performance</b> – Centralized I/O, minimal contention, and compact
 *       packet framing.</li>
 *   <li><b>Reliability</b> – Clean lifecycle, ordered delivery within channels,
 *       and controlled shutdown semantics.</li>
 *   <li><b>Scalability</b> – Many logical connections with only one real network
 *       connection per client.</li>
 * </ul>
 *
 * <p>
 * Together, these classes form a flexible, efficient communication layer used
 * throughout OA’s distributed messaging, event propagation, and multi-module
 * service communication.
 * </p>
 */
package com.viaoa.comm.multiplexer;

/* CODEX Invariants

MULTIPLEX-LIFECYCLE-001 — Physical Connection Lifecycle
Contract statement:
A multiplexer client or server physical connection must move through a deterministic lifecycle: configured,
starting, started/connected, stopping/closing, closed, or failed, and public lifecycle methods must honor that
state.
Rationale:
The physical connection is the transport authority for all logical channels; stale or ambiguous lifecycle state can
cause false-connected status, duplicate startup, leaked sockets, or lost logical channels.
Source scope:
OAMultiplexerClient.start(), close(), isConnected(), getSocket(); OAMultiplexerServer.start(), stop(),
stopServerSocket(), isStarted(); MultiplexerSocketController.start(), close(), isClosed().
Related CODEX findings:
none observed.
Suggested unit tests:
testClientStartConnectsOnce, testClientCloseTransitionsToDisconnected, testServerStartStopLifecycleIsDeterministic,
testServerStopServerSocketStopsAcceptingOnly.
Spec target section:
Multiplex Transport / Physical Connection Lifecycle

MULTIPLEX-LIFECYCLE-002 — Idempotent Close And Stop
Contract statement:
close(), stop(), and stopServerSocket() must be safe to call more than once and must not report success while
leaving owned sockets, controllers, queues, or threads active unexpectedly.
Rationale:
Transport cleanup often runs from finally blocks, socket exception handlers, shutdown hooks, and remote failure
paths.
Source scope:
OAMultiplexerClient.close(); OAMultiplexerServer.stop(), stopServerSocket(); MultiplexerSocketController.close();
MultiplexerServerSocketController.close(), stopAccepting(); VirtualSocket.close(); VirtualServerSocket.close().
Related CODEX findings:
none observed.
Suggested unit tests:
testClientCloseIsIdempotent, testServerStopIsIdempotent, testVirtualSocketCloseIsIdempotent.
Spec target section:
Multiplex Transport / Cleanup Semantics

MULTIPLEX-CONNECT-001 — Handshake Establishes Transport Validity
Contract statement:
A physical socket must not be treated as a valid multiplexer connection until the expected client/server handshake
and controller startup have completed.
Rationale:
Logical channels depend on protocol framing and connection identity; accepting invalid sockets as valid corrupts
transport state.
Source scope:
OAMultiplexerClient.start(); OAMultiplexerServer.start(); MultiplexerSocketController.verifyServerSideHandshake();
MultiplexerServerSocketController.onAcceptRealClientConnection(); getInvalidConnectionMessage().
Related CODEX findings:
none observed.
Suggested unit tests:
testInvalidHandshakeRejected, testClientStartFailsWhenControllerClosed,
testInvalidConnectionMessageReturnedByContract.
Spec target section:
Multiplex Transport / Handshake Semantics

MULTIPLEX-CHANNEL-001 — Named Virtual Endpoint Resolution
Contract statement:
A client-created virtual socket must resolve to the server virtual endpoint with the same name, and missing or
invalid names must fail visibly rather than silently routing to the wrong endpoint.
Rationale:
Named virtual sockets are the logical service boundary over one physical connection; wrong routing can cross service
streams.
Source scope:
OAMultiplexerClient.createSocket(String); OAMultiplexerServer.createServerSocket(String);
MultiplexerServerSocketController.getServerSocket(String); VirtualServerSocket.getName();
VirtualSocket.getServerSocketName().
Related CODEX findings:
none observed.
Suggested unit tests:
testCreateSocketConnectsToMatchingVirtualServerSocket, testUnknownServerSocketNameFailsVisibly,
testVirtualServerSocketNameIsStable.
Spec target section:
Multiplex Transport / Virtual Endpoint Routing

MULTIPLEX-CHANNEL-002 — Logical Channel Identity
Contract statement:
Each virtual socket/channel must have a stable connection id, socket id, and server-socket name for its lifetime,
and frame routing must use that identity without collision between live channels.
Rationale:
Channel identity is the routing key that isolates logical streams on a shared physical socket.
Source scope:
VirtualSocket.getConnectionId(), getId(), getServerSocketName(); MultiplexerSocketController.createSocket(...),
getMultiplexerSockets(), getLiveSocketCount(); OAMultiplexerClient.getConnectionId().
Related CODEX findings:
none observed.
Suggested unit tests:
testVirtualSocketIdsAreUniquePerConnection, testChannelIdentityStableUntilClose,
testFramesRouteToCorrectVirtualSocket.
Spec target section:
Multiplex Transport / Channel Identity

MULTIPLEX-CHANNEL-003 — Logical Channel Isolation
Contract statement:
Bytes written to one virtual socket must be delivered only to that virtual socket’s peer and must not interleave
into, block indefinitely through, or corrupt another logical channel.
Rationale:
Multiplexing correctness depends on logical channels behaving like independent sockets over one physical connection.
Source scope:
VirtualSocket.getInputStream(), getOutputStream(), read(...), write(...); MultiplexerInputStreamController;
MultiplexerOutputStreamController; MultiplexerSocketController.
Related CODEX findings:
none observed.
Suggested unit tests:
testTwoVirtualSocketsDoNotCrossDeliverBytes, testConcurrentChannelWritesRemainIsolated,
testClosingOneVirtualSocketDoesNotCloseOtherLiveChannels.
Spec target section:
Multiplex Transport / Channel Isolation

MULTIPLEX-FRAME-001 — Message Framing Preserves Boundaries
Contract statement:
The transport protocol must preserve frame boundaries by pairing each payload with the intended virtual socket id
and payload length, and corrupted or incomplete frames must fail visibly.
Rationale:
Frame corruption can route bytes to the wrong logical stream or report partial transport success to remote/sync
layers.
Source scope:
MultiplexerInputStreamController read/dispatch loop and processCommand(...); MultiplexerOutputStreamController
write/sendCommand(...); MultiplexerSocketController channel routing.
Related CODEX findings:
none observed.
Suggested unit tests:
testFrameHeaderRoutesPayloadToCorrectSocket, testTruncatedFrameClosesOrFailsTransportVisibly,
testInvalidFrameLengthRejected.
Spec target section:
Multiplex Transport / Frame Integrity

MULTIPLEX-ORDER-001 — Per-Channel Ordered Delivery
Contract statement:
Bytes and frames for a single virtual socket must be delivered to its peer in the same order they were written on
that virtual socket.
Rationale:
Remote calls, sync messages, and replication payloads require stream ordering within each logical channel.
Source scope:
VirtualSocket.write(...), read(...); MultiplexerOutputStreamController; MultiplexerInputStreamController;
MultiplexerSocketController.
Related CODEX findings:
none observed.
Suggested unit tests:
testSingleChannelPreservesWriteOrder, testLargeChunkedWritePreservesByteOrder,
testConcurrentOtherChannelTrafficDoesNotReorderChannel.
Spec target section:
Multiplex Transport / Ordered Delivery

MULTIPLEX-ORDER-002 — Cross-Channel Ordering Boundary
Contract statement:
The package must explicitly define that ordering is guaranteed per logical channel, while ordering between
independent logical channels is either unspecified or governed by documented controller scheduling rules.
Rationale:
Higher-level remote, sync, and replication code must not assume global cross-channel order unless the transport
promises it.
Source scope:
OAMultiplexerClient.createSocket(...); OAMultiplexerServer.createServerSocket(...);
MultiplexerOutputStreamController scheduling; MultiplexerInputStreamController dispatch.
Related CODEX findings:
none observed.
Suggested unit tests:
testPerChannelOrderingDoesNotImplyGlobalOrdering, testCrossChannelOrderingContractDocumentedByBehavior.
Spec target section:
Multiplex Transport / Ordering Boundary

MULTIPLEX-IO-001 — Partial Read And Write Visibility
Contract statement:
A read or write operation must not silently report success for bytes not delivered, accepted, buffered, or failed
according to the virtual socket stream contract.
Rationale:
Partial transport progress affects remote calls, serialization, sync, and replication; false-success creates
corrupted protocol state.
Source scope:
VirtualSocket.read(...), write(...); MultiplexerInputStreamController; MultiplexerOutputStreamController;
OAMultiplexerClient.getReadCount/getReadSize/getWriteCount/getWriteSize; OAMultiplexerServer metrics.
Related CODEX findings:
none observed.
Suggested unit tests:
testVirtualSocketWriteFailureIsVisible, testVirtualSocketReadEOFContract,
testPartialPhysicalWriteDoesNotReportCompleteLogicalWrite.
Spec target section:
Multiplex Transport / I/O Completion

MULTIPLEX-FAIL-001 — Transport Failure Visibility
Contract statement:
Socket exceptions, EOF, invalid frames, handshake failure, timeout, close during I/O, and controller failures must
be observable through exceptions, close state, callbacks, or diagnostics, and must not appear as successful sends or
receives.
Rationale:
Remote, sync, and replication layers must distinguish successful transport delivery from broken communication.
Source scope:
OAMultiplexerClient.onSocketException(...), onClose(...), isConnected(); OAMultiplexerServer.onClientConnect(...),
onClientDisconnect(...); MultiplexerSocketController.onSocketException(...), close(...); VirtualSocket read/write/
close.
Related CODEX findings:
none observed.
Suggested unit tests:
testPhysicalSocketExceptionInvokesClientCallback, testClientCloseCallbackReceivesErrorFlag,
testReadAfterTransportCloseFailsByContract.
Spec target section:
Multiplex Transport / Failure Visibility

MULTIPLEX-CLOSE-001 — Logical Close Semantics
Contract statement:
Closing a virtual socket must notify the peer according to the close-command contract, release local channel
resources, unblock waiting reads/writes, and preserve other channels on the same physical connection unless the
physical connection is closing.
Rationale:
Virtual sockets must behave like independent sockets while sharing one real socket.
Source scope:
VirtualSocket.close(boolean); MultiplexerSocketController.closeSocket(...);
MultiplexerInputStreamController.closeSocket(...); MultiplexerOutputStreamController.sendCommand(...).
Related CODEX findings:
none observed.
Suggested unit tests:
testVirtualSocketCloseNotifiesPeer, testVirtualSocketCloseUnblocksPeerRead,
testVirtualSocketCloseDoesNotCloseOtherChannels.
Spec target section:
Multiplex Transport / Logical Close

MULTIPLEX-SERVER-001 — Server Accept Lifecycle
Contract statement:
A started multiplexer server must accept real connections and route virtual connection requests to registered
VirtualServerSockets; stopping the real server socket must prevent new real connections without corrupting existing
managed connections.
Rationale:
Server lifecycle separates listening from existing session management.
Source scope:
OAMultiplexerServer.start(), stopServerSocket(), stop(), isStarted(); MultiplexerServerSocketController.start(...),
stopAccepting(), getServerSocket(...); VirtualServerSocket.accept().
Related CODEX findings:
none observed.
Suggested unit tests:
testStopServerSocketStopsNewConnectionsOnly, testExistingConnectionsRemainAfterStopServerSocketByContract,
testStopClosesControllerAndConnections.
Spec target section:
Multiplex Transport / Server Accept Lifecycle

MULTIPLEX-BACKPRESSURE-001 — Throttle And Backpressure Semantics
Contract statement:
Configured throttle limits must apply deterministically to physical output throughput without corrupting frame
boundaries, starving channels indefinitely, or reporting delivery success after throttled failure.
Rationale:
Backpressure protects runtime transport without changing message semantics.
Source scope:
OAMultiplexerClient.setThrottleLimit/getThrottleLimit; OAMultiplexerServer.setThrottleLimit/getThrottleLimit;
MultiplexerOutputStreamController.setThrottleLimit/getThrottleLimit/getMaxWriteLength.
Related CODEX findings:
none observed.
Suggested unit tests:
testClientThrottleLimitAppliedToController, testServerThrottleLimitAppliedToConnections,
testThrottledWritesPreserveFrameBoundaries.
Spec target section:
Multiplex Transport / Backpressure

MULTIPLEX-SLOW-001 — Slow Consumer Boundary
Contract statement:
A slow or blocked logical channel must not permanently starve unrelated channels, and any timeout/disconnect policy
for slow consumers must be visible through the close/failure contract.
Rationale:
Multiple OA services share the same physical connection; one slow stream must not silently collapse sync, remote, or
replication traffic on other streams.
Source scope:
MultiplexerInputStreamController; MultiplexerOutputStreamController; VirtualSocket timeout methods;
MultiplexerServerSocketController.timeoutConnections().
Related CODEX findings:
none observed.
Suggested unit tests:
testSlowConsumerDoesNotStarveOtherChannel, testVirtualSocketTimeoutFailsVisibly,
testSlowConsumerCloseDoesNotCorruptOtherChannels.
Spec target section:
Multiplex Transport / Slow Consumer Semantics

MULTIPLEX-KEEPALIVE-001 — Keep-Alive Health Semantics
Contract statement:
Keep-alive pings must be scheduled only while the client is connected and enabled, must stop after close or
disablement, and ping failures must be visible through the transport failure/close contract.
Rationale:
Keep-alive exists to detect unhealthy long-lived distributed sessions without leaking background threads or hiding
broken connections.
Source scope:
OAMultiplexerClient.setKeepAlive(...), getKeepAlive(), runKeepAliveThread(), pingServer(), close(), isConnected();
MultiplexerOutputStreamController.sendPingCommand().
Related CODEX findings:
none observed.
Suggested unit tests:
testKeepAliveThreadStartsOnlyWhenEnabledAndConnected, testKeepAliveStopsAfterClose,
testPingFailureTransitionsToObservableFailure.
Spec target section:
Multiplex Transport / Connection Health

MULTIPLEX-METRICS-001 — Transport Metrics Reflect Controller State
Contract statement:
Read/write counts, byte counts, created socket/connection counts, and live socket/connection counts must reflect
controller-observed state and must not claim activity after uninitialized or closed controllers except by documented
zero/default behavior.
Rationale:
Runtime diagnostics and production monitoring depend on trustworthy transport counters.
Source scope:
OAMultiplexerClient.getCreatedSocketCount(), getLiveSocketCount(), getReadCount(), getReadSize(), getWriteCount(),
getWriteSize(); OAMultiplexerServer metric methods; controller metric methods.
Related CODEX findings:
none observed.
Suggested unit tests:
testClientMetricsZeroBeforeStart, testClientMetricsIncreaseAfterVirtualSocketTraffic,
testServerLiveConnectionCountUpdatesOnDisconnect.
Spec target section:
Multiplex Transport / Metrics

MULTIPLEX-CONCURRENT-001 — Concurrent Send Receive Correctness
Contract statement:
Concurrent reads, writes, virtual socket creation, virtual socket close, and controller close must be synchronized
or staged so that channel maps, frame routing, and stream state remain consistent.
Rationale:
Multiplexed transport is inherently concurrent; races can lose messages, route to wrong channels, or leak resources.
Source scope:
OAMultiplexerClient.createSocket/close; OAMultiplexerServer.createServerSocket/stop; MultiplexerSocketController; M
ultiplexerInputStreamController; MultiplexerOutputStreamController; VirtualSocket.
Related CODEX findings:
none observed.
Suggested unit tests:
testConcurrentVirtualSocketCreateAndCloseDoesNotCorruptCounts, testConcurrentWritesAcrossChannelsPreserveData,
testCloseDuringWriteFailsVisibly.
Spec target section:
Multiplex Transport / Concurrency

MULTIPLEX-RESOURCE-001 — Resource Ownership And Cleanup
Contract statement:
Sockets, server sockets, virtual sockets, controllers, input/output streams, accept threads, reader threads, keep-
alive threads, queues, and channel registrations owned by the multiplexer must be released on normal close and
failure close.
Rationale:
Long-lived OA distributed systems cannot leak transport threads or channel resources under reconnect, failure, or
shutdown cycles.
Source scope:
OAMultiplexerClient.close(); OAMultiplexerServer.stop(); MultiplexerServerSocketController.close();
MultiplexerSocketController.close(); VirtualSocket.close(); VirtualServerSocket.close().
Related CODEX findings:
none observed.
Suggested unit tests:
testClientCloseReleasesSocketAndControllerResources, testServerStopReleasesServerSocketAndControllers,
testRepeatedStartStopDoesNotLeakLiveConnections.
Spec target section:
Multiplex Transport / Resource Cleanup

MULTIPLEX-RECONNECT-001 — Reconnect Boundary
Contract statement:
A new physical connection after failure or close must not reuse stale logical channel ids, closed virtual socket
state, queues, or controller state as if they belonged to the new connection.
Rationale:
Reconnects are common in distributed OA systems; stale channel state can corrupt remote, sync, or replication
sessions.
Source scope:
OAMultiplexerClient.start(), close(), getConnectionId(), createSocket(...); OAMultiplexerServer connection id
handling; MultiplexerSocketController channel maps.
Related CODEX findings:
none observed.
Suggested unit tests:
testReconnectGetsFreshConnectionState, testOldVirtualSocketCannotWriteAfterReconnect,
testReconnectDoesNotReuseClosedChannelQueues.
Spec target section:
Multiplex Transport / Reconnect Semantics

MULTIPLEX-ACK-001 — Transport Delivery Versus Semantic Acknowledgment
Contract statement:
A successful virtual socket write means bytes were accepted by the transport contract, not that a remote method,
sync event, replication event, or business transaction committed unless a higher-level acknowledgment says so.
Rationale:
Transport-level delivery must not be confused with OA runtime semantic success.
Source scope:
VirtualSocket.write(...); MultiplexerOutputStreamController; integration with remote, sync, replication,
transaction, and graph runtime packages.
Related CODEX findings:
none observed.
Suggested unit tests:
testWriteSuccessDoesNotImplyRemoteSemanticAck, testTransportFailureDuringHigherLevelOperationIsVisible.
Spec target section:
Multiplex Transport / Delivery Boundary

MULTIPLEX-INTEGRATION-001 — Remote Sync Replication Transport Compatibility
Contract statement:
Multiplexer transport must preserve the ordering, isolation, failure visibility, and stream semantics required by
remote invocation, sync, replication, serialization, and runtime graph communication.
Rationale:
OA distributed graph behavior depends on transport correctness but owns higher-level semantic commit, replay, and
merge rules.
Source scope:
OAMultiplexerClient; OAMultiplexerServer; VirtualSocket; VirtualServerSocket; MultiplexerSocketController; remote/
sync/replication integration.
Related CODEX findings:
none observed.
Suggested unit tests:
testRemotePayloadRoundTripsOverVirtualSocket, testSyncMessagesPreservePerChannelOrder,
testReplicationTransportFailureDoesNotAppearCommitted.
Spec target section:
Multiplex Transport / Cross-Package Integration

MULTIPLEX-NAMING-001 — Package Naming Authority
Contract statement:
The package authority for this transport is the actual source package com.viaoa.comm.multiplexer; references to
com.viaoa.comm.multiplex must resolve to documentation/request terminology only unless an explicit alias package is
introduced.
Rationale:
Invariant and package-info coverage must bind to the package that exists in source so future tests and scans target
the correct runtime code.
Source scope:
src/main/java/com/viaoa/comm/multiplexer/package-info.java; OAMultiplexerClient; OAMultiplexerServer;
com.viaoa.comm.multiplexer.io.
Related CODEX findings:
none observed.
Suggested unit tests:
testPackageInvariantScanTargetsCommMultiplexer, testNoRuntimePackageAliasAssumedWithoutSourcePackage.
Spec target section:
Multiplex Transport / Package Authority

*/




