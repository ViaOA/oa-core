package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAMatrixFinalEdgeRegressionTest {

    public static class Parent extends OAObject {
        private String name;
        private final Hub<Child> children = new Hub<>(Child.class);
        public Parent() { }
        public Parent(String name) { this.name = name; }
        public String getName() { return name; }
        public Hub<Child> getChildren() { return children; }
    }

    public static class Child extends OAObject {
        private String name;
        private final Hub<GrandChild> grandChildren = new Hub<>(GrandChild.class);
        public Child() { }
        public Child(String name) { this.name = name; }
        public String getName() { return name; }
        public Hub<GrandChild> getGrandChildren() { return grandChildren; }
    }

    public static class GrandChild extends OAObject {
        private String name;
        public GrandChild() { }
        public GrandChild(String name) { this.name = name; }
        public String getName() { return name; }
    }

    private static Hub<Parent> parents() {
        Parent p = new Parent("P");
        Child c1 = new Child("C1");
        c1.getGrandChildren().add(new GrandChild("G1"));
        c1.getGrandChildren().add(new GrandChild("G2"));
        Child c2 = new Child("C2");
        c2.getGrandChildren().add(new GrandChild("G3"));
        p.getChildren().add(c1);
        p.getChildren().add(c2);

        Hub<Parent> hub = new Hub<>(Parent.class);
        hub.add(p);
        return hub;
    }

    @Test
    void nestedDetailColumnsBuildExpectedRowExpansion() {
        OAMatrix m = new OAMatrix();
        OAMatrix.Column parent = m.addColumn(parents());
        OAMatrix.Column child = m.addDetailColumn(parent, "children");
        m.addDetailColumn(child, "grandChildren");

        assertEquals(3, m.getRowCount());

        assertEquals("P", ((Parent) m.getRealObject(0, 0)).getName());
        assertEquals("P", ((Parent) m.getRealObject(1, 0)).getName());
        assertEquals("P", ((Parent) m.getRealObject(2, 0)).getName());

        assertEquals("C1", ((Child) m.getRealObject(0, 1)).getName());
        assertEquals("C1", ((Child) m.getRealObject(1, 1)).getName());
        assertEquals("C2", ((Child) m.getRealObject(2, 1)).getName());

        assertEquals("G1", ((GrandChild) m.getObject(0, 2)).getName());
        assertEquals("G2", ((GrandChild) m.getObject(1, 2)).getName());
        assertEquals("G3", ((GrandChild) m.getObject(2, 2)).getName());
    }

    @Test
    void nestedPropertyPathFromRootIsComposedCorrectly() {
        OAMatrix m = new OAMatrix();
        OAMatrix.Column parent = m.addColumn(parents());
        OAMatrix.Column child = m.addDetailColumn(parent, "children");

        assertEquals("children.grandChildren", m.getPropertyPathFromRoot(child, "grandChildren"));
    }

    @Test
    void getRealObjectReturnsNullForBlankColumnWithoutAncestor() {
        OAMatrix m = new OAMatrix();

        assertNull(m.getRealObject(0, 0));
    }

    @Test
    void verifyLinkPropertyAcceptsValidLinksAndRejectsScalars() {
        assertTrue(OAMatrix.verifyLinkProperty(Parent.class, "children"));
        assertFalse(OAMatrix.verifyLinkProperty(Parent.class, "name"));
    }

    @Test
    void getRootColumnNullIsNull() {
        OAMatrix m = new OAMatrix();

        assertNull(m.getRootColumn(null));
    }
}
