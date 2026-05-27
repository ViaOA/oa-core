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
 * Core persistence abstraction for the OA framework.
 * <p>
 * The {@code com.viaoa.datasource} package defines OA's unified DataSource
 * architecture — a flexible, pluggable layer that allows {@link com.viaoa.object.OAObject}
 * models to work seamlessly with any persistence provider, including JDBC,
 * REST services, distributed servers, or in-memory caches.
 * <p>
 * The design goal is to completely decouple business models and object graphs
 * from the underlying storage mechanism, while preserving full CRUD semantics,
 * transaction control, and identity consistency across all backends.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.viaoa.datasource.OADataSource} — abstract base class defining the
 *       CRUD and query contract for persistence providers.</li>
 *   <li>{@link com.viaoa.datasource.OADataSourceInterface} — formal interface used by
 *       all implementations to ensure compatibility.</li>
 *   <li>{@link com.viaoa.select.OASelect} — executes object-based queries and
 *       streams results through {@link com.viaoa.datasource.OADataSourceIterator}.</li>
 *   <li>{@link com.viaoa.datasource.OADataSourceDelegate} — utility for locating and
 *       managing registered DataSources.</li>
 *   <li>{@link com.viaoa.select.OASelectManager} — background manager that monitors
 *       and cleans up active query iterators.</li>
 *   <li>{@link com.viaoa.filter.OASelectFilter} — filter bridge for in-memory and
 *       DataSource-level selection logic.</li>
 * </ul>
 *
 * <h2>Design Highlights</h2>
 * <ul>
 *   <li>Supports any persistence type (SQL, REST, distributed, cache, custom).</li>
 *   <li>Object-graph queries automatically translated into native query syntax.</li>
 *   <li>Full CRUD lifecycle integration with {@link com.viaoa.object.OAObject}.</li>
 *   <li>Thread-safe registration, iteration, and transaction participation.</li>
 *   <li>Zero code changes required when switching DataSource implementations.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * OADataSource ds = new OADataSourceJDBC("jdbc:mysql://...");
 * OADataSource.register(ds, Customer.class);
 * OASelect<Customer> select = new OASelect<>(Customer.class, "lastName = ?", new Object[]{"Smith"});
 * for (Customer c : select) {
 *     System.out.println(c.getFirstName());
 * }
 * select.close();
 * }</pre>
 *
 * @see com.viaoa.object.OAObject
 * @see com.viaoa.datasource.OADataSource
 * @see com.viaoa.select.OASelect
 */
package com.viaoa.datasource;

//CODEX unit tests <todo>


/* CODEX Invariants

DS-RUNTIME-001 — Datasource Runtime Authority
Contract statement:
com.viaoa.datasource defines the high-level persistence boundary between OA Object Graph runtime state and storage-
backed, remote, generated-key, or cache-backed datasource implementations.
Rationale:
Datasources are not simple adapters. They control object loading, selecting, saving, deleting, counting, identity
assignment, relationship mutation, cache reconciliation, and runtime visibility.
Source scope:
OADataSource, OADataSourceInterface, OADataSourceIterator, OADataSourceListIterator, OADataSourceEmptyIterator,
OASaveDeleteListener, datasource subpackages, runtime datasource service integration.
Related CODEX findings:
Existing package-info notes routing, write-policy, remote default datasource, autonumber, iterator, object-cache,
and client/server datasource risks.
Suggested unit tests:
testDatasourceContractRoutesObjectGraphPersistence(),
testDatasourceOperationSuccessDistinctFromCacheAndTransactionSuccess(),
testDatasourceFailureDoesNotAppearSuccessful()
Spec target section:
Datasource Runtime / Core Responsibility

DS-REGISTRY-001 — Registered Datasource Routing
Contract statement:
Runtime datasource lookup must return the enabled datasource authoritative for the requested class and filter,
respecting datasource registration order, enabled state, class support, and last-datasource fallback semantics.
Rationale:
Object save/load/select/count/delete behavior depends on deterministic class-to-datasource routing. Wrong routing
sends persistence work to the wrong backend.
Source scope:
OADataSource.getEnabled(), setEnabled(...), isClassSupported(...), getLast(), setLast(...), runtime datasource
registry/service, object datasource service integration.
Related CODEX findings:
Datasource registry lifecycle/order risks are noted in runtime/package-info; OADataSourceClient registration path is
CODEX-commented.
Suggested unit tests:
testDatasourceRoutingUsesFirstNonLastSupportedDatasource(), testDatasourceRoutingUsesLastDatasourceOnlyAsFallback(),
testDisabledDatasourceIsSkipped()
Spec target section:
Datasource Runtime / Routing Semantics

DS-REGISTRY-002 — Construction Versus Registration
Contract statement:
Constructing a datasource must not be treated as runtime registration unless the constructor or factory explicitly
documents and performs registration.
Rationale:
Silent construction without registry visibility creates false “datasource exists” assumptions and can make selects
or saves silently find no datasource.
Source scope:
OADataSource constructor, OADataSourceClient constructor, OADataSourceAuto constructor, OADataSourceObjectCache
constructor, runtime datasource registration paths.
Related CODEX findings:
OADataSourceClient registration path can create a datasource not discoverable through runtime datasource lookup.
Suggested unit tests:
testDatasourceConstructorDoesNotRegisterAutomaticallyUnlessContracted(),
testExplicitRegistrationMakesDatasourceDiscoverable(), testClientDatasourceCreationPathRegistersWhenRequired()
Spec target section:
Datasource Runtime / Registration Lifecycle

DS-CLASS-001 — Availability Is Not Class Support
Contract statement:
A datasource being available must not imply support for every class; class-specific operations must route through
isClassSupported(clazz, filter) or an equivalent supported-class authority.
Rationale:
OA runtimes can use multiple datasources with different class ownership. Availability is health; class support is
semantic ownership.
Source scope:
OADataSource.isAvailable(), isClassSupported(Class, OAFilter), isClassSupported(Class), runtime datasource lookup.
Related CODEX findings:
Remote datasource default/fallback behavior is CODEX-commented.
Suggested unit tests:
testAvailableDatasourceNotSelectedWhenClassUnsupported(),
testFallbackDatasourceDoesNotOverridePrimarySupportedDatasource(), testClassSpecificOperationRequiresClassSupport()
Spec target section:
Datasource Runtime / Class Ownership

DS-LIFECYCLE-001 — Datasource Lifecycle State
Contract statement:
Datasource enable, close, reopen, availability, read-only, ignore-writes, and last-fallback state must have
deterministic lifecycle semantics visible to routing and operation callers.
Rationale:
Runtime services must know whether a datasource can be selected, used, closed, reopened, or skipped.
Source scope:
OADataSource.getEnabled(), setEnabled(...), isAvailable(), close(), reopen(int), getLast(), setLast(...),
getReadOnly(), getIgnoreWrites().
Related CODEX findings:
Read-only/ignore-writes enforcement is CODEX-commented; lifecycle/order risks are noted in package-info.
Suggested unit tests:
testClosedDatasourceRejectedOrReopenedByContract(), testDisabledDatasourceNotRouted(),
testLastDatasourceFallbackLifecycleIsDeterministic()
Spec target section:
Datasource Runtime / Lifecycle Semantics

DS-WRITE-001 — Write Policy Authority
Contract statement:
If read-only or ignore-writes flags are exposed as datasource contracts, normal save/delete/insert/update paths must
either enforce them consistently or document them as advisory-only implementation hints.
Rationale:
Applications must not believe writes are blocked or ignored while normal graph save/delete paths still write.
Source scope:
OADataSource.setReadOnly(...), getReadOnly(), setIgnoreWrites(...), getIgnoreWrites(), insert(...), update(...),
delete(...), save(...), object datasource save/delete gateways.
Related CODEX findings:
OADataSource CODEX notes read-only/ignore-writes flags are exposed but shared save/delete paths might not enforce
them.
Suggested unit tests:
testReadOnlyDatasourceBlocksNormalSaveWhenContractRequires(),
testIgnoreWritesDatasourceSkipsNormalDeleteWhenContractRequires(), testWritePolicyAdvisoryModeIsExplicit()
Spec target section:
Datasource Runtime / Write Policy

DS-SAVE-001 — Save Delegation By Object Lifecycle
Contract statement:
OADataSource.save(obj) must route new OAObjects to insert(obj) and existing OAObjects to update(obj), unless a
concrete datasource explicitly overrides with equivalent lifecycle semantics.
Rationale:
Graph lifecycle and persistence state must agree on whether an object is being created or updated.
Source scope:
OADataSource.save(...), insert(...), update(...), OAObject.getNew(), object datasource service integration.
Related CODEX findings:
None observed.
Suggested unit tests:
testSaveNewObjectDelegatesToInsert(), testSaveExistingObjectDelegatesToUpdate(),
testSaveNullObjectIsNoOpByContract()
Spec target section:
Datasource Runtime / Save Semantics

DS-DELETE-001 — Delete Routing
Contract statement:
Delete and deleteAll must affect only the datasource authoritative for the object/class according to runtime routing
and must preserve object/cache lifecycle visibility according to the owning operation contract.
Rationale:
Incorrect delete routing can leave stale cache state or delete from the wrong backend.
Source scope:
OADataSource.delete(...), deleteAll(...), datasource subpackage implementations, object datasource delete services.
Related CODEX findings:
Existing package-info notes no direct package bug beyond remote/detail routing risks.
Suggested unit tests:
testDeleteRoutesToClassDatasource(), testDeleteAllRoutesToClassDatasourceOnly(),
testDeleteFailureLeavesObjectRetryable()
Spec target section:
Datasource Runtime / Delete Semantics

DS-IDENTITY-001 — Datasource Object Identity
Contract statement:
Objects loaded or selected through a datasource must reconcile with graph/cache identity so that the same persistent
OAObjectKey maps to one authoritative runtime object identity.
Rationale:
OA depends on identity stability across Hubs, links, serialization, sync, replication, and query results.
Source scope:
OADataSource.getObject(...), select(...), OADataSourceIterator implementations, datasource subpackages, cache/object
integration.
Related CODEX findings:
Existing package-info maps identity concerns to graph/cache invariants and remote select/cache update paths.
Suggested unit tests:
testDatasourceGetObjectReturnsCachedIdentityForSameKey(), testDatasourceSelectReconcilesObjectsWithCache(),
testRemoteSelectUpdatesClientObjectIdentityCache()
Spec target section:
Datasource Runtime / Identity and Cache Semantics

DS-KEY-001 — Object Key Resolution
Contract statement:
Datasource getObject overloads must convert supplied string, numeric, object, and composite IDs into OAObjectKey
values using OA metadata semantics before selecting or resolving objects.
Rationale:
Key construction is the bridge between external datasource identifiers and runtime identity/cache authority.
Source scope:
OADataSource.getObject(Class,String), getObject(Class,int), getObject(Class,long), getObject(Class,Object),
getObject(Class,Object[]), getObject(Class,OAObjectKey), getObject(OAObjectInfo,Class,OAObjectKey,boolean).
Related CODEX findings:
None observed.
Suggested unit tests:
testGetObjectStringIdUsesObjectKeyMetadata(), testGetObjectCompositeIdsUsesOAObjectKey(),
testGetObjectNullClassOrKeyReturnsNull()
Spec target section:
Datasource Runtime / Key Semantics

DS-SELECT-001 — Select Result Boundary
Contract statement:
A datasource select must return a valid OADataSourceIterator for a valid zero-or-more result set, or a clear no-
datasource/no-select value by contract; empty results should be represented by an empty iterator where a select path
exists.
Rationale:
OASelect and Hub loading must distinguish “valid query with no rows” from “no datasource/select unavailable.”
Source scope:
OADataSource.select(...), selectPassthru(...), OADataSourceEmptyIterator, OADataSourceListIterator,
ObjectCacheIterator, client/server iterators.
Related CODEX findings:
Remote missing-master detail select is CODEX-commented; object-cache no-result semantics are noted in package-info.
Suggested unit tests:
testObjectCacheSelectNoResultsReturnsEmptyIterator(), testUnsupportedSelectPathReturnsDefinedNoDatasourceValue(),
testValidSelectWithZeroRowsDoesNotLookLikeDatasourceMissing()
Spec target section:
Datasource Runtime / Select Semantics

DS-SELECT-002 — Query and Path Delegation
Contract statement:
Datasource-backed and in-memory selections must interpret queryWhere, params, queryOrder, whereObject,
propertyFromWhereObject, extraWhere, filter, max, and dirty flags according to OA query, path, metadata, and filter
contracts.
Rationale:
Selects drive Hubs, detail loading, UI, reports, and object traversal; inconsistent interpretation creates wrong
graph views.
Source scope:
OADataSource select overloads, count overloads, selectPassthru overloads, OADataSourceObjectCache,
OADataSourceClient, OASelect integration.
Related CODEX findings:
Remote detail count/select with stale or missing where-object is CODEX-commented.
Suggested unit tests:
testSelectWhereObjectScopeMatchesMetadataLink(), testSelectAppliesQueryParamsAndExtraWhere(),
testSelectOrderAndMaxFollowContract()
Spec target section:
Datasource Runtime / Query and Path Semantics

DS-COUNT-001 — Count and Select Consistency
Contract statement:
count(...) must count the same logical scope that select(...) would return for the same class, query, params, where-
object, property path, filter, extraWhere, dirty, and max semantics, within datasource capability.
Rationale:
Pre-count, UI paging, Hub loading, and select decisions rely on count/select consistency.
Source scope:
OADataSource.count overloads, countPassthru(...), select overloads, datasource implementations.
Related CODEX findings:
Remote detail count with stale/missing where-object is CODEX-commented.
Suggested unit tests:
testCountMatchesSelectForWhereObjectDetail(), testCountReturnsZeroForMissingWhereObjectDetailWhenContractRequires(),
testCountPassthruMatchesPassthruSelectScope()
Spec target section:
Datasource Runtime / Count-Select Consistency

DS-ITERATOR-001 — Iterator Forward Progress
Contract statement:
Datasource iterators must be forward-only and stable: repeated hasNext() calls must not skip rows, next() after
hasNext() true must return the pending row, and iteration must terminate deterministically.
Rationale:
OASelect and Hub loading commonly call hasNext before next; skipping or duplicating objects corrupts loaded Hubs and
query results.
Source scope:
OADataSourceIterator, OADataSourceListIterator, OADataSourceEmptyIterator, ObjectCacheIterator, client/server
iterator implementations.
Related CODEX findings:
The OADataSourceListIterator.hasNext() boundary bug was fixed; remote batch Object[] issue was fixed.
Suggested unit tests:
testListIteratorHasNextDoesNotSkip(), testObjectCacheIteratorHasNextNextSequence(),
testRemoteIteratorHasNextNextSequence()
Spec target section:
Datasource Runtime / Iterator Semantics

DS-ITERATOR-002 — Iterator Close/Remove Lifecycle
Contract statement:
For datasource iterators, remove() is the OA close/release contract unless a concrete iterator provides a separate
close path; it must release datasource, remote, cursor, or streaming resources even when it does not remove the
current element.
Rationale:
OA historically uses Iterator.remove() as query close; remote and streaming datasources depend on this for cleanup.
Source scope:
OADataSourceIterator.remove(), OADataSource.getObject(...), OADataSourceClient iterator remove, remote datasource
iterator operations.
Related CODEX findings:
OADataSource.getObject iterator cleanup was fixed; package-info notes iterator resource cleanup.
Suggested unit tests:
testGetObjectClosesIteratorInFinally(), testRemoteIteratorRemoveReleasesServerIterator(),
testIteratorRemoveIsIdempotentReleaseByContract()
Spec target section:
Datasource Runtime / Iterator Lifecycle

DS-ITERATOR-003 — Empty Iterator Semantics
Contract statement:
OADataSourceEmptyIterator represents a valid empty result set and must not be confused with datasource failure or
unavailable selection.
Rationale:
No-result behavior is normal and must be distinguishable from no datasource, failed query, or unsupported operation.
Source scope:
OADataSourceEmptyIterator.hasNext(), next(), getQuery(), getQuery2(), remove(), forEachRemaining(...), datasource
select consumers.
Related CODEX findings:
None observed.
Suggested unit tests:
testEmptyIteratorHasNoNext(), testEmptyIteratorNextReturnsDefinedNoResultValue(),
testEmptyIteratorRemoveIsNoOpRelease()
Spec target section:
Datasource Runtime / Empty Result Semantics

DS-CACHE-001 — Cache Reconciliation Boundary
Contract statement:
Datasource load/select/save/delete operations must coordinate with object cache authority so cache state, object
lifecycle state, and datasource state do not silently diverge.
Rationale:
Datasource and cache are separate authorities that must be reconciled; otherwise duplicate objects, stale objects,
or deleted objects remain visible incorrectly.
Source scope:
OADataSource.getObject(...), select(...), save(...), delete(...), datasource subpackages, object/cache services.
Related CODEX findings:
Object-cache datasource and remote select identity/cache invariants; graph identity invariants cover broader cache
authority.
Suggested unit tests:
testDatasourceLoadReconcilesWithCache(), testDatasourceDeleteRemovesOrMarksCacheStateByContract(),
testDatasourceSaveDoesNotCreateDuplicateCachedObject()
Spec target section:
Datasource Runtime / Cache Coordination

DS-DETAIL-001 — Detail Selection Scope
Contract statement:
Datasource selections scoped by whereObject and propertyFromWhereObject must be limited to the intended link/detail
relationship, or return an explicit empty/no-result outcome when the relationship scope cannot be resolved.
Rationale:
Detail Hubs depend on datasource selection not accidentally widening into all objects of the detail class.
Source scope:
OADataSource select/count overloads with whereObject/propertyNameFromWhereObject,
OADataSourceObjectCache.select(...), OADataSourceClient.select(...), remote datasource paths.
Related CODEX findings:
Remote stale/missing where-object can become unscoped select; CODEX-commented.
Suggested unit tests:
testObjectCacheDetailSelectReturnsOnlyLinkedObjects(),
testRemoteDetailSelectMissingMasterReturnsEmptyWhenContractRequires(), testInvalidDetailPathFailsVisibly()
Spec target section:
Datasource Runtime / Detail Select Semantics

DS-CS-001 — Client/Server Routing Boundary
Contract statement:
Client/server datasource operations must delegate class-specific persistence and select work to the server-
authoritative datasource, except for explicitly documented local cache optimizations.
Rationale:
Distributed OA relies on the server being persistence-authoritative while clients maintain cache views.
Source scope:
OADataSourceClient, remote datasource operations, OADataSource.isClient(), runtime role routing.
Related CODEX findings:
Client datasource registration path is CODEX-commented; remote classless datasource routing is CODEX-commented.
Suggested unit tests:
testClientInsertDelegatesToRemoteDatasource(), testClientSelectDelegatesToRemoteUnlessLocalSelectAllCacheApplies(),
testSingleUserSelectDoesNotUseOADataSourceClient()
Spec target section:
Datasource Runtime / Client-Server Routing

DS-CS-002 — Classless Remote Command Routing
Contract statement:
Remote classless datasource commands such as availability, storage support, assign-id-on-create, and execute must
either route to a defined default datasource or explicitly return unavailable by contract.
Rationale:
Classless commands cannot be routed by class. Silent null behavior can make a valid server datasource appear
unavailable.
Source scope:
Remote datasource integration, OADataSourceClient.isAvailable(), supportsStorage(), getAssignIdOnCreate(),
execute(...).
Related CODEX findings:
Remote classless default datasource behavior is CODEX-commented.
Suggested unit tests:
testRemoteClasslessSupportsStorageUsesDefaultDatasourceWhenConfigured(),
testRemoteClasslessExecuteHasDefinedNoDatasourceResult(),
testRemoteClassSpecificUnsupportedDoesNotUseDefaultDatasource()
Spec target section:
Datasource Runtime / Remote Default Datasource

DS-AUTONUM-001 — Generated Identity Boundary
Contract statement:
Datasource assignId and willCreatePropertyValue behavior must follow OA metadata and generated-key authority
boundaries, preserving cache identity and avoiding duplicate keys.
Rationale:
Generated IDs are persistent identity. Incorrect assignment corrupts cache, object graph links, sync, replication,
and datasource rows.
Source scope:
OADataSource.assignId(...), willCreatePropertyValue(...), getAssignIdOnCreate(), setAssignIdOnCreate(...),
OADataSourceAuto, NextNumber.
Related CODEX findings:
startNextNumber adjustment race and concurrent lower-bound race are CODEX-commented in autonumber.
Suggested unit tests:
testAutonumberAssignsUniqueIdsForSameClass(), testStartingNextNumberActsAsLowerBound(),
testWillCreatePropertyValueMatchesGeneratedKeyAuthority()
Spec target section:
Datasource Runtime / Generated Identity

DS-STORAGE-001 — Storage Capability Boundary
Contract statement:
supportsStorage() must accurately describe whether the datasource supports persistent insert/update/delete storage;
specialized datasources such as autonumber-only or cache-only sources must not be treated as external persistence
unless explicitly contracted.
Rationale:
Routing a persistence operation to a non-storage datasource creates false success, lost writes, or null selects.
Source scope:
OADataSource.supportsStorage(), OADataSourceAuto.supportsStorage(), OADataSourceObjectCache,
OADataSourceClient.supportsStorage().
Related CODEX findings:
Autonumber datasource is not a storage datasource; remote supports-storage routing is CODEX-commented.
Suggested unit tests:
testAutonumberSupportsStorageFalse(), testObjectCacheStorageCapabilityMatchesContract(),
testRemoteSupportsStorageReflectsServerDatasource()
Spec target section:
Datasource Runtime / Storage Capability

DS-TRANSACTION-001 — Transaction Participation Boundary
Contract statement:
Datasource operations must observe active OATransaction state for batch allowance, transaction presence, read-only
write exceptions, and commit/rollback boundaries according to the owning transaction contract.
Rationale:
Datasource operation success is not necessarily transaction commit success, and write policy can be affected by
transaction context.
Source scope:
OADataSource.isAllowingBatch(), isInTransaction(), getIgnoreWrites(), OATransaction, datasource implementations.
Related CODEX findings:
Read-only/ignore-writes contract is CODEX-commented.
Suggested unit tests:
testDatasourceSeesActiveTransaction(), testDatasourceBatchAllowedWhenTransactionUsesBatch(),
testTransactionAllowWritesOverridesIgnoreWritesByContract()
Spec target section:
Datasource Runtime / Transaction Boundary

DS-LISTENER-001 — Save/Delete Listener Semantics
Contract statement:
Save/delete listener callbacks must have explicit before/during/after semantics: participant failures must be
visible, observer failures must follow owner isolation rules, and cleanup/finalization callbacks must not silently
skip required remaining cleanup.
Rationale:
Datasource listener behavior can affect persistence approval, side effects, event visibility, and cleanup.
Source scope:
OASaveDeleteListener.onInsert(...), onUpdate(...), onDelete(...), object datasource service listener integration.
Related CODEX findings:
No direct package-local finding; package-info identifies listener exception visibility as invariant.
Suggested unit tests:
testSaveDeleteListenerFailureVisibleWhenParticipant(), testObserverListenerFailurePolicyIsExplicit(),
testAfterDeleteCleanupListenerFailureDoesNotHidePartialCleanup()
Spec target section:
Datasource Runtime / Listener Semantics

DS-FAIL-001 — Datasource Failure Visibility
Contract statement:
Failed load, select, count, insert, update, save, delete, deleteAll, assign-id, relationship update, blob load,
execute, metadata, iterator, or storage operation must be caller-visible or explicitly represented by the method
contract; silent false-success is not allowed.
Rationale:
Datasource failures can otherwise leave object graph, cache, persistence, transaction, sync, or replication state
divergent.
Source scope:
OADataSource abstract and concrete operation contracts, OADataSourceIterator, datasource subpackages, object
datasource service integration.
Related CODEX findings:
Read-only/ignore-writes ambiguity, client datasource registration path, remote classless routing, remote detail
select/count, autonumber race, iterator cleanup.
Suggested unit tests:
testInsertExceptionPropagatesAsIncompleteOperation(), testFailedInsertLeavesObjectRetryable(),
testRemoteSelectFailureDoesNotAppearAsEmptySuccessfulSelect()
Spec target section:
Datasource Runtime / Failure Semantics

DS-PARTIAL-001 — Partial Progress Visibility
Contract statement:
If datasource work partially completes across cache update, object state change, remote invocation, storage write,
transaction boundary, iterator fetch, or relationship update, the incomplete boundary must remain visible and retry-
safe.
Rationale:
Partial persistence progress is common under exceptions, disconnects, corrupt files, and transaction failures. It
must not masquerade as fully committed Object Graph state.
Source scope:
OADataSource.save/delete/update/insert/select/count APIs, OADataSourceIterator, clientserver/objectcache/autonumber
subpackages, transaction integration.
Related CODEX findings:
Remote return-on-queue ambiguity, object-cache storage file boundary, autonumber duplicate assignment risk, iterator
cleanup.
Suggested unit tests:
testFailedSaveDoesNotCommitFalseObjectState(), testPartialRemoteDatasourceMutationIsVisible(),
testStorageWriteFailureDoesNotReportSuccess()
Spec target section:
Datasource Runtime / Partial Progress

DS-NULL-001 — Null, Empty, Missing, and Deleted State
Contract statement:
Null input, empty result, no datasource, missing object, deleted object, and unsupported operation outcomes must be
deterministic and distinguishable where callers need to separate no-result from failure.
Rationale:
OA runtime paths are generic and frequently pass optional values. Ambiguous null/empty behavior hides missing
datasource or failed load conditions.
Source scope:
OADataSource.getObject(...), select(...), count(...), deleteAll(...), OADataSourceEmptyIterator, concrete datasource
implementations.
Related CODEX findings:
Remote missing-master detail select/count and no-datasource select behavior noted in package-info.
Suggested unit tests:
testGetObjectNullClassOrKeyReturnsNull(), testEmptySelectReturnsEmptyIteratorWhenSelectPathExists(),
testMissingObjectResultDistinctFromDatasourceFailure()
Spec target section:
Datasource Runtime / Null and No-Result Semantics

DS-TL-001 — Runtime Context Restoration
Contract statement:
Any ThreadLocal/OAThreadLocal loading, saving, deleting, transaction, sync, replication, security, or graph context
set during datasource operations must be restored with try/finally by the owning datasource or caller boundary.
Rationale:
Datasource work often occurs inside object graph, sync, transaction, serialization, and remote flows. Context
leakage can misroute operations or suppress required events/sync.
Source scope:
OADataSource operation boundaries, datasource subpackages, object/hub graph datasource services, transaction/runtime
thread services.
Related CODEX findings:
ThreadLocal restoration is a cross-package invariant; no direct parent-package mutation noted in source.
Suggested unit tests:
testDatasourceLoadRestoresLoadingContextOnFailure(), testDatasourceSaveRestoresSyncContextOnFailure(),
testDatasourceTransactionContextNotLeakedAcrossOperations()
Spec target section:
Datasource Runtime / ThreadLocal Context

DS-CONCURRENT-001 — Concurrent Datasource Access
Contract statement:
Shared datasource state, metadata caches, iterator state, class-support caches, object caches, and storage resources
must be thread-safe, safely published, or explicitly scoped to one thread/operation.
Rationale:
Datasources are runtime shared infrastructure used by UI, background loading, sync, replication, remote calls, and
transactions.
Source scope:
OADataSource implementations, OADataSourceIterator implementations, datasource subpackages, metadata/cache fields.
Related CODEX findings:
OADataSourceClient getMaxLength HashMap race; autonumber startNextNumber race; object-cache live iteration
concurrency boundaries.
Suggested unit tests:
testConcurrentDatasourceMetadataLookupStable(), testConcurrentAutonumberAssignmentDoesNotDuplicateIds(),
testConcurrentObjectCacheSelectsDoNotCorruptIteratorState()
Spec target section:
Datasource Runtime / Concurrency

DS-RESOURCE-001 — Resource Cleanup
Contract statement:
Datasource-owned iterators, streams, remote cursors, storage files, compression streams, transactions, and other
resources must be closed, removed, released, or explicitly transferred on success, failure, exhaustion,
cancellation, and owner shutdown.
Rationale:
Datasource resources can hold remote server state, file descriptors, object references, or transaction resources.
Source scope:
OADataSourceIterator.remove(), OADataSource.getObject(...), OADataSourceObjectCache storage methods, client/server
iterators, close(), reopen(int).
Related CODEX findings:
OADataSource.getObject iterator cleanup was fixed; object-cache storage cleanup uses finally; remote iterator
cleanup is package-level invariant.
Suggested unit tests:
testGetObjectClosesIteratorInFinally(), testRemoteIteratorRemovedWhenClientCloses(),
testSaveToStorageFileClosesResourcesWhenSerializationThrows()
Spec target section:
Datasource Runtime / Resource Cleanup

DS-COMPAT-001 — Cross-Package Datasource Compatibility
Contract statement:
Datasource behavior must remain compatible with object, Hub, cache, metadata, select, query, path, transaction,
serialization, sync, replication, remote, graph/runtime, autonumber, client/server, and object-cache contracts.
Rationale:
Datasource is the persistence boundary for executable OA blueprints. It coordinates with nearly every runtime
package and must preserve each package’s authority.
Source scope:
com.viaoa.datasource.*, datasource subpackages, OAObject/Hub/cache/select/query/path/transaction/serialize/sync/
replication/remote/graph integration.
Related CODEX findings:
Existing package-info notes that production readiness depends on cross-package tests covering graph lifecycle, Hub
loading, object identity, sync/replication, and datasource routing together.
Suggested unit tests:
testDatasourceRoutingIdentityCacheAndSelectIntegration(), testDatasourceSaveDeleteTransactionSyncIntegration(),
testDatasourceObjectCacheClientServerAutonumberContractsDoNotConflict()
Spec target section:
Datasource Runtime / Cross-Package Integration

*/
