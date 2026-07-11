package com.viaoa.oa.api.services.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubDataOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubDataOps.class;

        assertEquals("com.viaoa.oa.api.services.hubs.HubDataOps", type.getName());
        assertEquals("com.viaoa.oa.api.services.hubs", type.getPackageName());
        assertEquals("HubDataOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubDataOps.class;

        assertTrue(type.isInterface(), "HubDataOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubDataOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubDataOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
