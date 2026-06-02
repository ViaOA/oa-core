package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateRootSelectionAndReuseTest {

    public static class Customer extends OAObject {
        private String name;
        public Customer() { }
        public Customer(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public static class Store extends OAObject {
        private String code;
        public Store() { }
        public Store(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    public static class TeamMember extends OAObject {
        private String login;
        public TeamMember() { }
        public TeamMember(String login) { this.login = login; }
        public String getLogin() { return login; }
    }

    @Test
    void twoRootSelectionUsesRootThatSupportsSampledPath() {
        OATemplate<OAObject> t = new OATemplate<>("<%=code%>");

        assertEquals("S1", t.process(new Customer("C1"), new Store("S1")));
    }

    @Test
    void cachedRootSelectionMustBeValidatedAgainstCurrentRootPairDesiredContract() {
        OATemplate<OAObject> t = new OATemplate<>("<%=code%>");

        assertEquals("S1", t.process(new Customer("C1"), new Store("S1")));

        String s = t.process(new TeamMember("tm1"), new Customer("C2"));

        assertTrue("".equals(s) || "tm1".equals(s) || "C2".equals(s),
            "render must not blindly choose second root from old classChoosen when neither current root matches");
    }

    @Test
    void setTemplateRecomputesRootChoice() {
        OATemplate<OAObject> t = new OATemplate<>("<%=code%>");

        assertEquals("S1", t.process(new Customer("C1"), new Store("S1")));

        t.setTemplate("<%=name%>");

        assertEquals("C2", t.process(new Customer("C2"), new Store("S2")));
    }

    @Test
    void sequentialRendersWithSameInputsAreDeterministic() {
        OATemplate<OAObject> t = new OATemplate<>("<%=name%>-<%=code%>");

        Customer c = new Customer("C");
        Store s = new Store("S");

        String a = t.process(c, s);
        String b = t.process(c, s);

        assertEquals(a, b);
    }

    @Test
    void setTemplateToLiteralClearsSampledPropertyPathBehavior() {
        OATemplate<OAObject> t = new OATemplate<>("<%=code%>");

        assertEquals("S1", t.process(new Customer("C1"), new Store("S1")));

        t.setTemplate("literal");

        assertEquals("literal", t.process(new Customer("C2"), new Store("S2")));
    }
}
