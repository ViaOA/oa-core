package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class OACircularQueueConcurrentOrderingTest {

    static class IntQueue extends OACircularQueue<Integer> {
        IntQueue(int size) {
            super(Integer.class, size);
        }
    }

    @Test
    void concurrentSingleProducerSingleRawConsumerPreservesOrder() throws Exception {
        IntQueue q = new IntQueue(2000);
        long start = q.getHeadPostion();
        int total = 500;

        ExecutorService es = Executors.newFixedThreadPool(2);
        try {
            Future<?> producer = es.submit(() -> {
                for (int i = 0; i < total; i++) {
                    q.addMessage(i);
                }
            });

            Future<List<Integer>> consumer = es.submit(() -> {
                List<Integer> out = new ArrayList<>();
                long pos = start;
                while (out.size() < total) {
                    Integer[] vals = q.getMessages(pos, 50, 100);
                    if (vals != null) {
                        Collections.addAll(out, vals);
                        pos += vals.length;
                    }
                }
                return out;
            });

            producer.get(5, TimeUnit.SECONDS);
            List<Integer> vals = consumer.get(5, TimeUnit.SECONDS);

            assertEquals(total, vals.size());
            for (int i = 0; i < total; i++) {
                assertEquals(i, vals.get(i));
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void registeredSessionsMaintainIndependentPositions() throws Exception {
        IntQueue q = new IntQueue(100);
        long p1 = q.registerSession(1);
        long p2 = q.registerSession(2);

        for (int i = 0; i < 10; i++) {
            q.addMessage(i);
        }

        Integer[] s1a = q.getMessages(1, p1, 3, 0);
        assertArrayEquals(new Integer[] { 0, 1, 2 }, s1a);

        Integer[] s2a = q.getMessages(2, p2, 10, 0);
        assertArrayEquals(new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, s2a);

        Integer[] s1b = q.getMessages(1, p1 + s1a.length, 10, 0);
        assertArrayEquals(new Integer[] { 3, 4, 5, 6, 7, 8, 9 }, s1b);
    }

    @Test
    void concurrentRegisteredConsumersEachSeeCompleteOrderedStream() throws Exception {
        IntQueue q = new IntQueue(1000);
        long p1 = q.registerSession(1);
        long p2 = q.registerSession(2);
        int total = 200;

        ExecutorService es = Executors.newFixedThreadPool(3);
        try {
            Future<?> producer = es.submit(() -> {
                for (int i = 0; i < total; i++) {
                    q.addMessage(i);
                }
            });

            Future<List<Integer>> c1 = consumeSession(es, q, 1, p1, total);
            Future<List<Integer>> c2 = consumeSession(es, q, 2, p2, total);

            producer.get(5, TimeUnit.SECONDS);

            assertOrdered(c1.get(5, TimeUnit.SECONDS), total);
            assertOrdered(c2.get(5, TimeUnit.SECONDS), total);
        } finally {
            es.shutdownNow();
        }
    }

    private static Future<List<Integer>> consumeSession(ExecutorService es, IntQueue q, int sessionId, long start, int total) {
        return es.submit(() -> {
            List<Integer> out = new ArrayList<>();
            long pos = start;
            while (out.size() < total) {
                Integer[] vals = q.getMessages(sessionId, pos, 20, 100);
                if (vals != null) {
                    Collections.addAll(out, vals);
                    pos += vals.length;
                }
            }
            return out;
        });
    }

    private static void assertOrdered(List<Integer> vals, int total) {
        assertEquals(total, vals.size());
        for (int i = 0; i < total; i++) {
            assertEquals(i, vals.get(i));
        }
    }
}
