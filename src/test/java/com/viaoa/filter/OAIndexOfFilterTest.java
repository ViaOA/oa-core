package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAIndexOfFilterTest {


    @Test
    void valueConstructorsCompareDirectStrings() {
        assertTrue(new OAIndexOfFilter("Pad").isUsed("Brake Pad"));
        assertFalse(new OAIndexOfFilter("pad").isUsed("Brake Pad"));
        assertTrue(new OAIndexOfFilter("pad", true).isUsed("Brake Pad"));
    }

    @Test
    void pathConstructorsCompareResolvedPropertyStrings() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OAIndexOfFilter(FilterTestSupport.ITEM_NAME_PATH, "Pad").isUsed(graph.invoice));
        assertTrue(new OAIndexOfFilter(new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH), "pad", true).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OAIndexOfFilter filter = new OAIndexOfFilter(FilterTestSupport.ITEM_NAME_PATH, "Pad");

        assertEquals("Brake Pad", filter.getPropertyValue(FilterTestSupport.graph().invoice));
    }
}
