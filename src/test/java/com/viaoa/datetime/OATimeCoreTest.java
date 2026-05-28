package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OATimeCoreTest {
    private TimeZone originalJvmTimeZone;
    private TimeZone originalOaTimeZone;
    private Locale originalLocale;
    private String originalOutputFormat;

    @BeforeEach
    void setUp() {
        originalJvmTimeZone = TimeZone.getDefault();
        originalOaTimeZone = OADateTime.getDefaultTimeZone();
        originalLocale = Locale.getDefault();
        originalOutputFormat = OATime.getGlobalOutputFormat();
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utc);
        OADateTime.setDefaultTimeZone(utc);
        Locale.setDefault(Locale.US);
        OATime.setGlobalOutputFormat(OATime.Format6);
    }

    @AfterEach
    void tearDown() {
        OATime.setGlobalOutputFormat(originalOutputFormat);
        Locale.setDefault(originalLocale);
        OADateTime.setDefaultTimeZone(originalOaTimeZone);
        TimeZone.setDefault(originalJvmTimeZone);
    }

    @Test
    void fieldConstructorPreservesClockFieldsAndClearsDate() {
        OATime time = new OATime(7, 8, 9, 123);

        assertEquals(0, time.getYear());
        assertEquals(0, time.getMonth());
        assertEquals(0, time.getDay());
        assertEquals(7, time.get24Hour());
        assertEquals(8, time.getMinute());
        assertEquals(9, time.getSecond());
        assertEquals(123, time.getMilliSecond());
    }

    @Test
    void stringParsingAndFormattingUseExplicitFormat() {
        OATime time = new OATime("07:08:09.123", OATime.Format6);

        assertEquals(7, time.get24Hour());
        assertEquals(8, time.getMinute());
        assertEquals(9, time.getSecond());
        assertEquals(123, time.getMilliSecond());
        assertEquals("07:08:09.123", time.toString(OATime.Format6));
    }

    @Test
    void localTimeRoundTripPreservesMillis() {
        LocalTime localTime = LocalTime.of(7, 8, 9, 123_000_000);
        OATime time = new OATime(localTime);

        assertEquals(localTime, time.getLocalTime());
    }

    @Test
    void timeValueReturnsNullForNullBlankAndInvalidInput() {
        assertNull(OATime.timeValue(null));
        assertNull(OATime.timeValue(""));
        assertNull(OATime.timeValue("not-a-time"));
    }

    @Test
    void stringConstructorForInvalidInputCurrentlyThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new OATime("not-a-time"));
    }

    @Test
    void timeComparisonIgnoresDatePortionForOATime() {
        OATime time = new OATime(7, 8, 9, 123);
        OADateTime dateTime = new OADateTime(2026, 4, 18, 7, 8, 9, 123);

        assertEquals(0, time.compare(dateTime));
    }

    @Test
    void addDaysOnTimeReturnsIndependentTimeForNonZeroAmount() {
        OATime time = new OATime(7, 8, 9, 123);
        OADateTime added = time.addDays(1);

        assertInstanceOf(OATime.class, added);
        assertNotSame(time, added);
        assertEquals(7, added.get24Hour());
        assertEquals(8, added.getMinute());
    }

    @Test
    void addDaysZeroCurrentlyReturnsSameInstance() {
        OATime time = new OATime(7, 8, 9, 123);

        assertSame(time, time.addDays(0));
    }
}
