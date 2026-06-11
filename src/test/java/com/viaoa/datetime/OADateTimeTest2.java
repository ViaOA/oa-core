
/*
This proposed OADateTimeTest2 covers the missing/strengthening items from the review: Floating captured-zone stability, Floating
  serialization read-time zone capture, DST timeline/calendar edge cases, parse type inference and strictness, month-end/leap arithmetic,
  explicit formatting precedence/effective zone, and stronger _time-only equality/comparison assertions.
*/

  package com.viaoa.datetime;

  import static org.junit.jupiter.api.Assertions.*;

  import java.io.ByteArrayInputStream;
  import java.io.ByteArrayOutputStream;
  import java.io.ObjectInputStream;
  import java.io.ObjectOutputStream;
  import java.time.Duration;
  import java.time.Instant;
  import java.time.LocalDate;
  import java.time.LocalDateTime;
  import java.time.LocalTime;
  import java.time.ZoneId;
  import java.time.ZoneOffset;
  import java.time.ZonedDateTime;
  import java.util.Locale;
  import java.util.TimeZone;

  import org.junit.jupiter.api.AfterEach;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;

  import com.viaoa.datetime.OADateTime.DateTimeType;

  class OADateTimeTest2 {
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

          assertEquals(CHICAGO, OADateTime.getDefaultZoneId());
          assertEquals("MM/dd/yyyy HH:mm:ss.SSS", OADateTime.getGlobalOutputFormat());
      }

      @AfterEach
      void afterEach() {
          OADateTime.setGlobalOutputFormat(originalGlobalOutputFormat);
          OADateTime.setDefaultZoneId(originalDefaultZoneId);
          OADateTime.setLocale(originalJvmLocale);
          Locale.setDefault(originalJvmLocale);
          TimeZone.setDefault(originalJvmTimeZone);

          assertEquals(originalGlobalOutputFormat, OADateTime.getGlobalOutputFormat());
          assertEquals(originalDefaultZoneId, OADateTime.getDefaultZoneId());
          assertEquals(originalJvmLocale, Locale.getDefault());
          assertEquals(originalJvmTimeZone, TimeZone.getDefault());
      }

      @Test
      void floatingConstructorsCaptureZoneIdAndRemainStableAfterDefaultZoneChanges() {
          LocalDateTime ldt = LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000);

          OADateTime dateTime = new OADateTime(ldt);
          OADateTime date = new OADateTime(LocalDate.of(2026, 6, 9));
          OADateTime time = new OADateTime(LocalTime.of(10, 30, 15, 123_000_000));

          assertFloatingCapturedZone(dateTime, CHICAGO);
          assertFloatingCapturedZone(date, CHICAGO);
          assertFloatingCapturedZone(time, CHICAGO);

          long dateTimeMillis = ldt.atZone(CHICAGO).toInstant().toEpochMilli();
          long dateMillis = LocalDate.of(2026, 6, 9).atStartOfDay(CHICAGO).toInstant().toEpochMilli();
          long timeMillis = LocalDateTime.of(1970, 1, 1, 10, 30, 15, 123_000_000)
                  .atZone(CHICAGO).toInstant().toEpochMilli();

          assertEquals(dateTimeMillis, dateTime.getTime());
          assertEquals(dateMillis, date.getTime());
          assertEquals(timeMillis, time.getTime());

          OADateTime.setDefaultZoneId(NEW_YORK);

          assertFloatingCapturedZone(dateTime, CHICAGO);
          assertFields(dateTime, 2026, 6, 9, 10, 30, 15, 123);
          assertEquals(dateTimeMillis, dateTime.getTime());

          assertFloatingCapturedZone(date, CHICAGO);
          assertFields(date, 2026, 6, 9, 0, 0, 0, 0);
          assertEquals(dateMillis, date.getTime());

          assertFloatingCapturedZone(time, CHICAGO);
          assertFields(time, 1970, 1, 1, 10, 30, 15, 123);
          assertEquals(timeMillis, time.getTime());
      }

      @Test
      void parsedFloatingCapturesZoneIdAndRemainStableAfterDefaultZoneChanges() {
          OADateTime parsed = OADateTime.valueOf("06/09/2026 10:30:15.123", "MM/dd/yyyy HH:mm:ss.SSS", false);
          assertNotNull(parsed);
          assertFloatingCapturedZone(parsed, CHICAGO);

          long expectedMillis = LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000)
                  .atZone(CHICAGO).toInstant().toEpochMilli();
          assertEquals(expectedMillis, parsed.getTime());
          assertFields(parsed, 2026, 6, 9, 10, 30, 15, 123);

          OADateTime.setDefaultZoneId(LOS_ANGELES);

          assertFloatingCapturedZone(parsed, CHICAGO);
          assertEquals(expectedMillis, parsed.getTime());
          assertFields(parsed, 2026, 6, 9, 10, 30, 15, 123);
      }

      @Test
      void floatingDeserializationCapturesReadTimeDefaultZoneAndRemainStableAfterLaterDefaultZoneChanges() throws Exception {
          OADateTime floating = new OADateTime(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000));
          assertFloatingCapturedZone(floating, CHICAGO);

          byte[] bytes = serialize(floating);

          OADateTime.setDefaultZoneId(NEW_YORK);
          OADateTime copy = deserialize(bytes);

          assertFloatingCapturedZone(copy, CHICAGO);
          assertFields(copy, 2026, 6, 9, 10, 30, 15, 123);
          assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000)
                  .atZone(CHICAGO).toInstant().toEpochMilli(), copy.getTime());

          OADateTime.setDefaultZoneId(LOS_ANGELES);

          assertFloatingCapturedZone(copy, CHICAGO);
          assertFields(copy, 2026, 6, 9, 10, 30, 15, 123);
          assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000)
                  .atZone(CHICAGO).toInstant().toEpochMilli(), copy.getTime());
      }

      @Test
      void floatingZoneConversionsDistinguishSameInstantFromSameWallTime() {
          OADateTime floating = new OADateTime(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000));
          assertFloatingCapturedZone(floating, CHICAGO);

          OADateTime sameInstant = floating.withZoneIdSameInstant(NEW_YORK);
          assertEquals(floating.getTime(), sameInstant.getTime());
          assertEquals(NEW_YORK, sameInstant.zoneId);
          assertFields(sameInstant, 2026, 6, 9, 11, 30, 15, 123);

          OADateTime sameWallTime = floating.withZoneIdSameWallTime(NEW_YORK);
          assertNotEquals(floating.getTime(), sameWallTime.getTime());
          assertEquals(NEW_YORK, sameWallTime.zoneId);
          assertFields(sameWallTime, 2026, 6, 9, 10, 30, 15, 123);
          assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000)
                  .atZone(NEW_YORK).toInstant().toEpochMilli(), sameWallTime.getTime());
      }

      @Test
      void springForwardCalendarAndTimelineBehaviorUseJavaTimeExpectations() {
          OADateTime beforeGap = new OADateTime(NEW_YORK, 2026, 3, 8, 1, 30, 0, 0)
                  .withType(DateTimeType.ZonedInstant);

          OADateTime plusHour = beforeGap.plusHours(1);
          ZonedDateTime expectedPlusHour = beforeGap.getZonedDateTime().plusHours(1);
          assertEquals(expectedPlusHour.toInstant().toEpochMilli(), plusHour.getTime());
          assertFields(plusHour, 2026, 3, 8, 3, 30, 0, 0);

          OADateTime plusDay = beforeGap.plusDays(1);
          ZonedDateTime expectedPlusDay = beforeGap.getZonedDateTime().plusDays(1);
          assertEquals(expectedPlusDay.toInstant().toEpochMilli(), plusDay.getTime());
          assertFields(plusDay, 2026, 3, 9, 1, 30, 0, 0);

          assertEquals(Duration.between(beforeGap.getInstant(), plusDay.getInstant()), beforeGap.betweenDuration(plusDay));
          assertEquals(23, beforeGap.betweenHours(plusDay));
      }

      @Test
      void fallBackRepeatedHourUsesTimelineDurationAndCalendarDateMath() {
          ZonedDateTime first130 = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, NEW_YORK).withEarlierOffsetAtOverlap();
          ZonedDateTime second130 = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, NEW_YORK).withLaterOffsetAtOverlap();

          OADateTime first = new OADateTime(first130);
          OADateTime second = new OADateTime(second130);

          assertEquals(first130.toInstant().toEpochMilli(), first.getTime());
          assertEquals(second130.toInstant().toEpochMilli(), second.getTime());
          assertEquals(Duration.ofHours(1), first.betweenDuration(second));
          assertEquals(1, first.betweenHours(second));
          assertEquals(0, first.betweenDays(second));
      }

      @Test
      void parsingTypeInferenceAndStrictFailuresArePinned() {
          OADateTime noZone = OADateTime.valueOf("06/09/2026 10:30:15.123", "MM/dd/yyyy HH:mm:ss.SSS", false);
          assertNotNull(noZone);
          assertFloatingCapturedZone(noZone, CHICAGO);
          assertFields(noZone, 2026, 6, 9, 10, 30, 15, 123);

          OADateTime offsetOnly = OADateTime.valueOf("2026-06-09T10:30:15-05", "yyyy-MM-dd'T'HH:mm:ssX", false);
          assertNotNull(offsetOnly);
          assertEquals(DateTimeType.Instant, offsetOnly.getType());
          assertNull(offsetOnly.zoneId);
          assertEquals(Instant.parse("2026-06-09T15:30:15Z").toEpochMilli(), offsetOnly.getTime());

          OADateTime regionZone = OADateTime.valueOf("2026-06-09 10:30:15 America/New_York", "yyyy-MM-dd HH:mm:ss VV", false);
          assertNotNull(regionZone);
          assertEquals(DateTimeType.ZonedInstant, regionZone.getType());
          assertEquals(NEW_YORK, regionZone.zoneId);
          assertFields(regionZone, 2026, 6, 9, 10, 30, 15, 0);

          OADateTime dateOnly = OADateTime.valueOf("2026-06-09", "yyyy-MM-dd", false);
          assertNotNull(dateOnly);
          assertFloatingCapturedZone(dateOnly, CHICAGO);
          assertFields(dateOnly, 2026, 6, 9, 0, 0, 0, 0);

          assertNull(OADateTime.valueOf("02/30/2026 10:30", "MM/dd/yyyy HH:mm", false));
          assertNull(OADateTime.valueOf("06/09/2026 10:30 trailing", "MM/dd/yyyy HH:mm", false));
      }

      @Test
      void monthEndAndLeapYearArithmeticUseZonedDateTimeRules() {
          OADateTime monthEnd = new OADateTime(CHICAGO, 2026, 1, 31, 10, 30, 0, 0)
                  .withType(DateTimeType.ZonedInstant);

          OADateTime plusMonth = monthEnd.plusMonths(1);
          ZonedDateTime expectedPlusMonth = monthEnd.getZonedDateTime().plusMonths(1);
          assertEquals(expectedPlusMonth.toInstant().toEpochMilli(), plusMonth.getTime());
          assertEquals(monthEnd.getType(), plusMonth.getType());
          assertEquals(CHICAGO, plusMonth.zoneId);
          assertFields(plusMonth, 2026, 2, 28, 10, 30, 0, 0);

          OADateTime leapDay = new OADateTime(CHICAGO, 2024, 2, 29, 10, 30, 0, 0)
                  .withType(DateTimeType.ZonedInstant);

          OADateTime plusYear = leapDay.plusYears(1);
          assertEquals(leapDay.getZonedDateTime().plusYears(1).toInstant().toEpochMilli(), plusYear.getTime());
          assertEquals(DateTimeType.ZonedInstant, plusYear.getType());
          assertEquals(CHICAGO, plusYear.zoneId);
          assertFields(plusYear, 2025, 2, 28, 10, 30, 0, 0);

          OADateTime nextLeapDay = new OADateTime(CHICAGO, 2028, 2, 29, 10, 30, 0, 0);
          assertEquals(4, leapDay.betweenYears(nextLeapDay));
          assertEquals(48, leapDay.betweenMonths(nextLeapDay));
          assertEquals(leapDay.getLocalDate().until(nextLeapDay.getLocalDate()).getDays(), leapDay.betweenPeriod(nextLeapDay).getDays());
      }

      @Test
      void equalityHashCodeAndCompareToUseTimeOnlyAcrossTypeAndZone() {
          long millis = Instant.parse("2026-06-09T15:30:15.123Z").toEpochMilli();

          OADateTime instant = new OADateTime(millis);
          OADateTime zoned = new OADateTime(millis, NEW_YORK).withType(DateTimeType.ZonedInstant);
          OADateTime floatingType = new OADateTime(millis, LOS_ANGELES).withType(DateTimeType.Floating);

          assertEquals(instant, zoned);
          assertEquals(instant, floatingType);
          assertEquals(instant.hashCode(), zoned.hashCode());
          assertEquals(instant.hashCode(), floatingType.hashCode());
          assertEquals(0, instant.compareTo(zoned));
          assertEquals(0, instant.compareTo(floatingType));
          assertEquals(0, zoned.compare(floatingType));

          assertNotEquals(instant, new OADateTime(millis + 1, NEW_YORK).withType(DateTimeType.ZonedInstant));
      }

      @Test
      void formattingUsesEffectiveZoneAndExplicitFormatOverridesInstanceAndGlobalFormats() {
          long millis = Instant.parse("2026-06-09T15:30:15.123Z").toEpochMilli();
          OADateTime dt = new OADateTime(millis, NEW_YORK);

          OADateTime.setGlobalOutputFormat("yyyy-MM-dd HH:mm");
          dt.setFormat("MM/dd/yyyy HH:mm");

          assertEquals("06/09/2026 11:30", dt.toString());
          assertEquals("2026/06/09 11:30:15.123", dt.toString("yyyy/MM/dd HH:mm:ss.SSS"));

          OADateTime sameInstantChicago = dt.withZoneIdSameInstant(CHICAGO);
          assertEquals("2026/06/09 10:30:15.123", sameInstantChicago.toString("yyyy/MM/dd HH:mm:ss.SSS"));
      }

      @Test
      void formattingFallbackUsesBuiltInPatternAndEffectiveZone() {
          OADateTime.setGlobalOutputFormat(null);

          OADateTime zoned = new OADateTime(CHICAGO, 2026, 6, 9, 10, 30, 0, 0);
          assertEquals("2026-Jun-09 10:30AM CDT", zoned.toString());

          OADateTime instantWithoutExplicitZone = new OADateTime(
                  ZonedDateTime.of(2026, 6, 9, 10, 30, 0, 0, CHICAGO).toInstant().toEpochMilli());
          assertNull(instantWithoutExplicitZone.zoneId);
          assertEquals("2026-Jun-09 10:30AM", instantWithoutExplicitZone.toString());
      }

      @Test
      void customSerializedFormDoesNotSerializeInstanceFormat() throws Exception {
          OADateTime floating = new OADateTime(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000));
          floating.setFormat("yyyyMMdd HHmmss");

          OADateTime copy = roundTrip(floating);

          assertFloatingCapturedZone(copy, CHICAGO);
          assertFields(copy, 2026, 6, 9, 10, 30, 15, 123);
          assertNull(copy.getFormat());
      }

      private static void assertFloatingCapturedZone(OADateTime dt, ZoneId expectedZoneId) {
          assertEquals(DateTimeType.Floating, dt.getType());
          assertNotNull(dt.zoneId);
          assertEquals(expectedZoneId, dt.zoneId);
          assertEquals(expectedZoneId, dt.getZoneId());
      }

      private static void assertFields(OADateTime dt, int year, int month, int day, int hour, int minute, int second, int millisecond) {
          assertEquals(year, dt.getYear());
          assertEquals(month, dt.getMonthValue());
          assertEquals(day, dt.getDayOfMonth());
          assertEquals(hour, dt.getHour());
          assertEquals(minute, dt.getMinute());
          assertEquals(second, dt.getSecond());
          assertEquals(millisecond, dt.getMilliSecond());
      }

      private static byte[] serialize(OADateTime dt) throws Exception {
          ByteArrayOutputStream bytes = new ByteArrayOutputStream();
          try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
              out.writeObject(dt);
          }
          return bytes.toByteArray();
      }

      private static OADateTime deserialize(byte[] bytes) throws Exception {
          try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
              return (OADateTime) in.readObject();
          }
      }

      private static OADateTime roundTrip(OADateTime dt) throws Exception {
          return deserialize(serialize(dt));
      }
  }



