package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderConvenienceEdgeTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);

        public Hub<Child> getChildren() {
            return children;
        }
    }

    public static class Child extends OAObject {
        private String code;
        private Integer score;

        public Child() {
        }

        public Child(String code, Integer score) {
            this.code = code;
            this.score = score;
        }

        public String getCode() {
            return code;
        }

        public Integer getScore() {
            return score;
        }
    }

    private static Root root() {
        Root root = new Root();
        root.getChildren().add(new Child("A", 10));
        root.getChildren().add(new Child("B", 30));
        root.getChildren().add(new Child("B", 20));
        root.getChildren().add(new Child("C", null));
        return root;
    }

    @Test
    void findLargestReturnsNullWhenNoResults() {
        Root root = new Root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertNull(finder.findLargest(root, "score"));
    }

    @Test
    void findSmallestReturnsNullWhenNoResults() {
        Root root = new Root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertNull(finder.findSmallest(root, "score"));
    }

    @Test
    void findDuplicatesReturnsEmptyWhenNoDuplicates() {
        Root root = new Root();
        root.getChildren().add(new Child("A", 10));
        root.getChildren().add(new Child("B", 20));

        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertTrue(finder.findDuplicates(root, "code").isEmpty());
    }

    @Test
    void findDuplicatesWorksWithActiveFilterAndRestoresIt() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.addFilter((OAFilter<Child>) c -> c.getScore() == null || c.getScore() >= 20);

        List<Child> dups = finder.findDuplicates(root, "code");

        assertEquals(2, dups.size());
        assertTrue(dups.stream().allMatch(c -> "B".equals(c.getCode())));

        List<Child> after = finder.find(root);
        assertEquals(3, after.size());
    }

    @Test
    void findFirstFindLastAndCanFindFirstOnEmptyResultAreConsistent() {
        Root root = new Root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertNull(finder.findFirst(root));
        assertNull(finder.findLast(root));
        assertFalse(finder.canFindFirst(root));
    }

    @Test
    void findNextAfterLastRootReturnsNull() {
        Root r1 = root();
        Hub<Root> hub = new Hub<>(Root.class);
        hub.add(r1);

        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertNull(finder.findNext(hub, r1));
    }
}
