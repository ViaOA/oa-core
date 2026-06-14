package com.viaoa.datasource;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Store;
import com.viaoa.object.OAObject;

class OASaveDeleteListenerTest {

    @Test
    void onInsertCanBeImplementedForOAObjects() {
        RecordingListener listener = new RecordingListener();
        Store store = new Store(1);

        listener.onInsert(store);

        assertSame(store, listener.inserted);
    }

    @Test
    void onUpdateCanBeImplementedForOAObjects() {
        RecordingListener listener = new RecordingListener();
        Store store = new Store(2);

        listener.onUpdate(store);

        assertSame(store, listener.updated);
    }

    @Test
    void onDeleteCanBeImplementedForOAObjects() {
        RecordingListener listener = new RecordingListener();
        Store store = new Store(3);

        listener.onDelete(store);

        assertSame(store, listener.deleted);
    }

    private static class RecordingListener implements OASaveDeleteListener {
        OAObject inserted;
        OAObject updated;
        OAObject deleted;

        @Override
        public void onInsert(OAObject obj) {
            inserted = obj;
        }

        @Override
        public void onUpdate(OAObject obj) {
            updated = obj;
        }

        @Override
        public void onDelete(OAObject obj) {
            deleted = obj;
        }
    }
}
