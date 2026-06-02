package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

class OACircularQueueWaitNotifyTest {

    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) {
            super(size);
        }
    }

    @Test
    void blockingReadWakesWhenMessageIsEnqueued() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.getHeadPostion();

        ExecutorService es = Executors.newSingleThreadExecutor();
        try {
            Future<String[]> fut = es.submit(() -> q.getMessages(pos, 10));

            Thread.sleep(50);
            q.addMessage("A");

            assertArrayEquals(new String[] { "A" }, fut.get(2, TimeUnit.SECONDS));
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void multipleWaitingConsumersWakeOnEnqueueDesiredContract() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.getHeadPostion();

        ExecutorService es = Executors.newFixedThreadPool(2);
        try {
            Future<String[]> f1 = es.submit(() -> q.getMessages(pos, 10));
            Future<String[]> f2 = es.submit(() -> q.getMessages(pos, 10));

            Thread.sleep(50);
            q.addMessage("A");

            assertArrayEquals(new String[] { "A" }, f1.get(2, TimeUnit.SECONDS));
            assertArrayEquals(new String[] { "A" }, f2.get(2, TimeUnit.SECONDS));
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void timedWaitReturnsWithoutMessage() {
        StringQueue q = new StringQueue(5);
        long pos = q.getHeadPostion();

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            assertNull(q.getMessages(pos, 10, 25));
        });
    }
}
