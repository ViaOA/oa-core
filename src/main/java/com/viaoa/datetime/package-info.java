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
package com.viaoa.datetime;

//CODEX unit tests <todo>

/* CODEX Invariants

DT-TYPE-001 — Temporal Semantic Type Boundaries
Contract statement:
OA date/time values must preserve their declared semantic type: OADate represents a calendar date, OATime represents
a clock time, OADateTime represents a date-time value, and instant/timestamp paths preserve point-in-time meaning
unless a method explicitly converts across semantic boundaries.
Rationale:
OA properties, datasource values, query criteria, serialization, UI bindings, scheduling, and Hub sorting depend on
knowing whether a value means a day, a time-of-day, a local date-time, or an instant.
Source scope:
OADate, OATime, OADateTime constructors, getLocalDate, getLocalTime, getLocalDateTime, getInstant, getZonedDateTime,
convertTo, clearTime, clearDate.
Related CODEX findings:
Date/time construction drift; LocalDate/LocalDateTime timezone drift; ZonedDateTime zone loss in related conversion
paths.
Suggested unit tests:
testOADatePreservesDateOnlySemantics, testOATimePreservesTimeOnlySemantics,
testOADateTimePreservesDateTimeSemantics, testInstantPathPreservesPointInTime.
Spec target section:
Datetime Runtime / Semantic Type Boundaries

DT-DATE-001 — Date-Only Calendar Day Semantics
Contract statement:
OADate operations must ignore time-of-day and preserve the intended calendar day across construction, formatting,
comparison, serialization, and conversion.
Rationale:
Business dates must not shift due to time-of-day residue, timezone conversion, serialization, or host JVM defaults.
Source scope:
OADate constructors, OADate.getLocalDate, OADate.toString, OADate.valueOf/dateValue, OADateTime.clearTime, OADateTi
me.compareTo, serialization methods.
Related CODEX findings:
OADate(Date), OADate(long), OADate(Calendar), OADate(OADateTime), OADate(LocalDate), and clearTime can be sensitive
to timezone handling.
Suggested unit tests:
testOADateSameDayAcrossDefaultTimezones, testOADateSerializationPreservesCalendarDay, testOADateCompareIgnoresTime,
testOADateLocalDateRoundTripPreservesDay.
Spec target section:
Datetime Runtime / Date-Only Semantics

DT-TIME-001 — Time-Only Clock Semantics
Contract statement:
OATime operations must preserve clock-time fields independently of calendar date unless an API explicitly converts
through an instant or date-time boundary.
Rationale:
Time-of-day values are used for schedules, UI input, reports, rules, and datasource fields; they must not
accidentally depend on an epoch date or local timezone side effects.
Source scope:
OATime constructors, OATime.getLocalTime, OATime.toString, OATime.valueOf/timeValue, OADateTime.clearDate, OADateTi
me.compareTo.
Related CODEX findings:
OATime(Date), OATime(Time), OATime(long), and clearDate can depend on JVM default timezone behavior.
Suggested unit tests:
testOATimeSameClockTimeAcrossDefaultTimezones, testOATimeSerializationPreservesClockFields,
testOATimeCompareIgnoresDate, testOATimeLocalTimeRoundTripPreservesMillis.
Spec target section:
Datetime Runtime / Time-Only Semantics

DT-INSTANT-001 — Instant And Epoch Millisecond Preservation
Contract statement:
Instant-bearing constructors, accessors, conversion, and timestamp operations must preserve the exact epoch-
millisecond point in time unless the method explicitly documents wall-clock reinterpretation.
Rationale:
Sync, replication, audit timestamps, datasource timestamps, cache comparisons, and logs require stable point-in-time
behavior.
Source scope:
OADateTime(Date), OADateTime(long), OADateTime(Timestamp), OADateTime(Instant), OADateTime(ZonedDateTime), getTime,
getInstant, getDate, getCalendar, convertToUTC, convertTo.
Related CODEX findings:
getInstant/getZonedDateTime/getLocalDateTime reconstruction concerns; DST overlap offset selection.
Suggested unit tests:
testInstantRoundTripPreservesEpochMillis, testTimestampRoundTripPreservesMillis,
testDateConstructorPreservesEpochMillis, testConvertToUTCDoesNotChangeInstant.
Spec target section:
Datetime Runtime / Instant Semantics

DT-TZ-001 — Explicit Timezone Authority
Contract statement:
Timezone-sensitive operations must use the instance timezone or OA default timezone explicitly; they must not
accidentally depend on the JVM default timezone except where the method contract says so.
Rationale:
Distributed OA applications can run on servers, clients, and replication nodes with different host settings.
Temporal interpretation must remain intentional and repeatable.
Source scope:
OADateTime.defaultTimeZone, setDefaultTimeZone, getDefaultTimeZone, setTimeZone, getTimeZone, setTimeZoneUTC,
_getCal, toStringMain, constructors from local and instant-bearing types.
Related CODEX findings:
LocalDateTime constructor uses JVM default timezone; date/time constructors and clear operations can use default
timezone unexpectedly.
Suggested unit tests:
testDefaultTimeZoneControlsFieldAccess, testParsingWithoutZoneUsesOADefaultTimeZone,
testConstructorUsesDocumentedTimezoneSource, testInstanceTimezoneControlsFormatting.
Spec target section:
Datetime Runtime / Timezone Semantics

DT-TZ-002 — Zone Identity And Offset Preservation
Contract statement:
Zone-bearing values must preserve their zone identity and offset where the target type can represent them; zone loss
is allowed only when converting to a zone-less target and must be documented.
Rationale:
Zoned date-time values encode more than epoch millis. Losing zone identity can change wall-clock display and
serialized values across nodes.
Source scope:
OADateTime(ZonedDateTime), getZonedDateTime, setTimeZone, convertTo, OATimeZone, OATimeZone.TZ.
Related CODEX findings:
ZonedDateTime zone discarded in related converter formatting; getInstant/getZonedDateTime reconstruction concerns.
Suggested unit tests:
testZonedDateTimeConstructorPreservesZoneWhenRepresentable, testGetZonedDateTimeUsesInstanceTimezone,
testConvertToPreservesInstantAndChangesZone, testZoneLossOnlyOccursForZoneLessTargets.
Spec target section:
Datetime Runtime / Zone Semantics

DT-DST-001 — DST Gap And Overlap Determinism
Contract statement:
Construction, parsing, arithmetic, comparison, serialization, and conversion across daylight-saving gaps and
overlaps must choose deterministic results under the active timezone contract.
Rationale:
Schedules, replicated timestamps, reports, and datasource values must not drift or choose different offsets across
JVMs or runtime nodes.
Source scope:
OADateTime constructors, setCalendar, valueOfMain, addHours/addDays/addMonths/addYears, getInstant, convertTo,
setTimeZone.
Related CODEX findings:
getInstant can pick the wrong offset during fallback overlap; timezone reconstruction concerns.
Suggested unit tests:
testDstGapConstructionIsDeterministic, testDstOverlapInstantRoundTrip, testAddHoursAcrossDstTransition,
testDateOnlyAcrossDstDoesNotShiftDay.
Spec target section:
Datetime Runtime / DST Semantics

DT-FIELD-001 — Field Mutation Validity
Contract statement:
Field constructors and setters must either commit a valid temporal value for the requested semantic type or fail
visibly; invalid field combinations must not silently roll over to a different date/time unless explicitly
documented.
Rationale:
Silent rollover turns invalid normal-use input into a plausible but wrong business date, schedule time, query value,
or persisted timestamp.
Source scope:
OADateTime field constructors, setCalendar(int,...), setDate, setTime, setYear, setMonth, setDay, setHour,
set12Hour, set24Hour, setAM_PM, setMinute, setSecond, setMilliSecond, OADate/OATime field constructors.
Related CODEX findings:
Invalid field rollover concerns; set12Hour can lose AM/PM state.
Suggested unit tests:
testInvalidDateFieldsFailOrDocumentRollover, testInvalidTimeFieldsFailOrDocumentRollover,
testSet12HourPreservesCurrentAmPm, testFieldSetterDoesNotCommitPartialInvalidState.
Spec target section:
Datetime Runtime / Field Mutation Semantics

DT-ARITH-001 — Arithmetic Type And Boundary Semantics
Contract statement:
Date/time arithmetic must return a value of the same semantic type and define deterministic behavior for zero
amounts, month-end, leap-year, DST transitions, and millisecond boundaries.
Rationale:
Schedulers, date range searches, reports, cache keys, and recurring business rules depend on predictable arithmetic.
Source scope:
addDays/subtractDays, addWeeks/subtractWeeks, addMonths/subtractMonths, addYears/subtractYears, addHours/
subtractHours, addMinutes/subtractMinutes, addSeconds/subtractSeconds, addMilliSeconds/subtractMilliSeconds.
Related CODEX findings:
addDays(0) can return this; field rollover concerns.
Suggested unit tests:
testAddZeroReturnsIndependentSameTypeValue, testAddMonthAtMonthEndIsDeterministic,
testAddYearFromLeapDayIsDeterministic, testAddMillisecondsPreservesPrecision.
Spec target section:
Datetime Runtime / Arithmetic Semantics

DT-BETWEEN-001 — Elapsed Unit Semantics
Contract statement:
betweenYears, betweenMonths, betweenDays, betweenHours, betweenMinutes, betweenSeconds, and betweenMilliSeconds must
use a documented unit boundary rule and produce deterministic signed results.
Rationale:
Date range filters, scheduling, aging logic, reports, and cache gap calculations rely on a clear distinction between
calendar-field differences and fully elapsed units.
Source scope:
OADateTime.betweenYears, betweenMonths, betweenDays, betweenHours, betweenMinutes, betweenSeconds, betweenMilliSeco
nds, OADate.between/betweenOrEqual/betweenNotEqual.
Related CODEX findings:
Year/month elapsed calculations can ignore lower fields.
Suggested unit tests:
testBetweenYearsUsesDocumentedBoundaryRule, testBetweenMonthsUsesDocumentedBoundaryRule,
testBetweenDaysIsSignedAndDeterministic, testBetweenMillisecondsPreservesExactDelta.
Spec target section:
Datetime Runtime / Difference Semantics

DT-FORMAT-001 — Deterministic Formatting
Contract statement:
Formatting must use the intended semantic type, format string, instance/global format, timezone, and locale
consistently, and must not leak formatter state between calls.
Rationale:
UI display, templates, reports, JSON/XML strings, datasource strings, logs, and serialization depend on stable
output.
Source scope:
OADateTime.toString, toString(String), toStringMain, OADate.toString, OATime.toString, setGlobalOutputFormat/
getGlobalOutputFormat, setFormat/getFormat, getFormatter.
Related CODEX findings:
RFC literal Z formatting issue; pooled SimpleDateFormat timezone retention.
Suggested unit tests:
testDateFormatRoundTrip, testTimeFormatRoundTrip, testDateTimeFormatUsesInstanceTimezone,
testRfcZuluFormatUsesUtcSemantics, testFormatterTimezoneDoesNotLeakBetweenCalls.
Spec target section:
Datetime Runtime / Formatting Semantics

DT-PARSE-001 — Strict And Complete Parsing
Contract statement:
Parsing must consume the intended input under the selected format, reject invalid normal-use values, and avoid
lenient normalization unless a method explicitly documents fallback behavior.
Rationale:
Silent parse normalization corrupts OA properties, datasource criteria, query filters, schedules, and replicated
state while appearing successful.
Source scope:
OADateTime.valueOfMain, valueOf overloads, OADate.valueOf/dateValue/valueOf2, OATime.valueOf/timeValue, string cons
tructors, fixDate.
Related CODEX findings:
Lenient parse normalization; partial input consumption; OATime(String) NPE after parse failure.
Suggested unit tests:
testInvalidDateDoesNotNormalize, testInvalidTimeDoesNotNormalize, testParseRequiresFullInputConsumption,
testStringConstructorFailsPredictablyOnBadInput.
Spec target section:
Datetime Runtime / Parsing Semantics

DT-ROUNDTRIP-001 — Parse/Format Round Trip Stability
Contract statement:
Where OA depends on textual persistence or display/edit cycles, formatting output must parse back to the same
semantic value under the same type, format, timezone, and locale, subject only to documented precision or semantic-
type loss.
Rationale:
Datasource string fields, UI forms, templates, reports, serialization, and generated tooling often require stable
round trips.
Source scope:
OADate.toString/valueOf, OATime.toString/valueOf, OADateTime.toString/valueOf, global parse/output format registrie
s.
Related CODEX findings:
Formatter timezone state; parse leniency; temporal dimension drift.
Suggested unit tests:
testOADateFormatParseRoundTrip, testOATimeFormatParseRoundTrip, testOADateTimeFormatParseRoundTrip,
testRoundTripHonorsLocaleAndTimezone.
Spec target section:
Datetime Runtime / Round-Trip Semantics

DT-LOCALE-001 — Locale-Sensitive Format Authority
Contract statement:
Locale-dependent parsing and formatting must be explicit, deterministic, and reflected consistently in global parse
and output formats.
Rationale:
OA applications can run across users, servers, and clients with different locale defaults. Date interpretation must
remain intentional.
Source scope:
OADateTime.setLocale, OADate.setLocale, OADateTime.getFormat(int, Locale), OADateTime.getFormat(int), global parse
format vectors, global output formats.
Related CODEX findings:
Parser/formatter state concerns.
Suggested unit tests:
testSetLocaleChangesDateParseOrderByContract, testLocaleSpecificFormatIsDeterministic,
testLocaleChangeDoesNotLeaveStaleParseState, testGlobalParseFormatsRemainConsistentAfterLocaleChange.
Spec target section:
Datetime Runtime / Locale Semantics

DT-SQL-001 — SQL Temporal Boundary Semantics
Contract statement:
SQL Date, Time, and Timestamp interop must preserve their SQL semantic boundaries: date-only, time-only, or
timestamp/instant.
Rationale:
Datasource persistence and query criteria must not mix date-only, time-only, and timestamp semantics.
Source scope:
OADate(java.sql.Time), OATime(java.sql.Time), OADateTime(java.sql.Time), OADateTime(java.sql.Timestamp), JdbcFormat
constants, getDate/getTime behavior.
Related CODEX findings:
Time-only/date-only construction can depend on JVM default timezone.
Suggested unit tests:
testSqlDatePreservesDateOnly, testSqlTimePreservesTimeOnly, testSqlTimestampPreservesEpochMillis,
testJdbcFormatsRoundTripWithinSemanticType.
Spec target section:
Datetime Runtime / SQL Conversion Semantics

DT-COMPARE-001 — Comparison Ordering Semantics
Contract statement:
Comparison, before/after, and between methods must compare the intended semantic value and must not treat non-
convertible values as ordered.
Rationale:
Hub sorting/filtering, query logic, datasource criteria, cache keys, scheduling, and object matching require stable
temporal ordering.
Source scope:
OADateTime.compareTo, compare, before/isBefore, after/isAfter, OADate.between variants, OATime.compare.
Related CODEX findings:
Non-convertible compareTo sentinel can make after() true.
Suggested unit tests:
testAfterNonComparableIsNotTrue, testBeforeNonComparableIsNotTrue, testCompareCrossSemanticTypesUsesDocumentedRule,
testBetweenRejectsNonComparableBoundary.
Spec target section:
Datetime Runtime / Comparison Semantics

DT-EQUALS-001 — Equality And HashCode Compatibility
Contract statement:
equals and hashCode must be compatible for each temporal semantic type, and equality must reflect the same semantic
value used by comparison where applicable.
Rationale:
OA date/time values can be used in Hubs, maps, sets, cache keys, filters, and matching logic. Equality/hash drift
causes missing or duplicate entries.
Source scope:
OADateTime.equals, OADateTime.hashCode, inherited OADate/OATime equality behavior, compareTo.
Related CODEX findings:
OADate/OATime equals vs hashCode mismatch.
Suggested unit tests:
testOADateEqualsHashCodeContract, testOATimeEqualsHashCodeContract, testOADateTimeEqualsHashCodeContract,
testCompareZeroImpliesEqualsWhereContracted.
Spec target section:
Datetime Runtime / Equality Semantics

DT-MUTABLE-001 — Mutable State Isolation
Contract statement:
Public APIs must not expose mutable Date, Calendar, formatter, timezone, or pooled state that can mutate an OA
temporal value behind its back or affect later operations.
Rationale:
Hidden aliasing can corrupt object property values, filters, Hub state, serialization output, and date-range caches.
Source scope:
OADateTime.getDate, getCalendar, getTimeZone, _getCal/_releaseCal, constructors from Date/Calendar/TimeZone,
arithmetic methods.
Related CODEX findings:
addDays(0) can return the same mutable instance; pooled formatter/calendar state concerns.
Suggested unit tests:
testGetDateReturnsDefensiveCopy, testGetCalendarReturnsDefensiveCopy,
testGetTimeZoneMutationDoesNotCorruptInstanceIfContractRequiresIsolation, testArithmeticDoesNotAliasOriginal.
Spec target section:
Datetime Runtime / Mutable State Isolation

DT-THREAD-001 — Shared Temporal State Thread Safety
Contract statement:
Shared parser, formatter, calendar, locale, timezone, and global format state must be immutable, synchronized,
thread-confined, safely published, or reset before reuse.
Rationale:
Date/time formatting and parsing are used by UI, datasource, serialization, sync, remote, reports, and background
threads under concurrency.
Source scope:
OADateTime.simpleDateFormats, getFormatter, valueOfMain, toStringMain, poolGregorianCalendar, global format vectors,
defaultTimeZone, static locale/output format state.
Related CODEX findings:
Pooled SimpleDateFormat retains mutable timezone state.
Suggested unit tests:
testConcurrentFormattingIsDeterministic, testConcurrentParsingDoesNotShareTimezoneState,
testCalendarPoolDoesNotLeakFieldsBetweenUses, testGlobalFormatUpdatesAreSafelyObserved.
Spec target section:
Datetime Runtime / Thread Safety

DT-FAIL-001 — Failure And Fallback Visibility
Contract statement:
Invalid normal-use date/time input must fail visibly or return null according to the method contract; fallback to
current/default/normalized values is allowed only when explicitly documented and tested.
Rationale:
Silent fallback values are false success and can corrupt persisted properties, filters, schedules, UI data, and
replication timestamps.
Source scope:
OADateTime.setCalendar(String), valueOfMain, string constructors, OADate.valueOf/valueOf2, OATime.valueOf/timeValue,
OATimeZone.getTimeZone, field setters.
Related CODEX findings:
Invalid parse normalization; invalid field rollover; null/invalid OATime(String) failure path.
Suggested unit tests:
testInvalidParseReturnsNullOrThrowsByContract, testBadTimezoneDoesNotSilentlyUseWrongZone,
testNullStringConstructorBehaviorIsDocumented, testFallbackToCurrentDateRequiresDocumentedInput.
Spec target section:
Datetime Runtime / Failure Semantics

DT-TZONE-001 — Timezone Lookup Determinism
Contract statement:
OATimeZone lookup by ID, display name, short name, UTC offset, or TimeZone instance must resolve deterministically
to the intended OA timezone entry or fail as null/visible failure.
Rationale:
Timezone lookup feeds parsing, formatting, scheduling, user context, and distributed runtime behavior. Wrong or
ambiguous resolution causes temporal drift.
Source scope:
OATimeZone.getTimeZone, getTimeZoneById, getOATimeZone(String), getOATimeZone(TimeZone), getUtcTimeZone,
getShortNames, getOATimeZones.
Related CODEX findings:
Bad timezone fallback concerns.
Suggested unit tests:
testTimeZoneByIdResolvesExpectedZone, testUnknownTimeZoneDoesNotResolveToWrongZone,
testUtcOffsetLookupIsDeterministic, testShortNamesAreStableAndSorted.
Spec target section:
Datetime Runtime / Timezone Lookup Semantics

DT-RANGE-001 — Date Range Cache Coverage Semantics
Contract statement:
A date-range cache entry must represent loaded coverage only when its backing data state satisfies the cache
contract; missing-gap detection and item retrieval must not disagree about whether a range is loaded.
Rationale:
OA tooling and runtime loaders can use date ranges to avoid redundant loading. False coverage causes silent missing
results.
Source scope:
com.viaoa.datetime.cache.OADateRangeCache, DateRange, add, findMissingGaps, getCacheItems, clearCache, getDate.
Related CODEX findings:
OADateRangeCache.add(DateRange) can add a range with null list that findMissingGaps treats as covered while
getCacheItems returns no data.
Suggested unit tests:
testDateRangeWithLoadedItemsCoversGap, testDateRangeWithoutItemsDoesNotSilentlyMarkLoadedCoverage,
testGetCacheItemsFiltersByDateInclusively, testClearCacheRestoresMissingGaps.
Spec target section:
Datetime Runtime / Date Range Cache Semantics

DT-CONV-001 — Converter And Compare Alignment
Contract statement:
OA datetime helpers must remain consistent with com.viaoa.converter and com.viaoa.compare: conversions must preserve
temporal semantic type, and comparisons must use the same semantic value rules.
Rationale:
The same temporal value can flow through property conversion, datasource conversion, filters, queries, templates,
serialization, and Hub sorting.
Source scope:
OADateTime.convert, valueOf, compareTo, OADate/OATime valueOf helpers, OAConv datetime converters, OACompare
temporal comparison paths.
Related CODEX findings:
String conversion and comparison paths can inherit parser and non-comparable sentinel behavior.
Suggested unit tests:
testOAConvDateMatchesOADateValueOf, testOAConvTimeMatchesOATimeValueOf, testOACompareDateMatchesOADateCompareTo,
testConverterCompareRoundTripPreservesSemanticType.
Spec target section:
Cross-Package Contracts / Converter and Compare Alignment

DT-DETERMINISM-001 — Same Temporal Inputs Produce Same Results
Contract statement:
For the same input value, semantic type, format, timezone, locale, and global configuration state, date/time APIs
must produce the same observable result or same visible failure.
Rationale:
OA runtime behavior must be repeatable for query parameters, datasource values, reports, cache keys, serialized
values, scheduling, and sync/replication.
Source scope:
All public/protected behavior in OADate, OATime, OADateTime, OATimeZone, and OADateRangeCache where applicable.
Related CODEX findings:
Timezone drift, locale/formatter state, parse normalization, DST reconstruction, and mutable pooled state all
threaten deterministic behavior.
Suggested unit tests:
testRepeatedFormattingIsStable, testRepeatedParsingIsStable, testSameTimezoneAndLocaleProduceSameResult,
testFailureResultIsRepeatable.
Spec target section:
Datetime Runtime / Determinism

*/
