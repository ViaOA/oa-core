package com.viaoa.oa.api.internal.objects;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectDSOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectDSOps.class;

        assertEquals("com.viaoa.oa.api.internal.objects.OAObjectDSOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.objects", type.getPackageName());
        assertEquals("OAObjectDSOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectDSOps.class;

        assertTrue(type.isInterface(), "OAObjectDSOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "OAObjectDSOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectDSOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
