package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OACircularQueueResizeAndResetContractTest {
    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) { super(size); }
    }

    @Test
    void resizeBeforeAnyUseIsAllowed() {
        StringQueue q = new StringQueue(2);
        q.setSize(5);
        assertEquals(5, q.getSize());
        assertEquals(0, q.getHeadPostion());
    }

    @Test
    void resizeAfterEnqueueFailsOrPreservesMessagesDesiredContract() throws Exception {
        StringQueue q = new StringQueue(3);
        long tail = q.getHeadPostion();
        q.addMessage("A"); q.addMessage("B");
        try {
            q.setSize(6);
        } catch (RuntimeException ex) {
            assertArrayEquals(new String[] { "A", "B" }, q.getMessages(tail, 10, 0));
            return;
        }
        assertArrayEquals(new String[] { "A", "B" }, q.getMessages(tail, 10, 0));
    }

    @Test
    void resizeAfterSessionRegistrationFailsOrPreservesSessionPositionDesiredContract() throws Exception {
        StringQueue q = new StringQueue(3);
        long pos = q.registerSession(1);
        q.addMessage("A");
        try {
            q.setSize(6);
        } catch (RuntimeException ex) {
            assertArrayEquals(new String[] { "A" }, q.getMessages(1, pos, 10, 0));
            return;
        }
        assertArrayEquals(new String[] { "A" }, q.getMessages(1, pos, 10, 0));
    }

    @Test
    void resizeToSameSizeAfterUseStillMustNotClearMessagesDesiredContract() throws Exception {
        StringQueue q = new StringQueue(3);
        long tail = q.getHeadPostion();
        q.addMessage("A");
        try {
            q.setSize(3);
        } catch (RuntimeException ex) {
            assertArrayEquals(new String[] { "A" }, q.getMessages(tail, 10, 0));
            return;
        }
        assertArrayEquals(new String[] { "A" }, q.getMessages(tail, 10, 0));
    }

    @Test
    void invalidResizeDoesNotCorruptExistingQueueDesiredContract() throws Exception {
        StringQueue q = new StringQueue(3);
        long tail = q.getHeadPostion();
        q.addMessage("A");
        assertThrows(RuntimeException.class, () -> q.setSize(0));
        assertEquals(3, q.getSize());
        assertArrayEquals(new String[] { "A" }, q.getMessages(tail, 10, 0));
    }
}
