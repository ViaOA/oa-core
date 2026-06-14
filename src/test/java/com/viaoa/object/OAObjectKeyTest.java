package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class OAObjectKeyTest {

    @Test
    void constructorWithIdsAndGuidStoresDefensiveIdsAndGuid() {
        UUID guid = UUID.randomUUID();
        Object[] ids = new Object[] { 1, "A" };
        OAObjectKey key = new OAObjectKey(ids, guid);
        ids[0] = 99;
        Object[] returned = key.getObjectIds();
        returned[1] = "B";

        assertArrayEquals(new Object[] { 1, "A" }, key.getObjectIds());
        assertEquals(guid, key.getGuid());
    }

    @Test
    void constructorWithIdsUsesNullGuid() {
        OAObjectKey key = new OAObjectKey(new Object[] { 7 });

        assertArrayEquals(new Object[] { 7 }, key.getObjectIds());
        assertNull(key.getGuid());
    }

    @Test
    void constructorWithIntStoresSingleIntegerId() {
        assertArrayEquals(new Object[] { 5 }, new OAObjectKey(5).getObjectIds());
    }

    @Test
    void constructorWithLongStoresSingleLongId() {
        assertArrayEquals(new Object[] { 5L }, new OAObjectKey(5L).getObjectIds());
    }

    @Test
    void constructorWithObjectStoresSingleObjectId() {
        assertArrayEquals(new Object[] { "abc" }, new OAObjectKey("abc").getObjectIds());
    }

    @Test
    void getObjectIdsReturnsNullWhenConstructedWithNullIds() {
        assertNull(new OAObjectKey((Object[]) null).getObjectIds());
    }

    @Test
    void getGuidReturnsConfiguredGuid() {
        UUID guid = UUID.randomUUID();

        assertEquals(guid, new OAObjectKey(new Object[] { 1 }, guid).getGuid());
    }

    @Test
    void hasValidObjectIdsRequiresNonEmptyNonNullIds() {
        assertFalse(new OAObjectKey((Object[]) null).hasValidObjectIds());
        assertFalse(new OAObjectKey(new Object[0]).hasValidObjectIds());
        assertFalse(new OAObjectKey(new Object[] { 1, null }).hasValidObjectIds());
        assertTrue(new OAObjectKey(new Object[] { 1, "A" }).hasValidObjectIds());
    }

    @Test
    void equalsUsesGuidWhenEitherKeyHasGuid() {
        UUID guid = UUID.randomUUID();

        assertEquals(new OAObjectKey(new Object[] { 1 }, guid), new OAObjectKey(new Object[] { 2 }, guid));
        assertNotEquals(new OAObjectKey(new Object[] { 1 }, guid), new OAObjectKey(new Object[] { 1 }));
    }

    @Test
    void equalsUsesObjectIdsWhenNoGuidExists() {
        assertEquals(new OAObjectKey(new Object[] { 1, "A" }), new OAObjectKey(new Object[] { 1, "A" }));
        assertNotEquals(new OAObjectKey(new Object[] { 1, "A" }), new OAObjectKey(new Object[] { 1, "B" }));
        assertNotEquals(new OAObjectKey(new Object[] { 1 }), "notKey");
    }

    @Test
    void hashCodeMatchesGuidOrIdsEquality() {
        UUID guid = UUID.randomUUID();

        assertEquals(new OAObjectKey(new Object[] { 1 }, guid).hashCode(), new OAObjectKey(new Object[] { 2 }, guid).hashCode());
        assertEquals(new OAObjectKey(new Object[] { 1, "A" }).hashCode(), new OAObjectKey(new Object[] { 1, "A" }).hashCode());
    }

    @Test
    void compareToHandlesNullNonKeyGuidAndIdOrdering() {
        UUID smaller = new UUID(0, 1);
        UUID larger = new UUID(0, 2);

        assertTrue(new OAObjectKey(1).compareTo(null) > 0);
        assertTrue(new OAObjectKey(1).compareTo("x") > 0);
        assertTrue(new OAObjectKey(new Object[] { 1 }, smaller).compareTo(new OAObjectKey(new Object[] { 1 }, larger)) < 0);
        assertTrue(new OAObjectKey(new Object[] { 1 }).compareTo(new OAObjectKey(new Object[] { 2 })) < 0);
        assertTrue(new OAObjectKey(new Object[] { 1 }).compareTo(new OAObjectKey(new Object[] { 1, 2 })) < 0);
        assertEquals(0, new OAObjectKey(new Object[] { 1 }).compareTo(new OAObjectKey(new Object[] { 1 })));
    }

    @Test
    void toStringIncludesGuidAndIds() {
        UUID guid = UUID.randomUUID();
        String value = new OAObjectKey(new Object[] { 1, "A" }, guid).toString();

        assertTrue(value.contains(guid.toString()));
        assertTrue(value.contains("[1, A]"));
    }
}
