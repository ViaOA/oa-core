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

class OADateTest {
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId UTC = ZoneOffset.UTC;

    private ZoneId originalDefaultZoneId;
    private String originalDateOutputFormat;
    private String originalDateTimeOutputFormat;
    private Locale originalLocale;

    @BeforeEach
    void beforeEach() {
        originalDefaultZoneId = OADateTime.getDefaultZoneId();
        originalDateOutputFormat = OADate.getGlobalOutputFormat();
        originalDateTimeOutputFormat = OADateTime.getGlobalOutputFormat();
        originalLocale = Locale.getDefault();
        OADateTime.setDefaultZoneId(CHICAGO);
        OADateTime.setLocale(Locale.US);
        OADate.setLocale(Locale.US);
        OADate.setGlobalOutputFormat(null);
        OADateTime.setGlobalOutputFormat(null);
    }

    @AfterEach
    void afterEach() {
        OADate.removeGlobalParseFormat("yyyy/MM/dd");
        OADate.setGlobalOutputFormat(originalDateOutputFormat);
        OADateTime.setGlobalOutputFormat(originalDateTimeOutputFormat);
        OADate.setLocale(originalLocale);
        OADateTime.setLocale(originalLocale);
        OADateTime.setDefaultZoneId(originalDefaultZoneId);
    }

    @Test
    void constructorsNormalizeToDateOnlyFloating() {
        OADate explicit = new OADate(2026, 6, 9);
        OADate localDate = new OADate(LocalDate.of(2026, 6, 9));
        OADate fromDateTime = new OADate(new OADateTime(NEW_YORK, 2026, 6, 9, 23, 59, 59, 999));
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(UTC), Locale.US);
        calendar.set(2026, Calendar.JUNE, 9, 23, 59, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        OADate fromCalendar = new OADate(calendar);

        assertDateOnly(explicit, LocalDate.of(2026, 6, 9));
        assertDateOnly(localDate, LocalDate.of(2026, 6, 9));
        assertDateOnly(fromDateTime, LocalDate.of(2026, 6, 9));
        assertDateOnly(fromCalendar, LocalDate.of(2026, 6, 9));
    }

    @Test
    void dateAndLongConstructorsDropTimePortionInDefaultZone() {
        Instant morning = ZonedDateTime.of(2026, 6, 9, 0, 0, 0, 0, CHICAGO).toInstant();
        Instant evening = ZonedDateTime.of(2026, 6, 9, 23, 59, 59, 999_000_000, CHICAGO).toInstant();

        OADate fromMorningDate = new OADate(Date.from(morning));
        OADate fromEveningDate = new OADate(Date.from(evening));
        OADate fromLong = new OADate(evening.toEpochMilli());

        assertDateOnly(fromMorningDate, LocalDate.of(2026, 6, 9));
        assertDateOnly(fromEveningDate, LocalDate.of(2026, 6, 9));
        assertDateOnly(fromLong, LocalDate.of(2026, 6, 9));
        assertEquals(fromMorningDate.getTime(), fromEveningDate.getTime());
        assertEquals(fromMorningDate.getTime(), fromLong.getTime());
    }

    @Test
    void nullDateConstructorUsesCurrentDateRange() {
        LocalDate before = LocalDate.now(CHICAGO);
        OADate date = new OADate((Date) null);
        LocalDate after = LocalDate.now(CHICAGO);

        assertEquals(DateTimeType.Floating, date.getType());
        assertTrue(!date.getLocalDate().isBefore(before) && !date.getLocalDate().isAfter(after));
        assertMidnight(date);
    }

    @Test
    void invalidConstructorInputsThrowCurrentExceptions() {
        assertThrows(RuntimeException.class, () -> new OADate(2026, 0, 1));
        assertThrows(RuntimeException.class, () -> new OADate(2026, 13, 1));
        assertThrows(IllegalArgumentException.class, () -> new OADate("not a date"));
        assertThrows(NullPointerException.class, () -> new OADate((OADateTime) null));
        assertThrows(NullPointerException.class, () -> new OADate((Calendar) null));
        assertThrows(NullPointerException.class, () -> new OADate((LocalDate) null));
    }

    @Test
    void parsingMethodsReturnOADateAndNormalizeTimeAway() {
        OADate.addGlobalParseFormat("yyyy/MM/dd");

        OADate direct = OADate.dateValue("2026/06/09", "yyyy/MM/dd");
        OADate fallback = OADate.dateValue("2026/06/09");
        OADateTime value = OADate.valueOf("06/09/2026 23:59:59", "MM/dd/yyyy HH:mm:ss", false);

        assertDateOnly(direct, LocalDate.of(2026, 6, 9));
        assertDateOnly(fallback, LocalDate.of(2026, 6, 9));
        assertInstanceOf(OADate.class, value);
        assertDateOnly((OADate) value, LocalDate.of(2026, 6, 9));
        assertNull(OADate.dateValue(null));
        assertNull(OADate.dateValue("not a date"));
        assertNull(OADate.valueOf("2026/06/09 trailing", "yyyy/MM/dd", false));
    }

    @Test
    void stringConstructorsParseWithDefaultAndExplicitFormats() {
        OADate explicit = new OADate("2026/06/09", "yyyy/MM/dd");
        OADate defaultFormat = new OADate("06/09/2026");

        assertDateOnly(explicit, LocalDate.of(2026, 6, 9));
        assertDateOnly(defaultFormat, LocalDate.of(2026, 6, 9));
    }

    @Test
    void formattingUsesOADateFormatPrecedence() {
        OADate date = new OADate(2026, 6, 9);

        OADate.setGlobalOutputFormat("yyyy-MM-dd");
        assertEquals("2026-06-09", date.toString());
        date.setFormat("MM/dd/yyyy");
        assertEquals("06/09/2026", date.toString());
        assertEquals("2026/06/09", date.toString("yyyy/MM/dd"));
        OADate.setGlobalOutputFormat(null);
        OADate unformatted = new OADate(2026, 6, 9);
        assertEquals("2026-Jun-09", unformatted.toString());
    }

    @Test
    void inheritedWithMethodsReturnOADateAndKeepMidnight() {
        OADate base = new OADate(2026, 6, 9);

        assertDateOnly(base.withDateTime(2027, 7, 8, 9, 10, 11, 12), LocalDate.of(2027, 7, 8));
        assertDateOnly(base.withDate(2027, 7, 8), LocalDate.of(2027, 7, 8));
        assertDateOnly(base.withYear(2027), LocalDate.of(2027, 6, 9));
        assertDateOnly(base.withMonth(java.time.Month.JULY), LocalDate.of(2026, 7, 9));
        assertDateOnly(base.withMonthValue(8), LocalDate.of(2026, 8, 9));
        assertDateOnly(base.withDayOfMonth(10), LocalDate.of(2026, 6, 10));
        assertDateOnly(base.withoutDate(), LocalDate.of(1970, 1, 1));
        assertDateOnly(base.withDate(new OADate(2028, 2, 29)), LocalDate.of(2028, 2, 29));
        assertDateOnly(base.withDate(null), LocalDate.of(1970, 1, 1));
        assertDateOnly(base.withTime(23, 59, 59, 999), LocalDate.of(2026, 6, 9));
        assertDateOnly(base.withHours(23), LocalDate.of(2026, 6, 9));
        assertDateOnly(base.withMinutes(59), LocalDate.of(2026, 6, 9));
        assertDateOnly(base.withSeconds(59), LocalDate.of(2026, 6, 9));
        assertDateOnly(base.withMilliSeconds(999), LocalDate.of(2026, 6, 9));
        assertDateOnly(base.withoutTime(), LocalDate.of(2026, 6, 9));
        assertDateOnly(base.withoutSecondAndMilliSecond(), LocalDate.of(2026, 6, 9));
        assertDateOnly(base.withTime(new OATime(23, 59, 59, 999)), LocalDate.of(2026, 6, 9));
        assertDateOnly(base.withTime(null), LocalDate.of(2026, 6, 9));
        assertDateOnly(base, LocalDate.of(2026, 6, 9));
    }

    @Test
    void inheritedArithmeticReturnsOADateAndKeepsMidnight() {
        OADate base = new OADate(2024, 2, 29);

        assertDateOnly(base.plusYears(1), LocalDate.of(2025, 2, 28));
        assertDateOnly(base.subtractYears(1), LocalDate.of(2023, 2, 28));
        assertDateOnly(new OADate(2026, 1, 31).plusMonths(1), LocalDate.of(2026, 2, 28));
        assertDateOnly(base.minusMonths(1), LocalDate.of(2024, 1, 29));
        assertDateOnly(base.plusDays(1), LocalDate.of(2024, 3, 1));
        assertDateOnly(base.minusDays(1), LocalDate.of(2024, 2, 28));
        assertDateOnly(base.plusDay(), LocalDate.of(2024, 3, 1));
        assertDateOnly(base.minusDay(), LocalDate.of(2024, 2, 28));
        assertDateOnly(base.addWeeks(1), LocalDate.of(2024, 3, 7));
        assertDateOnly(base.minusWeeks(1), LocalDate.of(2024, 2, 22));
        assertDateOnly(base.plusHours(25), LocalDate.of(2024, 3, 1));
        assertDateOnly(base.minusHours(1), LocalDate.of(2024, 2, 28));
        assertDateOnly(base.plusMinutes(24 * 60), LocalDate.of(2024, 3, 1));
        assertDateOnly(base.minusMinutes(1), LocalDate.of(2024, 2, 28));
        assertDateOnly(base.plusSeconds(24 * 60 * 60), LocalDate.of(2024, 3, 1));
        assertDateOnly(base.minusSeconds(1), LocalDate.of(2024, 2, 28));
        assertDateOnly(base.plusMilliSeconds(24 * 60 * 60 * 1000), LocalDate.of(2024, 3, 1));
        assertDateOnly(base.minusMilliSeconds(1), LocalDate.of(2024, 2, 28));
    }

    @Test
    void zoneConversionsReturnOADateAndKeepDateOnlyInvariant() {
        OADate base = new OADate(2026, 6, 9);
        OADateTime sameInstant = base.withZoneIdSameInstant(UTC);
        OADateTime sameWall = base.withZoneIdSameWallTime(NEW_YORK);

        assertDateOnly(sameInstant, Instant.ofEpochMilli(base.getTime()).atZone(UTC).toLocalDate());
        assertEquals(UTC, sameInstant.getZoneId());
        assertDateOnly(sameWall, LocalDate.of(2026, 6, 9));
        assertEquals(NEW_YORK, sameWall.getZoneId());
        assertDateOnly(base.withTimeZoneUTCSameInstant(), Instant.ofEpochMilli(base.getTime()).atZone(UTC).toLocalDate());
        assertDateOnly(base.withTimeZoneUTCSameWallTime(), LocalDate.of(2026, 6, 9));
    }

    @Test
    void comparisonAndIntervalMethodsUseInheritedEpochMillisBehavior() {
        OADate d1 = new OADate(2026, 6, 9);
        OADate d2 = new OADate(2026, 6, 9);
        OADate d3 = new OADate(2026, 6, 10);

        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        assertTrue(d1.compareTo(d3) < 0);
        assertTrue(d1.before(d3));
        assertTrue(d3.after(d1));
        assertTrue(d2.betweenOrEqual(d1, d3));
        assertFalse(d2.betweenNotEqual(d1, d2));
        assertEquals(1, d1.betweenDays(d3));
        assertEquals(24, d1.betweenHours(d3));
        assertEquals(24 * 60, d1.betweenMinutes(d3));
        assertEquals(24 * 60 * 60, d1.betweenSeconds(d3));
        assertEquals(24L * 60 * 60 * 1000, d1.betweenMilliSeconds(d3));
    }

    @Test
    void sameDisplayedDateResolvedInDifferentZonesCanCompareUnequal() {
        OADate chicago = new OADate(2026, 6, 9);
        OADateTime.setDefaultZoneId(UTC);
        OADate utc = new OADate(2026, 6, 9);

        assertDateOnly(chicago, LocalDate.of(2026, 6, 9));
        assertDateOnly(utc, LocalDate.of(2026, 6, 9));
        assertNotEquals(chicago.getTime(), utc.getTime());
        assertNotEquals(chicago, utc);
    }

    private static void assertDateOnly(OADateTime dt, LocalDate expected) {
        assertInstanceOf(OADate.class, dt);
        assertEquals(DateTimeType.Floating, dt.getType());
        assertEquals(expected, dt.getLocalDate());
        assertMidnight(dt);
    }

    private static void assertMidnight(OADateTime dt) {
        assertEquals(LocalTime.MIDNIGHT, dt.getLocalTime());
        assertEquals(0, dt.getHour());
        assertEquals(0, dt.getMinute());
        assertEquals(0, dt.getSecond());
        assertEquals(0, dt.getMilliSecond());
    }
}
