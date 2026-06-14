package com.viaoa.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class OARemoteThreadServiceTest {
    @Test
    void nonRemoteThreadDefaultsAreSafe() {
        TestRemoteThreadService service = new TestRemoteThreadService();

        assertFalse(service.isRemoteThread());
        assertTrue(service.isSafeToCallRemoteMethod());
        assertTrue(service.startedNextThread());
        assertNull(service.getRequestInfo());
        assertFalse(service.shouldEventsBeQueued());
        assertFalse(service.queueEvent(() -> fail("non-remote thread must not queue")));

        service.startNextThread();
        assertTrue(service.notified.get());
    }

    private static class TestRemoteThreadService extends OARemoteThreadService {
        final AtomicBoolean notified = new AtomicBoolean();

        @Override
        protected void callThreadLocalNotifyWaitingThread() {
            notified.set(true);
        }
    }
}
