package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubSortOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubSortOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubSortOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubSortOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubSortOps.class;

        assertTrue(type.isInterface(), "HubSortOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubSortOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubSortOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
