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
 * Core synchronization layer for distributed OA models.
 * <p>
 * This package integrates {@link com.viaoa.object.OAObject OAObject} /
 * {@link com.viaoa.hub.Hub Hub} observability with the multiplexer-based
 * remoting classes to keep models synchronized across JVMs.
 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li>Managing {@link OASyncServer} instances that host remote sync endpoints,
 *       sessions and file transfer.</li>
 *   <li>Managing {@link OASyncClient} instances that connect to remote servers,
 *       obtain {@code RemoteServer}, {@code RemoteSession}, {@code RemoteClient}
 *       and {@code RemoteSync} proxies, and keep the local object graph in sync.</li>
 *   <li>Providing static helpers in {@link OASyncDelegate} to resolve the
 *       appropriate server, client and remote interfaces for a given model
 *       package and to determine whether the current code path is executing
 *       on a client or on a server.</li>
 *   <li>Optionally combining multiple servers into a single logical model via
 *       the experimental {@link OASyncCombinedClient}.</li>
 * </ul>
 * The typical usage pattern is:
 * <ol>
 *   <li>Create and start an {@link OASyncServer} on the server JVM.</li>
 *   <li>Create and start one {@link OASyncClient} per application JVM.</li>
 *   <li>Allow {@link OASyncDelegate} / {@link OASync} to route sync operations
 *       for {@link com.viaoa.object.OAObject} and {@link com.viaoa.hub.Hub} so that
 *       changes are automatically propagated between client and server.</li>
 * </ol>
 */
package com.viaoa.sync;

//CODEX unit tests <todo>

/* CODEX Invariants

SYNC-RUNTIME-001 — Live Graph Synchronization Authority
Contract statement:
com.viaoa.sync is the runtime authority for real-time connected synchronization of OAObject lifecycle changes, Hub
membership changes, property changes, cache visibility, remote sync endpoints, and live distributed graph state
between OA participants.
Rationale:
Sync must preserve OA/OG runtime semantics across JVM boundaries; it is not a separate data model or best-effort
notification layer.
Source scope:
OASyncClient, OASyncServer, com.viaoa.sync.remote.RemoteSyncInterface, RemoteSyncImpl, RemoteServerInterface,
RemoteServerImpl, RemoteClientInterface, RemoteClientImpl, RemoteSessionInterface, RemoteSessionImpl.
Related CODEX findings:
Existing CODEX notes around stale remote proxies, partial startup cleanup, stale session state, false-success
delivery, and session tracking.
Suggested unit tests:
testSyncClientStartCreatesRequiredRemoteInterfaces(), testSyncServerStartPublishesRequiredRemoteEndpoints(),
testSyncRoutesMutationsThroughActiveAuthority()
Spec target section:
Sync Runtime / Core Responsibility

SYNC-ROLE-001 — Sync Role Boundaries
Contract statement:
Sync behavior must be determined by the active OA runtime role: single-user runtimes must not use remote sync paths;
clients must route authoritative changes to the server; servers must apply and fan out changes according to
connected-client eligibility.
Rationale:
Incorrect role interpretation can cause local-only applications to send sync messages, clients to mutate
authoritative state incorrectly, or servers to omit required fan-out.
Source scope:
OASyncClient, OASyncServer, OASyncDelegate usage, RemoteSyncImpl, RemoteServerImpl, RemoteClientImpl.
Related CODEX findings:
Existing package-info notes role-semantics coverage and client/server routing risks.
Suggested unit tests:
testSingleUserDoesNotUseRemoteSyncPaths(), testClientRoutesAuthoritativeChangesToServer(),
testServerAppliesAndFansOutEligibleClientChange()
Spec target section:
Sync Runtime / Runtime Role Semantics

SYNC-LIFECYCLE-001 — Client/Server Lifecycle Truth
Contract statement:
Sync client and server lifecycle state must reflect committed startup, connected, disconnected, stopping, stopped,
and failed states; partial startup or shutdown must not publish a successful started/connected state.
Rationale:
Reconnect logic, monitoring, routing, callbacks, and remote endpoint lookup depend on truthful lifecycle state.
Source scope:
OASyncClient.start(), OASyncClient.stop(boolean), OASyncClient.isStarted(), OASyncClient.isConnected(),
OASyncServer.start(), OASyncServer.stop(), ClientInfo, ServerInfo.
Related CODEX findings:
Partial client startup cleanup, partial server startup cleanup, ServerInfo.started not updated, stale proxies after
stop/reconnect.
Suggested unit tests:
testClientStartedFalseAfterFailedStartupCleanup(), testServerInfoStartedTracksServerLifecycle(),
testStoppedClientDoesNotExposeStaleRemoteProxies()
Spec target section:
Sync Runtime / Lifecycle State

SYNC-SESSION-001 — Session Identity
Contract statement:
Each connected client must have exactly one active server-side sync session identity used for filtering, callbacks,
retention, locking, and remote endpoint lookup; reconnect must establish a new clean session identity.
Rationale:
Per-client sync state is session-scoped. Reusing or duplicating session identity can misroute updates, leak locks,
or apply stale filtering decisions.
Source scope:
OASyncServer.onClientConnect(), OASyncServer.onClientDisconnect(), OASyncServer.getRemoteSession(...),
OASyncServer.getRemoteClient(...), RemoteSessionImpl, ClientInfo.
Related CODEX findings:
Session creation ordering window, null remoteSession on disconnect, stale ClientInfo after disconnect, stale remote
proxies after reconnect.
Suggested unit tests:
testClientConnectCreatesSingleSessionState(), testReconnectDoesNotReuseOldGuidTracking(),
testDisconnectBeforeRemoteSessionCreationDoesNotThrow()
Spec target section:
Sync Runtime / Session Lifecycle

SYNC-SESSION-002 — Session Accounting
Contract statement:
Server session counters and session maps must remain consistent with actual remote multiplexer sessions across
connect, disconnect, duplicate removal, late update, and failed setup paths.
Rationale:
Administrative status, disconnect cleanup, remote lookup, and reconnect handling rely on accurate connected-session
state.
Source scope:
OASyncServer.getRemoteMultiplexerServer(), OASyncServer.onSessionCreated(), OASyncServer.onSessionRemoved(),
OASyncServer.getSessionCount(), OASyncServer.onUpdate(...), hmClientInfoExt.
Related CODEX findings:
Unconditional session-count decrement, late client update clearing disconnect state, remote facade lookup after
disconnect.
Suggested unit tests:
testDuplicateRemoveSessionDoesNotDecrementSessionCountTwice(),
testLateClientUpdateAfterDisconnectDoesNotReactivateSession(), testLateRemoteLookupAfterDisconnectFailsVisibly()
Spec target section:
Sync Runtime / Session Accounting

SYNC-ORDER-001 — Ordered Sync Mutation Processing
Contract statement:
Sync messages that mutate OAObject or Hub state must be sent, received, and applied in deterministic order wherever
local OA semantics depend on ordering.
Rationale:
Property changes, object lifecycle changes, Hub add/remove/insert/move/clear, delete, and refresh operations are
order-sensitive.
Source scope:
OASyncServer.SyncQueueName, OASyncServer.QueueSize, OASyncServer.getRemoteMultiplexerServer(), RemoteSyncInterface,
RemoteSyncImpl, RemoteClientInterface, RemoteServerInterface.
Related CODEX findings:
Existing CODEX notes around queue paths, authoritative ordering, Hub mutation return handling, and replay/resync
ordering.
Suggested unit tests:
testPropertyChangeOrderingIsPreservedAcrossSync(), testHubAddMoveRemoveOrderingIsPreservedAcrossSync(),
testDeleteAfterHubRemoveAppliesInCommittedOrder()
Spec target section:
Sync Runtime / Message Ordering

SYNC-DELIVERY-001 — Eligible Change Delivery
Contract statement:
A committed local change eligible for sync must either be delivered to the intended active endpoint or fail visibly
through caller-visible, logged, or operationally observable failure.
Rationale:
Silent sync message loss creates distributed graph divergence while making the local operation appear successful.
Source scope:
OASyncClient.objectCreated(...), OASyncClient.updateObjectsWithoutHubs(...), RemoteSyncImpl.propertyChange(...),
RemoteSyncImpl.addToHub(...), RemoteSyncImpl.removeFromHub(...), RemoteServerImpl.save(...).
Related CODEX findings:
Swallowed objectCreated failure, background no-hub worker death, remote save false-success, ignored remote boolean
returns.
Suggested unit tests:
testObjectCreatedRemoteFailureIsNotSilentlyLost(), testNoHubUpdateWorkerFailureDoesNotAcceptUnprocessedWork(),
testFailedRemoteSaveDoesNotAppearSuccessful()
Spec target section:
Sync Runtime / Delivery Semantics

SYNC-DELIVERY-002 — Active Endpoint Delivery
Contract statement:
Sync delivery success must refer to the current active session, remote proxy, and endpoint; stale proxies or old
connection state must not be treated as successful delivery targets.
Rationale:
Reconnect and session replacement are normal distributed runtime operations. Delivery to stale endpoints silently
loses or misroutes state.
Source scope:
OASyncClient.getRemoteServer(), OASyncClient.getRemoteSession(), OASyncClient.getRemoteClient(),
OASyncClient.getRemoteSync(), OASyncClient.stop(boolean), OASyncServer.getRemoteSession(...),
OASyncServer.getRemoteClient(...).
Related CODEX findings:
Stale proxies after reconnect, worker-cached RemoteSessionInterface, static client tracking maps across sessions.
Suggested unit tests:
testReconnectUsesFreshRemoteSession(), testBackgroundWorkersUseCurrentSessionAfterReconnect(),
testStopClearsStaleRemoteInterfaces()
Spec target section:
Sync Runtime / Reconnect Delivery

SYNC-IDENTITY-001 — Distributed Object Identity
Contract statement:
Sync messages must identify target objects by stable OA identity sufficient to resolve the intended receiver-side
object without conflating GUID identity, OAObjectKey identity, datasource primary key identity, or business/match-
key identity.
Rationale:
Distributed mutation must target the same semantic object. Identity drift creates duplicate objects, wrong-object
mutation, or broken cache reconciliation.
Source scope:
RemoteSyncInterface.propertyChange(...), RemoteSyncInterface.addToHub(...), RemoteSyncInterface.removeFromHub(...),
RemoteSyncInterface.serverDelete(...), RemoteServerImpl.getObject(...), RemoteServerImpl.getObjectUsingPkey(...),
RemoteSyncImpl.getObject(...), RemoteClientImpl.getObject(...).
Related CODEX findings:
Datasource/cache reload paths not preserving requested GUID, detail-load GUID drift, primary-key reload identity
concerns.
Suggested unit tests:
testPropertyChangeTargetsObjectByGuidIdentity(), testSyncReloadByPrimaryKeyDoesNotChangeGuidIdentity(),
testDetailLoadByObjectKeyPreservesGuidIdentity()
Spec target section:
Sync Runtime / Identity Semantics

SYNC-CACHE-001 — Cache Reconciliation
Contract statement:
Sync cache lookup must reconcile object identity with authoritative key/datasource state; cache misses must not turn
valid authoritative sync operations into silent no-ops when recovery is part of the operation contract.
Rationale:
OA caches can be weak, transient, or incomplete. Sync must not confuse cache presence with distributed object
existence.
Source scope:
RemoteServerImpl.save(...), RemoteClientImpl.refresh(...), RemoteClientImpl.createCopy(...),
RemoteSessionImpl.setLock(...), RemoteSessionImpl.updateObjectsWithoutHubs(...), RemoteDataSource.getObject(...).
Related CODEX findings:
Save cache-only false-success, refresh cache-only no-op, createCopy cache-only null, lock cache-only false.
Suggested unit tests:
testRemoteSaveLoadsObjectAfterServerCacheMiss(), testRemoteRefreshFailsVisiblyWhenTargetCannotBeResolved(),
testRemoteLockLoadsObjectAfterServerCacheMiss()
Spec target section:
Sync Runtime / Cache and Datasource Boundaries

SYNC-GUID-TRACK-001 — Client Object Tracking Accuracy
Contract statement:
Server-side per-client GUID tracking must represent objects actually known, serialized, retained, or visible to that
client; it must not be committed before successful delivery or retained after a newer valid state supersedes it.
Rationale:
GUID tracking controls filtering, reference-vs-full serialization, retention, duplicate prevention, and resync
behavior.
Source scope:
RemoteSessionImpl.hmGuid, RemoteSessionImpl.objectCreated(...), RemoteSessionImpl.objectsFinalized(...),
ClientGetDetail.afterSerialize(...), RemoteClientImpl.setCached(...), OASyncClient.objectCreated(...),
OASyncClient.objectFinalized(...).
Related CODEX findings:
hmGuid.putIfAbsent before updateObjectCache, stale finalized GUID removal, objectCreated failure, trailing finalized
GUID batch.
Suggested unit tests:
testGuidMapOnlyRecordsSuccessfullyDeliveredObjects(), testGuidNotRegisteredBeforeSuccessfulObjectDelivery(),
testStaleFinalizationDoesNotRemoveReloadedGuid()
Spec target section:
Sync Runtime / Client Object Tracking

SYNC-RETENTION-001 — Objects Outside Hubs
Contract statement:
Objects referenced by a client outside loaded Hubs must remain tracked and retained until the client releases,
finalizes, or disconnects them according to sync lifecycle rules.
Rationale:
Client-held objects outside Hub membership are still live distributed graph state and must not disappear from server
retention assumptions.
Source scope:
OASyncClient.updateObjectsWithoutHubs(...), OASyncClient.startObjectsWithoutHubsThread(),
RemoteSessionImpl.updateObjectsWithoutHubs(...), RemoteSessionImpl.saveCache(...), RemoteSessionImpl.clearCaches().
Related CODEX findings:
No-hub worker exits with live queue, static no-hub map across sessions, cache-miss no-op in server retention.
Suggested unit tests:
testClientHeldObjectOutsideHubIsRetainedOnServer(), testNoHubRetentionSurvivesServerCacheEviction(),
testReconnectDoesNotDropQueuedRetentionUpdates()
Spec target section:
Sync Runtime / Retention Semantics

SYNC-HUB-001 — Hub Mutation Semantics
Contract statement:
Remote Hub add, insert, remove, move, sort, refresh, remove-all, and clear-change messages must preserve the same
membership, ordering, active-object, and relationship semantics as the equivalent committed local Hub operation.
Rationale:
Hub state is core OA graph structure. Remote application must not produce a different semantic collection than local
mutation would.
Source scope:
RemoteSyncInterface, RemoteSyncImpl.addToHub(...), RemoteSyncImpl.insertInHub(...),
RemoteSyncImpl.removeFromHub(...), RemoteSyncImpl.removeAllFromHub(...), RemoteSyncImpl.moveObjectInHub(...),
RemoteSyncImpl.sort(...), RemoteSyncImpl.refresh(...), RemoteSyncImpl.clearHubChanges(...).
Related CODEX findings:
Ignored remote boolean returns in Hub CS paths, removeAll unloaded state issue, Hub mutation filtering risks.
Suggested unit tests:
testRemoteHubAddMatchesLocalHubAddSemantics(), testRemoteHubMovePreservesOrder(),
testRemoteRemoveAllPreservesLoadedStateContract()
Spec target section:
Sync Runtime / Hub Semantics

SYNC-HUB-002 — Hub Message Eligibility
Contract statement:
Server filtering of Hub sync messages must suppress only messages that a client cannot semantically apply; it must
not suppress required messages for clients that have the relevant object, Hub, or loaded detail state.
Rationale:
Over-filtering causes missing Hub updates; under-filtering sends unusable messages or duplicate changes.
Source scope:
OASyncServer.shouldSendSyncMessageToClient(...), RemoteSessionImpl.hmGuid,
ClientGetDetail.wasFullySentToClient(...), RemoteClientImpl.getRemoteDataSource().setCached(...).
Related CODEX findings:
Cache miss in add/insert filtering, stale hmGuid states, originator/non-originator filtering risks.
Suggested unit tests:
testLoadedClientReceivesHubInsertWhenMasterKnown(), testUnloadedClientDoesNotReceiveUnusableHubMutation(),
testNonOriginatingClientReceivesEligibleHubAdd()
Spec target section:
Sync Runtime / Hub Message Filtering

SYNC-PROP-001 — Property Change Semantics
Contract statement:
Synchronized property changes must apply to the intended object and property, preserve the committed value semantics
of the source runtime, and publish receiver-side observable changes consistently with local OA property mutation
rules.
Rationale:
Property sync drives UI, filters, triggers, serialization, calculated state, and downstream distributed consistency.
Source scope:
RemoteSyncInterface.propertyChange(...), RemoteSyncImpl.propertyChange(...), OASyncClient.objectSentToServer(...),
object/hub event integration through sync services.
Related CODEX findings:
Message identity issues, event ordering concerns, false-success delivery concerns.
Suggested unit tests:
testRemotePropertyChangeTargetsCorrectObjectAndProperty(), testRemotePropertyChangeFiresOnePropertyEvent(),
testRemotePropertyChangeDoesNotEchoBackToOriginator()
Spec target section:
Sync Runtime / Property Change Semantics

SYNC-OBJECT-001 — Object Lifecycle Sync
Contract statement:
Object create, sent-to-server, save, delete, refresh, and reload sync behavior must preserve OA object lifecycle
state, identity, cache membership, and datasource visibility across connected runtimes.
Rationale:
Distributed graphs must agree on whether an object exists, is deleted, is saved, is loaded, and is cache-visible.
Source scope:
OASyncClient.objectCreated(...), OASyncClient.objectSentToServer(...), RemoteSyncImpl.serverDelete(...),
RemoteSyncImpl.clientDelete(...), RemoteServerImpl.save(...), RemoteClientImpl.refresh(...), RemoteDataSource.
Related CODEX findings:
objectCreated failure, remote save false-success, refresh no-op, cache-only lifecycle operations.
Suggested unit tests:
testObjectCreateBecomesVisibleToServerOrFails(), testRemoteDeletePreservesLifecycleAndCacheState(),
testRemoteRefreshDoesNotReportSuccessOnMissingObject()
Spec target section:
Sync Runtime / Object Lifecycle Semantics

SYNC-ECHO-001 — Echo Suppression
Contract statement:
A sync-applied remote mutation must not re-emit redundant outbound sync messages back to its originator unless the
operation explicitly requires rebroadcast.
Rationale:
Echo loops duplicate events, inflate queues, and can cause divergent Hub or object state.
Source scope:
RemoteSyncImpl, RemoteServerImpl.save(...), OASyncServer.shouldSendSyncMessageToClient(...),
OAThreadLocalService.sendSyncMessages usage by sync/object/hub services.
Related CODEX findings:
sendSyncMessages restoration notes, remote replay/broadcast echo invariant area, originator filtering notes.
Suggested unit tests:
testClientAppliedServerPropertyChangeDoesNotEchoBack(),
testServerAppliedClientDeleteDoesNotEchoToOriginatorUnexpectedly()
Spec target section:
Sync Runtime / Originator Filtering

SYNC-ECHO-002 — Non-Originator Delivery
Contract statement:
Originator filtering must suppress only redundant sender echo and must still deliver eligible changes to other
connected clients.
Rationale:
Correct fan-out requires distinguishing echo prevention from legitimate downstream propagation.
Source scope:
OASyncServer.shouldSendSyncMessageToClient(...), RemoteSessionImpl.hmGuid,
ClientGetDetail.wasFullySentToClient(...), RemoteClientImpl.setCached(...).
Related CODEX findings:
hmGuid can misrepresent successful client delivery, stale finalization can remove reloaded GUID.
Suggested unit tests:
testOriginatorDoesNotReceiveDuplicateHubAdd(), testNonOriginatingClientReceivesEligibleHubAdd(),
testFilteringStateMatchesActuallyDeliveredObjects()
Spec target section:
Sync Runtime / Fan-Out Filtering

SYNC-EVENT-001 — Event Publication Semantics
Contract statement:
Sync-applied object and Hub mutations must publish the same committed event semantics as equivalent local
operations, with no duplicate events and no missing required events.
Rationale:
OA listeners, bindings, UI state, triggers, filters, cache coordination, and replication hooks depend on event
correctness.
Source scope:
RemoteSyncImpl.propertyChange(...), RemoteSyncImpl.addToHub(...), RemoteSyncImpl.removeFromHub(...),
RemoteSyncImpl.serverDelete(...), RemoteSyncImpl.clientDelete(...), Hub/object event integration.
Related CODEX findings:
Event ordering and delete sync paths reviewed; duplicate/missing event risks tied to echo and failure behavior.
Suggested unit tests:
testRemotePropertyChangeFiresOnePropertyEvent(), testRemoteHubRemoveFiresExpectedHubEventOrder(),
testRemoteDeletePublishesCommittedDeleteEvents()
Spec target section:
Sync Runtime / Event Semantics

SYNC-TL-001 — Sync ThreadLocal Restoration
Contract statement:
Any sync code that changes runtime ThreadLocal or context flags, including sendSyncMessages, must restore the
previous value with try/finally on success, failure, and early return.
Rationale:
Leaked sync context can suppress required outbound messages, allow echo loops, or contaminate unrelated remote
requests on reused threads.
Source scope:
RemoteServerImpl.save(...), RemoteSyncImpl remote-apply paths, OASyncClient/OASyncServer callback paths,
OAThreadLocalService integration.
Related CODEX findings:
ThreadLocal restoration repeatedly reviewed; sendSyncMessages restoration is a package invariant area.
Suggested unit tests:
testRemoteSaveRestoresSendSyncMessagesOnException(), testRemoteApplyDoesNotLeakSendSyncMessages(),
testRemoteThreadDoesNotLeakSyncContextBetweenRequests()
Spec target section:
Sync Runtime / ThreadLocal State

SYNC-REMOTE-001 — Transport Versus Semantic Success
Contract statement:
Sync must distinguish transport delivery, remote invocation completion, and synchronized graph semantic success;
lower-level delivery success alone must not be reported as successful graph synchronization.
Rationale:
A message can reach the remote JVM while the remote apply, datasource operation, callback, cache reconciliation, or
Hub mutation fails.
Source scope:
OASyncClient.getRemoteSync(), OASyncClient.getRemoteServer(), RemoteSyncImpl boolean-return methods,
RemoteServerImpl.save(...), RemoteClientImpl.refresh(...), RemoteDataSource, remote multiplexer integration.
Related CODEX findings:
Ignored remote boolean returns, swallowed remote exceptions, false-success return paths.
Suggested unit tests:
testTransportSuccessRemoteApplyFailureIsVisible(), testClientHubAddFailsVisiblyWhenServerRejectsAdd(),
testRemoteInvocationFailureDoesNotCommitLocalSuccess()
Spec target section:
Sync Runtime / Remote Boundary Semantics

SYNC-FAIL-001 — Failure Visibility
Contract statement:
Failed sync send, receive, apply, reconnect, shutdown, detail-load, save, delete, file transfer, or cache
reconciliation must be caller-visible or operationally observable; sync must not silently report success for
incomplete distributed work.
Rationale:
Silent false-success is a primary cause of distributed object graph divergence and production data inconsistency.
Source scope:
OASyncClient.getDetail(...), OASyncClient.uploadFile(...), OASyncClient.downloadFile(...),
RemoteServerImpl.save(...), RemoteClientImpl.refresh(...), RemoteClientImpl.createCopy(...), RemoteDataSource,
ClientFile, ServerFile.
Related CODEX findings:
getDetail returns null after exception, save false ignored, refresh no-op, createCopy null on cache miss, file
transfer partial overwrite.
Suggested unit tests:
testFailedRemoteDetailLoadDoesNotReturnLegitimateNull(), testFailedRemoteSaveDoesNotAppearSuccessful(),
testFailedFileDownloadDoesNotCommitReplacementFile()
Spec target section:
Sync Runtime / Failure Semantics

SYNC-PARTIAL-001 — Partial Progress Boundaries
Contract statement:
When a sync operation makes partial progress before failure, the incomplete state must be visible through failure
signaling, observable diagnostics, or explicit recovery state; it must not masquerade as fully committed
synchronized graph state.
Rationale:
OA does not require every distributed operation to be atomic, but incomplete distributed work must be detectable and
recoverable.
Source scope:
RemoteSyncImpl, RemoteServerImpl, RemoteClientImpl, RemoteDataSource, OASyncClient.start(),
OASyncClient.stop(boolean), ClientFile, ServerFile.
Related CODEX findings:
Partial startup cleanup, partial file replacement, swallowed remote exceptions, cache-state precommit before
successful delivery.
Suggested unit tests:
testPartialRemoteApplyFailureIsVisible(), testPartialClientStartupDoesNotExposeStartedState(),
testPartialFileUploadDoesNotReplaceCommittedFile()
Spec target section:
Sync Runtime / Partial Progress

SYNC-RECONNECT-001 — Reconnect and Retry Correctness
Contract statement:
Reconnect and retry must use fresh session, proxy, callback, GUID-tracking, retention, and worker state; they must
not reuse corrupted or stale state from a failed connection.
Rationale:
Reconnect is normal in distributed OA deployments and must recover without losing queued semantic work or applying
stale session assumptions.
Source scope:
OASyncClient.start(), OASyncClient.stop(boolean), OASyncClient.startDistributedGCThread(),
OASyncClient.startObjectsWithoutHubsThread(), OASyncServer.onClientConnect(), OASyncServer.onClientDisconnect(),
RemoteSessionImpl.hmGuid.
Related CODEX findings:
Stale proxies, static maps, stale worker RemoteSessionInterface, dead worker queue, old remote data source state.
Suggested unit tests:
testReconnectDoesNotUseOldRemoteDataSourceState(), testReconnectDoesNotDropQueuedRetentionUpdates(),
testDistributedGCWorkerUsesCurrentSessionAfterReconnect()
Spec target section:
Sync Runtime / Retry and Reconnect

SYNC-WORKER-001 — Background Worker Lifecycle
Contract statement:
Sync background workers for distributed GC, no-Hub retention, update polling, and background data load must start,
stop, recover, and fail visibly according to sync lifecycle state; duplicate starts must not create duplicate
authoritative workers for the same runtime role.
Rationale:
These workers affect sync correctness, not only performance. Lost or duplicated workers can lose retention updates,
leak stale GUIDs, or amplify load.
Source scope:
OASyncClient.startDistributedGCThread(), OASyncClient.startObjectsWithoutHubsThread(),
OASyncClient.startUpdateThread(...), OASyncServer.startUpdateThread(...),
OASyncServer.startLoadDataInBackgroundThread().
Related CODEX findings:
No-hub worker exits with live queue, stale cached RemoteSessionInterface, repeated start creates duplicate server
background workers.
Suggested unit tests:
testNoHubWorkerRestartsOrFailsVisibleAfterRepeatedRemoteErrors(),
testRepeatedServerStartDoesNotCreateDuplicateLoadThreads(), testDistributedGCWorkerFlushesTrailingFinalizedGuids()
Spec target section:
Sync Runtime / Worker Lifecycle

SYNC-FILE-001 — Sync File Transfer Commit Semantics
Contract statement:
Sync file upload/download operations must not expose partially transferred files as committed output; existing files
must not be deleted or overwritten until the transfer status and required bytes are complete.
Rationale:
File transfer supports distributed runtime artifacts and must preserve I/O correctness across disconnect, EOF, and
filesystem failure.
Source scope:
OASyncClient.uploadFile(...), OASyncClient.downloadFile(...), ClientFile.download(...), ClientFile.upload(...),
ServerFile.downloadFile(...), ServerFile.uploadFile(...).
Related CODEX findings:
Client download deletes local file before server status, server upload overwrites before full receipt.
Suggested unit tests:
testFailedDownloadDoesNotDeleteExistingLocalFile(), testFailedUploadDoesNotOverwriteExistingServerFile(),
testInterruptedFileTransferReportsFailure()
Spec target section:
Sync Runtime / File Transfer Commit Semantics

SYNC-FILE-002 — Sync File Resource Cleanup
Contract statement:
Sockets, streams, files, and transfer resources opened by sync file operations must be closed on success, failure,
cancellation, disconnect, and shutdown unless ownership is explicitly transferred.
Rationale:
Leaked file-transfer resources can block sync workers, leak descriptors, and degrade distributed runtime
reliability.
Source scope:
ClientFile.download(...), ClientFile.upload(...), ServerFile.downloadFile(...), ServerFile.uploadFile(...),
ServerFile.stop().
Related CODEX findings:
Stream cleanup on transfer failure, stop closes sockets in one try block.
Suggested unit tests:
testClientFileDownloadClosesSocketOnReadFailure(), testServerFileStopClosesBothSocketsWhenOneCloseFails(),
testUploadFailureClosesInputAndNetworkStreams()
Spec target section:
Sync Runtime / File Resource Cleanup

SYNC-REPLICATION-001 — Live Sync Versus Replication Boundary
Contract statement:
com.viaoa.sync defines connected real-time synchronization semantics; offline, durable, or replay/merge replication
semantics must be explicitly delegated to replication services and must not be assumed from transport delivery
alone.
Rationale:
Live sync and eventual/offline replication have different ordering, durability, retry, and merge contracts.
Conflating them causes false correctness assumptions.
Source scope:
OASyncClient, OASyncServer, RemoteSyncImpl, RemoteServerImpl, RemoteClientImpl, integration boundaries with
com.viaoa.remote, com.viaoa.comm, com.viaoa.replication, com.viaoa.transaction.
Related CODEX findings:
Existing notes distinguish replay/resync risks and cross-package identity/cache/datasource/remote/session
invariants.
Suggested unit tests:
testLiveSyncDoesNotClaimDurableReplicationCommit(),
testReplicationReplayUsesExplicitReplayContractNotLiveTransportSuccess()
Spec target section:
Sync Runtime / Sync-Replication Boundary

SYNC-INTEGRATION-001 — Cross-Package Runtime Compatibility
Contract statement:
Sync behavior must remain compatible with OA object, Hub, cache, graph, transaction, datasource, remote, comm,
serialization, trigger, and replication contracts; sync may coordinate those packages but must not redefine their
ownership or semantic authority.
Rationale:
Sync is cross-cutting distributed runtime infrastructure. Correctness depends on preserving package-specific
authority boundaries while coordinating live graph state.
Source scope:
com.viaoa.sync., com.viaoa.sync.remote., com.viaoa.object., com.viaoa.hub., com.viaoa.cache., com.viaoa.graph.,
com.viaoa.transaction., com.viaoa.datasource., com.viaoa.remote., com.viaoa.comm., com.viaoa.serialize.,
com.viaoa.trigger., com.viaoa.replication.*.
Related CODEX findings:
Many sync findings map to identity/cache/datasource/remote/session invariants rather than standalone sync-only
behavior.
Suggested unit tests:
testSyncRemoteSerializeCacheIdentityRoundTrip(), testSyncDatasourceSelectHubMembershipConsistency(),
testSyncAppliedMutationPreservesTriggerAndEventContracts()
Spec target section:
Sync Runtime / Cross-Package Contracts

*/