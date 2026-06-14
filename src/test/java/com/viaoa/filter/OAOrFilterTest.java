package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAOrFilterTest {


    @Test
    void constructorAndIsUsedAcceptWhenEitherFilterAccepts() {
        OAFilter<Object> t = obj -> true;
        OAFilter<Object> f = obj -> false;

        assertTrue(new OAOrFilter(t, f).isUsed("x"));
        assertTrue(new OAOrFilter(f, t).isUsed("x"));
        assertFalse(new OAOrFilter(f, f).isUsed("x"));
        assertTrue(new OAOrFilter(null, null).isUsed("x"));
    }
}
