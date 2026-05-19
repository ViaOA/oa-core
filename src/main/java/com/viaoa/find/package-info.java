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

/* CODEX Invariants

1. Finder Runtime Contracts

  FIND-RUNTIME-001 — Finder Results Are Deterministic
  Contract statement: For the same root graph state, path, filters, max count, lazy-load mode, and recursive settings,
  OAFinder and OAHierFinder must return the same results in the same traversal order.
  Rationale: Finder is used by cache/search/select/template/query-style behavior; nondeterminism causes false matches,
  missed objects, and unstable application logic.
  Source locations: OAFinder.find(...), OAFinder._find(...), OAFinder.performFind(...), OAHierFinder.findFirst(...).
  Known related CODEX findings: setup failure can leave partial embedded filter state and affect retry determinism.
  Suggested unit tests: testFinderReturnsDeterministicResultsForSameGraph,
  testHierFinderReturnsSameFirstMatchForSameHierarchy.
  Spec target section: Finder Runtime / Deterministic Traversal Semantics.

  FIND-RUNTIME-002 — Finder Instance Execution State Is Per Search
  Contract statement: Per-search state such as alFound, traversal stack, cascades, bStop, rootHubPos, and temporary
  helper state must not leak into later searches on the same finder.
  Rationale: OAFinder is reusable; stale state creates false negatives, truncated results, or wrong stack diagnostics.
  Source locations: OAFinder.find(F), OAFinder.find(List,F), OAFinder.find(Hub,F), OAFinder._find(Hub,F).
  Known related CODEX findings: prior retry-state cleanup issues for object/list/Hub roots; fixed except setup
  rollback is CODEX-noted.
  Suggested unit tests: testFinderStateClearedAfterSuccessfulSearch, testFinderStateClearedAfterTraversalException,
  testHubFinderRetryAfterFilterExceptionDoesNotReuseCascadeState.
  Spec target section: Finder Runtime / Execution State Semantics.

  FIND-RUNTIME-003 — maxFound Is Temporary When Used By Convenience Methods
  Contract statement: Methods such as findFirst, findNext, and canFindFirst may temporarily set maxFound, but must
  restore the caller’s configured value even when traversal throws.
  Rationale: A failed convenience lookup must not silently truncate later searches.
  Source locations: OAFinder.canFindFirst, OAFinder.findFirst(F), OAFinder.findFirst(Hub), OAFinder.findNext(Hub,F).
  Known related CODEX findings: prior missing finally restoration; fixed.
  Suggested unit tests: testFindFirstRestoresMaxFoundAfterException, testCanFindFirstRestoresMaxFoundAfterException.
  Spec target section: Finder Runtime / Result Limit Semantics.

  FIND-RUNTIME-004 — Setup Must Fully Succeed Before Finder Is Considered Initialized
  Contract statement: A finder must not appear initialized unless path parsing, link/method resolution, recursive
  metadata, cascades, and embedded filter construction all complete successfully.
  Rationale: Partial setup can make a retry return silently wrong results.
  Source locations: OAFinder.setup(Class), OAFinder._setup(Class).
  Known related CODEX findings: setup can leave partially installed embedded filters/state when embedded filter
  creation throws; CODEX-commented/deferred.
  Suggested unit tests: testFinderSetupFailureDoesNotMarkInitialized,
  testFinderSetupFailureRollsBackEmbeddedFiltersAndState.
  Spec target section: Finder Runtime / Setup and Retry Semantics.

  2. Object / Hub Traversal Contracts

  FIND-TRAVERSE-001 — Object Root Traversal Starts From The Supplied Root
  Contract statement: find(F objectRoot) must traverse only from the supplied object root and must not inherit stale
  root mode from prior Hub/list searches.
  Rationale: Finder overloads must be independently correct; prior calls must not change root semantics.
  Source locations: OAFinder.find(F), OAFinder.find(), OAFinder.setRoot(F).
  Known related CODEX findings: prior recursive-root default leak from Hub-root search; fixed.
  Suggested unit tests: testObjectRootFindDoesNotReusePriorHubRecursiveRootDefault.
  Spec target section: Finder Runtime / Object Root Traversal.

  FIND-TRAVERSE-002 — Hub Root Traversal Uses Hub Order And Stop Semantics
  Contract statement: find(Hub,F) must traverse Hub elements in Hub order, starting after objectLastUsed when
  supplied, and must stop immediately when bStop is set.
  Rationale: Finder powers paging/next-match behavior and must not skip or reorder Hub entries.
  Source locations: OAFinder.find(Hub,F), OAFinder._find(Hub,F), OAFinder.getRootHubPos.
  Known related CODEX findings: none observed.
  Suggested unit tests: testHubFindTraversesInHubOrder, testHubFindStartsAfterLastUsedObject,
  testHubFindStopsWhenOnFoundCallsStop.
  Spec target section: Finder Runtime / Hub Traversal.

  FIND-TRAVERSE-003 — List Root Traversal Ignores Null Roots Without Failing
  Contract statement: find(List,F) must tolerate null entries, choose setup metadata from the first non-null root, and
  return empty results for null/empty/all-null lists.
  Rationale: List-based root searches should be stable for normal OA list inputs.
  Source locations: OAFinder.find(List,F).
  Known related CODEX findings: prior leading-null setup NPE; fixed.
  Suggested unit tests: testListFindSkipsLeadingNullRoots, testListFindAllNullReturnsEmpty,
  testListFindEmptyReturnsEmpty.
  Spec target section: Finder Runtime / List Root Traversal.

  FIND-TRAVERSE-004 — Null Hub And Null Object Roots Have Explicit Semantics
  Contract statement: Null object root returns null for object-root search; null Hub root returns an empty result list
  for Hub-root search and must not install sibling helpers.
  Rationale: Callers need stable null-root behavior without accidental NPEs.
  Source locations: OAFinder.find(F), OAFinder.find(Hub,F), OAFinder._find(Hub,F).
  Known related CODEX findings: null Hub sibling helper NPE; fixed.
  Suggested unit tests: testFindNullObjectRootReturnsNull, testFindNullHubReturnsEmptyWithLazyMode.
  Spec target section: Finder Runtime / Null Root Semantics.

  3. Path / Property Traversal Contracts

  FIND-PATH-001 — Finder Path Must Resolve To OAObject Or Hub Target
  Contract statement: OAFinder property paths must end at an OAObject or Hub traversal target, not a scalar property.
  Invalid terminal scalar paths must fail visibly.
  Rationale: OAFinder<F,T> returns OAObject targets; scalar path success would produce invalid casts or silent wrong
  results.
  Source locations: OAFinder._setup(Class), OAPath.getLinkInfos, OAPath.getMethods.
  Known related CODEX findings: none observed.
  Suggested unit tests: testFinderRejectsScalarTerminalPath, testFinderAcceptsObjectTerminalPath.
  Spec target section: Finder Runtime / Path Resolution.

  FIND-PATH-002 — Traversal Advances Exactly One Segment Per Link Step
  Contract statement: For non-recursive link traversal, each linkInfos[pos] access must recurse to pos + 1, while Hub
  values at a step must iterate their elements without advancing the segment again.
  Rationale: Wrong position movement creates skipped path segments or repeated segment evaluation.
  Source locations: OAFinder._find(Object,int).
  Known related CODEX findings: none observed.
  Suggested unit tests: testFinderTraversesNestedObjectPath,
  testFinderTraversesHubSegmentWithoutSkippingNextPathSegment.
  Spec target section: Finder Runtime / Property Path Traversal.

  FIND-PATH-003 — Stack Diagnostics Reflect Actual Traversal Path
  Contract statement: When enabled, stack objects and property names must describe the active traversal path seen by
  onFound and overridden hooks.
  Rationale: Stack data is used by custom finder implementations and diagnostics.
  Source locations: OAFinder.setEnabledStack, OAFinder.push, OAFinder.pop, OAFinder.getStackObjects,
  OAFinder.getStackPropertyNames.
  Known related CODEX findings: none observed.
  Suggested unit tests: testFinderStackObjectsDuringOnFound, testFinderStackPropertyNamesMatchTraversal.
  Spec target section: Finder Runtime / Traversal Stack Semantics.

  4. Hierarchical Finder Contracts

  FIND-HIER-001 — Hierarchical Finder Returns The First Matching Value
  Contract statement: OAHierFinder.findFirst must return the first property value encountered by its hierarchy
  traversal that satisfies the supplied filter.
  Rationale: OAHierFinder is used for inherited/defaulted values across object hierarchies.
  Source locations: OAHierFinder.findFirst, OAHierFinder.findFirstValue.
  Known related CODEX findings: none observed.
  Suggested unit tests: testHierFinderReturnsStartingObjectValueFirst, testHierFinderReturnsNearestParentValue.
  Spec target section: Finder Runtime / Hierarchical First-Match Semantics.

  FIND-HIER-002 — Include-From-Object Controls Starting Object Evaluation
  Contract statement: When bIncludeFromObject is false, the starting object must not be evaluated for the target
  property except where recursive-check-only semantics explicitly allow/deny traversal.
  Rationale: Hierarchical defaults often need “inherit from parent only” behavior.
  Source locations: OAHierFinder(String,String,boolean), OAHierFinder.findFirstValue.
  Known related CODEX findings: none observed.
  Suggested unit tests: testHierFinderExcludeFromObjectSkipsStartingValue,
  testHierFinderIncludeFromObjectUsesStartingValue.
  Spec target section: Finder Runtime / Hierarchical Include Semantics.

  FIND-HIER-003 — Hierarchical Path Segments Must Follow Metadata Links
  Contract statement: OAHierFinder must traverse each hierarchy segment using OAPath link metadata and evaluate parent
  recursion according to reverse-link metadata.
  Rationale: Hierarchical traversal must follow OA metadata truth rather than string-only assumptions.
  Source locations: OAHierFinder.findFirst, OAHierFinder.findFirstValue, OAPath.getLinkInfos.
  Known related CODEX findings: none observed.
  Suggested unit tests: testHierFinderTraversesPathLinkSegments,
  testHierFinderUsesReverseLinkToLimitRecursiveParentSearch.
  Spec target section: Finder Runtime / Hierarchical Path Semantics.

  5. Recursive Link Traversal Contracts

  FIND-RECURSIVE-001 — Explicit Recursive Root Setting Overrides Defaults
  Contract statement: If caller invokes setAllowRecursiveRoot, that value must be honored and must not be recomputed
  from Hub/detail metadata.
  Rationale: Callers must be able to control recursive-root traversal deterministically.
  Source locations: OAFinder.setAllowRecursiveRoot, OAFinder.find(List,F), OAFinder.find(Hub,F).
  Known related CODEX findings: none observed.
  Suggested unit tests: testExplicitRecursiveRootFalseIsHonoredForRecursiveDetailHub,
  testExplicitRecursiveRootTrueIsHonoredForObjectRoot.
  Spec target section: Finder Runtime / Recursive Root Semantics.

  FIND-RECURSIVE-002 — Implicit Recursive Root Defaults Are Root-Type Scoped
  Contract statement: When recursive-root behavior is not explicitly set, object/list roots default to non-recursive
  root traversal, while recursive detail Hub roots may enable recursive root traversal for that search only.
  Rationale: Defaults must support recursive detail Hubs without contaminating later searches.
  Source locations: OAFinder.find(List,F), OAFinder.find(Hub,F), OAFinder.find(F).
  Known related CODEX findings: prior Hub-root default leaked into object-root search; fixed.
  Suggested unit tests: testRecursiveDetailHubEnablesRecursiveRootForThatSearch,
  testRecursiveRootDefaultRestoredAfterHubFind.
  Spec target section: Finder Runtime / Recursive Default Semantics.

  FIND-RECURSIVE-003 — Recursive Link Traversal Must Preserve Path Position Intentionally
  Contract statement: Recursive parent/root traversal may recurse at the same path position only when it is
  semantically moving through a recursive relationship, not consuming a normal path segment.
  Rationale: Recursive traversal must search parent/sibling hierarchies without corrupting path segment evaluation.
  Source locations: OAFinder._find(Object,int), OAHierFinder.findFirstValue.
  Known related CODEX findings: recursive counter in OAHierFinder is scoped to recursive hierarchy links, not general
  hierarchy-loop detection.
  Suggested unit tests: testFinderRecursiveRootKeepsSamePathPosition,
  testHierFinderRecursiveParentKeepsSamePathPosition.
  Spec target section: Finder Runtime / Recursive Position Semantics.

  6. Filter Integration Contracts

  FIND-FILTER-001 — Terminal Object Filters Gate Inclusion Only At Target Position
  Contract statement: Finder filters must be applied to candidate target objects after path traversal reaches the
  terminal object position.
  Rationale: Applying filters too early or too late creates false positives/negatives.
  Source locations: OAFinder._find(Object,int), OAFinder.addFilter, OAFinder.isUsed.
  Known related CODEX findings: none observed.
  Suggested unit tests: testFinderFilterAppliesToTerminalObject, testFinderFilterDoesNotRejectIntermediateObject.
  Spec target section: Finder Runtime / Filter Evaluation.

  FIND-FILTER-002 — Programmatic Filter Composition Is Stable
  Contract statement: addFilter, addOrFilter, and addAndFilter must compose filters predictably, and temporary helper
  searches must not consume pending composition state.
  Rationale: Finder callers can build complex filters incrementally; helper methods must not mutate builder state.
  Source locations: OAFinder.addFilter, OAFinder.addOrFilter, OAFinder.addAndFilter, OAFinder.findLargest,
  OAFinder.findSmallest, OAFinder.findDuplicates.
  Known related CODEX findings: temporary helper methods consumed pending OR/AND state; fixed.
  Suggested unit tests: testAddOrFilterComposesNextFilterWithOr, testFindLargestDoesNotConsumePendingOrFilterState.
  Spec target section: Finder Runtime / Filter Composition.

  FIND-FILTER-003 — Embedded PropertyPath Filters Are Part Of Finder Setup
  Contract statement: Filters embedded in a property path must be created once during successful setup and applied as
  part of the finder’s filter chain.
  Rationale: Path-level filter directives are semantic traversal constraints.
  Source locations: OAFinder._setup(Class), OAFinder.createHubFilter, OAPath.getFilterNames,
  OAPath.getFilterConstructors.
  Known related CODEX findings: setup failure can partially install embedded filters; CODEX-commented/deferred.
  Suggested unit tests: testEmbeddedPathFilterRestrictsFinderResults,
  testEmbeddedPathFilterCreationFailureDoesNotReturnResults.
  Spec target section: Finder Runtime / Embedded Filter Semantics.

  FIND-FILTER-004 — Helper Filters Must Be Temporary
  Contract statement: findLargest, findSmallest, and findDuplicates may add internal filters during execution, but
  must restore the caller’s original filter chain and composition flags afterward.
  Rationale: Helper calls should not change later finder behavior.
  Source locations: OAFinder.findLargest, OAFinder.findSmallest, OAFinder.findDuplicates.
  Known related CODEX findings: prior helper filter chain contamination and pending composition-state mutation; fixed.
  Suggested unit tests: testFindSmallestRestoresOriginalFilterAfterException,
  testFindDuplicatesRestoresOriginalFilterAndCompositionFlags.
  Spec target section: Finder Runtime / Temporary Helper Filter Semantics.

  7. Null / Empty / Unresolved Reference Contracts

  FIND-NULL-001 — Null Traversal Values Stop That Branch
  Contract statement: If a link/property traversal produces null, that traversal branch must stop without adding a
  result or throwing for normal null reference cases.
  Rationale: Optional OA references are normal; finder must treat them as absent paths.
  Source locations: OAFinder.find(Object,int), OAFinder._find(Object,int), OAHierFinder.findFirstValue.
  Known related CODEX findings: none observed.
  Suggested unit tests: testFinderNullIntermediateReferenceStopsBranch, testHierFinderNullParentReturnsNoMatch.
  Spec target section: Finder Runtime / Null Reference Semantics.

  FIND-NULL-002 — Null Result Ambiguity Is Explicit For FindFirst
  Contract statement: findFirst may return null both for “no match” and “matched null value”; callers needing
  existence must use canFindFirst or filter semantics that disambiguate.
  Rationale: Prevents accidental misinterpretation of null-valued matches.
  Source locations: OAFinder.findFirst(F), OAFinder.canFindFirst.
  Known related CODEX findings: none observed.
  Suggested unit tests: testCanFindFirstDetectsNullMatchWhenFindFirstWouldReturnNull.
  Spec target section: Finder Runtime / Null Match Semantics.

  FIND-NULL-003 — Duplicate Finder Excludes Null Values By Contract
  Contract statement: findDuplicates must not treat null property values as duplicate values.
  Rationale: Null usually means “missing value,” not a duplicate business value.
  Source locations: OAFinder.DuplicateFilter.isUsed, OAFinder.findDuplicates.
  Known related CODEX findings: none observed.
  Suggested unit tests: testFindDuplicatesIgnoresNullValues,
  testFindDuplicatesIncludesFirstAndLaterDuplicateNonNullValues.
  Spec target section: Finder Runtime / Duplicate Value Semantics.

  FIND-UNRESOLVED-001 — Use-Only-Loaded Mode Must Not Materialize Unloaded Data
  Contract statement: When useOnlyLoadedData is true, finder traversal must not trigger lazy loading; unloaded
  required links must call onDataNotFound and stop that branch.
  Rationale: Callers use this mode for in-memory-only scans and predictable non-loading behavior.
  Source locations: OAFinder.setUseOnlyLoadedData, OAFinder._find(Object,int), OAFinder.onDataNotFound.
  Known related CODEX findings: none observed.
  Suggested unit tests: testUseOnlyLoadedDataDoesNotLoadUnloadedLink, testUseOnlyLoadedDataCallsOnDataNotFound.
  Spec target section: Finder Runtime / Loaded-State Semantics.

  8. Cycle / Depth Protection Contracts

  FIND-CYCLE-001 — Finder Must Not Loop Forever On Object Graph Cycles
  Contract statement: Traversal must use cascade/depth protection so normal cyclic object graphs do not cause infinite
  recursion.
  Rationale: OA object graphs commonly contain bidirectional links and recursive relationships.
  Source locations: OAFinder.find(Object,int), OAFinder._find(Object,int), OACascade.
  Known related CODEX findings: recursive-root traversal can bypass normal cascade protection; reported/noted.
  Suggested unit tests: testFinderBidirectionalLinksDoNotLoop, testFinderRecursiveRootCycleDoesNotLoopForever.
  Spec target section: Finder Runtime / Cycle Protection.

  FIND-CYCLE-002 — Depth Cap Protects Against Runaway Traversal
  Contract statement: Finder recursion must stop when traversal depth exceeds the internal safety cap.
  Rationale: Prevents stack overflow from malformed or unexpectedly deep path traversal.
  Source locations: OAFinder.find(Object,int).
  Known related CODEX findings: none observed.
  Suggested unit tests: testFinderStopsAtDepthCap, testFinderDepthCapDoesNotAddFalseResult.
  Spec target section: Finder Runtime / Depth Protection.

  FIND-CYCLE-003 — Hierarchical Recursive Counter Applies To Recursive Hierarchy Links
  Contract statement: OAHierFinder’s recursive counter bounds traversal through recursive hierarchy links; it is not a
  general model-cycle detector unless explicitly extended.
  Rationale: This matches current intended semantics and prevents overclaiming cycle protection.
  Source locations: OAHierFinder.findFirstValue.
  Known related CODEX findings: parent-loop finding clarified as out of scope for that counter.
  Suggested unit tests: testHierFinderRecursiveHierarchyCounterStopsDeepRecursiveParentSearch.
  Spec target section: Finder Runtime / Hierarchy Recursion Semantics.

  9. Lazy Load Interaction Contracts

  FIND-LAZY-001 — Lazy Mode May Install Sibling Helper For Hub Roots
  Contract statement: When searching a non-null Hub root with lazy loading enabled, finder may install a temporary
  OASiblingHelper for the duration of that search.
  Rationale: Sibling helper optimizes related lazy loads during Hub scans.
  Source locations: OAFinder.find(Hub,F), OASiblingHelper.
  Known related CODEX findings: null Hub no longer creates sibling helper; fixed.
  Suggested unit tests: testHubFindRegistersSiblingHelperInLazyMode, testNullHubFindDoesNotRegisterSiblingHelper.
  Spec target section: Finder Runtime / Lazy Load Optimization.

  FIND-LAZY-002 — Sibling Helper Must Always Be Removed
  Contract statement: Any sibling helper installed by finder must be removed in finally, even when traversal throws.
  Rationale: Thread-local sibling helpers must not leak into unrelated graph operations.
  Source locations: OAFinder.find(Hub,F), OAThreadLocalService.addSiblingHelper,
  OAThreadLocalService.removeSiblingHelper.
  Known related CODEX findings: none observed.
  Suggested unit tests: testFinderRemovesSiblingHelperAfterTraversalException.
  Spec target section: Finder Runtime / ThreadLocal Lazy Helper Cleanup.

  FIND-LAZY-003 — Use-Only-Loaded Mode Disables Sibling Helper
  Contract statement: When useOnlyLoadedData is true, finder must not install sibling helpers or trigger prefetch
  behavior.
  Rationale: In-memory-only traversal must be strict and side-effect constrained.
  Source locations: OAFinder.find(Hub,F), OAFinder.getUseOnlyLoadedData.
  Known related CODEX findings: none observed.
  Suggested unit tests: testUseOnlyLoadedDataDoesNotInstallSiblingHelper.
  Spec target section: Finder Runtime / Loaded-Only Traversal.

  10. Failure / Silent Wrong-Result Contracts

  FIND-FAIL-001 — Visible Exceptions Are Valid, Silent Wrong Results Are Not
  Contract statement: Invalid paths, failed embedded filter construction, and failing property/filter evaluation may
  throw visible exceptions; they must not be converted into successful empty or partial results unless explicitly
  documented.
  Rationale: Silent false negatives are worse than visible incomplete-operation signaling.
  Source locations: OAFinder._setup, OAFinder._find, OAHierFinder.findFirstValue.
  Known related CODEX findings: setup failure rollback deferred because retry can be silently wrong.
  Suggested unit tests: testInvalidFinderPathThrowsVisibleException, testFailingFilterDoesNotReturnSilentEmptyResult.
  Spec target section: Finder Runtime / Failure Semantics.

  FIND-FAIL-002 — Finder Retry After Failure Must Be Correct Or Fail Consistently
  Contract statement: After a caller-visible failure, retrying the same finder must either behave as a fresh search or
  fail consistently; it must not reuse stale partial traversal/setup state to return wrong results.
  Rationale: OA operations may retry after incomplete operations.
  Source locations: OAFinder.find(F), OAFinder.find(Hub,F), OAFinder.find(List,F), OAFinder.setup.
  Known related CODEX findings: traversal state cleanup fixed; setup rollback still CODEX-commented/deferred.
  Suggested unit tests: testFinderRetryAfterTraversalFailureIsFresh,
  testFinderRetryAfterSetupFailureDoesNotReturnWrongResults.
  Spec target section: Finder Runtime / Retry Semantics.

  FIND-FAIL-003 — Convenience Helper Methods Must Not Have Persistent Side Effects
  Contract statement: Helper methods that temporarily alter finder state must restore all caller-visible state,
  including filters, composition flags, max count, recursive-root defaults, sibling helpers, stack, and cascades.
  Rationale: Helper calls should be query operations, not configuration mutations.
  Source locations: OAFinder.findFirst, OAFinder.canFindFirst, OAFinder.findLargest, OAFinder.findSmallest,
  OAFinder.findDuplicates, OAFinder.find(Hub,F).
  Known related CODEX findings: maxFound restoration fixed; helper filter/composition restoration fixed; recursive-
  root default restoration fixed.
  Suggested unit tests: testFindFirstRestoresMaxFound, testFindLargestRestoresFinderState,
  testHubFindRestoresRecursiveRootDefault.
  Spec target section: Finder Runtime / Helper Side-Effect Semantics.

  11. Test Coverage Matrix

  FIND-RUNTIME-001: testFinderReturnsDeterministicResultsForSameGraph,
  testHierFinderReturnsSameFirstMatchForSameHierarchy
  FIND-RUNTIME-002: testFinderStateClearedAfterSuccessfulSearch, testFinderStateClearedAfterTraversalException,
  testHubFinderRetryAfterFilterExceptionDoesNotReuseCascadeState
  FIND-RUNTIME-003: testFindFirstRestoresMaxFoundAfterException, testCanFindFirstRestoresMaxFoundAfterException
  FIND-RUNTIME-004: testFinderSetupFailureDoesNotMarkInitialized,
  testFinderSetupFailureRollsBackEmbeddedFiltersAndState
  FIND-TRAVERSE-001: testObjectRootFindDoesNotReusePriorHubRecursiveRootDefault
  FIND-TRAVERSE-002: testHubFindTraversesInHubOrder, testHubFindStartsAfterLastUsedObject,
  testHubFindStopsWhenOnFoundCallsStop
  FIND-TRAVERSE-003: testListFindSkipsLeadingNullRoots, testListFindAllNullReturnsEmpty
  FIND-TRAVERSE-004: testFindNullObjectRootReturnsNull, testFindNullHubReturnsEmptyWithLazyMode
  FIND-PATH-001: testFinderRejectsScalarTerminalPath, testFinderAcceptsObjectTerminalPath
  FIND-PATH-002: testFinderTraversesNestedObjectPath, testFinderTraversesHubSegmentWithoutSkippingNextPathSegment
  FIND-HIER-001: testHierFinderReturnsStartingObjectValueFirst, testHierFinderReturnsNearestParentValue
  FIND-HIER-002: testHierFinderExcludeFromObjectSkipsStartingValue
  FIND-RECURSIVE-001: testExplicitRecursiveRootFalseIsHonoredForRecursiveDetailHub
  FIND-RECURSIVE-002: testRecursiveRootDefaultRestoredAfterHubFind
  FIND-CYCLE-001: testFinderBidirectionalLinksDoNotLoop, testFinderRecursiveRootCycleDoesNotLoopForever
  FIND-LAZY-001: testHubFindRegistersSiblingHelperInLazyMode
  FIND-LAZY-002: testFinderRemovesSiblingHelperAfterTraversalException
  FIND-FAIL-001: testInvalidFinderPathThrowsVisibleException, testFailingFilterDoesNotReturnSilentEmptyResult
  FIND-FAIL-002: testFinderRetryAfterTraversalFailureIsFresh,
  testFinderRetryAfterSetupFailureDoesNotReturnWrongResults
  FIND-FAIL-003: testFindLargestRestoresFinderState, testHubFindRestoresRecursiveRootDefault


*/






