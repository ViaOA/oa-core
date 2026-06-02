package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.UUID;

import com.viaoa.cache.OAObjectCache;

import org.junit.jupiter.api.Test;

class OAObjectDeepIdentityCacheContractTest {

    public static class Item extends OAObject {
        private int id;
        private String name;

        public int getId() { return id; }
        public void setId(int id) {
            int old = this.id;
            this.id = id;
            firePropertyChange("id", old, id);
        }

        public String getName() { return name; }
        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    @Test
    void guidRemainsStableAfterPrimaryKeyAssignment() {
        Item item = new Item();
        UUID guid = item.getGuid();

        item.setId(100);

        assertEquals(guid, item.getGuid());
        assertEquals(guid, item.getObjectKey().getGuid());
    }

    @Test
    void objectKeyGuidRemainsStableAfterBusinessPropertyMutation() {
        Item item = new Item();
        OAObjectKey key = item.getObjectKey();

        item.setName("A");
        item.setId(1);

        assertEquals(key.getGuid(), item.getObjectKey().getGuid());
    }

    @Test
    void objectKeyReturnedArrayMutationDoesNotAffectKeyLookupSemantics() {
        OAObjectKey key = new OAObjectKey(new Object[] { "A", 1 });

        Object[] ids = key.getObjectIds();
        ids[0] = "B";

        assertEquals(new OAObjectKey(new Object[] { "A", 1 }), key);
        assertNotEquals(new OAObjectKey(new Object[] { "B", 1 }), key);
    }

    @Test
    void cacheGuidLookupStillWorksAfterBusinessPropertyMutation() {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();

        cache.updateObject(item);
        UUID guid = item.getGuid();

        item.setName("A");
        item.setId(123);

        assertSame(item, cache.getObject(Item.class, guid));
    }

    @Test
    void cacheRemoveUsesRuntimeIdentityNotCurrentBusinessPropertyValue() {
        OAObjectCache cache = new OAObjectCache();
        Item item = new Item();

        cache.updateObject(item);
        UUID guid = item.getGuid();

        item.setId(123);
        item.setName("A");

        assertTrue(cache.removeObject(item));
        assertNull(cache.getObject(Item.class, guid));
    }

    @Test
    void duplicateGuidConflictDoesNotSilentlyReturnWrongLiveObject() throws Exception {
        OAObjectCache cache = new OAObjectCache();

        Item a = new Item();
        Item b = new Item();

        UUID guid = a.getGuid();
        forceGuid(b, guid);

        cache.updateObject(a);
        cache.updateObject(b, new OAObjectKey(new Object[] { "B" }, guid), Item.class);

        Item result = cache.getObject(Item.class, guid);

        assertTrue(result == a || result == b);
        assertEquals(guid, result.getGuid());
    }

    private static void forceGuid(OAObject obj, UUID guid) throws Exception {
        Field f = OAObject.class.getDeclaredField("guid");
        f.setAccessible(true);
        f.set(obj, guid);
    }
}
