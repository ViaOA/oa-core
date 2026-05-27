package com.viaoa.compare;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class OAObjectCompareTest {

    @Test
    void comparesSimpleObjectsAndReportsChangedField() throws Exception {
        CapturingObjectCompare comp = new CapturingObjectCompare();

        assertTrue(comp.compare(new Sample("a", 1), new Sample("a", 1)));
        assertTrue(comp.mismatches.isEmpty());

        comp = new CapturingObjectCompare();
        assertFalse(comp.compare(new Sample("a", 1), new Sample("a", 2)));
        assertTrue(comp.mismatches.stream().anyMatch(s -> s.contains(".count")));
    }

    @Test
    void comparesArraysUsingCurrentUnorderedSemantics() throws Exception {
        OAObjectCompare comp = new OAObjectCompare();

        assertTrue(comp.compare(new String[] { "a", "b" }, new String[] { "b", "a" }));
        assertFalse(comp.compare(new String[] { "a", "b" }, new String[] { "a", "c" }));
    }

    @Test
    void detectsArrayLengthMismatch() throws Exception {
        CapturingObjectCompare comp = new CapturingObjectCompare();

        assertFalse(comp.compare(new String[] { "a" }, new String[] { "a", "b" }));
        assertTrue(comp.mismatches.stream().anyMatch(s -> s.contains("length=1") && s.contains("length=2")));
    }

    @Test
    void handlesCyclesWithoutInfiniteRecursion() throws Exception {
        Node left = new Node("root");
        left.next = left;

        Node right = new Node("root");
        right.next = right;

        assertTrue(new OAObjectCompare().compare(left, right));

        right.name = "changed";
        assertFalse(new OAObjectCompare().compare(left, right));
    }

    static class CapturingObjectCompare extends OAObjectCompare {
        final List<String> mismatches = new ArrayList<>();

        @Override
        public void foundOne(String propertyPath, Object objLeft, Object objRight) {
            mismatches.add(propertyPath + ":" + objLeft + ":" + objRight);
        }
    }

    static class Sample {
        String name;
        int count;

        Sample(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    static class Node {
        String name;
        Node next;

        Node(String name) {
            this.name = name;
        }
    }
}
