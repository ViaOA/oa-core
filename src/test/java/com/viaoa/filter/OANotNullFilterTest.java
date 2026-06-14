package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OANotNullFilterTest {


    @Test
    void noArgConstructorAcceptsNonNullOnly() {
        assertFalse(new OANotNullFilter().isUsed(null));
        assertTrue(new OANotNullFilter().isUsed(""));
    }

    @Test
    void pathConstructorsCheckResolvedPropertyForNotNull() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OANotNullFilter(FilterTestSupport.ITEM_NAME_PATH).isUsed(graph.invoice));
        assertTrue(new OANotNullFilter(new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH)).isUsed(graph.invoice));
        graph.item.setName(null);
        assertFalse(new OANotNullFilter(FilterTestSupport.ITEM_NAME_PATH).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OANotNullFilter filter = new OANotNullFilter(FilterTestSupport.ITEM_NAME_PATH);

        assertEquals("Brake Pad", filter.getPropertyValue(FilterTestSupport.graph().invoice));
    }
}
