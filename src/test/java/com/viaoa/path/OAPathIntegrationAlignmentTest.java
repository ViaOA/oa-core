package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.filter.OAEqualFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathIntegrationAlignmentTest {

    public static class Root extends OAObject {
        private Child child;
        private final Hub<Child> children = new Hub<>(Child.class);

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
        private GrandChild grandChild;

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
        a.setGrandChild(new GrandChild("GA"));
        Child b = new Child("B");
        b.setGrandChild(new GrandChild("GB"));
        root.setChild(a);
        root.getChildren().add(a);
        root.getChildren().add(b);
        root.getChildren().setAO(b);
        return root;
    }

    @Test
    void filterPathMatchesDirectPathValueForOneToOnePath() {
        Root root = root();

        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");
        Object direct = pp.getValue(root);

        assertEquals("A", direct);
        assertTrue(new OAEqualFilter("child.name", direct).isUsed(root));
        assertFalse(new OAEqualFilter("child.name", "B").isUsed(root));
    }

    @Test
    void finderPathTerminalObjectsMatchDirectHubTraversalForHubPath() {
        Root root = root();

        OAFinder<Root, Child> finder = new OAFinder<>("children");
        List<Child> found = finder.find(root);

        assertEquals(2, found.size());
        assertSame(root.getChildren().getAt(0), found.get(0));
        assertSame(root.getChildren().getAt(1), found.get(1));
    }

    @Test
    void pathIntermediateHubActiveObjectSemanticsDifferFromFinderAllMemberTraversalByContract() {
        Root root = root();

        OAPath<Root> path = new OAPath<>(Root.class, "children.name");
        assertEquals("B", path.getValue(root));

        OAFinder<Root, Child> finder = new OAFinder<>("children");
        assertEquals(2, finder.find(root).size());
    }

    @Test
    void finderNestedPathFindsAllTargetsWhilePathUsesActiveObjects() {
        Root root = root();

        OAPath<Root> path = new OAPath<>(Root.class, "children.grandChild.name");
        assertEquals("GB", path.getValue(root));

        OAFinder<Root, GrandChild> finder = new OAFinder<>("children.grandChild");
        List<GrandChild> found = finder.find(root);

        assertEquals(2, found.size());
        assertEquals("GA", found.get(0).getName());
        assertEquals("GB", found.get(1).getName());
    }
}
