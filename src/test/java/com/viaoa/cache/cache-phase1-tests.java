package com.viaoa.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

import org.junit.jupiter.api.Test;

class OAObjectIndexKeyContractTest {

    @Test
    void constructorDefensivelyClonesIds() {
        Object[] ids = { "A", 1 };
        OAObjectIndexKey key = new OAObjectIndexKey(ids);
        ids[0] = "B";
        assertArrayEquals(new Object[] { "A", 1 }, key.getIds());
    }

    @Test
    void equalsAndHashCodeUseAllOrderedComponents() {
        OAObjectIndexKey a = new OAObjectIndexKey(new Object[] { "A", 1 });
        OAObjectIndexKey b = new OAObjectIndexKey(new Object[] { "A", 1 });
        OAObjectIndexKey c = new OAObjectIndexKey(new Object[] { 1, "A" });

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void compositeKeyCanBeUsedAsHashMapKey() {
        OAObjectIndexKey a = new OAObjectIndexKey(new Object[] { "A", 1 });
        OAObjectIndexKey b = new OAObjectIndexKey(new Object[] { "A", 1 });

        Map<OAObjectIndexKey, String> map = new HashMap<>();
        map.put(a, "value");

        assertEquals("value", map.get(b));
    }

    @Test
    void hasValidIdsRequiresNonNullIds() {
        assertFalse(new OAObjectIndexKey(null).hasValidIds());
        assertFalse(new OAObjectIndexKey(new Object[0]).hasValidIds());
        assertFalse(new OAObjectIndexKey(new Object[] { null }).hasValidIds());
        assertTrue(new OAObjectIndexKey(new Object[] { "A" }).hasValidIds());
    }

    @Test
    void toStringUsesArrayFormat() {
        OAObjectIndexKey key = new OAObjectIndexKey(new Object[] { "A", 1 });
        assertEquals("[A, 1]", key.toString());
    }
}

class OAObjectIndexBasicContractTest {

    public static class Item extends OAObject {
    }

    private static UUID invokeLookup(OAObjectIndex idx, Class<?> c, OAObjectIndexKey ik) throws Exception {
        Method m = OAObjectIndex.class.getDeclaredMethod("lookupGuid", Class.class, OAObjectIndexKey.class);
        m.setAccessible(true);
        return (UUID) m.invoke(idx, c, ik);
    }

    private static boolean invokeAdd(OAObjectIndex idx, Class<?> c, OAObjectIndexKey ik, UUID guid) throws Exception {
        Method m = OAObjectIndex.class.getDeclaredMethod("addToIndex", Class.class, OAObjectIndexKey.class, UUID.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(idx, c, ik, guid);
    }

    @Test
    void addAndLookupGuidByCompositeKey() throws Exception {
        OAObjectIndex idx = new OAObjectIndex();
        UUID guid = UUID.randomUUID();
        OAObjectIndexKey key = new OAObjectIndexKey(new Object[] { "A", 1 });

        assertTrue(invokeAdd(idx, Item.class, key, guid));
        assertEquals(guid, invokeLookup(idx, Item.class, key));
    }

    @Test
    void invalidKeysAreNotIndexed() throws Exception {
        OAObjectIndex idx = new OAObjectIndex();

        assertFalse(invokeAdd(idx, Item.class, new OAObjectIndexKey(null), UUID.randomUUID()));
        assertFalse(invokeAdd(idx, Item.class, new OAObjectIndexKey(new Object[] { null }), UUID.randomUUID()));
    }

    @Test
    void classIsolationPreventsCrossClassHits() throws Exception {
        class Other extends OAObject {
        }

        OAObjectIndex idx = new OAObjectIndex();
        UUID guid = UUID.randomUUID();

        invokeAdd(idx, Item.class, new OAObjectIndexKey(new Object[] { "A" }), guid);

        assertNull(invokeLookup(idx, Other.class, new OAObjectIndexKey(new Object[] { "A" })));
    }

    @Test
    void lookupByObjectKeyUsesObjectIds() throws Exception {
        OAObjectIndex idx = new OAObjectIndex();
        UUID guid = UUID.randomUUID();

        invokeAdd(idx, Item.class, new OAObjectIndexKey(new Object[] { "A", 1 }), guid);

        OAObjectKey ok = new OAObjectKey(new Object[] { "A", 1 });

        assertEquals(guid, idx.lookupGuid(Item.class, ok));
    }
}

class OAObjectCacheBasicContractTest {

    public static class Item extends OAObject {
    }

    private static UUID guid(OAObject obj) throws Exception {
        Field f = OAObject.class.getDeclaredField("guid");
        f.setAccessible(true);
        return (UUID) f.get(obj);
    }

    @Test
    void emptyCacheReturnsMisses() {
        OAObjectCache cache = new OAObjectCache();

        assertNull(cache.getObject(Item.class, UUID.randomUUID()));
        assertNull(cache.getObject(Item.class, new Object[] { "A" }));
    }

    @Test
    void updateObjectMakesObjectReachableByGuid() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();

        cache.updateObject(item);

        assertSame(item, cache.getObject(Item.class, guid(item)));
    }

    @Test
    void clearCacheRemovesGuidLookup() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();

        cache.updateObject(item);
        cache.clearCache();

        assertNull(cache.getObject(Item.class, guid(item)));
    }

    @Test
    void getTotalReflectsPerClassCacheSize() {
        OAObjectCache cache = new OAObjectCache();

        cache.updateObject(new Item());
        cache.updateObject(new Item());

        assertEquals(2, cache.getTotal(Item.class));
    }

    @Test
    void repeatedUpdateForSameObjectReturnsFoundOnSecondCall() {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();

        assertFalse(cache.updateObject(item));
        assertTrue(cache.updateObject(item));
    }
}

class OAObjectCacheListenerLifecycleTest {

    static class Item extends OAObject {
    }

    static class RecordingListener implements OAObjectCacheListener<Item> {
        int addCount;
        int hubAddCount;
        int hubRemoveCount;
        int loadCount;
        int propCount;

        @Override
        public void afterPropertyChange(Item obj, String propertyName, Object oldValue, Object newValue) {
            propCount++;
        }

        @Override
        public void afterAdd(Item obj) {
            addCount++;
        }

        @Override
        public void afterAdd(Hub<Item> hub, Item obj) {
            hubAddCount++;
        }

        @Override
        public void afterRemove(Hub<Item> hub, Item obj) {
            hubRemoveCount++;
        }

        @Override
        public void afterLoad(Item obj) {
            loadCount++;
        }
    }

    @Test
    void listenerMethodsCanBeInvokedIndependently() {
        RecordingListener li = new RecordingListener();
        Item item = new Item();
        Hub<Item> hub = new Hub<>(Item.class);

        li.afterAdd(item);
        li.afterAdd(hub, item);
        li.afterRemove(hub, item);
        li.afterLoad(item);
        li.afterPropertyChange(item, "name", "a", "b");

        assertEquals(1, li.addCount);
        assertEquals(1, li.hubAddCount);
        assertEquals(1, li.hubRemoveCount);
        assertEquals(1, li.loadCount);
        assertEquals(1, li.propCount);
    }
}

class OAObjectCacheFilterBasicContractTest {

    public static class Item extends OAObject {
        private boolean active;

        public Item() {
        }

        public Item(boolean active) {
            this.active = active;
        }

        public boolean getActive() {
            return active;
        }
    }

    static class TestFilter extends OAObjectCacheFilter<Item> {
        TestFilter(Hub<Item> hub) {
            super(hub);
        }

        @Override
        public boolean isUsed(Item obj) {
            return obj != null && obj.getActive();
        }
    }

    @Test
    void constructorRejectsNullHub() {
        assertThrows(RuntimeException.class, () -> new TestFilter(null));
    }

    @Test
    void addNullFilterIsSafe() {
        Hub<Item> hub = new Hub<>(Item.class);
        TestFilter filter = new TestFilter(hub);

        assertDoesNotThrow(() -> filter.addFilter((OAFilter<Item>) null));
        filter.close();
    }

    @Test
    void closeCanBeCalledMoreThanOnce() {
        Hub<Item> hub = new Hub<>(Item.class);
        TestFilter filter = new TestFilter(hub);

        assertDoesNotThrow(filter::close);
        assertDoesNotThrow(filter::close);
    }
}

class OAObjectCacheTriggerBasicContractTest {

    public static class Item extends OAObject {
        private boolean active;

        public Item() {
        }

        public Item(boolean active) {
            this.active = active;
        }

        public boolean getActive() {
            return active;
        }
    }

    static class TestTrigger extends OAObjectCacheTrigger<Item> {
        int triggerCount;

        TestTrigger() {
            super(Item.class);
        }

        @Override
        public boolean isUsed(Item obj) {
            return obj != null && obj.getActive();
        }

        @Override
        public void onTrigger(Item obj) {
            triggerCount++;
        }
    }

    @Test
    void constructorRejectsNullClass() {
        assertThrows(RuntimeException.class, () -> new OAObjectCacheTrigger<Item>(null) {
            @Override
            public boolean isUsed(Item obj) {
                return true;
            }

            @Override
            public void onTrigger(Item obj) {
            }
        });
    }

    @Test
    void closeCanBeCalledRepeatedly() {
        TestTrigger trig = new TestTrigger();

        assertDoesNotThrow(trig::close);
        assertDoesNotThrow(trig::close);
    }
}
