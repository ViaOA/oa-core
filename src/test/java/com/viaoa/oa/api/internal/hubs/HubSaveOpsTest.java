package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubSaveOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubSaveOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubSaveOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubSaveOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubSaveOps.class;

        assertTrue(type.isInterface(), "HubSaveOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubSaveOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubSaveOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
