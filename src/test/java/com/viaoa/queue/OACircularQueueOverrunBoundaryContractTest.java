package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OACircularQueueOverrunBoundaryContractTest {
    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) { super(size); }
    }

    @Test
    void exactlyFullBoundaryIsReadableAcrossAvailabilityAndBatchReadDesiredContract() throws Exception {
        StringQueue q = new StringQueue(4);
        long tail = q.getHeadPostion();
        q.addMessage("A"); q.addMessage("B"); q.addMessage("C"); q.addMessage("D");
        assertEquals(4, q.getAmountAvailable(tail));
        assertArrayEquals(new String[] { "A", "B", "C", "D" }, q.getMessages(tail, 10, 0));
    }

    @Test
    void onePastCapacityBoundaryThrowsAcrossAvailabilityAndBatchRead() {
        StringQueue q = new StringQueue(4);
        long tail = q.getHeadPostion();
        q.addMessage("A"); q.addMessage("B"); q.addMessage("C"); q.addMessage("D"); q.addMessage("E");
        assertThrows(Exception.class, () -> q.getAmountAvailable(tail));
        assertThrows(Exception.class, () -> q.getMessages(tail, 10, 0));
    }

    @Test
    void amountAvailableForFutureTailDoesNotReturnNegativeDesiredContract() throws Exception {
        StringQueue q = new StringQueue(4);
        int amt = q.getAmountAvailable(q.getHeadPostion() + 10);
        assertTrue(amt >= 0, "availability must never be negative");
        assertEquals(0, amt);
    }

    @Test
    void rawReadForFutureTailReturnsNullAndDoesNotChangeHead() throws Exception {
        StringQueue q = new StringQueue(4);
        long head = q.getHeadPostion();
        assertNull(q.getMessages(head + 10, 10, 0));
        assertEquals(head, q.getHeadPostion());
    }

    @Test
    void maxReturnAmountCannotMaskOverrun() {
        StringQueue q = new StringQueue(3);
        long tail = q.getHeadPostion();
        q.addMessage("A"); q.addMessage("B"); q.addMessage("C"); q.addMessage("D");
        assertThrows(Exception.class, () -> q.getMessages(tail, 1, 0));
    }

    @Test
    void getMessageCannotMaskOverrun() {
        StringQueue q = new StringQueue(3);
        long tail = q.getHeadPostion();
        q.addMessage("A"); q.addMessage("B"); q.addMessage("C"); q.addMessage("D");
        assertThrows(Exception.class, () -> q.getMessage(tail, 0));
    }
}
