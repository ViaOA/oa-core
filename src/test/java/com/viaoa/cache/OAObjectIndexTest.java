package com.viaoa.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.Product;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

class OAObjectIndexTest {

    private static class ExposedIndex extends OAObjectIndex {
        boolean add(Class<? extends OAObject> c, OAObjectIndexKey ik, UUID guid) {
            return addToIndex(c, ik, guid);
        }

        UUID lookup(Class<? extends OAObject> c, OAObjectIndexKey ik) {
            return lookupGuid(c, ik);
        }

        boolean remove(Class<? extends OAObject> c, OAObjectIndexKey ik) {
            return removeFromIndex(c, ik);
        }
    }

    @Test
    void addToIndexObjectRejectsNullObject() {
        assertFalse(new OAObjectIndex().addToIndex(null));
    }

    @Test
    void addToIndexObjectAndKeyRejectsNullInputs() {
        ExposedIndex index = new ExposedIndex();
        Item item = new Item(1);

        assertFalse(index.addToIndex(null, new OAObjectKey(1)));
        assertFalse(index.addToIndex(item, null));
    }

    @Test
    void addToIndexClassKeyGuidRequiresValidClassKeyAndGuid() {
        ExposedIndex index = new ExposedIndex();
        UUID guid = UUID.randomUUID();

        assertFalse(index.add(null, new OAObjectIndexKey(new Object[] { 1 }), guid));
        assertFalse(index.add(Item.class, null, guid));
        assertFalse(index.add(Item.class, new OAObjectIndexKey(new Object[] { 1 }), null));
        assertFalse(index.add(Item.class, new OAObjectIndexKey(new Object[] { null }), guid));
        assertTrue(index.add(Item.class, new OAObjectIndexKey(new Object[] { 1 }), guid));
    }

    @Test
    void lookupGuidByIdsAndObjectKeyUsesClassSpecificIndex() {
        ExposedIndex index = new ExposedIndex();
        UUID guid = UUID.randomUUID();
        index.add(Item.class, new OAObjectIndexKey(new Object[] { 101 }), guid);

        assertEquals(guid, index.lookupGuid(Item.class, new Object[] { 101 }));
        assertEquals(guid, index.lookupGuid(Item.class, new OAObjectKey(101)));
        assertNull(index.lookupGuid(Product.class, new Object[] { 101 }));
        assertNull(index.lookupGuid(null, new Object[] { 101 }));
        assertNull(index.lookupGuid(Item.class, (OAObjectKey) null));
        assertNull(index.lookupGuid(Item.class, new Object[] { null }));
    }

    @Test
    void lookupGuidByIndexKeyRejectsNullOrInvalidInputs() {
        ExposedIndex index = new ExposedIndex();

        assertNull(index.lookup(null, new OAObjectIndexKey(new Object[] { 1 })));
        assertNull(index.lookup(Item.class, null));
        assertNull(index.lookup(Item.class, new OAObjectIndexKey(new Object[] { null })));
    }

    @Test
    void removeFromIndexObjectRejectsNullObject() {
        assertFalse(new OAObjectIndex().removeFromIndex(null));
    }

    @Test
    void removeFromIndexByClassAndObjectKeyRemovesExistingEntry() {
        ExposedIndex index = new ExposedIndex();
        UUID guid = UUID.randomUUID();
        OAObjectKey key = new OAObjectKey(22);
        index.add(Item.class, new OAObjectIndexKey(new Object[] { 22 }), guid);

        assertTrue(index.removeFromIndex(Item.class, key));
        assertNull(index.lookupGuid(Item.class, key));
        assertFalse(index.removeFromIndex(Item.class, key));
        assertFalse(index.removeFromIndex(null, key));
        assertFalse(index.removeFromIndex(Item.class, (OAObjectKey) null));
    }

    @Test
    void removeFromIndexByIndexKeyRejectsInvalidInputs() {
        ExposedIndex index = new ExposedIndex();

        assertFalse(index.remove(null, new OAObjectIndexKey(new Object[] { 1 })));
        assertFalse(index.remove(Item.class, null));
        assertFalse(index.remove(Item.class, new OAObjectIndexKey(new Object[] { null })));
    }

    @Test
    void updateIndexReplacesOldKeyWithNewKey() {
        ExposedIndex index = new ExposedIndex();
        Item item = new Item(123);
        OAObjectKey oldKey = new OAObjectKey(new Object[] { "OLD" }, UUID.randomUUID());
        OAObjectKey newKey = new OAObjectKey(new Object[] { "NEW" }, item.getGuid());

        index.add(Item.class, new OAObjectIndexKey(oldKey.getObjectIds()), oldKey.getGuid());
        index.updateIndex(item, newKey, oldKey);

        assertNull(index.lookupGuid(Item.class, oldKey));
        assertEquals(item.getGuid(), index.lookupGuid(Item.class, newKey));
    }

    @Test
    void updateIndexDoesNothingWhenIdsAreUnchanged() {
        ExposedIndex index = new ExposedIndex();
        Item item = new Item(124);
        UUID oldGuid = UUID.randomUUID();
        OAObjectKey oldKey = new OAObjectKey(new Object[] { "SAME" }, oldGuid);
        OAObjectKey newKey = new OAObjectKey(new Object[] { "SAME" }, item.getGuid());

        index.add(Item.class, new OAObjectIndexKey(oldKey.getObjectIds()), oldGuid);
        index.updateIndex(item, newKey, oldKey);

        assertEquals(oldGuid, index.lookupGuid(Item.class, oldKey));
    }

    @Test
    void clearRemovesAllClasses() {
        ExposedIndex index = new ExposedIndex();
        index.add(Item.class, new OAObjectIndexKey(new Object[] { 1 }), UUID.randomUUID());
        index.add(Product.class, new OAObjectIndexKey(new Object[] { 2 }), UUID.randomUUID());

        index.clear();

        assertNull(index.lookupGuid(Item.class, new Object[] { 1 }));
        assertNull(index.lookupGuid(Product.class, new Object[] { 2 }));
    }

    @Test
    void clearClassRemovesOnlyThatClass() {
        ExposedIndex index = new ExposedIndex();
        UUID productGuid = UUID.randomUUID();
        index.add(Item.class, new OAObjectIndexKey(new Object[] { 1 }), UUID.randomUUID());
        index.add(Product.class, new OAObjectIndexKey(new Object[] { 2 }), productGuid);

        index.clear(Item.class);
        index.clear(null);

        assertNull(index.lookupGuid(Item.class, new Object[] { 1 }));
        assertEquals(productGuid, index.lookupGuid(Product.class, new Object[] { 2 }));
    }
}
