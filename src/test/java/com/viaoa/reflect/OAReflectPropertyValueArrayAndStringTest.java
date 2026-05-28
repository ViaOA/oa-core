package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class OAReflectPropertyValueArrayAndStringTest {

    public static class Root {
        private Child child = new Child("child");

        public Child getChild() {
            return child;
        }

        public Child getNullChild() {
            return null;
        }
    }

    public static class Child {
        private String name;

        public Child(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Integer getScore() {
            return 42;
        }
    }

    @Test
    void getPropertyValueWithAmountLimitsInvocationDepth() {
        Root root = new Root();
        Method[] ms = OAReflect.getMethods(Root.class, "child.name");

        assertSame(root, OAReflect.getPropertyValue(root, ms, 0));
        assertInstanceOf(Child.class, OAReflect.getPropertyValue(root, ms, 1));
        assertEquals("child", OAReflect.getPropertyValue(root, ms, 2));
        assertEquals("child", OAReflect.getPropertyValue(root, ms, 99));
    }

    @Test
    void getPropertyValueAsStringForMethodArrayReturnsNullOnNullIntermediate() {
        Root root = new Root();
        Method[] ms = OAReflect.getMethods(Root.class, "nullChild.name");

        assertNull(OAReflect.getPropertyValueAsString(root, ms));
    }

    @Test
    void getPropertyValueAsStringForMethodArrayUsesFormatOnTerminalValue() {
        Root root = new Root();
        Method[] ms = OAReflect.getMethods(Root.class, "child.score");

        assertEquals("0042", OAReflect.getPropertyValueAsString(root, ms, "0000"));
    }

    @Test
    void getPropertyValueAsStringNullMethodReturnsNullValueForObjectToStringFallback() {
        Root root = new Root();

        assertEquals("NULL", OAReflect.getPropertyValueAsString(root, null, null, "NULL"));
    }

    @Test
    void executeMethodStopsAtNullIntermediate() {
        Root root = new Root();
        Method[] ms = OAReflect.getMethods(Root.class, "nullChild.name");

        assertNull(OAReflect.executeMethod(root, ms));
    }

    @Test
    void executeMethodThrowsWhenPathCannotResolve() {
        Root root = new Root();

        assertThrows(RuntimeException.class, () -> OAReflect.executeMethod(root, "missing.name"));
    }
}
