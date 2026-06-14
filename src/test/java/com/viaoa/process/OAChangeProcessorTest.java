package com.viaoa.process;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Store;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;

class OAChangeProcessorTest {

    @Test
    void constructorWithoutThreadPoolCreatesProcessor() {
        TestChangeProcessor processor = new TestChangeProcessor(false);

        assertEquals(0, processor.processCount);
    }

    @Test
    void addListenerNullHubIsNoOp() {
        TestChangeProcessor processor = new TestChangeProcessor(false);

        assertDoesNotThrow(() -> processor.addListener(null, Store.P_Name));
        assertDoesNotThrow(() -> processor.addListener(null, (String) Store.P_Name));
        assertEquals(0, processor.processCount);
    }

    @Test
    void simplePropertyListenerCurrentImplementationDoesNotRegisterWithHub() {
        Hub<Store> hub = new Hub<>(Store.class);
        Store store = new Store();
        hub.add(store);
        TestChangeProcessor processor = new TestChangeProcessor(false);

        processor.addListener(hub, (String) Store.P_Name);
        store.setName("Main");

        assertEquals(0, processor.processCount);
    }

    private static class TestChangeProcessor extends OAChangeProcessor {
        volatile int processCount;
        final AtomicReference<HubEvent> lastEvent = new AtomicReference<>();

        TestChangeProcessor(boolean useThreadPool) {
            super(useThreadPool);
        }

        @Override
        protected void process(HubEvent evt) {
            processCount++;
            lastEvent.set(evt);
        }
    }
}
