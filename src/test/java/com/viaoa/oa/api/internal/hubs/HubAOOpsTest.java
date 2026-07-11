package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubAOOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubAOOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubAOOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubAOOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubAOOps.class;

        assertTrue(type.isInterface(), "HubAOOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubAOOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubAOOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
