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
 * <p>
 */
package com.viaoa.replication;

/* CODEX Invariants

ID: REPL-ORDER-001
  Contract statement: Replication records must be captured, transmitted, applied, and replayed in deterministic
  sequence order.
  Rationale: Store-Corp convergence depends on every site seeing the same ordered mutation stream.
  Source locations: OAReplicationBase.processQueue, OAReplicationClient.runSendSyncMessagesToMaster,
  OAReplicationClient.onNewMessageFromMaster, OAReplicationMaster.ReplClientSession.process.
  Related CODEX findings: sequence-gap detection for master/client replay; duplicate master message filtering.
  Suggested unit tests: testClientRejectsOrResyncsOnMasterSequenceGap, testMasterRejectsOrResyncsOnClientSequenceGap.
  Spec target section: Replication Runtime / Ordering Semantics.

  ID: REPL-TLOG-001
  Contract statement: A replication TLog record must represent one complete, independent, replayable change.
  Rationale: Replay must not depend on mutable live object state or Java serialization back-references from previous
  records.
  Source locations: OAReplTLog, OAReplicationClient.writeTLog, OAReplicationMaster.writeTLog,
  OAReplicationClient.onNewSyncMessage, OAReplicationMaster.onNewSyncMessage.
  Related CODEX findings: ObjectOutputStream handle reuse; mutable ri.args payload drift.
  Suggested unit tests: testTLogSerializesUpdatedObjectStateAcrossMultipleRecords,
  testQueuedReplicationMessagePayloadDoesNotDriftAfterSourceObjectMutates.
  Spec target section: Replication Runtime / Transaction Log Semantics.

  ID: REPL-DURABLE-001
  Contract statement: A sequence, checkpoint, or delivery acknowledgment must not advance before the corresponding
  replication state is durably logged or safely applied.
  Rationale: Premature durability claims cause lost changes after crash/reconnect.
  Source locations: OAReplicationMaster.remoteMaster.processMessage, OAReplicationMaster.onNewSyncMessage,
  OAReplicationClient.onNewSyncMessage, OAReplicationClient.runSendSyncMessagesToMaster.
  Related CODEX findings: master advances received client seq before apply; master/client seq increments before TLog
  write succeeds.
  Suggested unit tests: testMasterDoesNotAckClientSeqBeforeApplyOrDurableCommit,
  testMasterSeqDoesNotAdvanceWhenTLogWriteFails, testClientSeqDoesNotAdvanceWhenLocalTLogWriteFails.
  Spec target section: Replication Runtime / Durability Semantics.

  ID: REPL-CHECKPOINT-001
  Contract statement: Replay offsets and last-sent/last-received/last-processed sequence markers must advance only
  after the matching operation is complete under its contract.
  Rationale: Incorrect offsets create silent skip, duplicate replay, or unretryable state.
  Source locations: OAReplicationClient.lastSentClientSeq, OAReplicationClient.masterSeq,
  OAReplicationMaster.ReplClientSession.lastSentMasterSeq, lastReceivedClientSeq, lastProcessedClientSeq.
  Related CODEX findings: bGotSeqFromMaster not per connection; reconnect session sequence initialization gaps.
  Suggested unit tests: testClientRechecksMasterAckSequenceAfterReconnect,
  testReconnectDoesNotReapplyAlreadyProcessedClientSeq.
  Spec target section: Replication Runtime / Checkpoint Semantics.

  ID: REPL-REPLAY-001
  Contract statement: Replay must never silently skip, duplicate, reorder, or partially apply a record while reporting
  success.
  Rationale: Replication replay is authoritative recovery behavior. Silent wrong replay corrupts downstream state.
  Source locations: OAReplicationClient.loadTLogFile, OAReplicationMaster.loadTLogFile,
  OAReplicationClient.onNewMessageFromMaster, OAReplicationMaster.ReplClientSession.process.
  Related CODEX findings: partial trailing TLog record handling; duplicate/gap sequence handling.
  Suggested unit tests: testTLogLoadTruncatesPartialTrailingRecordBeforeAppend,
  testClientIgnoresDuplicateMasterSequence.
  Spec target section: Replication Runtime / Replay Semantics.

  ID: REPL-RESYNC-001
  Contract statement: Reconnect/resync must reconcile both sides’ durable sequence state before sending or applying
  uncertain messages.
  Rationale: Disconnect boundaries are where duplicate execution and missed records most often occur.
  Source locations: OAReplClientConnection.start, RemoteMasterRegisterInterface.registerClient,
  OAReplicationClient.runSendSyncMessagesToMaster, OAReplicationMaster.ReplClientSession.
  Related CODEX findings: reconnect ack handshake not refreshed; constructor does not initialize all session replay
  markers.
  Suggested unit tests: testClientRechecksMasterAckSequenceAfterReconnect,
  testMasterSessionInitializesReplayPositionFromReconnectingClient.
  Spec target section: Replication Runtime / Reconnect and Resync.

  ID: REPL-ECHO-001
  Contract statement: Originator/source filtering must suppress only echo of the originating change and must not
  suppress legitimate downstream changes.
  Rationale: Bad source filtering either loops messages forever or drops required propagation.
  Source locations: OAReplicationClient.onNewSyncMessage, OAReplicationMaster.onNewSyncMessage,
  OAReplicationMaster.ReplClientSession.process, OAThreadLocalService.replicationSource.
  Related CODEX findings: source stored as client guid for all client TLogs; ThreadLocal source not restored to
  previous value.
  Suggested unit tests: testReplicationSourceRestoresPreviousThreadLocalValue,
  testMasterDoesNotEchoSourceClientButSendsToOthers.
  Spec target section: Replication Runtime / Originator Filtering.

  ID: REPL-IDENTITY-001
  Contract statement: Replication must preserve GUID identity, primary-key identity, and business-key identity without
  conflating them.
  Rationale: Object convergence requires the same logical object to resolve to the correct runtime/cache instance at
  every site.
  Source locations: OAReplTLog.args, sync RemoteSyncInterface payloads, cache/object application through
  RemoteSyncImpl.
  Related CODEX findings: none beyond payload snapshot concerns.
  Suggested unit tests: testReplayResolvesSameGuidToSameObject, testReplayDoesNotConflateBusinessKeyWithGuid.
  Spec target section: Replication Runtime / Object Identity.

  ID: REPL-CACHE-001
  Contract statement: Replicated object application must not create duplicate cached objects or overwrite newer/
  correct live instances incorrectly.
  Rationale: Cache drift breaks Hub membership, property changes, datasource saves, and later replay.
  Source locations: OAReplicationClient.onNewMessageFromMaster, OAReplicationMaster.ReplClientSession.process,
  RemoteSyncImpl.
  Related CODEX findings: boolean return values from remote sync apply ignored.
  Suggested unit tests: testReplicationDoesNotAdvanceSequenceWhenRemoteSyncReturnsFalse,
  testReplayUsesExistingCachedObjectForSameIdentity.
  Spec target section: Replication Runtime / Cache Consistency.

  ID: REPL-HUB-001
  Contract statement: Replicated Hub operations must preserve membership, order, move/insert/remove semantics, and
  active-object side effects according to Hub contracts.
  Rationale: Hub state is a primary OA graph structure; replicated order or membership drift creates visible
  application divergence.
  Source locations: RemoteSyncInterface.addToHub, insertInHub, removeFromHub, removeAllFromHub, moveObjectInHub, sort;
  replication apply paths.
  Related CODEX findings: boolean return values ignored; sequence gaps can skip Hub operations.
  Suggested unit tests: testReplayPreservesHubInsertOrder, testReplayDoesNotAdvanceWhenHubApplyReturnsFalse.
  Spec target section: Replication Runtime / Hub Semantics.

  ID: REPL-EVENT-001
  Contract statement: Replicated object and Hub events must be captured after committed sync events and replayed in
  event order.
  Rationale: Replication is built from sync events; event ordering must match OG runtime behavior.
  Source locations: OAReplicationBase.processQueue, OAReplicationClient.onNewSyncMessage,
  OAReplicationMaster.onNewSyncMessage.
  Related CODEX findings: queue position advances before TLog commit; queue session cleanup on failure.
  Suggested unit tests: testReplicationQueuePositionDoesNotAdvanceWhenTLogWriteFails,
  testReplicatedEventsPreserveSyncQueueOrder.
  Spec target section: Replication Runtime / Event Capture.

  ID: REPL-FAIL-001
  Contract statement: Failed write, send, apply, replay, or checkpoint operations must be visible and must not look
  successful.
  Rationale: Silent false-success prevents retry and creates hidden production divergence.
  Source locations: writeTLog, loadTLogFile, runSendSyncMessagesToMaster, onNewMessageFromMaster,
  ReplClientSession.process.
  Related CODEX findings: ignored renameTo result; failed enqueue still advances received seq; ignored boolean apply
  results.
  Suggested unit tests: testClientTLogRewriteFailureDoesNotReportSuccess,
  testMasterDoesNotAdvanceClientSeqWhenClientMessageEnqueueFails.
  Spec target section: Replication Runtime / Failure Semantics.

  ID: REPL-RETRY-001
  Contract statement: Retry after failed write/apply/send/reconnect must not corrupt ordering, duplicate state, or
  skip required messages.
  Rationale: Replication exists to support offline/reconnect and must be retry-safe.
  Source locations: OAReplicationClient.runSendSyncMessagesToMaster, OAReplicationClient.loadTLogFile,
  OAReplicationMaster.ReplClientSession.process.
  Related CODEX findings: failed client message polled and dropped; unsent local TLogs skipped on restart.
  Suggested unit tests: testMasterRetainsClientMessageWhenApplyFails, testClientRestartRequeuesUnsentLocalTLogRecords.
  Spec target section: Replication Runtime / Retry Semantics.

  ID: REPL-TL-001
  Contract statement: Replication ThreadLocal context must be restored with try/finally to the previous value, not
  blindly cleared.
  Rationale: Source/context leakage can suppress legitimate messages or echo replicated work.
  Source locations: OAReplicationClient.onNewMessageFromMaster, OAReplicationMaster.ReplClientSession.process,
  OAThreadLocalService.
  Related CODEX findings: ThreadLocal source cleared to null; master source restoration not protected by outer
  finally.
  Suggested unit tests: testReplicationSourceRestoresPreviousThreadLocalValue,
  testMasterRestoresReplicationSourceWhenClientApplyLoopFails.
  Spec target section: Replication Runtime / ThreadLocal Context.

  ID: REPL-CONCURRENT-001
  Contract statement: Concurrent replication processing must not corrupt TLog streams, queues, sequence state,
  sessions, or graph application state.
  Rationale: Replication uses background queue capture, sender threads, remote callbacks, and master processing
  threads.
  Source locations: OAReplicationClient.writeTLog, runSendSyncMessagesToMaster, OAReplicationBase.start/stop, OARepli
  cationMaster.hmClientSession.
  Related CODEX findings: client TLog rewrite races normal writes; method-cache lazy map is not thread-safe; duplicate
  guid sessions.
  Suggested unit tests: testClientTLogRewriteDoesNotRaceWithQueueCaptureWrite,
  testReplicationMethodLookupIsThreadSafeDuringConcurrentDispatch,
  testMasterRejectsOrReplacesDuplicateClientGuidSession.
  Spec target section: Replication Runtime / Concurrency.

  ID: REPL-LIFECYCLE-001
  Contract statement: Replication start/stop must accurately represent running state and must not leave active
  threads, lookups, sessions, or open resources after stop returns.
  Rationale: False lifecycle state breaks restart, tests, and production failover.
  Source locations: OAReplicationBase.start/stop, OAReplicationClient.start/stop, OAReplicationMaster.start/stop,
  OAReplClientConnection.start/stop.
  Related CODEX findings: start partial failure leaves resources; stop does not join sender/master threads; master
  lookup not removed.
  Suggested unit tests: testReplicationStartFailureCleansPartialThreadsAndResources,
  testClientStopWaitsForSenderThreadTLogRewriteBeforeReturning, testMasterStopRemovesReplicationLookup.
  Spec target section: Replication Runtime / Lifecycle.

  ID: REPL-SESSION-001
  Contract statement: A replication client guid must map to at most one active authoritative session unless duplicate
  sessions are explicitly merged/rejected.
  Rationale: Replication source identity is guid-based; multiple active sessions for one guid corrupt source filtering
  and delivery state.
  Source locations: OAReplicationMaster.registerClient, hmClientSession, onClientDisconnected.
  Related CODEX findings: duplicate guid sessions not rejected/reconciled; disconnect hook not wired.
  Suggested unit tests: testMasterRejectsOrReplacesDuplicateClientGuidSession,
  testReplicationMasterRemovesSessionWhenClientDisconnects.
  Spec target section: Replication Runtime / Session Ownership.

  ID: REPL-COMPAT-001
  Contract statement: Replication behavior must remain compatible with sync, remote, queue, cache, object, Hub,
  datasource, runtime, and future merge-engine contracts.
  Rationale: Replication is a cross-cutting runtime feature; it depends on lower-level OA contracts being respected
  end to end.
  Source locations: OAReplicationBase, OAReplicationClient, OAReplicationMaster, OAReplClientConnection, remote
  replication interfaces.
  Related CODEX findings: multiple findings map to sync queue, remote call, ThreadLocal, and TLog boundaries.
  Suggested unit tests: testReplicationWorksAcrossSyncRemoteQueueAndCacheContracts,
  testReplicationReplayCooperatesWithDatasourceSaveDeleteContracts.
  Spec target section: Replication Runtime / Cross-Package Compatibility.

  Suggested package-level spec summary

  - com.viaoa.replication owns OA Store-Corp replication capture, durable transaction logging, client-master
    forwarding, replay/resync, and reconnect behavior.
  - It must preserve ordered sync semantics across disconnected runtimes.
  - It must never claim a sequence, checkpoint, delivery, or apply succeeded before the required durable/apply
    boundary.
  - It must never silently drop, duplicate, reorder, or partially apply a replication record.
  - It must preserve OA object identity, cache identity, Hub membership/order, object lifecycle, and event semantics
    during replay.
  - It must use source/origin tracking to prevent echo while still forwarding legitimate downstream changes.
  - It must restore ThreadLocal replication context exactly.
  - It assumes OA-controlled sync/remote/queue transport, but still must handle normal disconnects, retries, crashes,
    partial writes, and restart recovery.
  - Unit tests should cover TLog append/replay, sequence gaps, duplicate replay, reconnect handshakes, source
    filtering, ThreadLocal cleanup, Hub/object replay, and lifecycle start/stop.
  - Scenario/stress/failure tests should cover crash during TLog write, crash after remote apply before ack, reconnect
    after uncertain send, duplicate guid connections, delayed queue capture, concurrent stop/restart, and Store-Corp
    offline resync convergence.


*/



