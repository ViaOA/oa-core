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
package com.viaoa.load;

/* CODEX Invariants

Load Invariants

  ID: LOAD-STATE-001
  Contract statement: Load behavior must preserve the distinction between unloaded, loading, loaded-empty, loaded-
  with-values, locked/loading-elsewhere, failed, and incomplete states wherever OA behavior depends on that
  distinction.
  Rationale: OA lazy references, Hubs, serialization, sync, and replication need to know whether missing data means
  “not loaded yet” or “loaded and empty.”
  Source locations: OALoader._load(...), OAPreLoader.load(...), OALinkInfo.isLoaded(...), OALinkInfo.isLocked(...).
  Related CODEX findings: locked/unloaded links are silently skipped and not counted as incomplete.
  Suggested unit tests: testLockedUnloadedLinkIsReportedIncomplete, testLoadedEmptyDistinctFromUnloaded,
  testFailedLoadDoesNotAppearLoaded.
  Spec target section: Load Runtime / Load-State Semantics.

  ID: LOAD-LAZY-001
  Contract statement: Lazy-load traversal must not expose a reference or Hub as fully loaded until the required value,
  empty marker, or membership state has been committed.
  Rationale: Callers, filters, serializers, and templates must not see partially initialized graph state as
  authoritative.
  Source locations: OALoader._load(...), OAPreLoader.loadOtoM(...), OAPreLoader.loadRecursive(...), metadata link
  getValue/isLoaded.
  Related CODEX findings: worker exceptions are swallowed; many-to-many preload no-ops while appearing complete.
  Suggested unit tests: testLazyLoadFailureLeavesReferenceRetryable,
  testManyToManyPreloadDoesNotReportSuccessWhenUnsupported, testPartialHubHydrationDoesNotMarkLoaded.
  Spec target section: Load Runtime / Lazy-Load Semantics.

  ID: LOAD-FAIL-001
  Contract statement: Load failure must be caller-visible or explicitly recorded as incomplete; load APIs must not
  silently report success after skipped, failed, or unsupported load work.
  Rationale: Silent preload/load success can lead to missing references, empty Hubs, stale cache assumptions, and bad
  replication/serialization state.
  Source locations: OALoader.load(...), OALoader.waitUntilDone(), worker catch blocks, OAPreLoader._load(...),
  OAPreLoader.loadMtoM(...).
  Related CODEX findings: async worker exceptions logged/swallowed; OAPreLoader stops at first non-MANY; loadMtoM no-
  op.
  Suggested unit tests: testWorkerExceptionIsVisibleToCaller, testUnsupportedPathSegmentFailsOrReportsIncomplete,
  testUnsupportedManyToManyPreloadDoesNotSilentlySucceed.
  Spec target section: Load Runtime / Failure Semantics.

  ID: LOAD-RETRY-001
  Contract statement: After failed, stopped, cancelled, or no-op load, loader state must remain retryable and wait-
  safe.
  Rationale: OA production loads may be cancelled, interrupted, or fail from datasource errors; retry must not inherit
  stale lifecycle flags or counters.
  Source locations: OALoader.load(...), OALoader.setup(...), OALoader.onThreadDone(...), OALoader.waitUntilDone().
  Related CODEX findings: abMainThreadRunning starts true; setup failure leaves running state true; worker early
  return can leak active-thread count.
  Suggested unit tests: testWaitUntilDoneAfterNullLoadReturns, testWaitUntilDoneAfterSetupFailureReturns,
  testRetryAfterFailedSetupReinitializesState, testStopBeforeWorkerStartDoesNotLeakThreadCount.
  Spec target section: Load Runtime / Retry Semantics.

  ID: LOAD-IDENTITY-001
  Contract statement: Loading must preserve OA identity semantics: GUID identity, primary-key identity, and business-
  key identity must resolve to the correct existing OAObject when available.
  Rationale: Duplicate objects for the same row/key corrupt graph relationships, cache indexes, Hub membership, sync,
  and replication.
  Source locations: OAPreLoader.load(Class, OALinkInfo), OASelect, datasource/object cache integration, graph object
  services.
  Related CODEX findings: none observed directly in com.viaoa.load; identity risk is delegated to select/cache/
  datasource contracts.
  Suggested unit tests: testPreloadUsesCachedObjectIdentity, testLoadDoesNotCreateDuplicateObjectForSameKey,
  testHydratedHubUsesCanonicalObjects.
  Spec target section: Load Runtime / Identity Semantics.

  ID: LOAD-CACHE-001
  Contract statement: Cache warming during load must not overwrite newer live instances, register duplicate
  identities, or leave cache indexes inconsistent after partial failure.
  Rationale: Load is often used to warm object graphs before normal access; cache state must remain authoritative.
  Source locations: OAPreLoader.load(...), OALoader._load(...), OASelect, object cache services.
  Related CODEX findings: none observed directly in com.viaoa.load; related to datasource/cache invariants.
  Suggested unit tests: testPreloadDoesNotReplaceNewerCachedInstance, testFailedPreloadDoesNotCorruptCacheIndex,
  testRetryPreloadReusesCacheIdentity.
  Spec target section: Load Runtime / Cache Interaction.

  ID: LOAD-LINK-001
  Contract statement: Relationship hydration must follow OA metadata cardinality, reverse-link, ownership, recursive-
  link, and private-link rules.
  Rationale: Metadata defines object graph truth; loading must not create relationships that violate model semantics.
  Source locations: OAPreLoader._load(...), loadOtoM(...), loadMtoM(...), loadRecursive(...), OALinkInfo.
  Related CODEX findings: non-MANY path segments silently stop; many-to-many no-op; recursive-root traversal lacks
  cycle protection.
  Suggested unit tests: testOneToManyPreloadHydratesReverseHub, testPathWithOneSegmentIsHandledOrRejected,
  testRecursiveLoadCycleDoesNotLoopForever, testPrivateLinkIsNotHydratedPublicly.
  Spec target section: Load Runtime / Link Hydration Semantics.

  ID: LOAD-HUB-001
  Contract statement: Hub/detail loading must preserve membership, metadata-defined ordering, duplicate rules, active-
  object expectations, and detail/master consistency.
  Rationale: Hubs are OA’s collection and relationship surface; preload must not silently initialize them in the wrong
  order or with missing membership.
  Source locations: OAPreLoader.loadOtoM(...), loadRecursive(...), load(Class, OALinkInfo), Hub.add(...).
  Related CODEX findings: wrong reselect sort order; class-only preload result cache reused across different link sort
  contracts.
  Suggested unit tests: testPreloadedDetailHubUsesLinkSortOrder, testRecursiveHubUsesRecursiveSortOrder,
  testSameTargetClassDifferentLinksUseCorrectOrder, testRepeatedPreloadDoesNotDuplicateHubMembership.
  Spec target section: Load Runtime / Hub Hydration Semantics.

  ID: LOAD-DS-001
  Contract statement: Datasource selects, iterators, streams, and result resources opened during load must be closed
  on success, cancellation, stop, and exception paths.
  Rationale: Background loading can touch large datasets; leaked datasource resources can exhaust connections/cursors
  and destabilize production systems.
  Source locations: OALoader.load(OASelect), OAPreLoader.load(Class, OALinkInfo), OASelect.close().
  Related CODEX findings: OALoader.load(OASelect) does not close select on early exit; OAPreLoader.load(Class, ...)
  lacks finally close around select lifecycle.
  Suggested unit tests: testLoaderClosesSelectOnStop, testLoaderClosesSelectOnException,
  testPreLoaderClosesSelectAfterSuccess, testPreLoaderClosesSelectAfterFailure.
  Spec target section: Load Runtime / Datasource Resource Semantics.

  ID: LOAD-TL-001
  Contract statement: Any ThreadLocal, sibling-helper, loading flag, or context state installed during load must be
  restored with try/finally in every thread that installs it.
  Rationale: Load operations run in foreground and background threads; context leakage can affect later graph
  traversal, lazy-load behavior, sync, or UI logic.
  Source locations: OALoader.load(...), worker Runnable.run(), OAPreLoader.load(),
  OAThreadLocalService.setLoading(...), addSiblingHelper/removeSiblingHelper.
  Related CODEX findings: worker early bStop return skips sibling-helper/finally cleanup and thread count cleanup.
  Suggested unit tests: testMainThreadSiblingHelperRemovedAfterLoadFailure,
  testWorkerSiblingHelperRemovedAfterException, testPreLoaderRestoresLoadingFlag,
  testStopBeforeWorkerStartDoesNotLeakContext.
  Spec target section: Load Runtime / ThreadLocal Context Semantics.

  ID: LOAD-CONCURRENT-001
  Contract statement: Concurrent load work must not expose false completion, corrupt shared Hub/cache state, or clear
  shared loader context while workers still need it.
  Rationale: OALoader is explicitly multi-threaded; completion, counters, root hub context, and worker lifecycle must
  be visible and balanced.
  Source locations: OALoader.aiThreadsUsed, executorService, hubFrom, abMainThreadRunning, onThreadDone(...), worker
  tasks.
  Related CODEX findings: hubFrom cleared before workers finish; worker active count leak; swallowed worker
  exceptions.
  Suggested unit tests: testRootHubContextAvailableUntilWorkersFinish, testConcurrentWorkersAllDecrementActiveCount,
  testWaitUntilDoneWaitsForWorkers, testWorkerFailureMarksLoaderIncomplete.
  Spec target section: Load Runtime / Concurrent Loading Semantics.

  ID: LOAD-SETUP-001
  Contract statement: Loader setup must complete before traversal begins, and failed setup must not leave lifecycle
  flags, path metadata, counters, or executor state indicating a valid active load.
  Rationale: Path metadata and root class determine traversal correctness; setup failure must be safely retryable.
  Source locations: OALoader.setup(Class), public load(...) methods, propertyPath, linkInfos, recursiveLinkInfos,
  liRecursiveRoot.
  Related CODEX findings: setup failure leaves abMainThreadRunning true; parsed path state reused across different
  root classes.
  Suggested unit tests: testSetupFailureLeavesLoaderWaitSafe, testSetupDifferentRootClassRebuildsMetadata,
  testInvalidPathDoesNotStartExecutor, testCountersResetOnlyForActualLoadAttempt.
  Spec target section: Load Runtime / Setup-Before-Traversal Semantics.

  ID: LOAD-SERIALIZE-001
  Contract statement: Serialization, sync, replication, select, and query code may rely on load-state correctness:
  loaded references/Hubs must be complete enough for their advertised state.
  Rationale: OA graph consumers cannot independently revalidate every lazy-load state; the load package must preserve
  the contract.
  Source locations: OALoader, OAPreLoader, graph object/hub services, select/datasource/cache integration.
  Related CODEX findings: false-success preload paths; worker failure hidden from callers.
  Suggested unit tests: testSerializedLoadedHubContainsPreloadedMembers,
  testReplicationDoesNotSeePartiallyLoadedAsComplete, testSelectAfterPreloadUsesCorrectLoadedState.
  Spec target section: Cross-Package Contracts / Load-State Consumers.

  Suggested Package-Level Spec Summary

  com.viaoa.load is responsible for explicit preload and traversal loading of OA object graphs, including lazy
  references, Hubs, recursive relationships, and datasource-backed object sets.

  It must guarantee that a caller can distinguish successful complete load, incomplete load, stopped/cancelled load,
  failed load, and unsupported load paths.

  It must never silently report success when a requested relationship path, many-to-many link, locked reference,
  worker task, or datasource select was skipped or failed.

  It must preserve OA identity: loaded objects must resolve through the normal datasource/cache/graph identity
  contracts and must not create duplicate graph instances.

  It must hydrate relationships according to metadata: cardinality, reverse links, recursive links, private links,
  sort order, and Hub membership rules.

  It must keep Hub state consistent: membership, ordering, detail/master relationships, duplicate behavior, and
  active-object assumptions must remain valid.

  It must clean up datasource resources and select iterators on success, failure, cancellation, and stop paths.

  It must restore ThreadLocal loading flags and sibling-helper context in both main and worker threads.

  It must be retry-safe: failed or no-op loads must not leave lifecycle flags, worker counters, executor state, path
  metadata, or root hub context corrupt.

  Cross-package assumptions: metadata defines link truth, path defines traversal shape, select/datasource own result
  identity/resource lifecycle, cache owns canonical object identity, hub owns membership semantics, and graph/runtime
  own context/thread-local behavior.

  Likely unit-test categories: load-state transitions, failed-load retry, worker lifecycle, select cleanup, many-to-
  many/one-to-many preload, recursive cycle protection, Hub sort/order hydration, locked-link behavior, ThreadLocal
  cleanup, and serialization/sync consumers of loaded state.

*/



