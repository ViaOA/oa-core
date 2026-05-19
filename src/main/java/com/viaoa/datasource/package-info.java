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
 *   <li>{@link com.viaoa.select.OASelectFilter} — filter bridge for in-memory and
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

/* CODEX Invariants

1. Datasource Runtime Contracts

  DS-RUNTIME-001 — Registered Datasource Routing Is Authoritative

  Contract statement:
  OARuntime.datasource().get(clazz, filter) must return the enabled datasource that is authoritative for the
  requested class, respecting datasource ordering and getLast() fallback semantics.

  Rationale:
  OAObject save/load/select behavior depends on deterministic class-to-datasource routing. Wrong routing can send
  persistence, selection, or autonumber work to the wrong backend.

  Source locations:
  com.viaoa.runtime.OADataSourceService.get(...)
  OADataSource.isClassSupported(...)
  OADataSource.getLast()
  OAObjectDSService.getDataSource(...)

  Known related CODEX findings:
  Datasource registry lifecycle/order risks are CODEX-commented in runtime.

  Suggested unit tests:
  testDatasourceRoutingUsesFirstNonLastSupportedDatasource()
  testDatasourceRoutingUsesLastDatasourceOnlyAsFallback()
  testDisabledDatasourceIsSkipped()

  Spec target section:
  Datasource Runtime / Routing Semantics

  DS-RUNTIME-002 — Datasource Availability Must Not Imply Class Support

  Contract statement:
  A datasource being available does not mean it supports every class. Class-specific operations must route through
  isClassSupported(clazz, filter).

  Rationale:
  OA can have multiple datasources: object-cache, autonumber, client/server, JDBC, etc. Availability is runtime
  health; class support is semantic ownership.

  Source locations:
  OADataSource.isAvailable()
  OADataSource.isClassSupported(...)
  OADataSourceService.get(...)

  Known related CODEX findings:
  Remote datasource default/fallback behavior is CODEX-commented.

  Suggested unit tests:
  testAvailableDatasourceNotSelectedWhenClassUnsupported()
  testFallbackDatasourceDoesNotOverridePrimarySupportedDatasource()

  Spec target section:
  Datasource Runtime / Class Ownership

  DS-RUNTIME-003 — Datasource Constructors Must Not Imply Runtime Registration Unless Explicit

  Contract statement:
  Constructing a datasource must not be treated as runtime registration unless the constructor or factory explicitly
  documents and performs registration.

  Rationale:
  OA 4.0 runtime ownership should be explicit. Silent construction without registration can produce false
  “datasource exists” assumptions.

  Source locations:
  OADataSource constructor
  OADataSourceClient constructor
  OADataSourceAuto constructor
  OADataSourceObjectCache constructor
  OARuntime.datasource().register(...)

  Known related CODEX findings:
  OADataSourceClient registration path is CODEX-commented.

  Suggested unit tests:
  testDatasourceConstructorDoesNotRegisterAutomatically()
  testExplicitRegistrationMakesDatasourceDiscoverable()

  Spec target section:
  Datasource Runtime / Lifecycle

  2. Save/Delete Contracts

  DS-SAVE-001 — Save Must Resolve To Insert Or Update By Object New-State

  Contract statement:
  OADataSource.save(obj) must call insert(obj) when obj.getNew() is true and update(obj) otherwise.

  Rationale:
  Graph lifecycle and datasource persistence must agree on whether an object is new or existing.

  Source locations:
  OADataSource.save(...)
  OADataSource.insert(...)
  OADataSource.update(...)
  OAObjectDSService.save(...)

  Known related CODEX findings:
  none observed.

  Suggested unit tests:
  testSaveNewObjectDelegatesToInsert()
  testSaveExistingObjectDelegatesToUpdate()

  Spec target section:
  Datasource Runtime / Save-Delete Semantics

  DS-SAVE-002 — Delete/DeleteAll Must Affect The Authoritative Datasource For The Class

  Contract statement:
  Delete operations must be routed to the datasource selected for the object class. deleteAll(c) must affect only
  the datasource authoritative for class c.

  Rationale:
  Incorrect delete routing can create stale object/cache state or delete from the wrong backend.

  Source locations:
  OADataSource.delete(...)
  OADataSource.deleteAll(...)
  OADataSourceClient.delete(...)
  OADataSourceClient.deleteAll(...)
  RemoteDataSource.datasource(...)

  Known related CODEX findings:
  none observed beyond already accepted object-cache behavior.

  Suggested unit tests:
  testDeleteRoutesToClassDatasource()
  testDeleteAllRoutesToClassDatasourceOnly()

  Spec target section:
  Datasource Runtime / Delete Semantics

  DS-SAVE-003 — Write Policy Flags Must Have Explicit Authority

  Contract statement:
  If setReadOnly(true) or setIgnoreWrites(true) is exposed as a datasource contract, normal OA save/delete paths
  must either enforce it or explicitly define it as advisory-only.

  Rationale:
  Applications must not believe writes are blocked when OA still writes through normal graph save/delete paths.

  Source locations:
  OADataSource.setReadOnly(...)
  OADataSource.setIgnoreWrites(...)
  OADataSource.getIgnoreWrites()
  OAObjectDSService.save/delete paths

  Known related CODEX findings:
  Read-only / ignore-writes enforcement is CODEX-commented in OADataSource.

  Suggested unit tests:
  testReadOnlyDatasourceBlocksNormalSaveWhenContractRequires()
  testIgnoreWritesDatasourceSkipsNormalDeleteWhenContractRequires()

  Spec target section:
  Datasource Runtime / Write Policy

  3. Select/Iterator Contracts

  DS-SELECT-001 — Select Must Return A Valid Iterator Or A Clear No-Result Value

  Contract statement:
  select(...) may return null only to mean no datasource/select path exists. Empty query results should prefer an
  iterator that has no rows.

  Rationale:
  OASelect and Hub loading need to distinguish “no datasource/select unavailable” from “valid query with zero
  results.”

  Source locations:
  OADataSource.select(...)
  OADataSourceEmptyIterator
  OADataSourceObjectCache.select(...)
  OADataSourceClient.select(...)

  Known related CODEX findings:
  Remote missing-master detail select is CODEX-commented.

  Suggested unit tests:
  testObjectCacheSelectNoResultsReturnsEmptyIterator()
  testClientSelectUnsupportedClassReturnsNullIterator()

  Spec target section:
  Datasource Runtime / Select Semantics

  DS-SELECT-002 — Count Must Match Select Scope

  Contract statement:
  count(...) must count the same logical scope that select(...) would return for the same class, where-object,
  property path, filters, and max.

  Rationale:
  Hub pre-count, UI paging, and select decisions rely on count/select consistency.

  Source locations:
  OADataSource.count(...)
  OADataSourceObjectCache.select(...)
  OADataSourceClient.count(...)
  RemoteDataSource.datasource(...)

  Known related CODEX findings:
  Remote detail count with stale/missing where-object is CODEX-commented.

  Suggested unit tests:
  testCountMatchesSelectForWhereObjectDetail()
  testCountReturnsZeroForMissingWhereObjectDetailWhenContractRequires()

  Spec target section:
  Datasource Runtime / Count-Select Consistency

  DS-ITERATOR-001 — Iterator Remove Is The OA Close Contract

  Contract statement:
  For datasource iterators, remove() must release datasource/remote resources even when it does not remove the
  current element.

  Rationale:
  OA historically uses Iterator.remove() as query close. Remote iterators and streaming datasources depend on this
  for cleanup.

  Source locations:
  OADataSourceIterator.remove()
  OADataSource.getObject(...)
  OADataSourceClient.MyIterator.remove()
  RemoteDataSource.datasource(...) IT_REMOVE

  Known related CODEX findings:
  none observed.

  Suggested unit tests:
  testGetObjectClosesIteratorInFinally()
  testRemoteIteratorRemoveReleasesServerIterator()

  Spec target section:
  Datasource Runtime / Iterator Lifecycle

  DS-ITERATOR-002 — Iterator HasNext/Next Must Be Stable And Forward-Only

  Contract statement:
  Calling hasNext() repeatedly must not skip objects. Calling next() after hasNext() returns true must return that
  same pending object.

  Rationale:
  OASelect and Hub loading commonly call hasNext() before next(); skipping or duplicating objects corrupts Hub
  contents.

  Source locations:
  OADataSourceListIterator
  ObjectCacheIterator
  OADataSourceClient.MyIterator

  Known related CODEX findings:
  The OADataSourceListIterator.hasNext() boundary bug was fixed.

  Suggested unit tests:
  testListIteratorHasNextDoesNotSkip()
  testObjectCacheIteratorHasNextNextSequence()
  testRemoteIteratorHasNextNextSequence()

  Spec target section:
  Datasource Runtime / Iterator Semantics

  DS-ITERATOR-003 — Remote Iterator Batches Must Preserve Element Type And Order

  Contract statement:
  Remote datasource iterator batches must preserve result order and must be consumable by the client without array
  runtime type failures.

  Rationale:
  Client/server select must behave like local select. Array type mismatch or ordering drift breaks distributed Hub
  loading.

  Source locations:
  OADataSourceClient.MyIterator.getMoreFromServer()
  RemoteDataSource.datasourceNext(...)

  Known related CODEX findings:
  The Object[] to OAObject[] cast issue was fixed by using Object[] client-side.

  Suggested unit tests:
  testRemoteSelectBatchDoesNotThrowArrayCastException()
  testRemoteSelectBatchPreservesOrder()

  Spec target section:
  Datasource Runtime / Remote Iterator Semantics

  4. Identity/Cache Contracts

  DS-IDENTITY-001 — Datasource Returned Objects Must Resolve To Graph Identity

  Contract statement:
  Objects returned from datasource getObject(...) or select(...) must integrate with the graph object cache so the
  same persistent key maps to one graph identity.

  Rationale:
  OA depends on identity stability across Hubs, links, serialization, sync, and replication.

  Source locations:
  OADataSource.getObject(...)
  OADataSourceClient.MyIterator.getMoreFromServer()
  RemoteDataSource.getObject(...)
  OADataSourceObjectCache.select(...)

  Known related CODEX findings:
  none observed in datasource package; graph identity invariants cover this more broadly.

  Suggested unit tests:
  testDatasourceGetObjectReturnsCachedIdentityForSameKey()
  testRemoteSelectUpdatesClientObjectIdentityCache()

  Spec target section:
  Datasource Runtime / Identity Semantics

  DS-CACHE-001 — Object-Cache Datasource Selects From OAObjectCache

  Contract statement:
  OADataSourceObjectCache must select from the authoritative OA object cache, not from a separate divergent object
  store.

  Rationale:
  The object-cache datasource is a view of graph runtime identity. Maintaining a second cache would create identity
  and delete divergence.

  Source locations:
  OADataSourceObjectCache.select(...)
  ObjectCacheIterator
  OAObjectCacheService.find(...)

  Known related CODEX findings:
  Previous object-cache private set behavior was removed/accepted.

  Suggested unit tests:
  testObjectCacheDatasourceSelectUsesGlobalObjectCache()
  testDeletedObjectIsNotReturnedFromObjectCacheDatasource()

  Spec target section:
  Datasource Runtime / Object Cache Datasource

  DS-CACHE-002 — Object-Cache Detail Selection Must Respect Where-Object Link Scope

  Contract statement:
  When selecting through whereObject and propertyFromWhereObject, results must be limited to the linked/detail
  scope, or empty if the scope cannot be resolved.

  Rationale:
  Detail Hubs depend on datasource selection not accidentally widening into all objects of the detail class.

  Source locations:
  OADataSourceObjectCache.select(...)
  RemoteDataSource.datasource(...) COUNT / SELECT

  Known related CODEX findings:
  Remote stale/missing where-object can become unscoped select; CODEX-commented.

  Suggested unit tests:
  testObjectCacheDetailSelectReturnsOnlyLinkedObjects()
  testRemoteDetailSelectMissingMasterReturnsEmptyWhenFixed()

  Spec target section:
  Datasource Runtime / Detail Select Semantics

  5. Client/Server/SingleUser Routing Contracts

  DS-CS-001 — Client Datasource Must Delegate Class Operations To Remote Server

  Contract statement:
  OADataSourceClient must forward supported class operations to the remote datasource and must not silently perform
  local persistence unless explicitly using local select-all cache optimization.

  Rationale:
  Client/server OA relies on the server being persistence-authoritative.

  Source locations:
  OADataSourceClient.insert/update/save/delete/select/count
  RemoteDataSource.datasource(...)

  Known related CODEX findings:
  Client datasource registration path is CODEX-commented.

  Suggested unit tests:
  testClientInsertDelegatesToRemoteDatasource()
  testClientSelectDelegatesToRemoteUnlessLocalSelectAllCacheApplies()

  Spec target section:
  Datasource Runtime / Client-Server Routing

  DS-CS-002 — Classless Remote Datasource Commands Need Explicit Default Datasource Semantics

  Contract statement:
  Remote classless commands such as availability, storage support, assign-id-on-create, and execute must either
  route to a defined default datasource or explicitly return unavailable.

  Rationale:
  Classless datasource commands cannot be correctly routed by class. Silent null behavior can make a valid server
  datasource appear unavailable.

  Source locations:
  RemoteDataSource.getDataSource()
  RemoteDataSource.datasource(...) IS_AVAILABLE / SUPPORTSSTORAGE / EXECUTE / GET_ASSIGN_ID_ON_CREATE

  Known related CODEX findings:
  Remote classless default datasource behavior is CODEX-commented.

  Suggested unit tests:
  testRemoteClasslessSupportsStorageUsesDefaultDatasourceWhenConfigured()
  testRemoteClasslessExecuteHasDefinedNoDatasourceResult()

  Spec target section:
  Datasource Runtime / Remote Default Datasource

  DS-CS-003 — SingleUser Must Use Local Datasource Path

  Contract statement:
  In SingleUser mode, datasource operations must execute locally through the normal runtime datasource registry, not
  through client/server remote routing.

  Rationale:
  SingleUser is standalone/local. Treating it as client or actual sync server can skip local persistence or attempt
  nonexistent routing.

  Source locations:
  OARuntime.datasource()
  OAObjectDSService
  OADataSourceClient.isClient()

  Known related CODEX findings:
  none observed in datasource package.

  Suggested unit tests:
  testSingleUserSaveUsesLocalDatasource()
  testSingleUserSelectDoesNotUseOADataSourceClient()

  Spec target section:
  Datasource Runtime / SingleUser Semantics

  6. Autonumber Contracts

  DS-AUTONUM-001 — Autonumber Assignment Must Be Unique Per Class And Property

  Contract statement:
  For each supported class/property sequence, assignId(obj) must assign a unique value not already present in graph
  cache for that class.

  Rationale:
  Duplicate object keys corrupt graph identity, Hub membership, datasource updates, and replication.

  Source locations:
  OADataSourceAuto.getNextNumber(...)
  OADataSourceAuto.assignId(...)
  NextNumber

  Known related CODEX findings:
  startNextNumber adjustment race is CODEX-commented.

  Suggested unit tests:
  testAutonumberAssignsUniqueIdsForSameClass()
  testConcurrentAutonumberAssignmentDoesNotDuplicateIdsWhenFixed()

  Spec target section:
  Datasource Runtime / Autonumber Semantics

  DS-AUTONUM-002 — Starting Next Number Is A Lower Bound

  Contract statement:
  When setStartingNextNumber(x) is configured, assigned IDs for that datasource sequence must never be less than x.

  Rationale:
  Import/migration and external sequence coordination depend on start values not being ignored or regressed.

  Source locations:
  OADataSourceAuto.setStartingNextNumber(...)
  OADataSourceAuto.getNextNumber(...)
  OADataSourceAuto.assignId(...)

  Known related CODEX findings:
  Concurrent lower-bound adjustment race is CODEX-commented.

  Suggested unit tests:
  testStartingNextNumberActsAsLowerBound()
  testConcurrentStartingNextNumberDoesNotRegressWhenFixed()

  Spec target section:
  Datasource Runtime / Autonumber Lower Bound

  DS-AUTONUM-003 — Autonumber Datasource Is Not A Storage Datasource

  Contract statement:
  OADataSourceAuto may support ID assignment, but must not be treated as supporting select/count/storage
  persistence.

  Rationale:
  Autonumber is a fallback ID service. Routing it as a persistence datasource can produce false success or null
  selects.

  Source locations:
  OADataSourceAuto.supportsStorage()
  OADataSourceAuto.select(...)
  OADataSourceAuto.count(...)
  OADataSourceService.get(...)

  Known related CODEX findings:
  none observed.

  Suggested unit tests:
  testAutonumberSupportsStorageFalse()
  testAutonumberSelectReturnsNoPersistenceResults()

  Spec target section:
  Datasource Runtime / Autonumber Role

  7. Listener/Event Contracts

  DS-LISTENER-001 — Datasource Save/Delete Listener Exceptions Must Not Be Hidden As Success

  Contract statement:
  If save/delete listener behavior participates in persistence approval or side effects, listener failure must
  either propagate or be explicitly documented as non-authoritative.

  Rationale:
  Silent listener failure can make persistence appear successful while required side effects did not run.

  Source locations:
  OASaveDeleteListener
  OAObjectDSService save/delete integration points

  Known related CODEX findings:
  none observed in datasource package.

  Suggested unit tests:
  testSaveListenerExceptionPreventsFalseSuccess()
  testDeleteListenerExceptionPreventsFalseSuccess()

  Spec target section:
  Datasource Runtime / Listener Semantics

  DS-LISTENER-002 — Listener Ordering Must Be Deterministic Where Semantically Observable

  Contract statement:
  When multiple datasource save/delete listeners are used, their invocation order must be deterministic if
  application behavior can observe side effects.

  Rationale:
  Generated applications and business rules should not depend on accidental iteration order.

  Source locations:
  OASaveDeleteListener
  graph/object datasource service listener usage

  Known related CODEX findings:
  none observed.

  Suggested unit tests:
  testSaveListenersFireInRegisteredOrder()
  testDeleteListenersFireInRegisteredOrder()

  Spec target section:
  Datasource Runtime / Listener Ordering

  8. Failure/Retry/Transaction Contracts

  DS-FAILURE-001 — Datasource Exceptions Must Signal Incomplete Persistence

  Contract statement:
  If insert/update/delete/select fails and throws, OA callers must be able to treat the operation as incomplete.
  Datasource code must not convert failure into apparent success.

  Rationale:
  OA accepts partial-progress semantics, but hidden failure creates stale cache, retry, and replication bugs.

  Source locations:
  OADataSource.save(...)
  OADataSourceClient.save/delete/deleteAll
  RemoteDataSource.datasource(...)

  Known related CODEX findings:
  none observed beyond known remote stale-object cases.

  Suggested unit tests:
  testInsertExceptionPropagatesAsIncompleteOperation()
  testRemoteDeleteFailureDoesNotReportFalseSuccessWhenContractRequires()

  Spec target section:
  Datasource Runtime / Failure Semantics

  DS-FAILURE-002 — Retry After Datasource Failure Must Not Be Made Incorrect By Local State

  Contract statement:
  A failed datasource operation must not mark local object/datasource state as successfully persisted if the
  operation did not complete.

  Rationale:
  Retry must remain meaningful. False persisted state can prevent later correction.

  Source locations:
  OADataSource.save(...)
  OADataSourceAuto.assignId(...)
  RemoteDataSource.datasource(...) INSERT_WO_REFERENCES
  OAObjectDSService

  Known related CODEX findings:
  none observed in datasource package.

  Suggested unit tests:
  testFailedInsertLeavesObjectRetryable()
  testFailedRemoteInsertWithoutReferencesDoesNotMarkNewFalseWhenInsertThrows()

  Spec target section:
  Datasource Runtime / Retry Semantics

  DS-TRANSACTION-001 — Datasource Must Cooperate With Current OATransaction

  Contract statement:
  Datasource implementations must respect active OATransaction context for batch behavior, read-only/write
  exceptions, and transaction-aware persistence where supported.

  Rationale:
  OA applications need graph operations and datasource writes to participate in a consistent transaction boundary.

  Source locations:
  OADataSource.isAllowingBatch()
  OADataSource.isInTransaction()
  OADataSource.getIgnoreWrites()
  OATransaction

  Known related CODEX findings:
  Read-only / ignore-writes contract is CODEX-commented.

  Suggested unit tests:
  testDatasourceSeesActiveTransaction()
  testDatasourceBatchAllowedWhenTransactionUsesBatch()
  testTransactionCanOverrideIgnoreWritesWhenContractRequires()

  Spec target section:
  Datasource Runtime / Transaction Cooperation

  9. Resource Cleanup Contracts

  DS-RESOURCE-001 — Query Iterators Must Be Closed On All Normal Object Lookup Paths

  Contract statement:
  Any helper method that opens a datasource iterator for lookup must close it in finally.

  Rationale:
  Remote and streaming datasources can hold server cursors, sockets, result sets, or cache references.

  Source locations:
  OADataSource.getObject(...)
  OADataSourceIterator.remove()
  OADataSourceClient.MyIterator.remove()

  Known related CODEX findings:
  OADataSource.getObject(...) iterator cleanup was fixed.

  Suggested unit tests:
  testGetObjectClosesIteratorWhenHasNextThrows()
  testGetObjectClosesIteratorWhenNextThrows()

  Spec target section:
  Datasource Runtime / Iterator Cleanup

  DS-RESOURCE-002 — Remote Select Iterator IDs Must Be Released

  Contract statement:
  Every remote select iterator ID created by RemoteDataSource must be removed when exhausted or explicitly closed by
  the client.

  Rationale:
  Server-side iterator leaks can retain datasource resources and cached object references.

  Source locations:
  RemoteDataSource.datasource(...) SELECT / SELECTPASSTHRU / IT_REMOVE
  RemoteDataSource.datasourceNext(...)
  OADataSourceClient.MyIterator.remove()

  Known related CODEX findings:
  none observed.

  Suggested unit tests:
  testRemoteIteratorRemovedWhenExhausted()
  testRemoteIteratorRemovedWhenClientCloses()

  Spec target section:
  Datasource Runtime / Remote Iterator Cleanup

  DS-RESOURCE-003 — Storage File Streams And Compressors Must Be Closed/Ended On Failure

  Contract statement:
  Object-cache storage save/load must close streams and end compressor/decompressor state even when serialization or
  deserialization throws.

  Rationale:
  Object-cache datasource is often used for local persistence/testing. File descriptor or inflater/deflater leaks
  can destabilize long-running tools.

  Source locations:
  OADataSourceObjectCache.saveToStorageFile(...)
  OADataSourceObjectCache.loadFromStorageFile(...)

  Known related CODEX findings:
  none observed; cleanup currently uses finally blocks.

  Suggested unit tests:
  testSaveToStorageFileClosesResourcesWhenSerializationThrows()
  testLoadFromStorageFileClosesResourcesWhenDeserializationThrows()

  Spec target section:
  Datasource Runtime / Storage Resource Cleanup

  10. Test Coverage Matrix

  Core runtime routing:

  - testDatasourceRoutingUsesFirstNonLastSupportedDatasource
  - testDatasourceRoutingUsesLastDatasourceOnlyAsFallback
  - testDisabledDatasourceIsSkipped
  - testDatasourceConstructorDoesNotRegisterAutomatically

  Save/delete:

  - testSaveNewObjectDelegatesToInsert
  - testSaveExistingObjectDelegatesToUpdate
  - testDeleteRoutesToClassDatasource
  - testDeleteAllRoutesToClassDatasourceOnly
  - testReadOnlyDatasourceBlocksNormalSaveWhenContractRequires

  Select/count/iterator:

  - testObjectCacheSelectNoResultsReturnsEmptyIterator
  - testCountMatchesSelectForWhereObjectDetail
  - testListIteratorHasNextDoesNotSkip
  - testObjectCacheIteratorHasNextNextSequence
  - testRemoteIteratorHasNextNextSequence
  - testGetObjectClosesIteratorInFinally

  Identity/cache:

  - testDatasourceGetObjectReturnsCachedIdentityForSameKey
  - testRemoteSelectUpdatesClientObjectIdentityCache
  - testObjectCacheDatasourceSelectUsesGlobalObjectCache
  - testDeletedObjectIsNotReturnedFromObjectCacheDatasource

  Client/server/single-user:

  - testClientInsertDelegatesToRemoteDatasource
  - testClientSelectDelegatesToRemoteUnlessLocalSelectAllCacheApplies
  - testRemoteClasslessSupportsStorageUsesDefaultDatasourceWhenConfigured
  - testSingleUserSaveUsesLocalDatasource
  - testSingleUserSelectDoesNotUseOADataSourceClient

  Autonumber:

  - testAutonumberAssignsUniqueIdsForSameClass
  - testStartingNextNumberActsAsLowerBound
  - testAutonumberSupportsStorageFalse
  - testAutonumberSelectReturnsNoPersistenceResults

  Failure/retry/transaction:

  - testInsertExceptionPropagatesAsIncompleteOperation
  - testFailedInsertLeavesObjectRetryable
  - testDatasourceSeesActiveTransaction
  - testDatasourceBatchAllowedWhenTransactionUsesBatch

  Resource cleanup:

  - testRemoteIteratorRemovedWhenExhausted
  - testRemoteIteratorRemovedWhenClientCloses
  - testSaveToStorageFileClosesResourcesWhenSerializationThrows
  - testLoadFromStorageFileClosesResourcesWhenDeserializationThrows


*/

/*qqqqqqqqqqqqqqqqqqqqqq Other
CODEX

INVARIANTS


once all of the reported items are addressed, how do you feel it is ready to move forward for Production
  Application development?

• Yes, assuming the reported items are fixed or explicitly CODEX-deferred with tests around the intended behavior,
  com.viaoa.datasource looks ready to move forward for production application development.

  My confidence would be highest if these invariants are proven by tests:

  - datasource routing always picks the correct datasource by class, enabled state, and getLast()
  - OADataSourceAuto only participates as intended for ID assignment
  - client/server select/count/save/delete paths behave identically to local datasource paths where they should
  - iterators are always closed through the OA select lifecycle
  - object-cache datasource returns correct identity-stable objects
  - save/delete failures do not create false success
  - SingleUser, Server, and Client modes each use the correct datasource path
  - read-only / ignore-writes behavior is either enforced or documented as advisory

  The design is not toy-level. It is a real kernel persistence abstraction: local storage, object cache,
  autonumbering, and remote datasource delegation all sit behind one semantic contract. Once the known correctness
  edges are covered, I would consider it production-suitable for OA applications.

  The next risk is not the datasource package by itself. It is integration: graph lifecycle, hub loading, object
  identity, sync/replication, and datasource routing must be tested together. If those cross-package tests pass,
  this is ready to build serious production systems on.




*/


