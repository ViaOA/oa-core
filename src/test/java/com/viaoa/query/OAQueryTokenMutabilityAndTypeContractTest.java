package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OAQueryTokenMutabilityAndTypeContractTest implements OAQueryTokenType {
    @Test void tokenFieldsArePlainMutableDataCarrierCurrentContract() {
        OAQueryToken token = new OAQueryToken();
        token.type = VARIABLE;
        token.subtype = 123;
        token.value = "name";
        assertEquals(VARIABLE, token.type);
        assertEquals(123, token.subtype);
        assertEquals("name", token.value);
    }

    @Test void operatorIdentityDependsOnCurrentTypeField() {
        OAQueryToken token = new OAQueryToken();
        token.type = VARIABLE;
        assertFalse(token.isOperator());
        token.type = EQUAL;
        assertTrue(token.isOperator());
        token.type = NOTLIKE;
        assertTrue(token.isOperator());
    }

    @Test void tokenTypeConstantsAreStable() {
        assertEquals(1, EOF);
        assertEquals(2, NUMBER);
        assertEquals(12, EQUAL);
        assertEquals(13, NOTEQUAL);
        assertEquals(17, LIKE);
        assertEquals(18, NOTLIKE);
        assertEquals(29, IN);
        assertEquals(30, COMMA);
    }
}
