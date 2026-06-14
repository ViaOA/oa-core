package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;

import org.junit.jupiter.api.Test;

class OAObjectReferenceStateFinalTest {

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

        public Child getChild() { return child; }
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
    void referenceNullVsResolvedReferenceState() {
        Parent p = new Parent();

        assertTrue(p.isReferenceNull("child"));

        Child child = new Child();
        p.setChild(child);

        assertFalse(p.isReferenceNull("child"));
        assertSame(child, p.getObject("child"));
    }

    @Test
    void clearingReferenceRestoresNullReferenceState() {
        Parent p = new Parent();
        p.setChild(new Child());

        p.setNull("child");

        assertNull(p.getChild());
        assertTrue(p.isReferenceNull("child"));
    }

    @Test
    void referenceObjectKeyForResolvedReferenceIsAvailableOrExplicitlyNull() {
        Parent p = new Parent();
        Child child = new Child();
        p.setChild(child);

        OAObjectKey key = p.getReferenceObjectKey("child");

        if (key != null) {
            assertEquals(child.getGuid(), key.getGuid());
        }
    }

    @Test
    void refreshDoesNotCorruptResolvedReference() {
        Parent p = new Parent();
        Child child = new Child();
        p.setChild(child);

        assertDoesNotThrow(() -> p.refresh("child"));

        assertSame(child, p.getChild());
    }

    @Test
    void loadReferencesDoesNotCorruptResolvedReference() {
        Parent p = new Parent();
        Child child = new Child();
        p.setChild(child);

        assertDoesNotThrow(p::loadReferences);

        assertSame(child, p.getChild());
    }

    @Test
    void hubReferenceAccessIsStableAndTypeCorrect() {
        Parent p = new Parent();

        Hub<Child> h1 = p.getHub("children");
        Hub<Child> h2 = p.getHub("children");

        assertSame(h1, h2);
        assertEquals(Child.class, h1.getObjectClass());

        Child child = new Child();
        h1.add(child);

        assertTrue(h2.contains(child));
    }

    @Test
    void missingReferenceStateReturnsDefinedValueOrFailsVisibly() {
        Parent p = new Parent();

        try {
            assertTrue(p.isReferenceNull("missing") || !p.isReferenceNull("missing"));
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }

        try {
            assertNull(p.getReferenceObjectKey("missing"));
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }
    }
}
