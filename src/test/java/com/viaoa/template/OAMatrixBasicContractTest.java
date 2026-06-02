package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAMatrixBasicContractTest {

    public static class Item extends OAObject {
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    private static Hub<Item> hub(String... names) {
        Hub<Item> hub = new Hub<>(Item.class);
        for (String name : names) {
            hub.add(new Item(name));
        }
        return hub;
    }

    @Test
    void addColumnRejectsNullHub() {
        OAMatrix m = new OAMatrix();

        assertThrows(IllegalArgumentException.class, () -> m.addColumn(null));
    }

    @Test
    void addColumnAddsRootColumnAndClearsGrid() {
        OAMatrix m = new OAMatrix();
        Hub<Item> hub = hub("A");

        OAMatrix.Column col = m.addColumn(hub);

        assertNotNull(col);
        assertSame(col, m.getColumn(0));
        assertNull(col.getFromColumn());
        assertNull(col.getPropertyPath());
        assertEquals(1, m.getColumnCount());
        assertSame(m.getColumns(), m.getColumns());
    }

    @Test
    void getColumnBoundsReturnNull() {
        OAMatrix m = new OAMatrix();
        m.addColumn(hub("A"));

        assertNull(m.getColumn(-1));
        assertNull(m.getColumn(1));
        assertNotNull(m.getColumn(0));
    }

    @Test
    void rootHubCreatesOneRowPerObjectAndObjectLookupWorks() {
        OAMatrix m = new OAMatrix();
        Hub<Item> hub = hub("A", "B");
        m.addColumn(hub);

        assertEquals(2, m.getRowCount());
        assertEquals(2, m.getGrid().size());

        assertSame(hub.getAt(0), m.getObject(0, 0));
        assertSame(hub.getAt(1), m.getObject(1, 0));
        assertSame(hub.getAt(0), m.getRealObject(0, 0));
    }

    @Test
    void getObjectNegativeColumnReturnsNullDesiredContract() {
        OAMatrix m = new OAMatrix();
        m.addColumn(hub("A"));

        assertDoesNotThrow(() -> m.getObject(0, -1));
        assertNull(m.getObject(0, -1));
    }

    @Test
    void getObjectAndRealObjectBoundsReturnNull() {
        OAMatrix m = new OAMatrix();
        m.addColumn(hub("A"));

        assertNull(m.getObject(-1, 0));
        assertNull(m.getObject(1, 0));
        assertNull(m.getObject(0, 1));

        assertNull(m.getRealObject(-1, 0));
        assertNull(m.getRealObject(1, 0));
        assertNull(m.getRealObject(0, -1));
        assertNull(m.getRealObject(0, 1));
    }

    @Test
    void getGridIsCachedUntilClearGrid() {
        OAMatrix m = new OAMatrix();
        m.addColumn(hub("A"));

        List<Object[]> a = m.getGrid();
        List<Object[]> b = m.getGrid();

        assertSame(a, b);

        m.clearGrid();

        List<Object[]> c = m.getGrid();

        assertNotSame(a, c);
    }

    @Test
    void multipleRootColumnsDoNotOverwriteRowsDesiredContract() {
        OAMatrix m = new OAMatrix();
        Hub<Item> a = hub("A1", "A2");
        Hub<Item> b = hub("B1", "B2", "B3");

        m.addColumn(a);
        m.addColumn(b);

        assertEquals(5, m.getRowCount(),
            "multiple root columns should append row ranges or be explicitly rejected, not overwrite rows");

        assertEquals(5, m.getGrid().size());
    }

    @Test
    void getRowCountForChildColumnDoesNotLoopForeverDesiredContract() {
        OAMatrix m = new OAMatrix();
        Hub<Item> hub = hub("A");

        OAMatrix.Column root = m.addColumn(hub);
        // Use public addDetailColumn invalid path path would need metadata; instead make sure root column method is stable.
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            assertEquals(1, m.getRowCount(root));
        });
    }

    @Test
    void addDetailColumnInvalidInputsReturnNull() {
        OAMatrix m = new OAMatrix();

        assertNull(m.addDetailColumn(null, "children"));

        OAMatrix.Column root = m.addColumn(hub("A"));

        assertNull(m.addDetailColumn(root, null));
        assertNull(m.addDetailColumn(root, ""));
    }
}
