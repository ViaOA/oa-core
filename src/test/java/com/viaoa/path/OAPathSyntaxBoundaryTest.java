package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathSyntaxBoundaryTest {

    public static class Root extends OAObject {
        private Child child;
        private final Hub<Child> children = new Hub<>(Child.class);
        private String name;

        public Root() {
        }

        public Root(String name) {
            this.name = name;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }

        public Hub<Child> getChildren() {
            return children;
        }

        public String getName() {
            return name;
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
    void leadingDotDoesNotResolveHiddenSegmentCurrentContract() {
        assertThrows(IllegalArgumentException.class, () -> new OAPath<>(Root.class, ".child.name"));
    }

    @Test
    void repeatedDotDoesNotResolveWrongPropertyCurrentContract() {
        assertThrows(IllegalArgumentException.class, () -> new OAPath<>(Root.class, "child..name"));
    }

    @Test
    void trailingDotDoesNotResolveWrongPropertyCurrentContract() {
        assertThrows(IllegalArgumentException.class, () -> new OAPath<>(Root.class, "child."));
    }

    @Test
    void malformedCastSyntaxFailsVisibly() {
        assertThrows(IllegalArgumentException.class, () -> new OAPath<>(Root.class, "(Child.child.name"));
        assertThrows(IllegalArgumentException.class, () -> new OAPath<>(Root.class, "Child)child.name"));
    }

    @Test
    void malformedFilterSyntaxDoesNotHangParser() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertThrows(IllegalArgumentException.class, () -> new OAPath<>(Root.class, "children:Recent(\"abc.name"));
        });
    }

    @Test
    void quotedTextWithDotsAndColonsInsideFilterDoesNotSplitSegments() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            OAPath<Root> pp = new OAPath<>(Root.class, "children:Named(\"a.b:c\").name", true);

            assertArrayEquals(new String[] { "children", "name" }, pp.getProperties());
            assertEquals("Named", pp.getFilterNames()[0]);
            assertEquals("(\"a.b:c\")", pp.getFilterParams()[0]);
        });
    }

    @Test
    void leadingRootQualifierWithFullyQualifiedClassNameWorks() {
        String fqcn = Root.class.getName();
        OAPath<Root> pp = new OAPath<>(Root.class, "[" + fqcn + "].child.name");

        assertEquals(Root.class, pp.getFromClass());
        assertArrayEquals(new String[] { "child", "name" }, pp.getProperties());
    }

    @Test
    void toStringEmptySegmentPathUsesRootToStringCurrentContract() {
        Root root = new Root("root");
        OAPath<Root> pp = new OAPath<>(Root.class, "");

        assertSame(root, pp.getValue(root));
    }
}
