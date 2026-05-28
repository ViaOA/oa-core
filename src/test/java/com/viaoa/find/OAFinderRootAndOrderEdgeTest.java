package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderRootAndOrderEdgeTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);
        private String name;

        public Root() {
        }

        public Root(String name) {
            this.name = name;
        }

        public Hub<Child> getChildren() {
            return children;
        }

        public String getName() {
            return name;
        }
    }

    public static class Child extends OAObject {
        private String name;

        public Child() {
        }

        public Child(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static Root root(String rootName, String... childNames) {
        Root r = new Root(rootName);
        for (String name : childNames) {
            r.getChildren().add(new Child(name));
        }
        return r;
    }

    @Test
    void listFindStartsAfterLastUsedRoot() {
        Root r1 = root("r1", "A", "B");
        Root r2 = root("r2", "C");
        Root r3 = root("r3", "D");

        OAFinder<Root, Child> finder = new OAFinder<>("children");

        List<Child> result = finder.find(Arrays.asList(r1, r2, r3), r1);

        assertEquals(2, result.size());
        assertEquals("C", result.get(0).getName());
        assertEquals("D", result.get(1).getName());
    }

    @Test
    void listFindWithLastUsedNotInListStartsAtBeginningCurrentContract() {
        Root r1 = root("r1", "A");
        Root r2 = root("r2", "B");
        Root missing = root("missing", "Z");

        OAFinder<Root, Child> finder = new OAFinder<>("children");

        List<Child> result = finder.find(Arrays.asList(r1, r2), missing);

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals("B", result.get(1).getName());
    }

    @Test
    void hubFindWithLastUsedNotInHubStartsAtBeginningCurrentContract() {
        Root r1 = root("r1", "A");
        Root r2 = root("r2", "B");
        Root missing = root("missing", "Z");

        Hub<Root> hub = new Hub<>(Root.class);
        hub.add(r1);
        hub.add(r2);

        OAFinder<Root, Child> finder = new OAFinder<>("children");

        List<Child> result = finder.find(hub, missing);

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals("B", result.get(1).getName());
    }

    @Test
    void hubFindUpdatesRootHubPositionAtEndOfScan() {
        Root r1 = root("r1", "A");
        Root r2 = root("r2", "B");

        Hub<Root> hub = new Hub<>(Root.class);
        hub.add(r1);
        hub.add(r2);

        OAFinder<Root, Child> finder = new OAFinder<>("children");
        List<Child> result = finder.find(hub);

        assertEquals(2, result.size());
        assertTrue(finder.getRootHubPos() >= 2);
    }

    @Test
    void changingRootObjectAfterConstructionUsesNewRoot() {
        Root r1 = root("r1", "A");
        Root r2 = root("r2", "B");

        OAFinder<Root, Child> finder = new OAFinder<>(r1, "children");

        assertEquals("A", finder.findFirst().getName());

        finder.setRoot(r2);

        assertEquals("B", finder.findFirst().getName());
    }

    @Test
    void changingRootHubAfterConstructionUsesNewHub() {
        Hub<Root> h1 = new Hub<>(Root.class);
        h1.add(root("r1", "A"));

        Hub<Root> h2 = new Hub<>(Root.class);
        h2.add(root("r2", "B"));

        OAFinder<Root, Child> finder = new OAFinder<>(h1, "children");

        assertEquals("A", finder.findFirst().getName());

        finder.setRoot(h2);

        assertEquals("B", finder.findFirst().getName());
    }
}
