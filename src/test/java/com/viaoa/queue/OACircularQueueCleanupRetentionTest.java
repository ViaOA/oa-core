package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OACircularQueueCleanupRetentionTest {

    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) {
            super(size);
        }

        void cleanupNow() {
            cleanupQueue();
        }
    }

    @Test
    void cleanupDoesNotClearUnreadMessageForSlowSession() throws Exception {
        StringQueue q = new StringQueue(10);
        long slow = q.registerSession(1);
        long fast = q.registerSession(2);

        q.addMessage("A");
        q.addMessage("B");
        q.addMessage("C");

        assertArrayEquals(new String[] { "A", "B", "C" }, q.getMessages(2, fast, 10, 0));

        q.cleanupNow();

        assertArrayEquals(new String[] { "A", "B", "C" }, q.getMessages(1, slow, 10, 0));
    }

    @Test
    void cleanupClearsOnlyAfterAllSessionsAdvancePastMessages() throws Exception {
        StringQueue q = new StringQueue(10);
        long p1 = q.registerSession(1);
        long p2 = q.registerSession(2);

        q.addMessage("A");
        q.addMessage("B");

        assertArrayEquals(new String[] { "A", "B" }, q.getMessages(1, p1, 10, 0));
        assertArrayEquals(new String[] { "A", "B" }, q.getMessages(2, p2, 10, 0));

        q.cleanupNow();

        assertNull(q.getMessagesAtPos(0));
        assertNull(q.getMessagesAtPos(1));
    }

    @Test
    void cleanupWithNoSessionsLeavesMessagesAvailableToRawTail() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.getHeadPostion();

        q.addMessage("A");
        q.addMessage("B");

        q.cleanupNow();

        assertArrayEquals(new String[] { "A", "B" }, q.getMessages(pos, 10, 0));
    }

    @Test
    void unregisteringSlowSessionAllowsCleanupAfterRemainingSessionAdvances() throws Exception {
        StringQueue q = new StringQueue(10);
        long slow = q.registerSession(1);
        long fast = q.registerSession(2);

        q.addMessage("A");
        q.addMessage("B");

        assertArrayEquals(new String[] { "A", "B" }, q.getMessages(2, fast, 10, 0));

        q.unregisterSession(1);
        q.cleanupNow();

        assertNull(q.getMessagesAtPos(0));
        assertNull(q.getMessagesAtPos(1));
    }

    @Test
    void cleanupAfterPartialProgressKeepsUnreadTail() throws Exception {
        StringQueue q = new StringQueue(10);
        long p1 = q.registerSession(1);
        long p2 = q.registerSession(2);

        q.addMessage("A");
        q.addMessage("B");
        q.addMessage("C");

        assertArrayEquals(new String[] { "A" }, q.getMessages(1, p1, 1, 0));
        assertArrayEquals(new String[] { "A", "B", "C" }, q.getMessages(2, p2, 10, 0));

        q.cleanupNow();

        assertNull(q.getMessagesAtPos(0));
        assertEquals("B", q.getMessagesAtPos(1));
        assertEquals("C", q.getMessagesAtPos(2));

        assertArrayEquals(new String[] { "B", "C" }, q.getMessages(1, p1 + 1, 10, 0));
    }
}
