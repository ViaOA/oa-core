package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryOperatorTokenTest implements OAQueryTokenType {

    private static OAQueryToken first(String query) {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery(query);
        return tm.getNext();
    }

    @Test
    void comparisonOperatorsTokenizeCorrectly() {
        assertOperator("=", EQUAL, "=");
        assertOperator("==", EQUAL, "==");
        assertOperator(">", GT, ">");
        assertOperator(">=", GE, ">=");
        assertOperator("<", LT, "<");
        assertOperator("<=", LE, "<=");
        assertOperator("!=", NOTEQUAL, "!=");
    }

    @Test
    void angleBracketNotEqualTokenizesAsSingleNotEqualOperator() {
        OAQueryToken token = first("<>");

        assertEquals(NOTEQUAL, token.type);
        assertEquals("<>", token.value);
        assertTrue(token.isOperator());
    }

    @Test
    void bangWithoutEqualThrows() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("!");

        RuntimeException ex = assertThrows(RuntimeException.class, tm::getNext);
        assertTrue(ex.getMessage().contains("Token '!' not valid"));
    }

    @Test
    void logicalSymbolOperatorsTokenizeCorrectly() {
        assertToken("&", AND, "&");
        assertToken("&&", AND, "&&");
        assertToken("|", OR, "|");
        assertToken("||", OR, "||");
    }

    @Test
    void keywordOperatorsTokenizeCaseInsensitively() {
        assertToken("and", AND, "and");
        assertToken("AND", AND, "AND");
        assertToken("or", OR, "or");
        assertToken("LIKE", LIKE, "LIKE");
        assertToken("notlike", NOTLIKE, "notlike");
        assertToken("IN", IN, "IN");
        assertToken("null", NULL, "null");
    }

    @Test
    void notLikeIsOperator() {
        OAQueryToken token = first("NOTLIKE");

        assertEquals(NOTLIKE, token.type);
        assertTrue(token.isOperator(), "NOTLIKE must be treated as comparison operator");
    }

    @Test
    void allComparisonOperatorsReportIsOperator() {
        int[] types = { OPERATOR, GT, GE, LT, LE, EQUAL, NOTEQUAL, LIKE, NOTLIKE };

        for (int type : types) {
            OAQueryToken token = new OAQueryToken();
            token.type = type;
            assertTrue(token.isOperator(), "type=" + type);
        }
    }

    @Test
    void nonOperatorsReportFalse() {
        int[] types = { EOF, NUMBER, SEPERATORBEGIN, SEPERATOREND, VARIABLE, AND, OR, NULL, STRINGSQ, STRINGDQ, PASSTHRU, QUESTION, FUNCTIONBEGIN, FUNCTIONEND, IN, COMMA };

        for (int type : types) {
            OAQueryToken token = new OAQueryToken();
            token.type = type;
            assertFalse(token.isOperator(), "type=" + type);
        }
    }

    private static void assertOperator(String query, int type, String value) {
        OAQueryToken token = first(query);
        assertEquals(type, token.type, "type for " + query);
        assertEquals(value, token.value, "value for " + query);
        assertTrue(token.isOperator(), "isOperator for " + query);
    }

    private static void assertToken(String query, int type, String value) {
        OAQueryToken token = first(query);
        assertEquals(type, token.type, "type for " + query);
        assertEquals(value, token.value, "value for " + query);
    }
}
