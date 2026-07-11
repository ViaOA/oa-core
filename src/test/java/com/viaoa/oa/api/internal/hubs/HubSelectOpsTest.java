package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubSelectOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubSelectOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubSelectOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubSelectOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubSelectOps.class;

        assertTrue(type.isInterface(), "HubSelectOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubSelectOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubSelectOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
