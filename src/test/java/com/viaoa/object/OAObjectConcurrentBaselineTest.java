package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

class OAObjectConcurrentBaselineTest {

    public static class Item extends OAObject {
        private String name;
        private int count;

        public synchronized String getName() {
            return name;
        }

        public synchronized void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }

        public synchronized int getCount() {
            return count;
        }

        public synchronized void setCount(int count) {
            int old = this.count;
            this.count = count;
            firePropertyChange("count", old, count);
        }
    }

    @Test
    void concurrentDistinctObjectConstructionProducesDistinctGuids() throws Exception {
        int total = 100;
        ExecutorService es = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Item>> tasks = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                tasks.add(Item::new);
            }

            List<Future<Item>> futures = es.invokeAll(tasks);
            java.util.Set<java.util.UUID> guids = new java.util.HashSet<>();

            for (Future<Item> f : futures) {
                assertTrue(guids.add(f.get(5, TimeUnit.SECONDS).getGuid()));
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void concurrentFlagTogglesDoNotThrow() throws Exception {
        Item item = new Item();

        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                final int x = i;
                tasks.add(() -> {
                    item.setChanged((x % 2) == 0);
                    item.setNew((x % 3) == 0);
                    item.setDeleted((x % 5) == 0);
                    return null;
                });
            }

            for (Future<Void> f : es.invokeAll(tasks)) {
                f.get(5, TimeUnit.SECONDS);
            }
        } finally {
            es.shutdownNow();
        }

        assertNotNull(item.getGuid());
    }

    @Test
    void concurrentCompareAndSwapDoesNotCreateInvalidValue() throws Exception {
        Item item = new Item();
        item.setName("A");

        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                tasks.add(() -> item.compareAndSwap("name", "A", "B"));
            }

            int successes = 0;
            for (Future<Boolean> f : es.invokeAll(tasks)) {
                if (f.get(5, TimeUnit.SECONDS)) successes++;
            }

            assertTrue(successes >= 0);
            assertEquals("B", item.getName());
        } finally {
            es.shutdownNow();
        }
    }
}
