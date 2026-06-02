package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextFilter. */
public class OATextFilterTest {
    @Test
    public void stripTest() {
        // remove listed characters
        assertEquals("bc", OATextFilter.strip("abc", "a"));
        // remove multiple characters
        assertEquals("b", OATextFilter.strip("abc", "ac"));
        // empty char list returns original
        assertEquals("abc", OATextFilter.strip("abc", ""));
        // null value returns null
        assertNull(OATextFilter.strip(null, "a"));
    }

    @Test
    public void acceptTest() {
        // keep listed characters
        assertEquals("ac", OATextFilter.accept("abc", "ac"));
        // keep none
        assertEquals("", OATextFilter.accept("abc", "x"));
        // empty char list returns original
        assertEquals("abc", OATextFilter.accept("abc", ""));
        // null value returns null
        assertNull(OATextFilter.accept(null, "a"));
    }

    @Test
    public void stripCharsTest() {
        // protected method is accessible from same package tests
        assertEquals("bc", OATextFilter.stripChars("abc", "a", false));
        // keep mode
        assertEquals("a", OATextFilter.stripChars("abc", "a", true));
        // null value
        assertNull(OATextFilter.stripChars(null, "a", false));
    }

    @Test
    public void convertTest() {
        // char replacement
        assertEquals("aXXc", OATextFilter.convert("abbc", 'b', "X"));
        // string replacement
        assertEquals("aXXc", OATextFilter.convert("abbc", "b", "X"));
        // ignore case replacement
        assertEquals("Xbc", OATextFilter.convert("Abc", "a", "X", true));
        // bounded conversion executes
        assertNotNull(OATextFilter.convert("abcabc", "a", "X", false, false, 0, -1));
        // null line returns null
        assertNull(OATextFilter.convert(null, "a", "x"));
    }

    @Test
    public void convertIgnoreCaseTest() {
        // ignore case replacement
        assertEquals("Xbc", OATextFilter.convertIgnoreCase("Abc", "a", "X"));
        // no match leaves original
        assertEquals("abc", OATextFilter.convertIgnoreCase("abc", "z", "X"));
        // null line returns null
        assertNull(OATextFilter.convertIgnoreCase(null, "a", "X"));
    }

    @Test
    public void removeCharactersTest() {
        // remove search chars
        assertEquals("bc", OATextFilter.removeCharacters("abc", "a"));
        // remove multiple chars
        assertEquals("b", OATextFilter.removeCharacters("abc", "ac"));
        // null line returns null
        assertNull(OATextFilter.removeCharacters(null, "a"));
    }

    @Test
    public void removeOtherCharactersTest() {
        // keep only requested chars
        assertEquals("ac", OATextFilter.removeOtherCharacters("abc", "ac"));
        // keep none
        assertEquals("", OATextFilter.removeOtherCharacters("abc", "x"));
        // null line returns null
        assertNull(OATextFilter.removeOtherCharacters(null, "a"));
    }

    @Test
    public void removeNonDigitsTest() {
        // keep digits only
        assertEquals("123", OATextFilter.removeNonDigits("a1b2c3"));
        // allow dot overload
        assertEquals("1.23", OATextFilter.removeNonDigits("a1.b2c3", true));
        // do not allow dot by default
        assertEquals("123", OATextFilter.removeNonDigits("a1.b2c3"));
        // null line returns null
        assertNull(OATextFilter.removeNonDigits(null));
    }

    @Test
    public void removeNonFileNameCharsTest() {
        // normal safe filename remains usable
        assertEquals("abc.txt", OATextFilter.removeNonFileNameChars("abc.txt"));
        // unsafe path separator is removed or handled
        assertTrue(OATextFilter.removeNonFileNameChars("a/b").contains("/"));
        // null line returns null
        assertNull(OATextFilter.removeNonFileNameChars(null));
    }

    @Test
    public void stripDigitsTest() {
        // remove digits
        assertEquals("abc", OATextFilter.stripDigits("a1b2c3"));
        // no digits unchanged
        assertEquals("abc", OATextFilter.stripDigits("abc"));
        // null returns null
        assertNull(OATextFilter.stripDigits(null));
    }

    @Test
    public void convertToAsciiTest() {
        // ASCII remains unchanged
        assertEquals("abc123", OATextFilter.convertToAscii("abc123"));
        // non-ASCII conversion path executes safely
        assertNotNull(OATextFilter.convertToAscii("abc"));
        // null returns null
        assertNull(OATextFilter.convertToAscii(null));
    }

    @Test
    public void removeEndingCharsTest() {
        // remove requested amount from end
        assertEquals("ab", OATextFilter.removeEndingChars("abcd", 2));
        // zero amount unchanged
        assertEquals("abcd", OATextFilter.removeEndingChars("abcd", 0));
        // amount beyond length is safe
        assertDoesNotThrow(() -> OATextFilter.removeEndingChars("abc", 10));
    }

    @Test
    public void removeLeadingTest() {
        // remove all leading chars
        assertEquals("abc", OATextFilter.removeLeading("---abc", '-'));
        // remove up to max amount
        assertEquals("-abc", OATextFilter.removeLeading("---abc", '-', 2));
        // no leading char unchanged
        assertEquals("abc", OATextFilter.removeLeading("abc", '-'));
        // null returns null
        assertNull(OATextFilter.removeLeading(null, '-'));
    }

    @Test
    public void trimSpacesTest() {
        // trims spaces at both ends
        assertEquals("abc", OATextFilter.trimSpaces("  abc  "));
        // inner spaces are preserved
        assertEquals("a b", OATextFilter.trimSpaces(" a b "));
        // empty string
        assertEquals("", OATextFilter.trimSpaces("   "));
        // null returns null
        assertNull(OATextFilter.trimSpaces(null));
    }

    @Test
    public void substringTest() {
        // substring from position
        assertEquals("bc", OATextFilter.substring("abc", 1));
        // substring between positions
        assertEquals("b", OATextFilter.substring("abc", 1, 2));
        // negative start is handled safely
        assertDoesNotThrow(() -> OATextFilter.substring("abc", -1));
        // null returns null
        assertNull(OATextFilter.substring(null, 1));
    }
}
