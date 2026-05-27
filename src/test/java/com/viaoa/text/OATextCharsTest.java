package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class OATextCharsTest {

    @Test
    void hasDigitsHandlesNullEmptyAndMixedText() {
        assertFalse(OATextChars.hasDigits(null));
        assertFalse(OATextChars.hasDigits(""));
        assertFalse(OATextChars.hasDigits("abc"));
        assertTrue(OATextChars.hasDigits("abc123"));
    }

    @Test
    void firstCharHelpersHandleNullEmptyAndSimpleText() {
        assertNull(OATextChars.makeFirstCharLower(null));
        assertEquals("", OATextChars.makeFirstCharLower(""));
        assertEquals("abc", OATextChars.makeFirstCharLower("Abc"));
        assertEquals("abc", OATextChars.makeFirstCharLower("abc"));

        assertNull(OATextChars.makeFirstCharUpper(null));
        assertEquals("", OATextChars.makeFirstCharUpper(""));
        assertEquals("Abc", OATextChars.makeFirstCharUpper("abc"));
        assertEquals("Abc", OATextChars.makeFirstCharUpper("Abc"));
    }

    @Test
    void makeFirstUpperCharsLowerDocumentsAcronymBehavior() {
        assertNull(OATextChars.makeFirstUpperCharsLower(null));
        assertEquals("", OATextChars.makeFirstUpperCharsLower(""));
        assertEquals("gsmrServer", OATextChars.makeFirstUpperCharsLower("GSMRServer"));
        assertEquals("url", OATextChars.makeFirstUpperCharsLower("URL"));
        assertEquals("urlValue", OATextChars.makeFirstUpperCharsLower("URLValue"));
    }

    @Test
    void upperAndLowerAreNullSafe() {
        assertNull(OATextChars.upper(null));
        assertNull(OATextChars.lower(null));
        assertEquals("ABC", OATextChars.upper("abc"));
        assertEquals("abc", OATextChars.lower("ABC"));
    }

    @Test
    void upperAndLowerCurrentlyUseDefaultLocale() {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));

            assertEquals("İD", OATextChars.upper("id"));
            assertEquals("ı", OATextChars.lower("I"));
        } finally {
            Locale.setDefault(old);
        }
    }
}
