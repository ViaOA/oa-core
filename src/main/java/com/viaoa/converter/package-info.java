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
package com.viaoa.converter;

//CODEX unit tests <todo>


/* CODEX Invariants

CONV-DISPATCH-001 — Target-Driven Converter Dispatch
Contract statement:
Conversion must be selected by the requested target type, not by incidental source type behavior, except when the
source value is already assignable to the requested target and no format-specific conversion is required.
Rationale:
OA uses conversion for property assignment, datasource hydration, query parameters, serialization, display
formatting, and tooling. The requested target type is the semantic contract; dispatch drift can produce values that
look valid but belong to the wrong runtime type or semantic domain.
Source scope:
OAConverter.addConverter, OAConverter.getConverter, OAConverter.convert, OAConv facade methods, all
OAConverterInterface implementations.
Related CODEX findings:
OAConverterEnum.convert can return an enum instance from the wrong enum class.
Suggested unit tests:
converterDispatchUsesRequestedTargetType, assignableValueBypassesConversionOnlyWithoutFormat,
enumConverterRejectsDifferentEnumClass, oaConvDelegatesToOAConverterSemantics.
Spec target section:
Converter Runtime / Dispatch Semantics

CONV-TYPE-001 — Result Type Compatibility
Contract statement:
A successful conversion result must be null or assignable to the requested target type, including primitive-wrapper
targets handled through helper methods.
Rationale:
Returning an incompatible object corrupts OAObject property state, datasource values, query criteria, or
serialization output and can fail later far from the conversion boundary.
Source scope:
OAConverter.convert, OAConverterInterface.convert, OAConverterEnum.convert, OAConverterClass.convert, numeric/
boolean/temporal converter implementations.
Related CODEX findings:
OAConverterEnum.convert can return OtherEnum when TargetEnum was requested.
Suggested unit tests:
convertReturnsAssignableTargetType, primitiveHelperUsesDocumentedWrapperConversion,
enumConverterReturnsOnlyTargetEnumConstants, classConverterReturnsClassOrNull.
Spec target section:
Converter Runtime / Type Semantics

CONV-NULL-001 — Null Input Semantics
Contract statement:
Null input handling must be explicit by conversion family: nullable object targets may return null, display/string
formatting paths return a non-null display string, and primitive helper methods return documented defaults or throw
only for invalid non-null input.
Rationale:
OA distinguishes absent values, empty display text, primitive defaults, and failed conversion. Mixing those meanings
corrupts property state, UI output, query values, and datasource writes.
Source scope:
OAConverter.convert, OAConverter.toString, OAConverter.toInt/toLong/toDouble/toBoolean/toChar, OAConverterString, O
AConverterBoolean, OAConverterNumber, temporal converters.
Related CODEX findings:
VEnum direct convertToString(null) differs from central display normalization.
Suggested unit tests:
nullToStringReturnsEmptyString, nullToNullableTemporalReturnsNull, nullToPrimitiveHelperReturnsDocumentedDefault,
invalidNonNullPrimitiveHelperDoesNotUseNullDefault.
Spec target section:
Converter Runtime / Null and Default Semantics

CONV-EMPTY-001 — Empty And Blank String Semantics
Contract statement:
Empty and blank strings must be handled consistently for each target family: recognized as null-equivalent only
where the target contract says so, trimmed before parsing where whitespace is accepted, and never treated as a
successful unrelated value.
Rationale:
OA receives string values from UI fields, templates, datasource drivers, config, and imports. Whitespace handling
must not cause silent value loss or inconsistent conversion between related types.
Source scope:
OAConverterString.convert, OAConverterBoolean.convert, OAConverterNumber.convert, OAConverterDate, OAConverterTime,
OAConverterTimestamp, OAConverterLocalDate/LocalTime/LocalDateTime, OAConverterZonedDateTime.
Related CODEX findings:
OAConverterTime.convert and OAConverterTimestamp.convert trim strings for the empty check but parse the original
untrimmed value.
Suggested unit tests:
blankStringToNullableTargetReturnsNullByContract, paddedTimeStringParsesTrimmedValue,
paddedTimestampStringParsesTrimmedValue, emptyStringDoesNotBecomeMisleadingNumericOrTemporalValue.
Spec target section:
Converter Runtime / Empty String Semantics

CONV-FAIL-001 — Failed Conversion Visibility
Contract statement:
A conversion that cannot preserve the requested semantic value must return null or throw through a documented helper
path; it must not silently produce a plausible but wrong success value.
Rationale:
Silent false-success can corrupt OAObject properties, query comparisons, datasource values, reports, sync payloads,
and serialized state while appearing valid to callers.
Source scope:
All OAConverterInterface.convert implementations, OAConverter.toXxx helper methods, OAConverterString delegation,
temporal and numeric converters.
Related CODEX findings:
Signed numeric zero boolean conversion, BigDecimal precision loss, Calendar formatting blank output, Date formatting
losing time, ZonedDateTime formatting losing zone.
Suggested unit tests:
invalidStringConversionReturnsNull, helperThrowsForInvalidNonNullInput, failedTemporalParseDoesNotUseCurrentDate,
failedCalendarFormatDoesNotReturnBlankForValidCalendar.
Spec target section:
Converter Runtime / Failure Semantics

CONV-DEFAULT-001 — Defaults Are Intentional
Contract statement:
Default values used by conversion helpers must be target-type intentional and documented; defaults must not hide
invalid non-null input or partial conversion failure.
Rationale:
OA primitive-null support and reflection assignment require clear separation between null, blank, zero, false, and
failed conversion.
Source scope:
OAConverter.toInt/toLong/toShort/toByte/toFloat/toDouble/toBoolean/toChar, OAConverterNumber.convert, OAConverterBo
olean.convert, OAConverterString.convert.
Related CODEX findings:
none.
Suggested unit tests:
nullNumericHelperReturnsZeroByContract, nullBooleanHelperReturnsFalseByContract,
invalidNumericStringDoesNotReturnZeroSilently, invalidBooleanStringDoesNotReturnFalseUnlessContracted.
Spec target section:
Converter Runtime / Default Value Semantics

CONV-NUMERIC-001 — Numeric Value Preservation
Contract statement:
Numeric conversion must preserve the source numeric meaning until the requested target type, formatter, or
documented rounding rule requires narrowing.
Rationale:
OA numeric values can represent money, quantities, IDs, ordering keys, counters, and datasource columns. Accidental
precision loss is data corruption.
Source scope:
OAConverterNumber.convert, OAConverterBigDecimal.convert, OAConverter.toBigDecimal/toBD,
OAConverter.toDouble/toFloat/toLong/toInt, OAConverterNumber.convertToString.
Related CODEX findings:
OAConverterBigDecimal.convert can lose precision by parsing through a generic Number/Double path.
Suggested unit tests:
bigDecimalStringPreservesExactDecimalValue, bigIntegerToBigDecimalPreservesValue,
decimalStringToDoubleNarrowsOnlyAtDoubleBoundary, formattedBigDecimalRoundTripsWithinContract.
Spec target section:
Converter Runtime / Numeric Precision

CONV-NUMERIC-002 — Numeric Parsing Completeness
Contract statement:
Formatted numeric parsing, including grouping separators, currency symbols, and shorthand suffixes, must either
consume the intended numeric representation completely or fail; partial interpretation is not success.
Rationale:
Partial numeric parsing silently changes business values from UI, report, import, or datasource strings.
Source scope:
OAConverterNumber.convert, OAConverterNumber.cleanNumber, OAConverterNumber.getFormatter,
OAConverterBigDecimal.convert.
Related CODEX findings:
Existing package invariant references decimal k/M suffix multiplier behavior.
Suggested unit tests:
numberParsesGroupingAndCurrencyByContract, numberParsesIntegerKiloSuffix, numberParsesDecimalKiloSuffix,
numberRejectsPartialParse.
Spec target section:
Converter Runtime / Numeric Parsing

CONV-ROUND-001 — Rounding And Truncation Boundaries
Contract statement:
Rounding, truncation, scale changes, and integer narrowing must occur only at explicit formatter rules, explicit
helper parameters, or documented target-type boundaries.
Rationale:
OA must distinguish intentional display rounding or target narrowing from accidental loss of precision during
intermediate conversion.
Source scope:
OAConverterNumber.convert, OAConverterNumber.convertToString, OAConverter.toBigDecimal(double,int),
OAConverter.round.
Related CODEX findings:
none.
Suggested unit tests:
doubleToScaledBigDecimalUsesRequestedRounding, integerTargetNarrowsAtTargetBoundary,
numberFormatterAppliesExpectedRounding, roundUsesRequestedDecimalPlaces.
Spec target section:
Converter Runtime / Rounding Semantics

CONV-STRING-001 — Deterministic String Conversion
Contract statement:
Conversion to String for display/runtime formatting must be deterministic and non-null, with explicit charset
behavior for byte-oriented sources.
Rationale:
OA templates, reports, UI bindings, logs, serialization boundaries, and datasource text values depend on stable
textual output across JVMs, servers, and clients.
Source scope:
OAConverter.toString, OAConverterString.convert, OAConverterString.convertToString, all converter convertToString
methods, byte[]/Blob/Clob handling.
Related CODEX findings:
OAConverterString.convert decodes Blob bytes with the platform default charset while byte[] uses UTF-8.
Suggested unit tests:
toStringNeverReturnsNull, byteArrayStringUsesUtf8, blobStringUsesDocumentedCharset, clobStringPreservesContent.
Spec target section:
Converter Runtime / String Semantics

CONV-FORMAT-001 — Format Parameter Semantics
Contract statement:
When a format is supplied, parsing and formatting must interpret that format consistently for the target semantic
type; malformed or incomplete format tokens must fail predictably rather than causing incidental runtime exceptions.
Rationale:
Formats are part of OA’s UI, report, template, datasource, and serialization-facing contract. Inconsistent format
handling produces silent wrong output or hidden runtime failures.
Source scope:
OAConverter.getFormat, OAConverter.toString overloads, OAConverterNumber, OAConverterBoolean, OAConverterString,
OAConverterDate/OADate/OADateTime/OATime-related converters.
Related CODEX findings:
OAConverterBoolean.convert can throw NullPointerException for a one-field custom boolean format.
Suggested unit tests:
booleanCustomFormatRoundTrips, booleanOneFieldFormatReturnsNullForUnrecognizedValue,
dateTimeFormattedStringParsesBack, stringFormatAppliesMaskDeterministically.
Spec target section:
Converter Runtime / Format Contracts

CONV-BOOL-001 — Boolean Vocabulary Semantics
Contract statement:
Boolean conversion must use a defined vocabulary for strings, numbers, characters, and custom true/false/null
formats; unknown values must return null or a documented default, not accidental truth.
Rationale:
Boolean values control flags, filtering, permissions, UI enablement, and datasource state. False allow/deny style
errors can result from wrong coercion.
Source scope:
OAConverterBoolean.convert, OAConverterBoolean.convertToString, OAConverter.toBoolean overloads.
Related CODEX findings:
Signed numeric zero strings can convert to true; one-field custom boolean format can throw NullPointerException.
Suggested unit tests:
booleanNumericZeroIsFalse, booleanSignedZeroIsFalse, booleanNonZeroNumericStringIsTrue,
booleanUnknownCustomFormatReturnsNull, booleanCustomNullTokenFormats.
Spec target section:
Converter Runtime / Boolean Semantics

CONV-TEMPORAL-001 — Temporal Semantic Dimension Preservation
Contract statement:
Date-only, time-only, date-time, instant, local, zoned, SQL temporal, and OA temporal conversions must preserve the
semantic dimension of the requested target; loss of date, time, instant, or zone information is allowed only where
the target type cannot represent it and the rule is documented.
Rationale:
OA uses temporal values in datasource persistence, query criteria, UI display, sorting, sync/replication, and
serialization. Accidental temporal dimension loss changes business meaning.
Source scope:
OAConverterDate, OAConverterCalendar, OAConverterSqlDate, OAConverterTime, OAConverterTimestamp, OAConverterOADate,
OAConverterOADateTime, OAConverterOATime, OAConverterInstant, OAConverterLocalDate, OAConverterLocalDateTime,
OAConverterLocalTime, OAConverterZonedDateTime.
Related CODEX findings:
OAConverterDate.convertToString can clear time by formatting java.util.Date through OADate;
OAConverterLocalTime.convert misses normal time-bearing source types.
Suggested unit tests:
javaUtilDateFormattingPreservesTimeForDateTimeFormat, sqlDateDropsTimeByContract,
localTimeAcceptsOADateTimeAndLocalDateTimeSources, temporalTargetDocumentsDimensionLoss.
Spec target section:
Converter Runtime / Temporal Semantics

CONV-TEMPORAL-002 — Timezone And Zone Identity Semantics
Contract statement:
Instant-bearing and zone-bearing conversions must preserve instants and zone identity where the target type can
represent them; use of system-default timezone must be explicit for local or zone-less sources.
Rationale:
Distributed OA runtimes, sync, replication, datasource conversion, and UI formatting must avoid accidental timezone
drift between servers, clients, and test environments.
Source scope:
OAConverterInstant, OAConverterZonedDateTime, OAConverterZoneId, OAConverterTimeZone, OAConverterLocalDateTime,
OAConverterLocalDate, OAConverterDate, OAConverterTimestamp, OADateTime-based conversions.
Related CODEX findings:
OAConverterZonedDateTime.convertToString can discard the source ZoneId/offset.
Suggested unit tests:
zonedDateTimeFormattingPreservesZoneContract, instantToLocalDateTimeUsesSystemZoneByContract,
zoneIdConverterRejectsUnknownZone, timeZoneConverterRoundTripsKnownZone.
Spec target section:
Converter Runtime / Timezone Semantics

CONV-TEMPORAL-003 — Temporal Binary Numeric Input Semantics
Contract statement:
Numeric and byte-array temporal inputs must be interpreted consistently as epoch milliseconds where that is the
package contract, and unsupported byte encodings must fail predictably rather than throwing incidental low-level
exceptions.
Rationale:
OA datasource, serialization, and replication paths may pass compact numeric representations. Related temporal
converters must not diverge unexpectedly for normal OA values.
Source scope:
OAConverterDate, OAConverterTime, OAConverterTimestamp, OAConverterLocalDateTime, OAConverterLocalTime,
OAConverterZonedDateTime, OAConverterInstant.
Related CODEX findings:
OAConverterLocalDateTime.convert requires exactly 8 bytes via ByteBuffer.getLong while related converters accept
variable-length numeric byte arrays.
Suggested unit tests:
temporalByteArrayEpochMillisParsesConsistently, localDateTimeShortByteArrayFailsPredictably,
temporalNumberEpochMillisUsesDocumentedZone, invalidTemporalBytesDoNotThrowBufferUnderflow.
Spec target section:
Converter Runtime / Temporal Binary Input Semantics

CONV-ENUM-001 — Enum Target Semantics
Contract statement:
Enum conversion must be deterministic by requested enum class, canonical name, or ordinal; unknown values or enum
values from another enum class must return null rather than a wrong enum instance.
Rationale:
Enums encode domain state, workflow state, and generated model metadata. Returning the wrong enum corrupts object
behavior and can break later assignment or serialization.
Source scope:
OAConverterEnum.convert, OAConverterEnum.convertToString, OAConverterVEnum.convert,
OAConverterVEnum.convertToString.
Related CODEX findings:
OAConverterEnum.convert can accept any Enum instance.
Suggested unit tests:
enumNameMatchesIgnoringCase, enumOrdinalMatchesTargetEnumOnly, enumRejectsDifferentEnumClass,
enumUnknownStringReturnsNull, enumToStringUsesCanonicalName.
Spec target section:
Converter Runtime / Enum Semantics

CONV-CLASS-001 — Class Resolution Semantics
Contract statement:
Class conversion must resolve the intended class name through supported class-loading rules or fail as null/visible
failure; it must not silently resolve to an unrelated class identity.
Rationale:
Class values drive OA metadata, reflection, code generation, runtime routing, and serialization compatibility. Class
identity drift can corrupt metadata and object graph behavior.
Source scope:
OAConverterClass.convert, OAConverterClass.convertToString, OAConverter.convert(Class.class,...).
Related CODEX findings:
none.
Suggested unit tests:
classConverterRoundTripsClassName, classConverterReturnsNullForUnknownClass,
classConverterUsesSupportedClassLoaderPath, classConverterDoesNotResolveWrongClass.
Spec target section:
Converter Runtime / Class Semantics

CONV-SPECIAL-001 — Specialized Value Representation Semantics
Contract statement:
Specialized converters for Character, Calendar, TimeZone, ZoneId, VEnum, and similar domain-adjacent values must
preserve their documented semantic representation and return null or an empty display string only according to the
target/display contract.
Rationale:
These values often bridge runtime metadata, UI display, scheduling, and temporal configuration. Blank or fallback
output for a populated value is silent wrong behavior.
Source scope:
OAConverterCharacter, OAConverterCalendar, OAConverterTimeZone, OAConverterZoneId, OAConverterVEnum,
OAConverterString delegation paths.
Related CODEX findings:
OAConverterCalendar.convertToString can return blank for normal Calendar values; VEnum direct convertToString(null)
differs from central display normalization.
Suggested unit tests:
calendarToStringFormatsPopulatedCalendar, characterConverterHandlesStringAndNumberInputs,
timeZoneConverterFormatsKnownZone, vEnumToStringNullBehaviorMatchesContract.
Spec target section:
Converter Runtime / Specialized Type Semantics

CONV-ROUNDTRIP-001 — Parse/Format Round Trip Stability
Contract statement:
Where OA relies on reversible text representation, convertToString output must parse back to the same semantic value
under the same target type and format, subject only to documented precision, timezone, or target-type loss.
Rationale:
UI fields, templates, reports, datasource string columns, serialization text, and generated tooling often depend on
stable round trips.
Source scope:
OAConverterNumber, OAConverterBigDecimal, OAConverterBoolean, OAConverterEnum, OAConverterClass, OAConverterDate/
time converters, OAConverterZoneId, OAConverterTimeZone.
Related CODEX findings:
ZonedDateTime formatting can lose zone; java.util.Date formatting can lose time; BigDecimal string path can lose
precision.
Suggested unit tests:
numberRoundTripWithFormat, bigDecimalRoundTripPreservesPrecision, booleanRoundTripWithFormat, enumRoundTripByName,
dateTimeRoundTripWithFormat, zonedDateTimeRoundTripPreservesContract.
Spec target section:
Converter Runtime / Round-Trip Semantics

CONV-HELPER-001 — Convenience API Consistency
Contract statement:
OAConv and OAConverter helper methods must preserve the same conversion semantics as the registered converter
implementations, except for documented primitive-helper defaults and exception behavior.
Rationale:
OA code uses both direct converter classes and convenience facades. Divergent behavior creates package-dependent
conversion results for the same value.
Source scope:
OAConv, OAConverter.convert, OAConverter.toString, OAConverter.toXxx helper methods, all registered converter
implementations.
Related CODEX findings:
VEnum direct convertToString(null) differs from central toString normalization.
Suggested unit tests:
oaConvMatchesOAConverterForCoreTypes, directConverterAndCentralConverterAgreeForStringFormatting,
primitiveHelperExceptionSemanticsAreDocumented, helperUsesRegisteredConverterForTarget.
Spec target section:
Converter Runtime / Facade Consistency

CONV-REGISTRY-001 — Converter Registry Consistency
Contract statement:
Converter registration and lookup must publish a coherent converter mapping for each target type, including
superclass lookup, without exposing partially registered or stale converter state.
Rationale:
Converters are global runtime infrastructure used across OA packages. Registry drift can make identical conversion
requests behave differently across threads or runtime phases.
Source scope:
OAConverter.hmClassConverter, OAConverter.addConverter, OAConverter.getConverter, static converter initialization.
Related CODEX findings:
none.
Suggested unit tests:
registeredConverterIsImmediatelyVisible, subclassLookupFindsSuperclassConverter,
replacedConverterIsUsedDeterministically, concurrentConverterLookupSeesCoherentMapping.
Spec target section:
Converter Runtime / Registry Semantics

CONV-THREAD-001 — Shared Converter Thread Safety
Contract statement:
Shared parser, formatter, registry, and converter state must be immutable, safely published, synchronized, pooled
with exclusive ownership, or method-local.
Rationale:
Converters are global utilities used by UI, datasource, serialization, sync, queue, schedule, remote, and background
threads. Formatter or registry races can produce wrong values or intermittent failures.
Source scope:
OAConverter registry and global format fields, OAConverterNumber.FormatPool/getFormatter/releaseFormatter, DecimalF
ormat use, date/time formatter use, static converter instances.
Related CODEX findings:
none.
Suggested unit tests:
numberFormatterConcurrentParseIsStable, numberFormatterConcurrentFormatIsStable,
converterRegistryConcurrentLookupIsStable, temporalConcurrentFormatParseIsStable.
Spec target section:
Converter Runtime / Thread Safety

CONV-DETERMINISM-001 — Same Inputs Produce Same Observable Result
Contract statement:
For the same target type, source value, format, registered converter state, locale/timezone assumptions, and helper
path, conversion must produce the same observable result or the same visible failure.
Rationale:
OA comparison, query binding, datasource persistence, serialization, generated tooling, and UI display require
deterministic conversion behavior for repeatable runtime correctness.
Source scope:
All classes in com.viaoa.converter, especially OAConverter, OAConv, OAConverterNumber, OAConverterBoolean,
OAConverterString, and all temporal converters.
Related CODEX findings:
Platform-default Blob decoding and timezone-discarding ZonedDateTime formatting can make results environment-
dependent.
Suggested unit tests:
sameInputSameOutputAcrossRepeatedCalls, blobTextUsesStableCharset, timezoneSensitiveConversionUsesDocumentedZone,
converterFailureIsRepeatable.
Spec target section:
Converter Runtime / Determinism

CONV-INTEGRATION-001 — Runtime Integration Boundaries
Contract statement:
Converter behavior must remain compatible with OAObject property assignment, Hub/UI display, query/select comparison
values, datasource hydration/persistence, serialization, config, logging, sync/replication payloads, and generated
model/tooling expectations.
Rationale:
The converter package is a shared semantic boundary. A conversion result that is locally plausible but incompatible
with downstream OA packages can corrupt object state or runtime decisions.
Source scope:
OAConverter, OAConv, all OAConverterInterface implementations, package-level default format APIs.
Related CODEX findings:
Precision loss, temporal dimension loss, timezone loss, enum target mismatch, and charset drift all illustrate
cross-package runtime risk.
Suggested unit tests:
converterValueCanAssignToOAObjectProperty, datasourceStringValueConvertsToExpectedPropertyType,
queryParameterConversionPreservesComparisonIntent, serializationTextRoundTripPreservesSemanticValue.
Spec target section:
Converter Runtime / Cross-Package Contracts

*/


