package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

class OAObjectFinalConcurrencyStressTest {

    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
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
    void manyConcurrentObjectsHaveUniqueGuidsAndStableKeys() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Item>> tasks = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                int x = i;
                tasks.add(() -> {
                    Item item = new Item();
                    item.setName("I" + x);
                    item.setCount(x);
                    return item;
                });
            }

            Set<java.util.UUID> guids = ConcurrentHashMap.newKeySet();

            for (Future<Item> f : es.invokeAll(tasks)) {
                Item item = f.get(5, TimeUnit.SECONDS);
                assertTrue(guids.add(item.getGuid()));
                assertEquals(item.getGuid(), item.getObjectKey().getGuid());
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void concurrentSerializationOfIndependentObjectsIsStable() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                int x = i;
                tasks.add(() -> {
                    Item item = new Item();
                    item.setName("I" + x);
                    item.setCount(x);
                    Item copy = roundTrip(item);
                    return item.getGuid().equals(copy.getGuid())
                        && item.getName().equals(copy.getName())
                        && item.getCount() == copy.getCount();
                });
            }

            for (Future<Boolean> f : es.invokeAll(tasks)) {
                assertTrue(f.get(10, TimeUnit.SECONDS));
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void concurrentListenersAndMutationsDoNotCorruptIdentity() throws Exception {
        Item item = new Item();
        var guid = item.getGuid();

        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                int x = i;
                tasks.add(() -> {
                    java.beans.PropertyChangeListener li = evt -> { };
                    item.addPropertyChangeListener("name", li);
                    item.setName("N" + x);
                    item.removePropertyChangeListener("name", li);
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

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
