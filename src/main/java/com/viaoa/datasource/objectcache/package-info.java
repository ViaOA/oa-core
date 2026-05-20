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
 * Provides an in-memory {@link com.viaoa.datasource.OADataSource}
 * implementation and supporting iterators.
 * <p>
 * Classes in this package allow OA applications to operate without an external
 * database by storing objects directly in memory and serializing them to disk
 * when needed.
 *
 * <ul>
 *   <li>{@link com.viaoa.datasource.objectcache.OADataSourceObjectCache} —
 *       full in-memory data source with compressed save/load support.</li>
 *   <li>{@link com.viaoa.datasource.objectcache.ObjectCacheIterator} —
 *       streaming iterator for cache-based queries.</li>
 * </ul>
 */
package com.viaoa.datasource.objectcache;

/* CODEX Invariants

DSCACHE-RUNTIME-001 — Object-Cache Datasource Authority
Contract statement:
com.viaoa.datasource.objectcache defines the datasource bridge over the live OA object cache, allowing cache-backed
select/query/load behavior and optional storage-file save/load without external database authority.
Rationale:
This package is not generic in-memory collection logic. It exposes live Object Graph cache state through datasource
APIs, so cache identity, metadata, query, traversal, and serialization contracts must remain intact.
Source scope:
OADataSourceObjectCache, ObjectCacheIterator, OADataSourceAuto superclass integration, OARuntime graph/cache
services.
Related CODEX findings:
No direct CODEX comments in package source; package-info is shallow and package behavior implies cache/datasource
boundary contracts.
Suggested unit tests:
testObjectCacheDatasourceSelectsFromRuntimeCache(), testObjectCacheDatasourceDoesNotBypassCacheAuthority(),
testObjectCacheDatasourceStorageRoundTripPreservesCacheIdentity()
Spec target section:
Datasource ObjectCache / Core Responsibility

DSCACHE-AUTHORITY-001 — Cache Versus Datasource Boundary
Contract statement:
OADataSourceObjectCache uses the active graph/object cache as its data source and must not claim stronger external
persistence authority than cache state plus explicit storage-file save/load provide.
Rationale:
Cache-backed datasource success is not the same as external database persistence success. Runtime callers need clear
authority boundaries.
Source scope:
OADataSourceObjectCache.select(...), selectPassthru(...), insert(...), insertWithoutReferences(...),
saveToStorageFile(...), loadFromStorageFile(...).
Related CODEX findings:
None observed.
Suggested unit tests:
testObjectCacheDatasourceSelectReflectsCacheState(),
testObjectCacheDatasourceStorageFileIsExplicitPersistenceBoundary(),
testObjectCacheDatasourceDoesNotClaimExternalDatabaseAuthority()
Spec target section:
Datasource ObjectCache / Authority Boundary

DSCACHE-IDENTITY-001 — Cache Identity Consistency
Contract statement:
Cache-backed selection and storage load/save must preserve OAObject identity, OAObjectKey semantics, cache
authority, and duplicate-object prevention for each graph/class.
Rationale:
The object cache is an identity authority. Returning or loading duplicate semantic objects corrupts links, Hub
membership, sync, replication, and datasource behavior.
Source scope:
OADataSourceObjectCache.select(...), ObjectCacheIterator, loadFromStorageFile(...), _loadFromStorageFile(...),
OAObjectInputStream.resolveObject(...), OARuntime graph object-cache services.
Related CODEX findings:
None observed.
Suggested unit tests:
testObjectCacheIteratorReturnsCachedAuthoritativeInstance(), testStorageLoadReconcilesObjectsThroughCacheIdentity(),
testCacheBackedSelectDoesNotCreateDuplicateObjectForSameKey()
Spec target section:
Datasource ObjectCache / Identity Semantics

DSCACHE-INSERT-001 — Cache Class Visibility
Contract statement:
Inserting an object through OADataSourceObjectCache must make the object’s class visible to cache-backed storage
enumeration and later cache-backed datasource operations without changing object identity incorrectly.
Rationale:
The package tracks participating classes for storage-file save and object-cache datasource behavior.
Source scope:
OADataSourceObjectCache.insert(...), insertWithoutReferences(...), hsClass, OADataSourceAuto.assignId(...).
Related CODEX findings:
None observed.
Suggested unit tests:
testInsertAddsObjectClassToStorageEnumeration(), testInsertWithoutReferencesAddsObjectClassToStorageEnumeration(),
testNullInsertDoesNotChangeClassVisibility()
Spec target section:
Datasource ObjectCache / Insert and Class Tracking

DSCACHE-SELECT-001 — Deterministic Cache Selection
Contract statement:
Given the same graph/cache state, metadata, query text, parameters, where-object context, filter, order expression,
max, and dirty flag, cache-backed selection must produce deterministic eligibility and ordering according to OA
query/path/filter/comparator contracts.
Rationale:
Cache-backed datasource behavior must be predictable enough to substitute for datasource selection in tests, local
runtimes, and client-side cache paths.
Source scope:
OADataSourceObjectCache.select(...), selectPassthru(...), OAQueryFilter, OAAndFilter, OAComparator,
ObjectCacheIterator.
Related CODEX findings:
None observed.
Suggested unit tests:
testObjectCacheSelectAppliesQueryFilterDeterministically(),
testObjectCacheSelectAppliesExtraWhereAndFilterTogether(), testObjectCacheSelectOrderIsDeterministic()
Spec target section:
Datasource ObjectCache / Select Semantics

DSCACHE-QUERY-001 — Query Failure Visibility
Contract statement:
Invalid queryWhere or extraWhere expressions must fail visibly and must not be treated as empty filters, no-result
success, or silent partial selection.
Rationale:
A malformed in-memory query that silently succeeds can return the wrong object set and mislead callers using the
object cache as a datasource.
Source scope:
OADataSourceObjectCache.select(...), OAQueryFilter construction for queryWhere and extraWhere.
Related CODEX findings:
None observed.
Suggested unit tests:
testInvalidQueryWhereFailsVisibly(), testInvalidExtraWhereFailsVisibly(),
testQueryParseFailureDoesNotReturnPartialSelection()
Spec target section:
Datasource ObjectCache / Query Semantics

DSCACHE-WHERE-001 — Where-Object Relationship Semantics
Contract statement:
whereObject and propertyFromWhereObject selection must resolve relationships using OA metadata/path semantics and
must return only objects semantically related to the whereObject under the resolved link, reverse link,
selectFromPropertyPath, equalPropertyPath, or direct property value.
Rationale:
Relationship-based selection must preserve Object Graph link semantics when the cache is used as a datasource.
Source scope:
OADataSourceObjectCache.select(...), OAObjectInfo, OALinkInfo, OAPath, OAFinder, OAEqualFilter, OARuntime graph
property access.
Related CODEX findings:
None observed.
Suggested unit tests:
testSelectFromWhereObjectDirectOneLinkReturnsRelatedObject(), testSelectFromWhereObjectHubLinkReturnsMembers(),
testSelectFromWhereObjectInvalidPropertyPathFailsVisibly()
Spec target section:
Datasource ObjectCache / Relationship Selection

DSCACHE-PATH-001 — Path and Reverse-Path Boundary
Contract statement:
Property-path based cache selection must distinguish valid path syntax, metadata-valid path resolution, reverse-path
availability, and runtime object availability; inability to resolve a required semantic path must fail visibly or
return an explicit empty iterator by contract.
Rationale:
Path resolution drives relationship selection. Confusing invalid metadata with no results hides model errors.
Source scope:
OADataSourceObjectCache.select(...), OAPath, OAPath.getReversePropertyPath(), OALinkInfo.getReverseLinkInfo().
Related CODEX findings:
None observed.
Suggested unit tests:
testValidWherePropertyPathUsesReversePathQuery(), testMissingReversePathReturnsEmptyIteratorByContract(),
testInvalidWherePropertyPathThrowsMetadataFailure()
Spec target section:
Datasource ObjectCache / Path Semantics

DSCACHE-ITERATOR-001 — ObjectCacheIterator Progress
Contract statement:
ObjectCacheIterator must make monotonic progress through cache batches, apply its filter consistently, respect max
result limits, and terminate without duplicate returns or infinite loops.
Rationale:
Iterator correctness determines whether cache-backed selects are complete, bounded, and deterministic.
Source scope:
ObjectCacheIterator.next(), hasNext(), getNext(), _next(), setMax(...), getMax(), lastFetchObject, alFetchObjects,
posFetchObjects, bFetchIsDone.
Related CODEX findings:
None observed.
Suggested unit tests:
testObjectCacheIteratorReturnsEachMatchingObjectOnce(), testObjectCacheIteratorHonorsMaxLimit(),
testObjectCacheIteratorTerminatesWhenCacheExhausted()
Spec target section:
Datasource ObjectCache / Iterator Semantics

DSCACHE-ITERATOR-002 — Iterator Mutation Visibility
Contract statement:
Cache mutation during ObjectCacheIterator traversal must have owner-defined visibility: the iterator may be live/
cache-backed or snapshot-like, but it must not duplicate, skip due to internal corruption, or loop indefinitely
because of concurrent cache changes.
Rationale:
The object cache is live runtime state and can mutate while selection is in progress.
Source scope:
ObjectCacheIterator, OARuntime graph object-cache find service, OADataSourceObjectCache.select(...).
Related CODEX findings:
None observed.
Suggested unit tests:
testObjectCacheIteratorConcurrentMutationHasDefinedVisibility(),
testIteratorDoesNotLoopIndefinitelyWhenCacheChanges(),
testIteratorDoesNotReturnInternalDuplicateUnderConcurrentInsert()
Spec target section:
Datasource ObjectCache / Live Cache Iteration

DSCACHE-FILTER-001 — Filter Composition Semantics
Contract statement:
Cache-backed selection must compose caller filters, query filters, extraWhere filters, relationship filters, and
local iterator filters with deterministic AND semantics unless a specific path returns a directly materialized
related collection.
Rationale:
Filter composition determines selection correctness and must not silently drop one of the caller’s constraints.
Source scope:
OADataSourceObjectCache.select(...), OAAndFilter, OAQueryFilter, OAEqualFilter, ObjectCacheIterator filter use.
Related CODEX findings:
None observed.
Suggested unit tests:
testSelectCombinesCallerFilterAndQueryWhere(), testSelectCombinesExtraWhereAndRelationshipFilter(),
testDirectHubRelationshipSelectionAppliesCallerFilter()
Spec target section:
Datasource ObjectCache / Filter Semantics

DSCACHE-ORDER-001 — Ordering Semantics
Contract statement:
When queryOrder is supplied, cache-backed selection must sort the complete selected result set using OAComparator
before returning it; without queryOrder, iterator order is cache traversal order and must not be treated as sorted.
Rationale:
Ordering affects UI, query, and deterministic test behavior. Sorted and unsorted results have different contracts.
Source scope:
OADataSourceObjectCache.select(...), selectPassthru(...), OAComparator, OADataSourceListIterator,
ObjectCacheIterator.
Related CODEX findings:
None observed.
Suggested unit tests:
testObjectCacheSelectSortsWhenQueryOrderProvided(), testObjectCacheSelectWithoutOrderDoesNotPromiseSortedOrder(),
testRelationshipSelectionSortsMaterializedListWhenOrdered()
Spec target section:
Datasource ObjectCache / Ordering Semantics

DSCACHE-LIMIT-001 — Max Result Limit
Contract statement:
The max result limit must cap the number of returned objects for cache-backed iterators and materialized sorted
selections according to datasource contract.
Rationale:
Limit semantics affect paging, performance, and query correctness.
Source scope:
OADataSourceObjectCache.select(...), ObjectCacheIterator.setMax(...), ObjectCacheIterator._next(),
OADataSourceListIterator use after materialization.
Related CODEX findings:
None observed.
Suggested unit tests:
testObjectCacheIteratorHonorsMaxLimit(), testObjectCacheOrderedSelectionHonorsMaxContract(),
testZeroMaxMeansUnlimitedByContract()
Spec target section:
Datasource ObjectCache / Result Limit Semantics

DSCACHE-STORAGE-001 — Storage File Save Boundary
Contract statement:
saveToStorageFile must serialize a deterministic representation of supported cached classes and optional extra
object, include blobs as configured, flush/finish/close compression streams, and fail visibly on I/O or
serialization failure.
Rationale:
Storage-file save is the explicit persistence boundary for object-cache datasource state. Partial or failed writes
must not appear successful.
Source scope:
OADataSourceObjectCache.saveToStorageFile(...), _saveToStorageFile(...), OAObjectSerializer, DeflaterOutputStream,
ObjectOutputStream, lock.writeLock().
Related CODEX findings:
None observed in package; I/O and serialization boundary contracts apply.
Suggested unit tests:
testSaveToStorageFileWritesClassGroupedCachedObjects(), testSaveToStorageFileIncludesExtraObjectWhenProvided(),
testSaveToStorageFileFailureIsVisible()
Spec target section:
Datasource ObjectCache / Storage Save Semantics

DSCACHE-STORAGE-002 — Storage File Load Boundary
Contract statement:
loadFromStorageFile must deserialize cache state through OAObjectInputStream/OAObjectSerializer, reconcile loaded
objects with runtime identity, update class visibility for loaded objects, and fail visibly on corrupt or incomplete
payloads.
Rationale:
Storage-file load imports object graph state into the live cache; corrupt or partial loads must not appear as
complete cache state.
Source scope:
OADataSourceObjectCache.loadFromStorageFile(...), _loadFromStorageFile(...), OAObjectInputStream.resolveObject(...),
OAObjectSerializer.getObject(), InflaterInputStream.
Related CODEX findings:
None observed in package; serialization/load boundary contracts apply.
Suggested unit tests:
testLoadFromStorageFileRestoresCachedObjects(), testLoadFromStorageFileUpdatesClassVisibility(),
testLoadFromStorageFileCorruptPayloadFailsVisibly()
Spec target section:
Datasource ObjectCache / Storage Load Semantics

DSCACHE-RESOURCE-001 — Storage Resource Cleanup
Contract statement:
File, compression, and object streams opened by storage save/load must be closed or ended on success and failure,
and locks acquired for storage operations must be released with try/finally.
Rationale:
Storage operations can run in long-lived runtimes. Resource or lock leaks can block later selects, saves, or loads.
Source scope:
OADataSourceObjectCache.saveToStorageFile(...), loadFromStorageFile(...), Deflater.end(), Inflater.end(), close
calls, lock.writeLock().
Related CODEX findings:
None observed in package.
Suggested unit tests:
testSaveToStorageFileClosesStreamsOnSerializationFailure(), testLoadFromStorageFileClosesStreamsOnReadFailure(),
testStorageWriteLockReleasedAfterFailure()
Spec target section:
Datasource ObjectCache / Resource Cleanup

DSCACHE-LOCK-001 — Storage Lock Boundary
Contract statement:
Storage save/load must use write-lock protection for cache serialization/deserialization boundaries, and concurrent
selects or mutations must have defined visibility relative to that lock boundary.
Rationale:
Storage persistence must not observe internally inconsistent class/cache state or load into a concurrently mutating
state without a defined contract.
Source scope:
OADataSourceObjectCache.lock, saveToStorageFile(...), loadFromStorageFile(...), _saveToStorageFile(...),
_loadFromStorageFile(...), insert(...), insertWithoutReferences(...), select(...).
Related CODEX findings:
None observed.
Suggested unit tests:
testStorageSaveSeesConsistentClassSetUnderConcurrentInsert(),
testStorageLoadLockPreventsConcurrentInconsistentSave(), testSelectDuringStorageOperationHasDefinedVisibility()
Spec target section:
Datasource ObjectCache / Locking and Visibility

DSCACHE-AUTONUM-001 — Autonumber Integration
Contract statement:
Object-cache datasource generated-ID behavior must remain compatible with OADataSourceAuto semantics: ID assignment
must follow metadata, cache collision checks, and configured assign-on-create/insert lifecycle.
Rationale:
In-memory datasource identity still participates in cache keys and object graph identity.
Source scope:
OADataSourceObjectCache extends OADataSourceAuto, assignId(...), insert(...), insertWithoutReferences(...).
Related CODEX findings:
No direct objectcache CODEX; autonumber package has generated identity concurrency/starting-number notes.
Suggested unit tests:
testObjectCacheInsertAssignsIdThroughAutoNumberContract(), testObjectCacheAssignIdUsesAutoNumberMetadata(),
testObjectCacheGeneratedIdDoesNotDuplicateCachedKey()
Spec target section:
Datasource ObjectCache / Autonumber Integration

DSCACHE-FAIL-001 — Failure Visibility
Contract statement:
Failures in cache-backed query parsing, path resolution, metadata resolution, iteration, storage save/load,
serialization, compression, or cache identity reconciliation must be caller-visible or explicitly represented as
empty/no-result by contract; failure must not silently appear as successful selection or persistence.
Rationale:
Object-cache datasource behavior is often used in tests and local runtimes; silent wrong results create false
confidence and runtime divergence.
Source scope:
OADataSourceObjectCache.select(...), selectPassthru(...), saveToStorageFile(...), loadFromStorageFile(...),
ObjectCacheIterator.
Related CODEX findings:
None observed in package.
Suggested unit tests:
testInvalidMetadataSelectionFailsVisibly(), testIteratorCacheFailureDoesNotAppearAsCleanEnd(),
testStorageFailureDoesNotReportSuccess()
Spec target section:
Datasource ObjectCache / Failure Semantics

DSCACHE-NULL-001 — Null, Empty, and No-Result Semantics
Contract statement:
Null file, missing file, null where relationship, no reverse path, empty cache, no matching filter, and unsupported
selection state must return deterministic false, empty iterator, null, or visible failure according to the method
contract.
Rationale:
No-result state and failure state must remain distinguishable where callers depend on datasource correctness.
Source scope:
OADataSourceObjectCache.loadFromStorageFile(...), saveToStorageFile(...), select(...),
ObjectCacheIterator.hasNext(), next().
Related CODEX findings:
None observed.
Suggested unit tests:
testLoadNullFileReturnsFalse(), testLoadMissingFileReturnsFalse(), testNullWhereRelationshipReturnsEmptyIterator(),
testEmptyCacheIteratorHasNoNext()
Spec target section:
Datasource ObjectCache / Null and Empty Semantics

DSCACHE-CONCURRENT-001 — Concurrent Cache-Backed Selection Safety
Contract statement:
Cache-backed datasource selection and iteration must be thread-safe according to the object cache’s concurrency
model, and ObjectCacheIterator instance state must be safe for its documented usage boundary.
Rationale:
Object cache selection can run from UI, background, sync, and test threads while the cache is live.
Source scope:
ObjectCacheIterator synchronized hasNext()/getNext(), _next(), OADataSourceObjectCache.select(...), graph object-
cache find/visit services.
Related CODEX findings:
None observed.
Suggested unit tests:
testObjectCacheIteratorSequentialThreadSafety(), testConcurrentObjectCacheSelectsDoNotCorruptIteratorState(),
testConcurrentCacheMutationDuringSelectionDoesNotThrowUnexpectedly()
Spec target section:
Datasource ObjectCache / Concurrency

DSCACHE-TL-001 — Runtime Context Boundary
Contract statement:
Object-cache datasource operations must not leak ThreadLocal/OAThreadLocal loading, serialization, transaction,
sync, or graph context; any context installed by owning services must be restored with try/finally.
Rationale:
Cache-backed selection and storage can be invoked inside load, serialization, transaction, sync, or test contexts.
Source scope:
OADataSourceObjectCache, ObjectCacheIterator, graph/cache/serialization callers.
Related CODEX findings:
No direct ThreadLocal mutation observed; this is a cross-package integration boundary.
Suggested unit tests:
testObjectCacheSelectDoesNotLeakLoadingContext(), testStorageLoadDoesNotLeakSerializationContext(),
testIteratorFailureDoesNotLeakRuntimeContext()
Spec target section:
Datasource ObjectCache / Runtime Context

DSCACHE-INTEGRATION-001 — Cross-Package Cache Datasource Compatibility
Contract statement:
Object-cache datasource behavior must remain compatible with datasource, cache, object, Hub, select, query, path,
filter, compare, transaction, serialization, sync, replication, autonumber, and graph/runtime contracts.
Rationale:
This package sits at the boundary between datasource APIs and live cache/graph state. It must preserve package
authority boundaries while providing deterministic in-memory datasource behavior.
Source scope:
OADataSourceObjectCache, ObjectCacheIterator, OADataSourceAuto, OARuntime graph services, OAQueryFilter, OAPath,
OAFinder, OAComparator, OAObjectSerializer.
Related CODEX findings:
No package-local CODEX comments; cross-package authority and failure contracts apply.
Suggested unit tests:
testObjectCacheDatasourceCompatibleWithOASelectAndOAQuery(),
testObjectCacheDatasourcePreservesHubRelationshipSelection(),
testObjectCacheStorageRoundTripCompatibleWithSerializationAndCacheContracts()
Spec target section:
Datasource ObjectCache / Cross-Package Integration

*/
