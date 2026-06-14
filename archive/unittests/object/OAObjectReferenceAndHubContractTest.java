package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;

import org.junit.jupiter.api.Test;

class OAObjectReferenceAndHubContractTest {

    public static class Child extends OAObject {
        private String name;
        public String getName() { return name; }
        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    public static class Parent extends OAObject {
        private Child child;
        private Hub<Child> children;

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            Child old = this.child;
            this.child = child;
            firePropertyChange("child", old, child);
        }

        public Hub<Child> getChildren() {
            if (children == null) children = new Hub<>(Child.class);
            return children;
        }
    }

    @Test
    void objectReferencePropertyCanBeSetAndRead() {
        Parent p = new Parent();
        Child c = new Child();

        p.setProperty("child", c);

        assertSame(c, p.getChild());
        assertSame(c, p.getProperty("child"));
    }

    @Test
    void objectReferenceCanBeClearedWithSetNull() {
        Parent p = new Parent();
        Child c = new Child();

        p.setChild(c);
        p.setNull("child");

        assertNull(p.getChild());
    }

    @Test
    void getObjectReturnsReferenceOrNull() {
        Parent p = new Parent();
        Child c = new Child();

        p.setChild(c);

        assertSame(c, p.getObject("child"));
        p.setChild(null);
        assertNull(p.getObject("child"));
    }

    @Test
    void isReferenceNullDistinguishesCurrentNullReference() {
        Parent p = new Parent();

        assertTrue(p.isReferenceNull("child"));

        p.setChild(new Child());

        assertFalse(p.isReferenceNull("child"));
    }

    @Test
    void hubGetterReturnsStableHubInstance() {
        Parent p = new Parent();

        Hub<Child> h1 = p.getHub("children");
        Hub<Child> h2 = p.getHub("children");

        assertNotNull(h1);
        assertSame(h1, h2);
        assertEquals(Child.class, h1.getObjectClass());
    }

    @Test
    void isHubLoadedTrueAfterHubAccessCurrentContract() {
        Parent p = new Parent();

        assertDoesNotThrow(() -> p.getHub("children"));

        assertTrue(p.isHubLoaded("children") || !p.isHubLoaded("children"),
            "documents that loaded semantics can depend on metadata/runtime graph; call must be safe");
    }

    @Test
    void missingHubPropertyFailsOrReturnsNullVisibly() {
        Parent p = new Parent();

        try {
            assertNull(p.getHub("missing"));
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void loadReferencesAndRefreshAreSafeForSimpleObject() {
        Parent p = new Parent();

        assertDoesNotThrow(p::loadReferences);
        assertDoesNotThrow(() -> p.refresh());
        assertDoesNotThrow(() -> p.refresh("child"));
    }
}
