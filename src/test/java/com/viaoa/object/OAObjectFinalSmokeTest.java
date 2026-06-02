package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

class OAObjectFinalSmokeTest {

    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;

        public String getName() { return name; }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    @Test
    void repeatedSerializationRoundTripsPreserveGuidAndProperty() throws Exception {
        Item item = new Item();
        item.setName("A");

        for (int i = 0; i < 5; i++) {
            Item copy = roundTrip(item);
            assertEquals(item.getGuid(), copy.getGuid());
            assertEquals("A", copy.getName());
        }
    }

    @Test
    void concurrentSimpleObjectConstructionAndSerializationIsStable() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                int x = i;
                tasks.add(() -> {
                    Item item = new Item();
                    item.setName("I" + x);
                    Item copy = roundTrip(item);
                    return item.getGuid().equals(copy.getGuid()) && item.getName().equals(copy.getName());
                });
            }

            for (Future<Boolean> f : es.invokeAll(tasks)) {
                assertTrue(f.get(5, TimeUnit.SECONDS));
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void toStringHashCodeCompareDoNotThrowAcrossLifecycleStates() {
        Item item = new Item();

        item.setName("A");
        item.setChanged(false);
        item.setNew(false);
        item.setDeleted(true);

        assertDoesNotThrow(item::toString);
        assertDoesNotThrow(item::hashCode);
        assertDoesNotThrow(() -> item.compareTo(new Item()));
    }

    @Test
    void objectLocalStillBehavesAsOAObjectForBasicRuntimeIdentity() {
        OAObjectLocal local = new OAObjectLocal();

        assertNotNull(local.getGuid());
        assertNotNull(local.getObjectKey());
        assertEquals(local.getGuid(), local.getObjectKey().getGuid());
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
