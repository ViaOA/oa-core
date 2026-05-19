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

/* CODEX Invariants

1. Filter Runtime Contracts

  FILTER-RUNTIME-001 — Filters Are Deterministic Predicates
  Contract statement: For the same input object state, a filter must return the same boolean result unless its
  configured source state, path source, Hub AO, or dependent object graph changes.
  Rationale: Filters drive Hub membership, in-memory selects, query fallback, UI views, and object graph traversal.
  Non-deterministic predicates create silent false positives/false negatives.
  Source locations: OAFilter, all OA*Filter implementations.
  Known related CODEX findings: none observed.
  Suggested unit tests: testFilterIsDeterministicForStableInput, testFilterResultChangesOnlyWhenSourceStateChanges
  Spec target section: Filter Runtime / Predicate Semantics

  FILTER-RUNTIME-002 — Filters Must Not Silently Change Their Contract Based On Wrapper Context
  Contract statement: A filter applied directly to a root object and a filter applied through a property wrapper must
  preserve the intended semantic level: root-object filters operate on root objects; value filters operate on already-
  resolved terminal values.
  Rationale: HubFilter/OAFinder wrappers resolve paths before invoking nested filters. Double-applying paths or using
  root-level filters as value filters causes silent wrong results.
  Source locations: OAEqualFilter, OATrueFilter, OAFalseFilter, HubFilter._addFilter, OAFinder.add*Filter.
  Known related CODEX findings: HubFilter true/false helper regression where path-aware true/false filters were
  applied after _addFilter already resolved the property.
  Suggested unit tests: testWrappedFilterReceivesResolvedTerminalValue,
  testPathAwareFilterReceivesRootObjectWhenUsedDirectly
  Spec target section: Filter Runtime / Wrapper Semantics

  2. Boolean Result Contracts

  FILTER-BOOLEAN-001 — Constant Filters Are True Constants
  Contract statement: No-arg OATrueFilter must always return true; no-arg OAFalseFilter must always return false,
  regardless of input object, including null, Boolean values, OAObjects, Hubs, or scalar values.
  Rationale: Constant filters are used as sentinels for impossible or unconditional query/filter states. They must not
  accidentally behave as equality filters.
  Source locations: OATrueFilter, OAFalseFilter, OAQueryFilter.parseIn, OAQueryFilter.parseCompoundIn.
  Known related CODEX findings: Empty IN (?) needed a true constant-false sentinel; old no-arg OAFalseFilter behaved
  as equality-to-false.
  Suggested unit tests: testNoArgTrueFilterAlwaysReturnsTrue, testNoArgFalseFilterAlwaysReturnsFalse,
  testFalseFilterRejectsBooleanFalseCandidate
  Spec target section: Filter Runtime / Constant Filters

  FILTER-BOOLEAN-002 — Path Boolean Filters Compare Resolved Values
  Contract statement: Path-aware true/false filters must compare the resolved property/path value to Boolean.TRUE or
  Boolean.FALSE; they must not be treated as global constants.
  Rationale: OA supports boolean property filtering through HubFilter and OAFinder helpers. These must reflect
  property values, not ignore them.
  Source locations: OATrueFilter(String/OAPath), OAFalseFilter(String/OAPath), HubFilter.addTrueFilter,
  HubFilter.addFalseFilter, OAFinder.addTrueFilter, OAFinder.addFalseFilter.
  Known related CODEX findings: HubFilter helper temporarily used constant filters and ignored resolved boolean
  property values.
  Suggested unit tests: testTrueFilterWithPathUsesResolvedProperty, testFalseFilterWithPathUsesResolvedProperty
  Spec target section: Filter Runtime / Boolean Property Filters

  3. Null / Empty Handling Contracts

  FILTER-NULL-001 — Null Candidate Handling Is Explicit
  Contract statement: Filters must have deterministic behavior for null candidate objects. If the filter cannot
  evaluate a path from a null candidate, it must return false unless the filter explicitly checks for null.
  Rationale: Filtering pipelines can encounter null candidates during edge cases, intermediate path traversal, or
  generic predicate use. Null must not produce accidental NPEs.
  Source locations: OANullFilter, OANotNullFilter, OAEmptyFilter, OANotEmptyFilter, OAEqualPathFilter, OAInFilter.
  Known related CODEX findings: OAInFilter.isUsed(null) and OAEqualPathFilter.isUsed(null) previously had null
  candidate NPE paths.
  Suggested unit tests: testInFilterReturnsFalseForNullCandidate, testEqualPathFilterReturnsFalseForNullCandidate,
  testNullFilterAcceptsNullCandidate
  Spec target section: Filter Runtime / Null Semantics

  FILTER-EMPTY-001 — Empty And Not-Empty Use OACompare Semantics
  Contract statement: Empty/not-empty filters must delegate to OA-wide empty semantics and must be consistent with
  OACompare.isEmpty(..., true).
  Rationale: OA uses flexible empty semantics across strings, Hubs, arrays, collections, and null-like values. Filters
  must not invent conflicting definitions.
  Source locations: OAEmptyFilter, OANotEmptyFilter, OACompare.isEmpty.
  Known related CODEX findings: none observed.
  Suggested unit tests: testEmptyFilterMatchesNullAndEmptyString, testEmptyFilterMatchesEmptyHub,
  testNotEmptyFilterRejectsEmptyCollection
  Spec target section: Filter Runtime / Empty Semantics

  4. Equality / Comparison Contracts

  FILTER-EQUAL-001 — Equality Filters Use OACompare Semantics
  Contract statement: Equality and not-equality filters must use OACompare equality semantics, including OA-specific
  object/key/string/number/date handling.
  Rationale: Filters must match OA-wide compare behavior used by selects, paths, Hubs, and object graph logic.
  Source locations: OAEqualFilter, OANotEqualFilter, OAEqualPathFilter, OACompare.
  Known related CODEX findings: OAEqualPathFilter previously used == for scalar resolved values, causing false
  negatives.
  Suggested unit tests: testEqualFilterUsesOACompare, testNotEqualFilterNegatesOACompare,
  testEqualPathFilterComparesScalarValuesByValue
  Spec target section: Filter Runtime / Equality Semantics

  FILTER-EQUAL-002 — Equality Options Survive Finder Decomposition
  Contract statement: When a path filter is split into finder traversal plus a nested terminal filter, all configured
  comparison options must be copied to the nested filter.
  Rationale: A filter must return the same result whether the property path crosses a many-link or not.
  Source locations: OAEqualFilter.isUsed, OAFilterDelegate.createFinder.
  Known related CODEX findings: OAEqualFilter decimal-place comparison was not propagated to the nested finder filter.
  Suggested unit tests: testEqualFilterIgnoreCasePreservedAcrossManyPath,
  testEqualFilterDecimalPlacesPreservedAcrossManyPath
  Spec target section: Filter Runtime / Finder-Decomposed Equality

  FILTER-COMPARE-001 — Greater/Less Filters Use OACompare Ordering
  Contract statement: Greater, less, greater-or-equal, and less-or-equal filters must delegate ordering decisions to
  OACompare.
  Rationale: Numeric, date/time, string, and OAObject comparison behavior must remain consistent across filter and
  query-like APIs.
  Source locations: OAGreaterFilter, OAGreaterOrEqualFilter, OALessFilter, OALessOrEqualFilter, OACompare.
  Known related CODEX findings: none observed.
  Suggested unit tests: testGreaterFilterUsesOACompare, testLessOrEqualFilterBoundaryMatchesOACompare
  Spec target section: Filter Runtime / Comparison Semantics

  5. Range / Between Contracts

  FILTER-RANGE-001 — Between Boundary Semantics Are Explicit
  Contract statement: OABetweenFilter must be exclusive; OABetweenOrEqualFilter must be inclusive. Boundary matches
  must not be ambiguous.
  Rationale: Range filters are used for dates, numbers, and query fallback. Boundary mistakes cause silent inclusion/
  exclusion bugs.
  Source locations: OABetweenFilter, OABetweenOrEqualFilter, OACompare.isBetween, OACompare.isBetweenOrEqual.
  Known related CODEX findings: none observed.
  Suggested unit tests: testBetweenFilterExcludesLowerAndUpperBoundary, testBetweenOrEqualFilterIncludesBoundaries
  Spec target section: Filter Runtime / Range Semantics

  FILTER-RANGE-002 — Range Filters Preserve Path/Finder Semantics
  Contract statement: A range filter on a path crossing a many-link must evaluate whether any reachable terminal value
  satisfies the range condition.
  Rationale: Path filters over Hubs represent existential graph predicates. Returning existence without applying the
  terminal range condition is wrong; applying the condition to the wrong object is wrong.
  Source locations: OABetweenFilter, OABetweenOrEqualFilter, OAFilterDelegate.createFinder, OAFinder.
  Known related CODEX findings: finder-backed filters were reviewed for existential behavior; current design is
  acceptable.
  Suggested unit tests: testBetweenFilterAcrossManyPathMatchesAnyChildInRange,
  testBetweenFilterAcrossManyPathRejectsWhenNoChildInRange
  Spec target section: Filter Runtime / Path Range Semantics

  6. String Matching Contracts

  FILTER-STRING-001 — LIKE And NOT LIKE Are Logical Opposites
  Contract statement: OALikeFilter must match OACompare LIKE semantics; OANotLikeFilter must return the logical
  negation for the same resolved value and pattern.
  Rationale: Query fallback and in-memory Hub filtering must not diverge for wildcard matching.
  Source locations: OALikeFilter, OANotLikeFilter, OACompare.isLike, OAQueryFilter.
  Known related CODEX findings: none observed.
  Suggested unit tests: testLikeFilterMatchesWildcardPattern, testNotLikeFilterNegatesLikeFilter
  Spec target section: Filter Runtime / LIKE Semantics

  FILTER-STRING-002 — Contains / IndexOf / StartsWith Operate On OA String Conversion
  Contract statement: Contains, index-of, and starts-with filters must convert both operands through OA string
  conversion and return false if either converted value is null.
  Rationale: OA filters support non-string property values in UI/query-like filtering while keeping null behavior
  deterministic.
  Source locations: OAContainsFilter, OAIndexOfFilter, OAStartsWithFilter, OAString.toString.
  Known related CODEX findings: none observed.
  Suggested unit tests: testContainsFilterUsesOAStringConversion, testStartsWithFilterReturnsFalseForNullValue,
  testIndexOfFilterReturnsFalseForNullPattern
  Spec target section: Filter Runtime / String Predicate Semantics

  7. Case-Insensitive / Locale Stability Contracts

  FILTER-CASE-001 — Ignore-Case Matching Must Be Deterministic
  Contract statement: Case-insensitive filters must produce the same result for the same data across JVM default
  locales.
  Rationale: OA applications can run client/server/sync/replication components in different runtime environments.
  Locale-sensitive case conversion can create divergent filter results.
  Source locations: OAContainsFilter, OAIndexOfFilter, OAStartsWithFilter, OAEqualFilter, OANotEqualFilter, OAString
  case helpers.
  Known related CODEX findings: Locale-stable case folding noted in OAString for 4.0; direct filter issue deferred
  unless separately fixed.
  Suggested unit tests: testIgnoreCaseFiltersAreStableUnderTurkishLocale, testOAStringCaseFoldUsesLocaleStableRules
  Spec target section: Filter Runtime / Case-Insensitive Matching

  FILTER-CASE-002 — Equality Ignore-Case Uses OACompare Semantics
  Contract statement: Case-insensitive equality and not-equality filters must delegate to OACompare’s ignore-case
  comparison path.
  Rationale: Equality behavior must remain consistent across direct filters, query filters, and OACompare callers.
  Source locations: OAEqualFilter, OANotEqualFilter, OACompare.isEqual.
  Known related CODEX findings: none observed.
  Suggested unit tests: testEqualFilterIgnoreCaseUsesOACompare, testNotEqualFilterIgnoreCaseNegatesOACompare
  Spec target section: Filter Runtime / Case-Insensitive Equality

  8. Path-Based Filter Contracts

  FILTER-PATH-001 — Path Filters Resolve Terminal Values Exactly Once Per Semantic Layer
  Contract statement: A path-aware filter receiving a root object must resolve its configured path. A wrapper that
  already resolved the path must pass a value-level filter, not another root/path filter for the same path.
  Rationale: Double path application causes false negatives or exceptions; failing to resolve a path causes false
  positives.
  Source locations: OAEqualFilter, OATrueFilter, OAFalseFilter, HubFilter._addFilter, OAFinder.
  Known related CODEX findings: HubFilter true/false helpers temporarily double-applied path filters.
  Suggested unit tests: testHubFilterPathWrapperAppliesTerminalValueFilter,
  testPathFilterDirectUseResolvesPathFromRootObject
  Spec target section: Filter Runtime / Path Resolution Semantics

  FILTER-PATH-002 — Many-Link Path Filters Are Existential Predicates
  Contract statement: If a path crosses a many-link, the filter must return true when at least one reachable target
  object satisfies the terminal filter.
  Rationale: OA path filtering over Hubs means “any child matches” unless a different contract is explicitly
  documented.
  Source locations: OAFilterDelegate.createFinder, OAEqualFilter, OALikeFilter, OABetweenFilter, OANullFilter,
  OANotNullFilter, OAFinder.
  Known related CODEX findings: finder-backed filters were reviewed for correct existential behavior.
  Suggested unit tests: testEqualFilterAcrossManyPathMatchesAnyChild,
  testLikeFilterAcrossManyPathRejectsWhenNoChildMatches
  Spec target section: Filter Runtime / Many-Link Path Semantics

  FILTER-PATH-003 — Path Source Changes Must Be Reflected Or Explicitly Deferred
  Contract statement: Filters whose source object depends on Hub AO or master/detail context must refresh derived
  state when the source object changes, or clearly document that refresh is caller-managed/deferred.
  Rationale: Cached source path values can cause stale filter results after AO/master changes.
  Source locations: OAEqualPathFilter, OAInFilter.
  Known related CODEX findings: existing comments note refresh gaps when parent/master object changes.
  Suggested unit tests: testEqualPathFilterRefreshesWhenHubAOChanges, testInFilterRefreshesWhenHubAOChanges
  Spec target section: Filter Runtime / Dynamic Path Source Semantics

  9. Filter Composition Contracts

  FILTER-COMPOSE-001 — AND / OR / XOR Follow Documented Truth Tables
  Contract statement: Composition filters must preserve boolean algebra for non-null delegate filters. Null delegate
  behavior must be documented and consistent.
  Rationale: Query parsing and dynamic filter construction depend on predictable boolean composition.
  Source locations: OAAndFilter, OAOrFilter, OAXorFilter, OABlockFilter.
  Known related CODEX findings: Empty IN previously pushed null filters; null delegate behavior could turn impossible
  conditions into no-ops.
  Suggested unit tests: testAndFilterTruthTable, testOrFilterTruthTable, testXorFilterTruthTable,
  testNullDelegateBehaviorIsDocumented
  Spec target section: Filter Runtime / Composition Semantics

  FILTER-COMPOSE-002 — Block Filters Are Logical AND Blocks
  Contract statement: OABlockFilter must accept an object only when all contained filters accept it; an absent filter
  array represents no filtering.
  Rationale: Compound IN and grouped filter operations use block filters to represent tuple conjunctions.
  Source locations: OABlockFilter, OAQueryFilter.parseCompoundIn.
  Known related CODEX findings: compound IN (?) empty list needed explicit false sentinel instead of null.
  Suggested unit tests: testBlockFilterRequiresAllFilters, testBlockFilterWithNullArrayActsAsNoFilter
  Spec target section: Filter Runtime / Block Composition

  10. Query Filter Contracts

  FILTER-QUERY-001 — Query Filters Must Preserve Query Truth Tables
  Contract statement: OAQueryFilter must evaluate in-memory query expressions according to the same logical semantics
  expected by OA query/datasource behavior.
  Rationale: Object-cache filtering and datasource selection must not return different result sets for the same query.
  Source locations: OAQueryFilter, OAAndFilter, OAOrFilter, OABlockFilter.
  Known related CODEX findings: Existing CODEX notes AND/OR precedence mismatch with SQL-like semantics.
  Suggested unit tests: testQueryFilterAndOrPrecedenceMatchesSpec, testQueryFilterParenthesesOverridePrecedence
  Spec target section: Filter Runtime / Query Truth Tables

  FILTER-QUERY-002 — Query Parse Must Consume The Whole Expression
  Contract statement: A query filter must either consume all tokens or throw an invalid-query exception. It must not
  silently ignore trailing tokens.
  Rationale: Accepting partial queries creates silent false positives/false negatives.
  Source locations: OAQueryFilter.parse, OAQueryFilter.parseBlock, parser chain methods.
  Known related CODEX findings: trailing valid-expression tokens noted/deferred in comments.
  Suggested unit tests: testQueryFilterRejectsTrailingTokensAfterValidExpression,
  testQueryFilterRejectsDanglingOperator
  Spec target section: Filter Runtime / Query Parser Completeness

  FILTER-QUERY-003 — Parameter Counts Must Be Exact
  Contract statement: Query placeholders must map exactly to provided arguments. Missing or extra arguments must fail
  construction or be explicitly defined.
  Rationale: In-memory filters must not silently treat missing placeholders as literal "?" or ignore extra values.
  Source locations: OAQueryFilter.getValueToUse, OAQueryFilter constructor.
  Known related CODEX findings: Existing CODEX notes missing ? args can silently mis-filter.
  Suggested unit tests: testQueryFilterRejectsMissingParameter, testQueryFilterRejectsExtraParameter
  Spec target section: Filter Runtime / Query Parameters

  FILTER-QUERY-004 — Empty IN Is False
  Contract statement: IN with an empty list must evaluate false, including simple and compound/object-key IN forms.
  Rationale: Empty set membership is mathematically false. Treating it as no filter silently broadens results.
  Source locations: OAQueryFilter.parseIn, OAQueryFilter.parseCompoundIn, OAFalseFilter.
  Known related CODEX findings: simple and compound empty IN (?) previously pushed null/no-op filters.
  Suggested unit tests: testQueryFilterSimpleInEmptyListMatchesNothing,
  testQueryFilterCompoundInEmptyListMatchesNothing
  Spec target section: Filter Runtime / Query IN Semantics

  FILTER-QUERY-005 — Query Literal Semantics Must Match Datasource Semantics
  Contract statement: Boolean, null, string, numeric, and key literals in in-memory query filters must match
  datasource query conversion semantics.
  Rationale: OASelect/object-cache fallback must not diverge from DB-backed select behavior.
  Source locations: OAQueryFilter, OAQueryTokenizer, datasource query conversion.
  Known related CODEX findings: Existing CODEX notes TRUE/FALSE literal mismatch between JDBC conversion and
  OAQueryFilter.
  Suggested unit tests: testQueryFilterBooleanLiteralMatchesDatasourceSemantics,
  testQueryFilterNullLiteralMatchesDatasourceSemantics
  Spec target section: Filter Runtime / Query Literal Semantics

  11. Exception / Failure Contracts

  FILTER-FAILURE-001 — Invalid Filters Fail Loudly During Construction Or Setup
  Contract statement: Invalid property paths, malformed query expressions, invalid IN parameter types, and unsupported
  query structures must fail visibly rather than returning broad or narrow silent results.
  Rationale: Silent wrong-result behavior is worse than construction failure for filtering because it corrupts Hub/
  select contents without a clear signal.
  Source locations: OAQueryFilter, OAInFilter, OAEqualPathFilter, OAPath construction.
  Known related CODEX findings: trailing-token parser issue; missing parameter issue.
  Suggested unit tests: testInvalidQueryThrowsAtConstruction, testInvalidInArgumentTypeThrows,
  testInvalidPathFilterDoesNotSilentlyMatchAll
  Spec target section: Filter Runtime / Failure Semantics

  FILTER-FAILURE-002 — Select Optimization Must Preserve In-Memory Filtering Unless Fully Pushed Down
  Contract statement: updateSelect() may return false only when the select/datasource has fully absorbed the filter
  semantics. If optimization is unsupported, partial, or unsafe, it must return true so in-memory filtering remains
  active.
  Rationale: A false return suppresses post-select filtering. Returning false without full pushdown silently broadens
  results.
  Source locations: OAFilter.updateSelect, OAAndFilter.updateSelect, OAEqualPathFilter.updateSelect,
  OAInFilter.updateSelect.
  Known related CODEX findings: OAEqualPathFilter.updateSelect previously returned false even when scalar source value
  or missing reverse path prevented pushdown.
  Suggested unit tests: testUpdateSelectReturnsTrueWhenOptimizationUnsupported,
  testEqualPathUpdateSelectDoesNotSuppressMemoryFilterForScalarValue
  Spec target section: Filter Runtime / Select Optimization Semantics

  FILTER-FAILURE-003 — Utility Search Return Values Must Reflect Actual Matches
  Contract statement: Recursive utility filters/search helpers must return true only when a match was actually found.
  Traversal over arrays/fields must aggregate child results correctly.
  Rationale: Utilities such as null finders are often used for diagnostics and validation. Incorrect return values
  hide or invent matches.
  Source locations: OAFindNull.
  Known related CODEX findings: OAFindNull array branch previously returned true for arrays with no null elements.
  Suggested unit tests: testFindNullReturnsFalseForArrayWithoutNulls, testFindNullReturnsTrueForArrayWithNullElement
  Spec target section: Filter Runtime / Utility Search Semantics

  12. Test Coverage Matrix

  FILTER-RUNTIME-001
  Tests: testFilterIsDeterministicForStableInput, testFilterResultChangesOnlyWhenSourceStateChanges

  FILTER-BOOLEAN-001
  Tests: testNoArgTrueFilterAlwaysReturnsTrue, testNoArgFalseFilterAlwaysReturnsFalse

  FILTER-BOOLEAN-002
  Tests: testTrueFilterWithPathUsesResolvedProperty, testFalseFilterWithPathUsesResolvedProperty

  FILTER-NULL-001
  Tests: testInFilterReturnsFalseForNullCandidate, testEqualPathFilterReturnsFalseForNullCandidate,
  testNullFilterAcceptsNullCandidate

  FILTER-EMPTY-001
  Tests: testEmptyFilterMatchesNullAndEmptyString, testNotEmptyFilterRejectsEmptyHub

  FILTER-EQUAL-001
  Tests: testEqualFilterUsesOACompare, testEqualPathFilterComparesScalarValuesByValue

  FILTER-EQUAL-002
  Tests: testEqualFilterDecimalPlacesPreservedAcrossManyPath, testEqualFilterIgnoreCasePreservedAcrossManyPath

  FILTER-COMPARE-001
  Tests: testGreaterFilterUsesOACompare, testLessOrEqualFilterBoundaryMatchesOACompare

  FILTER-RANGE-001
  Tests: testBetweenFilterExcludesBoundaries, testBetweenOrEqualFilterIncludesBoundaries

  FILTER-RANGE-002
  Tests: testBetweenFilterAcrossManyPathMatchesAnyChildInRange

  FILTER-STRING-001
  Tests: testLikeFilterMatchesWildcardPattern, testNotLikeFilterNegatesLikeFilter

  FILTER-STRING-002
  Tests: testContainsFilterUsesOAStringConversion, testStartsWithFilterReturnsFalseForNullValue

  FILTER-CASE-001
  Tests: testIgnoreCaseFiltersAreStableUnderTurkishLocale, testOAStringCaseFoldUsesLocaleStableRules

  FILTER-CASE-002
  Tests: testEqualFilterIgnoreCaseUsesOACompare, testNotEqualFilterIgnoreCaseNegatesOACompare

  FILTER-PATH-001
  Tests: testHubFilterPathWrapperAppliesTerminalValueFilter, testPathFilterDirectUseResolvesPathFromRootObject

  FILTER-PATH-002
  Tests: testEqualFilterAcrossManyPathMatchesAnyChild, testLikeFilterAcrossManyPathRejectsWhenNoChildMatches

  FILTER-PATH-003
  Tests: testEqualPathFilterRefreshesWhenHubAOChanges, testInFilterRefreshesWhenHubAOChanges

  FILTER-COMPOSE-001
  Tests: testAndFilterTruthTable, testOrFilterTruthTable, testXorFilterTruthTable

  FILTER-COMPOSE-002
  Tests: testBlockFilterRequiresAllFilters, testBlockFilterWithNullArrayActsAsNoFilter

  FILTER-QUERY-001
  Tests: testQueryFilterAndOrPrecedenceMatchesSpec, testQueryFilterParenthesesOverridePrecedence

  FILTER-QUERY-002
  Tests: testQueryFilterRejectsTrailingTokensAfterValidExpression

  FILTER-QUERY-003
  Tests: testQueryFilterRejectsMissingParameter, testQueryFilterRejectsExtraParameter

  FILTER-QUERY-004
  Tests: testQueryFilterSimpleInEmptyListMatchesNothing, testQueryFilterCompoundInEmptyListMatchesNothing

  FILTER-QUERY-005
  Tests: testQueryFilterBooleanLiteralMatchesDatasourceSemantics, testQueryFilterNullLiteralMatchesDatasourceSemantics

  FILTER-FAILURE-001
  Tests: testInvalidQueryThrowsAtConstruction, testInvalidInArgumentTypeThrows

  FILTER-FAILURE-002
  Tests: testUpdateSelectReturnsTrueWhenOptimizationUnsupported,
  testEqualPathUpdateSelectDoesNotSuppressMemoryFilterForScalarValue

  FILTER-FAILURE-003
  Tests: testFindNullReturnsFalseForArrayWithoutNulls, testFindNullReturnsTrueForArrayWithNullElement


*/






