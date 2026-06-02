package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OACircularQueueEnqueueVisibilityTest {

    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) {
            super(size);
        }
    }

    @Test
    void successfulEnqueueIsVisibleToRawPositionConsumer() throws Exception {
        StringQueue q = new StringQueue(5);
        long tail = q.getHeadPostion();

        int slot = q.addMessageToQueue("A");

        assertEquals(1, q.getHeadPostion());
        assertEquals("A", q.getMessagesAtPos(slot));

        String[] msgs = q.getMessages(tail, 10, 0);
        assertArrayEquals(new String[] { "A" }, msgs);
    }

    @Test
    void successfulEnqueueIsVisibleToRegisteredSession() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.registerSession(101);

        q.addMessage("A");
        q.addMessage("B");

        String[] msgs = q.getMessages(101, pos, 10, 0);

        assertArrayEquals(new String[] { "A", "B" }, msgs);
    }

    @Test
    void getMessageWithFiniteWaitReturnsNullWhenNoMessageAvailable() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.getHeadPostion();

        assertNull(q.getMessage(pos, 1));
    }

    @Test
    void getMessageReturnsFirstMessageWhenAvailable() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.getHeadPostion();

        q.addMessage("A");
        q.addMessage("B");

        assertEquals("A", q.getMessage(pos, 0));
    }

    @Test
    void maxReturnAmountLimitsBatchSize() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.getHeadPostion();

        q.addMessage("A");
        q.addMessage("B");
        q.addMessage("C");

        assertArrayEquals(new String[] { "A", "B" }, q.getMessages(pos, 2, 0));
    }

    @Test
    void maxReturnAmountZeroReturnsAllAvailableCurrentContract() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.getHeadPostion();

        q.addMessage("A");
        q.addMessage("B");

        assertArrayEquals(new String[] { "A", "B" }, q.getMessages(pos, 0, 0));
    }

    @Test
    void emptyImmediateReadReturnsNullArrayCurrentContract() throws Exception {
        StringQueue q = new StringQueue(5);

        assertNull(q.getMessages(q.getHeadPostion(), 10, 0));
    }

    @Test
    void addMessageAliasDelegatesToAddMessageToQueue() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.getHeadPostion();

        q.addMessage("A");

        assertArrayEquals(new String[] { "A" }, q.getMessages(pos, 10, 0));
    }
}
