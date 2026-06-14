package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OABlockFilterTest {


    @Test
    void constructorAndIsUsedApplyAndBlockSemantics() {
        OAFilter<Object> t = obj -> true;
        OAFilter<Object> f = obj -> false;

        assertTrue(new OABlockFilter(t, t).isUsed("x") && !new OABlockFilter(t, f).isUsed("x") && new OABlockFilter((OAFilter[]) null).isUsed("x"));
    }
}
