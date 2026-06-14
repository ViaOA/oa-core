package com.viaoa.runtime.thread;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.viaoa.datetime.OADateTime;

class OAThreadTest {
    @Test
    void runExecutesRunnableAndSleepHelpersHandleSafeInputs() {
        AtomicInteger counter = new AtomicInteger();
        OAThread thread = new OAThread(counter::incrementAndGet);

        thread.run();
        assertEquals(1, counter.get());

        assertDoesNotThrow(() -> {
            OAThread.yield();
            OAThread.delay(-1);
            OAThread.sleep(-1);
            OAThread.sleepUntil(null);
            OAThread.sleepUntil(new OADateTime(0L), 1);
        });
    }
}
