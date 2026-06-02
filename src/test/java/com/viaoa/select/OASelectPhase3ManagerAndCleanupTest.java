package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OASelectPhase3ManagerAndCleanupTest {

    public static class Item extends OAObject {
    }

    @AfterEach
    void cleanup() throws Exception {
        managerMap().clear();
        OASelectManager.setTimeLimit(300);
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentHashMap<Integer, WeakReference<OASelect>> managerMap() throws Exception {
        Field f = OASelectManager.class.getDeclaredField("hmSelect");
        f.setAccessible(true);
        return (ConcurrentHashMap<Integer, WeakReference<OASelect>>) f.get(null);
    }

    static class TestSelect extends OASelect<Item> {
        TestSelect() {
            super(Item.class);
        }

        void markStartedWithLastRead(long t) {
            bHasBeenStarted = true;
            lastReadTime = t;
        }
    }

    @Test
    void addTracksSelectByWeakReferenceAndRemoveDeletesIt() throws Exception {
        TestSelect sel = new TestSelect();

        OASelectManager.add(sel);

        Map<Integer, WeakReference<OASelect>> map = managerMap();
        assertTrue(map.containsKey(sel.getId()));
        assertSame(sel, map.get(sel.getId()).get());

        OASelectManager.remove(sel);

        assertFalse(map.containsKey(sel.getId()));
    }

    @Test
    void performCleanupRemovesCancelledSelect() throws Exception {
        TestSelect sel = new TestSelect();
        OASelectManager.add(sel);

        sel.cancel();

        OASelectManager.performCleanup();

        assertFalse(managerMap().containsKey(sel.getId()));
    }

    @Test
    void performCleanupSkipsNeverStartedSelect() throws Exception {
        TestSelect sel = new TestSelect();
        OASelectManager.add(sel);

        OASelectManager.setTimeLimit(1);
        OASelectManager.performCleanup();

        assertTrue(managerMap().containsKey(sel.getId()));
        assertFalse(sel.isCancelled());
    }

    @Test
    void performCleanupCancelsExpiredStartedSelect() throws Exception {
        TestSelect sel = new TestSelect();
        sel.markStartedWithLastRead(System.currentTimeMillis() - 10_000L);

        OASelectManager.setTimeLimit(1);
        OASelectManager.add(sel);

        OASelectManager.performCleanup();

        assertTrue(sel.isCancelled());
        assertFalse(managerMap().containsKey(sel.getId()));
    }

    @Test
    void performCleanupDoesNotCancelFreshStartedSelect() throws Exception {
        TestSelect sel = new TestSelect();
        sel.markStartedWithLastRead(System.currentTimeMillis());

        OASelectManager.setTimeLimit(60);
        OASelectManager.add(sel);

        OASelectManager.performCleanup();

        assertFalse(sel.isCancelled());
        assertTrue(managerMap().containsKey(sel.getId()));
    }

    @Test
    void performCleanupRemovesClearedWeakReference() throws Exception {
        TestSelect sel = new TestSelect();
        int id = sel.getId();

        managerMap().put(id, new WeakReference<OASelect>(null));

        OASelectManager.performCleanup();

        assertFalse(managerMap().containsKey(id));
    }

    @Test
    void addNullDoesNotStartOrAddEntry() throws Exception {
        int size = managerMap().size();

        OASelectManager.add(null);

        assertEquals(size, managerMap().size());
    }
}
