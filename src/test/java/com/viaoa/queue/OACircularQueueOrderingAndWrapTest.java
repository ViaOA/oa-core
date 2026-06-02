package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OACircularQueueOrderingAndWrapTest {

    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) {
            super(size);
        }
    }

    @Test
    void logicalHeadIncreasesAcrossWraparound() {
        StringQueue q = new StringQueue(3);

        for (int i = 0; i < 10; i++) {
            assertEquals(i, q.getHeadPostion());
            q.addMessage("M" + i);
        }

        assertEquals(10, q.getHeadPostion());
    }

    @Test
    void physicalReturnSlotWrapsWhileHeadIsLogicalPosition() {
        StringQueue q = new StringQueue(3);

        int p0 = q.addMessageToQueue("M0");
        int p1 = q.addMessageToQueue("M1");
        int p2 = q.addMessageToQueue("M2");
        int p3 = q.addMessageToQueue("M3");

        assertEquals(0, p0);
        assertEquals(1, p1);
        assertEquals(2, p2);
        assertEquals(0, p3, "return value is physical slot after wraparound");
        assertEquals(4, q.getHeadPostion(), "head remains logical stream position");
    }

    @Test
    void rawPositionReadsPreserveOrderBeforeWrapOverrun() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.getHeadPostion();

        q.addMessage("A");
        q.addMessage("B");
        q.addMessage("C");

        assertArrayEquals(new String[] { "A", "B", "C" }, q.getMessages(pos, 10, 0));
    }

    @Test
    void rawPositionReadsAfterWrapFromRetainedTailPreserveOrder() throws Exception {
        StringQueue q = new StringQueue(5);

        for (int i = 0; i < 3; i++) {
            q.addMessage("old" + i);
        }

        long retainedTail = q.getHeadPostion();

        q.addMessage("A");
        q.addMessage("B");
        q.addMessage("C");
        q.addMessage("D");

        assertArrayEquals(new String[] { "A", "B", "C", "D" }, q.getMessages(retainedTail, 10, 0));
    }

    @Test
    void exactlyFullQueueBoundaryReadableAndAmountAvailableConsistentDesiredContract() throws Exception {
        StringQueue q = new StringQueue(3);
        long pos = q.getHeadPostion();

        q.addMessage("A");
        q.addMessage("B");
        q.addMessage("C");

        assertEquals(3, q.getAmountAvailable(pos), "exactly full retained window should be available, not overrun");
        assertArrayEquals(new String[] { "A", "B", "C" }, q.getMessages(pos, 10, 0));
    }

    @Test
    void overrunAfterCapacityExceededThrowsVisibleFailure() {
        StringQueue q = new StringQueue(3);
        long pos = q.getHeadPostion();

        q.addMessage("A");
        q.addMessage("B");
        q.addMessage("C");
        q.addMessage("D");

        assertThrows(Exception.class, () -> q.getMessages(pos, 10, 0));
        assertThrows(Exception.class, () -> q.getAmountAvailable(pos));
    }
}
