package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextUtil. */
public class OATextUtilTest {
    @Test
    public void appendTest() {
        // append with default space separator
        assertEquals("a b", OATextUtil.append("a", "b"));
        // append with custom separator
        assertEquals("a/b", OATextUtil.append("a", "b", "/"));
        // null original returns append value
        assertEquals("b", OATextUtil.append(null, "b"));
        // null append leaves original usable
        assertNotNull(OATextUtil.append("a", null));
    }

    @Test
    public void prependTest() {
        // prepend with custom separator
        assertEquals("a/b", OATextUtil.prepend("b", "a", "/"));
        // null original returns prepend value
        assertEquals("a", OATextUtil.prepend(null, "a", "/"));
        // null prepend leaves original usable
        assertNotNull(OATextUtil.prepend("b", null, "/"));
    }

    @Test
    public void concatTest() {
        // simple concat uses default behavior
        assertEquals("a b", OATextUtil.concat("a", "b"));
        // object overload
        assertEquals("a/123", OATextUtil.concat("a", 123, "/"));
        // string separator overload
        assertEquals("a/b", OATextUtil.concat("a", "b", "/"));
        // force separator overload
        assertNotNull(OATextUtil.concat("a", "", "/", true));
        // null base is safe
        assertNotNull(OATextUtil.concat(null, "b"));
    }

    @Test
    public void colorToHexTest() {
        // color converts to hex text
        assertNotNull(OATextUtil.colorToHex(new Color(1, 2, 3)));
        // result starts with #
        assertTrue(OATextUtil.colorToHex(new Color(1, 2, 3)).startsWith("#"));
        // null color is safe
        assertNull(OATextUtil.colorToHex(null));
    }

    @Test
    public void makeJavaIdentifierTest() {
        // spaces converted with an underscore
        assertEquals("first_name", OATextUtil.makeJavaIdentifier("first name"));
        // invalid punctuation removed or converted
        assertNotNull(OATextUtil.makeJavaIdentifier("first-name"));
        // empty string is safe
        assertNotNull(OATextUtil.makeJavaIdentifier(""));
        // null input
        assertNull(OATextUtil.makeJavaIdentifier(null));
    }

    @Test
    public void getEndTest() {
        // last characters
        assertEquals("cd", OATextUtil.getEnd("abcd", 2));
        // length beyond value returns original
        assertEquals("abcd", OATextUtil.getEnd("abcd", 10));
        // zero length returns empty or safe value
        assertNotNull(OATextUtil.getEnd("abcd", 0));
        // null input
        assertNull(OATextUtil.getEnd(null, 2));
    }

    @Test
    public void getLastTest() {
        // alias for end behavior
        assertEquals(OATextUtil.getEnd("abcd", 2), OATextUtil.getLast("abcd", 2));
        // length beyond value returns original
        assertEquals("abcd", OATextUtil.getLast("abcd", 10));
        // null input
        assertNull(OATextUtil.getLast(null, 2));
    }

    @Test
    public void getBeginTest() {
        // first characters
        assertEquals("ab", OATextUtil.getBegin("abcd", 2));
        // length beyond value returns original
        assertEquals("abcd", OATextUtil.getBegin("abcd", 10));
        // zero length returns empty or safe value
        assertNotNull(OATextUtil.getBegin("abcd", 0));
        // null input
        assertNull(OATextUtil.getBegin(null, 2));
    }

    @Test
    public void getFirstTest() {
        // alias for begin behavior
        assertEquals(OATextUtil.getBegin("abcd", 2), OATextUtil.getFirst("abcd", 2));
        // length beyond value returns original
        assertEquals("abcd", OATextUtil.getFirst("abcd", 10));
        // null input
        assertNull(OATextUtil.getFirst(null, 2));
    }

    @Test
    public void parseIntTest() {
        // simple integer
        assertEquals(123, OATextUtil.parseInt("123"));
        // numeric run with text
        assertEquals(123, OATextUtil.parseInt("123abc"));
        // Parses the first contiguous numeric run
        assertEquals(123, OATextUtil.parseInt("abc123"));
        // null input
        assertEquals(0, OATextUtil.parseInt(null));
    }

    @Test
    public void convertToLikeSearchTest() {
        // star converted to SQL wildcard style
        assertEquals("abc%", OATextUtil.convertToLikeSearch("abc*"));
        // missing wildcard gets trailing wildcard
        assertTrue(OATextUtil.convertToLikeSearch("abc").endsWith("%"));
        // null input is safe
        assertNull(OATextUtil.convertToLikeSearch(null));
    }

    @Test
    public void getVerticalNumberLinesTest() {
        // normal range returns text
        assertNotNull(OATextUtil.getVerticalNumberLines(1, 5));
        // single value range
        assertNotNull(OATextUtil.getVerticalNumberLines(1, 1));
        // reversed range is safe
        assertNotNull(OATextUtil.getVerticalNumberLines(5, 1));
    }

    @Test
    public void getVerticalHexTest() {
        // byte array converts to vertical hex text
        assertNotNull(OATextUtil.getVerticalHex(new byte[] { 1, 2, 3 }));
        // empty array
        assertNotNull(OATextUtil.getVerticalHex(new byte[0]));
        // null array is safe
        assertNull(OATextUtil.getVerticalHex(null));
    }

    @Test
    public void bytesToHexTest() {
        // bytes convert to uppercase hex
        assertEquals("01020F", OATextUtil.bytesToHex(new byte[] { 1, 2, 15 }));
        // empty array converts to empty string
        assertEquals("", OATextUtil.bytesToHex(new byte[0]));
        // null array is safe
        assertNull(OATextUtil.bytesToHex(null));
    }

    @Test
    public void hexToBytesTest() {
        // hex converts to bytes
        assertArrayEquals(new byte[] { 1, 2, 15 }, OATextUtil.hexToBytes("01020F"));
        // lowercase hex is accepted
        assertArrayEquals(new byte[] { 10 }, OATextUtil.hexToBytes("0a"));
        // empty string converts safely
        assertNotNull(OATextUtil.hexToBytes(""));
    }

    @Test
    public void createStringTest() {
        // repeat character
        assertEquals("xxx", OATextUtil.createString('x', 3));
        // zero length
        assertEquals("", OATextUtil.createString('x', 0));
        // negative length is rejected by StringBuilder sizing
        assertThrows(NegativeArraySizeException.class, () -> OATextUtil.createString('x', -1));
    }

    @Test
    public void createPropertyPathTest() {
        // simple segments
        assertEquals("Order.Customer", OATextUtil.createPropertyPath("Order", "Customer"));
        // null segment is skipped
        assertEquals("Order.Customer", OATextUtil.createPropertyPath("Order", null, "Customer"));
        // class overload includes class-related path behavior
        assertNotNull(OATextUtil.createPropertyPath(String.class, "bytes"));
    }
}
