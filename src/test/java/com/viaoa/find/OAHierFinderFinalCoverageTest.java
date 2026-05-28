package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAHierFinderFinalCoverageTest {

    public static class Node extends OAObject {
        private String value;
        private Boolean enabled;
        private Node parent;
        private Node owner;

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

        public Node getOwner() {
            return owner;
        }

        public void setOwner(Node owner) {
            this.owner = owner;
        }
    }

    @Test
    void defaultConstructorIncludesFromObject() {
        Node node = new Node("self", true);
        node.setParent(new Node("parent", true));

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent");

        assertEquals("self", finder.findFirstNotEmpty(node));
    }

    @Test
    void findFirstEmptyCanFindNullStartingValue() {
        Node node = new Node(null, true);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", true);

        assertNull(finder.findFirstEmpty(node));
    }

    @Test
    void findFirstTrueReturnsNullWhenAllValuesFalseOrNull() {
        Node parent = new Node("parent", null);
        Node child = new Node("child", false);
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("enabled", "parent", true);

        assertNull(finder.findFirstTrue(child));
    }

    @Test
    void repeatedDifferentRootsDoNotReuseFoundValue() {
        Node parent1 = new Node("p1", true);
        Node child1 = new Node("", false);
        child1.setParent(parent1);

        Node child2 = new Node("", false);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", true);

        assertEquals("p1", finder.findFirstNotEmpty(child1));
        assertNull(finder.findFirstNotEmpty(child2));
    }

    @Test
    void ownerThenParentPathCanFindNearestAvailableValue() {
        Node ownerParent = new Node("ownerParent", true);
        Node owner = new Node("", true);
        owner.setParent(ownerParent);

        Node child = new Node("", false);
        child.setOwner(owner);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "owner.parent", false);

        assertEquals("ownerParent", finder.findFirstNotEmpty(child));
    }
}
