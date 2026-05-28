package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryLiteralTokenTest implements OAQueryTokenType {

    private static OAQueryToken first(String query) {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery(query);
        return tm.getNext();
    }

    @Test
    void singleQuotedStringPreservesInnerText() {
        OAQueryToken token = first("'Smith'");

        assertEquals(STRINGSQ, token.type);
        assertEquals("Smith", token.value);
    }

    @Test
    void doubleQuotedStringPreservesInnerText() {
        OAQueryToken token = first("\"Smith\"");

        assertEquals(STRINGDQ, token.type);
        assertEquals("Smith", token.value);
    }

    @Test
    void escapedStringPreservesInnerText() {
        OAQueryToken token = first("{Smith}");

        assertEquals(STRINGESC, token.type);
        assertEquals("Smith", token.value);
    }

    @Test
    void backslashEscapedQuoteIsPreservedInsideQuotedString() {
        OAQueryToken token = first("'a\\'b'");

        assertEquals(STRINGSQ, token.type);
        assertEquals("a'b", token.value);
    }

    @Test
    void doubledSingleQuoteProducesSeparateStringTokensForSqlStyleLiteral() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("'CT13''6'");

        OAQueryToken t1 = tm.getNext();
        OAQueryToken t2 = tm.getNext();

        assertEquals(STRINGSQ, t1.type);
        assertEquals("CT13", t1.value);
        assertEquals(STRINGSQ, t2.type);
        assertEquals("6", t2.value);
    }

    @Test
    void unterminatedSingleQuotedStringThrows() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("'abc");

        assertThrows(RuntimeException.class, tm::getNext);
    }

    @Test
    void unterminatedDoubleQuotedStringThrows() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("\"abc");

        assertThrows(RuntimeException.class, tm::getNext);
    }

    @Test
    void unterminatedEscapedStringThrows() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("{abc");

        assertThrows(RuntimeException.class, tm::getNext);
    }

    @Test
    void literalFollowedByIdentifierLeavesIdentifierForNextToken() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("'abc' name");

        OAQueryToken t1 = tm.getNext();
        OAQueryToken t2 = tm.getNext();

        assertEquals(STRINGSQ, t1.type);
        assertEquals("abc", t1.value);
        assertEquals(VARIABLE, t2.type);
        assertEquals("name", t2.value);
    }
}
