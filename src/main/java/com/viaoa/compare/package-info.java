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
package com.viaoa.compare;

/* CODEX Invariants

Comparison Invariants

  ID: COMPARE-EQ-001
  Contract statement: Equality comparison must return true only when the two values are semantically equal under the
  active OA comparison mode.
  Rationale: OACompare feeds filters, finders, Hub decisions, datasource object-cache filtering, property-change
  checks, and report/template logic. False equality causes silent runtime decisions.
  Source locations: OACompare.isEqual, OACompare.compare, OAEqualFilter, OANotEqualFilter callers
  Related CODEX findings: numeric precision collapse; null/default equality; unknown token matching broad tokens
  Suggested unit tests: testIsEqualDoesNotCollapseDistinctNumericValues, testIsEqualDoesNotMatchUnknownAgainstNotNull,
  testIsEqualNullDefaultContract
  Spec target section: Compare Runtime / Equality Semantics

  ID: COMPARE-ORDER-001
  Contract statement: Ordering comparison must be deterministic for the same input state and must not return arbitrary
  order after failed comparison.
  Rationale: Hub sorting, in-memory select ordering, reports, and object-cache datasource ordering depend on
  repeatable order.
  Source locations: OACompare.compare, OAComparator.compare, OADataSourceObjectCache.select, OASelect finder-result
  sorting
  Related CODEX findings: OAComparator fallback can return -1 both directions; incompatible comparable fallback
  Suggested unit tests: testComparatorAntisymmetryForMixedComparableTypes, testObjectCacheSelectOrderIsStable
  Spec target section: Compare Runtime / Ordering Semantics

  ID: COMPARE-NULL-001
  Contract statement: Null semantics must be explicit: null may only equal null or documented null/empty tokens,
  unless a specific primitive-null/default coercion contract applies.
  Rationale: Null means absence/unknown in object properties and criteria. Implicit null-to-zero/false/string-empty
  matching can corrupt filtering decisions.
  Source locations: OACompare.compare, OANullObject, OANotNullObject, OAEmptyObject, OANotExist
  Related CODEX findings: null converted to numeric/boolean defaults; isIn(null, ...) cannot match null
  Suggested unit tests: testNullVsZeroComparisonContract, testNullVsFalseComparisonContract,
  testNullTokenComparisonSemantics
  Spec target section: Compare Runtime / Null Semantics

  ID: COMPARE-NUMERIC-001
  Contract statement: Numeric comparison must compare numeric value without unintended precision loss before the final
  contracted target precision.
  Rationale: Numeric comparisons drive filters, query-like matching, sorting, and duplicate detection. Precision loss
  creates false equality/order.
  Source locations: OACompare.compare(Object,Object,int), OACompare.compare(double,double,int), OAGreaterThanZero,
  OALessThanZero
  Related CODEX findings: mixed numeric doubleValue() fallback; large double scaled-long collapse; greater/less-than-
  zero underflow
  Suggested unit tests: testBigIntegerCompareBeyondDoublePrecision, testBigDecimalTinyGreaterThanZero,
  testLargeDoubleDecimalCompareDoesNotCollapse
  Spec target section: Compare Runtime / Numeric Precision

  ID: COMPARE-BIGDECIMAL-001
  Contract statement: BigDecimal and BigInteger comparison must either preserve exact value or intentionally normalize
  according to documented OA decimal-place rules.
  Rationale: OA business data often uses money, quantity, IDs, and calculated values where decimal precision matters.
  Source locations: OACompare.compare, OAConverterNumber, OAConverterBigDecimal interaction
  Related CODEX findings: mixed BigDecimal/BigInteger converted through double; decimalPlaces same-wrapper
  inconsistency
  Suggested unit tests: testBigDecimalMixedNumberExactComparison, testDecimalPlacesZeroAppliesConsistently,
  testBigIntegerVsLongBoundaryComparison
  Spec target section: Compare Runtime / Exact Numeric Types

  ID: COMPARE-STRING-001
  Contract statement: String comparison must clearly distinguish case-sensitive mode from case-insensitive mode and
  use locale-stable behavior when deterministic runtime behavior is required.
  Rationale: Distributed OA behavior must not change by JVM default locale.
  Source locations: OACompare.isEqual(..., boolean), OACompare.isLike, OAComparator.compare
  Related CODEX findings: default-locale toLowerCase() / toUpperCase() usage
  Suggested unit tests: testCaseInsensitiveCompareIsLocaleStable, testComparatorStringCaseInsensitiveSortStable,
  testLikeCaseInsensitiveLocaleStable
  Spec target section: Compare Runtime / String Semantics

  ID: COMPARE-LIKE-001
  Contract statement: Wildcard comparison must preserve pattern token order and must not match by overlapping prefix/
  suffix tokens unless explicitly documented.
  Rationale: LIKE behavior is used by filters/search/text decisions; false positives are silent data-selection bugs.
  Source locations: OACompare.isLike, OALikeFilter, OANotLikeFilter
  Related CODEX findings: interior wildcard overlap such as ab*bc matching abc
  Suggested unit tests: testLikeInteriorWildcardDoesNotOverlapTokens, testLikePrefixSuffixSemantics,
  testNotLikeIsExactInverse
  Spec target section: Compare Runtime / Wildcard Matching

  ID: COMPARE-DATE-001
  Contract statement: Date/time comparison must preserve the intended OA temporal meaning: date-only, time-only, local
  date-time, and instant semantics must not drift through operand order, timezone, or formatting conversion.
  Rationale: OA comparisons are used in schedules, queries, filters, and reports where date semantics must be stable.
  Source locations: OACompare.compare, OADate, OADateTime, OATime conversion branches, Java temporal converter paths
  Related CODEX findings: operand-order-dependent string/date-style comparison for non-OA temporal types
  Suggested unit tests: testStringDateCompareIsSymmetric, testOADateDateOnlyCompareIgnoresTimeAsContracted,
  testInstantStringCompareDoesNotDependOnOperandOrder
  Spec target section: Compare Runtime / Temporal Semantics

  ID: COMPARE-CONVERT-001
  Contract statement: Conversion before comparison must not create misleading equality or ordering; failed conversion
  must either produce a documented non-match/order or fail visibly.
  Rationale: OA’s flexible coercion is useful only if it does not silently change business meaning.
  Source locations: OACompare.compare conversion block, OAConverter.convert, OAConv helpers
  Related CODEX findings: null/default conversion; string/date operand-order conversion; boolean/numeric converter
  findings from converter package
  Suggested unit tests: testFailedConversionDoesNotReturnEquality, testConversionCompareIsSymmetricForSupportedTypes,
  testInvalidCriterionDoesNotMatchByFallback
  Spec target section: Compare Runtime / Conversion Before Compare

  ID: COMPARE-SPECIAL-001
  Contract statement: Special comparison tokens must have central OACompare semantics consistent with their own
  documented predicate semantics.
  Rationale: Tokens are used by HubChangeListener, filters, criteria, callbacks, and object-state handling. Direct
  equals() and OACompare.compare() must not disagree unexpectedly.
  Source locations: OASpecialCompareObject, OAAnyValueObject, OANullObject, OANotNullObject, OAEmptyObject,
  OANotEmptyObject, OAUnknownObject, OANotExist, OAGreaterThanZero, OALessThanZero, OACompare.compare
  Related CODEX findings: greater/less-than-zero not handled in OACompare; unknown matched by broad tokens; empty/not-
  empty mismatch with documented emptiness
  Suggested unit tests: testSpecialTokensThroughOACompare, testUnknownObjectMatchesOnlyContractedUnknown,
  testEmptyTokensUseOAEmptinessRules
  Spec target section: Compare Runtime / Special Predicate Objects

  ID: COMPARE-COLLECTION-001
  Contract statement: Array, Hub, and collection-style comparison must apply the same scalar comparison contract
  recursively and must not drop active precision/case semantics.
  Rationale: OA frequently compares Hubs, arrays, and property-path results in filters and object matching.
  Source locations: OACompare.compare array/Hub branches, OACompare.isIn, OAObjectCompare array branch
  Related CODEX findings: recursive array/Hub comparisons drop decimalPlaces; isIn(null, ...) null mismatch;
  OAObjectCompare null array element issue
  Suggested unit tests: testArrayComparisonPreservesDecimalPlaces, testHubComparisonPreservesDecimalPlaces,
  testIsInNullElementContract
  Spec target section: Compare Runtime / Collection Semantics

  ID: COMPARE-COMPARATOR-001
  Contract statement: Comparator implementations used for sorting must obey antisymmetry, transitivity, and stable
  null ordering.
  Rationale: Java sorting can fail or produce unstable order when comparator contracts are violated; OA uses
  comparator sorting in Hubs and object-cache selects.
  Source locations: OAComparator.compare, HubSortListener, OASelect, OADataSourceObjectCache
  Related CODEX findings: incompatible comparable fallback; non-comparable values compare equal; lazy init publication
  race; order parser spacing issue
  Suggested unit tests: testOAComparatorAntisymmetry, testOAComparatorTransitivity,
  testOAComparatorMultiColumnDescParsingWithSpaces
  Spec target section: Compare Runtime / Comparator Contract

  ID: COMPARE-THREAD-001
  Contract statement: Shared comparison helpers must be immutable, safely initialized, or externally confined before
  concurrent use.
  Rationale: Hub sorting and shared selectors can run from multiple runtime paths; first-use races can silently sort
  incorrectly.
  Source locations: OAComparator.methodss, OAComparator.bAscendings, singleton special tokens
  Related CODEX findings: OAComparator publishes methodss before bAscendings
  Suggested unit tests: testOAComparatorConcurrentFirstUseDirectionStability,
  testSpecialCompareTokensAreStatelessSingletons
  Spec target section: Compare Runtime / Thread Safety

  ID: COMPARE-FAIL-001
  Contract statement: Comparison failure must not silently degrade to equality, arbitrary order, or misleading string
  comparison unless explicitly documented for that method.
  Rationale: A fallback that hides failure can select wrong objects, sort wrong, or suppress property-change handling.
  Source locations: OACompare.compare final fallback, OAComparator.compare catch/fallback paths, OAObjectCompare
  reporting paths
  Related CODEX findings: non-comparable property values compare as 0; comparator fallback returns -1 both directions
  Suggested unit tests: testNonComparableDistinctValuesDoNotCompareEqualByDefault,
  testComparatorFailedCompareDoesNotViolateContract, testCompareFailureModeIsDocumented
  Spec target section: Compare Runtime / Failure Semantics

  Suggested package-level spec summary

  com.viaoa.compare is OA’s central comparison layer for equality, ordering, predicates, and flexible coercion. It
  supports filters, Hub sorting/filtering, datasource object-cache selection, finders, reports, templates, callbacks,
  object matching, and duplicate detection.

  It must guarantee deterministic equality and ordering for the same runtime state. It must preserve semantic
  distinctions between null, empty, unknown, not-exist, numeric zero, boolean false, and ordinary values. Numeric
  comparison must avoid accidental precision loss, especially for BigDecimal, BigInteger, and large values. String
  comparison must make case-sensitive/case-insensitive behavior explicit and locale-stable where runtime determinism
  matters. Date/time comparison must preserve OA date/time semantics and avoid operand-order or timezone drift.

  It must never silently report equality for semantically different values, silently hide failed comparison as
  success, or use arbitrary fallback ordering where comparator contracts matter. Comparator classes must be stable,
  symmetric, transitive, and safely initialized when shared.

  Cross-package assumptions: converters provide documented coercion semantics; date/time classes define temporal
  meaning; Hub/select/filter packages rely on compare results as runtime truth; metadata/path/object layers may feed
  values into comparison without revalidating comparison semantics.

  Likely unit-test categories: scalar equality, null/default behavior, special tokens, exact numeric comparison,
  decimal-place comparison, string/case/locale behavior, wildcard behavior, date/time coercion symmetry, array/Hub
  recursion, comparator contract tests, concurrent comparator initialization, and failure/fallback behavior.


*/



