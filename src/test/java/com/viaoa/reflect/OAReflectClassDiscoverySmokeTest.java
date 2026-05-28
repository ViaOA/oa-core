package com.viaoa.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAReflectClassDiscoverySmokeTest {

    @Test
    void getClassPathReturnsPathForNormalClass() {
        String path = OAReflect.getClassPath(OAReflect.class);

        assertNotNull(path);
        assertFalse(path.isBlank());
    }

    @Test
    void getClassPathHandlesNullClass() {
        assertNull(OAReflect.getClassPath(null));
    }

    @Test
    void getClassPathForPrimitiveDoesNotThrowUnexpectedNpe() {
        try {
            String path = OAReflect.getClassPath(int.class);
            assertTrue(path == null || path.length() >= 0);
        } catch (NullPointerException ex) {
            fail("getClassPath should not throw NPE for primitive class");
        }
    }

    @Test
    void getOAObjectClassesReturnsPackageClassNames() throws Exception {
        String[] names = OAReflect.getOAObjectClasses("com.viaoa.reflect");

        assertNotNull(names);
        assertTrue(Arrays.asList(names).contains("OAReflect"));
    }

    @Test
    void deprecatedGetClassesDelegatesToOAObjectClasses() throws Exception {
        String[] a = OAReflect.getClasses("com.viaoa.reflect");
        String[] b = OAReflect.getOAObjectClasses("com.viaoa.reflect");

        assertArrayEquals(a, b);
    }

    @Test
    void classDiscoveryReturnsNoInnerClassNames() throws Exception {
        String[] names = OAReflect.getOAObjectClasses("com.viaoa.reflect");

        assertTrue(Arrays.stream(names).noneMatch(s -> s.contains("$")));
    }
}
