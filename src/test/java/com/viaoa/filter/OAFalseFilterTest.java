package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFalseFilterTest {


    @Test
    void noArgConstructorAlwaysRejects() {
        OAFalseFilter filter = new OAFalseFilter();

        assertFalse(filter.isUsed(null));
        assertFalse(filter.isUsed(Boolean.FALSE));
        assertFalse(filter.isUsed(FilterTestSupport.graph().product));
    }

    @Test
    void stringPathConstructorChecksResolvedBooleanProperty() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();

        assertFalse(new OAFalseFilter(FilterTestSupport.SEALED_PACKAGE_PATH).isUsed(graph.invoice));
        graph.product.setSealedPackage(false);
        assertTrue(new OAFalseFilter(FilterTestSupport.SEALED_PACKAGE_PATH).isUsed(graph.invoice));
    }

    @Test
    void oaPathConstructorChecksResolvedBooleanProperty() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        graph.product.setSealedPackage(false);

        assertTrue(new OAFalseFilter(new com.viaoa.path.OAPath(FilterTestSupport.SEALED_PACKAGE_PATH)).isUsed(graph.invoice));
    }
}
