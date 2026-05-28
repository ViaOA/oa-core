package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.filter.OAFilter;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAHierFinderBasicTest {

    public static class Node extends OAObject {
        private String value;
        private Boolean enabled;
        private Node parent;

        public Node() {
        }

        public Node(String value, Boolean enabled) {
            this.value = value;
            this.enabled = enabled;
        }

        public String getValue() {
            return value;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }
    }

    @Test
    void findFirstNullRootReturnsNull() {
        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent");

        assertNull(finder.findFirst(null));
        assertNull(finder.findFirstNotEmpty(null));
        assertNull(finder.findFirstEmpty(null));
        assertNull(finder.findFirstNotNull(null));
        assertNull(finder.findFirstTrue(null));
    }

    @Test
    void includeFromObjectReturnsStartingObjectValueFirst() {
        Node parent = new Node("parent", true);
        Node child = new Node("child", false);
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", true);

        assertEquals("child", finder.findFirst(child));
    }

    @Test
    void excludeFromObjectSkipsStartingObjectAndFindsParentValue() {
        Node parent = new Node("parent", true);
        Node child = new Node("child", false);
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", false);

        assertEquals("parent", finder.findFirst(child));
    }

    @Test
    void findFirstNotEmptySkipsEmptyStartingValueAndFindsParent() {
        Node parent = new Node("parent", true);
        Node child = new Node("", false);
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", true);

        assertEquals("parent", finder.findFirstNotEmpty(child));
    }

    @Test
    void findFirstEmptyFindsEmptyStartingValue() {
        Node parent = new Node("parent", true);
        Node child = new Node("", false);
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", true);

        assertEquals("", finder.findFirstEmpty(child));
    }

    @Test
    void findFirstNotNullFindsFirstNonNullValue() {
        Node parent = new Node("parent", true);
        Node child = new Node(null, false);
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", true);

        assertEquals("parent", finder.findFirstNotNull(child));
    }

    @Test
    void findFirstTrueUsesBooleanConversion() {
        Node parent = new Node("parent", true);
        Node child = new Node("child", false);
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("enabled", "parent", true);

        assertEquals(Boolean.TRUE, finder.findFirstTrue(child));
    }

    @Test
    void customFilterControlsAcceptedHierValue() {
        Node parent = new Node("parent", true);
        Node child = new Node("child", false);
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", true);
        OAFilter onlyParent = obj -> "parent".equals(obj);

        assertEquals("parent", finder.findFirst(child, onlyParent));
    }
}
