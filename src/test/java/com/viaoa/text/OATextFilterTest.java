package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OATextFilterTest {

    @Test
    void stripAndAcceptApplyBlacklistAndWhitelistRules() {
        assertEquals("bc", OATextFilter.strip("abcabc", "a"));
        assertEquals("aa", OATextFilter.accept("abcabc", "a"));
        assertNull(OATextFilter.strip(null, "a"));
        assertEquals("abc", OATextFilter.strip("abc", null));
        assertEquals("abc", OATextFilter.accept("abc", ""));
    }

    @Test
    void convertReplacesAllMatchesAndSupportsIgnoreCase() {
        assertEquals("XbcXbc", OATextFilter.convert("abcabc", "a", "X"));
        assertEquals("XbcXbc", OATextFilter.convertIgnoreCase("AbCaBc", "a", "X"));
        assertEquals("abc", OATextFilter.convert("abc", null, "x"));
        assertNull(OATextFilter.convert(null, "a", "x"));
    }

    @Test
    void convertFirstOnlyCurrentlyDropsRemainderAfterFirstReplacement() {
        assertEquals("X", OATextFilter.convert("abcabc", "a", "X", false, true, 0, -1));
    }

    @Test
    void convertNegativeStartPositionCurrentlyThrows() {
        assertThrows(StringIndexOutOfBoundsException.class,
            () -> OATextFilter.convert("abc", "a", "x", false, false, -1, -1));
    }

    @Test
    void removeCharacterHelpersFilterExpectedCharacters() {
        assertEquals("bc", OATextFilter.removeCharacters("abc", "a"));
        assertEquals("aa", OATextFilter.removeOtherCharacters("abcabc", "a"));
        assertEquals("123", OATextFilter.removeNonDigits("a1-b2.c3"));
        assertEquals("12.3", OATextFilter.removeNonDigits("a1-b2.3", true));
        assertEquals("C:\\temp file.txt", OATextFilter.removeNonFileNameChars("C:\\temp<> file.txt"));
    }

    @Test
    void removeEndingCharsDocumentsCurrentNegativeAmountBehavior() {
        assertEquals("ab", OATextFilter.removeEndingChars("abc", 1));
        assertEquals("", OATextFilter.removeEndingChars("abc", 99));
        assertNull(OATextFilter.removeEndingChars(null, 1));
        assertThrows(StringIndexOutOfBoundsException.class, () -> OATextFilter.removeEndingChars("abc", -1));
    }

    @Test
    void removeLeadingAndTrimSpacesUseCurrentContracts() {
        assertEquals("abc", OATextFilter.removeLeading("...abc", '.'));
        assertEquals("..abc", OATextFilter.removeLeading("...abc", '.', 1));
        assertEquals("a b c", OATextFilter.trimSpaces("  a   b c  "));
        assertEquals("", OATextFilter.trimSpaces("   "));
        assertNull(OATextFilter.trimSpaces(null));
    }

    @Test
    void substringIsEndIndexBasedNotLengthBased() {
        assertEquals("bcd", OATextFilter.substring("abcdef", 1, 4));
        assertEquals("cdef", OATextFilter.substring("abcdef", 2, 99));
        assertEquals("", OATextFilter.substring("abcdef", -1, 2));
        assertEquals("", OATextFilter.substring("abcdef", 5, 2));
        assertNull(OATextFilter.substring(null, 0, 1));
    }
}
