package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class OAObjectKeyNormalizationAndComparisonTest {

    static class Item extends OAObject {
    }

    @Test
    void nestedOAObjectIdNormalizesToNestedOAObjectKey() {
        Item item = new Item();

        OAObjectKey key = new OAObjectKey(new Object[] { "A", item });
        Object[] ids = key.getObjectIds();

        assertEquals("A", ids[0]);
        assertInstanceOf(OAObjectKey.class, ids[1]);
        assertEquals(item.getGuid(), ((OAObjectKey) ids[1]).getGuid());
    }

    @Test
    void nestedObjectKeyEqualityUsesNormalizedKey() {
        Item item = new Item();

        OAObjectKey a = new OAObjectKey(new Object[] { item });
        OAObjectKey b = new OAObjectKey(new Object[] { item.getObjectKey() });

        assertEquals(a, b);
    }

    @Test
    void compareToHandlesDifferentIdArrayLengthsDeterministically() {
        OAObjectKey one = new OAObjectKey(new Object[] { "A" });
        OAObjectKey two = new OAObjectKey(new Object[] { "A", 1 });

        int x = one.compareTo(two);
        int y = two.compareTo(one);

        assertEquals(Integer.signum(x), -Integer.signum(y));
    }

    @Test
    void compareToHandlesNonComparableIdsDeterministically() {
        Object a = new Object();
        Object b = new Object();

        OAObjectKey ka = new OAObjectKey(new Object[] { a });
        OAObjectKey kb = new OAObjectKey(new Object[] { b });

        int ab = ka.compareTo(kb);
        int ba = kb.compareTo(ka);

        assertEquals(Integer.signum(ab), -Integer.signum(ba));
    }

    @Test
    void compareToNullGuidVsNonNullGuidIsDefined() {
        OAObjectKey noGuid = new OAObjectKey(new Object[] { "A" });
        OAObjectKey withGuid = new OAObjectKey(new Object[] { "A" }, UUID.randomUUID());

        assertTrue(noGuid.compareTo(withGuid) < 0);
        assertTrue(withGuid.compareTo(noGuid) > 0);
    }

    @Test
    void toStringIncludesGuidOrIds() {
        OAObjectKey idsOnly = new OAObjectKey(new Object[] { "A", 1 });
        assertTrue(idsOnly.toString().contains("A"));

        UUID guid = UUID.randomUUID();
        OAObjectKey guidKey = new OAObjectKey(new Object[] { "A" }, guid);
        assertTrue(guidKey.toString().contains(guid.toString()));
    }
}
