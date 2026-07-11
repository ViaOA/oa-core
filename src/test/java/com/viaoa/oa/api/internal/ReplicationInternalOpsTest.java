package com.viaoa.oa.api.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class ReplicationInternalOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = ReplicationInternalOps.class;

        assertEquals("com.viaoa.oa.api.internal.ReplicationInternalOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal", type.getPackageName());
        assertEquals("ReplicationInternalOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = ReplicationInternalOps.class;

        assertTrue(type.isInterface(), "ReplicationInternalOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "ReplicationInternalOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = ReplicationInternalOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
