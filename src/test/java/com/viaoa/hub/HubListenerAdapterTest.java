package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.hub.HubListener.InsertLocation;

class HubListenerAdapterTest {
    @Test
    void constructorsExposeListenerMetadata() {
        Object listener = new Object();

        HubListenerAdapter<Register> empty = new HubListenerAdapter<>();
        assertNull(empty.getListener());
        assertNull(empty.getName());
        assertNull(empty.getDescription());

        HubListenerAdapter<Register> named = new HubListenerAdapter<>(listener, "name", "description");
        assertSame(listener, named.getListener());
        assertEquals("name", named.getName());
        assertEquals("description", named.getDescription());

        assertNull(named.getLocation());
        named.setLocation(InsertLocation.FIRST);
        assertEquals(InsertLocation.FIRST, named.getLocation());
    }

    @Test
    void callbackMethodsAreNoOpsByDefault() {
        HubListenerAdapter<Register> adapter = new HubListenerAdapter<>();
        HubEvent<Register> event = new HubEvent<>(new Hub<>(Register.class));

        assertDoesNotThrow(() -> {
            adapter.afterChangeActiveObject(event);
            adapter.beforePropertyChange(event);
            adapter.afterPropertyChange(event);
            adapter.beforeInsert(event);
            adapter.afterInsert(event);
            adapter.beforeMove(event);
            adapter.afterMove(event);
            adapter.beforeAdd(event);
            adapter.afterAdd(event);
            adapter.beforeRemove(event);
            adapter.afterRemove(event);
            adapter.beforeRemoveAll(event);
            adapter.afterRemoveAll(event);
            adapter.beforeSave(event);
            adapter.afterSave(event);
            adapter.beforeDelete(event);
            adapter.afterDelete(event);
            adapter.beforeSelect(event);
            adapter.afterSort(event);
            adapter.onNewList(event);
            adapter.afterNewList(event);
            adapter.afterLoad(event);
            adapter.beforeRefresh(event);
        });
    }
}
