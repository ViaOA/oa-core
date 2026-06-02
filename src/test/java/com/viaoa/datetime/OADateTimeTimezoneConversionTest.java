package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OADateTimeTimezoneConversionTest {

    private TimeZone originalJvmTz;
    private TimeZone originalOaTz;

    @BeforeEach
    void setUtc() {
        originalJvmTz = TimeZone.getDefault();
        originalOaTz = OADateTime.getDefaultTimeZone();
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utc);
        OADateTime.setDefaultTimeZone(utc);
    }

    @AfterEach
    void restore() {
        OADateTime.setDefaultTimeZone(originalOaTz);
        TimeZone.setDefault(originalJvmTz);
    }

    @Test
    void setTimeZoneChangesWallClockViewButPreservesInstant() {
        OADateTime dt = new OADateTime(2026, 4, 27, 12, 0, 0, 0);
        long before = dt.getTime();

        dt.setTimeZone(TimeZone.getTimeZone("America/Chicago"));

        assertEquals(before, dt.getTime());
        assertEquals("America/Chicago", dt.getTimeZone().getID());
    }

    @Test
    void setTimeZoneUtcUsesUtcZone() {
        OADateTime dt = new OADateTime(2026, 4, 27, 12, 0, 0, 0);

        dt.setTimeZoneUTC();

        assertEquals("UTC", dt.getTimeZone().getID());
        assertEquals("2026-05-27 12:00:00", dt.toString("yyyy-MM-dd HH:mm:ss"));
    }

    @Test
    void convertToPreservesInstantAndChangesTimezone() {
        OADateTime dt = new OADateTime(2026, 4, 27, 12, 0, 0, 0);
        long before = dt.getTime();

        OADateTime converted = dt.convertTo(TimeZone.getTimeZone("America/New_York"));

        assertNotSame(dt, converted);
        assertEquals(before, converted.getTime());
        assertEquals("America/New_York", converted.getTimeZone().getID());
    }

    @Test
    void defaultTimezoneControlsFieldConstructionWhenNoInstanceZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
        OADateTime.setDefaultTimeZone(TimeZone.getTimeZone("UTC"));

        OADateTime dt = new OADateTime(2026, 4, 27, 12, 0, 0, 0);

        assertEquals(12, dt.get24Hour());
        
        String s = dt.toString("yyyy-MM-dd HH:mm:ss");
        assertEquals("2026-05-27 17:00:00", s);
    }
}




