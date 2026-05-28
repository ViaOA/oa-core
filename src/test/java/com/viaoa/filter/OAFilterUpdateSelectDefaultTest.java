package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterUpdateSelectDefaultTest {

    @Test
    void defaultUpdateSelectOnLambdaFilterReturnsTrueCurrentContract() {
        OAFilter f = obj -> true;

        assertTrue(f.updateSelect(null));
    }

    @Test
    void logicalFiltersDelegateUpdateSelectResults() {
        OAFilter push = new OAFilter() {
            @Override
            public boolean isUsed(Object obj) {
                return true;
            }

            @Override
            public boolean updateSelect(com.viaoa.select.OASelect select) {
                return true;
            }
        };

        OAFilter noPush = new OAFilter() {
            @Override
            public boolean isUsed(Object obj) {
                return true;
            }

            @Override
            public boolean updateSelect(com.viaoa.select.OASelect select) {
                return false;
            }
        };

        assertTrue(new OAAndFilter(push, noPush).updateSelect(null));
        assertTrue(new OAOrFilter(noPush, push).updateSelect(null));
        assertFalse(new OAAndFilter(noPush, noPush).updateSelect(null));
        assertFalse(new OAOrFilter(noPush, noPush).updateSelect(null));
    }

    @Test
    void logicalFiltersWithNullDelegatesDoNotContributeSelectPushdown() {
        assertFalse(new OAAndFilter(null, null).updateSelect(null));
        assertFalse(new OAOrFilter(null, null).updateSelect(null));
    }
}
