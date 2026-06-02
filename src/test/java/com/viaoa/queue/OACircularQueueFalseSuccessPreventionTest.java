package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class OACircularQueueFalseSuccessPreventionTest {
    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) { super(size); }
    }

    static class SlowProtectedQueue extends OACircularQueue<String> {
        volatile boolean shouldWait = true;
        volatile int hookCalls;
        SlowProtectedQueue(int size) { super(size); }
        @Override
        protected boolean shouldWaitOnSlowSession(int sessionId, int msSinceLastRead) {
            hookCalls++;
            return shouldWait;
        }
    }

    @Test
    void unregisteredSessionIdDoesNotReturnSuccessfulRawReadDesiredContract() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.registerSession(1);
        q.addMessage("A");
        q.unregisterSession(1);
        assertThrows(Exception.class, () -> q.getMessages(1, pos, 10, 0));
    }

    @Test
    void unknownSessionIdWithExistingSessionsDoesNotReturnSuccessfulRawReadDesiredContract() {
        StringQueue q = new StringQueue(10);
        long pos = q.registerSession(1);
        q.addMessage("A");
        assertThrows(Exception.class, () -> q.getMessages(99, pos, 10, 0));
    }

    @Test
    void duplicateRegisterDoesNotSilentlyResetTrackedPositionDesiredContract() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.registerSession(1);
        q.addMessage("A");
        assertThrows(Exception.class, () -> q.registerSession(1));
        assertArrayEquals(new String[] { "A" }, q.getMessages(1, pos, 10, 0));
    }

    @Test
    void interruptedProducerWaitDoesNotReportCleanSuccessDesiredContract() throws Exception {
        SlowProtectedQueue q = new SlowProtectedQueue(3);
        q.registerSession(1);
        Thread.sleep(1100);
        q.addMessage("A"); q.addMessage("B"); q.addMessage("C");

        AtomicBoolean completed = new AtomicBoolean();
        AtomicBoolean interrupted = new AtomicBoolean();

        Thread t = new Thread(() -> {
            try {
                q.addMessage("D");
                completed.set(true);
            } finally {
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });
        t.start();
        Thread.sleep(100);
        t.interrupt();
        t.join(2000);

        assertFalse(completed.get() && !interrupted.get(),
            "interrupted producer must not silently report success while clearing interrupt status");
    }

    @Test
    void futureSessionTailDoesNotAcknowledgeUndeliveredMessagesDesiredContract() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.registerSession(1);
        assertThrows(Exception.class, () -> q.getMessages(1, q.getHeadPostion() + 100, 10, 0));
        q.addMessage("A");
        assertArrayEquals(new String[] { "A" }, q.getMessages(1, pos, 10, 0));
    }
}
