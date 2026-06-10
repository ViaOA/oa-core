
/*
 Covers these DST gaps: nonexistent spring local construction/parsing, ambiguous fall construction/parsing, offset-aware formatting across
  both DST boundaries, Floating and ZonedInstant serialization at DST transitions, same-instant vs same-wall-time conversion across zones
  near DST, and exact 23/25-hour DST days.
*/

  package com.viaoa.datetime;

  import static org.junit.jupiter.api.Assertions.*;

  import java.io.ByteArrayInputStream;
  import java.io.ByteArrayOutputStream;
  import java.io.ObjectInputStream;
  import java.io.ObjectOutputStream;
  import java.time.Duration;
  import java.time.LocalDateTime;
  import java.time.ZoneId;
  import java.time.ZoneOffset;
  import java.time.ZonedDateTime;
  import java.util.Locale;
  import java.util.TimeZone;

  import org.junit.jupiter.api.AfterEach;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;

  import com.viaoa.datetime.OADateTime.DateTimeType;

  class OADateTimeTest3 {
      private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
      private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
      private static final ZoneId UTC = ZoneOffset.UTC;

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
          OADateTime.setDefaultZoneId(NEW_YORK);
          OADateTime.setGlobalOutputFormat("uuuu-MM-dd HH:mm:ss.SSS");
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
      void springForwardNonexistentLocalTimeConstructionUsesJavaTimeResolution() {
          LocalDateTime missingLocal = LocalDateTime.of(2026, 3, 8, 2, 30);
          ZonedDateTime expected = missingLocal.atZone(NEW_YORK);

          OADateTime dt = new OADateTime(NEW_YORK, 2026, 3, 8, 2, 30, 0, 0);

          assertEquals(DateTimeType.Instant, dt.getType());
          assertEquals(NEW_YORK, dt.zoneId);
          assertEquals(expected.toInstant().toEpochMilli(), dt.getTime());
          assertSameWallFields(expected.toLocalDateTime(), dt);
          assertFields(dt, 2026, 3, 8, 3, 30, 0, 0);
      }

      @Test
      void springForwardNonexistentRegionZoneParsingUsesJavaTimeResolution() {
          LocalDateTime missingLocal = LocalDateTime.of(2026, 3, 8, 2, 30);
          ZonedDateTime expected = missingLocal.atZone(NEW_YORK);

          OADateTime parsed = OADateTime.valueOf(
                  "2026-03-08 02:30 America/New_York",
                  "yyyy-MM-dd HH:mm VV",
                  false);

          assertNotNull(parsed);
          assertEquals(DateTimeType.ZonedInstant, parsed.getType());
          assertEquals(NEW_YORK, parsed.zoneId);
          assertEquals(expected.toInstant().toEpochMilli(), parsed.getTime());
          assertSameWallFields(expected.toLocalDateTime(), parsed);
          assertFields(parsed, 2026, 3, 8, 3, 30, 0, 0);
      }

      @Test
      void fallBackAmbiguousLocalTimeConstructionUsesJavaTimeDefaultResolution() {
          LocalDateTime ambiguousLocal = LocalDateTime.of(2026, 11, 1, 1, 30);
          ZonedDateTime expected = ambiguousLocal.atZone(NEW_YORK);

          OADateTime dt = new OADateTime(NEW_YORK, 2026, 11, 1, 1, 30, 0, 0);

          assertEquals(DateTimeType.Instant, dt.getType());
          assertEquals(NEW_YORK, dt.zoneId);
          assertEquals(expected.toInstant().toEpochMilli(), dt.getTime());
          assertEquals(expected.getOffset(), dt.getZonedDateTime().getOffset());
          assertSameWallFields(expected.toLocalDateTime(), dt);
          assertFields(dt, 2026, 11, 1, 1, 30, 0, 0);
      }

      @Test
      void fallBackAmbiguousRegionZoneParsingUsesJavaTimeDefaultResolution() {
          LocalDateTime ambiguousLocal = LocalDateTime.of(2026, 11, 1, 1, 30);
          ZonedDateTime expected = ambiguousLocal.atZone(NEW_YORK);

          OADateTime parsed = OADateTime.valueOf(
                  "2026-11-01 01:30 America/New_York",
                  "yyyy-MM-dd HH:mm VV",
                  false);

          assertNotNull(parsed);
          assertEquals(DateTimeType.ZonedInstant, parsed.getType());
          assertEquals(NEW_YORK, parsed.zoneId);
          assertEquals(expected.toInstant().toEpochMilli(), parsed.getTime());
          assertEquals(expected.getOffset(), parsed.getZonedDateTime().getOffset());
          assertSameWallFields(expected.toLocalDateTime(), parsed);
      }

      @Test
      void formattingShowsOffsetChangeAcrossSpringForwardTransition() {
          OADateTime before = new OADateTime(
                  ZonedDateTime.of(2026, 3, 8, 1, 30, 0, 0, NEW_YORK));
          OADateTime after = new OADateTime(
                  ZonedDateTime.of(2026, 3, 8, 3, 30, 0, 0, NEW_YORK));

          assertEquals("2026-03-08 01:30 -05:00", before.toString("uuuu-MM-dd HH:mm XXX"));
          assertEquals("2026-03-08 03:30 -04:00", after.toString("uuuu-MM-dd HH:mm XXX"));
      }

      @Test
      void formattingShowsOffsetChangeAcrossFallBackTransition() {
          ZonedDateTime earlierOverlap = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, NEW_YORK)
                  .withEarlierOffsetAtOverlap();
          ZonedDateTime laterOverlap = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, NEW_YORK)
                  .withLaterOffsetAtOverlap();

          OADateTime before = new OADateTime(earlierOverlap);
          OADateTime after = new OADateTime(laterOverlap);

          assertEquals("2026-11-01 01:30 -04:00", before.toString("uuuu-MM-dd HH:mm XXX"));
          assertEquals("2026-11-01 01:30 -05:00", after.toString("uuuu-MM-dd HH:mm XXX"));
      }

      @Test
      void floatingSerializationAtSpringTransitionPreservesResolvedWallFieldsAndCapturedZone() throws Exception {
          OADateTime.setDefaultZoneId(NEW_YORK);
          OADateTime floating = new OADateTime(LocalDateTime.of(2026, 3, 8, 2, 30));

          assertFloating(floating, NEW_YORK);
          assertFields(floating, 2026, 3, 8, 3, 30, 0, 0);

          OADateTime copy = roundTrip(floating);

          assertFloating(copy, NEW_YORK);
          assertFields(copy, 2026, 3, 8, 3, 30, 0, 0);

          OADateTime.setDefaultZoneId(CHICAGO);

          assertFloating(copy, NEW_YORK);
          assertFields(copy, 2026, 3, 8, 3, 30, 0, 0);
      }

      @Test
      void floatingSerializationAtFallTransitionPreservesAmbiguousWallFieldsAndCapturedZone() throws Exception {
          OADateTime.setDefaultZoneId(NEW_YORK);
          OADateTime floating = new OADateTime(LocalDateTime.of(2026, 11, 1, 1, 30));

          assertFloating(floating, NEW_YORK);
          assertFields(floating, 2026, 11, 1, 1, 30, 0, 0);

          OADateTime copy = roundTrip(floating);

          assertFloating(copy, NEW_YORK);
          assertFields(copy, 2026, 11, 1, 1, 30, 0, 0);
          assertEquals(floating.getZonedDateTime().getOffset(), copy.getZonedDateTime().getOffset());

          OADateTime.setDefaultZoneId(CHICAGO);

          assertFloating(copy, NEW_YORK);
          assertFields(copy, 2026, 11, 1, 1, 30, 0, 0);
      }

      @Test
      void zonedInstantSerializationAtSpringTransitionPreservesInstantZoneAndOffset() throws Exception {
          ZonedDateTime zdt = ZonedDateTime.of(2026, 3, 8, 3, 30, 0, 0, NEW_YORK);
          OADateTime dt = new OADateTime(zdt);

          OADateTime copy = roundTrip(dt);

          assertEquals(DateTimeType.ZonedInstant, copy.getType());
          assertEquals(NEW_YORK, copy.zoneId);
          assertEquals(zdt.toInstant().toEpochMilli(), copy.getTime());
          assertSameWallFields(zdt.toLocalDateTime(), copy);
          assertEquals(zdt.getOffset(), copy.getZonedDateTime().getOffset());
          assertEquals("2026-03-08 03:30 -04:00", copy.toString("uuuu-MM-dd HH:mm XXX"));
      }

      @Test
      void zonedInstantSerializationAtFallTransitionPreservesBothRepeatedHourInstants() throws Exception {
          ZonedDateTime earlierOverlap = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, NEW_YORK)
                  .withEarlierOffsetAtOverlap();
          ZonedDateTime laterOverlap = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, NEW_YORK)
                  .withLaterOffsetAtOverlap();

          OADateTime earlierCopy = roundTrip(new OADateTime(earlierOverlap));
          OADateTime laterCopy = roundTrip(new OADateTime(laterOverlap));

          assertEquals(DateTimeType.ZonedInstant, earlierCopy.getType());
          assertEquals(DateTimeType.ZonedInstant, laterCopy.getType());
          assertEquals(NEW_YORK, earlierCopy.zoneId);
          assertEquals(NEW_YORK, laterCopy.zoneId);

          assertEquals(earlierOverlap.toInstant().toEpochMilli(), earlierCopy.getTime());
          assertEquals(laterOverlap.toInstant().toEpochMilli(), laterCopy.getTime());
          assertNotEquals(earlierCopy, laterCopy);

          assertFields(earlierCopy, 2026, 11, 1, 1, 30, 0, 0);
          assertFields(laterCopy, 2026, 11, 1, 1, 30, 0, 0);
          assertEquals("-04:00", earlierCopy.toString("XXX"));
          assertEquals("-05:00", laterCopy.toString("XXX"));
          assertEquals(Duration.ofHours(1), earlierCopy.betweenDuration(laterCopy));
      }

      @Test
      void sameInstantAndSameWallTimeConversionsAcrossDstBoundaryRemainDistinct() {
          OADateTime newYorkBeforeGap = new OADateTime(NEW_YORK, 2026, 3, 8, 1, 30, 0, 0)
                  .withType(DateTimeType.ZonedInstant);

          OADateTime sameInstantChicago = newYorkBeforeGap.withZoneIdSameInstant(CHICAGO);
          ZonedDateTime expectedSameInstant = newYorkBeforeGap.getInstant().atZone(CHICAGO);

          assertEquals(newYorkBeforeGap.getTime(), sameInstantChicago.getTime());
          assertEquals(CHICAGO, sameInstantChicago.zoneId);
          assertSameWallFields(expectedSameInstant.toLocalDateTime(), sameInstantChicago);

          OADateTime sameWallChicago = newYorkBeforeGap.withZoneIdSameWallTime(CHICAGO);
          ZonedDateTime expectedSameWall = LocalDateTime.of(2026, 3, 8, 1, 30).atZone(CHICAGO);

          assertNotEquals(newYorkBeforeGap.getTime(), sameWallChicago.getTime());
          assertEquals(CHICAGO, sameWallChicago.zoneId);
          assertFields(sameWallChicago, 2026, 3, 8, 1, 30, 0, 0);
          assertEquals(expectedSameWall.toInstant().toEpochMilli(), sameWallChicago.getTime());
      }

      @Test
      void exactSpringForwardDayHasOneCalendarDayAndTwentyThreeTimelineHours() {
          OADateTime start = new OADateTime(NEW_YORK, 2026, 3, 8, 0, 0, 0, 0);
          OADateTime end = new OADateTime(NEW_YORK, 2026, 3, 9, 0, 0, 0, 0);

          assertEquals(1, start.betweenDays(end));
          assertEquals(23, start.betweenHours(end));
          assertEquals(Duration.ofHours(23), start.betweenDuration(end));
      }

      @Test
      void exactFallBackDayHasOneCalendarDayAndTwentyFiveTimelineHours() {
          OADateTime start = new OADateTime(NEW_YORK, 2026, 11, 1, 0, 0, 0, 0);
          OADateTime end = new OADateTime(NEW_YORK, 2026, 11, 2, 0, 0, 0, 0);

          assertEquals(1, start.betweenDays(end));
          assertEquals(25, start.betweenHours(end));
          assertEquals(Duration.ofHours(25), start.betweenDuration(end));
      }

      private static void assertFloating(OADateTime dt, ZoneId expectedZoneId) {
          assertEquals(DateTimeType.Floating, dt.getType());
          assertNotNull(dt.zoneId);
          assertEquals(expectedZoneId, dt.zoneId);
          assertEquals(expectedZoneId, dt.getZoneId());
      }

      private static void assertSameWallFields(LocalDateTime expected, OADateTime actual) {
          assertEquals(expected.getYear(), actual.getYear());
          assertEquals(expected.getMonthValue(), actual.getMonthValue());
          assertEquals(expected.getDayOfMonth(), actual.getDayOfMonth());
          assertEquals(expected.getHour(), actual.getHour());
          assertEquals(expected.getMinute(), actual.getMinute());
          assertEquals(expected.getSecond(), actual.getSecond());
          assertEquals(expected.getNano() / 1_000_000, actual.getMilliSecond());
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

