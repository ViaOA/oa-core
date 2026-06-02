package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for OATextGrammar. */
public class OATextGrammarTest {
    @Test
    public void getDisplayNameTest() {
        // camel case is split
        assertEquals("First Name", OATextGrammar.getDisplayName("firstName"));
        // underscores become spaces
        assertEquals("First Name", OATextGrammar.getDisplayName("first_name"));
        // acronym boundary is handled
        assertEquals("URL Value", OATextGrammar.getDisplayName("URLValue"));
        // null returns empty string
        assertEquals("", OATextGrammar.getDisplayName(null));
    }

    @Test
    public void createDisplayNameTest() {
        // delegates to display name behavior
        assertEquals(OATextGrammar.getDisplayName("firstName"), OATextGrammar.createDisplayName("firstName"));
        // null returns empty string
        assertEquals("", OATextGrammar.createDisplayName(null));
    }

    @Test
    public void convertToDisplayNameTest() {
        // delegates to display name behavior
        assertEquals(OATextGrammar.getDisplayName("firstName"), OATextGrammar.convertToDisplayName("firstName"));
        // null returns empty string
        assertEquals("", OATextGrammar.convertToDisplayName(null));
    }

    @Test
    public void makeSingularTest() {
        // simple plural
        assertEquals("car", OATextGrammar.makeSingular("cars"));
        // ies plural
        assertEquals("city", OATextGrammar.makeSingular("cities"));
        // already singular remains usable
        assertNotNull(OATextGrammar.makeSingular("car"));
        // null-safe behavior
        assertEquals("", OATextGrammar.makeSingular(null));
    }

    @Test
    public void getAorAnTest() {
        // vowel sound simple case
        assertEquals("an", OATextGrammar.getAorAn("apple"));
        // consonant sound simple case
        assertEquals("a", OATextGrammar.getAorAn("car"));
        // blank input is safe
        assertNotNull(OATextGrammar.getAorAn(""));
        // null input is safe
        assertNotNull(OATextGrammar.getAorAn(null));
    }

    @Test
    public void makePluralTest() {
        // simple plural
        assertEquals("cars", OATextGrammar.makePlural("car"));
        // y ending
        assertEquals("cities", OATextGrammar.makePlural("city"));
        // s-like ending executes
        assertNotNull(OATextGrammar.makePlural("box"));
        // null-safe behavior
        assertEquals("", OATextGrammar.makePlural(null));
    }

    @Test
    public void makePossessiveTest() {
        // normal possessive
        assertEquals("car's", OATextGrammar.makePossessive("car"));
        // s-ending possessive
        assertEquals("class'", OATextGrammar.makePossessive("class"));
        // null-safe behavior
        assertEquals("", OATextGrammar.makePossessive(null));
    }

    @Test
    public void getPossessiveTest() {
        // delegates to possessive behavior
        assertEquals(OATextGrammar.makePossessive("car"), OATextGrammar.getPossessive("car"));
        // null-safe behavior
        assertEquals("", OATextGrammar.getPossessive(null));
    }

    @Test
    public void getTitleTest() {
        // title case simple sentence
        assertNotNull(OATextGrammar.getTitle("hello world"));
        // basedOn overload executes
        assertNotNull(OATextGrammar.getTitle("hello world", "Hello World"));
        // empty string
        assertNotNull(OATextGrammar.getTitle(""));
        // null-safe behavior
        assertNotNull(OATextGrammar.getTitle(null));
    }

    @Test
    public void getShortNameTest() {
        // short name within max length
        assertNotNull(OATextGrammar.getShortName("First Middle Last", 5));
        // max length boundary
        assertTrue(OATextGrammar.getShortName("First Middle Last", 3).length() <= 3);
        // null input is safe
        assertNotNull(OATextGrammar.getShortName(null, 3));
    }
}
