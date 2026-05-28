package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Vector;
import org.junit.jupiter.api.Test;

class OAQueryTokenizerPrecedenceAndGroupingTest implements OAQueryTokenType {
    @Test void andOrExpressionKeepsAllLogicalTokensInOrder() {
        Vector<OAQueryToken> t = tokens("a = 1 or b = 2 and c = 3");
        assertValues(t, "a", "=", "1", "or", "b", "=", "2", "and", "c", "=", "3");
        assertEquals(OR, t.get(3).type);
        assertEquals(AND, t.get(7).type);
    }

    @Test void parenthesesPreserveExplicitGroupingTokens() {
        Vector<OAQueryToken> t = tokens("(a = 1 or b = 2) and c = 3");
        assertEquals(SEPERATORBEGIN, t.get(0).type);
        assertTrue(t.stream().anyMatch(x -> x.type == SEPERATOREND));
        assertTrue(t.stream().anyMatch(x -> x.type == AND));
        assertTrue(t.stream().anyMatch(x -> x.type == OR));
    }

    @Test void nestedParenthesesParseDeterministically() {
        Vector<OAQueryToken> t = tokens("((a = 1) or (b = 2 and c = 3))");
        assertTrue(t.stream().filter(x -> x.type == SEPERATORBEGIN).count() >= 4);
        assertTrue(t.stream().filter(x -> x.type == SEPERATOREND).count() >= 4);
        assertTrue(t.stream().anyMatch(x -> x.type == OR));
        assertTrue(t.stream().anyMatch(x -> x.type == AND));
    }

    @Test void invalidGroupingThrows() {
        assertInvalid("(a = 1 or)");
        assertInvalid("(and a = 1)");
        assertInvalid("(a =)");
        assertInvalid("()");
        assertInvalid("( )");
        assertInvalid("a = 1, b = 2");
    }

    @Test void repeatedParseProducesSameTokenStream() {
        String q = "(a = 1 or b = 2) and c >= 3";
        Vector<OAQueryToken> first = tokens(q);
        for (int i = 0; i < 10; i++) assertSameTokens(first, tokens(q));
    }

    private static Vector<OAQueryToken> tokens(String q) { return new OAQueryTokenizer().convertToTokens(q); }
    private static void assertInvalid(String q) { assertThrows(RuntimeException.class, () -> tokens(q), q); }
    private static void assertValues(Vector<OAQueryToken> t, String... vals) {
        assertEquals(vals.length, t.size());
        for (int i = 0; i < vals.length; i++) assertEquals(vals[i], t.get(i).value, "index=" + i);
    }
    private static void assertSameTokens(Vector<OAQueryToken> a, Vector<OAQueryToken> b) {
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).type, b.get(i).type, "type index=" + i);
            assertEquals(a.get(i).value, b.get(i).value, "value index=" + i);
        }
    }
}
