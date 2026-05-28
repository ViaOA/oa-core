package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathDelegateTest {

    public static class Root extends OAObject {
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
    void createRootPropertyPathDelegatesToOAPathConstructor() throws Exception {
        OAPath pp = OAPathDelegate.createRootPropertyPath("[Root].child.name", Root.class);

        assertNotNull(pp);
        assertEquals(Root.class, pp.getFromClass());
        assertArrayEquals(new String[] { "child", "name" }, pp.getProperties());
    }

    @Test
    void getPropertyPathForClassesReturnsNullForNullInputs() {
        assertNull(OAPathDelegate.getPropertyPathforClasses(null, new Class[] { Child.class }));
        assertNull(OAPathDelegate.getPropertyPathforClasses(new Hub<>(Root.class), null));
    }

    @Test
    void getPropertyPathForClassesFindsSingleMatchingLink() {
        Hub<Root> hub = new Hub<>(Root.class);

        String path = OAPathDelegate.getPropertyPathforClasses(hub, new Class[] { Child.class });

        assertNotNull(path);
        assertTrue(path.equals("child") || path.equals("children"));
    }

    @Test
    void getPropertyPathForClassesReturnsNullWhenSegmentCannotBeResolved() {
        Hub<Child> hub = new Hub<>(Child.class);

        assertNull(OAPathDelegate.getPropertyPathforClasses(hub, new Class[] { Root.class }));
    }
}
