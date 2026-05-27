package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class OATextUtilTest {

    @Test
    void appendPrependAndConcatDocumentSeparatorSemantics() {
        assertEquals("hello world", OATextUtil.append("hello", "world"));
        assertEquals("hello/world", OATextUtil.append("hello", "world", "/"));
        assertEquals("prefix/hello", OATextUtil.prepend("hello", "prefix", "/"));

        assertEquals("", OATextUtil.concat(null, null));
        assertEquals("a b", OATextUtil.concat("a", "b"));
        assertEquals("a,b", OATextUtil.concat("a", "b", ","));
        assertEquals("anullb", OATextUtil.concat("a", "b", null));
    }

    @Test
    void colorToHexCurrentlyIncludesAlphaChannel() {
        assertNull(OATextUtil.colorToHex(null));
        assertEquals("#010203FF", OATextUtil.colorToHex(new Color(1, 2, 3)));
        assertEquals("#01020304", OATextUtil.colorToHex(new Color(1, 2, 3, 4)));
    }

    @Test
    void makeJavaIdentifierReplacesInvalidPartsButCurrentlyAllowsInvalidFirstCharacter() {
        assertNull(OATextUtil.makeJavaIdentifier(null));
        assertEquals("abc", OATextUtil.makeJavaIdentifier("abc"));
        assertEquals("a_b", OATextUtil.makeJavaIdentifier("a-b"));
        assertEquals("1abc", OATextUtil.makeJavaIdentifier("1abc"));
    }

    @Test
    void getBeginAndEndHandleNullBoundsAndCurrentCharBasedUnicodeBehavior() {
        assertNull(OATextUtil.getBegin(null, 1));
        assertNull(OATextUtil.getEnd(null, 1));
        assertEquals("", OATextUtil.getBegin("abc", 0));
        assertEquals("", OATextUtil.getEnd("abc", 0));
        assertEquals("ab", OATextUtil.getBegin("abc", 2));
        assertEquals("bc", OATextUtil.getEnd("abc", 2));
        assertEquals("abc", OATextUtil.getBegin("abc", 99));
        assertEquals("abc", OATextUtil.getEnd("abc", 99));

        String highSurrogateOnly = OATextUtil.getBegin("😀x", 1);
        assertEquals(1, highSurrogateOnly.length());
        assertTrue(Character.isHighSurrogate(highSurrogateOnly.charAt(0)));
    }

    @Test
    void parseIntParsesFirstNumericRunAndDocumentsOverflowBehavior() {
        assertEquals(0, OATextUtil.parseInt(null));
        assertEquals(0, OATextUtil.parseInt("abc"));
        assertEquals(123, OATextUtil.parseInt("abc123def456"));
        assertEquals(-123, OATextUtil.parseInt("abc-123def"));
        assertEquals(Integer.MIN_VALUE, OATextUtil.parseInt("2147483648"));
    }

    @Test
    void convertToLikeSearchReplacesStarsAndAddsTrailingWildcard() {
        assertNull(OATextUtil.convertToLikeSearch(null));
        assertEquals("abc%", OATextUtil.convertToLikeSearch("abc"));
        assertEquals("ab%c", OATextUtil.convertToLikeSearch("ab*c"));
        assertEquals("ab%c%", OATextUtil.convertToLikeSearch("ab*c*"));
    }

    @Test
    void verticalNumberLinesProduceTwoRows() {
        String value = OATextUtil.getVerticalNumberLines(0, 12);

        assertNotNull(value);
        assertTrue(value.contains("\n"));
        assertTrue(value.length() >= 13);
    }
}
