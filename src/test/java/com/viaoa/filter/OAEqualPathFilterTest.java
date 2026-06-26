package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.select.OASelect;

class OAEqualPathFilterTest {


    @Test
    void hubConstructorComparesFromHubActiveObjectPathToCandidatePath() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        com.viaoa.hub.Hub<com.test.pos.model.oa.Item> items = new com.viaoa.hub.Hub<>(com.test.pos.model.oa.Item.class);
        items.add(graph.item);
        items.setAO(graph.item);

        OAEqualPathFilter filter = new OAEqualPathFilter(items, com.test.pos.model.oa.Item.P_Name,
                com.test.pos.model.oa.Item.P_Name);

        assertNotNull(filter.getPropertyPath());
        assertTrue(filter.isUsed(graph.item));
        com.test.pos.model.oa.Item other = new com.test.pos.model.oa.Item(2000);
        other.setName("Rotor");
        assertFalse(filter.isUsed(other));
    }

    @Test
    void objectConstructorComparesSourcePathToCandidatePathByValue() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        OAEqualPathFilter filter = new OAEqualPathFilter(graph.item, com.test.pos.model.oa.Item.P_Name, com.test.pos.model.oa.Item.P_Name);

        assertNotNull(filter.getPropertyPath());
        boolean b = filter.isUsed(graph.item);
        assertTrue(b);
    }

    @Test
    void updateSelectReturnsTrueWithoutPriorCandidatePathSetup() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        OAEqualPathFilter filter = new OAEqualPathFilter(graph.item, com.test.pos.model.oa.Item.P_Name,
                com.test.pos.model.oa.Item.P_Name);

        assertTrue(filter.updateSelect(new OASelect(com.test.pos.model.oa.Item.class)));
    }
}
