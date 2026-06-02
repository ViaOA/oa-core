package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class OACircularQueueWaitNotifyEdgeTest {
    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) { super(size); }
    }

    @Test
    void twoUntimedWaitersBothWakeForSameNewMessageDesiredContract() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.getHeadPostion();
        ExecutorService es = Executors.newFixedThreadPool(2);
        try {
            Future<String[]> f1 = es.submit(() -> q.getMessages(pos));
            Future<String[]> f2 = es.submit(() -> q.getMessages(pos));
            Thread.sleep(75);
            q.addMessage("A");
            assertArrayEquals(new String[] { "A" }, f1.get(2, TimeUnit.SECONDS));
            assertArrayEquals(new String[] { "A" }, f2.get(2, TimeUnit.SECONDS));
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void timedAndUntimedWaitersDoNotMaskEachOtherDesiredContract() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.getHeadPostion();
        ExecutorService es = Executors.newFixedThreadPool(2);
        try {
            Future<String[]> timed = es.submit(() -> q.getMessages(pos, 10, 1000));
            Future<String[]> untimed = es.submit(() -> q.getMessages(pos));
            Thread.sleep(75);
            q.addMessage("A");
            assertArrayEquals(new String[] { "A" }, timed.get(2, TimeUnit.SECONDS));
            assertArrayEquals(new String[] { "A" }, untimed.get(2, TimeUnit.SECONDS));
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void waiterThatTimesOutDoesNotBlockLaterWaiterNotification() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.getHeadPostion();
        assertNull(q.getMessages(pos, 10, 10));
        ExecutorService es = Executors.newSingleThreadExecutor();
        try {
            Future<String[]> fut = es.submit(() -> q.getMessages(pos));
            Thread.sleep(75);
            q.addMessage("A");
            assertArrayEquals(new String[] { "A" }, fut.get(2, TimeUnit.SECONDS));
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void finiteWaitReturnsPromptlyWhenMessageAlreadyAvailable() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.getHeadPostion();
        q.addMessage("A");
        long start = System.currentTimeMillis();
        assertArrayEquals(new String[] { "A" }, q.getMessages(pos, 10, 5000));
        assertTrue(System.currentTimeMillis() - start < 1000);
    }
}
