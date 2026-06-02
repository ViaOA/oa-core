package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Vector;
import org.junit.jupiter.api.Test;

class OAQueryPassthruRegressionTest implements OAQueryTokenType {
    @Test void passthruBodyMayContainBracketsUntilTerminator() {
        Vector<OAQueryToken> t = tokens("PASS[abc [nested-ish] still text]THRU");
        assertEquals(1, t.size());
        assertEquals(PASSTHRU, t.get(0).type);
        assertEquals("abc [nested-ish] still text", t.get(0).value);
    }

    @Test void passthruCanContainOperatorLikeTextWithoutParsingIt() {
        Vector<OAQueryToken> t = tokens("PASS[name = 'Bob' and age >= 18]THRU");
        assertEquals(1, t.size());
        assertEquals(PASSTHRU, t.get(0).type);
        assertEquals("name = 'Bob' and age >= 18", t.get(0).value);
    }

    @Test void unterminatedPassthruFailsEveryTime() {
        for (int i = 0; i < 5; i++) assertThrows(RuntimeException.class, () -> tokens("PASS[name = 'Bob'"));
    }

    @Test void passthruFollowedByExpressionWithoutLogicalOperatorIsRejected() {
        assertThrows(RuntimeException.class, () -> tokens("PASS[x]THRU name = 'Bob'"));
    }

    @Test void passthruFollowedByLogicalOperatorExpressionParses() {
        Vector<OAQueryToken> t = tokens("PASS[x]THRU and name = 'Bob'");
        assertEquals(PASSTHRU, t.get(0).type);
        assertTrue(t.stream().anyMatch(x -> x.type == AND));
        assertTrue(t.stream().anyMatch(x -> "name".equals(x.value)));
    }

    static Vector<OAQueryToken> tokens(String q) { return new OAQueryTokenizer().convertToTokens(q); }
}
