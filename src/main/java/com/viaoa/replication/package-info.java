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

//CODEX unit tests <todo>

/* CODEX Invariants

REPL-RUNTIME-001 — Eventual Graph Replication Authority
Contract statement:
com.viaoa.replication is the runtime authority for durable, ordered, eventual replication of OA Object Graph
mutations between replication participants; it must preserve OAObject, Hub, cache, identity, event, and lifecycle
semantics while allowing offline/reconnect catch-up.
Rationale:
Replication is the eventual-consistency layer for distributed OA graphs, not a best-effort message copier. Its
output must converge to the same semantic graph state that ordered committed mutations describe.
Source scope:
OAReplicationBase, OAReplicationClient, OAReplicationMaster, OAReplTLog, OAReplClientConnection integration,
replication remote interfaces, sync RemoteSyncInterface dispatch.
Related CODEX findings:
Existing CODEX notes around ordering, TLog durability, reconnect handshakes, false-success replay, ThreadLocal
source handling, and session ownership.
Suggested unit tests:
testReplicationCapturesCommittedSyncMutation(), testReplicationConvergesAfterOfflineReconnect(),
testReplicationPreservesObjectHubAndCacheSemantics()
Spec target section:
Replication Runtime / Core Responsibility

REPL-SYNC-BOUNDARY-001 — Sync Versus Replication Boundary
Contract statement:
Replication must be distinct from live sync: sync may deliver connected real-time changes, but replication owns
durable capture, replay, reconnect catch-up, retry, and eventual semantic convergence.
Rationale:
Transport or live-sync success does not prove durable replication success. Conflating the layers hides lost records
and uncertain replay state.
Source scope:
OAReplicationBase.processQueue(), OAReplicationClient.onNewSyncMessage(...),
OAReplicationMaster.onNewSyncMessage(...), OAReplicationClient.runSendSyncMessagesToMaster(),
OAReplicationMaster.ReplClientSession.process().
Related CODEX findings:
Existing notes around sync queue boundaries, remote call boundaries, sequence checkpointing, and TLog write
failures.
Suggested unit tests:
testLiveSyncSuccessDoesNotAdvanceReplicationCheckpointWithoutTLogCommit(),
testReplicationReplayUsesDurableTLogNotTransientSyncQueue()
Spec target section:
Replication Runtime / Sync-Replication Boundary

REPL-LIFECYCLE-001 — Replication Lifecycle Truth
Contract statement:
Replication start, stop, reconnect, and failed-start states must reflect committed runtime state; partial startup or
shutdown must not leave active threads, lookups, open TLog streams, queue sessions, or remote registrations while
reporting stopped or successfully started.
Rationale:
False lifecycle state breaks restart, failover, reconnect, and production observability.
Source scope:
OAReplicationBase.start(), OAReplicationBase.stop(), OAReplicationClient.start(), OAReplicationClient.stop(),
OAReplicationMaster.start(), OAReplicationMaster.stop(), OAReplClientConnection.start(),
OAReplClientConnection.stop().
Related CODEX findings:
Start partial failure leaves resources, stop does not join sender/master threads, master lookup not removed, queue
session cleanup on failure.
Suggested unit tests:
testReplicationStartFailureCleansPartialThreadsAndResources(), testClientStopWaitsForSenderThreadBeforeReturning(),
testMasterStopRemovesReplicationLookupAndClosesTLog()
Spec target section:
Replication Runtime / Lifecycle State

REPL-SESSION-001 — Replication Participant Identity
Contract statement:
A replication participant identity, including client guid and master/client session state, must map to at most one
authoritative active replication session unless duplicate sessions are explicitly rejected, disabled, or reconciled.
Rationale:
Replication source filtering, sequence ownership, reconnect catch-up, and downstream delivery rely on stable
participant identity.
Source scope:
OAReplicationMaster.registerClient(...), OAReplicationMaster.hmClientSession,
OAReplicationMaster.onClientDisconnected(...), OAReplicationClient.guid, OAReplClientConnection.
Related CODEX findings:
Duplicate guid sessions not rejected/reconciled, disconnect hook not wired, reconnect session marker initialization
gaps.
Suggested unit tests:
testMasterRejectsOrReplacesDuplicateClientGuidSession(), testReplicationMasterRemovesSessionWhenClientDisconnects(),
testReconnectUsesSingleAuthoritativeSession()
Spec target section:
Replication Runtime / Session Ownership

REPL-ORDER-001 — Ordered Change Stream
Contract statement:
Replication records must be captured, logged, transmitted, applied, and replayed in deterministic sequence order
within the relevant master/client stream.
Rationale:
Eventual convergence depends on all participants observing the same ordered mutation stream or failing visibly when
order cannot be proven.
Source scope:
OAReplicationBase.processQueue(), OAReplicationClient.runSendSyncMessagesToMaster(),
OAReplicationClient.onNewMessageFromMaster(...), OAReplicationMaster.ReplClientSession.process(),
OAReplicationMaster.RemoteMasterInterface.processMessage(...).
Related CODEX findings:
Sequence-gap detection for master/client replay, duplicate master message filtering, failed client message poll/
drop.
Suggested unit tests:
testClientRejectsOrResyncsOnMasterSequenceGap(), testMasterRejectsOrResyncsOnClientSequenceGap(),
testDuplicateMasterSequenceDoesNotReapply()
Spec target section:
Replication Runtime / Ordering Semantics

REPL-CHECKPOINT-001 — Checkpoint Advancement
Contract statement:
Replay offsets, last-sent, last-received, last-processed, and acknowledgment sequence markers must advance only
after the matching capture, durable write, delivery, apply, or replay operation is complete under its contract.
Rationale:
Premature checkpoint movement causes silent skips, duplicate replay, unretryable failure, or false catch-up state.
Source scope:
OAReplicationClient.lastSentClientSeq, OAReplicationClient.masterSeq,
OAReplicationClient.runSendSyncMessagesToMaster(), OAReplicationMaster.ReplClientSession.lastSentMasterSeq,
lastReceivedClientSeq, lastProcessedClientSeq, lastReceivedMasterSeq, lastProcessedMasterSeq.
Related CODEX findings:
Master advances received client seq before apply, bGotSeqFromMaster not per connection, reconnect session sequence
initialization gaps, queue position advances before TLog commit.
Suggested unit tests:
testMasterDoesNotAckClientSeqBeforeApplyOrDurableCommit(), testClientRechecksMasterAckSequenceAfterReconnect(),
testQueuePositionDoesNotAdvanceWhenTLogWriteFails()
Spec target section:
Replication Runtime / Checkpoint Semantics

REPL-TLOG-001 — Independent Durable Records
Contract statement:
Each OAReplTLog record must represent one complete, independent, replayable change whose payload is not affected by
later mutation of source objects, argument arrays, stream back-references, or in-memory queue state.
Rationale:
Replay and crash recovery must reconstruct the recorded change, not the later state of Java objects or
ObjectOutputStream handles.
Source scope:
OAReplTLog, OAReplicationClient.writeTLog(...), OAReplicationMaster.writeTLog(...),
OAReplicationClient.onNewSyncMessage(...), OAReplicationMaster.onNewSyncMessage(...).
Related CODEX findings:
ObjectOutputStream handle reuse, mutable RequestInfo args payload drift, payload snapshot concerns.
Suggested unit tests:
testTLogSerializesUpdatedObjectStateAcrossMultipleRecords(),
testQueuedReplicationMessagePayloadDoesNotDriftAfterSourceObjectMutates(),
testTLogRecordIsReplayableAfterSourceObjectMutation()
Spec target section:
Replication Runtime / Transaction Log Semantics

REPL-DURABLE-001 — Durability Before Acknowledgment
Contract statement:
A sequence, delivery acknowledgment, replay marker, or catch-up claim must not advance before the corresponding
replication record is durably logged or safely applied according to the operation boundary.
Rationale:
Premature durability claims lose changes after crash, reconnect, or remote failure.
Source scope:
OAReplicationClient.writeTLog(...), OAReplicationMaster.writeTLog(...), OAReplicationClient.onNewSyncMessage(...),
OAReplicationMaster.onNewSyncMessage(...), OAReplicationMaster.remoteMaster.processMessage(...),
OAReplicationClient.runSendSyncMessagesToMaster().
Related CODEX findings:
Master/client sequence increments before TLog write succeeds, failed enqueue still advances received seq, remote
client receipt durability ambiguity.
Suggested unit tests:
testMasterSeqDoesNotAdvanceWhenTLogWriteFails(), testClientSeqDoesNotAdvanceWhenLocalTLogWriteFails(),
testMasterDoesNotAdvanceClientSeqWhenClientMessageEnqueueFails()
Spec target section:
Replication Runtime / Durability Semantics

REPL-REPLAY-001 — Replay Correctness
Contract statement:
Replay must not silently skip, duplicate, reorder, or partially apply a record while reporting success; duplicate,
missing, corrupt, or partial records must trigger explicit ignore, retry, truncation, resync, or failure behavior.
Rationale:
Replay is authoritative recovery behavior. Silent wrong replay creates permanent graph divergence.
Source scope:
OAReplicationClient.loadTLogFile(), OAReplicationMaster.loadTLogFile(),
OAReplicationClient.onNewMessageFromMaster(...), OAReplicationMaster.ReplClientSession.process().
Related CODEX findings:
Partial trailing TLog record handling, duplicate/gap sequence handling, unsent local TLogs skipped on restart.
Suggested unit tests:
testTLogLoadTruncatesPartialTrailingRecordBeforeAppend(), testClientIgnoresDuplicateMasterSequence(),
testClientRestartRequeuesUnsentLocalTLogRecords()
Spec target section:
Replication Runtime / Replay Semantics

REPL-RECONNECT-001 — Reconnect and Catch-Up
Contract statement:
Reconnect and resync must reconcile both sides’ durable sequence state before sending, applying, or acknowledging
uncertain messages.
Rationale:
Disconnect boundaries are where duplicate execution, missed records, and stale checkpoint assumptions most often
occur.
Source scope:
OAReplClientConnection.start(), RemoteMasterRegisterInterface.registerClient(...),
OAReplicationClient.runSendSyncMessagesToMaster(), OAReplicationMaster.ReplClientSession,
OAReplicationClient.loadTLogFile().
Related CODEX findings:
Reconnect ack handshake not refreshed, constructor does not initialize all session replay markers, bGotSeqFromMaster
not connection-scoped.
Suggested unit tests:
testClientRechecksMasterAckSequenceAfterReconnect(),
testMasterSessionInitializesReplayPositionFromReconnectingClient(),
testReconnectDoesNotReapplyAlreadyProcessedClientSeq()
Spec target section:
Replication Runtime / Reconnect and Resync

REPL-RETRY-001 — Retry Safety
Contract statement:
Retry after failed write, send, apply, replay, reconnect, or shutdown must not corrupt ordering, duplicate committed
state, drop required records, or advance checkpoints without the corresponding operation completing.
Rationale:
Replication exists to survive offline and failure conditions; retry must be a correctness path, not a corruption
path.
Source scope:
OAReplicationClient.runSendSyncMessagesToMaster(), OAReplicationClient.loadTLogFile(),
OAReplicationMaster.ReplClientSession.process(), OAReplicationMaster.remoteMaster.processMessage(...).
Related CODEX findings:
Failed client message polled and dropped, unsent local TLogs skipped on restart, failed enqueue still advances
received seq.
Suggested unit tests:
testMasterRetainsClientMessageWhenApplyFails(), testClientRestartRequeuesUnsentLocalTLogRecords(),
testFailedSendIsRetriedWithoutSequenceSkip()
Spec target section:
Replication Runtime / Retry Semantics

REPL-SOURCE-001 — Originator Filtering
Contract statement:
Replication source/origin filtering must suppress only the originating participant’s redundant echo and must not
suppress legitimate downstream propagation to other participants.
Rationale:
Bad source filtering either loops replicated changes indefinitely or drops required propagation.
Source scope:
OAReplicationClient.onNewSyncMessage(...), OAReplicationMaster.onNewSyncMessage(...),
OAReplicationMaster.ReplClientSession.process(), OAThreadLocalService.replicationSource.
Related CODEX findings:
Source stored as client guid for all client TLogs, ThreadLocal source not restored to previous value.
Suggested unit tests:
testMasterDoesNotEchoSourceClientButSendsToOthers(), testSourceFilteringDoesNotSuppressNonOriginatingClient(),
testClientAppliedMasterMessageDoesNotCreateEchoRecord()
Spec target section:
Replication Runtime / Originator Filtering

REPL-TL-001 — Replication Context Restoration
Contract statement:
Replication ThreadLocal/runtime context, including replicationSource and sync-message suppression state where
applicable, must be restored to the previous value with try/finally on success, failure, and early exit.
Rationale:
Context leakage can suppress legitimate changes, echo replicated work, or contaminate unrelated sync/remote requests
on reused threads.
Source scope:
OAReplicationClient.onNewMessageFromMaster(...), OAReplicationMaster.ReplClientSession.process(),
OAReplicationClient.onNewSyncMessage(...), OAReplicationMaster.onNewSyncMessage(...), OAThreadLocalService.
Related CODEX findings:
ThreadLocal source cleared to null instead of restored, master source restoration not protected by outer finally.
Suggested unit tests:
testReplicationSourceRestoresPreviousThreadLocalValue(),
testMasterRestoresReplicationSourceWhenClientApplyLoopFails(), testNestedReplicationContextRestoresOuterValue()
Spec target section:
Replication Runtime / ThreadLocal Context

REPL-IDENTITY-001 — Replicated Object Identity
Contract statement:
Replication must preserve GUID identity, OAObjectKey identity, datasource primary-key identity, and business/match-
key identity without conflating them during capture, TLog serialization, replay, cache lookup, and graph
application.
Rationale:
Semantic convergence requires each site to resolve replicated changes to the same logical object and authoritative
cache instance.
Source scope:
OAReplTLog.args, OAReplicationClient.onNewMessageFromMaster(...), OAReplicationMaster.ReplClientSession.process(),
sync RemoteSyncInterface payloads, RemoteSyncImpl application.
Related CODEX findings:
Payload snapshot concerns and remote sync apply false-success risks.
Suggested unit tests:
testReplayResolvesSameGuidToSameObject(), testReplayDoesNotConflateBusinessKeyWithGuid(),
testReplayUsesExistingCachedObjectForSameIdentity()
Spec target section:
Replication Runtime / Object Identity

REPL-CACHE-001 — Cache and Live Instance Consistency
Contract statement:
Replicated apply must reconcile with OA cache authority and must not create duplicate authoritative instances,
overwrite newer correct live instances, or advance sequence state when cache/object application fails.
Rationale:
Cache drift breaks Hub membership, property changes, datasource saves, and later replay.
Source scope:
OAReplicationClient.onNewMessageFromMaster(...), OAReplicationMaster.ReplClientSession.process(), RemoteSyncImpl,
cache/object application paths.
Related CODEX findings:
Boolean return values from remote sync apply ignored, cache consistency concerns during replay.
Suggested unit tests:
testReplicationDoesNotAdvanceSequenceWhenRemoteSyncReturnsFalse(),
testReplayUsesExistingCachedObjectForSameIdentity(), testReplayFailureDoesNotCorruptCacheIndexes()
Spec target section:
Replication Runtime / Cache Consistency

REPL-HUB-001 — Replicated Hub Semantics
Contract statement:
Replicated Hub operations must preserve membership, order, insert, move, remove, remove-all, sort, refresh, active-
object, and relationship semantics according to Hub and sync contracts.
Rationale:
Hub state is primary OA graph structure. Replicated membership or ordering drift creates visible application
divergence.
Source scope:
RemoteSyncInterface.addToHub(...), insertInHub(...), removeFromHub(...), removeAllFromHub(...),
moveObjectInHub(...), sort(...), refresh(...), replication apply paths.
Related CODEX findings:
Boolean return values ignored, sequence gaps can skip Hub operations.
Suggested unit tests:
testReplayPreservesHubInsertOrder(), testReplayPreservesHubMoveAndRemoveOrder(),
testReplayDoesNotAdvanceWhenHubApplyReturnsFalse()
Spec target section:
Replication Runtime / Hub Semantics

REPL-PROP-001 — Replicated Property Semantics
Contract statement:
Replicated property changes must apply to the intended object and property, preserve the recorded committed value,
and remain ordered relative to other property, Hub, create, save, and delete records in the same stream.
Rationale:
Property replication drives semantic graph state, UI, filters, triggers, serialization, and downstream convergence.
Source scope:
OAReplTLog, OAReplicationBase.processQueue(), OAReplicationClient.onNewMessageFromMaster(...),
OAReplicationMaster.ReplClientSession.process(), RemoteSyncInterface.propertyChange(...).
Related CODEX findings:
Sequence-gap detection and payload snapshot concerns.
Suggested unit tests:
testReplayPropertyChangeAppliesRecordedValueNotLaterMutation(), testReplayPropertyChangesRemainOrderedWithDelete(),
testPropertyReplayTargetsCorrectObjectAndProperty()
Spec target section:
Replication Runtime / Property Semantics

REPL-OBJECT-001 — Replicated Object Lifecycle
Contract statement:
Replicated create, update, save, delete, refresh, and lifecycle records must preserve OA object lifecycle state,
cache visibility, datasource assumptions, and observable graph semantics across participants.
Rationale:
Distributed participants must converge on whether an object exists, is deleted, is saved, and is visible in the
graph.
Source scope:
OAReplTLog, OAReplicationClient.onNewMessageFromMaster(...), OAReplicationMaster.ReplClientSession.process(),
RemoteSyncInterface server/client lifecycle methods, RemoteSyncImpl.
Related CODEX findings:
Boolean apply return ignored, sequence gaps can skip lifecycle operations, false-success apply risks.
Suggested unit tests:
testReplayCreateUpdateDeleteConvergesObjectLifecycle(), testReplayDoesNotAdvanceWhenDeleteApplyFails(),
testReplayRefreshDoesNotCreateFalseSuccess()
Spec target section:
Replication Runtime / Object Lifecycle Semantics

REPL-EVENT-001 — Event Capture and Replay Ordering
Contract statement:
Replication must capture committed sync/object/Hub events in their semantic order and replay them in an order that
preserves observable OA event and graph state contracts.
Rationale:
Replication derives from runtime change events; event order must match committed OG behavior for convergence.
Source scope:
OAReplicationBase.processQueue(), OAReplicationClient.onNewSyncMessage(...),
OAReplicationMaster.onNewSyncMessage(...), sync queue integration.
Related CODEX findings:
Queue position advances before TLog commit, queue session cleanup on failure.
Suggested unit tests:
testReplicationQueuePositionDoesNotAdvanceWhenTLogWriteFails(), testReplicatedEventsPreserveSyncQueueOrder(),
testReplayPublishesEventsInCommittedOrder()
Spec target section:
Replication Runtime / Event Capture

REPL-MERGE-001 — Merge and Conflict Visibility
Contract statement:
When replay, reconnect, or catch-up encounters conflicting, stale, missing-dependency, or non-convergent state,
replication must make the conflict visible or route it to an explicit merge/resync policy; it must not silently
choose an arbitrary winner.
Rationale:
Eventual consistency requires deterministic convergence or visible conflict. Silent conflict resolution can corrupt
business state.
Source scope:
OAReplicationClient.onNewMessageFromMaster(...), OAReplicationMaster.ReplClientSession.process(), sequence/version
fields in OAReplTLog, future merge/version-vector integration points.
Related CODEX findings:
Sequence gaps, duplicate filtering, stale reconnect state, and failed apply handling indicate missing explicit
conflict boundaries.
Suggested unit tests:
testReplayGapRequiresResyncOrVisibleFailure(), testStaleVersionDoesNotSilentlyOverwriteNewerState(),
testMissingDependencyDuringReplayIsVisible()
Spec target section:
Replication Runtime / Merge and Conflict Boundaries

REPL-FAIL-001 — Failure Visibility
Contract statement:
Failed capture, write, rename, load, send, receive, apply, replay, checkpoint, reconnect, shutdown, or cleanup must
be caller-visible or operationally observable; replication must not silently report success for incomplete work.
Rationale:
Silent false-success prevents retry and creates hidden distributed divergence.
Source scope:
OAReplicationClient.writeTLog(...), OAReplicationClient.loadTLogFile(),
OAReplicationClient.runSendSyncMessagesToMaster(), OAReplicationMaster.writeTLog(...),
OAReplicationMaster.loadTLogFile(), OAReplicationMaster.ReplClientSession.process(),
OAReplicationMaster.remoteMaster.processMessage(...).
Related CODEX findings:
Ignored renameTo result, failed enqueue still advances received seq, ignored boolean apply results, stream cleanup
issues.
Suggested unit tests:
testClientTLogRewriteFailureDoesNotReportSuccess(),
testMasterDoesNotAdvanceClientSeqWhenClientMessageEnqueueFails(), testRemoteApplyFalseIsReplicationFailure()
Spec target section:
Replication Runtime / Failure Semantics

REPL-PARTIAL-001 — Partial Progress Visibility
Contract statement:
If replication partially captures, logs, sends, applies, replays, or cleans up a change before failure, the
incomplete boundary must remain visible through state, diagnostics, retry position, or explicit resync requirement.
Rationale:
Partial progress is normal under crash, EOF, disconnect, and remote failure, but it must not masquerade as semantic
convergence.
Source scope:
OAReplicationClient.runSendSyncMessagesToMaster(), OAReplicationClient.writeTLog(...),
OAReplicationMaster.ReplClientSession.process(), OAReplicationBase.processQueue(), loadTLogFile() methods.
Related CODEX findings:
Partial trailing TLog record handling, failed client message poll/drop, sequence advancement before durable/apply
boundary.
Suggested unit tests:
testPartialTrailingTLogRecordDoesNotAppearCommitted(), testFailedApplyLeavesRetryableState(),
testCrashAfterTLogBeforeAckDoesNotLoseRecord()
Spec target section:
Replication Runtime / Partial Progress

REPL-CONCURRENT-001 — Concurrent Replication State Safety
Contract statement:
Concurrent queue capture, TLog writing, TLog rewrite, sender processing, master processing, method lookup, session
registration, stop, and reconnect must not corrupt streams, queues, sequence state, sessions, or graph application
state.
Rationale:
Replication uses background threads, remote callbacks, queues, and session maps; shared state must remain
deterministic under normal concurrency.
Source scope:
OAReplicationClient.writeTLog(...), OAReplicationClient.runSendSyncMessagesToMaster(),
OAReplicationClient.loadTLogFile(), OAReplicationBase.start(), OAReplicationBase.stop(),
OAReplicationBase.getMethod(...), OAReplicationMaster.hmClientSession,
OAReplicationMaster.ReplClientSession.process().
Related CODEX findings:
Client TLog rewrite races normal writes, method-cache lazy map is not thread-safe, duplicate guid sessions.
Suggested unit tests:
testClientTLogRewriteDoesNotRaceWithQueueCaptureWrite(),
testReplicationMethodLookupIsThreadSafeDuringConcurrentDispatch(),
testMasterRejectsOrReplacesDuplicateClientGuidSession()
Spec target section:
Replication Runtime / Concurrency

REPL-IO-001 — Replication Log I/O Boundaries
Contract statement:
Replication log files and streams opened by the package must be flushed, closed, replaced, truncated, or reopened
according to explicit commit boundaries; failed I/O must not corrupt the committed log or leave ambiguous append
state.
Rationale:
The TLog is the durable source of replay truth. I/O ambiguity directly becomes replication ambiguity.
Source scope:
OAReplicationClient.openTLogFile(), OAReplicationClient.createNewTLogFile(...), OAReplicationClient.writeTLog(...),
OAReplicationClient.loadTLogFile(), OAReplicationMaster.openTLogFile(), OAReplicationMaster.createNewTLogFile(...),
OAReplicationMaster.writeTLog(...), OAReplicationMaster.loadTLogFile().
Related CODEX findings:
Ignored renameTo result, stream cleanup in loadTLogFile, ObjectOutputStream handle reuse, TLog rewrite races.
Suggested unit tests:
testTLogRenameFailureLeavesOriginalLogUsable(), testLoadTLogFileClosesStreamsOnFailure(),
testTLogAppendAfterRewriteUsesValidStreamState()
Spec target section:
Replication Runtime / Durable Log I/O

REPL-APPLY-001 — Apply Result Authority
Contract statement:
When replication invokes sync/remote application methods, the result and thrown exceptions must determine whether
the record is considered applied; Boolean.FALSE or failed invocation must not advance processed sequence without
explicit retry/resync handling.
Rationale:
Replication success depends on semantic graph application, not merely reflective method invocation.
Source scope:
OAReplicationClient.onNewMessageFromMaster(...), OAReplicationMaster.ReplClientSession.process(),
OAReplicationBase.getMethod(...), RemoteSyncInterface methods.
Related CODEX findings:
Boolean return values from remote sync apply ignored, method.invoke result ignored, remote apply false-success.
Suggested unit tests:
testReplicationDoesNotAdvanceSequenceWhenRemoteSyncReturnsFalse(),
testMethodInvocationExceptionLeavesRecordRetryable(), testUnsupportedReplicationMethodFailsVisibly()
Spec target section:
Replication Runtime / Apply Semantics

REPL-COMPAT-001 — Cross-Package Compatibility
Contract statement:
Replication behavior must remain compatible with sync, remote, comm, queue, cache, object, Hub, datasource, runtime,
transaction, serialization, trigger, and future merge/version contracts; replication coordinates these packages but
must not redefine their semantic authority.
Rationale:
Replication is cross-cutting runtime infrastructure. Correctness depends on preserving lower-level OA contracts end
to end.
Source scope:
OAReplicationBase, OAReplicationClient, OAReplicationMaster, OAReplTLog, OAReplClientConnection, com.viaoa.sync.,
com.viaoa.remote., com.viaoa.comm., com.viaoa.queue., com.viaoa.cache., com.viaoa.object., com.viaoa.hub.,
com.viaoa.datasource., com.viaoa.transaction., com.viaoa.serialize..
Related CODEX findings:
Multiple findings map to sync queue, remote invocation, ThreadLocal, TLog, cache, and apply boundaries.
Suggested unit tests:
testReplicationWorksAcrossSyncRemoteQueueAndCacheContracts(),
testReplicationReplayCooperatesWithDatasourceSaveDeleteContracts(),
testReplicationReplayPreservesTriggerAndSerializationContracts()
Spec target section:
Replication Runtime / Cross-Package Compatibility

*/


