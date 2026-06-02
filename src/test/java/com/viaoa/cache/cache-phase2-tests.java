package com.viaoa.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.callback.OACallback;
import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

import org.junit.jupiter.api.Test;

class OAObjectIndexUpdateRemoveContractTest {

    public static class Item extends OAObject {
    }

    private static boolean invokeAdd(OAObjectIndex idx, Class<?> c, OAObjectIndexKey ik, UUID guid) throws Exception {
        Method m = OAObjectIndex.class.getDeclaredMethod("addToIndex", Class.class, OAObjectIndexKey.class, UUID.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(idx, c, ik, guid);
    }

    private static UUID invokeLookup(OAObjectIndex idx, Class<?> c, OAObjectIndexKey ik) throws Exception {
        Method m = OAObjectIndex.class.getDeclaredMethod("lookupGuid", Class.class, OAObjectIndexKey.class);
        m.setAccessible(true);
        return (UUID) m.invoke(idx, c, ik);
    }

    private static boolean invokeRemove(OAObjectIndex idx, Class<?> c, OAObjectIndexKey ik) throws Exception {
        Method m = OAObjectIndex.class.getDeclaredMethod("removeFromIndex", Class.class, OAObjectIndexKey.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(idx, c, ik);
    }

    @Test
    void removeFromIndexRemovesOnlyTargetKey() throws Exception {
        OAObjectIndex idx = new OAObjectIndex();
        UUID g1 = UUID.randomUUID();
        UUID g2 = UUID.randomUUID();

        OAObjectIndexKey k1 = new OAObjectIndexKey(new Object[] { "A" });
        OAObjectIndexKey k2 = new OAObjectIndexKey(new Object[] { "B" });

        invokeAdd(idx, Item.class, k1, g1);
        invokeAdd(idx, Item.class, k2, g2);

        assertTrue(invokeRemove(idx, Item.class, k1));

        assertNull(invokeLookup(idx, Item.class, k1));
        assertEquals(g2, invokeLookup(idx, Item.class, k2));
    }

    @Test
    void removeMissingKeyReturnsFalseOrLeavesStateUnchanged() throws Exception {
        OAObjectIndex idx = new OAObjectIndex();
        UUID guid = UUID.randomUUID();
        OAObjectIndexKey key = new OAObjectIndexKey(new Object[] { "A" });

        invokeAdd(idx, Item.class, key, guid);

        invokeRemove(idx, Item.class, new OAObjectIndexKey(new Object[] { "missing" }));

        assertEquals(guid, invokeLookup(idx, Item.class, key));
    }

    @Test
    void clearClassRemovesOnlyThatClass() throws Exception {
        class Other extends OAObject {
        }

        OAObjectIndex idx = new OAObjectIndex();
        UUID g1 = UUID.randomUUID();
        UUID g2 = UUID.randomUUID();

        OAObjectIndexKey key = new OAObjectIndexKey(new Object[] { "A" });

        invokeAdd(idx, Item.class, key, g1);
        invokeAdd(idx, Other.class, key, g2);

        idx.clear(Item.class);

        assertNull(invokeLookup(idx, Item.class, key));
        assertEquals(g2, invokeLookup(idx, Other.class, key));
    }

    @Test
    void clearAllRemovesAllClassIndexes() throws Exception {
        class Other extends OAObject {
        }

        OAObjectIndex idx = new OAObjectIndex();
        OAObjectIndexKey key = new OAObjectIndexKey(new Object[] { "A" });

        invokeAdd(idx, Item.class, key, UUID.randomUUID());
        invokeAdd(idx, Other.class, key, UUID.randomUUID());

        idx.clear();

        assertNull(invokeLookup(idx, Item.class, key));
        assertNull(invokeLookup(idx, Other.class, key));
    }
}

class OAObjectCacheRemoveAndVisitContractTest {

    public static class Item extends OAObject {
    }

    private static UUID guid(OAObject obj) throws Exception {
        Field f = OAObject.class.getDeclaredField("guid");
        f.setAccessible(true);
        return (UUID) f.get(obj);
    }

    @Test
    void removeObjectRemovesGuidLookup() throws Exception {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();

        cache.updateObject(item);
        UUID guid = guid(item);

        assertSame(item, cache.getObject(Item.class, guid));

        assertTrue(cache.removeObject(item));

        assertNull(cache.getObject(Item.class, guid));
    }

    @Test
    void removeObjectNullReturnsFalse() {
        OAObjectCache cache = new OAObjectCache();

        assertFalse(cache.removeObject(null));
    }

    @Test
    void removeObjectTwiceReturnsFalseSecondTime() {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();

        cache.updateObject(item);

        assertTrue(cache.removeObject(item));
        assertFalse(cache.removeObject(item));
    }

    @Test
    void visitInvokesCallbackForCachedObjects() {
        OAObjectCache cache = new OAObjectCache();
        Item a = new Item();
        Item b = new Item();

        cache.updateObject(a);
        cache.updateObject(b);

        AtomicInteger cnt = new AtomicInteger();

        cache.visit(Item.class, new OACallback<OAObject>() {
            @Override
            public boolean updateObject(OAObject obj) {
                assertInstanceOf(Item.class, obj);
                cnt.incrementAndGet();
                return true;
            }
        });

        assertEquals(2, cnt.get());
    }

    @Test
    void visitStopsWhenCallbackReturnsFalse() {
        OAObjectCache cache = new OAObjectCache();
        cache.updateObject(new Item());
        cache.updateObject(new Item());

        AtomicInteger cnt = new AtomicInteger();

        cache.visit(Item.class, new OACallback<OAObject>() {
            @Override
            public boolean updateObject(OAObject obj) {
                cnt.incrementAndGet();
                return false;
            }
        });

        assertEquals(1, cnt.get());
    }

    @Test
    void visitWithNullCallbackIsSafeNoop() {
        OAObjectCache cache = new OAObjectCache();
        cache.updateObject(new Item());

        assertDoesNotThrow(() -> cache.visit(Item.class, null));
    }
}

class OAObjectCacheHubAdderContractTest {

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

    static class ActiveAdder extends OAObjectCacheHubAdder<Item> {
        ActiveAdder(Hub<Item> hub) {
            super(hub);
        }

        @Override
        public boolean isUsed(Item obj) {
            return obj != null && obj.getActive();
        }
    }

    @Test
    void constructorRejectsNullHub() {
        assertThrows(IllegalArgumentException.class, () -> new ActiveAdder(null));
    }

    @Test
    void afterAddAddsOnlyUsedObjects() {
        Hub<Item> hub = new Hub<>(Item.class);
        ActiveAdder adder = new ActiveAdder(hub);

        Item active = new Item(true);
        Item inactive = new Item(false);

        adder.afterAdd(active);
        adder.afterAdd(inactive);

        assertTrue(hub.contains(active));
        assertFalse(hub.contains(inactive));

        adder.close();
    }

    @Test
    void afterLoadDelegatesToAfterAdd() {
        Hub<Item> hub = new Hub<>(Item.class);
        ActiveAdder adder = new ActiveAdder(hub);

        Item active = new Item(true);

        adder.afterLoad(active);

        assertTrue(hub.contains(active));

        adder.close();
    }

    @Test
    void afterAddNullIsSafe() {
        Hub<Item> hub = new Hub<>(Item.class);
        ActiveAdder adder = new ActiveAdder(hub);

        assertDoesNotThrow(() -> adder.afterAdd(null));

        adder.close();
    }

    @Test
    void closeIsIdempotent() {
        Hub<Item> hub = new Hub<>(Item.class);
        ActiveAdder adder = new ActiveAdder(hub);

        assertDoesNotThrow(adder::close);
        assertDoesNotThrow(adder::close);
    }

    @Test
    void weakHubClearedCausesClosePathOnAfterAddCurrentContract() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        ActiveAdder adder = new ActiveAdder(hub);

        Field f = OAObjectCacheHubAdder.class.getDeclaredField("wfHub");
        f.setAccessible(true);
        f.set(adder, new WeakReference<Hub<Item>>(null));

        assertDoesNotThrow(() -> adder.afterAdd(new Item(true)));
        assertDoesNotThrow(adder::close);
    }
}

class OAObjectCacheFilterAndTriggerFilterContractTest {

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

    static class BaseFilter extends OAObjectCacheFilter<Item> {
        BaseFilter(Hub<Item> hub) {
            super(hub);
        }
    }

    static class BaseTrigger extends OAObjectCacheTrigger<Item> {
        int count;

        BaseTrigger() {
            super(Item.class);
        }

        @Override
        public void onTrigger(Item obj) {
            count++;
        }
    }

    @Test
    void cacheFilterWithoutAddedFiltersReturnsFalseCurrentContract() {
        Hub<Item> hub = new Hub<>(Item.class);
        BaseFilter filter = new BaseFilter(hub);

        assertFalse(filter.isUsed(new Item(true, 10)));

        filter.close();
    }

    @Test
    void cacheFilterRequiresAllAddedFilters() {
        Hub<Item> hub = new Hub<>(Item.class);
        BaseFilter filter = new BaseFilter(hub);

        filter.addFilter(item -> item.getActive(), false);
        filter.addFilter(item -> item.getAmount() > 5, false);

        assertTrue(filter.isUsed(new Item(true, 10)));
        assertFalse(filter.isUsed(new Item(false, 10)));
        assertFalse(filter.isUsed(new Item(true, 1)));

        filter.close();
    }

    @Test
    void cacheTriggerWithoutAddedFiltersReturnsTrueCurrentContract() {
        BaseTrigger trig = new BaseTrigger();

        assertTrue(trig.isUsed(new Item(false, 0)));

        trig.close();
    }

    @Test
    void cacheTriggerRequiresAllAddedFilters() {
        BaseTrigger trig = new BaseTrigger();

        trig.addFilter(item -> item.getActive(), false);
        trig.addFilter(item -> item.getAmount() > 5, false);

        assertTrue(trig.isUsed(new Item(true, 10)));
        assertFalse(trig.isUsed(new Item(false, 10)));
        assertFalse(trig.isUsed(new Item(true, 1)));

        trig.close();
    }

    @Test
    void addDependentPropertyIgnoresNullAndEmpty() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        BaseFilter filter = new BaseFilter(hub);
        BaseTrigger trig = new BaseTrigger();

        filter.addDependentProperty(null, false);
        filter.addDependentProperty("", false);
        trig.addDependentProperty(null);
        trig.addDependentProperty("");

        Field ff = OAObjectCacheFilter.class.getDeclaredField("dependentPropertyPaths");
        ff.setAccessible(true);
        assertNull(ff.get(filter));

        Field ft = OAObjectCacheTrigger.class.getDeclaredField("dependentPropertyPaths");
        ft.setAccessible(true);
        assertNull(ft.get(trig));

        filter.close();
        trig.close();
    }
}
