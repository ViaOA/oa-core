package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderStateAndFilterTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);

        public Hub<Child> getChildren() {
            return children;
        }
    }

    public static class Child extends OAObject {
        private String name;
        private int score;

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
    }

    private static Root root() {
        Root root = new Root();
        root.getChildren().add(new Child("A", 10));
        root.getChildren().add(new Child("B", 20));
        root.getChildren().add(new Child("C", 30));
        return root;
    }

    @Test
    void canFindFirstTemporarilyUsesMaxFoundAndRestoresCallerValue() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.setMaxFound(2);

        assertTrue(finder.canFindFirst(root));
        assertEquals(2, finder.getMaxFound());
    }

    @Test
    void findFirstTemporarilyUsesMaxFoundAndRestoresCallerValue() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.setMaxFound(2);

        Child child = finder.findFirst(root);

        assertNotNull(child);
        assertEquals("A", child.getName());
        assertEquals(2, finder.getMaxFound());
    }

    @Test
    void findFirstOnFinderRootUsesConfiguredObjectRoot() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>(root, "children");

        assertEquals("A", finder.findFirst().getName());
        assertEquals(3, finder.find().size());
    }

    @Test
    void findOnFinderRootUsesConfiguredHubRootAndUseAllFlag() {
        Root r1 = root();
        Root r2 = root();
        r2.getChildren().clear();
        r2.getChildren().add(new Child("Z", 99));

        Hub<Root> hub = new Hub<>(Root.class);
        hub.add(r1);
        hub.add(r2);

        OAFinder<Root, Child> finder = new OAFinder<>(hub, "children", true);

        List<Child> result = finder.find();

        assertEquals(4, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals("Z", result.get(3).getName());
    }

    @Test
    void findOnConfiguredHubWithUseAllFalseUsesActiveObjectOnly() {
        Root r1 = root();
        Root r2 = root();
        r2.getChildren().clear();
        r2.getChildren().add(new Child("Z", 99));

        Hub<Root> hub = new Hub<>(Root.class);
        hub.add(r1);
        hub.add(r2);
        hub.setAO(r2);

        OAFinder<Root, Child> finder = new OAFinder<>(hub, "children", false);

        List<Child> result = finder.find();

        assertEquals(1, result.size());
        assertEquals("Z", result.get(0).getName());
    }

    @Test
    void clearFiltersRemovesPriorFilterChain() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.addFilter((OAFilter<Child>) child -> child.getScore() > 20);

        assertEquals(1, finder.find(root).size());

        finder.clearFilters();

        assertEquals(3, finder.find(root).size());
        assertNull(finder.getFilter());
    }

    @Test
    void addOrFilterComposesNextFilterWithOr() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.addFilter((OAFilter<Child>) child -> "A".equals(child.getName()));
        finder.addOrFilter();
        finder.addFilter((OAFilter<Child>) child -> "C".equals(child.getName()));

        List<Child> result = finder.find(root);

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getName());
        assertEquals("C", result.get(1).getName());
    }

    @Test
    void addAndFilterComposesNextFilterWithAnd() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.addFilter((OAFilter<Child>) child -> child.getScore() >= 20);
        finder.addAndFilter();
        finder.addFilter((OAFilter<Child>) child -> child.getScore() <= 20);

        List<Child> result = finder.find(root);

        assertEquals(1, result.size());
        assertEquals("B", result.get(0).getName());
    }

    @Test
    void stopFromOnFoundEndsSearchImmediately() {
        Root root = root();
        AtomicInteger found = new AtomicInteger();

        OAFinder<Root, Child> finder = new OAFinder<>("children") {
            @Override
            protected void onFound(Child obj) {
                super.onFound(obj);
                found.incrementAndGet();
                stop();
            }
        };

        List<Child> result = finder.find(root);

        assertEquals(1, result.size());
        assertEquals(1, found.get());
        assertTrue(finder.getStop());
    }

    @Test
    void finderCanBeReusedAfterStop() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children") {
            @Override
            protected void onFound(Child obj) {
                super.onFound(obj);
                stop();
            }
        };

        assertEquals(1, finder.find(root).size());
        assertEquals(1, finder.find(root).size());
    }
}
