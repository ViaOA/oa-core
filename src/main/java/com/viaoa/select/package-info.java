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
package com.viaoa.select;

//CODEX unit tests 20260528

/* CODEX Invariants

SELECT-LIFECYCLE-001 — Defined Select Lifecycle
Contract statement:
An OASelect must have a clear lifecycle: configured, started/opened, iterating, completed, cancelled/closed, or
failed. Lifecycle state must determine which operations may start selection, return results, release resources, or
report completion.
Rationale:
OASelect owns datasource iterator state, finder result state, Hub load behavior, and runtime query execution.
Ambiguous lifecycle state causes leaked iterators, false empty results, or stale result reuse.
Source scope:
OASelect constructors, select, _select, hasBeenStarted, isSelectingNow, hasNextCompleted, hasMore, next, _next,
cancel, close, closeQuery.
Related CODEX findings:
OASelect CODEX notes lifecycle synchronization concerns around cancel, closeQuery, _next, iterator state mutation,
and finalize cleanup.
Suggested unit tests:
testSelectOpenCreatesIterator, testSelectCloseReleasesIterator, testClosedSelectHasNextFalse,
testClosedSelectNextReturnsDefinedNoResult.
Spec target section:
Select Runtime / Lifecycle Semantics

SELECT-CONFIG-001 — Configuration Snapshot At Open
Contract statement:
The select class, where clause, parameters, order, filters, finder, where object/hub, dirty flag, passthru flag,
max, and fetch settings used for execution must be coherent for a single opened select lifecycle.
Rationale:
Changing selection inputs during iteration creates inconsistent result sets that do not represent any single
semantic query.
Source scope:
OASelect.setSelectClass, setWhere, setParams, add, setOrder/setOrderBy/setSortBy, setFilter, setDataSourceFilter,
setFinder, setWhereObject, setWhereHub, setDirty, setMax, setFetchAmount, select/_select.
Related CODEX findings:
Existing invariants note configuration stability once opened.
Suggested unit tests:
testSelectUsesConfigurationAtOpenTime, testChangingWhereAfterOpenDoesNotCorruptIteratorByContract,
testChangingParamsAfterOpenDoesNotChangeActiveIterator.
Spec target section:
Select Runtime / Configuration Lifecycle

SELECT-REUSE-001 — Reopen And Retry Use Fresh State
Contract statement:
Reusing an OASelect after close, cancel, exhaustion, or failure must start a fresh selection lifecycle and must not
reuse exhausted, cancelled, partially consumed, or failed iterator/finder state.
Rationale:
Retry and repeated query execution must be deterministic and must not silently skip rows or replay stale results.
Source scope:
OASelect.reset, reset(boolean), select, _select, closeQuery, query, alFinderResults, posFinderResults, amountRead,
amountCount, bHasNextCompleted.
Related CODEX findings:
Existing package invariants note retry/closed select behavior reviewed.
Suggested unit tests:
testReopenAfterCloseCreatesFreshIterator, testReopenDoesNotReuseExhaustedIterator,
testRetryAfterSelectFailureCreatesFreshIterator, testRetryAfterFinderSelectFailureUsesFreshFinderResults.
Spec target section:
Select Runtime / Reuse Semantics

SELECT-DS-001 — Runtime Datasource Routing
Contract statement:
OASelect must resolve datasource execution through OA runtime datasource authority for the selected class and
runtime mode, falling back to alternate/cache/finder paths only under documented select contracts.
Rationale:
OA supports multiple datasources, client/server routing, local object cache, finder-based selection, and single-user
execution. Routing drift returns wrong objects or hides missing datasource registration.
Source scope:
OASelect.getDataSource, select/_select, OADataSourceService/OARuntime datasource integration, OADataSource select/
count methods.
Related CODEX findings:
Existing invariants note datasource routing and client datasource registration concerns.
Suggested unit tests:
testSelectUsesRuntimeDatasourceForClass, testSelectUsesFallbackDatasourceOnlyWhenPrimaryUnavailable,
testSelectNoDatasourceHasDefinedBehavior.
Spec target section:
Select Runtime / Datasource Routing

SELECT-DS-002 — Missing Datasource Semantics
Contract statement:
If no datasource or valid alternate selection path is available for the selected class, OASelect must enter a
documented unavailable/cancelled/no-result state and must not falsely appear to have executed a successful query.
Rationale:
Silent no-op selects hide missing runtime registration and can mark Hubs or callers as if data was truly empty.
Source scope:
OASelect._select, getDataSource, cancel, hasMore, next, missing datasource branch.
Related CODEX findings:
Client datasource registration/no-datasource path CODEX-commented in package notes.
Suggested unit tests:
testMissingDatasourceBehaviorExplicit, testMissingDatasourceDoesNotReturnFalseRows,
testMissingDatasourceDoesNotMarkHubLoadedByContract.
Spec target section:
Select Runtime / Missing Datasource Semantics

SELECT-ITERATOR-001 — Iterator Ownership And Cleanup
Contract statement:
When OASelect opens or receives a datasource iterator, OASelect owns that iterator and must remove/close it on
close, cancel, exhaustion, failure, timeout cleanup, or retry.
Rationale:
Datasource and remote iterators can retain server cursors, sockets, result sets, cached objects, and remote iterator
IDs.
Source scope:
OASelect.query, select/_select, hasMore, next/_next, cancel, close, closeQuery, OASelectManager.performCleanup,
OADataSourceIterator.remove.
Related CODEX findings:
Existing package invariants note iterator lifecycle and cleanup issues reviewed.
Suggested unit tests:
testSelectCloseCallsIteratorRemove, testSelectExhaustionClosesIteratorByContract, testCancelSelectClosesIterator,
testSelectNextExceptionClosesIterator.
Spec target section:
Select Runtime / Iterator Ownership

SELECT-ITERATOR-002 — Forward-Only Iteration Semantics
Contract statement:
hasMore/hasNext must not skip selected objects, and next after hasMore/hasNext must return the expected pending
object or documented no-result value. Iteration must be forward-only unless the select is explicitly reopened.
Rationale:
Hub loading, foreach iteration, pagination, and caller loops depend on normal iterator semantics.
Source scope:
OASelect.hasMore/hasNext, next/_next, iterator(), OADataSourceIterator.hasNext/next integration, finder result iter
ation.
Related CODEX findings:
Datasource iterator boundary bug noted as fixed in package invariants.
Suggested unit tests:
testSelectHasNextDoesNotSkipObject, testSelectNextAfterHasNextReturnsSamePendingObject,
testIteratorForEachUsesSelectSemantics, testFinderResultIterationIsForwardOnly.
Spec target section:
Select Runtime / Iterator Semantics

SELECT-WHERE-001 — Where Clause And Parameter Semantics
Contract statement:
Where text and parameter values must be treated as one coherent expression. Parameter binding must preserve order,
value identity, null semantics, and target comparison intent.
Rationale:
Wrong parameter binding returns the wrong object set and creates memory-vs-datasource drift.
Source scope:
OASelect.setWhere overloads, setParams/getParams, add, select/_select, OADataSource.select/count calls, OASelectFil
ter/OAQueryFilter integration.
Related CODEX findings:
Query/filter edge cases reviewed in package notes.
Suggested unit tests:
testSelectWhereParameterMatchesExpectedObject, testSelectMultipleParametersBindInOrder,
testSelectNullParameterPreservesNullSemantics, testAddWhereClausePreservesPriorParameters.
Spec target section:
Select Runtime / Where Semantics

SELECT-WHERE-002 — Where Object And Hub Scope Semantics
Contract statement:
whereObject, whereHub active object, and whereObjectPropertyPath must constrain selection to objects related through
the metadata-defined reverse path, regardless of datasource, finder, cache, or in-memory execution path.
Rationale:
Where-object selection scopes detail/object relationships. Ignoring this scope can return matching objects from the
whole cache or datasource instead of the intended graph branch.
Source scope:
OASelect.setWhereObject, setWhereHub, setWhereObjectPropertyPath, setWhereHubPropertyPath, _select datasource and f
inder branches, OADataSource.select/count where-object overloads.
Related CODEX findings:
OASelect CODEX notes finder path does not apply whereObject/whereObjectPropertyPath constraints;
whereObjectPropertyPath is an unvalidated String.
Suggested unit tests:
testWhereObjectConstrainsDatasourceSelect, testWhereObjectConstrainsFinderSelect,
testWhereHubActiveObjectConstrainsSelect, testInvalidWhereObjectPropertyPathFailsVisibly.
Spec target section:
Select Runtime / Graph Scope Semantics

SELECT-PATH-001 — Metadata-Validated Select Paths
Contract statement:
Where-object paths, order paths, sort paths, query property paths, and datasource filter paths must resolve
according to OAPath and OA metadata semantics before they are treated as executable selection constraints.
Rationale:
Loose strings can silently resolve wrong properties, fail only in one execution mode, or be ignored by finder/cache
paths.
Source scope:
OASelect.whereObjectPropertyPath, where/order/sort fields, OASelectFilter, OAQueryFilter, OAPath/datasource/filter
integration.
Related CODEX findings:
OASelect CODEX notes whereObjectPropertyPath should be an OAPath/OAPropertyPath semantic object resolved against
metadata.
Suggested unit tests:
testSelectOrderPathResolvesMetadataProperty, testWhereObjectPropertyPathValidatedAgainstMetadata,
testInvalidOrderPathFailsOrUsesDocumentedFallback, testSelectPathSemanticsMatchOAPath.
Spec target section:
Select Runtime / Path and Metadata Semantics

SELECT-ORDER-001 — Deterministic Result Ordering
Contract statement:
When an order/sort expression is supplied, selected result iteration must follow that order for all execution paths
that claim ordering support. Unsupported ordering must fail visibly or use a documented fallback.
Rationale:
Generated UI, reports, Hub loading, projections, and repeatable tests depend on stable result ordering.
Source scope:
OASelect.setOrder/setOrderBy/setSortBy, getOrder/getSortBy, datasource select order argument, finder branch OACompa
rator sorting, object-cache datasource ordering.
Related CODEX findings:
Existing invariants note object-cache query order behavior reviewed.
Suggested unit tests:
testSelectOrderAscendingByProperty, testSelectOrderNestedPropertyByContract,
testFinderSelectAppliesSameOrderAsDatasourceSelect, testUnsupportedOrderFailsOrFallbackByContract.
Spec target section:
Select Runtime / Ordering Semantics

SELECT-FILTER-001 — Filter Restriction Semantics
Contract statement:
OA filters supplied to a select must further restrict results and must never broaden the datasource/cache/finder
selection scope. Datasource filters and post-fetch filters must have explicit ownership and execution boundaries.
Rationale:
Filters are caller constraints. Misapplied filters produce false positives or memory-vs-datasource result drift.
Source scope:
OASelect.setFilter/getFilter, setHubFilter/getHubFilter, setDataSourceFilter/getDataSourceFilter, OASelectFilter, _
select filter construction, next post-filtering.
Related CODEX findings:
Existing invariants note filter/select-all cache behavior reviewed.
Suggested unit tests:
testSelectFilterExcludesNonMatchingObject, testSelectFilterDoesNotBroadenWhereScope,
testDataSourceFilterAndPostFilterBothRestrictResults, testFinderSelectAppliesAllFilters.
Spec target section:
Select Runtime / Filter Semantics

SELECT-MAX-001 — Max Applies To Returned Results
Contract statement:
max must limit the number of objects returned to callers according to the public select contract, after all required
where/finder/filter semantics are applied unless the API explicitly defines datasource pre-limit behavior.
Rationale:
Paging, Hub loads, performance-sensitive selects, and caller expectations depend on max counting returned semantic
matches, not merely raw datasource candidates.
Source scope:
OASelect.setMax/getMax, _select datasource max argument, _next amountRead enforcement, next post-filtering, finder
result iteration.
Related CODEX findings:
OASelect CODEX concern: max is enforced before oaFilter post-filtering for datasource results.
Suggested unit tests:
testSelectMaxLimitsReturnedRows, testSelectMaxCountsPostFilterMatchesByContract,
testSelectMaxZeroMeansUnlimitedByContract.
Spec target section:
Select Runtime / Max Semantics

SELECT-COUNT-001 — Count Scope Consistency
Contract statement:
count must represent the same class, where, parameters, where-object/hub scope, filter capability, dirty mode, and
max semantics as the equivalent select execution path, subject only to documented datasource limitations.
Rationale:
Hub pre-count, UI paging, load decisions, and performance logic rely on count/select consistency.
Source scope:
OASelect.getCount, setCountFirst/getCountFirst, amountCount, _select count branches, OADataSource.count/select.
Related CODEX findings:
Existing invariants note remote missing where-object count/select issue.
Suggested unit tests:
testCountMatchesSelectForSimpleWhere, testCountMatchesSelectForDetailWhereObject,
testCountHonorsWhereHubActiveObject, testCountFirstUsesSameScopeAsSelect.
Spec target section:
Select Runtime / Count Consistency

SELECT-COUNT-002 — Count Unavailable Is Not Zero
Contract statement:
A failed, unavailable, or unsupported count must be distinguishable from a valid count of zero.
Rationale:
Zero is a meaningful result. Treating count failure as zero hides datasource/query errors and affects Hub/load
decisions.
Source scope:
OASelect.getCount, amountCount sentinel behavior, datasource count return values.
Related CODEX findings:
Existing invariants note count unavailable behavior reviewed.
Suggested unit tests:
testUnavailableCountReturnsDefinedSentinel, testValidZeroCountDistinguishedFromUnavailable,
testCountExceptionDoesNotReturnFalseZero.
Spec target section:
Select Runtime / Count Failure Semantics

SELECT-IDENTITY-001 — Selected Object Identity
Contract statement:
Objects returned by OASelect must resolve through OA graph/cache identity semantics so the same datasource key maps
to the authoritative cached OAObject instance where applicable.
Rationale:
Selecting must not create duplicate runtime identities for the same persistent object. Duplicate identities corrupt
Hubs, links, serialization, sync, and cache semantics.
Source scope:
OASelect, datasource iterator integration, object cache service, client datasource iterator, finder/cache selection
paths.
Related CODEX findings:
Identity/cache issues reviewed in graph/cache/datasource scans.
Suggested unit tests:
testSelectReturnsCachedIdentityForExistingKey, testRepeatedSelectReturnsSameIdentityForSameKey,
testFinderSelectPreservesObjectIdentity, testRemoteSelectResolvesCachedIdentity.
Spec target section:
Select Runtime / Object Identity

SELECT-IDENTITY-002 — Deleted Object Visibility
Contract statement:
Completed-deleted objects must not be returned as live authoritative select results unless dirty/deleted-including
mode explicitly defines that behavior.
Rationale:
Returning deleted objects creates ghost rows in Hubs, UI, object graphs, sync, and projections.
Source scope:
OASelect.getDirty/setDirty, datasource select, object-cache iterator, object lifecycle integration.
Related CODEX findings:
Deleted/cache behavior reviewed in package notes.
Suggested unit tests:
testSelectExcludesDeletedObjectByDefault, testSelectDirtyModeBehaviorForDeletedObjectByContract,
testCacheSelectDoesNotReturnCompletedDeletedObject.
Spec target section:
Select Runtime / Deleted Object Semantics

SELECT-HUB-001 — Hub Load Membership Semantics
Contract statement:
When OASelect is used to load a Hub, the Hub must receive exactly the selected objects in selected order, subject to
Hub filtering/sorting and append/rewind contracts.
Rationale:
Hubs are a primary consumer of OASelect. Select/Hubs must agree on membership, ordering, active object behavior, and
load state.
Source scope:
OASelect.setAppend/getAppend, setRewind/getRewind, select iteration, Hub select/load services.
Related CODEX findings:
Hub lazy-load/select correctness reviewed in package notes.
Suggested unit tests:
testHubSelectLoadsSelectedObjectsInOrder, testHubSelectDoesNotAddObjectsOutsideScope,
testAppendSelectPreservesExistingHubMembershipByContract, testRewindSelectSetsActiveObjectByContract.
Spec target section:
Select Runtime / Hub Loading

SELECT-HUB-002 — Failed Select Must Not Publish Loaded Hub State
Contract statement:
A select failure during Hub loading must not mark the Hub as loaded, empty, or complete unless the operation
actually completed under the Hub load contract.
Rationale:
False loaded state prevents retry and hides missing data in live object graphs.
Source scope:
OASelect failure paths, Hub select/load services, close/cancel/failure boundaries.
Related CODEX findings:
Lazy-load loaded/empty bugs reviewed in package notes.
Suggested unit tests:
testFailedHubSelectDoesNotMarkLoaded, testFailedHubSelectCanRetry,
testCancelledHubSelectDoesNotMarkCompleteUnlessContracted.
Spec target section:
Select Runtime / Hub Load State

SELECT-ROLE-001 — Runtime Role Routing
Contract statement:
Client, server, and single-user runtime modes must select through the documented authority: client selects delegate
to remote/server datasource unless a documented local cache optimization applies; single-user selects use local
datasource routing.
Rationale:
Server/client/single-user role drift causes stale results, wrong authority, and incorrect remote iterator ownership.
Source scope:
OASelect datasource routing, OADataSourceClient/RemoteDataSource integration, OARuntime/OASync role checks.
Related CODEX findings:
Client datasource registration path and OASync role semantics reviewed.
Suggested unit tests:
testClientSelectDelegatesToRemoteDatasource, testClientSelectUsesLocalSelectAllCacheWhenDocumented,
testSingleUserSelectUsesLocalDatasource, testSingleUserSelectDoesNotUseRemoteClient.
Spec target section:
Select Runtime / Runtime Role Semantics

SELECT-REMOTE-001 — Remote Iterator Release
Contract statement:
Client/server select must release remote iterator IDs and server-side iterator resources when closed, exhausted,
cancelled, timed out, or failed.
Rationale:
Remote iterators retain server resources and cached object references. Leaked iterators degrade long-running
production systems.
Source scope:
OASelect.close/cancel/closeQuery, OADataSourceClient iterator integration, RemoteDataSource datasourceNext/IT_REMOVE
behavior, OASelectManager timeout cleanup.
Related CODEX findings:
Remote iterator cleanup reviewed in package notes.
Suggested unit tests:
testRemoteSelectCloseReleasesServerIterator, testRemoteSelectExhaustionReleasesServerIterator,
testRemoteSelectFailureReleasesServerIterator, testRemoteSelectTimeoutReleasesServerIterator.
Spec target section:
Select Runtime / Remote Iterator Lifecycle

SELECT-CANCEL-001 — Cancel And Close Boundaries
Contract statement:
Cancel and close must stop future iteration for the current lifecycle, release owned iterator/finder resources,
unregister manager tracking, and make subsequent hasMore/next behavior deterministic.
Rationale:
UI/background selects and Hub loads require cancellable, leak-free lifecycle boundaries.
Source scope:
OASelect.cancel, close, closeQuery, hasMore, next, isCancelled, hasNextCompleted, OASelectManager.remove.
Related CODEX findings:
OASelect CODEX notes cancellation/close synchronization concerns.
Suggested unit tests:
testCancelSelectStopsIteration, testCancelSelectClosesIterator, testClosedSelectHasNextFalse,
testCloseUnregistersSelectManager.
Spec target section:
Select Runtime / Cancellation and Close Semantics

SELECT-MANAGER-001 — Active Select Manager Cleanup
Contract statement:
OASelectManager must track active selects without preventing garbage collection, remove closed/cancelled/cleared
references, and cancel expired started selects without corrupting active selection state.
Rationale:
Long-running selects can leak datasource or remote resources. Manager cleanup is a production lifecycle safety net.
Source scope:
OASelectManager.add, remove, performCleanup, setTimeLimit, weak-reference map, cleanup daemon thread.
Related CODEX findings:
OASelect CODEX notes finalize cleanup is unreliable; manager cleanup participates in lifecycle safety.
Suggested unit tests:
testSelectManagerTracksActiveSelectWeakly, testSelectManagerRemovesClosedSelect,
testSelectManagerCancelsExpiredStartedSelect, testSelectManagerDoesNotCancelNeverStartedSelect.
Spec target section:
Select Runtime / Manager Cleanup Semantics

SELECT-THREAD-001 — Select Instance Concurrency Contract
Contract statement:
A single OASelect instance must either serialize lifecycle/iteration operations or document caller synchronization
requirements. Concurrent select, next, hasMore, cancel, close, and reset must not corrupt iterator state or return
false-success results.
Rationale:
Selects can be used by UI, background loaders, Hub loading, timeout cleanup, and cancellation paths. Races can leak
resources, skip results, or return objects after close.
Source scope:
OASelect synchronized methods, volatile fields, cancel, close, _next, hasMore, select, closeQuery,
OASelectManager.performCleanup.
Related CODEX findings:
OASelect CODEX notes thread-safety claim conflicts with non-uniform synchronization around cancel, closeQuery,
_next, and iterator mutation.
Suggested unit tests:
testConcurrentCancelAndNextDoesNotReturnAfterClose, testConcurrentCloseAndHasMoreReleasesIteratorOnce,
testConcurrentSelectAndCancelLeavesDeterministicState.
Spec target section:
Select Runtime / Concurrency Semantics

SELECT-TL-001 — ThreadLocal/Sibling Helper Cleanup
Contract statement:
Any ThreadLocal or sibling-helper context installed during select filtering or iteration must be removed in finally-
style cleanup before returning to caller.
Rationale:
ThreadLocal leakage can alter unrelated lazy-load, Hub, or graph traversal behavior.
Source scope:
OASelect.next, OASiblingHelper handling, OAThreadLocalService.addSiblingHelper/removeSiblingHelper, filter evaluati
on around datasource iterator results.
Related CODEX findings:
Existing select/filter/lazy-load scans emphasized cleanup and context restoration.
Suggested unit tests:
testSelectFilterRemovesSiblingHelperAfterSuccess, testSelectFilterRemovesSiblingHelperAfterFilterException,
testSiblingHelperDoesNotLeakToNextOperation.
Spec target section:
Select Runtime / ThreadLocal Cleanup

SELECT-FAIL-001 — Failure Visibility
Contract statement:
Datasource failure, query parse failure, invalid path/order failure, filter failure, finder failure, conversion
failure, or iterator failure must be visible or produce a documented no-result state. Failure must not silently
appear as a successful empty select.
Rationale:
Silent empty results hide production data correctness issues and can publish incorrect Hub/cache/runtime state.
Source scope:
OASelect.select/_select, getCount, hasMore, next/_next, OASelectFilter, datasource iterator calls, finder branch.
Related CODEX findings:
Existing package invariants note false-success/silent no-op risks.
Suggested unit tests:
testSelectDatasourceExceptionPropagates, testSelectParseFailureDoesNotReturnFalseEmptyResult,
testFinderFailureDoesNotReturnCompleteEmptyResult, testInvalidOrderPathFailsOrFallbackByContract.
Spec target section:
Select Runtime / Failure Semantics

SELECT-STATE-001 — No Partial Select Commit
Contract statement:
A failed or cancelled select must not publish partially completed state as fully selected, counted, ordered, Hub-
loaded, or exhausted. Partial progress is allowed only when observable through iteration and lifecycle state.
Rationale:
Partial-progress false success corrupts Hubs, loaded flags, count decisions, retry behavior, and user-facing result
sets.
Source scope:
OASelect.amountRead, amountCount, bHasBeenStarted, bHasNextCompleted, bCancelled, query, alFinderResults, Hub load
integration, closeQuery.
Related CODEX findings:
Hub loaded/empty false-success risks and retry behavior reviewed in package notes.
Suggested unit tests:
testFailedSelectDoesNotMarkCompleted, testCancelledSelectDoesNotReportSuccessfulExhaustionUnlessContracted,
testPartialIterationCountReflectsOnlyReturnedObjects, testRetryAfterPartialFailureStartsFresh.
Spec target section:
Select Runtime / Partial Progress Semantics

SELECT-NOOP-001 — Silent No-Op Boundaries
Contract statement:
Returning no results without error is valid only for documented states: valid empty query result, closed/cancelled
select, missing datasource by explicit contract, or configured no-result fallback. Other no-op outcomes must be
visible failures.
Rationale:
Missing datasource, invalid query, invalid path, or failed execution must not masquerade as legitimate empty data.
Source scope:
OASelect._select missing datasource path, cancel/close behavior, hasMore/next, no-result branches.
Related CODEX findings:
Client datasource registration/no-datasource path CODEX-commented in package notes.
Suggested unit tests:
testCancelledSelectNoResultIsDistinguishableFromQueryEmptyWhenContractRequires,
testMissingDatasourceBehaviorExplicit, testInvalidQueryNotSilentNoOp.
Spec target section:
Select Runtime / Silent No-Op Rules

SELECT-AUTHORITY-001 — Select Package Runtime Authority
Contract statement:
com.viaoa.select is the package authority for OA object-selection lifecycle, iterator ownership, select result
visibility, and selection boundary semantics. Query, path, datasource, cache, finder, filter, Hub, and graph
packages provide subordinate semantics that OASelect must preserve.
Rationale:
OA selects bridge executable blueprint metadata and live/datasource object views. Central selection authority
prevents drift between datasource-backed, cache-backed, Hub-backed, and finder-backed selection.
Source scope:
OASelect, OASelectFilter, OASelectManager, integrations with query/path/datasource/cache/filter/find/hub/runtime
packages.
Related CODEX findings:
Where-object finder bypass, unvalidated whereObjectPropertyPath, max/filter ordering, datasource routing, iterator
lifecycle, and concurrency notes all illustrate package boundary contracts.
Suggested unit tests:
testSelectPreservesQueryPathDatasourceSemantics, testSelectPreservesCacheIdentitySemantics,
testSelectPreservesHubLoadSemantics, testFinderAndDatasourceSelectScopesMatch.
Spec target section:
Select Runtime / Cross-Package Authority

SELECT-DETERMINISM-001 — Same Inputs Produce Same Selection
Contract statement:
For the same select configuration, metadata, runtime role, datasource/cache state, graph state, query/path/filter
semantics, ordering, and lifecycle stage, OASelect must produce the same ordered result stream or the same visible
failure.
Rationale:
Selection is an AI-readable and runtime-readable semantic contract over executable blueprints and live object graph/
datasource state. Determinism is required for digital twin runtime behavior, generated code, Hubs, projections,
tests, and production correctness.
Source scope:
All public behavior in OASelect, OASelectFilter, OASelectManager, and select integrations.
Related CODEX findings:
Finder where-object bypass, thread-safety concerns, no-op datasource behavior, and filter/max ordering can threaten
deterministic selection.
Suggested unit tests:
testSameSelectConfigurationReturnsSameOrderedResults, testSameInvalidSelectFailsConsistently,
testFinderAndDatasourceModesReturnSameScopeWhenConfiguredEquivalently.
Spec target section:
Select Runtime / Determinism

*/


