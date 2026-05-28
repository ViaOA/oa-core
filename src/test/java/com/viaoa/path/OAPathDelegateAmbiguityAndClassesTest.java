package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathDelegateAmbiguityAndClassesTest {

    public static class Root extends OAObject {
        private Child child;
        private Other other;

        public Child getChild() {
            return child;
        }

        public Other getOther() {
            return other;
        }
    }

    public static class AmbiguousRoot extends OAObject {
        private Child child;
        private Child otherChild;

        public Child getChild() {
            return child;
        }

        public Child getOtherChild() {
            return otherChild;
        }
    }

    public static class Child extends OAObject {
        private GrandChild grandChild;

        public GrandChild getGrandChild() {
            return grandChild;
        }
    }

    public static class Other extends OAObject {
    }

    public static class GrandChild extends OAObject {
    }

    @Test
    void getPropertyPathForMultipleClassesBuildsDotPath() {
        Hub<Root> hub = new Hub<>(Root.class);

        String path = OAPathDelegate.getPropertyPathforClasses(hub, new Class[] { Child.class, GrandChild.class });

        assertEquals("child.grandChild", path);
    }

    @Test
    void getPropertyPathForClassesReturnsNullWhenMiddleSegmentMissing() {
        Hub<Root> hub = new Hub<>(Root.class);

        assertNull(OAPathDelegate.getPropertyPathforClasses(hub, new Class[] { Other.class, GrandChild.class }));
    }

    @Test
    void getPropertyPathForClassesThrowsWhenMultipleLinksMatchSameTargetClass() {
        Hub<AmbiguousRoot> hub = new Hub<>(AmbiguousRoot.class);

        assertThrows(RuntimeException.class, () ->
            OAPathDelegate.getPropertyPathforClasses(hub, new Class[] { Child.class })
        );
    }

    @Test
    void createRootPropertyPathRejectsUnknownRootClass() {
        assertThrows(Exception.class, () ->
            OAPathDelegate.createRootPropertyPath("[MissingRoot].child", Root.class)
        );
    }
}
