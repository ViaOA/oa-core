package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OABetweenOrEqualFilterTest {


    @Test
    void valueConstructorUsesInclusiveBounds() {
        assertTrue(new OABetweenOrEqualFilter(10, 20).isUsed(10));
        assertFalse(new OABetweenOrEqualFilter(10, 20).isUsed(9));
    }

    @Test
    void pathConstructorsCompareResolvedPropertyValues() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OABetweenOrEqualFilter(FilterTestSupport.PRICE_PATH, 12.50, 20).isUsed(graph.invoice));
        assertFalse(new OABetweenOrEqualFilter(new com.viaoa.path.OAPath(FilterTestSupport.PRICE_PATH), 13, 20)
                .isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OABetweenOrEqualFilter filter = new OABetweenOrEqualFilter(FilterTestSupport.PRICE_PATH, 10, 20);

        assertEquals(12.50, (Double) filter.getPropertyValue(FilterTestSupport.graph().invoice), 0.001);
    }
}
