package com.viaoa.oa.api;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class SyncOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = SyncOps.class;

        assertEquals("com.viaoa.oa.api.SyncOps", type.getName());
        assertEquals("com.viaoa.oa.api", type.getPackageName());
        assertEquals("SyncOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = SyncOps.class;

        assertTrue(type.isInterface(), "SyncOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "SyncOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = SyncOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
