package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubDeleteOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubDeleteOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubDeleteOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubDeleteOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubDeleteOps.class;

        assertTrue(type.isInterface(), "HubDeleteOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubDeleteOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubDeleteOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
