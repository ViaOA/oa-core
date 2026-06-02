package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OACircularQueueStatusAndBoundaryTest {

    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) {
            super(size);
        }
    }

    @Test
    void amountAvailableMatchesReadableMessages() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.getHeadPostion();

        assertEquals(0, q.getAmountAvailable(pos));

        q.addMessage("A");
        q.addMessage("B");

        assertEquals(2, q.getAmountAvailable(pos));
        assertEquals(2, q.getMessages(pos, 10, 0).length);
    }

    @Test
    void amountAvailableNeverNegativeForFutureTailDesiredContract() throws Exception {
        StringQueue q = new StringQueue(5);

        assertEquals(0, q.getAmountAvailable(q.getHeadPostion() + 10),
            "future tail should clamp/report zero or fail visibly, not return negative availability");
    }

    @Test
    void rawFutureTailReadDoesNotReturnNegativeOrWrongData() throws Exception {
        StringQueue q = new StringQueue(5);

        assertNull(q.getMessages(q.getHeadPostion() + 10, 10, 0));
    }

    @Test
    void getMessageFiniteWaitHandlesEmptyResultWithoutThrowing() throws Exception {
        StringQueue q = new StringQueue(5);

        assertNull(q.getMessage(q.getHeadPostion(), 1));
    }

    @Test
    void headPositionReportsCommittedLogicalHead() {
        StringQueue q = new StringQueue(2);

        assertEquals(0, q.getHeadPostion());

        q.addMessage("A");
        assertEquals(1, q.getHeadPostion());

        q.addMessage("B");
        assertEquals(2, q.getHeadPostion());
    }

    @Test
    void nullMessagesAreVisibleAsQueuedSlotsCurrentContract() throws Exception {
        StringQueue q = new StringQueue(3);
        long pos = q.getHeadPostion();

        q.addMessage(null);
        q.addMessage("B");

        String[] msgs = q.getMessages(pos, 10, 0);

        assertNotNull(msgs);
        assertEquals(2, msgs.length);
        assertNull(msgs[0]);
        assertEquals("B", msgs[1]);
    }
}
