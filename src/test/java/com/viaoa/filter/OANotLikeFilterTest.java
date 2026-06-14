package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OANotLikeFilterTest {


    @Test
    void valueConstructorNegatesLikeWildcardMatching() {
        assertFalse(new OANotLikeFilter("Brake*").isUsed("Brake Pad"));
        assertTrue(new OANotLikeFilter("Rotor*").isUsed("Brake Pad"));
    }

    @Test
    void pathConstructorsCompareResolvedPropertyStrings() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertFalse(new OANotLikeFilter(FilterTestSupport.ITEM_NAME_PATH, "Brake*").isUsed(graph.invoice));
        assertTrue(new OANotLikeFilter(new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH), "Rotor*")
                .isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OANotLikeFilter filter = new OANotLikeFilter(FilterTestSupport.ITEM_NAME_PATH, "Rotor*");

        assertEquals("Brake Pad", filter.getPropertyValue(FilterTestSupport.graph().invoice));
    }
}
