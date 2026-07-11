package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubViewOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubViewOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubViewOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubViewOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubViewOps.class;

        assertTrue(type.isInterface(), "HubViewOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubViewOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubViewOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
