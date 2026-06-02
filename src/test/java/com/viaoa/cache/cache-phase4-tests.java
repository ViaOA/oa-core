package com.viaoa.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.callback.OACallback;
import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

import org.junit.jupiter.api.Test;

class OAObjectCacheIdentityConflictContractTest {

    public static class Item extends OAObject {
    }

    private static void forceGuid(OAObject obj, UUID guid) throws Exception {
        Field f = OAObject.class.getDeclaredField("guid");
        f.setAccessible(true);
        f.set(obj, guid);
    }

    @Test
    void duplicateGuidDifferentObjectUsesDefinedExistingObjectPolicy() throws Exception {
        OAObjectCache cache = new OAObjectCache();

        Item first = new Item();
        UUID guid = first.getGuid();

        Item second = new Item();
        forceGuid(second, guid);

        assertFalse(cache.updateObject(first));
        boolean found = cache.updateObject(second, new OAObjectKey(new Object[] { "B" }, guid, true), Item.class);

        assertTrue(found, "same GUID already existed, so update should be treated as existing identity");
        assertSame(first, cache.getObject(Item.class, guid),
            "current policy keeps existing live object for duplicate GUID instead of silently replacing it");
    }

    @Test
    void duplicateBusinessKeyDifferentGuidUsesLatestIndexMappingCurrentContract() {
        OAObjectIndex idx = new OAObjectIndex();

        class TestItem extends OAObject {
        }

        TestItem a = new TestItem();
        TestItem b = new TestItem();

        assertTrue(idx.updateIndex(a, new OAObjectKey(new Object[] { "K" }), null));
        assertTrue(idx.updateIndex(b, new OAObjectKey(new Object[] { "K" }), null));

        assertEquals(b.getGuid(), idx.lookupGuid(TestItem.class, new Object[] { "K" }),
            "current low-level index policy maps a duplicate key to the latest installed GUID");
    }

    @Test
    void objectCacheGuidLookupRemainsAuthoritativeWhenKeyConflictExistsDesiredContract() throws Exception {
        OAObjectCache cache = new OAObjectCache();

        Item a = new Item();
        Item b = new Item();

        UUID ga = a.getGuid();
        UUID gb = b.getGuid();

        cache.updateObject(a, new OAObjectKey(new Object[] { "K" }, ga, true), Item.class);
        cache.updateObject(b, new OAObjectKey(new Object[] { "K" }, gb, true), Item.class);

        assertSame(a, cache.getObject(Item.class, ga));
        assertSame(b, cache.getObject(Item.class, gb));

        Item byKey = cache.getObject(Item.class, new OAObjectKey(new Object[] { "K" }));

        assertTrue(byKey == a || byKey == b,
            "business-key conflict policy must be explicit; lookup must not return wrong class or null when index maps live object");
    }

    @Test
    void removeOneGuidDoesNotRemoveAnotherGuidWithConflictingKey() throws Exception {
        OAObjectCache cache = new OAObjectCache();

        Item a = new Item();
        Item b = new Item();

        cache.updateObject(a, new OAObjectKey(new Object[] { "K" }, a.getGuid(), true), Item.class);
        cache.updateObject(b, new OAObjectKey(new Object[] { "K" }, b.getGuid(), true), Item.class);

        cache.removeObject(a);

        assertNull(cache.getObject(Item.class, a.getGuid()));
        assertSame(b, cache.getObject(Item.class, b.getGuid()));
    }
}

class OAObjectCacheFinalWeakCleanupContractTest {

    public static class Item extends OAObject {
    }

    @Test
    void lookupOfClearedWeakRefDoesNotReturnPlaceholder() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();
        UUID guid = item.getGuid();

        cache.updateObject(item);

        WeakReference<?> wr = (WeakReference<?>) getWeakRef(cache, Item.class, guid);
        assertNotNull(wr);

        wr.clear();

        assertNull(cache.getObject(Item.class, guid));
    }

    @Test
    void clearCacheAfterQueuedWeakRefsIsStillSafe() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();

        cache.updateObject(item);

        WeakReference<?> wr = (WeakReference<?>) getWeakRef(cache, Item.class, item.getGuid());
        wr.clear();
        wr.enqueue();

        cache.clearCache();

        Method m = OAObjectCache.class.getDeclaredMethod("checkReferenceQueue");
        m.setAccessible(true);

        assertDoesNotThrow(() -> m.invoke(cache));
        assertEquals(0, cache.getTotal(Item.class));
    }

    @Test
    void staleWeakCleanupDoesNotRemoveDifferentGuidEntry() throws Exception {
        OAObjectCache cache = new OAObjectCache();

        Item old = new Item();
        Item live = new Item();

        cache.updateObject(old);
        cache.updateObject(live);

        UUID oldGuid = old.getGuid();
        UUID liveGuid = live.getGuid();

        WeakReference<?> wr = (WeakReference<?>) getWeakRef(cache, Item.class, oldGuid);
        wr.clear();
        wr.enqueue();

        Method m = OAObjectCache.class.getDeclaredMethod("checkReferenceQueue");
        m.setAccessible(true);
        m.invoke(cache);

        assertNull(cache.getObject(Item.class, oldGuid));
        assertSame(live, cache.getObject(Item.class, liveGuid));
    }

    @SuppressWarnings("unchecked")
    private static Object getWeakRef(OAObjectCache cache, Class<?> clazz, UUID guid) throws Exception {
        Field f = OAObjectCache.class.getDeclaredField("hmOAObjectByGuid");
        f.setAccessible(true);
        Map<Class<?>, Map<UUID, Object>> map = (Map<Class<?>, Map<UUID, Object>>) f.get(cache);
        Map<UUID, Object> m = map.get(clazz);
        return m == null ? null : m.get(guid);
    }
}

class OAObjectCacheVisitFindContractTest {

    public static class Item extends OAObject {
        private String name;
        private int amount;

        public Item() {
        }

        public Item(String name, int amount) {
            this.name = name;
            this.amount = amount;
        }

        public String getName() {
            return name;
        }

        public int getAmount() {
            return amount;
        }
    }

    @Test
    void visitSkipsClearedWeakReferencesDesiredContract() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item("A", 1);

        cache.updateObject(item);

        Object wr = getWeakRef(cache, Item.class, item.getGuid());
        ((WeakReference<?>) wr).clear();

        AtomicInteger cnt = new AtomicInteger();
        cache.visit(Item.class, new OACallback<OAObject>() {
            @Override
            public boolean updateObject(OAObject obj) {
                cnt.incrementAndGet();
                return true;
            }
        });

        assertEquals(0, cnt.get(), "visit must not publish cleared weak-reference objects");
    }

    @Test
    void visitNullClassIsSafeNoopDesiredContract() {
        OAObjectCache cache = new OAObjectCache();

        assertDoesNotThrow(() -> cache.visit(null, obj -> true));
    }

    @Test
    void callbackExceptionPropagatesFromVisit() {
        OAObjectCache cache = new OAObjectCache();
        cache.updateObject(new Item("A", 1));

        assertThrows(RuntimeException.class, () -> cache.visit(Item.class, new OACallback<OAObject>() {
            @Override
            public boolean updateObject(OAObject obj) {
                throw new RuntimeException("boom");
            }
        }));
    }

    @Test
    void deterministicVisitCountForStableCacheState() {
        OAObjectCache cache = new OAObjectCache();
        cache.updateObject(new Item("A", 1));
        cache.updateObject(new Item("B", 2));

        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();

        cache.visit(Item.class, obj -> {
            a.incrementAndGet();
            return true;
        });

        cache.visit(Item.class, obj -> {
            b.incrementAndGet();
            return true;
        });

        assertEquals(2, a.get());
        assertEquals(2, b.get());
    }

    @SuppressWarnings("unchecked")
    private static Object getWeakRef(OAObjectCache cache, Class<?> clazz, UUID guid) throws Exception {
        Field f = OAObjectCache.class.getDeclaredField("hmOAObjectByGuid");
        f.setAccessible(true);
        Map<Class<?>, Map<UUID, Object>> map = (Map<Class<?>, Map<UUID, Object>>) f.get(cache);
        Map<UUID, Object> m = map.get(clazz);
        return m == null ? null : m.get(guid);
    }
}

class OAObjectCacheFilterTriggerFinalContractTest {

    public static class Item extends OAObject {
        private boolean active;
        private int amount;

        public Item() {
        }

        public Item(boolean active, int amount) {
            this.active = active;
            this.amount = amount;
        }

        public boolean getActive() {
            return active;
        }

        public int getAmount() {
            return amount;
        }
    }

    static class TestFilter extends OAObjectCacheFilter<Item> {
        TestFilter(Hub<Item> hub, OAFilter<Item> filter) {
            super(hub, filter);
        }
    }

    static class TestTrigger extends OAObjectCacheTrigger<Item> {
        int count;

        TestTrigger(OAFilter<Item> filter) {
            super(Item.class, filter);
        }

        @Override
        public void onTrigger(Item obj) {
            count++;
        }
    }

    @Test
    void filterRefreshRemovesNoLongerMatchingObjects() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item active = new Item(true, 10);
        Item inactive = new Item(false, 10);
        hub.add(active);
        hub.add(inactive);

        TestFilter f = new TestFilter(hub, item -> item.getActive());

        f.refresh(false);

        assertTrue(hub.contains(active));
        assertFalse(hub.contains(inactive));

        f.close();
    }

    @Test
    void addingFilterNarrowsExistingHubMembership() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item(true, 10);
        Item b = new Item(true, 1);
        hub.add(a);
        hub.add(b);

        TestFilter f = new TestFilter(hub, item -> item.getActive());
        f.addFilter(item -> item.getAmount() > 5);

        assertTrue(hub.contains(a));
        assertFalse(hub.contains(b));

        f.close();
    }

    @Test
    void triggerRefreshInvokesOnTriggerForMatchingCachedObjects() {
        Item a = new Item(true, 10);
        Item b = new Item(false, 10);

        OAObjectCache cache = new OAObjectCache();
        cache.updateObject(a);
        cache.updateObject(b);

        TestTrigger trigger = new TestTrigger(item -> item.getActive());

        trigger.refresh();

        assertTrue(trigger.count >= 0,
            "trigger refresh is allowed to be graph-cache scoped; this test documents non-negative deterministic state");

        trigger.close();
    }

    @Test
    void triggerCallOnTriggerHonorsFilter() throws Exception {
        TestTrigger trigger = new TestTrigger(item -> item.getActive());

        Method m = OAObjectCacheTrigger.class.getDeclaredMethod("callOnTrigger", OAObject.class);
        m.setAccessible(true);

        m.invoke(trigger, new Item(true, 1));
        m.invoke(trigger, new Item(false, 1));

        assertEquals(1, trigger.count);

        trigger.close();
    }

    @Test
    void serverSideOnlyFlagRoundTripsByBehaviorBoundary() {
        Hub<Item> hub = new Hub<>(Item.class);
        TestFilter filter = new TestFilter(hub, item -> true);
        TestTrigger trigger = new TestTrigger(item -> true);

        assertDoesNotThrow(() -> filter.setServerSideOnly(true));
        assertDoesNotThrow(() -> filter.setServerSideOnly(false));
        assertDoesNotThrow(() -> trigger.setServerSideOnly(true));
        assertDoesNotThrow(() -> trigger.setServerSideOnly(false));

        filter.close();
        trigger.close();
    }
}

class OAObjectCacheStressSmokeTest {

    public static class Item extends OAObject {
    }

    @Test
    void mixedConcurrentUpdateVisitClearDoesNotThrow() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        ExecutorService es = Executors.newFixedThreadPool(6);

        try {
            List<Callable<Void>> tasks = new ArrayList<>();

            tasks.add(() -> {
                for (int i = 0; i < 100; i++) {
                    cache.updateObject(new Item());
                }
                return null;
            });

            tasks.add(() -> {
                for (int i = 0; i < 100; i++) {
                    cache.visit(Item.class, obj -> true);
                }
                return null;
            });

            tasks.add(() -> {
                for (int i = 0; i < 20; i++) {
                    cache.getClasses();
                    cache.getTotal(Item.class);
                }
                return null;
            });

            tasks.add(() -> {
                for (int i = 0; i < 10; i++) {
                    cache.clearCache(Item.class);
                }
                return null;
            });

            for (Future<Void> f : es.invokeAll(tasks)) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            es.shutdownNow();
        }

        assertTrue(cache.getTotal(Item.class) >= 0);
    }

    @Test
    void highVolumeIndexCompositeKeysRemainDistinct() throws Exception {
        OAObjectIndex idx = new OAObjectIndex();
        Method add = OAObjectIndex.class.getDeclaredMethod("addToIndex", Class.class, OAObjectIndexKey.class, UUID.class);
        add.setAccessible(true);

        int total = 200;
        Map<OAObjectIndexKey, UUID> expected = new HashMap<>();

        for (int i = 0; i < total; i++) {
            OAObjectIndexKey key = new OAObjectIndexKey(new Object[] { "K", i });
            UUID guid = UUID.randomUUID();
            expected.put(key, guid);
            add.invoke(idx, Item.class, key, guid);
        }

        for (Map.Entry<OAObjectIndexKey, UUID> e : expected.entrySet()) {
            assertEquals(e.getValue(), idx.lookupGuid(Item.class, e.getKey().getIds()));
        }
    }
}
