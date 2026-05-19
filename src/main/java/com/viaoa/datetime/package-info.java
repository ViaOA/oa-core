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

/* CODEX Invariants

Date/Time Invariants

  ID: DT-TYPE-001
  Contract statement: OA date/time values must preserve their declared semantic type: OADate is date-only, OATime is
  time-only, OADateTime is wall-clock datetime, and instant/timestamp conversions preserve point-in-time meaning.
  Rationale: OA properties, query values, serializers, and UI bindings rely on knowing whether a value means a
  calendar day, clock time, or instant.
  Source locations: OADate, OATime, OADateTime, constructors, convert(...), getDate(), getInstant(),
  getLocalDateTime().
  Related CODEX findings: Date/time/timezone construction drift; ZonedDateTime zone discarded; LocalDateTime uses JVM
  default timezone.
  Suggested unit tests: testOADatePreservesDateOnlySemantics, testOATimePreservesTimeOnlySemantics,
  testOADateTimePreservesDateTimeSemantics.
  Spec target section: Datetime Runtime / Semantic Type Boundaries.

  ID: DT-DATE-001
  Contract statement: OADate comparisons, formatting, serialization, and cache usage must ignore time-of-day and
  preserve the intended calendar day across JVM default timezones.
  Rationale: Business dates must not shift when servers, clients, or replication nodes run in different zones.
  Source locations: OADate, OADateTime.clearTime(), OADateTime.compareTo(...), OADateTime.writeObject/readObject.
  Related CODEX findings: OADate(Date), OADate(long), clearTime, OADate(Calendar), OADate(OADateTime),
  OADate(LocalDate) timezone drift.
  Suggested unit tests: testOADateSameDayAcrossJvmDefaultTimezones, testOADateSerializationPreservesCalendarDay,
  testOADateCompareIgnoresTime.
  Spec target section: Datetime Runtime / Date-Only Semantics.

  ID: DT-TIME-001
  Contract statement: OATime must represent clock time independent of calendar date unless a method explicitly
  converts through a datetime/instant boundary.
  Rationale: Time-of-day fields are used for schedules, UI input, templates, and rules; they must not depend on
  January 1 epoch date behavior or local timezone side effects.
  Source locations: OATime, OADateTime.clearDate(), OATime.getLocalTime(), OADateTime.compareTo(...).
  Related CODEX findings: OATime(Date), OATime(Time), OATime(long), clearDate depend on JVM default timezone.
  Suggested unit tests: testOATimeSameClockTimeAcrossJvmDefaultTimezones, testOATimeSerializationPreservesClockFields,
  testOATimeCompareIgnoresDate.
  Spec target section: Datetime Runtime / Time-Only Semantics.

  ID: DT-INSTANT-001
  Contract statement: Instant/timestamp conversions must preserve the exact epoch-millisecond point in time unless the
  API explicitly says it is changing wall-clock fields.
  Rationale: Sync, replication, audit timestamps, datasource values, and cache comparisons require stable instants.
  Source locations: OADateTime(Instant), OADateTime(Date), OADateTime(Timestamp), getInstant(), getZonedDateTime(),
  convertTo(...).
  Related CODEX findings: getInstant() reconstructs from local fields; ZonedDateTime zone discarded.
  Suggested unit tests: testInstantRoundTripPreservesEpochMillis, testTimestampRoundTripPreservesMillis,
  testConvertToDoesNotCorruptInstantWhenContractIsInstantConversion.
  Spec target section: Datetime Runtime / Instant Semantics.

  ID: DT-TZ-001
  Contract statement: Timezone-sensitive operations must use the instance timezone or OA default timezone explicitly,
  not accidentally depend on JVM default timezone.
  Rationale: Distributed OA applications cannot allow date/time interpretation to change by deployment host.
  Source locations: OADateTime.defaultTimeZone, _getCal(), setDefaultTimeZone(...), getTimeZone(), toStringMain(...),
  constructors.
  Related CODEX findings: pooled formatter timezone retention; LocalDateTime uses system default; date/time
  constructors use JVM default.
  Suggested unit tests: testDefaultTimeZoneControlsFieldAccess, testParsingWithoutZoneUsesOADefaultTimeZone,
  testConstructorDoesNotUseJvmDefaultWhenOADefaultDiffers.
  Spec target section: Datetime Runtime / Timezone Semantics.

  ID: DT-DST-001
  Contract statement: DST gaps and overlaps must have deterministic behavior for construction, arithmetic, comparison,
  serialization, and conversion.
  Rationale: Scheduling, reports, and replicated timestamps must not silently shift or choose different offsets across
  nodes.
  Source locations: OADateTime.setTimeZone(...), addDays/addHours, _getCal(), getInstant(), convertTo(...).
  Related CODEX findings: getInstant() can pick wrong offset during fall-back overlap; timezone reconstruction issues.
  Suggested unit tests: testDstGapConstructionIsDeterministic, testDstOverlapInstantRoundTrip,
  testAddHoursAcrossDstTransition.
  Spec target section: Datetime Runtime / DST Semantics.

  ID: DT-ARITH-001
  Contract statement: Date/time arithmetic must return a new value of the same semantic type and must define
  deterministic month-end, leap-year, zero-amount, and boundary behavior.
  Rationale: Schedulers, searches, reports, and date-range caches depend on predictable arithmetic.
  Source locations: addDays/addWeeks/addMonths/addYears/addHours/addMinutes/addSeconds/addMilliSeconds, betweenYears/
  betweenMonths/betweenDays.
  Related CODEX findings: addDays(0) returns this; year/month elapsed calculations ignore lower fields; field rollover
  concerns.
  Suggested unit tests: testAddZeroReturnsIndependentInstance, testAddMonthAtMonthEnd, testAddYearFromLeapDay,
  testBetweenMonthsUsesDocumentedSemantics.
  Spec target section: Datetime Runtime / Arithmetic Semantics.

  ID: DT-FORMAT-001
  Contract statement: Formatting must use the intended semantic type, format string, timezone, and locale
  consistently.
  Rationale: Datasource strings, JSON/XML values, reports, templates, and UI display must not vary
  nondeterministically.
  Source locations: toString(...), toStringMain(...), OADate.toString(...), OATime.toString(...), global/instance
  format setters.
  Related CODEX findings: RFC literal Z formatting issue; formatter timezone state.
  Suggested unit tests: testDateFormatRoundTrip, testTimeFormatRoundTrip, testDateTimeFormatUsesInstanceTimeZone,
  testRfcZuluFormatMeansUtc.
  Spec target section: Datetime Runtime / Formatting Semantics.

  ID: DT-PARSE-001
  Contract statement: Parsing failure must not silently produce current/default or normalized wrong values unless that
  fallback is explicitly documented for that API.
  Rationale: Silent wrong parse values corrupt OA properties, query filters, datasource criteria, and replicated
  state.
  Source locations: valueOfMain(...), OADate.valueOf(...), OATime.valueOf(...), OADateTime.valueOf(...), string
  constructors.
  Related CODEX findings: lenient parse normalization; partial input consumption; OATime(String) NPE after parse
  failure.
  Suggested unit tests: testInvalidDateDoesNotNormalize, testInvalidTimeDoesNotNormalize,
  testParseRequiresFullInputConsumption, testStringConstructorFailsIntentionallyOnBadInput.
  Spec target section: Datetime Runtime / Parsing Semantics.

  ID: DT-LOCALE-001
  Contract statement: Locale-dependent parsing and formatting must be explicit, deterministic, and consistently
  reflected in global parse/output formats.
  Rationale: OA apps may run across users, servers, and clients with different locales; date interpretation must
  remain intentional.
  Source locations: setLocale(...), getFormat(...), vecDateTimeParseFormat, vecDateParseFormat, global output formats.
  Related CODEX findings: none observed beyond parser/formatter state issues.
  Suggested unit tests: testSetLocaleChangesDateParseOrder, testLocaleSpecificFormatIsDeterministic,
  testLocaleChangeDoesNotLeaveStaleParseState.
  Spec target section: Datetime Runtime / Locale Semantics.

  ID: DT-SQL-001
  Contract statement: SQL Date, Time, and Timestamp conversions must preserve their SQL semantic boundaries: date-
  only, time-only, or timestamp/instant.
  Rationale: Datasource persistence and query criteria must not mix date-only and timestamp semantics.
  Source locations: OADate(Time), OATime(java.sql.Time), OADateTime(java.sql.Time), OADateTime(Timestamp), JdbcFormat
  constants.
  Related CODEX findings: time-only/date-only construction depends on JVM default timezone.
  Suggested unit tests: testSqlDatePreservesDateOnly, testSqlTimePreservesTimeOnly,
  testSqlTimestampPreservesInstantMillis.
  Spec target section: Datetime Runtime / SQL Conversion Semantics.

  ID: DT-COMPARE-001
  Contract statement: Comparison and equality must compare the intended semantic value and must not treat non-
  comparable values as ordered. Equal values must have compatible hash codes.
  Rationale: Hub sorting/filtering, datasource criteria, cache keys, object matching, and query behavior depend on
  stable comparison contracts.
  Source locations: compareTo(...), equals(...), hashCode(), before/after/between, OADate.between(...).
  Related CODEX findings: OADate/OATime equals vs hashCode mismatch; non-convertible compareTo sentinel can make
  after() true.
  Suggested unit tests: testOADateEqualsHashCodeContract, testOATimeEqualsHashCodeContract,
  testAfterNonComparableIsNotTrue, testCompareCrossSemanticTypes.
  Spec target section: Datetime Runtime / Comparison Semantics.

  ID: DT-MUTABLE-001
  Contract statement: Public APIs must not leak shared mutable Date, Calendar, formatter, timezone, or pooled state
  that can mutate an OA value behind its back.
  Rationale: OA values are used throughout object state, filters, hubs, and serialization; aliasing can cause hidden
  state drift.
  Source locations: getDate(), getCalendar(), _getCal/_releaseCal, OADateRangeCache.DateRange, constructors from
  mutable types.
  Related CODEX findings: addDays(0) returns same mutable instance; date-range cache can treat empty range as loaded.
  Suggested unit tests: testGetDateReturnsDefensiveCopy, testGetCalendarReturnsDefensiveCopy,
  testArithmeticDoesNotAliasOriginal, testDateRangeCacheRequiresLoadedCoverage.
  Spec target section: Datetime Runtime / Mutable State Isolation.

  ID: DT-THREAD-001
  Contract statement: Shared parser/formatter/calendar state must be synchronized, immutable, thread-local, or
  otherwise confined.
  Rationale: Date/time conversion is used in server-side formatting, parsing, reports, templates, serialization, and
  sync paths under concurrency.
  Source locations: simpleDateFormats, getFormatter(), toStringMain(...), valueOfMain(...), poolGregorianCalendar.
  Related CODEX findings: pooled SimpleDateFormat retains mutable timezone state.
  Suggested unit tests: testConcurrentFormattingIsDeterministic, testConcurrentParsingDoesNotShareTimezoneState,
  testCalendarPoolDoesNotLeakFieldsBetweenUses.
  Spec target section: Datetime Runtime / Thread Safety.

  ID: DT-FAIL-001
  Contract statement: Date/time APIs must fail visibly for invalid normal-use values unless the method explicitly
  documents a fallback such as “single space means current date/time.”
  Rationale: Silent fallback values are false success and can corrupt persisted properties, filters, schedules, and
  replication timestamps.
  Source locations: setCalendar(String), valueOfMain(...), string constructors, OATimeZone.getTimeZone(...), field
  setters.
  Related CODEX findings: invalid parse normalization; invalid field rollover; null/invalid OATime(String) failure
  path.
  Suggested unit tests: testInvalidFieldConstructorFailsOrDocumentsRollover,
  testBadTimezoneDoesNotSilentlyUseWrongZone, testNullStringConstructorBehaviorIsDocumented.
  Spec target section: Datetime Runtime / Failure Semantics.

  ID: DT-CONV-001
  Contract statement: OA datetime helpers must remain consistent with com.viaoa.converter and com.viaoa.compare:
  conversions must preserve semantic type, and comparisons must use the same semantic rules.
  Rationale: The same date/time value can flow through property conversion, datasource conversion, filters, queries,
  templates, and Hub sorting.
  Source locations: OADateTime.convert(...), valueOf(...), compareTo(...), OAConv datetime converters, OACompare.
  Related CODEX findings: string conversion and comparison paths can inherit parser and non-comparable sentinel
  behavior.
  Suggested unit tests: testOAConvDateMatchesOADateValueOf, testOACompareDateMatchesOADateCompareTo,
  testConverterCompareRoundTripPreservesSemanticType.
  Spec target section: Cross-Package Contracts / Converter and Compare Alignment.

  Suggested Package-Level Spec Summary

  com.viaoa.datetime owns OA’s core date/time semantic layer: date-only, time-only, datetime, timezone-aware display,
  and instant/timestamp interop.

  It must guarantee deterministic behavior across JVM default timezone, OA default timezone, locale, serialization,
  datasource persistence, comparison, and formatting.

  It must never silently normalize invalid normal-use input into a different date/time unless that behavior is
  explicitly documented and tested.

  OADate must preserve calendar-day meaning. OATime must preserve clock-time meaning. OADateTime and instant/timestamp
  paths must preserve point-in-time meaning.

  Parsing/formatting must be round-trip safe where OA depends on persisted, serialized, query, template, or UI values.

  Timezone and DST behavior must be explicit at every boundary where wall-clock fields and epoch millis interact.

  Comparison/equality/hash behavior must match OA semantic type expectations and must remain safe for Hubs, filters,
  caches, and datasource criteria.

  Mutable Date, Calendar, formatter, timezone, and pooled state must not leak or create nondeterministic results.

  Likely unit-test categories: semantic type preservation, timezone drift, DST gaps/overlaps, parse/format round
  trips, SQL conversion, arithmetic boundaries, comparison/hash contracts, concurrent formatter/parser use, and
  failure/fallback behavior.

*/



