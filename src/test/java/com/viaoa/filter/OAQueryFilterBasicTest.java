package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryFilterBasicTest {

    public static class Bean {
        private String name;
        private Integer age;
        private Boolean active;
        private String status;
        private String code;

        public Bean(String name, Integer age, Boolean active, String status, String code) {
            this.name = name;
            this.age = age;
            this.active = active;
            this.status = status;
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public Boolean getActive() {
            return active;
        }

        public String getStatus() {
            return status;
        }

        public String getCode() {
            return code;
        }
    }

    @Test
    void equalAndNotEqualExpressionsUsePropertyValues() {
        OAQueryFilter<Bean> eq = new OAQueryFilter<>(Bean.class, "name = 'Bob'");
        OAQueryFilter<Bean> ne = new OAQueryFilter<>(Bean.class, "name != 'Bob'");

        assertTrue(eq.isUsed(new Bean("Bob", 42, true, "VIP", "A1")));
        assertFalse(eq.isUsed(new Bean("Sue", 42, true, "VIP", "A1")));

        assertFalse(ne.isUsed(new Bean("Bob", 42, true, "VIP", "A1")));
        assertTrue(ne.isUsed(new Bean("Sue", 42, true, "VIP", "A1")));
    }

    @Test
    void equalityIsCaseInsensitiveByCurrentQueryFilterContract() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "name = 'bob'");

        assertTrue(f.isUsed(new Bean("Bob", 42, true, "VIP", "A1")));
        assertTrue(f.isUsed(new Bean("BOB", 42, true, "VIP", "A1")));
    }

    @Test
    void relationalExpressionsUseOACompareOrdering() {
        Bean bean = new Bean("Bob", 42, true, "VIP", "A1");

        assertTrue(new OAQueryFilter<>(Bean.class, "age > 40").isUsed(bean));
        assertFalse(new OAQueryFilter<>(Bean.class, "age > 42").isUsed(bean));

        assertTrue(new OAQueryFilter<>(Bean.class, "age >= 42").isUsed(bean));
        assertFalse(new OAQueryFilter<>(Bean.class, "age >= 43").isUsed(bean));

        assertTrue(new OAQueryFilter<>(Bean.class, "age < 50").isUsed(bean));
        assertFalse(new OAQueryFilter<>(Bean.class, "age < 42").isUsed(bean));

        assertTrue(new OAQueryFilter<>(Bean.class, "age <= 42").isUsed(bean));
        assertFalse(new OAQueryFilter<>(Bean.class, "age <= 41").isUsed(bean));
    }

    @Test
    void likeAndNotLikeExpressionsUseOACompareLikeSemantics() {
        Bean bean = new Bean("abcdef", 42, true, "VIP", "A1");

        assertTrue(new OAQueryFilter<>(Bean.class, "name LIKE 'ab*ef'").isUsed(bean));
        assertTrue(new OAQueryFilter<>(Bean.class, "name LIKE 'ab%ef'").isUsed(bean));
        assertFalse(new OAQueryFilter<>(Bean.class, "name LIKE 'ab*z'").isUsed(bean));

        assertFalse(new OAQueryFilter<>(Bean.class, "name NOTLIKE 'ab*ef'").isUsed(bean));
        assertTrue(new OAQueryFilter<>(Bean.class, "name NOTLIKE 'ab*z'").isUsed(bean));
    }

    @Test
    void nullExpressionMatchesNullProperty() {
        Bean nullName = new Bean(null, 42, true, "VIP", "A1");
        Bean fullName = new Bean("Bob", 42, true, "VIP", "A1");

        assertTrue(new OAQueryFilter<>(Bean.class, "name = null").isUsed(nullName));
        assertFalse(new OAQueryFilter<>(Bean.class, "name = null").isUsed(fullName));

        assertFalse(new OAQueryFilter<>(Bean.class, "name != null").isUsed(nullName));
        assertTrue(new OAQueryFilter<>(Bean.class, "name != null").isUsed(fullName));
    }

    @Test
    void placeholderArgumentsAreSubstitutedInOrder() {
        OAQueryFilter<Bean> f = new OAQueryFilter<>(Bean.class, "name = ? AND age >= ?", new Object[] { "Bob", 40 });

        assertTrue(f.isUsed(new Bean("Bob", 42, true, "VIP", "A1")));
        assertFalse(f.isUsed(new Bean("Bob", 39, true, "VIP", "A1")));
        assertFalse(f.isUsed(new Bean("Sue", 42, true, "VIP", "A1")));
    }

    @Test
    void selectFilterDelegatesToQueryFilterBehavior() {
        OASelectFilter<Bean> f = new OASelectFilter<>(Bean.class, "name = ? AND age >= ?", new Object[] { "Bob", 40 });

        assertTrue(f.isUsed(new Bean("Bob", 42, true, "VIP", "A1")));
        assertFalse(f.isUsed(new Bean("Sue", 42, true, "VIP", "A1")));
    }

    @Test
    void emptyOrInvalidQueryThrowsAtConstruction() {
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, ""));
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "name ="));
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "AND name = 'Bob'"));
    }
}
