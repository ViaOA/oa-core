package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class OAReflectOverloadDeterminismTest {

    interface Marker {
    }

    static class MarkerImpl implements Marker {
    }

    static class Parent {
    }

    static class Child extends Parent {
    }

    public static class Target {
        public String call(Object value) {
            return "object";
        }

        public String call(String value) {
            return "string";
        }

        public String call(CharSequence value) {
            return "charseq";
        }

        public String call(Number value) {
            return "number";
        }

        public String call(Integer value) {
            return "integer";
        }

        public String marker(Marker value) {
            return "marker";
        }

        public String parent(Parent value) {
            return "parent";
        }

        public String numeric(Integer value) {
            return "integer";
        }

        public String numeric(Long value) {
            return "long";
        }

        public String numeric(BigDecimal value) {
            return "bd";
        }
    }

    @Test
    void exactStringOverloadIsStableAcrossRepeatedLookups() {
        Method first = OAReflect.getMethod(Target.class, "call", new Object[] { "abc" });

        assertNotNull(first);
        assertEquals(String.class, first.getParameterTypes()[0]);

        for (int i = 0; i < 25; i++) {
            assertEquals(first, OAReflect.getMethod(Target.class, "call", new Object[] { "abc" }));
        }
    }

    @Test
    void interfaceAssignableLookupFindsInterfaceMethod() {
        Method m = OAReflect.getMethod(Target.class, "marker", new Object[] { new MarkerImpl() });

        assertNotNull(m);
        assertEquals(Marker.class, m.getParameterTypes()[0]);
    }

    @Test
    void superclassAssignableLookupFindsSuperclassMethod() {
        Method m = OAReflect.getMethod(Target.class, "parent", new Object[] { new Child() });

        assertNotNull(m);
        assertEquals(Parent.class, m.getParameterTypes()[0]);
    }

    @Test
    void nullOverloadResolutionIsDeterministicEvenIfAmbiguous() {
        Method first = OAReflect.getMethod(Target.class, "call", new Object[] { null });

        assertNotNull(first);

        for (int i = 0; i < 25; i++) {
            assertEquals(first, OAReflect.getMethod(Target.class, "call", new Object[] { null }));
        }
    }

    @Test
    void numericClassParamDoesNotInventLossyCompatibility() {
        assertEquals(Integer.class, OAReflect.getMethod(Target.class, "numeric", Integer.class).getParameterTypes()[0]);
        assertEquals(Long.class, OAReflect.getMethod(Target.class, "numeric", Long.class).getParameterTypes()[0]);
        assertEquals(BigDecimal.class, OAReflect.getMethod(Target.class, "numeric", BigDecimal.class).getParameterTypes()[0]);

        assertNull(OAReflect.getMethod(Target.class, "numeric", Double.class));
    }

    @Test
    void sameInvalidLookupFailsConsistently() {
        List<Method> results = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            results.add(OAReflect.getMethod(Target.class, "missing", new Object[] { "x" }));
        }

        assertTrue(results.stream().allMatch(m -> m == null));
    }
}
