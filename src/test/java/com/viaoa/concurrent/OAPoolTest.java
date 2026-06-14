package com.viaoa.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class OAPoolTest {

    private static class StringPool extends OAPool<StringBuilder> {
        final AtomicInteger created = new AtomicInteger();
        final List<StringBuilder> removed = new ArrayList<>();

        StringPool(int min, int max) {
            super(StringBuilder.class, min, max);
        }

        @Override
        protected StringBuilder create() {
            return new StringBuilder("r").append(created.incrementAndGet());
        }

        @Override
        protected void removed(StringBuilder resource) {
            removed.add(resource);
        }
    }

    private static class InferredStringPool extends OAPool<StringBuilder> {
        InferredStringPool(int min, int max) {
            super(min, max);
        }

        @Override
        protected StringBuilder create() {
            return new StringBuilder();
        }

        @Override
        protected void removed(StringBuilder resource) {
        }
    }

    @Test
    void constructorsAcceptExplicitOrInferredResourceType() {
        assertNotNull(new StringPool(0, 1));
        assertNotNull(new InferredStringPool(0, 1));
    }

    @Test
    void highMarkTimeLimitMinimumAndMaximumAccessorsRoundTrip() {
        StringPool pool = new StringPool(1, 2);

        pool.setHighMarkTimeLimit(10);
        pool.setMinimum(0);
        pool.setMaximum(3);

        assertEquals(0, pool.getMinimum());
        assertEquals(3, pool.getMaximum());
    }

    @Test
    void currentSizeAndCurrentUsedReflectGetAndRelease() {
        StringPool pool = new StringPool(0, 2);

        StringBuilder resource = pool.get();

        assertEquals(1, pool.getCurrentSize());
        assertEquals(1, pool.getCurrentUsed());

        pool.release(resource);

        assertEquals(0, pool.getCurrentUsed());
    }

    @Test
    void loadMinimumCreatesAvailableResources() {
        StringPool pool = new StringPool(2, 3);

        pool.loadMinimum();

        assertEquals(2, pool.getCurrentSize());
        assertEquals(0, pool.getCurrentUsed());
        assertEquals(2, pool.getAllItems().length);
    }

    @Test
    void getReusesReleasedResource() {
        StringPool pool = new StringPool(0, 1);
        StringBuilder first = pool.get();
        pool.release(first);

        StringBuilder second = pool.get();

        assertSame(first, second);
        pool.release(second);
    }

    @Test
    void getBlocksWhenMaximumReachedUntilRelease() throws Exception {
        StringPool pool = new StringPool(0, 1);
        StringBuilder first = pool.get();
        CountDownLatch started = new CountDownLatch(1);
        OAExecutorService executor = new OAExecutorService(1, "pool");
        try {
            Future<?> future = executor.submit(() -> {
                started.countDown();
                StringBuilder second = pool.get();
                pool.release(second);
            });

            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertFalse(future.isDone());
            pool.release(first);
            future.get(1, TimeUnit.SECONDS);
        }
        finally {
            pool.release(first);
            executor.getExecutorService().shutdownNow();
        }
    }

    @Test
    void removeDropsManagedResourceAndCallsRemovedCallback() {
        StringPool pool = new StringPool(0, 2);
        StringBuilder resource = pool.get();

        pool.remove(resource);

        assertEquals(0, pool.getCurrentSize());
        assertEquals(0, pool.getCurrentUsed());
        assertEquals(List.of(resource), pool.removed);
    }

    @Test
    void releaseNullAndUnknownResourceAreIgnored() {
        StringPool pool = new StringPool(0, 1);

        pool.release(null);
        pool.release(new StringBuilder("unknown"));

        assertEquals(0, pool.getCurrentSize());
        assertEquals(0, pool.getCurrentUsed());
    }

    @Test
    void getAllItemsIncludesAvailableAndUsedResources() {
        StringPool pool = new StringPool(0, 2);
        StringBuilder first = pool.get();
        StringBuilder second = pool.get();

        Object[] all = pool.getAllItems();

        assertEquals(2, all.length);
        assertTrue(List.of(all).contains(first));
        assertTrue(List.of(all).contains(second));
        pool.release(first);
        pool.release(second);
    }

    @Test
    void addExternalResourceMakesItAvailable() {
        StringPool pool = new StringPool(0, 1);
        StringBuilder external = new StringBuilder("external");

        pool.add(null);
        pool.add(external);

        assertEquals(1, pool.getCurrentSize());
        assertSame(external, pool.get());
    }
}
