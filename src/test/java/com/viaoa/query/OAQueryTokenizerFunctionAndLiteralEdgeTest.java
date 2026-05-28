package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Vector;
import org.junit.jupiter.api.Test;

class OAQueryTokenizerFunctionAndLiteralEdgeTest implements OAQueryTokenType {
    @Test void singleArgumentFunctionParsesWithFunctionTokens() {
        assertTypes(tokens("lower(name) = 'bob'"), VARIABLE, FUNCTIONBEGIN, VARIABLE, FUNCTIONEND, EQUAL, STRINGSQ);
    }

    @Test void nestedFunctionParses() {
        Vector<OAQueryToken> t = tokens("lower(trim(name)) = 'bob'");
        assertTrue(t.stream().filter(x -> x.type == FUNCTIONBEGIN).count() >= 2);
        assertTrue(t.stream().filter(x -> x.type == FUNCTIONEND).count() >= 2);
        assertTrue(t.stream().anyMatch(x -> x.type == EQUAL));
    }

    @Test void multiArgumentFunctionParsesWithCommas() {
        Vector<OAQueryToken> t = tokens("concat(firstName, lastName) = 'Bob Smith'");
        assertTrue(t.stream().anyMatch(x -> x.type == FUNCTIONBEGIN));
        assertTrue(t.stream().anyMatch(x -> x.type == COMMA));
        assertTrue(t.stream().anyMatch(x -> "firstName".equals(x.value)));
        assertTrue(t.stream().anyMatch(x -> "lastName".equals(x.value)));
    }

    @Test void escapedAndDoubleQuotedLiteralsCanBeComparisonValues() {
        Vector<OAQueryToken> a = tokens("code = {A'B}");
        assertTypes(a, VARIABLE, EQUAL, STRINGESC);
        assertEquals("A'B", a.get(2).value);
        Vector<OAQueryToken> b = tokens("code = \"ABC\"");
        assertTypes(b, VARIABLE, EQUAL, STRINGDQ);
        assertEquals("ABC", b.get(2).value);
    }

    @Test void passthruCanBeUsedAsOpaqueValue() {
        Vector<OAQueryToken> t = tokens("PASS[lower(name) = 'bob']THRU");
        assertTypes(t, PASSTHRU);
        assertEquals("lower(name) = 'bob'", t.get(0).value);
    }

    @Test void invalidFunctionOrLiteralThrows() {
        assertInvalid("lower(name)) = 'bob'");
        assertInvalid("lower(name) 'bob'");
        assertInvalid("code = 'ABC");
        assertInvalid("code = \"ABC");
        assertInvalid("code = {ABC");
    }

    private static Vector<OAQueryToken> tokens(String q) { return new OAQueryTokenizer().convertToTokens(q); }
    private static void assertInvalid(String q) { assertThrows(RuntimeException.class, () -> tokens(q), q); }
    private static void assertTypes(Vector<OAQueryToken> t, int... types) {
        assertEquals(types.length, t.size());
        for (int i = 0; i < types.length; i++) assertEquals(types[i], t.get(i).type, "index=" + i + " value=" + t.get(i).value);
    }
}
