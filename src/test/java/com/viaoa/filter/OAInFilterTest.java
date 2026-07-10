package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAInFilterTest {


    @Test
    void hubConstructorChecksDirectHubMembership() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        com.viaoa.hub.Hub<com.test.pos.model.oa.Item> hub = new com.viaoa.hub.Hub<>(com.test.pos.model.oa.Item.class);
        hub.add(graph.item);

        OAInFilter filter = new OAInFilter(hub);

        assertTrue(filter.isUsed(graph.item));
        assertFalse(filter.isUsed(new com.test.pos.model.oa.Item(9999)));
        assertFalse(filter.isUsed(null));
    }

    @Test
    void hubPathConstructorUsesHubActiveObjectAsMembershipSource() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        com.viaoa.hub.Hub<com.test.pos.model.oa.Invoice> invoices = new com.viaoa.hub.Hub<>(com.test.pos.model.oa.Invoice.class);
        invoices.add(graph.invoice);
        invoices.setAO(graph.invoice);

        OAInFilter filter = new OAInFilter(invoices, com.test.pos.model.oa.Invoice.P_InvoiceBaskets);

        assertNotNull(filter.getPath());
        assertTrue(filter.isUsed(graph.basket));
    }

    @Test
    void objectPathConstructorUsesObjectAsMembershipSource() {
        FilterTestSupport.PosGraph graph = FilterTestSupport.graph();
        OAInFilter filter = new OAInFilter(graph.invoice, com.test.pos.model.oa.Invoice.P_InvoiceBaskets);

        assertNotNull(filter.getPath());
        assertTrue(filter.isUsed(graph.basket));
    }

    @Test
    void updateSelectDefaultsToMemoryFilteringWhenNoReverseOptimizationApplies() {
        OAInFilter filter = new OAInFilter((com.viaoa.hub.Hub) null);

        assertTrue(filter.updateSelect(new com.viaoa.select.OASelect(com.test.pos.model.oa.Item.class)));
    }
}
