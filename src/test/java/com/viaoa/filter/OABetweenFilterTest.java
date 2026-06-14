package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OABetweenFilterTest {


    @Test
    void valueConstructorUsesExclusiveBounds() {
        assertTrue(new OABetweenFilter(10, 20).isUsed(15));
        assertFalse(new OABetweenFilter(10, 20).isUsed(10));
    }

    @Test
    void pathConstructorsCompareResolvedPropertyValues() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OABetweenFilter(FilterTestSupport.PRICE_PATH, 10, 20).isUsed(graph.invoice));
        assertFalse(new OABetweenFilter(new com.viaoa.path.OAPath(FilterTestSupport.PRICE_PATH), 12.50, 20)
                .isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OABetweenFilter filter = new OABetweenFilter(FilterTestSupport.PRICE_PATH, 10, 20);

        assertEquals(12.50, (Double) filter.getPropertyValue(FilterTestSupport.graph().invoice), 0.001);
    }
}
