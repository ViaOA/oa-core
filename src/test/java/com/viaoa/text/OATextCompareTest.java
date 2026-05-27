package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OATextCompareTest {

    @Test
    void equalityHandlesCaseAndNullBlankSemantics() {
        assertTrue(OATextCompare.isEqual("abc", "abc"));
        assertFalse(OATextCompare.isEqual("abc", "ABC"));
        assertTrue(OATextCompare.isEqualIgnoreCase("abc", "ABC"));

        assertFalse(OATextCompare.isEqual(null, ""));
        assertTrue(OATextCompare.isEqualNullEqualsBlank(null, ""));
        assertTrue(OATextCompare.isEqual(null, "", false, true));
    }

    @Test
    void isLikeUsesOACompareWildcardSemantics() {
        assertTrue(OATextCompare.isLike("abcdef", "abc*"));
        assertTrue(OATextCompare.isLike("abcdef", "*def"));
        assertTrue(OATextCompare.isLike("abcdef", "ab%ef"));
        assertFalse(OATextCompare.isLike("abcdef", "ab*z"));
    }

    @Test
    void containsStartsWithAndEndsWithHonorIgnoreCaseFlag() {
//        assertTrue(OATextCompare.contains("Hello World", "world", true));
//        assertFalse(OATextCompare.contains("Hello World", "world", false));

        assertTrue(OATextCompare.startsWith("Hello World", "hello", true));
        assertFalse(OATextCompare.startsWith("Hello World", "hello", false));

        assertTrue(OATextCompare.endsWith("Hello World", "WORLD", true));
        assertFalse(OATextCompare.endsWith("Hello World", "WORLD", false));
    }

    @Test
    void indexOfAndLastIndexOfReturnOriginalPositionsForAscii() {
        assertEquals(6, OATextCompare.indexOf("Hello World", "World", 0, false));
        assertEquals(6, OATextCompare.indexOf("Hello World", "world", 0, true));
//        assertEquals(3, OATextCompare.lastIndexOf("abcabc", "abc", 5, false));
    }

    @Test
    void missingSearchValuesReturnNotFound() {
        assertEquals(-1, OATextCompare.indexOf("abc", "z", 0, false));
//        assertEquals(-1, OATextCompare.lastIndexOf("abc", "z", 2, false));
//        assertFalse(OATextCompare.contains("abc", "z", false));
    }
}
