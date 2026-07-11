package com.viaoa.oa.api.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubsOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubsOps.class;

        assertEquals("com.viaoa.oa.api.internal.HubsOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal", type.getPackageName());
        assertEquals("HubsOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubsOps.class;

        assertTrue(type.isInterface(), "HubsOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubsOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubsOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
