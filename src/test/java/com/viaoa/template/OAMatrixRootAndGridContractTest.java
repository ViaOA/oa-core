package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAMatrixRootAndGridContractTest {

    public static class Item extends OAObject {
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    private static Hub<Item> hub(String... names) {
        Hub<Item> hub = new Hub<>(Item.class);
        for (String name : names) hub.add(new Item(name));
        return hub;
    }

    @Test
    void emptyMatrixHasZeroColumnsAndRows() {
        OAMatrix m = new OAMatrix();

        assertEquals(0, m.getColumnCount());
        assertEquals(0, m.getRowCount());
        assertTrue(m.getGrid().isEmpty());
        assertNull(m.getObject(0, 0));
        assertNull(m.getRealObject(0, 0));
    }

    @Test
    void singleRootHubBuildsStableGridRows() {
        Hub<Item> hub = hub("A", "B", "C");
        OAMatrix m = new OAMatrix();
        m.addColumn(hub);

        List<Object[]> grid = m.getGrid();

        assertEquals(3, grid.size());
        assertEquals(1, grid.get(0).length);
        assertSame(hub.getAt(0), grid.get(0)[0]);
        assertSame(hub.getAt(1), grid.get(1)[0]);
        assertSame(hub.getAt(2), grid.get(2)[0]);
    }

    @Test
    void rootHubEmptyBuildsNoRowsButKeepsColumn() {
        OAMatrix m = new OAMatrix();
        m.addColumn(new Hub<>(Item.class));

        assertEquals(1, m.getColumnCount());
        assertEquals(0, m.getRowCount());
        assertTrue(m.getGrid().isEmpty());
    }

    @Test
    void multipleRootColumnsAppendOrRejectButMustNotOverlayDesiredContract() {
        OAMatrix m = new OAMatrix();
        Hub<Item> a = hub("A1", "A2");
        Hub<Item> b = hub("B1", "B2", "B3");

        m.addColumn(a);
        m.addColumn(b);

        try {
            assertEquals(5, m.getRowCount());
            assertEquals(5, m.getGrid().size());
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage(), "if multiple root columns are unsupported, reject explicitly");
        }
    }

    @Test
    void clearGridAfterUnderlyingHubMutationRebuildsRows() {
        Hub<Item> hub = hub("A");
        OAMatrix m = new OAMatrix();
        m.addColumn(hub);

        assertEquals(1, m.getRowCount());

        hub.add(new Item("B"));

        assertEquals(1, m.getRowCount(), "cached grid remains until clearGrid");

        m.clearGrid();

        assertEquals(2, m.getRowCount());
        assertSame(hub.getAt(1), m.getObject(1, 0));
    }

    @Test
    void addingColumnInvalidatesGridCache() {
        OAMatrix m = new OAMatrix();
        m.addColumn(hub("A"));

        List<Object[]> first = m.getGrid();

        m.addColumn(hub("B"));

        assertNotSame(first, m.getGrid());
    }

    @Test
    void getObjectNegativeColumnDoesNotThrowDesiredContract() {
        OAMatrix m = new OAMatrix();
        m.addColumn(hub("A"));

        assertDoesNotThrow(() -> m.getObject(0, -1));
        assertNull(m.getObject(0, -1));
    }

    @Test
    void getRowCountColumnUsesRootForChildColumnsWithoutInfiniteLoopDesiredContract() {
        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(hub("A", "B"));

        assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), () -> {
            assertEquals(2, m.getRowCount(root));
        });
    }
}
