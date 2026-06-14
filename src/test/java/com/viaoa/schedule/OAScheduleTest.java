package com.viaoa.schedule;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime;

class OAScheduleTest {

    @Test
    void constructorCreatesEmptySchedule() {
        OASchedule<String> schedule = new OASchedule<>();

        assertEquals(0, schedule.size());
        assertEquals(0, schedule.getSize());
        assertFalse(schedule.isEndOfList());
        assertNull(schedule.next());
        assertTrue(schedule.isEndOfList());
    }

    @Test
    void clearRangeNoOpsForEmptyNullAndReversedRanges() {
        OASchedule<String> schedule = new OASchedule<>();

        schedule.clear(null, null);
        schedule.clear(dt(11), dt(10));

        assertEquals(0, schedule.size());
    }

    @Test
    void addRangeStoresReferenceAndIgnoresInvalidInputs() {
        OASchedule<String> schedule = new OASchedule<>();

        schedule.add(null, dt(10), "ignored");
        schedule.add(dt(11), dt(10), "ignored");
        schedule.add(dt(9), dt(10), "open");

        assertEquals(1, schedule.size());
        OADateTimeRange<String> range = schedule.next();
        assertEquals(dt(9), range.getBegin());
        assertEquals(dt(10), range.getEnd());
        assertEquals("open", range.getReference());
    }

    @Test
    void addConvenienceMethodMergesOverlappingRanges() {
        OASchedule<String> schedule = new OASchedule<>();

        schedule.add(dt(9), dt(11));
        schedule.add(dt(10), dt(12), "second");

        assertEquals(1, schedule.size());
        OADateTimeRange<String> range = schedule.next();
        assertEquals(dt(9), range.getBegin());
        assertEquals(dt(12), range.getEnd());
        assertEquals(2, range.getChildren().size());
    }

    @Test
    void clearRangeSplitsExistingRange() {
        OASchedule<String> schedule = new OASchedule<>();
        schedule.add(dt(9), dt(13), "original");

        schedule.clear(dt(10), dt(12));

        assertEquals(2, schedule.size());
        OADateTimeRange<String> first = schedule.next();
        OADateTimeRange<String> second = schedule.next();
        assertEquals(dt(9), first.getBegin());
        assertEquals(dt(10), first.getEnd());
        assertEquals(dt(12), second.getBegin());
        assertEquals(dt(13), second.getEnd());
    }

    @Test
    void resetAndRewindClearEndOfListFlagForCurrentCursorContract() {
        OASchedule<String> schedule = new OASchedule<>();
        schedule.add(dt(9), dt(10), "first");

        assertEquals("first", schedule.next().getReference());
        assertNull(schedule.next());
        assertTrue(schedule.isEndOfList());

        schedule.reset();
        assertFalse(schedule.isEndOfList());
        schedule.rewind();
        assertFalse(schedule.isEndOfList());
    }

    @Test
    void nextEmptyReturnsGapsAroundRanges() {
        OASchedule<String> schedule = new OASchedule<>();
        schedule.add(dt(9), dt(10));
        schedule.add(dt(12), dt(13));

        OADateTimeRange<String> beforeFirst = schedule.nextEmpty();
        OADateTimeRange<String> between = schedule.nextEmpty();

        assertNull(beforeFirst.getBegin());
        assertEquals(dt(9), beforeFirst.getEnd());
        assertEquals(dt(10), between.getBegin());
        assertEquals(dt(12), between.getEnd());
    }

    @Test
    void clearAllRemovesRanges() {
        OASchedule<String> schedule = new OASchedule<>();
        schedule.add(dt(9), dt(10));

        schedule.clear();

        assertEquals(0, schedule.getSize());
        assertFalse(schedule.iterator().hasNext());
    }

    @Test
    void iteratorTraversesRangesUsingCurrentCursorContract() {
        OASchedule<String> schedule = new OASchedule<>();
        schedule.add(dt(9), dt(10), "first");

        Iterator<OADateTimeRange<String>> iterator = schedule.iterator();

        assertTrue(iterator.hasNext());
        assertEquals("first", iterator.next().getReference());
        assertTrue(iterator.hasNext(), "Current cursor-backed iterator reports one extra terminal element.");
        assertNull(iterator.next());
        assertFalse(iterator.hasNext());
        assertDoesNotThrow(iterator::remove);
    }

    @Test
    void isRangeAddedUsesInclusiveMatchingBoundariesAndCurrentNonMatchThrows() {
        assertFalse(scheduleWithSingleRange().isRangeAdded(null));
        assertTrue(scheduleWithSingleRange().isRangeAdded(dt(9)));
        assertTrue(scheduleWithSingleRange().isRangeAdded(dt(10)));
        assertThrows(NullPointerException.class, () -> scheduleWithSingleRange().isRangeAdded(dt(11)));
    }

    private static OASchedule<String> scheduleWithSingleRange() {
        OASchedule<String> schedule = new OASchedule<>();
        schedule.add(dt(9), dt(10));
        return schedule;
    }

    private static OADateTime dt(int hour) {
        return new OADateTime(2026, 6, 9, hour, 0, 0, 0);
    }
}
