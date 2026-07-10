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

//CODEX unit tests <todo>

/* CODEX Invariants

LOAD-STATE-001 — Load State Distinction
Contract statement:
Load behavior must preserve distinct states for unloaded, loading, loaded-empty, loaded-with-values, locked/loading-
elsewhere, failed, stopped/cancelled, and incomplete wherever OA behavior depends on that distinction.
Rationale:
OA lazy references, Hubs, serialization, sync, replication, filters, templates, and graph traversal need to know
whether missing data means “not loaded yet” or “loaded and empty.”
Source scope:
OALoader.load(...), OALoader._load(...), OALoader.waitUntilDone(), OAPreLoader.load(...), OAPreLoader._load(...),
OALinkInfo isLoaded/isLocked behavior.
Related CODEX findings:
locked/unloaded links are silently skipped and not counted as incomplete.
Suggested unit tests:
loadLockedUnloadedLinkIsReportedIncomplete(), loadLoadedEmptyDistinctFromUnloaded(),
loadFailedReferenceDoesNotAppearLoaded().
Spec target section:
Load Runtime / Load-State Semantics.

LOAD-LIFECYCLE-001 — Deterministic Load Lifecycle
Contract statement:
Load operations must follow a deterministic lifecycle: setup, traversal/materialization, relationship hydration,
completion, failure/stop handling, cleanup, and retry readiness.
Rationale:
Loading participates in object graph hydration, datasource/cache coordination, lazy reference resolution, and
runtime bootstrap; lifecycle ambiguity causes false completion and stale graph state.
Source scope:
OALoader constructor, load(Hub), load(OASelect), load(OAObject), stop(), waitUntilDone(), setup(...), onThreadDone
behavior; OAPreLoader constructor and load().
Related CODEX findings:
abMainThreadRunning starts true; setup failure leaves running state true; worker early return can leak active-thread
count.
Suggested unit tests:
loadNullRootLeavesLoaderWaitSafe(), loadSetupFailureLeavesLoaderRetryable(),
loadStopBeforeWorkerStartDoesNotLeakThreadCount().
Spec target section:
Load Runtime / Lifecycle Semantics.

LOAD-SETUP-001 — Setup Before Traversal
Contract statement:
Loader setup must complete successfully before traversal begins, and failed setup must not publish active lifecycle
flags, path metadata, counters, or executor state as a valid load.
Rationale:
Root class and path metadata determine traversal correctness; setup failure must be visible and retry-safe.
Source scope:
OALoader.setup(Class), path, linkInfos, recursiveLinkInfos, liRecursiveRoot, public load(...) methods.
Related CODEX findings:
setup failure leaves abMainThreadRunning true; parsed path state reused across different root classes.
Suggested unit tests:
loadSetupFailureDoesNotStartExecutor(), loadDifferentRootClassRebuildsPathMetadata(),
loadInvalidPathFailsBeforeTraversal().
Spec target section:
Load Runtime / Setup and Metadata Semantics.

LOAD-LAZY-001 — Lazy Reference Materialization
Contract statement:
Lazy references and Hubs must not be exposed as fully loaded until required value, empty marker, membership, or
failure/incomplete state has been committed.
Rationale:
Callers, filters, serializers, templates, sync, and replication must not treat partially initialized graph state as
authoritative.
Source scope:
OALoader._load(...), OAPreLoader.loadOtoM(...), loadRecursive(...), metadata getValue/isLoaded behavior.
Related CODEX findings:
worker exceptions are swallowed; many-to-many preload no-ops while appearing complete.
Suggested unit tests:
loadLazyFailureLeavesReferenceRetryable(), loadManyToManyUnsupportedDoesNotReportSuccess(),
loadPartialHubHydrationDoesNotMarkLoadedComplete().
Spec target section:
Load Runtime / Lazy-Load Semantics.

LOAD-MATERIALIZE-001 — Object Materialization Semantics
Contract statement:
Object loading must materialize OAObjects according to metadata, datasource, cache, graph identity, and lifecycle
rules before exposing them as usable runtime objects.
Rationale:
Materialized objects become part of live Object Graph behavior and may be traversed, serialized, persisted, synced,
or replicated.
Source scope:
OALoader._load(...), OAPreLoader.load(Class, OALinkInfo), OASelect integration, datasource/cache/object service
boundaries.
Related CODEX findings:
none observed directly in load package; identity/materialization risk is delegated to select/cache/datasource
contracts.
Suggested unit tests:
loadMaterializedObjectHasRuntimeMetadata(), loadMaterializedObjectUsesDatasourceValues(),
loadDoesNotExposeHalfMaterializedObjectAsComplete.
Spec target section:
Load Runtime / Object Materialization Semantics.

LOAD-IDENTITY-001 — Identity Reconciliation During Load
Contract statement:
Loading must preserve OA identity semantics by resolving GUID, primary-key, and runtime object-key identity to the
correct canonical OAObject when available.
Rationale:
Duplicate objects for the same row/key corrupt graph relationships, cache indexes, Hub membership, sync, and
replication.
Source scope:
OAPreLoader.load(Class, OALinkInfo), OALoader._load(...), OASelect integration, cache/datasource/object service
boundaries.
Related CODEX findings:
none observed directly in com.viaoa.load; identity authority belongs to cache/datasource/object contracts.
Suggested unit tests:
loadUsesCachedObjectIdentity(), loadDoesNotCreateDuplicateObjectForSameKey(), hydratedHubUsesCanonicalObjects().
Spec target section:
Load Runtime / Identity and Cache Semantics.

LOAD-CACHE-001 — Cache Coordination
Contract statement:
Cache warming and cache reconciliation during load must not overwrite newer live instances, register duplicate
identities, or leave indexes inconsistent after partial failure.
Rationale:
Load operations often prepare object graphs before normal access; cache state must remain authoritative and graph-
consistent.
Source scope:
OAPreLoader.load(...), OALoader._load(...), OASelect integration, object cache services.
Related CODEX findings:
none observed directly in com.viaoa.load; related to datasource/cache invariants.
Suggested unit tests:
loadPreloadDoesNotReplaceNewerCachedInstance(), loadFailedPreloadDoesNotCorruptCacheIndex(),
loadRetryReusesCanonicalCacheIdentity().
Spec target section:
Load Runtime / Cache Interaction Semantics.

LOAD-LINK-001 — Metadata-Driven Relationship Hydration
Contract statement:
Relationship hydration must follow OA metadata cardinality, reverse-link, owner/detail, recursive-link, private-
link, sort, and cascade/load rules.
Rationale:
Metadata defines object graph truth; loading must not create relationships or loaded states that violate model
semantics.
Source scope:
OAPreLoader._load(...), loadOtoM(...), loadMtoM(...), loadRecursive(...), OALoader._load(...), OALinkInfo.
Related CODEX findings:
non-MANY path segments silently stop; many-to-many preload no-op; recursive-root traversal lacks cycle protection.
Suggested unit tests:
loadOneToManyHydratesDetailHubByMetadata(), loadUnsupportedPathSegmentFailsOrReportsIncomplete(),
loadPrivateLinkDoesNotHydratePublicly().
Spec target section:
Load Runtime / Relationship Hydration Semantics.

LOAD-HUB-001 — Hub Hydration Semantics
Contract statement:
Hub/detail loading must preserve membership, metadata-defined ordering, duplicate rules, detail/master consistency,
and active-object expectations.
Rationale:
Hubs are OA’s collection and relationship surface; load must not silently initialize Hubs with missing, duplicate,
or incorrectly ordered members.
Source scope:
OAPreLoader.loadOtoM(...), loadRecursive(...), load(Class, OALinkInfo), Hub.add(...), OALoader._load(...).
Related CODEX findings:
wrong reselect sort order; class-only preload result cache reused across different link sort contracts.
Suggested unit tests:
loadPreloadedDetailHubUsesLinkSortOrder(), loadRecursiveHubUsesRecursiveSortOrder(),
loadSameTargetClassDifferentLinksUseCorrectOrder(), loadRepeatedPreloadDoesNotDuplicateHubMembership.
Spec target section:
Load Runtime / Hub Hydration Semantics.

LOAD-TRAVERSE-001 — Recursive Traversal and Cycle Protection
Contract statement:
Recursive loading must prevent infinite traversal while still visiting legitimate reachable objects according to
metadata and traversal scope.
Rationale:
OA object graphs can contain cycles and recursive relationships; load must be bounded without suppressing required
objects.
Source scope:
OALoader._load(...), OAPreLoader.loadRecursive(...), recursiveLinkInfos, liRecursiveRoot, cascade/traversal tracking
boundaries.
Related CODEX findings:
recursive-root traversal lacks cycle protection.
Suggested unit tests:
loadRecursiveCycleDoesNotLoopForever(), loadRecursiveTraversalVisitsDistinctReachableObjects(),
loadRecursiveRootUsesCycleGuard().
Spec target section:
Load Runtime / Recursive Traversal Semantics.

LOAD-FAIL-001 — Load Failure Visibility
Contract statement:
Load failure must be caller-visible or explicitly recorded as incomplete; load APIs must not silently report success
after skipped, failed, unsupported, stopped, or partial load work.
Rationale:
Silent load success can leave missing references, empty Hubs, stale cache assumptions, and incorrect serialization/
sync/replication state.
Source scope:
OALoader.load(...), waitUntilDone(), worker catch blocks, OAPreLoader.load(...), OAPreLoader._load(...),
loadMtoM(...).
Related CODEX findings:
async worker exceptions logged/swallowed; OAPreLoader stops at first non-MANY; loadMtoM no-op.
Suggested unit tests:
loadWorkerExceptionIsVisibleToCaller(), loadUnsupportedPathSegmentReportsIncomplete(),
loadUnsupportedManyToManyPreloadDoesNotSilentlySucceed().
Spec target section:
Load Runtime / Failure and False-Success Prevention.

LOAD-PARTIAL-001 — Partial Progress Visibility
Contract statement:
If a load operation fails after some objects, links, Hubs, or references are materialized, the partial state must be
observable as incomplete or rolled back by the owning runtime contract.
Rationale:
Partial graph hydration can mislead serializers, sync, replication, templates, filters, and callers into using
incomplete graph state.
Source scope:
OALoader._load(...), OAPreLoader.loadOtoM(...), loadRecursive(...), load(Class,...), OASelect integration.
Related CODEX findings:
worker failure hidden from callers; false-success preload paths.
Suggested unit tests:
loadPartialHubHydrationReportsIncomplete(), loadWorkerFailureMarksLoaderIncomplete(),
loadSerializationDoesNotSeePartialLoadAsComplete().
Spec target section:
Load Runtime / Partial Progress Semantics.

LOAD-RETRY-001 — Retry After Failed or Stopped Load
Contract statement:
After failed, stopped, cancelled, unsupported, or no-op load, loader state must remain retryable and wait-safe
without stale lifecycle flags, path metadata, counters, thread state, or context.
Rationale:
Production load failures may be transient; retries must not skip work or inherit corrupted state.
Source scope:
OALoader.load(...), stop(), setup(...), waitUntilDone(), onThreadDone(...), worker lifecycle fields.
Related CODEX findings:
abMainThreadRunning starts true; setup failure leaves running state true; worker early return can leak active-thread
count.
Suggested unit tests:
loadWaitUntilDoneAfterNullLoadReturns(), loadRetryAfterFailedSetupReinitializesState(),
loadStopBeforeWorkerStartDoesNotLeakCounters().
Spec target section:
Load Runtime / Retry Semantics.

LOAD-DS-001 — Datasource Resource Lifecycle
Contract statement:
Datasource selects, iterators, streams, and result resources opened or consumed during load must be closed or
released on success, failure, cancellation, stop, and exception paths unless ownership is explicitly transferred.
Rationale:
Background loading can touch large datasets; leaked datasource resources can exhaust connections/cursors and
destabilize long-running systems.
Source scope:
OALoader.load(OASelect), OAPreLoader.load(Class, OALinkInfo), OASelect.close() boundaries.
Related CODEX findings:
OALoader.load(OASelect) does not close select on early exit; OAPreLoader.load(Class, ...) lacks finally close around
select lifecycle.
Suggested unit tests:
loadClosesSelectOnStop(), loadClosesSelectOnException(), preLoaderClosesSelectAfterSuccess(),
preLoaderClosesSelectAfterFailure().
Spec target section:
Load Runtime / Datasource Resource Semantics.

LOAD-TL-001 — ThreadLocal and Runtime Context Restoration
Contract statement:
Any ThreadLocal, sibling-helper, loading flag, graph/runtime context, or loader context installed during load must
be restored with try/finally in every thread that installs it.
Rationale:
Load operations run in foreground and background threads; leaked context can affect later traversal, lazy loading,
sync, triggers, datasource work, or UI logic.
Source scope:
OALoader.load(...), worker Runnable.run(), OAPreLoader.load(), OAThreadLocalService.setLoading(...),
addSiblingHelper/removeSiblingHelper.
Related CODEX findings:
worker early bStop return skips sibling-helper/finally cleanup and thread count cleanup.
Suggested unit tests:
loadMainThreadSiblingHelperRemovedAfterFailure(), loadWorkerSiblingHelperRemovedAfterException(),
preLoaderRestoresLoadingFlag(), loadStopBeforeWorkerStartDoesNotLeakContext().
Spec target section:
Load Runtime / ThreadLocal Context Semantics.

LOAD-CONCURRENT-001 — Concurrent Loading Correctness
Contract statement:
Concurrent load work must not expose false completion, corrupt shared Hub/cache state, leak worker counters, clear
shared context while workers need it, or hide worker failures.
Rationale:
OALoader is explicitly multi-threaded; completion, counters, root context, worker lifecycle, and failure state must
remain balanced and visible.
Source scope:
OALoader aiThreadsUsed, executorService, hubFrom, abMainThreadRunning, onThreadDone(...), worker tasks.
Related CODEX findings:
hubFrom cleared before workers finish; worker active count leak; swallowed worker exceptions.
Suggested unit tests:
loadRootHubContextAvailableUntilWorkersFinish(), loadConcurrentWorkersAllDecrementActiveCount(),
loadWaitUntilDoneWaitsForWorkers(), loadWorkerFailureMarksLoaderIncomplete().
Spec target section:
Load Runtime / Concurrent Loading Semantics.

LOAD-ORDER-001 — Load Ordering and Sort Semantics
Contract statement:
Loaded Hubs and relationship results must use the metadata-defined sort/order contract for the specific link being
hydrated.
Rationale:
Order is observable through Hubs, UI, serialization, sync, replication, and application logic; class-level reuse
must not override link-specific ordering.
Source scope:
OAPreLoader.load(Class, OALinkInfo), loadOtoM(...), loadRecursive(...), result cache by class/link/sort.
Related CODEX findings:
wrong reselect sort order; class-only preload result cache reused across different link sort contracts.
Suggested unit tests:
loadDetailHubUsesActiveLinkSortOrder(), loadClassReuseDoesNotOverrideDifferentLinkSort(),
loadRecursiveRelationshipUsesRecursiveSortOrder().
Spec target section:
Load Runtime / Ordering Semantics.

LOAD-UNSUPPORTED-001 — Unsupported Load Path Semantics
Contract statement:
Unsupported load shapes, including unsupported cardinality or relationship types, must fail visibly or be reported
as incomplete rather than no-oping as success.
Rationale:
Callers rely on preload/load to make requested graph state available; unsupported paths must not leave graph state
missing silently.
Source scope:
OAPreLoader._load(...), loadMtoM(...), loadOtoM(...), OALoader.setup(...), path/linkInfos.
Related CODEX findings:
OAPreLoader stops at first non-MANY segment; loadMtoM no-op.
Suggested unit tests:
loadUnsupportedManyToManyPreloadReportsIncomplete(), loadOneSegmentPathHandledOrRejectedByContract(),
loadNonManyPathSegmentDoesNotSilentlyStop().
Spec target section:
Load Runtime / Unsupported Path Semantics.

LOAD-SERIALIZE-001 — Load-State Consumer Contract
Contract statement:
Serialization, sync, replication, select, query, path, filter, template, and graph traversal code may rely on load-
state correctness: loaded references and Hubs must be complete enough for their advertised state.
Rationale:
OA graph consumers cannot independently revalidate every lazy-load state; the load package must preserve advertised
loaded/incomplete boundaries.
Source scope:
OALoader, OAPreLoader, graph object/hub services, select/datasource/cache integration.
Related CODEX findings:
false-success preload paths; worker failure hidden from callers.
Suggested unit tests:
loadSerializedLoadedHubContainsPreloadedMembers(), loadReplicationDoesNotSeePartiallyLoadedAsComplete(),
loadSelectAfterPreloadUsesCorrectLoadedState().
Spec target section:
Load Runtime / Load-State Consumer Semantics.

LOAD-BOUNDARY-001 — Datasource Load Success Versus Object Graph Load Success
Contract statement:
Datasource select/read success, cache update success, and semantic Object Graph load success are distinct; load APIs
must not conflate them unless all required graph hydration and visibility conditions are satisfied.
Rationale:
A row can be read while relationship hydration, cache reconciliation, Hub membership, or loaded-state publication
still fails.
Source scope:
OALoader.load(OASelect), OAPreLoader.load(Class,...), OASelect integration, cache/object/hub/graph boundaries.
Related CODEX findings:
select resource and partial hydration findings illustrate distinction.
Suggested unit tests:
loadDatasourceReadSuccessDoesNotImplyHubHydrationSuccess(), loadCacheUpdateSuccessDoesNotImplyRelationshipLoaded(),
loadGraphLoadSuccessRequiresCommittedLoadedState().
Spec target section:
Load Runtime / Datasource-Cache-Graph Boundary Semantics.

LOAD-INTEGRATION-001 — Cross-Package Loading Compatibility
Contract statement:
Load behavior must remain compatible with datasource, object, hub, cache, graph, select, query, path, serialization,
sync, replication, transaction, metadata, and runtime contracts.
Rationale:
Loading is a runtime boundary that prepares executable Object Graph state for use by nearly every OA subsystem.
Source scope:
OALoader; OAPreLoader; OALinkInfo/metadata integration; OASelect/datasource/cache boundaries; object/hub/graph
consumers.
Related CODEX findings:
many-to-many no-op, false-success paths, select cleanup, ThreadLocal cleanup, and ordering issues affect cross-
package consumers.
Suggested unit tests:
loadDatasourceCacheAndHubStayConsistent(), loadPathQuerySerializationObserveSameLoadedState(),
loadSyncReplicationDoNotReceiveFalseLoadedState().
Spec target section:
Load Runtime / Cross-Package Integration Semantics.

*/


