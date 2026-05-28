package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderFinalCoverageSmokeTest {

    public static class Root extends OAObject {
        private Child child;
        private final Hub<Child> children = new Hub<>(Child.class);

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }

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
    void finderWithNoConfiguredRootReturnsNullFromFindConvenience() {
        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertNull(finder.find());
        assertNull(finder.findFirst());
        assertNull(finder.findLast());
    }

    @Test
    void oneToOnePathFindsSingleObject() {
        Root root = new Root();
        root.setChild(new Child("one"));

        OAFinder<Root, Child> finder = new OAFinder<>("child");

        assertEquals(1, finder.find(root).size());
        assertEquals("one", finder.findFirst(root).getName());
    }

    @Test
    void oneToOneNullPathReturnsEmptyResult() {
        Root root = new Root();

        OAFinder<Root, Child> finder = new OAFinder<>("child");

        assertTrue(finder.find(root).isEmpty());
        assertNull(finder.findFirst(root));
        assertFalse(finder.canFindFirst(root));
    }

    @Test
    void getPropertyPathIsInitializedAfterSuccessfulFind() {
        Root root = new Root();
        root.getChildren().add(new Child("A"));

        OAFinder<Root, Child> finder = new OAFinder<>("children");

        assertNull(finder.getPropertyPath());

        finder.find(root);

        assertNotNull(finder.getPropertyPath());
        assertEquals("children", finder.getPropertyPath().getPropertyPath());
    }
}
