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


/* CODEX Invariants

1. Select Lifecycle Contracts

  SELECT-LIFECYCLE-001 — OASelect Has A Defined Open/Iterate/Close Lifecycle
  Contract statement: An OASelect must transition through a clear lifecycle: configured, opened, iterated, and
  closed.
  Rationale: Select owns datasource iterator state and must clean it up deterministically.
  Source locations: OASelect, datasource iterator integration.
  Known related CODEX findings: iterator lifecycle/cleanup issues were reviewed.
  Suggested unit tests: testSelectOpenCreatesIterator(), testSelectCloseReleasesIterator()
  Spec target section: Select Runtime / Lifecycle Semantics

  SELECT-LIFECYCLE-002 — Select Configuration Is Stable Once Opened
  Contract statement: Once a select is opened, where/order/filter/max configuration used for the datasource query
  must remain stable for that open lifecycle.
  Rationale: Changing query inputs mid-iteration creates inconsistent result sets.
  Source locations: OASelect query configuration/open methods.
  Known related CODEX findings: none observed.
  Suggested unit tests: testSelectUsesConfigurationAtOpenTime(),
  testChangingWhereAfterOpenDoesNotCorruptIteratorByContract()
  Spec target section: Select Runtime / Query Lifecycle

  SELECT-LIFECYCLE-003 — Reopen/Reuse Must Start A Fresh Iterator Lifecycle
  Contract statement: Reusing an OASelect after close/open must create a new datasource iterator and not reuse
  exhausted/closed state.
  Rationale: Select retry and repeated query execution must be deterministic.
  Source locations: OASelect.open, OASelect.close, iterator fields.
  Known related CODEX findings: retry/closed select behavior reviewed.
  Suggested unit tests: testReopenAfterCloseCreatesFreshIterator(), testReopenDoesNotReuseExhaustedIterator()
  Spec target section: Select Runtime / Reuse Semantics

  2. Datasource Routing Contracts

  SELECT-DS-001 — Select Uses Runtime Datasource Routing For Class
  Contract statement: OASelect must select its datasource through the runtime datasource service for the selected
  class and filter context.
  Rationale: OA supports multiple datasources, object cache, remote client datasource, and fallback/autonumber
  routing.
  Source locations: OASelect, OARuntime.datasource(), OADataSourceService.
  Known related CODEX findings: datasource routing and client registration issues were CODEX-commented.
  Suggested unit tests: testSelectUsesRuntimeDatasourceForClass(),
  testSelectUsesFallbackDatasourceOnlyWhenPrimaryUnavailable()
  Spec target section: Select Runtime / Datasource Routing

  SELECT-DS-002 — No Datasource Must Be A Defined No-Result/Unavailable State
  Contract statement: If no datasource is available for a select class, OASelect must enter a defined closed/
  cancelled/no-result state and must not falsely appear to have queried successfully.
  Rationale: Silent no-op selects can hide missing datasource registration.
  Source locations: OASelect, datasource lookup paths.
  Known related CODEX findings: client datasource registration path was CODEX-commented.
  Suggested unit tests: testSelectNoDatasourceHasDefinedBehavior(), testMissingDatasourceDoesNotReturnFalseRows()
  Spec target section: Select Runtime / Missing Datasource Semantics

  3. Iterator / Resource Cleanup Contracts

  SELECT-ITERATOR-001 — OASelect Owns The Datasource Iterator It Opens
  Contract statement: When OASelect opens a datasource iterator, it is responsible for closing/removing it when
  select closes, exhausts, or fails.
  Rationale: Remote and streaming datasource iterators can hold server resources.
  Source locations: OASelect, OADataSourceIterator.remove().
  Known related CODEX findings: iterator cleanup issues fixed in datasource/base select paths.
  Suggested unit tests: testSelectCloseCallsIteratorRemove(), testSelectExhaustionClosesIteratorByContract()
  Spec target section: Select Runtime / Iterator Ownership

  SELECT-ITERATOR-002 — Select Failure Must Close Open Iterator
  Contract statement: If iteration fails after opening a datasource iterator, OASelect must close/remove the
  iterator before propagating failure.
  Rationale: Prevents leaked remote/server cursors after exceptions.
  Source locations: OASelect.next/hasNext/close, iterator handling.
  Known related CODEX findings: resource cleanup was a scan focus.
  Suggested unit tests: testSelectNextExceptionClosesIterator(), testSelectHasNextExceptionClosesIterator()
  Spec target section: Select Runtime / Failure Cleanup

  SELECT-ITERATOR-003 — HasNext/Next Must Be Forward-Only And Stable
  Contract statement: hasNext() must not skip rows, and next() after hasNext() must return the expected pending
  object.
  Rationale: Hub loading and caller iteration depend on normal iterator semantics.
  Source locations: OASelect, datasource iterator wrappers.
  Known related CODEX findings: datasource iterator boundary bug fixed.
  Suggested unit tests: testSelectHasNextDoesNotSkipObject(), testSelectNextAfterHasNextReturnsSamePendingObject()
  Spec target section: Select Runtime / Iterator Semantics

  4. Where / Order / Filter Contracts

  SELECT-WHERE-001 — Where Clause And Parameters Must Be Applied Together
  Contract statement: Query where text and parameter array must be passed to datasource/filter creation as one
  consistent expression.
  Rationale: Wrong parameter binding returns wrong objects.
  Source locations: OASelect, OADataSource.select/count, filter/query integration.
  Known related CODEX findings: query/filter edge cases reviewed.
  Suggested unit tests: testSelectWhereParameterMatchesExpectedObject(), testSelectMultipleParametersBindInOrder()
  Spec target section: Select Runtime / Where Semantics

  SELECT-ORDER-001 — Order Clause Must Determine Result Order When Supported
  Contract statement: If an order expression is supplied, result iteration must follow that order for datasources
  that support/order in memory.
  Rationale: Generated UI, reports, and Hub loading require deterministic ordering.
  Source locations: OASelect, datasource select, object-cache datasource sort behavior.
  Known related CODEX findings: object-cache query order behavior reviewed.
  Suggested unit tests: testSelectOrderAscendingByProperty(), testSelectOrderNestedPropertyByContract()
  Spec target section: Select Runtime / Order Semantics

  SELECT-FILTER-001 — Filter Must Further Restrict Results Without Changing Datasource Scope
  Contract statement: An OAFilter supplied to select must restrict returned objects and must not broaden the
  datasource query scope.
  Rationale: Filters are caller constraints, not datasource ownership changes.
  Source locations: OASelect, OADataSource.select, object-cache datasource, client datasource local select-all
  optimization.
  Known related CODEX findings: filter/select-all cache behavior reviewed.
  Suggested unit tests: testSelectFilterExcludesNonMatchingObject(), testSelectFilterDoesNotBroadenWhereScope()
  Spec target section: Select Runtime / Filter Semantics

  SELECT-MAX-001 — Max Limits Returned Object Count
  Contract statement: max must limit the number of objects returned by select iteration according to contract.
  Rationale: Paging, pre-count, and performance-sensitive loads rely on max.
  Source locations: OASelect, OADataSource.select, ObjectCacheIterator.setMax.
  Known related CODEX findings: max/fetch behavior reviewed.
  Suggested unit tests: testSelectMaxLimitsReturnedRows(), testSelectMaxZeroMeansUnlimitedByContract()
  Spec target section: Select Runtime / Max Semantics

  5. Count / Select Consistency Contracts

  SELECT-COUNT-001 — Count Matches Equivalent Select Scope
  Contract statement: count must count the same class/where/filter/where-object scope that equivalent select would
  return, subject to max semantics.
  Rationale: Hub pre-count, UI paging, and load decisions rely on consistency.
  Source locations: OASelect, OADataSource.count, OADataSource.select.
  Known related CODEX findings: remote missing where-object count/select issue CODEX-commented.
  Suggested unit tests: testCountMatchesSelectForSimpleWhere(), testCountMatchesSelectForDetailWhereObject()
  Spec target section: Select Runtime / Count Consistency

  SELECT-COUNT-002 — Count Failure Must Not Masquerade As Zero Unless Contract Says So
  Contract statement: If count cannot be performed, failure/unavailable must be distinguishable from a valid count
  of zero.
  Rationale: Zero has semantic meaning; unavailable count should not hide datasource failure.
  Source locations: OASelect, datasource count methods returning -1.
  Known related CODEX findings: count unavailable behavior reviewed.
  Suggested unit tests: testUnavailableCountReturnsDefinedSentinel(),
  testValidZeroCountDistinguishedFromUnavailable()
  Spec target section: Select Runtime / Count Failure Semantics

  6. Object Identity Contracts

  SELECT-IDENTITY-001 — Selected Objects Must Be Graph Identity Objects
  Contract statement: Objects returned by OASelect must resolve through OA graph/cache identity semantics.
  Rationale: Selecting must not create duplicate runtime identities for the same persistent object.
  Source locations: OASelect, datasource select, object cache service, client datasource iterator.
  Known related CODEX findings: identity/cache issues reviewed in graph/cache/datasource scans.
  Suggested unit tests: testSelectReturnsCachedIdentityForExistingKey(),
  testRepeatedSelectReturnsSameIdentityForSameKey()
  Spec target section: Select Runtime / Object Identity

  SELECT-IDENTITY-002 — Select Must Not Return Deleted Objects As Authoritative Results
  Contract statement: Completed-deleted objects must not be returned as live authoritative select results unless a
  dirty/deleted-including mode explicitly says so.
  Rationale: Prevents ghost objects in Hubs/UI.
  Source locations: OASelect, datasource select, object-cache iterator, object lifecycle services.
  Known related CODEX findings: deleted/cache behavior reviewed.
  Suggested unit tests: testSelectExcludesDeletedObjectByDefault(),
  testSelectDirtyModeBehaviorForDeletedObjectByContract()
  Spec target section: Select Runtime / Deleted Object Semantics

  7. Hub Integration Contracts

  SELECT-HUB-001 — Hub Select Loads Hub Membership According To Select Scope
  Contract statement: Loading a Hub from OASelect must add exactly the selected objects in selected order, subject
  to Hub filtering/sorting contracts.
  Rationale: Hubs are the main consumer of OASelect.
  Source locations: OASelect, Hub select/load services.
  Known related CODEX findings: Hub lazy-load/select correctness reviewed.
  Suggested unit tests: testHubSelectLoadsSelectedObjectsInOrder(), testHubSelectDoesNotAddObjectsOutsideScope()
  Spec target section: Select Runtime / Hub Loading

  SELECT-HUB-002 — Hub Select Must Not Mark Hub Loaded On Failed Select
  Contract statement: A Hub select failure must not mark the Hub as loaded or empty.
  Rationale: False loaded state prevents retry and hides data.
  Source locations: OASelect, Hub select/data services.
  Known related CODEX findings: lazy-load loaded/empty bugs reviewed.
  Suggested unit tests: testFailedHubSelectDoesNotMarkLoaded(), testFailedHubSelectCanRetry()
  Spec target section: Select Runtime / Hub Load State

  8. Client / Server / SingleUser Contracts

  SELECT-CS-001 — Client Select Delegates To Server Unless Local Cache Optimization Applies
  Contract statement: In client mode, select must use OADataSourceClient/remote datasource unless a documented local
  select-all cache optimization applies.
  Rationale: Server is authoritative for persistent query results.
  Source locations: OASelect, OADataSourceClient, RemoteDataSource.
  Known related CODEX findings: client datasource registration path CODEX-commented.
  Suggested unit tests: testClientSelectDelegatesToRemoteDatasource(),
  testClientSelectUsesLocalSelectAllCacheWhenDocumented()
  Spec target section: Select Runtime / Client Select Semantics

  SELECT-CS-002 — SingleUser Select Uses Local Datasource Path
  Contract statement: SingleUser mode must execute select through local runtime datasource routing, not client/
  server remote routing.
  Rationale: SingleUser is standalone/local.
  Source locations: OASelect, runtime sync role checks, datasource service.
  Known related CODEX findings: OASync role semantics reviewed.
  Suggested unit tests: testSingleUserSelectUsesLocalDatasource(), testSingleUserSelectDoesNotUseRemoteClient()
  Spec target section: Select Runtime / SingleUser Select Semantics

  SELECT-CS-003 — Remote Select Iterator IDs Must Be Released
  Contract statement: Client/server select must release remote iterator IDs when closed, exhausted, or failed.
  Rationale: Server-side iterators retain datasource resources and cached object references.
  Source locations: OADataSourceClient.MyIterator, RemoteDataSource.datasourceNext/IT_REMOVE, OASelect.close.
  Known related CODEX findings: remote iterator cleanup reviewed.
  Suggested unit tests: testRemoteSelectCloseReleasesServerIterator(),
  testRemoteSelectExhaustionReleasesServerIterator()
  Spec target section: Select Runtime / Remote Iterator Lifecycle

  9. Cancellation / Close Contracts

  SELECT-CLOSE-001 — Closed Select Returns No More Results
  Contract statement: Once closed, an OASelect must not return additional objects from its previous iterator.
  Rationale: Close is the caller’s resource and lifecycle boundary.
  Source locations: OASelect.close, OASelect.hasNext/next.
  Known related CODEX findings: closed/cancelled select behavior reviewed.
  Suggested unit tests: testClosedSelectHasNextFalse(), testClosedSelectNextReturnsDefinedNoResult()
  Spec target section: Select Runtime / Close Semantics

  SELECT-CANCEL-001 — Cancelled Select Must Stop Iteration And Cleanup
  Contract statement: Cancelling a select must stop future iteration and release owned iterator resources.
  Rationale: UI/background selects and Hub loads must be cancellable without leaks.
  Source locations: OASelect cancellation/close paths.
  Known related CODEX findings: cancellation/cleanup behavior reviewed.
  Suggested unit tests: testCancelSelectStopsIteration(), testCancelSelectClosesIterator()
  Spec target section: Select Runtime / Cancellation Semantics

  10. Failure / Retry / Silent No-Op Contracts

  SELECT-FAILURE-001 — Select Failure Must Be Visible Or Defined As No-Result
  Contract statement: Select execution failure must not silently look like a successful empty result unless the API
  explicitly defines that fallback.
  Rationale: Silent empty results hide datasource/query failures.
  Source locations: OASelect, datasource select wrappers.
  Known related CODEX findings: false-success/silent no-op risks reviewed.
  Suggested unit tests: testSelectDatasourceExceptionPropagates(),
  testSelectParseFailureDoesNotReturnFalseEmptyResult()
  Spec target section: Select Runtime / Failure Semantics

  SELECT-FAILURE-002 — Retry After Failed Select Must Use Fresh Iterator State
  Contract statement: Retrying a select after failure must not reuse partially consumed or failed iterator state.
  Rationale: Retry must produce a clean query execution.
  Source locations: OASelect.open/close, iterator fields.
  Known related CODEX findings: retry behavior reviewed.
  Suggested unit tests: testRetryAfterSelectFailureCreatesFreshIterator(),
  testRetryAfterRemoteSelectFailureUsesNewRemoteIteratorId()
  Spec target section: Select Runtime / Retry Semantics

  SELECT-NOOP-001 — Silent No-Op Select Is Allowed Only For Documented No-Datasource/Cancelled States
  Contract statement: Returning no results without error is valid only for documented states such as no datasource
  by contract, closed/cancelled select, or valid empty query result.
  Rationale: Prevents missing datasource/query bugs from masquerading as legitimate empty results.
  Source locations: OASelect, datasource routing, close/cancel methods.
  Known related CODEX findings: client datasource registration/no datasource path CODEX-commented.
  Suggested unit tests: testMissingDatasourceBehaviorExplicit(),
  testCancelledSelectNoResultIsDistinguishableFromQueryEmptyWhenContractRequires()
  Spec target section: Select Runtime / Silent No-Op Rules

  11. Test Coverage Matrix

  Lifecycle:

  - testSelectOpenCreatesIterator
  - testSelectCloseReleasesIterator
  - testSelectUsesConfigurationAtOpenTime
  - testReopenAfterCloseCreatesFreshIterator
  - testReopenDoesNotReuseExhaustedIterator

  Datasource routing:

  - testSelectUsesRuntimeDatasourceForClass
  - testSelectUsesFallbackDatasourceOnlyWhenPrimaryUnavailable
  - testSelectNoDatasourceHasDefinedBehavior
  - testMissingDatasourceDoesNotReturnFalseRows

  Iterator/resource:

  - testSelectCloseCallsIteratorRemove
  - testSelectNextExceptionClosesIterator
  - testSelectHasNextDoesNotSkipObject
  - testSelectNextAfterHasNextReturnsSamePendingObject

  Where/order/filter/max:

  - testSelectWhereParameterMatchesExpectedObject
  - testSelectMultipleParametersBindInOrder
  - testSelectOrderAscendingByProperty
  - testSelectOrderNestedPropertyByContract
  - testSelectFilterExcludesNonMatchingObject
  - testSelectMaxLimitsReturnedRows
  - testSelectMaxZeroMeansUnlimitedByContract

  Count/select:

  - testCountMatchesSelectForSimpleWhere
  - testCountMatchesSelectForDetailWhereObject
  - testUnavailableCountReturnsDefinedSentinel
  - testValidZeroCountDistinguishedFromUnavailable

  Identity/Hub:

  - testSelectReturnsCachedIdentityForExistingKey
  - testRepeatedSelectReturnsSameIdentityForSameKey
  - testSelectExcludesDeletedObjectByDefault
  - testHubSelectLoadsSelectedObjectsInOrder
  - testFailedHubSelectDoesNotMarkLoaded

  Client/server/single-user:

  - testClientSelectDelegatesToRemoteDatasource
  - testClientSelectUsesLocalSelectAllCacheWhenDocumented
  - testSingleUserSelectUsesLocalDatasource
  - testSingleUserSelectDoesNotUseRemoteClient
  - testRemoteSelectCloseReleasesServerIterator

  Close/cancel/failure:

  - testClosedSelectHasNextFalse
  - testClosedSelectNextReturnsDefinedNoResult
  - testCancelSelectStopsIteration
  - testCancelSelectClosesIterator
  - testSelectDatasourceExceptionPropagates
  - testRetryAfterSelectFailureCreatesFreshIterator
  - testRetryAfterRemoteSelectFailureUsesNewRemoteIteratorId


*/


