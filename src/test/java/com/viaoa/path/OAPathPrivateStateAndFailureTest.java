package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathPrivateStateAndFailureTest {

    public static class Root extends OAObject {
        private String name = "root";
        private Child child = new Child("child");

        public String getName() {
            return name;
        }

        public Child getChild() {
            return child;
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
    void failedTraversalDoesNotChangeCompiledMetadataForValidPath() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        int props = pp.getProperties().length;
        int methods = pp.getMethods().length;
        int classes = pp.getClasses().length;
        int links = pp.getLinkInfos().length;

        assertThrows(RuntimeException.class, () -> pp.getValue(new Object()));

        assertEquals(props, pp.getProperties().length);
        assertEquals(methods, pp.getMethods().length);
        assertEquals(classes, pp.getClasses().length);
        assertEquals(links, pp.getLinkInfos().length);

        assertEquals("child", pp.getValue(new Root()));
    }

    @Test
    void lenientInvalidPathDoesNotReturnSimilarlyNamedRootValue() {
        Root root = new Root();
        OAPath<Root> pp = new OAPath<>(Root.class, "missingName", true);

        Object val = null;
        try {
            val = pp.getValue(root);
        } catch (RuntimeException expectedAllowedByLenientSetupCurrentContract) {
            return;
        }

        assertNotEquals("root", val);
    }

    @Test
    void wrongPathFailureDoesNotCachePriorSuccessfulValue() {
        Root root = new Root();
        OAPath<Root> good = new OAPath<>(Root.class, "child.name");

        assertEquals("child", good.getValue(root));

        OAPath<Root> bad = new OAPath<>(Root.class, "missing.name", true);

        try {
            assertNull(bad.getValue(root));
        } catch (RuntimeException expectedAllowedByCurrentContract) {
            assertEquals("child", good.getValue(root));
        }
    }

    @Test
    void getValueWithStartPositionRejectsInvalidPositionCurrentContract() {
        Root root = new Root();
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        assertThrows(RuntimeException.class, () -> pp.getValue(root, 99));
    }
}
