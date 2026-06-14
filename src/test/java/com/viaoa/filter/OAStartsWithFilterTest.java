package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAStartsWithFilterTest {


    @Test
    void valueConstructorsCompareDirectStrings() {
        assertTrue(new OAStartsWithFilter("Brake").isUsed("Brake Pad"));
        assertFalse(new OAStartsWithFilter("brake").isUsed("Brake Pad"));
        assertTrue(new OAStartsWithFilter("brake", true).isUsed("Brake Pad"));
    }

    @Test
    void pathConstructorsCompareResolvedPropertyStrings() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OAStartsWithFilter(FilterTestSupport.ITEM_NAME_PATH, "Brake").isUsed(graph.invoice));
        assertTrue(new OAStartsWithFilter(new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH), "brake", true).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OAStartsWithFilter filter = new OAStartsWithFilter(FilterTestSupport.ITEM_NAME_PATH, "Brake");

        assertEquals("Brake Pad", filter.getPropertyValue(FilterTestSupport.graph().invoice));
    }
}
