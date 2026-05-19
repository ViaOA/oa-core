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

/* CODEX Invariants

Converter Invariants

  ID: CONV-TYPE-001
  Contract statement: conversion result type must be assignable to the requested target type, including primitive-
  wrapper targets.
  Rationale: OA uses converters for property assignment, datasource hydration, comparison, and serialization;
  returning the wrong type can corrupt object state or fail later.
  Source locations: OAConverter.convert, OAConverter.getConverter, all OAConverterInterface.convert implementations.
  Related CODEX findings: enum converter returning an enum from the wrong enum class.
  Suggested unit tests: convertReturnsRequestedTargetType, convertPrimitiveTargetUsesWrapper,
  enumConverterRejectsDifferentEnumType
  Spec target section: Converter Runtime / Type Dispatch

  ID: CONV-TYPE-002
  Contract statement: converter lookup must prefer the explicit target converter and only fall back to assignable
  source values when no conversion is needed.
  Rationale: source-type-driven fallback can bypass target-specific semantics and produce misleading values.
  Source locations: OAConverter.getConverter, OAConverter.convert, OAConverterString.convert.
  Related CODEX findings: none.
  Suggested unit tests: converterLookupUsesTargetClass, assignableValueBypassesOnlyWhenNoFormat,
  stringConverterDelegatesToSourceConverterForFormatting
  Spec target section: Converter Runtime / Dispatch Semantics

  ID: CONV-NULL-001
  Contract statement: null input behavior must be explicit per target type: object-like targets may return null,
  display-string conversion returns "", and primitive helper methods return documented primitive defaults or throw on
  invalid non-null input.
  Rationale: OA distinguishes “missing value,” “empty display text,” and primitive default values during reflection/
  property assignment.
  Source locations: OAConverter.convert, OAConverter.toString, OAConverter.toInt, OAConverter.toBoolean,
  OAConverterNumber.convert, OAConverterBoolean.convert, temporal converters.
  Related CODEX findings: none.
  Suggested unit tests: nullToStringReturnsBlank, nullToObjectDateReturnsNull, nullToPrimitiveHelperReturnsDefault,
  invalidNonNullPrimitiveHelperThrows
  Spec target section: Converter Runtime / Null Semantics

  ID: CONV-FAIL-001
  Contract statement: failed conversion must return null or throw a caller-visible exception; it must not produce a
  plausible but wrong success value.
  Rationale: silent false success can corrupt OAObject properties, query values, datasource values, and serialized
  state.
  Source locations: all convert methods; OAConverter.toXxx helper methods.
  Related CODEX findings: signed numeric zero boolean conversion, BigDecimal precision loss, Calendar formatting blank
  output.
  Suggested unit tests: invalidStringConversionReturnsNull, helperThrowsForInvalidNonNullInput,
  failedTemporalParseDoesNotUseDefaultDate
  Spec target section: Converter Runtime / Failure Semantics

  ID: CONV-NUMERIC-001
  Contract statement: numeric conversion must preserve source numeric meaning until the requested target type requires
  a narrowing boundary.
  Rationale: OA numeric values can represent money, quantities, IDs, sort keys, and datasource columns.
  Source locations: OAConverterNumber.convert, OAConverterBigDecimal.convert, OAConverter.toBigDecimal.
  Related CODEX findings: BigDecimal string parsing through generic Number can lose precision.
  Suggested unit tests: bigDecimalStringPreservesPrecision, bigIntegerToBigDecimalPreservesValue,
  decimalStringToDoubleUsesExpectedPrecisionBoundary
  Spec target section: Converter Runtime / Numeric Precision

  ID: CONV-NUMERIC-002
  Contract statement: numeric shorthand, grouping, currency, and format parsing must either produce the intended
  numeric value or fail; partial numeric interpretation is not success.
  Rationale: UI/report/import inputs often use formatted numbers; wrong parsing silently changes business values.
  Source locations: OAConverterNumber.convert, OAConverterNumber.cleanNumber, OAConverterNumber.getFormatter.
  Related CODEX findings: decimal k/M suffix multiplier wrong.
  Suggested unit tests: numberCleanParsesIntegerKiloSuffix, numberCleanParsesDecimalKiloSuffix,
  numberRejectsPartialParse
  Spec target section: Converter Runtime / Numeric Parsing

  ID: CONV-NUMERIC-003
  Contract statement: rounding and truncation must be explicit, formatter-driven, or target-type-driven.
  Rationale: OA must distinguish intentional narrowing from accidental precision loss.
  Source locations: OAConverterNumber.convert, OAConverterNumber.convertToString,
  OAConverter.toBigDecimal(double,int).
  Related CODEX findings: none.
  Suggested unit tests: doubleToScaledBigDecimalUsesHalfUp, integerTargetNarrowsAtTargetBoundary,
  numberFormatterUsesHalfUpRounding
  Spec target section: Converter Runtime / Rounding Semantics

  ID: CONV-STRING-001
  Contract statement: conversion to String must be deterministic and non-null for display/runtime formatting paths.
  Rationale: OA templates, reports, UI bindings, logs, and serialization expect stable textual values.
  Source locations: OAConverter.toString, OAConverterString.convert, every convertToString.
  Related CODEX findings: VEnum.convertToString(null) returns null but central toString normalizes it; Blob default
  charset mismatch.
  Suggested unit tests: toStringNeverReturnsNull, byteArrayStringUsesUtf8, blobStringUsesDocumentedCharset
  Spec target section: Converter Runtime / String Semantics

  ID: CONV-FORMAT-001
  Contract statement: when a format is supplied, formatting and parsing must interpret that format consistently for
  the target semantic type.
  Rationale: OA relies on formats for UI input/output, templates, reports, and datasource string values.
  Source locations: OAConverterNumber, OAConverterBoolean, OAConverterString, date/time converters.
  Related CODEX findings: boolean one-field custom format NPE; trimmed string parsed untrimmed in SQL time/timestamp
  converters.
  Suggested unit tests: booleanCustomFormatRoundTrips, dateTimeFormattedStringParsesBack,
  timeConverterParsesTrimmedString
  Spec target section: Converter Runtime / Format Contracts

  ID: CONV-DATE-001
  Contract statement: date-only, time-only, date-time, and instant conversions must preserve their intended semantic
  dimension.
  Rationale: OA has distinct semantic types; mixing them incorrectly causes persisted values, comparisons, and UI
  output to drift.
  Source locations: OAConverterDate, OAConverterSqlDate, OAConverterTime, OAConverterTimestamp, OAConverterOADate,
  OAConverterOADateTime, OAConverterOATime, Java time converters.
  Related CODEX findings: java.util.Date.convertToString clears time through OADate; LocalDate/LocalTime missing
  normal temporal source types.
  Suggested unit tests: dateFormattingPreservesTimeWhenDateTimeFormatUsed, sqlDateDropsTimeByContract,
  localDateAcceptsDateTimeSources, localTimeAcceptsDateTimeSources
  Spec target section: Converter Runtime / Temporal Semantics

  ID: CONV-DATE-002
  Contract statement: timezone use must be explicit: instant-bearing values preserve instants; local/date-only values
  use documented system/OA timezone rules only where required.
  Rationale: OA sync, replication, datasource values, and distributed clients must avoid accidental timezone drift.
  Source locations: OAConverterInstant, OAConverterZonedDateTime, OAConverterLocalDateTime, OAConverterLocalDate,
  OAConverterTimestamp, OAConverterDate.
  Related CODEX findings: ZonedDateTime.convertToString discards source zone.
  Suggested unit tests: zonedDateTimeFormattingPreservesZoneContract, instantToLocalDateTimeUsesSystemZoneByContract,
  localDateToDateUsesSystemZoneByContract
  Spec target section: Converter Runtime / Timezone Semantics

  ID: CONV-BOOL-001
  Contract statement: boolean conversion must use a defined vocabulary for strings, numbers, characters, and custom
  true/false/null formats.
  Rationale: booleans often control filtering, flags, permissions, and datasource values; unknown values must not
  silently become accepted truth.
  Source locations: OAConverterBoolean.convert, OAConverterBoolean.convertToString, OAConverter.toBoolean.
  Related CODEX findings: signed numeric zero strings convert true; one-field custom format can throw NPE.
  Suggested unit tests: booleanNumericZeroIsFalse, booleanSignedZeroIsFalse, booleanUnknownCustomFormatReturnsNull,
  booleanCustomNullTokenFormats
  Spec target section: Converter Runtime / Boolean Semantics

  ID: CONV-ENUM-001
  Contract statement: enum conversion must be deterministic by target enum type, canonical name, or ordinal; unknown
  values return null rather than a wrong enum.
  Rationale: enum values commonly encode domain state and workflow state. Wrong constants corrupt object behavior.
  Source locations: OAConverterEnum.convert, OAConverterEnum.convertToString, OAConverterVEnum.
  Related CODEX findings: enum converter accepts any enum instance.
  Suggested unit tests: enumNameMatchesIgnoringCase, enumOrdinalMatchesTargetEnumOnly, enumRejectsDifferentEnumClass,
  enumUnknownStringReturnsNull
  Spec target section: Converter Runtime / Enum Semantics

  ID: CONV-CLASS-001
  Contract statement: class conversion must resolve the exact requested class name through supported class loaders or
  fail as null/visible failure.
  Rationale: class values can drive metadata, reflection, code generation, and runtime routing.
  Source locations: OAConverterClass.convert, OAConverterClass.convertToString.
  Related CODEX findings: none.
  Suggested unit tests: classConverterRoundTripsName, classConverterUsesContextClassLoaderFallback,
  classConverterReturnsNullForUnknownClass
  Spec target section: Converter Runtime / Class Semantics

  ID: CONV-THREAD-001
  Contract statement: shared parser/formatter instances must be synchronized, pooled with exclusive ownership,
  immutable, or method-local.
  Rationale: converters are global utilities and can be used concurrently by UI, sync, datasource, serialization, and
  background threads.
  Source locations: OAConverterNumber.FormatPool, OAConverterNumber.getFormatter, OAConverterNumber.releaseFormatter,
  OA date/time formatter use.
  Related CODEX findings: none.
  Suggested unit tests: numberFormatterConcurrentParseIsStable, numberFormatterConcurrentFormatIsStable,
  dateTimeConcurrentFormatParseIsStable
  Spec target section: Converter Runtime / Thread Safety

  ID: CONV-DISPATCH-001
  Contract statement: direct converter calls and OAConverter/OAConv helper calls must preserve the same conversion
  semantics except for documented primitive-helper defaults and exceptions.
  Rationale: OA code uses both direct converters and convenience helpers; semantic drift creates package-dependent
  behavior.
  Source locations: OAConverter, OAConv, all OAConverterInterface implementations.
  Related CODEX findings: VEnum.convertToString(null) direct result differs from central toString normalization.
  Suggested unit tests: oaConvMatchesOAConverter, directConverterAndCentralToStringAgreeForNullDisplay,
  primitiveHelperExceptionSemanticsAreDocumented
  Spec target section: Converter Runtime / Helper Consistency

  ID: CONV-ROUNDTRIP-001
  Contract statement: where OA depends on persistence/display round-trip behavior, convertToString output must be
  parseable back to the same semantic value under the same format.
  Rationale: OA templates, reports, UI fields, serialization, and datasource string values depend on stable reversible
  representations.
  Source locations: numeric, boolean, enum, class, date/time converters.
  Related CODEX findings: ZonedDateTime.convertToString loses zone; Date formatting through OADate loses time.
  Suggested unit tests: numberRoundTripWithFormat, booleanRoundTripWithFormat, enumRoundTripByName,
  dateTimeRoundTripWithFormat, zonedDateTimeRoundTripPreservesContract
  Spec target section: Converter Runtime / Round-Trip Semantics

  ID: CONV-DEFAULT-001
  Contract statement: default values used during conversion must be target-type intentional and documented, not
  accidental fallback values.
  Rationale: OA primitive-null abstraction and object property semantics require clear separation between null, blank,
  zero, false, and failed conversion.
  Source locations: OAConverterNumber.convert, OAConverterBoolean.convert, primitive helper methods in OAConverter.
  Related CODEX findings: none.
  Suggested unit tests: numberNullConvertsToZeroByContract, booleanNullConvertsToFalseByContract,
  objectTemporalNullConvertsToNull, invalidInputDoesNotUseDefault
  Spec target section: Converter Runtime / Default Value Semantics

  Suggested Package-Level Spec Summary

  - com.viaoa.converter is OA’s central type-coercion layer for property assignment, datasource values, serialization,
    comparisons, UI, reports, and templates.
  - Conversions are target-type driven: the requested type defines the semantic contract.
  - Failed conversion must be explicit: return null or throw through helper methods, never silently produce a
    plausible wrong value.
  - Null/default behavior is part of the contract and must differ intentionally between object targets, display
    strings, and primitive helper methods.
  - Numeric conversion must preserve precision until an explicit target-type narrowing or formatter rounding boundary.
  - Temporal conversion must respect OA’s semantic split between date-only, time-only, date-time, instant, and zoned
    values.
  - Format strings are part of the conversion contract and should support stable parse/format behavior where OA relies
    on round trips.
  - Converter state must be safe for concurrent use because converters are global runtime utilities.
  - OAConv is only a convenience facade; it must not diverge from OAConverter behavior.
  - Converter unit tests should be contract tests, not only class-specific examples.


*/



