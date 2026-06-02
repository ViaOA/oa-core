package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.filter.OAFilter;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectPhase2LifecycleAndIteratorTest {

    public static class Item extends OAObject {
        private String name;

        public Item() {
        }

        public Item(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class TestIterator implements OADataSourceIterator<Item> {
        final List<Item> values;
        int pos;
        volatile boolean removed;
        volatile boolean closed;
        final AtomicInteger hasNextCalls = new AtomicInteger();
        final AtomicInteger nextCalls = new AtomicInteger();

        TestIterator(List<Item> values) {
            this.values = values;
        }

        @Override
        public boolean hasNext() {
            hasNextCalls.incrementAndGet();
            return pos < values.size();
        }

        @Override
        public Item next() {
            nextCalls.incrementAndGet();
            if (pos >= values.size()) return null;
            return values.get(pos++);
        }

        @Override
        public void remove() {
            removed = true;
            closed = true;
        }

        @Override
        public String getQuery() {
            return "select * from item";
        }

        @Override
        public String getQuery2() {
            return "query2";
        }

        @Override
        public com.viaoa.graph.sibling.OASiblingHelper getSiblingHelper() {
            return null;
        }
    }

    static class TestSelect extends OASelect<Item> {
        TestIterator iterator;
        List<Item> seed = new ArrayList<>();
        RuntimeException selectFailure;
        RuntimeException nextFailure;
        AtomicBoolean selected = new AtomicBoolean();

        TestSelect() {
            super(Item.class);
        }

        @Override
        protected void _select() {
            if (selectFailure != null) throw selectFailure;

            selected.set(true);
            bHasBeenStarted = true;
            bCancelled = false;
            bHasNextCompleted = false;
            amountRead = 0;
            amountCount = seed.size();

            iterator = new TestIterator(seed) {
                @Override
                public Item next() {
                    if (nextFailure != null) throw nextFailure;
                    return super.next();
                }
            };
            query = iterator;
        }
    }

    @Test
    void closeReleasesIteratorAndStopsIteration() {
        TestSelect sel = new TestSelect();
        sel.seed.add(new Item("A"));

        assertEquals("A", sel.next().getName());

        sel.close();

        assertTrue(sel.isCancelled());
        assertTrue(sel.iterator.removed);
        assertNull(sel.next());
        assertFalse(sel.hasMore());
    }

    @Test
    void cancelReleasesIteratorAndMarksCompleted() {
        TestSelect sel = new TestSelect();
        sel.seed.add(new Item("A"));

        sel.select();
        sel.cancel();

        assertTrue(sel.isCancelled());
        assertTrue(sel.hasNextCompleted());
        assertTrue(sel.iterator.removed);
        assertNull(sel.next());
    }

    @Test
    void iteratorIsClosedOnExhaustion() {
        TestSelect sel = new TestSelect();
        sel.seed.add(new Item("A"));
        sel.seed.add(new Item("B"));

        assertEquals("A", sel.next().getName());
        assertEquals("B", sel.next().getName());
        assertNull(sel.next());

        assertTrue(sel.hasNextCompleted());
        assertTrue(sel.iterator.removed);
    }

    @Test
    void hasMoreDoesNotSkipPendingObject() {
        TestSelect sel = new TestSelect();
        sel.seed.add(new Item("A"));
        sel.seed.add(new Item("B"));

        assertTrue(sel.hasMore());
        assertTrue(sel.hasMore());

        assertEquals("A", sel.next().getName());
        assertEquals("B", sel.next().getName());
    }

    @Test
    void countMatchesSeededResults() {
        TestSelect sel = new TestSelect();
        sel.seed.add(new Item("A"));
        sel.seed.add(new Item("B"));
        sel.seed.add(new Item("C"));

        sel.select();

        assertEquals(3, sel.getCount());
    }

    @Test
    void nextFailureDoesNotMasqueradeAsEmptySuccess() {
        TestSelect sel = new TestSelect();
        sel.seed.add(new Item("A"));
        sel.nextFailure = new RuntimeException("boom");

        RuntimeException ex = assertThrows(RuntimeException.class, sel::next);

        assertEquals("boom", ex.getMessage());
        assertTrue(sel.hasBeenStarted());
    }

    @Test
    void failedSelectCanRetryWithFreshIterator() {
        TestSelect sel = new TestSelect();
        sel.selectFailure = new RuntimeException("fail");

        assertThrows(RuntimeException.class, sel::select);

        sel.selectFailure = null;
        sel.seed.add(new Item("A"));

        sel.reset();

        assertEquals("A", sel.next().getName());
        assertNull(sel.next());
    }

    @Test
    void resetCreatesFreshLifecycleAndIterator() {
        TestSelect sel = new TestSelect();
        sel.seed.add(new Item("A"));

        assertEquals("A", sel.next().getName());
        assertNull(sel.next());

        TestIterator old = sel.iterator;

        sel.reset();

        sel.seed.clear();
        sel.seed.add(new Item("B"));

        assertEquals("B", sel.next().getName());
        assertNotSame(old, sel.iterator);
    }

    @Test
    void filterRestrictsReturnedObjectsAndMaxCountsReturnedMatches() {
        TestSelect sel = new TestSelect();
        sel.seed.add(new Item("A"));
        sel.seed.add(new Item("B"));
        sel.seed.add(new Item("C"));

        sel.setFilter(new OAFilter<Item>() {
            @Override
            public boolean isUsed(Item obj) {
                return !"B".equals(obj.getName());
            }
        });
        sel.setMax(2);

        assertEquals("A", sel.next().getName());
        assertEquals("C", sel.next().getName());
        assertNull(sel.next());

        assertEquals(2, sel.getAmountRead());
    }

    @Test
    void getDataSourceQueryDelegatesToIterator() {
        TestSelect sel = new TestSelect();
        sel.seed.add(new Item("A"));

        sel.select();

        assertEquals("select * from item", sel.getDataSourceQuery());
        assertEquals("query2", sel.getDataSourceQuery2());
    }

    @Test
    void iteratorHasNextIsUsedDuringSelectionSetup() {
        TestSelect sel = new TestSelect();
        sel.seed.add(new Item("A"));

        sel.select();

        assertTrue(sel.iterator.hasNextCalls.get() > 0);
    }

    @Test
    void closeWithoutIteratorIsSafe() {
        TestSelect sel = new TestSelect();

        assertDoesNotThrow(sel::close);
        assertTrue(sel.isCancelled());
    }

    @Test
    void hasMoreFalseAfterCloseAndExhaustion() {
        TestSelect sel = new TestSelect();
        sel.seed.add(new Item("A"));

        assertTrue(sel.hasMore());
        assertEquals("A", sel.next().getName());
        assertFalse(sel.hasMore());

        sel.reset();
        sel.seed.clear();
        sel.seed.add(new Item("B"));

        sel.close();

        assertFalse(sel.hasMore());
    }
}
