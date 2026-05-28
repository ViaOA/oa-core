package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderCycleAndDepthTest {

    public static class Node extends OAObject {
        private String name;
        private Node next;
        private final Hub<Node> children = new Hub<>(Node.class);

        public Node() {
        }

        public Node(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Node getNext() {
            return next;
        }

        public void setNext(Node next) {
            this.next = next;
        }

        public Hub<Node> getChildren() {
            return children;
        }
    }

    @Test
    void bidirectionalLikeCycleDoesNotLoopForever() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.setNext(b);
        b.setNext(a);

        OAFinder<Node, Node> finder = new OAFinder<>("next.next.next.next.next");

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            List<Node> result = finder.find(a);
            assertNotNull(result);
        });
    }

    @Test
    void hubCycleDoesNotLoopForeverWhenPathHasFiniteSegments() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.getChildren().add(b);
        b.getChildren().add(a);

        OAFinder<Node, Node> finder = new OAFinder<>("children.children.children");

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            List<Node> result = finder.find(a);
            assertNotNull(result);
        });
    }

    @Test
    void extremelyDeepPathFailsOrStopsWithoutHanging() {
        Node root = new Node("root");

        StringBuilder path = new StringBuilder("next");
        for (int i = 0; i < 150; i++) {
            path.append(".next");
        }

        OAFinder<Node, Node> finder = new OAFinder<>(path.toString());

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            try {
                List<Node> result = finder.find(root);
                assertNotNull(result);
            } catch (RuntimeException expectedForInvalidOrTooDeepPath) {
                assertNotNull(expectedForInvalidOrTooDeepPath);
            }
        });
    }
}
