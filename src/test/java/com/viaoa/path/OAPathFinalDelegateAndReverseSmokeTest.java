package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathFinalDelegateAndReverseSmokeTest {

    public static class Root extends OAObject {
        private Child child;
        private final Hub<Child> children = new Hub<>(Child.class);

        public Child getChild() {
            return child;
        }

        public Hub<Child> getChildren() {
            return children;
        }
    }

    public static class Child extends OAObject {
        private Root root;
        private String name;

        public Root getRoot() {
            return root;
        }

        public String getName() {
            return name;
        }
    }

    public static class Other extends OAObject {
    }

    @Test
    void delegatePathForNoClassesReturnsNullCurrentContract() {
        Hub<Root> hub = new Hub<>(Root.class);

        assertNull(OAPathDelegate.getPropertyPathforClasses(hub, new Class[0]));
    }

    @Test
    void delegatePathForUnreachableClassReturnsNull() {
        Hub<Root> hub = new Hub<>(Root.class);

        assertNull(OAPathDelegate.getPropertyPathforClasses(hub, new Class[] { Other.class }));
    }

    @Test
    void reversePathForPathWithoutResolvableReverseReturnsNullOrCompatiblePath() {
        OAPath<Root> pp = new OAPath<>(Root.class, "children");

        OAPath rev = pp.getReversePath(true);

        if (rev != null) {
            assertEquals(Child.class, rev.getFromClass());
            assertNotNull(rev.getPropertyPath());
        }
    }

    @Test
    void reversePathCacheDoesNotChangeLinkMetadata() {
        OAPath<Root> pp = new OAPath<>(Root.class, "children");

        int links = pp.getLinkInfos().length;
        String linkOnly = pp.getPathLinksOnly();

        pp.getReversePath(true);
        pp.getReversePath(false);

        assertEquals(links, pp.getLinkInfos().length);
        assertEquals(linkOnly, pp.getPathLinksOnly());
    }

    @Test
    void createRootPropertyPathKeepsOriginalPathText() throws Exception {
        OAPath pp = OAPathDelegate.createRootPropertyPath("[Root].child.name", Root.class);

        assertEquals("[Root].child.name", pp.getPropertyPath());
        assertEquals(Root.class, pp.getFromClass());
        assertArrayEquals(new String[] { "child", "name" }, pp.getProperties());
    }
}
