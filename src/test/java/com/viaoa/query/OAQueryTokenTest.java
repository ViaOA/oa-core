package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryTokenTest implements OAQueryTokenType {

    @Test
    void publicFieldsAreMutableTokenState() {
        OAQueryToken token = new OAQueryToken();

        token.type = VARIABLE;
        token.subtype = NUMBER;
        token.value = "item.name";

        assertEquals(VARIABLE, token.type);
        assertEquals(NUMBER, token.subtype);
        assertEquals("item.name", token.value);
    }

    @Test
    void isOperatorReturnsTrueForCurrentOperatorSetAndFalseForOtherTokens() {
        assertTrue(token(OPERATOR).isOperator());
        assertTrue(token(GT).isOperator());
        assertTrue(token(GE).isOperator());
        assertTrue(token(LT).isOperator());
        assertTrue(token(LE).isOperator());
        assertTrue(token(EQUAL).isOperator());
        assertTrue(token(NOTEQUAL).isOperator());
        assertTrue(token(LIKE).isOperator());

        assertFalse(token(NOTLIKE).isOperator());
        assertFalse(token(VARIABLE).isOperator());
        assertFalse(token(NULL).isOperator());
        assertFalse(token(EOF).isOperator());
    }

    private static OAQueryToken token(int type) {
        OAQueryToken token = new OAQueryToken();
        token.type = type;
        return token;
    }
}
