package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OALikeFilterTest {


    @Test
    void valueConstructorUsesLikeWildcardMatching() {
        assertTrue(new OALikeFilter("Brake*").isUsed("Brake Pad"));
        assertFalse(new OALikeFilter("Rotor*").isUsed("Brake Pad"));
    }

    @Test
    void pathConstructorsCompareResolvedPropertyStrings() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OALikeFilter(FilterTestSupport.ITEM_NAME_PATH, "Brake*").isUsed(graph.invoice));
        assertTrue(new OALikeFilter(new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH), "*Pad")
                .isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OALikeFilter filter = new OALikeFilter(FilterTestSupport.ITEM_NAME_PATH, "Brake*");

        assertEquals("Brake Pad", filter.getPropertyValue(FilterTestSupport.graph().invoice));
    }
}
