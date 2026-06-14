package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

class OAObjectDeepInvariantSmokeTest {

    public static class Item extends OAObject {
        private String name;
        private int count;

        public synchronized String getName() { return name; }
        public synchronized void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }

        public synchronized int getCount() { return count; }
        public synchronized void setCount(int count) {
            int old = this.count;
            this.count = count;
            firePropertyChange("count", old, count);
        }
    }

    @Test
    void concurrentPropertyMutationsPreserveGuidAndObjectKey() throws Exception {
        Item item = new Item();
        var guid = item.getGuid();

        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                final int x = i;
                tasks.add(() -> {
                    item.setName("N" + x);
                    item.setCount(x);
                    item.setChanged((x % 2) == 0);
                    return null;
                });
            }

            for (Future<Void> f : es.invokeAll(tasks)) {
                f.get(5, TimeUnit.SECONDS);
            }
        } finally {
            es.shutdownNow();
        }

        assertEquals(guid, item.getGuid());
        assertEquals(guid, item.getObjectKey().getGuid());
    }

    @Test
    void repeatedFlagAndPropertyTransitionsDoNotThrow() {
        Item item = new Item();

        for (int i = 0; i < 100; i++) {
            item.setName("N" + i);
            item.setCount(i);
            item.setChanged((i % 2) == 0);
            item.setNew((i % 3) == 0);
            item.setDeleted((i % 5) == 0);
            assertNotNull(item.getGuid());
            assertEquals(item.getGuid(), item.getObjectKey().getGuid());
        }
    }

    @Test
    void compareHashToStringStableEnoughForRepeatedRuntimeUse() {
        Item a = new Item();
        Item b = new Item();

        for (int i = 0; i < 100; i++) {
            assertNotNull(a.toString());
            assertEquals(a.hashCode(), a.hashCode());
            assertEquals(0, a.compareTo(a));
            assertEquals(Integer.signum(a.compareTo(b)), -Integer.signum(b.compareTo(a)));
        }
    }
}
