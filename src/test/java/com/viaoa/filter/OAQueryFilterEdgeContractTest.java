package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryFilterEdgeContractTest {

    public static class Bean {
        private String name;
        private Integer age;
        private String status;
        private String code;

        public Bean(String name, Integer age, String status, String code) {
            this.name = name;
            this.age = age;
            this.status = status;
            this.code = code;
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

        public String getCode() {
            return code;
        }
    }

    @Test
    void nestedParenthesesAreEvaluatedDeterministically() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(
            Bean.class,
            "((name = 'Bob' OR name = 'Sue') AND (status = 'VIP' OR status = 'NEW'))"
        );

        assertTrue(f.isUsed(new Bean("Bob", 42, "VIP", "A")));
        assertTrue(f.isUsed(new Bean("Sue", 42, "NEW", "A")));
        assertFalse(f.isUsed(new Bean("Bob", 42, "DONE", "A")));
        assertFalse(f.isUsed(new Bean("Tim", 42, "VIP", "A")));
    }

    @Test
    void unmatchedParenthesesThrowAtConstruction() {
        assertThrows(RuntimeException.class, () ->
            new OAQueryFilter<>(Bean.class, "(name = 'Bob'")
        );

        assertThrows(RuntimeException.class, () ->
            new OAQueryFilter<>(Bean.class, "name = 'Bob')")
        );
    }

    @Test
    void unknownPropertyThrowsAtConstructionOrRejectsAtEvaluationByCurrentContract() {
        OAQueryFilter<Bean> f = null;
        try {
            f = new OAQueryFilter<>(Bean.class, "missing = 'x'");
        } catch (RuntimeException ex) {
            return;
        }

        assertFalse(f.isUsed(new Bean("Bob", 42, "VIP", "A")));
    }

    @Test
    void quotedStringWithSpacesIsParsedAsSingleValue() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "status = 'Very Important'");

        assertTrue(f.isUsed(new Bean("Bob", 42, "Very Important", "A")));
        assertFalse(f.isUsed(new Bean("Bob", 42, "Very", "A")));
    }

    @Test
    void numericStringQueryValueComparesNumericallyThroughOACompare() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "age = '042'");

        assertTrue(f.isUsed(new Bean("Bob", 42, "VIP", "A")));
        assertFalse(f.isUsed(new Bean("Bob", 43, "VIP", "A")));
    }

    @Test
    void nullCandidateIsRejectedByNormalQueryFilterCurrentContract() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "name = 'Bob'");

        assertFalse(f.isUsed(null));
    }

    @Test
    void repeatedEvaluationIsDeterministic() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(
            Bean.class,
            "name = 'Bob' AND age >= 40 AND status LIKE 'V*'"
        );
        Bean bean = new Bean("Bob", 42, "VIP", "A");

        for (int i = 0; i < 10; i++) {
            assertTrue(f.isUsed(bean));
        }
    }
}
