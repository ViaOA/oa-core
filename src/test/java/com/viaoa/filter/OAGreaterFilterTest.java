package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAGreaterFilterTest {


    @Test
    void valueConstructorComparesDirectValues() {
        assertTrue(new OAGreaterFilter(10).isUsed(11));
        assertFalse(new OAGreaterFilter(10).isUsed(10));
    }

    @Test
    void pathConstructorsCompareResolvedPropertyValues() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OAGreaterFilter(FilterTestSupport.PRICE_PATH, 10).isUsed(graph.invoice));
        assertFalse(new OAGreaterFilter(new com.viaoa.path.OAPath(FilterTestSupport.PRICE_PATH), 20).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OAGreaterFilter filter = new OAGreaterFilter(FilterTestSupport.PRICE_PATH, 20);

        assertEquals(12.50, (Double) filter.getPropertyValue(FilterTestSupport.graph().invoice), 0.001);
    }
}
