package com.viaoa.datetime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

class OATimeZoneTest {

    @Test
    void utcTimeZoneLookupReturnsUtc() {
        TimeZone utc = OATimeZone.getTimeZoneUTC();

        assertNotNull(utc);
        assertEquals("UTC", utc.getID());
        assertSame(utc, OATimeZone.getTimeZoneUTC());
    }

    @Test
    void namedTimeZoneConstantsResolveById() {
        assertEquals("America/Chicago", OATimeZone.getTimeZone(OATimeZone.TZ_Chicago).getID());
        assertEquals("America/New_York", OATimeZone.getTimeZoneById(OATimeZone.TZ_NewYork).getID());
        assertEquals("Asia/Tokyo", OATimeZone.getTimeZone(OATimeZone.TZ_Tokyo).getID());
    }

    @Test
    void emptyLookupUsesSystemDefault() {
        assertEquals(TimeZone.getDefault(), OATimeZone.getTimeZone(null));
        assertEquals(TimeZone.getDefault().getID(), OATimeZone.getTimeZoneById("").getID());
    }

    @Test
    void invalidTimeZoneReturnsNullInsteadOfGmtFallback() {
        assertNull(OATimeZone.getTimeZone("No/Such_Zone"));
        assertNull(OATimeZone.getTimeZoneById("No/Such_Zone"));
        assertNull(OATimeZone.getOATimeZone("No/Such_Zone"));
    }

    @Test
    void oaTimeZoneWrapperHasDisplayAndUnderlyingZone() {
        OATimeZone.TZ tz = OATimeZone.getOATimeZone(TimeZone.getTimeZone("America/Chicago"));

        assertNotNull(tz);
        assertEquals("America/Chicago", tz.id);
        assertNotNull(tz.timeZone);
        assertTrue(tz.getDisplay().contains("America/Chicago"));
    }

    @Test
    void availableZonesAreCachedAndSortedByRawOffset() {
        ArrayList<OATimeZone.TZ> zones = OATimeZone.getOATimeZones();

        assertNotNull(zones);
        assertFalse(zones.isEmpty());
        for (int i = 1; i < zones.size(); i++) {
            assertTrue(zones.get(i - 1).timeZone.getRawOffset() <= zones.get(i).timeZone.getRawOffset());
        }
    }

    @Test
    void shortNamesAreReturnedSortedAndUnique() {
        String[] names = OATimeZone.getShortNames();

        assertNotNull(names);
        assertTrue(names.length > 0);
        for (int i = 1; i < names.length; i++) {
            assertTrue(names[i - 1].compareTo(names[i]) <= 0);
            assertNotEquals(names[i - 1], names[i]);
        }
    }

    @Test
    void utcOffsetLookupReturnsMatchingWrapperWhenAvailable() {
        OATimeZone.TZ utc = OATimeZone.getUtcTimeZone(0);

        assertNotNull(utc);
        assertEquals("UTC-00", utc.utcValue);
    }
}
