package com.viaoa.oa.api.services.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubCombineOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubCombineOps.class;

        assertEquals("com.viaoa.oa.api.services.hubs.HubCombineOps", type.getName());
        assertEquals("com.viaoa.oa.api.services.hubs", type.getPackageName());
        assertEquals("HubCombineOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubCombineOps.class;

        assertTrue(type.isInterface(), "HubCombineOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubCombineOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubCombineOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
