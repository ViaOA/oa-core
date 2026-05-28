package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathTraversalTest {

    public static class Root extends OAObject {
        private String name;
        private Child child;
        private final Hub<Child> children = new Hub<>(Child.class);

        public Root() {
        }

        public Root(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }

        public Hub<Child> getChildren() {
            return children;
        }
    }

    public static class Child extends OAObject {
        private String name;
        private Integer score;
        private GrandChild grandChild;

        public Child() {
        }

        public Child(String name, Integer score) {
            this.name = name;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public Integer getScore() {
            return score;
        }

        public GrandChild getGrandChild() {
            return grandChild;
        }

        public void setGrandChild(GrandChild grandChild) {
            this.grandChild = grandChild;
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
        Root root = new Root("root");
        Child child = new Child("child", 42);
        child.setGrandChild(new GrandChild("grand"));
        root.setChild(child);
        root.getChildren().add(new Child("A", 10));
        root.getChildren().add(new Child("B", 20));
        root.getChildren().setAO(root.getChildren().getAt(1));
        return root;
    }

    @Test
    void getValueReturnsScalarTerminalValue() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        assertEquals("child", pp.getValue(root));
    }

    @Test
    void getValueReturnsNestedScalarTerminalValue() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "child.grandChild.name");

        assertEquals("grand", pp.getValue(root));
    }

    @Test
    void getValueReturnsObjectTerminalValue() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "child");

        assertSame(root.getChild(), pp.getValue(root));
    }

    @Test
    void getValueReturnsHubTerminalValue() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "children");

        assertSame(root.getChildren(), pp.getValue(root));
    }

    @Test
    void intermediateHubUsesActiveObjectForContinuation() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "children.name");

        assertEquals("B", pp.getValue(root));

        root.getChildren().setAO(root.getChildren().getAt(0));

        assertEquals("A", pp.getValue(root));
    }

    @Test
    void nullRootReturnsNull() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        assertNull(pp.getValue(null));
        assertNull(pp.getValueAsString(null));
        assertNull(pp.getLastLinkValue(null));
    }

    @Test
    void nullIntermediateValueStopsTraversalAndReturnsNull() {
        Root root = new Root("root");
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        assertNull(pp.getValue(root));
        assertNull(pp.getValueAsString(root));
    }

    @Test
    void nullIntermediateHubActiveObjectStopsTraversalAndReturnsNull() {
        Root root = root();
        root.getChildren().setAO(null);

        OAPath<Root> pp = new OAPath<>(Root.class, "children.name");

        assertNull(pp.getValue(root));
    }

    @Test
    void getLastLinkValueStopsAtLastLink() {
        Root root = root();

        OAPath<Root> scalar = new OAPath<>(Root.class, "child.name");
        assertSame(root.getChild(), scalar.getLastLinkValue(root));

        OAPath<Root> nested = new OAPath<>(Root.class, "child.grandChild.name");
        assertSame(root.getChild().getGrandChild(), nested.getLastLinkValue(root));
    }

    @Test
    void getValueWithLinksOnlyStopsBeforeScalarTerminal() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        assertSame(root.getChild(), pp.getValue(null, root, true));
    }

    @Test
    void getValueFromStartPositionContinuesFromAlreadyResolvedObject() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "child.grandChild.name");

        assertEquals("grand", pp.getValue(root.getChild(), 1));
        assertEquals("grand", pp.getValue(root.getChild().getGrandChild(), 2));
    }

    @Test
    void repeatedEvaluationIsStableAndDoesNotMutateCompiledMetadata() {
        Root root = root();
        OAPath<Root> pp = new OAPath<>(Root.class, "child.grandChild.name");

        int propCount = pp.getProperties().length;
        int methodCount = pp.getMethods().length;
        int classCount = pp.getClasses().length;

        for (int i = 0; i < 10; i++) {
            assertEquals("grand", pp.getValue(root));
            assertEquals(propCount, pp.getProperties().length);
            assertEquals(methodCount, pp.getMethods().length);
            assertEquals(classCount, pp.getClasses().length);
        }
    }
}
