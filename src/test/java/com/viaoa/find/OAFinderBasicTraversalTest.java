package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderBasicTraversalTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);
        private Child child;
        private String name;

        public Root() {
        }

        public Root(String name) {
            this.name = name;
        }

        public Hub<Child> getChildren() {
            return children;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }

        public String getName() {
            return name;
        }
    }

    public static class Child extends OAObject {
        private String name;
        private int score;
        private GrandChild grandChild;

        public Child() {
        }

        public Child(String name, int score) {
            this.name = name;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public int getScore() {
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

    private static Root rootWithChildren() {
        Root root = new Root("root");
        Child a = new Child("A", 10);
        Child b = new Child("B", 20);
        root.getChildren().add(a);
        root.getChildren().add(b);
        root.setChild(a);
        a.setGrandChild(new GrandChild("GA"));
        b.setGrandChild(new GrandChild("GB"));
        return root;
    }

    @Test
    void findNullObjectRootReturnsNull() {
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertNull(finder.find((Root) null));
        assertNull(finder.findFirst((Root) null));
        assertFalse(finder.canFindFirst(null));
    }

    @Test
    void findNullHubRootReturnsEmptyList() {
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        List<Child> result = finder.find((Hub<Root>) null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findNullListRootReturnsEmptyList() {
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        List<Child> result = finder.find((List<Root>) null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findEmptyAndAllNullListsReturnEmptyList() {
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertTrue(finder.find(Collections.emptyList()).isEmpty());
        assertTrue(finder.find(Arrays.asList(null, null)).isEmpty());
    }

    @Test
    void findTraversesObjectToHubPathInHubOrder() {
        Root root = rootWithChildren();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        List<Child> result = finder.find(root);

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals("B", result.get(1).getName());
    }

    @Test
    void findFirstAndFindLastUseTraversalOrder() {
        Root root = rootWithChildren();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertEquals("A", finder.findFirst(root).getName());
        assertEquals("B", finder.findLast(root).getName());
    }

    @Test
    void findFromHubTraversesRootObjectsInHubOrder() {
        Root r1 = rootWithChildren();
        Root r2 = new Root("root2");
        r2.getChildren().add(new Child("C", 30));

        Hub<Root> hub = new Hub<>(Root.class);
        hub.add(r1);
        hub.add(r2);

        OAFinder<Root, Child> finder = new OAFinder<>("children");
        List<Child> result = finder.find(hub);

        assertEquals(3, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals("B", result.get(1).getName());
        assertEquals("C", result.get(2).getName());
    }

    @Test
    void findNextStartsAfterLastUsedRootObject() {
        Root r1 = rootWithChildren();
        Root r2 = new Root("root2");
        r2.getChildren().add(new Child("C", 30));

        Hub<Root> hub = new Hub<>(Root.class);
        hub.add(r1);
        hub.add(r2);

        OAFinder<Root, Child> finder = new OAFinder<>("children");

        Child next = finder.findNext(hub, r1);

        assertNotNull(next);
        assertEquals("C", next.getName());
    }

    @Test
    void findListSkipsNullRootsAndPreservesListOrder() {
        Root r1 = rootWithChildren();
        Root r2 = new Root("root2");
        r2.getChildren().add(new Child("C", 30));

        OAFinder<Root, Child> finder = new OAFinder<>("children");
        List<Child> result = finder.find(Arrays.asList(null, r1, null, r2));

        assertEquals(3, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals("B", result.get(1).getName());
        assertEquals("C", result.get(2).getName());
    }

    @Test
    void maxFoundStopsAfterConfiguredNumberOfResults() {
        Root root = rootWithChildren();
        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.setMaxFound(1);

        List<Child> result = finder.find(root);

        assertEquals(1, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals(1, finder.getMaxFound());
    }

    @Test
    void terminalFilterAppliesOnlyToFoundObjects() {
        Root root = rootWithChildren();
        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.addFilter((OAFilter<Child>) child -> child != null && child.getScore() >= 20);

        List<Child> result = finder.find(root);

        assertEquals(1, result.size());
        assertEquals("B", result.get(0).getName());
    }

    @Test
    void nestedObjectPathCanFindGrandChild() {
        Root root = rootWithChildren();
        OAFinder<Root, GrandChild> finder = new OAFinder<>("children.grandChild");

        List<GrandChild> result = finder.find(root);

        assertEquals(2, result.size());
        assertEquals("GA", result.get(0).getName());
        assertEquals("GB", result.get(1).getName());
    }
}
