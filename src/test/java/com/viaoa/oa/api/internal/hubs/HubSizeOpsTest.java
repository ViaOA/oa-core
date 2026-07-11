package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubSizeOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubSizeOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubSizeOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubSizeOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubSizeOps.class;

        assertTrue(type.isInterface(), "HubSizeOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubSizeOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubSizeOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
