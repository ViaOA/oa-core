package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OACircularQueueFinalApiBoundaryTest {
    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) { super(size); }
    }

    @Test
    void msWaitUntilNotifiedConstantIsNegativeOne() {
        assertEquals(-1, new StringQueue(1).msWaitUntilNotified);
    }

    @Test
    void getHeadPostionSpellingApiStillWorks() {
        StringQueue q = new StringQueue(2);
        assertEquals(0, q.getHeadPostion());
        q.addMessage("A");
        assertEquals(1, q.getHeadPostion());
    }

    @Test
    void getMessagesAtPosReturnsPhysicalSlotNotLogicalTail() {
        StringQueue q = new StringQueue(2);
        q.addMessage("A"); q.addMessage("B"); q.addMessage("C");
        assertEquals("C", q.getMessagesAtPos(0));
        assertEquals("B", q.getMessagesAtPos(1));
        assertNull(q.getMessagesAtPos(2));
    }

    @Test
    void addMessageReturnValueDocumentsPhysicalSlotAfterWrap() {
        StringQueue q = new StringQueue(2);
        assertEquals(0, q.addMessage("A"));
        assertEquals(1, q.addMessage("B"));
        assertEquals(0, q.addMessage("C"));
        assertEquals(3, q.getHeadPostion());
    }

    @Test
    void nullNameCanBeSetAfterNonNullName() {
        StringQueue q = new StringQueue(2);
        q.setName("queue");
        assertEquals("queue", q.getName());
        q.setName(null);
        assertNull(q.getName());
    }
}
