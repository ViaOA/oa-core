package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAQueryFilterTest {


    @Test
    void classQueryConstructorParsesAndEvaluatesSimpleEquality() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        OAQueryFilter<com.test.pos.model.oa.Item> filter = new OAQueryFilter<>(com.test.pos.model.oa.Item.class,
                com.test.pos.model.oa.Item.P_Name + " = 'Brake Pad'");

        assertTrue(filter.isUsed(graph.item));
    }

    @Test
    void classQueryArgsConstructorSubstitutesQuestionMarkParameters() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        OAQueryFilter<com.test.pos.model.oa.Item> filter = new OAQueryFilter<>(com.test.pos.model.oa.Item.class,
                com.test.pos.model.oa.Item.P_Name + " = ?", new Object[] { "Brake Pad" });

        assertTrue(filter.isUsed(graph.item));
        assertFalse(new OAQueryFilter<>(com.test.pos.model.oa.Item.class, com.test.pos.model.oa.Item.P_Name + " = ?",
                new Object[] { "Rotor" }).isUsed(graph.item));
    }

    @Test
    void constructorRejectsEmptyQuery() {
        assertThrows(RuntimeException.class, () -> new OAQueryFilter<>(com.test.pos.model.oa.Item.class, ""));
    }
}
