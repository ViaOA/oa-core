package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextChars. */
public class OATextCharsTest {
    @Test
    public void hasDigitsTest() {
        // contains a digit
        assertTrue(OATextChars.hasDigits("abc1"));
        // no digits
        assertFalse(OATextChars.hasDigits("abc"));
        // empty string
        assertFalse(OATextChars.hasDigits(""));
        // null input
        assertFalse(OATextChars.hasDigits(null));
    }

    @Test
    public void makeFirstCharLowerTest() {
        // normal case
        assertEquals("abc", OATextChars.makeFirstCharLower("Abc"));
        // already lower
        assertEquals("abc", OATextChars.makeFirstCharLower("abc"));
        // empty string is safe
        assertEquals("", OATextChars.makeFirstCharLower(""));
        // null input is safe
        assertNull(OATextChars.makeFirstCharLower(null));
    }

    @Test
    public void makeFirstUpperCharsLowerTest() {
        // leading uppercase run is lowered
        assertEquals("urlValue", OATextChars.makeFirstUpperCharsLower("URLValue"));
        // single leading uppercase is lowered
        assertEquals("name", OATextChars.makeFirstUpperCharsLower("Name"));
        // already lower
        assertEquals("name", OATextChars.makeFirstUpperCharsLower("name"));
        // null input is safe
        assertNull(OATextChars.makeFirstUpperCharsLower(null));
    }

    @Test
    public void makeFirstCharUpperTest() {
        // normal case
        assertEquals("Abc", OATextChars.makeFirstCharUpper("abc"));
        // already upper
        assertEquals("Abc", OATextChars.makeFirstCharUpper("Abc"));
        // empty string is safe
        assertEquals("", OATextChars.makeFirstCharUpper(""));
        // null input is safe
        assertNull(OATextChars.makeFirstCharUpper(null));
    }

    @Test
    public void upperTest() {
        // normal case
        assertEquals("ABC", OATextChars.upper("abc"));
        // mixed case
        assertEquals("ABC", OATextChars.upper("AbC"));
        // empty string
        assertEquals("", OATextChars.upper(""));
        // null input
        assertNull(OATextChars.upper(null));
    }

    @Test
    public void lowerTest() {
        // normal case
        assertEquals("abc", OATextChars.lower("ABC"));
        // mixed case
        assertEquals("abc", OATextChars.lower("AbC"));
        // empty string
        assertEquals("", OATextChars.lower(""));
        // null input
        assertNull(OATextChars.lower(null));
    }
}
