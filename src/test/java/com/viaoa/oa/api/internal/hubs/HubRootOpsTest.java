package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubRootOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubRootOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubRootOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubRootOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubRootOps.class;

        assertTrue(type.isInterface(), "HubRootOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubRootOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubRootOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
