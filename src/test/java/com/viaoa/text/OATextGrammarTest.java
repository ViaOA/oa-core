package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OATextGrammarTest {

    @Test
    void displayNameHandlesCamelCaseUnderscoresAndAcronyms() {
        assertEquals("", OATextGrammar.getDisplayName(null));
        assertEquals("", OATextGrammar.getDisplayName(""));
        assertEquals("First Name", OATextGrammar.getDisplayName("firstName"));
        assertEquals("First Name", OATextGrammar.getDisplayName("first_name"));
        assertEquals("URL Value", OATextGrammar.getDisplayName("URLValue"));
        assertEquals("US America", OATextGrammar.getDisplayName("USAmerica"));

        assertEquals(OATextGrammar.getDisplayName("firstName"), OATextGrammar.createDisplayName("firstName"));
        assertEquals(OATextGrammar.getDisplayName("firstName"), OATextGrammar.convertToDisplayName("firstName"));
    }

    @Test
    void singularAndPluralRulesDocumentCurrentOABehavior() {
        assertEquals("", OATextGrammar.makeSingular(null));
        assertEquals("box", OATextGrammar.makeSingular("boxes"));
        assertEquals("company", OATextGrammar.makeSingular("companies"));
        assertEquals("cat", OATextGrammar.makeSingular("cats"));
        assertEquals("class", OATextGrammar.makeSingular("class"));

        assertEquals("", OATextGrammar.makePlural(null));
        assertEquals("boxes", OATextGrammar.makePlural("box"));
        assertEquals("companies", OATextGrammar.makePlural("company"));
        assertEquals("cats", OATextGrammar.makePlural("cat"));
        assertEquals("classes", OATextGrammar.makePlural("class"));
        assertEquals("buzzs", OATextGrammar.makePlural("buzz"));
    }

    @Test
    void articlesAndPossessivesUseSimpleCurrentRules() {
        assertEquals("a", OATextGrammar.getAorAn(null));
        assertEquals("a", OATextGrammar.getAorAn(""));
        assertEquals("an", OATextGrammar.getAorAn("apple"));
        assertEquals("a", OATextGrammar.getAorAn("banana"));

        assertEquals("", OATextGrammar.makePossessive(null));
        assertEquals("car's", OATextGrammar.makePossessive("car"));
        assertEquals("class'", OATextGrammar.makePossessive("class"));
        assertEquals("CAR'S", OATextGrammar.makePossessive("CAR"));

        assertEquals("car's", OATextGrammar.getPossessive("car"));
        assertEquals("class'", OATextGrammar.getPossessive("class"));
    }

    @Test
    void titleCaseAndShortNameAreDeterministic() {
        assertEquals("", OATextGrammar.getTitle(null));
        assertEquals("", OATextGrammar.getTitle(""));
        assertEquals("Hello World", OATextGrammar.getTitle("hello world"));
        assertEquals("Hello World", OATextGrammar.getTitle("HELLO WORLD"));

        assertEquals("", OATextGrammar.getShortName(null, 4));
        assertEquals("", OATextGrammar.getShortName("", 4));
        assertEquals("oapos", OATextGrammar.getShortName("OAPOS", 5));
        assertEquals("ov", OATextGrammar.getShortName("OrderValue", 4));
    }
}
