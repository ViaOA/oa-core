package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class OAReflectClassDiscoveryEdgeTest {

    @Test
    void getClassPathForArrayClassDoesNotThrowNpe() {
        try {
            String path = OAReflect.getClassPath(String[].class);
            assertTrue(path == null || path.length() >= 0);
        } catch (NullPointerException ex) {
            fail("getClassPath should not throw NPE for array class");
        }
    }

    @Test
    void getClassPathForVoidPrimitiveDoesNotThrowNpe() {
        try {
            String path = OAReflect.getClassPath(void.class);
            assertTrue(path == null || path.length() >= 0);
        } catch (NullPointerException ex) {
            fail("getClassPath should not throw NPE for void.class");
        }
    }

    @Test
    void getOAObjectClassesForMissingPackageReturnsEmptyArray() throws Exception {
        String[] names = OAReflect.getOAObjectClasses("com.viaoa.reflect.package.that.does.not.exist");

        assertNotNull(names);
        assertEquals(0, names.length);
    }

    @Test
    void getOAObjectClassesDoesNotReturnDuplicateLogicalNames() throws Exception {
        String[] names = OAReflect.getOAObjectClasses("com.viaoa.reflect");

        Set<String> set = new HashSet<>(Arrays.asList(names));

        assertEquals(set.size(), names.length, "class discovery should dedupe logical class names");
    }

    @Test
    void getOAObjectClassesIsDeterministicAcrossRepeatedCalls() throws Exception {
        String[] first = OAReflect.getOAObjectClasses("com.viaoa.reflect");
        String[] second = OAReflect.getOAObjectClasses("com.viaoa.reflect");

        assertArrayEquals(first, second);
    }
}
