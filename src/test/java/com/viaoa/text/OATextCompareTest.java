package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextCompare. */
public class OATextCompareTest {
    @Test
    public void isEqualTest() {
        // exact match
        assertTrue(OATextCompare.isEqual("abc", "abc"));
        // mismatch
        assertFalse(OATextCompare.isEqual("abc", "ABC"));
        // ignore case overload
        assertTrue(OATextCompare.isEqual("abc", "ABC", true));
        // null equals blank option
        assertTrue(OATextCompare.isEqual(null, "", false, true));
        // null does not equal blank by default
        assertFalse(OATextCompare.isEqual(null, ""));
    }

    @Test
    public void isEqualIgnoreCaseTest() {
        // case-insensitive match
        assertTrue(OATextCompare.isEqualIgnoreCase("abc", "ABC"));
        // different text remains different
        assertFalse(OATextCompare.isEqualIgnoreCase("abc", "xyz"));
        // null comparison is safe
        assertFalse(OATextCompare.isEqualIgnoreCase(null, "abc"));
    }

    @Test
    public void isEqualNullEqualsBlankTest() {
        // null and empty are treated as equal
        assertTrue(OATextCompare.isEqualNullEqualsBlank(null, ""));
        // blank and null are treated as equal
        assertTrue(OATextCompare.isEqualNullEqualsBlank("", null));
        // nonblank values still compare normally
        assertFalse(OATextCompare.isEqualNullEqualsBlank("abc", null));
    }

    @Test
    public void equalsTest() {
        // alias for equality
        assertTrue(OATextCompare.equals("abc", "abc"));
        // case-sensitive by default
        assertFalse(OATextCompare.equals("abc", "ABC"));
        // optional ignore case
        assertTrue(OATextCompare.equals("abc", "ABC", true));
    }

    @Test
    public void isNotEqualTest() {
        // different values
        assertTrue(OATextCompare.isNotEqual("abc", "xyz"));
        // same values
        assertFalse(OATextCompare.isNotEqual("abc", "abc"));
        // ignore case overload
        assertFalse(OATextCompare.isNotEqual("abc", "ABC", true));
        // null equals blank option
        assertFalse(OATextCompare.isNotEqual(null, "", false, true));
    }

    @Test
    public void isNotEqualNullEqualsBlankTest() {
        // null and empty are treated as equal
        assertFalse(OATextCompare.isNotEqualNullEqualsBlank(null, ""));
        // nonblank and null are different
        assertTrue(OATextCompare.isNotEqualNullEqualsBlank("abc", null));
    }

    @Test
    public void notEqualsTest() {
        // alias for not equal
        assertTrue(OATextCompare.notEquals("abc", "xyz"));
        // same values are not different
        assertFalse(OATextCompare.notEquals("abc", "abc"));
        // optional ignore case
        assertFalse(OATextCompare.notEquals("abc", "ABC", true));
    }

    @Test
    public void isLikeTest() {
        // exact pattern
        assertTrue(OATextCompare.isLike("abc", "abc"));
        // wildcard pattern
        assertTrue(OATextCompare.isLike("abcdef", "abc*"));
        // mismatch
        assertFalse(OATextCompare.isLike("abcdef", "xyz*"));
    }

    @Test
    public void compareTest() {
        // equal values
        assertEquals(0, OATextCompare.compare("abc", "abc"));
        // lexical order
        assertTrue(OATextCompare.compare("abc", "abd") < 0);
        assertTrue(OATextCompare.compare("abd", "abc") > 0);
        // null-safe behavior
        assertDoesNotThrow(() -> OATextCompare.compare(null, "abc"));
    }

    @Test
    public void indexOfTest() {
        // normal search
        assertEquals(1, OATextCompare.indexOf("abc", "b"));
        // start position
        assertEquals(3, OATextCompare.indexOf("abcabc", "a", 1));
        // ignore case
        assertEquals(1, OATextCompare.indexOf("aBc", "b", true));
        // not found
        assertEquals(-1, OATextCompare.indexOf("abc", "x"));
        // null-safe behavior
        assertEquals(-1, OATextCompare.indexOf(null, "a"));
    }

    @Test
    public void lastIndexOfTest() {
        // normal reverse search
        assertEquals(3, OATextCompare.lastIndexOf("abcabc", "a"));
        // ignore case
        assertEquals(3, OATextCompare.lastIndexOf("abcAbc", "a", true));
        // not found
        assertEquals(-1, OATextCompare.lastIndexOf("abc", "x"));
        // null-safe behavior
        assertEquals(-1, OATextCompare.lastIndexOf(null, "a"));
    }

    @Test
    public void containsTest() {
        // normal contains
        assertTrue(OATextCompare.contains("abc", "b"));
        // start position overload
        assertFalse(OATextCompare.contains("abc", "a", 1));
        // ignore case overload
        assertTrue(OATextCompare.contains("abc", "B", 0, true));
        // null-safe behavior
        assertFalse(OATextCompare.contains(null, "a"));
    }

    @Test
    public void startsWithTest() {
        // normal prefix
        assertTrue(OATextCompare.startsWith("abc", "ab"));
        // mismatch
        assertFalse(OATextCompare.startsWith("abc", "bc"));
        // ignore case overload
        assertTrue(OATextCompare.startsWith("abc", "AB", true));
        // null-safe behavior
        assertFalse(OATextCompare.startsWith(null, "a"));
    }

    @Test
    public void endsWithTest() {
        // normal suffix
        assertTrue(OATextCompare.endsWith("abc", "bc"));
        // mismatch
        assertFalse(OATextCompare.endsWith("abc", "ab"));
        // ignore case overload
        assertTrue(OATextCompare.endsWith("abc", "BC", true));
        // null-safe behavior
        assertFalse(OATextCompare.endsWith(null, "a"));
    }

    @Test
    public void appendIfMissingTest() {
        // missing suffix is appended
        assertEquals("abc/", OATextCompare.appendIfMissing("abc", "/"));
        // existing suffix is not duplicated
        assertEquals("abc/", OATextCompare.appendIfMissing("abc/", "/"));
        // ignore case overload
        assertEquals("abcX", OATextCompare.appendIfMissing("abcX", "x", true));
    }

    @Test
    public void prefixIfMissingTest() {
        // missing prefix is added
        assertEquals("/abc", OATextCompare.prefixIfMissing("abc", "/"));
        // existing prefix is not duplicated
        assertEquals("/abc", OATextCompare.prefixIfMissing("/abc", "/"));
        // ignore case overload
        assertEquals("Xabc", OATextCompare.prefixIfMissing("Xabc", "x", true));
    }
}
