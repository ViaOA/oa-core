package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryPassthruTokenTest implements OAQueryTokenType {

    @Test
    void passthruPreservesOpaqueBody() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("PASS[lower(name) = 'smith']THRU");

        OAQueryToken token = tm.getNext();

        assertEquals(PASSTHRU, token.type);
        assertEquals("lower(name) = 'smith'", token.value);
    }

    @Test
    void passthruIsCaseInsensitiveForBoundary() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("pass[abc]thru");

        OAQueryToken token = tm.getNext();

        assertEquals(PASSTHRU, token.type);
        assertEquals("abc", token.value);
    }

    @Test
    void passthruDoesNotConsumeFollowingExpression() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("PASS[x]THRU and name = 'Bob'");

        assertEquals(PASSTHRU, tm.getNext().type);

        OAQueryToken and = tm.getNext();
        assertEquals(AND, and.type);
        assertEquals("and", and.value);
    }

    @Test
    void unterminatedPassthruThrows() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("PASS[lower(name) = 'smith'");

        assertThrows(RuntimeException.class, tm::getNext);
    }

    @Test
    void passVariableWithoutBracketRemainsVariable() {
        OAQueryTokenManager tm = new OAQueryTokenManager();
        tm.setQuery("PASSVALUE");

        OAQueryToken token = tm.getNext();

        assertEquals(VARIABLE, token.type);
        assertEquals("PASSVALUE", token.value);
    }
}
