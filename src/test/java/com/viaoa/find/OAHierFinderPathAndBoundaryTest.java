package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAHierFinderPathAndBoundaryTest {

    public static class Node extends OAObject {
        private String value;
        private String fallback;
        private Node parent;
        private Node owner;

        public Node() {
        }

        public Node(String value, String fallback) {
            this.value = value;
            this.fallback = fallback;
        }

        public String getValue() {
            return value;
        }

        public String getFallback() {
            return fallback;
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
    void hierFinderCanTraverseTwoSegmentPath() {
        Node ownerParent = new Node("ownerParentValue", "ownerParentFallback");
        Node owner = new Node("", "");
        owner.setParent(ownerParent);

        Node child = new Node("", "");
        child.setOwner(owner);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "owner.parent", false);

        assertEquals("ownerParentValue", finder.findFirstNotEmpty(child));
    }

    @Test
    void hierFinderStopsAtNullIntermediatePath() {
        Node child = new Node("", "");
        child.setOwner(null);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "owner.parent", false);

        assertNull(finder.findFirstNotEmpty(child));
    }

    @Test
    void hierFinderCanSearchDifferentPropertyOnSamePath() {
        Node parent = new Node("", "fallbackParent");
        Node child = new Node("", "");
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("fallback", "parent", true);

        assertEquals("fallbackParent", finder.findFirstNotEmpty(child));
    }

    @Test
    void includeFromObjectFalseWithNoParentReturnsNull() {
        Node child = new Node("childValue", "fallback");

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", false);

        assertNull(finder.findFirstNotEmpty(child));
    }

    @Test
    void findFirstEmptyCanFindParentEmptyWhenStartExcluded() {
        Node parent = new Node("", "fallback");
        Node child = new Node("childValue", "fallback");
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", false);

        assertEquals("", finder.findFirstEmpty(child));
    }
}
