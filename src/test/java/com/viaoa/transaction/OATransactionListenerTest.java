package com.viaoa.transaction;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

class OATransactionListenerTest {

    @Test
    void commitCanBeImplementedAndReceivesTransaction() {
        OATransaction tx = new OATransaction();
        RecordingListener listener = new RecordingListener();

        listener.commit(tx);

        assertSame(tx, listener.transactions.get(0));
        assertEquals(List.of("commit"), listener.calls);
    }

    @Test
    void rollbackCanBeImplementedAndReceivesTransaction() {
        OATransaction tx = new OATransaction();
        RecordingListener listener = new RecordingListener();

        listener.rollback(tx);

        assertSame(tx, listener.transactions.get(0));
        assertEquals(List.of("rollback"), listener.calls);
    }

    @Test
    void executeOpenBatchesCanBeImplementedAndReceivesTransaction() {
        OATransaction tx = new OATransaction();
        RecordingListener listener = new RecordingListener();

        listener.executeOpenBatches(tx);

        assertSame(tx, listener.transactions.get(0));
        assertEquals(List.of("batch"), listener.calls);
    }

    private static class RecordingListener implements OATransactionListener {
        final List<String> calls = new ArrayList<>();
        final List<OATransaction> transactions = new ArrayList<>();

        @Override
        public void commit(OATransaction t) {
            calls.add("commit");
            transactions.add(t);
        }

        @Override
        public void rollback(OATransaction t) {
            calls.add("rollback");
            transactions.add(t);
        }

        @Override
        public void executeOpenBatches(OATransaction t) {
            calls.add("batch");
            transactions.add(t);
        }
    }
}
