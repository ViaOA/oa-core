package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class OATextRemainingEdgeTest {

    @Test
    void utilHexRoundTripAndCurrentInvalidHexBehavior() {
        byte[] bytes = new byte[] { 0x00, 0x0F, 0x10, (byte) 0xFF };

        String hex = OATextUtil.bytesToHex(bytes);

        assertEquals("000F10FF", hex);
        assertArrayEquals(bytes, OATextUtil.hexToBytes(hex));
        assertNull(OATextUtil.hexToBytes(null));
        assertThrows(StringIndexOutOfBoundsException.class, () -> OATextUtil.hexToBytes("ABC"));
        assertArrayEquals(new byte[] { (byte) -17 }, OATextUtil.hexToBytes("GG"));
    }

    @Test
    void utilCreatePropertyPathDocumentsEmptySegmentBehavior() {
        assertEquals("", OATextUtil.createPath((String[]) null));
        assertEquals("Order..Customer", OATextUtil.createPath("Order", "", "Customer"));
        assertEquals("Order:items.active.Customer", OATextUtil.createPath("Order", ":items.active", "Customer"));
        assertEquals("(java.lang.String)Order..Customer", OATextUtil.createPath(String.class, "Order", "", "Customer"));
    }

    @Test
    void utilCreateStringDocumentsBounds() {
        assertEquals("", OATextUtil.createString('x', 0));
        assertEquals("xxx", OATextUtil.createString('x', 3));
        assertThrows(NegativeArraySizeException.class, () -> OATextUtil.createString('x', -1));
    }

    @Test
    void escapeXmlLegalAndIllegalCharacterHelpers() {
        assertFalse(OATextEscape.isLegalXml(null));
        assertTrue(OATextEscape.isLegalXml("abc\tdef"));
        assertFalse(OATextEscape.isLegalXml("a&b"));
        assertFalse(OATextEscape.isLegalXml("a\nb"));
        assertEquals("A<OAXML#1/>B", OATextEscape.encodeIllegalXml("A\u0001B"));
        assertEquals("A\u0001B", OATextEscape.decodeIllegalXml("A<OAXML#1/>B"));
        assertEquals("&lt;OAXML#1/&gt;", OATextEscape.encodeIllegalXml((char) 1, true));
    }

    @Test
    void escapeHighlightUsesCurrentCaseMatchingContract() {
        assertEquals("hello <b>Via</b>OA", OATextEscape.hilite("hello ViaOA", "Via", "<b>", "</b>", false));
        assertEquals("hello <b>Via</b>OA", OATextEscape.hilite("hello ViaOA", "via", "<b>", "</b>", true));
        assertEquals("hello ViaOA", OATextEscape.hilite("hello ViaOA", "missing", "<b>", "</b>", true));
        assertNull(OATextEscape.hilite(null, "x", "<b>", "</b>", true));
    }

    @Test
    void tokenizerParseLineNullEmptyAndSizeEstimateContracts() {
        assertNull(OATextTokenizer.parseLine(null, ',', true));
        assertNull(OATextTokenizer.parseLine("a,b", (char) 0, true));
        assertArrayEquals(new String[0], OATextTokenizer.parseLine("", ',', true));
        assertArrayEquals(new String[] { "a", "b" }, OATextTokenizer.parseLine("a,b", ',', false, 1));
    }

    @Test
    void tokenizerFieldAndFieldAtBoundaryContracts() {
        assertEquals("a", OATextTokenizer.field("a,b,c", ",", 1));
        assertEquals("b", OATextTokenizer.field("a,b,c", ",", 2));
        assertEquals("b,c", OATextTokenizer.field("a,b,c", ",", 2, -1));
        assertEquals("a", OATextTokenizer.fieldAt("a,b,c", ",", 0));
        assertEquals("b,c", OATextTokenizer.fieldAt("a,b,c", ",", 1, -1));
        assertNull(OATextTokenizer.fieldAt("a,b,c", ",", 5));
    }

    @Test
    void lineWrapSetMaxRowsZeroCurrentlyThrowsButDefaultIsUnlimited() {
        OATextLineWrap wrap = new OATextLineWrap(3, "|");

        assertEquals(0, wrap.getMaxRows());
        assertThrows(IllegalArgumentException.class, () -> wrap.setMaxRows(0));
        assertThrows(IllegalArgumentException.class, () -> wrap.withMaxRows(0));
    }

    @Test
    void lineWrapConfigurationSettersAreFluentAndNullSafe() {
        OATextLineWrap wrap = new OATextLineWrap();

        assertSame(wrap, wrap.withMaxWidth(4));
        assertSame(wrap, wrap.withMinSegmentLen(2));
        assertSame(wrap, wrap.withBreakChars(null));
        assertSame(wrap, wrap.withSeparator(null));
        assertEquals(4, wrap.getMaxWidth());
        assertEquals(2, wrap.getMinSegmentLen());
        assertEquals("", wrap.getBreakChars());
        assertEquals("", wrap.getSeparator());
    }

    @Test
    void lineWrapSingleWidthDoesNotEmitBlankRows() {
        List<String> rows = new OATextLineWrap(3, "|").wrap("abc def");

        assertFalse(rows.contains(""), "width=1 wrapping should not emit blank rows");
        assertEquals(Arrays.asList("abc", "def"), rows);
    }

    @Test
    void lineWrapFinalRowTruncationDocumentsCurrentWhitespaceBehavior() {
        List<String> rows = new OATextLineWrap(10, "|").withMaxRows(1).wrap("abc defghijkl");

        assertEquals(1, rows.size());
        assertEquals("abc def...", rows.get(0));
    }
}
