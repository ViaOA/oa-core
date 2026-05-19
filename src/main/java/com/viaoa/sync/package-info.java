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

/* CODEX Invariants

1. Sync Runtime Contracts

  ID: SYNC-RUNTIME-001
  Contract statement: com.viaoa.sync is the client/server synchronization runtime for propagating OAObject, Hub,
  datasource, detail-load, cache, and lifecycle changes between OA runtimes.
  Rationale: OG correctness depends on sync being the distributed extension of local object graph semantics, not a
  separate data model.
  Source locations: OASyncClient, OASyncServer, RemoteSyncInterface, RemoteSyncImpl, RemoteServerInterface,
  RemoteServerImpl, RemoteClientInterface, RemoteClientImpl, RemoteSessionInterface, RemoteSessionImpl.
  Related CODEX findings: stale remote proxies after reconnect; partial startup cleanup; stale session state.
  Suggested unit tests: testSyncClientStartCreatesRequiredRemoteInterfaces(),
  testSyncServerStartPublishesRequiredRemoteEndpoints()
  Spec target section: Sync Runtime / Core Responsibilities

  ID: SYNC-ORDER-001
  Contract statement: Sync messages that mutate OAObject or Hub state must be delivered and applied in deterministic
  order where local OA semantics depend on ordering.
  Rationale: Property changes, Hub add/remove/move, delete, and refresh operations are order-sensitive. Reordering can
  corrupt Hub membership or object lifecycle state.
  Source locations: OASyncServer.SyncQueueName, OASyncServer.QueueSize, RemoteSyncInterface, RemoteClientInterface,
  RemoteServerInterface, OASyncServer.getRemoteMultiplexerServer().
  Related CODEX findings: queue bypass/return-on-queue paths reviewed; CS authoritative ordering accepted.
  Suggested unit tests: testPropertyChangeOrderingIsPreserved(), testHubAddMoveRemoveOrderingIsPreservedAcrossSync()
  Spec target section: Sync Runtime / Message Ordering

  ID: SYNC-DELIVERY-001
  Contract statement: A committed local change that is eligible for sync must either be sent to the authoritative
  endpoint or fail visibly.
  Rationale: Silent message loss causes client/server divergence and breaks replication foundations.
  Source locations: OASyncClient.objectCreated, OASyncClient.updateObjectsWithoutHubs, RemoteSyncImpl.propertyChange,
  RemoteSyncImpl.addToHub, RemoteSyncImpl.removeFromHub, RemoteServerImpl.save.
  Related CODEX findings: swallowed objectCreated failure; background no-hub worker death; remote save false-success
  path.
  Suggested unit tests: testObjectCreatedRemoteFailureIsNotSilentlyLost(),
  testNoHubUpdateWorkerFailureDoesNotAcceptUnprocessedWork()
  Spec target section: Sync Runtime / Delivery Semantics

  ID: SYNC-DELIVERY-002
  Contract statement: Sync delivery success must mean the message reached the intended active session/endpoint, not a
  stale proxy or old connection.
  Rationale: Reconnect and session reuse are normal OA operations; stale endpoints can silently lose or misapply
  state.
  Source locations: OASyncClient.stop, OASyncClient.getRemoteServer, OASyncClient.getRemoteSession,
  OASyncClient.getRemoteClient, OASyncClient.getRemoteSync, OASyncServer.getRemoteSession,
  OASyncServer.getRemoteClient.
  Related CODEX findings: stale proxies after reconnect; worker-cached RemoteSessionInterface.
  Suggested unit tests: testReconnectUsesFreshRemoteSession(), testBackgroundWorkersUseCurrentSessionAfterReconnect()
  Spec target section: Sync Runtime / Reconnect Delivery

  2. Message Identity / Replay / Resync Contracts

  ID: SYNC-MSG-001
  Contract statement: Sync messages must identify target objects by stable OA identity sufficient to resolve the
  intended object on the receiver.
  Rationale: Remote sync applies state by class, OAObjectKey, GUID, property, and Hub path. Incorrect identity creates
  duplicate or wrong-object mutation.
  Source locations: RemoteSyncInterface.propertyChange, addToHub, insertInHub, removeFromHub, removeAllFromHub,
  serverDelete, clientDelete; RemoteSyncImpl.getObject; RemoteServerImpl.getObject; RemoteDataSource.getObject;
  ClientGetDetail.getDetail.
  Related CODEX findings: datasource/cache reload paths not preserving requested GUID.
  Suggested unit tests: testPropertyChangeTargetsObjectByGuidIdentity(),
  testRemoteReloadPreservesRequestedObjectGuid()
  Spec target section: Sync Runtime / Message Identity

  ID: SYNC-REPLAY-001
  Contract statement: Replay or resync must preserve object identity, Hub membership, lifecycle state, and event
  order.
  Rationale: Replication PoC and reconnect recovery depend on resync producing the same semantic graph state as live
  ordered sync.
  Source locations: RemoteSyncImpl, RemoteClientImpl.refresh, ClientGetDetail, RemoteDataSource,
  RemoteSessionImpl.hmGuid.
  Related CODEX findings: refresh cache-only no-op; stale GUID finalization; detail-load GUID drift.
  Suggested unit tests: testResyncPreservesGuidAndHubMembership(), testReplayAppliesDeleteAfterPriorHubRemoveInOrder()
  Spec target section: Sync Runtime / Replay and Resync

  ID: SYNC-RESYNC-001
  Contract statement: Reconnect must create a clean session identity and must not inherit stale per-session GUID,
  retention, callback, or remote proxy state.
  Rationale: Sync filtering and retention are session-specific. Reuse across sessions causes false filtering and stale
  server assumptions.
  Source locations: OASyncClient.stop, OASyncClient.start, RemoteSessionImpl.hmGuid,
  RemoteSessionImpl.hmObjectsWithoutHubs, OASyncServer.hmClientInfoExt.
  Related CODEX findings: static client maps; stale remote proxies; stale background worker session.
  Suggested unit tests: testReconnectDoesNotReuseOldGuidTracking(), testReconnectDoesNotReuseOldRemoteCallback()
  Spec target section: Sync Runtime / Reconnect Semantics

  3. Originator / Echo Contracts

  ID: SYNC-ECHO-001
  Contract statement: A remote-applied sync message must not echo outbound sync messages unless the operation
  explicitly requires rebroadcast.
  Rationale: Echo loops can duplicate Hub/object events and produce divergent state under client/server fan-out.
  Source locations: RemoteSyncImpl, RemoteServerImpl.save, OAThreadLocalService.sendSyncMessages use through sync
  services, OASyncServer.shouldSendSyncMessageToClient.
  Related CODEX findings: sendSyncMessages restoration reviewed; remote replay/broadcast echo noted as invariant area.
  Suggested unit tests: testClientAppliedServerPropertyChangeDoesNotEchoBack(),
  testServerAppliedClientDeleteDoesNotEchoToOriginatorUnexpectedly()
  Spec target section: Sync Runtime / Originator Filtering

  ID: SYNC-ECHO-002
  Contract statement: Originator filtering must suppress only the sender’s redundant echo, not legitimate downstream
  delivery to other clients.
  Rationale: Over-filtering causes missed updates; under-filtering causes duplicate application.
  Source locations: OASyncServer.shouldSendSyncMessageToClient, RemoteSessionImpl.hmGuid, ClientGetDetail.hmGuid,
  RemoteClientImpl.getRemoteDataSource().setCached.
  Related CODEX findings: hmGuid can lie after failed setCached; stale finalization can remove reloaded GUID.
  Suggested unit tests: testOriginatorDoesNotReceiveDuplicateHubAdd(),
  testNonOriginatingClientReceivesEligibleHubAdd()
  Spec target section: Sync Runtime / Client Filtering

  4. Identity / Cache Contracts

  ID: SYNC-IDENTITY-001
  Contract statement: GUID identity, primary-key identity, and business-key identity must remain distinct during sync,
  datasource reload, detail load, and remote lookup.
  Rationale: OA uses GUID runtime identity and object keys for distributed object graph consistency. Conflating
  identities creates duplicate objects or wrong sync routing.
  Source locations: RemoteServerImpl.getObject, RemoteServerImpl.getObjectUsingPkey, RemoteSyncImpl.getObject,
  RemoteClientImpl.getObject, RemoteDataSource.getObject, ClientGetDetail.getDetail.
  Related CODEX findings: original GUID reassignment missing on datasource reload paths.
  Suggested unit tests: testSyncReloadByPrimaryKeyDoesNotChangeGuidIdentity(),
  testDetailLoadByObjectKeyPreservesGuidIdentity()
  Spec target section: Sync Runtime / Identity Semantics

  ID: SYNC-CACHE-001
  Contract statement: Server-side cache misses must not turn valid sync operations into silent no-ops when datasource/
  key resolution can recover the target object.
  Rationale: OA caches can be weak/transient; sync authority must be key/datasource aware where the operation is
  authoritative.
  Source locations: RemoteServerImpl.save, RemoteClientImpl.refresh, RemoteClientImpl.createCopy,
  RemoteSessionImpl.setLock, RemoteSessionImpl.updateObjectsWithoutHubs.
  Related CODEX findings: save cache-only false-success; refresh cache-only no-op; createCopy cache-only null; lock
  cache-only false.
  Suggested unit tests: testRemoteSaveLoadsObjectAfterServerCacheMiss(),
  testRemoteLockLoadsObjectAfterServerCacheMiss()
  Spec target section: Sync Runtime / Cache and Datasource Recovery

  ID: SYNC-CACHE-002
  Contract statement: hmGuid must accurately represent objects currently known to exist on the client.
  Rationale: It controls message filtering, reference-vs-full serialization, retention, and resync behavior.
  Source locations: RemoteSessionImpl.hmGuid, RemoteSessionImpl.objectCreated, RemoteSessionImpl.objectsFinalized,
  ClientGetDetail.afterSerialize, RemoteClientImpl.setCached.
  Related CODEX findings: stale finalized GUID removal; setCached registers GUID before successful retention;
  objectCreated failure.
  Suggested unit tests: testGuidMapOnlyRecordsSuccessfullyDeliveredObjects(),
  testStaleFinalizationDoesNotRemoveReloadedGuid()
  Spec target section: Sync Runtime / Client Object Tracking

  ID: SYNC-CACHE-003
  Contract statement: Objects retained because a client references them outside hubs must remain tracked until the
  client releases or finalizes them.
  Rationale: Client-held objects can otherwise be lost from server retention/save-cache behavior.
  Source locations: OASyncClient.updateObjectsWithoutHubs, RemoteSessionImpl.updateObjectsWithoutHubs,
  RemoteSessionImpl.saveCache, RemoteSessionImpl.clearCaches.
  Related CODEX findings: no-hub worker death; cache-miss no-op in server retention; static no-hub map across
  sessions.
  Suggested unit tests: testClientHeldObjectOutsideHubIsRetainedOnServer(),
  testNoHubRetentionSurvivesServerCacheEviction()
  Spec target section: Sync Runtime / Retention Semantics

  5. Object / Hub Sync Contracts

  ID: SYNC-HUB-001
  Contract statement: Hub add, insert, remove, move, sort, refresh, and clear-change messages must preserve local Hub
  semantics on the receiver.
  Rationale: Hubs are the core OA collection and relationship abstraction. Remote application must behave like the
  same local operation.
  Source locations: RemoteSyncInterface, RemoteSyncImpl.addToHub, insertInHub, removeFromHub, removeAllFromHub,
  moveObjectInHub, sort, refresh, clearHubChanges.
  Related CODEX findings: ignored remote boolean returns in Hub CS paths; removeAll unloaded state issue.
  Suggested unit tests: testRemoteHubAddMatchesLocalHubAddSemantics(), testRemoteHubMovePreservesOrder()
  Spec target section: Sync Runtime / Hub Semantics

  ID: SYNC-HUB-002
  Contract statement: Hub messages must not be filtered out for a client that has the relevant object and loaded Hub
  state.
  Rationale: Incorrect filtering silently creates divergent Hub contents.
  Source locations: OASyncServer.shouldSendSyncMessageToClient, RemoteSessionImpl.hmGuid,
  ClientGetDetail.wasFullySentToClient.
  Related CODEX findings: cache miss in add/insert filtering; stale hmGuid states.
  Suggested unit tests: testLoadedClientReceivesHubInsertWhenMasterKnown(),
  testUnloadedClientDoesNotReceiveUnusableHubMutation()
  Spec target section: Sync Runtime / Hub Message Filtering

  ID: SYNC-HUB-003
  Contract statement: Remote Hub operations that fail authoritatively must not be treated as local success by the
  caller.
  Rationale: False success causes client/server Hub divergence.
  Source locations: RemoteSyncImpl return values; HubCSService remote-call integration; RemoteClientImpl.deleteAll.
  Related CODEX findings: ignored return values for add/insert/remove/move/sort/deleteAll.
  Suggested unit tests: testClientHubAddFailsVisiblyWhenServerRejectsAdd(),
  testClientDeleteAllDoesNotReturnSuccessWhenServerDeleteAllFails()
  Spec target section: Sync Runtime / Hub Failure Semantics

  ID: SYNC-EVENT-001
  Contract statement: Sync-applied object and Hub mutations must produce the same committed event semantics as local
  operations, without duplicate or missing events.
  Rationale: OA listeners, UI, query/filter, cache, and replication hooks depend on event correctness.
  Source locations: RemoteSyncImpl.propertyChange, RemoteSyncImpl.addToHub, RemoteSyncImpl.removeFromHub,
  RemoteSyncImpl.serverDelete, RemoteSyncImpl.clientDelete.
  Related CODEX findings: event ordering and delete sync paths reviewed in graph/hub scans.
  Suggested unit tests: testRemotePropertyChangeFiresOnePropertyEvent(),
  testRemoteHubRemoveFiresExpectedHubEventOrder()
  Spec target section: Sync Runtime / Event Semantics

  6. ThreadLocal / Context Contracts

  ID: SYNC-TL-001
  Contract statement: Code that changes sendSyncMessages must restore the previous value with try/finally.
  Rationale: A leaked sync-send flag can either suppress required messages or echo remote-applied changes.
  Source locations: RemoteServerImpl.save, graph/object/hub services that invoke sync; OAThreadLocalService.
  Related CODEX findings: thread-local restoration repeatedly reviewed; no new unbalanced path in final sync pass.
  Suggested unit tests: testRemoteSaveRestoresSendSyncMessagesOnException(),
  testRemoteApplyDoesNotLeakSendSyncMessages()
  Spec target section: Sync Runtime / ThreadLocal State

  ID: SYNC-TL-002
  Contract statement: Remote-thread baseline reset is not a substitute for balanced cleanup by sync code that changes
  thread-local state.
  Rationale: Remote worker reuse must not carry sync context from one request into another.
  Source locations: RemoteSyncInterface.serverDelete(runInRemoteThread=true), remote multiplexer callbacks, runtime
  thread services.
  Related CODEX findings: accepted runtime invariant from earlier runtime/remote scans.
  Suggested unit tests: testRemoteThreadDoesNotLeakSyncContextBetweenRequests()
  Spec target section: Sync Runtime / Remote Thread Context

  7. Connection / Session Lifecycle Contracts

  ID: SYNC-SESSION-001
  Contract statement: A connected client must have exactly one active server-side session identity for sync filtering,
  callbacks, locks, and retention.
  Rationale: Multiple or missing session identities corrupt per-client state.
  Source locations: OASyncServer.onClientConnect, OASyncServer.getRemoteSession, OASyncServer.getRemoteClient,
  RemoteSessionImpl, ClientInfo.
  Related CODEX findings: client startup can accept null remote session/client; session creation ordering window.
  Suggested unit tests: testClientConnectCreatesSingleSessionState(),
  testClientStartFailsWhenSessionLookupReturnsNull()
  Spec target section: Sync Runtime / Session Lifecycle

  ID: SYNC-SESSION-002
  Contract statement: Disconnect cleanup must clear locks, callbacks, transient proxies, and client-retention state
  without throwing from partially initialized sessions.
  Rationale: Clients can disconnect during startup, reconnect, or failure. Cleanup must be robust under normal
  lifecycle timing.
  Source locations: OASyncServer.onClientDisconnect, RemoteSessionImpl.clearLocks, RemoteClientImpl.close,
  OASyncClient.stop.
  Related CODEX findings: null remoteSession on disconnect; stale proxies; iterator leak on close.
  Suggested unit tests: testDisconnectBeforeRemoteSessionCreationDoesNotThrow(),
  testDisconnectClearsClientCallbackAndLocks()
  Spec target section: Sync Runtime / Disconnect Cleanup

  ID: SYNC-SESSION-003
  Contract statement: Server and client lifecycle flags must reflect committed startup/shutdown state.
  Rationale: Monitoring, admin, test readiness, and reconnect logic rely on lifecycle state being truthful.
  Source locations: OASyncClient.start, OASyncClient.stop, OASyncServer.start, OASyncServer.stop, ClientInfo.started,
  ServerInfo.started.
  Related CODEX findings: partial client startup cleanup; ServerInfo.started not updated; partial server startup
  cleanup.
  Suggested unit tests: testClientStartedFalseAfterFailedStartupCleanup(),
  testServerInfoStartedTracksServerLifecycle()
  Spec target section: Sync Runtime / Lifecycle State

  ID: SYNC-SESSION-004
  Contract statement: Session counters and session maps must remain consistent with actual remote multiplexer
  sessions.
  Rationale: Admin and lifecycle decisions depend on accurate connected-session state.
  Source locations: OASyncServer.aiSessionCount, OASyncServer.getRemoteMultiplexerServer().createSession,
  removeSession, hmClientInfoExt.
  Related CODEX findings: unconditional session-count decrement; stale ClientInfo after disconnect.
  Suggested unit tests: testDuplicateRemoveSessionDoesNotDecrementSessionCountTwice(),
  testLateClientUpdateDoesNotClearDisconnectTimestamp()
  Spec target section: Sync Runtime / Session Accounting

  8. Failure / Retry Contracts

  ID: SYNC-FAIL-001
  Contract statement: Failed sync apply must be caller-visible or operationally visible; it must not silently appear
  successful.
  Rationale: Silent false-success is the fastest path to distributed divergence.
  Source locations: OASyncClient.getDetail, RemoteServerImpl.save, RemoteClientImpl.refresh,
  RemoteClientImpl.createCopy, RemoteDataSource.datasource, RemoteSyncImpl boolean return methods.
  Related CODEX findings: getDetail returns null after exception; save false ignored; refresh no-op; createCopy null
  on cache miss.
  Suggested unit tests: testFailedRemoteDetailLoadDoesNotReturnLegitimateNull(),
  testFailedRemoteSaveDoesNotAppearSuccessful()
  Spec target section: Sync Runtime / Failure Visibility

  ID: SYNC-RETRY-001
  Contract statement: Retry/reconnect must not duplicate state, skip required messages, or continue using stale
  session/proxy state.
  Rationale: Normal OA deployments must tolerate disconnect/reconnect without corrupting graph state.
  Source locations: OASyncClient.start, OASyncClient.stop, background GC/no-hub workers, RemoteSessionImpl.hmGuid,
  RemoteClientImpl.remoteDataSource.
  Related CODEX findings: stale proxies; static maps; stale worker rsi; dead worker queue.
  Suggested unit tests: testReconnectDoesNotDropQueuedRetentionUpdates(),
  testReconnectDoesNotUseOldRemoteDataSourceState()
  Spec target section: Sync Runtime / Retry and Reconnect

  ID: SYNC-FAIL-002
  Contract statement: Partial progress is allowed only when the caller or operations channel can detect incomplete
  sync work.
  Rationale: OA does not require every operation to be atomic, but incomplete distributed work must not masquerade as
  committed.
  Source locations: RemoteSyncImpl, RemoteServerImpl, RemoteClientImpl, RemoteDataSource, ClientFile, ServerFile.
  Related CODEX findings: file transfer partial overwrite; swallowed remote exceptions; false-success return paths.
  Suggested unit tests: testPartialRemoteApplyFailureIsVisible(), testFailedFileDownloadDoesNotCommitReplacementFile()
  Spec target section: Sync Runtime / Partial Progress

  9. Queue / Worker / Concurrency Contracts

  ID: SYNC-CONCURRENT-001
  Contract statement: Background sync workers must not permanently stall delivery after transient remote failures.
  Rationale: Distributed GC, no-hub retention, and update workers are part of sync correctness, not just diagnostics.
  Source locations: OASyncClient.startDistributedGCThread, OASyncClient.startObjectsWithoutHubsThread,
  OASyncClient.startUpdateThread, OASyncServer.startLoadDataInBackgroundThread.
  Related CODEX findings: no-hub worker exits with live queue; stale cached RemoteSessionInterface; repeated start
  creates duplicate server background workers.
  Suggested unit tests: testNoHubWorkerRestartsOrFailsVisibleAfterRepeatedRemoteErrors(),
  testDistributedGCWorkerUsesCurrentSessionAfterReconnect()
  Spec target section: Sync Runtime / Background Workers

  ID: SYNC-CONCURRENT-002
  Contract statement: Concurrent session updates, disconnects, and remote calls must not expose stale or half-removed
  session state as active.
  Rationale: Client disconnects and in-flight remote calls are normal. Sync state must remain deterministic.
  Source locations: OASyncServer.hmClientInfoExt, OASyncServer.onUpdate, OASyncServer.onClientDisconnect,
  OASyncServer.getRemoteSession, OASyncServer.getRemoteClient.
  Related CODEX findings: late update clears disconnect timestamp; remote facade lookup after disconnect NPE.
  Suggested unit tests: testLateClientUpdateAfterDisconnectDoesNotReactivateSession(),
  testLateRemoteLookupAfterDisconnectReturnsFailureNotNpe()
  Spec target section: Sync Runtime / Concurrent Session State

  ID: SYNC-CONCURRENT-003
  Contract statement: Sync filtering state must be updated only after the underlying object delivery/retention
  operation succeeds.
  Rationale: Precommitting filter state causes server to believe clients have objects they did not receive.
  Source locations: RemoteClientImpl.getRemoteDataSource().setCached, ClientGetDetail.afterSerialize,
  RemoteSessionImpl.objectCreated.
  Related CODEX findings: hmGuid.putIfAbsent before updateObjectCache; stale objectCreated state.
  Suggested unit tests: testGuidNotRegisteredBeforeSuccessfulObjectDelivery(),
  testSerializerGuidStateMatchesActuallySerializedObjects()
  Spec target section: Sync Runtime / Concurrent Filter State

  10. File Sync Contracts

  ID: SYNC-FILE-001
  Contract statement: File download/upload must not replace an existing durable file until the transfer has completed
  successfully.
  Rationale: File sync is internal OA transport, but ordinary disconnect or filesystem failure must not corrupt
  existing files.
  Source locations: ClientFile.download, ClientFile.upload, ServerFile.downloadFile, ServerFile.uploadFile.
  Related CODEX findings: client download deletes local file before server status; server upload overwrites before
  full receipt.
  Suggested unit tests: testFailedDownloadDoesNotDeleteExistingLocalFile(),
  testFailedUploadDoesNotOverwriteExistingServerFile()
  Spec target section: Sync Runtime / File Transfer Semantics

  ID: SYNC-FILE-002
  Contract statement: File transfer sockets and streams must be closed on success and failure.
  Rationale: Leaked file sockets can block transfer workers and degrade sync runtime reliability.
  Source locations: ClientFile.download, ClientFile.upload, ServerFile.downloadFile, ServerFile.uploadFile,
  ServerFile.stop.
  Related CODEX findings: stream cleanup on transfer failure; stop closes sockets in one try block.
  Suggested unit tests: testClientFileDownloadClosesSocketOnReadFailure(),
  testServerFileStopClosesBothSocketsWhenOneCloseFails()
  Spec target section: Sync Runtime / File Resource Cleanup

  11. Cross-Package Compatibility Contracts

  ID: SYNC-CROSS-001
  Contract statement: Sync behavior must remain compatible with remote request/response correlation, queue delivery,
  serialization identity, cache uniqueness, object lifecycle, Hub semantics, datasource authority, and runtime role
  contracts.
  Rationale: Sync is a cross-cutting distributed runtime layer, not an isolated package.
  Source locations: com.viaoa.sync.*, com.viaoa.remote.*, com.viaoa.queue.*, com.viaoa.serialize.*, com.viaoa.cache.*,
  com.viaoa.object.*, com.viaoa.hub.*, com.viaoa.datasource.*, com.viaoa.runtime.*.
  Related CODEX findings: many sync bugs map to identity/cache/datasource/remote/session invariants.
  Suggested unit tests: testSyncRemoteSerializeCacheIdentityRoundTrip(),
  testSyncDatasourceSelectHubMembershipConsistency()
  Spec target section: Sync Runtime / Cross-Package Contracts

  ID: SYNC-ROLE-001
  Contract statement: Sync logic must distinguish actual server, actual client, and single-user runtime roles.
  Rationale: SingleUser is local runtime, not sync server. Misrouting can skip persistence or attempt remote behavior
  incorrectly.
  Source locations: graph sync service integration, OASyncClient, OASyncServer, RemoteSyncImpl, RemoteClientImpl,
  RemoteServerImpl.
  Related CODEX findings: role-semantics pass completed separately.
  Suggested unit tests: testSingleUserDoesNotUseRemoteSyncPaths(), testClientRoutesAuthoritativeChangesToServer()
  Spec target section: Sync Runtime / Role Semantics

  Test Coverage Matrix

  - Ordering: testPropertyChangeOrderingIsPreserved, testHubAddMoveRemoveOrderingIsPreservedAcrossSync
  - Delivery: testObjectCreatedRemoteFailureIsNotSilentlyLost, testFailedRemoteSaveDoesNotAppearSuccessful
  - Echo/originator: testClientAppliedServerPropertyChangeDoesNotEchoBack,
    testNonOriginatingClientReceivesEligibleHubAdd
  - Replay/resync: testResyncPreservesGuidAndHubMembership, testReconnectDoesNotReuseOldGuidTracking
  - Identity/cache: testRemoteReloadPreservesRequestedObjectGuid, testGuidMapOnlyRecordsSuccessfullyDeliveredObjects
  - Hub: testRemoteHubAddMatchesLocalHubAddSemantics, testRemoteHubMovePreservesOrder
  - ThreadLocal: testRemoteSaveRestoresSendSyncMessagesOnException,
    testRemoteThreadDoesNotLeakSyncContextBetweenRequests
  - Session lifecycle: testClientConnectCreatesSingleSessionState,
    testDisconnectBeforeRemoteSessionCreationDoesNotThrow
  - Failure/retry: testFailedRemoteDetailLoadDoesNotReturnLegitimateNull,
    testReconnectDoesNotUseOldRemoteDataSourceState
  - File transfer: testFailedDownloadDoesNotDeleteExistingLocalFile,
    testFailedUploadDoesNotOverwriteExistingServerFile
  - Cross-package: testSyncRemoteSerializeCacheIdentityRoundTrip, testSyncDatasourceSelectHubMembershipConsistency

  Suggested Package-Level Spec Summary

  - com.viaoa.sync owns OA client/server object graph synchronization.
  - It propagates OAObject property changes, Hub mutations, deletes, refreshes, datasource requests, detail loads, and
    retained-object state.
  - It must preserve deterministic ordering where object, Hub, lifecycle, or replication correctness depends on order.
  - It must never silently report success for failed remote sync, failed authoritative apply, failed detail load, or
    failed session routing.
  - It must preserve GUID identity separately from primary-key and business-key identity.
  - It must keep per-client session state truthful: GUID map, retained objects, callbacks, locks, remote proxies, and
    datasource iterators.
  - It must suppress unintended originator echoes without suppressing valid downstream sync.
  - It must restore thread-local sync state after remote/apply operations.
  - It assumes OA-controlled internal transport, but still must handle normal disconnect, reconnect, retry, and
    partial failure deterministically.
  - It is a foundation for replication, so sync invariants should be tested with multi-client ordering, reconnect,
    replay/resync, and cache-eviction scenarios.

*/
