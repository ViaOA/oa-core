package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class OAFilterNullEmptyDirectTest {

    @Test
    void nullAndNotNullFiltersUseDirectCandidateWhenNoPath() {
        assertTrue(new OANullFilter().isUsed(null));
        assertFalse(new OANullFilter().isUsed("x"));

        assertFalse(new OANotNullFilter().isUsed(null));
        assertTrue(new OANotNullFilter().isUsed("x"));
    }

    @Test
    void emptyFilterMatchesOAEmptyValues() {
        OAEmptyFilter f = new OAEmptyFilter();

        assertTrue(f.isUsed(null));
        assertTrue(f.isUsed(""));
        assertTrue(f.isUsed(0));
        assertTrue(f.isUsed(0L));
        assertTrue(f.isUsed(0.0d));
        assertTrue(f.isUsed(-0.0d));
        assertTrue(f.isUsed(BigDecimal.ZERO));
        assertTrue(f.isUsed(false));
        assertTrue(f.isUsed(new Object[0]));
        assertTrue(f.isUsed(Collections.emptyList()));
        assertTrue(f.isUsed(Collections.emptyMap()));
    }

    @Test
    void emptyFilterRejectsNonEmptyValues() {
        OAEmptyFilter f = new OAEmptyFilter();

        assertFalse(f.isUsed("x"));
        assertFalse(f.isUsed(1));
        assertFalse(f.isUsed(true));
        assertFalse(f.isUsed(new Object[] { "x" }));
        assertFalse(f.isUsed(Collections.singletonList("x")));
        assertFalse(f.isUsed(Collections.singletonMap("x", "y")));
    }

    @Test
    void notEmptyFilterIsLogicalOppositeForDirectValues() {
        OANotEmptyFilter f = new OANotEmptyFilter();

        assertFalse(f.isUsed(null));
        assertFalse(f.isUsed(""));
        assertFalse(f.isUsed(0));
        assertFalse(f.isUsed(false));

        assertTrue(f.isUsed("x"));
        assertTrue(f.isUsed(1));
        assertTrue(f.isUsed(true));
        assertTrue(f.isUsed(Collections.singletonList("x")));
    }
}
