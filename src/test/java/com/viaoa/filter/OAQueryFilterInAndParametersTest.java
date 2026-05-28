package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class OAQueryFilterInAndParametersTest {

    public static class Bean {
        private String name;
        private Integer age;
        private String status;

        public Bean(String name, Integer age, String status) {
            this.name = name;
            this.age = age;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public String getStatus() {
            return status;
        }
    }

    @Test
    void inExpressionMatchesLiteralList() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "status IN ('NEW','VIP','HOLD')");

        assertTrue(f.isUsed(new Bean("Bob", 42, "VIP")));
        assertTrue(f.isUsed(new Bean("Sue", 42, "NEW")));
        assertFalse(f.isUsed(new Bean("Tim", 42, "DONE")));
    }

    @Test
    void notInExpressionNegatesLiteralListIfSupported() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "status NOT IN ('NEW','VIP','HOLD')");

        assertFalse(f.isUsed(new Bean("Bob", 42, "VIP")));
        assertTrue(f.isUsed(new Bean("Tim", 42, "DONE")));
    }

    @Test
    void inExpressionWithSinglePlaceholderListUsesListValues() {
        List<String> statuses = Arrays.asList("NEW", "VIP", "HOLD");
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "status IN (?)", new Object[] { statuses });

        assertTrue(f.isUsed(new Bean("Bob", 42, "VIP")));
        assertFalse(f.isUsed(new Bean("Tim", 42, "DONE")));
    }

    @Test
    void placeholderScalarWorksInsideInExpressionCurrentContract() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "status IN (?)", new Object[] { "VIP" });

        assertTrue(f.isUsed(new Bean("Bob", 42, "VIP")));
        assertFalse(f.isUsed(new Bean("Tim", 42, "DONE")));
    }

    @Test
    void missingPlaceholderArgumentShouldThrowAtConstruction() {
        /*
         * Desired contract: in-memory filtering should match JDBC path and reject
         * missing parameters instead of treating '?' as a literal.
         */
        assertThrows(RuntimeException.class, () ->
            new OAQueryFilter<>(Bean.class, "status = ?", new Object[0])
        );
    }

    @Test
    void extraPlaceholderArgumentsShouldThrowAtConstruction() {
        /*
         * Desired contract: exact parameter count. If current code allows extra
         * args, this test exposes the contract gap.
         */
        assertThrows(RuntimeException.class, () ->
            new OAQueryFilter<>(Bean.class, "status = ?", new Object[] { "VIP", "EXTRA" })
        );
    }

    @Test
    void booleanLiteralTokensShouldMatchBooleanPropertiesIfSupported() {
        class BoolBean {
            private Boolean active;
            BoolBean(Boolean active) { this.active = active; }
            public Boolean getActive() { return active; }
        }

        /*
         * CODEX flagged possible divergence where JDBC recognizes TRUE/FALSE but
         * OAQueryFilter treats them as strings. Desired contract: Boolean tokens.
         */
        assertTrue(new OAQueryFilter<>(BoolBean.class, "active = true").isUsed(new BoolBean(true)));
        assertFalse(new OAQueryFilter<>(BoolBean.class, "active = true").isUsed(new BoolBean(false)));
        assertTrue(new OAQueryFilter<>(BoolBean.class, "active = false").isUsed(new BoolBean(false)));
    }
}
