package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateForeachHubTest {

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

    private static Hub<Line> hub() {
        Hub<Line> hub = new Hub<>(Line.class);
        hub.add(new Line("A", 10));
        hub.add(new Line("B", 20));
        hub.add(new Line("C", 30));
        return hub;
    }

    @Test
    void foreachRootHubIteratesInHubOrder() {
        OATemplate<Line> t = new OATemplate<>("<%=foreach%><%=name%>,<%=end%>");

        assertEquals("A,B,C,", t.process(hub()));
    }

    @Test
    void emptyForeachRendersNoRows() {
        OATemplate<Line> t = new OATemplate<>("<%=foreach%><%=name%><%=end%>");

        assertEquals("", t.process(new Hub<>(Line.class)));
    }

    @Test
    void foreachCanRenderMultiplePropertiesFromCurrentObject() {
        OATemplate<Line> t = new OATemplate<>("<%=foreach%>[<%=name%>:<%=amount, 0%>]<%=end%>");

        assertEquals("[A:10][B:20][C:30]", t.process(hub()));
    }

    @Test
    void foreachCounterIncrementsPerIteration() {
        OATemplate<Line> t = new OATemplate<>("<%=foreach%><%=#counter%>:<%=name%>;<%=end%>");

        assertEquals("1:A;2:B;3:C;", t.process(hub()));
    }

    @Test
    void foreachCounterResetsBetweenSequentialProcessCallsDesiredContract() {
        OATemplate<Line> t = new OATemplate<>("<%=foreach%><%=#counter%><%=end%>");

        assertEquals("123", t.process(hub()));
        assertEquals("123", t.process(hub()),
            "counter state must be render-local, not leaked across process calls");
    }

    @Test
    void countCommandCountsRootHub() {
        OATemplate<Line> t = new OATemplate<>("<%=#count%>");

        assertEquals("3", t.process(hub()));
    }

    @Test
    void sumCommandUsesHubPropertyValuePropertyAndFormatDesiredContract() {
        OATemplate<Line> t = new OATemplate<>("<%=#sum amount, 0.00%>");

        assertEquals("60.00", t.process(hub()),
            "#sum should sum the requested value property and apply optional format");
    }

    @Test
    void stopProcessingDuringForeachReturnsCancelled() {
        OATemplate<Line> t = new OATemplate<Line>("<%=foreach%><%=name%><%=end%>") {
            @Override
            protected String getOutputText(String text) {
                stopProcessing();
                return super.getOutputText(text);
            }
        };

        assertEquals("cancelled", t.process(hub()));
    }

    @Test
    void malformedForeachMissingEndIsParseError() {
        OATemplate<Line> t = new OATemplate<>("<%=foreach%><%=name%>");

        t.process(hub());

        assertTrue(t.getHasParseError());
    }
}
