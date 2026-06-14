package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryTokenTypeTest implements OAQueryTokenType {

    @Test
    void constantsKeepCurrentStableValues() {
        assertEquals(1, EOF);
        assertEquals(2, NUMBER);
        assertEquals(3, OPERATOR);
        assertEquals(4, SEPERATORBEGIN);
        assertEquals(5, SEPERATOREND);
        assertEquals(7, VARIABLE);
        assertEquals(8, GT);
        assertEquals(9, GE);
        assertEquals(10, LT);
        assertEquals(11, LE);
        assertEquals(12, EQUAL);
        assertEquals(13, NOTEQUAL);
        assertEquals(14, AND);
        assertEquals(15, OR);
        assertEquals(17, LIKE);
        assertEquals(18, NOTLIKE);
        assertEquals(19, NULL);
        assertEquals(20, STRINGSQ);
        assertEquals(21, STRINGDQ);
        assertEquals(22, STRINGESC);
        assertEquals(23, TRUE);
        assertEquals(24, FALSE);
        assertEquals(25, PASSTHRU);
        assertEquals(26, QUESTION);
        assertEquals(27, FUNCTIONBEGIN);
        assertEquals(28, FUNCTIONEND);
        assertEquals(29, IN);
        assertEquals(30, COMMA);
    }
}
