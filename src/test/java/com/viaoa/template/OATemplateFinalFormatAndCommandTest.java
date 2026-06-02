package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateFinalFormatAndCommandTest {

    public static class Item extends OAObject {
        private String name;
        private double amount;

        public Item() { }
        public Item(String name, double amount) {
            this.name = name;
            this.amount = amount;
        }

        public String getName() { return name; }
        public double getAmount() { return amount; }
    }

    private static Hub<Item> hub() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A", 10.25));
        hub.add(new Item("B", 20.75));
        return hub;
    }

    @Test
    void sumCommandWithDecimalFormatUsesAmountPropertyDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("<%=#sum amount, 0.00%>");

        assertEquals("31.00", t.process(hub()));
    }

    @Test
    void sumCommandUnknownPropertyDoesNotThrowFalseSuccessContract() {
        OATemplate<Item> t = new OATemplate<>("<%=#sum missing, 0.00%>");

        assertDoesNotThrow(() -> t.process(hub()));
        assertTrue(t.process(hub()).matches("0(\\.00)?|"));
    }

    @Test
    void countAndSumAreDeterministicAcrossSequentialCalls() {
        OATemplate<Item> t = new OATemplate<>("<%=#count%>:<%=#sum amount, 0.00%>");

        String a = t.process(hub());
        String b = t.process(hub());

        assertEquals(a, b);
        assertEquals("2:31.00", a);
    }

    @Test
    void outputConversionDoesNotDoubleApplyAroundFormatBlockDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("<%=format 10%><%=name%><%=end%>");
        t.setOutputTextConversion("A", "AA");

        assertEquals("AA", t.process(new Item("A", 1)).trim());
    }

    @Test
    void formatBlockWithLiteralAndPropertyAppliesConversionConsistently() {
        OATemplate<Item> t = new OATemplate<>("<%=format 10%>A<%=name%><%=end%>");
        t.setOutputTextConversion("A", "X");

        String s = t.process(new Item("A", 1)).trim();

        assertTrue(s.contains("X"));
        assertFalse(s.contains("XXXX"), "conversion must not recursively amplify output");
    }
}
