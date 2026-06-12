package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime.DateTimeType;

class OATimeTest {
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId UTC = ZoneOffset.UTC;

    private ZoneId originalDefaultZoneId;
    private String originalTimeOutputFormat;
    private String originalDateTimeOutputFormat;
    private Locale originalLocale;

    @BeforeEach
    void beforeEach() {
        originalDefaultZoneId = OADateTime.getDefaultZoneId();
        originalTimeOutputFormat = OATime.getGlobalOutputFormat();
        originalDateTimeOutputFormat = OADateTime.getGlobalOutputFormat();
        originalLocale = Locale.getDefault();
        OADateTime.setDefaultZoneId(CHICAGO);
        OADateTime.setLocale(Locale.US);
        OATime.setGlobalOutputFormat(null);
        OADateTime.setGlobalOutputFormat(null);
    }

    @AfterEach
    void afterEach() {
        OATime.removeGlobalParseFormat("HH|mm|ss");
        OATime.setGlobalOutputFormat(originalTimeOutputFormat);
        OADateTime.setGlobalOutputFormat(originalDateTimeOutputFormat);
        OADateTime.setLocale(originalLocale);
        OADateTime.setDefaultZoneId(originalDefaultZoneId);
    }

    @Test
    void constructorsNormalizeToTimeOnlyFloating() {
        OATime fields = new OATime(10, 30, 15, 123);
        OATime noMillis = new OATime(10, 30, 15);
        OATime localTime = new OATime(LocalTime.of(10, 30, 15, 123_456_789));
        OATime dateTime = new OATime(new OADateTime(NEW_YORK, 2026, 6, 9, 10, 30, 15, 123));
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(NEW_YORK), Locale.US);
        calendar.set(2026, Calendar.JUNE, 9, 10, 30, 15);
        calendar.set(Calendar.MILLISECOND, 123);
        OATime fromCalendar = new OATime(calendar);

        assertTimeOnly(fields, LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(noMillis, LocalTime.of(10, 30, 15));
        assertTimeOnly(localTime, LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(dateTime, LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(fromCalendar, LocalTime.of(10, 30, 15, 123_000_000));
    }

    @Test
    void dateAndLongConstructorsDeriveTimeAndDropDateInDefaultZone() {
        Instant instant = ZonedDateTime.of(2026, 6, 9, 10, 30, 15, 123_000_000, CHICAGO).toInstant();

        OATime fromLong = new OATime(instant.toEpochMilli());
        OATime fromDate = new OATime(Date.from(instant));

        assertTimeOnly(fromLong, LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(fromDate, LocalTime.of(10, 30, 15, 123_000_000));
    }

    @Test
    void invalidConstructorInputsThrowCurrentExceptions() {
        assertThrows(RuntimeException.class, () -> new OATime(24, 0, 0, 0));
        assertThrows(RuntimeException.class, () -> new OATime(1, 60, 0, 0));
        assertThrows(RuntimeException.class, () -> new OATime(1, 0, 60, 0));
        assertThrows(RuntimeException.class, () -> new OATime(1, 0, 0, 1000));
        assertThrows(NullPointerException.class, () -> new OATime((Date) null));
        assertThrows(NullPointerException.class, () -> new OATime((Calendar) null));
        assertThrows(NullPointerException.class, () -> new OATime((OADateTime) null));
        assertThrows(NullPointerException.class, () -> new OATime((LocalTime) null));
        assertThrows(IllegalArgumentException.class, () -> new OATime("not a time"));
    }

    @Test
    void parsingMethodsSupportTimeOnlyFormatsAndNormalize() {
        OATime.addGlobalParseFormat("HH|mm|ss");

        assertTimeOnly(OATime.timeValue("10:30"), LocalTime.of(10, 30));
        assertTimeOnly(OATime.timeValue("10:30:15"), LocalTime.of(10, 30, 15));
        assertTimeOnly(OATime.timeValue("10:30AM"), LocalTime.of(10, 30));
        assertTimeOnly(OATime.timeValue("10:30:15PM"), LocalTime.of(22, 30, 15));
        assertTimeOnly(OATime.timeValue("10|30|15"), LocalTime.of(10, 30, 15));
        assertTimeOnly(OATime.timeValue("10-30-15", "HH-mm-ss"), LocalTime.of(10, 30, 15));
        assertInstanceOf(OATime.class, OATime.valueOf("10:30"));
        assertNull(OATime.timeValue(null));
        assertNull(OATime.timeValue("not a time"));
        assertNull(OATime.valueOf("10:30 trailing"));
    }

    @Test
    void stringConstructorsParseDefaultAndExplicitFormats() {
        assertTimeOnly(new OATime("10:30:15PM"), LocalTime.of(22, 30, 15));
        assertTimeOnly(new OATime("10-30-15", "HH-mm-ss"), LocalTime.of(10, 30, 15));
    }

    @Test
    void formattingUsesOATimeFormatPrecedence() {
        OATime time = new OATime(10, 30, 15, 123);

        OATime.setGlobalOutputFormat("HH:mm:ss");
        assertEquals("10:30:15", time.toString());
        time.setFormat("HH:mm");
        assertEquals("10:30", time.toString());
        assertEquals("10:30:15.123", time.toString("HH:mm:ss.SSS"));
        assertEquals("1970-01-01 10:30", time.toString("yyyy-MM-dd HH:mm"));
        OATime.setGlobalOutputFormat(null);
        OATime fallback = new OATime(10, 30, 0, 0);
        assertEquals("10:30AM", fallback.toString());
    }

    @Test
    void inheritedWithMethodsReturnOATimeAndKeepEpochDate() {
        OATime base = new OATime(10, 30, 15, 123);

        assertTimeOnly(base.withDateTime(2027, 7, 8, 9, 10, 11, 12), LocalTime.of(9, 10, 11, 12_000_000));
        assertTimeOnly(base.withDate(2027, 7, 8), LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(base.withYear(2027), LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(base.withMonth(java.time.Month.JULY), LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(base.withMonthValue(8), LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(base.withDayOfMonth(10), LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(base.withoutDate(), LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(base.withDate(new OADate(2028, 2, 29)), LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(base.withDate(null), LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(base.withTime(1, 2, 3, 4), LocalTime.of(1, 2, 3, 4_000_000));
        assertTimeOnly(base.withTime(1, 2, 3), LocalTime.of(1, 2, 3));
        assertTimeOnly(base.withTime(1, 2), LocalTime.of(1, 2));
        assertTimeOnly(base.withHours(11), LocalTime.of(11, 30, 15, 123_000_000));
        assertTimeOnly(base.withMinutes(31), LocalTime.of(10, 31, 15, 123_000_000));
        assertTimeOnly(base.withSeconds(16), LocalTime.of(10, 30, 16, 123_000_000));
        assertTimeOnly(base.withMilliSeconds(124), LocalTime.of(10, 30, 15, 124_000_000));
        assertTimeOnly(base.withoutTime(), LocalTime.MIDNIGHT);
        assertTimeOnly(base.withoutSecondAndMilliSecond(), LocalTime.of(10, 30));
        assertTimeOnly(base.withTime(new OATime(5, 6, 7, 8)), LocalTime.of(5, 6, 7, 8_000_000));
        assertTimeOnly(base.withTime(null), LocalTime.MIDNIGHT);
        assertTimeOnly(base, LocalTime.of(10, 30, 15, 123_000_000));
    }

    @Test
    void inheritedArithmeticReturnsOATimeAndKeepsEpochDate() {
        OATime base = new OATime(23, 59, 59, 999);

        assertTimeOnly(base.plusYears(1), LocalTime.of(23, 59, 59, 999_000_000));
        assertTimeOnly(base.subtractYears(1), LocalTime.of(23, 59, 59, 999_000_000));
        assertTimeOnly(base.plusMonths(1), LocalTime.of(23, 59, 59, 999_000_000));
        assertTimeOnly(base.minusMonths(1), LocalTime.of(23, 59, 59, 999_000_000));
        assertTimeOnly(base.plusDays(1), LocalTime.of(23, 59, 59, 999_000_000));
        assertTimeOnly(base.minusDays(1), LocalTime.of(23, 59, 59, 999_000_000));
        assertTimeOnly(base.plusDay(), LocalTime.of(23, 59, 59, 999_000_000));
        assertTimeOnly(base.minusDay(), LocalTime.of(23, 59, 59, 999_000_000));
        assertTimeOnly(base.addWeeks(1), LocalTime.of(23, 59, 59, 999_000_000));
        assertTimeOnly(base.minusWeeks(1), LocalTime.of(23, 59, 59, 999_000_000));
        assertTimeOnly(base.plusHours(1), LocalTime.of(0, 59, 59, 999_000_000));
        assertTimeOnly(base.minusHours(24), LocalTime.of(23, 59, 59, 999_000_000));
        assertTimeOnly(base.plusMinutes(1), LocalTime.of(0, 0, 59, 999_000_000));
        assertTimeOnly(base.minusMinutes(60), LocalTime.of(22, 59, 59, 999_000_000));
        assertTimeOnly(base.plusSeconds(1), LocalTime.of(0, 0, 0, 999_000_000));
        assertTimeOnly(base.minusSeconds(60), LocalTime.of(23, 58, 59, 999_000_000));
        assertTimeOnly(base.plusMilliSeconds(1), LocalTime.MIDNIGHT);
        assertTimeOnly(base.minusMilliSeconds(999), LocalTime.of(23, 59, 59));
    }

    @Test
    void zoneConversionsReturnOATimeAndKeepTimeOnlyInvariant() {
        OATime base = new OATime(10, 30, 15, 123);
        OADateTime sameInstant = base.withZoneIdSameInstant(UTC);
        OADateTime sameWall = base.withZoneIdSameWallTime(NEW_YORK);

        assertTimeOnly(sameInstant, Instant.ofEpochMilli(base.getTime()).atZone(UTC).toLocalTime());
        assertTimeOnly(sameWall, LocalTime.of(10, 30, 15, 123_000_000));
        assertTimeOnly(base.withTimeZoneUTCSameInstant(), Instant.ofEpochMilli(base.getTime()).atZone(UTC).toLocalTime());
        assertTimeOnly(base.withTimeZoneUTCSameWallTime(), LocalTime.of(10, 30, 15, 123_000_000));
    }

    @Test
    void comparisonAndIntervalMethodsUseInheritedEpochMillisBehavior() {
        OATime t1 = new OATime(10, 30, 0, 0);
        OATime t2 = new OATime(10, 30, 0, 0);
        OATime t3 = new OATime(11, 30, 0, 0);

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.compareTo(t3) < 0);
        assertTrue(t1.before(t3));
        assertTrue(t3.after(t1));
        assertTrue(t2.betweenOrEqual(t1, t3));
        assertFalse(t2.betweenNotEqual(t1, t2));
        assertEquals(0, t1.betweenDays(t3));
        assertEquals(1, t1.betweenHours(t3));
        assertEquals(60, t1.betweenMinutes(t3));
        assertEquals(3600, t1.betweenSeconds(t3));
        assertEquals(3_600_000, t1.betweenMilliSeconds(t3));
    }

    @Test
    void sameDisplayedTimeResolvedInDifferentZonesCanCompareUnequal() {
        OATime chicago = new OATime(15, 25, 0, 0);
        OADateTime.setDefaultZoneId(UTC);
        OATime utc = new OATime(15, 25, 0, 0);

        assertTimeOnly(chicago, LocalTime.of(15, 25));
        assertTimeOnly(utc, LocalTime.of(15, 25));
        assertNotEquals(chicago.getTime(), utc.getTime());
        assertNotEquals(chicago, utc);
    }

    private static void assertTimeOnly(OADateTime dt, LocalTime expected) {
        assertInstanceOf(OATime.class, dt);
        assertEquals(DateTimeType.Floating, dt.getType());
        assertEquals(LocalDate.of(1970, 1, 1), dt.getLocalDate());
        assertEquals(expected, dt.getLocalTime());
        assertEquals(1970, dt.getYear());
        assertEquals(1, dt.getMonthValue());
        assertEquals(1, dt.getDayOfMonth());
    }
}
