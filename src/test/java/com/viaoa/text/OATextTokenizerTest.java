package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class OATextTokenizerTest {

    @Test
    void countAndCountMatchesHaveDifferentContracts() {
        assertEquals(2, OATextTokenizer.count("a,b,c", ","));
        assertEquals(3, OATextTokenizer.countMatches("a,b,c", ","));
        assertEquals(3, OATextTokenizer.dcount("a,b,c", ","));
    }

    @Test
    void fieldAtUsesZeroBasedIndexing() {
        assertEquals("a", OATextTokenizer.fieldAt("a,b,c", ",", 0));
        assertEquals("b", OATextTokenizer.fieldAt("a,b,c", ",", 1));
        assertEquals("b,c", OATextTokenizer.fieldAt("a,b,c", ",", 1, -1));
        assertNull(OATextTokenizer.fieldAt("a,b,c", ",", 5));
    }

    @Test
    void parseLineHandlesBasicQuotedValues() {
        assertArrayEquals(new String[] { "a", "b,c", "d" }, OATextTokenizer.parseLine("a,\"b,c\",d", ',', true));
        assertArrayEquals(new String[] { "a", "b", "" }, OATextTokenizer.parseLine("a,b,", ',', true));
    }

    @Test
    void parseLineCurrentlyMishandlesWhitespaceAfterClosingQuote() {
        assertArrayEquals(new String[] { "a ,b" }, OATextTokenizer.parseLine("\"a\" ,b", ',', true));
    }

    @Test
    void parseLineCurrentlyDoesNotCollapseDoubledCsvQuotes() {
        assertArrayEquals(new String[] { "a\"\"b" }, OATextTokenizer.parseLine("\"a\"\"b\"", ',', true));
    }

    @Test
    void csvQuotesStringsAndDoublesInternalQuotes() {
        assertEquals("\"abc\"", OATextTokenizer.csv(null, "abc"));
        assertEquals("\"a,b\"", OATextTokenizer.csv(null, "a,b"));
        assertEquals("\"a\"\"b\"", OATextTokenizer.csv(null, "a\"b"));
    }

    @Test
    void csvCurrentlyDoesNotWrapValueThatStartsWithQuoteAfterDoubling() {
        assertEquals("\"\"abc", OATextTokenizer.csv(null, "\"abc"));
    }

    @Test
    void passwordMaskingDocumentsCurrentCaseSensitiveDefaultBug() {
        assertEquals("*****", OATextTokenizer.maskPassword("password", "secret"));
        assertEquals("secret", OATextTokenizer.maskPassword("Password", "secret"));
        assertEquals("*****", OATextTokenizer.maskPassword("Password", "secret", "*****", true, "password"));
    }

    @Test
    void cssMapDocumentsCurrentWhitespaceAndQuoteBehavior() {
        Map<String, String> map = OATextTokenizer.getCssMap("color: red; font-family: 'Times New Roman';");

        assertEquals("red", map.get("color"));
        assertTrue(map.containsKey(" font-family") || map.containsKey("font-family"));
    }

    @Test
    void tokenizeSupportsBasicNameValueAttributes() {
        String[] values = OATextTokenizer.tokenize("<input type=\"text\">", '=', true, true, '<', '>', (char) 0);
        assertNotNull(values);
        assertTrue(values.length >= 3);
    }
}
