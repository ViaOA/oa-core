package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OANotEmptyFilterTest {


    @Test
    void noArgConstructorUsesOANotEmptySemantics() {
        assertFalse(new OANotEmptyFilter().isUsed(null));
        assertFalse(new OANotEmptyFilter().isUsed(""));
        assertTrue(new OANotEmptyFilter().isUsed("x"));
    }

    @Test
    void pathConstructorsCheckResolvedPropertyForNotEmpty() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OANotEmptyFilter(FilterTestSupport.ITEM_NAME_PATH).isUsed(graph.invoice));
        assertTrue(new OANotEmptyFilter(new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH)).isUsed(graph.invoice));
        graph.item.setName("");
        assertFalse(new OANotEmptyFilter(FilterTestSupport.ITEM_NAME_PATH).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OANotEmptyFilter filter = new OANotEmptyFilter(FilterTestSupport.ITEM_NAME_PATH);

        assertEquals("Brake Pad", filter.getPropertyValue(FilterTestSupport.graph().invoice));
    }
}
