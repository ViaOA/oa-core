package com.viaoa.trigger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Store;
import com.viaoa.hub.HubEvent;

class OATriggerListenerTest {

    @Test
    void onTriggerReceivesRootHubEventAndPropertyPath() throws Exception {
        Store store = new Store();
        HubEvent<Store> event = new HubEvent<>(store, Store.P_Name, "old", "new");
        AtomicReference<Store> rootRef = new AtomicReference<>();
        AtomicReference<HubEvent> eventRef = new AtomicReference<>();
        AtomicReference<String> pathRef = new AtomicReference<>();
        OATriggerListener<Store> listener = (objRoot, hubEvent, propertyPathFromRoot) -> {
            rootRef.set(objRoot);
            eventRef.set(hubEvent);
            pathRef.set(propertyPathFromRoot);
        };

        listener.onTrigger(store, event, Store.P_Name);

        assertSame(store, rootRef.get());
        assertSame(event, eventRef.get());
        assertEquals(Store.P_Name, pathRef.get());
    }

    @Test
    void onTriggerCanRepresentNullRootFallback() throws Exception {
        Store store = new Store();
        HubEvent<Store> event = new HubEvent<>(store, Store.P_Name, "old", "new");
        AtomicReference<Store> rootRef = new AtomicReference<>(store);
        OATriggerListener<Store> listener = (objRoot, hubEvent, propertyPathFromRoot) -> rootRef.set(objRoot);

        listener.onTrigger(null, event, Store.P_Registers);

        assertNull(rootRef.get());
    }

    @Test
    void onTriggerExceptionIsObservableToCaller() {
        OATriggerListener<Store> listener = (objRoot, hubEvent, propertyPathFromRoot) -> {
            throw new IllegalStateException("boom");
        };

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listener.onTrigger(new Store(), new HubEvent<>(new Store()), Store.P_Name));
        assertEquals("boom", ex.getMessage());
    }
}
