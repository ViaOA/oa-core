package com.viaoa.queue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OACircularQueueSessionTest {

    static class StringQueue extends OACircularQueue<String> {
        StringQueue(int size) {
            super(size);
        }
    }

    @Test
    void registerSessionStartsAtCurrentHead() throws Exception {
        StringQueue q = new StringQueue(5);
        q.addMessage("old");

        long pos = q.registerSession(1);

        assertEquals(q.getHeadPostion(), pos);

        q.addMessage("new");

        assertArrayEquals(new String[] { "new" }, q.getMessages(1, pos, 10, 0));
    }

    @Test
    void sessionReadsDoNotReceiveSameMessageTwiceWhenCallerUsesAdvancedPosition() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.registerSession(1);

        q.addMessage("A");
        q.addMessage("B");

        String[] first = q.getMessages(1, pos, 1, 0);
        assertArrayEquals(new String[] { "A" }, first);

        String[] second = q.getMessages(1, pos + first.length, 10, 0);
        assertArrayEquals(new String[] { "B" }, second);
    }

    @Test
    void timedOutSessionReadDoesNotAdvancePastFutureMessages() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.registerSession(1);

        assertNull(q.getMessages(1, pos, 10, 1));

        q.addMessage("A");

        assertArrayEquals(new String[] { "A" }, q.getMessages(1, pos, 10, 0));
    }

    @Test
    void unregisterSessionStopsTrackedConsumptionForThatIdDesiredContract() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.registerSession(1);

        q.addMessage("A");
        q.unregisterSession(1);

        assertThrows(Exception.class, () -> q.getMessages(1, pos, 10, 0),
            "registered-session API should not silently downgrade missing session to raw positional read");
    }

    @Test
    void missingRegisteredSessionFailsVisiblyDesiredContract() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.getHeadPostion();

        q.registerSession(1);
        q.addMessage("A");

        assertThrows(Exception.class, () -> q.getMessages(999, pos, 10, 0),
            "unknown non-negative session id should not silently read as untracked consumer");
    }

    @Test
    void keepAliveForUnknownSessionIsSafeNoop() {
        StringQueue q = new StringQueue(5);

        assertDoesNotThrow(() -> q.keepAlive(99));
    }

    @Test
    void duplicateRegisterDoesNotSilentlySkipUnreadMessagesDesiredContract() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.registerSession(1);

        q.addMessage("A");

        assertThrows(Exception.class, () -> q.registerSession(1),
            "duplicate register should fail or be explicit reset instead of silently replacing unread session");

        assertArrayEquals(new String[] { "A" }, q.getMessages(1, pos, 10, 0));
    }

    @Test
    void futureTailForSessionDoesNotAdvanceSessionPastHeadDesiredContract() throws Exception {
        StringQueue q = new StringQueue(5);
        long pos = q.registerSession(1);

        long future = q.getHeadPostion() + 100;
        assertThrows(Exception.class, () -> q.getMessages(1, future, 10, 0),
            "session read with future tail should fail visibly, not normalize then advance from caller tail");

        q.addMessage("A");

        assertArrayEquals(new String[] { "A" }, q.getMessages(1, pos, 10, 0));
    }
}
