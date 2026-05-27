package com.viaoa.lang;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class OAStringTest {

    @ParameterizedTest
    @NullAndEmptySource
    void isEmptyTreatsNullAndEmptyStringAsEmpty(String value) {
        assertTrue(OAString.isEmpty(value));
        assertFalse(OAString.isNotEmpty(value));
    }

    @Test
    void isEmptyCanSkipTrimmingWhitespace() {
        assertFalse(OAString.isEmpty("   ", false));
        assertTrue(OAString.isEmpty("   ", true));
    }

    @Test
    void nullAndBlankEqualityVariantsAreExplicit() {
        assertFalse(OAString.isEqual(null, ""));
        assertTrue(OAString.isEqual(null, "", false, true));
        assertTrue(OAString.isEqualNullEqualsBlank(null, ""));
        assertFalse(OAString.isNotEqualNullEqualsBlank(null, ""));
    }

    @Test
    void nonNullHelpersUseEmptyStringOrSuppliedDefault() {
        assertEquals("", OAString.toNonNull(null));
        assertEquals("fallback", OAString.toNonNull(null, "fallback"));
        assertEquals("value", OAString.defaultString("value", "fallback"));
        assertEquals("fallback", OAString.notEmpty("", "fallback"));
        assertEquals("   ", OAString.notEmpty("   ", "fallback"));
    }

    @ParameterizedTest
    @CsvSource({
            "abcABC, bc, 1, false, true",
            "abcABC, BC, 0, false, true",
            "abcABC, BC, 0, true, true",
            "abcABC, a, 1, true, true"
    })
    void containsHonorsStartPositionAndCaseFlag(String value, String search, int start, boolean ignoreCase, boolean expected) {
        assertEquals(expected, OAString.contains(value, search, start, ignoreCase));
    }

    @Test
    void parseLineKeepsQuotedSeparatorTogether() {
        assertArrayEquals(new String[] { "alpha", "bravo,charlie", "delta" }, OAString.parseLine("alpha,\"bravo,charlie\",delta", ',', true));
    }

    @Test
    void cssMapParsesCompactDeclarations() {
        Map<String, String> map = OAString.getCssMap("color:red;font-size:12px;");

        assertEquals("red", map.get("color"));
        assertEquals("12px", map.get("font-size"));
    }

    @Test
    void substringHandlesOutOfRangePositions() {
        assertNull(OAString.substring(null, 1));
        assertEquals("cde", OAString.substring("abcde", 2));
        assertEquals("", OAString.substring("abcde", 99));
        assertEquals("bc", OAString.substring("abcde", 1, 3));
    }

    @Test
    void prefixAndAppendIfMissingRespectIgnoreCase() {
        assertEquals("pre-value", OAString.prefixIfMissing("value", "pre-"));
        assertEquals("PreValue", OAString.prefixIfMissing("PreValue", "pre", true));
        assertEquals("value.txt", OAString.appendIfMissing("value", ".txt"));
        assertEquals("value.TXT", OAString.appendIfMissing("value.TXT", ".txt", true));
    }

    @Test
    void byteHexRoundTripIsUppercaseAndReversible() {
        byte[] bytes = new byte[] { 0, 15, 16, -1 };

        String hex = OAString.bytesToHex(bytes);

        assertEquals("000F10FF", hex);
        assertArrayEquals(bytes, OAString.hexToBytes(hex));
    }

    @Test
    void createPropertyPathCurrentlyKeepsEmptySegmentsAndSkipsNullSegments() {
        assertEquals("customer..address.city", OAString.createPropertyPath("customer", "", null, "address.city"));
        assertEquals("(java.lang.String)length", OAString.createPropertyPath(String.class, "length"));
    }

    @Test
    void getClassNameIsNullSafe() {
        assertNull(OAString.getClassName(null));
        assertEquals("String", OAString.getClassName(String.class));
    }
}
