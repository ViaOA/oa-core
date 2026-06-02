package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OAQueryCompleteConsumptionTest {
    @Test void validLeadingExpressionWithGarbageTailIsRejected() {
        assertInvalid("name = 'Bob' garbage");
        assertInvalid("name = 'Bob' age");
        assertInvalid("name = 'Bob' 123");
        assertInvalid("name = 'Bob' ?");
    }

    @Test void validParenthesizedExpressionWithGarbageTailIsRejected() {
        assertInvalid("(name = 'Bob') garbage");
        assertInvalid("(name = 'Bob') (age = 1)");
    }

    @Test void validFunctionExpressionWithGarbageTailIsRejected() {
        assertInvalid("lower(name) = 'bob' garbage");
        assertInvalid("lower(name) = 'bob' lower(age)");
    }

    @Test void parserConsumesWholeExpressionForValidQueries() {
        assertDoesNotThrow(() -> tokens("name = 'Bob'"));
        assertDoesNotThrow(() -> tokens("(name = 'Bob' or age = 1) and status = 'A'"));
        assertDoesNotThrow(() -> tokens("id in (1,2,3)"));
    }

    @Test void eofOnlyEmptyQueryBoundaryIsDocumented() {
        assertThrows(RuntimeException.class, () -> tokens(""));
        assertThrows(RuntimeException.class, () -> tokens("   "));
    }

    static void tokens(String q) { new OAQueryTokenizer().convertToTokens(q); }
    static void assertInvalid(String q) { assertThrows(RuntimeException.class, () -> tokens(q), q); }
}
