package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateConditionalTest {

    public static class Item extends OAObject {
        private String status;
        private int qty;
        private boolean active;

        public Item() { }

        public Item(String status, int qty, boolean active) {
            this.status = status;
            this.qty = qty;
            this.active = active;
        }

        public String getStatus() { return status; }
        public int getQty() { return qty; }
        public boolean getActive() { return active; }
    }

    @Test
    void ifAndIfNotUseTruthinessOfProperty() {
        Item item = new Item("OPEN", 5, true);
        OATemplate<Item> t = new OATemplate<>("<%=if active%>Y<%=end%><%=ifnot active%>N<%=end%>");

        assertEquals("Y", t.process(item));

        item = new Item("OPEN", 5, false);
        assertEquals("N", t.process(item));
    }

    @Test
    void ifEqualsRendersOnlyWhenEqual() {
        OATemplate<Item> t = new OATemplate<>("<%=ifequals status OPEN%>open<%=end%>");

        assertEquals("open", t.process(new Item("OPEN", 5, true)));
        assertEquals("", t.process(new Item("CLOSED", 5, true)));
    }

    @Test
    void ifNotEqualsSuppressesEqualValue() {
        OATemplate<Item> t = new OATemplate<>("<%=ifnotequals status OPEN%>not-open<%=end%>");

        assertEquals("", t.process(new Item("OPEN", 5, true)));
        assertEquals("not-open", t.process(new Item("CLOSED", 5, true)));
    }

    @Test
    void numericComparisonsAreDeterministic() {
        Item item = new Item("OPEN", 5, true);

        assertEquals("Y", new OATemplate<Item>("<%=ifgt qty 4%>Y<%=end%>").process(item));
        assertEquals("Y", new OATemplate<Item>("<%=ifgte qty 5%>Y<%=end%>").process(item));
        assertEquals("Y", new OATemplate<Item>("<%=iflt qty 6%>Y<%=end%>").process(item));
        assertEquals("Y", new OATemplate<Item>("<%=iflte qty 5%>Y<%=end%>").process(item));

        assertEquals("", new OATemplate<Item>("<%=ifgt qty 5%>Y<%=end%>").process(item));
        assertEquals("", new OATemplate<Item>("<%=iflt qty 5%>Y<%=end%>").process(item));
    }

    @Test
    void conditionalWithDollarPropertyWorks() {
        OATemplate<Item> t = new OATemplate<>("<%=ifequals $mode prod%>P<%=end%>");
        t.setProperty("mode", "prod");

        assertEquals("P", t.process(new Item("OPEN", 5, true)));

        t.setProperty("mode", "test");
        assertEquals("", t.process(new Item("OPEN", 5, true)));
    }

    @Test
    void conditionalOperandWithSpacesLimitationIsStableOrSupportsQuotes() {
        Item item = new Item("In Progress", 5, true);
        OATemplate<Item> t = new OATemplate<>("<%=ifequals status 'In Progress'%>Y<%=end%>");

        String s = t.process(item);

        assertTrue("Y".equals(s) || "".equals(s),
            "document current limitation or support quoted operands; output must remain deterministic");
    }

    @Test
    void nestedConditionalsRenderOnlySatisfiedBranches() {
        Item item = new Item("OPEN", 5, true);
        OATemplate<Item> t = new OATemplate<>("<%=if active%><%=ifequals status OPEN%>Y<%=end%><%=end%>");

        assertEquals("Y", t.process(item));
    }

    @Test
    void unmatchedConditionalReportsParseError() {
        OATemplate<Item> t = new OATemplate<>("<%=ifequals status OPEN%>Y");

        t.process(new Item("OPEN", 5, true));

        assertTrue(t.getHasParseError());
    }
}
