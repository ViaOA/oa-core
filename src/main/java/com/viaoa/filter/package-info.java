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
 * Provides a comprehensive set of {@link com.viaoa.filter.OAFilter} subclasses
 * used to evaluate object-level conditions across Hubs, OASelect queries, and
 * the OA Object Graph.  Filters enable declarative, reusable, type-safe
 * selection logic without requiring SQL, reflection-based expression engines,
 * or custom comparator code.
 *
 * <p>
 * OA filters are used in multiple contexts:
 * </p>
 *
 * <ul>
 *   <li><b>Hub filtering</b> – dynamically include/exclude objects in a Hub.</li>
 *   <li><b>Derived Hubs</b> – apply filtering rules to detail collections,
 *       shared Hubs, or linked Hubs.</li>
 *   <li><b>OASelect queries</b> – optionally push filter logic down into the
 *       datasource via {@code updateSelect()}.</li>
 *   <li><b>Finder evaluation</b> – filters attached to an
 *       {@link com.viaoa.util.OAFinder} allow deep filtering across
 *       multi-valued property paths.</li>
 *   <li><b>UI controllers</b> – filter tables, type-ahead lists, and other
 *       interactive components.</li>
 * </ul>
 *
 * <h3>Core filter capabilities</h3>
 *
 * <p>
 * All filters share the following characteristics:
 * </p>
 *
 * <ul>
 *   <li><b>Serializable</b> – filters can be distributed between client and server.</li>
 *   <li><b>Property path aware</b> – supports nested paths via
 *       {@link com.viaoa.object.OAPath}, including many-relationships.</li>
 *   <li><b>Finder-enabled</b> – multi-valued segments automatically generate
 *       {@link com.viaoa.util.OAFinder} instances with embedded filters.</li>
 *   <li><b>Consistent comparison semantics</b> – all relational logic uses
 *       {@link com.viaoa.compare.OACompare} for type-safe evaluation.</li>
 *   <li><b>Composable</b> – filters can be combined through logical AND, OR,
 *       XOR, and block aggregations.</li>
 * </ul>
 *
 * <h3>Operator-style filters</h3>
 *
 * <p>
 * The package contains a wide array of comparison filters:
 * </p>
 *
 * <ul>
 *   <li>Equality / Inequality – {@code OAEqualFilter}, {@code OANotEqualFilter}</li>
 *   <li>Relational – {@code OALessFilter}, {@code OAGreaterFilter}, etc.</li>
 *   <li>Between / BetweenOrEqual – range evaluations</li>
 *   <li>Null / NotNull / Empty / NotEmpty</li>
 *   <li>String pattern matching – {@code OALikeFilter}, {@code OANotLikeFilter},
 *       {@code OAStartsWithFilter}, {@code OAContainsFilter}</li>
 *   <li>Membership – {@code OAInFilter} for arrays, collections, and Hubs</li>
 * </ul>
 *
 * <h3>Composite and logical filters</h3>
 *
 * <ul>
 *   <li>{@code OAAndFilter}</li>
 *   <li>{@code OAOrFilter}</li>
 *   <li>{@code OAXorFilter}</li>
 *   <li>{@code OABlockFilter}</li>
 * </ul>
 *
 * These allow complex multi-condition expressions to be assembled easily.
 *
 * <h3>Expression-based filtering</h3>
 *
 * <p>
 * {@link com.viaoa.filter.OAQueryFilter} provides an OQL/SQL-style expression
 * language that compiles queries such as:
 * </p>
 *
 * <pre>
 *   "lastName LIKE 'S*' AND (age >= 18 OR status = 'VIP')"
 * </pre>
 *
 * <p>
 * The parser converts the expression into a tree of OAFilter objects, enabling
 * powerful declarative filtering directly against the OA Object Graph.
 * </p>
 *
 * <h3>Design philosophy</h3>
 *
 * <p>
 * The filter package is designed for:
 * </p>
 *
 * <ul>
 *   <li><b>Simplicity</b> – each filter performs one role and is easy to test.</li>
 *   <li><b>Reusability</b> – filters can be attached anywhere: Hubs, finders,
 *       selects, or custom logic.</li>
 *   <li><b>Predictability</b> – all comparisons use a unified comparison engine.</li>
 *   <li><b>Performance</b> – no reflection-based evaluation; filters run at
 *       in-memory speeds.</li>
 * </ul>
 *
 * <p>
 * Together, the filters in this package form a comprehensive selection and
 * rule-evaluation framework used throughout OA to shape object graphs, perform
 * searches, enforce constraints, and support dynamic UI behavior.
 * </p>
 */
package com.viaoa.filter;

//CODEX unit tests 20260527

/* CODEX Invariants

FILTER-RUNTIME-001 — Deterministic Predicate Semantics
Contract statement:
For the same input object, resolved property/path values, filter configuration, source Hub active object, and
runtime metadata state, a filter must return the same boolean result.
Rationale:
Filters shape Hub membership, in-memory selects, finder traversal, UI lists, validation-style checks, and object
graph traversal. Non-deterministic predicates create silent false positives or false negatives.
Source scope:
OAFilter, all OA*Filter implementations, OAQueryFilter.isUsed, OAFindNull.
Related CODEX findings:
none.
Suggested unit tests:
testFilterResultIsDeterministicForStableObjectState, testFilterResultChangesOnlyWhenDependentStateChanges,
testQueryFilterResultIsDeterministicForStableInput.
Spec target section:
Filter Runtime / Predicate Semantics

FILTER-INCLUDE-001 — Include/Exclude Result Meaning
Contract statement:
A filter result of true must mean the candidate is included/accepted under that filter’s contract; false must mean
excluded/rejected. Filters must not use false to mean “not evaluated,” “unknown,” or “pushdown attempted.”
Rationale:
Hub filtering, query fallback, finder traversal, and select optimization all depend on a single boolean inclusion
contract.
Source scope:
OAFilter.isUsed, comparison filters, null/empty filters, string filters, OAInFilter, OAEqualPathFilter,
OAQueryFilter, OAFilter.updateSelect.
Related CODEX findings:
updateSelect false-success risks where memory filtering could be suppressed after incomplete datasource pushdown.
Suggested unit tests:
testFalseMeansCandidateRejectedNotUnknown, testUpdateSelectUnsupportedDoesNotSuppressMemoryFilter,
testFilterResultMeaningIsStableAcrossDirectAndWrappedUse.
Spec target section:
Filter Runtime / Inclusion Semantics

FILTER-CONSTANT-001 — Constant Filter Semantics
Contract statement:
No-arg OATrueFilter must always accept every candidate, and no-arg OAFalseFilter must always reject every candidate,
including nulls, scalar values, Boolean values, OAObjects, and Hubs.
Rationale:
Constant filters are used as sentinels for unconditional or impossible filter states. They must not accidentally
behave as equality filters.
Source scope:
OATrueFilter, OAFalseFilter, OAQueryFilter.parseIn, OAQueryFilter.parseCompoundIn.
Related CODEX findings:
Empty IN handling required a true constant-false sentinel; historical no-arg false behavior could act like equality-
to-false.
Suggested unit tests:
testNoArgTrueFilterAlwaysReturnsTrue, testNoArgFalseFilterAlwaysReturnsFalse,
testFalseFilterRejectsBooleanFalseCandidate, testConstantFiltersHandleNullCandidate.
Spec target section:
Filter Runtime / Constant Filters

FILTER-BOOLEAN-001 — Boolean Property Filter Semantics
Contract statement:
Path-aware true/false filters must compare the resolved property/path value to Boolean.TRUE or Boolean.FALSE; they
must not behave as global constants.
Rationale:
OA supports boolean property filtering through direct filters, Hub filters, and finder helpers. These filters must
reflect object state, not ignore it.
Source scope:
OATrueFilter(String/OAPath), OAFalseFilter(String/OAPath), HubFilter true/false helper interaction, OAFinder true/
false helper interaction.
Related CODEX findings:
HubFilter true/false helper regression where path-aware filters were applied at the wrong semantic layer.
Suggested unit tests:
testTrueFilterWithPathUsesResolvedBooleanProperty, testFalseFilterWithPathUsesResolvedBooleanProperty,
testBooleanPathFilterRejectsNullResolvedValueByContract.
Spec target section:
Filter Runtime / Boolean Property Filters

FILTER-NULL-001 — Null Candidate And Null Value Semantics
Contract statement:
Filters must handle null candidate objects and null resolved values deterministically. A filter that cannot evaluate
a path from a null candidate must return false unless its contract is specifically to test for null.
Rationale:
Filtering pipelines can encounter null candidates or null path values during generic predicate use, path traversal,
query fallback, or Hub/finder evaluation. Nulls must not cause accidental NPEs or broad matches.
Source scope:
OANullFilter, OANotNullFilter, OAEmptyFilter, OANotEmptyFilter, OAEqualFilter, OAEqualPathFilter, OAInFilter,
string/comparison filters.
Related CODEX findings:
OAInFilter.isUsed(null) and OAEqualPathFilter.isUsed(null) had null-candidate risk paths.
Suggested unit tests:
testInFilterReturnsFalseForNullCandidate, testEqualPathFilterReturnsFalseForNullCandidate,
testNullFilterAcceptsNullResolvedValue, testNotNullFilterRejectsNullResolvedValue.
Spec target section:
Filter Runtime / Null Semantics

FILTER-EMPTY-001 — Empty Value Semantics
Contract statement:
Empty and not-empty filters must use OA-wide empty semantics and remain consistent with OACompare/OA utility
behavior for nulls, blank strings, arrays, collections, Hubs, and other supported values.
Rationale:
OA uses flexible empty semantics across UI, Hub, object, query, and utility code. Filters must not invent
conflicting definitions.
Source scope:
OAEmptyFilter, OANotEmptyFilter, OACompare.isEmpty, OACompare.isNotEmpty.
Related CODEX findings:
none.
Suggested unit tests:
testEmptyFilterMatchesNullAndEmptyString, testEmptyFilterMatchesEmptyHubAndCollection,
testNotEmptyFilterRejectsEmptyCollection, testNotEmptyFilterAcceptsNonEmptyHub.
Spec target section:
Filter Runtime / Empty Semantics

FILTER-EQUAL-001 — Equality Uses OACompare Semantics
Contract statement:
Equality and not-equality filters must use OACompare equality semantics, including OAObject identity/key behavior,
scalar value equality, string/case options, number precision options, and date/time comparison behavior.
Rationale:
Filter equality must match OA-wide comparison rules used by Hubs, selects, queries, paths, object matching, and
graph traversal.
Source scope:
OAEqualFilter, OANotEqualFilter, OAEqualPathFilter, OACompare.
Related CODEX findings:
OAEqualPathFilter scalar comparison previously risked identity comparison instead of value comparison.
Suggested unit tests:
testEqualFilterUsesOACompare, testNotEqualFilterNegatesOACompare, testEqualPathFilterComparesScalarValuesByValue,
testEqualFilterObjectKeySemanticsMatchOACompare.
Spec target section:
Filter Runtime / Equality Semantics

FILTER-EQUAL-002 — Equality Options Survive Path Decomposition
Contract statement:
When an equality filter is decomposed into finder traversal plus a terminal value filter, all configured comparison
options must be preserved in the terminal filter.
Rationale:
A filter must produce equivalent results whether its path crosses a many-link or resolves directly to a scalar
value.
Source scope:
OAEqualFilter.isUsed, OAFilterDelegate.createFinder, OAFinder terminal filters.
Related CODEX findings:
OAEqualFilter decimal-place comparison option was not propagated to nested finder filter.
Suggested unit tests:
testEqualFilterIgnoreCasePreservedAcrossManyPath, testEqualFilterDecimalPlacesPreservedAcrossManyPath,
testEqualFilterAcrossDirectAndManyPathHasEquivalentTerminalSemantics.
Spec target section:
Filter Runtime / Finder-Decomposed Equality

FILTER-COMPARE-001 — Relational Comparison Semantics
Contract statement:
Greater-than, greater-or-equal, less-than, and less-or-equal filters must delegate ordering decisions to OACompare
and must not treat non-comparable values as ordered matches.
Rationale:
Numeric, date/time, string, and OAObject ordering must remain consistent across filters, query fallback, Hub
sorting, and compare utilities.
Source scope:
OAGreaterFilter, OAGreaterOrEqualFilter, OALessFilter, OALessOrEqualFilter, OACompare.
Related CODEX findings:
none.
Suggested unit tests:
testGreaterFilterUsesOACompare, testLessOrEqualFilterBoundaryMatchesOACompare,
testRelationalFilterRejectsNonComparableValue, testRelationalDateAndNumberComparisonMatchesOACompare.
Spec target section:
Filter Runtime / Comparison Semantics

FILTER-RANGE-001 — Range Boundary Semantics
Contract statement:
OABetweenFilter must use exclusive boundaries, and OABetweenOrEqualFilter must use inclusive boundaries. Boundary
behavior must be unambiguous for all OACompare-supported value types.
Rationale:
Range filters are used for dates, numbers, query fallback, and Hub filtering. Boundary mistakes silently include or
exclude records.
Source scope:
OABetweenFilter, OABetweenOrEqualFilter, OACompare.isBetween, OACompare.isBetweenOrEqual.
Related CODEX findings:
none.
Suggested unit tests:
testBetweenFilterExcludesLowerAndUpperBoundary, testBetweenOrEqualFilterIncludesBoundaries,
testRangeFilterUsesOACompareForDateAndNumberValues.
Spec target section:
Filter Runtime / Range Semantics

FILTER-STRING-001 — LIKE And NOT LIKE Semantics
Contract statement:
OALikeFilter must match OACompare LIKE semantics, and OANotLikeFilter must return the logical negation for the same
resolved value and pattern.
Rationale:
In-memory query fallback and Hub filtering must not diverge from OA query-style wildcard matching.
Source scope:
OALikeFilter, OANotLikeFilter, OACompare.isLike, OAQueryFilter.
Related CODEX findings:
none.
Suggested unit tests:
testLikeFilterMatchesWildcardPattern, testNotLikeFilterNegatesLikeFilter,
testLikeFilterNullValueBehaviorIsDeterministic.
Spec target section:
Filter Runtime / LIKE Semantics

FILTER-STRING-002 — Text Predicate Semantics
Contract statement:
Contains, index-of, and starts-with filters must convert operands through OA string conversion rules and return
false when the target value or pattern cannot be converted under the filter contract.
Rationale:
OA filters support non-string property values in UI and query-like filtering while keeping null and unsupported-type
behavior deterministic.
Source scope:
OAContainsFilter, OAIndexOfFilter, OAStartsWithFilter, OAString/OAStr conversion helpers.
Related CODEX findings:
none.
Suggested unit tests:
testContainsFilterUsesOAStringConversion, testStartsWithFilterReturnsFalseForNullValue,
testIndexOfFilterReturnsFalseForNullPattern, testTextFilterHandlesNonStringPropertyValues.
Spec target section:
Filter Runtime / String Predicate Semantics

FILTER-CASE-001 — Case-Insensitive Determinism
Contract statement:
Case-insensitive filters must produce stable results for the same data independent of JVM default locale, and
equality ignore-case behavior must remain aligned with OACompare.
Rationale:
OA applications can run clients, servers, sync nodes, and replication nodes in different locale environments.
Locale-sensitive case folding can create divergent filter results.
Source scope:
OAContainsFilter, OAIndexOfFilter, OAStartsWithFilter, OAEqualFilter, OANotEqualFilter, OACompare, OAString/OAStr
case helpers.
Related CODEX findings:
Locale-stable case folding noted for OAString/OA text behavior; no direct filter-specific CODEX finding.
Suggested unit tests:
testIgnoreCaseFiltersAreStableUnderTurkishLocale, testEqualFilterIgnoreCaseUsesOACompare,
testNotEqualFilterIgnoreCaseNegatesOACompare.
Spec target section:
Filter Runtime / Case-Insensitive Matching

FILTER-PATH-001 — Property Path Resolution Semantics
Contract statement:
A path-aware filter receiving a root object must resolve its configured path exactly once at the correct semantic
layer. A wrapper that already resolved the path must invoke a value-level filter rather than reapplying the same
path.
Rationale:
Double path application causes false negatives or exceptions; missing path resolution causes false positives or
broad matches.
Source scope:
OAEqualFilter, OANotEqualFilter, OABetweenFilter, OALikeFilter, OANullFilter, OANotNullFilter, OATrueFilter,
OAFalseFilter, HubFilter/OAFinder wrapper interactions.
Related CODEX findings:
HubFilter true/false helpers temporarily double-applied path filters.
Suggested unit tests:
testPathFilterDirectUseResolvesPathFromRootObject, testWrappedFilterReceivesResolvedTerminalValue,
testHubFilterPathWrapperAppliesTerminalValueFilter.
Spec target section:
Filter Runtime / Path Resolution Semantics

FILTER-PATH-002 — Many-Link Existential Semantics
Contract statement:
If a property path crosses a many-link, the filter must return true when at least one reachable terminal object or
value satisfies the terminal filter, unless a different contract is explicitly documented.
Rationale:
OA path filtering over Hubs and object graphs generally means “any reachable child matches.” Applying the condition
to the wrong object or treating existence alone as success produces wrong Hub/query contents.
Source scope:
OAFilterDelegate.createFinder, OAFinder integration, OAEqualFilter, OALikeFilter, OABetweenFilter, OANullFilter,
OANotNullFilter, OAInFilter.
Related CODEX findings:
Finder-backed filters reviewed for existential behavior.
Suggested unit tests:
testEqualFilterAcrossManyPathMatchesAnyChild, testLikeFilterAcrossManyPathRejectsWhenNoChildMatches,
testBetweenFilterAcrossManyPathMatchesAnyChildInRange.
Spec target section:
Filter Runtime / Many-Link Path Semantics

FILTER-PATH-003 — Dynamic Source Path State
Contract statement:
Filters whose comparison source depends on a Hub active object, master/detail context, or source OAObject path must
refresh derived comparison state when that source changes, or explicitly document caller-managed refresh.
Rationale:
Cached source path values can produce stale filter results after active-object or master/detail changes.
Source scope:
OAEqualPathFilter, OAInFilter, Hub-based constructors, source-object constructors.
Related CODEX findings:
Existing package notes refresh gaps when parent/master object changes.
Suggested unit tests:
testEqualPathFilterRefreshesWhenHubAOChanges, testInFilterRefreshesWhenHubAOChanges,
testSourceObjectPathChangeAffectsFilterResult.
Spec target section:
Filter Runtime / Dynamic Path Source Semantics

FILTER-COMPOSE-001 — Boolean Composition Semantics
Contract statement:
AND, OR, XOR, and block filters must preserve documented boolean algebra for configured delegate filters, including
deterministic behavior for null or absent delegate filters.
Rationale:
Query parsing and dynamic filter construction use composition filters to represent complex logic. Null delegate
ambiguity can turn impossible conditions into no-ops or broad matches.
Source scope:
OAAndFilter, OAOrFilter, OAXorFilter, OABlockFilter, OAQueryFilter.
Related CODEX findings:
Empty IN previously pushed null/no-op filters; compound empty IN needed explicit false sentinel behavior.
Suggested unit tests:
testAndFilterTruthTable, testOrFilterTruthTable, testXorFilterTruthTable, testBlockFilterRequiresAllFilters,
testNullDelegateBehaviorIsDocumented.
Spec target section:
Filter Runtime / Composition Semantics

FILTER-QUERY-001 — Query Filter Logical Semantics
Contract statement:
OAQueryFilter must evaluate in-memory query expressions according to the same logical semantics expected by OA
query/datasource behavior, including operator precedence, grouping, literal meaning, and comparison semantics.
Rationale:
Object-cache filtering and datasource-backed selection must not return different result sets for the same query
expression.
Source scope:
OAQueryFilter parser/evaluator, OAQueryTokenizer, OAAndFilter, OAOrFilter, OABlockFilter, comparison filters created
by query parsing.
Related CODEX findings:
Existing CODEX notes AND/OR precedence mismatch with SQL-like semantics and TRUE/FALSE literal mismatch with JDBC
conversion semantics.
Suggested unit tests:
testQueryFilterAndOrPrecedenceMatchesSpec, testQueryFilterParenthesesOverridePrecedence,
testQueryFilterBooleanLiteralMatchesDatasourceSemantics, testQueryFilterNullLiteralMatchesDatasourceSemantics.
Spec target section:
Filter Runtime / Query Semantics

FILTER-QUERY-002 — Query Parse Completeness
Contract statement:
A query filter must either consume the complete expression or reject it as invalid. It must not silently ignore
trailing tokens, dangling operators, or unparsed subexpressions.
Rationale:
Accepting partial queries creates silent false positives or false negatives and can broaden or narrow Hub/select
contents without a visible failure.
Source scope:
OAQueryFilter constructors, parse, parseBlock, parser chain methods, OAQueryTokenizer integration.
Related CODEX findings:
OAQueryFilter CODEX comment notes trailing valid-expression tokens can be ignored.
Suggested unit tests:
testQueryFilterRejectsTrailingTokensAfterValidExpression, testQueryFilterRejectsDanglingOperator,
testQueryFilterRejectsUnclosedGroup, testQueryFilterConsumesWholeTokenStream.
Spec target section:
Filter Runtime / Query Parser Completeness

FILTER-QUERY-003 — Query Parameter Binding Semantics
Contract statement:
Query placeholders must bind exactly to provided arguments in order. Missing, extra, or unsupported argument values
must fail construction or be explicitly defined by contract.
Rationale:
In-memory filters must not silently treat missing placeholders as literals, ignore extra values, or bind the wrong
value to a comparison.
Source scope:
OAQueryFilter constructors, getValueToUse, parseIn, parseCompoundIn.
Related CODEX findings:
Existing CODEX notes missing ? args can silently mis-filter.
Suggested unit tests:
testQueryFilterRejectsMissingParameter, testQueryFilterRejectsExtraParameter, testQueryFilterBindsParametersInOrder,
testQueryFilterInParameterUsesProvidedCollection.
Spec target section:
Filter Runtime / Query Parameters

FILTER-IN-001 — Membership Semantics
Contract statement:
IN and OAInFilter membership checks must use deterministic OA identity/value membership semantics for arrays,
collections, Hubs, object keys, and path-derived values. Empty membership sets must evaluate false.
Rationale:
Empty-set membership is mathematically false, and membership is central to query fallback, Hub filtering, object-key
matching, and graph traversal. Treating empty IN as no filter silently broadens results.
Source scope:
OAInFilter, OAQueryFilter.parseIn, OAQueryFilter.parseCompoundIn, OAFalseFilter sentinel use.
Related CODEX findings:
Simple and compound empty IN previously pushed null/no-op filters.
Suggested unit tests:
testInFilterMatchesCollectionMember, testInFilterMatchesHubMemberByIdentityContract,
testQueryFilterSimpleInEmptyListMatchesNothing, testQueryFilterCompoundInEmptyListMatchesNothing.
Spec target section:
Filter Runtime / Membership Semantics

FILTER-SELECT-001 — Select Pushdown Safety
Contract statement:
updateSelect may suppress in-memory filtering only when the full filter semantics were successfully represented in
the OASelect/datasource query. If pushdown is unsupported, incomplete, or uncertain, updateSelect must preserve
caller-side filtering.
Rationale:
A false updateSelect result can suppress post-select filtering. Returning false before full pushdown silently
broadens or narrows results.
Source scope:
OAFilter.updateSelect, OAAndFilter.updateSelect, OAEqualPathFilter.updateSelect, OAInFilter.updateSelect, OASelect
integration.
Related CODEX findings:
OAEqualPathFilter.updateSelect previously returned false even when scalar source value or missing reverse path
prevented complete pushdown.
Suggested unit tests:
testUpdateSelectReturnsTrueWhenOptimizationUnsupported,
testEqualPathUpdateSelectDoesNotSuppressMemoryFilterForScalarValue,
testInFilterUpdateSelectPreservesMemoryFilterWhenPushdownIncomplete.
Spec target section:
Filter Runtime / Select Optimization Semantics

FILTER-FAIL-001 — Invalid Filter Failure Visibility
Contract statement:
Invalid query expressions, invalid paths, invalid filter arguments, unsupported membership sources, and malformed
filter structures must fail visibly or return deterministic rejection; they must not silently match all, match none,
or partially evaluate as success unless explicitly documented.
Rationale:
Silent wrong-result behavior is worse than construction failure because it corrupts Hub/select/filter contents
without a clear signal.
Source scope:
OAQueryFilter, OAInFilter, OAEqualPathFilter, OAPath construction, comparison/path filters, constructor and setup
methods.
Related CODEX findings:
Trailing-token parser issue, missing parameter issue, invalid IN/no-op risks, updateSelect false-success risks.
Suggested unit tests:
testInvalidQueryThrowsAtConstruction, testInvalidInArgumentTypeThrowsOrRejectsDeterministically,
testInvalidPathFilterDoesNotSilentlyMatchAll, testMalformedFilterDoesNotPartiallySucceed.
Spec target section:
Filter Runtime / Failure Semantics

FILTER-STATE-001 — Filter State Isolation
Contract statement:
Filter instances must not leak stale derived state across candidates, retries, source object changes, or wrapper
contexts unless the state is explicitly part of the filter’s documented live source contract.
Rationale:
Filters are reusable runtime objects. Stale cached path values, source values, or delegate filters can create hidden
false positives/false negatives.
Source scope:
OAEqualPathFilter.setup/isUsed, OAInFilter.setup/isUsed, OAFilterDelegate.createFinder, OAQueryFilter parsed filter
tree, path-aware filters.
Related CODEX findings:
Dynamic source path refresh gaps noted for OAEqualPathFilter and OAInFilter.
Suggested unit tests:
testReusableFilterDoesNotLeakCandidateState, testQueryFilterDoesNotLeakPreviousEvaluationState,
testPathSourceFilterRefreshesDerivedStateWhenSourceChanges.
Spec target section:
Filter Runtime / State Isolation

FILTER-SIDE-001 — Predicate Evaluation Side-Effect Boundaries
Contract statement:
Calling isUsed must evaluate predicate state without mutating candidate object state, Hub membership, graph
relationships, or datasource state. Any lazy path loading or source refresh behavior must be explicit and compatible
with OA load/runtime contracts.
Rationale:
Filters are commonly invoked during iteration, Hub membership updates, query fallback, and UI refresh. Hidden
mutation during predicate evaluation can corrupt traversal or ordering.
Source scope:
All OAFilter.isUsed implementations, OAFilterDelegate/OAFinder integration, path-aware filters.
Related CODEX findings:
none.
Suggested unit tests:
testFilterEvaluationDoesNotMutateCandidateObject, testFilterEvaluationDoesNotChangeHubMembership,
testPathFilterLazyLoadBehaviorIsDocumented.
Spec target section:
Filter Runtime / Side-Effect Boundaries

FILTER-CONCURRENT-001 — Concurrent Evaluation Assumptions
Contract statement:
A filter must either be safe for concurrent read-only evaluation after construction or document that caller
synchronization is required when filter source state, parsed query state, or Hub-dependent state can change.
Rationale:
Filters can be reused by Hubs, selects, UI controllers, background tasks, and runtime services. Shared mutable
filter state can produce stale or inconsistent membership results.
Source scope:
OAQueryFilter parsed filter tree, OAEqualPathFilter, OAInFilter, OAFilterDelegate-created finders, all reusable
filter instances.
Related CODEX findings:
Dynamic source state refresh concerns imply mutable filter state that must be scoped or synchronized by contract.
Suggested unit tests:
testConcurrentReadOnlyFilterEvaluationIsStable, testConcurrentQueryFilterEvaluationDoesNotCorruptParsedState,
testHubDependentFilterRequiresDocumentedSynchronization.
Spec target section:
Filter Runtime / Concurrency Semantics

FILTER-UTILITY-001 — Utility Search Result Semantics
Contract statement:
Utility-style filters and searches must aggregate nested results correctly and return true only when the documented
condition is actually found.
Rationale:
Diagnostic, validation, and traversal utilities are often used to detect missing values or object graph problems.
Incorrect boolean aggregation hides or invents matches.
Source scope:
OAFindNull.findNull, OAFindNull.foundOne, array/field traversal logic.
Related CODEX findings:
OAFindNull array branch previously returned true for arrays with no null elements.
Suggested unit tests:
testFindNullReturnsFalseForArrayWithoutNulls, testFindNullReturnsTrueForArrayWithNullElement,
testFindNullReportsPropertyPathForFoundNull.
Spec target section:
Filter Runtime / Utility Search Semantics

FILTER-INTEGRATION-001 — Cross-Package Filter Compatibility
Contract statement:
Filter behavior must remain compatible with OACompare, OAPath, OAFinder, Hub filtering, OASelect/datasource
pushdown, query parsing, object/cache identity, and graph runtime semantics.
Rationale:
Filters are a shared selection layer across OA runtime packages. Divergence at package boundaries creates mismatched
Hub contents, datasource result sets, and object graph traversal behavior.
Source scope:
All com.viaoa.filter classes, OACompare integration, OAPath/OAFinder integration, HubFilter/OASelect integration,
OAQueryFilter.
Related CODEX findings:
Query/datasource literal mismatch, path wrapper mismatch, updateSelect pushdown false-success, equality option
propagation across finder decomposition.
Suggested unit tests:
testFilterAndOACompareAgreeForCoreTypes, testPathFilterAndOAFinderAgreeForManyLinkTraversal,
testQueryFilterAndDatasourceLiteralSemanticsAgree, testHubFilterAndDirectFilterAgreeForSamePath.
Spec target section:
Filter Runtime / Cross-Package Contracts

*/



