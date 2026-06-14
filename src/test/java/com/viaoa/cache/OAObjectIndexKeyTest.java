package com.viaoa.cache;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectIndexKeyTest {

    @Test
    void constructorClonesIdsAndNormalizesNullToEmptyKey() {
        Object[] ids = { 10, "A" };
        OAObjectIndexKey key = new OAObjectIndexKey(ids);
        ids[0] = 99;

        assertArrayEquals(new Object[] { 10, "A" }, key.getIds());

        OAObjectIndexKey empty = new OAObjectIndexKey(null);
        assertArrayEquals(new Object[0], empty.getIds());
    }

    @Test
    void hasValidIdsRequiresAtLeastOneNonNullId() {
        assertFalse(new OAObjectIndexKey(null).hasValidIds());
        assertFalse(new OAObjectIndexKey(new Object[0]).hasValidIds());
        assertFalse(new OAObjectIndexKey(new Object[] { 1, null }).hasValidIds());
        assertTrue(new OAObjectIndexKey(new Object[] { 1, "A" }).hasValidIds());
    }

    @Test
    void getIdsReturnsInternalConstructorClone() {
        Object[] ids = { 1, "X" };
        OAObjectIndexKey key = new OAObjectIndexKey(ids);

        Object[] keyIds = key.getIds();

        assertNotSame(ids, keyIds);
        assertArrayEquals(ids, keyIds);
    }

    @Test
    void equalsUsesIdContent() {
        OAObjectIndexKey key = new OAObjectIndexKey(new Object[] { 7, "A" });

        assertEquals(key, key);
        assertEquals(key, new OAObjectIndexKey(new Object[] { 7, "A" }));
        assertNotEquals(key, new OAObjectIndexKey(new Object[] { 7, "B" }));
        assertNotEquals(key, "not a key");
    }

    @Test
    void hashCodeUsesIdContent() {
        OAObjectIndexKey key1 = new OAObjectIndexKey(new Object[] { 7, "A" });
        OAObjectIndexKey key2 = new OAObjectIndexKey(new Object[] { 7, "A" });

        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void toStringUsesArrayFormat() {
        assertEquals("[7, A]", new OAObjectIndexKey(new Object[] { 7, "A" }).toString());
    }
}
