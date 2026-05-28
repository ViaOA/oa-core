package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterNegationConsistencyTest {

    public static class Bean {
        private String name;
        private Integer age;
        private String text;

        public Bean(String name, Integer age, String text) {
            this.name = name;
            this.age = age;
            this.text = text;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public String getText() {
            return text;
        }
    }

    @Test
    void notEqualIsNegationOfEqualForRepresentativeDirectValues() {
        Object[] values = { null, "", "abc", "5", 5, 5.0, true, false };

        for (Object match : values) {
            for (Object candidate : values) {
                assertEquals(
                    !new OAEqualFilter(match).isUsed(candidate),
                    new OANotEqualFilter(match).isUsed(candidate),
                    "match=" + match + ", candidate=" + candidate
                );
            }
        }
    }

    @Test
    void notLikeIsNegationOfLikeForRepresentativeDirectValues() {
        String[] candidates = { null, "", "abc", "abcdef", "xyz" };
        String[] patterns = { "*", "ab*", "*ef", "a*c", "z*" };

        for (String pattern : patterns) {
            for (String candidate : candidates) {
                assertEquals(
                    !new OALikeFilter(pattern).isUsed(candidate),
                    new OANotLikeFilter(pattern).isUsed(candidate),
                    "pattern=" + pattern + ", candidate=" + candidate
                );
            }
        }
    }

    @Test
    void notEmptyIsNegationOfEmptyForPathValues() {
        Bean empty = new Bean("", 0, null);
        Bean full = new Bean("Bob", 42, "abc");

        assertEquals(!new OAEmptyFilter("name").isUsed(empty), new OANotEmptyFilter("name").isUsed(empty));
        assertEquals(!new OAEmptyFilter("name").isUsed(full), new OANotEmptyFilter("name").isUsed(full));

        assertEquals(!new OAEmptyFilter("age").isUsed(empty), new OANotEmptyFilter("age").isUsed(empty));
        assertEquals(!new OAEmptyFilter("age").isUsed(full), new OANotEmptyFilter("age").isUsed(full));

        assertEquals(!new OAEmptyFilter("text").isUsed(empty), new OANotEmptyFilter("text").isUsed(empty));
        assertEquals(!new OAEmptyFilter("text").isUsed(full), new OANotEmptyFilter("text").isUsed(full));
    }

    @Test
    void notNullIsNegationOfNullForPathValues() {
        Bean empty = new Bean(null, null, null);
        Bean full = new Bean("Bob", 42, "abc");

        assertEquals(!new OANullFilter("name").isUsed(empty), new OANotNullFilter("name").isUsed(empty));
        assertEquals(!new OANullFilter("name").isUsed(full), new OANotNullFilter("name").isUsed(full));

        assertEquals(!new OANullFilter("age").isUsed(empty), new OANotNullFilter("age").isUsed(empty));
        assertEquals(!new OANullFilter("age").isUsed(full), new OANotNullFilter("age").isUsed(full));
    }

    @Test
    void notEqualPathFilterNegatesEqualPathFilterForRepresentativeValues() {
        Bean bean = new Bean("Bob", 42, "abc");

        assertEquals(!new OAEqualFilter("name", "Bob").isUsed(bean), new OANotEqualFilter("name", "Bob").isUsed(bean));
        assertEquals(!new OAEqualFilter("age", 42).isUsed(bean), new OANotEqualFilter("age", 42).isUsed(bean));
        assertEquals(!new OAEqualFilter("text", "xyz").isUsed(bean), new OANotEqualFilter("text", "xyz").isUsed(bean));
    }
}
