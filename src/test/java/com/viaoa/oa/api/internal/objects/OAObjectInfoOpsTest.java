package com.viaoa.oa.api.internal.objects;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectInfoOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectInfoOps.class;

        assertEquals("com.viaoa.oa.api.internal.objects.OAObjectInfoOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.objects", type.getPackageName());
        assertEquals("OAObjectInfoOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectInfoOps.class;

        assertTrue(type.isInterface(), "OAObjectInfoOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "OAObjectInfoOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectInfoOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
