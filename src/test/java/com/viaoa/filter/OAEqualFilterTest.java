package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAEqualFilterTest {


    @Test
    void valueConstructorComparesDirectObjects() {
        assertTrue(new OAEqualFilter("Brake Pad").isUsed("Brake Pad"));
        assertFalse(new OAEqualFilter("Brake Pad").isUsed("brake pad"));
    }

    @Test
    void stringPathConstructorComparesResolvedProperty() {
        assertTrue(new OAEqualFilter(FilterTestSupport.ITEM_NAME_PATH, "Brake Pad").isUsed(FilterTestSupport.graph().invoice));
    }

    @Test
    void oaPathConstructorComparesResolvedProperty() {
        assertTrue(new OAEqualFilter(new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH), "Brake Pad")
                .isUsed(FilterTestSupport.graph().invoice));
    }

    @Test
    void stringPathConstructorAcceptsGeneratedPropertyPathHelperChain() {
        assertEquals(FilterTestSupport.ITEM_NAME_PATH, FilterTestSupport.GENERATED_ITEM_NAME_PATH);
        assertTrue(new OAEqualFilter(FilterTestSupport.GENERATED_ITEM_NAME_PATH, "Brake Pad")
                .isUsed(FilterTestSupport.graph().invoice));
    }

    @Test
    void stringPathConstructorAcceptsOATextUtilCreatedPath() {
        assertEquals(FilterTestSupport.ITEM_NAME_PATH, FilterTestSupport.TEXT_UTIL_ITEM_NAME_PATH);
        assertTrue(new OAEqualFilter(FilterTestSupport.TEXT_UTIL_ITEM_NAME_PATH, "Brake Pad")
                .isUsed(FilterTestSupport.graph().invoice));
    }

    @Test
    void ignoreCaseConstructorComparesStringsIgnoringCase() {
        assertTrue(new OAEqualFilter(FilterTestSupport.ITEM_NAME_PATH, "brake pad", true).isUsed(FilterTestSupport.graph().invoice));
    }

    @Test
    void decimalPlacesConstructorRoundsFloatingValues() {
        OAEqualFilter filter = new OAEqualFilter(FilterTestSupport.PRICE_PATH, 12.499, 1);

        assertTrue(filter.isUsed(FilterTestSupport.graph().invoice));
    }

    @Test
    void settersAndGetterChangeComparisonOptions() {
        OAEqualFilter filter = new OAEqualFilter("brake pad");
        filter.setIgnoreCase(true);
        filter.setDeciPlaces(2);

        assertEquals(2, filter.getDeciPlaces());
        assertTrue(filter.isUsed("Brake Pad"));
    }

    @Test
    void protectedGetPropertyValueReturnsResolvedValue() {
        OAEqualFilter filter = new OAEqualFilter(FilterTestSupport.ITEM_NAME_PATH, "Brake Pad");

        assertEquals("Brake Pad", filter.getPropertyValue(FilterTestSupport.graph().invoice));
    }
}
