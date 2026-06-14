package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAEmptyFilterTest {


    @Test
    void noArgConstructorUsesOAEmptySemantics() {
        assertTrue(new OAEmptyFilter().isUsed(null));
        assertTrue(new OAEmptyFilter().isUsed(""));
        assertFalse(new OAEmptyFilter().isUsed("x"));
    }

    @Test
    void pathConstructorsCheckResolvedPropertyForEmpty() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        graph.item.setName("");

        assertTrue(new OAEmptyFilter(FilterTestSupport.ITEM_NAME_PATH).isUsed(graph.invoice));
        assertTrue(new OAEmptyFilter(new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH)).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OAEmptyFilter filter = new OAEmptyFilter(FilterTestSupport.ITEM_NAME_PATH);

        assertEquals("Brake Pad", filter.getPropertyValue(FilterTestSupport.graph().invoice));
    }
}
