package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathHubAndLastLinkBoundaryTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);
        private Child child;

        public Hub<Child> getChildren() {
            return children;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }
    }

    public static class Child extends OAObject {
        private String name;
        private GrandChild grandChild;
        private final Hub<GrandChild> grandChildren = new Hub<>(GrandChild.class);

        public Child() {
        }

        public Child(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public GrandChild getGrandChild() {
            return grandChild;
        }

        public void setGrandChild(GrandChild grandChild) {
            this.grandChild = grandChild;
        }

        public Hub<GrandChild> getGrandChildren() {
            return grandChildren;
        }
    }

    public static class GrandChild extends OAObject {
        private String name;

        public GrandChild() {
        }

        public GrandChild(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static Root root() {
        Root root = new Root();
        Child a = new Child("A");
        Child b = new Child("B");
        a.setGrandChild(new GrandChild("GA"));
        b.setGrandChild(new GrandChild("GB"));
        a.getGrandChildren().add(new GrandChild("GA1"));
        a.getGrandChildren().add(new GrandChild("GA2"));
        a.getGrandChildren().setAO(a.getGrandChildren().getAt(1));

        root.getChildren().add(a);
        root.getChildren().add(b);
        root.getChildren().setAO(a);
        root.setChild(a);
        return root;
    }

    @Test
    void hubTerminalPathReturnsHubNotActiveObject() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "children");

        Object val = pp.getValue(root);

        assertSame(root.getChildren(), val);
    }

    @Test
    void intermediateHubUsesActiveObjectForFollowingSegment() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "children.name");

        assertEquals("A", pp.getValue(root));

        root.getChildren().setAO(root.getChildren().getAt(1));

        assertEquals("B", pp.getValue(root));
    }

    @Test
    void nestedIntermediateHubsUseActiveObjectAtEachHubSegment() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "children.grandChildren.name");

        assertEquals("GA2", pp.getValue(root));

        Child activeChild = root.getChildren().getAO();
        activeChild.getGrandChildren().setAO(activeChild.getGrandChildren().getAt(0));

        assertEquals("GA1", pp.getValue(root));
    }

    @Test
    void lastLinkValueForPathEndingInHubReturnsThatHub() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "children");

        assertSame(root.getChildren(), pp.getLastLinkValue(root));
    }

    @Test
    void lastLinkValueForNestedScalarStopsAtLastObjectLink() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "child.grandChild.name");

        assertSame(root.getChild().getGrandChild(), pp.getLastLinkValue(root));
    }

    @Test
    void linksOnlyForNestedHubScalarStopsAtHubTerminalBeforeScalar() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "children.grandChildren.name");

        Object val = pp.getValue(null, root, true);

        assertSame(root.getChildren().getAO().getGrandChildren(), val);
    }

    @Test
    void nullActiveObjectInIntermediateHubStopsTraversal() {
        Root root = root();
        root.getChildren().setAO(null);

        OAPath<Root> pp = new OAPath<>(Root.class, "children.name");

        assertNull(pp.getValue(root));
        assertNull(pp.getLastLinkValue(root));
    }
}
