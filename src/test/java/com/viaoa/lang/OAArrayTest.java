package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Internal tests for OAArray.
 *
 * Strategy:
 * - One test method per public production method name.
 * - Overloads are tested inside the same methodNameTest().
 * - Comments explain what each assertion is checking.
 * - ASCII-only test data.
 */
public class OAArrayTest {

    @Test
    public void containsTest() {
        // object array: value found using equals
        assertTrue(OAArray.contains(new String[] { "a", "b" }, new String("a")));

        // object array: null value can be found
        assertTrue(OAArray.contains(new String[] { "a", null }, null));

        // object array: missing value returns false
        assertFalse(OAArray.contains(new String[] { "a", "b" }, "c"));

        // object array: null array returns false
        assertFalse(OAArray.contains((Object[]) null, "a"));

        // int array: value found
        assertTrue(OAArray.contains(new int[] { 1, 2 }, 2));

        // int array: missing value returns false
        assertFalse(OAArray.contains(new int[] { 1, 2 }, 3));

        // double array: direct value found
        assertTrue(OAArray.contains(new double[] { 1.5, 2.5 }, 2.5));

        // string array: current implementation ignores bCaseSensitive and uses equalsIgnoreCase
        assertTrue(OAArray.contains(new String[] { "ABC" }, "abc", true));
    }

    @Test
    public void containsExactTest() {
        // same object reference is found
        String value = new String("a");
        assertTrue(OAArray.containsExact(new String[] { value }, value));

        // equal but different object reference is not found
        assertFalse(OAArray.containsExact(new String[] { new String("a") }, new String("a")));

        // null value can be found by reference comparison
        assertTrue(OAArray.containsExact(new String[] { null }, null));

        // null array returns false
        assertFalse(OAArray.containsExact(null, "a"));
    }

    @Test
    public void isEqualTest() {
        // same reference is equal
        String[] values = { "a", "b" };
        assertTrue(OAArray.isEqual(values, values));

        // same values are equal
        assertTrue(OAArray.isEqual(new String[] { "a", "b" }, new String[] { "a", "b" }));

        // null elements are equal when in same position
        assertTrue(OAArray.isEqual(new String[] { "a", null }, new String[] { "a", null }));

        // different values are not equal
        assertFalse(OAArray.isEqual(new String[] { "a" }, new String[] { "b" }));

        // different lengths are not equal
        assertFalse(OAArray.isEqual(new String[] { "a" }, new String[] { "a", "b" }));

        // one null array is not equal to non-null
        assertFalse(OAArray.isEqual(null, new String[] { "a" }));
    }

    @Test
    public void indexOfTest() {
        // object array: found using equals
        assertEquals(1, OAArray.indexOf(new String[] { "a", "b" }, new String("b")));

        // object array: null value can be found
        assertEquals(1, OAArray.indexOf(new String[] { "a", null }, null));

        // object array: missing value returns -1
        assertEquals(-1, OAArray.indexOf(new String[] { "a", "b" }, "c"));

        // object array: start position is honored
        assertEquals(2, OAArray.indexOf(new String[] { "a", "b", "a" }, "a", 1));

        // invalid start position returns -1
        assertEquals(-1, OAArray.indexOf(new String[] { "a" }, "a", -1));

        // int array: found value returns index
        assertEquals(1, OAArray.indexOf(new int[] { 1, 2 }, 2));

        // double array: found value returns index
        assertEquals(1, OAArray.indexOf(new double[] { 1.5, 2.5 }, 2.5));

        // string array: current implementation ignores bCaseSensitive and uses equalsIgnoreCase
        assertEquals(0, OAArray.indexOf(new String[] { "ABC" }, "abc", true));
    }

    @Test
    public void addTest() {
        // typed object array: append one value
        String[] ss = OAArray.add(new String[] { "a" }, "b");
        assertArrayEquals(new String[] { "a", "b" }, ss);

        // typed object array: append multiple values
        ss = OAArray.add(new String[] { "a" }, "b", "c");
        assertArrayEquals(new String[] { "a", "b", "c" }, ss);

        // null typed array with non-null value creates array using value type
        ss = OAArray.add((String[]) null, "a");
        assertArrayEquals(new String[] { "a" }, ss);

        // explicit component type: append one value
        Object[] objs = OAArray.add(String.class, null, "a");
        assertEquals(String.class, objs.getClass().getComponentType());
        assertArrayEquals(new String[] { "a" }, objs);

        // explicit component type: append multiple values
        objs = OAArray.add(String.class, new String[] { "a" }, "b", "c");
        assertArrayEquals(new String[] { "a", "b", "c" }, objs);

        // primitive int array: append value
        assertArrayEquals(new int[] { 1, 2 }, OAArray.add(new int[] { 1 }, 2));

        // primitive boolean array: append value
        assertArrayEquals(new boolean[] { true, false }, OAArray.add(new boolean[] { true }, false));

        // primitive double array: append value
        assertArrayEquals(new double[] { 1.5, 2.5 }, OAArray.add(new double[] { 1.5 }, 2.5));

        // string array: duplicate allowed by default single-value overload
        assertArrayEquals(new String[] { "a", "a" }, OAArray.add(new String[] { "a" }, "a"));

        // string array: duplicate not added when bAllowDups is false
        assertArrayEquals(new String[] { "a" }, OAArray.add(new String[] { "a" }, new String[] { "a" }, false));
    }

    @Test
    public void removeValueTest() {
        // explicit component type: remove matching object value
        Object[] objs = OAArray.removeValue(String.class, new String[] { "a", "b" }, "a");
        assertArrayEquals(new String[] { "b" }, objs);

        // explicit component type: missing object value returns original values
        objs = OAArray.removeValue(String.class, new String[] { "a", "b" }, "c");
        assertArrayEquals(new String[] { "a", "b" }, objs);

        // int array: current source is expected to remove matching value
        int[] ints = OAArray.removeValue(new int[] { 1, 2 }, 1);
        assertArrayEquals(new int[] { 2 }, ints);

        // double array: current source is expected to remove matching value
        double[] ds = OAArray.removeValue(new double[] { 1.5, 2.5 }, 1.5);
        assertArrayEquals(new double[] { 2.5 }, ds);
    }

    @Test
    public void removeAtTest() {
        // typed object array: remove middle value
        assertArrayEquals(new String[] { "a", "c" }, OAArray.removeAt(new String[] { "a", "b", "c" }, 1));

        // typed object array: invalid position returns same reference
        String[] ss = new String[] { "a" };
        assertSame(ss, OAArray.removeAt(ss, -1));

        // explicit component type: remove value
        Object[] objs = OAArray.removeAt(String.class, new String[] { "a", "b" }, 0);
        assertEquals(String.class, objs.getClass().getComponentType());
        assertArrayEquals(new String[] { "b" }, objs);

        // int array: remove value at position
        assertArrayEquals(new int[] { 1, 3 }, OAArray.removeAt(new int[] { 1, 2, 3 }, 1));

        // double array: remove value at position
        assertArrayEquals(new double[] { 1.5, 3.5 }, OAArray.removeAt(new double[] { 1.5, 2.5, 3.5 }, 1));
    }

    @Test
    public void insertTest() {
        // typed object array: insert in middle
        assertArrayEquals(new String[] { "a", "x", "b" }, OAArray.insert(new String[] { "a", "b" }, "x", 1));

        // typed object array: insert past end appends
        assertArrayEquals(new String[] { "a", "x" }, OAArray.insert(new String[] { "a" }, "x", 10));

        // explicit component type: insert into null array
        Object[] objs = OAArray.insert(String.class, null, "a", 0);
        assertEquals(String.class, objs.getClass().getComponentType());
        assertArrayEquals(new String[] { "a" }, objs);

        // explicit component type: insert in middle
        objs = OAArray.insert(String.class, new String[] { "a", "b" }, "x", 1);
        assertArrayEquals(new String[] { "a", "x", "b" }, objs);
    }

    @Test
    public void reorderToMatchTest() {
        // reorders first array to match ordering in second array
        String[] a = { "b", "a", "c" };
        String[] b = { "a", "b", "c" };
        OAArray.reorderToMatch(a, b);
        assertArrayEquals(new String[] { "a", "b", "c" }, a);

        // empty arrays do not throw
        assertDoesNotThrow(() -> OAArray.reorderToMatch(new String[0], new String[0]));
    }

    @Test
    public void hasNullTest() {
        // array with null returns true
        assertTrue(OAArray.hasNull(new Object[] { "a", null }));

        // array without null returns false
        assertFalse(OAArray.hasNull(new Object[] { "a", "b" }));

        // empty array returns false
        assertFalse(OAArray.hasNull(new Object[0]));

        // null array returns false
        assertFalse(OAArray.hasNull(null));
    }
}
