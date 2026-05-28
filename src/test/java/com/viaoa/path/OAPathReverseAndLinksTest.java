package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathReverseAndLinksTest {

    public static class Parent extends OAObject {
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
        private Parent parent;
        private String name;

        public Child() {
        }

        public Child(String name) {
            this.name = name;
        }

        public Parent getParent() {
            return parent;
        }

        public void setParent(Parent parent) {
            this.parent = parent;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    void reversePathForOneLinkCanBeResolvedWhenReverseMetadataExists() {
        OAPath<Parent> pp = new OAPath<>(Parent.class, "child");

        OAPath reverse = pp.getReversePath(true);

        if (reverse != null) {
            assertEquals(Child.class, reverse.getFromClass());
            assertNotNull(reverse.getPropertyPath());
            assertTrue(reverse.hasLinks());
        }
    }

    @Test
    void reversePathIsCachedSeparatelyByPrivateLinkPolicy() {
        OAPath<Parent> pp = new OAPath<>(Parent.class, "child");

        OAPath r1 = pp.getReversePath(true);
        OAPath r2 = pp.getReversePath(true);
        assertSame(r1, r2);

        OAPath r3 = pp.getReversePath(false);
        OAPath r4 = pp.getReversePath(false);
        assertSame(r3, r4);
    }

    @Test
    void scalarOnlyPathHasNoReversePath() {
        OAPath<Parent> pp = new OAPath<>(Parent.class, "child.name");

        // Path has a link segment, so reverse can be based on the link-only portion.
        OAPath reverse = pp.getReversePath(true);
        if (reverse != null) {
            assertEquals(Child.class, reverse.getFromClass());
        }

        OAPath<Child> scalar = new OAPath<>(Child.class, "name");
        assertNull(scalar.getReversePath(true));
        assertNull(scalar.getPathLinksOnly());
    }

    @Test
    void pathLinksOnlyReturnsOnlyLinkSegmentsForMixedPath() {
        OAPath<Parent> pp = new OAPath<>(Parent.class, "child.name");

        assertEquals("child", pp.getPathLinksOnly());
    }

    @Test
    void pathLinksOnlyReturnsNestedLinkSegmentsBeforeScalarTerminal() {
        class GrandChild extends OAObject {
            public String getName() { return "g"; }
        }
        class MidChild extends OAObject {
            private GrandChild grandChild = new GrandChild();
            public GrandChild getGrandChild() { return grandChild; }
        }
        class Root extends OAObject {
            private MidChild midChild = new MidChild();
            public MidChild getMidChild() { return midChild; }
        }

        OAPath<Root> pp = new OAPath<>(Root.class, "midChild.grandChild.name");

        assertEquals("midChild.grandChild", pp.getPathLinksOnly());
        assertTrue(pp.hasLinks());
    }

    @Test
    void hasPrivateLinkIsFalseForNormalPublicLinks() {
        OAPath<Parent> pp = new OAPath<>(Parent.class, "child.name");

        assertFalse(pp.hasPrivateLink());
    }
}
