package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

class OATextSanitizeTest {

    @Test
    void defaultStringAndAliasesReturnNonNullValues() {
        assertEquals("", OATextSanitize.defaultString(null));
        assertEquals("fallback", OATextSanitize.defaultString(null, "fallback"));
        assertEquals("abc", OATextSanitize.defaultString("abc", "fallback"));

        assertEquals("", OATextSanitize.notNull(null));
        assertEquals("fallback", OATextSanitize.notNull(null, "fallback"));
        assertEquals("", OATextSanitize.nonNull(null));
        assertEquals("fallback", OATextSanitize.nonNull(null, "fallback"));
        assertEquals("", OATextSanitize.toNonNull(null));
        assertEquals("fallback", OATextSanitize.toNonNull(null, "fallback"));
        assertEquals("", OATextSanitize.getNonNull(null));
        assertEquals("fallback", OATextSanitize.getNonNull(null, "fallback"));
        assertEquals("", OATextSanitize.convertToNonNull(null));
        assertEquals("fallback", OATextSanitize.convertToNonNull(null, "fallback"));
    }

    @Test
    void toStringConvertsNullAndObjectsToSafeText() {
        assertEquals("", OATextSanitize.toString(null));
        assertEquals("abc", OATextSanitize.toString("abc"));
        assertEquals("123", OATextSanitize.toString(123));
    }

    @Test
    void isEmptyDefaultDoesNotTrimButExplicitTrimDoes() {
        assertTrue(OATextSanitize.isEmpty(null));
        assertTrue(OATextSanitize.isEmpty(""));
        assertFalse(OATextSanitize.isEmpty(" "));
        assertTrue(OATextSanitize.isEmpty(" ", true));
        assertFalse(OATextSanitize.isEmpty("x", true));
    }

    @Test
    void isEmptyDelegatesToConverterForContainers() {
        assertTrue(OATextSanitize.isEmpty(new Object[0]));
        assertFalse(OATextSanitize.isEmpty(new Object[] { "x" }));

        assertTrue(OATextSanitize.isEmpty(Collections.emptyList()));
        assertFalse(OATextSanitize.isEmpty(Collections.singletonList("x")));

        assertTrue(OATextSanitize.isEmpty(new HashMap<>()));
    }

    @Test
    void notEmptyAliasesAreLogicalNegationOfDefaultIsEmpty() {
        assertFalse(OATextSanitize.notEmpty(null));
        assertFalse(OATextSanitize.isNotEmpty(""));
        assertTrue(OATextSanitize.notEmpty(" "));
        assertTrue(OATextSanitize.isNotEmpty("x"));
    }
}
