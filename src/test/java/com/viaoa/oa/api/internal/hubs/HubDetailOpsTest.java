package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubDetailOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubDetailOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubDetailOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubDetailOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubDetailOps.class;

        assertTrue(type.isInterface(), "HubDetailOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubDetailOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubDetailOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
