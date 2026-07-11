package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubShareOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubShareOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubShareOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubShareOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubShareOps.class;

        assertTrue(type.isInterface(), "HubShareOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubShareOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubShareOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
