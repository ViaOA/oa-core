package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class OACircularQueueStressRegressionTest {
    static class IntQueue extends OACircularQueue<Integer> {
        IntQueue(int size) { super(Integer.class, size); }
    }

    @Test
    void producerConsumerContentionDoesNotCorruptRawStreamWithinRetainedWindow() throws Exception {
        IntQueue q = new IntQueue(5000);
        long tail = q.getHeadPostion();
        int total = 1000;
        ExecutorService es = Executors.newFixedThreadPool(2);
        try {
            Future<?> producer = es.submit(() -> {
                for (int i = 0; i < total; i++) {
                    q.addMessage(i);
                    if ((i % 50) == 0) Thread.yield();
                }
            });
            Future<List<Integer>> consumer = es.submit(() -> {
                List<Integer> out = new ArrayList<>();
                long pos = tail;
                while (out.size() < total) {
                    Integer[] vals = q.getMessages(pos, 25, 100);
                    if (vals != null) {
                        Collections.addAll(out, vals);
                        pos += vals.length;
                    }
                }
                return out;
            });
            producer.get(5, TimeUnit.SECONDS);
            List<Integer> out = consumer.get(5, TimeUnit.SECONDS);
            assertEquals(total, out.size());
            for (int i = 0; i < total; i++) assertEquals(i, out.get(i));
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void concurrentCleanupDoesNotClearUnreadMessagesForSlowSession() throws Exception {
        IntQueue q = new IntQueue(500);
        long slow = q.registerSession(1);
        long fast = q.registerSession(2);
        int total = 200;
        for (int i = 0; i < total; i++) q.addMessage(i);
        assertEquals(total, q.getMessages(2, fast, total, 0).length);

        ExecutorService es = Executors.newSingleThreadExecutor();
        try {
            Future<?> cleanup = es.submit(() -> {
                for (int i = 0; i < 50; i++) {
                    q.cleanupQueue();
                    Thread.yield();
                }
            });
            cleanup.get(5, TimeUnit.SECONDS);
        } finally {
            es.shutdownNow();
        }

        Integer[] slowRead = q.getMessages(1, slow, total, 0);
        assertEquals(total, slowRead.length);
        for (int i = 0; i < total; i++) assertEquals(i, slowRead[i]);
    }

    @Test
    void repeatedRegisterUnregisterDoesNotCorruptRawQueue() throws Exception {
        IntQueue q = new IntQueue(100);
        long rawTail = q.getHeadPostion();
        for (int i = 0; i < 20; i++) {
            long pos = q.registerSession(i);
            q.addMessage(i);
            q.unregisterSession(i);
            assertTrue(pos <= q.getHeadPostion());
        }
        Integer[] vals = q.getMessages(rawTail, 100, 0);
        assertEquals(20, vals.length);
        for (int i = 0; i < 20; i++) assertEquals(i, vals[i]);
    }
}
