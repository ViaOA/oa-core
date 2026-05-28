package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAReflectPropertyPathTest {

    public static class Root extends OAObject {
        private Child child;
        private final Hub<Child> children = new Hub<>(Child.class);
        private boolean active;
        private String name;

        public Root() {
        }

        public Root(String name, boolean active) {
            this.name = name;
            this.active = active;
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

        public boolean isActive() {
            return active;
        }

        public String getName() {
            return name;
        }
    }

    public static class Child extends OAObject {
        private String name;
        private GrandChild grandChild;

        public Child() {
        }

        public Child(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
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

    private static Root root() {
        Root root = new Root("root", true);
        Child child = new Child("child");
        child.setGrandChild(new GrandChild("grand"));
        root.setChild(child);

        root.getChildren().add(new Child("A"));
        root.getChildren().add(new Child("B"));
        root.getChildren().setAO(root.getChildren().getAt(1));

        return root;
    }

    @Test
    void getMethodsResolvesSimpleGetterPath() {
        Method[] ms = OAReflect.getMethods(Root.class, "child.name");

        assertNotNull(ms);
        assertEquals(2, ms.length);
        assertEquals("getChild", ms[0].getName());
        assertEquals("getName", ms[1].getName());
    }

    @Test
    void getMethodsResolvesNestedGetterPath() {
        Method[] ms = OAReflect.getMethods(Root.class, "child.grandChild.name");

        assertNotNull(ms);
        assertEquals(3, ms.length);
        assertEquals("getChild", ms[0].getName());
        assertEquals("getGrandChild", ms[1].getName());
        assertEquals("getName", ms[2].getName());
    }

    @Test
    void getMethodsShouldResolveBooleanIsGetter() {
        Method[] ms = OAReflect.getMethods(Root.class, "active");

        assertNotNull(ms);
        assertEquals(1, ms.length);
        assertEquals("isActive", ms[0].getName());
    }

    @Test
    void getMethodsThroughHubInsertsActiveObjectMethod() {
        Method[] ms = OAReflect.getMethods(Root.class, "children.name");

        assertNotNull(ms);
        assertEquals(3, ms.length);
        assertEquals("getChildren", ms[0].getName());
        assertEquals("getActiveObject", ms[1].getName());
        assertEquals("getName", ms[2].getName());
    }

    @Test
    void getMethodsLenientMissingPropertyReturnsNull() {
        assertNull(OAReflect.getMethods(Root.class, "missing", false));
        assertNull(OAReflect.getMethods(Root.class, "child.missing", false));
    }

    @Test
    void getMethodsStrictMissingPropertyThrows() {
        assertThrows(RuntimeException.class, () -> OAReflect.getMethods(Root.class, "missing", true));
        assertThrows(RuntimeException.class, () -> OAReflect.getMethods(Root.class, "child.missing", true));
    }

    @Test
    void malformedPathSegmentsShouldNotResolveToToString() {
        assertThrows(RuntimeException.class, () -> OAReflect.getMethods(Root.class, "child.", true));
        assertThrows(RuntimeException.class, () -> OAReflect.getMethods(Root.class, "child..name", true));
    }

    @Test
    void getPropertyValueExecutesResolvedMethodChain() {
        Root root = root();
        Method[] ms = OAReflect.getMethods(Root.class, "child.grandChild.name");

        assertEquals("grand", OAReflect.getPropertyValue(root, ms));
    }

    @Test
    void getPropertyValueStopsOnNullIntermediate() {
        Root root = new Root("root", true);
        Method[] ms = OAReflect.getMethods(Root.class, "child.name");

        assertNull(OAReflect.getPropertyValue(root, ms));
    }

    @Test
    void getPropertyValueThroughHubUsesActiveObject() {
        Root root = root();
        Method[] ms = OAReflect.getMethods(Root.class, "children.name");

        assertEquals("B", OAReflect.getPropertyValue(root, ms));

        root.getChildren().setAO(root.getChildren().getAt(0));
        assertEquals("A", OAReflect.getPropertyValue(root, ms));
    }

    @Test
    void executeMethodByPathUsesGetMethods() {
        Root root = root();

        assertEquals("grand", OAReflect.executeMethod(root, "child.grandChild.name"));
    }

    @Test
    void executeMethodRejectsNullOrEmptyPath() {
        Root root = root();

        assertNull(OAReflect.executeMethod(null, "name"));
        assertNull(OAReflect.executeMethod(root, null));
        assertNull(OAReflect.executeMethod(root, ""));
    }
}
