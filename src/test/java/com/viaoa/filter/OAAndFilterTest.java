package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAAndFilterTest {


    @Test
    void constructorAndIsUsedRequireBothFiltersWhenPresent() {
        OAFilter<Object> t = obj -> true;
        OAFilter<Object> f = obj -> false;

        assertTrue(new OAAndFilter(t, t).isUsed("x"));
        assertFalse(new OAAndFilter(f, t).isUsed("x"));
        assertFalse(new OAAndFilter(t, f).isUsed("x"));
        assertTrue(new OAAndFilter(null, t).isUsed("x"));
    }

    @Test
    void updateSelectReturnsTrueWhenEitherDelegateNeedsMemoryFilter() {
        OAFilter<Object> t = new OAFilter<Object>() {
            public boolean isUsed(Object obj) { return true; }
            public boolean updateSelect(com.viaoa.select.OASelect select) { return true; }
        };
        OAFilter<Object> f = new OAFilter<Object>() {
            public boolean isUsed(Object obj) { return true; }
            public boolean updateSelect(com.viaoa.select.OASelect select) { return false; }
        };

        assertTrue(new OAAndFilter(t, f).updateSelect(null));
        assertFalse(new OAAndFilter(f, f).updateSelect(null));
    }
}
