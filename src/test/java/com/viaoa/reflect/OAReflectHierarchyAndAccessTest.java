package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class OAReflectHierarchyAndAccessTest {

    interface LabelProvider {
        String getLabel();
    }

    public static class Base {
        public String getName() {
            return "base";
        }

        public String inheritedMethod() {
            return "inherited";
        }
    }

    public static class Sub extends Base implements LabelProvider {
        @Override
        public String getName() {
            return "sub";
        }

        @Override
        public String getLabel() {
            return "label";
        }

        public String subOnly() {
            return "subOnly";
        }

        protected String protectedMethod() {
            return "protected";
        }

        private String privateMethod() {
            return "private";
        }
    }

    @Test
    void inheritedPublicMethodIsResolved() throws Exception {
        Method m = OAReflect.getMethod(Sub.class, "inheritedMethod", 0);

        assertNotNull(m);
        assertEquals("inherited", m.invoke(new Sub()));
    }

    @Test
    void subclassOverrideIsResolvedAndInvoked() {
        Method m = OAReflect.getMethod(Sub.class, "getName", 0);

        assertNotNull(m);
        assertEquals("sub", OAReflect.getPropertyValue(new Sub(), m));
    }

    @Test
    void interfaceDeclaredPublicMethodIsResolvedFromImplementation() {
        Method m = OAReflect.getMethod(Sub.class, "getLabel", 0);

        assertNotNull(m);
        assertEquals("label", OAReflect.getPropertyValue(new Sub(), m));
    }

    @Test
    void nonPublicMethodsAreNotResolvedByPublicLookup() {
        assertNull(OAReflect.getMethod(Sub.class, "protectedMethod", 0));
        assertNull(OAReflect.getMethod(Sub.class, "privateMethod", 0));
    }

    @Test
    void getMethodsUsesSubclassOverrideForPropertyPath() {
        Method[] ms = OAReflect.getMethods(Sub.class, "name");

        assertNotNull(ms);
        assertEquals(1, ms.length);
        assertEquals("getName", ms[0].getName());
        assertEquals("sub", OAReflect.getPropertyValue(new Sub(), ms));
    }

    @Test
    void getMethodsFindsInterfacePropertyGetterOnImplementation() {
        Method[] ms = OAReflect.getMethods(Sub.class, "label");

        assertNotNull(ms);
        assertEquals(1, ms.length);
        assertEquals("getLabel", ms[0].getName());
        assertEquals("label", OAReflect.getPropertyValue(new Sub(), ms));
    }
}
