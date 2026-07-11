package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubFindOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubFindOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubFindOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubFindOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubFindOps.class;

        assertTrue(type.isInterface(), "HubFindOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubFindOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubFindOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
