package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class OACircularQueueTest {

    @Test
    void constructorsResolveGenericOrExplicitTypeAndInitializeSize() {
        StringQueue explicit = new StringQueue(4);
        assertEquals(4, explicit.getSize());
        assertEquals(0, explicit.getHeadPostion());

        GenericStringQueue generic = new GenericStringQueue();
        generic.setSize(3);
        assertEquals(3, generic.getSize());
        assertNull(generic.getMessagesAtPos(0));
    }

    @Test
    void protectedConstructorRequiresResolvableConcreteType() {
        RuntimeException ex = assertThrows(RuntimeException.class, RawQueue::new);
        assertTrue(ex.getMessage().contains("class must define <TYPE>"));
    }

    @Test
    void setSizeReplacesBackingArrayAndGetSizeReturnsCurrentSize() {
        StringQueue queue = new StringQueue(3);
        queue.addMessage("one");
        assertEquals("one", queue.getMessagesAtPos(0));

        queue.setSize(5);

        assertEquals(5, queue.getSize());
        assertNull(queue.getMessagesAtPos(0));
    }

    @Test
    void setSizeRejectsNegativeArraySize() {
        StringQueue queue = new StringQueue(2);

        assertThrows(NegativeArraySizeException.class, () -> queue.setSize(-1));
    }

    @Test
    void registerSessionStartsAtCurrentHeadAndUnregisterRemovesTracking() throws Exception {
        StringQueue queue = new StringQueue(4);
        queue.addMessage("before");

        long pos = queue.registerSession(7);
        queue.addMessage("after");

        assertEquals(1, pos);
        assertArrayEquals(new String[] { "after" }, queue.getMessages(7, pos, 10, 1));

        queue.unregisterSession(7);
        queue.addMessage("untracked");
        assertArrayEquals(new String[] { "untracked" }, queue.getMessages(7, 2, 10, 1));
    }

    @Test
    void cleanupQueueClearsSlotsConsumedByAllRegisteredSessions() throws Exception {
        StringQueue queue = new StringQueue(5);
        long sessionPos = queue.registerSession(1);
        queue.addMessage("one");
        queue.addMessage("two");

        assertArrayEquals(new String[] { "one" }, queue.getMessages(1, sessionPos, 1, 1));
        assertArrayEquals(new String[] { "two" }, queue.getMessages(1, sessionPos + 1, 1, 1));
        queue.callCleanupQueue();

        assertNull(queue.getMessagesAtPos(0));
    }

    @Test
    void getHeadPostionAdvancesWithEachAddedMessage() {
        StringQueue queue = new StringQueue(3);

        assertEquals(0, queue.getHeadPostion());
        queue.addMessageToQueue("one");
        assertEquals(1, queue.getHeadPostion());
        queue.addMessageToQueue("two");
        assertEquals(2, queue.getHeadPostion());
    }

    @Test
    void addMessageToQueueReturnsPhysicalArrayPositionAndWraps() {
        StringQueue queue = new StringQueue(2);

        assertEquals(0, queue.addMessageToQueue("one"));
        assertEquals(1, queue.addMessage("two"));
        assertEquals(0, queue.addMessageToQueue("three", 0));
        assertEquals(1, queue.addMessageToQueue("four", 0, -1));

        assertEquals("three", queue.getMessagesAtPos(0));
        assertEquals("four", queue.getMessagesAtPos(1));
        assertEquals(4, queue.getHeadPostion());
    }

    @Test
    void addMessageToQueueAllowsDuplicateAndNullMessages() throws Exception {
        StringQueue queue = new StringQueue(5);

        queue.addMessage("same");
        queue.addMessage("same");
        queue.addMessage(null);

        assertEquals("same", queue.getMessage(0, 1));
        assertEquals("same", queue.getMessage(1, 1));
        assertNull(queue.getMessage(2, 1));
    }

    @Test
    void addMessageToQueueMarksSlowInactiveSessionInsteadOfWaitingByDefault() throws Exception {
        StringQueue queue = new StringQueue(2);
        long pos = queue.registerSession(1);

        queue.addMessage("one");
        queue.addMessage("two");
        queue.addMessage("three");

        Exception ex = assertThrows(Exception.class, () -> queue.getMessages(1, pos, 10, 1));
        assertTrue(ex.getMessage().contains("message queue overrun"));
        assertEquals(3, queue.getHeadPostion());
    }

    @Test
    void shouldWaitOnSlowSessionDefaultIsFalse() {
        StringQueue queue = new StringQueue(2);

        assertFalse(queue.callShouldWaitOnSlowSession(1, 1_500));
    }

    @Test
    void getMessageWaitsForMessageAndReturnsFirstAvailableValue() throws Exception {
        StringQueue queue = new StringQueue(3);
        long pos = queue.getHeadPostion();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch waiting = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            try {
                waiting.countDown();
                result.set(queue.getMessage(pos));
            } catch (Throwable e) {
                failure.set(e);
            } finally {
                done.countDown();
            }
        }, "OACircularQueueTest-getMessage");
        thread.start();

        assertTrue(waiting.await(1, TimeUnit.SECONDS));
        queue.addMessage("delivered");
        assertTrue(done.await(2, TimeUnit.SECONDS));

        if (failure.get() != null) {
            fail(failure.get());
        }
        assertEquals("delivered", result.get());
    }

    @Test
    void getMessageWithFiniteWaitReturnsNullWhenQueueIsEmpty() throws Exception {
        StringQueue queue = new StringQueue(3);

        assertNull(queue.getMessage(queue.getHeadPostion(), 1));
    }

    @Test
    void getAmountAvailableReturnsDifferenceBetweenHeadAndTailAndDetectsOverrun() throws Exception {
        StringQueue queue = new StringQueue(3);
        queue.addMessage("one");
        queue.addMessage("two");

        assertEquals(2, queue.getAmountAvailable(0));
        assertEquals(1, queue.getAmountAvailable(1));
        assertEquals(0, queue.getAmountAvailable(2));

        queue.addMessage("three");
        assertThrows(Exception.class, () -> queue.getAmountAvailable(0));
    }

    @Test
    void getMessagesReturnsAvailableMessagesInFifoOrder() throws Exception {
        StringQueue queue = new StringQueue(6);
        queue.addMessage("one");
        queue.addMessage("two");
        queue.addMessage("three");
        queue.addMessage("four");

        assertArrayEquals(new String[] { "one", "two", "three" }, queue.getMessages(0, 3));
        assertArrayEquals(new String[] { "two", "three" }, queue.getMessages(1, 2));
        assertArrayEquals(new String[] { "two" }, queue.getMessages(1, 1, 1));
        assertNull(queue.getMessages(queue.getHeadPostion(), 10, 1));
    }

    @Test
    void getMessagesDetectsOverrunAndNormalizesFutureTailToCurrentHead() throws Exception {
        StringQueue queue = new StringQueue(2);
        queue.addMessage("one");
        queue.addMessage("two");
        queue.addMessage("three");

        assertThrows(Exception.class, () -> queue.getMessages(0, 10, 1));
        assertNull(queue.getMessages(100, 10, 1));
    }

    @Test
    void sessionGetMessagesUpdatesProgressAcrossRepeatedReads() throws Exception {
        StringQueue queue = new StringQueue(5);
        long pos = queue.registerSession(10);
        queue.addMessage("one");
        queue.addMessage("two");

        String[] firstBatch = queue.getMessages(10, pos, 1, 1);
        assertArrayEquals(new String[] { "one" }, firstBatch);

        queue.addMessage("three");
        assertArrayEquals(new String[] { "two" }, queue.getMessages(10, pos + firstBatch.length, 1, 1));
        assertArrayEquals(new String[] { "three" }, queue.getMessages(10, pos + firstBatch.length + 1, 1, 1));
    }

    @Test
    void keepAliveRefreshesInactiveRegisteredSession() throws Exception {
        StringQueue queue = new StringQueue(2);
        long pos = queue.registerSession(12);

        queue.addMessage("one");
        queue.keepAlive(12);
        assertArrayEquals(new String[] { "one" }, queue.getMessages(12, pos, 10, 1));

        assertDoesNotThrow(() -> queue.keepAlive(999));
    }

    @Test
    void getMessagesAtPosReturnsNullForInvalidPositions() {
        StringQueue queue = new StringQueue(2);
        queue.addMessage("one");

        assertNull(queue.getMessagesAtPos(-1));
        assertEquals("one", queue.getMessagesAtPos(0));
        assertNull(queue.getMessagesAtPos(2));
    }

    @Test
    void setNameAndGetNameRoundTrip() {
        StringQueue queue = new StringQueue(2);

        assertNull(queue.getName());
        queue.setName("test-queue");
        assertEquals("test-queue", queue.getName());
    }

    @Test
    void repeatedOperationsRemainDeterministicAfterWraparound() throws Exception {
        StringQueue queue = new StringQueue(5);
        long pos = queue.registerSession(44);

        for (int i = 0; i < 4; i++) {
            queue.addMessage("m" + i);
        }
        assertEquals(Arrays.asList("m0", "m1"), Arrays.asList(queue.getMessages(44, pos, 2, 1)));
        pos += 2;

        queue.addMessage("m4");
        queue.addMessage("m5");
        queue.addMessage("m6");

        assertEquals(Arrays.asList("m2", "m3"), Arrays.asList(queue.getMessages(44, pos, 2, 1)));
        pos += 2;
        assertEquals(Arrays.asList("m4", "m5"), Arrays.asList(queue.getMessages(44, pos, 2, 1)));
        assertEquals(7, queue.getHeadPostion());
    }

    private static class GenericStringQueue extends OACircularQueue<String> {
    }

    @SuppressWarnings({ "rawtypes", "serial" })
    private static class RawQueue extends OACircularQueue {
    }

    private static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) {
            super(String.class, size);
        }

        void callCleanupQueue() {
            cleanupQueue();
        }

        boolean callShouldWaitOnSlowSession(int sessionId, int msSinceLastRead) {
            return shouldWaitOnSlowSession(sessionId, msSinceLastRead);
        }
    }
}
