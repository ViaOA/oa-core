package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectPhase3ConcurrencyBoundaryTest {

    public static class Item extends OAObject {
        private String name;
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    static class BlockingIterator implements OADataSourceIterator<Item> {
        final CountDownLatch nextEntered = new CountDownLatch(1);
        final CountDownLatch releaseNext = new CountDownLatch(1);
        final AtomicInteger removeCalls = new AtomicInteger();
        volatile boolean removed;
        int pos;
        final List<Item> values = List.of(new Item("A"), new Item("B"));

        @Override
        public boolean hasNext() {
            return !removed && pos < values.size();
        }

        @Override
        public Item next() {
            nextEntered.countDown();
            try {
                releaseNext.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            if (removed || pos >= values.size()) return null;
            return values.get(pos++);
        }

        @Override
        public void remove() {
            removed = true;
            removeCalls.incrementAndGet();
        }

        @Override
        public String getQuery() {
            return "blocking";
        }

        @Override
        public String getQuery2() {
            return "blocking2";
        }

        @Override
        public com.viaoa.graph.sibling.OASiblingHelper getSiblingHelper() {
            return null;
        }
    }

    static class TestSelect extends OASelect<Item> {
        final BlockingIterator iterator = new BlockingIterator();

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
            query = iterator;
        }
    }

    @Test
    void concurrentCancelAndNextLeavesDeterministicClosedState() throws Exception {
        TestSelect sel = new TestSelect();
        sel.select();

        ExecutorService es = Executors.newSingleThreadExecutor();
        try {
            Future<Item> f = es.submit(sel::next);

            assertTrue(sel.iterator.nextEntered.await(1, TimeUnit.SECONDS));

            sel.cancel();
            sel.iterator.releaseNext.countDown();

            Item obj = f.get(2, TimeUnit.SECONDS);

            assertNull(obj, "next racing with cancel should not return an object after iterator was removed");
            assertTrue(sel.isCancelled());
            assertTrue(sel.hasNextCompleted());
            assertTrue(sel.iterator.removed);
            assertEquals(1, sel.iterator.removeCalls.get());
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void repeatedCloseAndCancelRemoveIteratorAtMostOnceDesiredContract() {
        TestSelect sel = new TestSelect();
        sel.select();

        sel.close();
        sel.cancel();
        sel.close();

        assertTrue(sel.iterator.removeCalls.get() <= 1,
            "close/cancel should be idempotent for owned iterator cleanup");
        assertTrue(sel.isCancelled());
    }

    @Test
    void concurrentHasMoreAndCloseDoesNotReturnTrueAfterClose() throws Exception {
        TestSelect sel = new TestSelect();
        sel.select();

        ExecutorService es = Executors.newFixedThreadPool(2);
        try {
            Future<?> close = es.submit(sel::close);
            close.get(1, TimeUnit.SECONDS);

            Future<Boolean> hasMore = es.submit(sel::hasMore);

            assertFalse(hasMore.get(1, TimeUnit.SECONDS));
            assertTrue(sel.isCancelled());
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void resetAfterConcurrentCancelAllowsFreshSelection() throws Exception {
        TestSelect sel = new TestSelect();
        sel.select();

        sel.cancel();
        sel.reset();

        assertFalse(sel.isCancelled());
        assertFalse(sel.hasNextCompleted());

        sel.select();
        sel.iterator.releaseNext.countDown();

        Item obj = sel.next();
        assertTrue(obj == null || "A".equals(obj.getName()));
    }
}
