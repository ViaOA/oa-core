package com.viaoa.process;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OACronProcessorTest {

    @Test
    void constructorStartsWithNoCronsAndNotRunning() {
        OACronProcessor processor = new OACronProcessor();

        assertArrayEquals(new OACron[0], processor.getCrons());
        assertFalse(processor.isRunning());
    }

    @Test
    void addIgnoresDuplicatesAndRemoveDeletesCron() {
        OACronProcessor processor = new OACronProcessor();
        TestCron cron = new TestCron();
        cron.setName("test");

        processor.add(cron);
        processor.add(cron);

        assertArrayEquals(new OACron[] { cron }, processor.getCrons());

        processor.remove(cron);

        assertArrayEquals(new OACron[0], processor.getCrons());
    }

    @Test
    void startAndStopToggleRunningReference() {
        OACronProcessor processor = new OACronProcessor();
        try {
            processor.start();
            assertTrue(processor.isRunning());
        }
        finally {
            processor.stop();
        }

        assertFalse(processor.isRunning());
    }

    @Test
    void exposedCallProcessUpdatesLastAndInvokesCron() {
        TestCronProcessor processor = new TestCronProcessor();
        TestCron cron = new TestCron();

        processor.exposeCallProcess(cron, true);

        assertNotNull(cron.getLast());
        assertEquals(1, cron.processCount);
        assertTrue(cron.lastManualFlag);
    }

    @Test
    void callProcessNullCronIsNoOp() {
        TestCronProcessor processor = new TestCronProcessor();

        assertDoesNotThrow(() -> processor.exposeCallProcess(null, true));
        assertDoesNotThrow(() -> processor.callProcessInAnotherThread(null, true));
    }

    @Test
    void beforeProcessDefaultHookIsNoOp() {
        TestCronProcessor processor = new TestCronProcessor();

        assertDoesNotThrow(() -> processor.exposeBeforeProcess());
    }

    private static class TestCronProcessor extends OACronProcessor {
        void exposeCallProcess(OACron cron, boolean manuallyCalled) {
            callProcess(cron, manuallyCalled);
        }

        void exposeBeforeProcess() {
            beforeProcess(null);
        }
    }

    private static class TestCron extends OACron {
        volatile int processCount;
        volatile boolean lastManualFlag;
        TestCron() {
            super("*", "*", "*", "*", "*");
        }

        @Override
        public void process(boolean bManuallyCalled) {
            processCount++;
            lastManualFlag = bManuallyCalled;
        }
    }
}
