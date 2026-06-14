package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class OAObjectKeyFinalEdgeTest {

    @Test
    void nullIdsWithNullGuidCompareEqualCurrentContract() {
        OAObjectKey a = new OAObjectKey((Object[]) null);
        OAObjectKey b = new OAObjectKey((Object[]) null);

        assertEquals(a, b);
        assertEquals(0, a.compareTo(b));
    }

    @Test
    void nullIdsAndEmptyIdsAreNotEqualButCompareDeterministically() {
        OAObjectKey a = new OAObjectKey((Object[]) null);
        OAObjectKey b = new OAObjectKey(new Object[0]);

        assertNotEquals(a, b);

        int ab = a.compareTo(b);
        int ba = b.compareTo(a);

        assertEquals(Integer.signum(ab), -Integer.signum(ba));
    }

    @Test
    void arraysWithNullComponentsHaveDeterministicEqualityAndHash() {
        OAObjectKey a = new OAObjectKey(new Object[] { "A", null });
        OAObjectKey b = new OAObjectKey(new Object[] { "A", null });
        OAObjectKey c = new OAObjectKey(new Object[] { null, "A" });

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertFalse(a.hasValidObjectIds());
    }

    @Test
    void guidOnlyComparisonHandlesNullIds() {
        UUID guid = UUID.randomUUID();

        OAObjectKey a = new OAObjectKey(null, guid);
        OAObjectKey b = new OAObjectKey(new Object[] { "A" }, guid);

        assertEquals(a, b);
        assertEquals(0, a.compareTo(b));
    }

    @Test
    void compareToHandlesMixedComparableClassesDeterministically() {
        OAObjectKey a = new OAObjectKey(new Object[] { "1" });
        OAObjectKey b = new OAObjectKey(new Object[] { 1 });

        int ab = a.compareTo(b);
        int ba = b.compareTo(a);

        assertEquals(Integer.signum(ab), -Integer.signum(ba));
    }

    @Test
    void compareToHandlesNestedObjectKeys() {
        OAObjectKey innerA = new OAObjectKey(new Object[] { "A" });
        OAObjectKey innerB = new OAObjectKey(new Object[] { "B" });

        OAObjectKey a = new OAObjectKey(new Object[] { innerA });
        OAObjectKey b = new OAObjectKey(new Object[] { innerB });

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    void hashCodeMatchesEqualsForGuidPrecedence() {
        UUID guid = UUID.randomUUID();

        OAObjectKey a = new OAObjectKey(new Object[] { "A" }, guid);
        OAObjectKey b = new OAObjectKey(new Object[] { "B" }, guid);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
