package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OAQueryTokenizerStructuralInvalidTest {
    @Test void rejectsOperatorsWithoutLeftOperand() {
        assertInvalid("= 5");
        assertInvalid("!= 5");
        assertInvalid("<> 5");
        assertInvalid("> 5");
        assertInvalid(">= 5");
        assertInvalid("< 5");
        assertInvalid("<= 5");
        assertInvalid("LIKE 'x'");
        assertInvalid("NOTLIKE 'x'");
        assertInvalid("IN (1,2)");
    }

    @Test void rejectsOperatorsWithoutRightOperand() {
        assertInvalid("name =");
        assertInvalid("name !=");
        assertInvalid("name <>");
        assertInvalid("name >");
        assertInvalid("name >=");
        assertInvalid("name <");
        assertInvalid("name <=");
        assertInvalid("name LIKE");
        assertInvalid("name NOTLIKE");
        assertInvalid("name IN");
    }

    @Test void rejectsDoubleOperatorsAndAdjacentValues() {
        assertInvalid("name = = 'Bob'");
        assertInvalid("name > > 1");
        assertInvalid("name LIKE LIKE 'B%'");
        assertInvalid("name 'Bob'");
        assertInvalid("'Bob' 'Sue'");
        assertInvalid("1 2");
    }

    @Test void rejectsDanglingSeparatorsAndChainedComparisons() {
        assertInvalid(",");
        assertInvalid("(,)");
        assertInvalid("(a = 1,)");
        assertInvalid("(,a = 1)");
        assertInvalid("a = b = c");
        assertInvalid("a < b < c");
        assertInvalid("a = b and c");
    }

    private static void assertInvalid(String q) {
        assertThrows(RuntimeException.class, () -> new OAQueryTokenizer().convertToTokens(q), q);
    }
}
