package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryTokenizerInvalidSyntaxTest {

    @Test
    void rejectsTrailingTokensAfterCompleteExpression() {
        assertInvalid("name = 'Bob' garbage");
        assertInvalid("name = 'Bob' age = 5");
    }

    @Test
    void rejectsDanglingLogicalOperators() {
        assertInvalid("name = 'Bob' and");
        assertInvalid("name = 'Bob' or");
        assertInvalid("and name = 'Bob'");
        assertInvalid("or name = 'Bob'");
    }

    @Test
    void rejectsMissingComparisonOperands() {
        assertInvalid("name =");
        assertInvalid("= 'Bob'");
        assertInvalid("age >");
        assertInvalid("> 5");
    }

    @Test
    void rejectsChainedComparisons() {
        assertInvalid("a = b = c");
        assertInvalid("a > b and c");
    }

    @Test
    void rejectsUnbalancedParentheses() {
        assertInvalid("(name = 'Bob'");
        assertInvalid("name = 'Bob')");
        assertInvalid("(name = 'Bob'))");
    }

    @Test
    void rejectsMalformedFunctionCall() {
        assertInvalid("lower(name = 'bob'");
        assertInvalid("lower() = 'bob'");
    }

    @Test
    void sameInvalidQueryFailsConsistently() {
        for (int i = 0; i < 10; i++) {
            assertInvalid("name = 'Bob' and");
        }
    }

    private static void assertInvalid(String query) {
        assertThrows(RuntimeException.class, () -> new OAQueryTokenizer().convertToTokens(query), query);
    }
}
