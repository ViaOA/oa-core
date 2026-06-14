package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OANullFilterTest {


    @Test
    void noArgConstructorAcceptsNullOnly() {
        assertTrue(new OANullFilter().isUsed(null));
        assertFalse(new OANullFilter().isUsed(""));
    }

    @Test
    void pathConstructorsCheckResolvedPropertyForNull() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        graph.item.setName(null);

        assertTrue(new OANullFilter(FilterTestSupport.ITEM_NAME_PATH).isUsed(graph.invoice));
        assertTrue(new OANullFilter(new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH)).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OANullFilter filter = new OANullFilter(FilterTestSupport.ITEM_NAME_PATH);

        assertEquals("Brake Pad", filter.getPropertyValue(FilterTestSupport.graph().invoice));
    }
}
