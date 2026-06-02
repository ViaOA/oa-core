package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.datetime.OADate;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateObjectPropertyRenderTest {

    public static class Address extends OAObject {
        private String city;
        public Address() { }
        public Address(String city) { this.city = city; }
        public String getCity() { return city; }
    }

    public static class Customer extends OAObject {
        private String name;
        private int age;
        private boolean active;
        private Address address;
        private OADate birthDate;

        public Customer() { }

        public Customer(String name, int age, boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean getActive() { return active; }
        public Address getAddress() { return address; }
        public OADate getBirthDate() { return birthDate; }

        public void setAddress(Address address) { this.address = address; }
        public void setBirthDate(OADate birthDate) { this.birthDate = birthDate; }
    }

    @Test
    void simpleObjectPropertyRenders() {
        Customer c = new Customer("Vince", 61, true);
        OATemplate<Customer> t = new OATemplate<>("Name=<%=name%>, Age=<%=age%>");

        assertEquals("Name=Vince, Age=61", t.process(c));
    }

    @Test
    void nestedObjectPropertyPathRenders() {
        Customer c = new Customer("Vince", 61, true);
        c.setAddress(new Address("Springfield"));

        OATemplate<Customer> t = new OATemplate<>("City=<%=address.city%>");

        assertEquals("City=Springfield", t.process(c));
    }

    @Test
    void nullIntermediatePathRendersBlank() {
        Customer c = new Customer("Vince", 61, true);

        OATemplate<Customer> t = new OATemplate<>("[<%=address.city%>]");

        assertEquals("[]", t.process(c));
        assertFalse(t.getHasParseError());
    }

    @Test
    void nullRootWithPropertyRendersBlankDesiredContract() {
        OATemplate<Customer> t = new OATemplate<>("[<%=name%>]");

        assertDoesNotThrow(() -> t.process((Customer) null));
        assertEquals("[]", t.process((Customer) null));
    }

    @Test
    void missingPropertyPathRendersBlankOrObservableFailureByContract() {
        Customer c = new Customer("Vince", 61, true);
        OATemplate<Customer> t = new OATemplate<>("[<%=missing.path%>]");

        try {
            assertEquals("[]", t.process(c));
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void booleanPropertyRendersStableText() {
        Customer c = new Customer("Vince", 61, true);
        OATemplate<Customer> t = new OATemplate<>("[<%=active%>]");

        assertEquals("[true]", t.process(c).toLowerCase());
    }

    @Test
    void explicitNumberFormatAppliesAtTypedBoundaryDesiredContract() {
        Customer c = new Customer("Vince", 61, true);
        OATemplate<Customer> t = new OATemplate<>("<%=age, 000%>");

        assertEquals("061", t.process(c),
            "explicit number format must apply before value is flattened to plain string");
    }

    @Test
    void explicitDateFormatAppliesAtTypedBoundaryDesiredContract() {
        Customer c = new Customer("Vince", 61, true);
        c.setBirthDate(new OADate(2026, 5, 28));

        OATemplate<Customer> t = new OATemplate<>("<%=birthDate, MM/dd/yyyy%>");

        assertEquals("05/28/2026", t.process(c),
            "explicit date format must apply to typed date value, not to already-converted default string");
    }

    @Test
    void setTemplateRecomputesSampledPropertyPathForTwoRootSelectionDesiredContract() {
        Customer c1 = new Customer("Customer", 61, true);
        Address a2 = new Address("AddressCity");

        OATemplate<OAObject> t = new OATemplate<>("<%=name%>");
        assertEquals("Customer", t.process(c1, a2));

        t.setTemplate("<%=city%>");

        assertEquals("AddressCity", t.process(c1, a2),
            "root selection cache must be invalidated when template text changes");
    }
}
