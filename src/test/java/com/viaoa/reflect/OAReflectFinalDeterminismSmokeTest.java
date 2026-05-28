package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class OAReflectFinalDeterminismSmokeTest {

    public static class Bean {
        private Child child = new Child("child");

        public Child getChild() { return child; }
        public String echo(String value) { return value; }
        public String echo(Object value) { return "object"; }
    }

    public static class Child {
        private String name;
        public Child(String name) { this.name = name; }
        public String getName() { return name; }
    }

    @Test
    void sameMethodLookupReturnsSameSemanticMethodRepeatedly() {
        Method first = OAReflect.getMethod(Bean.class, "echo", new Object[] { "x" });

        assertNotNull(first);
        assertEquals(String.class, first.getParameterTypes()[0]);

        for (int i = 0; i < 100; i++) {
            Method next = OAReflect.getMethod(Bean.class, "echo", new Object[] { "x" });
            assertEquals(first, next);
            assertEquals(String.class, next.getParameterTypes()[0]);
        }
    }

    @Test
    void samePropertyLookupReturnsEquivalentMethodChainRepeatedly() {
        Method[] first = OAReflect.getMethods(Bean.class, "child.name");

        assertNotNull(first);
        assertEquals(2, first.length);

        for (int i = 0; i < 50; i++) {
            Method[] next = OAReflect.getMethods(Bean.class, "child.name");
            assertEquals(first.length, next.length);
            assertEquals(first[0], next[0]);
            assertEquals(first[1], next[1]);
            assertEquals("child", OAReflect.getPropertyValue(new Bean(), next));
        }
    }

    @Test
    void sameInvalidLookupFailsConsistently() {
        for (int i = 0; i < 50; i++) {
            assertNull(OAReflect.getMethod(Bean.class, "missing", new Object[] { "x" }));
            assertNull(OAReflect.getMethods(Bean.class, "child.missing", false));
        }
    }
}
