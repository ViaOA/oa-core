package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderSetupAndFailureContractTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);

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
        root.getChildren().add(a);
        root.getChildren().add(new Child("B"));
        return root;
    }

    @Test
    void invalidScalarTerminalPathFailsEveryTimeAndDoesNotPoisonDifferentFinder() {
        Root root = root();

        OAFinder<Root, Child> bad = new OAFinder<>("children.name");

        assertThrows(RuntimeException.class, () -> bad.find(root));
        assertThrows(RuntimeException.class, () -> bad.find(root));

        OAFinder<Root, Child> good = new OAFinder<>("children");
        assertEquals(2, good.find(root).size());
    }

    @Test
    void setupFailureDoesNotConsumeCallerFilterChain() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children.name");
        finder.addFilter((OAFilter<Child>) child -> "A".equals(child.getName()));

        assertThrows(RuntimeException.class, () -> finder.find(root));
        assertNotNull(finder.getFilter());

        finder = new OAFinder<>("children");
        finder.addFilter((OAFilter<Child>) child -> "A".equals(child.getName()));
        List<Child> result = finder.find(root);

        assertEquals(1, result.size());
        assertEquals("A", result.get(0).getName());
    }

    @Test
    void traversalExceptionCleansStateAndCanRunAfterFilterCleared() {
        Root root = root();
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        finder.addFilter(obj -> {
            throw new IllegalArgumentException("test failure");
        });

        assertThrows(IllegalArgumentException.class, () -> finder.find(root));

        finder.clearFilters();

        assertEquals(2, finder.find(root).size());
    }

    @Test
    void onFoundExceptionCleansPerSearchStateAndCanRetryAfterSubclassStopsThrowing() {
        Root root = root();

        class ToggleFinder extends OAFinder<Root, Child> {
            boolean throwNow = true;

            ToggleFinder() {
                super("children");
            }

            @Override
            protected void onFound(Child obj) {
                if (throwNow) {
                    throw new IllegalStateException("boom");
                }
                super.onFound(obj);
            }
        }

        ToggleFinder finder = new ToggleFinder();

        assertThrows(IllegalStateException.class, () -> finder.find(root));

        finder.throwNow = false;

        assertEquals(2, finder.find(root).size());
    }

    @Test
    void nullIntermediateReferenceStopsOnlyThatBranch() {
        Root root = root();

        OAFinder<Root, GrandChild> finder = new OAFinder<>("children.grandChild");

        List<GrandChild> result = finder.find(root);

        assertEquals(1, result.size());
        assertEquals("GA", result.get(0).getName());
    }
}
