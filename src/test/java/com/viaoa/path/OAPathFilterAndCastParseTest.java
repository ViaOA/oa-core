package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathFilterAndCastParseTest {

    public static class Root extends OAObject {
        private final Hub<Child> children = new Hub<>(Child.class);
        private Child child;

        public Hub<Child> getChildren() {
            return children;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }
    }

    public static class Child extends OAObject {
        private String name;

        public String getName() {
            return name;
        }
    }

    public static class SpecialChild extends Child {
        private String special;

        public String getSpecial() {
            return special;
        }
    }

    @Test
    void filterSyntaxIsParsedOnIntendedSegment() {
        OAPath<Root> pp = new OAPath<>(Root.class, "children:Recent.name", true);

        assertArrayEquals(new String[] { "children", "name" }, pp.getProperties());
        assertEquals("Recent", pp.getFilterNames()[0]);
        assertNull(pp.getFilterNames()[1]);
        assertNull(pp.getFilterParams()[0]);
    }

    @Test
    void filterParamsAreStoredOnceWithoutDoubleWrappingInLinksOnlyPath() {
        OAPath<Root> pp = new OAPath<>(Root.class, "children:Recent(7).name", true);

        assertEquals("(7)", pp.getFilterParams()[0]);
        assertEquals("children:Recent(7)", pp.getPathLinksOnly());
        assertFalse(pp.getPathLinksOnly().contains("((7))"));
    }

    @Test
    void filterQuestionMarkParameterIsPreservedForRuntimeSubstitution() {
        OAPath<Root> pp = new OAPath<>(Root.class, "children:Recent(?).name", true);

        assertEquals("(?)", pp.getFilterParams()[0]);
        if (pp.getFilterParamValues()[0] != null && pp.getFilterParamValues()[0].length > 0) {
            assertEquals("?", pp.getFilterParamValues()[0][0]);
        }
    }

    @Test
    void quotedFilterParamDoesNotHangParserAndKeepsSegmentBoundaries() {
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
            OAPath<Root> pp = new OAPath<>(Root.class, "children:Named(\"a,b:c\").name", true);

            assertArrayEquals(new String[] { "children", "name" }, pp.getProperties());
            assertEquals("Named", pp.getFilterNames()[0]);
            assertEquals("(\"a,b:c\")", pp.getFilterParams()[0]);
        });
    }

    @Test
    void castSyntaxIsParsedOnSegment() {
        OAPath<Root> pp = new OAPath<>(Root.class, "(SpecialChild)child.special", true);

        assertArrayEquals(new String[] { "child", "special" }, pp.getProperties());
        assertEquals("SpecialChild", pp.getCastNames()[0]);
        assertNull(pp.getCastNames()[1]);
    }

    @Test
    void invalidCastClassThrowsInStrictMode() {
        assertThrows(IllegalArgumentException.class, () ->
            new OAPath<>(Root.class, "(MissingChild)child.name")
        );
    }

    @Test
    void leadingClassQualifierResolvesFromClassByPackage() {
        OAPath pp = new OAPath(Root.class, "[Root].child.name");

        assertEquals(Root.class, pp.getFromClass());
        assertArrayEquals(new String[] { "child", "name" }, pp.getProperties());
    }
}
