package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderNullMatchAmbiguityTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);
        private Child child;

        public Hub<Child> getChildren() {
            return children;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
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
    void findFirstReturnsNullForNoMatchAndCanFindFirstDisambiguatesNoMatch() {
        Root root = new Root();
        root.getChildren().add(new Child("A"));

        OAFinder<Root, Child> finder = new OAFinder<>("children");
        finder.addFilter((OAFilter<Child>) child -> "Z".equals(child.getName()));

        assertNull(finder.findFirst(root));
        assertFalse(finder.canFindFirst(root));
    }

    @Test
    void oneToOneNullReferenceProducesNoResultNotNullResultElement() {
        Root root = new Root();
        root.setChild(null);

        OAFinder<Root, Child> finder = new OAFinder<>("child");

        List<Child> result = finder.find(root);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertNull(finder.findFirst(root));
        assertFalse(finder.canFindFirst(root));
    }

    @Test
    void filterThatWouldAcceptNullIsNotCalledForMissingOneToOneTargetCurrentContract() {
        Root root = new Root();
        root.setChild(null);

        OAFinder<Root, Child> finder = new OAFinder<>("child");
        finder.addFilter(obj -> obj == null);

        assertTrue(finder.find(root).isEmpty());
        assertFalse(finder.canFindFirst(root));
    }
}
