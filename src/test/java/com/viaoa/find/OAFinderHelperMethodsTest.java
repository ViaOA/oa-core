package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderHelperMethodsTest {

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
        root.getChildren().add(new Child("B", 30));
        root.getChildren().add(new Child("C", 20));
        root.getChildren().add(new Child("B", 40));
        root.getChildren().add(new Child(null, 50));
        return root;
    }

    @Test
    void findLargestFindsObjectWithLargestPropertyValueAndRestoresOriginalFilter() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.addFilter(obj -> ((Child) obj).getScore() < 50);

        Child largest = finder.findLargest(root, "score");

        assertNotNull(largest);
        assertEquals(40, largest.getScore());

        List<Child> after = finder.find(root);
        assertEquals(4, after.size());
    }

    @Test
    void findSmallestFindsObjectWithSmallestPropertyValueAndRestoresOriginalFilter() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.addFilter(obj -> ((Child) obj).getScore() >= 20);

        Child smallest = finder.findSmallest(root, "score");

        assertNotNull(smallest);
        assertEquals(20, smallest.getScore());

        List<Child> after = finder.find(root);
        assertEquals(4, after.size());
    }

    @Test
    void findDuplicatesReturnsFirstAndLaterObjectsForDuplicateNonNullValues() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        List<Child> dups = finder.findDuplicates(root, "code");

        assertEquals(2, dups.size());
        assertTrue(dups.stream().allMatch(c -> "B".equals(c.getCode())));
    }

    @Test
    void findDuplicatesIgnoresNullValues() {
        Root root = new Root();
        root.getChildren().add(new Child(null, 10));
        root.getChildren().add(new Child(null, 20));

        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertTrue(finder.findDuplicates(root, "code").isEmpty());
    }

    @Test
    void helperMethodsWorkAgainstHubRoot() {
        Root r1 = root();
        Root r2 = new Root();
        r2.getChildren().add(new Child("Z", 99));

        Hub<Root> hub = new Hub<>(Root.class);
        hub.add(r1);
        hub.add(r2);

        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertEquals(99, finder.findLargest(hub, "score").getScore());
        assertEquals(10, finder.findSmallest(hub, "score").getScore());
    }
}
