package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAContainsFilterTest {


    @Test
    void valueConstructorsCompareDirectStrings() {
        assertTrue(new OAContainsFilter("Pad").isUsed("Brake Pad"));
        assertFalse(new OAContainsFilter("pad").isUsed("Brake Pad"));
        assertTrue(new OAContainsFilter("pad", true).isUsed("Brake Pad"));
    }

    @Test
    void pathConstructorsCompareResolvedPropertyStrings() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OAContainsFilter(FilterTestSupport.ITEM_NAME_PATH, "Pad").isUsed(graph.invoice));
        assertTrue(new OAContainsFilter(new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH), "pad", true).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OAContainsFilter filter = new OAContainsFilter(FilterTestSupport.ITEM_NAME_PATH, "Pad");

        assertEquals("Brake Pad", filter.getPropertyValue(FilterTestSupport.graph().invoice));
    }
}
