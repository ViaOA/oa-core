package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubCSOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubCSOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubCSOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubCSOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubCSOps.class;

        assertTrue(type.isInterface(), "HubCSOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubCSOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubCSOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
