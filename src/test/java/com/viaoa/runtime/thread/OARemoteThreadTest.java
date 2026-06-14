package com.viaoa.runtime.thread;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class OARemoteThreadTest {
    @Test
    void runnableAndStateMethodsAreDeterministicWithoutStartingThread() {
        OARemoteThread thread = new OARemoteThread();
        AtomicInteger counter = new AtomicInteger();

        thread.addRunnable(counter::incrementAndGet);
        thread.addRunnable(null);
        assertEquals(1, counter.get());

        assertFalse(thread.getAllowRunnable());
        thread.setAllowRunnable(true);
        assertTrue(thread.getAllowRunnable());

        assertFalse(thread.startedNextThread());
        thread.startNextThread();
        assertTrue(thread.startedNextThread());
        assertTrue(thread.msStartNextThread > 0);

        thread.setWaitingOnLock(true);
        assertTrue(thread.isWaitingOnLock());

        thread.setStartedNextThread(false);
        assertFalse(thread.startedNextThread());

        thread.setDefaultSendSyncMessages(true);
        assertTrue(thread.getDefaultSendSyncMessages());

        thread.reset();
        assertFalse(thread.startedNextThread());
        assertFalse(thread.isWaitingOnLock());
        assertEquals(0L, thread.msStartNextThread);
    }
}
