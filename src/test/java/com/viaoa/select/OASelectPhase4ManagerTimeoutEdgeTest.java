package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OASelectPhase4ManagerTimeoutEdgeTest {

    public static class Item extends OAObject {
    }

    @AfterEach
    void resetManager() throws Exception {
        map().clear();
        OASelectManager.setTimeLimit(300);
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentHashMap<Integer, WeakReference<OASelect>> map() throws Exception {
        Field f = OASelectManager.class.getDeclaredField("hmSelect");
        f.setAccessible(true);
        return (ConcurrentHashMap<Integer, WeakReference<OASelect>>) f.get(null);
    }

    static class StartedSelect extends OASelect<Item> {
        StartedSelect(long lastRead) {
            super(Item.class);
            bHasBeenStarted = true;
            lastReadTime = lastRead;
        }
    }

    @Test
    void startedSelectWithZeroLastReadIsNotTimedOut() throws Exception {
        StartedSelect sel = new StartedSelect(0);
        OASelectManager.setTimeLimit(1);
        OASelectManager.add(sel);

        OASelectManager.performCleanup();

        assertFalse(sel.isCancelled());
        assertTrue(map().containsKey(sel.getId()));
    }

    @Test
    void expiredSelectIsCancelledAndRemovedOnlyAfterStartedAndLastReadSet() throws Exception {
        StartedSelect sel = new StartedSelect(System.currentTimeMillis() - 5000);
        OASelectManager.setTimeLimit(1);
        OASelectManager.add(sel);

        OASelectManager.performCleanup();

        assertTrue(sel.isCancelled());
        assertFalse(map().containsKey(sel.getId()));
    }

    @Test
    void cleanupToleratesNullWeakReferenceValue() throws Exception {
        map().put(123456, null);

        assertDoesNotThrow(OASelectManager::performCleanup);

        assertTrue(map().containsKey(123456),
            "current implementation skips null WeakReference entries; this documents the boundary");
    }

    @Test
    void removeNullSelectFailsFastCurrentContract() {
        assertThrows(NullPointerException.class, () -> OASelectManager.remove(null));
    }

    @Test
    void setTimeLimitAcceptsPositiveOneSecond() {
        assertDoesNotThrow(() -> OASelectManager.setTimeLimit(1));
    }
}
