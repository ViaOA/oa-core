package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class OATextEscapeTest {

    @Test
    void convertFromHtmlDecodesKnownEntitiesInCurrentOrder() {
        assertNull(OATextEscape.convertFromHtml(null));
        assertEquals("&", OATextEscape.convertFromHtml("&amp;"));
        assertEquals("\"'<>\u0026", OATextEscape.convertFromHtml("&quot;&apos;&lt;&gt;&amp;"));
    }

    @Test
    void convertFromHtmlCurrentlyDoubleDecodesAmpLt() {
        assertEquals("<", OATextEscape.convertFromHtml("&amp;lt;"));
    }

    @Test
    void convertTextToHtmlEscapesPlainTextButTreatsAnglePairAsExistingHtml() {
        assertEquals("a &amp; b", OATextEscape.convertTextToHtml("a & b", false));
        assertEquals("<html>a &amp; b</html>", OATextEscape.convertTextToHtml("a & b", true));

        // Current behavior: any value containing both '<' and '>' is treated as already-HTML.
        assertEquals("1 < 2 > 0", OATextEscape.convertTextToHtml("1 < 2 > 0", false));
    }

    @Test
    void convertToXmlEscapesMarkupOutsideCdata() {
        assertEquals("a &amp; &lt;b&gt; &quot;x&quot; &apos;y&apos;", 
            OATextEscape.convertToXml("a & <b> \"x\" 'y'", false));
    }

    @Test
    void cdataModeCurrentlyLeavesTerminatorUnsafe() {
        assertEquals("abc]]>def", OATextEscape.convertToXml("abc]]>def", true));
    }

    @Test
    void escapeJsonDocumentsApostropheBackslashAndControlEscapes() {
        StringBuffer sb = new StringBuffer();
        OATextEscape.escapeJson("O'Brien\nC:\\temp", sb);

        assertEquals("O\\'Brien\\nC:\\\\temp", sb.toString());
    }

    @Test
    void unescapeJsonCurrentlyConvertsLiteralEscapedBackslashNIntoNewline() {
        assertEquals("\n", OATextEscape.unescapeJson("\\\\n"));
    }

    @Test
    void escapeThrowsOnNullButEscapeJsReturnsBlank() {
        assertThrows(NullPointerException.class, () -> OATextEscape.escape(null));
        assertEquals("", OATextEscape.escapeJs(null, '"'));
    }

    @Test
    void escapeJsEmbeddedHtmlCurrentlyLeavesBackslashSingle() {
        assertEquals("C:\\temp", OATextEscape.escapeJs("C:\\temp", '"', true));
        assertEquals("C:\\\\temp", OATextEscape.escapeJs("C:\\temp", '"', false));
    }

    @Test
    void htmlAttributeMapDocumentsCurrentQuoteRetention() {
        Map<String, String> map = OATextEscape.getHtmlAttributeMap("<input type=\"text\" data-x='a b' disabled>");
        assertEquals("\"text\"", map.get("type"));
        assertEquals("'a b'", map.get("data-x"));
        assertTrue(map.containsKey("disabled"));
    }

    @Test
    void illegalXmlMarkersAreDecodedEvenForLiteralLookingText() {
        assertEquals("A", OATextEscape.decodeIllegalXml("<OAXML#65/>"));
        assertFalse(OATextEscape.isLegalXml("<"));
        assertFalse(OATextEscape.isLegalXml("\u0001"));

        // Current behavior: unpaired surrogate is treated as legal.
        assertTrue(OATextEscape.isLegalXml("\uD800"));
    }
}
