package com.viaoa.process;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Store;
import com.viaoa.hub.Hub;

class OAChangeRefresherTest {

    @Test
    void constructorsSetInitialChangedState() {
        TestChangeRefresher normal = new TestChangeRefresher();
        TestChangeRefresher initialized = new TestChangeRefresher(true);

        assertFalse(normal.hasChanged());
        assertFalse(normal.isChanged());
        assertTrue(initialized.hasChanged());
        assertTrue(initialized.isChanged());
    }

    @Test
    void refreshMarksChangedBeforeProcessing() {
        TestChangeRefresher refresher = new TestChangeRefresher();

        refresher.refresh();

        assertTrue(refresher.hasChanged());
        assertTrue(refresher.isChanged());
    }

    @Test
    void simplePropertyListenerMarksChanged() {
        Hub<Store> hub = new Hub<>(Store.class);
        Store store = new Store();
        hub.add(store);
        TestChangeRefresher refresher = new TestChangeRefresher();

        refresher.addListener(hub, Store.P_Name);
        store.setName("Main");

        assertTrue(refresher.hasChanged());
    }

    @Test
    void varargsPropertyListenerMarksChanged() {
        Hub<Store> hub = new Hub<>(Store.class);
        Store store = new Store();
        hub.add(store);
        TestChangeRefresher refresher = new TestChangeRefresher();

        refresher.addListener(hub, Store.P_Name);
        store.setName("Main");

        assertTrue(refresher.hasChanged());
    }

    @Test
    void hubLevelListenerMarksChangedForCollectionChanges() {
        Hub<Store> hub = new Hub<>(Store.class);
        TestChangeRefresher refresher = new TestChangeRefresher();

        refresher.addListener(hub, (String) null);
        hub.add(new Store());

        assertTrue(refresher.hasChanged());
    }

    @Test
    void addListenerNullHubIsNoOp() {
        TestChangeRefresher refresher = new TestChangeRefresher();

        assertDoesNotThrow(() -> refresher.addListener(null, Store.P_Name));
        assertDoesNotThrow(() -> refresher.addListener(null, (String) Store.P_Name));
        assertFalse(refresher.hasChanged());
    }

    @Test
    void startProcessesRefreshAndStopSignalsThread() throws Exception {
        TestChangeRefresher refresher = new TestChangeRefresher();
        refresher.latch = new CountDownLatch(1);
        try {
            refresher.start();
            assertNotNull(refresher.getThread());
            assertTrue(refresher.getThread().isDaemon());

            refresher.refresh();

            assertTrue(refresher.latch.await(2, TimeUnit.SECONDS));
            assertEquals(1, refresher.processCount);
            assertFalse(refresher.hasChanged());
        }
        finally {
            refresher.stop();
        }
    }

    private static class TestChangeRefresher extends OAChangeRefresher {
        volatile int processCount;
        CountDownLatch latch;

        TestChangeRefresher() {
            super();
        }

        TestChangeRefresher(boolean initialize) {
            super(initialize);
        }

        @Override
        protected void process() {
            processCount++;
            if (latch != null) latch.countDown();
        }
    }
}
