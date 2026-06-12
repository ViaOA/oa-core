package com.viaoa.datetime.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.cache.OADateRangeCache.DateRange;

class OADateRangeCacheTest {
    @Test
    void findMissingGapsSplitsAroundCachedRanges() {
        TestCache cache = new TestCache();
        cache.add(range("2026-01-01", "2026-01-31", true));
        cache.add(range("2026-03-01", "2026-03-31", true));

        List<DateRange<TestObj>> missing = cache.findMissingGaps(new OADate("2026-01-01"), new OADate("2026-03-31"));

        assertEquals(1, missing.size());
        assertEquals(new OADate("2026-02-01"), missing.get(0).beginDate);
        assertEquals(new OADate("2026-02-28"), missing.get(0).endDate);
    }

    @Test
    void getCacheItemsReturnsItemsWithinRequestedRangeWithoutDuplicates() {
        TestCache cache = new TestCache();
        DateRange<TestObj> range = range("2026-01-01", "2026-01-05", true);
        range.getList().add(range.getList().get(2));
        cache.add(range);

        List<TestObj> items = cache.getCacheItems(new OADate("2026-01-02"), new OADate("2026-01-04"));
        Collections.sort(items);

        assertEquals(3, items.size());
        assertEquals(new OADate("2026-01-02"), items.get(0).date);
        assertEquals(new OADate("2026-01-03"), items.get(1).date);
        assertEquals(new OADate("2026-01-04"), items.get(2).date);
    }

    @Test
    void nullAndReversedInputsReturnNullForMissingGaps() {
        TestCache cache = new TestCache();

        assertNull(cache.findMissingGaps((DateRange<TestObj>) null));
        assertNull(cache.findMissingGaps(null, new OADate("2026-01-01")));
        assertNull(cache.findMissingGaps(new OADate("2026-01-02"), new OADate("2026-01-01")));
        assertNull(cache.getCacheItems(null, new OADate("2026-01-01")));
    }

    @Test
    void rangeWithNullListStillMarksCoverageButReturnsNoItems() {
        TestCache cache = new TestCache();
        cache.add(new DateRange<>(new OADate("2026-01-01"), new OADate("2026-01-31")));

        assertTrue(cache.findMissingGaps(new OADate("2026-01-01"), new OADate("2026-01-31")).isEmpty());
        assertTrue(cache.getCacheItems(new OADate("2026-01-01"), new OADate("2026-01-31")).isEmpty());
    }

    private static DateRange<TestObj> range(String begin, String end, boolean withItems) {
        DateRange<TestObj> range = new DateRange<>(new OADate(begin), new OADate(end));
        if (withItems) {
            List<TestObj> items = new ArrayList<>();
            OADate date = range.beginDate;
            while (!date.after(range.endDate)) {
                items.add(new TestObj(date));
                date = (OADate) date.plusDay();
            }
            range.setList(items);
        }
        return range;
    }

    private static class TestCache extends OADateRangeCache<TestObj> {
        @Override
        protected OADate getDate(TestObj obj) {
            return obj.date;
        }
    }

    private static class TestObj implements Comparable<TestObj> {
        final OADate date;

        TestObj(OADate date) {
            this.date = date;
        }

        @Override
        public int compareTo(TestObj other) {
            return this.date.compare(other.date);
        }
    }
}
