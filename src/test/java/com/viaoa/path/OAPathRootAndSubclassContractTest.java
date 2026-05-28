package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathRootAndSubclassContractTest {

    public static class Root extends OAObject {
        private Child child = new Child("base");
        private String name = "root";

        public Child getChild() {
            return child;
        }

        public String getName() {
            return name;
        }
    }

    public static class SubRoot extends Root {
        private String subName = "sub";

        public String getSubName() {
            return subName;
        }
    }

    public static class IncompatibleRoot extends OAObject {
        private String name = "wrong";

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
    void compiledPathForBaseClassWorksWithSubclassRoot() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child.name");

        assertEquals("base", pp.getValue(new SubRoot()));
    }

    @Test
    void compiledPathCanBeBuiltForSubclassProperty() {
        OAPath<SubRoot> pp = new OAPath<>(SubRoot.class, "subName");

        assertEquals("sub", pp.getValue(new SubRoot()));
    }

    @Test
    void baseCompiledPathRejectsIncompatibleRootEvenWithSamePropertyName() {
        OAPath pp = new OAPath(Root.class, "name");

        assertEquals("root", pp.getValue(new Root()));

        assertThrows(RuntimeException.class, () -> pp.getValue(new IncompatibleRoot()));
    }

    @Test
    void noClassConstructorBindsToFirstRuntimeRootClass() {
        OAPath<Root> pp = new OAPath<>("child.name");

        assertNull(pp.getFromClass());

        assertEquals("base", pp.getValue(new Root()));
        assertEquals(Root.class, pp.getFromClass());

        assertEquals("base", pp.getValue(new SubRoot()));
    }

    @Test
    void noClassConstructorAfterRuntimeBindingRejectsIncompatibleRoot() {
        OAPath pp = new OAPath("child.name");

        assertEquals("base", pp.getValue(new Root()));

        assertThrows(RuntimeException.class, () -> pp.getValue(new IncompatibleRoot()));
    }
}
