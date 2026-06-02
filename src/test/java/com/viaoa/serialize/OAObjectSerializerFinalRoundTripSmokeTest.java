package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerFinalRoundTripSmokeTest {

    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
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

    @Test
    void repeatedRoundTripsProduceSameSemanticResult() throws Exception {
        for (int i = 0; i < 10; i++) {
            OAObjectSerializer<Item> copy = roundTrip(new OAObjectSerializer<>(new Item("A"), i % 2 == 0));
            assertEquals("A", copy.getObject().getName());
        }
    }

    @Test
    void repeatedHubRoundTripsProduceSameSemanticMembershipOrder() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A"));
        hub.add(new Item("B"));

        for (int i = 0; i < 10; i++) {
            OAObjectSerializer<Hub<Item>> copy = roundTrip(new OAObjectSerializer<>(hub, i % 2 == 0));
            assertEquals(2, copy.getObject().getSize());
            assertEquals("A", copy.getObject().getAt(0).getName());
            assertEquals("B", copy.getObject().getAt(1).getName());
        }
    }

    @Test
    void independentConcurrentCompressedAndUncompressedRoundTripsAreStable() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                int x = i;
                tasks.add(() -> {
                    OAObjectSerializer<Item> copy = roundTrip(new OAObjectSerializer<>(new Item("I" + x), (x % 2) == 0));
                    return copy.getObject().getName();
                });
            }

            List<Future<String>> results = es.invokeAll(tasks);

            for (int i = 0; i < results.size(); i++) {
                assertEquals("I" + i, results.get(i).get(5, TimeUnit.SECONDS));
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void nullRootAndExtraCanBeSerializedRepeatedly() throws Exception {
        for (int i = 0; i < 5; i++) {
            OAObjectSerializer<Object> copy = roundTrip(new OAObjectSerializer<>(null, i % 2 == 0));
            assertNull(copy.getObject());
            assertNull(copy.getExtraObject());
        }
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
