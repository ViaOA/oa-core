package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.Register;
import com.viaoa.oa.OA;
import com.viaoa.runtime.OARuntime;

class OASelectManagerTest {

    @BeforeEach
    void beforeEach() {
        OA oa = OARuntime.createDefaultOA(Register.class);
        trackedSelects().clear();
        OASelectManager.setTimeLimit(300);
    }
    @AfterEach
    void afterEach() {
        OARuntime.oa(Register.class).close();
    }
   
    
    @Test
    void setTimeLimitRejectsNonPositiveAndAcceptsPositive() {
        assertThrows(IllegalArgumentException.class, () -> OASelectManager.setTimeLimit(0));
        assertThrows(IllegalArgumentException.class, () -> OASelectManager.setTimeLimit(-1));

        assertDoesNotThrow(() -> OASelectManager.setTimeLimit(1));
    }

    @Test
    void addIgnoresNullAndRemoveDeletesTrackedSelect() {
        Map<Integer, WeakReference<OASelect>> map = trackedSelects();
        assertTrue(map.isEmpty());

        OASelectManager.add(null);
        assertTrue(map.isEmpty());

        OASelect<Item> select = new OASelect<>(Item.class);
        OASelectManager.add(select);
        assertTrue(map.containsKey(select.getId()));

        OASelectManager.remove(select);
        assertFalse(map.containsKey(select.getId()));
    }

    @Test
    void removeNullUsesCurrentStrictContract() {
        assertThrows(NullPointerException.class, () -> OASelectManager.remove(null));
    }

    @Test
    void performCleanupRemovesCancelledSelectsAndClearedReferences() {
        Map<Integer, WeakReference<OASelect>> map = trackedSelects();
        OASelect<Item> cancelled = new OASelect<>(Item.class);
        OASelect<Item> active = new OASelect<>(Item.class);

        OASelectManager.add(cancelled);
        OASelectManager.add(active);
        map.put(999_999, new WeakReference<>(null));

        cancelled.cancel();
        OASelectManager.performCleanup();

        assertFalse(map.containsKey(cancelled.getId()));
        assertFalse(map.containsKey(999_999));
        assertTrue(map.containsKey(active.getId()));

        OASelectManager.remove(active);
    }

    @Test
    void performCleanupCancelsStartedSelectsPastTimeLimit() {
        Map<Integer, WeakReference<OASelect>> map = trackedSelects();
        ManagedSelect select = new ManagedSelect();
        select.markStartedAndIdle(System.currentTimeMillis() - 10_000L);

        OASelectManager.setTimeLimit(1);
        OASelectManager.add(select);
        OASelectManager.performCleanup();

        assertTrue(select.isCancelled());
        assertFalse(map.containsKey(select.getId()));
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentHashMap<Integer, WeakReference<OASelect>> trackedSelects() {
        try {
            Field field = OASelectManager.class.getDeclaredField("hmSelect");
            field.setAccessible(true);
            return (ConcurrentHashMap<Integer, WeakReference<OASelect>>) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static class ManagedSelect extends OASelect<Item> {
        ManagedSelect() {
            super(Item.class);
        }

        void markStartedAndIdle(long time) {
            this.bHasBeenStarted = true;
            this.lastReadTime = time;
        }

        @Override
        public synchronized boolean hasMore() {
            return true;
        }
    }
}
