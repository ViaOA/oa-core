package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryFilterLogicalPrecedenceTest {

    public static class Bean {
        private String a;
        private String b;
        private String c;

        public Bean(String a, String b, String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public String getA() {
            return a;
        }

        public String getB() {
            return b;
        }

        public String getC() {
            return c;
        }
    }

    @Test
    void andExpressionRequiresBothSides() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "a = 'Y' AND b = 'Y'");

        assertTrue(f.isUsed(new Bean("Y", "Y", "N")));
        assertFalse(f.isUsed(new Bean("Y", "N", "N")));
        assertFalse(f.isUsed(new Bean("N", "Y", "N")));
    }

    @Test
    void orExpressionAcceptsEitherSide() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "a = 'Y' OR b = 'Y'");

        assertTrue(f.isUsed(new Bean("Y", "N", "N")));
        assertTrue(f.isUsed(new Bean("N", "Y", "N")));
        assertFalse(f.isUsed(new Bean("N", "N", "N")));
    }

    @Test
    void parenthesesOverrideLogicalGrouping() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "(a = 'Y' OR b = 'Y') AND c = 'Y'");

        assertTrue(f.isUsed(new Bean("Y", "N", "Y")));
        assertTrue(f.isUsed(new Bean("N", "Y", "Y")));
        assertFalse(f.isUsed(new Bean("Y", "N", "N")));
        assertFalse(f.isUsed(new Bean("N", "N", "Y")));
    }

    @Test
    void orAndPrecedenceDocumentsCurrentParserBehavior() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "a = 'Y' OR b = 'Y' AND c = 'Y'");

        Bean aOnly = new Bean("Y", "N", "N");
        Bean bAndC = new Bean("N", "Y", "Y");
        Bean bOnly = new Bean("N", "Y", "N");

        /*
         * SQL-style precedence expects:
         *     a = 'Y' OR (b = 'Y' AND c = 'Y')
         *
         * CODEX previously flagged current in-memory parser risk:
         *     (a = 'Y' OR b = 'Y') AND c = 'Y'
         *
         * This locks the desired SQL-style behavior. If this fails, the test is
         * exposing the known precedence bug.
         */
        assertTrue(f.isUsed(aOnly));
        assertTrue(f.isUsed(bAndC));
        assertFalse(f.isUsed(bOnly));
    }

    @Test
    void trailingTokensAfterValidExpressionShouldBeRejected() {
        /*
         * CODEX flagged that a valid leading expression plus trailing garbage could
         * be silently accepted. Desired contract: query must be fully consumed.
         */
        assertThrows(RuntimeException.class, () ->
            new OAQueryFilter<>(Bean.class, "a = 'Y' garbage = 'N'")
        );
    }
}
