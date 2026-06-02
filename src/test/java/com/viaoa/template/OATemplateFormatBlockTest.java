package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateFormatBlockTest {

    static class Item extends OAObject {
        private String name;
        private int qty;

        public Item() { }

        public Item(String name, int qty) {
            this.name = name;
            this.qty = qty;
        }

        public String getName() { return name; }
        public int getQty() { return qty; }
    }

    @Test
    void formatBlockFormatsChildOutput() {
        OATemplate<Item> t = new OATemplate<>("<%=format 10%><%=name%><%=end%>");

        String s = t.process(new Item("abc", 5));

        assertNotNull(s);
        assertTrue(s.contains("abc"));
    }

    @Test
    void formatBlockNestedPropertyUsesActiveObject() {
        OATemplate<Item> t = new OATemplate<>("<%=format 10%>Name=<%=name%><%=end%>");

        assertTrue(t.process(new Item("A", 5)).contains("A"));
    }

    @Test
    void outputConversionInsideFormatBlockAppliesOnceDesiredContract() {
        OATemplate<Item> t = new OATemplate<>("<%=format 10%><%=name%><%=end%>");
        t.setOutputTextConversion("a", "aa");

        assertEquals("aa", t.process(new Item("a", 1)).trim());
    }

    @Test
    void nestedFormatBlocksRemainDeterministic() {
        OATemplate<Item> t = new OATemplate<>("<%=format 20%><%=format 10%><%=name%><%=end%><%=end%>");

        String a = t.process(new Item("abc", 5));
        String b = t.process(new Item("abc", 5));

        assertEquals(a, b);
        assertTrue(a.contains("abc"));
    }

    @Test
    void malformedFormatBlockReportsParseError() {
        OATemplate<Item> t = new OATemplate<>("<%=format 10%><%=name%>");

        t.process(new Item("abc", 5));

        assertTrue(t.getHasParseError());
    }
}
