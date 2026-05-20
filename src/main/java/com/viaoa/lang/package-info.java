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

LANG-RUNTIME-001 — Foundational Runtime Helpers
Contract statement:
com.viaoa.lang defines OA-specific language/runtime support helpers used by higher-level OA packages for strings,
arrays, mutable numeric carriers, tuple carriers, tri-state flags, and diagnostic object sizing.
Rationale:
These helpers are low-level runtime dependencies for object, Hub, graph, metadata, datasource, query, path,
serialization, sync, replication, template, filter, and UI/tooling behavior.
Source scope:
OAString, OAStr, OAArray, OAInteger, OADouble, OAFlagEnum, Tuple, Tuple3, SizeOf, package-info.java.
Related CODEX findings:
Existing package-info notes deterministic helper behavior, silent wrong-result prevention, locale sensitivity, array
mutation issues, and SizeOf diagnostic risks.
Suggested unit tests:
testLangHelpersAreDeterministicForSameInputs(), testLangHelpersDoNotHideWrongResultsAsSuccess(),
testLangContractsRemainStableForRuntimeCallers()
Spec target section:
Lang Runtime / Core Responsibility

LANG-DETERMINISM-001 — Deterministic Helper Results
Contract statement:
For the same inputs and documented runtime configuration, lang helpers must return deterministic results without
hidden state changes, except for explicitly mutable wrapper instances and documented global diagnostic state.
Rationale:
Higher-level OA runtime systems rely on these helpers inside metadata, path/query, formatting, filtering,
reflection, serialization, and generated code paths.
Source scope:
OAString, OAStr, OAArray, OAInteger, OADouble, Tuple, Tuple3, OAFlagEnum, SizeOf.
Related CODEX findings:
Locale-sensitive case folding and SizeOf shared-reference accounting are noted in existing package-info.
Suggested unit tests:
testLangHelpersAreDeterministicForSameInputs(), testStringCaseHelpersAreLocaleStableWhenRequired(),
testSizeOfSharedReferenceAccountingIsStable()
Spec target section:
Lang Runtime / Deterministic Behavior

LANG-FAIL-001 — Silent Wrong-Result Prevention
Contract statement:
A lang helper must either return the correct documented result, return the documented no-op/no-result value, or fail
visibly; it must not silently return an unchanged, partial, or misleading value for normal OA usage.
Rationale:
Silent helper failures propagate into object graph, filtering, reflection, array listener lists, metadata lists, and
serialization behavior.
Source scope:
OAArray.removeValue(...), OAArray.reorderToMatch(...), OAString.format(...), SizeOf.sizeOf(...), delegated text
helpers.
Related CODEX findings:
Primitive removeValue can return unchanged arrays; reorderToMatch can lose duplicate/null elements; SizeOf over/
under-count findings.
Suggested unit tests:
testArrayRemoveValueActuallyRemovesPrimitiveValue(), testReorderToMatchDoesNotLoseDuplicates(),
testSizeOfSharedReferenceAccountingIsStable()
Spec target section:
Lang Runtime / Failure Semantics

LANG-NULL-001 — Null Input Contracts
Contract statement:
Lang helpers that accept null inputs must return documented safe values such as null, empty string, false, -1, zero,
unchanged input, or visible failure according to the API contract.
Rationale:
OA generated and runtime code frequently passes optional values through low-level helpers.
Source scope:
OAString.toString(...), fmt(...), notNull(...), notEmpty(...), OAArray.contains(...), indexOf(...), removeAt(...),
add(...), SizeOf.sizeOf(...).
Related CODEX findings:
OAString.toString(byte[]) null path; OAArray.add(T[],T...) null varargs path; OAArray.removeValue null search
contract concern.
Suggested unit tests:
testOAStringNullConversionContracts(), testOAArrayNullSearchContracts(), testSizeOfNullReturnsZero()
Spec target section:
Lang Runtime / Null Handling

LANG-EMPTY-001 — Empty Value Semantics
Contract statement:
Empty strings, empty arrays, empty collections, and blank-like values must have documented behavior distinct from
null where the API exposes that distinction.
Rationale:
OA query, template, UI, metadata, listener, and generated-code paths frequently distinguish null, empty, and blank
values.
Source scope:
OAString.isEmpty(...), isNotEmpty(...), notEmpty(...), notNull(...), field/count helpers, OAArray.contains(...),
indexOf(...), removeAt(...), removeValue(...), hasNull(...).
Related CODEX findings:
Existing package-info notes empty array no-op/empty-result expectations.
Suggested unit tests:
testOAStringNullEmptyBlankSemantics(), testRemoveAtEmptyArrayReturnsSameArray(),
testIndexOfEmptyArrayReturnsMinusOne(), testHasNullEmptyArrayReturnsFalse()
Spec target section:
Lang Runtime / Empty Value Semantics

LANG-STRING-001 — OAString Facade Stability
Contract statement:
OAString and OAStr are stable OA string helper facades and must preserve delegated com.viaoa.text semantics without
adding divergent runtime behavior except where explicitly documented.
Rationale:
Legacy, generated, and runtime OA code use OAString/OAStr as central text utility entry points.
Source scope:
OAString, OAStr, delegated OAText* classes.
Related CODEX findings:
Existing package-info notes delegated text package findings apply to facade behavior.
Suggested unit tests:
testOAStringDelegatesTrimSpaces(), testOAStrInheritsOAStringBehavior(), testOAStringFmtMatchesOATextFormatFmt()
Spec target section:
Lang Runtime / String Facade Semantics

LANG-STRING-002 — Locale-Stable Matching Text
Contract statement:
Case conversion and case-insensitive matching used for lookup, filtering, comparison, search, or generated runtime
behavior must use locale-stable semantics unless an API explicitly declares locale-sensitive behavior.
Rationale:
OA server/client/filter behavior must not change under Turkish or other JVM default locales.
Source scope:
OAString.upper(...), lower(...), toUpperCase(...), toLowerCase(...), indexOf(... ignoreCase), contains(...
ignoreCase), startsWith(... ignoreCase), endsWith(... ignoreCase), filter/search users.
Related CODEX findings:
OAString/filter behavior notes locale-sensitive case folding risk.
Suggested unit tests:
testCaseInsensitiveCompareIsStableUnderTurkishLocale(), testUpperLowerHelpersUseDocumentedLocaleSemantics(),
testContainsIgnoreCaseLocaleStable()
Spec target section:
Lang Runtime / Locale-Stable Text Semantics

LANG-STRING-003 — String Transformation Semantics
Contract statement:
String transformation helpers must preserve documented character, delimiter, escaping, sanitization, display-name,
filename, XML/HTML/JSON, Java identifier, and path-building semantics without character loss unless explicitly
contracted.
Rationale:
These helpers prepare runtime-visible text for metadata, UI, serialization, query/path/template support, and
generated code.
Source scope:
OAString convert/remove/accept/strip/mask/display/plural/title/filename/xml/html/json/java-identifier/property-path
helpers.
Related CODEX findings:
Existing package-info delegates many transformation concerns to text package contracts.
Suggested unit tests:
testXmlHtmlJsonEscapeRoundTripByContract(), testJavaIdentifierGenerationProducesLegalIdentifier(),
testCreatePropertyPathUsesDeterministicSeparator()
Spec target section:
Lang Runtime / String Transformation Semantics

LANG-STRING-004 — String Formatting Facade
Contract statement:
OAString formatting helpers must preserve OA converter/text formatting semantics for string, numeric, date/time,
boolean, alignment, null-format, and empty-format cases.
Rationale:
Templates, display rendering, generated code, and reflection formatting depend on one stable formatting contract.
Source scope:
OAString.format(long,String), format(int,String), format(double,String), format(boolean,String),
format(OADateTime,String), format(OADate), format(String,String), pickFormat(...), fmt(...).
Related CODEX findings:
format(int,null) and format(double,null) inconsistency reported in existing package-info.
Suggested unit tests:
testFormatIntNullFormatUsesConverter(), testFormatDoubleNullFormatUsesConverter(),
testFormatAlignmentMarkersUseTextFormatter()
Spec target section:
Lang Runtime / Formatting Semantics

LANG-STRING-005 — Parsing Facade Semantics
Contract statement:
OAString parsing helpers must preserve delegated tokenizer, field, CSS, separator, quote, index, contains, and count
semantics without false progress or silent character loss.
Rationale:
Parsing helpers are compatibility entry points for templates, configuration, UI, generated code, and low-level
runtime text handling.
Source scope:
OAString.parseLine(...), getCssMap(...), getCSSMap(...), field(...), fieldAt(...), count(...), countMatches(...),
dcount(...), indexOf(...), contains(...).
Related CODEX findings:
Text package tokenizer and parsing contracts apply through OAString facade.
Suggested unit tests:
testOAStringParseLineDelegatesTokenizerSemantics(), testOAStringGetCssMapDelegatesTokenizerSemantics(),
testFieldAndCountSeparatorSemantics()
Spec target section:
Lang Runtime / Parsing Semantics

LANG-ARRAY-001 — Array Search Semantics
Contract statement:
Array search helpers must explicitly define and honor whether they use reference equality, equals equality, case-
sensitive string equality, or case-insensitive string equality.
Rationale:
OA uses arrays for listener lists, metadata lists, locks, callbacks, trigger lists, and runtime helper state.
Source scope:
OAArray.contains(...), containsExact(...), indexOf(...), indexOf(String[], String, boolean), related overloads.
Related CODEX findings:
contains(String[],...,bCaseSensitive) and indexOf(String[],...,bCaseSensitive) ignore bCaseSensitive.
Suggested unit tests:
testContainsObjectUsesEquals(), testContainsExactUsesReferenceOnly(), testStringIndexOfHonorsCaseSensitiveFlag()
Spec target section:
Lang Runtime / Array Search Semantics

LANG-ARRAY-002 — Array Component Type Preservation
Contract statement:
Array add, insert, remove, and copy helpers must preserve the intended runtime component type, especially when an
explicit Class parameter is supplied.
Rationale:
OA callers often cast returned arrays back to listener, metadata, Hub, trigger, lock, callback, or generated helper
array types.
Source scope:
OAArray.add(Class,Object[],Object), add(Class,Object[],Object...), add(T[],T), add(T[],T...), insert(...),
removeAt(...), removeValue(...).
Related CODEX findings:
Explicit-class overloads can ignore the Class argument; null-array typed add can infer overly narrow subtype.
Suggested unit tests:
testAddClassOverloadPreservesExplicitComponentType(), testRemoveAtClassOverloadPreservesExplicitComponentType(),
testTypedAddNullArraySubtypeBehaviorIsDocumented()
Spec target section:
Lang Runtime / Array Type Semantics

LANG-ARRAY-003 — Array Mutation Semantics
Contract statement:
Array mutation helpers must remove, insert, append, or replace exactly the documented element(s), preserve remaining
order unless reordering is requested, and return unchanged input only when no mutation is contracted.
Rationale:
False no-op mutation leaks listeners, locks, flags, or metadata entries and can corrupt runtime callback/event
behavior.
Source scope:
OAArray.removeValue(...), removeAt(...), add(...), insert(...), add(String[],String), primitive array helpers.
Related CODEX findings:
Primitive removeValue methods never assign pos; null object removal is a contract concern; negative insert position
can throw.
Suggested unit tests:
testRemoveValueIntRemovesFirstMatch(), testRemoveValueDoubleRemovesFirstMatch(),
testRemoveValueObjectRemovesFirstEqualsMatch(), testInsertNegativePositionUsesDocumentedBehavior()
Spec target section:
Lang Runtime / Array Mutation Semantics

LANG-ARRAY-004 — Array Reorder Preserves Contents
Contract statement:
reorderToMatch must preserve the multiset contents of the array being reordered, including duplicates and nulls
according to documented null-matching semantics.
Rationale:
Reordering helpers must change order without losing, duplicating, or nulling out runtime elements.
Source scope:
OAArray.reorderToMatch(...).
Related CODEX findings:
reorderToMatch can throw on null elements and can map duplicate equal elements to the same target slot, leaving null
holes.
Suggested unit tests:
testReorderToMatchPreservesDuplicates(), testReorderToMatchHandlesNullsAsDocumented(),
testReorderToMatchNoMatchLeavesOriginalUnchanged()
Spec target section:
Lang Runtime / Array Reorder Semantics

LANG-ARRAY-005 — Array Equality Semantics
Contract statement:
OAArray.isEqual must compare arrays by length and pairwise reference/equals semantics, with null elements matching
only null elements at the same position.
Rationale:
Array equality is used for keys, parameters, metadata arrays, and runtime helper comparisons.
Source scope:
OAArray.isEqual(...).
Related CODEX findings:
None observed.
Suggested unit tests:
testIsEqualSameReferenceTrue(), testIsEqualSameElementsTrue(), testIsEqualDifferentLengthFalse(),
testIsEqualNullElementPositionSemantics()
Spec target section:
Lang Runtime / Array Equality Semantics

LANG-NUMERIC-001 — Mutable Numeric Carriers
Contract statement:
OAInteger and OADouble provide instance-local mutable numeric values for by-reference accumulation, counters, and
callback state.
Rationale:
OA callbacks and inner classes use these wrappers to carry numeric state across invocation boundaries.
Source scope:
OAInteger constructors and get/set/add/subtract/isSet methods; OADouble constructors and get/set/add/subtract/isSet
methods.
Related CODEX findings:
None observed.
Suggested unit tests:
testOAIntegerAddSubtractMutatesValue(), testOADoubleAddSubtractMutatesValue(),
testOAIntegerInstancesAreIndependent()
Spec target section:
Lang Runtime / Numeric Wrapper Semantics

LANG-NUMERIC-002 — Explicit Set-State
Contract statement:
Constructing a numeric wrapper with a value or calling set must mark the wrapper as explicitly set; arithmetic
accumulation must either preserve current set-flag semantics or explicitly document that it changes set-state.
Rationale:
Callers use isSet() to distinguish default zero from explicitly assigned zero.
Source scope:
OAInteger(int), OAInteger.set(...), OAInteger.add/subtract(...), OAInteger.isSet(), OADouble(double), OADouble.set(
...), OADouble.add/subtract(...), OADouble.isSet().
Related CODEX findings:
Existing package-info notes arithmetic currently does not mark set and this must be contractual.
Suggested unit tests:
testOAIntegerConstructorMarksSet(), testOADoubleSetMarksSet(), testOAIntegerAddDoesNotMarkSetUnlessDocumented(),
testOADoubleAddDoesNotMarkSetUnlessDocumented()
Spec target section:
Lang Runtime / Numeric Set-State Semantics

LANG-NUMERIC-003 — Binary Rendering Width
Contract statement:
OAInteger.getAsBinary(int) must return exactly 32 bits and OAInteger.getAsBinary(long) must return exactly 64 bits.
Rationale:
These helpers are intended for deterministic numeric bit debugging.
Source scope:
OAInteger.getAsBinary(int), OAInteger.getAsBinary(long).
Related CODEX findings:
Obsolete viewBytes no-op was reported and later treated as testing-only.
Suggested unit tests:
testGetAsBinaryIntReturns32Chars(), testGetAsBinaryLongReturns64Chars()
Spec target section:
Lang Runtime / Numeric Binary Semantics

LANG-TUPLE-001 — Tuple Carrier Semantics
Contract statement:
Tuple and Tuple3 are immutable constructor-value carriers with final public fields and no value-key equality/hash
semantics beyond object identity.
Rationale:
They are lightweight internal carriers, not general-purpose value-key objects.
Source scope:
Tuple, Tuple3.
Related CODEX findings:
None observed.
Suggested unit tests:
testTupleStoresConstructorValues(), testTuple3StoresConstructorValues(), testTupleUsesIdentityEquality()
Spec target section:
Lang Runtime / Tuple Carrier Semantics

LANG-TUPLE-002 — Tuple Null Values
Contract statement:
Tuple and Tuple3 fields may hold null values without throwing during construction or field access.
Rationale:
OA uses tuples to carry optional paired or grouped runtime state.
Source scope:
Tuple(A,B), Tuple3(A,B,C).
Related CODEX findings:
None observed.
Suggested unit tests:
testTupleAllowsNullValues(), testTuple3AllowsNullValues()
Spec target section:
Lang Runtime / Tuple Null Semantics

LANG-FLAG-001 — Tri-State Flag Enum
Contract statement:
OAFlagEnum must represent exactly false, true, and either/indeterminate states, and callers must not infer
additional ordering or boolean conversion semantics unless explicitly added.
Rationale:
OA APIs sometimes need tri-state behavior without nullable booleans.
Source scope:
OAFlagEnum.False, OAFlagEnum.True, OAFlagEnum.Either.
Related CODEX findings:
None observed.
Suggested unit tests:
testOAFlagEnumHasExpectedValues(), testOAFlagEnumEitherRepresentsIndeterminateState(),
testOAFlagEnumHasNoExtraValues()
Spec target section:
Lang Runtime / Flag Semantics

LANG-SIZEOF-001 — Instrumentation Boundary
Contract statement:
SizeOf must report object sizing only when JVM Instrumentation is installed; without instrumentation, callers must
receive the documented unavailable result rather than a fabricated size.
Rationale:
SizeOf is diagnostic and depends on JVM agent lifecycle, not normal object graph runtime semantics.
Source scope:
SizeOf.premain(...), SizeOf.sizeOf(...).
Related CODEX findings:
Existing package-info notes SizeOf without instrumentation returns unavailable and shared-reference accounting
risks.
Suggested unit tests:
testSizeOfWithoutInstrumentationReturnsMinusOne(), testSizeOfNullReturnsZeroWhenInstrumentationAvailable(),
testSizeOfUnavailableDoesNotReportFakeSize()
Spec target section:
Lang Runtime / SizeOf Instrumentation

LANG-SIZEOF-002 — Size Traversal Accounting
Contract statement:
SizeOf reference traversal must use deterministic exclusion, default-size, primitive, array, shared-reference, and
cycle accounting semantics.
Rationale:
Diagnostic size values must not overcount or undercount shared/cyclic structures unpredictably.
Source scope:
SizeOf.sizeOf(Object, boolean), excludeClass(...), internal traversal/exclusion state.
Related CODEX findings:
SizeOf shared-reference over/under-count findings; static exclusion/default-size state noted.
Suggested unit tests:
testSizeOfSharedReferenceCountedByContract(), testSizeOfCycleDoesNotRecurseForever(),
testSizeOfExcludedClassUsesDocumentedBehavior()
Spec target section:
Lang Runtime / SizeOf Traversal

LANG-STATE-001 — Static State Visibility
Contract statement:
Any static or shared mutable state in lang helpers must be documented as global diagnostic/configuration state,
safely published, or explicitly not thread-safe.
Rationale:
Lang helpers are widely shared by runtime packages and generated code.
Source scope:
SizeOf instrumentation/exclusion state, OAString.NL, static helper methods, mutable numeric wrapper instance state.
Related CODEX findings:
Existing package-info notes SizeOf static instrumentation state and mutable wrapper instance-local state.
Suggested unit tests:
testSizeOfStaticExclusionStateIsGlobalByContract(), testMutableWrappersDoNotShareState(),
testStaticHelpersDoNotMutateHiddenRuntimeState()
Spec target section:
Lang Runtime / Shared State Semantics

LANG-CONCURRENT-001 — Thread-Safety Boundary
Contract statement:
Stateless/static helper methods must be safe for concurrent use under their documented state model, while mutable
wrappers and diagnostic global state must be treated according to their explicit ownership contract.
Rationale:
Lang helpers are called from UI, background workers, sync, replication, query, path, serialization, and datasource
code.
Source scope:
OAString, OAStr, OAArray, OAInteger, OADouble, Tuple, Tuple3, OAFlagEnum, SizeOf.
Related CODEX findings:
SizeOf global state and mutable wrapper state noted; no broad hidden ThreadLocal state observed.
Suggested unit tests:
testConcurrentOAStringCallsAreDeterministic(), testConcurrentOAArrayPureHelpersAreDeterministic(),
testMutableWrapperConcurrentUseRequiresOwnerSynchronization()
Spec target section:
Lang Runtime / Concurrency

LANG-COMPAT-001 — Legacy API Compatibility
Contract statement:
Legacy spelling aliases, facade methods, and compatibility entry points must preserve established OA semantics or
explicitly redirect to the canonical helper contract.
Rationale:
Generated and legacy OA code depends on stable helper names such as OAStr, Hungarian/display helpers, Java
identifier aliases, and formatting/parsing facades.
Source scope:
OAStr, OAString alias methods including fmt, trunc, mfcl, mfcu, makeJavaIndentifier, convertHungarian, HTML/XML case
aliases, defaultString/notNull helpers.
Related CODEX findings:
Existing package-info treats OAString/OAStr as stable compatibility facades.
Suggested unit tests:
testOAStrCompatibilityFacadeMatchesOAString(), testLegacyAliasMatchesCanonicalMethod(),
testMisspelledJavaIdentifierAliasPreservesContract()
Spec target section:
Lang Runtime / Compatibility Semantics

LANG-INTEGRATION-001 — Cross-Package Lang Compatibility
Contract statement:
Lang helper behavior must remain compatible with reflect, converter, text, object, Hub, graph, metadata, query,
path, datasource, serialization, sync, replication, template, filter, find, and trigger contracts.
Rationale:
The package provides foundational runtime behavior. A silent change in helper semantics can alter executable
blueprint interpretation and live graph behavior across OA.
Source scope:
com.viaoa.lang.*, delegated text/converter/compare integrations, consumers across OA runtime packages.
Related CODEX findings:
Existing package-info maps lang risks to text, filter, SizeOf, array/listener, metadata, and runtime helper use.
Suggested unit tests:
testLangStringFacadeCompatibleWithTextContracts(), testLangArrayHelpersCompatibleWithListenerMetadataUse(),
testLangHelpersDoNotBreakPathQueryTemplateContracts()
Spec target section:
Lang Runtime / Cross-Package Integration

*/