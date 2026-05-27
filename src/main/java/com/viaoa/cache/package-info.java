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
 * 
  * </p>
 */
package com.viaoa.cache;

//CODEX unit tests <todo>

/* CODEX Invariants

CACHE-IDENTITY-001 — One Authoritative Cached Identity Per Graph/Class/Key
Contract statement: Within one graph/cache scope, a class plus authoritative identity key must resolve to at most
one live OAObject instance unless an explicit detached/non-authoritative state is documented.
Rationale: OAObject references, Hubs, datasource loads, serialization, sync, and replication rely on identity
stability.
Source scope: OAObjectCache, OAObjectIndex, OAObjectIndexKey, OAObjectCacheListener, graph object cache service.
Related CODEX findings: Identity/cache drift findings from graph/cache scans.
Suggested unit tests: testSameGuidReturnsSameCachedObject(), testSameClassKeyReturnsSameCachedObject(),
testDuplicateAddUsesConfiguredDuplicatePolicy()
Spec target section: Cache Runtime / Identity Semantics

CACHE-IDENTITY-002 — GUID Is Runtime Identity Lookup
Contract statement: GUID lookup must resolve the live object instance associated with that GUID, or a defined miss
if no live object exists.
Rationale: GUID is OA runtime identity and is used before or independent of persistent object keys.
Source scope: OAObjectCache.getObject(Class, UUID), OAObjectCache.updateObject, OAObjectCache.removeObject,
OAObjectIndex GUID mappings.
Related CODEX findings: GUID/business-key distinction reviewed in graph/cache/object scans.
Suggested unit tests: testGetByGuidReturnsLiveObject(), testGuidLookupWorksForNewObjectWithoutKey(),
testGetByGuidReturnsNullAfterObjectCollectedAndCleaned()
Spec target section: Cache Runtime / GUID Lookup

CACHE-IDENTITY-003 — GUID And Business-Key Indexes Remain Distinct But Consistent
Contract statement: GUID and business-key indexes may both identify an object, but neither may silently overwrite
the other’s semantic role; both must agree for the same authoritative object after key assignment.
Rationale: New objects can have GUID before persistent keys; loaded objects can arrive by key and later reconcile to
GUID identity.
Source scope: OAObjectCache, OAObjectIndex, OAObjectIndexKey, OAObjectKey integration.
Related CODEX findings: GUID/key distinction and identity reconciliation findings.
Suggested unit tests: testNewObjectGuidLookupWorksBeforeKeyAssigned(), testKeyAssignedObjectResolvesByGuidAndKey(),
testBusinessKeyLookupDoesNotMutateGuidIdentityUnexpectedly()
Spec target section: Cache Runtime / GUID-Key Relationship

CACHE-IDENTITY-004 — Identity Conflicts Follow Defined OA Policy
Contract statement: If GUID identity and business-key identity point to different live objects, cache add/merge/
lookup behavior must follow a defined conflict policy rather than silently returning or installing the wrong
identity.
Rationale: Datasource reload, deserialization, sync, and replication can encounter identity reconciliation
conflicts.
Source scope: OAObjectCache.updateObject, getObject, removeObject, OAObjectIndex add/update paths.
Related CODEX findings: Identity conflict and duplicate authoritative instance risks from graph/cache scans.
Suggested unit tests: testDuplicateGuidDifferentKeyUsesDefinedPolicy(),
testDuplicateKeyDifferentGuidUsesDefinedPolicy(), testIdentityConflictDoesNotReturnWrongObject()
Spec target section: Cache Runtime / Identity Conflict Policy

CACHE-ADD-001 — Cache Add And Update Preserve Index Consistency
Contract statement: Adding or updating an object in cache must consistently publish GUID and key index state, or
fail visibly/return false without leaving split identity indexes.
Rationale: Split GUID/key indexes corrupt object lookup, retry, serialization, and Hub/reference resolution.
Source scope: OAObjectCache.updateObject overloads, OAObjectIndex.addToIndex, updateIndex, OAObjectIndexKey.
Related CODEX findings: Partial cache mutation and index consistency risks.
Suggested unit tests: testAddObjectCreatesGuidAndKeyIndexEntries(), testFailedAddDoesNotLeaveGuidOnlyIndex(),
testFailedUpdateDoesNotLeaveKeyOnlyIndex()
Spec target section: Cache Runtime / Cache Add Update Semantics

CACHE-REMOVE-001 — Cache Remove Targets Only The Intended Identity
Contract statement: Removing an object from cache must remove only the intended runtime identity and associated key/
GUID entries without corrupting unrelated live objects.
Rationale: Delete, eviction, weak cleanup, and reload must not remove newer or unrelated identities.
Source scope: OAObjectCache.removeObject, clearCache, OAObjectIndex.removeFromIndex, weak-reference cleanup paths.
Related CODEX findings: GUID removal/index consistency and stale cleanup findings.
Suggested unit tests: testRemoveByGuidRemovesOnlyMatchingObject(), testRemoveObjectRemovesGuidAndKeyEntries(),
testRemoveOneObjectDoesNotRemoveDifferentKeyObject()
Spec target section: Cache Runtime / Cache Removal Semantics

CACHE-LOOKUP-001 — Lookup Returns Live Object Or Defined Miss
Contract statement: Cache lookup must return a live OAObject reference or a defined miss; it must not return cleared
weak references, stale placeholders, or wrong-class objects.
Rationale: Callers treat returned objects as authoritative graph identities.
Source scope: OAObjectCache.getObject by GUID/key, getRandom, visit/find lookup paths, weak-reference access paths.
Related CODEX findings: Stale weak-ref behavior reviewed.
Suggested unit tests: testClearedWeakReferenceLookupReturnsMiss(), testLookupRemovesStaleReferenceWhenObserved(),
testLookupNeverReturnsWrongClassObject()
Spec target section: Cache Runtime / Lookup Semantics

CACHE-LOOKUP-002 — Lookup Is Deterministic For The Same Cache State
Contract statement: For the same cache state, class, GUID/key, and filter/finder inputs, lookup/visit/find behavior
must produce deterministic object identity results.
Rationale: Deterministic cache lookup is required by datasource refresh, link resolution, sync, serialization, and
tests.
Source scope: OAObjectCache.getObject overloads, visit, find, getRandom, OAObjectIndex.lookupGuid.
Related CODEX findings: Cache lookup determinism and filter behavior reviewed.
Suggested unit tests: testClassKeyLookupReturnsCachedObject(), testClassKeyLookupMissForUnknownKey(),
testVisitFindsSameObjectsForSameCacheState()
Spec target section: Cache Runtime / Deterministic Lookup

CACHE-INDEX-001 — Business-Key Index Maps Class And Key To GUID
Contract statement: The business-key index must map class plus full OAObjectKey/OAObjectIndexKey values to the GUID
of the matching live object or a defined miss.
Rationale: Datasource load, id-only references, lazy references, sync, and serialization resolve object identity
through class/key lookup.
Source scope: OAObjectIndex.addToIndex, lookupGuid, removeFromIndex, updateIndex; OAObjectIndexKey.
Related CODEX findings: Object-id index drift risks reviewed.
Suggested unit tests: testClassKeyLookupReturnsCachedGuid(), testClassKeyLookupMissForUnknownKey(),
testGetObjectByObjectKeyReturnsMatchingObject()
Spec target section: Cache Runtime / Business-Key Lookup

CACHE-INDEX-002 — Key Index Updates On Object Key Change
Contract statement: When an object key changes, old key index entries must be removed and new key entries installed
consistently.
Rationale: Stale key entries can return the wrong object or hide the current object from lookup.
Source scope: OAObjectIndex.updateIndex, addToIndex, removeFromIndex; OAObjectCache.updateObject with key.
Related CODEX findings: Key-change/cache-index consistency findings.
Suggested unit tests: testKeyChangeRemovesOldIndexEntry(), testKeyChangeInstallsNewIndexEntry(),
testOldKeyDoesNotResolveAfterKeyChange()
Spec target section: Cache Runtime / Key Index Consistency

CACHE-INDEX-003 — Composite Keys Preserve Full Component Semantics
Contract statement: Composite object IDs must compare and hash using all ordered key components exactly as defined
by OAObjectKey/OAObjectIndexKey semantics.
Rationale: Partial or wrong-order comparison creates identity collisions or missed cache hits.
Source scope: OAObjectIndexKey constructor, hasValidIds, getIds, equals, hashCode, toString; OAObjectIndex lookup/
add.
Related CODEX findings: none observed.
Suggested unit tests: testCompositeKeyRequiresAllParts(), testCompositeKeyOrderMattersByContract(),
testCompositeKeyHashEqualsMatchesEquals()
Spec target section: Cache Runtime / Composite Key Semantics

CACHE-INDEX-004 — Invalid Or Incomplete Keys Are Not Indexed As Authoritative
Contract statement: Null, partial, or invalid object IDs must not create authoritative key index entries, though the
object may still be cacheable by GUID/runtime identity.
Rationale: Partial keys can collide and resolve the wrong object.
Source scope: OAObjectIndexKey.hasValidIds, OAObjectIndex.addToIndex, OAObjectCache.updateObject.
Related CODEX findings: New object without key and incomplete key behavior reviewed.
Suggested unit tests: testNewObjectWithoutKeyDoesNotCreateKeyIndex(), testPartialCompositeKeyNotIndexed(),
testGuidLookupStillWorksForObjectWithoutValidKey()
Spec target section: Cache Runtime / Invalid Key Semantics

CACHE-GC-001 — Core Cache Uses Weak Reference Semantics
Contract statement: Core cache membership must not keep OAObjects alive unless they are retained elsewhere by
explicit runtime state.
Rationale: OA graphs can be large; cache identity must not become an unbounded strong-retention store.
Source scope: OAObjectCache weak-reference entries, reference queue cleanup, getTotal/clear behavior.
Related CODEX findings: Weak-reference lifecycle behavior reviewed.
Suggested unit tests: testCacheUsesWeakReferenceSemantics(), testUnreferencedCachedObjectCanBeCollected(),
testCollectedObjectEventuallyDisappearsFromCacheCount()
Spec target section: Cache Runtime / Weak Reference Semantics

CACHE-GC-002 — Cleared Weak References Are Not Valid Hits
Contract statement: A weak reference whose referent has been cleared must be treated as absent and must be removed
from lookup/index state when observed or cleaned.
Rationale: Returning dead/stale entries creates false cache hits and broken identity resolution.
Source scope: OAObjectCache lookup paths, checkReferenceQueue, OAObjectIndex cleanup/remove paths.
Related CODEX findings: Cleared weak-ref lookup and stale reference behavior reviewed.
Suggested unit tests: testClearedWeakReferenceNotReturned(), testLookupCleansStaleWeakReferenceIndex(),
testClearedWeakReferenceRemovedFromGuidIndex()
Spec target section: Cache Runtime / Stale Reference Semantics

CACHE-GC-003 — Weak Cleanup Removes All Stale Entries For That Object
Contract statement: When a cached object is GC-cleared, all GUID and business-key index entries for that cleared
object must be eligible for cleanup.
Rationale: Partial cleanup leaves stale key hits or index leaks.
Source scope: OAObjectCache.checkReferenceQueue, clearCache, removeObject, OAObjectIndex remove/clear.
Related CODEX findings: Reference cleanup and index leak risks reviewed.
Suggested unit tests: testWeakCleanupRemovesGuidAndKeyEntries(), testCollectedObjectRemovedFromKeyIndex(),
testCollectedObjectRemovedFromGuidIndex()
Spec target section: Cache Runtime / Weak Cleanup

CACHE-REFQUEUE-001 — Reference Queue Cleanup Preserves Newer Live Entries
Contract statement: Cleanup of an old cleared weak reference must not remove a newer live object that now owns the
same GUID/key entry.
Rationale: Stale cleanup can delete valid cache entries after replacement, reload, or re-add.
Source scope: OAObjectCache.checkReferenceQueue, removeObject, OAObjectIndex remove/update paths.
Related CODEX findings: Stale cleanup compare/remove risks.
Suggested unit tests: testOldClearedReferenceDoesNotRemoveNewLiveKeyEntry(),
testOldClearedReferenceDoesNotRemoveNewLiveGuidEntry(), testReferenceQueueCleanupPreservesLiveEntrySameClass()
Spec target section: Cache Runtime / Stale Reference Cleanup

CACHE-CONCURRENT-001 — Cache Add Remove Lookup Are Thread-Safe
Contract statement: Concurrent cache add, update, remove, cleanup, visit, and lookup must not corrupt GUID/key
indexes, return impossible identity combinations, or throw unrelated concurrent modification errors.
Rationale: OA runtime, server requests, sync, replication, UI, and datasource loading can operate concurrently.
Source scope: OAObjectCache, OAObjectIndex, OAObjectIndexKey, listener/filter/trigger integrations.
Related CODEX findings: Concurrent cache mutation and reference queue cleanup risks.
Suggested unit tests: testConcurrentAddLookupSameKeyStable(), testConcurrentRemoveLookupDoesNotCorruptIndex(),
testReferenceQueueCleanupDuringConcurrentAdd()
Spec target section: Cache Runtime / Concurrency Semantics

CACHE-CONCURRENT-002 — Duplicate Add Policy Is Atomic
Contract statement: Duplicate detection and cache add/update decision must occur atomically enough that concurrent
loads of the same identity cannot publish competing authoritative objects.
Rationale: Concurrent datasource loads or deserialization must not split identity authority.
Source scope: OAObjectCache.updateObject, OAObjectIndex add/update, duplicate policy behavior.
Related CODEX findings: Duplicate add and identity conflict risks.
Suggested unit tests: testConcurrentDuplicateAddCreatesSingleIdentity(),
testConcurrentDuplicateAddPolicyDeterministic(), testConcurrentDuplicateGuidDoesNotCreateSecondLiveIdentity()
Spec target section: Cache Runtime / Duplicate Add Concurrency

CACHE-LIFECYCLE-001 — New Objects Are Cacheable By Runtime Identity
Contract statement: New objects without persistent keys may be cached and found by GUID/runtime identity without
creating invalid business-key index entries.
Rationale: Unsaved objects participate in Hubs, links, UI, sync, serialization, and graph traversal before
persistent keys exist.
Source scope: OAObjectCache.updateObject, getObject by GUID, OAObjectIndex.addToIndex, OAObjectIndexKey.hasValidIds.
Related CODEX findings: New object GUID/key distinction reviewed.
Suggested unit tests: testNewObjectCachedByGuidBeforeKey(), testNewObjectWithoutKeyDoesNotCreateKeyIndex(),
testGuidLookupSurvivesBusinessKeyAssignment()
Spec target section: Cache Runtime / New Object Semantics

CACHE-LIFECYCLE-002 — Deleted Objects Do Not Remain Authoritative Cache Hits
Contract statement: After authoritative delete completion, cache lookup must not return the deleted object as a live
persistent object.
Rationale: Prevents ghost references, stale Hub membership, datasource/cache divergence, and sync conflicts.
Source scope: OAObjectCache.removeObject, clearCache, graph object delete/cache service integration.
Related CODEX findings: Delete/cache coordination findings from graph/object/hub scans.
Suggested unit tests: testDeletedObjectRemovedFromCacheIndexes(), testDeletedObjectNotReturnedByKeyLookup(),
testFailedDeleteDoesNotPrematurelyEvictObject()
Spec target section: Cache Runtime / Deleted Object Semantics

CACHE-LISTENER-001 — Cache Listeners Observe Completed Cache Mutations
Contract statement: Cache add/remove/load/property listeners must observe cache state after the mutation they are
notified about is complete.
Rationale: Listener logic may create Hubs, update views, trigger sync, or perform graph/runtime side effects based
on cache state.
Source scope: OAObjectCacheListener, OACacheListenerUtil, OAObjectCacheHubAdder, OAObjectCacheFilter,
OAObjectCacheTrigger.
Related CODEX findings: Listener/event ordering risks reviewed.
Suggested unit tests: testAddListenerCanLookupAddedObject(), testRemoveListenerCannotLookupRemovedObjectByKey(),
testAfterLoadListenerSeesCachedObject()
Spec target section: Cache Runtime / Listener Ordering

CACHE-LISTENER-002 — Listener Failure Policy Is Explicit
Contract statement: If cache listener failure is allowed to abort a mutation, the cache mutation must not appear
completed; otherwise listener failure must be clearly non-authoritative and must not corrupt indexes.
Rationale: Listener exceptions must not create silent split cache/listener side effects.
Source scope: OACacheListenerUtil, OAObjectCacheListener dispatch, OAObjectCacheTrigger, OAObjectCacheFilter.
Related CODEX findings: False-success listener risks reviewed.
Suggested unit tests: testListenerExceptionPolicyDefinedForAdd(), testListenerExceptionDoesNotCorruptCacheIndex(),
testListenerClosePreventsFutureCallbacks()
Spec target section: Cache Runtime / Listener Failure Semantics

CACHE-TRIGGER-001 — Cache Triggers Observe Stable Cache State
Contract statement: Cache triggers and dependent-property listeners must not observe or create partially updated
index state that violates identity invariants.
Rationale: Triggered side effects can recursively query, filter, or mutate cache/Hubs.
Source scope: OAObjectCacheTrigger, OAObjectCacheFilter, OACacheListenerUtil, dependent property trigger setup.
Related CODEX findings: Trigger/reentrancy risks reviewed.
Suggested unit tests: testCacheAddTriggerCanQueryStableIndex(), testReentrantCacheMutationDoesNotCorruptIndex(),
testDependentPropertyTriggerRefreshUsesStableCacheState()
Spec target section: Cache Runtime / Trigger Reentrancy

CACHE-FILTER-001 — Cache Filters Restrict Results Without Mutating Cache Identity
Contract statement: Filtering cache results must affect selection/view membership only; it must not alter cached
identity, GUID indexes, key indexes, or authoritative cache membership.
Rationale: Filters are views over cache, not cache mutation commands.
Source scope: OAObjectCacheFilter, OAObjectCache.find, visit, OAObjectCacheHubAdder, OAFilter integration.
Related CODEX findings: Cache filter/iterator behavior reviewed.
Suggested unit tests: testCacheFilterExcludesNonMatchingObject(), testCacheFilterDoesNotRemoveObjectFromCache(),
testAddingFilterRefreshesViewWithoutChangingCacheIdentity()
Spec target section: Cache Runtime / Filter Semantics

CACHE-HUBADDER-001 — Cache Hub Adders Add Only Matching Live Objects
Contract statement: Cache-to-Hub adders and select-all Hub integrations must add only live objects matching the Hub/
class/filter contract and must remove/ignore objects when they no longer qualify according to contract.
Rationale: Select-all Hubs and generated views rely on cache-driven membership without corrupting Hub contents.
Source scope: OAObjectCacheHubAdder, OAObjectCacheFilter, Hub integration, listener callbacks.
Related CODEX findings: Cache Hub adder behavior reviewed.
Suggested unit tests: testCacheHubAdderAddsLiveMatchingObject(),
testCacheHubAdderSkipsCollectedOrWrongClassObject(), testCacheHubAdderCloseStopsMembershipUpdates()
Spec target section: Cache Runtime / Hub Adder Semantics

CACHE-VISIT-001 — Cache Iteration Visits Live Objects Deterministically
Contract statement: Cache visit/find/random APIs must operate on live objects only, respect class/filter/finder
contracts, and define behavior for concurrent mutation.
Rationale: Cache iteration feeds traversal, tooling, filters, Hubs, and runtime analysis.
Source scope: OAObjectCache.visit overloads, find, getRandom, OAObjectCacheFilter, OAObjectCacheHubAdder.
Related CODEX findings: Cache iterator/filter behavior reviewed.
Suggested unit tests: testVisitSkipsCollectedObjects(), testVisitClassRestrictsToAssignableObjects(),
testFindUsesFinderWithoutChangingCacheState()
Spec target section: Cache Runtime / Iteration Semantics

CACHE-GRAPH-001 — Cache Authority Is Graph-Scoped
Contract statement: Cache identity and indexes must belong to the owning graph/runtime scope and must not silently
mix objects from another graph.
Rationale: Cross-graph cache leakage corrupts object identity, metadata, datasource routing, sync, and
serialization.
Source scope: OAObjectCache, OAObjectIndex, graph object cache service integration.
Related CODEX findings: Graph-scoped identity/cache authority findings.
Suggested unit tests: testSameKeyInDifferentGraphsDoesNotShareCacheIdentity(),
testCacheLookupUsesOwningGraphScope(), testForeignGraphObjectNotInstalledAsLocalAuthority()
Spec target section: Cache Runtime / Graph Ownership

CACHE-SERIAL-001 — Serialization Deserialization Resolves Through Cache Authority
Contract statement: Deserialization and remote/materialized object resolution must use cache identity rules to
return the authoritative cached instance when one exists.
Rationale: Serialization boundaries must not create duplicate object graph identities.
Source scope: OAObjectCache, OAObjectIndex, object serialization integration.
Related CODEX findings: Serialization/deserialization identity findings from graph/object/serialize scans.
Suggested unit tests: testDeserializeExistingKeyReturnsCachedIdentity(),
testDeserializeExistingGuidReturnsCachedIdentity(), testDeserializeConflictUsesDefinedCachePolicy()
Spec target section: Cache Runtime / Serialization Integration

CACHE-DS-001 — Datasource Refresh And Load Coordinate With Cache Identity
Contract statement: Datasource-loaded or refreshed objects must merge with or update the authoritative cached
identity according to cache conflict and lifecycle rules.
Rationale: Datasource refresh must not create duplicates, stale keys, or wrong live instances.
Source scope: OAObjectCache.updateObject, getObject by key, OAObjectIndex update, datasource service integration.
Related CODEX findings: Datasource load/cache merge and refresh coordination findings.
Suggested unit tests: testDatasourceLoadMergesWithCachedObject(), testDatasourceRefreshUpdatesSameCachedIdentity(),
testDatasourceLoadConflictUsesDefinedPolicy()
Spec target section: Cache Runtime / Datasource Integration

CACHE-SYNC-001 — Sync And Replication Preserve Cache Identity
Contract statement: Sync and replication object resolution must use cache authority so remote changes apply to the
correct live instance or fail visibly according to conflict policy.
Rationale: Distributed OA runtimes depend on stable identity across sync/replay.
Source scope: OAObjectCache, OAObjectIndex, graph sync/replication integration.
Related CODEX findings: Sync/replication cache assumptions reviewed in graph/sync/replication scans.
Suggested unit tests: testSyncUpdateAppliesToCachedIdentity(), testReplicationReplayUsesCacheKeyResolution(),
testSyncConflictDoesNotReturnWrongObject()
Spec target section: Cache Runtime / Sync Replication Integration

CACHE-FAILURE-001 — Cache Mutation Failure Must Not Leave Split Index State
Contract statement: If cache add, remove, update, key-change, cleanup, or listener-mediated mutation fails visibly,
GUID and key indexes must not be left disagreeing for the same authoritative object.
Rationale: Split indexes corrupt object identity and retry behavior.
Source scope: OAObjectCache.updateObject/removeObject, OAObjectIndex.addToIndex/removeFromIndex/updateIndex, listen
er/filter integrations.
Related CODEX findings: Partial mutation/index consistency risks reviewed.
Suggested unit tests: testFailedAddDoesNotLeaveGuidOnlyIndex(), testFailedRemoveDoesNotLeaveKeyOnlyIndex(),
testFailedKeyUpdateDoesNotLeaveOldAndNewKeysBothAuthoritative()
Spec target section: Cache Runtime / Failure Consistency

CACHE-FAILURE-002 — Cache Must Prefer Miss Or Visible Failure Over Wrong Identity
Contract statement: When cache cannot safely resolve identity conflict, stale state, invalid key, or cleanup race,
it must return a defined miss or fail visibly rather than return a semantically wrong object.
Rationale: Wrong identity is more damaging than a cache miss because it corrupts graph semantics.
Source scope: OAObjectCache.getObject/find/getRandom, OAObjectIndex.lookupGuid, conflict paths.
Related CODEX findings: Silent corruption and false-success risks reviewed.
Suggested unit tests: testIdentityConflictDoesNotReturnWrongObject(),
testStaleIndexReturnsNullInsteadOfWrongObject(), testInvalidKeyLookupReturnsDefinedMiss()
Spec target section: Cache Runtime / Silent Corruption Prevention

CACHE-DETERMINISM-001 — Same Cache State Produces Same Observable Behavior
Contract statement: For the same graph scope, object state, GUID/key indexes, weak-reference state, filters, and
concurrency-free inputs, cache APIs must produce deterministic lookup, visit, listener, and Hub-adder behavior.
Rationale: Deterministic cache behavior is required for object identity, datasource loading, serialization, sync, UI
views, and tests.
Source scope: OAObjectCache, OAObjectIndex, OAObjectIndexKey, OAObjectCacheFilter, OAObjectCacheHubAdder,
OAObjectCacheTrigger, OACacheListenerUtil.
Related CODEX findings: Package-wide identity, weak-reference, index, listener, filter, and failure findings.
Suggested unit tests: testSameCacheStateProducesSameGuidLookup(), testSameCacheStateProducesSameKeyLookup(),
testSameCacheStateProducesSameFilteredHubMembership()
Spec target section: Cache Runtime / Deterministic Cache Semantics

*/
