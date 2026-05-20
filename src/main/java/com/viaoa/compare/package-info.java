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

COMPARE-EQ-001 — Equality Means Semantic Equality Under The Active OA Mode
Contract statement: Equality comparison must return true only when the two values are semantically equal under the
active OA comparison mode, including configured case, null, numeric precision, special-token, and conversion rules.
Rationale: OACompare feeds filters, finders, Hub decisions, datasource object-cache filtering, property-change
checks, reports, templates, and duplicate detection.
Source scope: OACompare.isEqual, isEqualIgnoreCase, isNotEqual, isEqualOrIn, isIn, special comparison objects.
Related CODEX findings: Numeric precision collapse; null/default equality; unknown token matching broad tokens.
Suggested unit tests: testIsEqualDoesNotCollapseDistinctNumericValues(),
testIsEqualDoesNotMatchUnknownAgainstNotNull(), testIsEqualNullDefaultContract()
Spec target section: Compare Runtime / Equality Semantics

COMPARE-EQ-002 — Not-Equal Is The Contracted Inverse Of Equal
Contract statement: Not-equal helpers must be the inverse of equality for the same inputs and active comparison
mode, except where explicitly documented special-token semantics apply.
Rationale: Filters, query-like matching, callbacks, and property-change checks rely on equality and inequality being
consistent.
Source scope: OACompare.isNotEqual overloads, OACompare.isEqual overloads, OANot* special objects.
Related CODEX findings: Not-like/not-equal inverse concerns from compare/filter scans.
Suggested unit tests: testNotEqualIsInverseOfEqualForScalars(), testNotEqualIgnoreCaseIsInverseOfEqualIgnoreCase(),
testSpecialTokenNotEqualContract()
Spec target section: Compare Runtime / Equality Inversion

COMPARE-ORDER-001 — Ordering Is Deterministic For Same Inputs
Contract statement: Ordering comparison must produce the same result for the same input values, decimal-place mode,
conversion assumptions, and runtime state.
Rationale: Hub sorting, in-memory select ordering, reports, object-cache datasource ordering, and comparator usage
depend on repeatable order.
Source scope: OACompare.compare overloads, OAComparator.compare, OADataSourceObjectCache/select callers.
Related CODEX findings: Incompatible comparable fallback; arbitrary fallback ordering; object-cache select order
stability.
Suggested unit tests: testCompareSameInputsSameOrder(), testObjectCacheSelectOrderIsStable(),
testComparatorMixedComparableTypesUsesDefinedOrder()
Spec target section: Compare Runtime / Ordering Semantics

COMPARE-ORDER-002 — Greater Less Between Boundaries Use Compare Contract
Contract statement: Greater-than, less-than, equal-or-greater, equal-or-less, between, and equal-or-between helpers
must apply the same scalar comparison contract and boundary inclusivity documented by the method.
Rationale: Range comparisons drive filters, criteria, reports, and query-like decisions.
Source scope: OACompare.isGreater, isLess, isEqualOrGreater, isGreaterOrEqual, isEqualOrLess, isLessOrEqual,
isBetween, isEqualOrBetween, isBetweenOrEqual.
Related CODEX findings: Boundary/range behavior and decimal-place propagation findings.
Suggested unit tests: testBetweenExclusiveBoundaries(), testBetweenOrEqualInclusiveBoundaries(),
testRangeComparisonsPreserveDecimalPlaces()
Spec target section: Compare Runtime / Range Semantics

COMPARE-NULL-001 — Null Semantics Are Explicit
Contract statement: Null may equal only null or documented null/empty/special tokens, unless a specific primitive-
null/default coercion contract applies.
Rationale: Null means absence or unknown in object properties and criteria; implicit null-to-zero/false/empty
matching can corrupt filtering decisions.
Source scope: OACompare.compare, isEqual, isIn, OANullObject, OANotNullObject, OAEmptyObject, OANotEmptyObject,
OANotExist.
Related CODEX findings: Null converted to numeric/boolean defaults; isIn(null, ...) cannot match null; null token
comparison findings.
Suggested unit tests: testNullVsZeroComparisonContract(), testNullVsFalseComparisonContract(),
testNullTokenComparisonSemantics(), testIsInCanMatchNullByContract()
Spec target section: Compare Runtime / Null Semantics

COMPARE-EMPTY-001 — Empty And Not-Empty Use OA Emptiness Rules
Contract statement: Empty and not-empty special comparisons must use OA’s documented emptiness rules for null,
strings, arrays, Hubs, collections, and other supported values.
Rationale: Empty/not-empty is used by filters, criteria, UI enablement, and object state logic.
Source scope: OACompare.isEmpty, isNotEmpty, OAEmptyObject, OANotEmptyObject.
Related CODEX findings: Empty/not-empty mismatch with documented emptiness.
Suggested unit tests: testEmptyTokenUsesOAEmptinessRules(), testNotEmptyTokenUsesOAEmptinessRules(),
testWhitespaceStringEmptyTrimContract()
Spec target section: Compare Runtime / Empty Semantics

COMPARE-NUMERIC-001 — Numeric Comparison Avoids Unintended Precision Loss
Contract statement: Numeric comparison must compare numeric value without unintended precision loss before the final
contracted target precision.
Rationale: Numeric comparisons drive filters, sorting, duplicate detection, query-like criteria, money/quantity
decisions, and object matching.
Source scope: OACompare.compare(Object,Object,int), compare(double,double,int), isEqual numeric overloads, numeric
special compare objects.
Related CODEX findings: Mixed numeric doubleValue fallback; large double scaled-long collapse; greater/less-than-
zero underflow.
Suggested unit tests: testBigIntegerCompareBeyondDoublePrecision(), testBigDecimalTinyGreaterThanZero(),
testLargeDoubleDecimalCompareDoesNotCollapse()
Spec target section: Compare Runtime / Numeric Precision

COMPARE-NUMERIC-002 — Decimal Places Apply Consistently
Contract statement: Decimal-place comparison mode must be applied consistently across same-wrapper numeric operands,
mixed numeric operands, arrays/Hubs, range helpers, and equality/order helpers.
Rationale: A decimal-place argument is part of the comparison contract and must not be dropped depending on operand
shape.
Source scope: OACompare.compare(Object,Object,int), compare(double,double,int), isEqual with decimalPlaces, range
helpers, collection/array branches.
Related CODEX findings: decimalPlaces == 0 ignored for same-wrapper numeric operands; recursive array/Hub
comparisons drop decimalPlaces.
Suggested unit tests: testDecimalPlacesZeroAppliesConsistently(), testSameWrapperNumericDecimalPlacesApplied(),
testArrayComparisonPreservesDecimalPlaces(), testHubComparisonPreservesDecimalPlaces()
Spec target section: Compare Runtime / Decimal Place Semantics

COMPARE-BIGDECIMAL-001 — Exact Numeric Types Preserve Exact Value
Contract statement: BigDecimal and BigInteger comparison must preserve exact value or intentionally normalize
according to documented OA decimal-place rules.
Rationale: OA business data often uses money, quantity, IDs, and calculated values where exact numeric precision
matters.
Source scope: OACompare.compare, BigDecimal/BigInteger branches, converter interaction.
Related CODEX findings: Mixed BigDecimal/BigInteger converted through double; BigInteger/Long boundary precision
concerns.
Suggested unit tests: testBigDecimalMixedNumberExactComparison(), testBigIntegerVsLongBoundaryComparison(),
testBigDecimalScaleNormalizationByContract()
Spec target section: Compare Runtime / Exact Numeric Types

COMPARE-STRING-001 — String Comparison Defines Case And Locale Semantics
Contract statement: String comparison must distinguish case-sensitive from case-insensitive mode and must use
locale-stable behavior where deterministic runtime behavior is required.
Rationale: Distributed OA behavior must not change by JVM default locale.
Source scope: OACompare.isEqual with ignoreCase, isEqualIgnoreCase, isLike, OAComparator string property
comparisons.
Related CODEX findings: Default-locale toLowerCase/toUpperCase usage.
Suggested unit tests: testCaseInsensitiveCompareIsLocaleStable(), testComparatorStringCaseInsensitiveSortStable(),
testLikeCaseInsensitiveLocaleStable()
Spec target section: Compare Runtime / String Semantics

COMPARE-LIKE-001 — Wildcard Matching Preserves Pattern Semantics
Contract statement: Wildcard comparison must preserve pattern token order, prefix/suffix rules, case mode, and non-
overlap behavior according to documented LIKE semantics.
Rationale: LIKE behavior is used by filters, search, reports, and template decisions; false positives silently
select wrong data.
Source scope: OACompare.isLike, OALikeFilter/OANotLikeFilter callers.
Related CODEX findings: Interior wildcard overlap such as ab*bc matching abc.
Suggested unit tests: testLikeInteriorWildcardDoesNotOverlapTokens(), testLikePrefixSuffixSemantics(),
testNotLikeIsExactInverse()
Spec target section: Compare Runtime / Wildcard Matching

COMPARE-DATE-001 — Temporal Comparison Preserves Intended Date/Time Meaning
Contract statement: Date/time comparison must preserve the intended OA temporal meaning: date-only, time-only, local
date-time, and instant semantics must not drift through operand order, timezone, locale, or formatting conversion.
Rationale: OA comparisons are used in schedules, queries, filters, sorting, and reports where temporal meaning must
be stable.
Source scope: OACompare.compare temporal branches, OADate/OADateTime/OATime interactions, Java temporal converter
paths.
Related CODEX findings: Operand-order-dependent string/date-style comparison for non-OA temporal types.
Suggested unit tests: testStringDateCompareIsSymmetric(), testOADateDateOnlyCompareIgnoresTimeAsContracted(),
testInstantStringCompareDoesNotDependOnOperandOrder()
Spec target section: Compare Runtime / Temporal Semantics

COMPARE-CONVERT-001 — Conversion Before Compare Must Preserve Semantic Value
Contract statement: Conversion before comparison must not create misleading equality or ordering; failed conversion
must either produce a documented non-match/order or fail visibly.
Rationale: OA’s flexible coercion is useful only if it does not silently change business meaning.
Source scope: OACompare.compare conversion block, OAConverter.convert, OAConv helper interactions.
Related CODEX findings: Null/default conversion; string/date operand-order conversion; boolean/numeric converter
findings from converter package.
Suggested unit tests: testFailedConversionDoesNotReturnEquality(),
testConversionCompareIsSymmetricForSupportedTypes(), testInvalidCriterionDoesNotMatchByFallback()
Spec target section: Compare Runtime / Conversion Before Compare

COMPARE-TYPE-001 — Comparable And Non-Comparable Behavior Is Defined
Contract statement: Values that implement Comparable must be compared only when type-compatible under OA rules, and
non-comparable distinct values must not silently compare equal unless explicitly contracted.
Rationale: Arbitrary or hidden fallback comparison can corrupt sorting, filters, and duplicate detection.
Source scope: OACompare.compare final branches, OAComparator.compare/preCheck, OAObjectCompare.
Related CODEX findings: Incompatible comparable fallback; non-comparable values compare equal; comparator fallback
returns invalid order.
Suggested unit tests: testIncompatibleComparableTypesUseDefinedFailureOrOrder(),
testNonComparableDistinctValuesDoNotCompareEqualByDefault(), testCompareFailureModeIsDocumented()
Spec target section: Compare Runtime / Type Compatibility

COMPARE-SPECIAL-001 — Special Comparison Tokens Have Central OA Semantics
Contract statement: Special comparison token objects must have documented predicate semantics and
OACompare.compare/isEqual must honor those same semantics consistently.
Rationale: Tokens are used by HubChangeListener, filters, criteria, callbacks, object-state handling, and runtime
matching.
Source scope: OASpecialCompareObject, OAAnyValueObject, OANullObject, OANotNullObject, OAEmptyObject,
OANotEmptyObject, OAUnknownObject, OANotExist, OAGreaterThanZero, OALessThanZero, OACompare.compare.
Related CODEX findings: Greater/less-than-zero not handled in OACompare special-object path; unknown matched by
broad tokens; empty/not-empty mismatch.
Suggested unit tests: testSpecialTokensThroughOACompare(), testUnknownObjectMatchesOnlyContractedUnknown(),
testEmptyTokensUseOAEmptinessRules()
Spec target section: Compare Runtime / Special Predicate Objects

COMPARE-SPECIAL-002 — Numeric Predicate Tokens Preserve Numeric Precision
Contract statement: Greater-than-zero and less-than-zero special predicates must use OA numeric precision semantics
rather than lossy double-only comparison.
Rationale: Tiny BigDecimal/BigInteger values and underflow edge cases must not produce false predicate results.
Source scope: OAGreaterThanZero.equals, OALessThanZero.equals, OACompare special token handling.
Related CODEX findings: OAGreaterThanZero/OALessThanZero convert to Number and compare using doubleValue.
Suggested unit tests: testBigDecimalTinyGreaterThanZero(), testBigDecimalTinyLessThanZero(),
testSpecialNumericPredicatesDoNotUnderflowThroughDouble()
Spec target section: Compare Runtime / Numeric Predicate Tokens

COMPARE-COLLECTION-001 — Array Hub And Collection Comparison Recurses With Same Scalar Contract
Contract statement: Array, Hub, and collection-style comparison must apply the same scalar comparison contract
recursively, including decimal places, case mode, null rules, and special tokens.
Rationale: OA frequently compares Hubs, arrays, and property-path results in filters, object matching, and report
decisions.
Source scope: OACompare.compare array/Hub branches, OACompare.isIn, OAObjectCompare array branch.
Related CODEX findings: Recursive array/Hub comparisons drop decimalPlaces; isIn(null, ...) null mismatch;
OAObjectCompare null array element issue.
Suggested unit tests: testArrayComparisonPreservesDecimalPlaces(), testHubComparisonPreservesDecimalPlaces(),
testIsInNullElementContract(), testOAObjectCompareNullArrayElementContract()
Spec target section: Compare Runtime / Collection Semantics

COMPARE-OBJECT-001 — Object Comparison Uses Identity Key And Property Semantics By Contract
Contract statement: Object comparison must distinguish object identity/key comparison from property/value comparison
and must not silently compare the wrong semantic layer.
Rationale: OAObject matching, duplicate detection, cache filtering, and report comparisons depend on whether
identity or value semantics are intended.
Source scope: OACompare.compare object branches, OAObjectCompare.compare, OAObjectCompare.getKey, property-path
comparison callers.
Related CODEX findings: OAObjectCompare key/null/array/property compare findings.
Suggested unit tests: testOAObjectCompareUsesConfiguredKeyContract(),
testObjectIdentityCompareDiffersFromPropertyValueCompareByContract(),
testOAObjectCompareReportsPropertyPathMismatch()
Spec target section: Compare Runtime / Object Comparison Semantics

COMPARE-COMPARATOR-001 — Comparator Implementations Obey Java Comparator Contract
Contract statement: Comparators used for sorting must obey antisymmetry, transitivity, consistency for equal values,
and stable null ordering.
Rationale: Java sorting can fail or produce unstable order when comparator contracts are violated; OA uses
comparator sorting in Hubs and object-cache selects.
Source scope: OAComparator.compare, preCheck, init, HubSortListener, OASelect/object-cache sorting callers.
Related CODEX findings: Comparator fallback can return -1 both directions; incompatible comparable fallback; non-
comparable compare equal.
Suggested unit tests: testOAComparatorAntisymmetry(), testOAComparatorTransitivity(),
testOAComparatorStableNullOrdering()
Spec target section: Compare Runtime / Comparator Contract

COMPARE-COMPARATOR-002 — Comparator Property Path Parsing And Direction Are Deterministic
Contract statement: Comparator property path parsing, multi-column ordering, ascending/descending direction, and
null placement must be deterministic for the same property path string and class metadata.
Rationale: Hub sorting and datasource object-cache ordering depend on stable parsed sort configuration.
Source scope: OAComparator constructor, getPropertyPaths, getAsc, init, methodss/bAscendings.
Related CODEX findings: Order parser spacing issue; lazy init publication race.
Suggested unit tests: testOAComparatorMultiColumnDescParsingWithSpaces(),
testOAComparatorDirectionStableAfterInit(), testComparatorPropertyPathParsingDeterministic()
Spec target section: Compare Runtime / Comparator Parsing

COMPARE-THREAD-001 — Shared Comparison Helpers Are Safely Initialized
Contract statement: Shared comparison helpers must be immutable, safely initialized, or externally confined before
concurrent use.
Rationale: Hub sorting and shared selectors can run from multiple runtime paths; first-use races can silently sort
incorrectly.
Source scope: OAComparator.methodss, OAComparator.bAscendings, singleton special tokens.
Related CODEX findings: OAComparator publishes methodss before bAscendings.
Suggested unit tests: testOAComparatorConcurrentFirstUseDirectionStability(),
testSpecialCompareTokensAreStatelessSingletons()
Spec target section: Compare Runtime / Thread Safety

COMPARE-FAIL-001 — Comparison Failure Must Not Become Misleading Success
Contract statement: Comparison failure must not silently degrade to equality, arbitrary order, wrong string
conversion, or broad token match unless explicitly documented for that method.
Rationale: A fallback that hides failure can select wrong objects, sort wrong, or suppress property-change handling.
Source scope: OACompare.compare final fallback, OAComparator.compare catch/fallback paths, OAObjectCompare reporting
paths.
Related CODEX findings: Non-comparable property values compare as 0; comparator fallback returns -1 both directions;
invalid conversion fallback risks.
Suggested unit tests: testNonComparableDistinctValuesDoNotCompareEqualByDefault(),
testComparatorFailedCompareDoesNotViolateContract(), testCompareFailureModeIsDocumented()
Spec target section: Compare Runtime / Failure Semantics

COMPARE-DETERMINISM-001 — Same Inputs Produce Same Comparison Result
Contract statement: For the same values, comparison mode, decimal places, case mode, conversion assumptions, locale/
timezone assumptions, and metadata/path state, comparison helpers must produce the same result.
Rationale: Deterministic comparison is required for filters, sorting, finders, cache selection, reports, templates,
callbacks, and duplicate detection.
Source scope: OACompare, OAComparator, OAObjectCompare, special comparison objects.
Related CODEX findings: Package-wide precision, locale, temporal, comparator, conversion, and fallback findings.
Suggested unit tests: testSameScalarInputsProduceSameComparisonResult(),
testSameComparatorInputsProduceSameSortOrder(), testSameObjectCompareInputsProduceSameMismatchReport()
Spec target section: Compare Runtime / Deterministic Comparison Semantics

*/



