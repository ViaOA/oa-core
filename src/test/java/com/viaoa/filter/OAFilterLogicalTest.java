package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class OAFilterLogicalTest {

    private static OAFilter always(boolean value) {
        return obj -> value;
    }

    @Test
    void trueAndFalseConstantFiltersIgnoreCandidateValue() {
        OATrueFilter tf = new OATrueFilter();
        OAFalseFilter ff = new OAFalseFilter();

        assertTrue(tf.isUsed(null));
        assertTrue(tf.isUsed(Boolean.FALSE));
        assertTrue(tf.isUsed("anything"));

        assertFalse(ff.isUsed(null));
        assertFalse(ff.isUsed(Boolean.FALSE));
        assertFalse(ff.isUsed("anything"));
    }

    @Test
    void andFilterRequiresBothFiltersWhenPresent() {
        assertTrue(new OAAndFilter(always(true), always(true)).isUsed("x"));
        assertFalse(new OAAndFilter(always(true), always(false)).isUsed("x"));
        assertFalse(new OAAndFilter(always(false), always(true)).isUsed("x"));
        assertFalse(new OAAndFilter(always(false), always(false)).isUsed("x"));
    }

    @Test
    void andFilterTreatsNullDelegateAsAccepting() {
        assertTrue(new OAAndFilter(null, null).isUsed("x"));
        assertTrue(new OAAndFilter(null, always(true)).isUsed("x"));
        assertFalse(new OAAndFilter(null, always(false)).isUsed("x"));
        assertTrue(new OAAndFilter(always(true), null).isUsed("x"));
        assertFalse(new OAAndFilter(always(false), null).isUsed("x"));
    }

    @Test
    void andFilterShortCircuitsWhenFirstRejects() {
        AtomicInteger calls = new AtomicInteger();
        OAFilter second = obj -> {
            calls.incrementAndGet();
            return true;
        };

        assertFalse(new OAAndFilter(always(false), second).isUsed("x"));
        assertEquals(0, calls.get());
    }

    @Test
    void orFilterAcceptsWhenEitherFilterAccepts() {
        assertTrue(new OAOrFilter(always(true), always(true)).isUsed("x"));
        assertTrue(new OAOrFilter(always(true), always(false)).isUsed("x"));
        assertTrue(new OAOrFilter(always(false), always(true)).isUsed("x"));
        assertFalse(new OAOrFilter(always(false), always(false)).isUsed("x"));
    }

    @Test
    void orFilterTreatsBothNullDelegatesAsAccepting() {
        assertTrue(new OAOrFilter(null, null).isUsed("x"));
        assertTrue(new OAOrFilter(null, always(true)).isUsed("x"));
        assertFalse(new OAOrFilter(null, always(false)).isUsed("x"));
        assertTrue(new OAOrFilter(always(true), null).isUsed("x"));
        assertFalse(new OAOrFilter(always(false), null).isUsed("x"));
    }

    @Test
    void orFilterShortCircuitsWhenFirstAccepts() {
        AtomicInteger calls = new AtomicInteger();
        OAFilter second = obj -> {
            calls.incrementAndGet();
            return false;
        };

        assertTrue(new OAOrFilter(always(true), second).isUsed("x"));
        assertEquals(0, calls.get());
    }

    @Test
    void xorFilterAcceptsExactlyOneTrueDelegate() {
        assertFalse(new OAXorFilter(always(true), always(true)).isUsed("x"));
        assertTrue(new OAXorFilter(always(true), always(false)).isUsed("x"));
        assertTrue(new OAXorFilter(always(false), always(true)).isUsed("x"));
        assertFalse(new OAXorFilter(always(false), always(false)).isUsed("x"));
    }

    @Test
    void xorFilterTreatsBothNullDelegatesAsAcceptingCurrentContract() {
        assertTrue(new OAXorFilter(null, null).isUsed("x"));
        assertTrue(new OAXorFilter(null, always(true)).isUsed("x"));
        assertFalse(new OAXorFilter(null, always(false)).isUsed("x"));
        assertTrue(new OAXorFilter(always(true), null).isUsed("x"));
        assertFalse(new OAXorFilter(always(false), null).isUsed("x"));
    }

    @Test
    void blockFilterRequiresEveryContainedFilter() {
        assertTrue(new OABlockFilter(always(true), always(true), always(true)).isUsed("x"));
        assertFalse(new OABlockFilter(always(true), always(false), always(true)).isUsed("x"));
    }

    @Test
    void blockFilterWithNullArrayAcceptsAllCandidates() {
        assertTrue(new OABlockFilter((OAFilter[]) null).isUsed("x"));
        assertTrue(new OABlockFilter((OAFilter[]) null).isUsed(null));
    }
}
