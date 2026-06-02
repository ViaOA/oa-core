package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateCounterAndCommandStateTest {

    public static class Item extends OAObject {
        private String group;
        private int amount;

        public Item() { }
        public Item(String group, int amount) {
            this.group = group;
            this.amount = amount;
        }

        public String getGroup() { return group; }
        public int getAmount() { return amount; }
    }

    private static Hub<Item> hub() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A", 10));
        hub.add(new Item("A", 20));
        hub.add(new Item("B", 30));
        return hub;
    }

    @Test
    void unnamedCounterIncrementsWithinSingleForeach() {
        OATemplate<Item> t = new OATemplate<>("<%=foreach%><%=#counter%><%=end%>");

        assertEquals("123", t.process(hub()));
    }

    @Test
    void namedCounterTracksSeparateCounters() {
        OATemplate<Item> t = new OATemplate<>("<%=foreach%><%=#counter a%>-<%=#counter b%>;<%=end%>");

        assertEquals("1-1;2-2;3-3;", t.process(hub()));
    }

    @Test
    void countersAreRenderLocalAcrossSequentialCallsDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("<%=foreach%><%=#counter a%><%=end%>");

        assertEquals("123", t.process(hub()));
        assertEquals("123", t.process(hub()));
    }

    @Test
    void countCommandDoesNotConsumeForeachIterationState() {
        OATemplate<Item> t = new OATemplate<>("<%=#count%>:<%=foreach%><%=#counter%><%=end%>");

        assertEquals("3:123", t.process(hub()));
    }

    @Test
    void sumCommandWithFormatUsesPropertyAndFormatDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("<%=#sum amount, 0.00%>");

        assertEquals("60.00", t.process(hub()));
    }

    @Test
    void sumInsideForeachIsDeterministic() {
        OATemplate<Item> t = new OATemplate<>("<%=foreach%><%=#sum amount, 0%>;<%=end%>");

        String s1 = t.process(hub());
        String s2 = t.process(hub());

        assertEquals(s1, s2);
    }
}
