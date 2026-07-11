package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubPropertyOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubPropertyOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubPropertyOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubPropertyOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubPropertyOps.class;

        assertTrue(type.isInterface(), "HubPropertyOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubPropertyOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubPropertyOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
