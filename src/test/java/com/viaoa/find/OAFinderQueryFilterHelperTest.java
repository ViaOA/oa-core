package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderQueryFilterHelperTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);

        public Hub<Child> getChildren() {
            return children;
        }
    }

    public static class Child extends OAObject {
        private String name;
        private int score;
        private boolean active;

        public Child() {
        }

        public Child(String name, int score, boolean active) {
            this.name = name;
            this.score = score;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public int getScore() {
            return score;
        }

        public boolean getActive() {
            return active;
        }
    }

    private static Root root() {
        Root root = new Root();
        root.getChildren().add(new Child("Alpha", 10, true));
        root.getChildren().add(new Child("Beta", 20, false));
        root.getChildren().add(new Child("Gamma", 30, true));
        return root;
    }

    @Test
    void addEqualFilterMatchesTerminalObjectsByProperty() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.addEqualFilter("name", "Beta");

        List<Child> result = finder.find(root);

        assertEquals(1, result.size());
        assertEquals("Beta", result.get(0).getName());
    }

    @Test
    void addNotEqualFilterExcludesMatchingTerminalObjects() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.addNotEqualFilter("name", "Beta");

        List<Child> result = finder.find(root);

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(c -> "Beta".equals(c.getName())));
    }

    @Test
    void addGreaterAndLessFiltersCanBeComposed() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.addGreaterFilter("score", 10);
        finder.addLessFilter("score", 30);

        List<Child> result = finder.find(root);

        assertEquals(1, result.size());
        assertEquals("Beta", result.get(0).getName());
    }

    @Test
    void addGreaterOrEqualAndLessOrEqualFiltersIncludeBoundaries() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.addGreaterOrEqualFilter("score", 10);
        finder.addLessOrEqualFilter("score", 20);

        List<Child> result = finder.find(root);

        assertEquals(2, result.size());
        assertEquals("Alpha", result.get(0).getName());
        assertEquals("Beta", result.get(1).getName());
    }

    @Test
    void addLikeAndNotLikeFiltersMatchTerminalProperty() {
        Root root = root();

        OAFinder<Root, Child> like = new OAFinder<>("children");
        like.addLikeFilter("name", "A*");
        assertEquals(1, like.find(root).size());
        assertEquals("Alpha", like.findFirst(root).getName());

        OAFinder<Root, Child> notLike = new OAFinder<>("children");
        notLike.addNotLikeFilter("name", "A*");
        assertEquals(2, notLike.find(root).size());
    }

    @Test
    void addQueryFilterAppliesQueryToTerminalObjects() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.setQueryFilter("score >= 20 AND active = true");

        List<Child> result = finder.find(root);

        assertEquals(1, result.size());
        assertEquals("Gamma", result.get(0).getName());
    }

    @Test
    void clearFiltersRemovesQueryFilter() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.setQueryFilter("score >= 20");
        assertEquals(2, finder.find(root).size());

        finder.clearFilters();

        assertEquals(3, finder.find(root).size());
    }
}
