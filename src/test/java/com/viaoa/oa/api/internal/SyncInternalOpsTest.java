package com.viaoa.oa.api.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class SyncInternalOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = SyncInternalOps.class;

        assertEquals("com.viaoa.oa.api.internal.SyncInternalOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal", type.getPackageName());
        assertEquals("SyncInternalOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = SyncInternalOps.class;

        assertTrue(type.isInterface(), "SyncInternalOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "SyncInternalOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = SyncInternalOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
