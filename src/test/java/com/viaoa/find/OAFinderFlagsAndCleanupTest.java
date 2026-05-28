package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderFlagsAndCleanupTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);

        public Hub<Child> getChildren() {
            return children;
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

    private static Root root() {
        Root root = new Root();
        root.getChildren().add(new Child("A"));
        root.getChildren().add(new Child("B"));
        return root;
    }

    @Test
    void useOnlyLoadedDataFlagIsStoredAndDoesNotBreakLoadedTraversal() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertFalse(finder.getUseOnlyLoadedData());

        finder.setUseOnlyLoadedData(true);

        assertTrue(finder.getUseOnlyLoadedData());
        assertEquals(2, finder.find(root).size());
    }

    @Test
    void allowRecursiveRootFlagRoundTrips() {
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertFalse(finder.getAllowRecursiveRoot());

        finder.setAllowRecursiveRoot(true);
        assertTrue(finder.getAllowRecursiveRoot());

        finder.setAllowRecursiveRoot(false);
        assertFalse(finder.getAllowRecursiveRoot());
    }

    @Test
    void stopFlagIsResetAtStartOfEachSearch() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.stop();
        assertTrue(finder.getStop());

        assertEquals(2, finder.find(root).size());

        assertFalse(finder.getStop());
    }

    @Test
    void stackAccessorsReturnNullWhenStackTrackingDisabledOrAfterSearch() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertNull(finder.getStackObjects());
        assertNull(finder.getStackPropertyNames());

        finder.find(root);

        assertNull(finder.getStackObjects());
        assertNull(finder.getStackPropertyNames());
    }

    @Test
    void stackTrackingCanBeEnabledAndDisabledAcrossSearches() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.setEnabledStack(true);
        assertEquals(2, finder.find(root).size());

        assertNull(finder.getStackObjects());
        assertNull(finder.getStackPropertyNames());

        finder.setEnabledStack(false);
        assertEquals(2, finder.find(root).size());
    }
}
