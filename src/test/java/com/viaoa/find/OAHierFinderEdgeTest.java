package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import com.viaoa.filter.OAFilter;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAHierFinderEdgeTest {

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
    void findFirstReturnsNullWhenNoValueMatches() {
        Node parent = new Node("", false);
        Node child = new Node("", false);
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", true);

        assertNull(finder.findFirst(child, obj -> "missing".equals(obj)));
    }

    @Test
    void repeatedFindFirstDoesNotReusePriorFoundValue() {
        Node parent = new Node("parent", true);
        Node child = new Node("", false);
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", true);

        assertEquals("parent", finder.findFirstNotEmpty(child));

        parent = new Node("", true);
        child = new Node("", false);
        child.setParent(parent);

        assertNull(finder.findFirstNotEmpty(child));
    }

    @Test
    void findFirstTrueSkipsFalseAndFindsParentTrue() {
        Node parent = new Node("parent", true);
        Node child = new Node("child", false);
        child.setParent(parent);

        OAHierFinder<Node> finder = new OAHierFinder<>("enabled", "parent", true);

        assertEquals(Boolean.TRUE, finder.findFirstTrue(child));
    }

    @Test
    void customFilterCanAcceptNullValueWhenIncluded() {
        Node child = new Node(null, false);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", true);
        OAFilter acceptsNull = obj -> obj == null;

        assertNull(finder.findFirst(child, acceptsNull));
    }

    @Test
    void cyclicParentHierarchyDoesNotHangCurrentContract() {
        Node a = new Node("", false);
        Node b = new Node("", false);
        a.setParent(b);
        b.setParent(a);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "parent", true);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            Object val = finder.findFirstNotEmpty(a);
            assertNull(val);
        });
    }

    @Test
    void nonRecursiveOwnerPathCanReachOwnerValue() {
        Node owner = new Node("owner", true);
        Node child = new Node("", false);
        child.setOwner(owner);

        OAHierFinder<Node> finder = new OAHierFinder<>("value", "owner", false);

        assertEquals("owner", finder.findFirstNotEmpty(child));
    }
}
