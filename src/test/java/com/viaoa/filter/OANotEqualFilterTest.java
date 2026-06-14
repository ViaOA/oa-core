package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OANotEqualFilterTest {


    @Test
    void valueConstructorsNegateEqualSemantics() {
        assertFalse(new OANotEqualFilter("Brake Pad").isUsed("Brake Pad"));
        assertTrue(new OANotEqualFilter("Brake Pad").isUsed("Rotor"));
        assertFalse(new OANotEqualFilter("brake pad", true).isUsed("Brake Pad"));
    }

    @Test
    void pathConstructorsCompareResolvedProperty() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertFalse(new OANotEqualFilter(FilterTestSupport.ITEM_NAME_PATH, "Brake Pad").isUsed(graph.invoice));
        assertTrue(new OANotEqualFilter(new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH), "Rotor")
                .isUsed(graph.invoice));
        assertFalse(new OANotEqualFilter(FilterTestSupport.ITEM_NAME_PATH, "brake pad", true).isUsed(graph.invoice));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OANotEqualFilter filter = new OANotEqualFilter(FilterTestSupport.ITEM_NAME_PATH, "Rotor");

        assertEquals("Brake Pad", filter.getPropertyValue(FilterTestSupport.graph().invoice));
    }
}
