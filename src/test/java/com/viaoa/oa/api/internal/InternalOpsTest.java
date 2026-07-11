package com.viaoa.oa.api.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class InternalOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = InternalOps.class;

        assertEquals("com.viaoa.oa.api.internal.InternalOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal", type.getPackageName());
        assertEquals("InternalOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = InternalOps.class;

        assertTrue(type.isInterface(), "InternalOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "InternalOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = InternalOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
