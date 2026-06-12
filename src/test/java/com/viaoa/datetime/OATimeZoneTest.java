package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

class OATimeZoneTest {
    @Test
    void lookupByIdUtcOffsetAndDisplayFieldsReturnsExpectedZones() {
        OATimeZone.TZ chicago = OATimeZone.getOATimeZone(OATimeZone.TZ_Chicago);

        assertNotNull(chicago);
        assertEquals("America/Chicago", chicago.id);
        assertEquals(TimeZone.getTimeZone("America/Chicago"), OATimeZone.getTimeZone("America/Chicago"));
        assertEquals(TimeZone.getTimeZone("America/Chicago"), OATimeZone.getTimeZone(chicago.getDisplay()));
        assertEquals(TimeZone.getTimeZone("UTC"), OATimeZone.getTimeZoneUTC());
        assertEquals(TimeZone.getTimeZone("UTC"), OATimeZone.getTimeZoneById("UTC"));
    }

    @Test
    void emptyLookupUsesDefaultTimeZoneAndInvalidLookupReturnsNull() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            assertEquals(TimeZone.getDefault(), OATimeZone.getTimeZone(""));
            assertEquals(TimeZone.getDefault(), OATimeZone.getTimeZoneById(""));
            assertNull(OATimeZone.getTimeZone("Not/A_Real_Zone"));
            assertNull(OATimeZone.getOATimeZone((TimeZone) null));
            assertNull(OATimeZone.getTimeZoneById("Not/A_Real_Zone"));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void utcOffsetLookupCoversCommonWholeHourOffsets() {
        assertNotNull(OATimeZone.getUtcTimeZone(-5));
        assertNotNull(OATimeZone.getUtcTimeZone(0));
        assertNotNull(OATimeZone.getUtcTimeZone(9));
    }

    @Test
    void shortNamesAreSortedAndCached() {
        String[] names = OATimeZone.getShortNames();

        assertNotNull(names);
        assertTrue(names.length > 0);
        for (int i = 1; i < names.length; i++) {
            assertTrue(names[i - 1].compareTo(names[i]) <= 0);
        }
        assertSame(names, OATimeZone.getShortNames());
    }

    @Test
    void timeZoneListIsCurrentlyMutableAndMutationAffectsReturnedCache() {
        ArrayList<OATimeZone.TZ> zones = OATimeZone.getOATimeZones();
        int size = zones.size();
        OATimeZone.TZ removed = zones.remove(size - 1);
        try {
            assertEquals(size - 1, OATimeZone.getOATimeZones().size());
        } finally {
            zones.add(removed);
        }
    }
}
