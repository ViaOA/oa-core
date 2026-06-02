package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OACircularQueueBasicTest {

    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) {
            super(size);
        }
    }

    static class ExplicitQueue extends OACircularQueue<String> {
        ExplicitQueue(int size) {
            super(String.class, size);
        }
    }

    @Test
    void genericConstructorDiscoversMessageClass() {
        StringQueue q = new StringQueue(5);

        assertEquals(5, q.getSize());
        assertEquals(0, q.getHeadPostion());
    }

    @Test
    void explicitClassConstructorInitializesQueue() {
        ExplicitQueue q = new ExplicitQueue(3);

        assertEquals(3, q.getSize());
        assertEquals(0, q.getHeadPostion());
    }

    @Test
    void nameRoundTrips() {
        StringQueue q = new StringQueue(2);

        assertNull(q.getName());

        q.setName("syncQueue");

        assertEquals("syncQueue", q.getName());
    }

    @Test
    void setSizeRejectsZeroOrNegativeCapacityDesiredContract() {
        assertThrows(RuntimeException.class, () -> new StringQueue(0));
        assertThrows(RuntimeException.class, () -> new StringQueue(-1));

        StringQueue q = new StringQueue(2);
        assertThrows(RuntimeException.class, () -> q.setSize(0));
        assertThrows(RuntimeException.class, () -> q.setSize(-1));
    }

    @Test
    void setSizeBeforeUseAllocatesNewCapacity() {
        StringQueue q = new StringQueue(2);

        q.setSize(4);

        assertEquals(4, q.getSize());
        assertNull(q.getMessagesAtPos(0));
        assertNull(q.getMessagesAtPos(3));
        assertNull(q.getMessagesAtPos(4));
    }

    @Test
    void liveResizeAfterEnqueueFailsOrPreservesVisibleMessagesDesiredContract() throws Exception {
        StringQueue q = new StringQueue(4);
        long pos = q.getHeadPostion();

        q.addMessage("A");
        q.addMessage("B");

        try {
            q.setSize(8);
        } catch (RuntimeException expected) {
            assertArrayEquals(new String[] { "A", "B" }, q.getMessages(pos, 10, 0));
            return;
        }

        String[] msgs = q.getMessages(pos, 10, 0);
        assertNotNull(msgs, "live resize must not silently discard already queued messages");
        assertArrayEquals(new String[] { "A", "B" }, msgs);
    }

    @Test
    void getMessagesAtPosBoundsAreSafe() {
        StringQueue q = new StringQueue(2);

        assertNull(q.getMessagesAtPos(-1));
        assertNull(q.getMessagesAtPos(0));
        assertNull(q.getMessagesAtPos(2));

        q.addMessage("A");

        assertEquals("A", q.getMessagesAtPos(0));
    }
}
