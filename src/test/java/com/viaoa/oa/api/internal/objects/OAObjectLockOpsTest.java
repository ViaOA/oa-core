package com.viaoa.oa.api.internal.objects;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectLockOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectLockOps.class;

        assertEquals("com.viaoa.oa.api.internal.objects.OAObjectLockOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.objects", type.getPackageName());
        assertEquals("OAObjectLockOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectLockOps.class;

        assertTrue(type.isInterface(), "OAObjectLockOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "OAObjectLockOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectLockOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
