package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Vector;
import org.junit.jupiter.api.Test;

class OAQueryNullPredicateContractTest implements OAQueryTokenType {
    @Test void equalsNullHasStableTokenShape() {
        Vector<OAQueryToken> t = tokens("name = null");
        assertTypes(t, VARIABLE, EQUAL, NULL);
        assertValues(t, "name", "=", "null");
    }

    @Test void notEqualsNullAliasesHaveStableTokenShape() {
        Vector<OAQueryToken> a = tokens("name != null");
        assertTypes(a, VARIABLE, NOTEQUAL, NULL);
        assertValues(a, "name", "!=", "null");
        Vector<OAQueryToken> b = tokens("name <> null");
        assertTypes(b, VARIABLE, NOTEQUAL, NULL);
        assertValues(b, "name", "<>", "null");
    }

    @Test void isNullNormalizesToEqualityNullCurrentContract() {
        Vector<OAQueryToken> t = tokens("name is null");
        assertTypes(t, VARIABLE, EQUAL, NULL);
        assertEquals("is", t.get(1).value);
    }

    @Test void isNotNullFailsOrNormalizesToExplicitNotNullShape() {
        try {
            Vector<OAQueryToken> t = tokens("name is not null");
            assertEquals(VARIABLE, t.get(0).type);
            assertEquals(NULL, t.get(t.size() - 1).type);
            assertTrue(t.stream().anyMatch(x -> x.type == NOTEQUAL)
                || t.stream().anyMatch(x -> "not".equalsIgnoreCase(x.value)));
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    @Test void missingNullOperandFails() {
        assertInvalid("name is");
        assertInvalid("name != ");
        assertInvalid("name <> ");
    }

    static Vector<OAQueryToken> tokens(String q) { return new OAQueryTokenizer().convertToTokens(q); }
    static void assertInvalid(String q) { assertThrows(RuntimeException.class, () -> tokens(q), q); }
    static void assertTypes(Vector<OAQueryToken> t, int... types) {
        assertEquals(types.length, t.size());
        for (int i = 0; i < types.length; i++) assertEquals(types[i], t.get(i).type, "index=" + i);
    }
    static void assertValues(Vector<OAQueryToken> t, String... values) {
        assertEquals(values.length, t.size());
        for (int i = 0; i < values.length; i++) assertEquals(values[i], t.get(i).value, "index=" + i);
    }
}
