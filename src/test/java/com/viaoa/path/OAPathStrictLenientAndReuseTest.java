package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathStrictLenientAndReuseTest {

    public static class Root extends OAObject {
        private Child child;
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

    public static class OtherRoot extends OAObject {
        public String getName() {
            return "other";
        }
    }

    @Test
    void strictSetupInvalidPathThrowsRuntimeException() {
        OAPath<Root> pp = new OAPath<>("child.name");

        assertThrows(RuntimeException.class, () -> pp.setup(OtherRoot.class));
    }

    @Test
    void lenientConstructorDoesNotThrowForInvalidPath() {
        OAPath<Root> pp = new OAPath<>(Root.class, "missing.value", true);

        assertEquals("missing.value", pp.getPropertyPath());
        assertNotNull(pp.getProperties());
    }

    @Test
    void setupWithNullClassReportsNeedsDataToVerify() {
        OAPath<Root> pp = new OAPath<>("child.name");

        String error = pp.setup(null, null, false);

        assertNotNull(error);
        assertTrue(pp.getNeedsDataToVerify());
    }

    @Test
    void repeatedSetupWithSameClassShouldNotDuplicateArrays() {
        OAPath<Root> pp = new OAPath<>("child.name");

        pp.setup(Root.class);

        int properties = pp.getProperties().length;
        int methods = pp.getMethods().length;
        int classes = pp.getClasses().length;
        int links = pp.getLinkInfos().length;

        pp.setup(Root.class);

        assertEquals(properties, pp.getProperties().length);
        assertEquals(methods, pp.getMethods().length);
        assertEquals(classes, pp.getClasses().length);
        assertEquals(links, pp.getLinkInfos().length);
        assertArrayEquals(new String[] { "child", "name" }, pp.getProperties());
    }

    @Test
    void failedSetupShouldNotLeavePathUsableWithWrongMetadata() {
        OAPath<Root> pp = new OAPath<>("missing.value");

        assertThrows(RuntimeException.class, () -> pp.setup(Root.class));
        assertThrows(RuntimeException.class, () -> pp.setup(Root.class));
    }

    @Test
    void setupAfterConstructorWithoutClassUsesRuntimeRootClassOnGetValue() {
        Root root = new Root("root");
        root.setChild(new Child("child"));

        OAPath<Root> pp = new OAPath<>("child.name");

        assertEquals("child", pp.getValue(root));
        assertEquals(Root.class, pp.getFromClass());
        assertArrayEquals(new String[] { "child", "name" }, pp.getProperties());
    }

    @Test
    void incompatibleRootAfterSetupDoesNotSilentlyReturnSimilarlyNamedValue() {
        Root root = new Root("root");
        root.setChild(new Child("child"));
        OtherRoot other = new OtherRoot();

        OAPath pp = new OAPath(Root.class, "child.name");

        assertEquals("child", pp.getValue(root));

        assertThrows(RuntimeException.class, () -> pp.getValue(other));
    }
}
