package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.text.DateFormat;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime.DateTimeType;

class OADateTimeTest {
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId UTC = ZoneOffset.UTC;

    private ZoneId originalDefaultZoneId;
    private String originalGlobalOutputFormat;
    private Locale originalLocale;

    @BeforeEach
    void beforeEach() {
        originalDefaultZoneId = OADateTime.getDefaultZoneId();
        originalGlobalOutputFormat = OADateTime.getGlobalOutputFormat();
        originalLocale = Locale.getDefault();
        OADateTime.setDefaultZoneId(CHICAGO);
        OADateTime.setLocale(Locale.US);
        OADateTime.setGlobalOutputFormat(null);
    }

    @AfterEach
    void afterEach() {
        OADateTime.removeGlobalParseFormat("yyyy/MM/dd HH:mm:ss.SSS");
        OADateTime.removeGlobalParseFormat("yyyy/MM/dd HH:mm");
        OADateTime.setGlobalOutputFormat(originalGlobalOutputFormat);
        OADateTime.setLocale(originalLocale);
        OADateTime.setDefaultZoneId(originalDefaultZoneId);
    }

    @Test
    void noArgAndNullConstructorsUseCurrentTimeRange() {
        long before = System.currentTimeMillis();
        OADateTime dt = new OADateTime();
        OADateTime copyNull = new OADateTime((OADateTime) null);
        OADateTime instantNull = new OADateTime((Instant) null);
        OADateTime dateNull = new OADateTime((Date) null);
        long after = System.currentTimeMillis();

        assertBetween(dt.getTime(), before, after);
        assertBetween(copyNull.getTime(), before, after);
        assertBetween(instantNull.getTime(), before, after);
        assertBetween(dateNull.getTime(), before, after);
        assertEquals(DateTimeType.Instant, dt.getType());
        assertEquals(DateTimeType.Instant, instantNull.getType());
        assertEquals(DateTimeType.Instant, dateNull.getType());
    }

    @Test
    void epochMillisConstructorsPreserveInstantAndZoneMetadata() {
        long time = Instant.parse("2026-06-09T15:30:15.123Z").toEpochMilli();

        OADateTime plain = new OADateTime(time);
        OADateTime zoned = new OADateTime(time, NEW_YORK);
        OADateTime legacyZone = new OADateTime(time, TimeZone.getTimeZone(NEW_YORK));
        OADateTime nullZone = new OADateTime(time, (ZoneId) null);

        assertEquals(time, plain.getTime());
        assertEquals(DateTimeType.Instant, plain.getType());
        assertEquals(CHICAGO, plain.getZoneId());
        assertEquals(NEW_YORK, zoned.getZoneId());
        assertEquals(NEW_YORK, legacyZone.getZoneId());
        assertEquals(CHICAGO, nullZone.getZoneId());
        assertFields(zoned, 2026, 6, 9, 11, 30, 15, 123);
    }

    @Test
    void fieldConstructorsCreateFloatingValuesUsingDefaultZone() {
        OADateTime full = new OADateTime(2026, 6, 9, 10, 30, 15, 123);
        OADateTime dateOnly = new OADateTime(2026, 6, 9);
        OADateTime hourMinute = new OADateTime(2026, 6, 9, 10, 30);
        OADateTime hourMinuteSecond = new OADateTime(2026, 6, 9, 10, 30, 15);
        OADateTime monthEnum = new OADateTime(2026, Month.JUNE, 9, 10, 30, 15);

        assertEquals(DateTimeType.Floating, full.getType());
        assertEquals(CHICAGO, full.getZoneId());
        assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000).atZone(CHICAGO).toInstant().toEpochMilli(), full.getTime());
        assertFields(dateOnly, 2026, 6, 9, 0, 0, 0, 0);
        assertFields(hourMinute, 2026, 6, 9, 10, 30, 0, 0);
        assertFields(hourMinuteSecond, 2026, 6, 9, 10, 30, 15, 0);
        assertFields(monthEnum, 2026, 6, 9, 10, 30, 15, 0);
    }

    @Test
    void explicitZoneFieldConstructorUsesZoneForResolution() {
        OADateTime dt = new OADateTime(NEW_YORK, 2026, 6, 9, 10, 30, 15, 123);

        assertEquals(NEW_YORK, dt.getZoneId());
        assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000).atZone(NEW_YORK).toInstant().toEpochMilli(), dt.getTime());
        assertFields(dt, 2026, 6, 9, 10, 30, 15, 123);
    }

    @Test
    void fieldConstructorsRejectInvalidFields() {
        assertThrows(RuntimeException.class, () -> new OADateTime(2026, 6, 9, 10, 30, 15, -1));
        assertThrows(RuntimeException.class, () -> new OADateTime(2026, 6, 9, 10, 30, 15, 1000));
        assertThrows(RuntimeException.class, () -> new OADateTime(2026, 13, 9, 10, 30, 15, 0));
        assertThrows(RuntimeException.class, () -> new OADateTime(2026, 2, 30, 10, 30, 15, 0));
    }

    @Test
    void copyConstructorsPreserveExpectedState() {
        OADateTime source = new OADateTime(ZonedDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000, NEW_YORK));

        OADateTime copy = new OADateTime(source);
        OADateTime copyWithZone = new OADateTime(source, CHICAGO);

        assertEquals(source.getTime(), copy.getTime());
        assertEquals(source.getType(), copy.getType());
        assertEquals(source.getZoneId(), copy.getZoneId());
        assertEquals(source.getTime(), copyWithZone.getTime());
        assertEquals(source.getType(), copyWithZone.getType());
        assertEquals(CHICAGO, copyWithZone.getZoneId());
    }

    @Test
    void dateAndTimeConstructorComposesPublicFieldsAsFloating() {
        OADate date = new OADate(2026, 6, 9);
        OATime time = new OATime(10, 30, 15, 123);

        OADateTime dt = new OADateTime(date, time);
        OADateTime noTime = new OADateTime(date, (OATime) null);

        assertEquals(DateTimeType.Floating, dt.getType());
        assertEquals(CHICAGO, dt.getZoneId());
        assertFields(dt, 2026, 6, 9, 10, 30, 15, 123);
        assertFields(noTime, 2026, 6, 9, 0, 0, 0, 0);
    }

    @Test
    void calendarConstructorDerivesTypeFromCalendarZone() {
        Calendar defaultCalendar = Calendar.getInstance(TimeZone.getTimeZone(CHICAGO), Locale.US);
        defaultCalendar.set(2026, Calendar.JUNE, 9, 10, 30, 15);
        defaultCalendar.set(Calendar.MILLISECOND, 123);

        Calendar otherCalendar = Calendar.getInstance(TimeZone.getTimeZone(NEW_YORK), Locale.US);
        otherCalendar.setTimeInMillis(defaultCalendar.getTimeInMillis());

        OADateTime defaultZoneValue = new OADateTime(defaultCalendar);
        OADateTime otherZoneValue = new OADateTime(otherCalendar);

        assertEquals(DateTimeType.Instant, defaultZoneValue.getType());
        assertEquals(CHICAGO, defaultZoneValue.getZoneId());
        assertEquals(defaultCalendar.getTimeInMillis(), defaultZoneValue.getTime());
        assertEquals(DateTimeType.ZonedInstant, otherZoneValue.getType());
        assertEquals(NEW_YORK, otherZoneValue.getZoneId());
        assertEquals(otherCalendar.getTimeInMillis(), otherZoneValue.getTime());
    }

    @Test
    void javaTimeConstructorsCreateExpectedSemanticTypes() {
        Instant instant = Instant.parse("2026-06-09T15:30:15.123Z");
        LocalDateTime ldt = LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000);
        LocalDate ld = LocalDate.of(2026, 6, 9);
        ZonedDateTime zdt = ZonedDateTime.of(ldt, NEW_YORK);

        OADateTime fromInstant = new OADateTime(instant);
        OADateTime fromLocalDateTime = new OADateTime(ldt);
        OADateTime fromLocalDate = new OADateTime(ld);
        OADateTime fromZonedDateTime = new OADateTime(zdt);
        OADateTime fromDate = new OADateTime(Date.from(instant));

        assertEquals(DateTimeType.Instant, fromInstant.getType());
        assertEquals(instant.toEpochMilli(), fromInstant.getTime());
        assertEquals(DateTimeType.Floating, fromLocalDateTime.getType());
        assertEquals(CHICAGO, fromLocalDateTime.getZoneId());
        assertFields(fromLocalDateTime, 2026, 6, 9, 10, 30, 15, 123);
        assertEquals(DateTimeType.Floating, fromLocalDate.getType());
        assertFields(fromLocalDate, 2026, 6, 9, 0, 0, 0, 0);
        assertEquals(DateTimeType.ZonedInstant, fromZonedDateTime.getType());
        assertEquals(NEW_YORK, fromZonedDateTime.getZoneId());
        assertEquals(zdt.toInstant().toEpochMilli(), fromZonedDateTime.getTime());
        assertEquals(DateTimeType.Instant, fromDate.getType());
        assertEquals(instant.toEpochMilli(), fromDate.getTime());
    }

    @Test
    void stringConstructorsAndValueOfParseOrThrow() {
        OADateTime.addGlobalParseFormat("yyyy/MM/dd HH:mm:ss.SSS");

        OADateTime parsed = OADateTime.valueOf("2026/06/09 10:30:15.123", "yyyy/MM/dd HH:mm:ss.SSS", false);
        OADateTime constructor = new OADateTime("2026/06/09 10:30:15.123", "yyyy/MM/dd HH:mm:ss.SSS");

        assertNotNull(parsed);
        assertEquals(DateTimeType.Floating, parsed.getType());
        assertFields(parsed, 2026, 6, 9, 10, 30, 15, 123);
        assertEquals(parsed.getTime(), constructor.getTime());
        assertNull(OADateTime.valueOf(null));
        assertNull(OADateTime.valueOf(""));
        assertNull(OADateTime.valueOf("2026/06/09 10:30 trailing", "yyyy/MM/dd HH:mm", false));
        assertThrows(IllegalArgumentException.class, () -> new OADateTime("not a date"));
    }

    @Test
    void gettersAndLegacyBridgeMethodsReturnExpectedValues() {
        OADateTime dt = new OADateTime(NEW_YORK, 2026, 6, 9, 10, 30, 15, 123);

        assertEquals(2026, dt.getYear());
        assertEquals(6, dt.getMonthValue());
        assertEquals(Month.JUNE, dt.getMonth());
        assertEquals(9, dt.getDayOfMonth());
        assertEquals(10, dt.getHour());
        assertEquals(10, dt.get24Hour());
        assertEquals(30, dt.getMinute());
        assertEquals(15, dt.getSecond());
        assertEquals(123, dt.getMilliSecond());
        assertEquals(1, dt.getQuarter());
        assertEquals(DayOfWeek.TUESDAY, dt.getDayOfWeek());
        assertEquals(160, dt.getDayOfYear());
        assertEquals(2, dt.getWeekOfMonth());
        assertTrue(dt.getWeekOfYear() > 0);
        assertEquals(30, dt.getDaysInMonth());
        assertEquals(365, dt.getDaysInYear());
        assertEquals(30, dt.getLastDayOfMonth());
        assertEquals(LocalDate.of(2026, 6, 9), dt.getLocalDate());
        assertEquals(LocalTime.of(10, 30, 15, 123_000_000), dt.getLocalTime());
        assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000), dt.getLocalDateTime());
        assertEquals(ZonedDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000, NEW_YORK), dt.getZonedDateTime());
        assertEquals(dt.getTime(), dt.getInstant().toEpochMilli());
        assertEquals(2026, dt.getField(ChronoField.YEAR));
        assertEquals(TimeZone.getTimeZone(NEW_YORK), dt.getTimeZone());
        assertEquals(dt.getTime(), dt.getDate().getTime());
        assertEquals(dt.getTime(), dt.getCalendar().getTimeInMillis());
        assertEquals(TimeZone.getTimeZone(NEW_YORK), dt.getCalendar().getTimeZone());
    }

    @Test
    void withMethodsChangeRequestedFieldsAndLeaveOriginalUnchanged() {
        OADateTime base = new OADateTime(NEW_YORK, 2026, 6, 9, 10, 30, 15, 123);
        long originalTime = base.getTime();

        assertFields(base.withDateTime(2027, 7, 8, 9, 10, 11, 12), 2027, 7, 8, 9, 10, 11, 12);
        assertFields(base.withDate(2027, 7, 8), 2027, 7, 8, 10, 30, 15, 123);
        assertFields(base.withYear(2027), 2027, 6, 9, 10, 30, 15, 123);
        assertFields(base.withMonth(Month.JULY), 2026, 7, 9, 10, 30, 15, 123);
        assertFields(base.withMonthValue(8), 2026, 8, 9, 10, 30, 15, 123);
        assertFields(base.withDayOfMonth(10), 2026, 6, 10, 10, 30, 15, 123);
        assertFields(base.withoutDate(), 1970, 1, 1, 10, 30, 15, 123);
        assertFields(base.withDate(new OADate(2028, 2, 29)), 2028, 2, 29, 10, 30, 15, 123);
        assertFields(base.withDate(null), 1970, 1, 1, 10, 30, 15, 123);
        assertFields(base.withTime(1, 2, 3, 4), 2026, 6, 9, 1, 2, 3, 4);
        assertFields(base.withTime(1, 2, 3), 2026, 6, 9, 1, 2, 3, 0);
        assertFields(base.withTime(1, 2), 2026, 6, 9, 1, 2, 0, 0);
        assertFields(base.withHours(11), 2026, 6, 9, 11, 30, 15, 123);
        assertFields(base.withMinutes(31), 2026, 6, 9, 10, 31, 15, 123);
        assertFields(base.withSeconds(16), 2026, 6, 9, 10, 30, 16, 123);
        assertFields(base.withMilliSeconds(124), 2026, 6, 9, 10, 30, 15, 124);
        assertFields(base.withoutTime(), 2026, 6, 9, 0, 0, 0, 0);
        assertFields(base.withoutSecondAndMilliSecond(), 2026, 6, 9, 10, 30, 0, 0);
        assertFields(base.withTime(new OATime(5, 6, 7, 8)), 2026, 6, 9, 5, 6, 7, 8);
        assertFields(base.withTime(null), 2026, 6, 9, 0, 0, 0, 0);
        assertEquals(originalTime, base.getTime());
    }

    @Test
    void zoneConversionMethodsDistinguishInstantAndWallTime() {
        OADateTime base = new OADateTime(CHICAGO, 2026, 6, 9, 10, 30, 15, 123);

        OADateTime sameInstant = base.withZoneIdSameInstant(NEW_YORK);
        OADateTime sameWall = base.withZoneIdSameWallTime(NEW_YORK);
        OADateTime utcInstant = base.withTimeZoneUTCSameInstant();
        OADateTime utcWall = base.withTimeZoneUTCSameWallTime();

        assertEquals(base.getTime(), sameInstant.getTime());
        assertEquals(NEW_YORK, sameInstant.getZoneId());
        assertFields(sameInstant, 2026, 6, 9, 11, 30, 15, 123);
        assertNotEquals(base.getTime(), sameWall.getTime());
        assertEquals(NEW_YORK, sameWall.getZoneId());
        assertFields(sameWall, 2026, 6, 9, 10, 30, 15, 123);
        assertEquals(base.getTime(), utcInstant.getTime());
        assertEquals(UTC, utcInstant.getZoneId());
        assertEquals(UTC, utcWall.getZoneId());
        assertFields(utcWall, 2026, 6, 9, 10, 30, 15, 123);
    }

    @Test
    void comparisonMethodsUseEpochMillisOnly() {
        long time = Instant.parse("2026-06-09T15:30:15.123Z").toEpochMilli();
        OADateTime chicago = new OADateTime(time, CHICAGO);
        OADateTime newYork = new OADateTime(time, NEW_YORK);
        OADateTime earlier = new OADateTime(time - 1);
        OADateTime later = new OADateTime(time + 1);

        assertEquals(chicago, newYork);
        assertEquals(chicago.hashCode(), newYork.hashCode());
        assertEquals(0, chicago.compareTo(newYork));
        assertTrue(chicago.compareTo(null) > 0);
        assertTrue(chicago.compareTo("not a date") > 0);
        assertTrue(chicago.compare(earlier) > 0);
        assertTrue(chicago.before(later));
        assertTrue(chicago.isBefore(later));
        assertTrue(chicago.after(earlier));
        assertTrue(chicago.isAfter(earlier));
        assertTrue(chicago.betweenOrEqual(earlier, later));
        assertTrue(chicago.isBetweenOrEqual(earlier, later));
        assertTrue(chicago.betweenNotEqual(earlier, later));
        assertTrue(chicago.isBetweenNotEqual(earlier, later));
        assertFalse(chicago.betweenNotEqual(chicago, later));
    }

    @Test
    void arithmeticMethodsMatchJavaTimeAndPreserveTypeAndZone() {
        OADateTime base = new OADateTime(ZonedDateTime.of(2024, 2, 29, 10, 30, 15, 123_000_000, NEW_YORK));

        assertAdjusted(base, base.plusYears(1), base.getZonedDateTime().plusYears(1));
        assertAdjusted(base, base.subtractYears(1), base.getZonedDateTime().plusYears(-1));
        assertAdjusted(base, base.plusMonths(1), base.getZonedDateTime().plusMonths(1));
        assertAdjusted(base, base.minusMonths(1), base.getZonedDateTime().plusMonths(-1));
        assertAdjusted(base, base.plusDays(1), base.getZonedDateTime().plusDays(1));
        assertAdjusted(base, base.minusDays(1), base.getZonedDateTime().plusDays(-1));
        assertAdjusted(base, base.plusDay(), base.getZonedDateTime().plusDays(1));
        assertAdjusted(base, base.minusDay(), base.getZonedDateTime().plusDays(-1));
        assertAdjusted(base, base.addWeeks(2), base.getZonedDateTime().plusWeeks(2));
        assertAdjusted(base, base.minusWeeks(2), base.getZonedDateTime().plusDays(-14));
        assertAdjusted(base, base.plusHours(2), base.getZonedDateTime().plusHours(2));
        assertAdjusted(base, base.minusHours(2), base.getZonedDateTime().plusHours(-2));
        assertAdjusted(base, base.plusMinutes(2), base.getZonedDateTime().plusMinutes(2));
        assertAdjusted(base, base.minusMinutes(2), base.getZonedDateTime().plusMinutes(-2));
        assertAdjusted(base, base.plusSeconds(2), base.getZonedDateTime().plusSeconds(2));
        assertAdjusted(base, base.minusSeconds(2), base.getZonedDateTime().plusSeconds(-2));
        assertAdjusted(base, base.plusMilliSeconds(2), base.getZonedDateTime().plusNanos(2_000_000));
        assertAdjusted(base, base.minusMilliSeconds(2), base.getZonedDateTime().plusNanos(-2_000_000));
    }

    @Test
    void intervalMethodsReturnCalendarAndTimelineValues() {
        OADateTime start = new OADateTime(CHICAGO, 2024, 2, 29, 0, 0, 0, 0);
        OADateTime end = new OADateTime(CHICAGO, 2025, 2, 28, 1, 2, 3, 4);

        assertEquals(Period.between(start.getLocalDate(), end.getLocalDate()), start.betweenPeriod(end));
        assertEquals(Duration.between(start.getInstant(), end.getInstant()), start.betweenDuration(end));
        assertEquals(0, start.betweenYears(end));
        assertEquals(11, start.betweenMonths(end));
        assertEquals(365, start.betweenDays(end));
        assertEquals(Duration.between(start.getInstant(), end.getInstant()).toHours(), start.betweenHours(end));
        assertEquals(Duration.between(start.getInstant(), end.getInstant()).toMinutes(), start.betweenMinutes(end));
        assertEquals(Duration.between(start.getInstant(), end.getInstant()).getSeconds(), start.betweenSeconds(end));
        assertEquals(Duration.between(start.getInstant(), end.getInstant()).toMillis(), start.betweenMilliSeconds(end));
        assertEquals(Period.ZERO, start.betweenPeriod(null));
        assertEquals(Duration.ZERO, start.betweenDuration(null));
        assertEquals(0, start.betweenYears(null));
        assertEquals(0, start.betweenMonths(null));
        assertEquals(0, start.betweenDays(null));
        assertEquals(0, start.betweenHours(null));
        assertEquals(0, start.betweenMinutes(null));
        assertEquals(0, start.betweenSeconds(null));
        assertEquals(0, start.betweenMilliSeconds(null));
    }

    @Test
    void convertHandlesSupportedInputsAndCopyPolicy() {
        OADateTime original = new OADateTime(1234L, NEW_YORK);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(CHICAGO), Locale.US);
        calendar.setTimeInMillis(5678L);

        assertSame(original, OADateTime.convert(original, false));
        assertNotSame(original, OADateTime.convert(original, true));
        assertEquals(original.getTime(), OADateTime.convert(original, true).getTime());
        assertEquals(1234L, OADateTime.convert(new Date(1234L), false).getTime());
        assertEquals(1234L, OADateTime.convert(new java.sql.Time(1234L), false).getTime());
        assertEquals(1234L, OADateTime.convert(new Timestamp(1234L), false).getTime());
        assertEquals(5678L, OADateTime.convert(calendar, false).getTime());
        assertNotNull(OADateTime.convert("06/09/2026 10:30AM", false));
        assertNull(OADateTime.convert(null, false));
        assertNull(OADateTime.convert(42, false));
    }

    @Test
    void formattingAndStaticConfigurationUseExpectedPrecedence() {
        OADateTime dt = new OADateTime(NEW_YORK, 2026, 6, 9, 10, 30, 15, 123);

        OADateTime.setGlobalOutputFormat("yyyy-MM-dd HH:mm");
        assertEquals("2026-06-09 10:30", dt.toString());
        dt.setFormat("MM/dd/yyyy HH:mm:ss");
        assertEquals("06/09/2026 10:30:15", dt.toString());
        assertEquals("2026/06/09", dt.toString("yyyy/MM/dd"));
        assertEquals("2026-06-09 10:30", dt.toStringMain("yyyy-MM-dd HH:mm"));
        assertEquals("MM/dd/yyyy HH:mm:ss", dt.getFormat());
        assertEquals("yyyy-MM-dd HH:mm", OADateTime.getGlobalOutputFormat());
        assertNotNull(OADateTime.getFormat(DateFormat.SHORT));
        assertNotNull(OADateTime.getFormat(DateFormat.SHORT, Locale.US));
    }

    @Test
    void floatingCapturesZoneAndIsStableAfterDefaultZoneChange() {
        OADateTime floating = new OADateTime(LocalDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000));

        assertEquals(DateTimeType.Floating, floating.getType());
        assertEquals(CHICAGO, floating.getZoneId());
        OADateTime.setDefaultZoneId(UTC);

        assertEquals(CHICAGO, floating.getZoneId());
        assertFields(floating, 2026, 6, 9, 10, 30, 15, 123);
    }

    private static void assertAdjusted(OADateTime base, OADateTime actual, ZonedDateTime expected) {
        assertEquals(base.getType(), actual.getType());
        assertEquals(base.getZoneId(), actual.getZoneId());
        assertEquals(expected.toInstant().toEpochMilli(), actual.getTime());
        assertEquals(expected.toLocalDateTime(), actual.getLocalDateTime());
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

    private static void assertBetween(long value, long min, long max) {
        assertTrue(value >= min && value <= max, "value " + value + " outside [" + min + ", " + max + "]");
    }
}
