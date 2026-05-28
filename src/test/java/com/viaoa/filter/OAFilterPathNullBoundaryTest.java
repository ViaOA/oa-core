package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterPathNullBoundaryTest {

    public static class Bean {
        private Child child;
        private String name;

        public Bean(String name, Child child) {
            this.name = name;
            this.child = child;
        }

        public String getName() {
            return name;
        }

        public Child getChild() {
            return child;
        }
    }

    public static class Child {
        private String value;
        private Integer number;

        public Child(String value, Integer number) {
            this.value = value;
            this.number = number;
        }

        public String getValue() {
            return value;
        }

        public Integer getNumber() {
            return number;
        }
    }

    @Test
    void missingIntermediatePathIsTreatedAsNullResolvedValue() {
        Bean bean = new Bean("root", null);

        assertTrue(new OANullFilter("child.value").isUsed(bean));
        assertFalse(new OANotNullFilter("child.value").isUsed(bean));

        assertTrue(new OAEmptyFilter("child.value").isUsed(bean));
        assertFalse(new OANotEmptyFilter("child.value").isUsed(bean));

        assertFalse(new OAEqualFilter("child.value", "x").isUsed(bean));
        assertTrue(new OANotEqualFilter("child.value", "x").isUsed(bean));
    }

    @Test
    void nullTerminalValueBoundaryBehaviorIsDeterministic() {
        Bean bean = new Bean("root", new Child(null, null));

        assertTrue(new OANullFilter("child.value").isUsed(bean));
        assertFalse(new OANotNullFilter("child.value").isUsed(bean));

        assertTrue(new OAEqualFilter("child.value", null).isUsed(bean));
        assertFalse(new OANotEqualFilter("child.value", null).isUsed(bean));

        assertTrue(new OAEmptyFilter("child.number").isUsed(bean));
        assertFalse(new OANotEmptyFilter("child.number").isUsed(bean));
    }

    @Test
    void relationalFiltersRejectNullResolvedValues() {
        Bean bean = new Bean("root", new Child(null, null));

        assertFalse(new OAGreaterFilter("child.number", 1).isUsed(bean));
        assertFalse(new OAGreaterOrEqualFilter("child.number", 1).isUsed(bean));
        assertFalse(new OALessFilter("child.number", 1).isUsed(bean));
        assertFalse(new OALessOrEqualFilter("child.number", 1).isUsed(bean));
        assertFalse(new OABetweenFilter<>("child.number", 1, 10).isUsed(bean));
        assertFalse(new OABetweenOrEqualFilter("child.number", 1, 10).isUsed(bean));
    }

    @Test
    void stringFiltersRejectNullResolvedValuesExceptNotLikeNegation() {
        Bean bean = new Bean("root", new Child(null, null));

        assertFalse(new OALikeFilter("child.value", "*").isUsed(bean));
        assertTrue(new OANotLikeFilter("child.value", "*").isUsed(bean));
        assertFalse(new OAContainsFilter("child.value", "x").isUsed(bean));
        assertFalse(new OAStartsWithFilter("child.value", "x").isUsed(bean));
        assertFalse(new OAIndexOfFilter("child.value", "x").isUsed(bean));
    }
}
