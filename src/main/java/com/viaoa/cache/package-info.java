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


/* CODEX Invariants

1. Cache Identity Contracts

  CACHE-IDENTITY-001 — One Cached Runtime Identity Per GUID
  Contract statement: Within a graph/cache scope, a GUID must resolve to at most one live OAObject identity.
  Rationale: GUID lookup is OA’s runtime identity anchor before or alongside persistent keys.
  Source locations: OAObjectCache, cache add/get/remove paths, graph object cache service.
  Known related CODEX findings: identity/cache drift issues were handled in graph/cache scans.
  Suggested unit tests: testSameGuidReturnsSameCachedObject(), testDuplicateGuidDoesNotCreateSecondLiveIdentity()
  Spec target section: Cache Runtime / GUID Identity

  CACHE-IDENTITY-002 — Cache Add Must Enforce Object Uniqueness By Contract
  Contract statement: Adding an object to cache must honor the configured add/duplicate policy and must not silently
  create conflicting identities.
  Rationale: Duplicate cached identities corrupt Hubs, references, serialization, sync, and datasource merging.
  Source locations: OAObjectCache, object cache service add paths.
  Known related CODEX findings: duplicate/add-mode behavior reviewed.
  Suggested unit tests: testCacheAddRejectsDuplicateWhenNoDups(), testCacheAddIgnoreDupsReturnsExistingIdentity()
  Spec target section: Cache Runtime / Object Uniqueness

  CACHE-IDENTITY-003 — Cache Lookup Must Return Live Object Or Defined Miss
  Contract statement: A cache lookup must return a live object reference or a defined miss; it must not return
  cleared weak references or stale placeholders.
  Rationale: Callers treat returned objects as authoritative graph identities.
  Source locations: OAObjectCache, weak-ref lookup paths.
  Known related CODEX findings: stale weak-ref issues were part of cache scan.
  Suggested unit tests: testClearedWeakReferenceLookupReturnsMiss(), testLookupRemovesStaleReferenceWhenObserved()
  Spec target section: Cache Runtime / Lookup Semantics

  2. GUID Lookup Contracts

  CACHE-GUID-001 — GUID Lookup Is Independent Of Business Key State
  Contract statement: GUID lookup must work even when an object has no persistent key, a temporary key, or a changed
  key.
  Rationale: New/unsaved objects and deserialized objects require runtime identity before persistent identity is
  stable.
  Source locations: OAObjectCache, GUID map/index, object guid service.
  Known related CODEX findings: GUID/business-key distinction reviewed.
  Suggested unit tests: testGuidLookupWorksForNewObjectWithoutKey(), testGuidLookupSurvivesBusinessKeyChange()
  Spec target section: Cache Runtime / GUID Lookup

  CACHE-GUID-002 — GUID Removal Must Remove Only That Runtime Identity
  Contract statement: Removing by GUID/object must remove the intended runtime identity without corrupting unrelated
  business-key entries.
  Rationale: Cache eviction/delete must not remove other objects that share stale or transitional key state.
  Source locations: OAObjectCache, cache remove paths.
  Known related CODEX findings: remove/index consistency reviewed.
  Suggested unit tests: testRemoveByGuidRemovesOnlyMatchingObject(),
  testRemoveOneObjectDoesNotRemoveDifferentKeyObject()
  Spec target section: Cache Runtime / GUID Removal

  3. Business-Key / Object-ID Index Contracts

  CACHE-INDEX-001 — Business-Key Index Maps Class+Key To Live Identity
  Contract statement: For persistent-key lookup, class plus object key must resolve to the live cached identity for
  that persistent object or a defined miss.
  Rationale: Datasource load, references, and sync object resolution depend on class/key lookup.
  Source locations: OAObjectCache, object-id map/index, OAObjectKey.
  Known related CODEX findings: object-id index bugs were scanned.
  Suggested unit tests: testClassKeyLookupReturnsCachedObject(), testClassKeyLookupMissForUnknownKey()
  Spec target section: Cache Runtime / Business-Key Lookup

  CACHE-INDEX-002 — Key Index Must Update On Object Key Change
  Contract statement: When an object’s business key changes, old key index entries must be removed and new key
  entries installed atomically enough to avoid stale lookup.
  Rationale: Stale key entries create duplicate identities or route updates to the wrong object.
  Source locations: OAObjectCache, key-change update paths, graph key/cache services.
  Known related CODEX findings: key-change/cache-index consistency reviewed.
  Suggested unit tests: testKeyChangeRemovesOldIndexEntry(), testKeyChangeInstallsNewIndexEntry(),
  testOldKeyDoesNotReturnObjectAfterChange()
  Spec target section: Cache Runtime / Key Index Consistency

  CACHE-INDEX-003 — Composite Keys Must Preserve Component Semantics
  Contract statement: Composite object keys must index by the ordered component values exactly as defined by
  metadata.
  Rationale: Wrong composite-key behavior returns wrong object identity.
  Source locations: OAObjectCache, OAObjectKey, object key service.
  Known related CODEX findings: none observed.
  Suggested unit tests: testCompositeKeyLookupUsesAllComponents(), testCompositeKeyComponentOrderMatters()
  Spec target section: Cache Runtime / Composite Key Semantics

  4. GUID vs Business-Key Semantics

  CACHE-IDENTITY-004 — GUID Conflict And Key Conflict Resolution Must Be Defined
  Contract statement: If GUID identity and business-key identity point to different live objects, cache add/merge
  behavior must follow a defined conflict policy.
  Rationale: Deserialization, datasource reload, and sync can encounter identity reconciliation cases.
  Source locations: OAObjectCache, graph object cache service add/merge paths.
  Known related CODEX findings: identity reconciliation risks covered in graph/cache scans.
  Suggested unit tests: testGuidConflictUsesDefinedPolicy(), testBusinessKeyConflictUsesDefinedPolicy()
  Spec target section: Cache Runtime / Identity Conflict Resolution

  CACHE-IDENTITY-005 — Business Key Must Not Replace Runtime GUID Identity Silently
  Contract statement: Resolving by business key must not silently mutate or replace an existing GUID identity unless
  reconciliation rules explicitly allow it.
  Rationale: Prevents hidden object substitution in Hubs/references.
  Source locations: OAObjectCache, object cache service.
  Known related CODEX findings: none observed.
  Suggested unit tests: testBusinessKeyLookupDoesNotMutateGuidIdentityUnexpectedly(),
  testIdentityMergeIsObservableByContract()
  Spec target section: Cache Runtime / GUID-Key Interaction

  5. Weak Reference / GC Cleanup Contracts

  CACHE-GC-001 — Cleared Weak References Are Not Valid Cache Hits
  Contract statement: A weak reference whose referent has been cleared must be treated as absent and cleaned from
  indexes.
  Rationale: Returning dead/stale entries breaks identity lookup and can cause false cache hits.
  Source locations: OAObjectCache, weak-reference maps.
  Known related CODEX findings: stale weak-ref behavior reviewed.
  Suggested unit tests: testClearedWeakReferenceNotReturned(), testClearedWeakReferenceRemovedFromGuidIndex()
  Spec target section: Cache Runtime / Weak Reference Semantics

  CACHE-GC-002 — Weak Cleanup Must Remove All Index Entries For Same Object
  Contract statement: When a cached object is GC-cleared, all GUID and business-key index entries for that object
  must be eligible for cleanup.
  Rationale: Partial cleanup leaves stale key hits or index leaks.
  Source locations: OAObjectCache, cleanup/remove paths.
  Known related CODEX findings: reference cleanup issues reviewed.
  Suggested unit tests: testWeakCleanupRemovesGuidAndKeyEntries(), testWeakCleanupDoesNotRemoveLiveDifferentObject()
  Spec target section: Cache Runtime / Weak Cleanup

  6. Reference Queue Contracts

  CACHE-REFQUEUE-001 — Reference Queue Cleanup Must Be Safe Under Mutation
  Contract statement: Processing cleared references from the reference queue must not corrupt indexes while
  concurrent cache add/remove/lookup occurs.
  Rationale: Cache cleanup runs alongside normal graph runtime operations.
  Source locations: OAObjectCache, reference queue cleanup logic.
  Known related CODEX findings: reference queue cleanup and concurrency issues reviewed.
  Suggested unit tests: testReferenceQueueCleanupDuringConcurrentAdd(),
  testReferenceQueueCleanupDuringConcurrentLookup()
  Spec target section: Cache Runtime / Reference Queue Cleanup

  CACHE-REFQUEUE-002 — Reference Queue Cleanup Must Not Remove Newer Identity For Same Key
  Contract statement: Cleanup of an old cleared weak reference must not remove a newer live object that now owns the
  same GUID/key entry.
  Rationale: Stale cleanup can delete valid cache entries after replacement/reload.
  Source locations: OAObjectCache, weak reference cleanup compare/remove paths.
  Known related CODEX findings: stale cleanup CAS/match-value risks reviewed.
  Suggested unit tests: testOldClearedReferenceDoesNotRemoveNewLiveKeyEntry(),
  testOldClearedReferenceDoesNotRemoveNewLiveGuidEntry()
  Spec target section: Cache Runtime / Stale Reference Cleanup

  7. Cache Mutation / Concurrency Contracts

  CACHE-CONCURRENCY-001 — Cache Add/Remove/Lookup Must Be Thread-Safe
  Contract statement: Concurrent cache mutations and lookups must not corrupt GUID or key indexes, return wrong
  identity, or throw unexpected concurrent modification errors.
  Rationale: OA graph runtime can be used by server threads, sync, replication, and UI operations concurrently.
  Source locations: OAObjectCache, graph object cache service.
  Known related CODEX findings: concurrency risks were part of cache review.
  Suggested unit tests: testConcurrentAddLookupSameKeyStable(), testConcurrentRemoveLookupDoesNotCorruptIndex()
  Spec target section: Cache Runtime / Concurrent Mutation

  CACHE-CONCURRENCY-002 — Index Updates Must Be All-Or-Detectably-Incomplete
  1. Cache Identity Contracts

  CACHE-IDENTITY-001 — One Live Cached Identity Per Graph Object Identity
  Contract statement: For a given graph identity, the cache must resolve to one live OAObject instance, not multiple
  competing instances.
  Rationale: OAObject references, Hubs, datasource loads, serialization, sync, and replication rely on identity
  stability.
  Source locations: OAObjectCache, OAObjectCacheListener, graph object cache service.
  Known related CODEX findings: identity/cache drift issues were found in graph/cache scans.
  Suggested unit tests: testSameGuidReturnsSameCachedObject(), testDuplicateAddUsesConfiguredDuplicatePolicy()
  Spec target section: Cache Runtime / Identity Semantics

  CACHE-IDENTITY-002 — Cache Lookup Must Never Return Cleared Weak References As Objects
  Contract statement: Any cache lookup that encounters a cleared weak reference must treat it as absent and clean
  stale index state when required.
  Rationale: Stale weak refs create false cache hits and broken identity resolution.
  Source locations: OAObjectCache, weak-reference lookup/cleanup paths.
  Known related CODEX findings: stale weak-ref behavior was reviewed.
  Suggested unit tests: testClearedWeakReferenceDoesNotReturnObject(), testLookupCleansStaleWeakReferenceIndex()
  Spec target section: Cache Runtime / Stale Reference Semantics

  2. GUID Lookup Contracts

  CACHE-GUID-001 — GUID Lookup Is Runtime Identity Lookup
  Contract statement: Lookup by GUID must resolve the live object instance associated with that GUID, or null if no
  live object exists.
  Rationale: GUID is OA runtime identity and is used before or independent of persistent object keys.
  Source locations: OAObjectCache, object guid service, cache service.
  Known related CODEX findings: none observed beyond identity drift findings.
  Suggested unit tests: testGetByGuidReturnsLiveObject(), testGetByGuidReturnsNullAfterObjectCollectedAndCleaned()
  Spec target section: Cache Runtime / GUID Lookup

  CACHE-GUID-002 — GUID Index Must Update Atomically With Cache Add/Remove
  Contract statement: Adding/removing an object from cache must update GUID index state consistently with the object
  reference.
  Rationale: GUID lookup must not point to removed or wrong objects.
  Source locations: OAObjectCache.add/remove, graph cache service wrappers.
  Known related CODEX findings: cache/index consistency issues reviewed.
  Suggested unit tests: testAddObjectCreatesGuidIndexEntry(), testRemoveObjectRemovesGuidIndexEntry()
  Spec target section: Cache Runtime / GUID Index Consistency

  3. Business-Key / Object-ID Index Contracts

  CACHE-INDEX-001 — Object-ID Lookup Must Resolve Current Key Index
  Contract statement: Lookup by object ID/business key must use the current object key index and return the matching
  live object or null.
  Rationale: Datasource and link resolution depend on business-key lookup.
  Source locations: OAObjectCache, key index methods, graph object key service.
  Known related CODEX findings: object-key index drift issues reviewed.
  Suggested unit tests: testGetByObjectIdReturnsMatchingObject(), testGetByCompositeObjectIdReturnsMatchingObject()
  Spec target section: Cache Runtime / Business-Key Lookup

  CACHE-INDEX-002 — Key Index Must Change When Object Key Changes
  Contract statement: When an object key changes, the old key index must be removed and the new key index installed
  consistently.
  Rationale: Otherwise stale keys can find the wrong object or new keys cannot find the object.
  Source locations: OAObjectCache, key change paths, object key service.
  Known related CODEX findings: key-change/cache-index risks mapped here.
  Suggested unit tests: testKeyChangeRemovesOldIndex(), testKeyChangeAddsNewIndex(),
  testOldKeyDoesNotResolveAfterKeyChange()
  Spec target section: Cache Runtime / Key Index Mutation

  CACHE-INDEX-003 — Composite Keys Must Preserve Full Key Equality
  Contract statement: Composite object IDs must compare all key parts according to OAObjectKey semantics.
  Rationale: Partial or ordered-wrong comparison creates identity collisions.
  Source locations: OAObjectCache, OAObjectKey, key index lookup.
  Known related CODEX findings: none observed.
  Suggested unit tests: testCompositeKeyRequiresAllParts(), testCompositeKeyOrderMattersByContract()
  Spec target section: Cache Runtime / Composite Key Semantics

  4. GUID vs Business-Key Semantics

  CACHE-IDENTITY-003 — GUID And Business Key Are Distinct Indexes For Same Object
  Contract statement: GUID and business-key indexes may both identify an object, but neither may silently overwrite
  the other’s semantic role.
  Rationale: New objects can have GUID before persistent keys; loaded objects can arrive by key.
  Source locations: OAObjectCache, object guid/key services.
  Known related CODEX findings: GUID/key distinction reviewed in graph/object invariants.
  Suggested unit tests: testNewObjectGuidLookupWorksBeforeKeyAssigned(), testKeyAssignedObjectResolvesByGuidAndKey()
  Spec target section: Cache Runtime / GUID-Key Relationship

  CACHE-IDENTITY-004 — Conflicting GUID/Key Must Resolve By Defined OA Policy
  Contract statement: If an object arrives with a GUID/key combination that conflicts with existing cache entries,
  cache behavior must follow a defined conflict policy rather than silently corrupt identity.
  Rationale: Deserialization, sync, and datasource loads can encounter conflicts.
  Source locations: OAObjectCache.add, duplicate handling, graph cache service add modes.
  Known related CODEX findings: duplicate/identity conflict risks reviewed.
  Suggested unit tests: testDuplicateGuidDifferentKeyUsesDefinedPolicy(),
  testDuplicateKeyDifferentGuidUsesDefinedPolicy()
  Spec target section: Cache Runtime / Identity Conflict Policy

  5. Weak Reference / GC Cleanup Contracts

  CACHE-GC-001 — Cache Does Not Keep Objects Alive Unless Explicitly Retained Elsewhere
  Contract statement: Core object cache entries must use weak references so normal cache membership alone does not
  prevent garbage collection.
  Rationale: OA graphs can be large; cache identity should not become an unbounded strong-retention store.
  Source locations: OAObjectCache, weak-reference entry classes, reference cleanup paths.
  Known related CODEX findings: weak-reference behavior reviewed.
  Suggested unit tests: testCacheUsesWeakReferenceSemantics(), testUnreferencedCachedObjectCanBeCollected()
  Spec target section: Cache Runtime / Weak Reference Semantics

  CACHE-GC-002 — Cleared Weak References Must Be Removed From All Indexes
  Contract statement: When a cached object is collected, GUID and key indexes must eventually remove the stale
  entry.
  Rationale: Prevents stale lookup false positives and index growth.
  Source locations: OAObjectCache, reference cleanup methods.
  Known related CODEX findings: reference-queue cleanup risks reviewed.
  Suggested unit tests: testCollectedObjectRemovedFromGuidIndex(), testCollectedObjectRemovedFromKeyIndex()
  Spec target section: Cache Runtime / GC Cleanup

  6. Reference Queue Contracts

  CACHE-REFQUEUE-001 — Reference Queue Cleanup Must Preserve Live Entries
  Contract statement: Cleanup of collected weak references must remove only stale entries and must not remove live
  objects sharing nearby indexes/classes.
  Rationale: Cleanup is background/incremental; over-removal breaks identity lookup.
  Source locations: OAObjectCache, reference queue cleanup logic.
  Known related CODEX findings: reference queue cleanup issues reviewed.
  Suggested unit tests: testReferenceQueueCleanupRemovesOnlyCollectedEntry(),
  testReferenceQueueCleanupPreservesLiveEntrySameClass()
  Spec target section: Cache Runtime / Reference Queue Cleanup

  CACHE-REFQUEUE-002 — Lookup May Opportunistically Clean Stale Entries
  Contract statement: Lookup paths may clean stale weak refs encountered during lookup as long as cleanup preserves
  correctness.
  Rationale: Avoids requiring separate cleanup timing for correctness.
  Source locations: OAObjectCache lookup methods.
  Known related CODEX findings: stale weak-ref lookup behavior reviewed.
  Suggested unit tests: testLookupCleansClearedGuidReference(), testLookupCleansClearedKeyReference()
  Spec target section: Cache Runtime / Opportunistic Cleanup

  7. Cache Mutation / Concurrency Contracts

  CACHE-CONCURRENCY-001 — Cache Add/Remove/Lookup Must Be Thread-Safe
  Contract statement: Concurrent cache mutation and lookup must not corrupt indexes, throw unrelated runtime
  exceptions, or return impossible identity combinations.
  Rationale: OA runtime, sync, UI, and datasource loading can operate concurrently.
  Source locations: OAObjectCache, graph object cache service.
  Known related CODEX findings: concurrent cache mutation risks reviewed.
  Suggested unit tests: testConcurrentAddLookupSameClassStable(), testConcurrentRemoveLookupDoesNotCorruptIndex()
  Spec target section: Cache Runtime / Concurrency Semantics

  CACHE-CONCURRENCY-002 — Duplicate Add Policy Must Be Atomic
  Contract statement: Duplicate detection and add decision must occur as one atomic cache operation.
  Rationale: Two concurrent loads of the same key must not create competing cached identities.
  Source locations: OAObjectCache.add, graph cache service add modes.
  Known related CODEX findings: duplicate add risks reviewed.
  Suggested unit tests: testConcurrentDuplicateAddCreatesSingleIdentity(),
  testConcurrentDuplicateAddPolicyDeterministic()
  Spec target section: Cache Runtime / Duplicate Add Concurrency

  8. Deleted / New Object Contracts

  CACHE-LIFECYCLE-001 — New Objects Are Cacheable By Runtime Identity
  Contract statement: New objects without persistent keys may still be cached and found by GUID/runtime identity.
  Rationale: Unsaved objects participate in Hubs, links, UI, sync, and serialization.
  Source locations: OAObjectCache, guid service, object cache service.
  Known related CODEX findings: none observed.
  Suggested unit tests: testNewObjectCachedByGuidBeforeKey(), testNewObjectWithoutKeyDoesNotCreateKeyIndex()
  Spec target section: Cache Runtime / New Object Semantics

  CACHE-LIFECYCLE-002 — Deleted Objects Must Not Remain Authoritative Cache Hits
  Contract statement: After authoritative delete completion, cache lookup must not return the deleted object as a
  live persistent object.
  Rationale: Prevents ghost references and datasource/cache divergence.
  Source locations: OAObjectCache, object delete/cache service.
  Known related CODEX findings: delete/cache coordination issues reviewed.
  Suggested unit tests: testDeletedObjectRemovedFromCacheIndexes(), testDeletedObjectNotReturnedByKeyLookup()
  Spec target section: Cache Runtime / Deleted Object Semantics

  9. Cache Listener / Trigger Contracts

  CACHE-LISTENER-001 — Cache Add/Remove Listeners Observe Completed Cache Mutation
  Contract statement: Cache listeners must observe cache state after the add/remove mutation they are notified about
  has completed.
  Rationale: Listener logic often creates Hubs, triggers UI, or sync behavior based on cache state.
  Source locations: OAObjectCacheListener, cache service listener dispatch.
  Known related CODEX findings: listener/event ordering issues reviewed in graph/cache scans.
  Suggested unit tests: testAddListenerCanLookupAddedObject(), testRemoveListenerCannotLookupRemovedObjectByKey()
  Spec target section: Cache Runtime / Listener Ordering

  CACHE-LISTENER-002 — Listener Failure Must Not Produce False Cache Success
  Contract statement: If cache listener failure is contractually allowed to abort an operation, cache mutation must
  not appear completed; otherwise listener failure must be clearly non-authoritative.
  Rationale: Prevents silent inconsistent side effects.
  Source locations: cache listener dispatch, graph cache service.
  Known related CODEX findings: false-success listener risks reviewed.
  Suggested unit tests: testListenerExceptionPolicyDefinedForAdd(), testListenerExceptionDoesNotCorruptCacheIndex()
  Spec target section: Cache Runtime / Listener Failure Semantics

  CACHE-TRIGGER-001 — Cache Triggers Must Not Reenter Into Corrupt Mutation State
  Contract statement: Cache trigger/listener callbacks must not observe or create partially updated index state that
  violates identity invariants.
  Rationale: Triggered side effects can recursively query or mutate cache.
  Source locations: cache listener/trigger dispatch, graph trigger service.
  Known related CODEX findings: reentrancy risks reviewed.
  Suggested unit tests: testCacheAddTriggerCanQueryStableIndex(), testReentrantCacheMutationDoesNotCorruptIndex()
  Spec target section: Cache Runtime / Trigger Reentrancy

  10. Cache Filter / Hub Adder Contracts

  CACHE-FILTER-001 — Cache Filters Must Restrict Results Without Changing Cache Identity
  Contract statement: Filtering cache results must affect selection visibility only; it must not alter cached
  identity/index state.
  Rationale: Filters are views over cache, not cache mutation commands.
  Source locations: cache find/select methods, OAFilter, object-cache datasource iterator.
  Known related CODEX findings: object-cache iterator/filter behavior reviewed.
  Suggested unit tests: testCacheFilterExcludesNonMatchingObject(), testCacheFilterDoesNotRemoveObjectFromCache()
  Spec target section: Cache Runtime / Filter Semantics

  CACHE-HUBADDER-001 — Cache Hub Adders Must Add Only Matching Live Objects
  Contract statement: Cache-to-Hub adders/select-all Hub integration must add only live objects that match the Hub/
  class/filter contract.
  Rationale: Select-all Hubs and generated views rely on cache-driven membership.
  Source locations: cache service select-all Hub support, Hub adders, object-cache datasource.
  Known related CODEX findings: cache hub adder behavior reviewed.
  Suggested unit tests: testCacheHubAdderAddsLiveMatchingObject(),
  testCacheHubAdderSkipsCollectedOrWrongClassObject()
  Spec target section: Cache Runtime / Hub Adder Semantics

  11. Failure / Retry / Silent Corruption Contracts

  CACHE-FAILURE-001 — Cache Mutation Failure Must Not Leave Split Index State
  Contract statement: If cache add/remove/update fails visibly, indexes must not be left in a state where GUID
  lookup and key lookup disagree for the same object.
  Rationale: Split indexes corrupt object identity and retry behavior.
  Source locations: OAObjectCache.add/remove, key update paths.
  Known related CODEX findings: partial mutation/index consistency risks reviewed.
  Suggested unit tests: testFailedAddDoesNotLeaveGuidOnlyIndex(), testFailedRemoveDoesNotLeaveKeyOnlyIndex()
  Spec target section: Cache Runtime / Failure Consistency

  CACHE-FAILURE-002 — Cache Must Prefer Visible Failure Over Silent Wrong Identity
  Contract statement: When cache cannot safely resolve identity conflict or stale state, it must fail visibly or
  return no object rather than return a wrong object.
  Rationale: Wrong identity is more damaging than a miss because it corrupts graph semantics.
  Source locations: OAObjectCache lookup/add conflict paths.
  Known related CODEX findings: false-success/silent corruption risks reviewed.
  Suggested unit tests: testIdentityConflictDoesNotReturnWrongObject(),
  testStaleIndexReturnsNullInsteadOfWrongObject()
  Spec target section: Cache Runtime / Silent Corruption Prevention

  CACHE-RETRY-001 — Retry After Cache Miss/Cleanup Must Remain Correct
  Contract statement: After a stale weak-ref cleanup or cache miss, later datasource load/add must be able to
  install a correct fresh cache entry.
  Rationale: GC cleanup and lazy loading must cooperate.
  Source locations: OAObjectCache, datasource/object cache service.
  Known related CODEX findings: stale weak-ref retry behavior reviewed.
  Suggested unit tests: testLoadAfterStaleWeakRefCleanupInstallsFreshEntry(),
  testRetryAfterCacheMissFindsLoadedObject()
  Spec target section: Cache Runtime / Retry Semantics

  12. Test Coverage Matrix

  Identity/GUID:

  - testSameGuidReturnsSameCachedObject
  - testDuplicateAddUsesConfiguredDuplicatePolicy
  - testGetByGuidReturnsLiveObject
  - testAddObjectCreatesGuidIndexEntry
  - testRemoveObjectRemovesGuidIndexEntry

  Business key/index:

  - testGetByObjectIdReturnsMatchingObject
  - testGetByCompositeObjectIdReturnsMatchingObject
  - testKeyChangeRemovesOldIndex
  - testKeyChangeAddsNewIndex
  - testOldKeyDoesNotResolveAfterKeyChange
  - testCompositeKeyRequiresAllParts

  GUID vs key conflict:

  - testNewObjectGuidLookupWorksBeforeKeyAssigned
  - testKeyAssignedObjectResolvesByGuidAndKey
  - testDuplicateGuidDifferentKeyUsesDefinedPolicy
  - testDuplicateKeyDifferentGuidUsesDefinedPolicy

  Weak refs/GC/reference queue:

  - testCacheUsesWeakReferenceSemantics
  - testUnreferencedCachedObjectCanBeCollected
  - testCollectedObjectRemovedFromGuidIndex
  - testCollectedObjectRemovedFromKeyIndex
  - testReferenceQueueCleanupRemovesOnlyCollectedEntry
  - testLookupCleansClearedGuidReference

  Concurrency:

  - testConcurrentAddLookupSameClassStable
  - testConcurrentRemoveLookupDoesNotCorruptIndex
  - testConcurrentDuplicateAddCreatesSingleIdentity
  - testConcurrentDuplicateAddPolicyDeterministic

  Lifecycle:

  - testNewObjectCachedByGuidBeforeKey
  - testNewObjectWithoutKeyDoesNotCreateKeyIndex
  - testDeletedObjectRemovedFromCacheIndexes
  - testDeletedObjectNotReturnedByKeyLookup

  Listeners/triggers:

  - testAddListenerCanLookupAddedObject
  - testRemoveListenerCannotLookupRemovedObjectByKey
  - testListenerExceptionPolicyDefinedForAdd
  - testListenerExceptionDoesNotCorruptCacheIndex
  - testCacheAddTriggerCanQueryStableIndex
  - testReentrantCacheMutationDoesNotCorruptIndex

  Filters/Hub adders:

  - testCacheFilterExcludesNonMatchingObject
  - testCacheFilterDoesNotRemoveObjectFromCache
  - testCacheHubAdderAddsLiveMatchingObject
  - testCacheHubAdderSkipsCollectedOrWrongClassObject

  Failure/retry:

  - testFailedAddDoesNotLeaveGuidOnlyIndex
  - testFailedRemoveDoesNotLeaveKeyOnlyIndex
  - testIdentityConflictDoesNotReturnWrongObject
  - testStaleIndexReturnsNullInsteadOfWrongObject
  - testLoadAfterStaleWeakRefCleanupInstallsFreshEntry
  - testRetryAfterCacheMissFindsLoadedObject


*/


