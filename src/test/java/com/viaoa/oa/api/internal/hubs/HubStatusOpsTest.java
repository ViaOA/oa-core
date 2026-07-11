package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubStatusOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubStatusOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubStatusOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubStatusOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubStatusOps.class;

        assertTrue(type.isInterface(), "HubStatusOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubStatusOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubStatusOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
