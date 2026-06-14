package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAXorFilterTest {


    @Test
    void constructorAndIsUsedAcceptExactlyOneTrueFilter() {
        OAFilter<Object> t = obj -> true;
        OAFilter<Object> f = obj -> false;

        assertTrue(new OAXorFilter(t, f).isUsed("x"));
        assertTrue(new OAXorFilter(f, t).isUsed("x"));
        assertFalse(new OAXorFilter(t, t).isUsed("x"));
        assertFalse(new OAXorFilter(f, f).isUsed("x"));
        assertTrue(new OAXorFilter(null, null).isUsed("x"));
    }
}
