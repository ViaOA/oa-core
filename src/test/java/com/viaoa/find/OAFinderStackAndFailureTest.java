package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.object.OAObject;
import com.viaoa.hub.Hub;

import org.junit.jupiter.api.Test;

class OAFinderStackAndFailureTest {

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

    @Test
    void stackObjectsAndPropertyNamesAreAvailableDuringOnFound() {
        Root root = new Root();
        root.getChildren().add(new Child("A"));

        List<Object[]> stackObjects = new ArrayList<>();
        List<String[]> stackNames = new ArrayList<>();

        OAFinder<Root, Child> finder = new OAFinder<>("children") {
            @Override
            protected void onFound(Child obj) {
                stackObjects.add(getStackObjects());
                stackNames.add(getStackPropertyNames());
                super.onFound(obj);
            }
        };
        finder.setEnabledStack(true);

        List<Child> result = finder.find(root);

        assertEquals(1, result.size());
        assertEquals(1, stackObjects.size());
        assertTrue(stackObjects.get(0).length >= 2);
        assertEquals("[root]", stackNames.get(0)[0]);
        assertTrue(java.util.Arrays.asList(stackNames.get(0)).contains("children"));
    }

    @Test
    void invalidScalarTerminalPathFailsVisibly() {
        Root root = new Root();

        OAFinder<Root, Child> finder = new OAFinder<>("children.name");

        assertThrows(RuntimeException.class, () -> finder.find(root));
    }

    @Test
    void invalidPathDoesNotMakeFinderPermanentlySuccessfulOnRetry() {
        Root root = new Root();
        OAFinder<Root, Child> finder = new OAFinder<>("missingPath");

        assertThrows(RuntimeException.class, () -> finder.find(root));
        assertThrows(RuntimeException.class, () -> finder.find(root));
    }

    @Test
    void throwingFilterCleansPerSearchStateAndFinderCanRetry() {
        Root root = new Root();
        root.getChildren().add(new Child("A"));

        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.addFilter(obj -> {
            throw new IllegalStateException("boom");
        });

        assertThrows(IllegalStateException.class, () -> finder.find(root));
        assertThrows(IllegalStateException.class, () -> finder.find(root));

        finder.clearFilters();
        assertEquals(1, finder.find(root).size());
    }
}
