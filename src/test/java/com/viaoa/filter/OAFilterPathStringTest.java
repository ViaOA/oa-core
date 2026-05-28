package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterPathStringTest {

    public static class Bean {
        private String name;
        private String code;
        private Bean child;

        public Bean(String name, String code) {
            this.name = name;
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public Bean getChild() {
            return child;
        }

        public void setChild(Bean child) {
            this.child = child;
        }
    }

    @Test
    void likeAndNotLikeResolvePropertyPath() {
        Bean bean = new Bean("abcdef", "X1");

        assertTrue(new OALikeFilter("name", "ab*ef").isUsed(bean));
        assertTrue(new OALikeFilter("name", "ab%ef").isUsed(bean));
        assertFalse(new OALikeFilter("name", "ab*z").isUsed(bean));

        assertFalse(new OANotLikeFilter("name", "ab*ef").isUsed(bean));
        assertTrue(new OANotLikeFilter("name", "ab*z").isUsed(bean));
    }

    @Test
    void containsFilterResolvesPropertyPath() {
        Bean bean = new Bean("abcdef", "X1");

        assertTrue(new OAContainsFilter("name", "bc").isUsed(bean));
        assertFalse(new OAContainsFilter("name", "BC").isUsed(bean));
        assertTrue(new OAContainsFilter("name", "BC", true).isUsed(bean));
        assertFalse(new OAContainsFilter("name", "xy").isUsed(bean));
    }

    @Test
    void startsWithFilterResolvesPropertyPath() {
        Bean bean = new Bean("abcdef", "X1");

        assertTrue(new OAStartsWithFilter("name", "ab").isUsed(bean));
        assertFalse(new OAStartsWithFilter("name", "AB").isUsed(bean));
        assertTrue(new OAStartsWithFilter("name", "AB", true).isUsed(bean));
        assertFalse(new OAStartsWithFilter("name", "bc").isUsed(bean));
    }

    @Test
    void indexOfFilterResolvesPropertyPath() {
        Bean bean = new Bean("abcdef", "X1");

        assertTrue(new OAIndexOfFilter("name", "bc").isUsed(bean));
        assertFalse(new OAIndexOfFilter("name", "BC").isUsed(bean));
        assertTrue(new OAIndexOfFilter("name", "BC", true).isUsed(bean));
        assertFalse(new OAIndexOfFilter("name", "xy").isUsed(bean));
    }

    @Test
    void stringFiltersResolveNestedPropertyPath() {
        Bean parent = new Bean("parent", "P");
        parent.setChild(new Bean("abcdef", "C"));

        assertTrue(new OALikeFilter("child.name", "ab*ef").isUsed(parent));
        assertTrue(new OAContainsFilter("child.name", "cd").isUsed(parent));
        assertTrue(new OAStartsWithFilter("child.name", "ab").isUsed(parent));
        assertTrue(new OAIndexOfFilter("child.name", "de").isUsed(parent));
    }

    @Test
    void stringFiltersThroughNullChildReturnFalseOrNegationCurrentContract() {
        Bean parent = new Bean("parent", "P");

        assertFalse(new OALikeFilter("child.name", "*").isUsed(parent));
        assertTrue(new OANotLikeFilter("child.name", "*").isUsed(parent));
        assertFalse(new OAContainsFilter("child.name", "x").isUsed(parent));
        assertFalse(new OAStartsWithFilter("child.name", "x").isUsed(parent));
        assertFalse(new OAIndexOfFilter("child.name", "x").isUsed(parent));
    }

    @Test
    void stringFiltersHandleNullCandidateWithPathDeterministically() {
        assertFalse(new OALikeFilter("name", "*").isUsed(null));
        assertTrue(new OANotLikeFilter("name", "*").isUsed(null));
        assertFalse(new OAContainsFilter("name", "x").isUsed(null));
        assertFalse(new OAStartsWithFilter("name", "x").isUsed(null));
        assertFalse(new OAIndexOfFilter("name", "x").isUsed(null));
    }
}
