package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class OACircularQueueBackpressureHookTest {

    static class HookQueue extends OACircularQueue<String> {
        final AtomicInteger hookCalls = new AtomicInteger();
        volatile boolean waitForSlowSession;

        HookQueue(int size) {
            super(size);
        }

        @Override
        protected boolean shouldWaitOnSlowSession(int sessionId, int msSinceLastRead) {
            hookCalls.incrementAndGet();
            return waitForSlowSession;
        }
    }

    @Test
    void slowSessionHookCanMarkSessionInactiveWhenItReturnsFalse() throws Exception {
        HookQueue q = new HookQueue(3);
        long pos = q.registerSession(1);

        Thread.sleep(1100);

        for (int i = 0; i < 8; i++) {
            q.addMessage("M" + i);
        }

        assertTrue(q.hookCalls.get() > 0);

        assertThrows(Exception.class, () -> q.getMessages(1, pos, 10, 0));
    }

    @Test
    void protectedSlowSessionEventuallyFailsOrMarksStateDesiredContract() throws Exception {
        HookQueue q = new HookQueue(3);
        q.waitForSlowSession = true;
        long pos = q.registerSession(1);

        Thread.sleep(1100);

        for (int i = 0; i < 3; i++) {
            q.addMessage("M" + i);
        }

        assertTimeoutPreemptively(java.time.Duration.ofSeconds(8), () -> {
            q.addMessage("overflow");
        });

        assertTrue(q.hookCalls.get() > 0);

        assertThrows(Exception.class, () -> q.getMessages(1, pos, 10, 0),
            "if producer stops waiting for protected slow session, session must see explicit overrun/failure");
    }

    @Test
    void throttleDoesNotBreakOrderingWhenConsumersCatchUp() throws Exception {
        HookQueue q = new HookQueue(10);
        long pos = q.registerSession(1);

        for (int i = 0; i < 5; i++) {
            q.addMessageToQueue("M" + i, 2);
        }

        assertArrayEquals(new String[] { "M0", "M1", "M2", "M3", "M4" }, q.getMessages(1, pos, 10, 0));
    }

    @Test
    void ignoreThrottleSessionParameterPreventsThrottleForIgnoredSession() throws Exception {
        HookQueue q = new HookQueue(10);
        long pos = q.registerSession(1);

        for (int i = 0; i < 5; i++) {
            q.addMessageToQueue("M" + i, 2, 1);
        }

        assertArrayEquals(new String[] { "M0", "M1", "M2", "M3", "M4" }, q.getMessages(1, pos, 10, 0));
    }
}
