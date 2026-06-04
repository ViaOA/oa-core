package com.viaoa.datetime;

import java.util.ArrayList;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class OATimeZoneInvariantTest {
    @Test
    public void utcTimezoneLookupReturnsUtc() {
        TimeZone tz = OATimeZone.getTimeZoneUTC();

        assertNotNull(tz);
        assertEquals("UTC", tz.getID());
    }

    @Test
    public void getTimeZoneByIdFindsExactIanaIds() {
        TimeZone chicago = OATimeZone.getTimeZoneById("America/Chicago");
        TimeZone ny = OATimeZone.getTimeZoneById("America/New_York");

        assertNotNull(chicago);
        assertNotNull(ny);
        assertEquals("America/Chicago", chicago.getID());
        assertEquals("America/New_York", ny.getID());
    }

    @Test
    public void getOATimeZoneFindsExactId() {
        OATimeZone.TZ tz = OATimeZone.getOATimeZone("America/Chicago");

        assertNotNull(tz);
        assertEquals("America/Chicago", tz.id);
        assertNotNull(tz.timeZone);
    }

    @Test
    public void getOATimeZoneFindsUtcOffsetFallbackWhenAvailable() {
        OATimeZone.TZ tz = OATimeZone.getOATimeZone("UTC-06");

        assertNotNull(tz, "UTC offset fallback should find at least one matching zone");
        assertEquals("UTC-06", tz.utcValue);
        assertNotNull(tz.timeZone);
    }

    @Test
    public void getOATimeZoneFindsDisplayFallback() {
        OATimeZone.TZ chicago = OATimeZone.getOATimeZone("America/Chicago");
        assertNotNull(chicago);

        OATimeZone.TZ byDisplay = OATimeZone.getOATimeZone(chicago.getDisplay());

        assertNotNull(byDisplay);
        assertEquals(chicago.id, byDisplay.id);
    }

    @Test
    public void getOATimeZonesReturnsNonEmptyList() {
        ArrayList<OATimeZone.TZ> zones = OATimeZone.getOATimeZones();

        assertNotNull(zones);
        assertTrue(zones.size() > 0);
    }

    @Test
    public void ambiguousAbbreviationLookupIsOnlyRequiredToBeNonNullWhenPresent() {
        OATimeZone.TZ tz = OATimeZone.getOATimeZone("CST");

        assertNotNull(tz, "Ambiguous abbreviation behavior is deferred, but lookup should not be broken");
        assertNotNull(tz.timeZone);
    }
}