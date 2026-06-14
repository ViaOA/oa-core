package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OATrueFilterTest {


    @Test
    void noArgConstructorAlwaysAccepts() {
        OATrueFilter filter = new OATrueFilter();

        assertTrue(filter.isUsed(null));
        assertTrue(filter.isUsed(Boolean.FALSE));
        assertTrue(filter.isUsed(FilterTestSupport.graph().product));
    }

    @Test
    void stringPathConstructorChecksResolvedBooleanProperty() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OATrueFilter(FilterTestSupport.SEALED_PACKAGE_PATH).isUsed(graph.invoice));
        graph.product.setSealedPackage(false);
        assertFalse(new OATrueFilter(FilterTestSupport.SEALED_PACKAGE_PATH).isUsed(graph.invoice));
    }

    @Test
    void oaPathConstructorChecksResolvedBooleanProperty() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertTrue(new OATrueFilter(new com.viaoa.path.OAPath(FilterTestSupport.SEALED_PACKAGE_PATH)).isUsed(graph.invoice));
    }
}
