package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Vector;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;

class OAQueryTest implements OAQueryTokenType {

    @Test
    void parseDelegatesToTokenizerAndReturnsTokenVector() {
        Vector<OAQueryToken> tokens = new OAQuery().parse(Item.P_Name + " = 'Brake Pad'");

        assertToken(tokens.get(0), VARIABLE, Item.P_Name);
        assertToken(tokens.get(1), EQUAL, "=");
        assertToken(tokens.get(2), STRINGSQ, "Brake Pad");
        assertEquals(3, tokens.size());
    }

    @Test
    void parsePropagatesInvalidSyntax() {
        assertThrows(RuntimeException.class, () -> new OAQuery().parse(Item.P_Name + " = )"));
    }

    private static void assertToken(OAQueryToken token, int type, String value) {
        assertEquals(type, token.type);
        assertEquals(value, token.value);
    }
}
