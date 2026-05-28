package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class OAFilterInAndFindNullTest {

    static class Node {
        String name;
        Node child;
        Node[] children;
    }

    static class RecordingFindNull extends OAFindNull {
        final List<String> paths = new ArrayList<>();

        @Override
        protected void foundOne(String propertyPath) {
            paths.add(propertyPath);
        }
    }

    @Test
    void findNullReportsNullRoot() throws Exception {
        RecordingFindNull fn = new RecordingFindNull();

        assertTrue(fn.findNull(null));
        assertEquals(List.of(""), fn.paths);
    }

    @Test
    void findNullReportsFirstNullFieldPath() throws Exception {
        Node root = new Node();
        root.name = "root";
        root.child = null;

        RecordingFindNull fn = new RecordingFindNull();

        assertTrue(fn.findNull(root));
        assertTrue(fn.paths.stream().anyMatch(s -> s.endsWith(".child")));
    }

    @Test
    void findNullTraversesArraysAndReportsIndexedPath() throws Exception {
        Node root = new Node();
        root.name = "root";
        root.child = new Node();
        root.child.name = "child";
        root.child.child = root;
        root.children = new Node[] { root.child, null };

        RecordingFindNull fn = new RecordingFindNull();

        assertTrue(fn.findNull(root));
        assertTrue(fn.paths.stream().anyMatch(s -> s.contains("children[1]")));
    }

    @Test
    void findNullSkipsCircularReferences() throws Exception {
        Node root = new Node();
        root.name = "root";
        root.child = root;
        root.children = new Node[0];

        RecordingFindNull fn = new RecordingFindNull();

        assertFalse(fn.findNull(root));
        assertTrue(fn.paths.isEmpty());
    }
}
