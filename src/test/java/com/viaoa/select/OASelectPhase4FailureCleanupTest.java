package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectPhase4FailureCleanupTest {

    public static class Item extends OAObject {
        private String name;
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    static class FailingIterator implements OADataSourceIterator<Item> {
        int pos;
        AtomicInteger removeCalls = new AtomicInteger();

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Item next() {
            if (pos++ == 0) return new Item("A");
            throw new IllegalStateException("next failed");
        }

        @Override
        public void remove() {
            removeCalls.incrementAndGet();
        }

        @Override
        public String getQuery() { return "fail"; }

        @Override
        public String getQuery2() { return "fail2"; }

        @Override
        public com.viaoa.graph.sibling.OASiblingHelper getSiblingHelper() { return null; }
    }

    static class TestSelect extends OASelect<Item> {
        FailingIterator iterator = new FailingIterator();

        TestSelect() {
            super(Item.class);
        }

        @Override
        protected void _select() {
            bHasBeenStarted = true;
            bCancelled = false;
            bHasNextCompleted = false;
            amountRead = 0;
            amountCount = -1;
            query = iterator;
        }
    }

    @Test
    void nextExceptionIsVisibleNotFalseEmptyResult() {
        TestSelect sel = new TestSelect();

        assertEquals("A", sel.next().getName());

        RuntimeException ex = assertThrows(RuntimeException.class, sel::next);

        assertEquals("next failed", ex.getMessage());
        assertFalse(sel.hasNextCompleted());
    }

    @Test
    void closeAfterNextExceptionReleasesIterator() {
        TestSelect sel = new TestSelect();

        assertEquals("A", sel.next().getName());
        assertThrows(RuntimeException.class, sel::next);

        sel.close();

        assertEquals(1, sel.iterator.removeCalls.get());
        assertTrue(sel.isCancelled());
    }

    @Test
    void retryAfterIteratorFailureUsesFreshStateWhenSubclassSuppliesFreshIterator() {
        class FreshSelect extends TestSelect {
            @Override
            protected void _select() {
                iterator = new FailingIterator();
                super._select();
            }
        }

        FreshSelect sel = new FreshSelect();

        assertEquals("A", sel.next().getName());
        assertThrows(RuntimeException.class, sel::next);

        sel.reset();

        assertEquals("A", sel.next().getName());
    }

    @Test
    void filterExceptionDuringNextIsVisibleAndDoesNotIncrementAmountRead() {
        class FilterSelect extends OASelect<Item> {
            FailingIterator it = new FailingIterator();

            FilterSelect() {
                super(Item.class);
                setFilter(item -> {
                    throw new IllegalStateException("filter failed");
                });
            }

            @Override
            protected void _select() {
                bHasBeenStarted = true;
                bCancelled = false;
                bHasNextCompleted = false;
                amountRead = 0;
                amountCount = -1;
                query = it;
            }
        }

        FilterSelect sel = new FilterSelect();

        assertThrows(IllegalStateException.class, sel::next);
        assertEquals(0, sel.getAmountRead());
    }

    @Test
    void closeQueryCanBeCalledAfterFailureMoreThanOnce() {
        TestSelect sel = new TestSelect();

        assertEquals("A", sel.next().getName());
        assertThrows(RuntimeException.class, sel::next);

        assertDoesNotThrow(sel::closeQuery);
        assertDoesNotThrow(sel::closeQuery);
    }
}
