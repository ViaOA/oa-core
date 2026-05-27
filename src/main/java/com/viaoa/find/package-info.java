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
package com.viaoa.find;

//CODEX unit tests <todo>


/* CODEX Invariants

FIND-RUNTIME-001 — Deterministic Find Results
Contract statement:
For the same root graph state, property path, filters, max result count, lazy-load mode, recursive settings, and
metadata state, finder APIs must return the same matching objects or values in the same traversal order.
Rationale:
Finder results drive object graph traversal, Hub filtering, query fallback, templates, validation, and runtime
tooling. Nondeterministic traversal creates missed objects, false matches, and unstable runtime behavior.
Source scope:
OAFinder.find, OAFinder._find, OAFinder.findFirst/findNext/findLast, OAHierFinder.findFirst/findFirstValue.
Related CODEX findings:
OAFinder setup failure can leave partial embedded filter state and affect retry determinism.
Suggested unit tests:
testFinderReturnsDeterministicResultsForSameGraph, testFinderReturnsResultsInSameTraversalOrder,
testHierFinderReturnsSameFirstMatchForSameHierarchy.
Spec target section:
Finder Runtime / Deterministic Traversal Semantics

FIND-STATE-001 — Per-Search Execution State Isolation
Contract statement:
Per-search state such as found results, traversal stack, stop flag, root Hub position, cascade state, temporary
helper filters, and sibling helper state must be initialized and cleaned up for each search and must not leak into
later searches on the same finder.
Rationale:
OAFinder is reusable. Stale traversal state can silently truncate results, skip branches, duplicate matches, or
report incorrect stack diagnostics.
Source scope:
OAFinder.find overloads, OAFinder._find, OAFinder.findFirst/findNext/findLast,
OAFinder.findLargest/findSmallest/findDuplicates, OAFinder.getRootHubPos,
OAFinder.getStackObjects/getStackPropertyNames.
Related CODEX findings:
Prior retry-state cleanup issues were noted; setup rollback remains CODEX-noted.
Suggested unit tests:
testFinderStateClearedAfterSuccessfulSearch, testFinderStateClearedAfterTraversalException,
testFinderRetryAfterFilterExceptionDoesNotReuseCascadeState, testFindLargestRestoresFinderState.
Spec target section:
Finder Runtime / Execution State Semantics

FIND-SETUP-001 — Transactional Finder Setup
Contract statement:
A finder must not be considered initialized unless path parsing, link/method resolution, recursive metadata,
cascades, and embedded path-filter construction all complete successfully; failed setup must roll back all partially
installed state.
Rationale:
Partial setup can make retry behavior silently wrong by leaving duplicated filters, incomplete link metadata, stale
cascades, or mismatched property path state.
Source scope:
OAFinder.setup, OAFinder._setup, OAFinder.createHubFilter, OAPath filter metadata integration.
Related CODEX findings:
OAFinder CODEX comment: setup(Class) can leave partially installed embedded filters/state when embedded filter
creation throws.
Suggested unit tests:
testFinderSetupFailureDoesNotMarkInitialized, testFinderSetupFailureRollsBackEmbeddedFiltersAndState,
testFinderRetryAfterSetupFailureDoesNotReturnWrongResults.
Spec target section:
Finder Runtime / Setup and Retry Semantics

FIND-LIMIT-001 — Temporary Result Limits Are Restored
Contract statement:
Convenience methods that temporarily adjust maxFound or traversal limits must restore the caller’s configured values
on success, no match, stop, and exception.
Rationale:
A failed or short-circuit convenience lookup must not silently truncate later finder searches.
Source scope:
OAFinder.setMaxFound/getMaxFound, OAFinder.findFirst, OAFinder.findNext, OAFinder.findLast, OAFinder.canFindFirst.
Related CODEX findings:
Prior maxFound restoration issue was noted as fixed.
Suggested unit tests:
testFindFirstRestoresMaxFoundAfterSuccess, testFindFirstRestoresMaxFoundAfterException,
testCanFindFirstRestoresMaxFoundAfterException, testFindNextDoesNotPermanentlyChangeMaxFound.
Spec target section:
Finder Runtime / Result Limit Semantics

FIND-ROOT-001 — Root Overload Semantics
Contract statement:
Object-root, Hub-root, and List-root searches must use only the supplied root input for that search and must not
inherit stale root mode, recursive defaults, sibling helpers, or setup assumptions from prior searches.
Rationale:
Finder overloads are independent entry points. Prior calls must not change object graph scope or traversal behavior
for later calls.
Source scope:
OAFinder.setRoot, OAFinder.find(), OAFinder.find(F), OAFinder.find(Hub,F), OAFinder.find(List,F),
OAFinder._find(Hub,F).
Related CODEX findings:
Prior recursive-root default leak from Hub-root search was noted as fixed.
Suggested unit tests:
testObjectRootFindDoesNotReusePriorHubRootMode, testHubRootFindDoesNotReusePriorObjectRoot,
testListRootFindDoesNotReusePriorHubRecursiveDefault.
Spec target section:
Finder Runtime / Root Traversal Semantics

FIND-HUB-001 — Hub Traversal Order And Position
Contract statement:
Hub-root searches must traverse Hub elements in Hub order, start after objectLastUsed when supplied, update
rootHubPos consistently, and stop immediately when stop is requested.
Rationale:
Finder powers next-match, paging, UI navigation, and Hub-based scans. It must not skip, reorder, or continue after
caller-requested stop.
Source scope:
OAFinder.find(Hub,F), OAFinder._find(Hub,F), OAFinder.findNext, OAFinder.getRootHubPos, OAFinder.stop/getStop.
Related CODEX findings:
none.
Suggested unit tests:
testHubFindTraversesInHubOrder, testHubFindStartsAfterLastUsedObject, testHubFindStopsWhenOnFoundCallsStop,
testRootHubPosTracksMatchedPosition.
Spec target section:
Finder Runtime / Hub Traversal Semantics

FIND-LIST-001 — List Root Null And Order Semantics
Contract statement:
List-root searches must preserve list order, tolerate null entries, choose setup metadata from the first non-null
root, and return empty results for null, empty, or all-null lists.
Rationale:
List-root traversal should be stable for normal OA list inputs and must not fail because optional entries are
absent.
Source scope:
OAFinder.find(List,F), OAFinder.find(List).
Related CODEX findings:
Prior leading-null setup NPE was noted as fixed.
Suggested unit tests:
testListFindPreservesListOrder, testListFindSkipsLeadingNullRoots, testListFindAllNullReturnsEmpty,
testListFindEmptyReturnsEmpty.
Spec target section:
Finder Runtime / List Root Traversal Semantics

FIND-NULL-001 — Null Root And Null Reference Semantics
Contract statement:
Null roots and null intermediate traversal values must have explicit non-throwing semantics: null object-root search
returns null or no match as contracted, null Hub/List roots return empty results, and null references stop only that
traversal branch.
Rationale:
Optional OA references and absent roots are normal. Finder must treat them as absent paths without accidental NPEs
or broad matches.
Source scope:
OAFinder.find(F), OAFinder.find(Hub,F), OAFinder.find(List,F), OAFinder._find(Object,int),
OAHierFinder.findFirstValue.
Related CODEX findings:
Null Hub sibling helper NPE was noted as fixed.
Suggested unit tests:
testFindNullObjectRootReturnsNull, testFindNullHubReturnsEmpty, testFinderNullIntermediateReferenceStopsBranch,
testHierFinderNullParentReturnsNoMatch.
Spec target section:
Finder Runtime / Null Traversal Semantics

FIND-PATH-001 — OAPath Resolution Contract
Contract statement:
Finder property paths must resolve through OA metadata and must end at an OAObject or Hub traversal target for
object-finder results; invalid path segments or scalar terminal paths must fail visibly unless a scalar-value finder
contract explicitly applies.
Rationale:
OAFinder returns object targets. Treating scalar terminal properties as object traversal success causes invalid
casts or silent wrong results.
Source scope:
OAFinder.setup/_setup, OAFinder.getPropertyPath, OAPath link/method metadata, OAFinder._find(Object,int).
Related CODEX findings:
none.
Suggested unit tests:
testFinderRejectsScalarTerminalPath, testFinderAcceptsObjectTerminalPath, testFinderRejectsInvalidPropertyPath,
testFinderUsesMetadataForPathResolution.
Spec target section:
Finder Runtime / Path Resolution Semantics

FIND-PATH-002 — Path Segment Advancement
Contract statement:
Non-recursive traversal must consume exactly one path segment per link step; Hub values at a segment must iterate
their elements without incorrectly skipping or repeating path segments.
Rationale:
Wrong path-position movement produces missed branches, duplicated traversal, or evaluation of filters against the
wrong object.
Source scope:
OAFinder._find(Object,int), OAPath methods/linkInfos.
Related CODEX findings:
none.
Suggested unit tests:
testFinderTraversesNestedObjectPath, testFinderTraversesHubSegmentWithoutSkippingNextPathSegment,
testFinderDoesNotRepeatResolvedSegment.
Spec target section:
Finder Runtime / Property Path Traversal Semantics

FIND-RECURSIVE-001 — Recursive Root Control
Contract statement:
Explicit setAllowRecursiveRoot settings must be honored for the search and must not be recomputed from Hub/detail
metadata; implicit recursive-root defaults must be scoped to the current root type and restored afterward.
Rationale:
Callers must be able to control recursive-root traversal deterministically, and recursive detail Hub convenience
behavior must not contaminate later searches.
Source scope:
OAFinder.setAllowRecursiveRoot/getAllowRecursiveRoot, OAFinder.find(Hub,F), OAFinder.find(List,F), OAFinder.find(F).
Related CODEX findings:
Prior Hub-root recursive default leak into object-root search was noted as fixed.
Suggested unit tests:
testExplicitRecursiveRootFalseIsHonoredForRecursiveDetailHub, testExplicitRecursiveRootTrueIsHonoredForObjectRoot,
testRecursiveDetailHubDefaultRestoredAfterHubFind.
Spec target section:
Finder Runtime / Recursive Root Semantics

FIND-RECURSIVE-002 — Recursive Link Position Semantics
Contract statement:
Recursive traversal may remain at the same path position only when moving through a recursive relationship; normal
path traversal must advance position normally.
Rationale:
Recursive traversal must search parent/sibling hierarchies or recursive detail links without corrupting path segment
evaluation.
Source scope:
OAFinder._find(Object,int), OAHierFinder.findFirstValue, recursiveLinkInfos handling.
Related CODEX findings:
OAHierFinder recursive counter is scoped to recursive hierarchy links rather than general model-cycle detection.
Suggested unit tests:
testFinderRecursiveRootKeepsSamePathPosition, testRecursiveTraversalDoesNotConsumeNormalPathSegment,
testHierFinderRecursiveParentKeepsSamePathPosition.
Spec target section:
Finder Runtime / Recursive Position Semantics

FIND-CYCLE-001 — Cycle And Depth Termination
Contract statement:
Finder traversal must terminate on normal cyclic OA object graphs using cascade/visited/depth protections, and depth
caps must prevent runaway recursion without adding false results.
Rationale:
OA graphs commonly contain bidirectional links and recursive relationships. Finder must not hang, overflow the
stack, or silently duplicate cyclic results.
Source scope:
OAFinder._find(Object,int), OAFinder.find(Object,int), OACascade integration, OAHierFinder recursive traversal.
Related CODEX findings:
Recursive-root traversal can bypass normal cascade protection; parent-loop behavior clarified for OAHierFinder.
Suggested unit tests:
testFinderBidirectionalLinksDoNotLoop, testFinderRecursiveRootCycleDoesNotLoopForever, testFinderStopsAtDepthCap,
testDepthCapDoesNotAddFalseResult.
Spec target section:
Finder Runtime / Cycle Protection

FIND-IDENTITY-001 — Object Identity And Duplicate Result Semantics
Contract statement:
Finder traversal must preserve OA object identity and must not duplicate the same authoritative object in results
unless the API explicitly returns duplicates by value.
Rationale:
Finder results feed Hubs, object graph traversal, filters, and utility searches. Duplicate identity results can
cause duplicate UI rows, repeated processing, and incorrect graph semantics.
Source scope:
OAFinder.alFound/result accumulation, OAFinder.findDuplicates, OAFinder.DuplicateFilter, Hub/List/object traversal.
Related CODEX findings:
none.
Suggested unit tests:
testFinderDoesNotReturnSameObjectTwiceFromCyclicGraph, testFindDuplicatesUsesPropertyValueContract,
testFindDuplicatesIgnoresNullValues, testFindDuplicatesIncludesFirstAndLaterDuplicateNonNullValues.
Spec target section:
Finder Runtime / Identity and Duplicate Semantics

FIND-FILTER-001 — Terminal Filter Evaluation
Contract statement:
Finder filters must be evaluated against candidate target objects only after path traversal reaches the terminal
object position, unless a path-embedded filter explicitly constrains an intermediate segment.
Rationale:
Applying filters too early, too late, or to the wrong object creates false positives and false negatives.
Source scope:
OAFinder._find(Object,int), OAFinder.addFilter/setFilter/getFilter/clearFilters, OAFinder.isUsed, embedded path fil
ter setup.
Related CODEX findings:
none.
Suggested unit tests:
testFinderFilterAppliesToTerminalObject, testFinderFilterDoesNotRejectIntermediateObject,
testEmbeddedPathFilterRestrictsIntermediateTraversal.
Spec target section:
Finder Runtime / Filter Evaluation Semantics

FIND-FILTER-002 — Filter Composition Stability
Contract statement:
Programmatic filter composition through addFilter, addOrFilter, and addAndFilter must be predictable, and helper
searches must restore any temporary filter chain or pending composition state afterward.
Rationale:
Finder callers build complex filters incrementally. Helper methods must act as query operations, not permanent
configuration mutations.
Source scope:
OAFinder.addFilter, addOrFilter, addAndFilter, clearFilters, findLargest, findSmallest, findDuplicates.
Related CODEX findings:
Temporary helper methods previously consumed pending OR/AND state; helper filter chain contamination was noted as
fixed.
Suggested unit tests:
testAddOrFilterComposesNextFilterWithOr, testAddAndFilterComposesNextFilterWithAnd,
testFindLargestDoesNotConsumePendingOrFilterState, testFindDuplicatesRestoresOriginalFilterAndCompositionFlags.
Spec target section:
Finder Runtime / Filter Composition Semantics

FIND-FILTER-003 — Embedded Path Filter Setup
Contract statement:
Filters embedded in property paths must be created during successful setup and applied as semantic traversal
constraints; embedded filter creation failure must fail the setup visibly and leave no partial filter state.
Rationale:
Path-level filter directives are part of finder semantics. Partial installation causes silently wrong traversal on
retry.
Source scope:
OAFinder._setup, OAFinder.createHubFilter, OAPath.getFilterNames, OAPath.getFilterConstructors.
Related CODEX findings:
OAFinder CODEX comment: setup failure can leave partially installed embedded filters/state.
Suggested unit tests:
testEmbeddedPathFilterRestrictsFinderResults, testEmbeddedPathFilterCreationFailureDoesNotReturnResults,
testEmbeddedPathFilterCreationFailureRollsBackFilterChain.
Spec target section:
Finder Runtime / Embedded Filter Semantics

FIND-LOADED-001 — Loaded-Only Traversal Semantics
Contract statement:
When useOnlyLoadedData is true, finder traversal must not trigger lazy loading; unloaded required links must call
onDataNotFound and stop that branch.
Rationale:
Callers use loaded-only mode for in-memory scans, non-invasive diagnostics, and predictable no-I/O traversal.
Source scope:
OAFinder.setUseOnlyLoadedData/getUseOnlyLoadedData, OAFinder._find(Object,int), OAFinder.onDataNotFound.
Related CODEX findings:
none.
Suggested unit tests:
testUseOnlyLoadedDataDoesNotLoadUnloadedLink, testUseOnlyLoadedDataCallsOnDataNotFound,
testUseOnlyLoadedDataStopsOnlyUnloadedBranch.
Spec target section:
Finder Runtime / Loaded-State Semantics

FIND-LAZY-001 — Lazy Hub Scan Helper Lifecycle
Contract statement:
Lazy Hub scan optimizations such as sibling helpers may be installed only for the duration of the relevant Hub
search and must be removed in finally-style cleanup on success, stop, and exception.
Rationale:
Thread-local lazy-load helpers must not leak into unrelated graph operations or alter later runtime behavior.
Source scope:
OAFinder.find(Hub,F), OASiblingHelper integration, OAThreadLocalService.addSiblingHelper/removeSiblingHelper, useOn
lyLoadedData checks.
Related CODEX findings:
Null Hub no longer creates sibling helper; cleanup expectations noted.
Suggested unit tests:
testHubFindRegistersSiblingHelperInLazyMode, testFinderRemovesSiblingHelperAfterTraversalException,
testNullHubFindDoesNotRegisterSiblingHelper, testUseOnlyLoadedDataDoesNotInstallSiblingHelper.
Spec target section:
Finder Runtime / Lazy Load Optimization and ThreadLocal Cleanup

FIND-HIER-001 — Hierarchical First-Match Semantics
Contract statement:
OAHierFinder.findFirst must return the first property value encountered by its hierarchy traversal that satisfies
the supplied filter, following the configured include-from-object rule.
Rationale:
Hierarchical finder supports inherited/defaulted values across object hierarchies. Nearest applicable value
semantics must be deterministic.
Source scope:
OAHierFinder constructors, findFirst, findFirstValue, findFirstNotEmpty, findFirstEmpty, findFirstNotNull,
findFirstTrue.
Related CODEX findings:
none.
Suggested unit tests:
testHierFinderReturnsStartingObjectValueFirstWhenIncluded, testHierFinderReturnsNearestParentValue,
testHierFinderExcludeFromObjectSkipsStartingValue, testHierFinderFilterControlsAcceptedValue.
Spec target section:
Finder Runtime / Hierarchical First-Match Semantics

FIND-HIER-002 — Hierarchical Metadata Traversal
Contract statement:
OAHierFinder must traverse hierarchy path segments using OA metadata links and must apply recursive parent traversal
according to reverse-link metadata and configured recursion bounds.
Rationale:
Hierarchical traversal must follow OA metadata truth rather than string-only assumptions, and recursion bounds must
prevent runaway parent traversal.
Source scope:
OAHierFinder.findFirst, OAHierFinder.findFirstValue, OAPath link metadata, recursive counter behavior.
Related CODEX findings:
OAHierFinder recursive counter applies to recursive hierarchy links, not as a general cycle detector.
Suggested unit tests:
testHierFinderTraversesPathLinkSegments, testHierFinderUsesReverseLinkToLimitRecursiveParentSearch,
testHierFinderRecursiveCounterStopsDeepRecursiveParentSearch.
Spec target section:
Finder Runtime / Hierarchical Path Semantics

FIND-NULL-002 — FindFirst Null Ambiguity
Contract statement:
findFirst may return null for both “no match” and “matched null value”; callers that need existence semantics must
use canFindFirst or a filter that disambiguates null matches.
Rationale:
Null-valued matches are valid in some finder usages. The API contract must avoid accidental interpretation of null
as only “not found.”
Source scope:
OAFinder.findFirst, OAFinder.canFindFirst, OAHierFinder.findFirst variants.
Related CODEX findings:
none.
Suggested unit tests:
testCanFindFirstDetectsNullMatchWhenFindFirstWouldReturnNull, testFindFirstReturnsNullForNoMatch,
testNullValueMatchRequiresExplicitDisambiguation.
Spec target section:
Finder Runtime / Null Match Semantics

FIND-STACK-001 — Traversal Stack Diagnostics
Contract statement:
When stack tracking is enabled, stack objects and property names must reflect the actual active traversal path
visible to onFound and overridden hook methods, and must be cleaned up after traversal.
Rationale:
Custom finder implementations and diagnostics rely on accurate traversal stack context.
Source scope:
OAFinder.setEnabledStack, OAFinder.push/pop, OAFinder.getStackObjects, OAFinder.getStackPropertyNames, OAFinder.onF
ound.
Related CODEX findings:
none.
Suggested unit tests:
testFinderStackObjectsDuringOnFound, testFinderStackPropertyNamesMatchTraversal,
testFinderStackClearedAfterException.
Spec target section:
Finder Runtime / Traversal Stack Semantics

FIND-FAIL-001 — Failure Visibility And Partial Traversal
Contract statement:
Invalid paths, setup failures, filter construction failures, traversal exceptions, and unsupported finder structures
must fail visibly or return a documented no-result value; they must not silently return partial results as complete
success.
Rationale:
Silent false-success finder behavior corrupts Hub contents, query fallback, object graph traversal, validation, and
runtime tooling.
Source scope:
OAFinder.setup/_setup, OAFinder.find overloads, OAFinder._find, OAHierFinder.findFirstValue, filter setup and evalu
ation paths.
Related CODEX findings:
OAFinder setup rollback issue can make retry silently wrong after partial setup failure.
Suggested unit tests:
testInvalidFinderPathThrowsVisibleException, testFailingFilterDoesNotReturnSilentEmptyResult,
testFinderSetupFailureDoesNotExposePartialResults, testTraversalExceptionDoesNotReportCompleteSuccess.
Spec target section:
Finder Runtime / Failure Semantics

FIND-RETRY-001 — Retry Correctness After Failure
Contract statement:
After setup, traversal, filter, or hook failure, a later retry on the same finder must either execute with fresh
coherent state or fail consistently; it must not reuse stale partial traversal/setup state.
Rationale:
OA runtime operations may retry after incomplete work. Retrying with corrupted finder state creates missed objects,
duplicate filters, or false matches.
Source scope:
OAFinder.find overloads, OAFinder.setup, OAFinder._setup, OAFinder._find, helper methods that mutate temporary
state.
Related CODEX findings:
Traversal state cleanup was noted as fixed; setup rollback remains CODEX-commented/deferred.
Suggested unit tests:
testFinderRetryAfterTraversalFailureIsFresh, testFinderRetryAfterSetupFailureDoesNotReturnWrongResults,
testFinderRetryAfterEmbeddedFilterFailureDoesNotDuplicateFilters.
Spec target section:
Finder Runtime / Retry Semantics

FIND-CONCURRENT-001 — Finder Reuse And Concurrency Assumptions
Contract statement:
A finder instance must either be used by one search at a time or clearly document caller synchronization
requirements; shared mutable search state must not be assumed safe for concurrent searches on the same finder.
Rationale:
Finder instances carry mutable setup, result, stack, filter, and traversal state. Concurrent reuse can corrupt
results and traversal cleanup.
Source scope:
OAFinder mutable fields and all find/helper methods, OAHierFinder traversal state.
Related CODEX findings:
Per-search mutable state and setup rollback concerns imply single-search ownership requirements.
Suggested unit tests:
testSequentialFinderReuseIsStable, testConcurrentSameFinderUseRequiresDocumentedSynchronization,
testIndependentFinderInstancesCanRunConcurrently.
Spec target section:
Finder Runtime / Concurrency Semantics

FIND-INTEGRATION-001 — Cross-Package Finder Compatibility
Contract statement:
Finder behavior must remain compatible with OAPath metadata, OAFilter semantics, Hub ordering and active/detail
behavior, OAObject identity/cache semantics, load-state/lazy-load rules, cascade cycle protection, and graph/runtime
context boundaries.
Rationale:
Finder is a shared graph traversal layer. Boundary drift causes incorrect Hubs, query fallback, object graph
traversal, lazy loading, and runtime tooling output.
Source scope:
OAFinder, OAHierFinder, OAPath integration, OAFilter integration, Hub traversal, OACascade, OASiblingHelper,
OAThreadLocalService.
Related CODEX findings:
Setup rollback, recursive-root state, sibling helper cleanup, loaded-only traversal, and filter composition findings
all illustrate cross-package boundary assumptions.
Suggested unit tests:
testFinderAndOAPathAgreeOnTraversalTargets, testFinderAndOAFilterAgreeOnTerminalPredicateSemantics,
testFinderPreservesHubOrderAndObjectIdentity, testLoadedOnlyFinderRespectsLoadContracts.
Spec target section:
Finder Runtime / Cross-Package Contracts

*/



