  package com.viaoa.datetime;

  import static org.junit.jupiter.api.Assertions.*;

  import java.io.ByteArrayInputStream;
  import java.io.ByteArrayOutputStream;
  import java.io.ObjectInputStream;
  import java.io.ObjectOutputStream;
  import java.text.DateFormat;
  import java.text.SimpleDateFormat;
  import java.time.DayOfWeek;
  import java.time.Duration;
  import java.time.Instant;
  import java.time.LocalDate;
  import java.time.LocalDateTime;
  import java.time.LocalTime;
  import java.time.Month;
  import java.time.Period;
  import java.time.ZoneId;
  import java.time.ZoneOffset;
  import java.time.ZonedDateTime;
  import java.time.temporal.ChronoField;
  import java.time.temporal.WeekFields;
  import java.util.Calendar;
  import java.util.Date;
  import java.util.GregorianCalendar;
  import java.util.Locale;
  import java.util.TimeZone;

  import org.junit.jupiter.api.AfterEach;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;

  import com.viaoa.datetime.OADateTime.DateTimeType;

  class OADateTimeTest {
      private static final ZoneId UTC = ZoneOffset.UTC;
      private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
      private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
      private static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");

      private TimeZone originalJvmTimeZone;
      private Locale originalJvmLocale;
      private ZoneId originalDefaultZoneId;
      private String originalGlobalOutputFormat;

      @BeforeEach
      void beforeEach() {
          originalJvmTimeZone = TimeZone.getDefault();
          originalJvmLocale = Locale.getDefault();
          originalDefaultZoneId = OADateTime.getDefaultZoneId();
          originalGlobalOutputFormat = OADateTime.getGlobalOutputFormat();

          TimeZone.setDefault(TimeZone.getTimeZone(UTC));
          Locale.setDefault(Locale.US);
          OADateTime.setLocale(Locale.US);
          OADateTime.setDefaultZoneId(CHICAGO);
          OADateTime.setGlobalOutputFormat("MM/dd/yyyy HH:mm:ss.SSS");
      }

      @AfterEach
      void afterEach() {
          OADateTime.setGlobalOutputFormat(originalGlobalOutputFormat);
          OADateTime.setDefaultZoneId(originalDefaultZoneId);
          OADateTime.setLocale(originalJvmLocale);
          Locale.setDefault(originalJvmLocale);
          TimeZone.setDefault(originalJvmTimeZone);
      }

      @Test
      void constructorsFromCurrentTimeAreWithinCallRange() {
          long before = System.currentTimeMillis();
          OADateTime dt = new OADateTime();
          long after = System.currentTimeMillis();

          assertTrue(dt.getTime() >= before);
          assertTrue(dt.getTime() <= after);
          assertEquals(DateTimeType.Instant, dt.getType());
      }

      @Test
      void longConstructorsPreserveEpochMillisAndZone() {
          long millis = Instant.parse("2026-06-09T15:30:15.123Z").toEpochMilli();

          OADateTime instant = new OADateTime(millis);
          assertEquals(millis, instant.getTime());
          assertEquals(DateTimeType.Instant, instant.getType());
          assertEquals(CHICAGO, instant.getZoneId());

          OADateTime zoned = new OADateTime(millis, NEW_YORK);
          assertEquals(millis, zoned.getTime());
          assertEquals(NEW_YORK, zoned.getZoneId());
          assertFields(zoned, 2026, 6, 9, 11, 30, 15, 123);

          OADateTime legacyZone = new OADateTime(millis, TimeZone.getTimeZone(LOS_ANGELES));
          assertEquals(millis, legacyZone.getTime());
          assertEquals(LOS_ANGELES, legacyZone.getZoneId());
      }

      @Test
      void fieldConstructorsUseJavaTimeMonthValuesAndValidateMilliseconds() {
          OADateTime dt = new OADateTime(CHICAGO, 2026, 6, 9, 10, 30, 15, 123);
          long expected = ZonedDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000,
          CHICAGO).toInstant().toEpochMilli();

          assertEquals(expected, dt.getTime());
          assertEquals(CHICAGO, dt.getZoneId());
          assertFields(dt, 2026, 6, 9, 10, 30, 15, 123);

          assertThrows(RuntimeException.class, () -> new OADateTime(2026, 6, 9, 10, 30, 15, -1));
          assertThrows(RuntimeException.class, () -> new OADateTime(2026, 6, 9, 10, 30, 15, 1000));
      }

      @Test
      void dateOnlyAndMonthEnumConstructorsDefaultExpectedFields() {
          assertFields(new OADateTime(2026, 6, 9), 2026, 6, 9, 0, 0, 0, 0);
          assertFields(new OADateTime(2026, 6, 9, 10, 30), 2026, 6, 9, 10, 30, 0, 0);
          assertFields(new OADateTime(2026, Month.JUNE, 9, 10, 30, 15), 2026, 6, 9, 10, 30, 15, 0);
      }

      @Test
      void copyConstructorsPreserveOrReplaceSelectedMetadata() {
          OADateTime base = new OADateTime(CHICAGO, 2026, 6, 9, 10, 30, 15,
          123).withType(DateTimeType.ZonedInstant);

          OADateTime copy = new OADateTime(base);
          assertSameInstant(base, copy);
          assertEquals(CHICAGO, copy.getZoneId());
          assertEquals(DateTimeType.ZonedInstant, copy.getType());

          OADateTime copyWithZone = new OADateTime(base, NEW_YORK);
          assertSameInstant(base, copyWithZone);
          assertEquals(NEW_YORK, copyWithZone.getZoneId());
          assertEquals(DateTimeType.ZonedInstant, copyWithZone.getType());

          OADateTime copyWithType = new OADateTime(base, DateTimeType.Floating);
          assertSameInstant(base, copyWithType);
          assertEquals(CHICAGO, copyWithType.getZoneId());
          assertEquals(DateTimeType.Floating, copyWithType.getType());
      }

      @Test
      void calendarConstructorPreservesInstantAndSetsTypeFromCalendarZone() {
          long millis = Instant.parse("2026-06-09T15:30:15.123Z").toEpochMilli();

          Calendar defaultZoneCalendar = Calendar.getInstance(TimeZone.getTimeZone(CHICAGO));
          defaultZoneCalendar.setTimeInMillis(millis);
          OADateTime instant = new OADateTime(defaultZoneCalendar);
          assertEquals(millis, instant.getTime());
          assertEquals(DateTimeType.Instant, instant.getType());
          assertEquals(CHICAGO, instant.getZoneId());

          Calendar otherZoneCalendar = Calendar.getInstance(TimeZone.getTimeZone(NEW_YORK));
          otherZoneCalendar.setTimeInMillis(millis);
          OADateTime zoned = new OADateTime(otherZoneCalendar);
          assertEquals(millis, zoned.getTime());
          assertEquals(DateTimeType.ZonedInstant, zoned.getType());
          assertEquals(NEW_YORK, zoned.getZoneId());
      }

      @Test
      void javaTimeAndLegacyConstructorsSetExpectedTypes() {
          Instant instant = Instant.parse("2026-06-09T15:30:15.123Z");
          assertEquals(DateTimeType.Instant, new OADateTime(instant).getType());
          assertEquals(instant.toEpochMilli(), new OADateTime(instant).getTime());

          LocalDateTime ldt = LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000);
          OADateTime floating = new OADateTime(ldt);
          assertEquals(DateTimeType.Floating, floating.getType());
          assertFields(floating, 2026, 6, 9, 10, 30, 15, 123);

          OADateTime date = new OADateTime(LocalDate.of(2026, 6, 9));
          assertEquals(DateTimeType.Floating, date.getType());
          assertFields(date, 2026, 6, 9, 0, 0, 0, 0);

          OADateTime time = new OADateTime(LocalTime.of(10, 30, 15, 123_456_789));
          assertEquals(DateTimeType.Floating, time.getType());
          assertFields(time, 1970, 1, 1, 10, 30, 15, 123);

          ZonedDateTime zdt = ZonedDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000, NEW_YORK);
          OADateTime zoned = new OADateTime(zdt);
          assertEquals(DateTimeType.ZonedInstant, zoned.getType());
          assertEquals(NEW_YORK, zoned.getZoneId());
          assertEquals(zdt.toInstant().toEpochMilli(), zoned.getTime());

          Date legacyDate = new Date(instant.toEpochMilli());
          OADateTime legacy = new OADateTime(legacyDate);
          assertEquals(DateTimeType.Instant, legacy.getType());
          assertEquals(legacyDate.getTime(), legacy.getTime());
      }

      @Test
      void stringConstructorsParseOrThrow() {
          OADateTime dt = new OADateTime("06/09/2026 10:30:15.123", "MM/dd/yyyy HH:mm:ss.SSS");
          assertEquals(DateTimeType.Floating, dt.getType());
          assertFields(dt, 2026, 6, 9, 10, 30, 15, 123);

          assertThrows(IllegalArgumentException.class, () -> new OADateTime("not a date"));
      }

      @Test
      void fieldGettersAndConversionsUseEffectiveZone() {
          long millis = ZonedDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000,
          CHICAGO).toInstant().toEpochMilli();
          OADateTime dt = new OADateTime(millis, CHICAGO).withType(DateTimeType.ZonedInstant);

          assertEquals(millis, dt.getTime());
          assertEquals(DateTimeType.ZonedInstant, dt.getType());
          assertEquals(CHICAGO, dt.getZoneId());
          assertEquals(TimeZone.getTimeZone(CHICAGO), dt.getTimeZone());
          assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000), dt.getLocalDateTime());
          assertEquals(LocalDate.of(2026, 6, 9), dt.getLocalDate());
          assertEquals(LocalTime.of(10, 30, 15, 123_000_000), dt.getLocalTime());
          assertEquals(Instant.ofEpochMilli(millis), dt.getInstant());
          assertEquals(10, dt.getField(ChronoField.HOUR_OF_DAY));

          Calendar cal = dt.getCalendar();
          assertEquals(millis, cal.getTimeInMillis());
          assertEquals(TimeZone.getTimeZone(CHICAGO), cal.getTimeZone());
          assertFalse(cal.isLenient());

          assertEquals(new Date(millis), dt.getDate());
      }

      @Test
      void calendarFieldGettersReturnExpectedValues() {
          OADateTime dt = new OADateTime(CHICAGO, 2026, 7, 4, 10, 30, 15, 123);

          assertEquals(2026, dt.getYear());
          assertEquals(7, dt.getMonthValue());
          assertEquals(Month.JULY, dt.getMonth());
          assertEquals(4, dt.getDayOfMonth());
          assertEquals(10, dt.getHour());
          assertEquals(30, dt.getMinute());
          assertEquals(15, dt.getSecond());
          assertEquals(123, dt.getMilliSecond());
          assertEquals(2, dt.getQuarter());
          assertEquals(DayOfWeek.SATURDAY, dt.getDayOfWeek());
          assertEquals(185, dt.getDayOfYear());
          assertEquals(WeekFields.of(Locale.US).weekOfMonth().getFrom(dt.getZonedDateTime()),
          dt.getWeekOfMonth());
          assertEquals(WeekFields.of(Locale.US).weekOfYear().getFrom(dt.getZonedDateTime()),
          dt.getWeekOfYear());
      }

      @Test
      void monthAndYearLengthsIncludeLeapYears() {
          OADateTime feb2024 = new OADateTime(CHICAGO, 2024, 2, 10, 0, 0, 0, 0);
          OADateTime feb2025 = new OADateTime(CHICAGO, 2025, 2, 10, 0, 0, 0, 0);

          assertEquals(29, feb2024.getDaysInMonth());
          assertEquals(29, feb2024.getLastDayOfMonth());
          assertEquals(366, feb2024.getDaysInYear());
          assertEquals(28, feb2025.getDaysInMonth());
          assertEquals(28, feb2025.getLastDayOfMonth());
          assertEquals(365, feb2025.getDaysInYear());
      }

      @Test
      void withDateTimeMethodsPreserveTypeAndExpectedFields() {
          OADateTime base = new OADateTime(CHICAGO, 2026, 6, 9, 10, 30, 15,
          123).withType(DateTimeType.ZonedInstant);

          assertChanged(base.withDateTime(2027, 8, 10, 11, 31, 16, 124), DateTimeType.ZonedInstant, 2027, 8,
          10, 11, 31, 16, 124);
          assertChanged(base.withDate(2027, 8, 10), DateTimeType.ZonedInstant, 2027, 8, 10, 10, 30, 15, 123);
          assertChanged(base.withYear(2027), DateTimeType.ZonedInstant, 2027, 6, 9, 10, 30, 15, 123);
          assertChanged(base.withMonth(Month.AUGUST), DateTimeType.ZonedInstant, 2026, 8, 9, 10, 30, 15, 123);
          assertChanged(base.withMonthValue(8), DateTimeType.ZonedInstant, 2026, 8, 9, 10, 30, 15, 123);
          assertChanged(base.withDayOfMonth(10), DateTimeType.ZonedInstant, 2026, 6, 10, 10, 30, 15, 123);
          assertChanged(base.withoutDate(), DateTimeType.ZonedInstant, 1970, 1, 1, 10, 30, 15, 123);
          assertChanged(base.withDate((OADate) null), DateTimeType.ZonedInstant, 1970, 1, 1, 10, 30, 15, 123);
      }

      @Test
      void withTimeMethodsPreserveTypeAndExpectedFields() {
          OADateTime base = new OADateTime(CHICAGO, 2026, 6, 9, 10, 30, 15,
          123).withType(DateTimeType.ZonedInstant);

          assertChanged(base.withTime(11, 31, 16, 124), DateTimeType.ZonedInstant, 2026, 6, 9, 11, 31, 16,
          124);
          assertChanged(base.withTime(11, 31), DateTimeType.ZonedInstant, 2026, 6, 9, 11, 31, 0, 0);
          assertChanged(base.withHours(11), DateTimeType.ZonedInstant, 2026, 6, 9, 11, 30, 15, 123);
          assertChanged(base.withMinutes(31), DateTimeType.ZonedInstant, 2026, 6, 9, 10, 31, 15, 123);
          assertChanged(base.withSeconds(16), DateTimeType.ZonedInstant, 2026, 6, 9, 10, 30, 16, 123);
          assertChanged(base.withMilliSeconds(124), DateTimeType.ZonedInstant, 2026, 6, 9, 10, 30, 15, 124);
          assertChanged(base.withoutTime(), DateTimeType.ZonedInstant, 2026, 6, 9, 0, 0, 0, 0);
          assertChanged(base.withTime((OATime) null), DateTimeType.ZonedInstant, 2026, 6, 9, 0, 0, 0, 0);
      }

      @Test
      void withTypePreservesInstantAndZone() {
          OADateTime base = new OADateTime(CHICAGO, 2026, 6, 9, 10, 30, 15, 123);

          OADateTime floating = base.withType(DateTimeType.Floating);

          assertSameInstant(base, floating);
          assertEquals(CHICAGO, floating.getZoneId());
          assertEquals(DateTimeType.Floating, floating.getType());
      }

      @Test
      void zoneConversionsSupportSameInstantAndSameWallTime() {
          OADateTime chicago = new OADateTime(CHICAGO, 2026, 6, 9, 10, 30, 15,
          123).withType(DateTimeType.ZonedInstant);

          OADateTime sameInstant = chicago.withZoneIdSameInstant(NEW_YORK);
          assertEquals(chicago.getTime(), sameInstant.getTime());
          assertEquals(NEW_YORK, sameInstant.getZoneId());
          assertFields(sameInstant, 2026, 6, 9, 11, 30, 15, 123);

          OADateTime sameWallTime = chicago.withZoneIdSameWallTime(NEW_YORK);
          assertNotEquals(chicago.getTime(), sameWallTime.getTime());
          assertEquals(NEW_YORK, sameWallTime.getZoneId());
          assertFields(sameWallTime, 2026, 6, 9, 10, 30, 15, 123);

          OADateTime utcInstant = chicago.withTimeZoneUTCSameInstant();
          assertEquals(chicago.getTime(), utcInstant.getTime());
          assertEquals(UTC, utcInstant.getZoneId());

          OADateTime utcWallTime = chicago.withTimeZoneUTCSameWallTime();
          assertNotEquals(chicago.getTime(), utcWallTime.getTime());
          assertEquals(UTC, utcWallTime.getZoneId());
          assertFields(utcWallTime, 2026, 6, 9, 10, 30, 15, 123);
      }

      @Test
      void nullZoneConversionUsesDefaultZone() {
          OADateTime ny = new OADateTime(NEW_YORK, 2026, 6, 9, 10, 30, 15, 123);

          assertEquals(CHICAGO, ny.withZoneIdSameInstant(null).getZoneId());
          assertEquals(CHICAGO, ny.withZoneIdSameWallTime(null).getZoneId());
      }

      @Test
      void equalityHashCodeAndComparisonUseEpochMillisOnly() {
          long millis = Instant.parse("2026-06-09T15:30:15.123Z").toEpochMilli();
          OADateTime chicago = new OADateTime(millis, CHICAGO);
          OADateTime newYork = new OADateTime(millis, NEW_YORK).withType(DateTimeType.ZonedInstant);
          OADateTime later = new OADateTime(millis + 1, CHICAGO);

          assertEquals(chicago, newYork);
          assertEquals(chicago.hashCode(), newYork.hashCode());
          assertEquals(0, chicago.compareTo(newYork));
          assertTrue(chicago.compareTo(null) > 0);
          assertEquals(2, chicago.compareTo("not a date"));
          assertTrue(chicago.before(later));
          assertTrue(chicago.isBefore(later));
          assertTrue(later.after(chicago));
          assertTrue(later.isAfter(chicago));
      }

      @Test
      void betweenPredicatesRespectInclusiveAndExclusiveBounds() {
          OADateTime start = new OADateTime(1_000L);
          OADateTime middle = new OADateTime(2_000L);
          OADateTime end = new OADateTime(3_000L);

          assertTrue(middle.betweenOrEqual(start, end));
          assertTrue(middle.isBetweenOrEqual(start, end));
          assertTrue(start.betweenOrEqual(start, end));
          assertFalse(start.betweenNotEqual(start, end));
          assertTrue(middle.betweenNotEqual(start, end));
          assertTrue(middle.isBetweenNotEqual(start, end));
          assertFalse(end.isBetweenNotEqual(start, end));
      }

      @Test
      void arithmeticUsesZonedDateTimeSemanticsAndPreservesTypeAndZone() {
          OADateTime base = new OADateTime(CHICAGO, 2026, 6, 9, 10, 30, 15,
          123).withType(DateTimeType.ZonedInstant);

          assertArithmetic(base, base.plusYears(1), base.getZonedDateTime().plusYears(1));
          assertArithmetic(base, base.subtractYears(1), base.getZonedDateTime().minusYears(1));
          assertArithmetic(base, base.plusMonths(2), base.getZonedDateTime().plusMonths(2));
          assertArithmetic(base, base.minusMonths(2), base.getZonedDateTime().minusMonths(2));
          assertArithmetic(base, base.plusDays(3), base.getZonedDateTime().plusDays(3));
          assertArithmetic(base, base.minusDays(3), base.getZonedDateTime().minusDays(3));
          assertArithmetic(base, base.plusDay(), base.getZonedDateTime().plusDays(1));
          assertArithmetic(base, base.minusDay(), base.getZonedDateTime().minusDays(1));
          assertArithmetic(base, base.addWeeks(2), base.getZonedDateTime().plusWeeks(2));
          assertArithmetic(base, base.minusWeeks(2), base.getZonedDateTime().minusWeeks(2));
          assertArithmetic(base, base.plusHours(4), base.getZonedDateTime().plusHours(4));
          assertArithmetic(base, base.minusHours(4), base.getZonedDateTime().minusHours(4));
          assertArithmetic(base, base.plusMinutes(5), base.getZonedDateTime().plusMinutes(5));
          assertArithmetic(base, base.minusMinutes(5), base.getZonedDateTime().minusMinutes(5));
          assertArithmetic(base, base.plusSeconds(6), base.getZonedDateTime().plusSeconds(6));
          assertArithmetic(base, base.minusSeconds(6), base.getZonedDateTime().minusSeconds(6));
          assertArithmetic(base, base.plusMilliSeconds(7), base.getZonedDateTime().plusNanos(7_000_000));
          assertArithmetic(base, base.minusMilliSeconds(7), base.getZonedDateTime().minusNanos(7_000_000));
      }

      @Test
      void periodAndDurationMethodsUseCalendarOrTimelineAsDocumented() {
          OADateTime start = new OADateTime(CHICAGO, 2024, 2, 29, 10, 0, 0, 0);
          OADateTime end = new OADateTime(CHICAGO, 2025, 2, 28, 12, 30, 45, 250);

          assertEquals(Period.between(start.getLocalDate(), end.getLocalDate()), start.betweenPeriod(end));
          assertEquals(Duration.between(start.getInstant(), end.getInstant()), start.betweenDuration(end));
          assertEquals(0, start.betweenYears(end));
          assertEquals(11, start.betweenMonths(end));
          assertEquals(365, start.betweenDays(end));
          assertEquals(Duration.between(start.getInstant(), end.getInstant()).toHours(),
          start.betweenHours(end));
          assertEquals(Duration.between(start.getInstant(), end.getInstant()).toMinutes(),
          start.betweenMinutes(end));
          assertEquals(Duration.between(start.getInstant(), end.getInstant()).getSeconds(),
          start.betweenSeconds(end));
          assertEquals(Duration.between(start.getInstant(), end.getInstant()).toMillis(),
          start.betweenMilliSeconds(end));
      }

      @Test
      void periodAndDurationMethodsReturnZeroForNullArgument() {
          OADateTime dt = new OADateTime(CHICAGO, 2026, 6, 9, 10, 30, 15, 123);

          assertEquals(Period.ZERO, dt.betweenPeriod(null));
          assertEquals(Duration.ZERO, dt.betweenDuration(null));
          assertEquals(0, dt.betweenYears(null));
          assertEquals(0, dt.betweenMonths(null));
          assertEquals(0, dt.betweenDays(null));
          assertEquals(0, dt.betweenHours(null));
          assertEquals(0, dt.betweenMinutes(null));
          assertEquals(0, dt.betweenSeconds(null));
          assertEquals(0, dt.betweenMilliSeconds(null));
      }

      @Test
      void springForwardArithmeticAndTimelineDurationFollowJavaTime() {
          OADateTime beforeGap = new OADateTime(NEW_YORK, 2026, 3, 8, 1, 30, 0,
          0).withType(DateTimeType.ZonedInstant);

          OADateTime plusHour = beforeGap.plusHours(1);
          assertEquals(beforeGap.getZonedDateTime().plusHours(1).toInstant().toEpochMilli(),
          plusHour.getTime());
          assertFields(plusHour, 2026, 3, 8, 3, 30, 0, 0);

          OADateTime plusDay = beforeGap.plusDays(1);
          assertEquals(beforeGap.getZonedDateTime().plusDays(1).toInstant().toEpochMilli(),
          plusDay.getTime());
          assertFields(plusDay, 2026, 3, 9, 1, 30, 0, 0);

          OADateTime nextDaySameWall = new OADateTime(NEW_YORK, 2026, 3, 9, 1, 30, 0, 0);
          assertEquals(23, beforeGap.betweenHours(nextDaySameWall));
      }

      @Test
      void fallBackTimelineDurationUsesActualInstants() {
          ZonedDateTime first130 = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0,
          NEW_YORK).withEarlierOffsetAtOverlap();
          ZonedDateTime second130 = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0,
          NEW_YORK).withLaterOffsetAtOverlap();

          OADateTime first = new OADateTime(first130);
          OADateTime second = new OADateTime(second130);

          assertEquals(1, first.betweenHours(second));
          assertEquals(Duration.ofHours(1), first.betweenDuration(second));
          assertEquals(0, first.betweenDays(second));
      }

      @Test
      void valueOfHandlesNullEmptyBlankAndInvalidInput() {
          assertNull(OADateTime.valueOf(null));
          assertNull(OADateTime.valueOf(""));

          long before = System.currentTimeMillis();
          OADateTime blank = OADateTime.valueOf("   ");
          long after = System.currentTimeMillis();
          assertNotNull(blank);
          assertTrue(blank.getTime() >= before);
          assertTrue(blank.getTime() <= after);

          assertNull(OADateTime.valueOf("not a date"));
          assertNull(OADateTime.valueOf("02/30/2026 10:30", "MM/dd/yyyy HH:mm", false));
          assertNull(OADateTime.valueOf("06/09/2026 10:30 trailing", "MM/dd/yyyy HH:mm", false));
      }

      @Test
      void parsingDeterminesTypeFromLocalOffsetAndRegionZoneInputs() {
          OADateTime local = OADateTime.valueOf("06/09/2026 10:30:15.123", "MM/dd/yyyy HH:mm:ss.SSS", false);
          assertNotNull(local);
          assertEquals(DateTimeType.Floating, local.getType());
          assertFields(local, 2026, 6, 9, 10, 30, 15, 123);

          OADateTime offset = OADateTime.valueOf("2026-06-09T10:30:15-05", "yyyy-MM-dd'T'HH:mm:ssX", false);
          assertNotNull(offset);
          assertEquals(DateTimeType.Instant, offset.getType());
          assertEquals(Instant.parse("2026-06-09T15:30:15Z").toEpochMilli(), offset.getTime());

          OADateTime region = OADateTime.valueOf("2026-06-09 10:30:15 America/New_York", "yyyy-MM-dd HH:mm:ssVV", false);
          assertNotNull(region);
          assertEquals(DateTimeType.ZonedInstant, region.getType());
          assertEquals(NEW_YORK, region.getZoneId());
          assertFields(region, 2026, 6, 9, 10, 30, 15, 0);
      }

      @Test
      void parsingNormalizesYyyyAndDocumentsTwoDigitYearBehavior() {
          OADateTime yyyy = OADateTime.valueOf("06/09/2026", "MM/dd/yyyy", false);
          assertNotNull(yyyy);
          assertFields(yyyy, 2026, 6, 9, 0, 0, 0, 0);

          OADateTime yy = OADateTime.valueOf("06/09/26", "MM/dd/yy", false);
          assertNotNull(yy);
          assertFields(yy, 2026, 6, 9, 0, 0, 0, 0);
      }

      @Test
      void formattingUsesInstanceSuppliedAndGlobalFormats() {
          OADateTime dt = new OADateTime(CHICAGO, 2026, 6, 9, 10, 30, 15, 123);

          OADateTime.setGlobalOutputFormat("yyyy-MM-dd HH:mm:ss.SSS");
          assertEquals("2026-06-09 10:30:15.123", dt.toString());
          assertEquals("06/09/2026 10:30", dt.toString("MM/dd/yyyy HH:mm"));
          assertEquals("2026-06-09 10:30:15.123", dt.toStringMain("yyyy-MM-dd HH:mm:ss.SSS"));

          dt.setFormat("MM/dd/yyyy HH:mm:ss.SSS");
          assertEquals("MM/dd/yyyy HH:mm:ss.SSS", dt.getFormat());
          assertEquals("06/09/2026 10:30:15.123", dt.toString());
      }

      @Test
      void staticGlobalConfigurationCanBeChangedAndRestored() {
          OADateTime.setGlobalOutputFormat("yyyyMMddHHmm");
          assertEquals("yyyyMMddHHmm", OADateTime.getGlobalOutputFormat());

          OADateTime.setDefaultZoneId(LOS_ANGELES);
          assertEquals(LOS_ANGELES, OADateTime.getDefaultZoneId());

          OADateTime.setDefaultZoneId(null);
          assertEquals(ZoneId.systemDefault(), OADateTime.getDefaultZoneId());

          OADateTime.setLocale(null);
          assertEquals(DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()) instanceof
          SimpleDateFormat
                  ? ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT,
                  Locale.getDefault())).toPattern()
                  : null,
                  OADateTime.getFormat(DateFormat.SHORT));
      }

      @Test
      void globalParseFormatsCanBeAddedAndRemoved() {
          String customFormat = "uuuu**MM**dd HH_mm_ss";
          try {
              assertNull(OADateTime.valueOf("2026**06**09 10_30_15", customFormat, false));

              OADateTime.addGlobalParseFormat(customFormat);
              OADateTime parsed = OADateTime.valueOf("2026**06**09 10_30_15");
              assertNotNull(parsed);
              assertFields(parsed, 2026, 6, 9, 10, 30, 15, 0);
          } finally {
              OADateTime.removeGlobalParseFormat(customFormat);
          }

          assertNull(OADateTime.valueOf("2026**06**09 10_30_15"));
      }

      @Test
      void serializerRoundTripsInstantAndZonedInstantAuthoritativeFields() throws Exception {
          OADateTime instant = new OADateTime(Instant.parse("2026-06-09T15:30:15.123Z"));
          OADateTime instantCopy = roundTrip(instant);
          assertEquals(DateTimeType.Instant, instantCopy.getType());
          assertEquals(instant.getTime(), instantCopy.getTime());

          OADateTime zoned = new OADateTime(ZonedDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000, NEW_YORK));
          OADateTime zonedCopy = roundTrip(zoned);
          assertEquals(DateTimeType.ZonedInstant, zonedCopy.getType());
          assertEquals(zoned.getTime(), zonedCopy.getTime());
          assertEquals(NEW_YORK, zonedCopy.getZoneId());
      }

      @Test
      void serializerRoundTripsFloatingWallClockFieldsUnderDefaultZone() throws Exception {
          OADateTime floating = new OADateTime(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000));

          OADateTime sameZoneCopy = roundTrip(floating);
          assertEquals(DateTimeType.Floating, sameZoneCopy.getType());
          assertFields(sameZoneCopy, 2026, 6, 9, 10, 30, 15, 123);
          assertEquals(floating.getTime(), sameZoneCopy.getTime());

          OADateTime.setDefaultZoneId(NEW_YORK);
          OADateTime otherZoneCopy = roundTrip(floating);
          assertEquals(DateTimeType.Floating, otherZoneCopy.getType());
          assertFields(otherZoneCopy, 2026, 6, 9, 10, 30, 15, 123);
          assertNotEquals(floating.getTime(), otherZoneCopy.getTime());
      }

      @Test
      void legacyFormatterFactoryIsNonLenient() {
          SimpleDateFormat formatter = OADateTime.getFormatter();

          assertFalse(formatter.isLenient());
      }

      private static void assertChanged(OADateTime dt, DateTimeType type, int year, int month, int day, int
      hour, int minute, int second, int millisecond) {
          assertEquals(type, dt.getType());
          assertFields(dt, year, month, day, hour, minute, second, millisecond);
      }

      private static void assertFields(OADateTime dt, int year, int month, int day, int hour, int minute, int
      second, int millisecond) {
          assertEquals(year, dt.getYear());
          assertEquals(month, dt.getMonthValue());
          assertEquals(day, dt.getDayOfMonth());
          assertEquals(hour, dt.getHour());
          assertEquals(minute, dt.getMinute());
          assertEquals(second, dt.getSecond());
          assertEquals(millisecond, dt.getMilliSecond());
      }

      private static void assertSameInstant(OADateTime expected, OADateTime actual) {
          assertEquals(expected.getTime(), actual.getTime());
          assertEquals(expected.getInstant(), actual.getInstant());
      }

      private static void assertArithmetic(OADateTime base, OADateTime actual, ZonedDateTime expected) {
          assertEquals(expected.toInstant().toEpochMilli(), actual.getTime());
          assertEquals(base.getType(), actual.getType());
          assertEquals(base.getZoneId(), actual.getZoneId());
      }

      private static OADateTime roundTrip(OADateTime dt) throws Exception {
          ByteArrayOutputStream bytes = new ByteArrayOutputStream();
          try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
              out.writeObject(dt);
          }

          try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
              return (OADateTime) in.readObject();
          }
      }
  }
