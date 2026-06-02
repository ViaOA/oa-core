package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OACircularQueueSessionProgressEdgeTest {

    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) {
            super(size);
        }
    }

    @Test
    void sessionPositionAdvancesOnlyByDeliveredMessages() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.registerSession(1);

        q.addMessage("A");
        q.addMessage("B");
        q.addMessage("C");

        String[] first = q.getMessages(1, pos, 2, 0);
        assertArrayEquals(new String[] { "A", "B" }, first);

        q.addMessage("D");

        String[] next = q.getMessages(1, pos + first.length, 10, 0);
        assertArrayEquals(new String[] { "C", "D" }, next);
    }

    @Test
    void zeroMaxReturnAmountSessionReadAdvancesByAllDeliveredMessages() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.registerSession(1);

        q.addMessage("A");
        q.addMessage("B");

        String[] vals = q.getMessages(1, pos, 0, 0);
        assertArrayEquals(new String[] { "A", "B" }, vals);

        q.addMessage("C");

        assertArrayEquals(new String[] { "C" }, q.getMessages(1, pos + vals.length, 10, 0));
    }

    @Test
    void sessionOverrunIsVisibleAndRetryFromSameBadPositionStillFails() {
        StringQueue q = new StringQueue(3);
        long pos = q.registerSession(1);

        q.addMessage("A");
        q.addMessage("B");
        q.addMessage("C");
        q.addMessage("D");

        assertThrows(Exception.class, () -> q.getMessages(1, pos, 10, 0));
        assertThrows(Exception.class, () -> q.getMessages(1, pos, 10, 0));
    }

    @Test
    void keepAliveDoesNotAdvanceSessionPosition() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.registerSession(1);

        q.addMessage("A");
        q.keepAlive(1);

        assertArrayEquals(new String[] { "A" }, q.getMessages(1, pos, 10, 0));
    }

    @Test
    void rawReadDoesNotAcknowledgeRegisteredSession() throws Exception {
        StringQueue q = new StringQueue(10);
        long pos = q.registerSession(1);

        q.addMessage("A");
        q.addMessage("B");

        assertArrayEquals(new String[] { "A", "B" }, q.getMessages(pos, 10, 0));
        assertArrayEquals(new String[] { "A", "B" }, q.getMessages(1, pos, 10, 0));
    }
}
