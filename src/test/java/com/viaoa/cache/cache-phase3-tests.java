package com.viaoa.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

import org.junit.jupiter.api.Test;

class OAObjectIndexUpdateIndexContractTest {

    public static class Item extends OAObject {
    }

    private static boolean add(OAObjectIndex idx, Class<?> c, Object[] ids, UUID guid) throws Exception {
        Method m = OAObjectIndex.class.getDeclaredMethod("addToIndex", Class.class, OAObjectIndexKey.class, UUID.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(idx, c, new OAObjectIndexKey(ids), guid);
    }

    @Test
    void updateIndexRemovesOldKeyAndAddsNewKey() {
        OAObjectIndex idx = new OAObjectIndex();
        Item item = new Item();
        UUID guid = item.getGuid();

        assertDoesNotThrow(() -> add(idx, Item.class, new Object[] { "old" }, guid));

        idx.updateIndex(item, new OAObjectKey(new Object[] { "new" }), new OAObjectKey(new Object[] { "old" }));

        assertNull(idx.lookupGuid(Item.class, new Object[] { "old" }));
        assertEquals(guid, idx.lookupGuid(Item.class, new Object[] { "new" }));
    }

    @Test
    void updateIndexSameKeyIsNoop() {
        OAObjectIndex idx = new OAObjectIndex();
        Item item = new Item();
        UUID guid = item.getGuid();

        assertDoesNotThrow(() -> add(idx, Item.class, new Object[] { "same" }, guid));

        idx.updateIndex(item, new OAObjectKey(new Object[] { "same" }), new OAObjectKey(new Object[] { "same" }));

        assertEquals(guid, idx.lookupGuid(Item.class, new Object[] { "same" }));
    }

    @Test
    void updateIndexNullNewKeyOnlyRemovesOld() {
        OAObjectIndex idx = new OAObjectIndex();
        Item item = new Item();
        UUID guid = item.getGuid();

        assertDoesNotThrow(() -> add(idx, Item.class, new Object[] { "old" }, guid));

        idx.updateIndex(item, null, new OAObjectKey(new Object[] { "old" }));

        assertNull(idx.lookupGuid(Item.class, new Object[] { "old" }));
    }

    @Test
    void updateIndexNullOldKeyOnlyAddsNew() {
        OAObjectIndex idx = new OAObjectIndex();
        Item item = new Item();

        idx.updateIndex(item, new OAObjectKey(new Object[] { "new" }), null);

        assertEquals(item.getGuid(), idx.lookupGuid(Item.class, new Object[] { "new" }));
    }

    @Test
    void updateIndexRejectsInvalidNewKeyByNotAddingAuthoritativeEntry() {
        OAObjectIndex idx = new OAObjectIndex();
        Item item = new Item();

        idx.updateIndex(item, new OAObjectKey(new Object[] { null }), null);

        assertNull(idx.lookupGuid(Item.class, new Object[] { null }));
    }
}

class OAObjectCacheConcurrentContractTest {

    public static class Item extends OAObject {
    }

    @Test
    void concurrentAddLookupSameObjectsDoesNotCorruptCache() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        int total = 200;
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < total; i++) items.add(new Item());

        ExecutorService es = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (Item item : items) {
                tasks.add(() -> {
                    cache.updateObject(item);
                    assertSame(item, cache.getObject(Item.class, item.getGuid()));
                    return null;
                });
            }

            for (Future<Void> f : es.invokeAll(tasks)) {
                f.get(5, TimeUnit.SECONDS);
            }
        } finally {
            es.shutdownNow();
        }

        assertEquals(total, cache.getTotal(Item.class));
    }

    @Test
    void concurrentDuplicateUpdateSameObjectKeepsSingleEntry() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();

        ExecutorService es = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                tasks.add(() -> cache.updateObject(item));
            }

            for (Future<Boolean> f : es.invokeAll(tasks)) {
                f.get(5, TimeUnit.SECONDS);
            }
        } finally {
            es.shutdownNow();
        }

        assertEquals(1, cache.getTotal(Item.class));
        assertSame(item, cache.getObject(Item.class, item.getGuid()));
    }

    @Test
    void concurrentRemoveAndLookupDoesNotThrow() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Item item = new Item();
            items.add(item);
            cache.updateObject(item);
        }

        ExecutorService es = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (Item item : items) {
                tasks.add(() -> {
                    cache.getObject(Item.class, item.getGuid());
                    cache.removeObject(item);
                    cache.getObject(Item.class, item.getGuid());
                    return null;
                });
            }

            for (Future<Void> f : es.invokeAll(tasks)) {
                f.get(5, TimeUnit.SECONDS);
            }
        } finally {
            es.shutdownNow();
        }

        assertTrue(cache.getTotal(Item.class) >= 0);
    }

    @Test
    void concurrentVisitDuringMutationDoesNotThrowConcurrentModification() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        for (int i = 0; i < 50; i++) cache.updateObject(new Item());

        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            Future<?> visitor = es.submit(() -> {
                for (int i = 0; i < 20; i++) {
                    cache.visit(Item.class, obj -> true);
                }
            });
            Future<?> mutator = es.submit(() -> {
                for (int i = 0; i < 50; i++) {
                    Item item = new Item();
                    cache.updateObject(item);
                    cache.removeObject(item);
                }
            });

            visitor.get(5, TimeUnit.SECONDS);
            mutator.get(5, TimeUnit.SECONDS);
        } finally {
            es.shutdownNow();
        }
    }
}

class OAObjectCacheWeakReferenceCleanupContractTest {

    public static class Item extends OAObject {
    }

    @Test
    void manuallyClearedWeakRefLookupReturnsMissDesiredContract() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();
        UUID guid = item.getGuid();

        cache.updateObject(item);

        Object weakRef = getWeakRef(cache, Item.class, guid);
        assertNotNull(weakRef);

        Method clear = WeakReference.class.getDeclaredMethod("clear");
        clear.invoke(weakRef);

        assertNull(cache.getObject(Item.class, guid), "cleared weak ref must not be valid cache hit");
    }

    @Test
    void checkReferenceQueueRemovesQueuedClearedRef() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();
        UUID guid = item.getGuid();

        cache.updateObject(item);

        Object weakRef = getWeakRef(cache, Item.class, guid);
        assertNotNull(weakRef);

        ((WeakReference<?>) weakRef).clear();
        ((WeakReference<?>) weakRef).enqueue();

        Method m = OAObjectCache.class.getDeclaredMethod("checkReferenceQueue");
        m.setAccessible(true);
        m.invoke(cache);

        assertNull(getWeakRef(cache, Item.class, guid));
    }

    @Test
    void staleQueuedOldReferenceDoesNotRemoveNewLiveEntrySameGuidDesiredContract() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        Item old = new Item();
        UUID guid = old.getGuid();

        cache.updateObject(old);

        Object oldRef = getWeakRef(cache, Item.class, guid);
        assertNotNull(oldRef);

        Item newer = new Item();
        forceGuid(newer, guid);
        cache.updateObject(newer, new OAObjectKey(new Object[] { "new" }, guid, true), Item.class);

        ((WeakReference<?>) oldRef).clear();
        ((WeakReference<?>) oldRef).enqueue();

        Method m = OAObjectCache.class.getDeclaredMethod("checkReferenceQueue");
        m.setAccessible(true);
        m.invoke(cache);

        assertSame(newer, cache.getObject(Item.class, guid),
            "cleanup of old cleared weak ref must not remove newer live object for same GUID");
    }

    @SuppressWarnings("unchecked")
    private static Object getWeakRef(OAObjectCache cache, Class<?> clazz, UUID guid) throws Exception {
        Field f = OAObjectCache.class.getDeclaredField("hmOAObjectByGuid");
        f.setAccessible(true);
        Map<Class<?>, Map<UUID, Object>> map = (Map<Class<?>, Map<UUID, Object>>) f.get(cache);
        Map<UUID, Object> m = map.get(clazz);
        return m == null ? null : m.get(guid);
    }

    private static void forceGuid(OAObject obj, UUID guid) throws Exception {
        Field f = OAObject.class.getDeclaredField("guid");
        f.setAccessible(true);
        f.set(obj, guid);
    }
}

class OAObjectCacheListenerUtilContractTest {

    public static class Item extends OAObject {
    }

    static class RecordingUtil extends OACacheListenerUtil {
        int count;
        String lastProperty;
        String lastStack;

        RecordingUtil(Class clazz, String property) {
            super(clazz, property);
        }

        @Override
        public void onEvent(OAObject obj, String propertyName, Object oldValue, Object newValue, String stackTrace) {
            count++;
            lastProperty = propertyName;
            lastStack = stackTrace;
        }
    }

    @Test
    void closeCanBeCalledAfterConstruction() {
        RecordingUtil util = new RecordingUtil(Item.class, "name");

        assertDoesNotThrow(util::close);
    }

    @Test
    void onEventDefaultIsSafeNoop() {
        OACacheListenerUtil util = new OACacheListenerUtil(Item.class, "name");

        assertDoesNotThrow(() -> util.onEvent(new Item(), "name", "a", "b", "stack"));

        util.close();
    }

    @Test
    void capturedEventMethodCanStorePropertyAndStack() {
        RecordingUtil util = new RecordingUtil(Item.class, "name");

        util.onEvent(new Item(), "name", "a", "b", "stack");

        assertEquals(1, util.count);
        assertEquals("name", util.lastProperty);
        assertEquals("stack", util.lastStack);

        util.close();
    }
}
