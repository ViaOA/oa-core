package com.viaoa.schedule;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime;

class OADateTimeRangeTest {

    @Test
    void constructorStoresBoundsAndReference() {
        OADateTime begin = dt(9);
        OADateTime end = dt(10);
        OADateTimeRange<String> range = new OADateTimeRange<>(begin, end, "ref");

        assertSame(begin, range.getBegin());
        assertSame(end, range.getEnd());
        assertEquals("ref", range.getReference());
    }

    @Test
    void equalsUsesBeginAndEndOnly() {
        OADateTime begin = dt(9);
        OADateTime end = dt(10);
        OADateTimeRange<String> range = new OADateTimeRange<>(begin, end, "a");
        OADateTimeRange<String> sameBounds = new OADateTimeRange<>(new OADateTime(begin), new OADateTime(end), "b");
        OADateTimeRange<String> differentEnd = new OADateTimeRange<>(begin, dt(11), "a");

        assertEquals(range, range);
        assertEquals(range, sameBounds);
        assertNotEquals(range, differentEnd);
        assertNotEquals(range, null);
        assertNotEquals(range, "not a range");
    }

    @Test
    void hashCodeUsesBeginTime() {
        OADateTime begin = dt(9);
        OADateTimeRange<String> range = new OADateTimeRange<>(begin, dt(10), "ref");
        OADateTimeRange<String> nullBegin = new OADateTimeRange<>(null, dt(10), "ref");

        assertEquals(begin.hashCode(), range.hashCode());
        assertEquals(0, nullBegin.hashCode());
    }

    @Test
    void compareToOrdersByBeginAndHandlesUnsupportedValues() {
        OADateTimeRange<String> early = new OADateTimeRange<>(dt(8), dt(9), "early");
        OADateTimeRange<String> later = new OADateTimeRange<>(dt(10), dt(11), "later");

        assertEquals(0, early.compareTo(early));
        assertTrue(early.compareTo(later) < 0);
        assertTrue(later.compareTo(early) > 0);
        assertTrue(early.compareTo(null) > 0);
        assertTrue(early.compareTo("bad") > 0);
    }

    @Test
    void toStringFormatsClosedAndOpenEndedRanges() {
        OADateTime begin = dt(9);
        OADateTime end = dt(10);

        assertEquals(begin + " to " + end, new OADateTimeRange<>(begin, end, null).toString());
        assertEquals(begin + " to forever", new OADateTimeRange<>(begin, null, null).toString());
    }

    @Test
    void addChildIgnoresNullAndLazilyCreatesChildren() {
        OADateTimeRange<String> range = new OADateTimeRange<>(dt(9), dt(10), "parent");
        OADateTimeRange<String> child = new OADateTimeRange<>(dt(9), dt(9).plusMinutes(30), "child");

        assertTrue(range.getChildren().isEmpty());
        range.addChild(null);
        assertTrue(range.getChildren().isEmpty());
        range.addChild(child);

        assertEquals(1, range.getChildren().size());
        assertSame(child, range.getChildren().get(0));
    }

    private static OADateTime dt(int hour) {
        return new OADateTime(2026, 6, 9, hour, 0, 0, 0);
    }
}
