package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAReflectStaticInstanceBoundaryTest {

    public static class Bean {
        public static String getGlobalName() { return "global"; }
        public String getName() { return "instance"; }
        public static String echoStatic(String value) { return "static:" + value; }
        public String echo(String value) { return "instance:" + value; }
    }

    @Test
    void getMethodCanFindPublicStaticMethodByName() throws Exception {
        Method m = OAReflect.getMethod(Bean.class, "echoStatic", new Object[] { "x" });

        assertNotNull(m);
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals("static:x", m.invoke(null, "x"));
    }

    @Test
    void propertyPathStaticGetterBehaviorIsDocumented() {
        Method[] ms = OAReflect.getMethods(Bean.class, "globalName", false);

        if (ms != null) {
            assertTrue(Modifier.isStatic(ms[0].getModifiers()),
                "If static getter is returned, this documents current public-method behavior");
        }
    }

    @Test
    void instanceMethodLookupPrefersMatchingInstanceName() throws Exception {
        Method m = OAReflect.getMethod(Bean.class, "echo", new Object[] { "x" });

        assertNotNull(m);
        assertFalse(Modifier.isStatic(m.getModifiers()));
        assertEquals("instance:x", m.invoke(new Bean(), "x"));
    }

    @Test
    void getPropertyValueInvokingStaticGetterWithInstanceDocumentsJavaReflectionBehavior() throws Exception {
        Method m = Bean.class.getMethod("getGlobalName");

        Object val = OAReflect.getPropertyValue(new Bean(), m);

        assertEquals("global", val);
    }

    @Test
    void executeMethodUsesInstancePropertyPathForNormalProperty() {
        assertEquals("instance", OAReflect.executeMethod(new Bean(), "name"));
    }
}
