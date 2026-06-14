package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OALessOrEqualFilterTest {


    @Test
    void valueConstructorComparesDirectValues() {
        assertTrue(new OALessOrEqualFilter(10).isUsed(10));
        assertFalse(new OALessOrEqualFilter(10).isUsed(11));
    }

    @Test
    void pathConstructorsCompareResolvedPropertyValues() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OALessOrEqualFilter(FilterTestSupport.PRICE_PATH, 12.50).isUsed(graph.invoice));
        assertFalse(new OALessOrEqualFilter(new com.viaoa.path.OAPath(FilterTestSupport.PRICE_PATH), 12).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OALessOrEqualFilter filter = new OALessOrEqualFilter(FilterTestSupport.PRICE_PATH, 20);

        assertEquals(12.50, (Double) filter.getPropertyValue(FilterTestSupport.graph().invoice), 0.001);
    }
}
