package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectPhase4CloseCancelIdempotencyTest {

    public static class Item extends OAObject {
        private String name;
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    static class TrackingIterator implements OADataSourceIterator<Item> {
        final List<Item> items = List.of(new Item("A"), new Item("B"));
        int pos;
        AtomicInteger removeCalls = new AtomicInteger();

        @Override
        public boolean hasNext() {
            return pos < items.size();
        }

        @Override
        public Item next() {
            return pos < items.size() ? items.get(pos++) : null;
        }

        @Override
        public void remove() {
            removeCalls.incrementAndGet();
        }

        @Override
        public String getQuery() { return "q"; }

        @Override
        public String getQuery2() { return "q2"; }

        @Override
        public com.viaoa.graph.sibling.OASiblingHelper getSiblingHelper() { return null; }
    }

    static class TestSelect extends OASelect<Item> {
        TrackingIterator iterator;

        TestSelect() {
            super(Item.class);
        }

        @Override
        protected void _select() {
            bHasBeenStarted = true;
            bCancelled = false;
            bHasNextCompleted = false;
            amountRead = 0;
            amountCount = 2;
            iterator = new TrackingIterator();
            query = iterator;
        }
    }

    @Test
    void closeIsIdempotentForIteratorRemoval() {
        TestSelect sel = new TestSelect();
        sel.select();

        sel.close();
        sel.close();
        sel.close();

        assertTrue(sel.isCancelled());
        assertTrue(sel.hasNextCompleted());
        assertTrue(sel.iterator.removeCalls.get() <= 1);
    }

    @Test
    void cancelIsIdempotentForIteratorRemoval() {
        TestSelect sel = new TestSelect();
        sel.select();

        sel.cancel();
        sel.cancel();
        sel.cancel();

        assertTrue(sel.isCancelled());
        assertTrue(sel.hasNextCompleted());
        assertTrue(sel.iterator.removeCalls.get() <= 1);
    }

    @Test
    void closeThenCancelDoesNotDoubleRemoveIterator() {
        TestSelect sel = new TestSelect();
        sel.select();

        sel.close();
        sel.cancel();

        assertTrue(sel.iterator.removeCalls.get() <= 1);
    }

    @Test
    void cancelThenCloseDoesNotDoubleRemoveIterator() {
        TestSelect sel = new TestSelect();
        sel.select();

        sel.cancel();
        sel.close();

        assertTrue(sel.iterator.removeCalls.get() <= 1);
    }

    @Test
    void closeAfterExhaustionDoesNotDoubleRemoveIterator() {
        TestSelect sel = new TestSelect();

        assertEquals("A", sel.next().getName());
        assertEquals("B", sel.next().getName());
        assertNull(sel.next());

        int removeCallsAfterExhaustion = sel.iterator.removeCalls.get();

        sel.close();

        assertEquals(removeCallsAfterExhaustion, sel.iterator.removeCalls.get());
    }

    @Test
    void resetAfterCloseAllowsFreshIteratorLifecycle() {
        TestSelect sel = new TestSelect();

        sel.select();
        TrackingIterator old = sel.iterator;
        sel.close();

        sel.reset();
        sel.select();

        assertNotSame(old, sel.iterator);
        assertFalse(sel.isCancelled());
        assertEquals("A", sel.next().getName());
    }
}
