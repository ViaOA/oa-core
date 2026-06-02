package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAMatrixDetailColumnContractTest {

    public static class Order extends OAObject {
        private String name;
        private final Hub<Line> lines = new Hub<>(Line.class);

        public Order() { }
        public Order(String name) { this.name = name; }
        public String getName() { return name; }
        public Hub<Line> getLines() { return lines; }
    }

    public static class Line extends OAObject {
        private String name;
        public Line() { }
        public Line(String name) { this.name = name; }
        public String getName() { return name; }
    }

    private static Hub<Order> orders() {
        Hub<Order> hub = new Hub<>(Order.class);
        Order a = new Order("A");
        a.getLines().add(new Line("A1"));
        a.getLines().add(new Line("A2"));
        Order b = new Order("B");
        b.getLines().add(new Line("B1"));
        hub.add(a);
        hub.add(b);
        return hub;
    }

    @Test
    void addDetailColumnCreatesChildColumnWithFromAndPropertyPath() {
        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(orders());

        OAMatrix.Column child = m.addDetailColumn(root, "lines");

        assertNotNull(child);
        assertSame(root, child.getFromColumn());
        assertEquals("lines", child.getPropertyPath());
        assertEquals(2, m.getColumnCount());
    }

    @Test
    void detailColumnExpandsRowsAndAlignsParentObjects() {
        Hub<Order> orders = orders();
        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(orders());
        m.addDetailColumn(root, "lines");

        assertEquals(3, m.getRowCount());

        assertSame(orders.getAt(0), m.getRealObject(0, 0));
        assertSame(orders.getAt(0), m.getRealObject(1, 0));
        assertSame(orders.getAt(1), m.getRealObject(2, 0));

        assertEquals("A1", ((Line) m.getObject(0, 1)).getName());
        assertEquals("A2", ((Line) m.getObject(1, 1)).getName());
        assertEquals("B1", ((Line) m.getObject(2, 1)).getName());
    }

    @Test
    void parentCellMayBeBlankOnExpandedRowsButRealObjectResolvesAncestor() {
        Hub<Order> orders = orders();
        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(orders());
        m.addDetailColumn(root, "lines");

        assertSame(orders.getAt(0), m.getObject(0, 0));
        assertNull(m.getObject(1, 0));
        assertSame(orders.getAt(0), m.getRealObject(1, 0));
    }

    @Test
    void invalidDetailPathFailsWithControlledException() {
        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(orders());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> m.addDetailColumn(root, "name"));

        assertTrue(ex.getMessage().contains("invalid propertyPath"));
    }

    @Test
    void nestedDetailPathFromChildColumnUsesFullRootPath() {
        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(orders());
        OAMatrix.Column lines = m.addDetailColumn(root, "lines");

        assertEquals("lines.name", m.getPropertyPathFromRoot(lines, "name"));
    }

    @Test
    void getRootColumnForNestedColumnReturnsOriginalRoot() {
        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(orders());
        OAMatrix.Column lines = m.addDetailColumn(root, "lines");

        assertSame(root, m.getRootColumn(lines));
    }

    @Test
    void getRowCountOnChildColumnDoesNotLoopForeverDesiredContract() {
        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(orders());
        OAMatrix.Column lines = m.addDetailColumn(root, "lines");
        m.getGrid();

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            assertEquals(3, m.getRowCount(lines));
        });
    }

    @Test
    void emptyDetailHubKeepsParentRowVisibleByContract() {
        Hub<Order> hub = new Hub<>(Order.class);
        hub.add(new Order("A"));

        OAMatrix m = new OAMatrix();
        OAMatrix.Column root = m.addColumn(hub);
        m.addDetailColumn(root, "lines");

        assertEquals(1, m.getRowCount());
        assertSame(hub.getAt(0), m.getRealObject(0, 0));
        assertNull(m.getObject(0, 1));
    }
}
