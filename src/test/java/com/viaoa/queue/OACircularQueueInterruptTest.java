package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class OACircularQueueInterruptTest {

    static class WaitQueue extends OACircularQueue<String> {
        volatile boolean waitForSlowSession = true;

        WaitQueue(int size) {
            super(size);
        }

        @Override
        protected boolean shouldWaitOnSlowSession(int sessionId, int msSinceLastRead) {
            return waitForSlowSession;
        }
    }

    @Test
    void consumerWaitInterruptPropagatesAndRestoresInterruptContract() throws Exception {
        WaitQueue q = new WaitQueue(5);
        long pos = q.getHeadPostion();

        ExecutorService es = Executors.newSingleThreadExecutor();
        try {
            Future<?> fut = es.submit(() -> {
                try {
                    q.getMessages(pos);
                    fail("expected interrupt to stop wait");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    assertTrue(Thread.currentThread().isInterrupted());
                } catch (Exception ex) {
                    if (!(ex instanceof InterruptedException)) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            Thread.sleep(50);
            fut.cancel(true);
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void producerThrottleInterruptRestoresInterruptStatusDesiredContract() throws Exception {
        WaitQueue q = new WaitQueue(3);
        q.registerSession(1);

        Thread.sleep(1100);
        q.addMessage("A");
        q.addMessage("B");
        q.addMessage("C");

        AtomicBoolean interruptedAtEnd = new AtomicBoolean();

        Thread t = new Thread(() -> {
            try {
                q.addMessage("D");
            } finally {
                interruptedAtEnd.set(Thread.currentThread().isInterrupted());
            }
        });

        t.start();
        Thread.sleep(100);
        t.interrupt();
        t.join(2000);

        assertTrue(interruptedAtEnd.get(),
            "producer wait/sleep should preserve interrupt status instead of swallowing InterruptedException");
    }

    @Test
    void finiteConsumerWaitDoesNotLeaveThreadInterrupted() throws Exception {
        WaitQueue q = new WaitQueue(5);

        assertNull(q.getMessages(q.getHeadPostion(), 10, 10));
        assertFalse(Thread.currentThread().isInterrupted());
    }
}
