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
 * Provides client-side implementations for OA's distributed data-source layer.
 * <p>
 * Classes in this package enable OA applications to access remote
 * {@link com.viaoa.datasource.OADataSource} instances hosted on OA servers.
 * Communication occurs via {@link com.viaoa.sync.remote.RemoteClientInterface}
 * and the OA synchronization framework.
 *
 * <h2>Key Component</h2>
 * <ul>
 *   <li>{@link com.viaoa.datasource.clientserver.OADataSourceClient} —
 *       client-side proxy for remote OADataSource operations.</li>
 * </ul>
 */
package com.viaoa.datasource.clientserver;

/* CODEX Invariants

DSCS-RUNTIME-001 — Client/Server Datasource Authority
Contract statement:
com.viaoa.datasource.clientserver defines the client-side datasource proxy contract for server-authoritative OA
datasource operations across distributed OA runtimes.
Rationale:
Client/server datasource behavior is a persistence and loading boundary, not just remote method plumbing. Client
calls must route to the authoritative server datasource while preserving OA metadata, identity, cache, query, and
lifecycle semantics.
Source scope:
OADataSourceClient, RemoteClientInterface.datasource(...), RemoteClientInterface.datasourceReturnOnQueue(...),
RemoteClientInterface.datasourceNoReturn(...), OARuntime OA/datasource integration.
Related CODEX findings:
OADataSourceClient registration path can create a datasource that is not discoverable through the runtime datasource
registry.
Suggested unit tests:
testClientDatasourceRoutesToRemoteClient(), testClientDatasourceRegisteredForRuntimeLookup(),
testServerAuthoritativeDatasourceBoundaryPreserved()
Spec target section:
Datasource Client/Server / Core Responsibility

DSCS-REGISTRY-001 — Runtime Datasource Registration
Contract statement:
A client/server datasource created for an OA package must be discoverable through the runtime datasource registry
for classes it supports, or the creation path must fail visibly before selectors/loaders depend on it.
Rationale:
OASelect, lazy loading, and datasource lookup depend on runtime datasource discovery. A constructed but unregistered
client datasource causes silent no-datasource behavior.
Source scope:
OADataSourceClient(String packageName), OADataSourceClient(), OARuntime.datasource() integration, OASyncService
client creation path.
Related CODEX findings:
OADataSourceClient registration path: constructor assigns packageName but no registration occurs; later
OARuntime.datasource().get(...) can return null.
Suggested unit tests:
testSyncClientCreationRegistersClientDatasource(), testOASelectFindsClientDatasourceAfterSyncClientStart(),
testUnregisteredClientDatasourceDoesNotSilentlyCancelSelect()
Spec target section:
Datasource Client/Server / Registration Semantics

DSCS-REMOTE-001 — Remote Client Resolution
Contract statement:
OADataSourceClient must resolve and use the active RemoteClientInterface for its OA package, and stale or missing
remote clients must fail visibly before datasource work is reported successful.
Rationale:
Datasource calls crossing process boundaries depend on the correct active remote endpoint. Stale or null endpoints
cause lost loads, saves, deletes, or metadata queries.
Source scope:
OADataSourceClient.packageName, getRemoteClient(), verifyConnection(), OARuntime.oa(packageName), OA sync
remote-client lookup.
Related CODEX findings:
Registration and remote routing risks; remote invocation false-success is a cross-package boundary concern.
Suggested unit tests:
testClientDatasourceUsesRemoteClientForPackage(), testMissingRemoteClientFailsBeforeDatasourceOperation(),
testReconnectUsesCurrentRemoteClient()
Spec target section:
Datasource Client/Server / Remote Endpoint Semantics

DSCS-CLASS-001 — Class Support Routing
Contract statement:
Class-specific datasource operations must be routed only to a datasource that supports the target class; unsupported
class-specific operations must not fall through to an unrelated default datasource.
Rationale:
Incorrect class routing can select, load, save, delete, or count the wrong model type on the server.
Source scope:
OADataSourceClient.isClassSupported(...), hashClass support cache, select(...), count(...), getMaxLength(...),
updateMany2ManyLinks(...), getPropertyBlobValue(...), remote datasource operation codes.
Related CODEX findings:
RemoteDataSource comment: class-specific routing must require class support; classless commands need separate
default behavior.
Suggested unit tests:
testUnsupportedClassSpecificOperationDoesNotFallbackToDefaultDatasource(), testClassSupportResultCachedPerClass(),
testFilterWithLocalSelectAllHubUsesLocalCacheByContract()
Spec target section:
Datasource Client/Server / Class Routing

DSCS-CLASSLESS-001 — Classless Datasource Commands
Contract statement:
Classless datasource commands such as availability, supports-storage, assign-id-on-create, and execute must have an
explicit default datasource routing rule distinct from class-specific routing.
Rationale:
Classless commands historically operate against the default/first datasource, while class-specific fallback can be
unsafe. These cases must not share ambiguous routing.
Source scope:
OADataSourceClient.isAvailable(), supportsStorage(), getAssignIdOnCreate(), execute(...), remote operation codes
IS_AVAILABLE, SUPPORTSSTORAGE, GET_ASSIGN_ID_ON_CREATE, EXECUTE; server-side remote datasource boundary.
Related CODEX findings:
RemoteDataSource.getDataSource(null) behavior can disable valid classless datasource commands after class-specific
fallback was removed.
Suggested unit tests:
testClasslessSupportsStorageUsesDefaultDatasource(), testClasslessIsAvailableUsesDefaultDatasource(),
testUnsupportedClassSpecificIsClassSupportedDoesNotUseDefaultDatasource()
Spec target section:
Datasource Client/Server / Classless Command Routing

DSCS-SELECT-001 — Remote Select Delegation
Contract statement:
Remote select and passthrough select must send deterministic query, parameter, order, where-object key, max, dirty,
and filter-presence metadata to the server, then expose results through an iterator whose result interpretation is
deterministic.
Rationale:
Remote selection bridges query/path metadata, datasource authority, object identity, and local cache hydration.
Source scope:
OADataSourceClient.select(...), selectPassthru(...), MyIterator, operation codes SELECT, SELECTPASSTHRU, IT_NEXT,
IT_REMOVE.
Related CODEX findings:
Registration-path issue can make OASelect see no datasource; class routing and false-success boundaries apply.
Suggested unit tests:
testRemoteSelectSendsWhereObjectClassAndKey(), testRemoteSelectReturnsIteratorForServerToken(),
testRemoteSelectNullTokenReturnsNoIteratorByContract()
Spec target section:
Datasource Client/Server / Select Semantics

DSCS-ITERATOR-001 — Remote Iterator Lifecycle
Contract statement:
Remote datasource iterators must fetch server batches deterministically, apply local filters consistently, update
local cache/read-ahead state for returned objects, and release local iterator resources on exhaustion, remove,
close, or failure.
Rationale:
Iterator state controls lazy loading and cache participation. Leaked or stale iterator state can retain OA runtime
objects or miss returned records.
Source scope:
OADataSourceClient.MyIterator, hasNext(), next(), getMoreFromServer(), remove(), close(), getSiblingHelper(),
ObjectCacheIterator fallback.
Related CODEX findings:
No direct CODEX finding in this package, but iterator lifecycle is part of remote datasource correctness.
Suggested unit tests:
testRemoteIteratorFetchesBatchesUntilServerExhausted(),
testRemoteIteratorAppliesLocalFilterWithoutSendingFilterObject(),
testRemoteIteratorCloseClearsReadAheadAndSiblingHelper()
Spec target section:
Datasource Client/Server / Iterator Lifecycle

DSCS-CACHE-001 — Client Cache Reconciliation
Contract statement:
Objects returned from remote datasource operations must reconcile with the client OA/cache authority before
becoming visible as loaded runtime objects.
Rationale:
Client/server loading must not create duplicate object instances or bypass cache identity rules.
Source scope:
OADataSourceClient.MyIterator.getMoreFromServer(), callObjectCSUpdateObjectsWithoutHubs(...), ObjectCacheIterator
fallback, MyIterator.next() key mode, callObjectCacheGet(...), remote server getObject(...) boundary.
Related CODEX findings:
Remote datasource and sync notes around cache/key consistency; no direct source CODEX beyond boundary concerns.
Suggested unit tests:
testRemoteIteratorUpdatesObjectsWithoutHubsForReturnedObjects(), testSingleKeyIteratorUsesExistingCachedObject(),
testRemoteLoadDoesNotCreateDuplicateCachedObject()
Spec target section:
Datasource Client/Server / Cache Reconciliation

DSCS-IDENTITY-001 — Object Key Transmission
Contract statement:
Client/server datasource operations that reference an existing OAObject must transmit the object’s class and
OAObjectKey, not a stale object instance identity, wherever the server must resolve authoritative datasource state.
Rationale:
Object identity must remain stable across process boundaries. Runtime object references are not authoritative across
JVMs.
Source scope:
OADataSourceClient.count(...), select(...), updateMany2ManyLinks(...), getPropertyBlobValue(...), MyIterator key
mode, OARuntime OA object-key services.
Related CODEX findings:
Identity/cache boundary concerns from remote datasource and sync reviews.
Suggested unit tests:
testCountTransmitsWhereObjectClassAndKey(), testManyToManyUpdateTransmitsMasterClassAndKey(),
testBlobLoadTransmitsObjectClassAndKey()
Spec target section:
Datasource Client/Server / Identity Semantics

DSCS-MUTATION-001 — Remote Mutation Delegation
Contract statement:
Client insert, insertWithoutReferences, update, save, delete, deleteAll, updateMany2ManyLinks, and assignId must
delegate to the server datasource authority with operation-specific completion semantics made explicit.
Rationale:
Mutating datasource operations affect persistence, cache, sync, and object lifecycle. Fire-and-queue behavior must
not be confused with committed server persistence.
Source scope:
OADataSourceClient.insert(...), insertWithoutReferences(...), update(...), save(...), delete(...), deleteAll(...),
updateMany2ManyLinks(...), assignId(...), datasourceReturnOnQueue(...), datasource(...).
Related CODEX findings:
False-success prevention requested for failed remote save/delete/assign-id; return-on-queue operations can blur
accepted versus committed semantics.
Suggested unit tests:
testInsertDelegatesToRemoteDatasource(), testSaveReturnOnQueueDoesNotClaimCommittedPersistence(),
testDeleteAllNullClassIsNoOpByContract()
Spec target section:
Datasource Client/Server / Mutation Semantics

DSCS-ASYNC-001 — Queued Operation Boundary
Contract statement:
Operations sent with datasourceReturnOnQueue must define whether the method return means queued, remotely received,
applied, or persisted; queued acceptance must not be reported as semantic datasource success unless explicitly
contracted.
Rationale:
Save, delete, deleteAll, and assignId may return before server-side persistence completes. Callers must not mistake
queue acceptance for durable mutation.
Source scope:
OADataSourceClient.save(...), delete(...), deleteAll(...), assignId(...),
RemoteClientInterface.datasourceReturnOnQueue(...).
Related CODEX findings:
User request highlights distinction between datasource operation success, remote invocation success, transport
success, and semantic OA runtime success.
Suggested unit tests:
testQueuedSaveReturnMeansQueuedNotCommitted(), testQueuedDeleteFailureIsObservableByOwnerContract(),
testAssignIdQueuedBoundaryIsExplicit()
Spec target section:
Datasource Client/Server / Queued Operation Semantics

DSCS-COUNT-001 — Remote Count Semantics
Contract statement:
Remote count and passthrough count must return the authoritative server result when available, and must use a
deterministic unavailable/failure sentinel only when the remote response is not a valid count by contract.
Rationale:
Counts drive UI, paging, query decisions, and runtime policies. Ambiguous -1 results must not be confused with valid
zero or successful count.
Source scope:
OADataSourceClient.count(...), countPassthru(...), operation codes COUNT, COUNTPASSTHRU.
Related CODEX findings:
False-success prevention for failed remote count is part of requested scope; no direct source CODEX.
Suggested unit tests:
testRemoteCountReturnsServerInteger(), testRemoteCountInvalidResponseReturnsUnavailableSentinelByContract(),
testRemoteCountFailureIsVisibleOrDistinguishable()
Spec target section:
Datasource Client/Server / Count Semantics

DSCS-METADATA-001 — Remote Metadata Cache Consistency
Contract statement:
Client-side caches of remote datasource metadata, including class support, max length, supports-storage, and assign-
id-on-create, must be safely published, scoped to the active remote datasource/session, and invalidated or refreshed
when the remote datasource authority changes.
Rationale:
Stale or racy metadata cache values can cause wrong validation, missing datasource support, wrong save behavior, or
incorrect generated-key lifecycle decisions.
Source scope:
OADataSourceClient.hashClass, hmMax, bCalledSupportsStorage, bSupportsStorage, bCalledGetAssignIdOnCreate,
bGetAssignIdOnCreate, getMaxLength(...), setMaxLength(...), isClassSupported(...), supportsStorage(),
getAssignIdOnCreate().
Related CODEX findings:
getMaxLength uses unsynchronized HashMap under concurrent access; remote-client/session lifecycle can make cached
metadata stale.
Suggested unit tests:
testConcurrentGetMaxLengthCacheIsThreadSafe(), testMetadataCacheScopedToRemoteDatasourceSession(),
testSupportsStorageCacheRefreshesOrDocumentsStalenessAfterReconnect()
Spec target section:
Datasource Client/Server / Metadata Cache Semantics

DSCS-FAIL-001 — Remote Failure Visibility
Contract statement:
Failed remote load, select, count, save, delete, update, assign-id, blob, metadata, or execute operations must be
caller-visible or operationally observable; transport success or null/false/sentinel values must not silently
masquerade as semantic success unless documented by the operation contract.
Rationale:
Client/server datasource failure can otherwise create missing loads, skipped saves, stale cache state, or false UI/
runtime conclusions.
Source scope:
All OADataSourceClient remote-call methods, verifyConnection(), getRemoteClient(), MyIterator.getMoreFromServer(),
RemoteClientInterface datasource methods.
Related CODEX findings:
Registration failure can cause silent select/no datasource behavior; classless datasource routing can return null
for valid commands; max-length cache races can produce stale metadata.
Suggested unit tests:
testMissingRemoteClientFailsDatasourceOperation(), testRemoteSelectFailureDoesNotAppearAsEmptySuccessfulSelect(),
testRemoteSaveFailureIsObservableByOwnerContract()
Spec target section:
Datasource Client/Server / Failure Semantics

DSCS-PARTIAL-001 — Partial Progress Boundary
Contract statement:
If a client/server datasource operation partially completes across client cache update, remote invocation, server
apply, queued execution, or iterator fetch, the incomplete boundary must be visible through failure state, iterator
state, diagnostics, retry state, or explicit contract.
Rationale:
Distributed datasource work can fail after local or remote side effects. Partial progress must not be hidden as
complete OA runtime success.
Source scope:
OADataSourceClient save/delete/assignId queued calls, select iterator fetching, updateMany2ManyLinks(...),
getPropertyBlobValue(...), remote datasource response handling.
Related CODEX findings:
Return-on-queue and remote/default routing issues illustrate ambiguous partial progress boundaries.
Suggested unit tests:
testIteratorFetchFailureLeavesIteratorIncompleteVisible(), testQueuedMutationFailureDoesNotAppearCommitted(),
testPartialRemoteBlobLoadDoesNotReturnValidBlob()
Spec target section:
Datasource Client/Server / Partial Progress

DSCS-NULL-001 — Null and Unsupported Input Semantics
Contract statement:
Null inputs and unsupported operations must have deterministic behavior: no-op, false, null, -1, or visible failure
according to the method contract, and must not mutate remote datasource state accidentally.
Rationale:
Client/server datasources are often called from generic runtime paths. Null handling must be predictable and not
hide unintended remote work.
Source scope:
OADataSourceClient.isClassSupported(...), insertWithoutReferences(...), insert(...), update(...), save(...),
delete(...), deleteAll(...), setMaxLength(...), select(...), count(...).
Related CODEX findings:
No direct source CODEX beyond false-success concerns.
Suggested unit tests:
testNullObjectMutationMethodsAreNoOpByContract(), testNullClassSupportReturnsFalse(),
testNullDeleteAllClassDoesNotCallRemote()
Spec target section:
Datasource Client/Server / Null and Unsupported Semantics

DSCS-BLOB-001 — Remote Blob Boundary
Contract statement:
Remote blob property loading must resolve the target object by class and OAObjectKey, request the intended property,
and distinguish a valid null blob from unavailable, failed, or invalid remote response according to datasource
contract.
Rationale:
Blob values can be large or lazy-loaded. Incorrect blob resolution or silent failure corrupts persisted data
visibility.
Source scope:
OADataSourceClient.getPropertyBlobValue(...), operation code GET_PROPERTY, OARuntime OA object-key services.
Related CODEX findings:
False-success prevention for failed remote load/blob work is part of requested scope.
Suggested unit tests:
testRemoteBlobLoadUsesObjectClassKeyAndProperty(),
testRemoteBlobInvalidResponseDoesNotAppearAsValidBlobUnlessContracted(),
testRemoteBlobNullValueDistinguishedFromFailureByOwnerDecision()
Spec target section:
Datasource Client/Server / Blob Property Semantics

DSCS-M2M-001 — Many-to-Many Remote Update Semantics
Contract statement:
Many-to-many updates must transmit the master class, master key, added objects, removed objects, and relationship
property name so the server datasource can apply the relationship mutation to the intended authoritative
relationship.
Rationale:
M2M relationship updates affect Hub membership, link-table state, cache, sync, and replication behavior.
Source scope:
OADataSourceClient.updateMany2ManyLinks(...), operation code UPDATE_MANY2MANY_LINKS, OARuntime OA object-key
services.
Related CODEX findings:
No direct source CODEX; requested scope includes relationship and save/delete coordination.
Suggested unit tests:
testManyToManyUpdateTransmitsMasterIdentityAndChanges(), testManyToManyUpdateFailureIsVisible(),
testManyToManyUpdateDoesNotUseClientObjectIdentityAsAuthority()
Spec target section:
Datasource Client/Server / Relationship Mutation Semantics

DSCS-TL-001 — Runtime Context Restoration
Contract statement:
Any ThreadLocal, OAThreadLocal, sync, loading, transaction, security, or OA runtime context set while performing
client/server datasource work must be restored with try/finally by the owning caller or remote operation boundary.
Rationale:
Datasource calls can occur inside sync, lazy-load, transaction, serialization, and remote contexts. Leaked context
can suppress sync, misroute OA runtime work, or corrupt transaction state.
Source scope:
OADataSourceClient call boundaries, MyIterator fetching, RemoteClientInterface invocation boundaries, higher-level
sync/remote/datasource callers.
Related CODEX findings:
No direct ThreadLocal mutation in OADataSourceClient; ThreadLocal restoration is a cross-package boundary
requirement.
Suggested unit tests:
testRemoteDatasourceCallDoesNotLeakLoadingContext(), testQueuedDatasourceMutationDoesNotLeakSendSyncMessagesState(),
testIteratorFetchRestoresRuntimeContextOnFailure()
Spec target section:
Datasource Client/Server / Runtime Context

DSCS-CONCURRENT-001 — Shared Client State Safety
Contract statement:
Shared OADataSourceClient state, including remote client reference and metadata caches, must be thread-safe, safely
published, or explicitly scoped to single-threaded use.
Rationale:
Datasource clients are runtime shared infrastructure and can be accessed by UI, background loading, sync, and query
threads.
Source scope:
OADataSourceClient.remoteClientSync, hashClass, hmMax, supports-storage cache fields, assign-id-on-create cache
fields, MyIterator synchronized methods.
Related CODEX findings:
getMaxLength mutates a plain HashMap without synchronization under concurrent access.
Suggested unit tests:
testConcurrentGetMaxLengthCallsHaveStableCacheState(),
testConcurrentIsClassSupportedCallsDoNotCorruptSupportCache(),
testConcurrentRemoteClientLookupSafelyPublishesEndpoint()
Spec target section:
Datasource Client/Server / Concurrency

DSCS-RECONNECT-001 — Reconnect and Stale Remote State
Contract statement:
After reconnect or remote endpoint replacement, datasource operations must use the current active remote client and
must not rely on stale remote proxies, stale iterator tokens, or stale metadata cache values unless explicitly valid
across sessions.
Rationale:
Distributed datasource clients can survive disconnect/reconnect; stale state can misroute operations or make old
metadata appear authoritative.
Source scope:
OADataSourceClient.remoteClientSync, getRemoteClient(), metadata caches, MyIterator.id remote iterator token, sync
remote-client lifecycle.
Related CODEX findings:
Remote/datasource registration and session staleness are cross-package risks; metadata cache scoping is implicit.
Suggested unit tests:
testReconnectRefreshesRemoteDatasourceClientEndpoint(), testRemoteIteratorTokenInvalidAfterReconnectFailsVisibly(),
testMetadataCacheInvalidatedOrDocumentedAcrossReconnect()
Spec target section:
Datasource Client/Server / Reconnect Semantics

DSCS-INTEGRATION-001 — Cross-Package Datasource Compatibility
Contract statement:
Client/server datasource behavior must remain compatible with datasource, remote, comm, sync, replication,
transaction, cache, object, Hub, select, query, metadata, OA runtime, serialization, and runtime contracts.
Rationale:
Distributed datasource calls coordinate persistence authority, object identity, query evaluation, lazy loading,
cache visibility, and observable OA behavior across packages.
Source scope:
OADataSourceClient, RemoteClientInterface, OARuntime OA/datasource services, ObjectCacheIterator,
OASiblingHelper, OAObjectKey, OASelect/OAQuery integration, sync/remote infrastructure.
Related CODEX findings:
Registration-path failure, classless/class-specific routing ambiguity, max-length cache concurrency issue all map to
cross-package datasource/runtime boundaries.
Suggested unit tests:
testClientServerDatasourceSelectPreservesCacheIdentity(),
testClientServerDatasourceMutationCoordinatesWithSyncContracts(),
testClientServerDatasourceQueriesRespectMetadataAndKeySemantics()
Spec target section:
Datasource Client/Server / Cross-Package Integration

*/
