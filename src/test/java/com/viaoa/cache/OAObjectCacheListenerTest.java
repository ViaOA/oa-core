package com.viaoa.cache;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.viaoa.hub.Hub;

class OAObjectCacheListenerTest {

    private static class RecordingListener implements OAObjectCacheListener<Item> {
        int propertyChangeCount;
        int addCount;
        int hubAddCount;
        int hubRemoveCount;
        int loadCount;
        Item lastObject;

        @Override
        public void afterPropertyChange(Item obj, String propertyName, Object oldValue, Object newValue) {
            propertyChangeCount++;
            lastObject = obj;
        }

        @Override
        public void afterAdd(Item obj) {
            addCount++;
            lastObject = obj;
        }

        @Override
        public void afterAdd(Hub<Item> hub, Item obj) {
            hubAddCount++;
            lastObject = obj;
        }

        @Override
        public void afterRemove(Hub<Item> hub, Item obj) {
            hubRemoveCount++;
            lastObject = obj;
        }

        @Override
        public void afterLoad(Item obj) {
            loadCount++;
            lastObject = obj;
        }
    }

    @Test
    void listenerCallbacksCanBeImplementedIndependently() {
        RecordingListener listener = new RecordingListener();
        Hub<Item> hub = new Hub<>(Item.class);
        Item item = new Item(1);

        listener.afterPropertyChange(item, Item.P_Name, "old", "new");
        listener.afterAdd(item);
        listener.afterAdd(hub, item);
        listener.afterRemove(hub, item);
        listener.afterLoad(item);

        assertEquals(1, listener.propertyChangeCount);
        assertEquals(1, listener.addCount);
        assertEquals(1, listener.hubAddCount);
        assertEquals(1, listener.hubRemoveCount);
        assertEquals(1, listener.loadCount);
        assertSame(item, listener.lastObject);
    }
}
