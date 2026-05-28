package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Vector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class OAQueryTokenizerBasicParseTest implements OAQueryTokenType {

    @Test
    void simpleEqualityParsesToVariableOperatorLiteral() {
        Vector<OAQueryToken> tokens = tokens("name = 'Bob'");

        assertTypes(tokens, VARIABLE, EQUAL, STRINGSQ);
        assertValues(tokens, "name", "=", "Bob");
    }

    @Test
    void numericComparisonParses() {
        Vector<OAQueryToken> tokens = tokens("age >= 18");

        assertTypes(tokens, VARIABLE, GE, NUMBER);
        assertValues(tokens, "age", ">=", "18");
    }

    @Test
    void likeAndNotLikeParseAsComparisonOperators() {
        assertTypes(tokens("name LIKE 'B%'"), VARIABLE, LIKE, STRINGSQ);
        assertTypes(tokens("name NOTLIKE 'B%'"), VARIABLE, NOTLIKE, STRINGSQ);
    }

    @Test
    void notEqualAliasesParseToNotEqual() {
        assertTypes(tokens("status != 'X'"), VARIABLE, NOTEQUAL, STRINGSQ);
        assertTypes(tokens("status <> 'X'"), VARIABLE, NOTEQUAL, STRINGSQ);
    }

    @Test
    void andOrExpressionParsesEntireExpression() {
        Vector<OAQueryToken> tokens = tokens("name = 'Bob' and age >= 18 or status = 'A'");

        assertEquals("name = Bob and age >= 18 or status = A", values(tokens));
        assertTrue(tokens.stream().anyMatch(t -> t.type == AND));
        assertTrue(tokens.stream().anyMatch(t -> t.type == OR));
    }

    @Test
    void parenthesizedExpressionParsesWithSeparators() {
        Vector<OAQueryToken> tokens = tokens("(name = 'Bob' or name = 'Sue') and active = true");

        assertEquals(SEPERATORBEGIN, tokens.get(0).type);
        assertTrue(tokens.stream().anyMatch(t -> t.type == SEPERATOREND));
        assertTrue(tokens.stream().anyMatch(t -> t.type == AND));
    }

    @Test
    void functionCallParsesWithFunctionMarkers() {
        Vector<OAQueryToken> tokens = tokens("lower(name) = 'bob'");

        assertTypes(tokens, VARIABLE, FUNCTIONBEGIN, VARIABLE, FUNCTIONEND, EQUAL, STRINGSQ);
    }

    @Test
    void queryFacadeDelegatesToTokenizer() {
        OAQuery query = new OAQuery();

        Vector<OAQueryToken> tokens = query.parse("name = 'Bob'");

        assertTypes(tokens, VARIABLE, EQUAL, STRINGSQ);
    }

    @Test
    void tokenizerReuseDoesNotLeakPriorState() {
        OAQueryTokenizer qt = new OAQueryTokenizer();

        assertTypes(qt.convertToTokens("name = 'Bob'"), VARIABLE, EQUAL, STRINGSQ);
        assertTypes(qt.convertToTokens("age >= 18"), VARIABLE, GE, NUMBER);
    }

    private static Vector<OAQueryToken> tokens(String query) {
        return new OAQueryTokenizer().convertToTokens(query);
    }

    private static void assertTypes(Vector<OAQueryToken> tokens, int... types) {
        assertEquals(types.length, tokens.size(), "tokens=" + values(tokens));
        for (int i = 0; i < types.length; i++) {
            assertEquals(types[i], tokens.get(i).type, "index=" + i + ", tokens=" + values(tokens));
        }
    }

    private static void assertValues(Vector<OAQueryToken> tokens, String... values) {
        assertEquals(values.length, tokens.size());
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], tokens.get(i).value, "index=" + i);
        }
    }

    private static String values(Vector<OAQueryToken> tokens) {
        return tokens.stream().map(t -> t.value).collect(Collectors.joining(" "));
    }
}
