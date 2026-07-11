package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubAddRemoveOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubAddRemoveOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubAddRemoveOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubAddRemoveOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubAddRemoveOps.class;

        assertTrue(type.isInterface(), "HubAddRemoveOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubAddRemoveOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubAddRemoveOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
