package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateMatrixForeachContractTest {

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
        private double amount;

        public Line() { }
        public Line(String name, double amount) {
            this.name = name;
            this.amount = amount;
        }

        public String getName() { return name; }
        public double getAmount() { return amount; }
    }

    private static Hub<Order> orders() {
        Hub<Order> hub = new Hub<>(Order.class);
        Order a = new Order("A");
        a.getLines().add(new Line("A1", 10));
        a.getLines().add(new Line("A2", 20));
        Order b = new Order("B");
        b.getLines().add(new Line("B1", 30));
        hub.add(a);
        hub.add(b);
        return hub;
    }

    @Test
    void foreachWithManyLinkPathUsesMatrixAndRendersAllRows() {
        OATemplate<Order> t = new OATemplate<>("<%=foreach%><%=name%>:<%=lines.name%>;<%=end%>");

        assertEquals("A:A1;A:A2;B:B1;", t.process(orders()));
    }

    @Test
    void matrixForeachConditionUsesCurrentRowObjectDesiredContract() {
        OATemplate<Order> t = new OATemplate<>("<%=foreach%><%=if name%><%=name%>:<%=lines.name%>;<%=end%><%=end%>");

        assertEquals("A:A1;A:A2;B:B1;", t.process(orders()),
            "matrix-backed foreach must pass the row/root object to non-GetProp children such as if blocks");
    }

    @Test
    void matrixForeachNestedFormatUsesCurrentRowObjectDesiredContract() {
        OATemplate<Order> t = new OATemplate<>("<%=foreach%><%=format 20%><%=name%>:<%=lines.name%>;<%=end%><%=end%>");

        String s = t.process(orders());

        assertTrue(s.contains("A:A1;"));
        assertTrue(s.contains("A:A2;"));
        assertTrue(s.contains("B:B1;"));
    }

    @Test
    void matrixForeachMissingColumnMappingDoesNotThrowDesiredContract() {
        OATemplate<Order> t = new OATemplate<>("<%=foreach%><%=name%>:<%=lines.name%>:<%=missing.path%>;<%=end%>");

        assertDoesNotThrow(() -> t.process(orders()));
    }

    @Test
    void dataGridStateIsRestoredAfterExceptionDesiredContract() {
        OATemplate<Order> t = new OATemplate<>("<%=foreach%><%=lines.name%><%=end%>");
        Hub<Order> hub = orders();

        assertDoesNotThrow(() -> t.process(hub));

        t.setTemplate("<%=name%>");

        assertEquals("", t.process(hub),
            "cntInDataGrid must not leak into later render after matrix foreach lifecycle");
    }
}
