package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterComparisonDirectTest {

    @Test
    void equalFilterUsesOACompareForDirectValues() {
        assertTrue(new OAEqualFilter("5").isUsed("5"));
        assertTrue(new OAEqualFilter("5").isUsed("5.00"));
        assertFalse(new OAEqualFilter("5").isUsed("6"));
    }

    @Test
    void equalFilterIgnoreCaseAppliesToDirectStringValues() {
        assertFalse(new OAEqualFilter("abc").isUsed("ABC"));

        OAEqualFilter f = new OAEqualFilter("abc");
        f.setIgnoreCase(true);
        assertTrue(f.isUsed("ABC"));
    }

    @Test
    void equalFilterDecimalPlacesAppliesToDirectNumericValues() {
        OAEqualFilter f = new OAEqualFilter(null, 1.0044d, 2);

        assertTrue(f.isUsed(1.004d));
        assertFalse(f.isUsed(1.014d));
    }

    @Test
    void notEqualFilterNegatesEqualFilterSemantics() {
        assertFalse(new OANotEqualFilter("5").isUsed("5.00"));
        assertTrue(new OANotEqualFilter("5").isUsed("6"));
    }

    @Test
    void notEqualFilterIgnoreCaseAppliesToDirectStringValues() {
        assertTrue(new OANotEqualFilter("abc").isUsed("ABC"));
        assertFalse(new OANotEqualFilter("abc", true).isUsed("ABC"));
    }

    @Test
    void relationalFiltersUseOACompareOrdering() {
        assertTrue(new OAGreaterFilter(5).isUsed(6));
        assertFalse(new OAGreaterFilter(5).isUsed(5));
        assertFalse(new OAGreaterFilter(5).isUsed(4));

        assertTrue(new OAGreaterOrEqualFilter(5).isUsed(6));
        assertTrue(new OAGreaterOrEqualFilter(5).isUsed(5));
        assertFalse(new OAGreaterOrEqualFilter(5).isUsed(4));

        assertTrue(new OALessFilter(5).isUsed(4));
        assertFalse(new OALessFilter(5).isUsed(5));
        assertFalse(new OALessFilter(5).isUsed(6));

        assertTrue(new OALessOrEqualFilter(5).isUsed(4));
        assertTrue(new OALessOrEqualFilter(5).isUsed(5));
        assertFalse(new OALessOrEqualFilter(5).isUsed(6));
    }

    @Test
    void betweenFilterExcludesBoundaries() {
        OABetweenFilter<Integer> f = new OABetweenFilter<>(5, 10);

        assertFalse(f.isUsed(5));
        assertTrue(f.isUsed(6));
        assertTrue(f.isUsed(9));
        assertFalse(f.isUsed(10));
    }

    @Test
    void betweenOrEqualFilterIncludesBoundaries() {
        OABetweenOrEqualFilter f = new OABetweenOrEqualFilter(5, 10);

        assertFalse(f.isUsed(4));
        assertTrue(f.isUsed(5));
        assertTrue(f.isUsed(6));
        assertTrue(f.isUsed(10));
        assertFalse(f.isUsed(11));
    }

    @Test
    void relationalFiltersRejectNonComparableCurrentContract() {
        Object obj = new Object();

        assertFalse(new OAGreaterFilter(5).isUsed(obj));
        assertFalse(new OAGreaterOrEqualFilter(5).isUsed(obj));
        assertFalse(new OALessFilter(5).isUsed(obj));
        assertFalse(new OALessOrEqualFilter(5).isUsed(obj));
    }
}
