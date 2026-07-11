package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubEventOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubEventOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubEventOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubEventOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubEventOps.class;

        assertTrue(type.isInterface(), "HubEventOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubEventOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubEventOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
