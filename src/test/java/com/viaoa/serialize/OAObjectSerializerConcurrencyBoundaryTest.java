package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerConcurrencyBoundaryTest {

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
    void independentSerializersCanRoundTripConcurrently() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                final int x = i;
                tasks.add(() -> {
                    OAObjectSerializer<Item> copy = roundTrip(new OAObjectSerializer<>(new Item("I" + x), (x % 2) == 0));
                    return copy.getObject().getName();
                });
            }

            List<Future<String>> futures = es.invokeAll(tasks);

            for (int i = 0; i < futures.size(); i++) {
                assertEquals("I" + i, futures.get(i).get(5, TimeUnit.SECONDS));
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void independentContextsCanTrackSameObjectConcurrentlyWithoutSharedState() throws Exception {
        Item item = new Item("A");

        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> {
                    OASerializeContext ctx = new OASerializeContext();
                    assertFalse(ctx.hasWritten(item));
                    ctx.markWritten(item);
                    return ctx.hasWritten(item);
                });
            }

            for (Future<Boolean> f : es.invokeAll(tasks)) {
                assertTrue(f.get(5, TimeUnit.SECONDS));
            }
        } finally {
            es.shutdownNow();
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
