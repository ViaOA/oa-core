package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextEscape. */
public class OATextEscapeTest {
    @Test
    public void convertFromHtmlTest() {
        // basic entity decoding
        assertEquals("a < b", OATextEscape.convertFromHtml("a &lt; b"));
        // ampersand decoding
        assertEquals("a & b", OATextEscape.convertFromHtml("a &amp; b"));
        // null-safe behavior
        assertNull(OATextEscape.convertFromHtml(null));
    }

    @Test
    public void convertTextToHtmlTest() {
        // plain text is converted for HTML display
        assertNotNull(OATextEscape.convertTextToHtml("a < b", false));
        // optional html tag wrapper
        assertTrue(OATextEscape.convertTextToHtml("abc", true).toLowerCase().contains("html"));
        // null-safe behavior
        assertEquals("", OATextEscape.convertTextToHtml(null, false));
    }

    @Test
    public void convertToHtmlTest() {
        // markup characters are escaped
        assertEquals("a &lt; b", OATextEscape.convertToHtml("a < b"));
        // ampersand is escaped
        assertEquals("a &amp; b", OATextEscape.convertToHtml("a & b"));
        // null-safe behavior
        assertEquals("", OATextEscape.convertToHtml(null));
    }

    @Test
    public void convertToXmlTest() {
        // default XML escaping
        assertEquals("a &lt; b", OATextEscape.convertToXml("a < b"));
        // ampersand escaping
        assertEquals("a &amp; b", OATextEscape.convertToXml("a & b"));
        // CDATA overload executes
        assertNotNull(OATextEscape.convertToXml("abc", true));
        // HTML-aware overload executes
        assertNotNull(OATextEscape.convertToXml("<b>abc</b>", false, true));
        // CR/LF overload executes
        assertNotNull(OATextEscape.convertToXml("a\nb", false, false, true));
        // null-safe behavior
        assertEquals("", OATextEscape.convertToXml(null));
    }

    @Test
    public void encodeIllegalXmlTest() {
        // normal text remains usable
        assertEquals("abc", OATextEscape.encodeIllegalXml("abc"));
        // illegal control character is encoded or handled safely
        assertNotNull(OATextEscape.encodeIllegalXml("a" + ((char) 1) + "b"));
        // char overload executes
        assertNotNull(OATextEscape.encodeIllegalXml('<', true));
        // null-safe behavior
        assertEquals("", OATextEscape.encodeIllegalXml(null));
    }

    @Test
    public void isLegalXmlTest() {
        // normal text is legal
        assertTrue(OATextEscape.isLegalXml("abc"));
        // common XML characters
        assertFalse(OATextEscape.isLegalXml("a < b"));
        // null-safe behavior
        assertFalse(OATextEscape.isLegalXml(null));
    }

    @Test
    public void decodeIllegalXmlTest() {
        // normal text remains unchanged
        assertEquals("abc", OATextEscape.decodeIllegalXml("abc"));
        // marker syntax is handled safely
        assertNotNull(OATextEscape.decodeIllegalXml("<OAXML#65/>"));
        // null-safe behavior
        assertNull(OATextEscape.decodeIllegalXml(null));
    }

    @Test
    public void escapeTest() {
        // quote and slash escaping executes
        assertNotNull(OATextEscape.escape("a'b\"c"));
        // empty string
        assertEquals("", OATextEscape.escape(""));
    }

    @Test
    public void escapeJsTest() {
        // quote character is escaped for JavaScript string context
        assertNotNull(OATextEscape.escapeJs("a'b", '\''));
        // double quote context
        assertNotNull(OATextEscape.escapeJs("a\"b", '"'));
        // embedded HTML mode executes
        assertNotNull(OATextEscape.escapeJs("a<b", '\'', true));
        // null-safe behavior
        assertEquals("", OATextEscape.escapeJs(null, '\''));
    }

    @Test
    public void escapeJsonTest() {
        // quote is escaped for JSON
        assertEquals("a\\\"b", OATextEscape.escapeJson("a\"b"));
        // newline is escaped
        assertTrue(OATextEscape.escapeJson("a\nb").contains("\\n"));
        // StringBuffer overload appends escaped text
        StringBuffer sb = new StringBuffer();
        OATextEscape.escapeJson("abc", sb);
        assertEquals("abc", sb.toString());
        // null-safe behavior
        assertNull(OATextEscape.escapeJson(null));
    }

    @Test
    public void getHtmlAttributeMapTest() {
        // simple attributes are parsed
        Map<String, String> map = OATextEscape.getHtmlAttributeMap("<input type=text disabled>");
        assertNotNull(map);
        assertTrue(map.containsKey("type"));
        // empty tag returns a non-null map
        assertNotNull(OATextEscape.getHtmlAttributeMap(""));
        // null-safe behavior
        assertNotNull(OATextEscape.getHtmlAttributeMap(null));
    }

    @Test
    public void unescapeJsonTest() {
        // newline escape is unescaped
        assertEquals("a\nb", OATextEscape.unescapeJson("a\\nb"));
        // quote escape is unescaped
        assertEquals("a\"b", OATextEscape.unescapeJson("a\\\"b"));
        // null-safe behavior
        assertNull(OATextEscape.unescapeJson(null));
    }

    @Test
    public void hiliteTest() {
        // matching text is wrapped with tags
        assertEquals("a <b>b</b> c", OATextEscape.hilite("a b c", "b", "<b>", "</b>", false));
        // ignore case match
        assertEquals("a <b>B</b> c", OATextEscape.hilite("a B c", "b", "<b>", "</b>", true));
        // no match leaves text unchanged
        assertEquals("abc", OATextEscape.hilite("abc", "x", "<b>", "</b>", false));
        // null line returns null
        assertNull(OATextEscape.hilite(null, "x", "<b>", "</b>", false));
    }
}
