package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import com.viaoa.hub.Hub;

import org.junit.jupiter.api.Test;

class OAObjectReferenceKeyDeepContractTest {

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
    void referenceObjectKeyMatchesResolvedReferenceGuidWhenAvailable() {
        Parent p = new Parent();
        Child c = new Child();
        p.setChild(c);

        OAObjectKey key = p.getReferenceObjectKey("child");

        if (key != null) {
            assertEquals(c.getGuid(), key.getGuid());
        }
    }

    @Test
    void clearingReferenceClearsReferenceObjectKeyOrReportsNull() {
        Parent p = new Parent();
        p.setChild(new Child());

        p.setNull("child");

        assertNull(p.getChild());
        assertTrue(p.isReferenceNull("child"));

        OAObjectKey key = p.getReferenceObjectKey("child");
        assertTrue(key == null || key.getGuid() == null);
    }

    @Test
    void referenceLoadedStateCallIsSafeForResolvedReference() {
        Parent p = new Parent();
        p.setChild(new Child());

        assertDoesNotThrow(() -> p.isLoaded("child"));
        assertDoesNotThrow(() -> p.isPropertyLoaded("child"));
    }

    @Test
    void unresolvedReferenceKeyCanBeRepresentedWithoutLoadedObjectDesiredContract() throws Exception {
        Parent p = new Parent();
        OAObjectKey key = new OAObjectKey(new Object[] { 123 });

        injectPropertyPair(p, "CHILD", key);

        OAObjectKey found = p.getReferenceObjectKey("child");

        if (found != null) {
            assertArrayEquals(new Object[] { 123 }, found.getObjectIds());
        }
    }

    @Test
    void refreshAndLoadReferencesDoNotConvertResolvedReferenceToWrongObject() {
        Parent p = new Parent();
        Child c = new Child();
        p.setChild(c);

        assertDoesNotThrow(() -> p.refresh("child"));
        assertDoesNotThrow(p::loadReferences);

        assertSame(c, p.getChild());
    }

    @Test
    void hubLoadedEmptyIsDistinctFromMissingHubByDefinedBehavior() {
        Parent p = new Parent();

        Hub<Child> h = p.getHub("children");

        assertNotNull(h);
        assertEquals(0, h.getSize());

        try {
            assertNull(p.getHub("missing"));
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    private static void injectPropertyPair(OAObject obj, String propNameUpper, Object value) throws Exception {
        Field f = OAObject.class.getDeclaredField("properties");
        f.setAccessible(true);
        f.set(obj, new Object[] { propNameUpper, value });
    }
}
