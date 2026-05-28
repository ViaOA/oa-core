package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Vector;
import org.junit.jupiter.api.Test;

class OAQueryTokenizerReuseAndFailureStateTest implements OAQueryTokenType {
    @Test void tokenizerCanBeReusedAfterSuccessfulParse() {
        OAQueryTokenizer qt = new OAQueryTokenizer();
        assertTypes(qt.convertToTokens("name = 'Bob'"), VARIABLE, EQUAL, STRINGSQ);
        assertTypes(qt.convertToTokens("age >= 18"), VARIABLE, GE, NUMBER);
        assertTypes(qt.convertToTokens("id in (?)"), VARIABLE, IN, SEPERATORBEGIN, QUESTION, SEPERATOREND);
    }

    @Test void tokenizerCanBeReusedAfterFailedParse() {
        OAQueryTokenizer qt = new OAQueryTokenizer();
        assertThrows(RuntimeException.class, () -> qt.convertToTokens("name = 'Bob' and"));
        assertTypes(qt.convertToTokens("name = 'Sue'"), VARIABLE, EQUAL, STRINGSQ);
    }

    @Test void failedParseDoesNotExposeSuccessfulTokenVectorThroughNextParse() {
        OAQueryTokenizer qt = new OAQueryTokenizer();
        assertThrows(RuntimeException.class, () -> qt.convertToTokens("(name = 'Bob'"));
        Vector<OAQueryToken> t = qt.convertToTokens("id = 1");
        assertTypes(t, VARIABLE, EQUAL, NUMBER);
        assertEquals("id", t.get(0).value);
    }

    @Test void tokenManagerCanBeReusedAfterUnterminatedLiteralFailure() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("'abc");
        assertThrows(RuntimeException.class, tm::getNext);
        tm.setQuery("'def'");
        OAQueryToken token = tm.getNext();
        assertEquals(STRINGSQ, token.type);
        assertEquals("def", token.value);
    }

    @Test void independentTokenizerInstancesDoNotInterfere() {
        OAQueryTokenizer q1 = new OAQueryTokenizer();
        OAQueryTokenizer q2 = new OAQueryTokenizer();
        assertEquals("a", q1.convertToTokens("a = 1").get(0).value);
        assertEquals("b", q2.convertToTokens("b = 2").get(0).value);
    }

    private static void assertTypes(Vector<OAQueryToken> t, int... types) {
        assertEquals(types.length, t.size());
        for (int i = 0; i < types.length; i++) assertEquals(types[i], t.get(i).type, "index=" + i);
    }
}
