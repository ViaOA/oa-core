package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathParseAndMetadataTest {

    public static class Root extends OAObject {
        private String name;
        private Child child;
        private final Hub<Child> children = new Hub<>(Child.class);

        public Root() {
        }

        public Root(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
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
    }

    public static class Child extends OAObject {
        private String name;
        private Integer score;
        private GrandChild grandChild;

        public Child() {
        }

        public Child(String name, Integer score) {
            this.name = name;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public Integer getScore() {
            return score;
        }

        public GrandChild getGrandChild() {
            return grandChild;
        }

        public void setGrandChild(GrandChild grandChild) {
            this.grandChild = grandChild;
        }
    }

    public static class GrandChild extends OAObject {
        private String name;

        public GrandChild() {
        }

        public GrandChild(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    void constructorStoresRawPropertyPathBeforeSetup() {
        OAPath<Root> pp = new OAPath<>(" child.name ");

        assertEquals(" child.name ", pp.getPropertyPath());
        assertNull(pp.getFromClass());
        assertEquals(0, pp.getProperties().length);
    }

    @Test
    void setupParsesSimpleScalarPathSegmentsInOrder() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        assertArrayEquals(new String[] { "child", "name" }, pp.getProperties());
        assertEquals("child", pp.getFirstPropertyName());
        assertEquals("name", pp.getLastPropertyName());
        assertEquals(Root.class, pp.getFromClass());
        assertEquals(2, pp.getMethods().length);
        assertEquals(2, pp.getClasses().length);
        assertEquals(Child.class, pp.getClasses()[0]);
        assertEquals(String.class, pp.getClasses()[1]);
    }

    @Test
    void setupParsesNestedObjectPathSegmentsInOrder() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.grandChild.name");

        assertArrayEquals(new String[] { "child", "grandChild", "name" }, pp.getProperties());
        assertEquals("child", pp.getFirstPropertyName());
        assertEquals("name", pp.getLastPropertyName());
        assertEquals(3, pp.getMethods().length);
        assertEquals(GrandChild.class, pp.getClasses()[1]);
        assertEquals(String.class, pp.getClasses()[2]);
    }

    @Test
    void hubLinkPathReportsLinkAndHubSemantics() {
        OAPath<Root> pp = new OAPath<>(Root.class, "children");

        assertTrue(pp.hasLinks());
        assertTrue(pp.getHasHubProperty());
        assertNotNull(pp.getEndLinkInfo());
        assertNull(pp.getEndPropertyInfo());
        assertNull(pp.getEndCalcInfo());
        assertEquals("children", pp.getPathLinksOnly());
    }

    @Test
    void scalarTerminalPathHasNoEndLinkInfo() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        assertTrue(pp.hasLinks());
        assertNotNull(pp.getEndPropertyInfo());
        assertNull(pp.getEndLinkInfo());
        assertEquals("child", pp.getPathLinksOnly());
        assertFalse(pp.getDoesLastMethodHasHubParam());
    }

    @Test
    void emptyPathRepresentsRootObjectCurrentContract() {
        OAPath<Root> pp = new OAPath<>(Root.class, "");

        assertEquals("", pp.getPropertyPath());
        assertEquals(0, pp.getProperties().length);
        assertNull(pp.getFirstPropertyName());
        assertNull(pp.getLastPropertyName());
        assertFalse(pp.hasLinks());

        Root root = new Root("root");
        assertSame(root, pp.getValue(root));
    }

    @Test
    void blankPathRepresentsRootObjectAfterTrimCurrentContract() {
        OAPath<Root> pp = new OAPath<>(Root.class, "   ");

        Root root = new Root("root");

        assertSame(root, pp.getValue(root));
        assertEquals(0, pp.getProperties().length);
    }

    @Test
    void invalidStrictPathThrowsAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new OAPath<>(Root.class, "missing"));
        assertThrows(IllegalArgumentException.class, () -> new OAPath<>(Root.class, "child.missing"));
    }

    @Test
    void invalidLenientPathDoesNotThrowButRecordsNeedsDataOrPartialStateCurrentContract() {
        OAPath<Root> pp = new OAPath<>(Root.class, "missing", true);

        assertEquals("missing", pp.getPropertyPath());
        assertNotNull(pp.getProperties());
    }

    @Test
    void repeatedSetupShouldNotDuplicateCompiledMetadata() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        int propCount = pp.getProperties().length;
        int methodCount = pp.getMethods().length;
        int classCount = pp.getClasses().length;
        int linkCount = pp.getLinkInfos().length;

        pp.setup(Root.class);

        assertEquals(propCount, pp.getProperties().length);
        assertEquals(methodCount, pp.getMethods().length);
        assertEquals(classCount, pp.getClasses().length);
        assertEquals(linkCount, pp.getLinkInfos().length);
        assertArrayEquals(new String[] { "child", "name" }, pp.getProperties());
    }
}
