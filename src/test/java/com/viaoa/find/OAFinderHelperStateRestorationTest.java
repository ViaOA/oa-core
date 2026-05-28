package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderHelperStateRestorationTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);

        public Hub<Child> getChildren() {
            return children;
        }
    }

    public static class Child extends OAObject {
        private String code;
        private int score;

        public Child() {
        }

        public Child(String code, int score) {
            this.code = code;
            this.score = score;
        }

        public String getCode() {
            return code;
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
        root.getChildren().add(new Child("B", 40));
        return root;
    }

    @Test
    void findLargestDoesNotConsumePendingOrFilterState() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.addFilter((OAFilter<Child>) c -> "A".equals(c.getCode()));
        finder.addOrFilter();

        Child largest = finder.findLargest(root, "score");
        assertNotNull(largest);

        finder.addFilter((OAFilter<Child>) c -> "C".equals(c.getCode()));

        List<Child> result = finder.find(root);
        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getCode());
        assertEquals("C", result.get(1).getCode());
    }

    @Test
    void findSmallestDoesNotConsumePendingAndFilterState() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.addFilter((OAFilter<Child>) c -> c.getScore() >= 20);
        finder.addAndFilter();

        Child smallest = finder.findSmallest(root, "score");
        assertNotNull(smallest);

        finder.addFilter((OAFilter<Child>) c -> c.getScore() <= 30);

        List<Child> result = finder.find(root);
        assertEquals(2, result.size());
        assertEquals(20, result.get(0).getScore());
        assertEquals(30, result.get(1).getScore());
    }

    @Test
    void findDuplicatesRestoresOriginalFilterAndCompositionFlags() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.addFilter((OAFilter<Child>) c -> c.getScore() >= 20);
        finder.addOrFilter();

        List<Child> dups = finder.findDuplicates(root, "code");
        assertEquals(2, dups.size());

        finder.addFilter((OAFilter<Child>) c -> "A".equals(c.getCode()));

        List<Child> result = finder.find(root);

        assertEquals(4, result.size());
    }

    @Test
    void canFindFirstRestoresMaxFoundAfterFilterException() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.setMaxFound(3);
        finder.addFilter(obj -> {
            throw new IllegalStateException("boom");
        });

        assertThrows(IllegalStateException.class, () -> finder.canFindFirst(root));
        assertEquals(3, finder.getMaxFound());
    }
}
