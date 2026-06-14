package com.viaoa.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;

class OAObjectCacheTest {

    @BeforeEach
    void beforeEach() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
        OAObjectService os = (OAObjectService) og.objectsInternal();
        os.getOAObjectCacheService().removeAllObjects();
    }

    @Test
    void getClassesReturnsCachedObjectClasses() {
        OAObjectCache cache = new OAObjectCache();
        Item item = item(1, "A");
        Product product = new Product(2);

        cache.updateObject(item, new OAObjectKey(new Object[] { 1 }, item.getGuid()), Item.class);
        cache.updateObject(product, new OAObjectKey(new Object[] { 2 }, product.getGuid()), Product.class);

        List<Class<? extends OAObject>> classes = Arrays.asList(cache.getClasses());
        assertTrue(classes.contains(Item.class));
        assertTrue(classes.contains(Product.class));
    }

    @Test
    void getTotalReturnsClassSpecificCount() {
        OAObjectCache cache = new OAObjectCache();
        Item item1 = item(1, "A");
        Item item2 = item(2, "B");
        Product product = new Product(3);

        cache.updateObject(item1, new OAObjectKey(new Object[] { 1 }, item1.getGuid()), Item.class);
        cache.updateObject(item2, new OAObjectKey(new Object[] { 2 }, item2.getGuid()), Item.class);
        cache.updateObject(product, new OAObjectKey(new Object[] { 3 }, product.getGuid()), Product.class);

        assertEquals(2, cache.getTotal(Item.class));
        assertEquals(1, cache.getTotal(Product.class));
        assertEquals(0, cache.getTotal(Register.class));
    }

    @Test
    void clearCacheClassRemovesOnlyThatClassAndIndex() {
        OAObjectCache cache = new OAObjectCache();
        Item item = item(1, "A");
        Product product = new Product(2);
        cache.updateObject(item, new OAObjectKey(new Object[] { 1 }, item.getGuid()), Item.class);
        cache.updateObject(product, new OAObjectKey(new Object[] { 2 }, product.getGuid()), Product.class);

        cache.clearCache(Item.class);

        assertEquals(0, cache.getTotal(Item.class));
        assertEquals(1, cache.getTotal(Product.class));
        assertNull(cache.getObject(Item.class, new Object[] { 1 }));
        assertSame(product, cache.getObject(Product.class, new Object[] { 2 }));
    }

    @Test
    void clearCacheRemovesAllClassesAndIndexes() {
        OAObjectCache cache = new OAObjectCache();
        Item item = item(1, "A");
        Product product = new Product(2);
        cache.updateObject(item, new OAObjectKey(new Object[] { 1 }, item.getGuid()), Item.class);
        cache.updateObject(product, new OAObjectKey(new Object[] { 2 }, product.getGuid()), Product.class);

        cache.clearCache();

        assertEquals(0, cache.getClasses().length);
        assertNull(cache.getObject(Item.class, item.getGuid()));
        assertNull(cache.getObject(Product.class, new Object[] { 2 }));
    }

    @Test
    void getObjectByGuidReturnsCachedInstance() {
        OAObjectCache cache = new OAObjectCache();
        Item item = item(1, "A");
        cache.updateObject(item, new OAObjectKey(new Object[] { 1 }, item.getGuid()), Item.class);

        assertSame(item, cache.getObject(Item.class, item.getGuid()));
        assertNull(cache.getObject(Item.class, UUID.randomUUID()));
        assertNull(cache.getObject(Product.class, item.getGuid()));
    }

    @Test
    void getObjectByIdsUsesSecondaryIndex() {
        OAObjectCache cache = new OAObjectCache();
        Item item = item(1, "A");
        cache.updateObject(item, new OAObjectKey(new Object[] { 1001 }, item.getGuid()), Item.class);

        assertSame(item, cache.getObject(Item.class, new Object[] { 1001 }));
        assertNull(cache.getObject(null, new Object[] { 1001 }));
        assertNull(cache.getObject(Item.class, (Object[]) null));
        assertNull(cache.getObject(Item.class, new Object[] { null }));
    }

    @Test
    void getObjectByObjectKeyUsesGuidBeforeIndexLookup() {
        OAObjectCache cache = new OAObjectCache();
        Item item = item(1, "A");
        OAObjectKey key = new OAObjectKey(new Object[] { "BUSINESS" }, item.getGuid());
        cache.updateObject(item, key, Item.class);

        assertSame(item, cache.getObject(Item.class, key));
        assertSame(item, cache.getObject(Item.class, new OAObjectKey(new Object[] { "BUSINESS" })));
        assertNull(cache.getObject(null, key));
        assertNull(cache.getObject(Item.class, (OAObjectKey) null));
        assertNull(cache.getObject(Item.class, new OAObjectKey(new Object[] { "MISSING" })));
    }

    @Test
    void updateObjectWithRuntimeKeyAddsObject() {
        OAObjectCache cache = new OAObjectCache();
        Item item = item(1, "A");

        assertFalse(cache.updateObject(item));
        assertSame(item, cache.getObject(Item.class, item.getGuid()));
    }

    @Test
    void updateObjectWithExplicitKeyReportsDuplicateAndKeepsFirstInstance() {
        OAObjectCache cache = new OAObjectCache();
        Item first = item(1, "A");
        Item second = item(2, "B");
        OAObjectKey firstKey = new OAObjectKey(new Object[] { 1 }, first.getGuid());
        OAObjectKey secondKeySameGuid = new OAObjectKey(new Object[] { 2 }, first.getGuid());

        assertFalse(cache.updateObject(first, firstKey, Item.class));
        assertTrue(cache.updateObject(second, secondKeySameGuid, Item.class));

        assertSame(first, cache.getObject(Item.class, first.getGuid()));
        assertNull(cache.getObject(Item.class, new Object[] { 1 }));
        assertSame(first, cache.getObject(Item.class, new Object[] { 2 }));
        assertFalse(cache.updateObject(null, firstKey, Item.class));
        assertFalse(cache.updateObject(first, null, Item.class));
    }

    @Test
    void removeObjectRemovesCachedObjectAndIndex() {
        OAObjectCache cache = new OAObjectCache();
        Item item = item(1, "A");
        cache.updateObject(item);

        assertTrue(cache.removeObject(item));
        assertFalse(cache.removeObject(item));
        assertFalse(cache.removeObject(null));
        assertNull(cache.getObject(Item.class, item.getGuid()));
    }

    @Test
    void visitIteratesCachedObjectsAndStopsWhenCallbackReturnsFalse() {
        OAObjectCache cache = new OAObjectCache();
        Item item1 = item(1, "A");
        Item item2 = item(2, "B");
        cache.updateObject(item1, new OAObjectKey(new Object[] { 1 }, item1.getGuid()), Item.class);
        cache.updateObject(item2, new OAObjectKey(new Object[] { 2 }, item2.getGuid()), Item.class);

        List<Item> visited = new ArrayList<>();
        cache.visit(Item.class, obj -> {
            visited.add(obj);
            return false;
        });

        assertEquals(1, visited.size());
        assertTrue(visited.get(0) == item1 || visited.get(0) == item2);
    }

    @Test
    void findReturnsFirstMatchingObjectOrCollectsLimitedResults() {
        OAObjectCache cache = new OAObjectCache();
        Item item1 = item(1, "A");
        Item item2 = item(2, "B");
        cache.updateObject(item1, new OAObjectKey(new Object[] { 1 }, item1.getGuid()), Item.class);
        cache.updateObject(item2, new OAObjectKey(new Object[] { 2 }, item2.getGuid()), Item.class);

        Item first = cache.find(null, Item.class, null, false, 0, null);
        assertTrue(first == item1 || first == item2);

        List<Item> results = new ArrayList<>();
        Item lastAdded = cache.find(null, Item.class, null, false, 1, results);
        assertEquals(1, results.size());
        assertSame(results.get(0), lastAdded);

        List<Item> skippedNew = new ArrayList<>();
        assertNull(cache.find(null, Item.class, null, true, 0, skippedNew));
        assertTrue(skippedNew.isEmpty());
        assertNull(cache.find(null, Product.class, null, false, 0, null));
    }

    @Test
    void getRandomHandlesEmptyInvalidAndPopulatedCaches() {
        OAObjectCache cache = new OAObjectCache();
        assertNull(cache.getRandom(null, 1));
        assertNull(cache.getRandom(Item.class, 0));
        assertNull(cache.getRandom(Item.class, 1));

        Item item = item(1, "A");
        cache.updateObject(item, new OAObjectKey(new Object[] { 1 }, item.getGuid()), Item.class);

        assertSame(item, cache.getRandom(Item.class, 1));
    }

    private static Item item(int id, String code) {
        Item item = new Item(id);
        item.setCode(code);
        item.setName("Item " + code);
        return item;
    }
}
