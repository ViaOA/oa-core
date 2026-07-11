package com.viaoa.oa.api.internal.objects;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectSchedulerOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectSchedulerOps.class;

        assertEquals("com.viaoa.oa.api.internal.objects.OAObjectSchedulerOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.objects", type.getPackageName());
        assertEquals("OAObjectSchedulerOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectSchedulerOps.class;

        assertTrue(type.isInterface(), "OAObjectSchedulerOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "OAObjectSchedulerOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectSchedulerOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
