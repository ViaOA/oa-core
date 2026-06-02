package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectPhase3PartialProgressTest {

    public static class Item extends OAObject {
        private String name;
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    static class FailingIterator implements OADataSourceIterator<Item> {
        int pos;
        boolean removed;

        @Override
        public boolean hasNext() {
            return !removed && pos < 3;
        }

        @Override
        public Item next() {
            if (pos == 0) {
                pos++;
                return new Item("A");
            }
            throw new IllegalStateException("iterator failure");
        }

        @Override
        public void remove() {
            removed = true;
        }

        @Override
        public String getQuery() {
            return "failing";
        }

        @Override
        public String getQuery2() {
            return "failing2";
        }

        @Override
        public com.viaoa.graph.sibling.OASiblingHelper getSiblingHelper() {
            return null;
        }
    }

    static class TestSelect extends OASelect<Item> {
        FailingIterator iterator;

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
            iterator = new FailingIterator();
            query = iterator;
        }
    }

    @Test
    void partialIterationCountReflectsOnlyReturnedObjects() {
        TestSelect sel = new TestSelect();

        assertEquals("A", sel.next().getName());
        assertEquals(1, sel.getAmountRead());

        assertThrows(IllegalStateException.class, sel::next);

        assertEquals(1, sel.getAmountRead());
    }

    @Test
    void iteratorFailureDoesNotMarkCompletedSuccess() {
        TestSelect sel = new TestSelect();

        assertEquals("A", sel.next().getName());
        assertThrows(IllegalStateException.class, sel::next);

        assertFalse(sel.hasNextCompleted(),
            "iterator failure must not be indistinguishable from normal exhaustion");
    }

    @Test
    void closeAfterIteratorFailureReleasesIterator() {
        TestSelect sel = new TestSelect();

        assertEquals("A", sel.next().getName());
        assertThrows(IllegalStateException.class, sel::next);

        sel.close();

        assertTrue(sel.iterator.removed);
        assertTrue(sel.isCancelled());
    }

    @Test
    void retryAfterPartialFailureStartsFresh() {
        TestSelect sel = new TestSelect();

        assertEquals("A", sel.next().getName());
        assertThrows(IllegalStateException.class, sel::next);

        sel.reset();

        assertEquals("A", sel.next().getName());
    }
}
