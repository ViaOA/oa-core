package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextTokenizer. */
public class OATextTokenizerTest {
    @Test
    public void countTest() {
        // delimiter count style behavior
        assertEquals(2, OATextTokenizer.count("a,b,c", ","));
        // no delimiter means 0 field
        assertEquals(0, OATextTokenizer.count("abc", ","));
        // null string is safe
        assertEquals(0, OATextTokenizer.count(null, ","));
    }

    @Test
    public void countMatchesTest() {
        // string separator overload
        assertEquals(3, OATextTokenizer.countMatches("a,b,c", ","));
        // char separator overload
        assertEquals(3, OATextTokenizer.countMatches("a,b,c", ','));
        // no match
        assertEquals(1, OATextTokenizer.countMatches("abc", ","));
        // null string is safe
        assertEquals(0, OATextTokenizer.countMatches(null, ","));
    }

    @Test
    public void dcountTest() {
        // string separator overload
        assertEquals(3, OATextTokenizer.dcount("a,b,c", ","));
        // char separator overload
        assertEquals(3, OATextTokenizer.dcount("a,b,c", ','));
        // no delimiter
        assertEquals(1, OATextTokenizer.dcount("abc", ','));
        // null string is safe
        assertEquals(0, OATextTokenizer.dcount(null, ','));
    }

    @Test
    public void fieldTest() {
        // one-based field access
        assertEquals("a", OATextTokenizer.field("a,b,c", ",", 1));
        // one-based field range
        assertEquals("b,c", OATextTokenizer.field("a,b,c", ",", 2, 2));
        // char separator overload
        assertEquals("b", OATextTokenizer.field("a,b,c", ',', 2));
        // char separator range overload
        assertEquals("b,c", OATextTokenizer.field("a,b,c", ',', 2, 2));
        // out of range returns null
        assertNull(OATextTokenizer.field("a,b", ",", 5));
    }

    @Test
    public void fieldAtTest() {
        // zero-based field access
        assertEquals("a", OATextTokenizer.fieldAt("a,b,c", ",", 0));
        // zero-based field range
        assertEquals("b,c", OATextTokenizer.fieldAt("a,b,c", ",", 1, 2));
        // char separator overload
        assertEquals("b", OATextTokenizer.fieldAt("a,b,c", ',', 1));
        // char separator range overload
        assertEquals("b,c", OATextTokenizer.fieldAt("a,b,c", ',', 1, 2));
        // out of range returns null
        assertNull(OATextTokenizer.fieldAt("a,b", ",", 5));
    }

    @Test
    public void maskPasswordTest() {
        // default password word is masked
        assertNotEquals("secret", OATextTokenizer.maskPassword("password", "secret"));
        // custom return mask
        assertEquals("xxxxx", OATextTokenizer.maskPassword("password", "secret", (String) "xxxxx", new String[] {"password"}));
        // custom words overload
//        assertNotEquals("secret", OATextTokenizer.maskPassword("token", "secret", (String) "token"));
        // non-password field is unchanged
        assertEquals("value", OATextTokenizer.maskPassword("name", "value"));
        // case-sensitive overload can require exact case
        assertEquals("secret", OATextTokenizer.maskPassword("Password", "secret", "xxxxx", true, "password"));
    }

    @Test
    public void parseLineTest() {
        // simple CSV line
        assertArrayEquals(new String[] { "a", "b", "c" }, OATextTokenizer.parseLine("a,b,c", ',', false));
        // quoted value with separator
        assertArrayEquals(new String[] { "a,b", "c" }, OATextTokenizer.parseLine("\"a,b\",c", ',', true));
        // size estimate overload executes
        assertArrayEquals(new String[] { "a", "b" }, OATextTokenizer.parseLine("a,b", ',', false, 2));
        // null line returns null
        assertNull(OATextTokenizer.parseLine(null, ',', false));
    }

    @Test
    public void tokenizeTest() {
        // simple tokenization
        String[] vals = OATextTokenizer.tokenize("a,b", ',', false, false, (char) 0, (char) 0, (char) 0);
        assertNotNull(vals);
        assertTrue(vals.length >= 1);
        // include delimiter option executes
        assertNotNull(OATextTokenizer.tokenize("a,b", ',', false, true, (char) 0, (char) 0, (char) 0));
        // quoted/bracketed region option executes
        assertNotNull(OATextTokenizer.tokenize("a,(b,c)", ',', false, false, '(', ')', (char) 0));
    }

    @Test
    public void getCssMapTest() {
        // simple CSS declarations
        Map<String, String> map = OATextTokenizer.getCssMap("color:red;font-size:12px");
        assertNotNull(map);
        assertEquals("red", map.get("color"));
        // empty style returns non-null map
        assertNotNull(OATextTokenizer.getCssMap(""));
        // null style returns non-null map
        assertNotNull(OATextTokenizer.getCssMap(null));
    }

    @Test
    public void csvTest() {
        // first value starts CSV text
    	String s = OATextTokenizer.csv(null, "abc");
        assertEquals("\"abc\"", s);
        // second value is appended with comma
        s = OATextTokenizer.csv("abc", "def");
        assertEquals("abc,\"def\"", s);
        // value with comma is quoted
        s = OATextTokenizer.csv(null, "a,b");
        assertTrue(s.startsWith("\""));
        // null value appends empty field safely
        assertNotNull(OATextTokenizer.csv("abc", null));
    }
}
