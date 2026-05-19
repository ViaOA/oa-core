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
package com.viaoa.lang;

/* CODEX Invariants

 1. Lang Utility Runtime Contracts

  LANG-RUNTIME-001 — Lang Helpers Are Deterministic
  Contract statement: For the same inputs and runtime configuration, com.viaoa.lang helpers must return deterministic
  results without hidden state changes, except for explicitly mutable wrapper instances and documented static state.
  Rationale: These utilities are low-level OA building blocks used across graph, hub, datasource, filters, templates,
  and runtime code.
  Source locations: OAString, OAStr, OAArray, OAInteger, OADouble, Tuple, Tuple3, OAFlagEnum, SizeOf.
  Known related CODEX findings: locale-sensitive case folding noted through OAString/filter behavior; SizeOf shared-
  reference accounting findings.
  Suggested unit tests: testLangHelpersAreDeterministicForSameInputs,
  testStringCaseHelpersAreLocaleStableWhenRequired.
  Spec target section: Lang Utilities / Runtime Semantics.

  LANG-RUNTIME-002 — Lang Helpers Must Not Hide Wrong Results As Success
  Contract statement: A helper must either return the correct transformed/search/result value or fail visibly/return
  the documented no-op value; it must not silently return an unchanged or partially wrong result for normal OA usage.
  Rationale: Silent helper failures propagate into object graph, filtering, reflection, and serialization behavior.
  Source locations: OAArray.removeValue, OAArray.reorderToMatch, OAString.format, SizeOf.sizeOf.
  Known related CODEX findings: primitive removeValue returns unchanged array; reorderToMatch loses duplicate/null
  elements; SizeOf over/under-count issues.
  Suggested unit tests: testArrayRemoveValueActuallyRemovesPrimitiveValue, testReorderToMatchDoesNotLoseDuplicates,
  testSizeOfSharedReferenceAccountingIsStable.
  Spec target section: Lang Utilities / Silent Wrong-Result Prevention.

  2. String Helper Contracts

  LANG-STRING-001 — OAString Is A Stable Facade Over Text Utilities
  Contract statement: OAString and OAStr expose stable OA string helper APIs and must preserve the semantics of the
  delegated com.viaoa.text implementations.
  Rationale: Much legacy and generated OA code uses OAString as the central string utility entry point.
  Source locations: OAString, OAStr, delegated OAText* classes.
  Known related CODEX findings: none observed beyond delegated text package findings.
  Suggested unit tests: testOAStringDelegatesTrimSpaces, testOAStrInheritsOAStringBehavior.
  Spec target section: Lang Utilities / String Helper Semantics.

  LANG-STRING-002 — String Helpers Must Be Null-Aware Where Contracted
  Contract statement: OA string conversion/sanitization helpers that advertise non-null behavior must return
  documented fallback values for null inputs.
  Rationale: OA template, UI, and formatting code frequently relies on null-safe string rendering.
  Source locations: OAString.toString(Object), OAString.toString(String), OAString.toString(String,String),
  OAString.fmt(String), OAString.notNull, OAString.notEmpty.
  Known related CODEX findings: OAString.toString(byte[]) null byte-array path was reported.
  Suggested unit tests: testToStringNullObjectReturnsEmpty, testToStringNullStringUsesFallback,
  testToStringNullByteArrayUsesDocumentedBehavior.
  Spec target section: Lang Utilities / Null-Safe String Conversion.

  LANG-STRING-003 — Case Conversion Must Be Locale-Stable Where Used For Matching
  Contract statement: Case conversion used to implement matching, lookup, filter, or compare semantics must use
  locale-stable rules unless the API explicitly says it is locale-sensitive.
  Rationale: OA server/client/filter behavior must not change under Turkish or other JVM default locales.
  Source locations: OAString.upper/lower, OATextChars, filter usage via OAContainsFilter, OAIndexOfFilter, OAStartsWi
  thFilter.
  Known related CODEX findings: OAString CODEX note for locale-sensitive filter case folding.
  Suggested unit tests: testCaseInsensitiveCompareIsStableUnderTurkishLocale,
  testUpperLowerHelpersUseDocumentedLocaleSemantics.
  Spec target section: Lang Utilities / Locale-Stable Text Semantics.

  LANG-STRING-004 — Formatting Helpers Must Preserve Converter Null-Format Semantics
  Contract statement: Numeric/date/boolean formatting helpers must delegate null or empty format handling consistently
  to OA converter/text formatting semantics.
  Rationale: Template and reflection formatting call these helpers with optional formats.
  Source locations: OAString.format(long,String), OAString.format(int,String), OAString.format(double,String),
  OAString.format(boolean,String), OAString.format(OADateTime,String), OAString.format(String,String).
  Known related CODEX findings: format(int,null) and format(double,null) inconsistency reported.
  Suggested unit tests: testFormatIntNullFormatUsesConverter, testFormatDoubleNullFormatUsesConverter,
  testFormatLongNullFormatUsesConverter.
  Spec target section: Lang Utilities / Formatting Semantics.

  3. Array Helper Contracts

  LANG-ARRAY-001 — Array Search Semantics Are Explicit
  Contract statement: Search helpers must consistently define whether they use reference equality, equals, case-
  sensitive string equality, or case-insensitive string equality.
  Rationale: OA runtime uses arrays for listener lists, flags, locks, metadata lists, and generated helper state.
  Source locations: OAArray.contains, OAArray.containsExact, OAArray.indexOf,
  OAArray.indexOf(String[],String,boolean).
  Known related CODEX findings: contains(String[],...,bCaseSensitive) and indexOf(String[],...,bCaseSensitive) ignore
  bCaseSensitive.
  Suggested unit tests: testContainsObjectUsesEquals, testContainsExactUsesReferenceOnly,
  testStringIndexOfHonorsCaseSensitiveFlag.
  Spec target section: Lang Utilities / Array Search Semantics.

  LANG-ARRAY-002 — Array Mutation Helpers Preserve Component Type
  Contract statement: Add/remove/insert helpers must preserve the intended runtime component type, including explicit
  Class c overloads.
  Rationale: OA often casts returned arrays back to listener, metadata, Hub, trigger, lock, or callback array types.
  Source locations: OAArray.add(Class,Object[],Object), OAArray.add(Class,Object[],Object...),
  OAArray.removeAt(Class,Object[],int), OAArray.add(T[],T), OAArray.insert(T[],T,int).
  Known related CODEX findings: explicit-class overloads can ignore c when using Arrays.copyOf; null-array typed add
  can infer overly narrow subtype.
  Suggested unit tests: testAddClassOverloadPreservesExplicitComponentType,
  testRemoveAtClassOverloadPreservesExplicitComponentType, testTypedAddNullArraySubtypeBehaviorIsDocumented.
  Spec target section: Lang Utilities / Array Type Semantics.

  LANG-ARRAY-003 — Array Remove Helpers Remove Exactly The First Matching Value
  Contract statement: removeValue helpers must remove the first matching value and return the original array only when
  no match exists.
  Rationale: OA uses arrays as compact mutable lists; false no-op removal leaks listeners/locks/flags.
  Source locations: OAArray.removeValue(Class,Object[],Object), OAArray.removeValue(int[],int),
  OAArray.removeValue(double[],double).
  Known related CODEX findings: primitive removeValue methods never assign pos; null object removal is a contract
  concern.
  Suggested unit tests: testRemoveValueIntRemovesFirstMatch, testRemoveValueDoubleRemovesFirstMatch,
  testRemoveValueObjectRemovesFirstEqualsMatch.
  Spec target section: Lang Utilities / Array Remove Semantics.

  LANG-ARRAY-004 — Array Insert Bounds Are Defined
  Contract statement: Insert helpers must define behavior for negative, in-range, and beyond-end positions; they must
  not throw accidental index/copy exceptions for normal OA usage.
  Rationale: Listener/lock reordering code can use insert operations to move entries.
  Source locations: OAArray.insert(T[],T,int), OAArray.insert(Class,Object[],Object,int).
  Known related CODEX findings: negative insert position can throw; beyond-end padding behavior is noted as uncertain.
  Suggested unit tests: testInsertNegativePositionUsesDocumentedBehavior, testInsertAtZeroPrepends,
  testInsertBeyondEndAppendsOrPadsAsDocumented.
  Spec target section: Lang Utilities / Array Insert Semantics.

  LANG-ARRAY-005 — Reorder Must Preserve Multiset Contents
  Contract statement: reorderToMatch must not lose, duplicate, or null out elements when matching one array order to
  another.
  Rationale: Reordering helpers must preserve object membership while changing order.
  Source locations: OAArray.reorderToMatch.
  Known related CODEX findings: null elements throw; duplicate equal elements can map to same target slot and leave
  null holes.
  Suggested unit tests: testReorderToMatchPreservesDuplicates, testReorderToMatchHandlesNullsAsDocumented,
  testReorderToMatchNoMatchLeavesOriginalUnchanged.
  Spec target section: Lang Utilities / Array Reorder Semantics.

  4. Numeric Wrapper Contracts

  LANG-NUMERIC-001 — Numeric Wrappers Provide Mutable By-Reference Values
  Contract statement: OAInteger and OADouble hold mutable numeric values for by-reference accumulation and counters.
  Rationale: OA anonymous callbacks and finders use wrappers to accumulate values from inner classes.
  Source locations: OAInteger, OADouble, OAFunction.count, OAFunction.sum.
  Known related CODEX findings: none observed.
  Suggested unit tests: testOAIntegerAddSubtractMutatesValue, testOADoubleAddSubtractMutatesValue.
  Spec target section: Lang Utilities / Numeric Wrapper Semantics.

  LANG-NUMERIC-002 — Constructor And Set Mark Value As Explicitly Set
  Contract statement: Constructing a wrapper with a value or calling set must make isSet() return true.
  Rationale: Callers use isSet to distinguish default zero from explicitly assigned zero.
  Source locations: OAInteger(int), OAInteger.set, OAInteger.isSet, OADouble(double), OADouble.set, OADouble.isSet.
  Known related CODEX findings: none observed.
  Suggested unit tests: testOAIntegerConstructorMarksSet, testOADoubleSetMarksSet.
  Spec target section: Lang Utilities / Numeric Set-State Semantics.

  LANG-NUMERIC-003 — Accumulation Does Not Imply Explicit Set Unless Documented
  Contract statement: Arithmetic helpers must either preserve current set-flag semantics or explicitly document if
  accumulation marks the wrapper as set.
  Rationale: isSet is a semantic flag, separate from numeric value mutation.
  Source locations: OAInteger.add/subtract, OADouble.add/subtract.
  Known related CODEX findings: none observed; current JavaDoc says arithmetic does not change set flag for OAInteger,
  and OADouble currently behaves the same.
  Suggested unit tests: testOAIntegerAddDoesNotMarkSetUnlessDocumented, testOADoubleAddDoesNotMarkSetUnlessDocumented.
  Spec target section: Lang Utilities / Numeric Wrapper Set-State Semantics.

  LANG-NUMERIC-004 — Binary Rendering Uses Fixed Width
  Contract statement: OAInteger.getAsBinary(int) must return exactly 32 bits and getAsBinary(long) exactly 64 bits.
  Rationale: These helpers are intended for debugging numeric bit representation.
  Source locations: OAInteger.getAsBinary(int), OAInteger.getAsBinary(long).
  Known related CODEX findings: obsolete viewBytes no-op was reported and later commented as testing-only.
  Suggested unit tests: testGetAsBinaryIntReturns32Chars, testGetAsBinaryLongReturns64Chars.
  Spec target section: Lang Utilities / Numeric Binary Semantics.

  5. Tuple / Value Object Contracts

  LANG-TUPLE-001 — Tuple Objects Are Immutable Carriers
  Contract statement: Tuple and Tuple3 store constructor values in final public fields and do not impose equality/hash
  semantics beyond object identity.
  Rationale: They are lightweight internal carriers, not value-key classes.
  Source locations: Tuple, Tuple3.
  Known related CODEX findings: none observed.
  Suggested unit tests: testTupleStoresConstructorValues, testTuple3StoresConstructorValues,
  testTupleUsesIdentityEquality.
  Spec target section: Lang Utilities / Tuple Carrier Semantics.

  LANG-TUPLE-002 — Tuple Values May Be Null
  Contract statement: Tuple fields may hold null values without throwing during construction or access.
  Rationale: OA uses tuples to carry optional paired runtime state.
  Source locations: Tuple(A,B), Tuple3(A,B,C).
  Known related CODEX findings: none observed.
  Suggested unit tests: testTupleAllowsNullValues, testTuple3AllowsNullValues.
  Spec target section: Lang Utilities / Tuple Null Semantics.

  6. Flag Enum Contracts

  LANG-FLAG-001 — OAFlagEnum Is A Three-State Flag
  Contract statement: OAFlagEnum must represent exactly false, true, and either/indeterminate states.
  Rationale: OA APIs may need to express tri-state behavior without nullable booleans.
  Source locations: OAFlagEnum.False, OAFlagEnum.True, OAFlagEnum.Either.
  Known related CODEX findings: none observed.
  Suggested unit tests: testOAFlagEnumHasExpectedValues, testOAFlagEnumEitherRepresentsIndeterminateState.
  Spec target section: Lang Utilities / Flag Semantics.

  7. Null / Empty Handling Contracts

  LANG-NULL-001 — Null Inputs Return Documented Safe Values
  Contract statement: Lang helpers accepting null inputs must return documented safe values such as null, empty
  string, false, -1, or unchanged input.
  Rationale: OA low-level utilities are used heavily in generated and runtime code where nulls are normal.
  Source locations: OAString.toString, OAString.fmt, OAArray.contains, OAArray.indexOf, OAArray.removeAt, OAArray.add,
  SizeOf.sizeOf.
  Known related CODEX findings: OAString.toString(byte[]) null path; OAArray.add(T[],T...) null varargs path;
  OAArray.removeValue null search contract concern.
  Suggested unit tests: testOAStringNullConversionContracts, testOAArrayNullSearchContracts,
  testSizeOfNullReturnsZero.
  Spec target section: Lang Utilities / Null Handling Semantics.

  LANG-NULL-002 — Empty Array Inputs Are No-Op Or Empty Results
  Contract statement: Array helpers must handle empty arrays without throwing and return documented unchanged/empty
  results.
  Rationale: Empty arrays are common for listener and metadata storage.
  Source locations: OAArray.contains, OAArray.indexOf, OAArray.removeAt, OAArray.removeValue, OAArray.hasNull.
  Known related CODEX findings: none observed.
  Suggested unit tests: testRemoveAtEmptyArrayReturnsSameArray, testIndexOfEmptyArrayReturnsMinusOne,
  testHasNullEmptyArrayReturnsFalse.
  Spec target section: Lang Utilities / Empty Array Semantics.

  8. Equality / Compare Contracts

  LANG-EQUAL-001 — Object Array Equality Is Element-By-Element
  Contract statement: OAArray.isEqual must compare arrays by length and pairwise reference/equals semantics, with null
  elements matching only null elements at the same position.
  Rationale: OA helper equality must be predictable for parameter lists, keys, and metadata arrays.
  Source locations: OAArray.isEqual.
  Known related CODEX findings: none observed.
  Suggested unit tests: testIsEqualSameReferenceTrue, testIsEqualSameElementsTrue, testIsEqualDifferentLengthFalse,
  testIsEqualNullElementPositionSemantics.
  Spec target section: Lang Utilities / Array Equality Semantics.

  LANG-EQUAL-002 — String Equality Delegates To Text Compare Semantics
  Contract statement: OAString.equals, notEquals, isEqual, and related methods must match OATextCompare null/case
  behavior.
  Rationale: OA code uses OAString as the stable facade for string comparison.
  Source locations: OAString.equals, OAString.notEquals, OAString.isEqual, OAString.isNotEqual.
  Known related CODEX findings: locale-stability concern where case folding is used for matching.
  Suggested unit tests: testOAStringEqualsNullSemantics, testOAStringEqualsIgnoreCaseSemantics,
  testOAStringNotEqualsIsInverseForDocumentedCases.
  Spec target section: Lang Utilities / String Equality Semantics.

  9. Parsing / Formatting Contracts

  LANG-FORMAT-001 — String Formatting Delegates To Text Format
  Contract statement: OAString.format(String,String), pickFormat, and fmt must return OATextFormat formatting results
  without adding divergent behavior.
  Rationale: Templates and display formatting rely on one formatting contract.
  Source locations: OAString.format(String,String), OAString.pickFormat, OAString.fmt.
  Known related CODEX findings: none observed.
  Suggested unit tests: testOAStringFmtMatchesOATextFormatFmt, testPickFormatMatchesFormat.
  Spec target section: Lang Utilities / String Formatting Semantics.

  LANG-FORMAT-002 — Numeric Formatting Selects Text Alignment Formats By R/L/C Markers
  Contract statement: Numeric overloads may route to text formatting when format contains alignment markers R, L, or
  C; otherwise they must use OA converter numeric formatting.
  Rationale: OA templates use the same format strings for aligned text and numeric conversion.
  Source locations: OAString.format(int,String), OAString.format(double,String), OAString.format(long,String).
  Known related CODEX findings: null-format bug for int/double; fixed or previously reported depending on current
  branch.
  Suggested unit tests: testFormatIntAlignmentFormatUsesTextFormatter, testFormatDoubleNumericFormatUsesConverter,
  testFormatIntNullFormatUsesConverter.
  Spec target section: Lang Utilities / Numeric Formatting Semantics.

  LANG-PARSE-001 — Parsing Facades Preserve Text Tokenizer Contracts
  Contract statement: OAString.parseLine, CSS map parsing, index/contains helpers, and field helpers must preserve
  delegated tokenizer/compare behavior.
  Rationale: These methods are compatibility entry points for text parsing.
  Source locations: OAString.parseLine, OAString.getCssMap, OAString.field, OAString.indexOf, OAString.contains.
  Known related CODEX findings: none observed in lang facade; text package findings apply to delegate implementations.
  Suggested unit tests: testOAStringParseLineDelegatesTokenizerSemantics,
  testOAStringGetCssMapDelegatesTokenizerSemantics.
  Spec target section: Lang Utilities / Parsing Facade Semantics.

  10. Mutable / Static State Contracts

  LANG-STATE-001 — Mutable Wrapper State Is Instance-Local
  Contract statement: OAInteger and OADouble mutable values and set flags must be per instance and not shared.
  Rationale: Wrappers are used inside callbacks and accumulators.
  Source locations: OAInteger, OADouble.
  Known related CODEX findings: none observed.
  Suggested unit tests: testOAIntegerInstancesAreIndependent, testOADoubleInstancesAreIndependent.
  Spec target section: Lang Utilities / Mutable Instance State.

  LANG-STATE-002 — SizeOf Static Instrumentation State Is Global And Explicit
  Contract statement: SizeOf uses JVM agent Instrumentation and global exclusion/default-size state; callers must get
  -1 when instrumentation is not installed.
  Rationale: SizeOf is diagnostic and depends on JVM agent lifecycle.
  Source locations: SizeOf.premain, SizeOf.sizeOf, SizeOf.excludeClass.
  Known related CODEX findings: SizeOf shared-reference over/under-count findings.
  Suggested unit tests: testSizeOfWithoutInstrumentationReturnsMinusOne,
  testSizeOfNullReturnsZeroWhenInstrumentationAvailable.
  Spec target section: Lang Utilities / SizeOf Runtime State.

  LANG-STATE-003 — Static Exclusion Lists Must Not Corrupt Size Accounting
  Contract statement: Excluded classes must be consistently omitted from recursive size traversal without causing
  negative or inconsistent totals.
  Rationale: Exclusions are used to avoid shared singleton/cache overcounting.
  Source locations: SizeOf.excludeClass, SizeOf._sizeOf.
  Known related CODEX findings: SizeOf shared-reference pointer subtraction can undercount; object-array reference
  slots can overcount.
  Suggested unit tests: testSizeOfExcludedClassDoesNotAddReferencedObject, testSizeOfRepeatedExcludedReferencesStable.
  Spec target section: Lang Utilities / SizeOf Exclusion Semantics.

  11. Failure / Silent Wrong-Result Contracts

  LANG-FAIL-001 — Array Helpers Must Not Return False Success
  Contract statement: Mutation helpers must not return an apparently successful result that did not perform the
  requested mutation, unless no-op behavior is explicitly documented for the input.
  Rationale: False no-op array mutations can leak listeners, locks, flags, or metadata entries.
  Source locations: OAArray.removeValue, OAArray.insert, OAArray.removeAt, OAArray.reorderToMatch.
  Known related CODEX findings: primitive removeValue no-op; negative insert failure; reorder duplicate/null
  corruption.
  Suggested unit tests: testRemoveValueDoesNotReturnOriginalWhenMatchExists, testInsertNegativePositionDocumented,
  testReorderToMatchPreservesContents.
  Spec target section: Lang Utilities / Array Mutation Failure Semantics.

  LANG-FAIL-002 — Formatting Failures Must Be Consistent Across Overloads
  Contract statement: Equivalent formatting overloads must either all tolerate optional formats or all fail visibly
  under the same documented contract.
  Rationale: OA template/report code selects overloads based on runtime value type.
  Source locations: OAString.format overloads.
  Known related CODEX findings: int/double null-format inconsistency.
  Suggested unit tests: testFormatNullFormatConsistencyAcrossPrimitiveOverloads.
  Spec target section: Lang Utilities / Formatting Failure Semantics.

  LANG-FAIL-003 — SizeOf Must Not Produce Silent Negative Or Directionally Wrong Totals For Normal Graphs
  Contract statement: Recursive size accounting must handle shared references and object arrays consistently;
  diagnostic estimates may be approximate but must not be structurally wrong for normal object graphs.
  Rationale: Size diagnostics inform cache/runtime memory decisions.
  Source locations: SizeOf._sizeOf.
  Known related CODEX findings: shared references subtract defaultSize after contributing zero; object arrays do not
  subtract reference slot size.
  Suggested unit tests: testSizeOfSharedReferenceDoesNotUndercount,
  testSizeOfObjectArrayReferenceAccountingConsistentWithFields.
  Spec target section: Lang Utilities / SizeOf Failure Semantics.

  12. Test Coverage Matrix

  LANG-RUNTIME-001: testLangHelpersAreDeterministicForSameInputs, testStringCaseHelpersAreLocaleStableWhenRequired
  LANG-RUNTIME-002: testArrayRemoveValueActuallyRemovesPrimitiveValue, testReorderToMatchDoesNotLoseDuplicates,
  testSizeOfSharedReferenceAccountingIsStable
  LANG-STRING-001: testOAStringDelegatesTrimSpaces, testOAStrInheritsOAStringBehavior
  LANG-STRING-002: testToStringNullObjectReturnsEmpty, testToStringNullStringUsesFallback,
  testToStringNullByteArrayUsesDocumentedBehavior
  LANG-STRING-003: testCaseInsensitiveCompareIsStableUnderTurkishLocale,
  testUpperLowerHelpersUseDocumentedLocaleSemantics
  LANG-STRING-004: testFormatIntNullFormatUsesConverter, testFormatDoubleNullFormatUsesConverter
  LANG-ARRAY-001: testContainsObjectUsesEquals, testContainsExactUsesReferenceOnly,
  testStringIndexOfHonorsCaseSensitiveFlag
  LANG-ARRAY-002: testAddClassOverloadPreservesExplicitComponentType,
  testRemoveAtClassOverloadPreservesExplicitComponentType
  LANG-ARRAY-003: testRemoveValueIntRemovesFirstMatch, testRemoveValueDoubleRemovesFirstMatch,
  testRemoveValueObjectRemovesFirstEqualsMatch
  LANG-ARRAY-004: testInsertNegativePositionUsesDocumentedBehavior, testInsertAtZeroPrepends,
  testInsertBeyondEndAppendsOrPadsAsDocumented
  LANG-ARRAY-005: testReorderToMatchPreservesDuplicates, testReorderToMatchHandlesNullsAsDocumented
  LANG-NUMERIC-001: testOAIntegerAddSubtractMutatesValue, testOADoubleAddSubtractMutatesValue
  LANG-NUMERIC-002: testOAIntegerConstructorMarksSet, testOADoubleSetMarksSet
  LANG-NUMERIC-003: testOAIntegerAddDoesNotMarkSetUnlessDocumented, testOADoubleAddDoesNotMarkSetUnlessDocumented
  LANG-NUMERIC-004: testGetAsBinaryIntReturns32Chars, testGetAsBinaryLongReturns64Chars
  LANG-TUPLE-001: testTupleStoresConstructorValues, testTuple3StoresConstructorValues, testTupleUsesIdentityEquality
  LANG-TUPLE-002: testTupleAllowsNullValues, testTuple3AllowsNullValues
  LANG-FLAG-001: testOAFlagEnumHasExpectedValues, testOAFlagEnumEitherRepresentsIndeterminateState
  LANG-NULL-001: testOAStringNullConversionContracts, testOAArrayNullSearchContracts, testSizeOfNullReturnsZero
  LANG-EQUAL-001: testIsEqualSameReferenceTrue, testIsEqualSameElementsTrue, testIsEqualNullElementPositionSemantics
  LANG-FORMAT-002: testFormatIntAlignmentFormatUsesTextFormatter, testFormatDoubleNumericFormatUsesConverter,
  testFormatNullFormatConsistencyAcrossPrimitiveOverloads
  LANG-STATE-001: testOAIntegerInstancesAreIndependent, testOADoubleInstancesAreIndependent
  LANG-FAIL-003: testSizeOfSharedReferenceDoesNotUndercount,
  testSizeOfObjectArrayReferenceAccountingConsistentWithFields

*/






