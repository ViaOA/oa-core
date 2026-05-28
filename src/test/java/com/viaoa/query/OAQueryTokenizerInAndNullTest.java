package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Vector;

import org.junit.jupiter.api.Test;

class OAQueryTokenizerInAndNullTest implements OAQueryTokenType {

    @Test
    void simpleInListParses() {
        Vector<OAQueryToken> tokens = tokens("id in (1,2,3)");

        assertTypes(tokens, VARIABLE, IN, SEPERATORBEGIN, NUMBER, COMMA, NUMBER, COMMA, NUMBER, SEPERATOREND);
    }

    @Test
    void inQuestionPlaceholderParses() {
        assertTypes(tokens("id in (?)"), VARIABLE, IN, SEPERATORBEGIN, QUESTION, SEPERATOREND);
        assertTypes(tokens("id in ?"), VARIABLE, IN, QUESTION);
    }

    @Test
    void compositeInLiteralTuplesParse() {
        Vector<OAQueryToken> tokens = tokens("(date, store_number) in (('2021-12-15', 12345), ('2021-10-07', 67890))");

        assertTrue(tokens.stream().anyMatch(t -> t.type == IN));
        assertTrue(tokens.stream().filter(t -> t.type == COMMA).count() >= 3);
        assertTrue(tokens.stream().anyMatch(t -> "2021-12-15".equals(t.value)));
        assertTrue(tokens.stream().anyMatch(t -> "67890".equals(t.value)));
    }

    @Test
    void compositeInPlaceholderParses() {
        Vector<OAQueryToken> tokens = tokens("(date, store_number) in (?)");

        assertTrue(tokens.stream().anyMatch(t -> t.type == IN));
        assertTrue(tokens.stream().anyMatch(t -> t.type == QUESTION));
    }

    @Test
    void malformedInListThrows() {
        assertInvalid("id in ()");
        assertInvalid("id in (1,)");
        assertInvalid("id in (,1)");
        assertInvalid("id in (1,2");
    }

    @Test
    void equalsNullAndNotEqualsNullParse() {
        assertTypes(tokens("name = null"), VARIABLE, EQUAL, NULL);
        assertTypes(tokens("name != null"), VARIABLE, NOTEQUAL, NULL);
        assertTypes(tokens("name <> null"), VARIABLE, NOTEQUAL, NULL);
    }

    @Test
    void isNullAndIsNotNullHaveExplicitValidTokenShape() {
        assertTypes(tokens("name is null"), VARIABLE, EQUAL, NULL);

        Vector<OAQueryToken> tokens = tokens("name is not null");
        assertEquals(VARIABLE, tokens.get(0).type);
        assertEquals(NULL, tokens.get(tokens.size() - 1).type);
        assertTrue(tokens.stream().anyMatch(t -> "not".equalsIgnoreCase(t.value) || t.type == NOTEQUAL));
    }

    private static Vector<OAQueryToken> tokens(String query) {
        return new OAQueryTokenizer().convertToTokens(query);
    }

    private static void assertTypes(Vector<OAQueryToken> tokens, int... types) {
        assertEquals(types.length, tokens.size(), "token count");
        for (int i = 0; i < types.length; i++) {
            assertEquals(types[i], tokens.get(i).type, "index=" + i + " value=" + tokens.get(i).value);
        }
    }

    private static void assertInvalid(String query) {
        assertThrows(RuntimeException.class, () -> tokens(query), query);
    }
}
