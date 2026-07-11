package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubLinkOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubLinkOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubLinkOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubLinkOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubLinkOps.class;

        assertTrue(type.isInterface(), "HubLinkOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubLinkOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubLinkOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
