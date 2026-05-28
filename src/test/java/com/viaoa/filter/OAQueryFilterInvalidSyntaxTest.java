package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryFilterInvalidSyntaxTest {

    public static class Bean {
        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }
    }

    @Test
    void missingLeftOperandThrows() {
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "= 'Bob'"));
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "> 5"));
    }

    @Test
    void missingRightOperandThrows() {
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "name ="));
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "age >"));
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "name LIKE"));
    }

    @Test
    void danglingLogicalOperatorsThrow() {
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "name = 'Bob' AND"));
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "name = 'Bob' OR"));
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "AND name = 'Bob'"));
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "OR name = 'Bob'"));
    }

    @Test
    void malformedInExpressionsThrow() {
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "name IN"));
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "name IN ()"));
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "name IN ('A'"));
    }

    @Test
    void malformedNotInExpressionThrows() {
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "name NOT"));
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "name NOT IN"));
    }

    @Test
    void completelyUnknownExpressionThrows() {
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(Bean.class, "this is not a query"));
    }
}
