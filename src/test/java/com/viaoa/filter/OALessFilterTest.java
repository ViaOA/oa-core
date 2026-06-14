package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OALessFilterTest {


    @Test
    void valueConstructorComparesDirectValues() {
        assertTrue(new OALessFilter(10).isUsed(9));
        assertFalse(new OALessFilter(10).isUsed(10));
    }

    @Test
    void pathConstructorsCompareResolvedPropertyValues() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OALessFilter(FilterTestSupport.PRICE_PATH, 13).isUsed(graph.invoice));
        assertFalse(new OALessFilter(new com.viaoa.path.OAPath(FilterTestSupport.PRICE_PATH), 12.50).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OALessFilter filter = new OALessFilter(FilterTestSupport.PRICE_PATH, 20);

        assertEquals(12.50, (Double) filter.getPropertyValue(FilterTestSupport.graph().invoice), 0.001);
    }
}
