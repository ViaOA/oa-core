package com.viaoa.oa.api;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class ReplicationOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = ReplicationOps.class;

        assertEquals("com.viaoa.oa.api.ReplicationOps", type.getName());
        assertEquals("com.viaoa.oa.api", type.getPackageName());
        assertEquals("ReplicationOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = ReplicationOps.class;

        assertTrue(type.isInterface(), "ReplicationOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "ReplicationOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = ReplicationOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
