package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterUpdateSelectExtendedTest {

    private static OAFilter filterWithUpdate(boolean updateResult) {
        return new OAFilter() {
            @Override
            public boolean isUsed(Object obj) {
                return true;
            }

            @Override
            public boolean updateSelect(com.viaoa.select.OASelect select) {
                return updateResult;
            }
        };
    }

    @Test
    void andFilterUpdateSelectReturnsTrueIfEitherDelegateContributes() {
        assertTrue(new OAAndFilter(filterWithUpdate(true), filterWithUpdate(false)).updateSelect(null));
        assertTrue(new OAAndFilter(filterWithUpdate(false), filterWithUpdate(true)).updateSelect(null));
        assertTrue(new OAAndFilter(filterWithUpdate(true), filterWithUpdate(true)).updateSelect(null));
        assertFalse(new OAAndFilter(filterWithUpdate(false), filterWithUpdate(false)).updateSelect(null));
    }

    @Test
    void orFilterUpdateSelectReturnsTrueIfEitherDelegateContributes() {
        assertTrue(new OAOrFilter(filterWithUpdate(true), filterWithUpdate(false)).updateSelect(null));
        assertTrue(new OAOrFilter(filterWithUpdate(false), filterWithUpdate(true)).updateSelect(null));
        assertTrue(new OAOrFilter(filterWithUpdate(true), filterWithUpdate(true)).updateSelect(null));
        assertFalse(new OAOrFilter(filterWithUpdate(false), filterWithUpdate(false)).updateSelect(null));
    }

    @Test
    void xorAndBlockUpdateSelectUseDefaultInterfaceContractCurrentBehavior() {
        assertTrue(new OAXorFilter(filterWithUpdate(false), filterWithUpdate(false)).updateSelect(null));
        assertTrue(new OABlockFilter(filterWithUpdate(false), filterWithUpdate(false)).updateSelect(null));
    }

    @Test
    void directComparisonFiltersDefaultUpdateSelectCurrentContract() {
        assertTrue(new OAEqualFilter("x").updateSelect(null));
        assertTrue(new OANotEqualFilter("x").updateSelect(null));
        assertTrue(new OAGreaterFilter(1).updateSelect(null));
        assertTrue(new OALessFilter(1).updateSelect(null));
        assertTrue(new OALikeFilter("*").updateSelect(null));
    }
}
