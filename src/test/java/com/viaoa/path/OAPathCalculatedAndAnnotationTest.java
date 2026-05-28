package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAPathCalculatedAndAnnotationTest {

    public static class Root extends OAObject {
        private Child child;
        private String name;

        public Root() {
        }

        public Root(String name) {
            this.name = name;
        }

        @OAProperty(maxLength = 25)
        public String getName() {
            return name;
        }

        @OAOne
        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }

        @OACalculatedProperty
        public String getDisplayName() {
            return name == null ? "" : "Display-" + name;
        }

        @OACalculatedProperty
        public Child getCalculatedChild() {
            return child;
        }
    }

    public static class Child extends OAObject {
        private String name;

        public Child() {
        }

        public Child(String name) {
            this.name = name;
        }

        @OAProperty(maxLength = 20)
        public String getName() {
            return name;
        }
    }

    @Test
    void propertyAnnotationIsAvailableForTerminalProperty() {
        OAPath<Root> pp = new OAPath<>(Root.class, "name");

        assertNotNull(pp.getOAPropertyAnnotation());
        assertEquals(25, pp.getOAPropertyAnnotation().maxLength());
        assertNull(pp.getOACalculatedPropertyAnnotation());
        assertNull(pp.getOAOneAnnotation());
    }

    @Test
    void oneAnnotationIsAvailableForTerminalOneLink() {
        OAPath<Root> pp = new OAPath<>(Root.class, "child");

        assertNotNull(pp.getOAOneAnnotation());
        assertNull(pp.getOAPropertyAnnotation());
        assertNull(pp.getOACalculatedPropertyAnnotation());
    }

    @Test
    void calculatedPropertyAnnotationIsAvailableForTerminalCalcProperty() {
        OAPath<Root> pp = new OAPath<>(Root.class, "displayName");

        assertNotNull(pp.getOACalculatedPropertyAnnotation());
        assertNull(pp.getOAOneAnnotation());
    }

    @Test
    void calculatedScalarPropertyCanBeRead() {
        Root root = new Root("Bob");

        OAPath<Root> pp = new OAPath<>(Root.class, "displayName");

        assertEquals("Display-Bob", pp.getValue(root));
        assertEquals("Display-Bob", pp.getValueAsString(root));
    }

    @Test
    void calculatedObjectPropertyCanContinueTraversal() {
        Root root = new Root("Bob");
        root.setChild(new Child("Kid"));

        OAPath<Root> pp = new OAPath<>(Root.class, "calculatedChild.name");

        assertEquals("Kid", pp.getValue(root));
    }

    @Test
    void calculatedPropertyExceptionPropagatesCurrentContract() {
        class BadRoot extends OAObject {
            @OACalculatedProperty
            public String getBadValue() {
                throw new IllegalStateException("boom");
            }
        }

        OAPath<BadRoot> pp = new OAPath<>(BadRoot.class, "badValue");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> pp.getValue(new BadRoot()));
        assertTrue(ex.getMessage().contains("error invoking method"));
    }
}
