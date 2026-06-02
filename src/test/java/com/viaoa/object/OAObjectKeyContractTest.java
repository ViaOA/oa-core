package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OAObjectKeyContractTest {

    static class Item extends OAObject {
    }

    @Test
    void constructorDefensivelyCopiesObjectIds() {
        Object[] ids = { "A", 1 };

        OAObjectKey key = new OAObjectKey(ids);

        ids[0] = "B";

        assertArrayEquals(new Object[] { "A", 1 }, key.getObjectIds());
    }

    @Test
    void getObjectIdsReturnsClone() {
        OAObjectKey key = new OAObjectKey(new Object[] { "A", 1 });

        Object[] ids = key.getObjectIds();
        ids[0] = "B";

        assertArrayEquals(new Object[] { "A", 1 }, key.getObjectIds());
    }

    @Test
    void nullIdsRemainNullAndAreInvalid() {
        OAObjectKey key = new OAObjectKey((Object[]) null);

        assertNull(key.getObjectIds());
        assertFalse(key.hasValidObjectIds());
    }

    @Test
    void emptyIdsAreInvalid() {
        OAObjectKey key = new OAObjectKey(new Object[0]);

        assertArrayEquals(new Object[0], key.getObjectIds());
        assertFalse(key.hasValidObjectIds());
    }

    @Test
    void allIdsMustBeNonNullToBeValid() {
        assertFalse(new OAObjectKey(new Object[] { "A", null }).hasValidObjectIds());
        assertTrue(new OAObjectKey(new Object[] { "A", 1 }).hasValidObjectIds());
    }

    @Test
    void singleValueConstructorsCreateSingleIdKey() {
        assertArrayEquals(new Object[] { 5 }, new OAObjectKey(5).getObjectIds());
        assertArrayEquals(new Object[] { 5L }, new OAObjectKey(5L).getObjectIds());
        assertArrayEquals(new Object[] { "A" }, new OAObjectKey("A").getObjectIds());
    }

    @Test
    void guidTakesPrecedenceForEqualsAndHashCode() {
        UUID guid = UUID.randomUUID();

        OAObjectKey a = new OAObjectKey(new Object[] { "A" }, guid);
        OAObjectKey b = new OAObjectKey(new Object[] { "B" }, guid);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void oneNullGuidAndOneNonNullGuidAreNotEqualEvenWithSameIds() {
        UUID guid = UUID.randomUUID();

        OAObjectKey a = new OAObjectKey(new Object[] { "A" }, guid);
        OAObjectKey b = new OAObjectKey(new Object[] { "A" }, null);

        assertNotEquals(a, b);
    }

    @Test
    void idsAreUsedForEqualsOnlyWhenBothGuidsAreNull() {
        OAObjectKey a = new OAObjectKey(new Object[] { "A", 1 });
        OAObjectKey b = new OAObjectKey(new Object[] { "A", 1 });
        OAObjectKey c = new OAObjectKey(new Object[] { 1, "A" });

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void keyCanBeUsedAsHashMapKey() {
        OAObjectKey a = new OAObjectKey(new Object[] { "A", 1 });
        OAObjectKey b = new OAObjectKey(new Object[] { "A", 1 });

        Map<OAObjectKey, String> map = new HashMap<>();
        map.put(a, "value");

        assertEquals("value", map.get(b));
    }

    @Test
    void normalizeConvertsOAObjectIdsToObjectKeys() {
        Item item = new Item();

        OAObjectKey key = new OAObjectKey(new Object[] { item });
        Object[] ids = key.getObjectIds();

        assertEquals(1, ids.length);
        assertInstanceOf(OAObjectKey.class, ids[0]);
    }

    @Test
    void compareToNullAndNonKeyAreDefined() {
        OAObjectKey key = new OAObjectKey(new Object[] { "A" });

        assertTrue(key.compareTo(null) > 0);
        assertTrue(key.compareTo("A") > 0);
    }

    @Test
    void compareToUsesGuidWhenEitherGuidPresent() {
        UUID a = new UUID(0, 1);
        UUID b = new UUID(0, 2);

        OAObjectKey ka = new OAObjectKey(new Object[] { "Z" }, a);
        OAObjectKey kb = new OAObjectKey(new Object[] { "A" }, b);

        assertTrue(ka.compareTo(kb) < 0);
        assertTrue(kb.compareTo(ka) > 0);
        assertEquals(0, ka.compareTo(new OAObjectKey(new Object[] { "other" }, a)));
    }

    @Test
    void compareToUsesIdsWhenBothGuidNull() {
        OAObjectKey a = new OAObjectKey(new Object[] { "A", 1 });
        OAObjectKey b = new OAObjectKey(new Object[] { "A", 2 });

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(new OAObjectKey(new Object[] { "A", 1 })));
    }

    @Test
    void objectKeyIsSerializable() throws Exception {
        OAObjectKey key = new OAObjectKey(new Object[] { "A", 1 }, UUID.randomUUID());

        OAObjectKey copy = roundTrip(key);

        assertEquals(key, copy);
        assertArrayEquals(key.getObjectIds(), copy.getObjectIds());
        assertEquals(key.getGuid(), copy.getGuid());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
