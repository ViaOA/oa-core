package com.viaoa.transaction;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.util.*;

import org.junit.jupiter.api.*;

import com.viaoa.runtime.*;

class OATransactionTest {

    @AfterEach
    void afterEach() {
        clearCurrentTransaction();
    }

    @Test
    void constructorWithIsolationLevelStoresLevel() {
        OATransaction tx = new OATransaction(Connection.TRANSACTION_SERIALIZABLE);

        assertEquals(Connection.TRANSACTION_SERIALIZABLE, tx.getTransactionIsolationLevel());
        assertFalse(tx.isStarted());
    }

    @Test
    void defaultConstructorUsesReadCommittedIsolationLevel() {
        OATransaction tx = new OATransaction();

        assertEquals(Connection.TRANSACTION_READ_COMMITTED, tx.getTransactionIsolationLevel());
    }

    @Test
    void getTransactionIsolationLevelReturnsConfiguredValue() {
        OATransaction tx = new OATransaction(Connection.TRANSACTION_REPEATABLE_READ);

        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, tx.getTransactionIsolationLevel());
    }

    @Test
    void setUseBatchUpdatesBatchFlag() {
        OATransaction tx = new OATransaction();

        tx.setUseBatch(true);
        assertTrue(tx.getUseBatch());

        tx.setUseBatch(false);
        assertFalse(tx.getUseBatch());
    }

    @Test
    void getUseBatchDefaultsToFalse() {
        OATransaction tx = new OATransaction();

        assertFalse(tx.getUseBatch());
    }

    @Test
    void setAllowWritesIfDsIsReadonlyUpdatesFlag() {
        OATransaction tx = new OATransaction();

        tx.setAllowWritesIfDsIsReadonly(true);
        assertTrue(tx.getAllowWritesIfDsIsReadonly());

        tx.setAllowWritesIfDsIsReadonly(false);
        assertFalse(tx.getAllowWritesIfDsIsReadonly());
    }

    @Test
    void getAllowWritesIfDsIsReadonlyDefaultsToFalse() {
        OATransaction tx = new OATransaction();

        assertFalse(tx.getAllowWritesIfDsIsReadonly());
    }

    @Test
    void startBindsTransactionToCurrentThread() {
        OATransaction tx = new OATransaction();

        tx.start();

        assertTrue(tx.isStarted());
        assertSame(tx, currentTransaction());
    }

    @Test
    void startOverwritesExistingTransactionCurrentBehavior() {
        OATransaction outer = new OATransaction();
        OATransaction inner = new OATransaction();
        outer.start();

        inner.start();

        assertFalse(outer.isStarted());
        assertTrue(inner.isStarted());
        assertSame(inner, currentTransaction());
    }

    @Test
    void isStartedReturnsFalseBeforeStartAndAfterCompletion() {
        OATransaction tx = new OATransaction();

        assertFalse(tx.isStarted());

        tx.start();
        tx.commit();

        assertFalse(tx.isStarted());
    }

    @Test
    void rollbackNotifiesListenersInOrderAndClearsThreadTransaction() {
        OATransaction tx = new OATransaction();
        List<String> calls = new ArrayList<>();
        RecordingListener l1 = new RecordingListener("one", calls);
        RecordingListener l2 = new RecordingListener("two", calls);
        tx.addTransactionListener(l1);
        tx.addTransactionListener(l2);
        tx.start();

        tx.rollback();

        assertEquals(List.of("one.rollback", "two.rollback"), calls);
        assertFalse(tx.isStarted());
        assertNull(currentTransaction());
    }

    @Test
    void rollbackClearsThreadTransactionWhenListenerThrows() {
        OATransaction tx = new OATransaction();
        tx.addTransactionListener(new ThrowingListener("rollback"));
        tx.start();

        RuntimeException ex = assertThrows(RuntimeException.class, tx::rollback);

        assertEquals("rollback", ex.getMessage());
        assertFalse(tx.isStarted());
        assertNull(currentTransaction());
    }

    @Test
    void commitNotifiesListenersInOrderAndClearsThreadTransaction() {
        OATransaction tx = new OATransaction();
        List<String> calls = new ArrayList<>();
        RecordingListener l1 = new RecordingListener("one", calls);
        RecordingListener l2 = new RecordingListener("two", calls);
        tx.addTransactionListener(l1);
        tx.addTransactionListener(l2);
        tx.start();

        tx.commit();

        assertEquals(List.of("one.commit", "two.commit"), calls);
        assertFalse(tx.isStarted());
        assertNull(currentTransaction());
    }

    @Test
    void commitDoesNotExecuteOpenBatchesCurrentBehavior() {
        OATransaction tx = new OATransaction();
        List<String> calls = new ArrayList<>();
        tx.addTransactionListener(new RecordingListener("one", calls));
        tx.start();

        tx.commit();

        assertEquals(List.of("one.commit"), calls);
    }

    @Test
    void commitClearsThreadTransactionWhenListenerThrows() {
        OATransaction tx = new OATransaction();
        tx.addTransactionListener(new ThrowingListener("commit"));
        tx.start();

        RuntimeException ex = assertThrows(RuntimeException.class, tx::commit);

        assertEquals("commit", ex.getMessage());
        assertFalse(tx.isStarted());
        assertNull(currentTransaction());
    }

    @Test
    void addTransactionListenerIgnoresDuplicateListener() {
        OATransaction tx = new OATransaction();
        List<String> calls = new ArrayList<>();
        RecordingListener listener = new RecordingListener("same", calls);
        tx.addTransactionListener(listener);
        tx.addTransactionListener(listener);
        tx.start();

        tx.commit();

        assertEquals(List.of("same.commit"), calls);
    }

    @Test
    void addTransactionListenerAcceptsNullAndCommitFailsCurrentBehavior() {
        OATransaction tx = new OATransaction();
        tx.addTransactionListener(null);
        tx.start();

        assertThrows(NullPointerException.class, tx::commit);
        assertNull(currentTransaction());
    }

    @Test
    void removeTransactionListenerPreventsFutureCallbacks() {
        OATransaction tx = new OATransaction();
        List<String> calls = new ArrayList<>();
        RecordingListener listener = new RecordingListener("removed", calls);
        tx.addTransactionListener(listener);

        tx.removeTransactionListener(listener);
        tx.start();
        tx.commit();

        assertTrue(calls.isEmpty());
    }

    @Test
    void removeTransactionListenerIgnoresMissingAndNullListeners() {
        OATransaction tx = new OATransaction();

        assertDoesNotThrow(() -> tx.removeTransactionListener(new RecordingListener("x", new ArrayList<>())));
        assertDoesNotThrow(() -> tx.removeTransactionListener(null));
    }

    @Test
    void putStoresAndReplacesTransactionScopedValue() {
        OATransaction tx = new OATransaction();

        tx.put("key", "value1");
        assertEquals("value1", tx.get("key"));

        tx.put("key", "value2");
        assertEquals("value2", tx.get("key"));
    }

    @Test
    void getReturnsNullForMissingKeyAndSupportsNullKey() {
        OATransaction tx = new OATransaction();

        assertNull(tx.get("missing"));

        tx.put(null, "null-key");
        assertEquals("null-key", tx.get(null));
    }

    @Test
    void removeReturnsStoredValueAndClearsEntry() {
        OATransaction tx = new OATransaction();
        tx.put("key", "value");

        assertEquals("value", tx.remove("key"));
        assertNull(tx.get("key"));
        assertNull(tx.remove("key"));
    }

    @Test
    void executeOpenBatchesNotifiesListenersInOrder() {
        OATransaction tx = new OATransaction();
        List<String> calls = new ArrayList<>();
        tx.addTransactionListener(new RecordingListener("one", calls));
        tx.addTransactionListener(new RecordingListener("two", calls));

        tx.executeOpenBatches();

        assertEquals(List.of("one.batch", "two.batch"), calls);
    }

    @Test
    void executeOpenBatchesPropagatesListenerExceptionAndStopsCurrentIteration() {
        OATransaction tx = new OATransaction();
        List<String> calls = new ArrayList<>();
        tx.addTransactionListener(new ThrowingListener("batch"));
        tx.addTransactionListener(new RecordingListener("two", calls));

        RuntimeException ex = assertThrows(RuntimeException.class, tx::executeOpenBatches);

        assertEquals("batch", ex.getMessage());
        assertTrue(calls.isEmpty());
    }

    private static OATransaction currentTransaction() {
        OAThreadLocalService tls = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
        return tls.getTransaction();
    }

    private static void clearCurrentTransaction() {
        OAThreadLocalService tls = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
        if (tls.getTransaction() != null) {
            tls.setTransaction(null);
        }
    }

    private static class RecordingListener implements OATransactionListener {
        final String name;
        final List<String> calls;

        RecordingListener(String name, List<String> calls) {
            this.name = name;
            this.calls = calls;
        }

        @Override
        public void commit(OATransaction t) {
            calls.add(name + ".commit");
        }

        @Override
        public void rollback(OATransaction t) {
            calls.add(name + ".rollback");
        }

        @Override
        public void executeOpenBatches(OATransaction t) {
            calls.add(name + ".batch");
        }
    }

    private static class ThrowingListener implements OATransactionListener {
        final String method;

        ThrowingListener(String method) {
            this.method = method;
        }

        @Override
        public void commit(OATransaction t) {
            if ("commit".equals(method)) throw new RuntimeException(method);
        }

        @Override
        public void rollback(OATransaction t) {
            if ("rollback".equals(method)) throw new RuntimeException(method);
        }

        @Override
        public void executeOpenBatches(OATransaction t) {
            if ("batch".equals(method)) throw new RuntimeException(method);
        }
    }
}
