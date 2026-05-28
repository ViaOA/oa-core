package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class OAReflectMethodLookupTest {

    interface Named {
        String name();
    }

    static class Base {
        public String noArgs() {
            return "base";
        }

        public String takesString(String value) {
            return "string:" + value;
        }

        public String takesCharSequence(CharSequence value) {
            return "seq:" + value;
        }

        public String takesObject(Object value) {
            return "object:" + value;
        }

        public String takesNumber(Number value) {
            return "number:" + value;
        }

        public String takesInteger(Integer value) {
            return "integer:" + value;
        }

        public String takesInt(int value) {
            return "int:" + value;
        }

        public String takesNamed(Named value) {
            return "named:" + value.name();
        }

        public String overloaded(String value) {
            return "string:" + value;
        }

        public String overloaded(Object value) {
            return "object:" + value;
        }

        public String ambiguous(String value) {
            return "string:" + value;
        }

        public String ambiguous(Integer value) {
            return "integer:" + value;
        }

        private String hidden() {
            return "hidden";
        }
    }

    static class NamedImpl implements Named {
        @Override
        public String name() {
            return "impl";
        }
    }

    @Test
    void getMethodFindsNoArgMethodCaseInsensitive() {
        Method m = OAReflect.getMethod(Base.class, "NOARGS", 0);

        assertNotNull(m);
        assertEquals("noArgs", m.getName());
    }

    @Test
    void getMethodReturnsNullForNullOrBlankInputs() {
        assertNull(OAReflect.getMethod(null, "noArgs"));
        assertNull(OAReflect.getMethod(Base.class, null));
        assertNull(OAReflect.getMethod(Base.class, ""));
    }

    @Test
    void getMethodHonorsParameterCount() {
        assertNotNull(OAReflect.getMethod(Base.class, "takesString", 1));
        assertNull(OAReflect.getMethod(Base.class, "takesString", 0));
        assertNull(OAReflect.getMethod(Base.class, "takesString", 2));
    }

    @Test
    void getMethodWithArgsFindsExactRuntimeClassCurrentContract() {
        Method m = OAReflect.getMethod(Base.class, "takesString", new Object[] { "abc" });

        assertNotNull(m);
        assertEquals(String.class, m.getParameterTypes()[0]);
    }

    @Test
    void getMethodWithArgsShouldSupportPrimitiveWrapperCompatibility() {
        Method m = OAReflect.getMethod(Base.class, "takesInt", new Object[] { Integer.valueOf(5) });

        assertNotNull(m, "Integer argument should match int parameter");
        assertEquals(int.class, m.getParameterTypes()[0]);
    }

    @Test
    void getMethodWithArgsShouldSupportInterfaceAssignableArguments() {
        Method m = OAReflect.getMethod(Base.class, "takesNamed", new Object[] { new NamedImpl() });

        assertNotNull(m, "NamedImpl argument should match Named parameter");
        assertEquals(Named.class, m.getParameterTypes()[0]);
    }

    @Test
    void getMethodWithArgsShouldSupportSuperclassAssignableArguments() {
        Method m = OAReflect.getMethod(Base.class, "takesNumber", new Object[] { Integer.valueOf(5) });

        assertNotNull(m, "Integer argument should match Number parameter");
        assertEquals(Number.class, m.getParameterTypes()[0]);
    }

    @Test
    void exactOverloadShouldBePreferredOverObjectOverload() {
        Method m = OAReflect.getMethod(Base.class, "overloaded", new Object[] { "abc" });

        assertNotNull(m);
        assertEquals(String.class, m.getParameterTypes()[0]);
    }

    @Test
    void nullArgumentOverloadResolutionIsDocumentedAndDeterministic() {
        Method first = OAReflect.getMethod(Base.class, "ambiguous", new Object[] { null });

        assertNotNull(first);
        for (int i = 0; i < 10; i++) {
            Method again = OAReflect.getMethod(Base.class, "ambiguous", new Object[] { null });
            assertEquals(first, again);
        }
    }

    @Test
    void getMethodClassParamSupportsPrimitiveWrapperPair() {
        Method m = OAReflect.getMethod(Base.class, "takesInt", Integer.class);

        assertNotNull(m);
        assertEquals(int.class, m.getParameterTypes()[0]);
    }

    @Test
    void getMethodClassParamShouldNotTreatAllNumbersAsCompatible() {
        Method m = OAReflect.getMethod(Base.class, "takesInteger", Long.class);

        assertNull(m, "Long.class should not match Integer parameter without converter boundary");
    }

    @Test
    void privateMethodIsNotReturnedByPublicLookup() {
        assertNull(OAReflect.getMethod(Base.class, "hidden", 0));
    }
}
