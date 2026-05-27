package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAArrayTest {

    @Test
    void containsAndIndexOfAreNullSafeForObjectArrays() {
        String same = new String("same");
        String equal = new String("same");
        Object[] values = { null, same, "other" };

        assertFalse(OAArray.contains((Object[]) null, "x"));
        assertTrue(OAArray.contains(values, null));
        assertTrue(OAArray.contains(values, equal));
        assertTrue(OAArray.containsExact(values, same));
        assertFalse(OAArray.containsExact(values, equal));
        assertEquals(0, OAArray.indexOf(values, null));
        assertEquals(1, OAArray.indexOf(values, equal));
        assertEquals(-1, OAArray.indexOf(values, "missing"));
    }

    @Test
    void stringContainsCurrentlyIgnoresCaseSensitiveFlag() {
        String[] values = { "Alpha" };

        assertTrue(OAArray.contains(values, "alpha", true));
        assertEquals(0, OAArray.indexOf(values, "alpha", true));
    }

    @Test
    void addHandlesObjectPrimitiveAndStringArrays() {
        String[] strings = OAArray.add((String[]) null, "a");
        strings = OAArray.add(strings, (String) null);
        strings = OAArray.add(strings, new String[] { "b", null, "a", "c" }, false);

        assertArrayEquals(new String[] { "a", null, "b", "c" }, strings);
        assertArrayEquals(new int[] { 1, 2 }, OAArray.add(OAArray.add((int[]) null, 1), 2));
        assertArrayEquals(new boolean[] { true, false }, OAArray.add(OAArray.add((boolean[]) null, true), false));
        assertArrayEquals(new double[] { 1.5d, 2.5d }, OAArray.add(OAArray.add((double[]) null, 1.5d), 2.5d));
    }

    @Test
    void typedAddNullArrayWithNullValueCannotInferComponentType() {
        assertNull(OAArray.add((Object[]) null, (Object) null));
        assertArrayEquals(new String[] { null }, (String[]) OAArray.add(String.class, null, (Object) null));
    }

    @Test
    void removeAtHandlesBoundariesAndPreservesComponentType() {
        String[] values = { "a", "b", "c" };

        assertSame(values, OAArray.removeAt(values, -1));
        assertSame(values, OAArray.removeAt(values, 3));
        assertArrayEquals(new String[] { "b", "c" }, OAArray.removeAt(values, 0));
        assertArrayEquals(new String[] { "a", "c" }, OAArray.removeAt(values, 1));
        assertArrayEquals(new String[] { "a", "b" }, OAArray.removeAt(values, 2));
        assertEquals(String.class, OAArray.removeAt(values, 1).getClass().getComponentType());
    }

    @Test
    void removeValueHandlesObjectValuesButCurrentlyDoesNotRemoveNullOrPrimitiveValues() {
        String[] values = { "a", null, "b", "a" };

        assertArrayEquals(new String[] { null, "b", "a" }, (String[]) OAArray.removeValue(String.class, values, "a"));
        assertSame(values, OAArray.removeValue(String.class, values, null));
        assertArrayEquals(new int[] { 1, 2, 3 }, OAArray.removeValue(new int[] { 1, 2, 3 }, 2));
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d }, OAArray.removeValue(new double[] { 1.0d, 2.0d, 3.0d }, 2.0d));
    }

    @Test
    void insertAppendsWhenPositionIsPastEndAndInsertsInsideArray() {
        String[] values = { "a", "c" };

        assertArrayEquals(new String[] { "a", "b", "c" }, OAArray.insert(values, "b", 1));
        assertArrayEquals(new String[] { "a", "c", "d" }, OAArray.insert(values, "d", 99));
        assertArrayEquals(new String[] { "x" }, OAArray.insert((String[]) null, "x", 99));
        assertNull(OAArray.insert((Object[]) null, null, 0));
    }

    @Test
    void insertCurrentlyThrowsForNegativePosition() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> OAArray.insert(new String[] { "a" }, "b", -1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> OAArray.insert(String.class, new String[] { "a" }, "b", -1));
    }

    @Test
    void reorderToMatchMutatesOnlyWhenAllValuesAreFound() {
        String[] values = { "b", "a", "c" };

        OAArray.reorderToMatch(values, new String[] { "a", "b", "c" });
        assertArrayEquals(new String[] { "a", "b", "c" }, values);

        OAArray.reorderToMatch(values, new String[] { "x", "b", "c" });
        assertArrayEquals(new String[] { "a", "b", "c" }, values);
    }

    @Test
    void reorderToMatchCurrentlyFailsForNullAndDuplicateValues() {
        assertThrows(NullPointerException.class, () -> OAArray.reorderToMatch(new String[] { null }, new String[] { null }));

        String[] values = { "a", "a" };
        OAArray.reorderToMatch(values, new String[] { "a", "a" });

        assertArrayEquals(new String[] { "a", null }, values);
    }

    @Test
    void hasNullIsNullSafe() {
        assertFalse(OAArray.hasNull(null));
        assertFalse(OAArray.hasNull(new Object[] { "a", "b" }));
        assertTrue(OAArray.hasNull(new Object[] { "a", null }));
    }
}
