package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OADateCoreTest {
    private TimeZone originalJvmTimeZone;
    private TimeZone originalOaTimeZone;
    private Locale originalLocale;
    private String originalOutputFormat;

    @BeforeEach
    void setUp() {
        originalJvmTimeZone = TimeZone.getDefault();
        originalOaTimeZone = OADateTime.getDefaultTimeZone();
        originalLocale = Locale.getDefault();
        originalOutputFormat = OADate.getGlobalOutputFormat();
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utc);
        OADateTime.setDefaultTimeZone(utc);
        Locale.setDefault(Locale.US);
        OADate.setLocale(Locale.US);
        OADate.setGlobalOutputFormat(OADate.Format1);
    }

    @AfterEach
    void tearDown() {
        OADate.setGlobalOutputFormat(originalOutputFormat);
        Locale.setDefault(originalLocale);
        OADate.setLocale(originalLocale);
        OADateTime.setDefaultTimeZone(originalOaTimeZone);
        TimeZone.setDefault(originalJvmTimeZone);
    }

    @Test
    void fieldConstructorPreservesDateOnlyFieldsAndClearsTime() {
        OADate date = new OADate(2026, Calendar.MAY, 18);

        assertEquals(2026, date.getYear());
        assertEquals(Calendar.MAY, date.getMonth());
        assertEquals(18, date.getDay());
        assertEquals(0, date.get24Hour());
        assertEquals(0, date.getMinute());
        assertEquals(0, date.getSecond());
        assertEquals(0, date.getMilliSecond());
    }

    @Test
    void stringParsingAndFormattingUseExplicitFormat() {
        OADate date = new OADate("2026-05-18", OADate.Format1);

        assertEquals(2026, date.getYear());
        assertEquals(Calendar.MAY, date.getMonth());
        assertEquals(18, date.getDay());
        assertEquals("2026-05-18", date.toString(OADate.Format1));
        assertEquals("05/18/2026", date.toString(OADate.Format2));
    }

    @Test
    void dateValueReturnsNullForNullBlankAndInvalidInput() {
        assertNull(OADate.dateValue(null));
        assertNull(OADate.dateValue(""));
        assertNull(OADate.dateValue("not-a-date"));
    }

    @Test
    void localDateRoundTripPreservesCalendarDayInUtc() {
        LocalDate localDate = LocalDate.of(2026, 5, 18);
        OADate date = new OADate(localDate);

        assertEquals(localDate, date.getLocalDate());
        assertEquals("2026-05-18", date.toString(OADate.Format1));
    }

    @Test
    void dateComparisonIgnoresTimeOfDayForOADate() {
        OADate date = new OADate(2026, Calendar.MAY, 18);
        OADateTime dateTime = new OADateTime(2026, Calendar.MAY, 18, 23, 59, 58, 123);

        assertEquals(0, date.compareTo(dateTime));
        assertTrue(date.equals(dateTime));
    }

    @Test
    void betweenMethodsDocumentBoundaryBehavior() {
        OADate date = new OADate(2026, Calendar.MAY, 18);

        assertTrue(date.between(new OADate(2026, Calendar.MAY, 17), new OADate(2026, Calendar.MAY, 19)));
        assertTrue(date.betweenOrEqual(new OADate(2026, Calendar.MAY, 18), new OADate(2026, Calendar.MAY, 19)));
        assertFalse(date.betweenNotEqual(new OADate(2026, Calendar.MAY, 18), new OADate(2026, Calendar.MAY, 19)));
    }

    @Test
    void addDaysReturnsSameSemanticTypeForNonZeroAmount() {
        OADate date = new OADate(2026, Calendar.MAY, 18);
        OADate added = (OADate) date.addDays(1);

        assertNotSame(date, added);
        assertEquals(new OADate(2026, Calendar.MAY, 19), added);
    }

    @Test
    void addDaysZeroCurrentlyReturnsSameInstance() {
        OADate date = new OADate(2026, Calendar.MAY, 18);

        assertNotSame(date, date.addDays(0));
    }
}
