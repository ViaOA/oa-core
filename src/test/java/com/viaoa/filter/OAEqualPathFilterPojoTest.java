package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAEqualPathFilterPojoTest {

    public static class Bean {
        private String first;
        private String second;
        private Integer left;
        private Integer right;
        private Bean child;

        public Bean(String first, String second, Integer left, Integer right) {
            this.first = first;
            this.second = second;
            this.left = left;
            this.right = right;
        }

        public String getFirst() {
            return first;
        }

        public String getSecond() {
            return second;
        }

        public Integer getLeft() {
            return left;
        }

        public Integer getRight() {
            return right;
        }

        public Bean getChild() {
            return child;
        }

        public void setChild(Bean child) {
            this.child = child;
        }
    }

    @Test
    void equalPathFilterComparesTwoPathsOnSourceObject() {
        Bean bean = new Bean("same", "same", 5, 5);

        OAEqualPathFilter f1 = new OAEqualPathFilter(null, "first", "second");
        OAEqualPathFilter f2 = new OAEqualPathFilter(null, "left", "right");

        assertTrue(f1.isUsed(bean));
        assertTrue(f2.isUsed(bean));
    }

    @Test
    void equalPathFilterRejectsDifferentPathValues() {
        Bean bean = new Bean("one", "two", 5, 6);

        assertFalse(new OAEqualPathFilter(null, "first", "second").isUsed(bean));
        assertFalse(new OAEqualPathFilter(null, "left", "right").isUsed(bean));
    }

    @Test
    void equalPathFilterUsesOACompareScalarSemantics() {
        Bean bean = new Bean("5", "5.00", 5, 5);

        assertTrue(new OAEqualPathFilter(null, "first", "second").isUsed(bean));
    }

    @Test
    void equalPathFilterSupportsNestedCandidatePath() {
        Bean parent = new Bean("parent", "parent", 1, 1);
        parent.setChild(new Bean("same", "same", 9, 9));

        assertTrue(new OAEqualPathFilter(null, "child.first", "child.second").isUsed(parent));
        assertTrue(new OAEqualPathFilter(null, "child.left", "child.right").isUsed(parent));
    }

    @Test
    void equalPathFilterHandlesNullCandidateCurrentContract() {
        assertFalse(new OAEqualPathFilter(null, "first", "second").isUsed(null));
    }

    @Test
    void equalPathFilterHandlesNullResolvedPathValuesCurrentContract() {
        Bean bean = new Bean(null, null, null, null);

        assertTrue(new OAEqualPathFilter(null, "first", "second").isUsed(bean));
        assertTrue(new OAEqualPathFilter(null, "left", "right").isUsed(bean));
    }
}
