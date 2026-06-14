package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAGreaterOrEqualFilterTest {


    @Test
    void valueConstructorComparesDirectValues() {
        assertTrue(new OAGreaterOrEqualFilter(10).isUsed(10));
        assertFalse(new OAGreaterOrEqualFilter(10).isUsed(9));
    }

    @Test
    void pathConstructorsCompareResolvedPropertyValues() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OAGreaterOrEqualFilter(FilterTestSupport.PRICE_PATH, 12.50).isUsed(graph.invoice));
        assertFalse(new OAGreaterOrEqualFilter(new com.viaoa.path.OAPath(FilterTestSupport.PRICE_PATH), 13).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OAGreaterOrEqualFilter filter = new OAGreaterOrEqualFilter(FilterTestSupport.PRICE_PATH, 20);

        assertEquals(12.50, (Double) filter.getPropertyValue(FilterTestSupport.graph().invoice), 0.001);
    }
}
