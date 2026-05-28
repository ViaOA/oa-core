package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryTokenManagerBasicTest implements OAQueryTokenType {

    private static OAQueryToken next(String query) {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery(query);
        return tm.getNext();
    }

    @Test
    void nullQueryFailsWithClearBoundary() {
        OAQueryTokenManager tm = new OAQueryTokenManager();

        assertThrows(RuntimeException.class, () -> tm.setQuery(null));
    }

    @Test
    void emptyQueryReturnsEof() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("");

        OAQueryToken token = tm.getNext();

        assertEquals(EOF, token.type);
        assertEquals("", token.value);
    }

    @Test
    void whitespaceOnlyQueryReturnsEof() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery(" \t\r\n ");

        OAQueryToken token = tm.getNext();

        assertEquals(EOF, token.type);
        assertEquals("", token.value);
    }

    @Test
    void variablesCanContainLettersDigitsUnderscoreAndDot() {
        OAQueryToken token = next("customer_1.address.city");

        assertEquals(VARIABLE, token.type);
        assertEquals("customer_1.address.city", token.value);
    }

    @Test
    void numbersPreserveSignAndDecimalText() {
        assertToken("-123.45", NUMBER, "-123.45");
        assertToken("+123.45", NUMBER, "123.45");
        assertToken(".75", NUMBER, ".75");
    }

    @Test
    void questionCommaAndParenthesesTokenizeAsStructuralTokens() {
        assertToken("?", QUESTION, "?");
        assertToken(",", COMMA, ",");
        assertToken("(", SEPERATORBEGIN, "(");
        assertToken(")", SEPERATOREND, ")");
    }

    @Test
    void illegalCharacterThrowsVisibleFailure() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("@");

        RuntimeException ex = assertThrows(RuntimeException.class, tm::getNext);
        assertTrue(ex.getMessage().contains("Illegal token"));
    }

    @Test
    void setQueryResetsPositionAndBufferForReuse() {
        OAQueryTokenManager tm = new OAQueryTokenManager();

        tm.setQuery("name");
        assertEquals("name", tm.getNext().value);

        tm.setQuery("age");
        assertEquals("age", tm.getNext().value);
        assertEquals(EOF, tm.getNext().type);
    }

    private static void assertToken(String query, int type, String value) {
        OAQueryToken token = next(query);
        assertEquals(type, token.type, "type for query=" + query);
        assertEquals(value, token.value, "value for query=" + query);
    }
}
