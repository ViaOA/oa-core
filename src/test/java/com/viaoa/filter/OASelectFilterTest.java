package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OASelectFilterTest {


    @Test
    void classQueryArgsConstructorDelegatesToQueryFilter() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        OASelectFilter<com.test.pos.model.oa.Item> filter = new OASelectFilter<>(com.test.pos.model.oa.Item.class,
                com.test.pos.model.oa.Item.P_Name + " = ?", new Object[] { "Brake Pad" });

        assertTrue(filter.isUsed(graph.item));
    }

    @Test
    void classQueryConstructorDelegatesToQueryFilter() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        OASelectFilter<com.test.pos.model.oa.Item> filter = new OASelectFilter<>(com.test.pos.model.oa.Item.class,
                com.test.pos.model.oa.Item.P_Name + " = 'Brake Pad'");

        assertTrue(filter.isUsed(graph.item));
    }
}
