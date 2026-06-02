package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Internal tests for OATimeZone.
 *
 * Strategy:
 * - One test method per public production method name.
 * - Overloads are tested inside the same methodNameTest().
 * - Comments explain what each assertion is checking.
 * - Tests avoid relying on locale-specific display text when possible.
 */
public class OATimeZoneTest {

    @Test
    public void getDisplayTest() {
        // manually created TZ builds display from public fields
        OATimeZone.TZ tz = new OATimeZone.TZ();
        tz.utcValue = "UTC-06";
        tz.id = "America/Chicago";
        tz.longName = "Central Standard Time";
        tz.shortName = "CST";

        assertEquals("(UTC-06) America/Chicago (Central Standard Time/CST)", tz.getDisplay());
    }

    @Test
    public void getTimeZoneUTCTest() {
        // UTC timezone is cached and returned
        TimeZone tz = OATimeZone.getTimeZoneUTC();
        assertNotNull(tz);
        assertEquals("UTC", tz.getID());
    }

    @Test
    public void getLocalOATimeZoneTest() {
        // local timezone wrapper is available for system default zone
        OATimeZone.TZ tz = OATimeZone.getLocalOATimeZone();
        assertNotNull(tz);
        assertEquals(TimeZone.getDefault().getID(), tz.id);
    }

    @Test
    public void getLocalTimeZoneTest() {
        // local timezone is system default
        assertEquals(TimeZone.getDefault(), OATimeZone.getLocalTimeZone());
    }

    @Test
    public void getShortNamesTest() {
        // short names array is available and cached
        String[] ss1 = OATimeZone.getShortNames();
        String[] ss2 = OATimeZone.getShortNames();

        assertNotNull(ss1);
        assertTrue(ss1.length > 0);
        assertSame(ss1, ss2);
    }

    @Test
    public void getOATimeZonesTest() {
        // timezone list is available and cached
        ArrayList<OATimeZone.TZ> al1 = OATimeZone.getOATimeZones();
        ArrayList<OATimeZone.TZ> al2 = OATimeZone.getOATimeZones();

        assertNotNull(al1);
        assertFalse(al1.isEmpty());
        assertSame(al1, al2);

        // each entry has core fields populated
        OATimeZone.TZ first = al1.get(0);
        assertNotNull(first.id);
        assertNotNull(first.utcValue);
        assertNotNull(first.shortName);
        assertNotNull(first.longName);
        assertNotNull(first.timeZone);
    }

    @Test
    public void getTimeZoneTest() {
        // empty value returns system default timezone
        assertEquals(TimeZone.getDefault(), OATimeZone.getTimeZone(null));
        assertEquals(TimeZone.getDefault(), OATimeZone.getTimeZone(""));

        // direct ID lookup returns matching TimeZone
        assertEquals("UTC", OATimeZone.getTimeZone("UTC").getID());
        assertEquals("America/Chicago", OATimeZone.getTimeZone("America/Chicago").getID());

        // unknown value returns null when not resolved by Java TimeZone or OA lookup
        assertNull(OATimeZone.getTimeZone("not-a-time-zone"));
    }

    @Test
    public void getOATimeZoneTest() {
        // TimeZone overload returns matching wrapper
        OATimeZone.TZ tz1 = OATimeZone.getOATimeZone(TimeZone.getTimeZone("UTC"));
        assertNotNull(tz1);
        assertEquals("UTC", tz1.id);

        // String overload returns matching wrapper by ID
        OATimeZone.TZ tz2 = OATimeZone.getOATimeZone("America/Chicago");
        assertNotNull(tz2);
        assertEquals("America/Chicago", tz2.id);

        // empty string uses default timezone ID
        assertNotNull(OATimeZone.getOATimeZone(""));

        // null TimeZone returns null
        assertNull(OATimeZone.getOATimeZone((TimeZone) null));

        // unknown value returns null
        assertNull(OATimeZone.getOATimeZone("not-a-time-zone"));
    }

    @Test
    public void getUtcTimeZoneTest() {
        // UTC zero offset is available
        assertNotNull(OATimeZone.getUtcTimeZone(0));

        // common positive and negative offsets should not throw
        assertDoesNotThrow(() -> OATimeZone.getUtcTimeZone(-6));
        assertDoesNotThrow(() -> OATimeZone.getUtcTimeZone(9));
    }

    @Test
    public void getTimeZoneByIdTest() {
        // direct ID lookup returns TimeZone
        TimeZone tz = OATimeZone.getTimeZoneById("UTC");
        assertNotNull(tz);
        assertEquals("UTC", tz.getID());

        // empty value returns system default timezone
        assertEquals(TimeZone.getDefault(), OATimeZone.getTimeZoneById(""));

        // unknown ID returns null
        assertNull(OATimeZone.getTimeZoneById("not-a-time-zone"));
    }
}
