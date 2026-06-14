package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterTest {


    @Test
    void isUsedCanBeImplementedByLambda() {
        OAFilter<String> filter = value -> value != null && value.startsWith("A");

        assertTrue(filter.isUsed("ACME"));
        assertFalse(filter.isUsed("Brake"));
    }

    @Test
    void updateSelectDefaultsToTrue() {
        OAFilter<Object> filter = obj -> true;

        assertTrue(filter.updateSelect(null));
    }
}
