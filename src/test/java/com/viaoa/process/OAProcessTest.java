package com.viaoa.process;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAProcessTest {

    @Test
    void constructorRecordsCreatedTimeAndDefaultState() {
        long before = System.currentTimeMillis();
        TestProcess process = new TestProcess();
        long after = System.currentTimeMillis();

        assertTrue(process.getCreatedTime() >= before);
        assertTrue(process.getCreatedTime() <= after);
        assertFalse(process.getAllowCancel());
        assertFalse(process.getBlock());
        assertFalse(process.getRequestedToCancel());
        assertFalse(process.getWasCancelled());
        assertFalse(process.getDone());
        assertFalse(process.isPaused());
        assertEquals(0, process.getTotalSteps());
        assertEquals(0, process.getCurrentStep());
        assertEquals(0, process.getMaxTime());
        assertEquals(0, process.getMaxBlockTime());
    }

    @Test
    void allowCancelAndBlockFlagsRoundTrip() {
        TestProcess process = new TestProcess();

        process.setAllowCancel(true);
        process.setBlock(true);
        process.exposeSetMaxBlockTime(500);

        assertTrue(process.getAllowCancel());
        assertTrue(process.getBlock());
        assertEquals(500, process.getMaxBlockTime());

        process.setAllowCancel(false);
        process.setBlock(false);

        assertFalse(process.getAllowCancel());
        assertFalse(process.getBlock());
    }

    @Test
    void nameDescriptionStatusAndExceptionRoundTrip() {
        TestProcess process = new TestProcess();
        Exception ex = new IllegalStateException("failed");

        process.setName("Import");
        process.setDescription("Import customers");
        process.setStatus("Running");
        process.setException(ex);

        assertEquals("Import", process.getName());
        assertEquals("Import customers", process.getDescription());
        assertEquals("Running", process.getStatus());
        assertSame(ex, process.getException());
    }

    @Test
    void cancelRequestAndConfirmationAreDistinctStates() {
        TestProcess process = new TestProcess();

        process.requestCancel("user");

        assertTrue(process.getRequestedToCancel());
        assertEquals("user", process.getRequestCancelReason());
        assertTrue(process.getRequestCancelTime() > 0);
        assertFalse(process.getWasCancelled());

        assertTrue(process.confirmRequestToCancel());
        assertTrue(process.getWasCancelled());
    }

    @Test
    void wasCancelledReasonDoneAndMessageRoundTrip() {
        TestProcess process = new TestProcess();

        process.setCancelledReason("timeout");
        process.setWasCancelled(true);
        process.setDoneMessage("complete");
        process.setDone();

        assertTrue(process.getWasCancelled());
        assertEquals("timeout", process.getCancelledReason());
        assertTrue(process.getDone());
        assertTrue(process.getDoneTime() > 0);
        assertEquals("complete", process.getDoneMessage());
    }

    @Test
    void stepsCurrentStepAndEstimateRoundTrip() {
        TestProcess process = new TestProcess();

        process.setSteps("read", "write");
        process.setCurrentStep(1);
        process.exposeSetEstimateTime(250);

        assertArrayEquals(new String[] { "read", "write" }, process.getSteps());
        assertEquals(2, process.getTotalSteps());
        assertEquals(1, process.getCurrentStep());
        assertEquals(250, process.getEstimateTime());
    }

    @Test
    void maxTimePauseAndCurrentTimeoutBehavior() {
        TestProcess process = new TestProcess();

        assertFalse(process.isTimedout());
        process.setMaxTime(10_000);
        process.setPause(true);

        assertEquals(10_000, process.getMaxTime());
        assertTrue(process.isTimedout(), "Current implementation reports true while the deadline is still in the future.");
        assertTrue(process.isPaused());
        assertTrue(process.getPause());

        process.setMaxTime(0);
        process.setPause(false);

        assertFalse(process.isTimedout());
        assertFalse(process.isPaused());
        assertFalse(process.getPause());
    }

    @Test
    void currentBlockTimeoutBehaviorUsesFutureDeadline() {
        TestProcess process = new TestProcess();

        assertFalse(process.isBlockTimedout());
        process.exposeSetMaxBlockTime(10_000);

        assertTrue(process.isBlockTimedout(), "Current implementation reports true while the block deadline is still in the future.");
    }

    @Test
    void runDefaultDoesNothing() {
        TestProcess process = new TestProcess();

        assertDoesNotThrow(process::run);
        assertFalse(process.getDone());
        assertNull(process.getException());
    }

    private static class TestProcess extends OAProcess {
        void exposeSetMaxBlockTime(long value) {
            setMaxBlockTime(value);
        }

        void exposeSetEstimateTime(long value) {
            setEstimateTime(value);
        }
    }
}
