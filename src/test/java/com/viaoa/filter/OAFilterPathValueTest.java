package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterPathValueTest {

    public static class Bean {
        private String name;
        private Integer age;
        private Double amount;
        private Boolean active;
        private Bean child;

        public Bean(String name, Integer age, Double amount, Boolean active) {
            this.name = name;
            this.age = age;
            this.amount = amount;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public Double getAmount() {
            return amount;
        }

        public Boolean getActive() {
            return active;
        }

        public Bean getChild() {
            return child;
        }

        public void setChild(Bean child) {
            this.child = child;
        }
    }

    @Test
    void equalFilterResolvesSimplePropertyPath() {
        Bean bean = new Bean("Bob", 42, 10.25, true);

        assertTrue(new OAEqualFilter("name", "Bob").isUsed(bean));
        assertFalse(new OAEqualFilter("name", "Sue").isUsed(bean));
    }

    @Test
    void equalFilterPathIgnoreCaseAndDecimalPlaces() {
        Bean bean = new Bean("Bob", 42, 1.0044, true);

        assertFalse(new OAEqualFilter("name", "bob").isUsed(bean));

        OAEqualFilter ignoreCase = new OAEqualFilter("name", "bob", true);
        assertTrue(ignoreCase.isUsed(bean));

        OAEqualFilter dp = new OAEqualFilter("amount", 1.004, 2);
        assertTrue(dp.isUsed(bean));

        OAEqualFilter dpMiss = new OAEqualFilter("amount", 1.014, 2);
        assertFalse(dpMiss.isUsed(bean));
    }

    @Test
    void notEqualFilterResolvesSimplePropertyPath() {
        Bean bean = new Bean("Bob", 42, 10.25, true);

        assertFalse(new OANotEqualFilter("name", "Bob").isUsed(bean));
        assertTrue(new OANotEqualFilter("name", "Sue").isUsed(bean));
        assertFalse(new OANotEqualFilter("name", "bob", true).isUsed(bean));
    }

    @Test
    void relationalFiltersResolveNumericPropertyPath() {
        Bean bean = new Bean("Bob", 42, 10.25, true);

        assertTrue(new OAGreaterFilter("age", 40).isUsed(bean));
        assertFalse(new OAGreaterFilter("age", 42).isUsed(bean));

        assertTrue(new OAGreaterOrEqualFilter("age", 42).isUsed(bean));
        assertFalse(new OAGreaterOrEqualFilter("age", 43).isUsed(bean));

        assertTrue(new OALessFilter("age", 50).isUsed(bean));
        assertFalse(new OALessFilter("age", 42).isUsed(bean));

        assertTrue(new OALessOrEqualFilter("age", 42).isUsed(bean));
        assertFalse(new OALessOrEqualFilter("age", 41).isUsed(bean));
    }

    @Test
    void rangeFiltersResolveNumericPropertyPath() {
        Bean bean = new Bean("Bob", 42, 10.25, true);

        assertTrue(new OABetweenFilter<>("age", 40, 50).isUsed(bean));
        assertFalse(new OABetweenFilter<>("age", 42, 50).isUsed(bean));
        assertFalse(new OABetweenFilter<>("age", 40, 42).isUsed(bean));

        assertTrue(new OABetweenOrEqualFilter("age", 42, 50).isUsed(bean));
        assertTrue(new OABetweenOrEqualFilter("age", 40, 42).isUsed(bean));
        assertFalse(new OABetweenOrEqualFilter("age", 43, 50).isUsed(bean));
    }

    @Test
    void booleanTrueFalseFiltersResolvePropertyPath() {
        Bean active = new Bean("Bob", 42, 10.25, true);
        Bean inactive = new Bean("Sue", 39, 8.0, false);
        Bean unknown = new Bean("Nul", 39, 8.0, null);

        assertTrue(new OATrueFilter("active").isUsed(active));
        assertFalse(new OATrueFilter("active").isUsed(inactive));
        assertFalse(new OATrueFilter("active").isUsed(unknown));

        assertFalse(new OAFalseFilter("active").isUsed(active));
        assertTrue(new OAFalseFilter("active").isUsed(inactive));
        assertFalse(new OAFalseFilter("active").isUsed(unknown));
    }

    @Test
    void nullAndNotNullFiltersResolvePropertyPath() {
        Bean bean = new Bean(null, 42, 10.25, true);

        assertTrue(new OANullFilter("name").isUsed(bean));
        assertFalse(new OANotNullFilter("name").isUsed(bean));

        assertFalse(new OANullFilter("age").isUsed(bean));
        assertTrue(new OANotNullFilter("age").isUsed(bean));
    }

    @Test
    void emptyAndNotEmptyFiltersResolvePropertyPath() {
        Bean emptyName = new Bean("", 0, 0.0, false);
        Bean fullName = new Bean("Bob", 42, 10.25, true);

        assertTrue(new OAEmptyFilter("name").isUsed(emptyName));
        assertFalse(new OANotEmptyFilter("name").isUsed(emptyName));

        assertFalse(new OAEmptyFilter("name").isUsed(fullName));
        assertTrue(new OANotEmptyFilter("name").isUsed(fullName));

        assertTrue(new OAEmptyFilter("age").isUsed(emptyName));
        assertFalse(new OANotEmptyFilter("age").isUsed(emptyName));
    }

    @Test
    void nestedPathFiltersResolveThroughSingleValuedChild() {
        Bean parent = new Bean("Parent", 1, 1.0, true);
        Bean child = new Bean("Child", 9, 2.0, false);
        parent.setChild(child);

        assertTrue(new OAEqualFilter("child.name", "Child").isUsed(parent));
        assertTrue(new OALessFilter("child.age", 10).isUsed(parent));
        assertTrue(new OAFalseFilter("child.active").isUsed(parent));
        assertFalse(new OATrueFilter("child.active").isUsed(parent));
    }

    @Test
    void nestedPathThroughNullChildReturnsFalseUnlessTestingNullResolvedValueCurrentContract() {
        Bean parent = new Bean("Parent", 1, 1.0, true);

        assertFalse(new OAEqualFilter("child.name", "Child").isUsed(parent));
        assertFalse(new OALessFilter("child.age", 10).isUsed(parent));
        assertTrue(new OANullFilter("child.name").isUsed(parent));
        assertFalse(new OANotNullFilter("child.name").isUsed(parent));
    }

    @Test
    void pathFiltersHandleNullCandidateDeterministically() {
        assertFalse(new OAEqualFilter("name", "Bob").isUsed(null));
        assertFalse(new OANotEqualFilter("name", "Bob").isUsed(null));
        assertFalse(new OAGreaterFilter("age", 1).isUsed(null));
        assertFalse(new OALessFilter("age", 1).isUsed(null));
        assertFalse(new OANotNullFilter("name").isUsed(null));

        assertTrue(new OANullFilter("name").isUsed(null));
        assertTrue(new OAEmptyFilter("name").isUsed(null));
        assertFalse(new OANotEmptyFilter("name").isUsed(null));
    }
}
