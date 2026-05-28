package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.TimeZone;

import org.junit.jupiter.api.Test;

class OADateTimeFinalCoverageTest {

    @Test
    void sqlAndJsonFormatConstantsAreStable() {
        assertEquals("yyyy-MM-dd", OADate.JdbcFormat);
        assertEquals("HH:mm:ss", OATime.JdbcFormat);
        assertEquals("yyyy-MM-dd HH:mm:ss", OADateTime.JdbcFormat);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss", OADateTime.JsonFormat);
        assertEquals("yyyy-MM-dd'T'HH:mm:ssX", OADateTime.JsonFormatTZ);
    }

    @Test
    void oaTimeZoneConstantsResolveToKnownTimeZones() {
        assertEquals("America/New_York", OATimeZone.TZ_Eastern);
        assertEquals("America/Chicago", OATimeZone.TZ_Central);
        assertEquals("America/Los_Angeles", OATimeZone.TZ_Pacific);

        assertEquals("UTC", OATimeZone.getTimeZoneUTC().getID());
    }

    @Test
    void oaTimeZoneLookupByIdReturnsMatchingZone() {
        TimeZone tz = OATimeZone.getTimeZone("America/Chicago");

        assertNotNull(tz);
        assertEquals("America/Chicago", tz.getID());
    }

    @Test
    void oaTimeZoneWrapperHasDisplayFields() {
        OATimeZone.TZ tz = OATimeZone.getOATimeZone(TimeZone.getTimeZone("UTC"));

        assertNotNull(tz);
        assertEquals("UTC", tz.id);
        assertNotNull(tz.utcValue);
        assertNotNull(tz.shortName);
        assertNotNull(tz.longName);
        assertTrue(tz.getDisplay().contains("UTC"));
    }

    @Test
    void addAndSubtractConvenienceMethodsPreserveSemanticType() {
        OADate date = new OADate(2026, 4, 27);
        OATime time = new OATime(7, 8, 9, 123);
        OADateTime dt = new OADateTime(2026, 4, 27, 7, 8, 9, 123);

        assertInstanceOf(OADate.class, date.addDay());
        assertInstanceOf(OATime.class, time.addHours(1));
        assertInstanceOf(OADateTime.class, dt.addHours(1));
    }
}
