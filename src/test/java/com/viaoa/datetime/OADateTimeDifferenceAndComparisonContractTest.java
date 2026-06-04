package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OADateTimeDifferenceAndComparisonContractTest {

    private TimeZone originalJvmTimeZone;
    private TimeZone originalOaTimeZone;

    @BeforeEach
    void setUtc() {
        originalJvmTimeZone = TimeZone.getDefault();
        originalOaTimeZone = OADateTime.getDefaultTimeZone();
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utc);
        OADateTime.setDefaultTimeZone(utc);
    }

    @AfterEach
    void restore() {
        OADateTime.setDefaultTimeZone(originalOaTimeZone);
        TimeZone.setDefault(originalJvmTimeZone);
    }

    @Test
    void betweenYearsCurrentlyUsesCalendarYearBoundaryRule() {
        OADate start = new OADate(2025, Calendar.DECEMBER, 31);
        OADate end = new OADate(2026, Calendar.JANUARY, 1);

        assertEquals(1, start.betweenYears(end), "Current behavior counts calendar-year boundary, not full elapsed year.");
    }

    @Test
    void betweenMonthsCurrentlyUsesCalendarMonthBoundaryRule() {
        OADate start = new OADate(2026, Calendar.JANUARY, 31);
        OADate end = new OADate(2026, Calendar.FEBRUARY, 1);

        assertEquals(1, start.betweenMonths(end), "Current behavior counts calendar-month boundary, not full elapsed month.");
    }

    @Test
    void betweenDaysHoursMinutesSecondsAndMillisAreDeterministic() {
        OADateTime start = new OADateTime(2026, Calendar.MAY, 18, 10, 0, 0, 0);
        OADateTime end =   new OADateTime(2026, Calendar.MAY, 19, 11, 2, 3, 4);

        assertEquals(1, start.betweenDays(end));
        assertEquals(25, start.betweenHours(end));
        assertEquals(1502, start.betweenMinutes(end));
        assertEquals(90123, start.betweenSeconds(end));
        assertEquals(90123004L, start.betweenMilliSeconds(end));
    }

    @Test
    void afterNonComparableCurrentlyReturnsTrue() {
        OADateTime dt = new OADateTime(2026, Calendar.MAY, 18, 10, 0, 0, 0);

        assertTrue(dt.after(new Object()), "Current compareTo sentinel makes after(nonComparable) true.");
        assertFalse(dt.before(new Object()));
    }

    @Test
    void compareToEqualValuesReturnsZeroAndEqualsTrue() {
        OADateTime a = new OADateTime(2026, Calendar.MAY, 18, 10, 0, 0, 123);
        OADateTime b = new OADateTime(a.getTime());

        assertEquals(0, a.compareTo(b));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
