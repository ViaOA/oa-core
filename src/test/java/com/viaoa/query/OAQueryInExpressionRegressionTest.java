package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Vector;
import org.junit.jupiter.api.Test;

class OAQueryInExpressionRegressionTest implements OAQueryTokenType {
    @Test void inListWithStringsNumbersAndQuestionParses() {
        Vector<OAQueryToken> t = tokens("code in ('A', 2, ?)");
        assertEquals(VARIABLE, t.get(0).type);
        assertEquals(IN, t.get(1).type);
        assertTrue(t.stream().anyMatch(x -> x.type == STRINGSQ && "A".equals(x.value)));
        assertTrue(t.stream().anyMatch(x -> x.type == NUMBER && "2".equals(x.value)));
        assertTrue(t.stream().anyMatch(x -> x.type == QUESTION));
    }

    @Test void nestedCompositeInListParsesAllTupleValues() {
        Vector<OAQueryToken> t = tokens("(a,b) in ((1,2),(3,4))");
        assertTrue(t.stream().anyMatch(x -> x.type == IN));
        assertTrue(t.stream().filter(x -> x.type == NUMBER).count() >= 4);
        assertTrue(t.stream().filter(x -> x.type == COMMA).count() >= 3);
    }

    @Test void malformedNestedCompositeInListFails() {
        assertInvalid("(a,b) in ((1,2),(3,))");
        assertInvalid("(a,b) in ((1,2),(,4))");
        assertInvalid("(a,b) in ((1,2),(3,4)");
    }

    @Test void chainedInExpressionIsRejected() {
        assertInvalid("id in (1,2) in (3,4)");
    }

    @Test void inListMissingLeftSideFails() {
        assertInvalid("in (1,2)");
    }

    static Vector<OAQueryToken> tokens(String q) { return new OAQueryTokenizer().convertToTokens(q); }
    static void assertInvalid(String q) { assertThrows(RuntimeException.class, () -> tokens(q), q); }
}
