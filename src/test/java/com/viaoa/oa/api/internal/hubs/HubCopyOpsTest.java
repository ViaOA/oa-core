package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubCopyOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubCopyOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubCopyOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubCopyOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubCopyOps.class;

        assertTrue(type.isInterface(), "HubCopyOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubCopyOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubCopyOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
