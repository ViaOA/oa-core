package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterPackageContractTest {

    @Test
    void oaFilterIsFunctionalAndSerializable() {
        OAFilter f = obj -> obj != null;

        assertTrue(f instanceof java.io.Serializable);
        assertTrue(f.isUsed("x"));
        assertFalse(f.isUsed(null));
    }

    @Test
    void directFiltersAreSerializable() {
        assertTrue(new OAEqualFilter("x") instanceof java.io.Serializable);
        assertTrue(new OAGreaterFilter(1) instanceof java.io.Serializable);
        assertTrue(new OALikeFilter("*") instanceof java.io.Serializable);
        assertTrue(new OATrueFilter() instanceof java.io.Serializable);
    }
}
